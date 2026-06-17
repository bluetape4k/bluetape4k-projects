#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/data-cassandra-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/data-cassandra-diagram-02.png";
const W = 2220;
const H = 1225;
const c = { ink: "#0F172A", muted: "#475569", canvas: "#F8FAFC", frame: "#FFFFFF", line: "#CBD5E1", blue: "#2563EB", teal: "#0D9488", green: "#16A34A", orange: "#EA580C", purple: "#7C3AED", gray: "#64748B" };

const sources = [
  "data/cassandra/README.md",
  "data/cassandra/README.ko.md",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/GettableSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/SettableSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/QueryBuilderSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/RelationBuilderSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt",
];
for (const source of sources) if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
const readme = readFileSync(sources[0], "utf8");
if (!/Core API Structure[\s\S]*data-cassandra-diagram-02\.png/.test(readme)) throw new Error("README diagram slot not found");

const boxes = {
  cqlSession: { x: 105, y: 175, w: 380, h: 168, fill: "#F8FAFC", stroke: c.gray, stereo: "<<driver>>", title: "CqlSession", attrs: ["sync driver session"], methods: ["execute(...)", "prepare(...)"] },
  asyncSession: { x: 640, y: 175, w: 410, h: 168, fill: "#F8FAFC", stroke: c.gray, stereo: "<<driver>>", title: "AsyncCqlSession", attrs: ["async driver session"], methods: ["executeAsync(...)", "prepareAsync(...)"] },
  statement: { x: 1205, y: 175, w: 410, h: 168, fill: "#F8FAFC", stroke: c.gray, stereo: "<<driver>>", title: "Statement / Builder", attrs: ["Simple, Bound, Batch"], methods: ["driver native CQL objects"] },
  row: { x: 1770, y: 175, w: 340, h: 168, fill: "#F8FAFC", stroke: c.gray, stereo: "<<driver>>", title: "Row / Gettable", attrs: ["column definitions"], methods: ["codec registry", "typed getters"] },
  sessionSupport: { x: 115, y: 535, w: 425, h: 195, fill: "#EFF6FF", stroke: c.blue, stereo: "<<extension file>>", title: "CqlSessionSupport", attrs: ["cqlSession { builder }", "cqlSessionOf(defaults...)"], methods: ["builds CqlSessionBuilder", "applies contact/keyspace"] },
  asyncSupport: { x: 665, y: 535, w: 425, h: 195, fill: "#F0FDFA", stroke: c.teal, stereo: "<<extension file>>", title: "AsyncCqlSessionSupport", attrs: ["executeSuspending(...)", "prepareSuspending(...)"], methods: ["wraps statementOf(...)", "awaits CompletionStage"] },
  statementSupport: { x: 1215, y: 535, w: 425, h: 195, fill: "#FFF7ED", stroke: c.orange, stereo: "<<extension file>>", title: "StatementSupport", attrs: ["statementOf / simpleStatementOf", "boundStatementOf / batchStatementOf"], methods: ["validates nonblank CQL", "returns new driver statements"] },
  rowSupport: { x: 1765, y: 535, w: 340, h: 195, fill: "#ECFDF5", stroke: c.green, stereo: "<<extension file>>", title: "RowSupport", attrs: ["toMap / toNamedMap", "mapWithName / codecs"], methods: ["decodes via TypeCodec", "sorts named output"] },
  provider: { x: 115, y: 930, w: 425, h: 200, fill: "#EFF6FF", stroke: c.blue, stereo: "<<object>>", title: "CqlSessionProvider", attrs: ["default contact/datacenter", "keyspace session cache"], methods: ["newCqlSessionBuilder(...)", "getOrCreateSession(...)"] },
  querybuilder: { x: 665, y: 930, w: 425, h: 200, fill: "#F5F3FF", stroke: c.purple, stereo: "<<extension package>>", title: "querybuilder/*", attrs: ["bindMarker / raw / udt", "relation and term helpers"], methods: ["delegates to QueryBuilder", "keeps generated CQL native"] },
  admin: { x: 1215, y: 930, w: 425, h: 200, fill: "#FEF2F2", stroke: "#DC2626", stereo: "<<object>>", title: "CassandraAdmin", attrs: ["create/drop keyspace", "release version lookup"], methods: ["uses SchemaBuilder", "queries system.local"] },
  dataSupport: { x: 1765, y: 930, w: 340, h: 200, fill: "#ECFDF5", stroke: c.green, stereo: "<<extension package>>", title: "data/*", attrs: ["GettableSupport", "SettableSupport"], methods: ["reified typed access", "name, id, or index overloads"] },
};

const edges = [
  { type: "uses", color: c.blue, d: "M328 535 L328 343", label: { x: 420, y: 450, text: "builds" } },
  { type: "uses", color: c.teal, d: "M878 535 L878 343", label: { x: 980, y: 450, text: "awaits" } },
  { type: "uses", color: c.orange, d: "M1428 535 L1428 343", label: { x: 1530, y: 450, text: "creates" } },
  { type: "uses", color: c.green, d: "M1935 535 L1935 343", label: { x: 2028, y: 450, text: "decodes" } },
  { type: "uses", color: c.orange, d: "M1090 632 L1150 632 L1150 440 L1295 440 L1295 343", label: { x: 1135, y: 485, text: "uses statementOf" } },
  { type: "uses", color: c.green, d: "M1935 930 L1935 730", label: { x: 2028, y: 838, text: "typed access" } },
  { type: "uses", color: c.blue, d: "M328 930 L328 730", label: { x: 430, y: 838, text: "defaults/cache" } },
  { type: "uses", color: c.purple, d: "M878 930 L878 805 L1320 805 L1320 730", label: { x: 1110, y: 780, text: "feeds statements" } },
  { type: "uses", color: "#DC2626", d: "M1428 930 L1428 730", label: { x: 1532, y: 838, text: "schema admin" } },
  { type: "uses", color: c.gray, d: "M1640 630 L1765 630", label: { x: 1702, y: 605, text: "result rows" } },
];

function esc(v) { return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;"); }
function box(id) {
  const b = boxes[id], sep1 = b.y + 64, sep2 = b.y + 116;
  return `<g id="${id}"><rect class="umlBox" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/><line x1="${b.x}" y1="${sep1}" x2="${b.x + b.w}" y2="${sep1}" stroke="${b.stroke}"/><line x1="${b.x}" y1="${sep2}" x2="${b.x + b.w}" y2="${sep2}" stroke="${b.stroke}"/><text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 24}" text-anchor="middle">${esc(b.stereo)}</text><text class="classTitle" x="${b.x + b.w / 2}" y="${b.y + 50}" text-anchor="middle">${esc(b.title)}</text>${b.attrs.map((line, i) => `<text class="member" x="${b.x + 22}" y="${b.y + 88 + i * 20}">${esc(line)}</text>`).join("")}${b.methods.map((line, i) => `<text class="member" x="${b.x + 22}" y="${b.y + 143 + i * 20}">${esc(line)}</text>`).join("")}</g>`;
}
function nums(d) { return d.match(/-?\d+(?:\.\d+)?/g).map(Number); }
function arrowHead(e) {
  const n = nums(e.d), end = { x: n[n.length - 2], y: n[n.length - 1] }, prev = { x: n[n.length - 4], y: n[n.length - 3] };
  const dx = end.x - prev.x, dy = end.y - prev.y;
  if (dx < 0) return `<path class="solidOpenHead" d="M${end.x + 13} ${end.y - 7} L${end.x} ${end.y} L${end.x + 13} ${end.y + 7}" stroke="${e.color}"/>`;
  if (dx > 0) return `<path class="solidOpenHead" d="M${end.x - 13} ${end.y - 7} L${end.x} ${end.y} L${end.x - 13} ${end.y + 7}" stroke="${e.color}"/>`;
  if (dy < 0) return `<path class="solidOpenHead" d="M${end.x - 7} ${end.y + 13} L${end.x} ${end.y} L${end.x + 7} ${end.y + 13}" stroke="${e.color}"/>`;
  return `<path class="solidOpenHead" d="M${end.x - 7} ${end.y - 13} L${end.x} ${end.y} L${end.x + 7} ${end.y - 13}" stroke="${e.color}"/>`;
}
function label(l) {
  const w = Math.max(80, l.text.length * 8.2 + 18);
  return `<g class="edgeLabel" transform="translate(${l.x - w / 2} ${l.y - 14})"><rect width="${w}" height="28" rx="8"/><text x="${w / 2}" y="19" text-anchor="middle">${esc(l.text)}</text></g>`;
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Cassandra core API structure">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/></filter><style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}.subtitle,.sectionTitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}.section{fill:#F3F8FF;stroke:#94A3B8;stroke-width:1.6;stroke-dasharray:12 8}.umlBox{filter:url(#softShadow);stroke-width:2}.stereo{font-family:"Comic Mono";font-size:12px;fill:${c.muted}}.classTitle{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}.member{font-family:"Comic Mono";font-size:12.5px;fill:${c.muted}}.edge{fill:none;stroke-width:2.55;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:8 7}.solidOpenHead{fill:none;stroke-width:2.25;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:none}.edgeLabel rect{fill:#fff;stroke:${c.line};stroke-width:1.2;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:11.8px;fill:${c.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/>
<text class="title" x="76" y="86">Cassandra Core API Structure</text><text class="subtitle" x="78" y="118">Static extension files depend on driver primitives; the module does not hide the DataStax API behind a new repository abstraction.</text>
<rect class="section" x="62" y="145" width="2096" height="1010" rx="8"/><text class="sectionTitle" x="90" y="170">extension files and helper objects keep DataStax driver classes as the public contract</text>
<g id="edges">${edges.map((e) => `<path class="edge ${e.type}" d="${e.d}" stroke="${e.color}"/>`).join("\n")}</g><g id="arrowheads">${edges.map(arrowHead).join("\n")}</g><g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>${Object.keys(boxes).map(box).join("\n")}</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
