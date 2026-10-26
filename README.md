# Phone by Numio

A private, offline phone app for Android. Part of the Numio family, alongside Numio Calculator, Clock by Numio, Ezhuthola, Notes by Numio and Calendar by Numio.

- No internet permission
- No ads, analytics, tracking or accounts
- Everything the app keeps stays on your phone
- Free and open source (GPL-3.0)

Calls themselves still go through Android and your carrier, as with any phone app. "Offline" means the app itself never connects to the internet.

> **Status: foundation (v0.1.0).** This version has the Numio theme, the dial pad, the page layout and settings. It can't place calls yet. That comes in the next milestone.

## Milestones

| # | Milestone | What it adds |
|---|-----------|--------------|
| 0 | **Foundation** (this) | Project, Numio theme (dark / light / system), dial pad, tabs, settings, tests, CI |
| 1 | Default phone app | Ask for the default phone app role, dial intents, outgoing calls, `InCallService` with real incoming and ongoing call screens (answer, decline, mute, speaker, keypad, hold, end), call notification on the lock screen |
| 2 | Call history | Recents (incoming, outgoing, missed), call details, redial |
| 3 | Contacts | Contact list, search, favourites, contact details |
| 4 | Dual SIM | Choose the SIM per call, SIM labels in history |
| 5 | Blocking | Blocked numbers through Android's system block list and `CallScreeningService` |
| 6 | Polish | Accent colour picker, About page and Numio easter egg, accessibility pass, app icon, F-Droid listing |

Each milestone is its own pull request, tested on a real phone before merging.

## Architecture

- **Kotlin + Jetpack Compose + Material 3**, styled with the Numio palette
- One `app` module for now, organised by feature: `dialpad/` (plain Kotlin logic, unit tested), `data/` (settings in DataStore), `ui/` (one package per page, plus `theme/`)
- From milestone 1: a `telecom/` package with the `InCallService` and a single call state (`StateFlow`) that the call screens observe; ViewModels and repositories for call log and contacts
- Logic that doesn't need Android stays in plain Kotlin so it can be unit tested
- It will be split into Gradle modules only if it grows enough to need it

## Why Android 10 (API 29) as the minimum

Other Numio apps start at Android 8 (API 26). Phone by Numio starts at Android 10 because:

- **`RoleManager`** (API 29) is how an app asks to become the default phone app and the call screening app. Below 29 there's an older, separate path, and no call screening role at all.
- One code path means fewer bugs on the thing that matters most: calls actually working.
- Android 10 and newer covers the vast majority of phones in use today.

If supporting Android 8–9 turns out to matter, it can be added later with the older `TelecomManager` route.

## Permissions

This version asks for **no permissions at all**. Each permission below will be added only with the feature that needs it, and asked for only when you use that feature, never all at once on first launch.

| Permission | Milestone | Why |
|------------|-----------|-----|
| Default phone app role | 1 | Android only lets the default phone app show the call screens |
| `CALL_PHONE` | 1 | Place a call |
| `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT` | 1 | Show an incoming call, including on the lock screen |
| `READ_CALL_LOG`, `WRITE_CALL_LOG` | 2 | Show and clear call history |
| `READ_CONTACTS` | 3 | Show names and the contact list |
| `READ_PHONE_STATE` | 4 | List SIMs so you can choose one |
| Call screening role | 5 | Block numbers before they ring |

`INTERNET` will never be added. CI checks the release APK on every pull request and fails if it appears.

## Build and test

### Android Studio

1. **File → Open** and pick this folder.
2. Let Gradle sync. It uses the Gradle wrapper (9.6.0) and JDK 21.
3. Pick the **app** run configuration and your phone, then **Run**.

Debug builds install as **Phone Debug** with the app id `com.rr.numio.phone.debug`, next to the real app.

To run the unit tests: right-click `app/src/test` → **Run 'Tests in…'**.

### Terminal

```sh
./gradlew assembleDebug           # debug APK → app/build/outputs/apk/debug/
./gradlew testDebugUnitTest       # unit tests → app/build/reports/tests/
./gradlew assembleRelease         # unsigned release APK (R8 on)
```

### GitHub test builds

Every pull request builds a debug APK, runs the tests, builds the release APK and checks its permissions. Download **PhoneDebug.apk** from the run's **Artifacts** section.

Release APKs are signed by hand with the Numio key. The key is never added to GitHub.

## Licence and credits

Phone by Numio is free software under the [GNU GPL v3.0 only](LICENSE).

- [Inter](https://github.com/rsms/inter) font, SIL Open Font License 1.1 ([licence](licenses/Inter-OFL.txt))
- Icons from [Material Symbols](https://github.com/google/material-design-icons), Apache License 2.0

Built by RR with help from Claude.
