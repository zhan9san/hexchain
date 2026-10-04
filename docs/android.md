# HexChain — Android Requirements

## 1. Purpose

This document adds Android-specific details on top of the core requirements
in `docs/requirements.md`. Rule IDs such as R-2 or O-2 refer to that
document. Nothing here changes a core rule; if a conflict is found, the core
document wins and this document is fixed.

## 2. Platform and Technology

1. **A-1 Language and UI**: Kotlin with Jetpack Compose.
2. **A-2 Android version**: minimum Android 8.0 (API 26). The app compiles
   against API 37 (required by the Compose libraries) and targets API 36.
3. **A-3 Package**: `com.zhan9san.hexchain`.
4. **A-4 Distribution**: APK attached to GitHub Releases and mirrored to
   Gitee Releases for users in China (section 12). Play Store and Chinese
   app stores are out of scope for the first version.
5. **A-5 Offline**: the app works fully offline, uses no network and
   requests no permissions.

## 3. Screen Layout

1. **A-6 Orientation**: portrait only. Phones first; on tablets the same
   layout scales up.
2. **A-7 Single screen, no scrolling**: from top to bottom,

   | Part                         | Section | Height (360 dp phone) |
   | ---------------------------- | ------- | --------------------- |
   | Chart                        | 4       | ~425 dp               |
   | Round chips                  | 6       | ~48 dp                |
   | Digit slots and Clear button | 5, 7    | ~48 dp                |
   | Keypad                       | 5       | ~105 dp               |
   | **Total** (~690 dp usable)   |         | **~626 dp**           |

3. **A-8 Theme**: follows the system light/dark setting. Hexagon
   backgrounds, borders and digits have light and dark variants; the round
   colours (A-13) are the same in both.

## 4. Chart

1. **A-9 Drawn, not an image**: the chart is drawn by the app from the grid
   data in core section 3.2, using the layout in core section 3 (pointy-top
   hexagons, offset rows). `images/honeycomb.jpg` is not bundled.
2. **A-10 Look**: each hexagon shows its digit centred, with a thin border.
   Hexagon backgrounds use soft neutral tones; matching the photo's colours
   is not required. Partial edge hexagons are not drawn.
3. **A-11 Size and zoom**: the chart fits the screen width by default
   (about 30 dp per hexagon on a 360 dp phone). Pinch-to-zoom and pan are
   supported; double-tap resets the zoom. Tapping a cell does nothing else.
4. **A-12 Highlight**: every cell of `H(n)` (O-2) gets a semi-transparent
   fill and a solid outline in round `n`'s colour (A-13). When a cell is in
   several highlight sets (O-3), the latest round's colour is shown.
5. **A-13 Round colours**: colour-blind-safe palette, by round:

   | Round | Colour | Hex       |
   | ----- | ------ | --------- |
   | 1     | Blue   | `#0072B2` |
   | 2     | Orange | `#E69F00` |
   | 3     | Purple | `#CC79A7` |
   | 4     | Teal   | `#009E73` |

## 5. Input

1. **A-14 Keypad**: an on-screen keypad of 2 rows, `0 1 2 3 4` and
   `5 6 7 8 9`, with a backspace key at the end of the second row. The
   system keyboard is never shown.
2. **A-15 Current round**: three slots show the digits typed so far for the
   next round, in the next round's colour.
3. **A-16 Submit**: typing the 3rd digit submits the round immediately
   (R-1); there is no submit button. The slots then empty for the next
   round.
4. **A-17 Backspace**: removes the last digit of the round being typed. It
   cannot change a submitted round (R-10).
5. **A-18 Round limit**: after round 4 is submitted, the keypad is disabled
   and the text `round_limit` (section 10) replaces the digit slots (R-2).

## 6. Round Chips

1. **A-19 Chips**: one chip per submitted round, in a single row of up to 4
   chips. Each chip has the round's colour (A-13) and shows the 3 digits
   and, below them, the match count or the text `no_match` (O-2, R-9).
   Empty places stay blank.

## 7. Clear

1. **A-20 Clear button**: a Clear button at the end of the digit-slot row,
   enabled when at least one round exists.
2. **A-21 Confirmation**: tapping Clear opens a dialog (`clear_title`,
   `clear_ok`, `cancel`). Confirming clears all rounds and highlights and
   re-enables the keypad (R-10). There is no undo.

## 8. State

1. **A-22 Rotation and restarts**: submitted rounds survive configuration
   changes and process death, and are restored when the app is reopened.
   Only the rounds' digits are stored; results are recomputed on load. A
   partially typed round is not restored.

## 9. Accessibility

1. **A-23 Labels**: keypad keys, the backspace key, the Clear button and the
   round chips have content descriptions (chips use `round_label` plus the
   digits and result).
2. **A-24 Chart**: individual cells are not read out by screen readers in
   the first version.
3. **A-25 Touch targets**: keypad keys and buttons are at least 48 dp high.

## 10. Text

1. **A-26 Languages**: Simplified Chinese is the default. English is used
   only when the system language is English. All text lives in string
   resources.

Simplified Chinese (default):

| Key           | 中文                          |
| ------------- | ----------------------------- |
| `app_name`    | 蜂巢                          |
| `round_label` | 第 %d 轮                      |
| `match_count` | %d 个匹配                     |
| `no_match`    | 无匹配                        |
| `round_limit` | 最多 4 轮，请清除后重新开始。 |
| `clear`       | 清除                          |
| `clear_title` | 清除所有轮次？                |
| `clear_ok`    | 清除                          |
| `cancel`      | 取消                          |

English:

| Key           | English                                 |
| ------------- | --------------------------------------- |
| `app_name`    | HexChain                                |
| `round_label` | Round %d                                |
| `match_count` | 1 match / %d matches                    |
| `no_match`    | No match                                |
| `round_limit` | Maximum 4 rounds. Clear to start again. |
| `clear`       | Clear                                   |
| `clear_title` | Clear all rounds?                       |
| `clear_ok`    | Clear                                   |
| `cancel`      | Cancel                                  |

## 11. Testing

1. **A-27 Shared data**: the grid data and the test scenarios (rounds and
   expected matches) move into shared JSON files, `data/grid.json` and
   `tests/scenarios.json`, used by both the Python reference code/tests and
   the Android code/tests.
2. **A-28 Core logic tests**: Kotlin unit tests run every shared scenario
   and compare matches and highlight sets with the expected values.
3. **A-29 UI tests**: UI tests check which cells are highlighted in which
   round, not pixels. Pixel comparison stays in the Python image tests only.

## 12. Project and Delivery

1. **A-30 Location**: the Android project lives in `android/` in this
   repository, next to the shared data (A-27).
2. **A-31 CI**: GitHub Actions builds the app and runs the Python and Kotlin
   tests on every push and pull request.
3. **A-32 Signing**: release APKs are built and signed by GitHub Actions.
   The keystore and its password are stored as GitHub repository secrets
   `KEYSTORE_BASE64` (the keystore file, base64-encoded) and
   `KEYSTORE_PASSWORD`, with a backup kept outside GitHub; losing the key
   means users must uninstall before installing an update. The key alias is
   `hexchain` and the key password equals the keystore password.
   Key creation, backup and recovery steps: `docs/release.md`.
4. **A-33 Release**: pushing a tag `v<version>` builds a signed APK and
   attaches it to a GitHub Release and, when configured, a Gitee Release.
   The release notes start with the signing certificate fingerprint.
   Release steps: `docs/release.md`.
5. **A-34 Versioning**: version name starts at `1.0.0`; the version code
   starts at 1 and increases by 1 with every release.
6. **A-35 Icon**: an adaptive icon showing a simple hexagon in round 1's
   colour (A-13).
7. **A-36 Development**: command-line tools only (Java 21, Android SDK
   command-line tools, the Gradle wrapper); Android Studio is optional. CI
   is the source of truth for builds and tests. Setup and commands:
   `docs/CONTRIBUTING.md`.

## 13. Implementation Order

1. Move the grid and test scenarios into the shared JSON files (A-27) and
   update the Python code and tests to read them.
2. Implement the core logic in Kotlin with unit tests (A-28).
3. Build the screen (sections 3 to 9).
4. Set up CI, signing and releases (A-31 to A-33).

## 14. Definition of Done (Version 1)

1. All shared scenarios pass in the Python and Kotlin tests.
2. The app is checked by hand on Android 8.0 and the newest Android, on a
   small and a large phone screen, in light and dark mode, in English and
   Chinese.
3. A signed APK `v1.0.0` is attached to a GitHub Release.
