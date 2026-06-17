#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "spring-boot/r2dbc/README.md",
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

assertContains(sources[0], /CRUD Operation Hierarchy[\s\S]*spring-boot-r2dbc-diagram-03\.png/, "README CRUD hierarchy slot");
assertContains(sources[1], /findOneByIdSuspending[\s\S]*findFirstByIdOrNullSuspending/, "id helper functions");
assertContains(sources[2], /existsSuspending[\s\S]*selectAllSuspending[\s\S]*selectFirstOrNullSuspending/, "read function hierarchy");
assertContains(sources[3], /insertSuspending[\s\S]*insertOrNullSuspending/, "create function hierarchy");
assertContains(sources[4], /updateSuspending/, "update function hierarchy");
assertContains(sources[5], /deleteSuspending[\s\S]*deleteAllSuspending/, "delete function hierarchy");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  violet: ["#F5F3FF", "#7C3AED", "#6D28D9"],
  slate: ["#F8FAFC", "#64748B", "#475569"],
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
const height = 1360;
const columns = [
  { key: "Read", x: 100, color: "blue", title: "Read", functions: ["findOneById / findFirstById", "selectAll / select(query)", "selectOne / selectFirst", "exists / count"], delegate: "select<T>().matching(query)", returns: "Flow<T>, T, T?, Boolean, Long" },
  { key: "Update", x: 760, color: "pink", title: "Update", functions: ["updateSuspending(query, update)", "caller supplies Query", "caller supplies Update", "no entity instance is required"], delegate: "update<T>().matching(query).apply(update)", returns: "Long affected row count" },
  { key: "Create", x: 1420, color: "amber", title: "Create", functions: ["insertSuspending(entity)", "insertOrNullSuspending(entity)", "entity is passed to using(entity)", "nullable variant uses awaitSingleOrNull"], delegate: "insert<T>().using(entity)", returns: "T or T?" },
  { key: "Delete", x: 2080, color: "violet", title: "Delete", functions: ["deleteSuspending(query)", "deleteAllSuspending()", "Query.empty() for deleteAll", "deletes all matching rows"], delegate: "delete<T>().matching(query).all()", returns: "Long deleted row count" },
];

const body = [
  card({
    id: "Root",
    x: 730,
    y: 205,
    w: 1240,
    h: 230,
    color: "slate",
    kicker: "module surface",
    title: "Coroutine CRUD extensions over Spring Data R2DBC",
    lines: ["Adds suspend and Flow APIs without new repositories or subclasses", "Extension receivers are R2dbcEntityOperations plus Spring Data insert/update/delete operations", "Every branch keeps the XyzSuspending naming convention"],
    footer: "diagram groups functions by CRUD intent, not by source file order",
  }),
  ...columns.flatMap((column) => [
    card({
      id: `${column.key}Api`,
      x: column.x,
      y: 585,
      w: 520,
      h: 270,
      color: column.color,
      kicker: `${column.title.toLowerCase()} coroutine API`,
      title: `${column.title} functions`,
      lines: column.functions,
      footer: "public extension functions",
    }),
    card({
      id: `${column.key}Delegate`,
      x: column.x,
      y: 1010,
      w: 520,
      h: 230,
      color: column.color,
      kicker: "Spring Data delegation",
      title: column.delegate,
      lines: ["Reactive command is built by Spring Data", "Result is adapted with flow or await APIs"],
      footer: `returns ${column.returns}`,
    }),
  ]),
  edge({ from: "Root", to: "ReadApi", points: [[970, 435], [970, 505], [360, 505], [360, 585]], color: "blue", label: "query side", labelAt: [625, 486] }),
  edge({ from: "Root", to: "UpdateApi", points: [[1180, 435], [1180, 505], [1020, 505], [1020, 585]], color: "pink", label: "mutation side", labelAt: [1068, 486] }),
  edge({ from: "Root", to: "CreateApi", points: [[1520, 435], [1520, 505], [1680, 505], [1680, 585]], color: "amber", label: "insert side", labelAt: [1558, 486] }),
  edge({ from: "Root", to: "DeleteApi", points: [[1780, 435], [1780, 505], [2340, 505], [2340, 585]], color: "violet", label: "delete side", labelAt: [2025, 486] }),
  ...columns.map((column) => edge({
    from: `${column.key}Api`,
    to: `${column.key}Delegate`,
    points: [[column.x + 260, 855], [column.x + 260, 1010]],
    color: column.color,
    dashed: true,
    label: "delegates to",
    labelAt: [column.x + 278, 940],
  })),
];

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot R2DBC CRUD Operation Hierarchy Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:47px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .card{stroke-width:1.8;filter:url(#shadow)}.kicker{font-family:"Comic Mono";font-size:14px;fill:#475569}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:14px;fill:#334155}.foot{font-family:"Comic Mono";font-size:13px;fill:#475569}.divider{stroke-width:1.1;opacity:.42}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">R2DBC CRUD Operation Hierarchy</text>
<text class="subtitle" x="76" y="120">The module groups coroutine extensions by CRUD intent, then delegates each group to the matching Spring Data R2DBC fluent operation.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "spring-boot-r2dbc-diagram-03.svg");
const pngPath = join(OUT, "spring-boot-r2dbc-diagram-03.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated spring-boot-r2dbc-diagram-03.svg/png");
