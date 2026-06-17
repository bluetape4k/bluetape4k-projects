#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/data-mongodb-diagram-02";
const W = 1800;
const H = 1060;
const c = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  blue: "#2563EB",
  green: "#16A34A",
  purple: "#9333EA",
  orange: "#EA580C",
};

const sources = [
  "data/mongodb/README.md",
  "data/mongodb/README.ko.md",
  "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientSupport.kt",
  "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientProvider.kt",
  "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientExtensions.kt",
  "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensions.kt",
  "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensions.kt",
  "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensions.kt",
  "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupport.kt",
];
for (const source of sources) {
  if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
}
if (!/Module API Structure[\s\S]*data-mongodb-diagram-02\.png/.test(readFileSync(sources[0], "utf8"))) {
  throw new Error("README diagram slot not found");
}

const entry = [
  { x: 135, y: 285, w: 350, h: 92, fill: "#EFF6FF", stroke: c.blue, title: "Client creation", sub: "mongoClient, mongoClientOf, provider" },
  { x: 535, y: 285, w: 350, h: 92, fill: "#F0FDF4", stroke: c.green, title: "Database access", sub: "typed collections + names as List" },
  { x: 935, y: 285, w: 350, h: 92, fill: "#FAF5FF", stroke: c.purple, title: "Collection shortcuts", sub: "first, exists, upsert, findAsFlow" },
  { x: 1335, y: 285, w: 350, h: 92, fill: "#FFF7ED", stroke: c.orange, title: "Document + pipeline", sub: "BSON document and stage builders" },
];

const api = [
  {
    x: 135, y: 570, w: 350, h: 230, fill: "#EFF6FF", stroke: c.blue, title: "Client API",
    lines: ["mongoClient { Builder.() }", "mongoClientOf(url) { ... }", "MongoClientProvider cache", "listDatabaseNamesAsList()", "withClientSession / inTransaction"],
  },
  {
    x: 535, y: 570, w: 350, h: 230, fill: "#F0FDF4", stroke: c.green, title: "Database API",
    lines: ["getCollectionOf<T>(name)", "listCollectionNamesList()", "keeps native MongoDatabase", "no schema registry layer", "no collection wrapper type"],
  },
  {
    x: 935, y: 570, w: 350, h: 230, fill: "#FAF5FF", stroke: c.purple, title: "Collection API",
    lines: ["findFirst(filter)", "exists(filter)", "upsert(filter, update)", "findAsFlow(filter, sort, page)", "native CRUD remains suspend"],
  },
  {
    x: 1335, y: 570, w: 350, h: 230, fill: "#FFF7ED", stroke: c.orange, title: "DSL API",
    lines: ["documentOf(key to value)", "documentOf { put(...) }", "Document.getAs<T>(key)", "pipeline { add(stage) }", "match/group/sort/limit/skip"],
  },
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
    if (/^(false|true|null)$/.test(token)) color = "#C2410C";
    else if (/^(val|fun|return|suspend|inline|object)$/.test(token)) color = "#9333EA";
    else if (/^(Builder|MongoClientProvider|MongoDatabase|Document|MongoCollection|T|List)$/.test(token)) color = "#7C3AED";
    else if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(token) && next.startsWith("(")) color = "#2563EB";
    else if (/^[{}()=|./,<>\[\]]+$/.test(token)) color = "#64748B";
    return { token, color };
  });
}

function codeLine(text, centerX, y) {
  const parts = codeParts(text);
  const tokenWidth = (token) => {
    if (/^\s+$/.test(token)) return token.length * 3.9;
    if (/^[{}()=|./,<>\[\]]+$/.test(token)) return token.length * 5.25;
    return token.length * 6.55;
  };
  const width = parts.reduce((sum, part) => sum + tokenWidth(part.token), 0);
  let x = centerX - width / 2;
  return `<g>${parts.map((part) => {
    const item = `<text class="code" x="${x}" y="${y}" fill="${part.color}">${esc(part.token)}</text>`;
    x += tokenWidth(part.token);
    return item;
  }).join("")}</g>`;
}

function entryCard(card) {
  return `<g><rect class="card" x="${card.x}" y="${card.y}" width="${card.w}" height="${card.h}" rx="8" fill="${card.fill}" stroke="${card.stroke}"/><text class="entryTitle" x="${card.x + card.w / 2}" y="${card.y + 38}" text-anchor="middle">${esc(card.title)}</text><text class="sub" x="${card.x + card.w / 2}" y="${card.y + 68}" text-anchor="middle">${esc(card.sub)}</text></g>`;
}

function apiCard(card) {
  return `<g><rect class="card" x="${card.x}" y="${card.y}" width="${card.w}" height="${card.h}" rx="8" fill="${card.fill}" stroke="${card.stroke}"/><text class="cardTitle" x="${card.x + card.w / 2}" y="${card.y + 39}" text-anchor="middle">${esc(card.title)}</text><line x1="${card.x}" y1="${card.y + 62}" x2="${card.x + card.w}" y2="${card.y + 62}" stroke="${card.stroke}" stroke-opacity=".32"/>${card.lines.map((line, index) => codeLine(line, card.x + card.w / 2, card.y + 92 + index * 27)).join("")}</g>`;
}

const defs = [c.blue, c.green, c.purple, c.orange].map(marker).join("");
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="bluetape4k MongoDB module API structure">
<defs><filter id="shadow" x="-8%" y="-8%" width="116%" height="118%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity=".11"/></filter>${defs}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.6;filter:url(#shadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${c.ink}}.subtitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}.lane{fill:#F8FAFC;stroke:${c.line};stroke-width:1.5}.laneTitle{font-family:"Architects Daughter";font-size:24px;fill:${c.ink}}.card{filter:url(#shadow);stroke-width:1.9}.entryTitle{font-family:"Architects Daughter";font-size:24px;fill:${c.ink}}.cardTitle{font-family:"Architects Daughter";font-size:25px;fill:${c.ink}}.sub{font-family:"Comic Mono";font-size:12.5px;fill:#64748B}.code{font-family:"Comic Mono";font-size:12.2px;font-weight:700}.flow{fill:none;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="10"/>
<text class="title" x="72" y="84">bluetape4k-mongodb Module API Structure</text>
<text class="subtitle" x="76" y="116">Choose the smallest helper around the native coroutine driver API you already use.</text>
<rect class="lane" x="70" y="165" width="1660" height="285" rx="10"/>
<text class="laneTitle" x="104" y="215">API entrypoints</text>
${entry.map(entryCard).join("")}
<rect class="lane" x="70" y="500" width="1660" height="365" rx="10"/>
<text class="laneTitle" x="104" y="550">What each helper adds</text>
${api.map(apiCard).join("")}
${entry.map((card, index) => `<path class="flow" d="M${card.x + card.w / 2} ${card.y + card.h} L${card.x + card.w / 2} 570" stroke="${card.stroke}" marker-end="url(#arrow-${card.stroke.replace("#", "")})"/>`).join("")}
<rect class="lane" x="70" y="900" width="1660" height="90" rx="10"/>
<text class="laneTitle" x="104" y="955">Intentionally excluded</text>
<text class="sub" x="515" y="930">No wrappers for native suspend CRUD: insertOne, updateOne, deleteOne, createIndex, dropIndex</text>
<text class="sub" x="515" y="956">No custom filter/sort/update string DSL: use MongoDB Kotlin extensions for type-safe KProperty queries</text>
<text class="sub" x="515" y="982">No aggregateAsFlow(): native aggregate(pipeline) already returns Flow-compatible AggregateFlow&lt;T&gt;</text>
</svg>`;

writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
