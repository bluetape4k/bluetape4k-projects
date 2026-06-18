import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-bucket4j-sequence-01.svg";
const W = 1680;
const H = 1940;

const c = {
  bg: "#F8FAFC",
  ink: "#1F2937",
  muted: "#52616B",
  border: "#D6E2ED",
  life: "#A8B8C8",
  call: "#4F83BF",
  validate: "#2F9E6B",
  bucket: "#D08A2D",
  result: "#2E9C9B",
  error: "#C15A5A",
};

const participants = [
  { id: "caller", x: 130, w: 190, title: "Caller", role: "filter / service", fill: "#DBEAFE", stroke: "#3B82F6" },
  { id: "limiter", x: 435, w: 240, title: "LocalRateLimiter", role: "consume(key,tokens)", fill: "#DCFCE7", stroke: "#22C55E" },
  { id: "provider", x: 760, w: 250, title: "LocalBucketProvider", role: "keyed in-memory bucket", fill: "#FEF3C7", stroke: "#D97706" },
  { id: "bucket", x: 1100, w: 205, title: "LocalBucket", role: "try consume probe", fill: "#FCE7F3", stroke: "#DB2777" },
  { id: "result", x: 1400, w: 215, title: "RateLimitResult", role: "stable public result", fill: "#CCFBF1", stroke: "#14B8A6" },
];

const xs = Object.fromEntries(participants.map((p) => [p.id, p.x + p.w / 2]));
const lines = [];
const esc = (s) => s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
const approx = (s) => Math.max(150, s.length * 8.2 + 56);
const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));

function marker(id, color) {
  return `<marker id="${id}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse"><path d="M 0 0 L 10 5 L 0 10 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`;
}

function pill(n, text, color, x, y, w = approx(text)) {
  const px = clamp(x, 56, W - w - 56);
  lines.push(`<rect x="${px}" y="${y - 17}" width="${w}" height="34" rx="17" class="pill" stroke="${color}"/>`);
  lines.push(`<circle cx="${px + 24}" cy="${y}" r="13" fill="${color}"/>`);
  lines.push(`<text x="${px + 24}" y="${y + 4}" text-anchor="middle" class="badge">${n}</text>`);
  lines.push(`<text x="${px + 48}" y="${y + 5}" class="msg" fill="${color}">${esc(text)}</text>`);
}

function participant(p) {
  lines.push(`<rect x="${p.x}" y="160" width="${p.w}" height="76" rx="12" fill="${p.fill}" stroke="${p.stroke}" stroke-width="2.4"/>`);
  lines.push(`<text x="${p.x + p.w / 2}" y="190" text-anchor="middle" class="participant">${esc(p.title)}</text>`);
  lines.push(`<text x="${p.x + p.w / 2}" y="214" text-anchor="middle" class="role">${esc(p.role)}</text>`);
  lines.push(`<line x1="${p.x + p.w / 2}" y1="236" x2="${p.x + p.w / 2}" y2="1800" class="lifeline"/>`);
}

function msg(n, from, to, y, text, color, dashed = false, labelX = null, w = null) {
  const a = xs[from];
  const b = xs[to];
  const start = a < b ? a + 11 : a - 11;
  const end = a < b ? b - 11 : b + 11;
  const markerId = `arrow${n}`;
  lines.push(marker(markerId, color));
  const dash = dashed ? ` stroke-dasharray="10 8"` : "";
  lines.push(`<path d="M ${start} ${y} L ${end} ${y}" fill="none" stroke="${color}" stroke-width="${dashed ? 2.8 : 3.2}"${dash} marker-end="url(#${markerId})"/>`);
  const tw = w ?? approx(text);
  const lx = labelX ?? (Math.min(start, end) + Math.abs(end - start) / 2 - tw / 2);
  pill(n, text, color, lx, y - 34, tw);
}

function selfMsg(n, id, y, text, color, labelX = null, w = null) {
  const x = xs[id] + 14;
  const markerId = `arrowSelf${n}`;
  lines.push(marker(markerId, color));
  lines.push(`<path d="M ${x} ${y} L ${x + 86} ${y} L ${x + 86} ${y + 38} L ${x + 8} ${y + 38}" fill="none" stroke="${color}" stroke-width="3.0" marker-end="url(#${markerId})"/>`);
  const tw = w ?? approx(text);
  pill(n, text, color, labelX ?? x + 42, y - 30, tw);
}

lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
lines.push(`<title id="title">local rate limiter token consumption sequence</title>`);
lines.push(`<desc id="desc">LocalRateLimiter validates input, resolves a per-key LocalBucket, consumes by ConsumptionProbe, and maps the probe to a stable RateLimitResult.</desc>`);
lines.push(`<defs><style>
  .title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}
  .subtitle,.msg,.role,.note,.footer{font-family:"Comic Mono";fill:${c.muted}}
  .subtitle{font-size:18px}.participant{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}
  .role{font-size:13px}.msg{font-size:14px}.note{font-size:13px}.footer{font-size:14px;fill:#60727d}
  .frame{fill:#FEFEFC;stroke:#546E7A;stroke-width:2.5}.inner{fill:#FFFFFF;stroke:${c.border};stroke-width:2}
  .lifeline{stroke:${c.life};stroke-width:2;stroke-dasharray:8 8}
  .activation{rx:6;stroke-width:1.6}.pill{fill:#FFFFFF;stroke-width:1.5}.badge{font-family:"Comic Mono";font-size:12px;font-weight:700;fill:#FFFFFF}
  .alt{fill:#FFFFFF;fill-opacity:.12;stroke:#78909C;stroke-width:2.6;stroke-dasharray:12 8}
</style></defs>`);
lines.push(`<rect x="24" y="24" width="${W - 48}" height="${H - 48}" rx="22" class="frame"/>`);
lines.push(`<text x="${W / 2}" y="78" text-anchor="middle" class="title">Local Rate Limiter Token Consumption Flow</text>`);
lines.push(`<text x="${W / 2}" y="110" text-anchor="middle" class="subtitle">One Bucket4j probe maps each attempt to consumed, rejected, or error.</text>`);
lines.push(`<rect x="58" y="140" width="${W - 116}" height="1700" rx="18" class="inner"/>`);
participants.forEach(participant);
lines.push(`<rect x="${xs.limiter - 8}" y="302" width="16" height="1496" rx="6" fill="#DCFCE7" stroke="#22C55E" class="activation"/>`);
lines.push(`<rect x="${xs.provider - 8}" y="555" width="16" height="230" rx="6" fill="#FEF3C7" stroke="#D97706" class="activation"/>`);
lines.push(`<rect x="${xs.bucket - 8}" y="935" width="16" height="105" rx="6" fill="#FCE7F3" stroke="#DB2777" class="activation"/>`);
lines.push(`<rect x="${xs.result - 8}" y="1180" width="16" height="620" rx="6" fill="#CCFBF1" stroke="#14B8A6" class="activation"/>`);
msg(1, "caller", "limiter", 330, "consume(key, tokens)", c.call, false, 180);
selfMsg(2, "limiter", 430, "validate key and token range", c.validate, 610, 285);
msg(3, "limiter", "provider", 560, "resolveBucket(key)", c.bucket, false, 585);
selfMsg(4, "provider", 690, "reuse cached LocalBucket per key", c.bucket, 995, 320);
msg(5, "provider", "limiter", 820, "LocalBucket", c.result, true, 615, 180);
msg(6, "limiter", "bucket", 940, "tryConsumeAndReturnRemaining(tokens)", c.bucket, false, 645, 360);
msg(7, "bucket", "limiter", 1040, "ConsumptionProbe", c.result, true, 770, 210);
lines.push(`<rect x="96" y="1140" width="${W - 192}" height="660" rx="15" class="alt"/>`);
lines.push(`<rect x="118" y="1164" width="270" height="30" rx="15" fill="#F0FDF4" stroke="${c.validate}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="1184" class="note">alt probe is consumed</text>`);
lines.push(`<line x1="96" y1="1390" x2="${W - 96}" y2="1390" stroke="#78909C" stroke-width="2.2" stroke-dasharray="12 8"/>`);
lines.push(`<rect x="118" y="1424" width="300" height="30" rx="15" fill="#FFF7ED" stroke="${c.bucket}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="1444" class="note">else insufficient tokens</text>`);
lines.push(`<line x1="96" y1="1595" x2="${W - 96}" y2="1595" stroke="#78909C" stroke-width="2.2" stroke-dasharray="12 8"/>`);
lines.push(`<rect x="118" y="1630" width="295" height="30" rx="15" fill="#FEF2F2" stroke="${c.error}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="1650" class="note">else provider or bucket error</text>`);
msg(8, "limiter", "result", 1260, "consumed(tokens, remaining)", c.validate, false, 1050, 310);
msg(9, "result", "caller", 1340, "CONSUMED", c.result, true, 505, 160);
msg(10, "limiter", "result", 1465, "rejected(remaining, retryAfter)", c.bucket, false, 1050, 325);
msg(11, "result", "caller", 1545, "REJECTED", c.result, true, 505, 160);
msg(12, "limiter", "result", 1670, "error(sanitized message)", c.error, false, 1075, 280);
msg(13, "result", "caller", 1750, "ERROR", c.error, true, 510, 140);
lines.push(`<rect x="96" y="1858" width="${W - 192}" height="48" rx="12" fill="#FFFFFF" stroke="${c.border}" stroke-width="1.6"/>`);
lines.push(`<text x="${W / 2}" y="1888" text-anchor="middle" class="footer">Invalid requests fail before bucket lookup; cancellation is rethrown.</text>`);
lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
