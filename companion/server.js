/**
 * Onionpeel desktop companion — peel, policy, and delayed unpeel are controlled here.
 * Phone polls GET /api/sync on the same Wi-Fi LAN.
 */
const http = require("http");
const crypto = require("crypto");
const express = require("express");
const fs = require("fs");
const path = require("path");

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
const TOKEN = process.env.ONIONPEEL_TOKEN || crypto.randomBytes(16).toString("hex");
const STATE_FILE = path.join(__dirname, "state.json");

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

function loadState() {
  try {
    if (fs.existsSync(STATE_FILE)) {
      return JSON.parse(fs.readFileSync(STATE_FILE, "utf8"));
    }
  } catch (e) {
    console.warn("Could not load state.json, using defaults:", e.message);
  }
  return {
    peelDesired: false,
    unpeelAt: null,
    exitDelayHours: 24,
    blockedUrls: [...DEFAULT_BLOCKED_URLS],
    allowList: [...DEFAULT_ALLOW_LIST],
    lastPhoneReport: null,
  };
}

let state = loadState();

function saveState() {
  fs.writeFileSync(STATE_FILE, JSON.stringify(state, null, 2));
}

function normalizeHost(raw) {
  let host = String(raw || "").trim().toLowerCase();
  host = host.replace(/^https?:\/\//, "").replace(/^www\./, "");
  const slash = host.indexOf("/");
  if (slash >= 0) host = host.slice(0, slash);
  return host;
}

function effectivePeelDesired() {
  if (state.unpeelAt && Date.now() >= state.unpeelAt) {
    state.peelDesired = false;
    state.unpeelAt = null;
    saveState();
  }
  return Boolean(state.peelDesired);
}

function syncPayload() {
  return {
    peelDesired: effectivePeelDesired(),
    blockedUrls: state.blockedUrls,
    allowList: state.allowList,
    exitDelayHours: state.exitDelayHours,
    unpeelAt: state.unpeelAt,
    serverTime: Date.now(),
  };
}

const app = express();
app.use(express.json());

function auth(req, res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : req.query.token;
  if (token !== TOKEN) {
    return res.status(401).json({ error: "unauthorized" });
  }
  next();
}

/** Phone polls this every ~15s */
app.get("/api/sync", auth, (req, res) => {
  res.json(syncPayload());
});

/** Phone reports enforcement state + installed app catalog */
app.post("/api/phone/report", auth, (req, res) => {
  state.lastPhoneReport = {
    peelActive: Boolean(req.body.peelActive),
    apps: Array.isArray(req.body.apps) ? req.body.apps : [],
    at: Date.now(),
  };
  saveState();
  res.json({ ok: true });
});

/** Desktop: peel immediately, or schedule delayed unpeel */
app.post("/api/peel", auth, (req, res) => {
  const enabled = Boolean(req.body.enabled);
  if (enabled) {
    state.peelDesired = true;
    state.unpeelAt = null;
  } else if (state.peelDesired || effectivePeelDesired()) {
    const hours = Math.min(168, Math.max(1, Number(state.exitDelayHours) || 24));
    state.exitDelayHours = hours;
    state.peelDesired = true;
    state.unpeelAt = Date.now() + hours * 60 * 60 * 1000;
  } else {
    state.peelDesired = false;
    state.unpeelAt = null;
  }
  saveState();
  res.json({
    peelDesired: effectivePeelDesired(),
    unpeelAt: state.unpeelAt,
    exitDelayHours: state.exitDelayHours,
  });
});

/** Desktop: cancel a pending delayed unpeel */
app.post("/api/peel/cancel", auth, (req, res) => {
  state.unpeelAt = null;
  state.peelDesired = true;
  saveState();
  res.json({ peelDesired: effectivePeelDesired(), unpeelAt: null });
});

/** Desktop: update blocked URLs, allow-list, unpeel delay hours */
app.post("/api/policy", auth, (req, res) => {
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
  saveState();
  res.json({
    blockedUrls: state.blockedUrls,
    allowList: state.allowList,
    exitDelayHours: state.exitDelayHours,
  });
});

app.get("/api/status", (req, res) => {
  res.json({
    peelDesired: effectivePeelDesired(),
    unpeelAt: state.unpeelAt,
    exitDelayHours: state.exitDelayHours,
    blockedUrls: state.blockedUrls,
    allowList: state.allowList,
    lastPhoneReport: state.lastPhoneReport,
    token: TOKEN,
    serverTime: Date.now(),
    phoneUrls: phoneUrls(),
    browserUrl: `http://localhost:${PORT}`,
  });
});

app.use(express.static(path.join(__dirname, "public")));

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

function phoneUrls() {
  return getLanIps().map((ip) => `http://${ip}:${PORT}`);
}

const server = http.createServer(app);
server.listen(PORT, "0.0.0.0", () => {
  const ips = getLanIps();
  const tokenSource = process.env.ONIONPEEL_TOKEN ? "from ONIONPEEL_TOKEN / .env" : "random (set ONIONPEEL_TOKEN for beta)";
  console.log("\n  Onionpeel companion — closed beta\n");
  console.log(`  Browser:  http://localhost:${PORT}`);
  for (const ip of ips) {
    console.log(`  Phone URL: http://${ip}:${PORT}`);
  }
  console.log(`\n  Pairing token (${tokenSource}): ${TOKEN}\n`);
});
