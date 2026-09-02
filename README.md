# OnionPeel (Android)

Allow-list **peel mode** via **Device Owner**, with **browser-only control** (Shift-style). Peel on/off happens only in the cloud companion — the phone shows status and enforces policy.

## Platforms

| Platform | Repo | Status |
|----------|------|--------|
| **Android** | [bropunzal/onionpeel](https://github.com/bropunzal/onionpeel) (this repo) | Closed beta (Play + APK) |
| **iPhone** | [bropunzal/onionpeel-ios](https://github.com/bropunzal/onionpeel-ios) | Beta — build from source (Xcode) |

Both apps use the **same cloud companion** and API. Create a separate device token per phone in the companion browser.

## Closed beta

**v0.2.0-beta.1** — see **[BETA.md](BETA.md)** for tester onboarding.

| For testers | For maintainers |
|-------------|-----------------|
| [BETA.md](BETA.md) — Android setup | [CLOUD.md](CLOUD.md) — deploy companion |
| [onionpeel-ios BETA](https://github.com/bropunzal/onionpeel-ios/blob/main/BETA.md) — iPhone | [PLAY.md](PLAY.md) — Play closed testing |
| [Report feedback](https://github.com/bropunzal/onionpeel/issues/new?template=beta-feedback.yml) | `.\scripts\build-release.ps1` → signed AAB |
| Install via Play closed testing | |

## Quick start

### 1. Cloud companion

Deploy once (see [CLOUD.md](CLOUD.md)) or run locally for dev:

```powershell
cd companion
npm install
npm start
```

Open the companion URL → **Create new device** → copy **Server URL** + **pairing token**.

### 2. Phone

Factory reset → skip Google → install from Play (or `adb install`) → Device Owner:

```powershell
adb shell dpm set-device-owner com.ateeb.onionpeel/.admin.OnionpeelDeviceAdminReceiver
```

In OnionPeel: enter `https://YOUR_COMPANION_URL` + token → **Link desktop** → peel from the companion browser only.

## Design

Titanium black canvas, burnt olive + sage accents, IBM Plex Mono–style typewriter data tiles, watch-face metric layout. Browsers stay open; feed sites blocked via Chrome/Edge URL policy.

## What the phone cannot do

- Turn peel on or off (companion browser only)
- Edit allow-list or blocked URLs while peeled
- Emergency bypass (Shift model — open the companion in a browser)

## Build

```powershell
.\gradlew.bat assembleDebug          # local dev
.\scripts\build-release.ps1          # signed release AAB + APK (requires keystore)
```

Requires JDK 17+ and Android SDK 35.
