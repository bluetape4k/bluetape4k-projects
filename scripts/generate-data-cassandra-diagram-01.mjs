#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/data-cassandra-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/data-cassandra-diagram-01.png";
const W = 2140;
const H = 1135;
const c = {
  ink: "#0F172A", muted: "#475569", canvas: "#F8FAFC", frame: "#FFFFFF", line: "#CBD5E1",
  blue: "#2563EB", teal: "#0D9488", green: "#16A34A", orange: "#EA580C", purple: "#7C3AED", gray: "#64748B",
};

const sources = [
  "data/cassandra/README.md",
  "data/cassandra/README.ko.md",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/GettableSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/SettableSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/QueryBuilderSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt",
];
for (const source of sources) if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
const readme = readFileSync(sources[0], "utf8");
if (!/Extension Function API Overview[\s\S]*data-cassandra-diagram-01\.png/.test(readme)) throw new Error("README diagram slot not found");

const cards = {
  user: { x: 80, y: 155, w: 340, h: 172, fill: "#EFF6FF", stroke: c.blue, title: "Kotlin Caller", icon: "{}", body: ["uses extension functions", "keeps driver objects visible"] },
  session: { x: 535, y: 155, w: 385, h: 172, fill: "#EFF6FF", stroke: c.blue, title: "Session DSL", icon: "S", body: ["cqlSession { ... }", "cqlSessionOf(...)"] },
  statements: { x: 1035, y: 155, w: 400, h: 172, fill: "#FFF7ED", stroke: c.orange, title: "Statement Builders", icon: "Q", body: ["statementOf / simpleStatementOf", "boundStatementOf / batchStatementOf"] },
  driver: { x: 1550, y: 155, w: 430, h: 172, fill: "#F8FAFC", stroke: c.gray, title: "DataStax Driver", icon: "D", body: ["CqlSession / AsyncCqlSession", "Statement / Row / ResultSet"] },
  coroutine: { x: 230, y: 485, w: 420, h: 180, fill: "#F0FDFA", stroke: c.teal, title: "Coroutine Bridge", icon: "↯", body: ["executeSuspending(...)", "prepareSuspending(...)", "awaits driver futures"] },
  row: { x: 795, y: 485, w: 430, h: 180, fill: "#ECFDF5", stroke: c.green, title: "Row Mapping Helpers", icon: "R", body: ["toMap / toNamedMap", "mapWithCqlIdentifier", "codec-aware decoding"] },
  values: { x: 1370, y: 485, w: 430, h: 180, fill: "#ECFDF5", stroke: c.green, title: "Typed Value Access", icon: "T", body: ["getValue / getList / getMap", "setValue / setList / setMap", "name, id, or index based"] },
  querybuilder: { x: 380, y: 825, w: 440, h: 185, fill: "#F5F3FF", stroke: c.purple, title: "QueryBuilder Extensions", icon: "B", body: ["bindMarker / raw / udt", "relation and term helpers", "schema/query DSL stays driver-native"] },
  admin: { x: 1040, y: 825, w: 450, h: 185, fill: "#FEF2F2", stroke: "#DC2626", title: "Admin Utilities", icon: "A", body: ["create/drop keyspace", "read system.local release_version", "validates keyspace names"] },
};

const flows = [
  { color: c.blue, d: "M420 241 L535 241", label: ["creates", 477, 216] },
  { color: c.orange, d: "M920 241 L1035 241", label: ["builds", 978, 216] },
  { color: c.gray, d: "M1435 241 L1550 241", label: ["native", 1492, 216] },
  { color: c.teal, d: "M650 575 L700 575 L700 365 L1685 365 L1685 327", label: ["await futures", 1105, 340] },
  { color: c.green, d: "M1225 575 L1370 575", label: ["typed columns", 1298, 550] },
  { color: c.green, d: "M1585 485 L1585 385 L1765 385 L1765 327", label: ["reads Row data", 1645, 365] },
  { color: c.purple, d: "M600 825 L600 735 L1265 735 L1265 327", label: ["query DSL output", 895, 710] },
  { color: "#DC2626", d: "M1265 825 L1265 755 L2025 755 L2025 241 L1980 241", label: ["schema/query admin", 1620, 730] },
  { color: c.teal, d: "M420 327 L420 485", label: ["suspend usage", 505, 405] },
  { color: c.green, d: "M1035 327 L1035 485", label: ["result helpers", 1128, 405] },
];

function esc(v) { return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;"); }
function card(k) {
  const b = cards[k];
  return `<g id="${k}"><rect class="card" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/><rect class="icon" x="${b.x + 24}" y="${b.y + 28}" width="48" height="48" rx="8" fill="#fff" stroke="${b.stroke}"/><text class="iconText" x="${b.x + 48}" y="${b.y + 60}" text-anchor="middle" fill="${b.stroke}">${esc(b.icon)}</text><text class="cardTitle" x="${b.x + 92}" y="${b.y + 45}">${esc(b.title)}</text>${b.body.map((line, i) => `<text class="body" x="${b.x + 92}" y="${b.y + 78 + i * 25}">${esc(line)}</text>`).join("")}</g>`;
}
function arrow(color) {
  const id = color.replace("#", "a");
  return `<marker id="${id}" markerUnits="userSpaceOnUse" markerWidth="18" markerHeight="14" refX="17" refY="7" orient="auto"><path d="M1 1 L17 7 L1 13 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`;
}
function label(text, x, y) {
  const w = Math.max(74, text.length * 8.3 + 18);
  return `<g transform="translate(${x - w / 2} ${y - 16})"><rect width="${w}" height="28" rx="8" fill="#fff" stroke="${c.line}" opacity=".96"/><text class="label" x="${w / 2}" y="19" text-anchor="middle">${esc(text)}</text></g>`;
}

const markerDefs = [...new Set(flows.map((f) => f.color))].map(arrow).join("");
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Cassandra extension function API overview">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/></filter>${markerDefs}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}.subtitle,.sectionTitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}.section{fill:#F3F8FF;stroke:#94A3B8;stroke-width:1.6;stroke-dasharray:12 8}.card{stroke-width:2;filter:url(#softShadow)}.icon{stroke-width:1.5}.iconText{font-family:"Architects Daughter";font-size:25px}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${c.ink}}.body{font-family:"Comic Mono";font-size:13.2px;fill:${c.muted}}.flow{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.label{font-family:"Comic Mono";font-size:12px;fill:${c.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/>
<text class="title" x="76" y="86">Cassandra Extension Function API Overview</text><text class="subtitle" x="78" y="118">The module wraps DataStax driver primitives with Kotlin builders, coroutine awaits, typed row access, QueryBuilder helpers, and small admin utilities.</text>
<rect class="section" x="60" y="135" width="2020" height="900" rx="8"/><text class="sectionTitle" x="88" y="160">source-backed extension families; arrows point to the driver primitive each family creates, awaits, or reads</text>
<g transform="translate(0 45)"><g id="flows">${flows.map((f) => `<path class="flow" d="${f.d}" stroke="${f.color}" marker-end="url(#${f.color.replace("#", "a")})"/>`).join("\n")}</g>
<g id="labels">${flows.map((f) => label(f.label[0], f.label[1], f.label[2])).join("\n")}</g>
${Object.keys(cards).map(card).join("\n")}</g>
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
