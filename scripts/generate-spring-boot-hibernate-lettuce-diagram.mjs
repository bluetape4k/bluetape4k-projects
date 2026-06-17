#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";

const id = process.argv[2];
if (!["01", "02"].includes(id)) {
  console.error("Usage: node scripts/generate-spring-boot-hibernate-lettuce-diagram.mjs 01|02");
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
  const { size = 13.5, fill = "#4b5563", line = 18, anchor = "start", weight = 400 } = opts;
  return items
    .map((item, idx) => `<text x="${x}" y="${y + idx * line}" text-anchor="${anchor}" font-size="${size}" font-weight="${weight}" fill="${fill}">${esc(item)}</text>`)
    .join("\n");
}

function classCard({ x, y, w, h, name, stereotype, attrs = [], ops = [], fill, stroke }) {
  const nameLines = String(name).split("\n");
  const headerH = 32 + nameLines.length * 21;
  const attrH = attrs.length ? Math.max(34, attrs.length * 18 + 18) : 0;
  const opY = y + headerH + attrH;
  const opH = h - headerH - attrH;
  return `
  <g class="card" data-card="${esc(nameLines.join(" "))}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    <text x="${x + w / 2}" y="${y + 21}" text-anchor="middle" font-size="12.5" fill="${stroke}">${esc(stereotype)}</text>
    ${lines(nameLines, x + w / 2, y + 42, { size: 17, weight: 700, fill: "#111827", line: 20, anchor: "middle" })}
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

function line({ d, color, marker, width = 2.8, dash = "" }) {
  return `<path class="edge" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})"/>`;
}

function flowCard({ x, y, w, h, title, body = [], fill, stroke, accent }) {
  return `
  <g class="card" data-card="${esc(title)}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    <rect x="${x}" y="${y}" width="10" height="${h}" rx="5" fill="${accent || stroke}" opacity="0.9"/>
    <text x="${x + 26}" y="${y + 34}" font-size="18" font-weight="700" fill="#111827">${esc(title)}</text>
    ${lines(body, x + 26, y + 62, { size: 13.3, fill: "#475569", line: 19 })}
  </g>`;
}

function diagram01() {
  const width = 1500;
  const height = 1080;
  const cards = [
    classCard({
      x: 82,
      y: 208,
      w: 330,
      h: 174,
      name: "LettuceNearCache\nSpringProperties",
      stereotype: "<<ConfigurationProperties>>",
      attrs: ["enabled, redisUri, codec, useResp3", "local, redisTtl, metrics"],
      ops: ["prefix: bluetape4k.cache.lettuce-near"],
      fill: "#eff6ff",
      stroke: "#2563eb",
    }),
    classCard({
      x: 82,
      y: 432,
      w: 330,
      h: 164,
      name: "LettuceNearCacheHibernate\nAutoConfiguration",
      stereotype: "<<AutoConfiguration>>",
      attrs: ["conditions: RegionFactory + EntityManagerFactory"],
      ops: ["+ hibernatePropertiesCustomizer(props)"],
      fill: "#f0fdfa",
      stroke: "#0f766e",
    }),
    classCard({
      x: 82,
      y: 636,
      w: 330,
      h: 164,
      name: "LettuceNearCacheMetrics\nAutoConfiguration",
      stereotype: "<<AutoConfiguration>>",
      attrs: ["conditions: EntityManagerFactory + MeterRegistry"],
      ops: ["+ metricsBinder(emf, registry)"],
      fill: "#f5f3ff",
      stroke: "#7c3aed",
    }),
    classCard({
      x: 82,
      y: 828,
      w: 330,
      h: 148,
      name: "LettuceNearCacheActuator\nAutoConfiguration",
      stereotype: "<<AutoConfiguration>>",
      attrs: ["conditions: Endpoint + EntityManagerFactory"],
      ops: ["+ actuatorEndpoint(emf)"],
      fill: "#fff7ed",
      stroke: "#ea580c",
    }),
    classCard({
      x: 530,
      y: 386,
      w: 330,
      h: 168,
      name: "HibernatePropertiesCustomizer",
      stereotype: "<<Spring Boot contract>>",
      attrs: ["sets region.factory_class", "enables second-level cache"],
      ops: ["maps Redis, local cache, TTL, metrics flags"],
      fill: "#ecfdf5",
      stroke: "#16a34a",
    }),
    classCard({
      x: 530,
      y: 600,
      w: 330,
      h: 174,
      name: "LettuceNearCache\nMetricsBinder",
      stereotype: "<<SmartInitializingSingleton>>",
      attrs: ["EntityManagerFactory + MeterRegistry"],
      ops: ["+ afterSingletonsInstantiated()", "+ registers aggregate gauges"],
      fill: "#f8fafc",
      stroke: "#475569",
    }),
    classCard({
      x: 530,
      y: 806,
      w: 330,
      h: 184,
      name: "LettuceNearCache\nActuatorEndpoint",
      stereotype: "<<Endpoint id=nearcache>>",
      attrs: ["EntityManagerFactory"],
      ops: ["+ getAllRegionStats()", "+ getRegionStats(regionName)"],
      fill: "#fff7ed",
      stroke: "#ea580c",
    }),
    classCard({
      x: 1048,
      y: 214,
      w: 330,
      h: 154,
      name: "LettuceNearCache\nRegionFactory",
      stereotype: "<<Hibernate RegionFactory>>",
      attrs: ["owns region cache map", "L1 Caffeine + L2 Redis"],
      ops: ["+ getCaches()"],
      fill: "#ecfdf5",
      stroke: "#16a34a",
    }),
    classCard({
      x: 1048,
      y: 500,
      w: 330,
      h: 150,
      name: "Hibernate Runtime Contracts",
      stereotype: "<<external contracts>>",
      attrs: ["EntityManagerFactory", "SessionFactoryImplementor"],
      ops: ["unwrap session factory", "read RegionFactory and Statistics"],
      fill: "#eff6ff",
      stroke: "#2563eb",
    }),
    classCard({
      x: 1048,
      y: 778,
      w: 330,
      h: 172,
      name: "Observability Outputs",
      stereotype: "<<Actuator + Micrometer>>",
      attrs: ["RegionStats endpoint model", "Gauge metrics"],
      ops: ["nearcache reads", "lettuce.nearcache.* gauges"],
      fill: "#f5f3ff",
      stroke: "#7c3aed",
    }),
  ];

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Hibernate Lettuce class structure">
  <style>
    text { font-family: ${font}; dominant-baseline: alphabetic; }
  </style>
  <defs>
    <marker id="arrow-blue" markerUnits="userSpaceOnUse" markerWidth="12" markerHeight="12" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#2563eb" stroke="#2563eb" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-green" markerUnits="userSpaceOnUse" markerWidth="12" markerHeight="12" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#16a34a" stroke="#16a34a" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-purple" markerUnits="userSpaceOnUse" markerWidth="12" markerHeight="12" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#7c3aed" stroke="#7c3aed" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-orange" markerUnits="userSpaceOnUse" markerWidth="12" markerHeight="12" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#ea580c" stroke="#ea580c" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="open-gray" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#64748b" stroke-width="1.7" stroke-dasharray="none" stroke-linecap="round" stroke-linejoin="round"/>
    </marker>
  </defs>
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="74" y="62" font-size="30" font-weight="700" fill="#111827">Spring Boot Hibernate Lettuce Class Structure</text>
  <text x="74" y="92" font-size="15" fill="#64748b">Auto-configuration classes create the Hibernate customizer, metrics binder, and Actuator endpoint around the Lettuce Near Cache region factory.</text>

  ${group({ x: 50, y: 132, w: 396, h: 870, title: "AUTO-CONFIGURATION ENTRYPOINTS", note: "Conditions decide which beans are created.", fill: "#eff6ff", stroke: "#2563eb" })}
  ${group({ x: 492, y: 132, w: 410, h: 870, title: "CREATED BEANS AND ADAPTERS", note: "Bean methods adapt Boot to cache contracts.", fill: "#f8fafc", stroke: "#64748b" })}
  ${group({ x: 1010, y: 132, w: 410, h: 870, title: "RUNTIME CONTRACTS", note: "Hibernate and observers consume the adapters.", fill: "#ecfdf5", stroke: "#16a34a" })}

  ${line({ d: "M412 514 L530 514", color: "#16a34a", marker: "arrow-green" })}
  ${line({ d: "M412 687 L530 687", color: "#7c3aed", marker: "arrow-purple" })}
  ${line({ d: "M412 884 L530 884", color: "#ea580c", marker: "arrow-orange" })}
  ${line({ d: "M247 382 L247 400 L500 400 L500 430 L530 430", color: "#2563eb", marker: "arrow-blue", width: 2.5 })}
  ${line({ d: "M860 456 L968 456 L968 291 L1048 291", color: "#16a34a", marker: "arrow-green" })}
  ${line({ d: "M860 680 L990 680 L990 575 L1048 575", color: "#64748b", marker: "open-gray", width: 2.3, dash: "6 5" })}
  ${line({ d: "M860 884 L968 884 L968 847 L1048 847", color: "#ea580c", marker: "arrow-orange", width: 2.6 })}
  ${line({ d: "M860 690 L968 690 L968 847 L1048 847", color: "#7c3aed", marker: "arrow-purple", width: 2.5 })}

  ${cards.join("\n")}

  <g class="legend" transform="translate(74 1040)">
    <line x1="0" y1="0" x2="38" y2="0" stroke="#16a34a" stroke-width="2.8" marker-end="url(#arrow-green)"/>
    <text x="52" y="5" font-size="13" fill="#475569">creates or configures</text>
    <line x1="252" y1="0" x2="290" y2="0" stroke="#64748b" stroke-width="2.3" stroke-dasharray="6 5" marker-end="url(#open-gray)"/>
    <text x="304" y="5" font-size="13" fill="#475569">uses runtime contract</text>
    <line x1="526" y1="0" x2="564" y2="0" stroke="#7c3aed" stroke-width="2.5" marker-end="url(#arrow-purple)"/>
    <text x="578" y="5" font-size="13" fill="#475569">publishes metrics</text>
    <line x1="762" y1="0" x2="800" y2="0" stroke="#ea580c" stroke-width="2.6" marker-end="url(#arrow-orange)"/>
    <text x="814" y="5" font-size="13" fill="#475569">exposes endpoint</text>
  </g>
</svg>`;
}

function diagram02() {
  const width = 1500;
  const height = 1120;
  const cards = [
    flowCard({
      x: 84,
      y: 220,
      w: 354,
      h: 122,
      title: "App Context Starts",
      body: ["Dependency is on the classpath", "application.yml binds lettuce-near.*", "enabled defaults to true"],
      fill: "#eff6ff",
      stroke: "#2563eb",
    }),
    flowCard({
      x: 564,
      y: 220,
      w: 370,
      h: 122,
      title: "Condition Match",
      body: ["RegionFactory + EntityManagerFactory", "property bluetape4k.cache.lettuce-near.enabled", "backs off when disabled"],
      fill: "#f8fafc",
      stroke: "#64748b",
    }),
    flowCard({
      x: 1058,
      y: 220,
      w: 344,
      h: 122,
      title: "Disabled Branch",
      body: ["No HibernatePropertiesCustomizer", "No metrics binder", "No Actuator endpoint"],
      fill: "#fff1f2",
      stroke: "#e11d48",
      accent: "#e11d48",
    }),
    flowCard({
      x: 564,
      y: 408,
      w: 370,
      h: 124,
      title: "Customizer Bean",
      body: ["Spring Boot receives HibernatePropertiesCustomizer", "auto-configuration maps properties", "no user bean code is required"],
      fill: "#ecfdf5",
      stroke: "#16a34a",
    }),
    flowCard({
      x: 564,
      y: 604,
      w: 370,
      h: 140,
      title: "Hibernate Settings",
      body: ["region.factory_class points to RegionFactory", "second-level cache is enabled", "Redis URI, codec, L1 and TTL values are copied"],
      fill: "#ecfdf5",
      stroke: "#16a34a",
    }),
    flowCard({
      x: 564,
      y: 826,
      w: 370,
      h: 126,
      title: "RegionFactory Runtime",
      body: ["Hibernate builds the near-cache RegionFactory", "L1 Caffeine and L2 Redis hold region data", "Statistics become available to observers"],
      fill: "#f0fdfa",
      stroke: "#0f766e",
      accent: "#0f766e",
    }),
    flowCard({
      x: 1058,
      y: 430,
      w: 344,
      h: 126,
      title: "Metrics Binder",
      body: ["Requires MeterRegistry", "unwraps EntityManagerFactory", "registers aggregate gauges after singleton startup"],
      fill: "#f5f3ff",
      stroke: "#7c3aed",
    }),
    flowCard({
      x: 1058,
      y: 612,
      w: 344,
      h: 126,
      title: "Micrometer Gauges",
      body: ["lettuce.nearcache.active.regions", "lettuce.nearcache.total.local.size", "observed through /actuator/metrics"],
      fill: "#f5f3ff",
      stroke: "#7c3aed",
    }),
    flowCard({
      x: 1058,
      y: 826,
      w: 344,
      h: 126,
      title: "Actuator Endpoint",
      body: ["Requires Endpoint and EntityManagerFactory", "unwraps RegionFactory and Statistics", "serves /actuator/nearcache"],
      fill: "#fff7ed",
      stroke: "#ea580c",
    }),
  ];

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Hibernate Lettuce activation flow">
  <style>
    text { font-family: ${font}; dominant-baseline: alphabetic; }
  </style>
  <defs>
    <marker id="arrow-blue" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#2563eb" stroke="#2563eb" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-green" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#16a34a" stroke="#16a34a" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-purple" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#7c3aed" stroke="#7c3aed" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-orange" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#ea580c" stroke="#ea580c" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-rose" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#e11d48" stroke="#e11d48" stroke-width="0" stroke-dasharray="none"/>
    </marker>
  </defs>
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="74" y="62" font-size="30" font-weight="700" fill="#111827">Spring Boot Hibernate Lettuce Activation Flow</text>
  <text x="74" y="92" font-size="15" fill="#64748b">Property binding creates the Hibernate cache customizer first; metrics and Actuator attach only when their optional runtime contracts are present.</text>

  ${group({ x: 52, y: 132, w: 422, h: 248, title: "INPUT", note: "Dependency and configuration start the path.", fill: "#eff6ff", stroke: "#2563eb" })}
  ${group({ x: 526, y: 132, w: 446, h: 876, title: "HIBERNATE CACHE ACTIVATION", note: "Settings are prepared before Hibernate builds the RegionFactory.", fill: "#ecfdf5", stroke: "#16a34a" })}
  ${group({ x: 1020, y: 132, w: 420, h: 876, title: "BACK-OFF AND OBSERVABILITY", note: "Disabled, metrics, and Actuator paths stay separate.", fill: "#f8fafc", stroke: "#64748b" })}

  ${line({ d: "M438 281 L564 281", color: "#2563eb", marker: "arrow-blue", width: 3.2 })}
  ${line({ d: "M934 281 L1058 281", color: "#e11d48", marker: "arrow-rose", width: 2.5, dash: "7 5" })}
  ${line({ d: "M749 342 L749 408", color: "#16a34a", marker: "arrow-green", width: 3.2 })}
  ${line({ d: "M749 532 L749 604", color: "#16a34a", marker: "arrow-green", width: 3.2 })}
  ${line({ d: "M749 744 L749 826", color: "#0f766e", marker: "arrow-green", width: 3.2 })}
  ${line({ d: "M934 888 L990 888 L990 493 L1058 493", color: "#7c3aed", marker: "arrow-purple", width: 3.0 })}
  ${line({ d: "M1230 556 L1230 612", color: "#7c3aed", marker: "arrow-purple", width: 3.0 })}
  ${line({ d: "M934 888 L1058 888", color: "#ea580c", marker: "arrow-orange", width: 3.0 })}

  ${cards.join("\n")}

  <g class="legend" transform="translate(78 1058)">
    <line x1="0" y1="0" x2="42" y2="0" stroke="#16a34a" stroke-width="3.2" marker-end="url(#arrow-green)"/>
    <text x="58" y="5" font-size="13" fill="#475569">required activation path</text>
    <line x1="300" y1="0" x2="342" y2="0" stroke="#7c3aed" stroke-width="3" marker-end="url(#arrow-purple)"/>
    <text x="358" y="5" font-size="13" fill="#475569">metrics branch</text>
    <line x1="548" y1="0" x2="590" y2="0" stroke="#ea580c" stroke-width="3" marker-end="url(#arrow-orange)"/>
    <text x="606" y="5" font-size="13" fill="#475569">actuator branch</text>
    <line x1="798" y1="0" x2="840" y2="0" stroke="#e11d48" stroke-width="2.5" stroke-dasharray="7 5" marker-end="url(#arrow-rose)"/>
    <text x="856" y="5" font-size="13" fill="#475569">disabled branch</text>
  </g>
</svg>`;
}

const svg = id === "01" ? diagram01() : diagram02();
const svgPath = resolve(outDir, `spring-boot-hibernate-lettuce-diagram-${id}.svg`);
const pngPath = resolve(outDir, `spring-boot-hibernate-lettuce-diagram-${id}.png`);
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync("/Users/debop/.local/bin/cairosvg", [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`generated ${svgPath}`);
console.log(`generated ${pngPath}`);
