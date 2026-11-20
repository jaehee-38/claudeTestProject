#!/usr/bin/env bash
# Codespace Secrets → .env 자동 생성 스크립트
# Codespace Secrets에 등록된 값이 환경변수로 주입됩니다.

ENV_FILE="$(dirname "$0")/../.env"

cat > "$ENV_FILE" <<EOF
DB_URL=${DB_URL}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

DATA_GO_KR_SERVICE_KEY=${DATA_GO_KR_SERVICE_KEY}

KOREAEXIM_AUTH_KEY=${KOREAEXIM_AUTH_KEY}

KIS_APP_KEY=${KIS_APP_KEY}
KIS_APP_SECRET=${KIS_APP_SECRET}
KIS_ACCOUNT_NO=${KIS_ACCOUNT_NO}
KIS_MOCK=${KIS_MOCK:-true}
EOF

echo ".env generated from Codespace Secrets"
