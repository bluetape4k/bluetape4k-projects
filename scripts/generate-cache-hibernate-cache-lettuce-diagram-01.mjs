#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "cache/hibernate-cache-lettuce/README.md",
  "cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheProperties.kt",
  "cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt",
  "cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Near Cache 2-Tier Structure[\s\S]*cache-hibernate-cache-lettuce-diagram-01\.png/, "README near cache slot");
assertContains(sources[1], /buildNearCacheConfig[\s\S]*maxLocalSize[\s\S]*redisTtl[\s\S]*useRespProtocol3/, "near cache config properties");
assertContains(sources[2], /caches\.computeIfAbsent\(regionName\)[\s\S]*LettuceNearCache\(client, codec, properties\.buildNearCacheConfig\(regionName\)\)/, "per-region near cache creation");
assertContains(sources[3], /getFromCache[\s\S]*nearCache\.get\(cacheKey\(key\)\)[\s\S]*putIntoCache[\s\S]*nearCache\.put/, "storage access get and put");
assertContains(sources[3], /evictData\(key: Any\)[\s\S]*nearCache\.remove[\s\S]*evictData\(\)[\s\S]*nearCache\.clearAll/, "key and region evict");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  slate: ["#F8FAFC", "#64748B", "#475569"],
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

function card({ id, x, y, w, h, color, kicker, title, lines = [], footer = "" }) {
  const [fill, stroke, dark] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="kicker" x="${x + 22}" y="${y + 31}">${esc(kicker)}</text>
  <text class="cardTitle" x="${x + 22}" y="${y + 64}">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 86}H${x + w}" stroke="${dark}"/>
  ${lines.map((line, index) => `<text class="body" x="${x + 22}" y="${y + 118 + index * 24}">${esc(line)}</text>`).join("\n")}
  ${footer ? `<path class="divider" d="M${x} ${y + h - 46}H${x + w}" stroke="${dark}"/><text class="foot" x="${x + 22}" y="${y + h - 17}">${esc(footer)}</text>` : ""}
</g>`;
}

function edge({ from, to, points, color, dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0]}" y="${p[1]}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2700;
const height = 1260;
const body = [
  card({
    id: "HibernateRegion",
    x: 120,
    y: 265,
    w: 470,
    h: 250,
    color: "slate",
    kicker: "Hibernate ORM 7.2",
    title: "2nd Level Cache region",
    lines: ["entity, collection, query, timestamp regions", "AccessType.NONSTRICT_READ_WRITE", "Hibernate calls StorageAccess APIs"],
    footer: "regionName becomes the cache boundary",
  }),
  card({
    id: "RegionFactory",
    x: 740,
    y: 220,
    w: 540,
    h: 300,
    color: "blue",
    kicker: "RegionFactoryTemplate",
    title: "LettuceNearCacheRegionFactory",
    lines: ["parses hibernate.cache.lettuce.* properties", "creates one RedisClient and one codec", "caches.computeIfAbsent(regionName)", "shares LettuceNearCache per region"],
    footer: "owns cache and RedisClient lifecycle",
  }),
  card({
    id: "Properties",
    x: 740,
    y: 820,
    w: 540,
    h: 280,
    color: "green",
    kicker: "configuration parser",
    title: "LettuceNearCacheProperties",
    lines: ["redis_uri, codec, use_resp3", "local.max_size and expire_after_write", "redis_ttl.default plus per-region TTLs", "buildNearCacheConfig(regionName)"],
    footer: "timestamp region disables Redis TTL",
  }),
  card({
    id: "StorageAccess",
    x: 1430,
    y: 265,
    w: 520,
    h: 310,
    color: "teal",
    kicker: "DomainDataStorageAccess",
    title: "LettuceNearCacheStorageAccess",
    lines: ["normalizes Hibernate cache keys", "getFromCache -> nearCache.get(key)", "putIntoCache -> nearCache.put(key, value)", "evictData -> remove or clearAll"],
    footer: "Redis failures are logged and ignored or return null",
  }),
  card({
    id: "NearCache",
    x: 1430,
    y: 820,
    w: 520,
    h: 280,
    color: "violet",
    kicker: "per-region near cache",
    title: "LettuceNearCache",
    lines: ["cacheName == regionName", "read path: L1 first, then Redis L2", "write-through: local and Redis are updated", "evict: key removal or whole-region clearAll"],
    footer: "RegionFactory reuses the same instance per region",
  }),
  card({
    id: "L1",
    x: 2130,
    y: 640,
    w: 420,
    h: 230,
    color: "amber",
    kicker: "tier 1",
    title: "Caffeine local cache",
    lines: ["fast in-process reads", "maxLocalSize bound", "expireAfterWrite controls freshness"],
    footer: "cleared on evict and invalidation",
  }),
  card({
    id: "L2",
    x: 2130,
    y: 950,
    w: 420,
    h: 250,
    color: "pink",
    kicker: "tier 2",
    title: "Redis backing cache",
    lines: ["shared distributed cache", "region-prefixed keys avoid collisions", "codec selected from LettuceBinaryCodecs", "TTL comes from default or region override"],
    footer: "RESP3 enables client tracking",
  }),
  card({
    id: "DbFallback",
    x: 120,
    y: 900,
    w: 470,
    h: 220,
    color: "slate",
    kicker: "miss / failure behavior",
    title: "Database fallback",
    lines: ["get failure returns null", "Hibernate can load from DB", "put/evict failures do not break transactions"],
    footer: "cache is an optimization layer",
  }),
  edge({ from: "HibernateRegion", to: "RegionFactory", points: [[590, 390], [665, 350], [740, 360]], color: "blue", label: "requests region access", labelAt: [610, 330] }),
  edge({ from: "Properties", to: "RegionFactory", points: [[1010, 820], [1010, 520]], color: "green", dashed: true, label: "builds config and codec", labelAt: [1028, 690] }),
  edge({ from: "RegionFactory", to: "StorageAccess", points: [[1280, 390], [1365, 330], [1430, 365]], color: "teal", label: "creates StorageAccess", labelAt: [1308, 318] }),
  edge({ from: "StorageAccess", to: "NearCache", points: [[1690, 575], [1690, 820]], color: "violet", label: "delegates operations", labelAt: [1710, 705] }),
  edge({ from: "NearCache", to: "L1", points: [[1950, 895], [2040, 842], [2130, 765]], color: "amber", label: "L1 read/write/evict", labelAt: [1985, 848] }),
  edge({ from: "NearCache", to: "L2", points: [[1950, 1010], [2040, 1060], [2130, 1080]], color: "pink", label: "L2 read/write/evict", labelAt: [1982, 1058] }),
  edge({ from: "L2", to: "L1", points: [[2340, 950], [2340, 870]], color: "pink", dashed: true, label: "client tracking invalidation", labelAt: [2358, 920] }),
  edge({ from: "StorageAccess", to: "DbFallback", points: [[1430, 470], [1340, 470], [1340, 720], [360, 720], [360, 900]], color: "slate", dashed: true, label: "null on cache read failure", labelAt: [760, 700] }),
];

const svg = `<svg data-intent="Explain Hibernate Lettuce near cache two-tier structure for README diagram 01." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Hibernate Lettuce Near Cache Two Tier Structure Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:47px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .card{stroke-width:1.8;filter:url(#shadow)}.kicker{font-family:"Comic Mono";font-size:14px;fill:#475569}.cardTitle{font-family:"Architects Daughter";font-size:25px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:14px;fill:#334155}.foot{font-family:"Comic Mono";font-size:13px;fill:#475569}.divider{stroke-width:1.1;opacity:.42}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">Hibernate Lettuce Near Cache 2-Tier Structure</text>
<text class="subtitle" x="76" y="120">RegionFactory creates one region-scoped LettuceNearCache that fronts Redis with Caffeine, while StorageAccess bridges Hibernate cache operations.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "cache-hibernate-cache-lettuce-diagram-01.svg");
const pngPath = join(OUT, "cache-hibernate-cache-lettuce-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated cache-hibernate-cache-lettuce-diagram-01.svg/png");
