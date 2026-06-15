#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "spring-boot/r2dbc/README.md",
  "spring-boot/r2dbc/README.ko.md",
  "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/R2dbcEntityOperationExtensions.kt",
  "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveSelectOperationExtensions.kt",
  "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveInsertOperationExtensions.kt",
  "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveUpdateOperationExtensions.kt",
  "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveDeleteOperationExtensions.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Core Class Structure[\s\S]*spring-boot-r2dbc-diagram-01\.png/, "README class structure slot");
assertContains(sources[2], /R2dbcEntityOperations\.findOneByIdSuspending[\s\S]*selectOneSuspending[\s\S]*findFirstByIdOrNullSuspending/, "entity id helper extensions");
assertContains(sources[3], /existsSuspending[\s\S]*countSuspending[\s\S]*selectSuspending[\s\S]*selectFirstOrNullSuspending/, "select extensions");
assertContains(sources[4], /ReactiveInsertOperation\.insertSuspending[\s\S]*insertOrNullSuspending/, "insert extensions");
assertContains(sources[5], /ReactiveUpdateOperation\.updateSuspending[\s\S]*awaitSingle/, "update extension");
assertContains(sources[6], /ReactiveDeleteOperation\.deleteSuspending[\s\S]*deleteAllSuspending/, "delete extensions");

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

function box({ id, x, y, w, h, color, stereotype, title, attrs = [], methods = [] }) {
  const [fill, stroke, dark] = palette[color];
  const attrY = y + 78;
  const methodY = attrY + 32 + Math.max(24, attrs.length * 22);
  return `<g id="${esc(id)}">
  <rect class="classBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="stereotype" x="${x + w / 2}" y="${y + 28}" text-anchor="middle">${esc(stereotype)}</text>
  <text class="classTitle" x="${x + w / 2}" y="${y + 59}" text-anchor="middle">${esc(title)}</text>
  <path class="divider" d="M${x} ${attrY}H${x + w}" stroke="${dark}"/>
  ${attrs.map((line, index) => `<text class="member" x="${x + 24}" y="${attrY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
  <path class="divider" d="M${x} ${methodY}H${x + w}" stroke="${dark}"/>
  ${methods.map((line, index) => `<text class="member" x="${x + 24}" y="${methodY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
</g>`;
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

const width = 2600;
const height = 1450;
const body = [
  box({
    id: "R2dbcEntityOperations",
    x: 760,
    y: 210,
    w: 670,
    h: 245,
    color: "slate",
    stereotype: "<<Spring Data receiver>>",
    title: "R2dbcEntityOperations",
    attrs: ["base receiver for repository-style helpers", "also exposes select/insert/update/delete operations"],
    methods: ["extension functions add suspend and Flow APIs", "no subclass is introduced by this module"],
  }),
  box({
    id: "CoroutineSurface",
    x: 1620,
    y: 210,
    w: 730,
    h: 245,
    color: "teal",
    stereotype: "<<coroutine adaptation>>",
    title: "Reactor -> Coroutine Surface",
    attrs: ["awaitSingle()", "awaitSingleOrNull()", "flow()"],
    methods: ["suspend returns: T, T?, Boolean, Long", "streaming returns: Flow<T>"],
  }),
  box({
    id: "IdHelpers",
    x: 120,
    y: 565,
    w: 610,
    h: 285,
    color: "green",
    stereotype: "<<extension file>>",
    title: "R2dbcEntityOperationExtensions",
    attrs: ["receiver: R2dbcEntityOperations", "builds Query.where(idName).isEqual(id)"],
    methods: ["findOneByIdSuspending(id)", "findOneByIdOrNullSuspending(id)", "findFirstByIdSuspending(id)", "findFirstByIdOrNullSuspending(id)"],
  }),
  box({
    id: "SelectExtensions",
    x: 910,
    y: 555,
    w: 650,
    h: 315,
    color: "blue",
    stereotype: "<<extension file>>",
    title: "ReactiveSelectOperationExtensions",
    attrs: ["receiver: R2dbcEntityOperations", "uses select<T>().matching(query)"],
    methods: ["existsSuspending(query), countSuspending(query)", "selectSuspending(query), selectAllSuspending()", "selectOne/First Suspense variants", "nullable variants use awaitSingleOrNull()"],
  }),
  box({
    id: "InsertExtensions",
    x: 120,
    y: 970,
    w: 610,
    h: 250,
    color: "amber",
    stereotype: "<<extension file>>",
    title: "ReactiveInsertOperationExtensions",
    attrs: ["receiver: ReactiveInsertOperation", "uses insert<T>().using(entity)"],
    methods: ["insertSuspending(entity): T", "insertOrNullSuspending(entity): T?"],
  }),
  box({
    id: "UpdateExtensions",
    x: 900,
    y: 970,
    w: 610,
    h: 250,
    color: "pink",
    stereotype: "<<extension file>>",
    title: "ReactiveUpdateOperationExtensions",
    attrs: ["receiver: ReactiveUpdateOperation", "uses update<T>().matching(query).apply(update)"],
    methods: ["updateSuspending(query, update): Long", "returns affected row count"],
  }),
  box({
    id: "DeleteExtensions",
    x: 1680,
    y: 970,
    w: 610,
    h: 250,
    color: "violet",
    stereotype: "<<extension file>>",
    title: "ReactiveDeleteOperationExtensions",
    attrs: ["receiver: ReactiveDeleteOperation", "uses delete<T>().matching(query).all()"],
    methods: ["deleteSuspending(query): Long", "deleteAllSuspending(): Long"],
  }),
  box({
    id: "QueryObjects",
    x: 1720,
    y: 610,
    w: 560,
    h: 235,
    color: "slate",
    stereotype: "<<Spring Data values>>",
    title: "Query / Criteria / Update",
    attrs: ["Query.empty()", "Criteria.where(...).isEqual(...)", "Update values supplied by caller"],
    methods: ["read helpers create Query", "update/delete/select consume caller Query"],
  }),

  edge({ from: "R2dbcEntityOperations", to: "IdHelpers", points: [[760, 335], [410, 335], [410, 565]], color: "green", dashed: true, label: "extension receiver", labelAt: [450, 317] }),
  edge({ from: "R2dbcEntityOperations", to: "SelectExtensions", points: [[1095, 455], [1095, 555]], color: "blue", dashed: true, label: "extension receiver", labelAt: [1115, 520] }),
  edge({ from: "IdHelpers", to: "SelectExtensions", points: [[730, 720], [910, 720]], color: "green", label: "delegates after Query creation", labelAt: [755, 699] }),
  edge({ from: "QueryObjects", to: "SelectExtensions", points: [[1720, 720], [1560, 720]], color: "slate", dashed: true, label: "query input", labelAt: [1600, 699] }),
  edge({ from: "SelectExtensions", to: "CoroutineSurface", points: [[1560, 640], [1700, 640], [1700, 455]], color: "teal", label: "await / Flow bridge", labelAt: [1588, 620] }),
];

const svg = `<svg data-intent="Explain Spring Boot R2DBC core coroutine extension structure for README diagram 01." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot R2DBC Core Class Structure Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:47px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .classBox{stroke-width:1.8;filter:url(#shadow)}.stereotype{font-family:"Comic Mono";font-size:14px;fill:#475569}.classTitle{font-family:"Architects Daughter";font-size:25px;fill:#0F172A}
    .member{font-family:"Comic Mono";font-size:14px;fill:#334155}.divider{stroke-width:1.1;opacity:.42}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">Spring Boot R2DBC Core Class Structure</text>
<text class="subtitle" x="76" y="120">Coroutine extension groups over Spring Data R2DBC receivers: read helpers, typed select, insert, update, delete, and Reactor adaptation.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "spring-boot-r2dbc-diagram-01.svg");
const pngPath = join(OUT, "spring-boot-r2dbc-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated spring-boot-r2dbc-diagram-01.svg/png");
