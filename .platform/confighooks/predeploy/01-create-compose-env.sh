#!/bin/bash
set -euo pipefail
umask 077

CONFIG_JSON="$(/opt/elasticbeanstalk/bin/get-config environment)"

EB_CONFIG_JSON="$CONFIG_JSON" python3 - <<'PY'
import json
import os
import re

config = json.loads(os.environ["EB_CONFIG_JSON"])

password = config.get("POSTGRES_PASSWORD", "")
cors = config.get("CORS_ALLOWED_ORIGINS", "")

if not re.fullmatch(r"[a-fA-F0-9]{64}", password):
    raise SystemExit("Invalid POSTGRES_PASSWORD.")

if not cors.startswith(("http://", "https://")):
    raise SystemExit("Invalid CORS_ALLOWED_ORIGINS.")

destination = "/var/app/staging/.env"

fd = os.open(destination, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)

with os.fdopen(fd, "w") as output:
    output.write(f"POSTGRES_PASSWORD={password}\n")
    output.write(f"CORS_ALLOWED_ORIGINS={cors}\n")

print("Compose environment file created securely.")
PY
