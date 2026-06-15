#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "spring-boot/r2dbc/README.md",
  "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveSelectOperationExtensions.kt",
  "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveInsertOperationExtensions.kt",
  "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveUpdateOperationExtensions.kt",
  "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveDeleteOperationExtensions.kt",
  "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/R2dbcEntityOperationExtensions.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /R2DBC \+ Coroutines Data Flow[\s\S]*spring-boot-r2dbc-diagram-02\.png/, "README data flow slot");
assertContains(sources[1], /select<T>\(\)\.matching\(query\)\.flow\(\)[\s\S]*awaitSingleOrNull/, "select flow and await adapters");
assertContains(sources[2], /insert<T>\(\)\.using\(entity\)\.awaitSingle\(\)/, "insert await path");
assertContains(sources[3], /update<T>\(\)\.matching\(query\)\.apply\(update\)\.awaitSingle\(\)/, "update await path");
assertContains(sources[4], /delete<T>\(\)\.matching\(query\)\.all\(\)\.awaitSingle\(\)/, "delete await path");
assertContains(sources[5], /Query\.query\(Criteria\.where\(idName\)\.isEqual\(id\)\)/, "id query creation");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  slate: ["#F8FAFC", "#64748B", "#475569"],
  violet: ["#F5F3FF", "#7C3AED", "#6D28D9"],
};

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function markerDefs() {
  return Object.entries(palette).map(([name, [, , dark]]) => `
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 19 11 L 2 20 Z" fill="${dark}"/></marker>`).join("\n");
}

function card({ id, x, y, w, h, color, kicker, title, lines = [], footer = "" }) {
  const [fill, stroke, dark] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="kicker" x="${x + 22}" y="${y + 31}">${esc(kicker)}</text>
  <text class="cardTitle" x="${x + 22}" y="${y + 64}">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 86}H${x + w}" stroke="${dark}"/>
  ${lines.map((line, index) => `<text class="body" x="${x + 22}" y="${y + 118 + index * 24}">${esc(line)}</text>`).join("\n")}
  ${footer ? `<path class="divider" d="M${x} ${y + h - 46}H${x + w}" stroke="${dark}"/><text class="foot" x="${x + 22}" y="${y + h - 17}">${esc(footer)}</text>` : ""}
</g>`;
}

function lane({ x, y, text, color }) {
  const [, , dark] = palette[color];
  return `<text class="lane" x="${x}" y="${y}" fill="${dark}">${esc(text)}</text>`;
}

function edge({ from, to, points, color, dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0]}" y="${p[1]}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2700;
const height = 1540;
const body = [
  lane({ x: 96, y: 195, text: "caller input", color: "slate" }),
  card({
    id: "Caller",
    x: 110,
    y: 230,
    w: 430,
    h: 240,
    color: "slate",
    kicker: "service / repository",
    title: "Coroutine caller",
    lines: ["passes Query, id, entity, or Update", "calls *Suspending extension functions", "collects Flow<T> only when needed"],
    footer: "domain code stays suspend/Flow based",
  }),
  card({
    id: "IdQuery",
    x: 660,
    y: 230,
    w: 470,
    h: 240,
    color: "green",
    kicker: "id helper path",
    title: "Query creation",
    lines: ["Criteria.where(idName).isEqual(id)", "Query.query(criteria)", "then delegates to select helpers"],
    footer: "used by findOneById and findFirstById",
  }),
  card({
    id: "Database",
    x: 2200,
    y: 230,
    w: 360,
    h: 240,
    color: "slate",
    kicker: "R2DBC driver",
    title: "Relational database",
    lines: ["SQL is executed by Spring Data R2DBC", "rows and row counts return reactively", "driver remains non-blocking"],
    footer: "external I/O boundary",
  }),

  lane({ x: 96, y: 605, text: "read path", color: "blue" }),
  card({
    id: "ReadExtensions",
    x: 110,
    y: 640,
    w: 520,
    h: 250,
    color: "blue",
    kicker: "R2dbcEntityOperations extensions",
    title: "select / exists / count",
    lines: ["selectSuspending(query): Flow<T>", "selectOne/First variants: T or T?", "existsSuspending, countSuspending"],
    footer: "read API names stay XyzSuspending",
  }),
  card({
    id: "ReadFluent",
    x: 760,
    y: 640,
    w: 520,
    h: 250,
    color: "green",
    kicker: "Spring Data fluent API",
    title: "select<T>().matching(query)",
    lines: ["awaitExists() for exists", "awaitCount() for count", "one()/first() for scalar reads"],
    footer: "the query is immutable input",
  }),
  card({
    id: "ReadAdapter",
    x: 1410,
    y: 640,
    w: 520,
    h: 250,
    color: "teal",
    kicker: "reactor bridge",
    title: "flow / await adapters",
    lines: ["flow() converts multi-row Publisher", "awaitSingle() requires one value", "awaitSingleOrNull() maps empty to null"],
    footer: "Kotlin API boundary",
  }),

  lane({ x: 96, y: 1015, text: "write path", color: "amber" }),
  card({
    id: "WriteExtensions",
    x: 110,
    y: 1050,
    w: 520,
    h: 250,
    color: "amber",
    kicker: "insert / update / delete extensions",
    title: "write operations",
    lines: ["insertSuspending(entity)", "updateSuspending(query, update)", "deleteSuspending(query)"],
    footer: "all wait for completion explicitly",
  }),
  card({
    id: "WriteFluent",
    x: 760,
    y: 1050,
    w: 520,
    h: 250,
    color: "pink",
    kicker: "Spring Data fluent API",
    title: "mutation pipelines",
    lines: ["insert<T>().using(entity)", "update<T>().matching(query).apply(update)", "delete<T>().matching(query).all()"],
    footer: "Spring Data builds the reactive command",
  }),
  card({
    id: "WriteAdapter",
    x: 1410,
    y: 1050,
    w: 520,
    h: 250,
    color: "teal",
    kicker: "reactor bridge",
    title: "awaitSingle result",
    lines: ["insert returns saved entity", "update returns affected row count", "delete returns deleted row count"],
    footer: "suspends without blocking a thread",
  }),

  edge({ from: "Caller", to: "IdQuery", points: [[540, 350], [660, 350]], color: "slate", label: "id helpers", labelAt: [570, 329] }),
  edge({ from: "IdQuery", to: "ReadExtensions", points: [[895, 470], [895, 545], [370, 545], [370, 640]], color: "green", dashed: true, label: "delegates as Query", labelAt: [510, 526] }),
  edge({ from: "Caller", to: "ReadExtensions", points: [[325, 470], [325, 640]], color: "blue", dashed: true, label: "read calls", labelAt: [245, 575] }),
  edge({ from: "ReadExtensions", to: "ReadFluent", points: [[630, 765], [760, 765]], color: "blue", label: "select<T>()", labelAt: [660, 744] }),
  edge({ from: "ReadFluent", to: "ReadAdapter", points: [[1280, 765], [1410, 765]], color: "teal", label: "Publisher", labelAt: [1315, 744] }),
  edge({ from: "ReadAdapter", to: "Database", points: [[1930, 765], [2090, 765], [2090, 350], [2200, 350]], color: "slate", dashed: true, label: "non-blocking driver I/O", labelAt: [1975, 735] }),

  edge({ from: "Caller", to: "WriteExtensions", points: [[185, 470], [72, 470], [72, 1175], [110, 1175]], color: "amber", dashed: true, label: "write calls", labelAt: [90, 990] }),
  edge({ from: "WriteExtensions", to: "WriteFluent", points: [[630, 1175], [760, 1175]], color: "amber", label: "build mutation", labelAt: [657, 1154] }),
  edge({ from: "WriteFluent", to: "WriteAdapter", points: [[1280, 1175], [1410, 1175]], color: "teal", label: "Publisher", labelAt: [1315, 1154] }),
  edge({ from: "WriteAdapter", to: "Database", points: [[1930, 1175], [2140, 1175], [2140, 420], [2200, 420]], color: "slate", dashed: true, label: "non-blocking driver I/O", labelAt: [1968, 1135] }),
];

const svg = `<svg data-intent="Explain Spring Boot R2DBC coroutine data flow for README diagram 02." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot R2DBC Coroutines Data Flow Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:47px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .card{stroke-width:1.8;filter:url(#shadow)}.kicker{font-family:"Comic Mono";font-size:14px;fill:#475569}.cardTitle{font-family:"Architects Daughter";font-size:25px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:14px;fill:#334155}.foot{font-family:"Comic Mono";font-size:13px;fill:#475569}.divider{stroke-width:1.1;opacity:.42}
    .lane{font-family:"Comic Mono";font-size:16px;font-weight:700;letter-spacing:0}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">R2DBC + Coroutines Data Flow</text>
<text class="subtitle" x="76" y="120">Read and write extension functions translate coroutine-friendly calls into Spring Data R2DBC fluent operations, then adapt Reactor results back to Flow or suspend values.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "spring-boot-r2dbc-diagram-02.svg");
const pngPath = join(OUT, "spring-boot-r2dbc-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated spring-boot-r2dbc-diagram-02.svg/png");
