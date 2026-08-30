# OnionPeel Privacy Policy

**Last updated:** August 2026  
**Contact:** [GitHub Issues](https://github.com/bropunzal/onionpeel/issues)

## Summary

OnionPeel is a personal focus tool. It runs on your Android phone as a Device Owner app and syncs peel-mode policy from a companion server you pair with. **We do not sell your data.** Data stays on your phone and the companion server you connect to.

## What OnionPeel collects

### On your phone (local only)

- Your allow-list of apps and blocked URL domains
- Peel mode on/off state
- Companion server URL and pairing token (stored locally for sync)

### Sent to the companion server (when paired)

- Peel enforcement status (peeled or open)
- List of installed apps (package names and labels) so you can choose allowed apps in the browser UI
- No message content, browsing history, location, contacts, or photos

### Not collected

- Analytics or advertising IDs
- Account registration (pairing uses a random token only)
- Data sent to third parties

## Companion server

If you use the **hosted OnionPeel companion**, your phone polls the server over HTTPS every ~15 seconds. Policy and peel state for your device token are stored on that server.

If you **self-host** the companion, your data stays on your infrastructure.

## Permissions

| Permission | Why |
|------------|-----|
| Device Admin / Device Owner | Suspend non-allowed apps and enforce URL policy |
| QUERY_ALL_PACKAGES | List apps so you can choose the allow-list |
| Internet | Sync with companion server |
| Boot completed | Re-apply peel policy after reboot |

## Data retention

- **Phone:** Data remains until you uninstall, factory reset, or clear app data.
- **Companion server:** Device state persists until the server operator deletes it.

## Your choices

- Unpair by clearing companion URL/token in the app
- Factory reset removes Device Owner and all local data
- Request deletion of cloud device state by contacting the server operator

## Children

OnionPeel is not directed at children under 13.

## Changes

We may update this policy. The current version is always in this repository.

## Play Store URL

Use this file as your privacy policy URL in Google Play Console:

`https://github.com/bropunzal/onionpeel/blob/main/PRIVACY.md`
