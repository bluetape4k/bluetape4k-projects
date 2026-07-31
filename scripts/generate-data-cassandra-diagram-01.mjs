#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const root = process.cwd();
const outDir = join(root, "docs/images/readme-diagrams");
const svgPath = join(outDir, "data-cassandra-diagram-01.svg");
const pngPath = join(outDir, "data-cassandra-diagram-01.png");
const cairosvg = process.env.CAIROSVG ?? "/Users/debop/.local/bin/cairosvg";

const sources = [
  "data/cassandra/README.md",
  "data/cassandra/README.ko.md",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/GettableSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/data/SettableSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/QueryBuilderSupport.kt",
  "data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt",
];

for (const source of sources) {
  if (!existsSync(join(root, source))) throw new Error(`Missing source evidence: ${source}`);
}

function requireSource(index, pattern, label) {
  const text = readFileSync(join(root, sources[index]), "utf8");
  if (!pattern.test(text)) throw new Error(`Missing ${label}`);
}

requireSource(0, /Extension Function API Overview[\s\S]*data-cassandra-diagram-01\.png/, "README diagram slot");
requireSource(2, /cqlSession[\s\S]*cqlSessionOf/, "session DSL");
requireSource(3, /executeSuspending[\s\S]*executeAsync\(statement\)[\s\S]*await\(\)/, "coroutine execute bridge");
requireSource(3, /prepareSuspending[\s\S]*prepareAsync\(request\)[\s\S]*await\(\)/, "coroutine prepare bridge");
requireSource(4, /AsyncResultSet\.asFlow[\s\S]*fetchNextPage\(\)\.await/, "result set flow bridge");
requireSource(5, /statementOf[\s\S]*simpleStatementOf[\s\S]*boundStatementOf[\s\S]*batchStatementOf/, "statement builders");
requireSource(6, /Row\.toMap[\s\S]*toNamedMap[\s\S]*mapWithCqlIdentifier/, "row mapping helpers");
requireSource(7, /GettableById\.getValue[\s\S]*GettableByIndex\.getValue[\s\S]*GettableByName\.getValue/, "gettable helpers");
requireSource(8, /SettableById<T>\.setValue[\s\S]*SettableByIndex<T>\.setValue[\s\S]*SettableByName<T>\.setValue/, "settable helpers");
requireSource(9, /bindMarker[\s\S]*raw[\s\S]*udt/, "query builder helpers");
requireSource(10, /createKeyspace[\s\S]*dropKeyspace[\s\S]*getReleaseVersion/, "admin utilities");

const W = 1540;
const H = 880;
const font = "'Architects Daughter', 'Comic Mono', 'Helvetica Neue', Arial, sans-serif";
const c = {
  ink: "#111827",
  muted: "#64748b",
  line: "#cbd5e1",
  blue: "#2563eb",
  green: "#16a34a",
  orange: "#ea580c",
  purple: "#7c3aed",
  pink: "#db2777",
  slate: "#64748b",
};

function esc(value) {
  return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function lines(items, x, y, { size = 12.4, fill = "#475569", line = 18 } = {}) {
  return items.map((item, i) => `<text x="${x}" y="${y + i * line}" font-size="${size}" fill="${fill}">${esc(item)}</text>`).join("\n");
}

function layer({ x, title, note, stroke, fill }) {
  return `
  <g class="layer" data-layer="${esc(title)}">
    <rect x="${x}" y="126" width="320" height="620" rx="10" fill="${fill}" stroke="${stroke}" stroke-width="1.3" stroke-dasharray="7 5"/>
    <text x="${x + 20}" y="154" font-size="14" font-weight="700" fill="${stroke}">${esc(title)}</text>
    <text x="${x + 20}" y="176" font-size="12.2" fill="#64748b">${esc(note)}</text>
  </g>`;
}

function card({ x, y, title, sub, fill, stroke, icon }) {
  return `
  <g class="card" data-card="${esc(title)}">
    <rect x="${x}" y="${y}" width="272" height="82" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    <rect x="${x + 14}" y="${y + 18}" width="42" height="42" rx="8" fill="#ffffff" stroke="${stroke}" stroke-width="1.4"/>
    <text x="${x + 35}" y="${y + 46}" text-anchor="middle" font-size="20" font-weight="700" fill="${stroke}">${esc(icon)}</text>
    <text x="${x + 72}" y="${y + 31}" font-size="16.4" font-weight="700" fill="${c.ink}">${esc(title)}</text>
    ${lines(sub, x + 72, y + 56)}
  </g>`;
}

function edge({ y, color, marker, dash = "" }) {
  return `
    <path class="edge" d="M332 ${y} L420 ${y}" fill="none" stroke="${color}" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})"/>
    <path class="edge" d="M692 ${y} L780 ${y}" fill="none" stroke="${color}" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})"/>
    <path class="edge" d="M1052 ${y} L1140 ${y}" fill="none" stroke="${color}" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})"/>`;
}

function defs() {
  const marker = (id, color) => `<marker id="${id}" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 0 0 L 10 5 L 0 10 Z" fill="${color}" stroke="${color}" stroke-width="0" stroke-dasharray="none"/></marker>`;
  return `
  <defs>
    ${marker("arrow-blue", c.blue)}
    ${marker("arrow-green", c.green)}
    ${marker("arrow-orange", c.orange)}
    ${marker("arrow-purple", c.purple)}
    ${marker("arrow-pink", c.pink)}
    ${marker("arrow-slate", c.slate)}
  </defs>`;
}

const rows = [
  {
    y: 205,
    color: c.blue,
    marker: "arrow-blue",
    a: ["Create session", ["builder or factory call"], "#eff6ff", c.blue, "S"],
    b: ["Session DSL", ["cqlSession { ... }", "cqlSessionOf(...)"], "#eff6ff", c.blue, "K"],
    c: ["CqlSessionBuilder", ["contact point / keyspace", "datacenter / auth"], "#eff6ff", c.blue, "D"],
    d: ["CqlSession", ["driver session ready"], "#f8fafc", c.slate, "C"],
  },
  {
    y: 315,
    color: c.orange,
    marker: "arrow-orange",
    a: ["Build statement", ["CQL + values"], "#fff7ed", c.orange, "Q"],
    b: ["Statement DSL", ["statementOf(...)", "bound/batch builders"], "#fff7ed", c.orange, "B"],
    c: ["Driver Statement", ["Simple / Bound / Batch", "PrepareRequest"], "#fff7ed", c.orange, "D"],
    d: ["Executable CQL", ["sent through session"], "#fff7ed", c.orange, "E"],
  },
  {
    y: 425,
    color: c.green,
    marker: "arrow-green",
    a: ["Run async query", ["suspend function call"], "#ecfdf5", c.green, "A"],
    b: ["Coroutine Bridge", ["executeSuspending(...)", "prepareSuspending(...)"], "#ecfdf5", c.green, "C"],
    c: ["Driver Future", ["executeAsync(...).await()", "prepareAsync(...).await()"], "#ecfdf5", c.green, "F"],
    d: ["AsyncResultSet", ["awaited result"], "#ecfdf5", c.green, "R"],
  },
  {
    y: 535,
    color: c.purple,
    marker: "arrow-purple",
    a: ["Read rows", ["Row / Gettable"], "#f5f3ff", c.purple, "R"],
    b: ["Mapping Helpers", ["toMap / toNamedMap", "typed getValue/getList"], "#f5f3ff", c.purple, "M"],
    c: ["Codec / Column API", ["codec-aware decode", "name / id / index"], "#f5f3ff", c.purple, "T"],
    d: ["Kotlin Values", ["maps and typed values"], "#f5f3ff", c.purple, "V"],
  },
  {
    y: 645,
    color: c.pink,
    marker: "arrow-pink",
    a: ["Build schema", ["querybuilder or admin"], "#fdf2f8", c.pink, "A"],
    b: ["QueryBuilder Helpers", ["bindMarker / raw / udt", "create/drop keyspace"], "#fdf2f8", c.pink, "B"],
    c: ["SchemaBuilder / Query", ["driver builder output", "system.local version"], "#fdf2f8", c.pink, "D"],
    d: ["Cassandra Admin", ["keyspace and version"], "#fdf2f8", c.pink, "K"],
  },
];

const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg data-intent="Show how each public Cassandra extension family carries application intent through bluetape4k Kotlin helpers into DataStax driver primitives and runtime results." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Cassandra extension function API overview">
  <style>text { font-family: ${font}; dominant-baseline: alphabetic; }</style>
  ${defs()}
  <rect width="${W}" height="${H}" fill="#ffffff"/>
  <text x="72" y="62" font-size="30" font-weight="700" fill="${c.ink}">Cassandra Extension Function API Overview</text>
  <text x="72" y="92" font-size="15" fill="${c.muted}">Each row maps a public feature family to the DataStax driver primitive it creates, awaits, reads, or administers.</text>
  ${layer({ x: 48, title: "CALLER INTENT", note: "What application code wants to do.", stroke: c.blue, fill: "#eff6ff" })}
  ${layer({ x: 408, title: "BLUETAPE4K EXTENSIONS", note: "Kotlin-friendly extension entrypoints.", stroke: c.green, fill: "#ecfdf5" })}
  ${layer({ x: 768, title: "DATASTAX DRIVER", note: "Native builder/session/data APIs.", stroke: c.orange, fill: "#fff7ed" })}
  ${layer({ x: 1128, title: "RUNTIME RESULT", note: "Driver-visible object or effect.", stroke: c.slate, fill: "#f8fafc" })}
  <g class="edges">
    ${rows.map((row) => edge({ y: row.y + 41, color: row.color, marker: row.marker })).join("\n")}
  </g>
  <g class="cards">
    ${rows.map((row) => [
      card({ x: 60, y: row.y, title: row.a[0], sub: row.a[1], fill: row.a[2], stroke: row.a[3], icon: row.a[4] }),
      card({ x: 420, y: row.y, title: row.b[0], sub: row.b[1], fill: row.b[2], stroke: row.b[3], icon: row.b[4] }),
      card({ x: 780, y: row.y, title: row.c[0], sub: row.c[1], fill: row.c[2], stroke: row.c[3], icon: row.c[4] }),
      card({ x: 1140, y: row.y, title: row.d[0], sub: row.d[1], fill: row.d[2], stroke: row.d[3], icon: row.d[4] }),
    ].join("\n")).join("\n")}
  </g>
  <g class="legend" transform="translate(72 820)">
    <line x1="0" y1="0" x2="42" y2="0" stroke="${c.blue}" stroke-width="2.8" marker-end="url(#arrow-blue)"/><text x="58" y="5" font-size="13" fill="#475569">session creation</text>
    <line x1="260" y1="0" x2="302" y2="0" stroke="${c.orange}" stroke-width="2.8" marker-end="url(#arrow-orange)"/><text x="318" y="5" font-size="13" fill="#475569">statement building</text>
    <line x1="560" y1="0" x2="602" y2="0" stroke="${c.green}" stroke-width="2.8" marker-end="url(#arrow-green)"/><text x="618" y="5" font-size="13" fill="#475569">coroutine bridge</text>
    <line x1="830" y1="0" x2="872" y2="0" stroke="${c.purple}" stroke-width="2.8" marker-end="url(#arrow-purple)"/><text x="888" y="5" font-size="13" fill="#475569">row/value mapping</text>
    <line x1="1130" y1="0" x2="1172" y2="0" stroke="${c.pink}" stroke-width="2.8" marker-end="url(#arrow-pink)"/><text x="1188" y="5" font-size="13" fill="#475569">query/admin helpers</text>
  </g>
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(cairosvg, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`generated ${svgPath}`);
console.log(`generated ${pngPath}`);
