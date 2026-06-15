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
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListener.kt",
  "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCache.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /##### SuspendNearJCache \(코루틴\)[\s\S]*cache-cache-core-diagram-06\.png/, "README SuspendNearJCache diagram slot");
assertContains(sources[1], /interface\s+SuspendJCache<K:\s*Any,\s*V:\s*Any>[\s\S]*putAllFlow[\s\S]*registerCacheEntryListener/, "SuspendJCache contract");
assertContains(sources[2], /class\s+CaffeineSuspendJCache<K:\s*Any,\s*V:\s*Any>[\s\S]*AsyncCache<K,\s*V>[\s\S]*await\(\)/, "Caffeine suspend front cache");
assertContains(sources[3], /CoroutineScope\(SupervisorJob\(\) \+ Dispatchers\.IO\)[\s\S]*scope\.launch[\s\S]*targetCache\.putAll/, "listener coroutine scope update");
assertContains(sources[4], /class\s+SuspendNearJCache<K:\s*Any,\s*V:\s*Any>[\s\S]*:\s*SuspendJCache<K,\s*V>\s+by\s+backCache/, "SuspendNearJCache contract delegation");
assertContains(sources[4], /getDeeply\(key:\s*K\)[\s\S]*frontCache\.get\(key\)[\s\S]*backCache\.get\(key\)\?\.also\s*\{\s*value\s*->\s*frontCache\.put\(key,\s*value\)\s*\}/, "read miss back fill");
assertContains(sources[4], /override\s+suspend\s+fun\s+put\(key:\s*K,\s*value:\s*V\)[\s\S]*frontCache\.put\(key,\s*value\)[\s\S]*backCache\.put\(key,\s*value\)/, "write front then back");
assertContains(sources[4], /fun\s+<K:\s*Any,\s*V:\s*Any>\s+withoutListener/, "listener-free constructor");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  purple: ["#F5F3FF", "#7C3AED", "#6D28D9"],
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

function card({ id, x, y, w, h, color, title, subtitle = "", lines = [] }) {
  const [fill, stroke, dark] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="cardTitle" x="${x + 26}" y="${y + 46}">${esc(title)}</text>
  ${subtitle ? `<text class="cardSub" x="${x + 28}" y="${y + 74}">${esc(subtitle)}</text>` : ""}
  <path class="divider" d="M${x} ${y + 92}H${x + w}" stroke="${dark}"/>
  ${lines.map((line, index) => `<text class="line" x="${x + 28}" y="${y + 126 + index * 29}">${esc(line)}</text>`).join("\n")}
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
const height = 1680;
const body = [
  chip({ x: 1470, y: 78, w: 210, color: "teal", label: "contract" }),
  chip({ x: 1705, y: 78, w: 210, color: "green", label: "near-cache" }),
  chip({ x: 1940, y: 78, w: 190, color: "blue", label: "front" }),
  chip({ x: 2155, y: 78, w: 190, color: "slate", label: "back" }),
  chip({ x: 2370, y: 78, w: 160, color: "pink", label: "events" }),
  card({
    id: "SuspendJCache",
    x: 120,
    y: 230,
    w: 610,
    h: 270,
    color: "teal",
    title: "SuspendJCache<K,V>",
    subtitle: "Coroutine JCache-like contract",
    lines: ["suspend reads and writes", "Flow entries and putAllFlow()", "listener registration contract", "unwrap() keeps provider escape hatch"],
  }),
  card({
    id: "SuspendNearJCache",
    x: 900,
    y: 220,
    w: 790,
    h: 330,
    color: "green",
    title: "SuspendNearJCache<K,V>",
    subtitle: "2-tier cache that implements SuspendJCache by backCache",
    lines: ["frontCache: SuspendJCache<K,V>", "backCache: SuspendJCache<K,V>", "invoke() registers a back-cache listener", "withoutListener() supports providers that cannot serialize listeners", "clear() closes front scope; clearAll() clears both tiers"],
  }),
  card({
    id: "Listener",
    x: 1880,
    y: 250,
    w: 600,
    h: 280,
    color: "pink",
    title: "SuspendJCacheEntryEventListener",
    subtitle: "Back-cache events update the local front cache",
    lines: ["CoroutineScope(SupervisorJob + Dispatchers.IO)", "created/updated -> targetCache.putAll(...)", "removed/expired -> targetCache.removeAll(...)", "launch() avoids blocking JCache event threads"],
  }),
  card({
    id: "ReadPath",
    x: 190,
    y: 720,
    w: 620,
    h: 280,
    color: "purple",
    title: "Read path",
    subtitle: "get() delegates to getDeeply()",
    lines: ["1. frontCache.get(key)", "2. on miss, backCache.get(key)", "3. back hit fills frontCache.put(key, value)", "getAll() and entries() read front cache"],
  }),
  card({
    id: "WritePath",
    x: 990,
    y: 735,
    w: 620,
    h: 260,
    color: "amber",
    title: "Write and mutation path",
    subtitle: "front first, then back for propagation",
    lines: ["put(), putAll(), putIfAbsent()", "remove(), removeAll(), replace()", "putAllFlow() collects Flow pairs", "Redisson bulk-event gaps are handled explicitly"],
  }),
  card({
    id: "FlowPath",
    x: 1820,
    y: 740,
    w: 600,
    h: 245,
    color: "slate",
    title: "Flow surface",
    subtitle: "Streaming view over the local tier",
    lines: ["entries(): Flow<SuspendJCacheEntry<K,V>>", "getAll(keys): Flow<SuspendJCacheEntry<K,V>>", "Caffeine front awaits AsyncCache futures"],
  }),
  card({
    id: "FrontCache",
    x: 300,
    y: 1220,
    w: 680,
    h: 235,
    color: "blue",
    title: "Local front SuspendJCache",
    subtitle: "Usually CaffeineSuspendJCache in cache-core",
    lines: ["AsyncCache<K,V> + CompletableFuture.await()", "fast front hit path", "listener target for remote changes", "local listener registration is intentionally a no-op"],
  }),
  card({
    id: "BackCache",
    x: 1510,
    y: 1220,
    w: 760,
    h: 235,
    color: "slate",
    title: "Remote/back SuspendJCache",
    subtitle: "Provided by Lettuce, Hazelcast, Redisson, or another adapter",
    lines: ["delegated methods keep provider semantics", "registered listener emits created/updated/removed/expired events", "writes and removals propagate through this tier", "bulk remove/replace paths account for provider event gaps"],
  }),
  edge({ from: "SuspendNearJCache", to: "SuspendJCache", points: [[900, 385], [730, 385]], color: "green", marker: "triangle", dashed: true, label: "implements", labelAt: [745, 372] }),
  edge({ from: "SuspendNearJCache", to: "Listener", points: [[1690, 390], [1880, 390]], color: "pink", marker: "arrow", dashed: true, label: "registers", labelAt: [1750, 377] }),
  edge({ from: "SuspendNearJCache", to: "ReadPath", points: [[1080, 550], [1080, 650], [500, 650], [500, 720]], color: "purple", marker: "arrow", dashed: true, label: "read API", labelAt: [690, 637] }),
  edge({ from: "SuspendNearJCache", to: "WritePath", points: [[1295, 550], [1295, 735]], color: "amber", marker: "arrow", dashed: true, label: "write API", labelAt: [1312, 655] }),
  edge({ from: "SuspendNearJCache", to: "FlowPath", points: [[1508, 550], [1508, 650], [2110, 650], [2110, 740]], color: "slate", marker: "arrow", dashed: true, label: "Flow reads", labelAt: [1760, 637] }),
  edge({ from: "ReadPath", to: "FrontCache", points: [[500, 1000], [500, 1220]], color: "blue", marker: "arrow", dashed: true, label: "front first", labelAt: [518, 1110] }),
  edge({ from: "WritePath", to: "FrontCache", points: [[990, 875], [860, 875], [860, 1168], [790, 1168], [790, 1220]], color: "blue", marker: "arrow", dashed: true, label: "front update", labelAt: [878, 1020] }),
  edge({ from: "WritePath", to: "BackCache", points: [[1610, 875], [1740, 875], [1740, 1168], [1840, 1168], [1840, 1220]], color: "slate", marker: "arrow", dashed: true, label: "back update", labelAt: [1758, 1026] }),
  edge({ from: "ReadPath", to: "BackCache", points: [[810, 842], [890, 842], [890, 1108], [1470, 1108], [1470, 1260], [1510, 1260]], color: "slate", marker: "arrow", dashed: true, label: "miss lookup", labelAt: [1060, 1095] }),
  edge({ from: "BackCache", to: "FrontCache", points: [[1510, 1348], [1220, 1348], [980, 1348]], color: "blue", marker: "arrow", dashed: true, label: "fill or event sync", labelAt: [1140, 1335] }),
  edge({ from: "BackCache", to: "Listener", points: [[2270, 1340], [2530, 1340], [2530, 390], [2480, 390]], color: "pink", marker: "arrow", dashed: true, label: "entry events", labelAt: [2345, 1010] }),
];

const svg = `<svg data-intent="Explain SuspendNearJCache coroutine two-tier operations for cache-core README diagram 06." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="SuspendNearJCache Coroutine Operation Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .chip{stroke-width:1.6}.chipText{font-family:"Comic Mono";font-size:14px;fill:#334155}
    .card{stroke-width:1.8;filter:url(#shadow)}.cardTitle{font-family:"Architects Daughter";font-size:28px;fill:#0F172A}.cardSub{font-family:"Comic Mono";font-size:14px;fill:#475569}
    .line{font-family:"Comic Mono";font-size:14px;fill:#334155}.divider{stroke-width:1.1;opacity:.45}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">SuspendNearJCache Coroutine Operation Map</text>
<text class="subtitle" x="76" y="120">Read, write, Flow, and listener-driven synchronization paths for cache-core's coroutine two-tier near cache.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "cache-cache-core-diagram-06.svg");
const pngPath = join(OUT, "cache-cache-core-diagram-06.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated cache-cache-core-diagram-06.svg/png");
