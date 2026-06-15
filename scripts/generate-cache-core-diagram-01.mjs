#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "cache/cache-core/README.md",
  "cache/cache-core/README.ko.md",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheOperations.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/SuspendNearCacheOperations.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheStatistics.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheResilienceConfig.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecorator.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecorator.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCache.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[2], /interface\s+NearCacheOperations<V:\s*Any>:\s*AutoCloseable/, "blocking NearCacheOperations interface");
assertContains(sources[3], /interface\s+SuspendNearCacheOperations<V:\s*Any>/, "suspend NearCacheOperations interface");
assertContains(sources[4], /interface\s+NearCacheStatistics/, "statistics contract");
assertContains(sources[6], /class\s+ResilientNearCacheDecorator<V:\s*Any>[\s\S]*:\s*NearCacheOperations<V>/, "blocking decorator implementation");
assertContains(sources[7], /class\s+ResilientSuspendNearCacheDecorator<V:\s*Any>[\s\S]*:\s*SuspendNearCacheOperations<V>/, "suspend decorator implementation");
assertContains(sources[8], /class\s+NearJCache<K:\s*Any,\s*V:\s*Any>[\s\S]*:\s*JCache<K,\s*V>\s+by\s+backCache/, "NearJCache implementation");
assertContains(sources[9], /class\s+SuspendNearJCache<K:\s*Any,\s*V:\s*Any>[\s\S]*:\s*SuspendJCache<K,\s*V>\s+by\s+backCache/, "SuspendNearJCache implementation");

const palette = {
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  purple: ["#FAF5FF", "#9333EA", "#7E22CE"],
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
  <marker id="triangle-${name}" markerWidth="26" markerHeight="22" refX="23" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 23 11 L 2 20 Z" fill="#FFFFFF" stroke="${dark}" stroke-width="2.2"/></marker>
  <marker id="diamond-${name}" markerWidth="24" markerHeight="22" refX="22" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 11 L 12 2 L 22 11 L 12 20 Z" fill="#FFFFFF" stroke="${dark}" stroke-width="2.2"/></marker>`).join("\n");
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

function group(x, y, w, h, title, detail) {
  return `<g>
  <rect class="group" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/>
  <text class="groupTitle" x="${x + 24}" y="${y + 36}">${esc(title)}</text>
  <text class="groupDetail" x="${x + 24}" y="${y + 62}">${esc(detail)}</text>
</g>`;
}

function noteBox({ x, y, w, h, color, title, lines }) {
  const [fill, stroke] = palette[color];
  return `<g>
  <rect class="noteBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="noteTitle" x="${x + 28}" y="${y + 48}">${esc(title)}</text>
  ${lines.map((line, index) => `<text class="noteLine" x="${x + 30}" y="${y + 92 + index * 34}">${esc(line)}</text>`).join("\n")}
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

const width = 2600;
const height = 1850;
const body = [
  chip({ x: 1425, y: 78, w: 190, color: "blue", label: "interface" }),
  chip({ x: 1640, y: 78, w: 190, color: "pink", label: "decorator" }),
  chip({ x: 1855, y: 78, w: 190, color: "green", label: "support" }),
  chip({ x: 2070, y: 78, w: 250, color: "teal", label: "JCache bridge" }),
  `<text class="sectionLabel" x="260" y="205">Provider-neutral operation contracts</text>`,
  `<text class="sectionLabel" x="360" y="1100">JCache two-tier implementations</text>`,
  classBox({ id: "NearCacheOperations", x: 250, y: 245, w: 660, h: 335, color: "blue", stereotype: "<<interface>>", title: "NearCacheOperations<V>", attrs: ["+ cacheName: String", "+ isClosed: Boolean"], methods: ["+ get / getAll / containsKey", "+ put / replace / remove", "+ clearLocal / clearAll / sizes", "+ stats(): NearCacheStatistics", "+ close(): Unit"] }),
  classBox({ id: "ResilientNearCacheDecorator", x: 250, y: 755, w: 660, h: 245, color: "pink", stereotype: "<<class>>", title: "ResilientNearCacheDecorator<V>", attrs: ["delegate: NearCacheOperations<V>", "config: NearCacheResilienceConfig"], methods: ["+ retries blocking operations", "+ exposes same contract"] }),
  classBox({ id: "SuspendNearCacheOperations", x: 1575, y: 245, w: 740, h: 335, color: "teal", stereotype: "<<interface>>", title: "SuspendNearCacheOperations<V>", attrs: ["+ cacheName: String", "+ isClosed: Boolean"], methods: ["+ suspend get / put / remove", "+ clearLocal(): Unit", "+ suspend clearAll / backCacheSize / close", "+ stats(): NearCacheStatistics"] }),
  classBox({ id: "ResilientSuspendNearCacheDecorator", x: 1575, y: 755, w: 740, h: 245, color: "purple", stereotype: "<<class>>", title: "ResilientSuspendNearCacheDecorator<V>", attrs: ["delegate: SuspendNearCacheOperations<V>", "config: NearCacheResilienceConfig"], methods: ["+ retries suspend operations", "+ rethrows CancellationException"] }),
  classBox({ id: "NearCacheStatistics", x: 1045, y: 285, w: 380, h: 220, color: "green", stereotype: "<<interface>>", title: "NearCacheStatistics", attrs: ["+ localHits / localMisses", "+ backHits / backMisses", "+ localSize / evictions"], methods: ["+ hitRate: Double"] }),
  classBox({ id: "ResilienceConfig", x: 1045, y: 765, w: 380, h: 215, color: "amber", stereotype: "<<config>>", title: "NearCacheResilienceConfig", attrs: ["+ maxAttempts", "+ waitDuration / backoff", "+ getFailureStrategy"], methods: ["retry policy for wrappers"] }),
  classBox({ id: "JCache", x: 350, y: 1150, w: 520, h: 185, color: "blue", stereotype: "<<interface>>", title: "JCache<K,V>", attrs: ["cache contract"], methods: ["blocking API"] }),
  classBox({ id: "NearJCache", x: 350, y: 1465, w: 520, h: 305, color: "blue", stereotype: "<<class>>", title: "NearJCache<K,V>", attrs: ["frontCache: JCache<K,V>", "backCache: JCache<K,V>", "config: NearJCacheConfig<K,V>"], methods: ["JCache<K,V> by backCache", "get() reads front cache only", "getDeeply() reads back and fills front", "put/remove/replace sync back cache"] }),
  classBox({ id: "SuspendJCache", x: 1500, y: 1150, w: 620, h: 185, color: "teal", stereotype: "<<interface>>", title: "SuspendJCache<K,V>", attrs: ["cache contract"], methods: ["suspend API"] }),
  classBox({ id: "SuspendNearJCache", x: 1500, y: 1465, w: 620, h: 305, color: "teal", stereotype: "<<class>>", title: "SuspendNearJCache<K,V>", attrs: ["frontCache: SuspendJCache<K,V>", "backCache: SuspendJCache<K,V>", "config: NearJCacheConfig<K,V>"], methods: ["SuspendJCache<K,V> by backCache", "get() delegates to getDeeply()", "front miss reads back and fills front", "withoutListener() returns fallback"] }),
  noteBox({ x: 955, y: 1185, w: 450, h: 245, color: "slate", title: "Provider boundary", lines: ["Lettuce, Hazelcast, Redisson", "implement operation contracts", "JCache bridges are separate types"] }),
  noteBox({ x: 955, y: 1495, w: 450, h: 245, color: "green", title: "Reading rule", lines: ["Hollow triangle = realization", "Dashed line = dependency/implements", "Cards are moved before routing lines"] }),
  edge({ from: "ResilientNearCacheDecorator", to: "NearCacheOperations", points: [[580, 755], [580, 580]], color: "pink", marker: "triangle", dashed: true, label: "implements", labelAt: [596, 674] }),
  edge({ from: "ResilientSuspendNearCacheDecorator", to: "SuspendNearCacheOperations", points: [[1945, 755], [1945, 580]], color: "purple", marker: "triangle", dashed: true, label: "implements", labelAt: [1961, 674] }),
  edge({ from: "NearCacheOperations", to: "NearCacheStatistics", points: [[910, 395], [1045, 395]], color: "green", marker: "arrow", dashed: true, label: "stats()", labelAt: [942, 382] }),
  edge({ from: "SuspendNearCacheOperations", to: "NearCacheStatistics", points: [[1575, 425], [1425, 425]], color: "green", marker: "arrow", dashed: true, label: "stats()", labelAt: [1450, 412] }),
  edge({ from: "ResilientNearCacheDecorator", to: "ResilienceConfig", points: [[910, 875], [1045, 875]], color: "amber", marker: "arrow", dashed: true, label: "uses", labelAt: [956, 862] }),
  edge({ from: "ResilientSuspendNearCacheDecorator", to: "ResilienceConfig", points: [[1575, 895], [1425, 895]], color: "amber", marker: "arrow", dashed: true, label: "uses", labelAt: [1468, 882] }),
  edge({ from: "NearJCache", to: "JCache", points: [[610, 1465], [610, 1335]], color: "blue", marker: "triangle", dashed: true, label: "implements", labelAt: [626, 1406] }),
  edge({ from: "SuspendNearJCache", to: "SuspendJCache", points: [[1810, 1465], [1810, 1335]], color: "teal", marker: "triangle", dashed: true, label: "implements", labelAt: [1826, 1406] }),
];

const svg = `<svg data-intent="Recreate cache-cache-core diagram 01 as a source-backed UML class hierarchy with no icons." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="NearCache Interface Hierarchy">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .group{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.4}.groupTitle{font-family:"Architects Daughter";font-size:26px;fill:#0F172A}.groupDetail{font-family:"Comic Mono";font-size:14px;fill:#64748B}
    .classBox{stroke-width:1.8;filter:url(#shadow)}.stereotype{font-family:"Comic Mono";font-size:14px;fill:#475569}.classTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}
    .member{font-family:"Comic Mono";font-size:14px;fill:#334155}.divider{stroke-width:1.1;opacity:.45}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">NearCache Interface Hierarchy</text>
<text class="subtitle" x="76" y="120">cache-core contracts, decorators, statistics, and implementation boundaries from README and source.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "cache-cache-core-diagram-01.svg");
const pngPath = join(OUT, "cache-cache-core-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated cache-cache-core-diagram-01.svg/png");
