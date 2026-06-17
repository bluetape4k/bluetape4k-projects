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
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupport.kt",
  "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGenerator.kt",
];

for (const source of sources) {
  if (!existsSync(join(root, source))) throw new Error(`Missing source: ${source}`);
}

function requireSource(index, pattern, label) {
  const text = readFileSync(join(root, sources[index]), "utf8");
  if (!pattern.test(text)) throw new Error(`Missing ${label}`);
}

requireSource(0, /Cassandra Data Access Layer[\s\S]*spring-boot-cassandra-diagram-02\.png/, "README diagram slot");
requireSource(1, /executeSuspending[\s\S]*prepareSuspending/, "ReactiveSession bridge");
requireSource(2, /selectAsFlow[\s\S]*insertSuspending[\s\S]*truncateSuspending/, "Reactive operations bridge");
requireSource(3, /AsyncCassandraOperations\.executeSuspending[\s\S]*selectSuspending/, "Async operations bridge");
requireSource(4, /insertFlow[\s\S]*updateFlow[\s\S]*deleteFlow/, "batch bridge");
requireSource(5, /writeOptions[\s\S]*addWriteOptions[\s\S]*isPositiveTtl/, "options DSL");
requireSource(6, /createTableAndTypes[\s\S]*potentiallyCreateTableFor[\s\S]*truncate/, "schema utilities");

const font = "'Architects Daughter', 'Comic Mono', 'Helvetica Neue', Arial, sans-serif";

function esc(value) {
  return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function textLines(items, x, y, { size = 12.6, fill = "#475569", line = 18 } = {}) {
  return items.map((item, i) => `<text x="${x}" y="${y + i * line}" font-size="${size}" fill="${fill}">${esc(item)}</text>`).join("\n");
}

function layer({ x, y, w, h, title, note, fill, stroke }) {
  return `
  <g class="layer" data-layer="${esc(title)}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" stroke-width="1.3" stroke-dasharray="7 5"/>
    <text x="${x + 18}" y="${y + 26}" font-size="14" font-weight="700" fill="${stroke}">${esc(title)}</text>
    <text x="${x + 18}" y="${y + 47}" font-size="12.3" fill="#6b7280">${esc(note)}</text>
  </g>`;
}

function card({ x, y, w, h, title, lines, fill, stroke }) {
  return `
  <g class="card" data-card="${esc(title)}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    <rect x="${x}" y="${y}" width="10" height="${h}" rx="5" fill="${stroke}" opacity="0.9"/>
    <text x="${x + 26}" y="${y + 33}" font-size="17" font-weight="700" fill="#111827">${esc(title)}</text>
    ${textLines(lines, x + 26, y + 60)}
  </g>`;
}

function edge({ d, color, marker, width = 2.8, dash = "" }) {
  return `<path class="edge" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})"/>`;
}

function defs() {
  return `
  <defs>
    <marker id="arrow-blue" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#2563eb" stroke="#2563eb" stroke-width="0" stroke-dasharray="none"/></marker>
    <marker id="arrow-green" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#16a34a" stroke="#16a34a" stroke-width="0" stroke-dasharray="none"/></marker>
    <marker id="arrow-orange" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#ea580c" stroke="#ea580c" stroke-width="0" stroke-dasharray="none"/></marker>
    <marker id="arrow-purple" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#7c3aed" stroke="#7c3aed" stroke-width="0" stroke-dasharray="none"/></marker>
    <marker id="arrow-pink" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#db2777" stroke="#db2777" stroke-width="0" stroke-dasharray="none"/></marker>
  </defs>`;
}

const width = 1480;
const height = 760;
const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Cassandra data access layer">
  <style>text { font-family: ${font}; dominant-baseline: alphabetic; }</style>
  ${defs()}
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="72" y="62" font-size="30" font-weight="700" fill="#111827">Spring Boot Cassandra Data Access Layer</text>
  <text x="72" y="92" font-size="15" fill="#64748b">Application code moves left to right through bluetape4k coroutine adapters, Spring Data Cassandra operations, driver calls, and Cassandra.</text>
  ${layer({ x: 60, y: 130, w: 306, h: 512, title: "APPLICATION CONTRACT", note: "Call sites and entities choose entrypoints.", fill: "#eff6ff", stroke: "#2563eb" })}
  ${layer({ x: 408, y: 130, w: 306, h: 512, title: "BLUETAPE4K ADAPTERS", note: "Coroutine bridges and DSL helpers.", fill: "#ecfdf5", stroke: "#16a34a" })}
  ${layer({ x: 756, y: 130, w: 306, h: 512, title: "SPRING DATA CASSANDRA", note: "Operations, CQL, mapping metadata.", fill: "#fff7ed", stroke: "#ea580c" })}
  ${layer({ x: 1114, y: 130, w: 306, h: 512, title: "DRIVER AND CASSANDRA", note: "Session, async result, and backend.", fill: "#f8fafc", stroke: "#64748b" })}
  ${card({ x: 84, y: 220, w: 258, h: 86, title: "Service / Repository", lines: ["suspend calls", "Flow collection"], fill: "#eff6ff", stroke: "#2563eb" })}
  ${card({ x: 84, y: 362, w: 258, h: 86, title: "Batch / Select Use", lines: ["Flow batch calls", "select helpers"], fill: "#f5f3ff", stroke: "#7c3aed" })}
  ${card({ x: 84, y: 504, w: 258, h: 86, title: "Schema Bootstrap", lines: ["createTableAndTypes<T>()", "truncate<T>()"], fill: "#fdf2f8", stroke: "#db2777" })}
  ${card({ x: 432, y: 211, w: 258, h: 104, title: "Coroutine Extensions", lines: ["ReactiveSession bridge", "operations *Suspending", "awaitSingle / asFlow"], fill: "#ecfdf5", stroke: "#16a34a" })}
  ${card({ x: 432, y: 357, w: 258, h: 96, title: "Batch / Select Helpers", lines: ["insert/update/delete Flow", "count/exists/first/one/all"], fill: "#f5f3ff", stroke: "#7c3aed" })}
  ${card({ x: 432, y: 495, w: 258, h: 104, title: "Options / Criteria DSL", lines: ["query/write option builders", "addWriteOptions(...)", "Criteria.where(...) eq"], fill: "#fff7ed", stroke: "#ea580c" })}
  ${card({ x: 780, y: 211, w: 258, h: 104, title: "Operations APIs", lines: ["ReactiveCassandraOperations", "AsyncCassandraOperations", "CRUD/select/count/exists"], fill: "#ecfdf5", stroke: "#16a34a" })}
  ${card({ x: 780, y: 357, w: 258, h: 96, title: "Batch Operations", lines: ["Reactive batch contract", "Spring template batch path"], fill: "#f5f3ff", stroke: "#7c3aed" })}
  ${card({ x: 780, y: 495, w: 258, h: 104, title: "CQL / Mapping", lines: ["QueryOptions, WriteOptions", "SchemaFactory", "mappingContext"], fill: "#fff7ed", stroke: "#ea580c" })}
  ${card({ x: 1138, y: 220, w: 258, h: 86, title: "ReactiveSession", lines: ["execute(statement)", "prepare(statement)"], fill: "#eff6ff", stroke: "#2563eb" })}
  ${card({ x: 1138, y: 362, w: 258, h: 86, title: "Async Driver Path", lines: ["CompletableFuture", "AsyncResultSet"], fill: "#eff6ff", stroke: "#2563eb" })}
  ${card({ x: 1138, y: 504, w: 258, h: 86, title: "Apache Cassandra", lines: ["CQL execution", "metadata / keyspace"], fill: "#f8fafc", stroke: "#64748b" })}
  ${edge({ d: "M342 263 L432 263", color: "#16a34a", marker: "arrow-green" })}
  ${edge({ d: "M342 405 L432 405", color: "#7c3aed", marker: "arrow-purple", dash: "7 5" })}
  ${edge({ d: "M342 547 L432 547", color: "#db2777", marker: "arrow-pink", dash: "7 5" })}
  ${edge({ d: "M690 263 L780 263", color: "#16a34a", marker: "arrow-green" })}
  ${edge({ d: "M690 405 L780 405", color: "#7c3aed", marker: "arrow-purple" })}
  ${edge({ d: "M690 547 L780 547", color: "#ea580c", marker: "arrow-orange" })}
  ${edge({ d: "M1038 263 L1138 263", color: "#2563eb", marker: "arrow-blue" })}
  ${edge({ d: "M1038 263 L1086 263 L1086 405 L1138 405", color: "#2563eb", marker: "arrow-blue", dash: "7 5" })}
  ${edge({ d: "M1038 547 L1138 547", color: "#ea580c", marker: "arrow-orange" })}
  <g class="legend" transform="translate(72 714)">
    <line x1="0" y1="0" x2="42" y2="0" stroke="#16a34a" stroke-width="2.8" marker-end="url(#arrow-green)"/><text x="58" y="5" font-size="13" fill="#475569">main coroutine bridge</text>
    <line x1="280" y1="0" x2="322" y2="0" stroke="#7c3aed" stroke-width="2.8" stroke-dasharray="7 5" marker-end="url(#arrow-purple)"/><text x="338" y="5" font-size="13" fill="#475569">batch/select helper path</text>
    <line x1="590" y1="0" x2="632" y2="0" stroke="#db2777" stroke-width="2.8" stroke-dasharray="7 5" marker-end="url(#arrow-pink)"/><text x="648" y="5" font-size="13" fill="#475569">schema metadata path</text>
    <line x1="890" y1="0" x2="932" y2="0" stroke="#2563eb" stroke-width="2.8" marker-end="url(#arrow-blue)"/><text x="948" y="5" font-size="13" fill="#475569">driver execution</text>
  </g>
</svg>`;

const svgPath = join(outDir, "spring-boot-cassandra-diagram-02.svg");
const pngPath = join(outDir, "spring-boot-cassandra-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(cairosvg, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`generated ${svgPath}`);
console.log(`generated ${pngPath}`);
