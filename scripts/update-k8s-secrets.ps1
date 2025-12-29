# PowerShell script to update Kubernetes secrets for Production
# Usage: ./update-k8s-secrets.ps1

# Define secret values
$DB_USER = "admin"
$DB_PASS = "p)XWs2PQNq%4tJku"
$NEWS_API_KEY = "23c9561ec76e479fa72778d73ff629f6"
$QUIZ_AI_API_KEY = "AIzaSyBs82S0035DIYzVOWvU_3-Z99u2sOEjrj0"
$JWT_SECRET = "your_jwt_secret_key_must_be_long_enough_for_security_reasons"
$JWT_REFRESH_SECRET = "your_jwt_refresh_secret_key_must_be_long_enough"

Write-Host "Updating newsugar-secrets-prod..."

# Create or Update Secret (Dry run to yaml -> apply)
# Using generic secret creation
kubectl create secret generic newsugar-secrets-prod `
    --from-literal=db-username=$DB_USER `
    --from-literal=db-password=$DB_PASS `
    --from-literal=news-api-key=$NEWS_API_KEY `
    --from-literal=quiz-ai-api-key=$QUIZ_AI_API_KEY `
    --from-literal=jwt-secret=$JWT_SECRET `
    --from-literal=jwt-refresh-secret=$JWT_REFRESH_SECRET `
    --dry-run=client -o yaml | kubectl apply -f -

if ($LASTEXITCODE -eq 0) {
    Write-Host "Secrets updated successfully!" -ForegroundColor Green
} else {
    Write-Host "Failed to update secrets." -ForegroundColor Red
}
