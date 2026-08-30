# Onionpeel — Closed Beta (v0.2)

Thank you for testing. This is an **allow-list dumb phone** controlled **only from your desktop browser**. Peel mode uses Android **Device Owner** — setup is involved but intentional.

## What you need

| Requirement | Notes |
|-------------|--------|
| Android phone | API 26+ (Android 8.0). Tested on Samsung; others should work. |
| Windows or Mac PC | For desktop companion + `adb` provisioning |
| USB cable | Data-capable, not charge-only |
| Same Wi‑Fi | Phone and PC on one network for companion sync |
| Node.js 18+ | [nodejs.org](https://nodejs.org) — for desktop companion |
| Android platform tools | `adb` (via [Android Studio](https://developer.android.com/studio) SDK) |
| ~30 minutes | Factory reset + provisioning (one-time) |

## What beta testers should expect

- **Factory reset required** — Device Owner cannot be set on a phone with Google/Samsung accounts already added.
- **Desktop required** — Peel, unpeel, blocked sites, allowed apps, and unpeel delay are **only** on the companion web page.
- **Alpha-quality** — bugs, rough edges, and missing features are expected. Please report everything.

## Download

1. Get the latest **`onionpeel-*-beta.apk`** from [GitHub Releases](https://github.com/bropunzal/dumbphone/releases) (or link your organizer sent you).
2. Clone or download this repo for the **desktop companion** (`companion/` folder).

## Setup (one-time)

Follow the **13-step guide** in the Onionpeel app or at `http://localhost:8787` (Show steps) after starting the companion.

### Summary

**Phone**
1. Back up anything important.
2. **Factory reset.**
3. During setup: **Skip Google account** and **Skip Samsung** (or any OEM account).
4. Connect **Wi‑Fi** (same network as PC).
5. Enable **USB debugging** (Build number ×7 → Developer options).
6. Plug in USB → **File transfer** → allow debugging.

**PC**
```powershell
# Install APK (from repo root)
adb install releases\onionpeel-0.2.0-beta.1.apk

# Set Device Owner — must succeed before any account is added
adb shell dpm set-device-owner com.ateeb.onionpeel/.admin.OnionpeelDeviceAdminReceiver
```

Expected: `Success: Device owner set to package com.ateeb.onionpeel/...`

**Companion**
```powershell
cd companion
npm install
# Optional: fixed token so it does not change on restart
$env:ONIONPEEL_TOKEN = "pick-a-long-random-secret"
npm start
```

Open `http://localhost:8787` → copy **Phone URL** + **token**.

**Phone app**
- Open Onionpeel → enter URL + token → **LINK DESKTOP**
- Wait ~15s for sync
- On desktop: set blocked sites, allowed apps, unpeel delay → **Peel phone**

**After Device Owner:** you may add a **Google account** for Gmail/Play Store.

## Daily use

| Action | Where |
|--------|--------|
| Peel / request unpeel | Desktop browser only |
| Change blocked sites / allowed apps | Desktop browser only |
| Change unpeel delay (hours) | Desktop browser only |
| View status | Phone app (read-only) |

**Unpeel delay:** Requesting unpeel starts a timer (default 24h). Phone stays peeled until the timer ends. Cancel on desktop if needed.

## Updating the app during beta

Peel mode **blocks app installs**. To update:

1. Desktop → request unpeel (or wait for timer).
2. Phone shows **OPEN** (~15s after sync).
3. `adb install -r new-beta.apk`
4. Desktop → **Peel phone** again.

If companion **restarts**, token may change — re-link on phone unless you set `ONIONPEEL_TOKEN`.

## Known limitations (beta)

- URL blocking: **Chrome, Edge, Samsung Internet** only (not Firefox/in-app browsers).
- Feed apps (YouTube app, Instagram, etc.) are **hard-suspended** when peeled.
- Companion is **HTTP on LAN** — use trusted home Wi‑Fi only.
- No cloud remote control — PC must be on and companion running.
- Samsung/other OEM accounts **before** Device Owner will block provisioning.
- Completely unbypassable peel is impossible (factory reset always exists) — this is practical-maximum friction.

## What to test

- [ ] Full provisioning on your phone model
- [ ] Peel / delayed unpeel / cancel unpeel
- [ ] Blocked sites in Chrome and Samsung Internet
- [ ] YouTube **app** blocked when peeled
- [ ] Allowed apps (Phone, Maps, Chrome) still work
- [ ] Reboot while peeled — policy returns
- [ ] Desktop policy changes sync to phone
- [ ] Adding Google account **after** setup

## Feedback

Please report via [GitHub Issues](https://github.com/bropunzal/dumbphone/issues/new/choose) → **Beta feedback**, including:

- Phone model + Android version
- What you expected vs what happened
- Screenshots if useful
- Whether phone was peeled or open

**Do not post pairing tokens publicly.**

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `set-device-owner` fails: accounts | Remove all accounts or factory reset; skip Google + Samsung during setup |
| `adb devices` empty | Re-plug USB, allow debugging, try File transfer mode |
| `npm` not recognized | Install Node.js; restart terminal; or use full path `C:\Program Files\nodejs\npm.cmd` |
| Install failed: user restriction | Phone is peeled — unpeel from desktop first |
| Phone not syncing | Same Wi‑Fi? Companion running? Re-link with current token |
| YouTube still works | Uncheck YouTube in allowed apps; add `youtube.com` to blocked sites; wait 15s |

---

## For maintainers (shipping a beta build)

1. Bump `versionCode` / `versionName` in `app/build.gradle.kts`.
2. Run `.\scripts\build-beta.ps1` — outputs `releases/onionpeel-<version>-beta.apk`.
3. Commit source changes; push to `main`.
4. Create a [GitHub Release](https://github.com/bropunzal/dumbphone/releases/new) tagged `v0.2.0-beta.1` (match `versionName`).
5. Attach the APK; paste notes from `.github/RELEASE_TEMPLATE/beta.md`.
6. Send testers: release link + `BETA.md` + `companion/` folder (or full repo clone).

**Tester invite checklist:** factory reset warning, Node.js + adb, same Wi-Fi, optional `.env` token, feedback issue link.
