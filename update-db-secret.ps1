# Update-DB-Secret.ps1
# Terraform Apply 후 변경된 RDS Endpoint를 자동으로 K8s Secret에 업데이트하는 스크립트

# 1. Terraform으로 클러스터 정보 갱신 (EKS 접속 정보가 바뀔 수 있음)
Write-Host "Kubeconfig 업데이트 중..." -ForegroundColor Cyan
aws eks update-kubeconfig --region ap-northeast-2 --name newsugar-prod-eks

Write-Host "RDS 엔드포인트 조회 중..." -ForegroundColor Cyan
$rds_endpoint = aws rds describe-db-instances --query "DBInstances[0].Endpoint.Address" --output text

if (-not $rds_endpoint) {
    Write-Host " RDS 인스턴스를 찾을 수 없습니다. Terraform이 정상적으로 실행되었는지 확인하세요." -ForegroundColor Red
    exit 1
}

Write-Host "RDS Endpoint 발견: $rds_endpoint" -ForegroundColor Green

# JDBC URL 구성
$db_url = "jdbc:mysql://$($rds_endpoint):3306/news_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"

Write-Host "Kubernetes Secret (newsugar-secrets-prod) 업데이트 중..." -ForegroundColor Cyan

# 기존 Secret 백업 (선택 사항)
# kubectl get secret newsugar-secrets-prod -o yaml > newsugar-secrets-prod.backup.yaml

# Secret 업데이트 (Dry Run 후 Apply)
# 주의: db-url 키만 업데이트합니다.
# kubectl create secret generic newsugar-secrets-prod --from-literal=db-url=$db_url --dry-run=client -o yaml | kubectl apply -f -

# 더 안전한 방법: patch 사용
# Base64 인코딩
$db_url_base64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($db_url))

$patch_json = @"
{
    "data": {
        "db-url": "$db_url_base64"
    }
}
"@

# Patch 적용
$patch_file = "secret-patch.json"
$patch_json | Out-File -FilePath $patch_file -Encoding ASCII

kubectl patch secret newsugar-secrets-prod --type merge --patch-file $patch_file

# 임시 파일 삭제
Remove-Item $patch_file

Write-Host "Secret 업데이트 완료!" -ForegroundColor Green

Write-Host "백엔드 파드 재시작 중..." -ForegroundColor Cyan
kubectl rollout restart deployment newsugar-backend-prod

Write-Host "모든 작업이 완료되었습니다. 잠시 후 파드가 새 DB 주소로 연결됩니다." -ForegroundColor Yellow
