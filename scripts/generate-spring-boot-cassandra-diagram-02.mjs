#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "spring-boot/cassandra/README.md",
  "spring-boot/cassandra/README.ko.md",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveSessionCoroutines.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveCassandraOperationsCoroutines.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/AsyncCassandraOperationsCoroutines.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveCassandraBatchOperationsCoroutines.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupport.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGenerator.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Cassandra Data Access Layer[\s\S]*spring-boot-cassandra-diagram-02\.png/, "README data access diagram slot");
assertContains(sources[2], /ReactiveSession\.executeSuspending[\s\S]*prepareSuspending/, "ReactiveSession bridge");
assertContains(sources[3], /selectAsFlow[\s\S]*insertSuspending[\s\S]*truncateSuspending/, "Reactive operations bridge");
assertContains(sources[4], /AsyncCassandraOperations\.executeSuspending[\s\S]*selectSuspending/, "Async operations bridge");
assertContains(sources[5], /insertFlow[\s\S]*updateFlow[\s\S]*deleteFlow/, "batch Flow bridge");
assertContains(sources[6], /writeOptions[\s\S]*addWriteOptions[\s\S]*isPositiveTtl/, "write option DSL");
assertContains(sources[7], /createTableAndTypes[\s\S]*potentiallyCreateTableFor[\s\S]*truncate/, "schema utilities");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  purple: ["#F5F3FF", "#7C3AED", "#6D28D9"],
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

function lane({ x, y, w, h, title }) {
  return `<g>
  <rect class="lane" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/>
  <text class="laneTitle" x="${x + 28}" y="${y + 42}">${esc(title)}</text>
</g>`;
}

function card({ id, x, y, w, h, color, title, lines = [] }) {
  const [fill, stroke] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="cardTitle" x="${x + 26}" y="${y + 44}">${esc(title)}</text>
  ${lines.map((line, index) => `<text class="line" x="${x + 28}" y="${y + 82 + index * 27}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function edge({ from, to, points, color, dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0] + 8}" y="${p[1] - 8}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2500;
const height = 1460;
const body = [
  lane({ x: 70, y: 190, w: 2360, h: 190, title: "Application layer" }),
  lane({ x: 70, y: 435, w: 2360, h: 285, title: "bluetape4k coroutine and DSL layer" }),
  lane({ x: 70, y: 775, w: 2360, h: 265, title: "Spring Data Cassandra layer" }),
  lane({ x: 70, y: 1095, w: 2360, h: 270, title: "DataStax driver and Cassandra" }),
  card({
    id: "AppCode",
    x: 160,
    y: 250,
    w: 520,
    h: 100,
    color: "teal",
    title: "Service / Repository code",
    lines: ["suspend functions, Flow collection, repository calls"],
  }),
  card({
    id: "Entities",
    x: 860,
    y: 250,
    w: 520,
    h: 100,
    color: "slate",
    title: "Cassandra entities",
    lines: ["@Table, @PrimaryKey, optional Persistable/Auditable base"],
  }),
  card({
    id: "SchemaUse",
    x: 1580,
    y: 250,
    w: 650,
    h: 100,
    color: "pink",
    title: "Schema bootstrap",
    lines: ["createTableAndTypes<T>(), truncate<T>() when tests/tools need it"],
  }),
  card({
    id: "CoroutineBridge",
    x: 150,
    y: 505,
    w: 610,
    h: 155,
    color: "green",
    title: "Coroutine extension bridge",
    lines: ["ReactiveSession.executeSuspending / prepareSuspending", "ReactiveCassandraOperations selectAsFlow / *Suspending", "AsyncCassandraOperations future.await() bridge"],
  }),
  card({
    id: "BatchBridge",
    x: 905,
    y: 505,
    w: 520,
    h: 155,
    color: "purple",
    title: "Batch and select helpers",
    lines: ["insertFlow / updateFlow / deleteFlow", "TerminatingSelect count/exists/first/one/all suspending", "Flow is collected into Reactor mono"],
  }),
  card({
    id: "OptionsDsl",
    x: 1580,
    y: 505,
    w: 650,
    h: 155,
    color: "amber",
    title: "CQL options and query DSL",
    lines: ["queryOptions / insertOptions / writeOptions", "Insert/Update/Delete.addWriteOptions()", "Criteria.where(\"field\") eq value"],
  }),
  card({
    id: "SpringOps",
    x: 165,
    y: 840,
    w: 610,
    h: 140,
    color: "green",
    title: "ReactiveCassandraOperations",
    lines: ["Mono/Flux based CRUD, select, count, exists, truncate", "maps Statement, Query, Update, entity operations"],
  }),
  card({
    id: "BatchOps",
    x: 905,
    y: 840,
    w: 520,
    h: 140,
    color: "purple",
    title: "ReactiveCassandraBatchOperations",
    lines: ["batch insert/update/delete", "uses WriteOptions when supplied"],
  }),
  card({
    id: "CqlOps",
    x: 1580,
    y: 840,
    w: 650,
    h: 140,
    color: "amber",
    title: "CQL builders and mapping metadata",
    lines: ["QueryOptions, WriteOptions, Criteria, Query, Update", "SchemaFactory and Cassandra mapping context"],
  }),
  card({
    id: "ReactiveSession",
    x: 180,
    y: 1160,
    w: 560,
    h: 135,
    color: "blue",
    title: "ReactiveSession",
    lines: ["execute(statement), prepare(statement)", "returns ReactiveResultSet / PreparedStatement"],
  }),
  card({
    id: "AsyncDriver",
    x: 960,
    y: 1160,
    w: 520,
    h: 135,
    color: "blue",
    title: "Async driver path",
    lines: ["AsyncCassandraOperations", "CompletableFuture and AsyncResultSet"],
  }),
  card({
    id: "Cassandra",
    x: 1700,
    y: 1160,
    w: 480,
    h: 135,
    color: "slate",
    title: "Apache Cassandra",
    lines: ["CQL execution, table metadata, keyspace tables"],
  }),
  edge({ from: "AppCode", to: "CoroutineBridge", points: [[350, 350], [350, 505]], color: "green", label: "suspend API", labelAt: [368, 430] }),
  edge({ from: "AppCode", to: "BatchBridge", points: [[430, 350], [430, 405], [1165, 405], [1165, 505]], color: "purple", dashed: true, label: "Flow batches", labelAt: [760, 392] }),
  edge({ from: "AppCode", to: "OptionsDsl", points: [[510, 350], [510, 390], [1905, 390], [1905, 505]], color: "amber", dashed: true, label: "options DSL", labelAt: [1280, 377] }),
  edge({ from: "Entities", to: "SchemaUse", points: [[1380, 300], [1580, 300]], color: "pink", dashed: true, label: "entity metadata", labelAt: [1430, 284] }),
  edge({ from: "CoroutineBridge", to: "SpringOps", points: [[455, 660], [455, 840]], color: "green", label: "await/asFlow", labelAt: [473, 760] }),
  edge({ from: "BatchBridge", to: "BatchOps", points: [[1165, 660], [1165, 840]], color: "purple", label: "collect Flow", labelAt: [1183, 760] }),
  edge({ from: "OptionsDsl", to: "CqlOps", points: [[1905, 660], [1905, 840]], color: "amber", label: "builds", labelAt: [1923, 760] }),
  edge({ from: "SchemaUse", to: "CqlOps", points: [[2230, 300], [2320, 300], [2320, 910], [2230, 910]], color: "pink", dashed: true, label: "SchemaGenerator", labelAt: [2240, 742] }),
  edge({ from: "SpringOps", to: "ReactiveSession", points: [[455, 980], [455, 1160]], color: "blue", label: "session calls", labelAt: [473, 1080] }),
  edge({ from: "SpringOps", to: "AsyncDriver", points: [[775, 910], [845, 910], [845, 1228], [960, 1228]], color: "blue", dashed: true, label: "async variants", labelAt: [858, 1080] }),
  edge({ from: "BatchOps", to: "SpringOps", points: [[905, 910], [775, 910]], color: "purple", dashed: true, label: "same template", labelAt: [805, 892] }),
  edge({ from: "CqlOps", to: "Cassandra", points: [[1905, 980], [1905, 1160]], color: "amber", label: "CQL", labelAt: [1923, 1080] }),
  edge({ from: "ReactiveSession", to: "Cassandra", points: [[460, 1295], [460, 1335], [1940, 1335], [1940, 1295]], color: "blue", label: "execute prepared/simple statements", labelAt: [980, 1318] }),
  edge({ from: "AsyncDriver", to: "Cassandra", points: [[1480, 1228], [1700, 1228]], color: "blue", dashed: true, label: "AsyncResultSet", labelAt: [1530, 1208] }),
];

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Cassandra Data Access Layer Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .lane{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.3;stroke-dasharray:9 7}.laneTitle{font-family:"Architects Daughter";font-size:25px;fill:#0F172A}
    .card{stroke-width:1.8;filter:url(#shadow)}.cardTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}.line{font-family:"Comic Mono";font-size:14px;fill:#334155}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">Spring Boot Cassandra Data Access Layer</text>
<text class="subtitle" x="76" y="120">How application code moves through bluetape4k coroutine extensions, Spring Data Cassandra operations, DataStax driver APIs, and Cassandra schema utilities.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "spring-boot-cassandra-diagram-02.svg");
const pngPath = join(OUT, "spring-boot-cassandra-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated spring-boot-cassandra-diagram-02.svg/png");
