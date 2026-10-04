#!/usr/bin/env bash
# Create the release signing key and store it as GitHub secrets
# (docs/release.md section 1).
#
# usage: scripts/create-release-key.sh [keystore]   (default ~/hexchain-release.keystore)
#
# - If the keystore does not exist, it is created (alias "hexchain", RSA 4096).
# - If it exists, it is never replaced: the password is checked and the
#   secrets are uploaded again (e.g. after a secret was lost or set wrongly).
# Secrets: KEYSTORE_BASE64 (the keystore file) and KEYSTORE_PASSWORD.
# The password is read without echo and never passed on a command line.
set -euo pipefail

keystore="${1:-$HOME/hexchain-release.keystore}"
alias_name="hexchain"

for tool in keytool gh base64; do
  command -v "$tool" >/dev/null || { echo "error: $tool not found" >&2; exit 1; }
done
gh auth status >/dev/null 2>&1 || { echo "error: run 'gh auth login' first" >&2; exit 1; }
repo="${REPO:-$(gh repo view --json nameWithOwner --jq .nameWithOwner)}"

read -rsp "Keystore password: " KS_PASS; echo
export KS_PASS

if [ -e "$keystore" ]; then
  echo "Using the existing keystore $keystore (it is not replaced)."
  if ! keytool -list -keystore "$keystore" -storepass:env KS_PASS -alias "$alias_name" >/dev/null 2>&1; then
    echo "error: wrong password, or no '$alias_name' key in $keystore" >&2
    exit 1
  fi
else
  [ "${#KS_PASS}" -ge 6 ] || { echo "error: use at least 6 characters" >&2; exit 1; }
  read -rsp "Repeat the password: " again; echo
  [ "$KS_PASS" = "$again" ] || { echo "error: the passwords differ" >&2; exit 1; }
  keytool -genkeypair -keystore "$keystore" -alias "$alias_name" \
    -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=$alias_name" \
    -storepass:env KS_PASS -keypass:env KS_PASS >/dev/null 2>&1
  chmod 600 "$keystore"
  echo "Created $keystore"
fi

echo "Uploading the secrets to $repo ..."
base64 < "$keystore" | tr -d '\n' | gh secret set KEYSTORE_BASE64 -R "$repo"
printf '%s' "$KS_PASS" | gh secret set KEYSTORE_PASSWORD -R "$repo"

fingerprint=$(keytool -list -v -keystore "$keystore" -storepass:env KS_PASS -alias "$alias_name" \
  | sed -n 's/^.*SHA256: //p')
cat <<MSG

Done.
  Keystore:    $keystore
  Secrets:     KEYSTORE_BASE64, KEYSTORE_PASSWORD on $repo
  Fingerprint: SHA256 $fingerprint

Back up the keystore file AND its password together (e.g. in a password
manager). Without them, installed copies can never be updated.
MSG
