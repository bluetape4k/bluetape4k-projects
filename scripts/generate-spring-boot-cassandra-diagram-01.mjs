#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const root = process.cwd();
const outDir = join(root, "docs/images/readme-diagrams");
const cairosvg = process.env.CAIROSVG ?? "/Users/debop/.local/bin/cairosvg";

const sources = [
  "spring-boot/cassandra/README.md",
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
  if (!existsSync(join(root, source))) throw new Error(`Missing source: ${source}`);
}

function requireSource(index, pattern, label) {
  const text = readFileSync(join(root, sources[index]), "utf8");
  if (!pattern.test(text)) throw new Error(`Missing ${label}`);
}

requireSource(0, /Core Extension and Class Structure[\s\S]*spring-boot-cassandra-diagram-01\.png/, "README diagram slot");
requireSource(1, /ReactiveSession\.executeSuspending[\s\S]*prepareSuspending/, "ReactiveSession extensions");
requireSource(2, /ReactiveCassandraOperations\.selectAsFlow[\s\S]*truncateSuspending/, "ReactiveCassandraOperations extensions");
requireSource(3, /AsyncCassandraOperations\.executeSuspending[\s\S]*selectSuspending/, "AsyncCassandraOperations extensions");
requireSource(4, /ReactiveCassandraBatchOperations\.insertFlow[\s\S]*deleteFlow/, "batch extensions");
requireSource(5, /SelectWithProjection<\*>\.cast[\s\S]*TerminatingSelect<T>\.allSuspending/, "select operation extensions");
requireSource(6, /writeOptions[\s\S]*addWriteOptions[\s\S]*isPositiveTtl/, "options DSL");
requireSource(7, /Criteria\.where/, "criteria DSL");
requireSource(8, /abstract class AbstractCassandraPersistable<PK:\s*Any>:\s*Persistable<PK>/, "persistable base");
requireSource(9, /abstract class AbstractCassandraAuditable<U:\s*Any,\s*PK:\s*Any>:\s*AbstractCassandraPersistable<PK>/, "auditable base");
requireSource(10, /object SchemaGenerator[\s\S]*createTableAndTypes[\s\S]*truncate/, "schema generator");

const font = "'Architects Daughter', 'Comic Mono', 'Helvetica Neue', Arial, sans-serif";

function esc(value) {
  return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function textLines(items, x, y, { size = 13, fill = "#475569", line = 18, anchor = "start", weight = 400 } = {}) {
  return items.map((item, i) => `<text x="${x}" y="${y + i * line}" text-anchor="${anchor}" font-size="${size}" font-weight="${weight}" fill="${fill}">${esc(item)}</text>`).join("\n");
}

function classCard({ x, y, w, h, name, stereo, attrs = [], ops = [], fill, stroke }) {
  const nameLines = String(name).split("\n");
  const headerH = 34 + nameLines.length * 20;
  const attrH = attrs.length ? Math.max(34, attrs.length * 18 + 18) : 0;
  const opY = y + headerH + attrH;
  return `
  <g class="card" data-card="${esc(nameLines.join(" "))}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    <text x="${x + w / 2}" y="${y + 21}" text-anchor="middle" font-size="12.3" fill="${stroke}">${esc(stereo)}</text>
    ${textLines(nameLines, x + w / 2, y + 43, { size: 16.5, weight: 700, fill: "#111827", line: 20, anchor: "middle" })}
    <line x1="${x}" y1="${y + headerH}" x2="${x + w}" y2="${y + headerH}" stroke="${stroke}" stroke-width="1.1" opacity="0.55"/>
    ${attrH ? `<line x1="${x}" y1="${opY}" x2="${x + w}" y2="${opY}" stroke="${stroke}" stroke-width="1.1" opacity="0.45"/>` : ""}
    ${attrs.length ? textLines(attrs, x + 16, y + headerH + 23, { size: 12.5, fill: "#475569", line: 18 }) : ""}
    ${ops.length ? textLines(ops, x + 16, opY + 23, { size: 12.5, fill: "#374151", line: 18 }) : ""}
  </g>`;
}

function edge({ d, color, marker, width = 2.4, dash = "" }) {
  return `<path class="edge" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})"/>`;
}

function defs() {
  return `
  <defs>
    <marker id="open-blue" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#2563eb" stroke-width="1.8" stroke-dasharray="none" stroke-linecap="round" stroke-linejoin="round"/></marker>
    <marker id="open-green" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#16a34a" stroke-width="1.8" stroke-dasharray="none" stroke-linecap="round" stroke-linejoin="round"/></marker>
    <marker id="open-orange" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#ea580c" stroke-width="1.8" stroke-dasharray="none" stroke-linecap="round" stroke-linejoin="round"/></marker>
    <marker id="open-purple" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#7c3aed" stroke-width="1.8" stroke-dasharray="none" stroke-linecap="round" stroke-linejoin="round"/></marker>
    <marker id="open-pink" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#db2777" stroke-width="1.8" stroke-dasharray="none" stroke-linecap="round" stroke-linejoin="round"/></marker>
    <marker id="hollow-slate" markerUnits="userSpaceOnUse" markerWidth="15" markerHeight="15" viewBox="0 0 12 12" refX="11" refY="6" orient="auto"><path d="M 1 1 L 11 6 L 1 11 Z" fill="#ffffff" stroke="#64748b" stroke-width="1.5" stroke-dasharray="none"/></marker>
  </defs>`;
}

const width = 1480;
const height = 1220;
const cards = [
  classCard({ x: 82, y: 166, w: 360, h: 142, name: "ReactiveSession", stereo: "<<Spring Data receiver>>", attrs: ["execute(statement): Mono<ResultSet>", "prepare(statement): Mono<PreparedStatement>"], fill: "#eff6ff", stroke: "#2563eb" }),
  classCard({ x: 560, y: 150, w: 360, h: 174, name: "ReactiveCassandra\nOperations", stereo: "<<Spring Data receiver>>", attrs: ["select/count/exists CRUD API", "Flux and Mono result contracts"], fill: "#ecfdf5", stroke: "#16a34a" }),
  classCard({ x: 1038, y: 166, w: 360, h: 142, name: "AsyncCassandra\nOperations", stereo: "<<Spring Data receiver>>", attrs: ["CompletableFuture result API", "AsyncResultSet driver path"], fill: "#eff6ff", stroke: "#2563eb" }),
  classCard({ x: 82, y: 406, w: 360, h: 170, name: "ReactiveSession\nCoroutines", stereo: "<<extension file>>", attrs: ["receiver: ReactiveSession"], ops: ["executeSuspending(...)", "prepareSuspending(...)"], fill: "#eff6ff", stroke: "#2563eb" }),
  classCard({ x: 560, y: 390, w: 360, h: 208, name: "ReactiveCassandra\nOperations Coroutines", stereo: "<<extension file>>", attrs: ["receiver: ReactiveCassandraOperations"], ops: ["selectAsFlow(...)", "selectOne/count/exists suspending", "insert/update/delete/truncate"], fill: "#ecfdf5", stroke: "#16a34a" }),
  classCard({ x: 1038, y: 406, w: 360, h: 184, name: "AsyncCassandra\nOperations Coroutines", stereo: "<<extension file>>", attrs: ["receiver: AsyncCassandraOperations"], ops: ["executeSuspending(...)", "selectSuspending(...)", "sliceSuspending(...)"], fill: "#eff6ff", stroke: "#2563eb" }),
  classCard({ x: 82, y: 672, w: 360, h: 186, name: "Batch and Select\nSupport", stereo: "<<extension files>>", attrs: ["ReactiveCassandraBatchOperations", "ReactiveSelectOperation"], ops: ["insert/update/delete Flow", "count/exists/first/one/all"], fill: "#f5f3ff", stroke: "#7c3aed" }),
  classCard({ x: 560, y: 672, w: 360, h: 214, name: "CQL Options and\nCriteria DSL", stereo: "<<DSL helpers>>", attrs: ["QueryOptions, WriteOptions", "Criteria"], ops: ["query/write option builders", "addWriteOptions(...)", "Criteria.where(...) eq value"], fill: "#fff7ed", stroke: "#ea580c" }),
  classCard({ x: 560, y: 942, w: 360, h: 198, name: "SchemaGenerator", stereo: "<<object>>", attrs: ["depends on CassandraOperations", "uses mappingContext + SchemaFactory"], ops: ["createTableAndTypes<T>()", "potentiallyCreateTableFor<T>()", "truncate<T>() if table exists"], fill: "#fdf2f8", stroke: "#db2777" }),
  classCard({ x: 1038, y: 672, w: 360, h: 158, name: "AbstractCassandra\nPersistable<PK>", stereo: "<<abstract class>>", attrs: ["Persistable<PK>", "Serializable"], ops: ["isNew(): id == null"], fill: "#f8fafc", stroke: "#64748b" }),
  classCard({ x: 1038, y: 890, w: 360, h: 158, name: "AbstractCassandra\nAuditable<U,PK>", stereo: "<<abstract class>>", attrs: ["Auditable<U,PK,Instant>"], ops: ["isNew(): createdAt == null"], fill: "#f8fafc", stroke: "#64748b" }),
];

const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Cassandra core extension and class structure">
  <style>text { font-family: ${font}; dominant-baseline: alphabetic; }</style>
  ${defs()}
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="72" y="62" font-size="30" font-weight="700" fill="#111827">Spring Boot Cassandra Core Extension Structure</text>
  <text x="72" y="92" font-size="15" fill="#64748b">Coroutine extension files wrap Spring Data Cassandra receivers; model bases and SchemaGenerator cover entity identity, auditing, and schema bootstrap.</text>
  ${edge({ d: "M262 406 L262 308", color: "#2563eb", marker: "open-blue", dash: "7 5" })}
  ${edge({ d: "M740 390 L740 324", color: "#16a34a", marker: "open-green", dash: "7 5" })}
  ${edge({ d: "M1218 406 L1218 308", color: "#2563eb", marker: "open-blue", dash: "7 5" })}
  ${edge({ d: "M442 755 L498 755 L498 626 L650 626 L650 598", color: "#7c3aed", marker: "open-purple", dash: "7 5" })}
  ${edge({ d: "M740 672 L740 598", color: "#ea580c", marker: "open-orange", dash: "7 5" })}
  ${edge({ d: "M740 942 L740 910 L956 910 L956 504 L920 504", color: "#db2777", marker: "open-pink", dash: "7 5" })}
  ${edge({ d: "M1218 890 L1218 830", color: "#64748b", marker: "hollow-slate", width: 2.2 })}
  ${cards.join("\n")}
  <g class="legend" transform="translate(72 1184)">
    <line x1="0" y1="0" x2="38" y2="0" stroke="#2563eb" stroke-width="2.4" stroke-dasharray="7 5" marker-end="url(#open-blue)"/><text x="54" y="5" font-size="13" fill="#475569">extension depends on receiver API</text>
    <line x1="360" y1="0" x2="398" y2="0" stroke="#64748b" stroke-width="2.2" marker-end="url(#hollow-slate)"/><text x="414" y="5" font-size="13" fill="#475569">extends abstract base class</text>
    <line x1="685" y1="0" x2="723" y2="0" stroke="#db2777" stroke-width="2.4" stroke-dasharray="7 5" marker-end="url(#open-pink)"/><text x="739" y="5" font-size="13" fill="#475569">uses mapping/schema API</text>
  </g>
</svg>`;

const svgPath = join(outDir, "spring-boot-cassandra-diagram-01.svg");
const pngPath = join(outDir, "spring-boot-cassandra-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(cairosvg, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`generated ${svgPath}`);
console.log(`generated ${pngPath}`);
