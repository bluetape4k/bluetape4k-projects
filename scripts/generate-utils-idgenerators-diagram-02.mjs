#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/idgenerators/README.md",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/IdGenerator.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/LongIdGenerator.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/Snowflake.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/AbstractSnowflake.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/DefaultSnowflake.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/GlobalSnowflake.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/SnowflakeGenerator.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/Snowflakers.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/uuid/Uuid.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/uuid/UuidGenerator.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/Ksuid.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/KsuidGenerator.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/ULID.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/UlidGenerator.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/flake/Flake.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/hashids/Hashids.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Class Diagram[\s\S]*utils-idgenerators-diagram-02\.png/, "README class diagram slot");
assertContains(sources[1], /interface IdGenerator<ID>[\s\S]*fun nextId\(\): ID[\s\S]*fun nextIdAsString\(\): String/, "IdGenerator contract");
assertContains(sources[2], /interface LongIdGenerator: IdGenerator<Long>/, "LongIdGenerator contract");
assertContains(sources[3], /interface Snowflake: IdGenerator<Long>/, "Snowflake contract");
assertContains(sources[4], /abstract class AbstractSnowflake\(val sequencer: Sequencer\): Snowflake/, "AbstractSnowflake hierarchy");
assertContains(sources[5], /class DefaultSnowflake[\s\S]*AbstractSnowflake/, "DefaultSnowflake hierarchy");
assertContains(sources[6], /class GlobalSnowflake: AbstractSnowflake/, "GlobalSnowflake hierarchy");
assertContains(sources[7], /class SnowflakeGenerator[\s\S]*IdGenerator<Long> by snowflake/, "Snowflake adapter");
assertContains(sources[8], /object Snowflakers[\s\S]*val Default: Snowflake[\s\S]*val Global: Snowflake/, "Snowflakers facade");
assertContains(sources[9], /interface Generator: IdGenerator<UUID>[\s\S]*object V7: Generator/, "Uuid generator strategies");
assertContains(sources[10], /class UuidGenerator[\s\S]*IdGenerator<UUID> by generator/, "Uuid adapter");
assertContains(sources[11], /interface Generator: IdGenerator<String>[\s\S]*object Seconds: Generator[\s\S]*object Millis: Generator/, "Ksuid strategies");
assertContains(sources[12], /class KsuidGenerator[\s\S]*IdGenerator<String> by generator/, "Ksuid adapter");
assertContains(sources[13], /interface ULID[\s\S]*interface StatefulMonotonic: Factory/, "ULID monotonic contracts");
assertContains(sources[14], /class UlidGenerator[\s\S]*IdGenerator<String>[\s\S]*statefulMonotonic/, "Ulid adapter");
assertContains(sources[15], /class Flake[\s\S]*IdGenerator<ByteArray>/, "Flake ByteArray generator");
assertContains(sources[16], /class Hashids[\s\S]*fun encode\(vararg numbers: Long\)[\s\S]*fun decode\(hash: String\): LongArray/, "Hashids utility boundary");

const palette = {
  slate: ["#F8FAFC", "#64748B", "#475569"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
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
  return Object.entries(palette).map(([name, [, stroke, dark]]) => `
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 19 11 L 2 20 Z" fill="${dark}"/></marker>
  <marker id="inherit-${name}" markerWidth="26" markerHeight="22" refX="23" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 23 11 L 2 20 Z" fill="#FFFFFF" stroke="${stroke}" stroke-width="2.2"/></marker>`).join("\n");
}

function panel({ x, y, w, h, title }) {
  return `<g>
  <rect class="panel" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/>
  <text class="panelTitle" x="${x + 28}" y="${y + 44}">${esc(title)}</text>
</g>`;
}

function classBox({ id, x, y, w, h, color, kind, title, attrs = [], ops = [] }) {
  const [fill, stroke, dark] = palette[color];
  const attrY = y + 116;
  const opY = attrs.length ? attrY + attrs.length * 24 + 28 : y + 118;
  return `<g id="${esc(id)}">
  <rect class="classBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="kind" x="${x + w / 2}" y="${y + 30}" text-anchor="middle">${esc(kind)}</text>
  <text class="classTitle" x="${x + w / 2}" y="${y + 66}" text-anchor="middle">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 88}H${x + w}" stroke="${dark}"/>
  ${attrs.map((line, index) => `<text class="member" x="${x + 22}" y="${attrY + index * 24}">${esc(line)}</text>`).join("\n")}
  ${ops.length ? `<path class="divider" d="M${x} ${opY - 22}H${x + w}" stroke="${dark}"/>` : ""}
  ${ops.map((line, index) => `<text class="member" x="${x + 22}" y="${opY + index * 24}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function edge({ from, to, points, color, type = "uses" }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const marker = type === "inherit" || type === "implements" ? `inherit-${color}` : `arrow-${color}`;
  const klass = type === "implements" ? "edge dashed" : "edge";
  return `<path class="${klass}" data-from="${esc(from)}" data-to="${esc(to)}" d="${d}" stroke="${dark}" marker-end="url(#${marker})"/>`;
}

const width = 3600;
const height = 2700;

const panels = [
  panel({ x: 70, y: 430, w: 790, h: 2120, title: "Long ID contracts and Snowflake" }),
  panel({ x: 930, y: 430, w: 790, h: 2120, title: "UUID strategies and adapter" }),
  panel({ x: 1790, y: 430, w: 790, h: 2120, title: "Sortable string IDs" }),
  panel({ x: 2650, y: 430, w: 880, h: 2120, title: "ByteArray IDs and utilities" }),
];

const boxes = [
  classBox({
    id: "IdGenerator",
    x: 1235,
    y: 180,
    w: 1130,
    h: 190,
    color: "slate",
    kind: "<<interface>>",
    title: "IdGenerator<ID>",
    ops: ["nextId(): ID", "nextIdAsString(): String", "nextIds(size), nextIdsAsString(size)"],
  }),

  classBox({
    id: "LongIdGenerator",
    x: 145,
    y: 540,
    w: 640,
    h: 160,
    color: "green",
    kind: "<<interface>>",
    title: "LongIdGenerator",
    ops: ["IdGenerator<Long> with base-36 string conversion"],
  }),
  classBox({
    id: "Snowflake",
    x: 145,
    y: 790,
    w: 640,
    h: 190,
    color: "teal",
    kind: "<<interface>>",
    title: "Snowflake",
    ops: ["nextId(): Long", "parse(Long/String): SnowflakeId"],
  }),
  classBox({
    id: "AbstractSnowflake",
    x: 145,
    y: 1070,
    w: 640,
    h: 190,
    color: "teal",
    kind: "abstract class",
    title: "AbstractSnowflake",
    attrs: ["sequencer: Sequencer"],
    ops: ["nextId() from sequencer"],
  }),
  classBox({
    id: "DefaultSnowflake",
    x: 105,
    y: 1360,
    w: 300,
    h: 195,
    color: "blue",
    kind: "class",
    title: "DefaultSnowflake",
    attrs: ["machineId: Int"],
    ops: ["DefaultSequencer"],
  }),
  classBox({
    id: "GlobalSnowflake",
    x: 525,
    y: 1360,
    w: 300,
    h: 195,
    color: "blue",
    kind: "class",
    title: "GlobalSnowflake",
    ops: ["GlobalSequencer"],
  }),
  classBox({
    id: "SnowflakeGenerator",
    x: 145,
    y: 1650,
    w: 640,
    h: 235,
    color: "pink",
    kind: "adapter class",
    title: "SnowflakeGenerator",
    attrs: ["snowflake: Snowflake = Snowflakers.Default"],
    ops: ["IdGenerator<Long> by snowflake", "parse(Long/String)"],
  }),
  classBox({
    id: "Snowflakers",
    x: 145,
    y: 1990,
    w: 640,
    h: 250,
    color: "amber",
    kind: "object",
    title: "Snowflakers",
    attrs: ["Default: Snowflake", "Global: Snowflake"],
    ops: ["default(machineId)", "global()"],
  }),

  classBox({
    id: "UuidGeneratorContract",
    x: 1005,
    y: 540,
    w: 640,
    h: 190,
    color: "violet",
    kind: "<<interface>>",
    title: "Uuid.Generator",
    ops: ["IdGenerator<UUID>", "nextUUID(), nextBase62()", "nextUUIDs(size), nextBase62s(size)"],
  }),
  classBox({
    id: "Uuid",
    x: 1005,
    y: 835,
    w: 640,
    h: 265,
    color: "violet",
    kind: "object",
    title: "Uuid",
    attrs: ["V1, V4, V5, V6, V7: Generator", "random(), epochRandom()", "namebased(name)"],
    ops: ["JUG generators + Url62 encoding"],
  }),
  classBox({
    id: "UuidGenerator",
    x: 1005,
    y: 1210,
    w: 640,
    h: 235,
    color: "pink",
    kind: "adapter class",
    title: "UuidGenerator",
    attrs: ["generator: Uuid.Generator = Uuid.V7"],
    ops: ["IdGenerator<UUID> by generator", "nextUUID()"],
  }),

  classBox({
    id: "KsuidGeneratorContract",
    x: 1865,
    y: 540,
    w: 640,
    h: 190,
    color: "amber",
    kind: "<<interface>>",
    title: "Ksuid.Generator",
    ops: ["IdGenerator<String>", "generate(Instant/Date/LocalDateTime)", "prettyString(ksuid)"],
  }),
  classBox({
    id: "Ksuid",
    x: 1865,
    y: 835,
    w: 640,
    h: 250,
    color: "amber",
    kind: "object",
    title: "Ksuid",
    attrs: ["Seconds: 4 timestamp + 16 payload", "Millis: 8 timestamp + 12 payload"],
    ops: ["27-char Base62 string", "legacy methods delegate to Seconds"],
  }),
  classBox({
    id: "KsuidGenerator",
    x: 1865,
    y: 1185,
    w: 640,
    h: 235,
    color: "pink",
    kind: "adapter class",
    title: "KsuidGenerator",
    attrs: ["generator: Ksuid.Generator = Ksuid.Seconds"],
    ops: ["IdGenerator<String> by generator", "generate()"],
  }),
  classBox({
    id: "ULID",
    x: 1865,
    y: 1530,
    w: 640,
    h: 260,
    color: "teal",
    kind: "<<interface>>",
    title: "ULID",
    attrs: ["Factory, Monotonic", "StatefulMonotonic: Factory"],
    ops: ["nextULID(), randomULID()", "parseULID(), fromByteArray()"],
  }),
  classBox({
    id: "UlidGenerator",
    x: 1865,
    y: 1920,
    w: 640,
    h: 235,
    color: "green",
    kind: "class",
    title: "UlidGenerator",
    attrs: ["statefulMonotonic: ULID.StatefulMonotonic"],
    ops: ["IdGenerator<String>", "nextULID(), nextId()"],
  }),

  classBox({
    id: "Flake",
    x: 2745,
    y: 540,
    w: 650,
    h: 315,
    color: "blue",
    kind: "class",
    title: "Flake",
    attrs: ["nodeId: ByteArray", "clock: Clock", "sequence: atomic Int"],
    ops: ["IdGenerator<ByteArray>", "nextId(): 16 bytes", "nextIdAsString(): Base62"],
  }),
  classBox({
    id: "NodeIdentifier",
    x: 2745,
    y: 990,
    w: 650,
    h: 180,
    color: "green",
    kind: "<<interface>>",
    title: "NodeIdentifier",
    ops: ["MacAddressNodeIdentifier provides default node id"],
  }),
  classBox({
    id: "Hashids",
    x: 2745,
    y: 1340,
    w: 650,
    h: 320,
    color: "slate",
    kind: "utility class",
    title: "Hashids",
    attrs: ["salt, minHashLength", "alphabet, separators, guards"],
    ops: ["encode(Long...): String", "decode(String): LongArray", "encodeUUID(), decodeUUID()"],
  }),
];

const edges = [
  edge({ from: "LongIdGenerator", to: "IdGenerator", points: [[465, 540], [465, 405], [1445, 405], [1445, 370]], color: "green", type: "inherit" }),
  edge({ from: "Snowflake", to: "IdGenerator", points: [[745, 790], [745, 735], [835, 735], [835, 405], [1590, 405], [1590, 370]], color: "teal", type: "inherit" }),
  edge({ from: "Uuid.Generator", to: "IdGenerator", points: [[1325, 540], [1325, 370]], color: "violet", type: "inherit" }),
  edge({ from: "Ksuid.Generator", to: "IdGenerator", points: [[2185, 540], [2185, 405], [2010, 405], [2010, 370]], color: "amber", type: "inherit" }),
  edge({ from: "Flake", to: "IdGenerator", points: [[3070, 540], [3070, 405], [2260, 405], [2260, 370]], color: "blue", type: "implements" }),

  edge({ from: "AbstractSnowflake", to: "Snowflake", points: [[465, 1070], [465, 980]], color: "teal", type: "inherit" }),
  edge({ from: "DefaultSnowflake", to: "AbstractSnowflake", points: [[255, 1360], [255, 1300], [420, 1300], [420, 1260]], color: "blue", type: "inherit" }),
  edge({ from: "GlobalSnowflake", to: "AbstractSnowflake", points: [[675, 1360], [675, 1300], [510, 1300], [510, 1260]], color: "blue", type: "inherit" }),
  edge({ from: "Uuid", to: "Uuid.Generator", points: [[1325, 835], [1325, 730]], color: "violet", type: "implements" }),
  edge({ from: "UuidGenerator", to: "Uuid.Generator", points: [[1325, 1210], [1325, 1100], [1490, 1100], [1490, 730]], color: "pink" }),

  edge({ from: "Ksuid", to: "Ksuid.Generator", points: [[2185, 835], [2185, 730]], color: "amber", type: "implements" }),
  edge({ from: "KsuidGenerator", to: "Ksuid.Generator", points: [[2185, 1185], [2185, 1085], [2380, 1085], [2380, 730]], color: "pink" }),
  edge({ from: "UlidGenerator", to: "ULID", points: [[2185, 1920], [2185, 1790]], color: "green" }),

  edge({ from: "Flake", to: "NodeIdentifier", points: [[3070, 855], [3070, 990]], color: "green" }),
];

const svg = `<svg data-intent="Explain the utils/idgenerators class structure from current source contracts." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="ID Generators Class Structure">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}
    .frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:58px;fill:#0F172A}
    .subtitle{font-family:"Comic Mono";font-size:17px;fill:#475569}
    .panel{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5}
    .panelTitle{font-family:"Architects Daughter";font-size:30px;fill:#0F172A;paint-order:stroke;stroke:#FFFFFF;stroke-width:5px;stroke-linejoin:round}
    .classBox{filter:url(#shadow);stroke-width:2}
    .kind{font-family:"Comic Mono";font-size:14px;fill:#475569;font-weight:700}
    .classTitle{font-family:"Architects Daughter";font-size:28px;fill:#0F172A}
    .member{font-family:"Comic Mono";font-size:15px;fill:#334155}
    .divider{stroke-width:1;opacity:.38}
    .edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}
    .dashed{stroke-dasharray:10 10}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="36" y="30" width="${width - 72}" height="${height - 60}" rx="8"/>
<text class="title" x="76" y="100">ID Generators Class Structure</text>
<text class="subtitle" x="80" y="134">Class diagram only: contracts, adapter classes, strategy objects, and utility boundaries from current source.</text>
${panels.join("\n")}
${edges.join("\n")}
${boxes.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-idgenerators-diagram-02.svg");
const pngPath = join(OUT, "utils-idgenerators-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--output-width", String(width * 2), "--output-height", String(height * 2)], { stdio: "inherit" });
console.log("Generated utils-idgenerators-diagram-02.svg/png");
