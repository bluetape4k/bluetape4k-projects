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
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/Snowflakers.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/uuid/Uuid.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/UlidGenerator.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/Ksuid.kt",
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

assertContains(sources[0], /All Algorithms at a Glance[\s\S]*utils-idgenerators-diagram-01\.png/, "README algorithm overview slot");
assertContains(sources[0], /Snowflake[\s\S]*GlobalSnowflake[\s\S]*UUID v7[\s\S]*ULID[\s\S]*KSUID[\s\S]*Flake[\s\S]*Hashids/, "README selection guide algorithms");
assertContains(sources[1], /interface IdGenerator<ID>[\s\S]*nextId\(\)[\s\S]*nextIdsAsString/, "IdGenerator contract");
assertContains(sources[2], /interface LongIdGenerator: IdGenerator<Long>/, "LongIdGenerator contract");
assertContains(sources[3], /object Snowflakers[\s\S]*Default[\s\S]*Global[\s\S]*default\(machineId/, "Snowflakers entrypoint");
assertContains(sources[4], /object Uuid[\s\S]*object V1[\s\S]*object V4[\s\S]*object V6[\s\S]*object V7/, "UUID family");
assertContains(sources[5], /class UlidGenerator[\s\S]*StatefulMonotonic[\s\S]*nextULID/, "ULID generator");
assertContains(sources[6], /object Ksuid[\s\S]*object Seconds[\s\S]*object Millis[\s\S]*Base62/, "KSUID seconds and millis");
assertContains(sources[7], /class Flake[\s\S]*IdGenerator<ByteArray>[\s\S]*ID_SIZE_BYTES = 16/, "Flake 128-bit generator");
assertContains(sources[8], /obfuscation[\s\S]*class Hashids[\s\S]*encode\(vararg numbers: Long\)[\s\S]*decode\(hash: String\)/i, "Hashids obfuscation");

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
  return Object.entries(palette).map(([name, [, , dark]]) => `
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 19 11 L 2 20 Z" fill="${dark}"/></marker>`).join("\n");
}

function band({ x, y, w, h, title, subtitle }) {
  return `<g>
  <rect class="band" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/>
  <text class="sectionTitle" x="${x + 26}" y="${y + 46}">${esc(title)}</text>
  <text class="bandSub" x="${x + 26}" y="${y + 76}">${esc(subtitle)}</text>
</g>`;
}

function card({ id, x, y, w, h, color, title, badge, lines = [], footer = "" }) {
  footer = "";
  const [fill, stroke, dark] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="badge" x="${x + 22}" y="${y + 30}">${esc(badge)}</text>
  <text class="cardTitle" x="${x + 22}" y="${y + 68}">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 90}H${x + w}" stroke="${dark}"/>
  ${lines.map((line, index) => `<text class="body" x="${x + 22}" y="${y + 124 + index * 24}">${esc(line)}</text>`).join("\n")}
  ${footer ? `<path class="divider" d="M${x} ${y + h - 46}H${x + w}" stroke="${dark}"/><text class="foot" x="${x + 22}" y="${y + h - 17}">${esc(footer)}</text>` : ""}
</g>`;
}

function edge({ from, to, points, color }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  return `<path class="edge" data-from="${esc(from)}" data-to="${esc(to)}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>`;
}

const width = 3000;
const height = 1960;

const bands = [
  band({ x: 90, y: 300, w: 2820, h: 360, title: "Long output: sorted numeric identifiers", subtitle: "Use when storage wants compact numeric IDs and ordering." }),
  band({ x: 90, y: 720, w: 2820, h: 360, title: "UUID output: standard 128-bit identifiers", subtitle: "Use when database and ecosystem tooling already expect UUID." }),
  band({ x: 90, y: 1140, w: 2820, h: 360, title: "String output: lexicographic and URL-safe IDs", subtitle: "Use when IDs travel through URLs, logs, queues, or public APIs." }),
  band({ x: 90, y: 1560, w: 2820, h: 300, title: "Encoding and obfuscation helpers", subtitle: "Use only when representation, not uniqueness generation, is the concern." }),
];

const cards = [
  card({
    id: "Contract",
    x: 660,
    y: 150,
    w: 1680,
    h: 140,
    color: "slate",
    badge: "unified API",
    title: "IdGenerator<T> is the common surface",
    lines: ["nextId(), nextIdAsString(), nextIds(size), nextIdsAsString(size)"],
  }),
  card({
    id: "Snowflake",
    x: 170,
    y: 405,
    w: 760,
    h: 220,
    color: "blue",
    badge: "Long | sortable | 19 digits",
    title: "Snowflakers.Default",
    lines: ["distributed env per-machine IDs", "timestamp + machineId + sequence", "up to 4,096 IDs/ms/machine"],
    footer: "README: distributed env, per-machine IDs",
  }),
  card({
    id: "GlobalSnowflake",
    x: 1120,
    y: 405,
    w: 760,
    h: 220,
    color: "teal",
    badge: "Long | sortable | 19 digits",
    title: "Snowflakers.Global",
    lines: ["centralized/global sequencer", "1 ms capacity: 4096 * 1024", "same LongIdGenerator-friendly output"],
    footer: "README: centralized ID service",
  }),
  card({
    id: "LongAdapter",
    x: 2070,
    y: 405,
    w: 760,
    h: 220,
    color: "green",
    badge: "contract",
    title: "LongIdGenerator",
    lines: ["IdGenerator<Long>", "base36 nextIdAsString()", "bulk Long sequence helpers"],
  }),
  card({
    id: "UuidSortable",
    x: 170,
    y: 825,
    w: 760,
    h: 220,
    color: "violet",
    badge: "UUID | sortable | 36 chars",
    title: "Uuid.V7 / V6 / V1",
    lines: ["DB primary key sorting: prefer V7", "V6: reordered timestamp", "V1: MAC + Gregorian timestamp"],
    footer: "README: DB primary key, needs sorting",
  }),
  card({
    id: "UuidRandom",
    x: 1120,
    y: 825,
    w: 760,
    h: 220,
    color: "pink",
    badge: "UUID | non-sortable or deterministic",
    title: "Uuid.V4 / V5",
    lines: ["V4: SecureRandom-style fully random", "V5/namebased: SHA-1 name-based", "nextBase62() gives URL-safe form"],
    footer: "README: random or deterministic UUID",
  }),
  card({
    id: "UuidFactory",
    x: 2070,
    y: 825,
    w: 760,
    h: 220,
    color: "amber",
    badge: "factory",
    title: "Uuid.random / epochRandom",
    lines: ["custom Random injection", "version-specific generator objects", "bulk UUID sequences"],
  }),
  card({
    id: "Ulid",
    x: 170,
    y: 1245,
    w: 760,
    h: 220,
    color: "green",
    badge: "String | 26 chars | sortable",
    title: "UlidGenerator",
    lines: ["Crockford Base32", "StatefulMonotonic for same-ms ordering", "nextULID() exposes value object"],
    footer: "README: monotonic, string ID",
  }),
  card({
    id: "Ksuid",
    x: 1120,
    y: 1245,
    w: 760,
    h: 220,
    color: "amber",
    badge: "String | 27 chars | URL-safe",
    title: "Ksuid.Seconds / Millis",
    lines: ["Base62 encoded 20-byte payload", "Seconds: 4-byte timestamp + 16-byte random", "Millis: 8-byte timestamp + 12-byte random"],
    footer: "README: second or millisecond precision",
  }),
  card({
    id: "Flake",
    x: 2070,
    y: 1245,
    w: 760,
    h: 220,
    color: "blue",
    badge: "ByteArray | 128 bit | sortable",
    title: "Flake",
    lines: ["16-byte Boundary-style ID", "timestamp + nodeId + sequence", "hex/component/Base62 helpers"],
    footer: "README: 128-bit, high uniqueness",
  }),
  card({
    id: "Hashids",
    x: 170,
    y: 1648,
    w: 1160,
    h: 160,
    color: "pink",
    badge: "String | variable length | not unique by itself",
    title: "Hashids",
    lines: ["encode/decode Long arrays for short URLs; obfuscation only, not security"],
  }),
  card({
    id: "Encoding",
    x: 1510,
    y: 1648,
    w: 1160,
    h: 160,
    color: "slate",
    badge: "representation helpers",
    title: "Base62, Base32, Base36, Hex",
    lines: ["Url62 for UUID/Flake, Crockford Base32 for ULID, Base62 for KSUID"],
  }),
];

const edges = [];

const svg = `<svg data-intent="Explain all utils/idgenerators algorithms at a glance from current README and source entrypoints." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="All Algorithms at a Glance">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}
    .frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:58px;fill:#0F172A}
    .subtitle{font-family:"Comic Mono";font-size:17px;fill:#475569}
    .band{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5}
    .sectionTitle{font-family:"Comic Mono";font-size:20px;fill:#0F172A;font-weight:800}
    .bandSub{font-family:"Comic Mono";font-size:15px;fill:#64748B}
    .card{filter:url(#shadow);stroke-width:2}
    .badge{font-family:"Comic Mono";font-size:14px;fill:#475569;font-weight:700}
    .cardTitle{font-family:"Architects Daughter";font-size:28px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:15px;fill:#334155}
    .foot{font-family:"Comic Mono";font-size:13px;fill:#475569}
    .divider{stroke-width:1;opacity:.35}
    .edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="36" y="30" width="${width - 72}" height="${height - 60}" rx="8"/>
<text class="title" x="76" y="100">All Algorithms at a Glance</text>
<text class="subtitle" x="80" y="134">Pick by output type, ordering, distribution model, and representation constraints.</text>
${bands.join("\n")}
${edges.join("\n")}
${cards.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-idgenerators-diagram-01.svg");
const pngPath = join(OUT, "utils-idgenerators-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--output-width", String(width * 2), "--output-height", String(height * 2)], { stdio: "inherit" });
console.log("Generated utils-idgenerators-diagram-01.svg/png");
