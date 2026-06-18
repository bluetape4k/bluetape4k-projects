#!/usr/bin/env node
import { readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-kafka-diagram-02.svg";
const kafkaIcon =
  "data:image/png;base64," +
  readFileSync("/Users/debop/work/bluetape4k/bluetape4k-wiki/docs/icons/kafka/apache-kafka-logo.png").toString("base64");
const dbIcon =
  "data:image/svg+xml;base64," +
  readFileSync("/Users/debop/work/bluetape4k/bluetape4k-wiki/docs/icons/generic/database-server.svg").toString("base64");

const W = 1320;
const H = 700;
const C = {
  bg: "#ffffff",
  ink: "#111827",
  sub: "#4b5563",
  faint: "#6b7280",
  blue: "#2563eb",
  green: "#16a34a",
  purple: "#9333ea",
  orange: "#ea580c",
  gray: "#64748b",
  line: "#cbd5e1",
  blueFill: "#eff6ff",
  blueStroke: "#bfdbfe",
  greenFill: "#f0fdf4",
  greenStroke: "#bbf7d0",
  purpleFill: "#faf5ff",
  purpleStroke: "#ddd6fe",
  orangeFill: "#fff7ed",
  orangeStroke: "#fed7aa",
  tealFill: "#f0fdfa",
  tealStroke: "#99f6e4",
  grayFill: "#f8fafc",
  grayStroke: "#cbd5e1",
};

const esc = (s) =>
  String(s).replace(/[&<>"']/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&apos;" })[ch]);
const lines = [];
const add = (s = "") => lines.push(s);

function text(x, y, value, size = 14, color = C.ink, weight = 400, anchor = "start", extra = "") {
  add(`<text x="${x}" y="${y}" fill="${color}" font-size="${size}" font-weight="${weight}" text-anchor="${anchor}" ${extra}>${esc(value)}</text>`);
}

function card({ id, x, y, w, h, title, rows, fill, stroke, icon, iconSize = 44 }) {
  add(`<g id="${id}">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>`);
  let tx = x + 18;
  if (icon) {
    add(`<image href="${icon}" x="${x + 16}" y="${y + 22}" width="${iconSize}" height="${iconSize}" preserveAspectRatio="xMidYMid meet"/>`);
    tx = x + 74;
  }
  text(tx, y + 30, title, 16, C.ink, 700);
  rows.forEach((row, i) => text(tx, y + 57 + i * 18, row, 12.5, C.sub, 500));
  add(`</g>`);
}

function arrow(id, d, color, marker, label, lx, ly, dash = "", width = 3.4) {
  add(`<path id="${id}" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" ${dash ? `stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})" stroke-linecap="round" stroke-linejoin="round"/>`);
  if (label) {
    const bgW = Math.max(82, label.length * 7.2);
    add(`<rect x="${lx - bgW / 2}" y="${ly - 17}" width="${bgW}" height="24" rx="7" fill="${C.bg}" opacity="0.96"/>`);
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
text(660, 42, "Kafka Streams Processing Flow", 28, C.ink, 700, "middle");
text(660, 70, "DSL helper functions keep Serde, topology names, repartitioning, output binding, and state stores explicit", 14, C.faint, 500, "middle");

add(`<rect x="44" y="118" width="1260" height="500" rx="16" fill="#f8fafc" stroke="#cbd5e1" stroke-width="1.5"/>`);
text(72, 150, "stream topology assembly", 15, C.ink, 700);
text(72, 172, "topic boundaries use Kafka runtime components; helper cards are module API wrappers", 12, C.faint, 500);

card({ id: "input", x: 70, y: 246, w: 196, h: 118, title: "Input topic", rows: ["source records", "partitioned log"], fill: "#ffffff", stroke: "#f59e0b", icon: kafkaIcon, iconSize: 44 });
card({ id: "consumed", x: 306, y: 246, w: 210, h: 118, title: "consumedOf(...)", rows: ["key/value Serde", "timestamp extractor", "offset reset policy"], fill: C.blueFill, stroke: C.blueStroke });
card({ id: "topology", x: 556, y: 216, w: 260, h: 178, title: "KStream / KTable graph", rows: ["filter, map, aggregate", "branchedOf(...)", "joinedOf / windowedOf"], fill: C.greenFill, stroke: C.greenStroke });
card({ id: "produced", x: 866, y: 246, w: 196, h: 118, title: "producedOf(...)", rows: ["output Serde", "partitioner", "processor name"], fill: C.orangeFill, stroke: C.orangeStroke });
card({ id: "output", x: 1102, y: 246, w: 196, h: 118, title: "Output topic", rows: ["sink records", "downstream apps"], fill: "#ffffff", stroke: "#f59e0b", icon: kafkaIcon, iconSize: 44 });

card({ id: "grouped", x: 306, y: 456, w: 210, h: 110, title: "groupedOf / repartitionedOf", rows: ["group names", "partition count", "custom partitioner"], fill: C.purpleFill, stroke: C.purpleStroke });
card({ id: "internal", x: 556, y: 456, w: 260, h: 110, title: "Internal topic", rows: ["repartition changelog", "Kafka-managed log"], fill: "#ffffff", stroke: "#f59e0b", icon: kafkaIcon, iconSize: 44 });
card({ id: "materialized", x: 866, y: 456, w: 196, h: 110, title: "materializedOf(...)", rows: ["store type/name", "key/value Serde", "supplier overloads"], fill: C.tealFill, stroke: C.tealStroke });
card({ id: "state-store", x: 1102, y: 456, w: 196, h: 110, title: "State store", rows: ["key-value", "window/session"], fill: "#ffffff", stroke: "#0d9488", icon: dbIcon, iconSize: 42 });

arrow("input-consumed", "M 266 305 H 306", C.blue, "arrow-blue", "source", 286, 293);
arrow("consumed-topology", "M 516 305 H 556", C.blue, "arrow-blue", "bind", 536, 293);
arrow("topology-produced", "M 816 305 H 866", C.orange, "arrow-orange", "emit", 842, 293);
arrow("produced-output", "M 1062 305 H 1102", C.orange, "arrow-orange", "sink", 1082, 293);

arrow("topology-grouped", "M 648 394 V 424 H 414 V 456", C.purple, "arrow-purple", "group", 528, 412);
arrow("grouped-internal", "M 516 511 H 556", C.purple, "arrow-purple", "repartition", 536, 499);
arrow("internal-topology", "M 686 456 V 394", C.green, "arrow-green", null, 0, 0);
arrow("topology-materialized", "M 740 394 V 424 H 964 V 456", C.green, "arrow-green", "stateful", 852, 412);
arrow("materialized-store", "M 1062 511 H 1102", C.green, "arrow-green", "backing store", 1082, 499);

add(`<g transform="translate(76 656)">`);
add(`<line x1="0" y1="0" x2="38" y2="0" stroke="${C.blue}" stroke-width="3.4" marker-end="url(#arrow-blue)"/>`);
text(52, 5, "source binding", 12, C.faint, 600);
add(`<line x1="196" y1="0" x2="234" y2="0" stroke="${C.orange}" stroke-width="3.4" marker-end="url(#arrow-orange)"/>`);
text(248, 5, "output binding", 12, C.faint, 600);
add(`<line x1="414" y1="0" x2="452" y2="0" stroke="${C.purple}" stroke-width="3.4" marker-end="url(#arrow-purple)"/>`);
text(466, 5, "group/repartition", 12, C.faint, 600);
add(`<line x1="664" y1="0" x2="702" y2="0" stroke="${C.green}" stroke-width="3.4" marker-end="url(#arrow-green)"/>`);
text(716, 5, "stateful graph", 12, C.faint, 600);
add(`</g>`);

add(`</svg>`);
writeFileSync(out, lines.join("\n"));
console.log(out);
