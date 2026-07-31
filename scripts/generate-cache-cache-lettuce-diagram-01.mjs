#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/cache-cache-lettuce-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/cache-cache-lettuce-diagram-01.png";
const W = 2440;
const H = 1608;

const files = {
  readme: "cache/cache-lettuce/README.md",
  factories: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/LettuceCaches.kt",
  near: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt",
  suspendNear: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceSuspendNearCache.kt",
  config: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheConfig.kt",
  listener: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/TrackingInvalidationListener.kt",
  jcache: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt",
};

for (const f of Object.values(files)) if (!existsSync(f)) throw new Error(`Missing source evidence: ${f}`);
function need(file, pattern, label) {
  if (!pattern.test(readFileSync(file, "utf8"))) throw new Error(`Expected ${label} in ${file}`);
}

need(files.readme, /LettuceNearCache Class Hierarchy[\s\S]*cache-cache-lettuce-diagram-01\.png/, "README diagram slot");
need(files.factories, /object LettuceCaches[\s\S]*nearJCache\([\s\S]*suspendNearJCache\([\s\S]*nearCache\([\s\S]*suspendNearCache\(/, "LettuceCaches factories");
need(files.near, /class LettuceNearCache<V:\s*Any>[\s\S]*:\s*NearCacheOperations<V>[\s\S]*TrackingInvalidationListener/, "blocking native near cache");
need(files.suspendNear, /class LettuceSuspendNearCache<V:\s*Any>[\s\S]*:\s*SuspendNearCacheOperations<V>[\s\S]*CancellationException/, "suspend native near cache");
need(files.config, /data class LettuceNearCacheConfig[\s\S]*redisKey\(key: String\)[\s\S]*LettuceNearCacheConfigBuilder/, "native config");
need(files.listener, /class TrackingInvalidationListener[\s\S]*frontCache\.invalidateAll[\s\S]*clientTracking\(trackingEnabled\)/, "RESP3 invalidation");
need(files.jcache, /class LettuceJCache<K:\s*Any,\s*V:\s*Any>[\s\S]*javax\.cache\.Cache[\s\S]*putAll\(map: Map/, "Lettuce JCache bridge");

const C = {
  ink: "#0F172A", muted: "#475569", canvas: "#F8FAFC", frame: "#FFFFFF", line: "#CBD5E1",
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  orange: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  purple: ["#FAF5FF", "#7C3AED", "#6D28D9"],
  slate: ["#F8FAFC", "#64748B", "#475569"],
};

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}
function markerDefs() {
  return Object.entries(C).filter(([, v]) => Array.isArray(v)).map(([name, [, , dark]]) => `
    <marker id="open-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 19 11 L 2 20" fill="none" stroke="${dark}" stroke-width="2.7" stroke-linecap="round" stroke-linejoin="round"/></marker>
    <marker id="triangle-${name}" markerWidth="26" markerHeight="22" refX="23" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 23 11 L 2 20 Z" fill="#FFFFFF" stroke="${dark}" stroke-width="2.4"/></marker>`).join("");
}
function card({ id, x, y, w, h, color, stereo, title, attrs = [], methods = [] }) {
  const [fill, stroke, dark] = C[color];
  const attrY = y + 78;
  const methodY = attrY + 28 + Math.max(34, attrs.length * 24);
  return `<g id="${esc(id)}">
    <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
    <text class="stereo" x="${x + w / 2}" y="${y + 28}" text-anchor="middle">${esc(stereo)}</text>
    <text class="cardTitle" x="${x + w / 2}" y="${y + 58}" text-anchor="middle">${esc(title)}</text>
    <path class="divider" d="M${x} ${attrY}H${x + w}" stroke="${dark}"/>
    ${attrs.map((line, i) => `<text class="member" x="${x + 34}" y="${attrY + 28 + i * 24}">${esc(line)}</text>`).join("")}
    <path class="divider" d="M${x} ${methodY}H${x + w}" stroke="${dark}"/>
    ${methods.map((line, i) => `<text class="member" x="${x + 34}" y="${methodY + 28 + i * 24}">${esc(line)}</text>`).join("")}
  </g>`;
}
function note({ x, y, w, h, color, title, lines }) {
  const [fill, stroke] = C[color];
  return `<g><rect class="note" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
    <text class="noteTitle" x="${x + 34}" y="${y + 45}">${esc(title)}</text>
    ${lines.map((line, i) => `<text class="noteLine" x="${x + 34}" y="${y + 82 + i * 28}">${esc(line)}</text>`).join("")}
  </g>`;
}
function edge({ points, color, marker = "open", dashed = true, label = "", labelAt }) {
  const [, , dark] = C[color];
  const d = points.map((p, i) => {
    if (i === 0) return `M${p[0]} ${p[1]}`;
    if (i === points.length - 1) return `L${p[0]} ${p[1]}`;
    const previous = points[i - 1], next = points[i + 1];
    return `L${(previous[0] + p[0]) / 2} ${(previous[1] + p[1]) / 2} Q${p[0]} ${p[1]} ${(p[0] + next[0]) / 2} ${(p[1] + next[1]) / 2}`;
  }).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g><path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#${marker}-${color})"/>
    ${label ? `<text class="edgeLabel" x="${p[0] + 8}" y="${p[1] - 8}">${esc(label)}</text>` : ""}</g>`;
}

const body = [
  card({ id: "NearCacheOperations", x: 110, y: 240, w: 520, h: 210, color: "blue", stereo: "<<interface>>", title: "NearCacheOperations<V>", attrs: ["blocking near-cache contract"], methods: ["get/put/remove/replace", "clearLocal/clearAll/stats"] }),
  card({ id: "SuspendNearCacheOperations", x: 1810, y: 240, w: 520, h: 210, color: "teal", stereo: "<<interface>>", title: "SuspendNearCacheOperations<V>", attrs: ["coroutine near-cache contract"], methods: ["suspend get/put/remove", "clearLocal/clearAll/stats"] }),
  card({ id: "LettuceNearCache", x: 110, y: 610, w: 670, h: 330, color: "blue", stereo: "<<class>>", title: "LettuceNearCache<V>", attrs: ["frontCache: LettuceLocalCache<String,V>", "commands: RedisCommands<String,V>", "trackingListener: TrackingInvalidationListener<V>"], methods: ["front hit -> return; miss -> Redis GET + fill", "write-through SET/MSET + front update", "replace(old,new): EVALSHA then EVAL fallback", "remove/clearBack use UNLINK/SCAN"] }),
  card({ id: "LettuceSuspendNearCache", x: 1660, y: 610, w: 670, h: 330, color: "teal", stereo: "<<class>>", title: "LettuceSuspendNearCache<V>", attrs: ["frontCache: LettuceLocalCache<String,V>", "commands: RedisCoroutinesCommands<String,V>", "asyncCommands reused for hot paths"], methods: ["suspend read/write mirrors native behavior", "CancellationException is not hidden", "replace(old,new): EVALSHA then EVAL fallback", "close() shuts listener, connection, front cache"] }),
  card({ id: "LettuceLocalCache", x: 130, y: 1135, w: 520, h: 250, color: "green", stereo: "<<interface + impl>>", title: "LettuceLocalCache / Caffeine", attrs: ["local L1 cache", "optional stats"], methods: ["get/put/remove/clear", "invalidateAll from Redis tracking"] }),
  card({ id: "Redis", x: 960, y: 1135, w: 520, h: 250, color: "slate", stereo: "<<Redis via Lettuce>>", title: "Remote Back Cache", attrs: ["prefixed key: cacheName:key", "RESP3 CLIENT TRACKING"], methods: ["GET/MGET, SET/MSET", "UNLINK for delete", "SCAN cacheName:* for clearBack"] }),
  card({ id: "TrackingInvalidationListener", x: 1770, y: 1135, w: 520, h: 250, color: "pink", stereo: "<<listener>>", title: "TrackingInvalidationListener", attrs: ["PushListener on invalidate", "cacheName prefix filter"], methods: ["null payload -> front clear", "matching keys -> invalidateAll", "CLIENT TRACKING ON NOLOOP"] }),
  card({ id: "Config", x: 915, y: 610, w: 610, h: 240, color: "orange", stereo: "<<data class + builder>>", title: "LettuceNearCacheConfig<K,V>", attrs: ["cacheName, maxLocalSize", "frontExpireAfterWrite / redisTtl", "useRespProtocol3, recordStats"], methods: ["redisKey(key) => cacheName:key", "validates positive sizes and TTL"] }),
  note({ x: 845, y: 260, w: 745, h: 160, color: "purple", title: "LettuceCaches factory surface", lines: ["native: nearCache(), suspendNearCache()", "JCache bridge: jcache(), suspendJCache(), nearJCache(), suspendNearJCache()", "returns provider-neutral contracts where possible"] }),
  note({ x: 840, y: 920, w: 760, h: 145, color: "green", title: "JCache bridge remains separate", lines: ["LettuceJCache stores entries in Redis hash fields", "NearJCache/SuspendNearJCache wrap the shared cache-core two-tier JCache types"] }),
  edge({ points: [[445, 610], [445, 450]], color: "blue", marker: "triangle", label: "implements", labelAt: [462, 540] }),
  edge({ points: [[1995, 610], [1995, 450]], color: "teal", marker: "triangle", label: "implements", labelAt: [2012, 540] }),
  edge({ points: [[780, 735], [915, 735]], color: "orange", label: "uses config", labelAt: [810, 722] }),
  edge({ points: [[1660, 735], [1525, 735]], color: "orange", label: "uses config", labelAt: [1560, 722] }),
  edge({ points: [[455, 940], [455, 1135]], color: "green", label: "L1 front", labelAt: [472, 1048] }),
  edge({ points: [[1220, 850], [1640, 850], [1640, 1110], [1220, 1110], [1220, 1135]], color: "slate", label: "L2 Redis", labelAt: [1658, 990] }),
  edge({ points: [[1995, 940], [1995, 1135]], color: "pink", label: "tracking", labelAt: [2012, 1048] }),
  edge({ points: [[650, 1260], [960, 1260]], color: "slate", label: "miss/write", labelAt: [750, 1247] }),
  edge({ points: [[1480, 1260], [1770, 1260]], color: "pink", label: "invalidate push", labelAt: [1588, 1247] }),
  edge({ points: [[1770, 1320], [1700, 1320], [1700, 1425], [650, 1425], [650, 1320]], color: "green", label: "local invalidation", labelAt: [1135, 1412] }),
];

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="LettuceNearCache class hierarchy">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity=".10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:${C.canvas}}.frame{fill:${C.frame};stroke:${C.line};stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:${C.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${C.muted}}
    .card,.note{stroke-width:1.8;filter:url(#shadow)}.stereo{font-family:"Comic Mono";font-size:13px;fill:${C.muted}}.cardTitle,.noteTitle{font-family:"Architects Daughter";font-size:27px;fill:${C.ink}}
    .member,.noteLine{font-family:"Comic Mono";font-size:12px;fill:#334155}.divider{stroke-width:1.1;opacity:.45}
    .edge{fill:none;stroke-width:3.4;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:${C.muted}}
  </style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 64}" rx="8"/>
<text class="title" x="72" y="88">LettuceNearCache Class Hierarchy</text>
<text class="subtitle" x="76" y="121">Native Lettuce two-tier caches, shared config, Redis tracking invalidation, and the JCache bridge boundary.</text>
${body.join("\n")}
</svg>`;

writeFileSync(svgPath, `${svg.replace(/[ \t]+$/gm, "")}\n`);
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
