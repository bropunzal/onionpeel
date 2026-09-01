const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000;

function loadInviteCodes() {
  const raw = process.env.BETA_INVITE_CODES || "";
  return new Set(
    raw
      .split(",")
      .map((code) => code.trim())
      .filter(Boolean)
  );
}

function betaLoginRequired() {
  if (process.env.REQUIRE_BETA_LOGIN === "true") return true;
  if (process.env.REQUIRE_BETA_LOGIN === "false") return false;
  return loadInviteCodes().size > 0;
}

function createAuthStore(dataDir) {
  const authFile = path.join(dataDir, "auth.json");

  function load() {
    try {
      if (fs.existsSync(authFile)) {
        return JSON.parse(fs.readFileSync(authFile, "utf8"));
      }
    } catch (e) {
      console.warn("Could not load auth.json:", e.message);
    }
    return { users: {}, sessions: {} };
  }

  let data = load();

  function save() {
    fs.mkdirSync(dataDir, { recursive: true });
    fs.writeFileSync(authFile, JSON.stringify(data, null, 2));
  }

  function normalizeEmail(email) {
    return String(email || "").trim().toLowerCase();
  }

  function hashPassword(password, salt = crypto.randomBytes(16).toString("hex")) {
    const hash = crypto.scryptSync(password, salt, 64).toString("hex");
    return { salt, hash };
  }

  function verifyPassword(password, salt, hash) {
    const candidate = crypto.scryptSync(password, salt, 64).toString("hex");
    return crypto.timingSafeEqual(Buffer.from(candidate, "hex"), Buffer.from(hash, "hex"));
  }

  function findUserByEmail(email) {
    const normalized = normalizeEmail(email);
    return Object.entries(data.users).find(([, user]) => user.email === normalized) || null;
  }

  function register({ email, password, inviteCode }) {
    const normalized = normalizeEmail(email);
    if (!normalized || !normalized.includes("@")) {
      return { error: "invalid_email" };
    }
    if (!password || password.length < 8) {
      return { error: "weak_password", message: "Password must be at least 8 characters." };
    }
    if (betaLoginRequired()) {
      const codes = loadInviteCodes();
      if (!codes.has(String(inviteCode || "").trim())) {
        return { error: "invalid_invite", message: "Invalid beta invite code." };
      }
    }
    if (findUserByEmail(normalized)) {
      return { error: "email_taken" };
    }

    const userId = crypto.randomBytes(16).toString("hex");
    const { salt, hash } = hashPassword(password);
    data.users[userId] = {
      email: normalized,
      salt,
      passwordHash: hash,
      createdAt: Date.now(),
    };
    save();
    return { userId, email: normalized };
  }

  function login({ email, password }) {
    const match = findUserByEmail(email);
    if (!match) return { error: "invalid_credentials" };
    const [userId, user] = match;
    if (!verifyPassword(password, user.salt, user.passwordHash)) {
      return { error: "invalid_credentials" };
    }
    return { userId, email: user.email };
  }

  function createSession(userId) {
    const token = crypto.randomBytes(32).toString("hex");
    data.sessions[token] = {
      userId,
      expiresAt: Date.now() + SESSION_TTL_MS,
    };
    save();
    return token;
  }

  function getSession(token) {
    if (!token) return null;
    const session = data.sessions[token];
    if (!session) return null;
    if (Date.now() >= session.expiresAt) {
      delete data.sessions[token];
      save();
      return null;
    }
    const user = data.users[session.userId];
    if (!user) return null;
    return { userId: session.userId, email: user.email };
  }

  function deleteSession(token) {
    if (token && data.sessions[token]) {
      delete data.sessions[token];
      save();
    }
  }

  function getUserEmail(userId) {
    return data.users[userId]?.email || null;
  }

  return {
    betaLoginRequired,
    register,
    login,
    createSession,
    getSession,
    deleteSession,
    getUserEmail,
    reload: () => {
      data = load();
    },
  };
}

module.exports = { createAuthStore, betaLoginRequired, loadInviteCodes };
