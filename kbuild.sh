docker buildx build --platform linux/amd64,linux/arm64 \
-t kasbench/globeco-trade-service:latest \
-t kasbench/globeco-trade-service:1.0.1 \
--push .
kubectl delete -f k8s/deployment.yaml
kubectl apply -f k8s/deployment.yaml
