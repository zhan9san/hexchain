#!/usr/bin/env bash
# Publish a release APK to Gitee Releases (docs/release.md section 5).
#
# usage: GITEE_REPO=<owner>/<repo> GITEE_TOKEN=<token> \
#          scripts/publish-gitee.sh <tag> <apk> [notes-file]
#
# 1. Mirrors the tagged commit to the Gitee repo's main branch, plus the tag.
# 2. Creates the Gitee release for the tag, or reuses it if it exists.
# 3. Attaches the APK, unless a file with the same name is already attached.
# Safe to run again after a failure.
set -euo pipefail

tag="${1:?usage: scripts/publish-gitee.sh <tag> <apk> [notes-file]}"
apk="${2:?usage: scripts/publish-gitee.sh <tag> <apk> [notes-file]}"
notes_file="${3:-}"
: "${GITEE_REPO:?set GITEE_REPO=<owner>/<repo>}"
: "${GITEE_TOKEN:?set GITEE_TOKEN to a Gitee personal access token}"

for tool in git curl jq; do
  command -v "$tool" >/dev/null || { echo "error: $tool not found" >&2; exit 1; }
done
[ -f "$apk" ] || { echo "error: $apk not found" >&2; exit 1; }

api="https://gitee.com/api/v5/repos/$GITEE_REPO"
sha=$(git rev-parse "$tag^{commit}")
if [ -n "$notes_file" ]; then notes=$(cat "$notes_file"); else notes="HexChain $tag"; fi

echo "Mirroring $tag ($sha) to gitee.com/$GITEE_REPO ..."
auth=$(printf '%s:%s' "${GITEE_REPO%%/*}" "$GITEE_TOKEN" | base64 | tr -d '\n')
git -c http.extraHeader="Authorization: Basic $auth" push --force \
  "https://gitee.com/$GITEE_REPO.git" "$sha:refs/heads/main" "refs/tags/$tag:refs/tags/$tag"

release_id=$(curl -fsS "$api/releases/tags/$tag?access_token=$GITEE_TOKEN" 2>/dev/null \
  | jq -r '.id // empty' || true)
if [ -z "$release_id" ]; then
  echo "Creating the Gitee release $tag ..."
  release_id=$(curl -fsS -X POST "$api/releases" \
    --form-string "access_token=$GITEE_TOKEN" \
    --form-string "tag_name=$tag" \
    --form-string "name=$tag" \
    --form-string "body=$notes" \
    --form-string "target_commitish=$sha" | jq -r '.id')
else
  echo "Reusing the Gitee release $tag (id $release_id)."
fi
[ -n "$release_id" ] && [ "$release_id" != null ] || { echo "error: no release id" >&2; exit 1; }

name=$(basename "$apk")
if curl -fsS "$api/releases/$release_id/attach_files?access_token=$GITEE_TOKEN" \
    | jq -e --arg n "$name" 'any(.[]; .name == $n)' >/dev/null; then
  echo "$name is already attached."
else
  echo "Uploading $name ..."
  curl -fsS -X POST "$api/releases/$release_id/attach_files" \
    --form-string "access_token=$GITEE_TOKEN" -F "file=@$apk" >/dev/null
fi

echo "Done: https://gitee.com/$GITEE_REPO/releases/tag/$tag"
