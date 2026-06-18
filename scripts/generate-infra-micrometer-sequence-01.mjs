import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-micrometer-sequence-01.svg";
const W = 1760;
const H = 1480;

const c = {
  ink: "#1F2937",
  muted: "#52616B",
  border: "#D6E2ED",
  life: "#A8B8C8",
  call: "#4F83BF",
  factory: "#2F9E6B",
  adapter: "#D08A2D",
  recorder: "#8B5CF6",
  registry: "#2E9C9B",
  error: "#C15A5A",
};

const participants = [
  { id: "client", x: 90, w: 190, title: "Client", role: "service caller", fill: "#DBEAFE", stroke: "#3B82F6" },
  { id: "retrofit", x: 360, w: 210, title: "Retrofit", role: "CallAdapter lookup", fill: "#DCFCE7", stroke: "#22C55E" },
  { id: "factory", x: 640, w: 270, title: "MetricsFactory", role: "creates collector", fill: "#FEF3C7", stroke: "#D97706" },
  { id: "call", x: 980, w: 230, title: "MeasuredCall", role: "execute / enqueue", fill: "#F3E8FF", stroke: "#8B5CF6" },
  { id: "api", x: 1290, w: 190, title: "HTTP API", role: "remote service", fill: "#FEE2E2", stroke: "#DC2626" },
  { id: "registry", x: 1550, w: 165, title: "MeterRegistry", role: "Timer cache", fill: "#CCFBF1", stroke: "#14B8A6" },
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
  lines.push(`<line x1="${p.x + p.w / 2}" y1="238" x2="${p.x + p.w / 2}" y2="1330" class="lifeline"/>`);
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
  lines.push(`<path d="M ${x} ${y} L ${x + 88} ${y} L ${x + 88} ${y + 38} L ${x + 8} ${y + 38}" fill="none" stroke="${color}" stroke-width="3.0" marker-end="url(#${markerId})"/>`);
  const tw = w ?? approx(text);
  pill(n, text, color, labelX ?? x + 42, y - 30, tw);
}

function selfMsgLeft(n, id, y, text, color, labelX = null, w = null) {
  const x = xs[id] - 14;
  const markerId = `arrowSelfLeft${n}`;
  lines.push(marker(markerId, color));
  lines.push(`<path d="M ${x} ${y} L ${x - 88} ${y} L ${x - 88} ${y + 38} L ${x - 8} ${y + 38}" fill="none" stroke="${color}" stroke-width="3.0" marker-end="url(#${markerId})"/>`);
  const tw = w ?? approx(text);
  pill(n, text, color, labelX ?? x - tw - 42, y - 30, tw);
}

lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
lines.push(`<title id="title">retrofit2 metric collection sequence</title>`);
lines.push(`<desc id="desc">RetrofitMetricsFactory wraps Retrofit calls in MeasuredCall; MeasuredCall records success or exception tags into Micrometer timers.</desc>`);
lines.push(`<defs><style>
  .title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}
  .subtitle,.msg,.role,.note{font-family:"Comic Mono";fill:${c.muted}}
  .subtitle{font-size:18px}.participant{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}
  .role{font-size:13px}.msg{font-size:14px}.note{font-size:13px}
  .frame{fill:#FEFEFC;stroke:#546E7A;stroke-width:2.5}.inner{fill:#FFFFFF;stroke:${c.border};stroke-width:2}
  .lifeline{stroke:${c.life};stroke-width:2;stroke-dasharray:8 8}
  .activation{rx:6;stroke-width:1.6}.pill{fill:#FFFFFF;stroke-width:1.5}.badge{font-family:"Comic Mono";font-size:12px;font-weight:700;fill:#FFFFFF}
  .alt{fill:#FFFFFF;fill-opacity:.12;stroke:#78909C;stroke-width:2.6;stroke-dasharray:12 8}
</style></defs>`);
lines.push(`<rect x="24" y="24" width="${W - 48}" height="${H - 48}" rx="22" class="frame"/>`);
lines.push(`<text x="${W / 2}" y="78" text-anchor="middle" class="title">Retrofit2 Metric Collection Sequence</text>`);
lines.push(`<text x="${W / 2}" y="110" text-anchor="middle" class="subtitle">MeasuredCall records duration, status/outcome tags, and exception tags into retrofit2.requests timers.</text>`);
lines.push(`<rect x="58" y="140" width="${W - 116}" height="1240" rx="18" class="inner"/>`);
participants.forEach(participant);
lines.push(`<rect x="${xs.retrofit - 8}" y="310" width="16" height="210" rx="6" fill="#DCFCE7" stroke="#22C55E" class="activation"/>`);
lines.push(`<rect x="${xs.factory - 8}" y="385" width="16" height="220" rx="6" fill="#FEF3C7" stroke="#D97706" class="activation"/>`);
lines.push(`<rect x="${xs.call - 8}" y="565" width="16" height="585" rx="6" fill="#F3E8FF" stroke="#8B5CF6" class="activation"/>`);
lines.push(`<rect x="${xs.api - 8}" y="720" width="16" height="126" rx="6" fill="#FEE2E2" stroke="#DC2626" class="activation"/>`);
lines.push(`<rect x="${xs.registry - 8}" y="1010" width="16" height="126" rx="6" fill="#CCFBF1" stroke="#14B8A6" class="activation"/>`);
msg(1, "client", "retrofit", 330, "service method returns Call<T>", c.call, false, 225, 310);
msg(2, "retrofit", "factory", 410, "get(returnType, annotations)", c.factory, false, 458, 300);
selfMsg(3, "factory", 510, "create collector(baseUrl, uri)", c.factory, 822, 300);
msg(4, "factory", "retrofit", 600, "MeasuredCallAdapter", c.factory, true, 468, 245);
msg(5, "client", "call", 690, "execute() or enqueue(callback)", c.recorder, false, 360, 340);
msg(6, "call", "api", 760, "HTTP request", c.call, false, 1110, 190);
msg(7, "api", "call", 850, "response or exception", c.registry, true, 1080, 250);
lines.push(`<rect x="116" y="930" width="${W - 232}" height="250" rx="15" class="alt"/>`);
lines.push(`<rect x="140" y="954" width="310" height="30" rx="15" fill="#F0FDF4" stroke="${c.registry}" stroke-width="1.6"/>`);
lines.push(`<text x="164" y="974" class="note">success or failure measurement</text>`);
msg(8, "call", "registry", 1030, "recordTiming(tags, duration)", c.recorder, false, 1138, 310);
selfMsgLeft(9, "registry", 1120, "timerFor(tags).record(duration)", c.registry, 1365, 300);
msg(10, "call", "client", 1220, "return response or rethrow", c.registry, true, 610, 300);
lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
