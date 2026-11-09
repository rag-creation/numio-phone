# Phone by Numio

A private, offline phone app for Android. Part of the Numio family, alongside Numio Calculator, Clock by Numio, Ezhuthola, Notes by Numio and Calendar by Numio.

- No internet permission
- No ads, analytics, tracking or accounts
- Everything the app keeps stays on your phone
- Free and open source (GPL-3.0)

Calls themselves still go through Android and your carrier, as with any phone app. "Offline" means the app itself never connects to the internet.

> **Status: milestones 1–3 (in testing).** Phone by Numio can be your default phone app: it places calls, shows its own ringing and on-a-call screens (including over the lock screen), and has call history and contacts. Dual-SIM polish and blocking come next.

## Milestones

| # | Milestone | What it adds |
|---|-----------|--------------|
| 0 | Foundation ✓ | Project, Numio theme (dark / light / system), dial pad, tabs, settings, tests, CI |
| 1 | Default phone app ✓ | Ask for the default phone app role, dial intents, outgoing calls, `InCallService` with real incoming and ongoing call screens (answer, decline, mute, speaker, keypad, hold, end), call notification on the lock screen |
| 2 | **Call history** (this) | Recents (incoming, outgoing, missed), call details, redial |
| 3 | **Contacts** (this) | Contact list, search, favourites, contact details |
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

Nothing is asked for on first launch. Each permission is asked for only when you use the feature that needs it.

| Permission | Milestone | Why |
|------------|-----------|-----|
| Default phone app role | 1 | Android only lets the default phone app show the call screens |
| `CALL_PHONE` | 1 | Place a call |
| `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT` | 1 | Show an incoming call, including on the lock screen |
| `WAKE_LOCK` | 1 | Turn the screen off when the phone is at your ear |
| `READ_CALL_LOG` | 2 | Show call history (read only; nothing is copied) |
| `READ_CONTACTS` | 3 | Show names and the contact list |
| `WRITE_CONTACTS` | 3 | Add, edit and delete contacts, asked the first time you press Save. Saved to the phone's own storage, never online |
| `READ_PHONE_STATE` | 4 | List SIMs so you can choose one, and receive missed calls from Android so Numio can show its own missed-call notification (Call back / Message). Until it's allowed, Android's usual notification shows |
| Call screening (comes with being the default phone app) | 5 | Block hidden numbers, callers not in contacts and whole country codes before they ring. Single numbers go on Android's own blocked list, shared with SMS apps |

`INTERNET` will never be added. CI checks the release APK on every pull request and fails if it appears.

## Testing it as your phone app

1. Install the test APK and open it. Tap the call button on any number: Android asks whether Phone by Numio should be the default phone app. Say yes, then allow calls and notifications.
2. Keep your old phone app installed. To switch back: **Settings → Apps → Default apps → Phone app**, or Phone by Numio's own Settings → **Change default phone app**.

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
- The "Numio." wordmark uses letter shapes from [Chakra Petch](https://github.com/m4rc1e/Chakra-Petch), SIL Open Font License 1.1 ([licence](licenses/ChakraPetch-OFL.txt))
- Icons from [Material Symbols](https://github.com/google/material-design-icons), Apache License 2.0

Built by RR with help from Claude.
