#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -Xlint:all -Werror -d "$TMP" backend/src/main/java/com/easyhome/common/Rules.java verification/RulesChecks.java
java -cp "$TMP" com.easyhome.RulesChecks
