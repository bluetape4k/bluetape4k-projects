import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-lettuce-sequence-01.svg";
const W = 1760;
const H = 1840;

const c = {
  ink: "#1F2937",
  muted: "#52616B",
  border: "#D6E2ED",
  life: "#A8B8C8",
  call: "#4F83BF",
  redis: "#C15A5A",
  loader: "#D08A2D",
  writer: "#8B5CF6",
  result: "#2E9C9B",
  cache: "#2F9E6B",
};

const participants = [
  { id: "client", x: 95, w: 205, title: "Client", role: "map user", fill: "#DBEAFE", stroke: "#3B82F6" },
  { id: "map", x: 385, w: 270, title: "LettuceLoadedMap", role: "read/write-through", fill: "#DCFCE7", stroke: "#22C55E" },
  { id: "redis", x: 740, w: 220, title: "Redis", role: "TTL cache", fill: "#FEE2E2", stroke: "#DC2626" },
  { id: "loader", x: 1045, w: 225, title: "MapLoader", role: "load on miss", fill: "#FEF3C7", stroke: "#D97706" },
  { id: "writer", x: 1360, w: 245, title: "MapWriter", role: "write-through DB", fill: "#F3E8FF", stroke: "#8B5CF6" },
];

const xs = Object.fromEntries(participants.map((p) => [p.id, p.x + p.w / 2]));
const lines = [];
const esc = (s) => s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
const approx = (s) => Math.max(150, s.length * 8.2 + 58);
const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));

function marker(id, color) {
  return `<marker id="${id}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse"><path d="M 0 0 L 10 5 L 0 10 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`;
}

function pill(n, text, color, x, y, w = approx(text)) {
  const px = clamp(x, 58, W - w - 58);
  lines.push(`<rect x="${px}" y="${y - 17}" width="${w}" height="34" rx="17" class="pill" stroke="${color}"/>`);
  lines.push(`<circle cx="${px + 24}" cy="${y}" r="13" fill="${color}"/>`);
  lines.push(`<text x="${px + 24}" y="${y + 4}" text-anchor="middle" class="badge">${n}</text>`);
  lines.push(`<text x="${px + 48}" y="${y + 5}" class="msg" fill="${color}">${esc(text)}</text>`);
}

function participant(p) {
  lines.push(`<rect x="${p.x}" y="160" width="${p.w}" height="78" rx="12" fill="${p.fill}" stroke="${p.stroke}" stroke-width="2.4"/>`);
  lines.push(`<text x="${p.x + p.w / 2}" y="190" text-anchor="middle" class="participant">${esc(p.title)}</text>`);
  lines.push(`<text x="${p.x + p.w / 2}" y="216" text-anchor="middle" class="role">${esc(p.role)}</text>`);
  lines.push(`<line x1="${p.x + p.w / 2}" y1="238" x2="${p.x + p.w / 2}" y2="1710" class="lifeline"/>`);
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

lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
lines.push(`<title id="title">lettuce loaded map read-through and write-through flow</title>`);
lines.push(`<desc id="desc">LettuceLoadedMap reads Redis first, loads missing values through MapLoader, caches them with TTL, and writes through MapWriter before updating Redis.</desc>`);
lines.push(`<defs><style>
  .title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}
  .subtitle,.msg,.role,.note,.footer{font-family:"Comic Mono";fill:${c.muted}}
  .subtitle{font-size:18px}.participant{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}
  .role{font-size:13px}.msg{font-size:14px}.note{font-size:13px}.footer{font-size:14px;fill:#60727d}
  .frame{fill:#FEFEFC;stroke:#546E7A;stroke-width:2.5}.inner{fill:#FFFFFF;stroke:${c.border};stroke-width:2}
  .lifeline{stroke:${c.life};stroke-width:2;stroke-dasharray:8 8}
  .activation{rx:6;stroke-width:1.6}.pill{fill:#FFFFFF;stroke-width:1.5}.badge{font-family:"Comic Mono";font-size:12px;font-weight:700;fill:#FFFFFF}
  .alt{fill:#FFFFFF;fill-opacity:.58;stroke:#78909C;stroke-width:2.6;stroke-dasharray:12 8}
</style></defs>`);
lines.push(`<rect x="24" y="24" width="${W - 48}" height="${H - 48}" rx="22" class="frame"/>`);
lines.push(`<text x="${W / 2}" y="78" text-anchor="middle" class="title">LettuceLoadedMap Read-Through / Write-Through Flow</text>`);
lines.push(`<text x="${W / 2}" y="110" text-anchor="middle" class="subtitle">Misses load through MapLoader; WRITE_THROUGH persists through MapWriter before Redis SET EX.</text>`);
lines.push(`<rect x="58" y="140" width="${W - 116}" height="1638" rx="18" class="inner"/>`);
participants.forEach(participant);

lines.push(`<rect x="${xs.map - 8}" y="302" width="16" height="1436" rx="6" fill="#DCFCE7" stroke="#22C55E" class="activation"/>`);
lines.push(`<rect x="${xs.redis - 8}" y="372" width="16" height="1314" rx="6" fill="#FEE2E2" stroke="#DC2626" class="activation"/>`);
lines.push(`<rect x="${xs.loader - 8}" y="615" width="16" height="118" rx="6" fill="#FEF3C7" stroke="#D97706" class="activation"/>`);
lines.push(`<rect x="${xs.writer - 8}" y="1445" width="16" height="105" rx="6" fill="#F3E8FF" stroke="#8B5CF6" class="activation"/>`);

lines.push(`<rect x="90" y="280" width="${W - 180}" height="590" rx="15" class="alt"/>`);
lines.push(`<rect x="116" y="306" width="260" height="30" rx="15" fill="#F0FDF4" stroke="${c.cache}" stroke-width="1.6"/>`);
lines.push(`<text x="138" y="326" class="note">read-through miss</text>`);
msg(1, "client", "map", 385, "get(key)", c.call, false, 260, 170);
msg(2, "map", "redis", 470, "GET prefix:key", c.redis, false, 590, 215);
msg(3, "redis", "map", 555, "null", c.result, true, 608, 150);
msg(4, "map", "loader", 640, "load(key)", c.loader, false, 785, 180);
msg(5, "loader", "map", 725, "value", c.result, true, 805, 150);
msg(6, "map", "redis", 790, "SET prefix:key value EX ttl", c.cache, false, 520, 310);
msg(7, "map", "client", 850, "value", c.result, true, 205, 150);

lines.push(`<rect x="90" y="930" width="${W - 180}" height="355" rx="15" class="alt"/>`);
lines.push(`<rect x="116" y="956" width="245" height="30" rx="15" fill="#ECFEFF" stroke="${c.result}" stroke-width="1.6"/>`);
lines.push(`<text x="138" y="976" class="note">read-through hit</text>`);
msg(8, "client", "map", 1035, "get(key)", c.call, false, 260, 170);
msg(9, "map", "redis", 1120, "GET prefix:key", c.redis, false, 590, 215);
msg(10, "redis", "map", 1205, "value", c.result, true, 598, 150);
msg(11, "map", "client", 1270, "value", c.result, true, 205, 150);

lines.push(`<rect x="90" y="1360" width="${W - 180}" height="400" rx="15" class="alt"/>`);
lines.push(`<rect x="116" y="1386" width="305" height="30" rx="15" fill="#F3E8FF" stroke="${c.writer}" stroke-width="1.6"/>`);
lines.push(`<text x="138" y="1406" class="note">write-through set</text>`);
msg(12, "client", "map", 1460, "set(key, value)", c.call, false, 430, 220);
msg(13, "map", "writer", 1530, "write(mapOf(key to value))", c.writer, false, 900, 330);
msg(14, "writer", "map", 1600, "ok", c.result, true, 895, 150);
msg(15, "map", "redis", 1665, "SET prefix:key value EX ttl", c.cache, false, 520, 310);
msg(16, "map", "client", 1725, "ok", c.result, true, 205, 140);
lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
