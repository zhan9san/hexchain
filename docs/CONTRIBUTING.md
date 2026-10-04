# Contributing

How to set up a development environment, run the tests and make a change.
The commands are for macOS with [Homebrew](https://brew.sh); on other
systems install the same tools (Python 3, Java 21, Android SDK
command-line tools) with your package manager.

## 1. Which Document to Read

| Document               | What it defines                                     |
| ---------------------- | --------------------------------------------------- |
| `docs/requirements.md` | Core rules: grid, matching, rounds (platform-free)  |
| `docs/android.md`      | Android app: screen, input, text, testing, delivery |
| `docs/release.md`      | Signing and publishing releases (maintainers only)  |

The core document wins over the Android one; a rule changes in the core
document first.

## 2. Setup

### 2.1 Python (Core Reference Code and Image Tests)

```bash
python3 -m venv .venv
```

```bash
.venv/bin/pip install -r requirements.txt
```

### 2.2 Java and Android SDK

```bash
brew install --cask temurin@21
```

```bash
brew install --cask android-commandlinetools
```

Add to `~/.zshrc`, then open a new terminal:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

Accept the SDK licences (interactive):

```bash
sdkmanager --licenses
```

Install the SDK packages. API 37 is needed to compile the app; the system
images are for the two emulators:

```bash
sdkmanager "platform-tools" "emulator" "platforms;android-37.0" \
  "system-images;android-26;google_apis;arm64-v8a" \
  "system-images;android-36;google_apis;arm64-v8a"
```

On an Intel Mac use `x86_64` instead of `arm64-v8a`. Gradle itself needs no
installation: `android/gradlew` downloads it.

### 2.3 Emulators

The manual checks in `docs/android.md` section 14 use a small, old phone and
a large, new one:

```bash
avdmanager create avd -n small_api26 -d pixel_4a \
  -k "system-images;android-26;google_apis;arm64-v8a"
```

```bash
avdmanager create avd -n large_latest -d pixel_9_pro \
  -k "system-images;android-36;google_apis;arm64-v8a"
```

`avdmanager` may print `devices.xml` errors; they are harmless.

### 2.4 Check the Setup

```bash
java -version && sdkmanager --version && adb version && emulator -list-avds
```

### 2.5 Behind a Proxy

If Gradle downloads fail (e.g. "Remote host terminated the handshake" for
`dl.google.com`), pass the proxy on the command line or put it in your
**own** `~/.gradle/gradle.properties`, never in the repository:

```bash
./gradlew -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890 :app:testDebugUnitTest
```

Such failures can be intermittent; running the command again often works.

## 3. Running Things

Python commands run in the repository root; Gradle commands in `android/`.

| What                          | Command                                    |
| ----------------------------- | ------------------------------------------ |
| Core and image tests (Python) | `.venv/bin/python tests/test_honeycomb.py` |
| Core logic tests (Kotlin)     | `./gradlew :app:testDebugUnitTest`         |
| UI tests (running emulator)   | `./gradlew :app:connectedDebugAndroidTest` |
| Debug APK                     | `./gradlew :app:assembleDebug`             |
| Lint                          | `./gradlew :app:lintDebug`                 |

Start an emulator with a window:

```bash
emulator -avd large_latest
```

Install the debug APK on the running emulator or a USB-connected phone:

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Draw a highlight image for some rounds (output in `images/`, ignored by
git):

```bash
.venv/bin/python draw_highlights.py 3,4,7 0,5,1
```

## 4. Making a Change

### 4.1 Changing a Core Rule

1. Update `docs/requirements.md`.
2. Update or add scenarios in `tests/scenarios.json`: rounds, expected
   matches and excluded matches. They are shared by the Python and Kotlin
   tests.
3. Update `honeycomb.py` and
   `android/app/src/main/java/com/zhan9san/hexchain/core/Honeycomb.kt` the
   same way.
4. If highlights change, regenerate the affected images in
   `tests/expected/`, check them by eye, and commit them:

   ```bash
   .venv/bin/python draw_highlights.py 3,4,7 0,5,1 -o tests/expected/347_051.jpg
   ```

The grid digits live in `data/grid.json`, shared by both implementations.

### 4.2 Changing Only the Android App

1. Update `docs/android.md`.
2. Change the app; add UI tests for new behaviour.
3. Check the screen on both emulators, in light and dark mode, in Chinese
   and English.

### 4.3 Upgrading Pillow

The image tests compare pixels, so after changing the Pillow version in
`requirements.txt`, regenerate all images in `tests/expected/` and commit
them with the version change.

## 5. Pull Request Checklist

- [ ] Python tests, Kotlin unit tests and lint pass locally.
- [ ] UI tests pass on at least one emulator for app changes.
- [ ] Docs are updated and pass `markdownlint docs/*.md`.
- [ ] CI is green on the pull request.
- [ ] No signing keys, keystores or secrets are committed; releases are
  done by maintainers (`docs/release.md`).
