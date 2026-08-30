# Onionpeel desktop companion

Shift-style control: **peel mode is toggled only in this browser UI**, not on the phone.

## Start

```powershell
cd C:\Users\Ateeb\dumbphone\companion
npm install
npm start
```

Open `http://localhost:8787` on your PC. Copy the **pairing token** and your PC's LAN IP (shown in the terminal) into the Onionpeel app on your phone.

## How it works

1. Desktop holds `peelDesired` (true/false).
2. Phone polls `GET /api/sync` every 15 seconds on the same Wi-Fi.
3. Phone applies or removes peel mode to match — no toggle on the phone.
4. **PEELED / OPEN** hero on both desktop and phone (COROS x SATISFY–inspired watch-face typography and metric tiles).

## Security

- LAN only by default (HTTP). Use trusted home Wi-Fi.
- Token is printed at server start; store in phone once during pairing.
- Optional: set `ONIONPEEL_TOKEN` env var to a fixed secret.
