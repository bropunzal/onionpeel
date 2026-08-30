/**
 * Onionpeel desktop companion — peel mode is toggled ONLY here (Shift-style).
 * Phone polls /api/sync on the same Wi-Fi LAN.
 */
const http = require("http");
const crypto = require("crypto");
const express = require("express");
const path = require("path");

const PORT = process.env.PORT || 8787;
const TOKEN = process.env.ONIONPEEL_TOKEN || crypto.randomBytes(16).toString("hex");

let peelDesired = false;
let lastPhoneReport = null;

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
  res.json({
    peelDesired,
    serverTime: Date.now(),
  });
});

/** Phone reports actual enforcement state */
app.post("/api/phone/report", auth, (req, res) => {
  lastPhoneReport = {
    peelActive: Boolean(req.body.peelActive),
    at: Date.now(),
  };
  res.json({ ok: true });
});

/** Desktop browser toggles peel */
app.post("/api/peel", auth, (req, res) => {
  peelDesired = Boolean(req.body.enabled);
  res.json({ peelDesired });
});

app.get("/api/status", (req, res) => {
  res.json({
    peelDesired,
    lastPhoneReport,
    token: TOKEN,
  });
});

app.use(express.static(path.join(__dirname, "public")));

const server = http.createServer(app);
server.listen(PORT, "0.0.0.0", () => {
  const nets = require("os").networkInterfaces();
  const ips = [];
  for (const name of Object.keys(nets)) {
    for (const net of nets[name] || []) {
      if (net.family === "IPv4" && !net.internal) ips.push(net.address);
    }
  }
  console.log("\n  Onionpeel companion running\n");
  console.log(`  Browser:  http://localhost:${PORT}`);
  for (const ip of ips) {
    console.log(`  Phone URL: http://${ip}:${PORT}`);
  }
  console.log(`\n  Pairing token: ${TOKEN}\n`);
});
