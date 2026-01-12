# PowerShell script for Newsugar Backend Production Deployment Automation
# Usage: ./scripts/deploy-automation.ps1

# 1. Configuration Values
$DB_HOST = "newsugar-prod-aurora-cluster.cluster-c3qme6c6e7fj.ap-northeast-2.rds.amazonaws.com"
$DB_URL = "jdbc:mysql://${DB_HOST}:3306/news_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
$DB_USER = "admin"
$DB_PASS = "9NRVL&u-[byv9GS9"

# API Keys (Found in codebase/templates - Update if necessary)
$NEWS_API_KEY = "23c9561ec76e479fa72778d73ff629f6"
$QUIZ_AI_API_KEY = "AIzaSyBQvfAPhljpve7j0pkdPXepBbX3fCIe8H8"
$JWT_SECRET = "your_jwt_secret_key_must_be_long_enough_for_security_reasons"
$JWT_REFRESH_SECRET = "your_jwt_refresh_secret_key_must_be_long_enough"

Write-Host "========================================================"
Write-Host "   Newsugar Backend Production Deployment Automation    "
Write-Host "========================================================"

# 2. Update Kubernetes Secrets
Write-Host "`n[Step 1] Updating Kubernetes Secrets..."

# 2-1. db-secret (For Spring Boot Datasource)
Write-Host "  - Creating 'db-secret'..."
kubectl create secret generic db-secret `
    --from-literal=SPRING_DATASOURCE_URL=$DB_URL `
    --from-literal=SPRING_DATASOURCE_USERNAME=$DB_USER `
    --from-literal=SPRING_DATASOURCE_PASSWORD=$DB_PASS `
    --dry-run=client -o yaml | kubectl apply -f -

# 2-2. newsugar-secrets-prod (For Application Logic)
Write-Host "  - Creating 'newsugar-secrets-prod'..."
kubectl create secret generic newsugar-secrets-prod `
    --from-literal=NEWS_API_KEY=$NEWS_API_KEY `
    --from-literal=QUIZ_AI_API_KEY=$QUIZ_AI_API_KEY `
    --from-literal=JWT_SECRET=$JWT_SECRET `
    --from-literal=JWT_REFRESH_SECRET=$JWT_REFRESH_SECRET `
    --dry-run=client -o yaml | kubectl apply -f -

Write-Host "Secrets updated successfully!" -ForegroundColor Green

# 3. Apply ArgoCD Applications
Write-Host "`n[Step 2] Deploying ArgoCD Applications..."

# 3-1. Monitoring Stack (Prometheus + Grafana + AWS Logging)
Write-Host "  - Deploying Prometheus & Grafana Stack..."
kubectl apply -f k8s/argocd/prometheus-stack-app.yaml

Write-Host "  - Deploying AWS Logging (Fluent Bit & CW Agent)..."
kubectl apply -f k8s/argocd/monitoring-app.yaml

# 3-2. Backend Application
Write-Host "  - Deploying Backend Application..."
kubectl apply -f k8s/argocd/newsugar-backend-app.yaml

Write-Host "`n[Step 3] Verifying Deployment..."
Write-Host "Check ArgoCD dashboard or run 'kubectl get applications -n argocd'"

Write-Host "`nDeployment process initiated! ArgoCD will sync the changes shortly." -ForegroundColor Cyan
