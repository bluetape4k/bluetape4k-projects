#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";

const id = process.argv[2];
if (!["01", "02"].includes(id)) {
  console.error("Usage: node scripts/generate-spring-boot-hibernate-lettuce-demo-diagram.mjs 01|02");
  process.exit(1);
}

const root = resolve(dirname(new URL(import.meta.url).pathname), "..");
const outDir = resolve(root, "docs/images/readme-diagrams");
mkdirSync(outDir, { recursive: true });

const font = "'Architects Daughter', 'Comic Mono', 'Helvetica Neue', Arial, sans-serif";

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function lines(items, x, y, opts = {}) {
  const { size = 13, fill = "#475569", line = 18, anchor = "start", weight = 400 } = opts;
  return items
    .map((item, idx) => `<text x="${x}" y="${y + idx * line}" text-anchor="${anchor}" font-size="${size}" font-weight="${weight}" fill="${fill}">${esc(item)}</text>`)
    .join("\n");
}

function classCard({ x, y, w, h, name, stereotype, attrs = [], ops = [], fill, stroke }) {
  const nameLines = String(name).split("\n");
  const headerH = 34 + nameLines.length * 21;
  const attrH = attrs.length ? Math.max(34, attrs.length * 18 + 18) : 0;
  const opY = y + headerH + attrH;
  return `
  <g class="card" data-card="${esc(nameLines.join(" "))}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    <text x="${x + w / 2}" y="${y + 21}" text-anchor="middle" font-size="12.5" fill="${stroke}">${esc(stereotype)}</text>
    ${lines(nameLines, x + w / 2, y + 44, { size: 17, weight: 700, fill: "#111827", line: 20, anchor: "middle" })}
    <line x1="${x}" y1="${y + headerH}" x2="${x + w}" y2="${y + headerH}" stroke="${stroke}" stroke-width="1.1" opacity="0.55"/>
    ${attrH ? `<line x1="${x}" y1="${opY}" x2="${x + w}" y2="${opY}" stroke="${stroke}" stroke-width="1.1" opacity="0.45"/>` : ""}
    ${attrs.length ? lines(attrs, x + 16, y + headerH + 23, { size: 12.8, fill: "#475569", line: 18 }) : ""}
    ${ops.length ? lines(ops, x + 16, opY + 23, { size: 12.8, fill: "#374151", line: 18 }) : ""}
  </g>`;
}

function group({ x, y, w, h, title, note, fill, stroke }) {
  return `
  <g class="group" data-group="${esc(title)}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" stroke-width="1.3" stroke-dasharray="7 5"/>
    <text x="${x + 18}" y="${y + 26}" font-size="14" font-weight="700" fill="${stroke}">${esc(title)}</text>
    <text x="${x + 18}" y="${y + 47}" font-size="12.5" fill="#6b7280">${esc(note)}</text>
  </g>`;
}

function line({ d, color, marker, width = 2.6, dash = "" }) {
  return `<path class="edge" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})"/>`;
}

function flowCard({ x, y, w, h, title, body, fill, stroke }) {
  return `
  <g class="card" data-card="${esc(title)}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    <rect x="${x}" y="${y}" width="10" height="${h}" rx="5" fill="${stroke}" opacity="0.9"/>
    <text x="${x + 26}" y="${y + 34}" font-size="18" font-weight="700" fill="#111827">${esc(title)}</text>
    ${lines(body, x + 26, y + 62, { size: 13.3, fill: "#475569", line: 19 })}
  </g>`;
}

function defs() {
  return `
  <defs>
    <marker id="arrow-blue" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#2563eb" stroke="#2563eb" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-green" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#16a34a" stroke="#16a34a" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-orange" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#ea580c" stroke="#ea580c" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-purple" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#7c3aed" stroke="#7c3aed" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="open-gray" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#64748b" stroke-width="1.7" stroke-dasharray="none" stroke-linecap="round" stroke-linejoin="round"/>
    </marker>
  </defs>`;
}

function diagram01() {
  const width = 1480;
  const height = 960;
  const cards = [
    classCard({ x: 72, y: 194, w: 320, h: 118, name: "DemoApplication", stereotype: "<<SpringBootApplication>>", attrs: ["proxyBeanMethods = false"], ops: ["runApplication<DemoApplication>()"], fill: "#eff6ff", stroke: "#2563eb" }),
    classCard({ x: 72, y: 398, w: 320, h: 166, name: "ProductController", stereotype: "<<RestController>>", attrs: ["/api/products", "ProductRepository"], ops: ["+ getProduct(id)", "+ create/update/delete product"], fill: "#ecfdf5", stroke: "#16a34a" }),
    classCard({ x: 72, y: 646, w: 320, h: 206, name: "CacheController", stereotype: "<<RestController>>", attrs: ["/api/cache", "EntityManagerFactory"], ops: ["+ getCacheStats()", "+ evictRegion(region)", "+ evictAll()"], fill: "#fff7ed", stroke: "#ea580c" }),
    classCard({ x: 548, y: 358, w: 330, h: 152, name: "ProductRepository", stereotype: "<<JpaRepository>>", attrs: ["JpaRepository<Product, Long>"], ops: ["+ findByName(name)", "QueryHint cache.retrieveMode=USE"], fill: "#f0fdfa", stroke: "#0f766e" }),
    classCard({ x: 548, y: 590, w: 330, h: 178, name: "Product", stereotype: "<<Entity>>", attrs: ["@Cacheable", "@Cache(region = product)", "id, name, description, price"], ops: ["NONSTRICT_READ_WRITE 2LC region"], fill: "#f5f3ff", stroke: "#7c3aed" }),
    classCard({ x: 1038, y: 358, w: 330, h: 154, name: "EntityManagerFactory", stereotype: "<<JPA runtime>>", attrs: ["unwrapped to SessionFactoryImplementor"], ops: ["serviceRegistry.getService(RegionFactory)"], fill: "#eff6ff", stroke: "#2563eb" }),
    classCard({ x: 1038, y: 604, w: 330, h: 164, name: "LettuceNearCache\nRegionFactory", stereotype: "<<Hibernate RegionFactory>>", attrs: ["getCaches(): Map<String, cache>", "L1 Caffeine + L2 Redis"], ops: ["CacheController clears local cache only"], fill: "#ecfdf5", stroke: "#16a34a" }),
  ];
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Hibernate Lettuce demo class structure">
  <style>text { font-family: ${font}; dominant-baseline: alphabetic; }</style>
  ${defs()}
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="72" y="62" font-size="30" font-weight="700" fill="#111827">Hibernate Lettuce Demo Class Structure</text>
  <text x="72" y="92" font-size="15" fill="#64748b">The demo exposes product CRUD and L1 cache management around a cached Product entity and the Lettuce Near Cache RegionFactory.</text>
  ${group({ x: 42, y: 132, w: 390, h: 726, title: "HTTP ENTRYPOINTS", note: "Controllers demonstrate CRUD and cache management APIs.", fill: "#eff6ff", stroke: "#2563eb" })}
  ${group({ x: 510, y: 132, w: 410, h: 726, title: "DOMAIN AND REPOSITORY", note: "Spring Data JPA persists the cached Product entity.", fill: "#ecfdf5", stroke: "#16a34a" })}
  ${group({ x: 1000, y: 132, w: 410, h: 726, title: "HIBERNATE CACHE RUNTIME", note: "Runtime services expose the near-cache region map.", fill: "#f8fafc", stroke: "#64748b" })}
  ${line({ d: "M392 472 L548 472", color: "#16a34a", marker: "arrow-green" })}
  ${line({ d: "M713 510 L713 590", color: "#7c3aed", marker: "arrow-purple", width: 2.4, dash: "6 5" })}
  ${line({ d: "M392 728 L478 728 L478 828 L992 828 L992 435 L1038 435", color: "#ea580c", marker: "arrow-orange" })}
  ${line({ d: "M1203 512 L1203 604", color: "#2563eb", marker: "arrow-blue", width: 2.4, dash: "6 5" })}
  ${cards.join("\n")}
  <g class="legend" transform="translate(72 914)">
    <line x1="0" y1="0" x2="40" y2="0" stroke="#16a34a" stroke-width="2.6" marker-end="url(#arrow-green)"/><text x="56" y="5" font-size="13" fill="#475569">controller calls repository</text>
    <line x1="300" y1="0" x2="340" y2="0" stroke="#7c3aed" stroke-width="2.4" stroke-dasharray="6 5" marker-end="url(#arrow-purple)"/><text x="356" y="5" font-size="13" fill="#475569">repository manages entity</text>
    <line x1="610" y1="0" x2="650" y2="0" stroke="#ea580c" stroke-width="2.6" marker-end="url(#arrow-orange)"/><text x="666" y="5" font-size="13" fill="#475569">cache API unwraps EMF</text>
    <line x1="884" y1="0" x2="924" y2="0" stroke="#2563eb" stroke-width="2.4" stroke-dasharray="6 5" marker-end="url(#arrow-blue)"/><text x="940" y="5" font-size="13" fill="#475569">EMF exposes RegionFactory</text>
  </g>
</svg>`;
}

function diagram02() {
  const width = 1500;
  const height = 980;
  const cards = [
    flowCard({ x: 70, y: 206, w: 330, h: 118, title: "Product Request", body: ["GET /api/products/{id}", "POST, PUT, DELETE mutate data", "list endpoint bypasses 2LC focus"], fill: "#eff6ff", stroke: "#2563eb" }),
    flowCard({ x: 510, y: 206, w: 338, h: 118, title: "Spring Data JPA", body: ["ProductRepository delegates to Hibernate", "Product has @Cacheable and product region", "findByName uses retrieveMode=USE"], fill: "#ecfdf5", stroke: "#16a34a" }),
    flowCard({ x: 960, y: 206, w: 370, h: 118, title: "Hibernate 2LC", body: ["RegionFactory owns the cache regions", "product region TTL is 300s", "default Redis TTL is 120s"], fill: "#f0fdfa", stroke: "#0f766e" }),
    flowCard({ x: 960, y: 420, w: 370, h: 122, title: "L1 Local Cache", body: ["Caffeine near-cache", "max-size: 5000", "expire-after-write: 15m"], fill: "#f5f3ff", stroke: "#7c3aed" }),
    flowCard({ x: 960, y: 646, w: 370, h: 122, title: "L2 Redis Cache", body: ["Redis URI: redis://localhost:6379", "codec: lz4fory", "RESP3 tracking enabled"], fill: "#fff7ed", stroke: "#ea580c" }),
    flowCard({ x: 510, y: 646, w: 338, h: 122, title: "H2 Database", body: ["jdbc:h2:mem:demo", "ddl-auto: create-drop", "source of truth on cache miss"], fill: "#f8fafc", stroke: "#64748b" }),
    flowCard({ x: 70, y: 646, w: 330, h: 122, title: "Cache Management", body: ["GET /api/cache/stats reads localStats()", "DELETE /api/cache/evict clears L1 only", "Redis L2 is intentionally untouched"], fill: "#fff7ed", stroke: "#ea580c" }),
    flowCard({ x: 70, y: 420, w: 330, h: 122, title: "Actuator Metrics", body: ["/actuator/nearcache", "/actuator/metrics", "exposes health, info, metrics, nearcache"], fill: "#f5f3ff", stroke: "#7c3aed" }),
  ];
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Hibernate Lettuce demo runtime flow">
  <style>text { font-family: ${font}; dominant-baseline: alphabetic; }</style>
  ${defs()}
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="72" y="62" font-size="30" font-weight="700" fill="#111827">Hibernate Lettuce Demo Runtime Flow</text>
  <text x="72" y="92" font-size="15" fill="#64748b">Product CRUD uses Hibernate 2LC; cache management and Actuator observe or clear only the local near-cache where intended.</text>
  ${group({ x: 42, y: 132, w: 390, h: 684, title: "HTTP AND OPERATIONS", note: "User-facing endpoints drive product and cache scenarios.", fill: "#eff6ff", stroke: "#2563eb" })}
  ${group({ x: 472, y: 132, w: 416, h: 684, title: "JPA AND DATABASE", note: "Repository calls reach the database on cache miss.", fill: "#ecfdf5", stroke: "#16a34a" })}
  ${group({ x: 922, y: 132, w: 448, h: 684, title: "NEAR CACHE TIERS", note: "L1 Caffeine and L2 Redis are configured by application.yml.", fill: "#f8fafc", stroke: "#64748b" })}
  ${line({ d: "M400 265 L510 265", color: "#2563eb", marker: "arrow-blue", width: 3 })}
  ${line({ d: "M848 265 L960 265", color: "#16a34a", marker: "arrow-green", width: 3 })}
  ${line({ d: "M1145 324 L1145 420", color: "#7c3aed", marker: "arrow-purple", width: 3 })}
  ${line({ d: "M1145 542 L1145 646", color: "#ea580c", marker: "arrow-orange", width: 3 })}
  ${line({ d: "M960 720 L848 720", color: "#64748b", marker: "open-gray", width: 2.4, dash: "6 5" })}
  ${line({ d: "M400 707 L450 707 L450 590 L900 590 L900 520 L960 520", color: "#ea580c", marker: "arrow-orange", width: 2.8 })}
  ${line({ d: "M400 481 L960 481", color: "#7c3aed", marker: "arrow-purple", width: 2.8 })}
  ${cards.join("\n")}
  <g class="legend" transform="translate(72 894)">
    <line x1="0" y1="0" x2="42" y2="0" stroke="#16a34a" stroke-width="3" marker-end="url(#arrow-green)"/><text x="58" y="5" font-size="13" fill="#475569">cacheable data path</text>
    <line x1="260" y1="0" x2="302" y2="0" stroke="#ea580c" stroke-width="2.8" marker-end="url(#arrow-orange)"/><text x="318" y="5" font-size="13" fill="#475569">cache management path</text>
    <line x1="570" y1="0" x2="612" y2="0" stroke="#7c3aed" stroke-width="2.8" marker-end="url(#arrow-purple)"/><text x="628" y="5" font-size="13" fill="#475569">observability path</text>
    <line x1="830" y1="0" x2="872" y2="0" stroke="#64748b" stroke-width="2.4" stroke-dasharray="6 5" marker-end="url(#open-gray)"/><text x="888" y="5" font-size="13" fill="#475569">cache miss fallback</text>
  </g>
</svg>`;
}

const svg = id === "01" ? diagram01() : diagram02();
const svgPath = resolve(outDir, `spring-boot-hibernate-lettuce-demo-diagram-${id}.svg`);
const pngPath = resolve(outDir, `spring-boot-hibernate-lettuce-demo-diagram-${id}.png`);
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync("/Users/debop/.local/bin/cairosvg", [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`generated ${svgPath}`);
console.log(`generated ${pngPath}`);
