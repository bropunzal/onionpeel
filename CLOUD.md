# OnionPeel Cloud Companion

Host the control panel and sync API on the internet so testers do not need a PC running `npm start` or the same Wi‑Fi network.

## Architecture

- **One shared HTTPS URL** for everyone (e.g. `https://onionpeel-production.up.railway.app`)
- Each person clicks **Create new device** in the web UI → unique pairing token
- Phone enters **Server URL + token** once → polls every ~15s over the internet
- Control peel from **any browser** with the saved token

Local `npm start` still works for development (legacy single-token mode via `ONIONPEEL_TOKEN`).

## Deploy to Railway (recommended)

### 1. Create project

1. Sign up at [railway.app](https://railway.app)
2. **New Project** → **Deploy from GitHub repo** → select `bropunzal/onionpeel`
3. Set **Root Directory** to `companion`

### 2. Environment variables

| Variable | Required | Example |
|----------|----------|---------|
| `PUBLIC_URL` | Yes | `https://onionpeel-production.up.railway.app` |
| `PORT` | Auto-set by Railway | `8787` |
| `DATA_DIR` | Yes (with volume) | `/data` |
| `BETA_INVITE_CODES` | Yes for closed beta | `onion-beta-2026,friend-invite-1` |

Do **not** set `ONIONPEEL_TOKEN` in production — that enables legacy single-device mode.

When `BETA_INVITE_CODES` is set, testers must **create an account** with an invite code before they can create a device pairing. Each tester gets their own login; device tokens still control the phone.

### 3. Persistent storage

1. In Railway: **Add Volume** → mount at `/data`
2. Set `DATA_DIR=/data` so `devices.json` survives redeploys

### 4. Custom domain (optional)

Railway → Settings → Networking → generate domain or add your own. Update `PUBLIC_URL` to match.

### 5. Verify

```bash
curl https://YOUR_URL/api/info
# {"serverUrl":"https://YOUR_URL","legacyMode":false,"multiDevice":true}

curl -X POST https://YOUR_URL/api/devices
# {"token":"...","serverUrl":"https://YOUR_URL"}
```

Open the URL in a browser → **Create account** (invite code) → **Create new device** → copy Server URL + token to the phone app.

## Local development

```powershell
cd companion
npm install
npm start
# Legacy mode: set ONIONPEEL_TOKEN in .env for a fixed token
```

Multi-device mode runs when `ONIONPEEL_TOKEN` is **not** set.

## Security notes

- Pairing tokens are secrets — treat like passwords
- Beta invite codes gate who can register — share privately with testers
- `POST /api/devices` and auth endpoints are rate-limited (5/hour/IP)
- `/api/status` requires Bearer auth (token never leaked publicly)
- Use HTTPS in production (`PUBLIC_URL` must start with `https://`)

## Alternatives

The same [`Dockerfile`](Dockerfile) works on Fly.io, Render, or any Docker host. Mount a persistent volume at `DATA_DIR`.

## Cost

Railway hobby usage is typically ~$5/month for a small always-on Node service with a volume.
