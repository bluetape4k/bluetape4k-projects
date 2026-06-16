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
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveSelectOperationSupport.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupport.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/query/CriteriaSupport.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/model/AbstractCassandraPersistable.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/model/AbstractCassandraAuditable.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGenerator.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Core Class Structure[\s\S]*spring-boot-cassandra-diagram-01\.png/, "README core class structure slot");
assertContains(sources[2], /suspend fun ReactiveSession\.executeSuspending[\s\S]*prepareSuspending/, "ReactiveSession coroutine extensions");
assertContains(sources[3], /ReactiveCassandraOperations\.selectAsFlow[\s\S]*selectOneSuspending[\s\S]*truncateSuspending/, "ReactiveCassandraOperations coroutine extensions");
assertContains(sources[4], /AsyncCassandraOperations\.executeSuspending[\s\S]*selectSuspending[\s\S]*SliceImpl/, "AsyncCassandraOperations coroutine extensions");
assertContains(sources[5], /ReactiveCassandraBatchOperations\.insertFlow[\s\S]*deleteFlow/, "batch Flow extensions");
assertContains(sources[6], /SelectWithProjection<\*>\.cast[\s\S]*TerminatingSelect<T>\.allSuspending/, "select operation extensions");
assertContains(sources[7], /queryOptions[\s\S]*writeOptions[\s\S]*addWriteOptions[\s\S]*isPositiveTtl/, "CQL options DSL");
assertContains(sources[8], /infix fun Criteria\.eq/, "criteria DSL");
assertContains(sources[9], /abstract class AbstractCassandraPersistable<PK:\s*Any>:\s*Persistable<PK>/, "persistable base class");
assertContains(sources[10], /abstract class AbstractCassandraAuditable<U:\s*Any,\s*PK:\s*Any>:\s*AbstractCassandraPersistable<PK>/, "auditable base class");
assertContains(sources[11], /object SchemaGenerator[\s\S]*createTableAndTypes[\s\S]*truncate/, "schema generator");

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
  <marker id="arrow-${name}" markerWidth="18" markerHeight="18" refX="15" refY="9" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 15 9 L 2 16" fill="none" stroke="${dark}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="triangle-${name}" markerWidth="24" markerHeight="22" refX="21" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 21 11 L 2 20 Z" fill="#FFFFFF" stroke="${dark}" stroke-width="2" stroke-dasharray="none"/></marker>`).join("\n");
}

function classBox({ id, x, y, w, h, color, stereotype, title, attrs = [], methods = [] }) {
  const [fill, stroke, dark] = palette[color];
  const attrY = y + 76;
  const methodY = attrY + 34 + Math.max(24, attrs.length * 22);
  return `<g id="${esc(id)}">
  <rect class="classBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="stereotype" x="${x + w / 2}" y="${y + 28}" text-anchor="middle">${esc(stereotype)}</text>
  <text class="classTitle" x="${x + w / 2}" y="${y + 58}" text-anchor="middle">${esc(title)}</text>
  <path class="divider" d="M${x} ${attrY}H${x + w}" stroke="${dark}"/>
  ${attrs.map((line, index) => `<text class="member" x="${x + 24}" y="${attrY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
  <path class="divider" d="M${x} ${methodY}H${x + w}" stroke="${dark}"/>
  ${methods.map((line, index) => `<text class="member" x="${x + 24}" y="${methodY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function noteBox({ id, x, y, w, h, color, title, lines }) {
  const [fill, stroke] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="noteBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="noteTitle" x="${x + 28}" y="${y + 48}">${esc(title)}</text>
  ${lines.map((line, index) => `<text class="noteLine" x="${x + 30}" y="${y + 88 + index * 28}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function edge({ from, to, points, color, marker = "arrow", dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#${marker}-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0] + 8}" y="${p[1] - 8}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2350;
const height = 1750;
const body = [
  noteBox({
    id: "ExternalApis",
    x: 760,
    y: 220,
    w: 1080,
    h: 170,
    color: "slate",
    title: "Wrapped Spring Data and DataStax APIs",
    lines: ["ReactiveSession, ReactiveCassandraOperations, AsyncCassandraOperations, ReactiveCassandraBatchOperations", "QueryOptions, WriteOptions, Criteria, CassandraOperations, mapping metadata"],
  }),
  classBox({
    id: "ReactiveSessionExtensions",
    x: 90,
    y: 500,
    w: 570,
    h: 280,
    color: "teal",
    stereotype: "<<extension set>>",
    title: "ReactiveSessionCoroutines",
    attrs: ["receiver: ReactiveSession", "bridge: Reactor Mono -> suspend"],
    methods: ["executeSuspending(String / args / Statement)", "prepareSuspending(String / SimpleStatement)", "returns ReactiveResultSet or PreparedStatement"],
  }),
  classBox({
    id: "ReactiveOpsExtensions",
    x: 760,
    y: 480,
    w: 720,
    h: 345,
    color: "green",
    stereotype: "<<extension set>>",
    title: "ReactiveCassandraOperationsCoroutines",
    attrs: ["receiver: ReactiveCassandraOperations", "bridges Flux to Flow and Mono to suspend"],
    methods: ["selectAsFlow(statement / cql / query)", "selectOne*, slice*, count*, exists*", "insert/update/delete/truncate suspending", "deleteByIdSuspending and executeSuspending"],
  }),
  classBox({
    id: "AsyncOpsExtensions",
    x: 1590,
    y: 500,
    w: 570,
    h: 280,
    color: "blue",
    stereotype: "<<extension set>>",
    title: "AsyncCassandraOperationsCoroutines",
    attrs: ["receiver: AsyncCassandraOperations", "bridge: CompletableFuture -> suspend"],
    methods: ["executeSuspending(statement)", "selectSuspending(statement / cql / query)", "selectOneOrNullSuspending", "sliceSuspending with empty Slice fallback"],
  }),
  classBox({
    id: "BatchSelectExtensions",
    x: 90,
    y: 930,
    w: 570,
    h: 285,
    color: "purple",
    stereotype: "<<extension set>>",
    title: "Batch and Select support",
    attrs: ["ReactiveCassandraBatchOperations", "ReactiveSelectOperation.TerminatingSelect"],
    methods: ["insertFlow / updateFlow / deleteFlow", "Flow is collected into Reactor mono", "cast(), count/exists/first/one/all suspending"],
  }),
  classBox({
    id: "OptionsDsl",
    x: 760,
    y: 945,
    w: 720,
    h: 295,
    color: "amber",
    stereotype: "<<DSL helpers>>",
    title: "CQL Options and Criteria DSL",
    attrs: ["QueryOptions / InsertOptions / UpdateOptions", "WriteOptions / DeleteOptions", "Criteria"],
    methods: ["queryOptions / insertOptions / updateOptions", "writeOptions / deleteOptions", "Insert/Update/Delete.addWriteOptions()", "Criteria.where(\"field\") eq value"],
  }),
  classBox({
    id: "SchemaGenerator",
    x: 1590,
    y: 930,
    w: 570,
    h: 300,
    color: "pink",
    stereotype: "<<object>>",
    title: "SchemaGenerator",
    attrs: ["receiver dependency: CassandraOperations", "uses SchemaFactory and mappingContext"],
    methods: ["createTableAndTypes<T>()", "potentiallyCreateTableFor<T>()", "potentiallyCreateUdtFor nested properties", "truncate<T>() only when table exists"],
  }),
  classBox({
    id: "Persistable",
    x: 760,
    y: 1390,
    w: 570,
    h: 255,
    color: "slate",
    stereotype: "<<abstract class>>",
    title: "AbstractCassandraPersistable<PK>",
    attrs: ["implements Persistable<PK>", "implements Serializable"],
    methods: ["setId(id): abstract", "isNew(): id == null", "equals/hashCode use non-null id"],
  }),
  classBox({
    id: "Auditable",
    x: 1530,
    y: 1375,
    w: 620,
    h: 285,
    color: "slate",
    stereotype: "<<abstract class>>",
    title: "AbstractCassandraAuditable<U,PK>",
    attrs: ["extends AbstractCassandraPersistable<PK>", "implements Auditable<U, PK, Instant>", "created/modified columns"],
    methods: ["isNew(): createdAt == null", "get/setCreatedBy and CreatedDate", "get/setLastModifiedBy and LastModifiedDate"],
  }),
  edge({ from: "ReactiveSessionExtensions", to: "ExternalApis", points: [[375, 500], [375, 420], [900, 420], [900, 390]], color: "teal", dashed: true, label: "wraps", labelAt: [565, 407] }),
  edge({ from: "ReactiveOpsExtensions", to: "ExternalApis", points: [[1120, 480], [1120, 390]], color: "green", dashed: true, label: "wraps", labelAt: [1138, 445] }),
  edge({ from: "AsyncOpsExtensions", to: "ExternalApis", points: [[1875, 500], [1875, 420], [1700, 420], [1700, 390]], color: "blue", dashed: true, label: "wraps", labelAt: [1795, 407] }),
  edge({ from: "BatchSelectExtensions", to: "ReactiveOpsExtensions", points: [[375, 930], [375, 870], [1120, 870], [1120, 825]], color: "purple", dashed: true, label: "same coroutine bridge family", labelAt: [620, 857] }),
  edge({ from: "SchemaGenerator", to: "ExternalApis", points: [[1875, 930], [1875, 840], [2250, 840], [2250, 305], [1840, 305]], color: "pink", dashed: true, label: "mapping metadata", labelAt: [1930, 828] }),
  edge({ from: "Auditable", to: "Persistable", points: [[1530, 1518], [1330, 1518]], color: "slate", marker: "triangle", dashed: false, label: "extends", labelAt: [1390, 1505] }),
  edge({ from: "OptionsDsl", to: "ReactiveOpsExtensions", points: [[1240, 945], [1240, 825]], color: "amber", dashed: true, label: "options for writes", labelAt: [1258, 900] }),
];

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Cassandra Core Class Structure Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .classBox,.noteBox{stroke-width:1.8;filter:url(#shadow)}.stereotype{font-family:"Comic Mono";font-size:14px;fill:#475569}.classTitle{font-family:"Architects Daughter";font-size:26px;fill:#0F172A}
    .member,.noteLine{font-family:"Comic Mono";font-size:14px;fill:#334155}.divider{stroke-width:1.1;opacity:.45}.noteTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">Spring Boot Cassandra Core Structure</text>
<text class="subtitle" x="76" y="120">Coroutine extension sets, CQL/query DSL helpers, Cassandra entity base classes, and schema utilities in the Spring Boot 4 Cassandra module.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "spring-boot-cassandra-diagram-01.svg");
const pngPath = join(OUT, "spring-boot-cassandra-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated spring-boot-cassandra-diagram-01.svg/png");
