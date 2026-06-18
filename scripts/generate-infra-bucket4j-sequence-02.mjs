import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-bucket4j-sequence-02.svg";
const W = 1960;
const H = 1940;

const c = {
  bg: "#F8FAFC",
  ink: "#1F2937",
  muted: "#52616B",
  border: "#D6E2ED",
  life: "#A8B8C8",
  call: "#4F83BF",
  validate: "#2F9E6B",
  async: "#8B5CF6",
  store: "#D08A2D",
  result: "#2E9C9B",
  error: "#C15A5A",
};

const participants = [
  { id: "caller", x: 95, w: 215, title: "Coroutine Caller", role: "suspend consume", fill: "#DBEAFE", stroke: "#3B82F6" },
  { id: "limiter", x: 390, w: 300, title: "DistributedSuspend", role: "RateLimiter: timeout + await", fill: "#DCFCE7", stroke: "#22C55E" },
  { id: "provider", x: 770, w: 290, title: "AsyncBucketProxyProvider", role: "prefixed key resolver", fill: "#FEF3C7", stroke: "#D97706" },
  { id: "proxy", x: 1140, w: 235, title: "AsyncBucketProxy", role: "Bucket4j async API", fill: "#F3E8FF", stroke: "#8B5CF6" },
  { id: "store", x: 1455, w: 225, title: "Redis Store", role: "remote bucket state", fill: "#FEE2E2", stroke: "#DC2626" },
  { id: "result", x: 1705, w: 170, title: "Result", role: "public DTO", fill: "#CCFBF1", stroke: "#14B8A6" },
];

const xs = Object.fromEntries(participants.map((p) => [p.id, p.x + p.w / 2]));
const lines = [];
const esc = (s) => s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
const approx = (s) => Math.max(150, s.length * 8 + 58);
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
  lines.push(`<rect x="${p.x}" y="160" width="${p.w}" height="78" rx="12" fill="${p.fill}" stroke="${p.stroke}" stroke-width="2.4"/>`);
  lines.push(`<text x="${p.x + p.w / 2}" y="190" text-anchor="middle" class="participant">${esc(p.title)}</text>`);
  lines.push(`<text x="${p.x + p.w / 2}" y="216" text-anchor="middle" class="role">${esc(p.role)}</text>`);
  lines.push(`<line x1="${p.x + p.w / 2}" y1="238" x2="${p.x + p.w / 2}" y2="1800" class="lifeline"/>`);
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
lines.push(`<title id="title">distributed suspend rate limiter redis coroutine sequence</title>`);
lines.push(`<desc id="desc">DistributedSuspendRateLimiter resolves an AsyncBucketProxy, awaits a remote ConsumptionProbe, maps it to RateLimitResult, converts timeout to error, and rethrows coroutine cancellation.</desc>`);
lines.push(`<defs><style>
  .title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}
  .subtitle,.msg,.role,.note,.footer{font-family:"Comic Mono";fill:${c.muted}}
  .subtitle{font-size:18px}.participant{font-family:"Architects Daughter";font-size:21px;fill:${c.ink}}
  .role{font-size:13px}.msg{font-size:14px}.note{font-size:13px}.footer{font-size:14px;fill:#60727d}
  .frame{fill:#FEFEFC;stroke:#546E7A;stroke-width:2.5}.inner{fill:#FFFFFF;stroke:${c.border};stroke-width:2}
  .lifeline{stroke:${c.life};stroke-width:2;stroke-dasharray:8 8}
  .activation{rx:6;stroke-width:1.6}.pill{fill:#FFFFFF;stroke-width:1.5}.badge{font-family:"Comic Mono";font-size:12px;font-weight:700;fill:#FFFFFF}
  .alt{fill:#FFFFFF;fill-opacity:.58;stroke:#78909C;stroke-width:2.6;stroke-dasharray:12 8}
</style></defs>`);
lines.push(`<rect x="24" y="24" width="${W - 48}" height="${H - 48}" rx="22" class="frame"/>`);
lines.push(`<text x="${W / 2}" y="78" text-anchor="middle" class="title">Distributed Suspend Rate Limiter Redis Coroutine Flow</text>`);
lines.push(`<text x="${W / 2}" y="110" text-anchor="middle" class="subtitle">Await one async probe; timeout is ERROR, cancellation is rethrown.</text>`);
lines.push(`<rect x="58" y="140" width="${W - 116}" height="1688" rx="18" class="inner"/>`);
participants.forEach(participant);
lines.push(`<rect x="${xs.limiter - 8}" y="300" width="16" height="1488" rx="6" fill="#DCFCE7" stroke="#22C55E" class="activation"/>`);
lines.push(`<rect x="${xs.provider - 8}" y="550" width="16" height="250" rx="6" fill="#FEF3C7" stroke="#D97706" class="activation"/>`);
lines.push(`<rect x="${xs.proxy - 8}" y="892" width="16" height="332" rx="6" fill="#F3E8FF" stroke="#8B5CF6" class="activation"/>`);
lines.push(`<rect x="${xs.store - 8}" y="1030" width="16" height="138" rx="6" fill="#FEE2E2" stroke="#DC2626" class="activation"/>`);
lines.push(`<rect x="${xs.result - 8}" y="1344" width="16" height="444" rx="6" fill="#CCFBF1" stroke="#14B8A6" class="activation"/>`);
msg(1, "caller", "limiter", 328, "consume(key, tokens, timeout?)", c.call, false, 175, 320);
selfMsg(2, "limiter", 430, "validate key and token range", c.validate, 570, 285);
msg(3, "limiter", "provider", 560, "resolveBucket(key)", c.store, false, 662);
selfMsg(4, "provider", 690, "prefix + UTF-8 + key-size guard", c.store, 960, 340);
msg(5, "provider", "proxy", 820, "build async proxy", c.async, false, 948, 220);
msg(6, "provider", "limiter", 890, "AsyncBucketProxy", c.result, true, 650, 215);
msg(7, "limiter", "proxy", 980, "tryConsumeAndReturnRemaining(tokens)", c.async, false, 755, 360);
msg(8, "proxy", "store", 1088, "remote token probe", c.store, false, 1252, 235);
msg(9, "store", "proxy", 1178, "CompletionStage<Probe>", c.result, true, 1260, 270);
msg(10, "proxy", "limiter", 1268, "await() yields ConsumptionProbe", c.result, true, 820, 320);
lines.push(`<rect x="96" y="1340" width="${W - 192}" height="448" rx="15" class="alt"/>`);
lines.push(`<rect x="118" y="1364" width="302" height="30" rx="15" fill="#F0FDF4" stroke="${c.validate}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="1384" class="note">alt consumed or rejected</text>`);
lines.push(`<line x1="96" y1="1585" x2="${W - 96}" y2="1585" stroke="#78909C" stroke-width="2.2" stroke-dasharray="12 8"/>`);
lines.push(`<rect x="118" y="1614" width="255" height="30" rx="15" fill="#FEF2F2" stroke="${c.error}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="1634" class="note">else timeout</text>`);
lines.push(`<line x1="96" y1="1705" x2="${W - 96}" y2="1705" stroke="#78909C" stroke-width="2.2" stroke-dasharray="12 8"/>`);
lines.push(`<rect x="118" y="1734" width="290" height="30" rx="15" fill="#FEF2F2" stroke="${c.error}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="1754" class="note">else coroutine cancelled</text>`);
msg(11, "limiter", "result", 1460, "toRateLimitResult(probe)", c.validate, false, 1370, 285);
msg(12, "result", "caller", 1540, "CONSUMED or REJECTED", c.result, true, 505, 260);
msg(13, "limiter", "result", 1660, "RateLimitResult.error(timeout)", c.error, false, 1350, 320);
msg(14, "limiter", "caller", 1770, "CancellationException rethrows", c.error, true, 470, 330);
lines.push(`<rect x="96" y="1858" width="${W - 192}" height="48" rx="12" fill="#FFFFFF" stroke="${c.border}" stroke-width="1.6"/>`);
lines.push(`<text x="${W / 2}" y="1888" text-anchor="middle" class="footer">The timeout bounds the remote async operation; it is not a wait-for-refill policy.</text>`);
lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
