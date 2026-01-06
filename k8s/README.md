# K8s 배포 가이드

## 1. 기본 정보
- **Region**: 서울 (`ap-northeast-2`)
- **Account**: `0610-3980-4626`
- **ECR**: `newsugar-backend`
- **DR**: 도쿄 예정 (현재는 서울만)

## 2. 필수 준비 (Secret 등록)
배포 전에 이거 안 하면 서버 에러 남. (DB 비번 등)

### Dev (개발)
```bash
kubectl create secret generic newsugar-secrets-dev \
  --from-literal=db-url='jdbc:mysql://newsugar-db-dev:3306/news_db?useSSL=false' \
  --from-literal=db-username='dev_user' \
  --from-literal=db-password='dev_password' \
  --from-literal=news-api-key='키값' \
  --from-literal=quiz-ai-api-key='키값'
```

### Prod (운영)
DB/Redis 확정 전이라도 일단 넣어놔야 배포 됨.
```bash
kubectl create secret generic newsugar-secrets-prod \
  --from-literal=db-url='jdbc:mysql://prod-db-endpoint:3306/news_db?useSSL=false' \
  --from-literal=db-username='prod_user' \
  --from-literal=db-password='prod_password' \
  --from-literal=news-api-key='키값' \
  --from-literal=quiz-ai-api-key='키값'
```

## 3. 이미지 배포 (ECR)
스크립트에 계정 정보 다 박아둠. 옵션만 주면 됨.
```bash
chmod +x scripts/ecr-push.sh

# 개발용
./scripts/ecr-push.sh -e dev

# 운영용
./scripts/ecr-push.sh -e prod
```

## 4. K8s 적용
ArgoCD 쓰기 전까진 수동 적용.
```bash
# Dev
kubectl apply -f k8s/dev/

# Prod (임시 Redis 포함)
kubectl apply -f k8s/prod/
```

## 5. 참고
- **DR**: 나중에 도쿄 리전 뚫으면 `k8s/dr/` 폴더 따로 파는 거 추천.
- **이미지**: ECR 리전 복제 켜두면 도쿄에서도 바로 쓸 수 있음.
