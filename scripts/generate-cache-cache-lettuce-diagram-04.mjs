#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/cache-cache-lettuce-diagram-04.svg";
const pngPath = "docs/images/readme-diagrams/cache-cache-lettuce-diagram-04.png";
const W = 2226;
const H = 1380;

const files = {
  readme: "cache/cache-lettuce/README.ko.md",
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

need(files.readme, /Native Lettuce NearCache Structure[\s\S]*cache-cache-lettuce-diagram-04\.png/, "README diagram slot");
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
function edge({ from, to, points, color, marker = "open", dashed = true, label = "", labelAt }) {
  const [, , dark] = C[color];
  const d = points.map((p, i) => {
    if (i === 0) return `M${p[0]} ${p[1]}`;
    if (i === points.length - 1) return `L${p[0]} ${p[1]}`;
    const previous = points[i - 1], next = points[i + 1];
    return `L${(previous[0] + p[0]) / 2} ${(previous[1] + p[1]) / 2} Q${p[0]} ${p[1]} ${(p[0] + next[0]) / 2} ${(p[1] + next[1]) / 2}`;
  }).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g data-from="${esc(from)}" data-to="${esc(to)}"><path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#${marker}-${color})"/>
    ${label ? `<text class="edgeLabel" x="${p[0] + 8}" y="${p[1] - 8}">${esc(label)}</text>` : ""}</g>`;
}

const body = [
  note({ x: 92, y: 210, w: 410, h: 150, color: "purple", title: "Application call site", lines: ["LettuceCaches.nearCache()", "LettuceCaches.suspendNearCache()", "same 2-tier contract, different call style"] }),
  card({ id: "SyncFacade", x: 665, y: 190, w: 560, h: 265, color: "blue", stereo: "<<blocking facade>>", title: "LettuceNearCache<V>", attrs: ["NearCacheOperations<V>", "RedisCommands<String,V>"], methods: ["get: L1 hit or Redis GET + fill", "put/remove: write-through", "replace: Lua CAS with fallback"] }),
  card({ id: "SuspendFacade", x: 1490, y: 190, w: 560, h: 265, color: "teal", stereo: "<<coroutine facade>>", title: "LettuceSuspendNearCache<V>", attrs: ["SuspendNearCacheOperations<V>", "RedisCoroutinesCommands<String,V>"], methods: ["suspend get/put/remove", "preserves CancellationException", "close releases listener/connection"] }),
  card({ id: "Config", x: 825, y: 580, w: 610, h: 220, color: "orange", stereo: "<<shared config>>", title: "LettuceNearCacheConfig<K,V>", attrs: ["cacheName -> redis key prefix", "maxLocalSize, front TTL, redis TTL"], methods: ["redisKey(key) = cacheName:key", "RESP3 tracking and stats toggles"] }),
  card({ id: "Front", x: 265, y: 900, w: 520, h: 235, color: "green", stereo: "<<L1 local>>", title: "Caffeine Local Cache", attrs: ["fast front-cache hit path", "optional stats"], methods: ["get/put/remove/clear", "invalidateAll(keys) from tracking"] }),
  card({ id: "Redis", x: 875, y: 900, w: 520, h: 235, color: "slate", stereo: "<<L2 remote>>", title: "Redis via Lettuce", attrs: ["keys are isolated by cacheName", "RESP3 CLIENT TRACKING enabled"], methods: ["GET/MGET, SET/MSET", "UNLINK + SCAN for bulk cleanup"] }),
  card({ id: "Listener", x: 1485, y: 900, w: 520, h: 235, color: "pink", stereo: "<<push listener>>", title: "TrackingInvalidationListener", attrs: ["listens for Redis invalidation pushes", "filters cacheName-prefixed keys"], methods: ["null payload -> clear front", "matching keys -> invalidate local entries"] }),
  note({ x: 500, y: 1200, w: 1260, h: 96, color: "green", title: "Runtime guarantee", lines: ["Reads prefer L1, misses hydrate from Redis, writes update Redis and front cache, and Redis tracking pushes keep peer front caches coherent."] }),
  edge({ from: "Application", to: "SyncFacade", points: [[502, 285], [665, 285]], color: "blue", label: "sync API", labelAt: [545, 272] }),
  edge({ from: "Application", to: "SuspendFacade", points: [[300, 210], [300, 145], [1770, 145], [1770, 190]], color: "teal", label: "suspend API", labelAt: [1030, 132] }),
  edge({ from: "SyncFacade", to: "Config", points: [[945, 455], [945, 580]], color: "orange", label: "configured by", labelAt: [962, 520] }),
  edge({ from: "SuspendFacade", to: "Config", points: [[1770, 455], [1770, 520], [1315, 520], [1315, 580]], color: "orange", label: "configured by", labelAt: [1788, 505] }),
  edge({ from: "Config", to: "Front", points: [[945, 800], [945, 850], [525, 850], [525, 900]], color: "green", label: "front cache", labelAt: [600, 837] }),
  edge({ from: "Config", to: "Redis", points: [[1130, 800], [1130, 900]], color: "slate", label: "remote keyspace", labelAt: [1148, 860] }),
  edge({ from: "Config", to: "Listener", points: [[1315, 800], [1315, 850], [1745, 850], [1745, 900]], color: "pink", label: "tracking listener", labelAt: [1485, 837] }),
  edge({ from: "Front", to: "Redis", points: [[785, 1018], [875, 1018]], color: "slate", label: "miss/write", labelAt: [802, 1005] }),
  edge({ from: "Redis", to: "Listener", points: [[1395, 1018], [1485, 1018]], color: "pink", label: "invalidate push", labelAt: [1408, 1005] }),
  edge({ from: "Listener", to: "Front", points: [[1485, 1082], [1425, 1082], [1425, 1170], [525, 1170], [525, 1135]], color: "green", label: "local invalidate", labelAt: [930, 1157] }),
];

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="LettuceNearCache class hierarchy" data-allow-grid="true">
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
<text class="title" x="72" y="88">Native Lettuce NearCache Structure</text>
<text class="subtitle" x="76" y="121">Runtime structure for native sync/suspend Lettuce NearCache, L1/L2 access, and RESP3 invalidation.</text>
${body.join("\n")}
</svg>`;

writeFileSync(svgPath, `${svg.replace(/[ \t]+$/gm, "")}\n`);
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
