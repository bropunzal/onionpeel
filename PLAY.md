# Google Play — Closed Testing

Upload a signed **AAB** to distribute OnionPeel without manual APK sideloading. Device Owner setup via `adb` is still required once per phone.

## Prerequisites

- Google Play Developer account ($25 one-time): [play.google.com/console](https://play.google.com/console)
- Privacy policy URL: [PRIVACY.md](PRIVACY.md) on GitHub
- Signed release keystore (generate once, back up securely)

## 1. Generate signing keystore (once)

```powershell
keytool -genkey -v `
  -keystore onionpeel-release.jks `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias onionpeel
```

Store `onionpeel-release.jks` **outside the repo** or in a secure location. Never commit it.

## 2. Configure signing

```powershell
copy keystore.properties.example keystore.properties
# Edit keystore.properties with your paths and passwords
```

Or set environment variables:

- `ONIONPEEL_KEYSTORE` — path to `.jks` file
- `ONIONPEEL_KEYSTORE_PASSWORD`
- `ONIONPEEL_KEY_ALIAS`
- `ONIONPEEL_KEY_PASSWORD`

## 3. Build release AAB

```powershell
.\scripts\build-release.ps1
```

Outputs:

- `releases/onionpeel-0.2.0-beta.1.aab` → upload to Play Console
- `releases/onionpeel-0.2.0-beta.1.apk` → GitHub Release fallback

## 4. Create app in Play Console

1. **Create app** → name: OnionPeel, default language, app/game, free
2. **App access** — all functionality available without special access (testers use closed track)
3. **Ads** — No
4. **Content rating** — complete questionnaire (utility/productivity)
5. **Target audience** — 18+ recommended (Device Owner tool)
6. **Data safety** — declare:
   - App activity: app interactions (peel state sync) — not shared with third parties
   - Device or other IDs: pairing token — optional, for app functionality
   - See [PRIVACY.md](PRIVACY.md) for accurate answers
7. **Privacy policy** — `https://github.com/bropunzal/onionpeel/blob/main/PRIVACY.md`
8. **Device Admin** — declare Device Owner / device admin usage in store listing description

## 5. Closed testing track

1. **Testing → Closed testing** → Create track (e.g. "Beta testers")
2. **Create new release** → upload `onionpeel-*.aab`
3. Add release notes (copy from `.github/RELEASE_TEMPLATE/beta.md`)
4. **Testers** → Create email list → add tester Gmail addresses
5. Copy the **opt-in URL** and send to testers with [BETA.md](BETA.md) + cloud companion URL

## 6. Tester install flow

1. Open Play opt-in link on phone → accept → install from Play Store
2. Factory reset provisioning (see BETA.md)
3. `adb shell dpm set-device-owner com.ateeb.onionpeel/.admin.OnionpeelDeviceAdminReceiver`
4. Pair cloud companion URL + token in OnionPeel app

## Updating

1. Bump `versionCode` (required, must increase) and `versionName` in `app/build.gradle.kts`
2. `.\scripts\build-release.ps1`
3. Upload new AAB to the same closed testing track
4. Testers update from Play Store (phone must be OPEN — unpeel first)

## Play Console review notes

OnionPeel uses Device Owner for parental-style / focus-mode app blocking. In **App content → Sensitive app permissions**, explain:

- Device admin is required to suspend apps and enforce URL policies in managed browsers
- Users explicitly provision via adb after factory reset
- No remote surveillance; user controls their own device via paired companion token

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Upload rejected: version code | Increase `versionCode` in `build.gradle.kts` |
| Signing error | Check `keystore.properties` paths and passwords |
| Device Owner app policy | Ensure privacy policy and data safety form are complete |
| Testers can't see app | Confirm they accepted opt-in link with same Google account as Play |
