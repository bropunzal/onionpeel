# Dumbphone (Android)

Allow-list **dumb phone mode** for Android using **Device Owner** (the same enforcement layer as enterprise MDM / what Shift uses on Android). Only apps you explicitly allow stay usable. Everything else is **OS-suspended** (greyed out in the launcher), not hidden behind a wait screen.

iPhone (supervised MDM) is planned after Android is stable on a real device.

## What it does

- **Allow-list** — Phone, Messages, Maps, Calendar, Camera, banking, etc. You pick packages in the app.
- **Hard-block list** — Instagram, YouTube, TikTok, Reddit, Chrome, and other browsers stay suspended even if you mistakenly allow them.
- **Install freeze** — Cannot install apps while dumb mode is on (`DISALLOW_INSTALL_APPS`, unknown sources off).
- **Boot repair** — Re-applies suspension after reboot and when a new app is installed.
- **Exit delay** — Leaving dumb mode requires a countdown (default 24h, configurable 1–72h). No instant toggle.
- **Emergency passes** — 2 per month: 60s hold + phrase you set at setup.

## What it does *not* do (yet)

- Play Store listing (sideload / debug APK only)
- NFC / desktop unlock key
- Factory-reset lock (dangerous — enable only after exit path is tested)
- iOS / MDM server

## Requirements

- Android **8.0+** (API 26), tested target API 34
- A **dedicated phone** you are willing to factory-reset
- **Android Studio** (Ladybug or newer) with SDK 34
- **No Google account** on the phone during provisioning (Android platform rule for Device Owner)

## Provision (one-time)

1. **Factory reset** the phone.
2. Complete setup **without** signing into Google (skip account).
3. Enable **Developer options** → **USB debugging**.
4. Connect USB and install:

   ```bash
   cd C:\Users\Ateeb\dumbphone
   .\gradlew.bat installDebug
   ```

   Or build APK in Android Studio: **Build → Build APK(s)**.

5. Set Device Owner (must be first/only device admin, no accounts):

   ```bash
   adb shell dpm set-device-owner com.ateeb.dumbphone/.admin.DumbphoneDeviceAdminReceiver
   ```

   Success looks like: `Success: Device owner set to package ...`

6. Open **Dumbphone** on the phone:
   - Set emergency phrase (4+ chars)
   - Set exit delay
   - Check only tool apps you need
   - Tap **Enable dumb mode**

## Remove Device Owner (after exit delay or emergency)

```bash
adb shell dpm remove-active-admin com.ateeb.dumbphone/.admin.DumbphoneDeviceAdminReceiver
```

Or use the in-app exit flow after the countdown completes.

## Build locally

1. Install [Android Studio](https://developer.android.com/studio).
2. Open this folder as a project.
3. Sync Gradle, run on device or emulator.

**Note:** Device Owner **cannot** be set on a normal emulator with a Google account. Use a factory-reset physical device or a fresh AVD without Play Store / account.

## Architecture

| Piece | Role |
|-------|------|
| `DumbphoneDeviceAdminReceiver` | Device admin / owner component |
| `DumbModeController` | Suspend packages, restrictions, exit logic |
| `BootReceiver` / `PackageChangeReceiver` | Re-apply policy |
| `PrefsRepository` | Allow-list, timers, emergencies (local only) |
| `MainActivity` | Setup + allow-list UI |

## Honest bypass holes

Even at Device Owner:

- **Another phone / laptop** — out of scope
- **Factory reset** — always possible unless you lock it (not enabled in v0.1)
- **Allowed apps with feeds** — Gmail/Slack can still distract; keep them off the list

## Roadmap

1. Android v0.1 on your daily phone (this repo)
2. Harden: block Settings, suspend Play Store explicitly, schedule windows
3. iOS: supervised MDM profile + companion (same product, different stack)

## License

Private / personal use. Not for Play Store distribution without policy review for Device Owner apps.
