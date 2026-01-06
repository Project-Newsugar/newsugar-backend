# 모니터링 & 운영 전략

## 1. 툴 스택 (Hybrid)
AWS 관리형이랑 오픈소스 섞어서 씀. 가성비+편의성 세팅.

| 구분 | 도구 | 용도 |
|---|---|---|
| **통합 모니터링** | **CloudWatch** | AWS 리소스 (ALB, EKS, S3 등) 전반적인 상태 감시 |
| **데이터 수집** | **Prometheus** | K8s Pod 및 애플리케이션 지표 수집 |
| **시각화** | **Grafana** | Prometheus 데이터 시각화 (대시보드) |
| **트레이싱** | **AWS X-Ray** | 우리 앱(API) 성능 추적 (서브세그먼트로 상세 분석) |
| **로그 수집** | **Fluent Bit** | 로그 긁어서 CloudWatch로 전송 |
| **배포** | **ArgoCD** | GitOps 자동 배포 |

## 2. 핵심 지표 (이것만 보면 됨)
1. **ALB (로드밸런서)**: 5XX 에러율, 응답 속도 (P95)
2. **EKS (서버)**: 파드 개수, CPU/메모리 사용률
3. **RDS (DB)**: CPU, 커넥션 수
4. **Application**: API 응답 시간 (X-Ray), 힙 메모리 (Prometheus)

## 3. 데이터 흐름
- **로그**: Spring Boot -> 콘솔 -> Fluent Bit -> CloudWatch Logs
- **메트릭**: Spring Boot (Actuator) -> Prometheus (Scrape) -> Grafana
- **트레이싱**: Spring Boot (X-Ray SDK) -> X-Ray Daemon (UDP) -> AWS X-Ray Console
- **AWS 리소스**: AWS 서비스들 -> CloudWatch Metrics

## 4. 알람 정책 (최소화)
시도 때도 없이 울리면 안 보니까 진짜 급한 것만 설정.

| 상황 | 알림 | 대응 |
|---|---|---|
| **서버 에러 (5XX)** | Slack | 로그 확인 후 핫픽스 |
| **CPU 과부하** | Slack | 스케일링 설정 확인 |
| **DB 병목** | Slack | 쿼리 튜닝 필요 |
| **헬스체크 실패** | **전화/PagerDuty** | **비상 사태 (DR 가동 고려)** |

## 5. 배포 파이프라인 (GitOps)
1. **GitHub**: 코드 푸시
2. **Action**: 도커 빌드 -> ECR 업로드
3. **ArgoCD**: 새 이미지 감지 -> EKS 배포 (자동)
   - **Dev**: 바로 배포
   - **Prod**: Blue/Green 무중단 배포
