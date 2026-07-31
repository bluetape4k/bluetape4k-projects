#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "cache/cache-core/README.ko.md",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/SuspendNearCacheOperations.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecorator.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheStatistics.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheResilienceConfig.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/GetFailureStrategy.kt",
  "cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecoratorTest.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[1], /interface\s+SuspendNearCacheOperations<V:\s*Any>/, "suspend NearCacheOperations interface");
assertContains(sources[1], /suspend\s+fun\s+close\(\)/, "suspend close lifecycle");
assertContains(sources[1], /fun\s+clearLocal\(\)/, "non-suspend clearLocal");
assertContains(sources[1], /fun\s+stats\(\):\s*NearCacheStatistics/, "non-suspend stats");
assertContains(sources[2], /class\s+ResilientSuspendNearCacheDecorator<V:\s*Any>[\s\S]*:\s*SuspendNearCacheOperations<V>/, "resilient suspend decorator implementation");
assertContains(sources[2], /ignoreExceptions\(CancellationException::class\.java\)/, "retry ignores cancellation");
assertContains(sources[2], /catch\s*\(e:\s*CancellationException\)[\s\S]*throw e/, "CancellationException propagation");
assertContains(sources[3], /interface\s+NearCacheStatistics/, "statistics interface");
assertContains(sources[4], /data\s+class\s+NearCacheResilienceConfig/, "resilience config");
assertContains(sources[5], /enum\s+class\s+GetFailureStrategy/, "failure strategy enum");
assertContains(sources[6], /CancellationException[\s\S]*재전파/, "cancellation propagation tests");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  purple: ["#FAF5FF", "#9333EA", "#7E22CE"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  slate: ["#F8FAFC", "#64748B", "#475569"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
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
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 19 11 L 2 20" fill="none" stroke="${dark}" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/></marker>
  <marker id="triangle-${name}" markerWidth="26" markerHeight="22" refX="23" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 23 11 L 2 20 Z" fill="#FFFFFF" stroke="${dark}" stroke-width="2.2"/></marker>`).join("\n");
}

function classBox({ id, x, y, w, h, color, stereotype, title, attrs = [], methods = [] }) {
  const [fill, stroke, dark] = palette[color];
  const attrY = y + 76;
  const methodY = attrY + 34 + Math.max(24, attrs.length * 22);
  return `<g id="${esc(id)}">
  <rect class="classCard" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="stereotype" x="${x + w / 2}" y="${y + 28}" text-anchor="middle">${esc(stereotype)}</text>
  <text class="classTitle" x="${x + w / 2}" y="${y + 58}" text-anchor="middle">${esc(title)}</text>
  <path class="divider" d="M${x} ${attrY}H${x + w}" stroke="${dark}"/>
  ${attrs.map((line, index) => `<text class="member" x="${x + 40}" y="${attrY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
  <path class="divider" d="M${x} ${methodY}H${x + w}" stroke="${dark}"/>
  ${methods.map((line, index) => `<text class="member" x="${x + 40}" y="${methodY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function noteBox({ id = "", x, y, w, h, color, title, lines }) {
  const [fill, stroke] = palette[color];
  return `<g${id ? ` id="${esc(id)}"` : ""}>
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

function edge({ from, to, points, path = "", color, marker = "arrow", dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = path || points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g>
  <path class="edge ${dashed ? "dashed" : ""}" data-from="${esc(from)}" data-to="${esc(to)}" d="${d}" stroke="${dark}" marker-end="url(#${marker}-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0] + 8}" y="${p[1] - 8}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2400;
const height = 1645;
const body = [
  chip({ x: 1340, y: 78, w: 205, color: "teal", label: "suspend API" }),
  chip({ x: 1570, y: 78, w: 190, color: "purple", label: "decorator" }),
  chip({ x: 1785, y: 78, w: 190, color: "green", label: "statistics" }),
  chip({ x: 2000, y: 78, w: 190, color: "amber", label: "policy" }),
  `<text class="sectionLabel" x="710" y="205">Coroutine API surface</text>`,
  `<text class="sectionLabel" x="172" y="610">Retry and cancellation policy</text>`,
  `<text class="sectionLabel" x="1740" y="610">Local snapshot boundary</text>`,
  noteBox({
    id: "NoAutoCloseable",
    x: 700,
    y: 235,
    w: 780,
    h: 190,
    color: "slate",
    title: "No AutoCloseable inheritance",
    lines: ["AutoCloseable.close() is non-suspend", "Suspend contract declares suspend close()", "Lifecycle cleanup keeps coroutine cancellation semantics"],
  }),
  classBox({
    id: "SuspendNearCacheOperations",
    x: 560,
    y: 520,
    w: 1060,
    h: 395,
    color: "teal",
    stereotype: "<<interface>>",
    title: "SuspendNearCacheOperations<V>",
    attrs: ["+ cacheName: String", "+ isClosed: Boolean", "+ key type fixed to String"],
    methods: ["+ suspend read: get / getAll / containsKey", "+ suspend write: put / putAll / putIfAbsent / replace", "+ suspend delete: remove / removeAll / getAndRemove / getAndReplace", "+ local only: clearLocal() / localCacheSize() / stats()", "+ suspend back/lifecycle: clearAll() / backCacheSize() / close()"],
  }),
  classBox({
    id: "ResilientSuspendNearCacheDecorator",
    x: 560,
    y: 1180,
    w: 1060,
    h: 315,
    color: "purple",
    stereotype: "<<class>>",
    title: "ResilientSuspendNearCacheDecorator<V>",
    attrs: ["delegate: SuspendNearCacheOperations<V>", "config: NearCacheResilienceConfig", "retry: resilience4j Retry"],
    methods: ["implements the same suspend contract", "executeSuspendFunction wraps remote operations", "ignoreExceptions(CancellationException)", "CancellationException is rethrown from get/getAll/containsKey/close", "stats(), clearLocal(), localCacheSize() delegate directly"],
  }),
  classBox({
    id: "NearCacheResilienceConfig",
    x: 145,
    y: 665,
    w: 390,
    h: 250,
    color: "amber",
    stereotype: "<<data class>>",
    title: "NearCacheResilienceConfig",
    attrs: ["retryMaxAttempts", "retryWaitDuration", "retryExponentialBackoff", "getFailureStrategy"],
    methods: ["drives suspend retry policy"],
  }),
  classBox({
    id: "GetFailureStrategy",
    x: 145,
    y: 1088,
    w: 390,
    h: 210,
    color: "amber",
    stereotype: "<<enum>>",
    title: "GetFailureStrategy",
    attrs: ["RETURN_FRONT_OR_NULL", "PROPAGATE_EXCEPTION"],
    methods: ["handles ordinary get failures only"],
  }),
  classBox({
    id: "CancellationException",
    x: 145,
    y: 1360,
    w: 390,
    h: 185,
    color: "pink",
    stereotype: "<<coroutine signal>>",
    title: "CancellationException",
    attrs: ["not a fallback case", "not retried"],
    methods: ["must be rethrown"],
  }),
  classBox({
    id: "NearCacheStatistics",
    x: 1750,
    y: 665,
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
    x: 1750,
    y: 1088,
    w: 565,
    h: 250,
    color: "green",
    stereotype: "<<data class>>",
    title: "DefaultNearCacheStatistics",
    attrs: ["immutable counter snapshot", "same model as blocking API"],
    methods: ["hitRate derived from local/back counters", "stats() is non-suspend because local counters"],
  }),
  noteBox({
    x: 1750,
    y: 1360,
    w: 565,
    h: 205,
    color: "slate",
    title: "Reading rule",
    lines: ["Hollow triangle = interface realization", "Dashed arrow = dependency", "This diagram is coroutine-only"],
  }),
  edge({ from: "SuspendNearCacheOperations", to: "NoAutoCloseable", points: [[1090, 520], [1090, 425]], color: "slate", marker: "arrow", dashed: true, label: "declares close()", labelAt: [1106, 475] }),
  edge({ from: "ResilientSuspendNearCacheDecorator", to: "SuspendNearCacheOperations", points: [[1090, 1180], [1090, 915]], color: "purple", marker: "triangle", dashed: true, label: "implements", labelAt: [1106, 1048] }),
  edge({ from: "SuspendNearCacheOperations", to: "NearCacheStatistics", points: [[1620, 770], [1750, 770]], color: "green", marker: "arrow", dashed: true, label: "stats()", labelAt: [1650, 757] }),
  edge({ from: "DefaultNearCacheStatistics", to: "NearCacheStatistics", points: [[2033, 1088], [2033, 915]], color: "green", marker: "triangle", dashed: true, label: "implements", labelAt: [2050, 996] }),
  edge({ from: "ResilientSuspendNearCacheDecorator", to: "NearCacheResilienceConfig", points: [[800, 1180], [800, 1030], [430, 1030], [430, 915]], path: "M800 1180 V1048 Q800 1030 782 1030 H448 Q430 1030 430 1012 V915", color: "amber", marker: "arrow", dashed: true, label: "uses", labelAt: [590, 1018] }),
  edge({ from: "NearCacheResilienceConfig", to: "GetFailureStrategy", points: [[340, 915], [340, 1088]], color: "amber", marker: "arrow", dashed: true, label: "selects", labelAt: [357, 1010] }),
  edge({ from: "ResilientSuspendNearCacheDecorator", to: "CancellationException", points: [[560, 1415], [535, 1415]], color: "pink", marker: "arrow", dashed: true }),
];

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="SuspendNearCacheOperations Coroutine Class Diagram" data-intent="class-structure" data-evidence="cache-core README and Kotlin contracts" data-source-read="${sources.join(",")}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .sectionLabel{font-family:"Architects Daughter";font-size:24px;fill:#0F172A}
    .chip{stroke-width:1.6}.chipText{font-family:"Comic Mono";font-size:14px;fill:#334155}
    .classCard{stroke-width:1.8;filter:url(#shadow)}.stereotype{font-family:"Comic Mono";font-size:14px;fill:#475569}.classTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}
    .member{font-family:"Comic Mono";font-size:14px;fill:#334155}.divider{stroke-width:1.1;opacity:.45}
    .noteBox{stroke-width:1.7;filter:url(#shadow)}.noteTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}.noteLine{font-family:"Comic Mono";font-size:14px;fill:#334155}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">SuspendNearCacheOperations Coroutine Class Diagram</text>
<text class="subtitle" x="76" y="120">cache-core suspend near-cache API, coroutine cancellation contract, retry policy, and local statistics boundary.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "cache-cache-core-diagram-03.svg");
const pngPath = join(OUT, "cache-cache-core-diagram-03.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated cache-cache-core-diagram-03.svg/png");
