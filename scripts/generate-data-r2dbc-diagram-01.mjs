#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/data-r2dbc-diagram-01";
const W = 1800;
const H = 910;
const c = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  blue: "#2563EB",
  green: "#16A34A",
  teal: "#0D9488",
  orange: "#EA580C",
  purple: "#9333EA",
  pink: "#DB2777",
  lime: "#65A30D",
};

const sources = [
  "data/r2dbc/README.md",
  "data/r2dbc/README.ko.md",
  "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/DatabaseClientBuilder.kt",
  "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/DatabaseClientSupport.kt",
  "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/ParameterSupport.kt",
  "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/ReadableSupport.kt",
  "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/TransactionSupport.kt",
  "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupport.kt",
  "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/R2dbcPoolConfig.kt",
];
for (const source of sources) {
  if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
}
if (!/Extension Function API Overview[\s\S]*data-r2dbc-diagram-01\.png/.test(readFileSync(sources[0], "utf8"))) {
  throw new Error("README diagram slot not found");
}

const lanes = [
  { x: 75, y: 165, w: 390, h: 670, title: "Client receiver", note: "Pool and DatabaseClient creation.", color: c.blue },
  { x: 505, y: 165, w: 390, h: 670, title: "Execution spec receiver", note: "SQL specs and parameter binding.", color: c.teal },
  { x: 935, y: 165, w: 390, h: 670, title: "Fetch and row receivers", note: "Flow, suspend values, and typed columns.", color: c.purple },
  { x: 1365, y: 165, w: 360, h: 670, title: "Transaction boundary", note: "TransactionalOperator-backed suspend blocks.", color: c.pink },
];

const cards = {
  repo: { x: 120, y: 285, w: 300, h: 112, fill: "#EFF6FF", stroke: c.blue, title: "Repository code", body: ["suspend functions", "Flow callers"] },
  pool: { x: 120, y: 465, w: 300, h: 112, fill: "#F0FDF4", stroke: c.green, title: "Connection pool DSL", body: [{ code: "r2dbcConnectionPool(url)" }, { code: "highThroughput()" }] },
  client: { x: 120, y: 660, w: 300, h: 112, fill: "#F0FDFA", stroke: c.teal, title: "DatabaseClient", body: [{ code: "databaseClient(factory)" }, "Spring R2DBC receiver"] },

  sql: { x: 550, y: 285, w: 300, h: 122, fill: "#FFF7ED", stroke: c.orange, title: "SQL entry helpers", body: [{ code: "execute(sql)" }, { code: "sqlInsert / sqlUpdate" }, { code: "sqlDelete" }] },
  spec: { x: 550, y: 480, w: 300, h: 112, fill: "#FAF5FF", stroke: c.purple, title: "GenericExecuteSpec", body: ["Spring execution spec", "fetch, map, await/flow"] },
  bind: { x: 550, y: 665, w: 300, h: 132, fill: "#F0FDFA", stroke: c.teal, title: "Parameter binding", body: [{ code: "bindMap(parameters)" }, { code: "bindIndexedMap(parameters)" }, { code: "bindNullable<T>()" }] },

  fetch: { x: 980, y: 285, w: 300, h: 112, fill: "#FAF5FF", stroke: c.purple, title: "Fetch coroutine helpers", body: [{ code: "flow { row, meta -> ... }" }, { code: "awaitSingle()" }] },
  readable: { x: 980, y: 480, w: 300, h: 112, fill: "#F7FEE7", stroke: c.lime, title: "Readable typed access", body: [{ code: "getAs<T>(name)" }, { code: "int / uuid / localDateTime" }] },
  result: { x: 980, y: 665, w: 300, h: 112, fill: "#EFF6FF", stroke: c.blue, title: "Coroutine results", body: [{ code: "Flow<T>" }, { code: "T? / List<T> / Long" }] },

  tx: { x: 1405, y: 285, w: 280, h: 122, fill: "#FDF2F8", stroke: c.pink, title: "Transaction support", body: [{ code: "withTransactionSuspend { tx }" }, "commit / rollback by Spring"] },
  manager: { x: 1405, y: 480, w: 280, h: 112, fill: "#F9FAFB", stroke: c.muted, title: "Manager cache", body: ["ConnectionFactory key", "WeakHashMap + lock"] },
  contract: { x: 1405, y: 665, w: 280, h: 112, fill: "#FFF7ED", stroke: c.orange, title: "Module contract", body: ["Does not replace Spring R2DBC", "adds Kotlin convenience"] },
};

const flows = [
  { color: c.green, d: "M270 397 L270 465", label: "pool", x: 305, y: 432 },
  { color: c.teal, d: "M270 577 L270 660", label: "client", x: 310, y: 620 },
  { color: c.orange, d: "M420 716 L465 716 Q485 716 485 696 L485 366 Q485 346 505 346 L550 346", label: "sql", x: 440, y: 530 },
  { color: c.purple, d: "M700 407 L700 480", label: "spec", x: 735, y: 445 },
  { color: c.teal, d: "M700 592 L700 665", label: "bind", x: 735, y: 630 },
  { color: c.purple, d: "M850 536 L910 536 Q930 536 930 516 L930 361 Q930 341 950 341 L980 341", label: "fetch", x: 885, y: 440 },
  { color: c.lime, d: "M1130 397 L1130 480", label: "row", x: 1164, y: 440 },
  { color: c.blue, d: "M1130 592 L1130 665", label: "result", x: 1172, y: 630 },
  { color: c.pink, d: "M1545 407 L1545 480", label: "manager", x: 1588, y: 445, dash: true },
  { color: c.orange, d: "M1545 592 L1545 665", label: "contract", x: 1594, y: 630, dash: true },
];

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function marker(color) {
  const id = `arrow-${color.replace("#", "")}`;
  return `<marker id="${id}" markerWidth="15" markerHeight="14" refX="13" refY="7" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 14 7 L 1 13 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`;
}

function codeParts(text) {
  const tokens = text.split(/(\s+|[{}()=|./,<>\[\]]+)/).filter(Boolean);
  return tokens.map((token, index) => {
    const next = tokens.slice(index + 1).find((part) => !/^\s+$/.test(part)) ?? "";
    let color = "#0F172A";
    let css = "";
    if (/^(false|true|null|suspend|inline|fun|val)$/.test(token)) {
      color = "#9333EA";
      css = "syntax-keyword";
    } else if (/^(GenericExecuteSpec|DatabaseClient|Flow|ConnectionFactory|T|List|Long)$/.test(token)) {
      color = "#7C3AED";
      css = "syntax-type";
    } else if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(token) && next.startsWith("(")) {
      color = "#2563EB";
      css = "syntax-function";
    } else if (/^[{}()=|./,<>\[\]]+$/.test(token)) {
      color = "#64748B";
      css = "syntax-operator";
    }
    return { token, color, css };
  });
}

function codeLine(text, centerX, y) {
  const parts = codeParts(text);
  const estimatedWidth = [...text].reduce((width, char) => {
    if (/[A-Z]/.test(char)) return width + 8.1;
    if (/[a-z0-9]/.test(char)) return width + 7.2;
    if (/\s/.test(char)) return width + 4.2;
    return width + 6.8;
  }, 0);
  return `<text class="code" x="${centerX - estimatedWidth / 2}" y="${y}">${parts.map((part) => part.css
    ? `<tspan class="${part.css}" fill="${part.color}">${esc(part.token)}</tspan>`
    : esc(part.token)).join("")}</text>`;
}

function card(key) {
  const b = cards[key];
  return `<g><rect class="card" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/><text class="cardTitle" x="${b.x + b.w / 2}" y="${b.y + 38}" text-anchor="middle">${esc(b.title)}</text>${b.body.map((line, index) => {
    const y = b.y + 70 + index * 24;
    if (typeof line === "object") return codeLine(line.code, b.x + b.w / 2, y);
    return `<text class="detail" x="${b.x + b.w / 2}" y="${y}" text-anchor="middle">${esc(line)}</text>`;
  }).join("")}</g>`;
}

function label(text, x, y) {
  const w = Math.max(76, text.length * 8 + 22);
  return `<g transform="translate(${x - w / 2} ${y - 14})"><rect width="${w}" height="27" rx="8" fill="#fff" stroke="${c.line}" opacity=".97"/><text class="edgeLabel" x="${w / 2}" y="18" text-anchor="middle">${esc(text)}</text></g>`;
}

const defs = [...new Set(flows.map((flow) => flow.color))].map(marker).join("");
const svg = `<svg data-intent="Map each Spring R2DBC receiver to the bluetape4k coroutine, binding, typed-row, pooling, and transaction conveniences that repository code can use." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="R2DBC extension function API overview">
<defs><filter id="shadow" x="-8%" y="-8%" width="116%" height="118%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity=".11"/></filter>${defs}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.6;filter:url(#shadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${c.ink}}.subtitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}.lane{fill:#F8FAFC;stroke:${c.line};stroke-width:1.5}.laneTitle{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}.laneNote{font-family:"Comic Mono";font-size:12.2px;fill:#64748B}.card{filter:url(#shadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}.detail{font-family:"Comic Mono";font-size:12.4px;fill:${c.muted}}.code{font-family:"Comic Mono";font-size:12px;font-weight:700}.syntax-keyword{fill:#9333EA}.syntax-type{fill:#7C3AED}.syntax-function{fill:#2563EB}.syntax-operator{fill:#64748B}.edge{fill:none;stroke-width:3.6;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:8 7}.edgeLabel{font-family:"Comic Mono";font-size:12.1px;fill:${c.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="10"/>
<text class="title" x="72" y="84">R2DBC Extension Function API Overview</text>
<text class="subtitle" x="76" y="116">Vertical receiver map: each column shows where bluetape4k adds Kotlin convenience around Spring R2DBC.</text>
${lanes.map((lane) => `<g><rect class="layer lane" x="${lane.x}" y="${lane.y}" width="${lane.w}" height="${lane.h}" rx="10"/><text class="laneTitle layerTitle" x="${lane.x + 28}" y="${lane.y + 42}">${esc(lane.title)}</text><text class="laneNote" x="${lane.x + 28}" y="${lane.y + 72}">${esc(lane.note)}</text></g>`).join("")}
<g>${flows.map((flow) => `<path class="edge${flow.dash ? " dashed" : ""}" d="${flow.d}" stroke="${flow.color}" marker-end="url(#arrow-${flow.color.replace("#", "")})"/>`).join("")}</g>
<g>${flows.map((flow) => label(flow.label, flow.x, flow.y)).join("")}</g>
${Object.keys(cards).map(card).join("")}
</svg>`;

writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
