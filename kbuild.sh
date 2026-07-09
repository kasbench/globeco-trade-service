#!/bin/bash
set -e

VERSION="1.0.3"
IMAGE="kasbench/globeco-trade-service"
BUILDER="globeco-multiarch"

# Ensure a buildx builder with the docker-container driver exists. This driver
# is required for multi-platform builds and for pushing a single multi-arch
# manifest list. The default "docker" driver cannot do either.
if ! docker buildx inspect "${BUILDER}" >/dev/null 2>&1; then
  docker buildx create --name "${BUILDER}" --driver docker-container --bootstrap --use
else
  docker buildx use "${BUILDER}"
fi

# Register QEMU emulators so the non-native architecture can run its AOT
# training run during the build (safe to re-run; no-op if already installed).
docker run --privileged --rm tonistiigi/binfmt --install all >/dev/null 2>&1 || true

# Build both architectures and push a single multi-arch manifest list
# (linux/amd64 + linux/arm64), each with its own architecture-matched AOT cache.
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag "${IMAGE}:${VERSION}" \
  --tag "${IMAGE}:latest" \
  --push \
  .

# Redeploy to Kubernetes
kubectl delete -f k8s/deployment.yaml
kubectl apply -f k8s/deployment.yaml
