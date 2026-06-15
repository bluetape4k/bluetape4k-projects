#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "cache/cache-core/README.ko.md",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheOperations.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheStatistics.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheResilienceConfig.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/GetFailureStrategy.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecorator.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[1], /interface\s+NearCacheOperations<V:\s*Any>:\s*AutoCloseable/, "blocking NearCacheOperations interface");
assertContains(sources[2], /interface\s+NearCacheStatistics/, "statistics interface");
assertContains(sources[2], /data\s+class\s+DefaultNearCacheStatistics[\s\S]*:\s*NearCacheStatistics/, "default statistics implementation");
assertContains(sources[3], /data\s+class\s+NearCacheResilienceConfig/, "resilience config");
assertContains(sources[4], /enum\s+class\s+GetFailureStrategy/, "get failure strategy enum");
assertContains(sources[5], /class\s+ResilientNearCacheDecorator<V:\s*Any>[\s\S]*:\s*NearCacheOperations<V>/, "resilient blocking decorator implementation");

const palette = {
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
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
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 19 11 L 2 20 Z" fill="${dark}"/></marker>
  <marker id="triangle-${name}" markerWidth="26" markerHeight="22" refX="23" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 23 11 L 2 20 Z" fill="#FFFFFF" stroke="${dark}" stroke-width="2.2"/></marker>`).join("\n");
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

function noteBox({ x, y, w, h, color, title, lines }) {
  const [fill, stroke] = palette[color];
  return `<g>
  <rect class="noteBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="noteTitle" x="${x + 28}" y="${y + 48}">${esc(title)}</text>
  ${lines.map((line, index) => `<text class="noteLine" x="${x + 30}" y="${y + 92 + index * 32}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function chip({ x, y, w, color, label }) {
  const [fill, stroke] = palette[color];
  return `<g>
  <rect class="chip" x="${x}" y="${y}" width="${w}" height="54" rx="18" fill="${fill}" stroke="${stroke}"/>
  <text class="chipText" x="${x + w / 2}" y="${y + 34}" text-anchor="middle">${esc(label)}</text>
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
const height = 1600;
const body = [
  chip({ x: 1325, y: 78, w: 180, color: "blue", label: "interface" }),
  chip({ x: 1528, y: 78, w: 180, color: "pink", label: "decorator" }),
  chip({ x: 1731, y: 78, w: 180, color: "green", label: "statistics" }),
  chip({ x: 1934, y: 78, w: 180, color: "amber", label: "config" }),
  `<text class="sectionLabel" x="650" y="205">Blocking API surface</text>`,
  `<text class="sectionLabel" x="178" y="600">Retry and failure policy</text>`,
  `<text class="sectionLabel" x="1508" y="600">Statistics snapshot model</text>`,
  classBox({
    id: "AutoCloseable",
    x: 765,
    y: 235,
    w: 560,
    h: 190,
    color: "slate",
    stereotype: "<<JDK interface>>",
    title: "AutoCloseable",
    attrs: [],
    methods: ["+ close(): Unit"],
  }),
  classBox({
    id: "NearCacheOperations",
    x: 605,
    y: 525,
    w: 880,
    h: 365,
    color: "blue",
    stereotype: "<<interface>>",
    title: "NearCacheOperations<V>",
    attrs: ["+ cacheName: String", "+ isClosed: Boolean", "+ key type fixed to String"],
    methods: ["+ read: get / getAll / containsKey", "+ write: put / putAll / putIfAbsent / replace", "+ delete: remove / removeAll / getAndRemove / getAndReplace", "+ manage: clearLocal / clearAll / localCacheSize / backCacheSize", "+ stats(): NearCacheStatistics", "+ close(): Unit"],
  }),
  classBox({
    id: "ResilientNearCacheDecorator",
    x: 605,
    y: 1110,
    w: 880,
    h: 320,
    color: "pink",
    stereotype: "<<class>>",
    title: "ResilientNearCacheDecorator<V>",
    attrs: ["delegate: NearCacheOperations<V>", "config: NearCacheResilienceConfig", "retry: resilience4j Retry"],
    methods: ["implements the same blocking contract", "read failures use GetFailureStrategy", "write/back operations run through retry", "clearLocal(), stats(), close() delegate directly"],
  }),
  classBox({
    id: "NearCacheResilienceConfig",
    x: 155,
    y: 650,
    w: 385,
    h: 250,
    color: "amber",
    stereotype: "<<data class>>",
    title: "NearCacheResilienceConfig",
    attrs: ["retryMaxAttempts", "retryWaitDuration", "retryExponentialBackoff", "getFailureStrategy"],
    methods: ["validated positive retry settings"],
  }),
  classBox({
    id: "GetFailureStrategy",
    x: 155,
    y: 1048,
    w: 385,
    h: 210,
    color: "amber",
    stereotype: "<<enum>>",
    title: "GetFailureStrategy",
    attrs: ["RETURN_FRONT_OR_NULL", "PROPAGATE_EXCEPTION"],
    methods: ["applies to back-cache GET failures"],
  }),
  classBox({
    id: "NearCacheStatistics",
    x: 1555,
    y: 650,
    w: 565,
    h: 250,
    color: "green",
    stereotype: "<<interface>>",
    title: "NearCacheStatistics",
    attrs: ["localHits / localMisses / localSize", "localEvictions", "backHits / backMisses"],
    methods: ["+ hitRate: Double"],
  }),
  classBox({
    id: "DefaultNearCacheStatistics",
    x: 1555,
    y: 1048,
    w: 565,
    h: 250,
    color: "green",
    stereotype: "<<data class>>",
    title: "DefaultNearCacheStatistics",
    attrs: ["immutable counter snapshot", "defaults all counters to zero"],
    methods: ["hitRate = (localHits + backHits) / total", "returns 0.0 when total is zero"],
  }),
  noteBox({
    x: 155,
    y: 1322,
    w: 385,
    h: 165,
    color: "slate",
    title: "Reading rule",
    lines: ["Hollow triangle = interface realization", "Dashed arrow = dependency", "This diagram is blocking-only"],
  }),
  edge({ from: "NearCacheOperations", to: "AutoCloseable", points: [[1045, 525], [1045, 425]], color: "blue", marker: "triangle", dashed: false, label: "extends", labelAt: [1062, 478] }),
  edge({ from: "ResilientNearCacheDecorator", to: "NearCacheOperations", points: [[1045, 1110], [1045, 890]], color: "pink", marker: "triangle", dashed: true, label: "implements", labelAt: [1062, 1002] }),
  edge({ from: "NearCacheOperations", to: "NearCacheStatistics", points: [[1485, 755], [1555, 755]], color: "green", marker: "arrow", dashed: true, label: "stats()", labelAt: [1495, 742] }),
  edge({ from: "DefaultNearCacheStatistics", to: "NearCacheStatistics", points: [[1838, 1048], [1838, 900]], color: "green", marker: "triangle", dashed: true, label: "implements", labelAt: [1855, 976] }),
  edge({ from: "ResilientNearCacheDecorator", to: "NearCacheResilienceConfig", points: [[605, 1220], [565, 1220], [565, 775], [540, 775]], color: "amber", marker: "arrow", dashed: true, label: "uses", labelAt: [577, 984] }),
  edge({ from: "NearCacheResilienceConfig", to: "GetFailureStrategy", points: [[348, 900], [348, 1048]], color: "amber", marker: "arrow", dashed: true, label: "selects", labelAt: [365, 982] }),
];

const svg = `<svg data-intent="Explain NearCacheOperations (Blocking) as a source-backed UML class diagram." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="NearCacheOperations Blocking Class Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .sectionLabel{font-family:"Architects Daughter";font-size:24px;fill:#0F172A}
    .chip{stroke-width:1.6}.chipText{font-family:"Comic Mono";font-size:14px;fill:#334155}
    .classBox{stroke-width:1.8;filter:url(#shadow)}.stereotype{font-family:"Comic Mono";font-size:14px;fill:#475569}.classTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}
    .member{font-family:"Comic Mono";font-size:14px;fill:#334155}.divider{stroke-width:1.1;opacity:.45}
    .noteBox{stroke-width:1.7;filter:url(#shadow)}.noteTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}.noteLine{font-family:"Comic Mono";font-size:14px;fill:#334155}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">NearCacheOperations Blocking Contract</text>
<text class="subtitle" x="76" y="120">cache-core blocking near-cache API, retry decorator, failure policy, and statistics snapshot model.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "cache-cache-core-diagram-02.svg");
const pngPath = join(OUT, "cache-cache-core-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated cache-cache-core-diagram-02.svg/png");
