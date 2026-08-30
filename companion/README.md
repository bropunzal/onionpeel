# OnionPeel desktop companion

Shift-style control: **peel mode is toggled only in the browser UI**, not on the phone.

## Cloud (production)

Deploy to Railway — see **[CLOUD.md](../CLOUD.md)**.

Testers open the public HTTPS URL → **Create new device** → copy Server URL + token to the phone app.

## Local dev

```powershell
cd companion
npm install
copy .env.example .env   # optional ONIONPEEL_TOKEN for legacy single-device mode
npm start
```

Open `http://localhost:8787`. Without `ONIONPEEL_TOKEN`, the server runs multi-device mode (same as cloud).

## How it works

1. Companion holds peel state, **blocked URLs**, **allowed apps**, and **unpeel delay hours** per device token.
2. Phone polls `GET /api/sync` every 15 seconds over HTTPS.
3. Phone applies policy and peel mode to match — no toggle or config edits on the phone.
4. **Request unpeel** starts a delay (default 24h, configurable 1–168h) before the phone opens.

## API

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `GET /api/info` | none | Server URL, mode |
| `POST /api/devices` | none | Create pairing (rate-limited) |
| `GET /api/sync` | Bearer | Phone poll |
| `GET /api/status` | Bearer | Browser dashboard |
| `POST /api/peel`, `/api/policy`, … | Bearer | Control |

## Security

- HTTPS in production (`PUBLIC_URL`)
- Pairing tokens are secrets — never share publicly
- `POST /api/devices` rate-limited to 5/hour/IP
