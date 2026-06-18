#!/usr/bin/env node
import { readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-kafka4-diagram-01.svg";
const kafkaIcon =
  "data:image/png;base64," +
  readFileSync("/Users/debop/work/bluetape4k/bluetape4k-wiki/docs/icons/kafka/apache-kafka-logo.png").toString("base64");

const W = 1320;
const H = 820;
const C = {
  bg: "#ffffff",
  ink: "#111827",
  sub: "#4b5563",
  muted: "#6b7280",
  line: "#d1d5db",
  blue: "#2563eb",
  green: "#16a34a",
  red: "#dc2626",
  orange: "#ea580c",
  purple: "#9333ea",
  teal: "#0d9488",
  blueFill: "#eff6ff",
  blueStroke: "#bfdbfe",
  greenFill: "#f0fdf4",
  greenStroke: "#bbf7d0",
  redFill: "#fef2f2",
  redStroke: "#fecaca",
  orangeFill: "#fff7ed",
  orangeStroke: "#fed7aa",
  purpleFill: "#faf5ff",
  purpleStroke: "#ddd6fe",
  tealFill: "#f0fdfa",
  tealStroke: "#99f6e4",
  grayFill: "#f9fafb",
  grayStroke: "#e5e7eb",
};

const esc = (s) =>
  String(s).replace(/[&<>"']/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&apos;" })[ch]);

const svg = [];
const add = (s = "") => svg.push(s);

function text(x, y, value, size = 14, color = C.ink, weight = 400, anchor = "start", attrs = "") {
  add(`<text x="${x}" y="${y}" fill="${color}" font-size="${size}" font-weight="${weight}" text-anchor="${anchor}" ${attrs}>${esc(value)}</text>`);
}

function rows(x, y, values, size = 13, color = C.sub, weight = 500, gap = 19) {
  values.forEach((value, i) => text(x, y + i * gap, value, size, color, weight));
}

function layer({ x, y, w, h, title, subtitle, fill, stroke }) {
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="16" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>`);
  text(x + 22, y + 32, title, 15, C.ink, 700);
  text(x + 22, y + 54, subtitle, 12, C.muted, 500);
}

function card({ id, x, y, w, h, title, subtitle, body, fill, stroke, icon = null, iconSize = 42 }) {
  add(`<g id="${id}">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="11" fill="${fill}" stroke="${stroke}" stroke-width="1.9"/>`);
  let tx = x + 20;
  if (icon) {
    add(`<image href="${icon}" x="${x + 18}" y="${y + 24}" width="${iconSize}" height="${iconSize}" preserveAspectRatio="xMidYMid meet"/>`);
    tx = x + 76;
  }
  text(tx, y + 31, title, 16, C.ink, 700);
  if (subtitle) text(tx, y + 53, subtitle, 12, C.muted, 600);
  rows(tx, y + (subtitle ? 78 : 59), body, 13, C.sub, 500, 18);
  add(`</g>`);
}

function arrow(id, d, color, marker, width = 3.2, dash = "") {
  add(`<path id="${id}" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" ${dash ? `stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})" stroke-linecap="round" stroke-linejoin="round"/>`);
}

function label(x, y, value, color) {
  const w = value.length * 7 + 18;
  add(`<rect x="${x - w / 2}" y="${y - 17}" width="${w}" height="24" rx="7" fill="${C.bg}" opacity="0.96"/>`);
  text(x, y, value, 12, color, 700, "middle");
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
  <marker id="arrow-red" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 0 0 L 12 6 L 0 12 Z" fill="${C.red}" stroke="${C.red}" stroke-width="0" stroke-dasharray="none"/>
  </marker>
  <marker id="arrow-orange" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 0 0 L 14 7 L 0 14 Z" fill="${C.orange}" stroke="${C.orange}" stroke-width="0" stroke-dasharray="none"/>
  </marker>
  <marker id="arrow-purple" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 0 0 L 14 7 L 0 14 Z" fill="${C.purple}" stroke="${C.purple}" stroke-width="0" stroke-dasharray="none"/>
  </marker>
  <marker id="arrow-teal" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 0 0 L 14 7 L 0 14 Z" fill="${C.teal}" stroke="${C.teal}" stroke-width="0" stroke-dasharray="none"/>
  </marker>
</defs>`);
add(`<rect width="${W}" height="${H}" fill="${C.bg}"/>`);

text(660, 43, "Kafka 4 Dependency Boundary", 28, C.ink, 700, "middle");
text(
  660,
  72,
  "Choose one Kafka API line, then keep every Apache Kafka artifact aligned with the Kafka 4 runtime",
  14,
  C.muted,
  500,
  "middle",
);

layer({
  x: 48,
  y: 112,
  w: 296,
  h: 608,
  title: "Application choice",
  subtitle: "one runtime classpath line",
  fill: C.grayFill,
  stroke: C.grayStroke,
});
layer({
  x: 384,
  y: 112,
  w: 350,
  h: 608,
  title: "bluetape4k-kafka4",
  subtitle: "same API package, Kafka 4 stack",
  fill: "#f8fafc",
  stroke: "#cbd5e1",
});
layer({
  x: 774,
  y: 112,
  w: 496,
  h: 608,
  title: "Pinned runtime and test deps",
  subtitle: "Kafka 4, Spring 4, Jackson 3, KRaft",
  fill: "#fff7ed",
  stroke: "#fed7aa",
});

card({
  id: "kafka3-line",
  x: 82,
  y: 184,
  w: 226,
  h: 118,
  title: "Kafka 3 line",
  subtitle: "existing module",
  body: ["bluetape4k-kafka", "Spring Kafka 3", "Jackson 2"],
  fill: C.grayFill,
  stroke: C.grayStroke,
});
card({
  id: "choose-kafka4",
  x: 82,
  y: 384,
  w: 226,
  h: 118,
  title: "Use kafka4 line",
  subtitle: "runtime classpath",
  body: ["implementation", "bluetape4k-kafka4", "for Boot 4 apps"],
  fill: C.blueFill,
  stroke: C.blueStroke,
});
card({
  id: "do-not-mix",
  x: 82,
  y: 548,
  w: 226,
  h: 116,
  title: "Do not mix",
  subtitle: "duplicate API package",
  body: ["bluetape4k-kafka", "and kafka4 both expose", "io.bluetape4k.kafka"],
  fill: C.redFill,
  stroke: C.redStroke,
});

card({
  id: "gradle-guard",
  x: 422,
  y: 188,
  w: 276,
  h: 136,
  title: "Gradle boundary",
  subtitle: "build.gradle.kts",
  body: ["all org.apache.kafka", "artifacts use kafka4", "prevents kafka3 downgrade"],
  fill: C.greenFill,
  stroke: C.greenStroke,
});
card({
  id: "module-api",
  x: 422,
  y: 374,
  w: 276,
  h: 144,
  title: "Kotlin API surface",
  subtitle: "same module shape",
  body: ["producer/consumer helpers", "coroutine Spring bridge", "Streams DSL and codecs"],
  fill: C.blueFill,
  stroke: C.blueStroke,
});
card({
  id: "lz4-guard",
  x: 422,
  y: 576,
  w: 276,
  h: 82,
  title: "LZ4 replacement",
  body: ["exclude org.lz4", "expose at.yawk.lz4 to consumers"],
  fill: C.orangeFill,
  stroke: C.orangeStroke,
});

card({
  id: "kafka-artifacts",
  x: 814,
  y: 188,
  w: 200,
  h: 136,
  title: "Apache Kafka",
  subtitle: "4.2.x artifacts",
  body: ["clients, streams", "generator", "server/common tests"],
  fill: "#ffffff",
  stroke: "#f59e0b",
  icon: kafkaIcon,
  iconSize: 42,
});
card({
  id: "spring-kafka4",
  x: 1042,
  y: 188,
  w: 190,
  h: 136,
  title: "Spring Kafka",
  subtitle: "4.x / Boot 4",
  body: ["strict non-null", "template boundaries", "embedded test APIs"],
  fill: C.greenFill,
  stroke: C.greenStroke,
});
card({
  id: "jackson3",
  x: 814,
  y: 378,
  w: 200,
  h: 128,
  title: "Jackson 3",
  subtitle: "tools.jackson",
  body: ["bluetape4k-jackson3", "JsonMapper", "type allowlist codec"],
  fill: C.purpleFill,
  stroke: C.purpleStroke,
});
card({
  id: "codec-deps",
  x: 1042,
  y: 378,
  w: 190,
  h: 128,
  title: "Codec deps",
  subtitle: "optional codecs",
  body: ["Kryo / Fory", "Snappy / Zstd", "LZ4 namespace swap"],
  fill: C.orangeFill,
  stroke: C.orangeStroke,
});
card({
  id: "kraft-tests",
  x: 814,
  y: 562,
  w: 418,
  h: 98,
  title: "KRaft-only embedded tests",
  body: ["Spring Kafka 4 embedded broker", "KRaft only; omit kraft = true"],
  fill: "#ffffff",
  stroke: "#f59e0b",
  icon: kafkaIcon,
  iconSize: 34,
});

// Classpath choice and blocked Kafka 3 line.
arrow("choose-to-module", "M 308 443 H 422", C.blue, "arrow-blue", 3.6);
label(365, 431, "selected line", C.blue);
arrow("mix-block", "M 308 606 H 384", C.red, "arrow-red", 3, "7 5");
label(360, 594, "blocked together", C.red);

// Dependency pins and module usage.
arrow("gradle-to-kafka", "M 698 248 H 814", C.green, "arrow-green", 3.6);
label(756, 236, "pins Kafka 4", C.green);
arrow("kafka-to-spring", "M 1014 256 H 1042", C.green, "arrow-green", 3.2);
arrow("module-to-jackson", "M 698 442 H 814", C.purple, "arrow-purple", 3.4);
arrow("module-to-codecs", "M 698 468 H 756 V 530 H 1034 V 442 H 1042", C.orange, "arrow-orange", 3.4);
arrow("module-to-tests", "M 560 518 V 535 H 780 V 611 H 814", C.teal, "arrow-teal", 3.4);
arrow("lz4-to-codecs", "M 698 617 H 742 V 552 H 1022 V 470 H 1042", C.orange, "arrow-orange", 3.4, "7 5");

// Legend.
add(`<g transform="translate(64, 762)">`);
text(0, 0, "Legend", 13, C.ink, 700);
add(`<line x1="72" y1="-4" x2="112" y2="-4" stroke="${C.blue}" stroke-width="3.2" marker-end="url(#arrow-blue)"/>`);
text(124, 0, "selected API line", 12, C.muted, 600);
add(`<line x1="274" y1="-4" x2="314" y2="-4" stroke="${C.green}" stroke-width="3.2" marker-end="url(#arrow-green)"/>`);
text(326, 0, "dependency alignment", 12, C.muted, 600);
add(`<line x1="520" y1="-4" x2="560" y2="-4" stroke="${C.red}" stroke-width="3" stroke-dasharray="7 5" marker-end="url(#arrow-red)"/>`);
text(572, 0, "forbidden classpath mix", 12, C.muted, 600);
add(`</g>`);

add(`</svg>`);

writeFileSync(out, svg.join("\n"));
console.log(out);
