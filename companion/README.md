# Onionpeel desktop companion

Shift-style control: **peel mode is toggled only in this browser UI**, not on the phone.

## Start

```powershell
cd C:\Users\Ateeb\dumbphone\companion
npm install
copy .env.example .env   # optional — set ONIONPEEL_TOKEN for stable pairing
npm start
```

Open `http://localhost:8787` on your PC. Copy the **pairing token** and your PC's LAN IP (shown in the terminal) into the Onionpeel app on your phone.

For closed beta, **set a fixed token** in `.env` so testers do not re-link after every companion restart.

## How it works

1. Desktop holds peel state, **blocked URLs**, **allowed apps**, and **unpeel delay hours**.
2. Phone polls `GET /api/sync` every 15 seconds on the same Wi-Fi.
3. Phone applies policy and peel mode to match — no toggle or config edits on the phone.
4. **Request unpeel** starts a delay (default 24h, configurable 1–168h) before the phone opens.

## Security

- LAN only by default (HTTP). Use trusted home Wi-Fi.
- Token is printed at server start; store in phone once during pairing.
- Optional: set `ONIONPEEL_TOKEN` env var to a fixed secret.
