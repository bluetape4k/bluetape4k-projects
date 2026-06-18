#!/usr/bin/env node
import { readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-kafka-diagram-01.svg";
const kafkaIcon =
  "data:image/png;base64," +
  readFileSync("/Users/debop/work/bluetape4k/bluetape4k-wiki/docs/icons/kafka/apache-kafka-logo.png").toString("base64");

const W = 1280;
const H = 820;
const C = {
  bg: "#ffffff",
  ink: "#111827",
  sub: "#4b5563",
  faint: "#6b7280",
  line: "#d1d5db",
  blue: "#2563eb",
  green: "#16a34a",
  purple: "#9333ea",
  orange: "#ea580c",
  tealFill: "#f0fdfa",
  tealStroke: "#99f6e4",
  blueFill: "#eff6ff",
  blueStroke: "#bfdbfe",
  greenFill: "#f0fdf4",
  greenStroke: "#bbf7d0",
  purpleFill: "#faf5ff",
  purpleStroke: "#ddd6fe",
  orangeFill: "#fff7ed",
  orangeStroke: "#fed7aa",
  grayFill: "#f9fafb",
  grayStroke: "#e5e7eb",
};

const esc = (s) =>
  String(s).replace(/[&<>"']/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&apos;" })[ch]);

const lines = [];
const add = (s = "") => lines.push(s);

function text(x, y, value, size = 14, color = C.ink, weight = 400, anchor = "start", extra = "") {
  add(`<text x="${x}" y="${y}" fill="${color}" font-size="${size}" font-weight="${weight}" text-anchor="${anchor}" ${extra}>${esc(value)}</text>`);
}

function wrapText(x, y, width, rows, size = 14, color = C.sub, weight = 400, anchor = "start", lineGap = 19) {
  rows.forEach((row, i) => text(x, y + i * lineGap, row, size, color, weight, anchor));
}

function layer(x, y, w, h, title, subtitle, fill, stroke) {
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="14" fill="${fill}" stroke="${stroke}" stroke-width="1.6"/>`);
  text(x + 22, y + 30, title, 15, C.ink, 700);
  if (subtitle) text(x + 22, y + 52, subtitle, 12, C.faint, 500);
}

function card({ id, x, y, w, h, title, rows, fill, stroke, titleColor = C.ink, icon, iconSize = 44 }) {
  add(`<g id="${id}">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>`);
  let tx = x + 20;
  if (icon) {
    add(`<image href="${icon}" x="${x + 18}" y="${y + 23}" width="${iconSize}" height="${iconSize}" preserveAspectRatio="xMidYMid meet"/>`);
    tx = x + 78;
  }
  text(tx, y + 31, title, 17, titleColor, 700);
  wrapText(tx, y + 58, w - (tx - x) - 18, rows, 13, C.sub, 500, "start", 18);
  add(`</g>`);
}

function arrowPath(id, d, color, marker, width = 3, dash = "", label = null, lx = 0, ly = 0) {
  add(`<path id="${id}" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" ${dash ? `stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})" stroke-linecap="round" stroke-linejoin="round"/>`);
  if (label) {
    const pad = label.length * 3.5 + 14;
    add(`<rect x="${lx - pad / 2}" y="${ly - 16}" width="${pad}" height="22" rx="6" fill="${C.bg}" opacity="0.94"/>`);
    text(lx, ly, label, 12, color, 700, "middle");
  }
}

add(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">`);
add(`<style>
  text { font-family: 'Architects Daughter', 'Comic Mono', 'Comic Sans MS', sans-serif; letter-spacing: 0; }
</style>`);
add(`<defs>
  <marker id="arrow-blue" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 0 0 L 14 7 L 0 14 Z" fill="${C.blue}" stroke="${C.blue}" stroke-width="0" stroke-dasharray="none"/>
  </marker>
  <marker id="arrow-green" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 0 0 L 14 7 L 0 14 Z" fill="${C.green}" stroke="${C.green}" stroke-width="0" stroke-dasharray="none"/>
  </marker>
  <marker id="arrow-purple" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 0 0 L 14 7 L 0 14 Z" fill="${C.purple}" stroke="${C.purple}" stroke-width="0" stroke-dasharray="none"/>
  </marker>
  <marker id="arrow-orange" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 0 0 L 14 7 L 0 14 Z" fill="${C.orange}" stroke="${C.orange}" stroke-width="0" stroke-dasharray="none"/>
  </marker>
</defs>`);
add(`<rect width="${W}" height="${H}" fill="${C.bg}"/>`);
text(640, 42, "Kafka API Structure", 28, C.ink, 700, "middle");
text(640, 70, "Kotlin client helpers, coroutine bridges, codecs, Spring adapters, and Streams DSL wrappers around Apache Kafka", 14, C.faint, 500, "middle");

layer(48, 104, 220, 612, "Application code", "uses small Kotlin entrypoints", C.grayFill, C.grayStroke);
layer(304, 104, 660, 612, "bluetape4k-kafka module", "source-backed API groups", "#f8fafc", "#cbd5e1");
layer(1004, 104, 228, 612, "Kafka runtime", "external broker and topics", "#fff7ed", "#fed7aa");

card({
  id: "app",
  x: 82,
  y: 356,
  w: 152,
  h: 136,
  title: "User code",
  rows: ["builds records", "selects codecs", "collects flows"],
  fill: "#ffffff",
  stroke: "#d1d5db",
});

card({
  id: "clients",
  x: 344,
  y: 170,
  w: 256,
  h: 132,
  title: "Client factories",
  rows: ["producerOf(...)", "consumerOf(...)", "TopicPartition helpers"],
  fill: C.blueFill,
  stroke: C.blueStroke,
});
card({
  id: "coroutines",
  x: 644,
  y: 170,
  w: 276,
  h: 132,
  title: "Coroutine producer",
  rows: ["suspendSend awaits callback", "sendAsFlow buffers results", "cancels pending Future"],
  fill: C.greenFill,
  stroke: C.greenStroke,
});
card({
  id: "codecs",
  x: 344,
  y: 366,
  w: 256,
  h: 150,
  title: "KafkaCodecs",
  rows: ["String / ByteArray / Jackson", "Kryo, Fory and compression", "type header allowlist guard"],
  fill: C.purpleFill,
  stroke: C.purpleStroke,
});
card({
  id: "spring",
  x: 644,
  y: 366,
  w: 276,
  h: 150,
  title: "Spring coroutine bridge",
  rows: ["KafkaOperations.suspendSend", "Suspend producer template", "Suspend consumer template"],
  fill: C.orangeFill,
  stroke: C.orangeStroke,
});
card({
  id: "streams",
  x: 344,
  y: 590,
  w: 576,
  h: 100,
  title: "Streams DSL helpers",
  rows: ["consumedOf, producedOf, groupedOf, materializedOf, joinedOf, windowedOf"],
  fill: C.tealFill,
  stroke: C.tealStroke,
});
card({
  id: "runtime",
  x: 1034,
  y: 340,
  w: 168,
  h: 168,
  title: "Apache Kafka",
  rows: ["brokers", "topics", "partitions"],
  fill: "#ffffff",
  stroke: "#f59e0b",
  icon: kafkaIcon,
  iconSize: 52,
});

arrowPath("app-clients", "M 234 392 H 292 V 236 H 344", C.blue, "arrow-blue", 3.6, "", "create", 292, 304);
arrowPath("app-codecs", "M 234 424 H 344", C.purple, "arrow-purple", 3.6, "", "serialize", 292, 412);
arrowPath("app-streams", "M 234 462 H 288 V 640 H 344", C.green, "arrow-green", 3.6, "", "topology", 288, 552);

arrowPath("clients-runtime", "M 600 236 H 622 V 94 H 1210 V 320 H 1118 V 340", C.blue, "arrow-blue", 3.4, "", "client calls", 848, 92);
arrowPath("coroutines-runtime", "M 920 236 H 986 V 376 H 1034", C.green, "arrow-green", 3.6, "", "await send", 986, 306);
arrowPath("spring-runtime", "M 920 421 H 1034", C.orange, "arrow-orange", 3.6, "", "reactor kafka", 974, 409);
arrowPath("codecs-runtime", "M 600 441 H 622 V 536 H 1006 V 464 H 1034", C.purple, "arrow-purple", 3.2, "8 5", "record bytes", 814, 522);
arrowPath("streams-runtime", "M 920 640 H 1118 V 508", C.green, "arrow-green", 3.6, "", "stream task", 1118, 570);

add(`<g transform="translate(66 746)">`);
add(`<line x1="0" y1="0" x2="36" y2="0" stroke="${C.blue}" stroke-width="3.6" marker-end="url(#arrow-blue)"/>`);
text(48, 5, "Kafka client API", 12, C.faint, 600);
add(`<line x1="178" y1="0" x2="214" y2="0" stroke="${C.green}" stroke-width="3.6" marker-end="url(#arrow-green)"/>`);
text(226, 5, "Coroutine / Streams", 12, C.faint, 600);
add(`<line x1="402" y1="0" x2="438" y2="0" stroke="${C.orange}" stroke-width="3.6" marker-end="url(#arrow-orange)"/>`);
text(450, 5, "Spring bridge", 12, C.faint, 600);
add(`<line x1="602" y1="0" x2="638" y2="0" stroke="${C.purple}" stroke-width="3.2" stroke-dasharray="8 5" marker-end="url(#arrow-purple)"/>`);
text(650, 5, "Codec bytes", 12, C.faint, 600);
add(`</g>`);

add(`</svg>`);
writeFileSync(out, lines.join("\n"));
console.log(out);
