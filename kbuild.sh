kubectl delete -f k8s/deployment.yaml
docker buildx build --platform linux/amd64,linux/arm64 -t kasbench/globeco-trade-service:latest --push .
kubectl apply -f k8s/deployment.yaml
