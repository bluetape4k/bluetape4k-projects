import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-kafka-logback-sequence-01.svg";
const W = 1980;
const H = 2040;

const c = {
  ink: "#1F2937",
  muted: "#52616B",
  border: "#D6E2ED",
  life: "#A8B8C8",
  call: "#4F83BF",
  encode: "#2F9E6B",
  key: "#D08A2D",
  export: "#8B5CF6",
  kafka: "#C15A5A",
  result: "#2E9C9B",
  warn: "#B45309",
};

const participants = [
  { id: "logback", x: 85, w: 190, title: "Logback", role: "logging event", fill: "#DBEAFE", stroke: "#3B82F6" },
  { id: "appender", x: 345, w: 250, title: "KafkaAppender", role: "queue + append", fill: "#DCFCE7", stroke: "#22C55E" },
  { id: "encoder", x: 660, w: 180, title: "Encoder", role: "event to bytes", fill: "#FEF3C7", stroke: "#D97706" },
  { id: "key", x: 910, w: 220, title: "KeyProvider", role: "partition key", fill: "#FCE7F3", stroke: "#DB2777" },
  { id: "exporter", x: 1205, w: 235, title: "KafkaExporter", role: "send strategy", fill: "#F3E8FF", stroke: "#8B5CF6" },
  { id: "producer", x: 1505, w: 230, title: "KafkaProducer", role: "async send", fill: "#FEE2E2", stroke: "#DC2626" },
  { id: "fallback", x: 1780, w: 145, title: "Fallback", role: "attached appenders", fill: "#CCFBF1", stroke: "#14B8A6" },
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
  lines.push(`<line x1="${p.x + p.w / 2}" y1="238" x2="${p.x + p.w / 2}" y2="1870" class="lifeline"/>`);
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

lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
lines.push(`<title id="title">kafka logback append sequence</title>`);
lines.push(`<desc id="desc">KafkaAppender drains deferred Kafka client logs, encodes normal events, builds a Kafka record, sends asynchronously through KafkaExporter, and routes failures to fallback appenders.</desc>`);
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
lines.push(`<text x="${W / 2}" y="78" text-anchor="middle" class="title">Kafka Logback Append Flow</text>`);
lines.push(`<text x="${W / 2}" y="110" text-anchor="middle" class="subtitle">Normal events send asynchronously; Kafka client logs are deferred.</text>`);
lines.push(`<rect x="58" y="140" width="${W - 116}" height="1760" rx="18" class="inner"/>`);
participants.forEach(participant);
lines.push(`<rect x="${xs.appender - 8}" y="302" width="16" height="1566" rx="6" fill="#DCFCE7" stroke="#22C55E" class="activation"/>`);
lines.push(`<rect x="${xs.encoder - 8}" y="872" width="16" height="144" rx="6" fill="#FEF3C7" stroke="#D97706" class="activation"/>`);
lines.push(`<rect x="${xs.key - 8}" y="1072" width="16" height="150" rx="6" fill="#FCE7F3" stroke="#DB2777" class="activation"/>`);
lines.push(`<rect x="${xs.exporter - 8}" y="1328" width="16" height="410" rx="6" fill="#F3E8FF" stroke="#8B5CF6" class="activation"/>`);
lines.push(`<rect x="${xs.producer - 8}" y="1445" width="16" height="335" rx="6" fill="#FEE2E2" stroke="#DC2626" class="activation"/>`);
lines.push(`<rect x="${xs.fallback - 8}" y="1802" width="16" height="98" rx="6" fill="#CCFBF1" stroke="#14B8A6" class="activation"/>`);
msg(1, "logback", "appender", 330, "doAppend(event)", c.call, false, 160, 210);
selfMsg(2, "appender", 430, "drainDeferQueue() before current event", c.encode, 445, 365);
lines.push(`<rect x="96" y="486" width="${W - 192}" height="260" rx="15" class="alt"/>`);
lines.push(`<rect x="118" y="512" width="300" height="30" rx="15" fill="#FFF7ED" stroke="${c.warn}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="532" class="note">opt Kafka client logger</text>`);
selfMsg(3, "appender", 575, "deferQueue.offer(event)", c.warn, 455, 250);
msg(4, "appender", "logback", 710, "queued; skip send", c.warn, true, 135, 235);
lines.push(`<rect x="405" y="790" width="310" height="30" rx="15" fill="#F0FDF4" stroke="${c.encode}" stroke-width="1.5"/>`);
lines.push(`<text x="430" y="810" class="note">normal branch enters append(event)</text>`);
msg(5, "appender", "encoder", 920, "encoder.encode(event)", c.encode, false, 535, 260);
msg(6, "encoder", "appender", 1000, "ByteArray value", c.result, true, 500, 210);
msg(7, "appender", "key", 1120, "keyProvider.get(event)", c.key, false, 655, 260);
msg(8, "key", "appender", 1208, "ByteArray key or null", c.result, true, 625, 250);
lines.push(`<rect x="730" y="1270" width="520" height="30" rx="15" fill="#FFF7ED" stroke="${c.key}" stroke-width="1.5"/>`);
lines.push(`<text x="755" y="1290" class="note">ProducerRecord: topic, partition, timestamp, key, value</text>`);
msg(9, "appender", "exporter", 1380, "export(producer, record, event, handler)", c.export, false, 830, 390);
msg(10, "exporter", "producer", 1480, "producer.send(record, callback)", c.kafka, false, 1260, 330);
lines.push(`<rect x="96" y="1548" width="${W - 192}" height="320" rx="15" class="alt"/>`);
lines.push(`<rect x="118" y="1572" width="275" height="30" rx="15" fill="#F0FDF4" stroke="${c.encode}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="1592" class="note">alt send accepted</text>`);
lines.push(`<line x1="96" y1="1698" x2="${W - 96}" y2="1698" stroke="#78909C" stroke-width="2.2" stroke-dasharray="12 8"/>`);
lines.push(`<rect x="118" y="1728" width="370" height="30" rx="15" fill="#FEF2F2" stroke="${c.kafka}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="1748" class="note">else callback or immediate failure</text>`);
msg(11, "exporter", "appender", 1640, "true after send accepted", c.result, true, 945, 270);
msg(12, "producer", "fallback", 1830, "handle -> appendLoopOnAppenders(event)", c.kafka, false, 1450, 360);
lines.push(`<rect x="96" y="1940" width="${W - 192}" height="48" rx="12" fill="#FFFFFF" stroke="${c.border}" stroke-width="1.6"/>`);
lines.push(`<text x="${W / 2}" y="1970" text-anchor="middle" class="footer">stop() drains deferred logs, flushes the producer, closes it, and then stops the appender.</text>`);
lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
