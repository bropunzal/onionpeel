/**
 * OnionPeel cloud companion — peel, policy, and delayed unpeel are controlled here.
 * Phone polls GET /api/sync over HTTPS (or LAN HTTP in local dev).
 */
const http = require("http");
const crypto = require("crypto");
const express = require("express");
const fs = require("fs");
const path = require("path");
const { createAuthStore, betaLoginRequired } = require("./auth");

function loadDotEnv() {
  const envPath = path.join(__dirname, ".env");
  if (!fs.existsSync(envPath)) return;
  for (const line of fs.readFileSync(envPath, "utf8").split("\n")) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eq = trimmed.indexOf("=");
    if (eq === -1) continue;
    const key = trimmed.slice(0, eq).trim();
    const value = trimmed.slice(eq + 1).trim();
    if (!process.env[key]) process.env[key] = value;
  }
}

loadDotEnv();

const PORT = process.env.PORT || 8787;
const LEGACY_TOKEN = process.env.ONIONPEEL_TOKEN || null;
const PUBLIC_URL = (process.env.PUBLIC_URL || "").replace(/\/$/, "");
const DATA_DIR = process.env.DATA_DIR || __dirname;
const DEVICES_FILE = path.join(DATA_DIR, "devices.json");
const LEGACY_STATE_FILE = path.join(__dirname, "state.json");

const DEFAULT_BLOCKED_URLS = [
  "instagram.com",
  "youtube.com",
  "youtu.be",
  "m.youtube.com",
  "tiktok.com",
  "reddit.com",
  "facebook.com",
  "x.com",
  "twitter.com",
  "snapchat.com",
  "pinterest.com",
  "threads.net",
];

const DEFAULT_ALLOW_LIST = [
  "com.google.android.dialer",
  "com.android.dialer",
  "com.google.android.apps.messaging",
  "com.android.mms",
  "com.google.android.apps.maps",
  "com.android.camera2",
  "com.google.android.GoogleCamera",
  "com.android.chrome",
  "com.microsoft.emmx",
  "com.sec.android.app.sbrowser",
  "org.mozilla.firefox",
  "com.brave.browser",
];

const legacyMode = Boolean(LEGACY_TOKEN);

function defaultDeviceState(ownerUserId = null) {
  return {
    peelDesired: false,
    unpeelAt: null,
    exitDelayHours: 24,
    blockedUrls: [...DEFAULT_BLOCKED_URLS],
    allowList: [...DEFAULT_ALLOW_LIST],
    lastPhoneReport: null,
    createdAt: Date.now(),
    ownerUserId,
  };
}

function loadDevices() {
  try {
    if (fs.existsSync(DEVICES_FILE)) {
      return JSON.parse(fs.readFileSync(DEVICES_FILE, "utf8"));
    }
  } catch (e) {
    console.warn("Could not load devices.json:", e.message);
  }
  return {};
}

let devices = loadDevices();
const authStore = createAuthStore(DATA_DIR);

function saveDevices() {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.writeFileSync(DEVICES_FILE, JSON.stringify(devices, null, 2));
}

function migrateLegacyState() {
  if (!legacyMode || Object.keys(devices).length > 0) return;
  try {
    if (fs.existsSync(LEGACY_STATE_FILE)) {
      const legacy = JSON.parse(fs.readFileSync(LEGACY_STATE_FILE, "utf8"));
      devices[LEGACY_TOKEN] = { ...defaultDeviceState(), ...legacy };
      saveDevices();
      console.log("Migrated legacy state.json to devices.json");
    }
  } catch (e) {
    console.warn("Legacy state migration skipped:", e.message);
  }
}

migrateLegacyState();

if (legacyMode && !devices[LEGACY_TOKEN]) {
  devices[LEGACY_TOKEN] = defaultDeviceState();
  saveDevices();
}

function normalizeHost(raw) {
  let host = String(raw || "").trim().toLowerCase();
  host = host.replace(/^https?:\/\//, "").replace(/^www\./, "");
  const slash = host.indexOf("/");
  if (slash >= 0) host = host.slice(0, slash);
  return host;
}

function effectivePeelDesired(state) {
  if (state.unpeelAt && Date.now() >= state.unpeelAt) {
    state.peelDesired = false;
    state.unpeelAt = null;
    saveDevices();
  }
  return Boolean(state.peelDesired);
}

function syncPayload(state) {
  return {
    peelDesired: effectivePeelDesired(state),
    blockedUrls: state.blockedUrls,
    allowList: state.allowList,
    exitDelayHours: state.exitDelayHours,
    unpeelAt: state.unpeelAt,
    serverTime: Date.now(),
  };
}

function publicServerUrl(req) {
  if (PUBLIC_URL) return PUBLIC_URL;
  const host = req.get("host");
  if (host) return `${req.protocol}://${host}`;
  return `http://localhost:${PORT}`;
}

function getLanIps() {
  const nets = require("os").networkInterfaces();
  const ips = [];
  for (const name of Object.keys(nets)) {
    for (const net of nets[name] || []) {
      if (net.family === "IPv4" && !net.internal) ips.push(net.address);
    }
  }
  return ips;
}

function phoneUrls(req) {
  if (PUBLIC_URL) return [PUBLIC_URL];
  return getLanIps().map((ip) => `http://${ip}:${PORT}`);
}

function extractToken(req) {
  const header = req.headers.authorization || "";
  if (header.startsWith("Bearer ")) return header.slice(7);
  return req.query.token || "";
}

function extractSessionToken(req) {
  const header = req.headers["x-session-token"];
  if (header) return header;
  const cookie = req.headers.cookie || "";
  const match = cookie.match(/(?:^|;\s*)onionpeel_session=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : "";
}

function requireSession(req, res, next) {
  const session = authStore.getSession(extractSessionToken(req));
  if (!session) {
    return res.status(401).json({ error: "login_required" });
  }
  req.sessionUser = session;
  next();
}

function auth(req, res, next) {
  const token = extractToken(req);
  const state = devices[token];
  if (!state) {
    return res.status(401).json({ error: "unauthorized" });
  }
  req.deviceToken = token;
  req.deviceState = state;
  next();
}

// Rate limit device creation: 5 per hour per IP
const createAttempts = new Map();
const CREATE_LIMIT = 5;
const CREATE_WINDOW_MS = 60 * 60 * 1000;

function rateLimitCreate(req, res, next) {
  const ip = req.ip || req.socket.remoteAddress || "unknown";
  const now = Date.now();
  const entry = createAttempts.get(ip) || { count: 0, resetAt: now + CREATE_WINDOW_MS };
  if (now > entry.resetAt) {
    entry.count = 0;
    entry.resetAt = now + CREATE_WINDOW_MS;
  }
  if (entry.count >= CREATE_LIMIT) {
    return res.status(429).json({ error: "too_many_devices", retryAfterMs: entry.resetAt - now });
  }
  entry.count += 1;
  createAttempts.set(ip, entry);
  next();
}

const app = express();
app.set("trust proxy", true);
app.use(express.json());

/** Public: server info (no secrets) */
app.get("/api/info", (req, res) => {
  res.json({
    serverUrl: publicServerUrl(req),
    legacyMode,
    multiDevice: !legacyMode,
    betaLoginRequired: betaLoginRequired(),
  });
});

/** Beta: register a tester account */
app.post("/api/auth/register", rateLimitCreate, (req, res) => {
  const result = authStore.register({
    email: req.body.email,
    password: req.body.password,
    inviteCode: req.body.inviteCode,
  });
  if (result.error) {
    const status = result.error === "email_taken" ? 409 : 400;
    return res.status(status).json(result);
  }
  const sessionToken = authStore.createSession(result.userId);
  res.status(201).json({
    sessionToken,
    email: result.email,
  });
});

/** Beta: log in */
app.post("/api/auth/login", rateLimitCreate, (req, res) => {
  const result = authStore.login({
    email: req.body.email,
    password: req.body.password,
  });
  if (result.error) {
    return res.status(401).json({ error: "invalid_credentials" });
  }
  const sessionToken = authStore.createSession(result.userId);
  res.json({
    sessionToken,
    email: result.email,
  });
});

/** Beta: current session */
app.get("/api/auth/me", (req, res) => {
  const session = authStore.getSession(extractSessionToken(req));
  if (!session) {
    return res.status(401).json({ error: "login_required" });
  }
  res.json({ email: session.email });
});

/** Beta: log out */
app.post("/api/auth/logout", (req, res) => {
  authStore.deleteSession(extractSessionToken(req));
  res.json({ ok: true });
});

/** List devices for the logged-in beta tester */
app.get("/api/devices", requireSession, (req, res) => {
  const owned = Object.entries(devices)
    .filter(([, state]) => state.ownerUserId === req.sessionUser.userId)
    .map(([token, state]) => ({
      token,
      label: state.label || `Device ${token.slice(0, 6)}`,
      createdAt: state.createdAt,
      lastPhoneReport: state.lastPhoneReport,
    }))
    .sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
  res.json({ devices: owned });
});

/** Create a new paired device (multi-device mode only) */
app.post("/api/devices", rateLimitCreate, (req, res) => {
  if (legacyMode) {
    return res.status(400).json({
      error: "legacy_mode",
      message: "This server uses a fixed ONIONPEEL_TOKEN. Use that token instead of creating a device.",
    });
  }
  if (betaLoginRequired()) {
    const session = authStore.getSession(extractSessionToken(req));
    if (!session) {
      return res.status(401).json({ error: "login_required" });
    }
    req.sessionUser = session;
  }
  const token = crypto.randomBytes(24).toString("hex");
  const ownerUserId = req.sessionUser?.userId || null;
  const label = String(req.body.label || "").trim().slice(0, 64);
  devices[token] = {
    ...defaultDeviceState(ownerUserId),
    ...(label ? { label } : {}),
  };
  saveDevices();
  res.status(201).json({
    token,
    serverUrl: publicServerUrl(req),
    label: devices[token].label || null,
  });
});

/** Phone polls this every ~15s */
app.get("/api/sync", auth, (req, res) => {
  res.json(syncPayload(req.deviceState));
});

/** Phone reports enforcement state + installed app catalog */
app.post("/api/phone/report", auth, (req, res) => {
  req.deviceState.lastPhoneReport = {
    peelActive: Boolean(req.body.peelActive),
    apps: Array.isArray(req.body.apps) ? req.body.apps : [],
    at: Date.now(),
  };
  saveDevices();
  res.json({ ok: true });
});

/** Browser: peel immediately, or schedule delayed unpeel */
app.post("/api/peel", auth, (req, res) => {
  const state = req.deviceState;
  const enabled = Boolean(req.body.enabled);
  if (enabled) {
    state.peelDesired = true;
    state.unpeelAt = null;
  } else if (state.peelDesired || effectivePeelDesired(state)) {
    const hours = Math.min(168, Math.max(1, Number(state.exitDelayHours) || 24));
    state.exitDelayHours = hours;
    state.peelDesired = true;
    state.unpeelAt = Date.now() + hours * 60 * 60 * 1000;
  } else {
    state.peelDesired = false;
    state.unpeelAt = null;
  }
  saveDevices();
  res.json({
    peelDesired: effectivePeelDesired(state),
    unpeelAt: state.unpeelAt,
    exitDelayHours: state.exitDelayHours,
  });
});

/** Browser: cancel a pending delayed unpeel */
app.post("/api/peel/cancel", auth, (req, res) => {
  const state = req.deviceState;
  state.unpeelAt = null;
  state.peelDesired = true;
  saveDevices();
  res.json({ peelDesired: effectivePeelDesired(state), unpeelAt: null });
});

/** Browser: update blocked URLs, allow-list, unpeel delay hours */
app.post("/api/policy", auth, (req, res) => {
  const state = req.deviceState;
  if (Array.isArray(req.body.blockedUrls)) {
    state.blockedUrls = [
      ...new Set(req.body.blockedUrls.map(normalizeHost).filter(Boolean)),
    ];
  }
  if (Array.isArray(req.body.allowList)) {
    state.allowList = [...new Set(req.body.allowList.filter(Boolean))];
  }
  if (req.body.exitDelayHours != null) {
    const hours = Number(req.body.exitDelayHours);
    if (!Number.isNaN(hours)) {
      state.exitDelayHours = Math.min(168, Math.max(1, Math.round(hours)));
    }
  }
  saveDevices();
  res.json({
    blockedUrls: state.blockedUrls,
    allowList: state.allowList,
    exitDelayHours: state.exitDelayHours,
  });
});

/** Browser: dashboard state (requires auth) */
app.get("/api/status", auth, (req, res) => {
  const state = req.deviceState;
  res.json({
    peelDesired: effectivePeelDesired(state),
    unpeelAt: state.unpeelAt,
    exitDelayHours: state.exitDelayHours,
    blockedUrls: state.blockedUrls,
    allowList: state.allowList,
    lastPhoneReport: state.lastPhoneReport,
    serverTime: Date.now(),
    serverUrl: publicServerUrl(req),
    phoneUrls: phoneUrls(req),
  });
});

app.use(express.static(path.join(__dirname, "public")));

const server = http.createServer(app);
server.listen(PORT, "0.0.0.0", () => {
  const ips = getLanIps();
  console.log("\n  OnionPeel companion\n");
  if (PUBLIC_URL) {
    console.log(`  Public URL: ${PUBLIC_URL}`);
  } else {
    console.log(`  Browser:  http://localhost:${PORT}`);
    for (const ip of ips) {
      console.log(`  LAN URL:  http://${ip}:${PORT}`);
    }
  }
  if (legacyMode) {
    console.log(`\n  Legacy mode — fixed token: ${LEGACY_TOKEN}\n`);
  } else if (betaLoginRequired()) {
    console.log("\n  Beta login enabled — testers register with invite code, then create devices\n");
  } else {
    console.log("\n  Multi-device mode — create devices at /api/devices or in the web UI\n");
  }
});
