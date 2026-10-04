# HexChain — Release Guide

How to sign and publish the Android app on GitHub Releases. The rules this
guide follows are in `docs/android.md` (A-32 to A-34); the automation is
`.github/workflows/release.yml`.

## 1. One-Time Setup

Do this once, before the first release.

### 1.1 Create the Key and the Secrets

Every release must be signed with the same key: Android only installs an
update signed with the key of the installed app. In the repository folder,
logged in with `gh auth login`, run:

```bash
scripts/create-release-key.sh
```

It asks for a password (at least 6 characters, typed twice), then

1. creates `~/hexchain-release.keystore` with one key, alias `hexchain`, readable
   by you only,
2. uploads two repository secrets used by the release workflow:

   | Secret              | Value                             |
   | ------------------- | --------------------------------- |
   | `KEYSTORE_BASE64`   | the keystore file, base64-encoded |
   | `KEYSTORE_PASSWORD` | the password                      |

3. prints the certificate fingerprint (section 1.3).

If `~/hexchain-release.keystore` already exists, the script never replaces it:
it checks the password and uploads the two secrets again. To use another
path, pass it as the first argument.

Keep the keystore outside the repository. `*.keystore` and `*.jks` are in
`.gitignore` as a safety net.

### 1.2 Back It Up

Store the keystore file **and** its password together in a safe place
outside GitHub, e.g. a password manager entry with the file attached.

If the key is lost, no installed copy can ever be updated (section 4.3).

### 1.3 Note the Certificate Fingerprint

The fingerprint identifies the key without revealing it. Publishing it lets
anyone check that a downloaded APK really comes from you. The script prints
it; to see it again:

```bash
keytool -list -v -keystore ~/hexchain-release.keystore -alias hexchain | grep SHA256
```

Keep the value to check releases against (section 2); the release workflow
also writes it into every release's notes.

## 2. Each Release

1. Bump the version in `android/gradle.properties` (A-34):
   - `appVersionCode`: previous value + 1.
   - `appVersionName`: the new version, e.g. `1.0.1`.
2. Commit and push to `main`, and wait for CI to pass.
3. Tag the commit with `v` + `appVersionName` and push the tag:

   ```bash
   git tag v1.0.1 && git push origin v1.0.1
   ```

4. The release workflow then
   - checks that the tag matches `appVersionName`,
   - runs the core logic tests,
   - builds the APK and signs it with the release key,
   - creates a GitHub Release with `hexchain-v1.0.1.apk` attached and the
     certificate fingerprint at the top of the notes, and
   - publishes the same APK and notes on Gitee (section 5), if configured.
5. Download the APK and check its signature matches the fingerprint from
   1.3:

   ```bash
   gh release download v1.0.1 --pattern '*.apk' --dir /tmp/hexchain
   ```

   ```bash
   apksigner verify --print-certs /tmp/hexchain/hexchain-v1.0.1.apk
   ```

   `apksigner` is in `$ANDROID_HOME/build-tools/<version>/`. It prints the
   SHA-256 digest in lowercase without colons; `keytool` and the release
   notes show the same value as `AB:CD:…`.

## 3. Installing on a Phone

- **Where to download**: users in China use the Gitee release
  (`https://gitee.com/<owner>/hexchain/releases`), which is fast there;
  others can use either. Both have the same signed APK.
- **First install**: download the APK on the phone, check the fingerprint
  in the release notes if in doubt, open the APK, and allow "Install
  unknown apps" for the browser or file app used.
- **Updates**: install the newer APK the same way; it replaces the old
  version and keeps the saved rounds.
- **Coming from a debug build**: a debug APK is signed with a different key
  (the machine's debug key), so uninstall it before installing a release
  for the first time.

## 4. When Things Go Wrong

### 4.1 The Tag Does Not Match the Version

The workflow stops with "Tag … does not match appVersionName". Delete the
tag, fix `gradle.properties`, commit, and tag again:

```bash
git tag -d v1.0.1 && git push --delete origin v1.0.1
```

### 4.2 A Secret Is Missing or Wrong

The workflow stops at "Restore the keystore" or at signing. Run
`scripts/create-release-key.sh` again: with the existing keystore it checks
the password and uploads both secrets again. Then re-run the failed
workflow from the GitHub Actions page.

### 4.3 The Key Is Lost

Restore it from the backup (section 1.2). Without a backup there is no
recovery: create a new key, publish under it, and every user must uninstall
the old app (losing saved rounds) before installing the new one.

### 4.4 The Key Is Leaked

Anyone with the key can publish an update that phones accept as yours.

1. Move the old keystore away and run `scripts/create-release-key.sh` to
   create a new key and replace both secrets (section 1.1).
2. Phones on Android 9 or later can move to the new key without
   reinstalling, if the release is signed with a key rotation proof
   created by `apksigner rotate` from the old and new keys. This needs a
   manual signing step; plan it before the next release.
3. Phones on Android 8.x cannot rotate keys; their users must uninstall and
   reinstall.
4. Publish the new fingerprint and say why it changed in the release notes.

### 4.5 The Gitee Job Fails

The GitHub Release is already published at that point. Fix the cause (e.g.
an expired `GITEE_TOKEN`: set it again, section 5.1) and use **Re-run
failed jobs** on the GitHub Actions page; only the Gitee job runs again.
Or publish from your own computer, which is fast from China (section 5.2).

## 5. Gitee Mirror (Users in China)

GitHub is slow or unreachable in China, so every release is also published
on Gitee by the `gitee` job of the release workflow, using
`scripts/publish-gitee.sh`. The Gitee repository is a mirror: its `main`
branch is overwritten with the released commit, so never commit on Gitee.

### 5.1 One-Time Setup

1. Create a Gitee account (phone verification is required; Gitee may also
   review public repositories).
2. Create an **empty, public** repository named `hexchain` (no README, no
   licence).
3. Create a personal access token at
   `https://gitee.com/profile/personal_access_tokens` with the `projects`
   scope.
4. Tell GitHub about it, in the repository folder. `gh` asks for the token:

   ```bash
   gh variable set GITEE_REPO --body "<gitee-owner>/hexchain"
   ```

   ```bash
   gh secret set GITEE_TOKEN
   ```

The `gitee` job is skipped until `GITEE_REPO` is set.

### 5.2 Publishing by Hand

To publish a release on Gitee from your own computer (e.g. after the job
failed), download the GitHub release and run the script:

```bash
gh release download v1.0.1 --pattern '*.apk' --dir /tmp/hexchain
```

```bash
GITEE_REPO=<gitee-owner>/hexchain GITEE_TOKEN=<token> \
  scripts/publish-gitee.sh v1.0.1 /tmp/hexchain/hexchain-v1.0.1.apk
```

The script is safe to run again: it reuses an existing Gitee release and
skips an APK that is already attached.
