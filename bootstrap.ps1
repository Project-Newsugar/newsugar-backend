# Infrastructure Bootstrap Script
# Usage: .\bootstrap.ps1 [-SecretName "newsugar/prod/db/auth-xxxx"]

param (
    [string]$SecretName = ""
)

$ErrorActionPreference = "Stop"

Write-Host ">>> [Bootstrap] Starting Infrastructure Setup..."

# 1. Update AWS Secret Manager Key
if ($SecretName -ne "") {
    $secretFile = "k8s\prod\external-secret.yaml"
    if (Test-Path $secretFile) {
        $content = Get-Content $secretFile -Raw
        $newContent = $content -replace "key: newsugar/prod/db/auth.*", "key: $SecretName"
        Set-Content -Path $secretFile -Value $newContent
        Write-Host ">>> [Secret] Updated ExternalSecret key"
    }
}

# 2. Setup Helm Repositories
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add argocd https://argoproj.github.io/argo-helm
helm repo update

# 3. Install/Upgrade Prometheus Stack
Write-Host ">>> [Prometheus] Installing kube-prometheus-stack..."
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack `
    --namespace monitoring `
    --set grafana.adminPassword="newsugar!2026" `
    --set grafana.service.type=LoadBalancer `
    --wait

# 4. Install/Upgrade ArgoCD
Write-Host ">>> [ArgoCD] Installing ArgoCD..."
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install argocd argocd/argo-cd `
    --namespace argocd `
    --set server.service.type=LoadBalancer `
    --wait

# 5. Apply Manifests
Write-Host ">>> [App] Applying Kubernetes Manifests..."
kubectl create namespace prod --dry-run=client -o yaml | kubectl apply -f -

if (Test-Path "k8s\prod\external-secret.yaml") { kubectl apply -f k8s\prod\external-secret.yaml }
if (Test-Path "k8s\prod\redis.yaml") { kubectl apply -f k8s\prod\redis.yaml }

if (Test-Path "k8s\prod") {
    kubectl apply -f k8s\prod\deployment.yaml
    kubectl apply -f k8s\prod\service.yaml
    kubectl apply -f k8s\prod\newsugar-frontend-prod.yaml
    kubectl apply -f k8s\prod\hpa.yaml
    if (Test-Path "k8s\prod\servicemonitor.yaml") { kubectl apply -f k8s\prod\servicemonitor.yaml }
}

Write-Host ">>> [Bootstrap] Completed. Grafana PW: newsugar!2026"
