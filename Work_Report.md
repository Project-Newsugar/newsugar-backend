# 백엔드 인프라 작업 내역

## 1. 작업 요약
- **환경 분리**: Dev(개발) / Prod(운영) 완전 분리 (설정, DB, 리소스 등)
- **배포 자동화**: 스크립트 하나로 ECR 빌드/푸시 끝냄
- **운영 준비**: 모니터링, 로깅 설계 완료 & GitOps(ArgoCD) 준비

---

## 2. 환경별 차이점

| 구분 | Dev (개발) | Prod (운영) |
|---|---|---|
| **목적** | 기능 테스트 | 실제 서비스 |
| **설정** | `application-dev.properties` | `application-prod.properties` |
| **DB** | 스키마 자동 변경 (`update`) | 변경 차단 (`validate`) |
| **규모** | 1대 고정 (비용 절약) | 2~10대 자동 조절 (HPA) |
| **배포** | `k8s/dev/` | `k8s/prod/` |

---

## 3. 사용법 (3단계 컷)

### 1) 로컬 개발
- 평소처럼 코딩.

### 2) 이미지 배포 (ECR)
- Git Bash 켜고 아래 명령어 실행.
```bash
# 권한 부여 (처음 한 번만)
chmod +x scripts/ecr-push.sh

# 개발섭 배포
./scripts/ecr-push.sh -e dev

# 운영섭 배포
./scripts/ecr-push.sh -e prod
```

### 3) K8s 적용
- 이미지 올라가면 K8s에 반영. (ArgoCD 붙으면 이 단계 삭제됨)
```bash
# Dev 반영
kubectl apply -f k8s/dev/deployment.yaml

# Prod 반영
kubectl apply -f k8s/prod/deployment.yaml
```

---

## 4. 인프라 툴 역할 정리 (족보)

| 도구 | 역할 | 비유 | 우리 할 일 |
|---|---|---|---|
| **Terraform** | EKS 클러스터 생성 | 건물 짓기 | 없음 (인프라팀 영역) |
| **Kubernetes** | 서버/서비스 설정 파일 (`yaml`) | 입주민 명단 | **우리가 만든 파일들 (핵심)** |
| **ArgoCD** | `yaml` 변경 감지해서 자동 배포 | 이사짐 센터 | 나중에 연동만 하면 됨 |
| **k9s** | 터미널 UI로 서버 상태 확인 | CCTV | **개인 PC에 깔아서 쓰면 개편함** |

---

## 5. 폴더 구조
```text
k8s/
├── dev/  # 개발계 설정
├── prod/ # 운영계 설정
└── argocd-app-*.yaml # ArgoCD 연동용
scripts/
└── ecr-push.sh # 배포 스크립트
Monitoring.md # 모니터링 전략
```
