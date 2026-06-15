#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/idgenerators/README.md",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/ULID.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/UlidGenerator.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/internal/ULIDFactory.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/internal/ULIDMonotonic.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/internal/ULIDStatefulMonotonic.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Generator selection:[\s\S]*utils-idgenerators-diagram-05\.png/, "README ULID generator selection slot");
assertContains(sources[1], /interface Factory[\s\S]*randomULID[\s\S]*nextULID[\s\S]*parseULID/, "Factory contract");
assertContains(sources[1], /interface Monotonic[\s\S]*fun nextULID\([\s\S]*previous: ULID[\s\S]*timestamp: Long/, "Monotonic contract");
assertContains(sources[1], /interface StatefulMonotonic: Factory/, "StatefulMonotonic contract");
assertContains(sources[1], /companion object: Factory by ULIDFactory\.Default[\s\S]*fun factory[\s\S]*fun monotonic[\s\S]*fun statefulMonotonic/, "ULID companion factories");
assertContains(sources[2], /class UlidGenerator[\s\S]*IdGenerator<String>[\s\S]*statefulMonotonic/, "UlidGenerator adapter");
assertContains(sources[3], /SecureRandom[\s\S]*class ULIDFactory[\s\S]*ULID\.Factory/, "ULIDFactory");
assertContains(sources[4], /class ULIDMonotonic[\s\S]*ULID\.Monotonic[\s\S]*previous\.increment/, "ULIDMonotonic");
assertContains(sources[5], /class ULIDStatefulMonotonic[\s\S]*ULID\.StatefulMonotonic[\s\S]*previousRef/, "ULIDStatefulMonotonic");

const width = 3400;
const height = 1720;

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
  <marker id="arrow-${name}" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto" markerUnits="userSpaceOnUse"><path d="M .9 .9 L 7 4 L .9 7.1 Z" fill="${dark}"/></marker>
  <marker id="inherit-${name}" markerWidth="10" markerHeight="10" refX="8.5" refY="5" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 9 5 L 1 9 Z" fill="#FFFFFF" stroke="${stroke}" stroke-width="1.6"/></marker>`).join("\n");
}

function classBox({ id, x, y, w, h, color, kind, title, attrs = [], ops = [] }) {
  const [fill, stroke, dark] = palette[color];
  const attrY = y + 116;
  const opY = attrs.length ? attrY + attrs.length * 25 + 28 : y + 118;
  return `<g id="${esc(id)}">
  <rect class="classBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="kind" x="${x + w / 2}" y="${y + 30}" text-anchor="middle">${esc(kind)}</text>
  <text class="classTitle" x="${x + w / 2}" y="${y + 66}" text-anchor="middle">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 88}H${x + w}" stroke="${dark}"/>
  ${attrs.map((line, index) => `<text class="member" x="${x + 24}" y="${attrY + index * 25}">${esc(line)}</text>`).join("\n")}
  ${ops.length ? `<path class="divider" d="M${x} ${opY - 22}H${x + w}" stroke="${dark}"/>` : ""}
  ${ops.map((line, index) => `<text class="member" x="${x + 24}" y="${opY + index * 25}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function lane({ x, y, w, h, title }) {
  return `<g>
  <rect class="lane" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/>
  <text class="laneTitle" x="${x + 26}" y="${y + 42}">${esc(title)}</text>
</g>`;
}

function edge({ from, to, points, color, type = "uses" }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const marker = type === "inherit" || type === "implements" ? `inherit-${color}` : `arrow-${color}`;
  const klass = type === "implements" ? "edge dashed" : "edge";
  return `<path class="${klass}" data-from="${esc(from)}" data-to="${esc(to)}" d="${d}" stroke="${dark}" marker-end="url(#${marker})"/>`;
}

const lanes = [
  lane({ x: 70, y: 250, w: 760, h: 1310, title: "Factory path" }),
  lane({ x: 890, y: 250, w: 760, h: 1310, title: "Monotonic path" }),
  lane({ x: 1710, y: 250, w: 760, h: 1310, title: "Stateful path" }),
  lane({ x: 2530, y: 250, w: 800, h: 1310, title: "Adapter and selection" }),
];

const boxes = [
  classBox({
    id: "ULID",
    x: 1220,
    y: 170,
    w: 960,
    h: 245,
    color: "blue",
    kind: "<<interface>>",
    title: "ULID",
    attrs: ["mostSignificantBits, leastSignificantBits", "timestamp: Long"],
    ops: ["toByteArray(), increment()", "Comparable<ULID>"],
  }),
  classBox({
    id: "ULIDCompanion",
    x: 1220,
    y: 480,
    w: 960,
    h: 260,
    color: "amber",
    kind: "companion object",
    title: "ULID",
    attrs: ["Factory by ULIDFactory.Default"],
    ops: ["factory(random): Factory", "monotonic(factory): Monotonic", "statefulMonotonic(factory): StatefulMonotonic"],
  }),

  classBox({
    id: "FactoryContract",
    x: 145,
    y: 860,
    w: 610,
    h: 245,
    color: "green",
    kind: "<<interface>>",
    title: "ULID.Factory",
    ops: ["randomULID(timestamp): String", "nextULID(timestamp): ULID", "parseULID(text): ULID", "fromByteArray(data): ULID"],
  }),
  classBox({
    id: "ULIDFactory",
    x: 145,
    y: 1280,
    w: 610,
    h: 245,
    color: "green",
    kind: "class",
    title: "ULIDFactory",
    attrs: ["random: Random = SecureRandom"],
    ops: ["creates random high/low bits", "renders 26-char Crockford text"],
  }),

  classBox({
    id: "MonotonicContract",
    x: 965,
    y: 860,
    w: 610,
    h: 220,
    color: "teal",
    kind: "<<interface>>",
    title: "ULID.Monotonic",
    ops: ["nextULID(previous, timestamp): ULID", "nextULIDStrict(...): ULID?"],
  }),
  classBox({
    id: "ULIDMonotonic",
    x: 965,
    y: 1280,
    w: 610,
    h: 245,
    color: "teal",
    kind: "class",
    title: "ULIDMonotonic",
    attrs: ["factory: ULID.Factory"],
    ops: ["same timestamp -> previous.increment()", "new timestamp -> factory.nextULID()"],
  }),

  classBox({
    id: "StatefulContract",
    x: 1785,
    y: 860,
    w: 610,
    h: 245,
    color: "violet",
    kind: "<<interface>>",
    title: "ULID.StatefulMonotonic",
    attrs: ["extends ULID.Factory"],
    ops: ["nextULID(timestamp): ULID", "nextULIDStrict(timestamp): ULID?"],
  }),
  classBox({
    id: "ULIDStatefulMonotonic",
    x: 1785,
    y: 1280,
    w: 610,
    h: 270,
    color: "violet",
    kind: "class",
    title: "ULIDStatefulMonotonic",
    attrs: ["factory: ULID.Factory", "monotonic: ULID.Monotonic", "previousRef: atomic<ULID?>"],
    ops: ["CAS loop updates previous ULID"],
  }),

  classBox({
    id: "UlidGenerator",
    x: 2625,
    y: 860,
    w: 610,
    h: 270,
    color: "pink",
    kind: "class",
    title: "UlidGenerator",
    attrs: ["factory: ULID.Factory = ULID", "statefulMonotonic: StatefulMonotonic"],
    ops: ["IdGenerator<String>", "nextULID(): ULID", "nextId(): 26-char String"],
  }),
  classBox({
    id: "Selection",
    x: 2625,
    y: 1190,
    w: 610,
    h: 315,
    color: "slate",
    kind: "selection guide",
    title: "Choose a generator",
    attrs: ["ULID.randomULID(): direct string", "ULID.nextULID(): direct value object", "ULID.monotonic(): caller tracks previous", "ULID.statefulMonotonic(): internal previous", "UlidGenerator(): IdGenerator<String> adapter"],
    ops: ["default adapter path is stateful monotonic"],
  }),
];

const edges = [
  edge({ from: "ULIDFactory", to: "ULID.Factory", points: [[450, 1280], [450, 1105]], color: "green", type: "implements" }),
  edge({ from: "ULIDMonotonic", to: "ULID.Monotonic", points: [[1270, 1280], [1270, 1080]], color: "teal", type: "implements" }),
  edge({ from: "ULIDStatefulMonotonic", to: "ULID.StatefulMonotonic", points: [[2090, 1280], [2090, 1105]], color: "violet", type: "implements" }),

  edge({ from: "ULID companion", to: "ULID.Factory", points: [[1460, 740], [1460, 805], [450, 805], [450, 860]], color: "green" }),
  edge({ from: "ULID companion", to: "ULID.Monotonic", points: [[1700, 740], [1700, 805], [1270, 805], [1270, 860]], color: "teal" }),
  edge({ from: "ULID companion", to: "ULID.StatefulMonotonic", points: [[1940, 740], [1940, 805], [2090, 805], [2090, 860]], color: "violet" }),

  edge({ from: "UlidGenerator", to: "ULID.StatefulMonotonic", points: [[2930, 860], [2930, 805], [2090, 805], [2090, 860]], color: "pink" }),
  edge({ from: "Selection", to: "UlidGenerator", points: [[2930, 1190], [2930, 1130]], color: "slate" }),
];

const svg = `<svg data-intent="Explain ULID generator selection and class relationships from current source." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="ULID Generator Selection">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}
    .frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:58px;fill:#0F172A}
    .subtitle{font-family:"Comic Mono";font-size:17px;fill:#475569}
    .lane{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5}
    .laneTitle{font-family:"Architects Daughter";font-size:30px;fill:#0F172A;paint-order:stroke;stroke:#FFFFFF;stroke-width:5px;stroke-linejoin:round}
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
<text class="title" x="76" y="100">ULID Generator Selection</text>
<text class="subtitle" x="80" y="134">Class diagram: direct factory, monotonic factory, stateful monotonic, and IdGenerator adapter paths.</text>
${lanes.join("\n")}
${edges.join("\n")}
${boxes.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-idgenerators-diagram-05.svg");
const pngPath = join(OUT, "utils-idgenerators-diagram-05.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--output-width", String(width * 2), "--output-height", String(height * 2)], { stdio: "inherit" });
console.log("Generated utils-idgenerators-diagram-05.svg/png");
