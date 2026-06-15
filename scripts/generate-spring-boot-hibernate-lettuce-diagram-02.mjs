#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "spring-boot/hibernate-lettuce/README.md",
  "spring-boot/hibernate-lettuce/README.ko.md",
  "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt",
  "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt",
  "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt",
  "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt",
  "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorEndpoint.kt",
  "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsBinder.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Auto-Configuration Activation Flow[\s\S]*spring-boot-hibernate-lettuce-diagram-02\.png/, "README activation flow slot");
assertContains(sources[2], /@ConfigurationProperties\(prefix = "bluetape4k\.cache\.lettuce-near"\)/, "properties binding");
assertContains(sources[3], /@ConditionalOnClass\(LettuceNearCacheRegionFactory::class,\s*EntityManagerFactory::class\)/, "hibernate class condition");
assertContains(sources[3], /@ConditionalOnProperty[\s\S]*matchIfMissing = true/, "hibernate property condition");
assertContains(sources[3], /hibernateProperties\["hibernate\.cache\.region\.factory_class"\][\s\S]*hibernate\.cache\.use_second_level_cache/, "hibernate property injection");
assertContains(sources[4], /@ConditionalOnClass\(Endpoint::class,\s*LettuceNearCacheRegionFactory::class,\s*EntityManagerFactory::class\)[\s\S]*@ConditionalOnBean\(EntityManagerFactory::class\)/, "actuator conditions");
assertContains(sources[5], /@ConditionalOnClass\(LettuceNearCacheRegionFactory::class,\s*EntityManagerFactory::class,\s*MeterRegistry::class\)[\s\S]*@ConditionalOnBean\(EntityManagerFactory::class,\s*MeterRegistry::class\)/, "metrics conditions");
assertContains(sources[6], /GET \/actuator\/nearcache[\s\S]*getAllRegionStats/, "actuator operation");
assertContains(sources[7], /lettuce\.nearcache\.active\.regions[\s\S]*lettuce\.nearcache\.total\.local\.size/, "metrics names");

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
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 19 11 L 2 20 Z" fill="${dark}"/></marker>`).join("\n");
}

function node({ id, x, y, w, h, color, title, lines = [], kind = "round" }) {
  const [fill, stroke] = palette[color];
  const rx = kind === "decision" ? 0 : 8;
  const titleY = kind === "decision" ? y + 68 : y + 42;
  const firstLineY = kind === "decision" ? y + 106 : y + 78;
  const shape = kind === "decision"
    ? `<path class="nodeBox" d="M${x + w / 2} ${y} L${x + w} ${y + h / 2} L${x + w / 2} ${y + h} L${x} ${y + h / 2} Z" fill="${fill}" stroke="${stroke}"/>`
    : `<rect class="nodeBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" fill="${fill}" stroke="${stroke}"/>`;
  return `<g id="${esc(id)}">
  ${shape}
  <text class="nodeTitle" x="${x + w / 2}" y="${titleY}" text-anchor="middle">${esc(title)}</text>
  ${lines.map((line, index) => `<text class="nodeLine" x="${x + w / 2}" y="${firstLineY + index * 26}" text-anchor="middle">${esc(line)}</text>`).join("\n")}
</g>`;
}

function edge({ from, to, points, color, dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0] + 8}" y="${p[1] - 8}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2500;
const height = 1540;
const body = [
  node({
    id: "Config",
    x: 110,
    y: 245,
    w: 520,
    h: 185,
    color: "teal",
    title: "application.yml",
    lines: ["bluetape4k.cache.lettuce-near.*", "enabled defaults to true", "local, Redis TTL, metrics options"],
  }),
  node({
    id: "ClassCondition",
    x: 760,
    y: 235,
    w: 470,
    h: 205,
    color: "amber",
    kind: "decision",
    title: "Classpath ready?",
    lines: ["RegionFactory", "EntityManagerFactory", "Spring Boot 4 Hibernate"],
  }),
  node({
    id: "HibernateAuto",
    x: 1380,
    y: 230,
    w: 610,
    h: 215,
    color: "green",
    title: "Hibernate auto-configuration",
    lines: ["binds LettuceNearCacheSpringProperties", "creates HibernatePropertiesCustomizer", "runs when enabled=true or missing"],
  }),
  node({
    id: "HibernateProps",
    x: 1380,
    y: 560,
    w: 610,
    h: 245,
    color: "green",
    title: "Hibernate properties injected",
    lines: ["region.factory_class = LettuceNearCacheRegionFactory", "use_second_level_cache = true", "redis_uri, codec, use_resp3", "local.max_size, expire_after_write, redis_ttl.*"],
  }),
  node({
    id: "RegionFactory",
    x: 760,
    y: 925,
    w: 600,
    h: 230,
    color: "slate",
    title: "Hibernate uses RegionFactory",
    lines: ["prepareForUse() creates Redis client and codec", "region storage uses LettuceNearCache", "L1 Caffeine + L2 Redis per region"],
  }),
  node({
    id: "MetricsCondition",
    x: 120,
    y: 790,
    w: 520,
    h: 185,
    color: "blue",
    title: "Metrics branch",
    lines: ["MeterRegistry bean present", "metrics.enabled=true or missing", "after Hibernate and Micrometer auto-config"],
  }),
  node({
    id: "MetricsBinder",
    x: 120,
    y: 1075,
    w: 520,
    h: 205,
    color: "blue",
    title: "Metrics binder",
    lines: ["unwraps SessionFactoryImplementor", "requires LettuceNearCacheRegionFactory", "registers active.regions and total.local.size"],
  }),
  node({
    id: "ActuatorCondition",
    x: 1840,
    y: 870,
    w: 530,
    h: 195,
    color: "pink",
    title: "Actuator branch",
    lines: ["Endpoint class on classpath", "EntityManagerFactory bean present", "enabled=true or missing"],
  }),
  node({
    id: "Endpoint",
    x: 1840,
    y: 1155,
    w: 530,
    h: 205,
    color: "pink",
    title: "NearCache endpoint",
    lines: ["GET /actuator/nearcache", "GET /actuator/nearcache/{region}", "reads getCaches(), local stats, Hibernate L2 stats"],
  }),
  edge({ from: "Config", to: "ClassCondition", points: [[630, 338], [760, 338]], color: "teal", label: "properties bind", labelAt: [650, 325] }),
  edge({ from: "ClassCondition", to: "HibernateAuto", points: [[1230, 338], [1380, 338]], color: "amber", label: "yes", labelAt: [1270, 325] }),
  edge({ from: "HibernateAuto", to: "HibernateProps", points: [[1685, 445], [1685, 560]], color: "green", label: "customizer", labelAt: [1703, 520] }),
  edge({ from: "HibernateProps", to: "RegionFactory", points: [[1685, 805], [1685, 900], [1060, 900], [1060, 925]], color: "green", label: "Hibernate boot", labelAt: [1260, 887] }),
  edge({ from: "Config", to: "MetricsCondition", points: [[370, 430], [370, 790]], color: "blue", dashed: true, label: "metrics settings", labelAt: [388, 650] }),
  edge({ from: "MetricsCondition", to: "MetricsBinder", points: [[380, 975], [380, 1075]], color: "blue", label: "creates", labelAt: [398, 1040] }),
  edge({ from: "MetricsBinder", to: "RegionFactory", points: [[640, 1180], [760, 1035]], color: "blue", dashed: true, label: "getCaches()", labelAt: [660, 1115] }),
  edge({ from: "HibernateProps", to: "ActuatorCondition", points: [[1990, 690], [2105, 690], [2105, 870]], color: "pink", dashed: true, label: "after Hibernate", labelAt: [2070, 775] }),
  edge({ from: "ActuatorCondition", to: "Endpoint", points: [[2105, 1065], [2105, 1155]], color: "pink", label: "creates", labelAt: [2123, 1125] }),
  edge({ from: "Endpoint", to: "RegionFactory", points: [[1840, 1260], [1360, 1260], [1360, 1040]], color: "pink", dashed: true, label: "getCaches() + stats", labelAt: [1500, 1247] }),
];

const svg = `<svg data-intent="Explain Spring Boot Hibernate Lettuce auto-configuration activation flow for README diagram 02." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Hibernate Lettuce Auto-Configuration Activation Flow">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .nodeBox{stroke-width:1.8;filter:url(#shadow)}.nodeTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}.nodeLine{font-family:"Comic Mono";font-size:14px;fill:#334155}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">Hibernate Lettuce Auto-Configuration Flow</text>
<text class="subtitle" x="76" y="120">How Spring Boot 4 conditions activate Hibernate 2nd Level Cache, then optionally expose Actuator and Micrometer integrations.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "spring-boot-hibernate-lettuce-diagram-02.svg");
const pngPath = join(OUT, "spring-boot-hibernate-lettuce-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated spring-boot-hibernate-lettuce-diagram-02.svg/png");
