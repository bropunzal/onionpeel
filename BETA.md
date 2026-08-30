# OnionPeel — Closed Beta (v0.2)

Thank you for testing. This is an **allow-list dumb phone** controlled **only from the cloud companion browser**. Peel mode uses Android **Device Owner** — setup is involved but intentional.

## What you need

| Requirement | Notes |
|-------------|--------|
| Android phone | API 26+ (Android 8.0). Tested on Samsung; others should work. |
| Windows or Mac PC | For **one-time** `adb` Device Owner setup only |
| USB cable | Data-capable, not charge-only |
| Internet on phone | Cloud sync over HTTPS — no same Wi‑Fi as PC required |
| Android platform tools | `adb` (via [Android Studio](https://developer.android.com/studio) SDK) |
| ~30 minutes | Factory reset + provisioning (one-time) |

## What beta testers should expect

- **Factory reset required** — Device Owner cannot be set on a phone with Google/Samsung accounts already added.
- **Browser required** — Peel, unpeel, blocked sites, allowed apps, and unpeel delay are **only** on the companion web page.
- **Alpha-quality** — bugs, rough edges, and missing features are expected. Please report everything.

## Download

**Primary:** Install from **Google Play closed testing** (invite link from organizer).

**Fallback:** [GitHub Releases](https://github.com/bropunzal/onionpeel/releases) APK + `adb install`.

## Setup (one-time)

Follow the **13-step guide** in the OnionPeel app, or open the [cloud companion](CLOUD.md) URL in a browser.

### Summary

**Phone**
1. Back up anything important.
2. **Factory reset.**
3. During setup: **Skip Google account** and **Skip Samsung** (or any OEM account).
4. Connect **Wi‑Fi** (any network with internet).
5. Enable **USB debugging** (Build number ×7 → Developer options).
6. Plug in USB → **File transfer** → allow debugging.

**PC (one-time provisioning)**
```powershell
# After installing from Play Store or:
# adb install releases\onionpeel-0.2.0-beta.1.apk

adb shell dpm set-device-owner com.ateeb.onionpeel/.admin.OnionpeelDeviceAdminReceiver
```

Expected: `Success: Device owner set to package com.ateeb.onionpeel/...`

**Cloud companion**
1. Open the companion URL (from organizer, or see [CLOUD.md](CLOUD.md)).
2. Click **Create new device**.
3. Copy **Server URL** + **pairing token**.

**Phone app**
- Open OnionPeel → enter Server URL + token → **LINK DESKTOP**
- Wait ~15s for sync
- In browser: set blocked sites, allowed apps, unpeel delay → **Peel phone**

**After Device Owner:** you may add a **Google account** for Gmail/Play Store.

## Daily use

| Action | Where |
|--------|--------|
| Peel / request unpeel | Companion browser (any device) |
| Change blocked sites / allowed apps | Companion browser |
| Change unpeel delay (hours) | Companion browser |
| View status | Phone app (read-only) |

**Unpeel delay:** Requesting unpeel starts a timer (default 24h). Phone stays peeled until the timer ends. Cancel in the companion if needed.

## Updating the app during beta

Peel mode **blocks app installs**. To update:

1. Companion → request unpeel (or wait for timer).
2. Phone shows **OPEN** (~15s after sync).
3. Update from Play Store, or `adb install -r new.apk`
4. Companion → **Peel phone** again.

Save your pairing token — you need it to control peel from any browser.

## Known limitations (beta)

- URL blocking: **Chrome, Edge, Samsung Internet** only (not Firefox/in-app browsers).
- Feed apps (YouTube app, Instagram, etc.) are **hard-suspended** when peeled.
- Device Owner setup still requires **adb** once (Android platform limit).
- Samsung/other OEM accounts **before** Device Owner will block provisioning.
- Completely unbypassable peel is impossible (factory reset always exists) — this is practical-maximum friction.

## What to test

- [ ] Full provisioning on your phone model
- [ ] Peel / delayed unpeel / cancel unpeel via cloud companion
- [ ] Blocked sites in Chrome and Samsung Internet
- [ ] YouTube **app** blocked when peeled
- [ ] Allowed apps (Phone, Maps, Chrome) still work
- [ ] Reboot while peeled — policy returns
- [ ] Companion policy changes sync to phone over cellular/Wi‑Fi
- [ ] Adding Google account **after** setup

## Feedback

Please report via [GitHub Issues](https://github.com/bropunzal/onionpeel/issues/new/choose) → **Beta feedback**, including:

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
| Install failed: user restriction | Phone is peeled — unpeel from companion first |
| Phone not syncing | Internet on phone? Correct HTTPS URL + token? |
| YouTube still works | Uncheck YouTube in allowed apps; add `youtube.com` to blocked sites; wait 15s |

---

## For maintainers

See [CLOUD.md](CLOUD.md) (companion deploy), [PLAY.md](PLAY.md) (Play Console), and [PRIVACY.md](PRIVACY.md) (privacy policy URL).

**Ship checklist:**
1. Bump `versionCode` / `versionName` in `app/build.gradle.kts`.
2. `.\scripts\build-release.ps1` → signed AAB for Play + APK for GitHub.
3. Deploy companion to Railway with `PUBLIC_URL` + volume at `/data`.
4. Upload AAB to Play closed testing; attach APK to GitHub Release.
5. Send testers: Play link + companion URL + `BETA.md`.
