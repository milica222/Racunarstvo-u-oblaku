#!/bin/bash
set -e

NAMESPACE="milicav"
KUBECTL="microk8s.kubectl"

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

K_FOLDS=$(awk -F': ' '/K_FOLDS:/ {
    gsub(/"/, "", $2)
    print $2
}' k8s/configmap.yaml)

echo "Namespace: $NAMESPACE"
echo "Broj foldova: $K_FOLDS"
echo

"${KUBECTL[@]}" delete job cross-validation \
    -n "$NAMESPACE" \
    --ignore-not-found=true

"${KUBECTL[@]}" apply \
    -n "$NAMESPACE" \
    -f k8s/configmap.yaml

"${KUBECTL[@]}" create configmap cv-code \
    -n "$NAMESPACE" \
    --from-file=main.py=src/main.py \
    --dry-run=client \
    -o yaml \
    | "${KUBECTL[@]}" apply \
        -n "$NAMESPACE" \
        -f -

"${KUBECTL[@]}" create configmap cv-data \
    -n "$NAMESPACE" \
    --from-file=HousingData.csv=data/HousingData.csv \
    --dry-run=client \
    -o yaml \
    | "${KUBECTL[@]}" apply \
        -n "$NAMESPACE" \
        -f -

sed "s/__K_FOLDS__/$K_FOLDS/g" \
    k8s/job.yaml \
    | "${KUBECTL[@]}" apply \
        -n "$NAMESPACE" \
        -f -

"${KUBECTL[@]}" wait \
    -n "$NAMESPACE" \
    --for=condition=complete \
    job/cross-validation \
    --timeout=30m

mkdir -p results
> results/folds.jsonl

PODS=$(
    "${KUBECTL[@]}" get pods \
        -n "$NAMESPACE" \
        -l job-name=cross-validation \
        -o jsonpath='{range .items[?(@.status.phase=="Succeeded")]}{.metadata.name}{"\n"}{end}'
)

for POD in $PODS
do
    "${KUBECTL[@]}" logs "$POD" \
        -n "$NAMESPACE" \
        | sed -n 's/^RESULT_JSON=//p' \
        >> results/folds.jsonl
done

K_FOLDS="$K_FOLDS" python3 scripts/aggregate_results.py
