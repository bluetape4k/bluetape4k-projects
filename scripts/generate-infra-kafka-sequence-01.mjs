#!/usr/bin/env node
import { readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-kafka-sequence-01.svg";
const kafkaIcon =
  "data:image/png;base64," +
  readFileSync("/Users/debop/work/bluetape4k/bluetape4k-wiki/docs/icons/kafka/apache-kafka-logo.png").toString("base64");

const W = 1720;
const H = 1780;

const c = {
  ink: "#1F2937",
  muted: "#52616B",
  border: "#D6E2ED",
  life: "#A8B8C8",
  app: "#4F83BF",
  bridge: "#2F9E6B",
  producer: "#2563EB",
  kafka: "#D08A2D",
  consumer: "#8B5CF6",
  result: "#2E9C9B",
  warn: "#C15A5A",
};

const participants = [
  { id: "app", x: 90, w: 210, title: "Coroutine App", role: "records + flows", fill: "#DBEAFE", stroke: "#3B82F6" },
  { id: "bridge", x: 395, w: 250, title: "Producer Extensions", role: "suspendSend / sendAsFlow", fill: "#DCFCE7", stroke: "#22C55E" },
  { id: "producer", x: 735, w: 220, title: "KafkaProducer", role: "send(record, callback)", fill: "#DBEAFE", stroke: "#2563EB" },
  { id: "topic", x: 1060, w: 240, title: "Kafka Topic", role: "broker append / partitions", fill: "#FEF3C7", stroke: "#D97706", icon: kafkaIcon },
  { id: "consumer", x: 1400, w: 230, title: "Consumer Side", role: "poll / receive / commit", fill: "#F3E8FF", stroke: "#8B5CF6" },
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
  lines.push(`<rect x="${p.x}" y="160" width="${p.w}" height="84" rx="12" fill="${p.fill}" stroke="${p.stroke}" stroke-width="2.4"/>`);
  if (p.icon) {
    lines.push(`<image href="${p.icon}" x="${p.x + 20}" y="182" width="48" height="30" preserveAspectRatio="xMidYMid meet"/>`);
    lines.push(`<text x="${p.x + p.w / 2 + 28}" y="190" text-anchor="middle" class="participant">${esc(p.title)}</text>`);
    lines.push(`<text x="${p.x + p.w / 2 + 28}" y="216" text-anchor="middle" class="role">${esc(p.role)}</text>`);
  } else {
    lines.push(`<text x="${p.x + p.w / 2}" y="192" text-anchor="middle" class="participant">${esc(p.title)}</text>`);
    lines.push(`<text x="${p.x + p.w / 2}" y="218" text-anchor="middle" class="role">${esc(p.role)}</text>`);
  }
  lines.push(`<line x1="${p.x + p.w / 2}" y1="244" x2="${p.x + p.w / 2}" y2="1610" class="lifeline"/>`);
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
  lines.push(`<path d="M ${x} ${y} L ${x + 92} ${y} L ${x + 92} ${y + 40} L ${x + 8} ${y + 40}" fill="none" stroke="${color}" stroke-width="3.0" marker-end="url(#${markerId})"/>`);
  const tw = w ?? approx(text);
  pill(n, text, color, labelX ?? x + 42, y - 30, tw);
}

lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
lines.push(`<title id="title">producer consumer message flow</title>`);
lines.push(`<desc id="desc">Producer coroutine extensions send Kafka records through callback-based KafkaProducer APIs, resume with RecordMetadata, and consumers receive records from the broker topic stream.</desc>`);
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
lines.push(`<text x="${W / 2}" y="78" text-anchor="middle" class="title">Producer / Consumer Message Flow</text>`);
lines.push(`<text x="${W / 2}" y="110" text-anchor="middle" class="subtitle">suspendSend bridges callback-style Kafka Producer sends to coroutine results.</text>`);
lines.push(`<rect x="58" y="140" width="${W - 116}" height="1500" rx="18" class="inner"/>`);
participants.forEach(participant);

lines.push(`<rect x="${xs.app - 8}" y="305" width="16" height="1118" rx="6" fill="#DBEAFE" stroke="#3B82F6" class="activation"/>`);
lines.push(`<rect x="${xs.bridge - 8}" y="395" width="16" height="390" rx="6" fill="#DCFCE7" stroke="#22C55E" class="activation"/>`);
lines.push(`<rect x="${xs.producer - 8}" y="545" width="16" height="250" rx="6" fill="#DBEAFE" stroke="#2563EB" class="activation"/>`);
lines.push(`<rect x="${xs.topic - 8}" y="675" width="16" height="630" rx="6" fill="#FEF3C7" stroke="#D97706" class="activation"/>`);
lines.push(`<rect x="${xs.consumer - 8}" y="1110" width="16" height="190" rx="6" fill="#F3E8FF" stroke="#8B5CF6" class="activation"/>`);

msg(1, "app", "bridge", 330, "suspendSend(record)", c.bridge, false, 220);
selfMsg(2, "bridge", 435, "install callback and suspend continuation", c.bridge, 545, 390);
msg(3, "bridge", "producer", 560, "send(record, callback)", c.producer, false, 590);
msg(4, "producer", "topic", 690, "append record to topic partition", c.kafka, false, 850, 330);
msg(5, "topic", "producer", 815, "RecordMetadata", c.result, true, 895, 200);
msg(6, "producer", "bridge", 925, "callback resumes continuation", c.bridge, true, 615, 330);
msg(7, "bridge", "app", 1035, "RecordMetadata", c.result, true, 235, 205);

lines.push(`<rect x="895" y="1100" width="650" height="255" rx="15" class="alt"/>`);
lines.push(`<rect x="918" y="1124" width="255" height="30" rx="15" fill="#F3E8FF" stroke="${c.consumer}" stroke-width="1.6"/>`);
lines.push(`<text x="940" y="1144" class="note">consumer branch</text>`);
msg(8, "consumer", "topic", 1195, "poll / receive", c.consumer, false, 1110, 205);
msg(9, "topic", "consumer", 1288, "ConsumerRecord", c.kafka, false, 1110, 215);
msg(10, "consumer", "app", 1425, "process record and commit offset", c.result, true, 665, 360);

lines.push(`<rect x="118" y="1490" width="560" height="92" rx="14" fill="#F0FDF4" stroke="#22C55E" stroke-width="1.8"/>`);
lines.push(`<text x="146" y="1522" class="participant">Cancellation contract</text>`);
lines.push(`<text x="146" y="1550" class="note">suspendSend cancels the pending Future when the coroutine is cancelled.</text>`);
lines.push(`<text x="146" y="1572" class="note">Normal flow completion flushes the producer after sending records.</text>`);

lines.push(`<rect x="96" y="1660" width="${W - 192}" height="48" rx="12" fill="#FFFFFF" stroke="${c.border}" stroke-width="1.6"/>`);
lines.push(`<text x="${W / 2}" y="1690" text-anchor="middle" class="footer">Producer extensions preserve Kafka callback semantics while exposing suspend and Flow APIs.</text>`);
lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
