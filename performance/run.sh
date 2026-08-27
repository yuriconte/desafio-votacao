#!/usr/bin/env sh
set -eu

PROFILE="${1:-smoke}"
case "$PROFILE" in
  smoke|load|volume) ;;
  *) echo "Perfil invalido: $PROFILE (use smoke, load ou volume)" >&2; exit 2 ;;
esac

REPO_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
RUN_ID="$(date +%Y%m%d-%H%M%S)-$PROFILE"
RESULT_DIR="$REPO_ROOT/performance-results/$RUN_ID"
COMPOSE_FILE="$REPO_ROOT/compose.performance.yaml"
mkdir -p "$RESULT_DIR"

export PERF_PROFILE="$PROFILE"
export PERF_RUN_ID="$RUN_ID"
export PERF_RATE="${PERF_RATE:-100}"
export PERF_DURATION="${PERF_DURATION:-2m}"
export PERF_TOTAL_VOTES="${PERF_TOTAL_VOTES:-200000}"
export PERF_VUS="${PERF_VUS:-100}"
export PERF_PREALLOCATED_VUS="${PERF_PREALLOCATED_VUS:-100}"

COMMIT=$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown)
DIRTY=false
if [ -n "$(git -C "$REPO_ROOT" status --porcelain 2>/dev/null || true)" ]; then DIRTY=true; fi
cat > "$RESULT_DIR/metadata.json" <<EOF
{
  "runId": "$RUN_ID",
  "executedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "profile": "$PROFILE",
  "gitCommit": "$COMMIT",
  "workingTreeDirty": $DIRTY,
  "docker": "$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo unknown)",
  "compose": "$(docker compose version --short 2>/dev/null || echo unknown)",
  "parameters": {
    "ratePerSecond": $PERF_RATE,
    "duration": "$PERF_DURATION",
    "totalVotes": $PERF_TOTAL_VOTES,
    "vus": $PERF_VUS,
    "preAllocatedVUs": $PERF_PREALLOCATED_VUS
  }
}
EOF

echo "Execucao: $RUN_ID"
echo "Dashboard durante o teste: http://localhost:5665"
echo "Resultados: $RESULT_DIR"

cleanup() {
  docker compose -f "$COMPOSE_FILE" down -v --remove-orphans
}
trap cleanup EXIT

docker compose -f "$COMPOSE_FILE" down -v --remove-orphans
docker compose -f "$COMPOSE_FILE" build perf-app
docker compose -f "$COMPOSE_FILE" up --no-build -d perf-app
set +e
docker compose -f "$COMPOSE_FILE" run --rm --service-ports k6 > "$RESULT_DIR/console.log" 2>&1
K6_EXIT_CODE=$?
set -e
cat "$RESULT_DIR/console.log"
if [ "$K6_EXIT_CODE" -ne 0 ]; then
  echo "O perfil $PROFILE falhou (codigo $K6_EXIT_CODE). Consulte $RESULT_DIR." >&2
  exit "$K6_EXIT_CODE"
fi

echo "Teste aprovado. Abra: $RESULT_DIR/report.html"
