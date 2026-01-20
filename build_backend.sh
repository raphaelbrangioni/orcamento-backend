#!/usr/bin/env bash
set -euo pipefail

# Helper to build and push the backend Spring Boot image
# Usage:
#   DOCKER_USER=<dockerhub_user> ./build_backend.sh <version>
# Example:
#   DOCKER_USER=rbrangioni ./build_backend.sh 1.0.0
# Notes:
# - Requires: docker login
# - Builds and pushes two tags: <version> and latest
# - Uses buildx (linux/amd64) if available

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found in PATH" >&2
  exit 1
fi

DOCKER_USER=${DOCKER_USER:-}
if [[ -z "${DOCKER_USER}" ]]; then
  echo "Please set DOCKER_USER environment variable. Example: DOCKER_USER=rbrangioni" >&2
  exit 1
fi

if [[ $# -lt 1 ]]; then
  echo "Usage: DOCKER_USER=${DOCKER_USER:-<user>} $0 <version>" >&2
  exit 1
fi

VERSION="$1"
IMAGE_NAME="orcamento-backend"
IMAGE_FULL_VER="${DOCKER_USER}/${IMAGE_NAME}:${VERSION}"
IMAGE_LATEST="${DOCKER_USER}/${IMAGE_NAME}:latest"

if command -v docker buildx >/dev/null 2>&1; then
  echo "[info] Using docker buildx (platform linux/amd64)"
  docker buildx build --platform linux/amd64 \
    -t "${IMAGE_FULL_VER}" \
    -t "${IMAGE_LATEST}" \
    --push .
else
  echo "[info] Using docker build"
  docker build -t "${IMAGE_FULL_VER}" -t "${IMAGE_LATEST}" .
  docker push "${IMAGE_FULL_VER}"
  docker push "${IMAGE_LATEST}"
fi

echo
echo "[done] Pushed: ${IMAGE_FULL_VER} and ${IMAGE_LATEST}"
echo
cat <<EOF
Next steps:
1) On cluster master, set the Deployment image (after we create the Helm chart or a plain manifest):
   kubectl -n orcamento set image deploy/orcamento-backend orcamento-backend=${IMAGE_FULL_VER}
   kubectl -n orcamento rollout status deploy/orcamento-backend

2) JDBC URL to use in Kubernetes (via env/Secret):
   jdbc:mysql://mysql-external.orcamento.svc.cluster.local:3306/orcamento?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
EOF
