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

assertContains(sources[0], /Layer Structure[\s\S]*cache-hibernate-cache-lettuce-diagram-02\.png/, "README layer structure slot");
assertContains(sources[1], /LettuceNearCacheConfig[\s\S]*createCodec[\s\S]*buildNearCacheConfig/, "properties to codec/config layer");
assertContains(sources[2], /RegionFactoryTemplate[\s\S]*createDomainDataStorageAccess[\s\S]*createStorageAccess/, "Hibernate SPI layer");
assertContains(sources[2], /RedisClient\.create[\s\S]*ProtocolVersion\.RESP3[\s\S]*ShutdownQueue\.register/, "Lettuce client lifecycle layer");
assertContains(sources[3], /DomainDataStorageAccess[\s\S]*cacheKey[\s\S]*nearCache\.(get|put|remove|clearAll)/, "StorageAccess bridge layer");

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

function band({ id, x, y, w, h, color, label }) {
  const [fill, stroke] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="band" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="bandLabel" x="${x + 22}" y="${y + 34}">${esc(label)}</text>
</g>`;
}

function card({ id, x, y, w, h, color, title, lines = [] }) {
  const [fill, stroke, dark] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="cardTitle" x="${x + 28}" y="${y + 38}">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 58}H${x + w}" stroke="${dark}"/>
  ${lines.map((line, index) => `<text class="body" x="${x + 34}" y="${y + 88 + index * 23}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function edge({ from, to, points, color, dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => {
    if (index === 0) return `M${point[0]} ${point[1]}`;
    if (index === points.length - 1) return `L${point[0]} ${point[1]}`;
    const previous = points[index - 1], next = points[index + 1];
    return `L${(previous[0] + point[0]) / 2} ${(previous[1] + point[1]) / 2} Q${point[0]} ${point[1]} ${(point[0] + next[0]) / 2} ${(point[1] + next[1]) / 2}`;
  }).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0]}" y="${p[1]}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2455;
const height = 1620;
const left = 90;
const bandWidth = 2275;
const body = [
  `<metadata data-allow-grid="true"/>`,
  band({ id: "ConfigLayer", x: left, y: 180, w: bandWidth, h: 210, color: "green", label: "configuration layer" }),
  card({ id: "HibernateProps", x: 260, y: 230, w: 610, h: 125, color: "green", title: "hibernate.cache.lettuce.*", lines: ["redis_uri, codec, use_resp3", "local sizing, local expiry, Redis TTL overrides"] }),
  card({ id: "Properties", x: 1060, y: 230, w: 610, h: 125, color: "green", title: "LettuceNearCacheProperties", lines: ["validates values and parses durations", "creates codec and builds LettuceNearCacheConfig"] }),
  card({ id: "Codec", x: 1860, y: 230, w: 520, h: 125, color: "green", title: "LettuceBinaryCodec", lines: ["jdk/kryo/fory and compressed choices", "created once and reused by the factory"] }),

  band({ id: "HibernateLayer", x: left, y: 445, w: bandWidth, h: 250, color: "blue", label: "Hibernate cache SPI layer" }),
  card({ id: "RegionFactory", x: 210, y: 500, w: 620, h: 150, color: "blue", title: "LettuceNearCacheRegionFactory", lines: ["extends RegionFactoryTemplate", "creates domain/query/timestamp StorageAccess", "default access type is NONSTRICT_READ_WRITE"] }),
  card({ id: "FactoryState", x: 1040, y: 500, w: 620, h: 150, color: "blue", title: "factory runtime state", lines: ["single RedisClient per factory", "single LettuceBinaryCodec per factory", "ConcurrentHashMap regionName -> LettuceNearCache"] }),
  card({ id: "Lifecycle", x: 1870, y: 500, w: 520, h: 150, color: "blue", title: "lifecycle", lines: ["prepareForUse initializes client", "releaseFromUse closes caches first", "ShutdownQueue also shuts RedisClient down"] }),

  band({ id: "BridgeLayer", x: left, y: 750, w: bandWidth, h: 240, color: "teal", label: "Hibernate to NearCache bridge layer" }),
  card({ id: "StorageAccess", x: 300, y: 805, w: 700, h: 140, color: "teal", title: "LettuceNearCacheStorageAccess", lines: ["wraps DomainDataStorageAccess", "normalizes Hibernate cache keys", "delegates get, put, contains, evict to nearCache"] }),
  card({ id: "FailurePolicy", x: 1320, y: 805, w: 700, h: 140, color: "teal", title: "failure policy", lines: ["get failure logs and returns null", "put and evict failures are logged and ignored", "release is no-op; factory owns shared caches"] }),

  band({ id: "NearCacheLayer", x: left, y: 1045, w: bandWidth, h: 235, color: "violet", label: "bluetape4k NearCache core layer" }),
  card({ id: "NearCacheConfig", x: 270, y: 1090, w: 600, h: 160, color: "violet", title: "LettuceNearCacheConfig", lines: ["cacheName is regionName", "maxLocalSize and front expiry", "redisTtl and RESP3 tracking flag"] }),
  card({ id: "NearCache", x: 1040, y: 1090, w: 600, h: 160, color: "violet", title: "LettuceNearCache", lines: ["Caffeine front cache", "Redis backing cache", "read-through, write-through, remove, clearAll"] }),
  card({ id: "RegionIsolation", x: 1810, y: 1090, w: 520, h: 160, color: "violet", title: "region isolation", lines: ["one NearCache per region", "regionName prefixes Redis keys", "timestamp region has no Redis TTL"] }),

  band({ id: "InfraLayer", x: left, y: 1335, w: bandWidth, h: 190, color: "slate", label: "infrastructure layer" }),
  card({ id: "Caffeine", x: 520, y: 1385, w: 620, h: 95, color: "amber", title: "Caffeine L1", lines: ["process-local speed and expiry"] }),
  card({ id: "LettuceRedis", x: 1510, y: 1385, w: 620, h: 95, color: "pink", title: "Lettuce Redis L2", lines: ["RedisClient, RESP3, shared TTL-backed entries"] }),

  edge({ from: "HibernateProps", to: "Properties", points: [[870, 292], [1060, 292]], color: "green", label: "parsed by", labelAt: [920, 271] }),
  edge({ from: "Properties", to: "Codec", points: [[1670, 292], [1860, 292]], color: "green", label: "createCodec()", labelAt: [1720, 271] }),
  edge({ from: "Properties", to: "RegionFactory", points: [[1365, 355], [1365, 420], [520, 420], [520, 500]], color: "green", label: "configures", labelAt: [900, 440] }),
  edge({ from: "RegionFactory", to: "FactoryState", points: [[830, 575], [1040, 575]], color: "blue", label: "owns", labelAt: [915, 554] }),
  edge({ from: "FactoryState", to: "Lifecycle", points: [[1660, 575], [1870, 575]], color: "blue", label: "shutdown order", labelAt: [1710, 554] }),
  edge({ from: "RegionFactory", to: "StorageAccess", points: [[520, 650], [520, 805]], color: "teal", label: "creates", labelAt: [538, 735] }),
  edge({ from: "StorageAccess", to: "FailurePolicy", points: [[1000, 875], [1320, 875]], color: "teal", label: "guards Redis failures", labelAt: [1060, 854] }),
  edge({ from: "StorageAccess", to: "NearCache", points: [[650, 945], [650, 1016], [1340, 1016], [1340, 1090]], color: "violet", label: "delegates to", labelAt: [930, 997] }),
  edge({ from: "Properties", to: "NearCacheConfig", points: [[1365, 355], [1365, 405], [65, 405], [65, 1020], [570, 1020], [570, 1090]], color: "green", dashed: true, label: "builds region config", labelAt: [175, 1005] }),
  edge({ from: "NearCacheConfig", to: "NearCache", points: [[870, 1170], [1040, 1170]], color: "violet", label: "initializes", labelAt: [910, 1149] }),
  edge({ from: "NearCache", to: "RegionIsolation", points: [[1640, 1170], [1810, 1170]], color: "violet", label: "scoped by", labelAt: [1680, 1149] }),
  edge({ from: "NearCache", to: "Caffeine", points: [[1240, 1250], [1240, 1308], [830, 1308], [830, 1385]], color: "amber", label: "front tier", labelAt: [955, 1289] }),
  edge({ from: "NearCache", to: "LettuceRedis", points: [[1340, 1250], [1340, 1308], [1820, 1308], [1820, 1385]], color: "pink", label: "back tier", labelAt: [1510, 1289] }),
];

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Hibernate Lettuce Cache Layer Structure Diagram" data-intent="Hibernate settings are parsed into region-specific Lettuce NearCache configuration, then the RegionFactory creates StorageAccess bridges that delegate between Caffeine L1 and Redis L2." data-evidence="cache/hibernate-cache-lettuce/README.md,cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt" data-source-read="cache/hibernate-cache-lettuce/README.md; cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:47px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .band{stroke-width:1.4;opacity:.38}.bandLabel{font-family:"Comic Mono";font-size:16px;font-weight:700;fill:#334155}
    .card{stroke-width:1.8;filter:url(#shadow)}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:14px;fill:#334155}.divider{stroke-width:1.1;opacity:.42}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">Hibernate Lettuce Cache Layer Structure</text>
<text class="subtitle" x="76" y="120">A vertical view of how Hibernate settings become region-scoped StorageAccess, NearCache configuration, and Caffeine plus Redis infrastructure.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "cache-hibernate-cache-lettuce-diagram-02.svg");
const pngPath = join(OUT, "cache-hibernate-cache-lettuce-diagram-02.png");
writeFileSync(svgPath, `${svg.replace(/[ \t]+$/gm, "")}\n`);
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated cache-hibernate-cache-lettuce-diagram-02.svg/png");
