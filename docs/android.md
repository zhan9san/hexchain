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
2. **A-7 Chart tab, no scrolling**: from top to bottom,

   | Part                         | Section | Height (360 dp phone) |
   | ---------------------------- | ------- | --------------------- |
   | Tabs (A-37)                  | 15      | ~48 dp                |
   | Chart                        | 4       | ~425 dp               |
   | Round chips                  | 6       | ~48 dp                |
   | Digit slots and Clear button | 5, 7    | ~48 dp                |
   | Keypad                       | 5       | ~105 dp               |
   | **Total** (~690 dp usable)   |         | **~674 dp**           |

   On shorter screens the chart shrinks to fit (A-11).

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
   cannot change a submitted round; editing one is done from its chip
   (A-49).
5. **A-18 Round limit**: after round 4 is submitted, the keypad is disabled
   and the text `round_limit` (section 10) replaces the digit slots (R-2).

## 6. Round Chips

1. **A-19 Chips**: one chip per submitted round, in a single row of up to 4
   chips. Each chip has the round's colour (A-13) and shows the 3 digits
   and, below them, the match count or the text `no_match` (O-2, R-9).
   Empty places stay blank. Tapping a chip opens its menu (A-49).

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

| Key                 | 中文                          |
| ------------------- | ----------------------------- |
| `app_name`          | 蜂巢                          |
| `round_label`       | 第 %d 轮                      |
| `match_count`       | %d 个匹配                     |
| `no_match`          | 无匹配                        |
| `round_limit`       | 最多 4 轮，请清除后重新开始。 |
| `clear`             | 清除                          |
| `clear_title`       | 清除所有轮次？                |
| `clear_ok`          | 清除                          |
| `cancel`            | 取消                          |
| `edit_round`        | 编辑第 %d 轮                  |
| `delete_round`      | 删除第 %d 轮                  |
| `editing_round`     | 编辑第 %d 轮：                |
| `tab_chart`         | 蜂巢                          |
| `tab_filter`        | 筛选                          |
| `filter_hint`       | 输入号码，用空格或逗号分隔    |
| `filter_digit`      | 数字                          |
| `mode_kill`         | 杀                            |
| `mode_keep`         | 留                            |
| `filter_count`      | 保留 %1$d 个，去掉 %2$d 个    |
| `filter_duplicates` | 重复 %1$d 个：%2$s            |
| `filter_invalid`    | 无效：%s                      |
| `copy`              | 复制                          |
| `copied`            | 已复制                        |

English:

| Key                 | English                                 |
| ------------------- | --------------------------------------- |
| `app_name`          | HexChain                                |
| `round_label`       | Round %d                                |
| `match_count`       | 1 match / %d matches                    |
| `no_match`          | No match                                |
| `round_limit`       | Maximum 4 rounds. Clear to start again. |
| `clear`             | Clear                                   |
| `clear_title`       | Clear all rounds?                       |
| `clear_ok`          | Clear                                   |
| `cancel`            | Cancel                                  |
| `edit_round`        | Edit round %d                           |
| `delete_round`      | Delete round %d                         |
| `editing_round`     | Round %d:                               |
| `tab_chart`         | Chart                                   |
| `tab_filter`        | Filter                                  |
| `filter_hint`       | Numbers, separated by spaces or commas  |
| `filter_digit`      | Digit                                   |
| `mode_kill`         | Kill                                    |
| `mode_keep`         | Keep                                    |
| `filter_count`      | Kept %1$d, removed %2$d                 |
| `filter_duplicates` | Duplicates: %1$d (%2$s)                 |
| `filter_invalid`    | Invalid: %s                             |
| `copy`              | Copy                                    |
| `copied`            | Copied                                  |

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
8. **A-48 Debug builds**: debug builds use the app ID
   `com.zhan9san.hexchain.debug`, the version name suffix `-debug` and the
   name "蜂巢 Debug" / "HexChain Debug", so they install next to the
   release app instead of conflicting with it (different signing keys).

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

## 15. Number Filter (Version 1.1)

Android details for the core number filter (core section 8).

### 15.1 Navigation

1. **A-37 Separate views**: the filter has its own view, separate from the
   chart view. A tab row at the top switches between the two views:
   `tab_chart` (the existing chart view, sections 3 to 7, otherwise
   unchanged) and `tab_filter` (the filter view, section 15.2). Each view
   keeps its own state when switching. The app opens on the view used
   last.

### 15.2 Filter View

From top to bottom:

1. **A-38 Number list**: a multi-line text field (`filter_hint`) using the
   system keyboard in number mode. Every separator of core section 8.1 is
   accepted, so pasting a list from a chat app works.
2. **A-39 Filter digit**: a row of 10 single-choice chips, `0` to `9`.
   No digit is selected at first; until one is, no result is shown.
3. **A-40 Mode**: a two-way switch, `mode_kill` / `mode_keep` (F-4, F-5).
   Kill is selected at first.
4. **A-41 Result**: updates on every change, without a button (O-4). Shows
   - `filter_count` with the kept and removed counts, e.g. "保留 3 个，
     去掉 2 个",
   - the kept numbers in a wrapping grid, in order (F-6),
   - `filter_duplicates` with the duplicate count and the duplicated
     numbers, only when there are duplicates (F-3), e.g. "重复 1 个：
     347 ×2", and
   - `filter_invalid` with the invalid tokens, only when there are some
     (F-2).
5. **A-42 Copy**: a `copy` button puts the kept numbers on the clipboard,
   separated by spaces, and briefly shows `copied`.
6. **A-43 Clear**: a clear button inside the text field empties the number
   list without confirmation. It does not affect the rounds (F-8).

### 15.3 State and Testing

1. **A-44 State**: the number list, filter digit, mode and selected tab are
   saved like the rounds (A-22).
2. **A-45 Shared filter scenarios**: `tests/scenarios.json` gains filter
   scenarios (number list, digit, mode, expected kept and removed numbers,
   duplicates and invalid tokens), used by the Python reference code and
   the Kotlin tests (A-27, A-28). The example in core section 8.4 is the
   first scenario.
3. **A-46 UI tests**: type a list, pick a digit and switch modes; check the
   kept numbers, both counts, the duplicates and the invalid tokens.
4. **A-47 Version**: released as `1.1.0` (A-34).

## 16. Edit and Delete Rounds (Version 1.2)

Android details for core rules R-11 to R-13.

### 16.1 Chip Menu

1. **A-49 Menu**: tapping a round chip opens a menu next to it with
   `edit_round` and `delete_round` (e.g. "编辑第 2 轮", "删除第 2 轮").
   Tapping outside closes it.

### 16.2 Edit

1. **A-50 Edit mode**: choosing `edit_round` starts edit mode for that
   round:
   - its chip gets a thicker outline,
   - the digit-slot row shows `editing_round` and three empty slots in that
     round's colour,
   - the Clear button is replaced by `cancel`, and
   - the keypad is enabled, even when 4 rounds exist (R-13).

   Digits typed for a new round before entering edit mode are discarded.
2. **A-51 Replace**: typing the 3rd digit replaces the round's digits,
   recomputes it and every later round (R-11), and leaves edit mode.
   Backspace removes typed digits only. `cancel`, or tapping the same chip
   and choosing `edit_round` again, leaves edit mode without changes.

### 16.3 Delete

1. **A-52 Delete at once**: choosing `delete_round` removes the round
   immediately, without confirmation and without undo (R-12). Later chips
   move left; every chip, highlight and colour follows its new round number
   (A-13). If that round was being edited, edit mode ends.

### 16.4 State and Testing

1. **A-53 State**: edits and deletes are saved like new rounds (A-22).
   Edit mode itself is not saved; it ends when the app is closed.
2. **A-54 Accessibility**: round chips are buttons; their label (A-23)
   tells screen readers that tapping opens the menu.
3. **A-55 Tests**: unit tests check O-5 (results after an edit or delete
   equal entering the remaining rounds from the start). UI tests use the
   example in core section 7.6: delete round 2, and edit round 2 to
   `7 8 9`, then check every round's highlight set; also edit a round
   while 4 rounds exist.
4. **A-56 Version**: released as `1.2.0` (A-34).

## 17. Repeated Digits (Version 1.3)

Android details for core rules R-14 and R-15.

1. **A-57 Input**: rounds with repeated digits (A B B, A A A) are typed
   like any other round (A-14 to A-16); nothing changes on screen.
2. **A-58 Counts**: the chip count (A-19) counts pairs for an A B B round
   and single cells for an A A A round.
3. **A-59 Tests**: the shared scenarios `abc_aaa_abb` and `aaa_abb` (core
   section 7.7) run in the Python, Kotlin and UI tests.
4. **A-60 Version**: released as `1.3.0` (A-34).
