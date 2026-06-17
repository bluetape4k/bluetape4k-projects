#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";
import { join } from "node:path";

const diagramId = process.argv[2];
if (!["01", "02", "03"].includes(diagramId)) {
  throw new Error("Pass exactly one diagram id: 01, 02, or 03");
}

const out = join(process.cwd(), "docs/images/readme-diagrams");
const cairosvg = process.env.CAIROSVG ?? "/Users/debop/.local/bin/cairosvg";

const palette = {
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  purple: ["#FAF5FF", "#9333EA", "#7E22CE"],
  slate: ["#F8FAFC", "#64748B", "#334155"],
};

function esc(value) {
  return String(value).replace(/[&<>"']/g, (ch) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "\"": "&quot;",
    "'": "&apos;",
  }[ch]));
}

function svgShell({ title, subtitle, width = 1700, height = 1120, body }) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title)}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/></filter>
  ${Object.entries(palette).map(([name, [, stroke]]) => `<marker id="arrow-${name}" markerWidth="13" markerHeight="13" refX="11" refY="6.5" orient="auto" markerUnits="userSpaceOnUse"><path d="M 0 0 L 13 6.5 L 0 13 Z" fill="${stroke}" stroke="${stroke}" stroke-width="1" stroke-dasharray="none"/></marker>`).join("\n  ")}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F6F9FC}.frame{fill:#fff;stroke:#CBD5E1;stroke-width:3;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .band{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:2}.bandTitle{font-family:"Architects Daughter";font-size:23px;fill:#334155}
    .card{filter:url(#shadow);stroke-width:2}.cardTitle{font-family:"Architects Daughter";font-size:25px;fill:#0F172A}.detail{font-family:"Comic Mono";font-size:14px;fill:#475569}
    .mono{font-family:"Comic Mono";font-size:13px;fill:#475569}.edge{fill:none;stroke-width:3.6;stroke-linecap:round;stroke-linejoin:round}
    .edgeDashed{stroke-dasharray:9 8}.note{fill:#F8FAFC;stroke:#CBD5E1;stroke-width:1.8}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="40" y="34" width="${width - 80}" height="${height - 74}" rx="30"/>
<text class="title" x="82" y="92">${esc(title)}</text>
<text class="subtitle" x="86" y="126">${esc(subtitle)}</text>
${body}
</svg>`;
}

function band(x, y, w, h, title, description = "") {
  const desc = description ? `<text class="mono" x="${x + 360}" y="${y + 36}" dominant-baseline="middle">${esc(description)}</text>` : "";
  return `<g><rect class="band" x="${x}" y="${y}" width="${w}" height="${h}" rx="16"/><text class="bandTitle" x="${x + 26}" y="${y + 36}" dominant-baseline="middle">${esc(title)}</text>${desc}</g>`;
}

function card(id, x, y, w, h, title, lines, color) {
  const [fill, stroke] = palette[color];
  const text = [
    `<text class="cardTitle" x="${x + w / 2}" y="${y + 34}" text-anchor="middle" dominant-baseline="middle">${esc(title)}</text>`,
    ...lines.map((line, index) => `<text class="detail" x="${x + w / 2}" y="${y + 66 + index * 20}" text-anchor="middle" dominant-baseline="middle">${esc(line)}</text>`),
  ].join("\n");
  return `<g id="${esc(id)}"><rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="12" fill="${fill}" stroke="${stroke}"/>\n${text}</g>`;
}

function edge(points, color, dashed = false) {
  const [, stroke] = palette[color];
  const d = points.map(([x, y], index) => `${index === 0 ? "M" : "L"}${x} ${y}`).join(" ");
  return `<path class="edge${dashed ? " edgeDashed" : ""}" d="${d}" stroke="${stroke}" marker-end="url(#arrow-${color})"/>`;
}

function label(x, y, text, color) {
  const [, stroke] = palette[color];
  return `<g><rect x="${x}" y="${y}" width="${text.length * 8 + 28}" height="26" rx="8" fill="#fff" stroke="#CBD5E1" stroke-width="1.4"/><text class="mono" x="${x + 14}" y="${y + 17}" fill="${stroke}">${esc(text)}</text></g>`;
}

function render01() {
  const body = [
    band(90, 168, 1520, 185, "Spring Data receiver"),
    band(90, 430, 1520, 255, "Kotlin extension surfaces"),
    band(90, 755, 1520, 315, "Objects handed to Spring Data MongoDB"),
    card("receiver", 735, 230, 480, 88, "ReactiveMongoOperations", ["receiver for coroutine extension functions"], "blue"),
    card("criteriaDsl", 120, 508, 320, 104, "Criteria DSL", ["criteria(), eq/gt/inValues", "andWith / orWith"], "green"),
    card("queryDsl", 480, 508, 320, 104, "Query extensions", ["queryOf(), sortAscBy()", "paginate(), limitTo(), skipTo()"], "amber"),
    card("coroutines", 840, 500, 390, 120, "Coroutine operation extensions", ["Flux<T> -> Flow<T>", "Mono<T> -> suspend / nullable", "writes await result metadata"], "teal"),
    card("updateDsl", 1310, 508, 250, 104, "Update DSL", ["setTo, incBy", "andSet / andInc"], "pink"),
    card("springTypes", 300, 810, 1080, 104, "Spring Data query objects", ["Criteria, Query, Update", "used by find/update/remove operations"], "purple"),
    card("driver", 650, 945, 520, 104, "Reactive Mongo driver", ["Flux/Mono publisher results", "collection command metadata"], "green"),
    edge([[975, 318], [1035, 500]], "blue"),
    edge([[280, 612], [280, 735], [520, 735], [520, 810]], "green"),
    edge([[640, 612], [640, 810]], "amber"),
    edge([[1435, 612], [1435, 735], [1240, 735], [1240, 810]], "pink"),
    edge([[1035, 620], [1035, 810]], "teal"),
    edge([[840, 914], [840, 945]], "purple"),
    label(1058, 404, "extension receiver", "blue"),
    label(650, 705, "builds query inputs", "purple"),
  ].join("\n");
  return svgShell({
    title: "MongoDB Coroutine Extension Structure",
    subtitle: "ReactiveMongoOperations is the receiver; coroutine operation helpers and DSL builders stay separated by responsibility.",
    height: 1180,
    body,
  });
}

function render02() {
  const body = [
    band(90, 168, 1520, 175, "Caller builds an operation", "Application code chooses a query, aggregation, entity write, or collection command."),
    band(90, 425, 1520, 265, "Reactive publisher boundary", "ReactiveMongoOperations returns Flux or Mono publishers; extensions convert them at this boundary."),
    band(90, 765, 1520, 230, "Coroutine-facing result", "Callers receive Flow streams, suspend values, or write metadata without handling publishers directly."),
    card("caller", 150, 226, 360, 86, "Repository/service code", ["chooses read, write, or collection operation"], "blue"),
    card("query", 670, 226, 360, 86, "Query / Aggregation", ["Spring Data MongoDB query objects"], "purple"),
    card("operations", 1190, 226, 360, 86, "ReactiveMongoOperations", ["returns Flux<T>, Mono<T>, or Mono<Result>"], "green"),
    card("many", 170, 502, 360, 116, "Many-result extensions", ["findAsFlow, findAllAsFlow", "aggregateAsFlow, tailAsFlow", "publisher.asFlow()"], "teal"),
    card("single", 670, 502, 360, 116, "Single-result extensions", ["findOneSuspending", "findByIdOrNullSuspending", "awaitSingle / awaitSingleOrNull"], "amber"),
    card("write", 1170, 502, 360, 116, "Write extensions", ["insert/save/update/remove", "await UpdateResult/DeleteResult"], "pink"),
    card("flow", 170, 832, 360, 92, "Flow<T>", ["streamed documents"], "teal"),
    card("suspend", 670, 832, 360, 92, "suspend result", ["T, T?, Long, Boolean, Unit"], "amber"),
    card("metadata", 1170, 832, 360, 92, "Write metadata", ["UpdateResult / DeleteResult"], "pink"),
    edge([[510, 269], [670, 269]], "blue"),
    edge([[1030, 269], [1190, 269]], "purple"),
    edge([[1370, 312], [1370, 380], [350, 380], [350, 502]], "teal"),
    edge([[1370, 312], [1370, 400], [850, 400], [850, 502]], "amber"),
    edge([[1370, 312], [1370, 502]], "pink"),
    edge([[350, 618], [350, 832]], "teal"),
    edge([[850, 618], [850, 832]], "amber"),
    edge([[1350, 618], [1350, 832]], "pink"),
    label(368, 394, "Flux<T>", "teal"),
    label(866, 414, "Mono<T>", "amber"),
    label(1378, 402, "Mono<Result>", "pink"),
  ].join("\n");
  return svgShell({
    title: "ReactiveMongoOperations Coroutine Flow",
    subtitle: "Reactive Mongo publishers are converted into Flow streams, suspend values, or write metadata.",
    body,
  });
}

function render03() {
  const body = [
    band(90, 168, 1520, 190, "Field-level DSL input", "String field helpers start either predicate construction or update document construction."),
    band(90, 445, 1520, 265, "Spring Data query objects", "Criteria becomes Query; Update stays separate until an update/remove operation consumes it."),
    band(90, 785, 1520, 210, "Execution", "Consumes built Query and Update objects."),
    card("fields", 150, 238, 360, 96, "Field strings", ["\"age\".criteria()", "\"name\" setTo value"], "blue"),
    card("criteriaDsl", 660, 238, 380, 108, "Criteria infix DSL", ["eq, gt, inValues", "andWith / orWith"], "green"),
    card("updateDsl", 1190, 238, 360, 108, "Update DSL", ["setTo, incBy, pushValue", "andSet / andInc"], "pink"),
    card("criteria", 170, 522, 360, 96, "Criteria", ["predicate tree"], "green"),
    card("query", 670, 522, 360, 96, "Query", ["queryOf + sort + paginate"], "amber"),
    card("update", 1170, 522, 360, 96, "Update", ["Mongo update document"], "pink"),
    card("operations", 610, 850, 500, 96, "ReactiveMongoOperations", ["find/update/remove executes Query and Update objects"], "purple"),
    edge([[510, 286], [660, 286]], "blue"),
    edge([[1040, 286], [1190, 286]], "pink", true),
    edge([[850, 346], [850, 420], [350, 420], [350, 522]], "green"),
    edge([[530, 570], [670, 570]], "amber"),
    edge([[1350, 346], [1350, 522]], "pink"),
    edge([[850, 618], [850, 850]], "amber"),
    edge([[1350, 618], [1350, 742], [980, 742], [980, 850]], "pink"),
    label(541, 248, "criteria()", "blue"),
    label(1058, 248, "field update", "pink"),
    label(878, 676, "query object", "amber"),
  ].join("\n");
  return svgShell({
    title: "Criteria / Query / Update DSL Flow",
    subtitle: "Field helpers build Criteria, Query, and Update objects before ReactiveMongoOperations executes them.",
    body,
  });
}

const renderers = { "01": render01, "02": render02, "03": render03 };
const file = `spring-boot-mongodb-diagram-${diagramId}`;
const svg = renderers[diagramId]();
const svgPath = join(out, `${file}.svg`);
const pngPath = join(out, `${file}.png`);
writeFileSync(svgPath, svg);
execFileSync(cairosvg, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${file}.svg/png`);
