#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/data-mongodb-diagram-03";
const W = 1800;
const H = 1080;
const intent = "Explain how bluetape4k aggregation helpers build ordered BSON stages before the native MongoDB coroutine driver executes aggregate as a Flow.";
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
};

const sources = [
  "data/mongodb/README.md",
  "data/mongodb/README.ko.md",
  "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupport.kt",
];
for (const source of sources) {
  if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
}
if (!/Aggregation Pipeline Data Flow[\s\S]*data-mongodb-diagram-03\.png/.test(readFileSync(sources[0], "utf8"))) {
  throw new Error("README diagram slot not found");
}

const top = [
  { x: 130, y: 300, w: 330, h: 122, fill: "#EFF6FF", stroke: c.blue, title: "pipeline { ... }", lines: ["MutableList<Bson> builder", "caller appends stages"] },
  { x: 570, y: 300, w: 330, h: 122, fill: "#F0FDF4", stroke: c.green, title: "List<Bson> stages", lines: ["match, group, sort, limit", "skip, project, unwind"] },
  { x: 985, y: 300, w: 330, h: 122, fill: "#FAF5FF", stroke: c.purple, title: "collection.aggregate", lines: ["native driver API", "no aggregateAsFlow wrapper"] },
  { x: 1390, y: 300, w: 330, h: 122, fill: "#FFF7ED", stroke: c.orange, title: "AggregateFlow<T>", lines: ["already implements Flow<T>", "collect / toList downstream"] },
];

const helpers = [
  { x: 145, y: 710, w: 240, h: 108, fill: "#EFF6FF", stroke: c.blue, title: "matchStage", lines: ["Aggregates.match(filter)"] },
  { x: 430, y: 710, w: 240, h: 108, fill: "#F0FDF4", stroke: c.green, title: "groupStage", lines: ['group("$id", fields)'] },
  { x: 715, y: 710, w: 240, h: 108, fill: "#FAF5FF", stroke: c.purple, title: "sortStage", lines: ["Aggregates.sort(sort)"] },
  { x: 1000, y: 710, w: 240, h: 108, fill: "#FFF7ED", stroke: c.orange, title: "limit / skip", lines: ["page stream source"] },
  { x: 1285, y: 710, w: 240, h: 108, fill: "#F0FDFA", stroke: c.teal, title: "projectStage", lines: ["projection Bson"] },
  { x: 1560, y: 710, w: 130, h: 108, fill: "#FDF2F8", stroke: c.pink, title: "unwind", lines: ['"$field"'] },
];

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function escText(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function marker(id, color) {
  return `<marker id="${id}" markerWidth="15" markerHeight="14" refX="13" refY="7" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 14 7 L 1 13 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`;
}

function codeParts(text) {
  const tokens = text.split(/(\s+|[{}()=|./,<>"$]+)/).filter(Boolean);
  return tokens.map((token, index) => {
    const next = tokens.slice(index + 1).find((part) => !/^\s+$/.test(part)) ?? "";
    let color = "#0F172A";
    if (/^(MutableList|Bson|List|AggregateFlow|Flow|T)$/.test(token)) color = "#7C3AED";
    else if (/^(match|group|sort|limit|skip|project|unwind|collect|toList)$/.test(token)) color = "#0D9488";
    else if (/^(Aggregates|collection)$/.test(token)) color = "#9333EA";
    else if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(token) && next.startsWith("(")) color = "#2563EB";
    else if (/^[{}()=|./,<>"$]+$/.test(token)) color = "#64748B";
    return { token, color };
  });
}

function codeLine(text, centerX, y) {
  const parts = codeParts(text);
  const tokenWidth = (token) => {
    if (/^\s+$/.test(token)) return token.length * 3.9;
    if (/^[{}()=|./,<>"$]+$/.test(token)) return token.length * 5.25;
    return token.length * 6.75;
  };
  const width = parts.reduce((sum, part) => sum + tokenWidth(part.token), 0);
  let x = centerX - width / 2;
  return `<g>${parts.map((part) => {
    const item = `<text class="code" x="${x}" y="${y}"><tspan class="syntax-token" fill="${part.color}">${escText(part.token)}</tspan></text>`;
    x += tokenWidth(part.token);
    return item;
  }).join("")}</g>`;
}

function card(card, large = false) {
  return `<g><rect class="card" x="${card.x}" y="${card.y}" width="${card.w}" height="${card.h}" rx="8" fill="${card.fill}" stroke="${card.stroke}"/><text class="${large ? "topTitle" : "helperTitle"}" x="${card.x + card.w / 2}" y="${card.y + (large ? 42 : 38)}" text-anchor="middle">${esc(card.title)}</text>${card.lines.map((line, index) => codeLine(line, card.x + card.w / 2, card.y + (large ? 76 : 74) + index * 27)).join("")}</g>`;
}

const sourceEvidence = sources.join("; ");
const svg = `<svg data-intent="${esc(intent)}" data-evidence="${esc(sourceEvidence)}" data-source-read="${esc(sourceEvidence)}" xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="MongoDB aggregation pipeline data flow">
<defs><filter id="shadow" x="-8%" y="-8%" width="116%" height="118%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity=".11"/></filter>${marker("flowArrow", c.blue)}${marker("helperArrow", c.green)}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.6;filter:url(#shadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${c.ink}}.subtitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}.lane{fill:#F8FAFC;stroke:${c.line};stroke-width:1.5}.laneTitle{font-family:"Architects Daughter";font-size:24px;fill:${c.ink}}.card{filter:url(#shadow);stroke-width:1.9}.topTitle{font-family:"Architects Daughter";font-size:24px;fill:${c.ink}}.helperTitle{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}.code{font-family:"Comic Mono";font-size:12.4px;font-weight:700}.syntax-token{fill:${c.ink}}.sub{font-family:"Comic Mono";font-size:12.5px;fill:#64748B}.flow{fill:none;stroke:${c.blue};stroke-width:4;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#flowArrow)}.helper{fill:none;stroke:${c.green};stroke-width:3.6;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#helperArrow)}.boundary{fill:none;stroke:${c.green};stroke-width:2.7;stroke-dasharray:8 8}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="10"/>
<text class="title" x="72" y="84">MongoDB Aggregation Pipeline Data Flow</text>
<text class="subtitle" x="76" y="116">bluetape4k builds the stage list; the MongoDB coroutine driver executes aggregate(...) as Flow.</text>
<rect class="lane" x="70" y="165" width="1660" height="390" rx="10"/>
<text class="laneTitle" x="104" y="215">Pipeline composition and execution boundary</text>
${top.map((item) => card(item, true)).join("")}
<path class="flow" d="M460 361 L570 361"/><path class="flow" d="M900 361 L985 361"/><path class="flow" d="M1315 361 L1390 361"/>
<rect class="lane" x="70" y="600" width="1660" height="270" rx="10"/>
<text class="laneTitle" x="104" y="650">Stage helper functions</text>
<rect class="boundary" x="105" y="680" width="1590" height="165" rx="10"/>
${helpers.map((item) => card(item, false)).join("")}
<path class="helper" d="M900 680 L900 575 L735 575 L735 422"/>
<text class="sub" x="680" y="836">stage helpers return Bson values that the builder appends in caller-defined order</text>
<rect class="lane" x="70" y="905" width="1660" height="100" rx="10"/>
<text class="laneTitle" x="104" y="960">Design contract</text>
<text class="sub" x="350" y="935">The DSL only composes ordered Bson stages; it does not own execution, cursor lifecycle, or collection typing.</text>
<text class="sub" x="350" y="965">Execute with native aggregate(stages), then use Flow operators such as collect(), toList(), map(), or filter().</text>
</svg>`;

writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
