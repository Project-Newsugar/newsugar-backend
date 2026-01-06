# Update K8s DB Configuration Script
# Usage: ./update-db-config.ps1 -DbUrl "jdbc:..." -DbUser "admin" -DbPassword "password"

param (
    [Parameter(Mandatory=$true)]
    [string]$DbUrl,

    [Parameter(Mandatory=$true)]
    [string]$DbUser,

    [Parameter(Mandatory=$true)]
    [string]$DbPassword
)

# Function to encode to Base64
function ConvertTo-Base64($String) {
    $Bytes = [System.Text.Encoding]::UTF8.GetBytes($String)
    return [Convert]::ToBase64String($Bytes)
}

$EncodedUrl = ConvertTo-Base64 $DbUrl
$EncodedUser = ConvertTo-Base64 $DbUser
$EncodedPassword = ConvertTo-Base64 $DbPassword

Write-Host "Updating Kubernetes Secrets..."
Write-Host "URL: $DbUrl"
Write-Host "User: $DbUser"

# Patch the Secret with all new values
# We update db-url, db-username, and db-password keys
kubectl patch secret newsugar-secrets-prod --type='json' -p="[
    {'op': 'replace', 'path': '/data/db-url', 'value': '$EncodedUrl'},
    {'op': 'replace', 'path': '/data/db-username', 'value': '$EncodedUser'},
    {'op': 'replace', 'path': '/data/db-password', 'value': '$EncodedPassword'}
]"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Secrets updated successfully."
    
    Write-Host "Restarting Backend Pods to apply changes..."
    kubectl delete pod -l app=newsugar-backend
    
    Write-Host "Done! Pods are restarting with new DB connection info."
} else {
    Write-Host "Failed to update secrets. Please check your connection." -ForegroundColor Red
}
