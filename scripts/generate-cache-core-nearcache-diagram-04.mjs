#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "cache/cache-core/README.ko.md",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCache.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCache.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntry.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListener.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCache.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfig.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[1], /interface\s+SuspendJCache<K:\s*Any,\s*V:\s*Any>/, "SuspendJCache interface");
assertContains(sources[1], /fun\s+entries\(\):\s*Flow<SuspendJCacheEntry<K,\s*V>>/, "entries flow contract");
assertContains(sources[1], /registerCacheEntryListener/, "listener registration contract");
assertContains(sources[2], /class\s+CaffeineSuspendJCache<K:\s*Any,\s*V:\s*Any>[\s\S]*:\s*SuspendJCache<K,\s*V>/, "Caffeine suspend implementation");
assertContains(sources[3], /data\s+class\s+SuspendJCacheEntry<K:\s*Any,\s*V:\s*Any>[\s\S]*:\s*Cache\.Entry<K,\s*V>/, "SuspendJCacheEntry JCache entry implementation");
assertContains(sources[4], /class\s+SuspendJCacheEntryEventListener<K:\s*Any,\s*V:\s*Any>[\s\S]*targetCache:\s*SuspendJCache<K,\s*V>/, "event listener targets SuspendJCache");
assertContains(sources[5], /class\s+SuspendNearJCache<K:\s*Any,\s*V:\s*Any>[\s\S]*:\s*SuspendJCache<K,\s*V>\s+by\s+backCache/, "SuspendNearJCache implementation");
assertContains(sources[5], /override\s+suspend\s+fun\s+get\(key:\s*K\):\s*V\?\s*=\s*getDeeply\(key\)/, "SuspendNearJCache get delegates deeply");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
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
  <marker id="arrow-${name}" markerWidth="7" markerHeight="7" refX="6" refY="3.5" orient="auto" markerUnits="userSpaceOnUse"><path d="M .8 .8 L 6 3.5 L .8 6.2 Z" fill="${dark}"/></marker>
  <marker id="triangle-${name}" markerWidth="11" markerHeight="9" refX="10" refY="4.5" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 10 4.5 L 1 8 Z" fill="#FFFFFF" stroke="${dark}" stroke-width="1.4"/></marker>`).join("\n");
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

const width = 2450;
const height = 1620;
const body = [
  chip({ x: 1390, y: 78, w: 190, color: "teal", label: "interface" }),
  chip({ x: 1605, y: 78, w: 220, color: "blue", label: "local impl" }),
  chip({ x: 1850, y: 78, w: 220, color: "green", label: "near-cache" }),
  chip({ x: 2095, y: 78, w: 190, color: "amber", label: "support" }),
  `<text class="sectionLabel" x="790" y="205">Coroutine JCache contract</text>`,
  `<text class="sectionLabel" x="390" y="785">Concrete implementations</text>`,
  `<text class="sectionLabel" x="1605" y="785">Two-tier near-cache use</text>`,
  classBox({
    id: "SuspendJCache",
    x: 650,
    y: 245,
    w: 1000,
    h: 385,
    color: "teal",
    stereotype: "<<interface>>",
    title: "SuspendJCache<K,V>",
    attrs: ["+ entries(): Flow<SuspendJCacheEntry<K,V>>", "+ isClosed(): Boolean", "+ unwrap(clazz): T?"],
    methods: ["+ suspend lifecycle: clear() / close()", "+ suspend reads: containsKey() / get()", "+ Flow reads: getAll() / getAll(keys)", "+ suspend writes: put / putAll / putAllFlow / putIfAbsent", "+ suspend mutations: getAndPut / getAndRemove / replace / remove", "+ listener registration / deregistration"],
  }),
  classBox({
    id: "CaffeineSuspendJCache",
    x: 350,
    y: 870,
    w: 640,
    h: 300,
    color: "blue",
    stereotype: "<<class>>",
    title: "CaffeineSuspendJCache<K,V>",
    attrs: ["cache: AsyncCache<K,V>", "closed: atomic Boolean"],
    methods: ["front-cache implementation", "awaits CompletableFuture values", "local listener registration is a no-op", "close() invalidates and cleans up Caffeine"],
  }),
  classBox({
    id: "SuspendNearJCache",
    x: 1200,
    y: 870,
    w: 760,
    h: 330,
    color: "green",
    stereotype: "<<class>>",
    title: "SuspendNearJCache<K,V>",
    attrs: ["frontCache: SuspendJCache<K,V>", "backCache: SuspendJCache<K,V>", "SuspendJCache<K,V> by backCache"],
    methods: ["get() delegates to getDeeply()", "front miss reads back and fills front", "writes update front then back", "withoutListener() supports listener-free backends"],
  }),
  classBox({
    id: "SuspendJCacheEntry",
    x: 1790,
    y: 265,
    w: 470,
    h: 245,
    color: "amber",
    stereotype: "<<data class>>",
    title: "SuspendJCacheEntry<K,V>",
    attrs: ["entryKey: K", "entryValue: V"],
    methods: ["implements Cache.Entry<K,V>", "unwrap only compatible classes"],
  }),
  classBox({
    id: "SuspendJCacheEntryEventListener",
    x: 90,
    y: 265,
    w: 500,
    h: 275,
    color: "pink",
    stereotype: "<<listener>>",
    title: "SuspendJCacheEntryEventListener",
    attrs: ["generic listener for <K,V>", "targetCache: SuspendJCache<K,V>", "scope: SupervisorJob + Dispatchers.IO"],
    methods: ["created/updated -> putAll", "removed/expired -> removeAll", "does not block JCache event thread"],
  }),
  noteBox({
    x: 250,
    y: 1260,
    w: 640,
    h: 190,
    color: "slate",
    title: "Provider boundary",
    lines: ["Caffeine is cache-core local front cache", "Lettuce/Hazelcast/Redisson provide back caches", "All provider back caches implement the same contract"],
  }),
  noteBox({
    x: 1290,
    y: 1270,
    w: 760,
    h: 180,
    color: "slate",
    title: "Reading rule",
    lines: ["Hollow triangle = implements", "Dashed arrow = dependency/return type", "04 explains SuspendJCache only; NearJCache details follow"],
  }),
  edge({ from: "CaffeineSuspendJCache", to: "SuspendJCache", points: [[670, 870], [670, 630]], color: "blue", marker: "triangle", dashed: true, label: "implements", labelAt: [687, 755] }),
  edge({ from: "SuspendNearJCache", to: "SuspendJCache", points: [[1580, 870], [1580, 630]], color: "green", marker: "triangle", dashed: true, label: "implements", labelAt: [1597, 755] }),
  edge({ from: "SuspendJCache", to: "SuspendJCacheEntry", points: [[1650, 405], [1790, 405]], color: "amber", marker: "arrow", dashed: true, label: "Flow entry", labelAt: [1680, 392] }),
  edge({ from: "SuspendJCacheEntryEventListener", to: "SuspendJCache", points: [[590, 405], [650, 405]], color: "pink", marker: "arrow", dashed: true }),
  edge({ from: "SuspendNearJCache", to: "CaffeineSuspendJCache", points: [[1200, 1035], [990, 1035]], color: "blue", marker: "arrow", dashed: true, label: "front", labelAt: [1080, 1022] }),
  edge({ from: "SuspendNearJCache", to: "ProviderBoundary", points: [[1580, 1200], [1580, 1250], [670, 1250], [670, 1260]], color: "slate", marker: "arrow", dashed: true, label: "back implementations", labelAt: [930, 1237] }),
];

const svg = `<svg data-intent="Explain SuspendJCache interface and immediate implementations for cache-core README diagram 04." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="SuspendJCache Interface Class Diagram">
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
<text class="title" x="72" y="86">SuspendJCache Coroutine Interface</text>
<text class="subtitle" x="76" y="120">cache-core JCache-like suspend contract, Flow entry model, local front cache, and two-tier near-cache usage.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "cache-cache-core-diagram-04.svg");
const pngPath = join(OUT, "cache-cache-core-diagram-04.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated cache-cache-core-diagram-04.svg/png");
