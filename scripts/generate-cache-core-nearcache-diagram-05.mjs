#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "cache/cache-core/README.ko.md",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheType.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCaching.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheEntryEventListener.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfig.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilder.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /##### NearJCache \(동기\)[\s\S]*cache-cache-core-diagram-05\.png/, "README NearJCache diagram slot");
assertContains(sources[1], /typealias\s+JCache<K,\s*V>\s*=\s*javax\.cache\.Cache<K,\s*V>/, "JCache typealias");
assertContains(sources[2], /object\s+Caffeine[\s\S]*getOrCreate[\s\S]*:\s*JCache<K,\s*V>/, "JCaching Caffeine front cache provider");
assertContains(sources[3], /class\s+JCacheEntryEventListener<K,\s*V>[\s\S]*targetCache:\s*JCache<K,\s*V>/, "listener targets JCache");
assertContains(sources[4], /class\s+NearJCache<K:\s*Any,\s*V:\s*Any>[\s\S]*frontCache:\s*JCache<K,\s*V>[\s\S]*backCache:\s*JCache<K,\s*V>[\s\S]*:\s*JCache<K,\s*V>\s+by\s+backCache/, "NearJCache front/back delegation");
assertContains(sources[4], /fun\s+getDeeply\(key:\s*K\):\s*V\?[\s\S]*frontCache\.get\(key\)[\s\S]*backCache\.get\(key\)/, "NearJCache front miss back fill");
assertContains(sources[4], /private\s+inline\s+fun\s+syncBackCache[\s\S]*config\.isSynchronous[\s\S]*asyncRunWithTimeout/, "syncBackCache mode");
assertContains(sources[5], /data\s+class\s+NearJCacheConfig<K:\s*Any,\s*V:\s*Any>[\s\S]*cacheManagerFactory[\s\S]*isSynchronous[\s\S]*syncRemoteTimeout/, "NearJCacheConfig settings");
assertContains(sources[6], /class\s+NearJCacheConfigBuilder<K:\s*Any,\s*V:\s*Any>[\s\S]*fun\s+build\(\):\s*NearJCacheConfig<K,\s*V>/, "NearJCacheConfigBuilder DSL");

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
  ${attrs.map((line, index) => `<text class="member" x="${x + 24}" y="${attrY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
  <path class="divider" d="M${x} ${methodY}H${x + w}" stroke="${dark}"/>
  ${methods.map((line, index) => `<text class="member" x="${x + 24}" y="${methodY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function noteBox({ id, x, y, w, h, color, title, lines }) {
  const [fill, stroke] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="noteBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="noteTitle" x="${x + 28}" y="${y + 48}">${esc(title)}</text>
  ${lines.map((line, index) => `<text class="noteLine" x="${x + 30}" y="${y + 92 + index * 30}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function chip({ x, y, w, color, label }) {
  const [fill, stroke] = palette[color];
  return `<g>
  <rect class="chip" x="${x}" y="${y}" width="${w}" height="54" rx="18" fill="${fill}" stroke="${stroke}"/>
  <text class="chipText" x="${x + w / 2}" y="${y + 34}" text-anchor="middle">${esc(label)}</text>
</g>`;
}

function edge({ from, to, points, d, color, marker = "arrow", dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const pathData = d ?? points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${pathData}" stroke="${dark}" marker-end="url(#${marker}-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0] + 8}" y="${p[1] - 8}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2360;
const height = 1670;
const body = [
  chip({ x: 1420, y: 78, w: 170, color: "teal", label: "JCache" }),
  chip({ x: 1615, y: 78, w: 210, color: "green", label: "near-cache" }),
  chip({ x: 1850, y: 78, w: 190, color: "amber", label: "config" }),
  chip({ x: 2065, y: 78, w: 190, color: "pink", label: "listener" }),
  `<text class="sectionLabel" x="880" y="205">JCache contract</text>`,
  `<text class="sectionLabel" x="900" y="770">Two-tier NearJCache</text>`,
  `<text class="sectionLabel" x="335" y="1295">Front cache</text>`,
  `<text class="sectionLabel" x="1510" y="1295">Back cache providers</text>`,
  classBox({
    id: "JCache",
    x: 685,
    y: 245,
    w: 760,
    h: 290,
    color: "teal",
    stereotype: "<<typealias>>",
    title: "JCache<K,V>",
    attrs: ["javax.cache.Cache<K,V>", "iterator(): Cache.Entry<K,V>", "registerCacheEntryListener(...)"],
    methods: ["standard reads: get / getAll / containsKey", "standard writes: put / putAll / putIfAbsent", "mutations: remove / removeAll / replace", "lifecycle: clear / close / isClosed"],
  }),
  classBox({
    id: "NearJCache",
    x: 735,
    y: 840,
    w: 900,
    h: 380,
    color: "green",
    stereotype: "<<class>>",
    title: "NearJCache<K,V>",
    attrs: ["frontCache: JCache<K,V>", "backCache: JCache<K,V>", "config: NearJCacheConfig<K,V>", "JCache<K,V> by backCache"],
    methods: ["get() and getAll() read front only", "getDeeply() fills front after back hit", "put/replace/remove update front first", "syncBackCache() writes back sync or async", "clear() clears front; clearAllCache() clears both"],
  }),
  classBox({
    id: "NearJCacheConfig",
    x: 1730,
    y: 250,
    w: 560,
    h: 315,
    color: "amber",
    stereotype: "<<data class>>",
    title: "NearJCacheConfig<K,V>",
    attrs: ["cacheManagerFactory: Factory<CacheManager>", "cacheName: String", "frontCacheConfiguration: MutableConfiguration<K,V>", "isSynchronous: Boolean", "syncRemoteTimeout: Long"],
    methods: ["default front cache: Caffeine", "default expiry: accessed 30 minutes", "DSL builder creates immutable config"],
  }),
  classBox({
    id: "JCacheEntryEventListener",
    x: 80,
    y: 300,
    w: 520,
    h: 285,
    color: "pink",
    stereotype: "<<listener>>",
    title: "JCacheEntryEventListener",
    attrs: ["generic listener for <K,V>", "targetCache: JCache<K,V>"],
    methods: ["created/updated -> putAll", "removed/expired -> removeAll", "runs on provider event delivery path"],
  }),
  noteBox({
    id: "FrontCache",
    x: 210,
    y: 1340,
    w: 680,
    h: 215,
    color: "blue",
    title: "Local front JCache",
    lines: ["Created by config.cacheManagerFactory", "Default factory loads Caffeine JCache provider", "NearJCache.get() intentionally checks front only", "getDeeply() can refill front from back"],
  }),
  noteBox({
    id: "BackCache",
    x: 1420,
    y: 1340,
    w: 760,
    h: 215,
    color: "slate",
    title: "Remote/back JCache",
    lines: ["Provided by Lettuce, Hazelcast, Redisson, or another JCache", "NearJCache delegates un-overridden JCache methods to backCache", "Back events are registered to update the local front cache", "syncBackCache chooses immediate or async remote write"],
  }),
  edge({ from: "NearJCache", to: "JCache", points: [[1185, 840], [1185, 535]], color: "green", marker: "triangle", dashed: true, label: "implements", labelAt: [1204, 700] }),
  edge({
    from: "NearJCacheConfig",
    to: "NearJCache",
    points: [[2010, 565], [2010, 1030], [1635, 1030]],
    d: "M2010 565 L2010 1018 Q2010 1030 1998 1030 L1635 1030",
    color: "amber",
    marker: "arrow",
    dashed: true,
    label: "configures",
    labelAt: [2022, 815],
  }),
  edge({ from: "JCacheEntryEventListener", to: "JCache", points: [[600, 435], [685, 435]], color: "pink", marker: "arrow", dashed: true }),
  edge({ from: "NearJCache", to: "FrontCache", points: [[550, 1220], [550, 1340]], color: "blue", marker: "arrow", dashed: true, label: "frontCache", labelAt: [550, 1285] }),
  edge({ from: "NearJCache", to: "BackCache", points: [[1455, 1220], [1455, 1340]], color: "slate", marker: "arrow", dashed: true, label: "backCache", labelAt: [1455, 1285] }),
  edge({
    from: "BackCache",
    to: "JCacheEntryEventListener",
    points: [[1800, 1555], [1800, 1590], [118, 1590], [118, 585]],
    d: "M1800 1555 L1800 1578 Q1800 1590 1788 1590 L130 1590 Q118 1590 118 1578 L118 585",
    color: "pink",
    marker: "arrow",
    dashed: true,
    label: "entry events",
    labelAt: [480, 1578],
  }),
];

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="NearJCache Synchronous Class Diagram">
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
<text class="title" x="72" y="86">NearJCache Synchronous Class Diagram</text>
<text class="subtitle" x="76" y="120">cache-core JCache alias, local front cache, remote back cache delegation, listener propagation, and sync/async back writes.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "cache-cache-core-diagram-05.svg");
const pngPath = join(OUT, "cache-cache-core-diagram-05.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated cache-cache-core-diagram-05.svg/png");
