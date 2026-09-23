#!/bin/bash
set -e

NAMESPACE="milicav"
KUBECTL="microk8s.kubectl"
IMAGE="milicavasovic222/wine-ml-api:latest"

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

"${KUBECTL[@]}" apply -n "$NAMESPACE" -f k8s/configmap.yaml
"${KUBECTL[@]}" apply -n "$NAMESPACE" -f k8s/pvc.yaml

sed "s|__IMAGE__|$IMAGE|g" k8s/deployment.yaml | "${KUBECTL[@]}" apply -n "$NAMESPACE" -f -

"${KUBECTL[@]}" apply -n "$NAMESPACE" -f k8s/service.yaml

"${KUBECTL[@]}" rollout status deployment/wine-ml-api -n "$NAMESPACE" --timeout=5m

POD=$("${KUBECTL[@]}" get pods -n "$NAMESPACE" -l app=wine-ml-api -o jsonpath='{.items[0].metadata.name}')

cat data/wine.csv | "${KUBECTL[@]}" exec -i -n "$NAMESPACE" "$POD" -- sh -c 'cat > /data/wine.csv'

echo "Deployment zavrsen."
echo "Pod: $POD"