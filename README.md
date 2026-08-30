# Onionpeel (Android)

Allow-list **peel mode** via **Device Owner**, with **desktop-only control** (Shift-style). Peel on/off happens only in the desktop companion browser — the phone shows status and enforces policy.

## Quick start

### 1. Desktop companion (your PC)

```powershell
cd C:\Users\Ateeb\dumbphone\companion
npm install
npm start
```

Open `http://localhost:8787` — big **PEELED / OPEN** toggle. Copy the pairing token.

### 2. Phone

Factory reset → skip Google → install APK → Device Owner:

```powershell
adb install app\build\outputs\apk\debug\app-debug.apk
adb shell dpm set-device-owner com.ateeb.onionpeel/.admin.OnionpeelDeviceAdminReceiver
```

In Onionpeel: enter `http://YOUR_PC_IP:8787` + token → **Link desktop** → configure allow-list while **OPEN** → peel from desktop only.

## Design

COROS x SATISFY APEX 4–inspired: titanium black canvas, burnt olive + sage accents, IBM Plex Mono–style typewriter data tiles, watch-face metric layout. Browsers stay open; feed sites blocked via Chrome/Edge URL policy.

## What the phone cannot do

- Turn peel on or off (desktop only)
- Edit allow-list or blocked URLs while peeled
- Emergency bypass (Shift model — walk to your computer)

## Build

Open in Android Studio or:

```powershell
.\gradlew.bat assembleDebug
```

Requires JDK 17+ and Android SDK 35.
