# 쿠버네티스 클러스터 부트스트랩 Makefile
# 사용법: make [command]
# 예: make all

.PHONY: all setup deploy clean update-secret

# 기본 변수 설정
REGION := ap-northeast-2
CLUSTER_NAME := newsugar-prod-eks
ACCOUNT_ID := 061039804626

# 전체 설치 및 배포 (이거 하나면 끝)
all: setup deploy
	@echo "========================================================"
	@echo "🚀  모든 설치 및 배포가 완료되었습니다!"
	@echo "    ArgoCD가 잠시 후 애플리케이션을 동기화할 것입니다."
	@echo "========================================================"

# 1. 필수 인프라 도구 설치 (ArgoCD, External Secrets)
setup:
	@echo "🔄 [1/3] Helm 리포지토리 추가 중..."
	helm repo add external-secrets https://charts.external-secrets.io
	helm repo update
	
	@echo "🔄 [2/3] External Secrets Operator 설치 중..."
	helm upgrade --install external-secrets external-secrets/external-secrets \
		-n external-secrets \
		--create-namespace \
		--set installCRDs=true
	
	@echo "🔄 [3/3] ArgoCD 설치 중..."
	kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
	kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
	@echo "✅  인프라 도구 설치 완료!"

# 2. 애플리케이션 배포 (ArgoCD App 등록)
deploy:
	@echo "🚀 ArgoCD Application 등록 중..."
	kubectl apply -f k8s/argocd-app.yaml
	@echo "✅  애플리케이션 배포 시작!"

# 유틸리티: AWS Secrets Manager 값 갱신 시 파드 재시작
restart:
	@echo "🔄 파드 재시작 (새로운 시크릿 적용)..."
	kubectl rollout restart deployment newsugar-backend-prod
	@echo "✅  재시작 완료!"

# 유틸리티: ArgoCD 비밀번호 확인
get-pass:
	@echo "🔑 ArgoCD Admin 비밀번호:"
	@kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
	@echo ""

# 청소: 모든 리소스 삭제
clean:
	@echo "🗑️  모든 리소스 삭제 중..."
	kubectl delete -f k8s/argocd-app.yaml --ignore-not-found
	helm uninstall external-secrets -n external-secrets --ignore-not-found
	kubectl delete ns argocd --ignore-not-found
	kubectl delete ns external-secrets --ignore-not-found
	@echo "✨  클린 완료!"
