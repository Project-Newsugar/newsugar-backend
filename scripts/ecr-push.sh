#!/bin/bash
# ECR Push Script for Git Bash (Auto-configured)
# 쓰는 법: ./scripts/ecr-push.sh -e dev (운영은 -e prod). 옵션 안 넣으면 기본값으로 돕니다.

# 귀찮아서 박아놓은 기본값들.
DEFAULT_REGION="ap-northeast-2"
DEFAULT_ACCOUNT_ID="061039804626"
DEFAULT_ENV="dev"

REGION=$DEFAULT_REGION
ACCOUNT_ID=$DEFAULT_ACCOUNT_ID
ENV=$DEFAULT_ENV

# 옵션 들어오면 덮어씁니다.
while getopts "r:a:e:" opt; do
  case $opt in
    r) REGION="$OPTARG"
    ;;
    a) ACCOUNT_ID="$OPTARG"
    ;;
    e) ENV="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    exit 1
    ;;
  esac
done

echo "============================================="
echo "AWS ECR Image Push Tool"
echo "Region: $REGION"
echo "Account ID: $ACCOUNT_ID"
echo "Environment: $ENV"
echo "============================================="

REPO_NAME="newsugar-backend"
IMAGE_TAG="$ENV"
ECR_URI="$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$REPO_NAME"
FULL_IMAGE_NAME="$ECR_URI:$IMAGE_TAG"

echo "1. AWS ECR 로그인 뚫습니다."
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"

if [ $? -ne 0 ]; then
    echo "Error: ECR Login failed."
    echo "Check if 'aws configure' is set up correctly."
    exit 1
fi

echo "2. 도커 이미지 굽습니다."
docker build -t $FULL_IMAGE_NAME .

if [ $? -ne 0 ]; then
    echo "Error: Docker build failed."
    exit 1
fi

echo "3. ECR로 올립니다."
docker push $FULL_IMAGE_NAME

if [ $? -ne 0 ]; then
    echo "Error: Docker push failed."
    exit 1
fi

echo "=== Success! Image pushed to: $FULL_IMAGE_NAME ==="
echo "Ready to deploy!"
