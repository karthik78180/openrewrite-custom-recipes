#!/usr/bin/env bash
# PreToolUse hook: validates Maven Central GAVs in build files before Edit/Write commits them.
#
# Reads tool input as JSON on stdin, extracts file path + proposed content, scans for
# Gradle string-notation dependencies (group:artifact:version), and checks repo1.maven.org
# directly with a HEAD request. Exits 2 to BLOCK only when the registry returns 404.
# Exits 0 (allow) on network failure, timeout, missing tools, or any ambiguity — we
# never want this hook to block real work because the network is slow.
#
# Why direct repo lookups instead of solrsearch: solrsearch is slow (often >5s per
# request) and sometimes misses recent versions. A HEAD on repo1.maven.org/maven2 is
# fast and authoritative.
#
# Why this exists: prior sessions referenced library versions that didn't exist on
# Maven Central (e.g. vertx-junit5 5.1.0), forcing rework across multiple files.

set -u

command -v jq   >/dev/null 2>&1 || exit 0
command -v curl >/dev/null 2>&1 || exit 0

PAYLOAD="$(cat)"
[ -z "$PAYLOAD" ] && exit 0

TOOL_NAME="$(printf '%s' "$PAYLOAD" | jq -r '.tool_name // empty' 2>/dev/null)"
case "$TOOL_NAME" in
  Edit|Write|MultiEdit) ;;
  *) exit 0 ;;
esac

FILE_PATH="$(printf '%s' "$PAYLOAD" | jq -r '.tool_input.file_path // empty' 2>/dev/null)"
[ -z "$FILE_PATH" ] && exit 0

case "$FILE_PATH" in
  *.gradle|*.gradle.kts|*.toml|*pom.xml) ;;
  *) exit 0 ;;
esac

CANDIDATE="$(printf '%s' "$PAYLOAD" | jq -r '
  (.tool_input.new_string // "") + "\n" +
  (.tool_input.content    // "") + "\n" +
  ((.tool_input.edits // []) | map(.new_string // "") | join("\n"))
' 2>/dev/null)"

[ -z "$CANDIDATE" ] && exit 0

# Match "group:artifact:version" in single or double quotes.
# group/artifact: alnum + . _ -; must start with a letter.
# version: must start with a digit (filters out variable refs like $version).
GAVS="$(
  printf '%s' "$CANDIDATE" \
  | grep -oE "['\"][a-zA-Z][a-zA-Z0-9._-]+:[a-zA-Z][a-zA-Z0-9._-]+:[0-9][a-zA-Z0-9._+-]+['\"]" \
  | tr -d "'\"" \
  | sort -u
)"

[ -z "$GAVS" ] && exit 0

MISSING=""
while IFS= read -r gav; do
  [ -z "$gav" ] && continue
  GROUP="${gav%%:*}"
  REST="${gav#*:}"
  ARTIFACT="${REST%%:*}"
  VERSION="${REST#*:}"
  [ -z "$GROUP" ] || [ -z "$ARTIFACT" ] || [ -z "$VERSION" ] && continue

  # group dots become path slashes
  GROUP_PATH="${GROUP//.//}"
  URL="https://repo1.maven.org/maven2/${GROUP_PATH}/${ARTIFACT}/${VERSION}/${ARTIFACT}-${VERSION}.pom"

  # HEAD with 5s timeout. -I = HEAD; -o /dev/null discards body. -w prints status code.
  STATUS="$(curl -sS -I -o /dev/null -w "%{http_code}" --max-time 5 "$URL" 2>/dev/null)"

  case "$STATUS" in
    200) ;;            # found, ok
    404) MISSING="${MISSING}${gav}"$'\n' ;;
    *)   ;;            # network failure / timeout / 5xx — allow, don't block
  esac
done <<<"$GAVS"

if [ -n "$MISSING" ]; then
  {
    echo "Maven Central does not have these dependencies referenced in $FILE_PATH:"
    printf '%s' "$MISSING" | sed 's/^/  - /'
    echo
    echo "Verify the group:artifact:version exists on https://search.maven.org/ before committing."
    echo "If this is a private/internal artifact, the user can override by accepting the edit."
  } >&2
  exit 2
fi

exit 0
