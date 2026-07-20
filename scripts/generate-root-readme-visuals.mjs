#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, readdirSync, statSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

const ROOT = process.cwd();
const diagramDir = join(ROOT, "docs/images/readme-diagrams");
const chartDir = join(ROOT, "docs/images/readme-charts");
const cairosvg = process.env.CAIROSVG ?? "cairosvg";

const groups = [
  ["bluetape4k", "Foundation", "Core contracts, coroutines, logging, BOM"],
  ["io", "I/O and Codecs", "HTTP, serialization, crypto, streaming"],
  ["data", "Data Access", "JDBC, R2DBC, Hibernate, MongoDB, Cassandra"],
  ["infra", "Infrastructure", "Redis, Kafka, telemetry, resilience"],
  ["cache", "Caching", "NearCache core plus backend bridges"],
  ["ktor", "Ktor Stack", "Server core, observability, testing"],
  ["spring-boot", "Spring Boot 4", "Auto-configurations and demos"],
  ["testing", "Testing", "Assertions, JUnit, Testcontainers"],
  ["utils", "Utilities", "states, workflow, money, JWT, ID, time"],
  ["virtualthread", "Virtual Threads", "JDK21 and JDK25 runtime helpers"],
  ["examples", "Examples", "Runnable demos and integration samples"],
];

const moduleCounts = Object.fromEntries(groups.map(([id]) => [id, countModules(id)]));
const totalModules = Object.values(moduleCounts).reduce((sum, value) => sum + value, 0);

ensureDir(diagramDir);
ensureDir(chartDir);

execFileSync(process.execPath, [join(ROOT, "scripts/generate-root-readme-overview-01.mjs")], { stdio: "inherit" });
writeVisual("root-readme-en-diagram-01", architectureSvg());
writeChart("root-readme-module-chart-01", moduleChartSvg());

console.log(`root-readme-visuals: modules=${totalModules} diagrams=2 charts=1 renderer=cairosvg`);

function countModules(group) {
  const base = join(ROOT, group);
  if (!existsSync(base)) return 0;
  let count = 0;
  const walk = (dir) => {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue;
      if (entry.name.startsWith(".") || entry.name === "build") continue;
      const child = join(dir, entry.name);
      if (existsSync(join(child, "build.gradle.kts"))) count += 1;
      walk(child);
    }
  };
  walk(base);
  return count;
}

function writeVisual(name, svg) {
  writeAsset(join(diagramDir, name), svg);
}

function writeChart(name, svg) {
  writeAsset(join(chartDir, name), svg);
}

function writeAsset(base, svg) {
  writeFileSync(`${base}.svg`, svg);
  execFileSync(cairosvg, [`${base}.svg`, "-o", `${base}.png`, "--scale", "2"], { stdio: "inherit" });
}

function architectureSvg() {
  const width = 1640;
  const height = 1100;
  const cards = [
    { id: "consumer", x: 120, y: 180, w: 320, h: 88, title: "Application Code", detail: "imports BOM", color: "blue" },
    { id: "bom", x: 660, y: 180, w: 320, h: 88, title: "bluetape4k-bom", detail: "dependency alignment", color: "green" },
    { id: "catalog", x: 1200, y: 180, w: 320, h: 88, title: "Version Catalog", detail: "central versions", color: "purple" },
    { id: "foundation", x: 140, y: 395, w: 320, h: 108, title: "Foundation", detail: "core + coroutines", color: "blue" },
    { id: "integration", x: 515, y: 395, w: 320, h: 108, title: "Integration APIs", detail: "io, data, infra", color: "teal" },
    { id: "appstack", x: 890, y: 395, w: 320, h: 108, title: "Application Stacks", detail: "Ktor + Spring Boot 4", color: "amber" },
    { id: "runtime", x: 1265, y: 395, w: 260, h: 108, title: "Runtime Options", detail: "coroutines + VT", color: "olive" },
    { id: "state", x: 160, y: 650, w: 300, h: 108, title: "Domain Utilities", detail: "states + workflow", color: "purple" },
    { id: "test", x: 525, y: 650, w: 300, h: 108, title: "Test Support", detail: "JUnit + containers", color: "pink" },
    { id: "examples", x: 890, y: 650, w: 300, h: 108, title: "Runnable Examples", detail: "Ktor and Spring demos", color: "gray" },
    { id: "publish", x: 1255, y: 650, w: 290, h: 108, title: "Published Artifacts", detail: "Maven modules", color: "green" },
  ];
  const routes = [
    route("consumer", "bom", [[440, 224], [660, 224]], "imports", "blue"),
    route("catalog", "bom", [[1200, 224], [980, 224]], "pins", "purple"),
    route("bom", "foundation", [[820, 268], [820, 330], [300, 330], [300, 395]], "aligns", "green"),
    route("foundation", "integration", [[460, 449], [515, 449]], "extends", "blue"),
    route("integration", "appstack", [[835, 449], [890, 449]], "feeds", "teal"),
    route("appstack", "runtime", [[1210, 449], [1265, 449]], "runs on", "amber"),
    route("foundation", "state", [[300, 503], [300, 650]], "shared types", "blue"),
    route("integration", "test", [[675, 503], [675, 650]], "verified by", "teal"),
    route("appstack", "examples", [[1050, 503], [1050, 650]], "demonstrates", "amber"),
    route("runtime", "publish", [[1395, 503], [1395, 650]], "packaged", "olive"),
    route("test", "publish", [[825, 704], [1255, 704]], "release confidence", "pink"),
  ];
  return frame({
    width,
    height,
    title: "Repository Module Architecture",
    subtitle: "Layered module responsibility from consumer entrypoint to published runtime artifacts.",
    desc: "Architecture diagram based on the current root README module structure and Gradle source tree.",
    intent: "Explain bluetape4k-projects as a layered architecture where consumer dependency alignment, foundation modules, integration APIs, application stacks, runtime choices, verification, examples, and published artifacts have distinct responsibilities.",
    evidence: "README.md, README.ko.md, AGENTS.md module groups, settings.gradle.kts, module build.gradle.kts files",
    sourceRead: "README.md;README.ko.md;AGENTS.md;settings.gradle.kts;*/build.gradle.kts",
    layers: [
      layer("Consumer entry and version alignment", 70, 140, 1500, 165),
      layer("Library architecture", 70, 360, 1500, 185),
      layer("Support, examples, and delivery", 70, 615, 1500, 185),
    ],
    cards,
    routes,
    footer: "Architecture excludes sequence call flows; sequence assets are preserved.",
  });
}

function moduleChartSvg() {
  const width = 1500;
  const height = 940;
  const max = Math.max(...Object.values(moduleCounts));
  const bars = groups.map(([id, label], index) => ({
    id,
    label,
    value: moduleCounts[id],
    y: 170 + index * 60,
  }));
  const lines = [];
  lines.push(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="title desc">`);
  lines.push(`<title id="title">Bluetape4k Module Composition Chart</title>`);
  lines.push(`<desc id="desc">Source-backed module composition chart generated from current Gradle module directories.</desc>`);
  lines.push(styleBlock());
  lines.push(`<rect width="${width}" height="${height}" fill="#fffaf0"/>`);
  lines.push(`<rect x="52" y="50" width="${width - 104}" height="${height - 100}" rx="18" fill="#fffdf8" stroke="#d9b97c" stroke-width="2"/>`);
  lines.push(`<text class="chartTitle" x="88" y="100">Module Composition Chart</text>`);
  lines.push(`<text class="chartSub" x="88" y="132">${totalModules} Gradle modules grouped by repository responsibility</text>`);
  lines.push(`<line x1="400" y1="160" x2="400" y2="830" stroke="#dcc6a0" stroke-width="1"/>`);
  lines.push(`<line x1="400" y1="830" x2="1330" y2="830" stroke="#dcc6a0" stroke-width="1"/>`);
  for (const tick of [0, 5, 10, 15, 20, 25, 30, 35]) {
    const x = 400 + (tick / Math.max(35, max)) * 900;
    lines.push(`<line x1="${x}" y1="826" x2="${x}" y2="836" stroke="#9f8b68"/>`);
    lines.push(`<text class="chartTick" x="${x}" y="858" text-anchor="middle">${tick}</text>`);
  }
  for (const bar of bars) {
    const barWidth = Math.max(18, (bar.value / Math.max(35, max)) * 900);
    lines.push(`<text class="chartLabel" x="370" y="${bar.y + 20}" text-anchor="end">${esc(bar.label)}</text>`);
    lines.push(`<rect x="400" y="${bar.y}" width="${barWidth.toFixed(1)}" height="34" rx="8" fill="${paletteFor(bar.id)[0]}" stroke="${paletteFor(bar.id)[1]}" stroke-width="1.5"/>`);
    lines.push(`<text class="chartValue" x="${400 + barWidth + 16}" y="${bar.y + 23}">${bar.value}</text>`);
  }
  lines.push(`<text class="chartFoot" x="88" y="875">Chart style preserved: warm canvas, horizontal bars, explicit counts, SVG+PNG pair.</text>`);
  lines.push(`</svg>`);
  return lines.join("\n");
}

function frame({ width, height, title, subtitle, desc, intent, evidence, sourceRead, layers, cards, routes, footer }) {
  const lines = [];
  lines.push(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="title desc" data-intent="${esc(intent)}" data-evidence="${esc(evidence)}" data-source-read="${esc(sourceRead)}">`);
  lines.push(`<title id="title">${esc(title)}</title>`);
  lines.push(`<desc id="desc">${esc(desc)}</desc>`);
  lines.push(styleBlock());
  lines.push(`<rect width="${width}" height="${height}" fill="#ffffff"/>`);
  lines.push(`<rect x="34" y="34" width="${width - 68}" height="${height - 68}" rx="8" fill="#f8fafc" stroke="#d1d5db" stroke-width="1.5"/>`);
  lines.push(`<text class="title" x="70" y="92">${esc(title)}</text>`);
  lines.push(`<text class="subtitle" x="70" y="124">${esc(subtitle)}</text>`);
  for (const item of layers) lines.push(layerSvg(item));
  for (const item of routes) lines.push(routeSvg(item));
  for (const item of cards) lines.push(cardSvg(item));
  lines.push(legendSvg(width - 520, height - 120));
  lines.push(`<text class="footer" x="70" y="${height - 46}">${esc(footer)}</text>`);
  lines.push(`</svg>`);
  return lines.join("\n");
}

function styleBlock() {
  return `<style>
    svg { font-family: "Architects Daughter", "Comic Mono", "Comic Sans MS", ui-sans-serif, system-ui, sans-serif; }
    .title { fill: #0f172a; font-size: 32px; font-weight: 800; letter-spacing: 0; }
    .subtitle { fill: #475569; font-size: 17px; font-weight: 500; }
    .layer { fill: #ffffff; stroke: #cbd5e1; stroke-width: 1.4; }
    .layerTitle { fill: #334155; font-size: 14px; font-weight: 800; letter-spacing: .2px; text-transform: uppercase; }
    .card { stroke-width: 1.7; filter: url(#cardShadow); }
    .cardTitle { fill: #0f172a; font-size: 16px; font-weight: 800; }
    .cardDetail { fill: #475569; font-size: 12px; font-weight: 600; }
    .iconBadge { stroke-width: 1.5; }
    .iconStroke { fill: none; stroke: #ffffff; stroke-width: 2.4; stroke-linecap: round; stroke-linejoin: round; }
    .iconFill { fill: #ffffff; }
    .route { fill: none; stroke-width: 2.6; stroke-linecap: round; stroke-linejoin: round; marker-end: url(#arrow); }
    .routeLabel { fill: #1e293b; font-size: 11px; font-weight: 700; text-anchor: middle; }
    .legend { fill: #ffffff; stroke: #cbd5e1; stroke-width: 1.4; filter: url(#cardShadow); }
    .legendText { fill: #334155; font-size: 12px; font-weight: 700; }
    .footer, .chartFoot { fill: #64748b; font-size: 13px; font-weight: 600; }
    .chartTitle { fill: #352617; font-size: 34px; font-weight: 700; }
    .chartSub { fill: #6f5b3c; font-size: 18px; }
    .chartLabel { fill: #3d3528; font-size: 16px; font-weight: 700; }
    .chartValue { fill: #3d3528; font-size: 15px; font-weight: 700; }
    .chartTick { fill: #7a6a52; font-size: 12px; }
  </style>
  <defs>
    <filter id="cardShadow" x="-8%" y="-12%" width="116%" height="130%">
      <feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0f172a" flood-opacity="0.10"/>
    </filter>
    <marker id="arrow" markerWidth="12" markerHeight="8" refX="10" refY="4" orient="auto">
      <path d="M0,0 L12,4 L0,8 Z" fill="#475569"/>
    </marker>
  </defs>`;
}

function layer(id, x, y, w, h) {
  return { id, x, y, w, h };
}

function layerSvg(item) {
  return `<g><rect class="layer" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="8"/><text class="layerTitle" x="${item.x + 20}" y="${item.y + 28}">${esc(item.id)}</text></g>`;
}

function cardSvg(item) {
  const [fill, stroke] = paletteFor(item.color);
  const iconX = item.x + 22;
  const iconY = item.y + Math.max(22, item.h / 2 - 21);
  return `<g id="${esc(item.id)}"><rect class="card" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="8" fill="${fill}" stroke="${stroke}"/>${iconSvg(item.id, iconX, iconY, stroke)}<text class="cardTitle" x="${item.x + 82}" y="${item.y + item.h / 2 - 6}">${esc(item.title)}</text><text class="cardDetail" x="${item.x + 82}" y="${item.y + item.h / 2 + 22}">${esc(item.detail)}</text></g>`;
}

function iconSvg(id, x, y, color) {
  const glyphs = {
    settings: `<path class="iconStroke" d="M13 9h16M13 21h16M13 33h16"/><circle class="iconFill" cx="19" cy="9" r="3"/><circle class="iconFill" cx="27" cy="21" r="3"/><circle class="iconFill" cx="21" cy="33" r="3"/>`,
    catalog: `<path class="iconStroke" d="M14 10h17v24H14zM19 16h7M19 22h7M19 28h5"/>`,
    bom: `<path class="iconStroke" d="M21 8l13 7v14l-13 7-13-7V15zM21 8v14M8 15l13 7 13-7"/>`,
    consumer: `<path class="iconStroke" d="M9 13h26v18H9zM15 36h14M21 31v5"/>`,
    foundation: `<path class="iconStroke" d="M10 32h24M13 28h18M16 24h12M21 10l13 14H8z"/>`,
    integration: `<path class="iconStroke" d="M12 12h10v10H12zM24 24h10v10H24zM22 17h6M17 22v6"/>`,
    appstack: `<path class="iconStroke" d="M10 13h24M10 21h24M10 29h24"/><circle class="iconFill" cx="15" cy="13" r="2"/><circle class="iconFill" cx="15" cy="21" r="2"/><circle class="iconFill" cx="15" cy="29" r="2"/>`,
    runtime: `<path class="iconStroke" d="M22 8v8M22 28v8M8 22h8M28 22h8M14 14l6 6M30 14l-6 6M14 30l6-6M30 30l-6-6"/>`,
    state: `<path class="iconStroke" d="M10 12h11v9H10zM23 23h11v9H23zM21 16h6M17 21v6"/>`,
    test: `<path class="iconStroke" d="M15 9h14M22 9v11l9 14H13l9-14z"/>`,
    publish: `<path class="iconStroke" d="M22 9v20M14 17l8-8 8 8M11 33h22"/>`,
    io: `<path class="iconStroke" d="M10 22h24M16 14l-6 8 6 8M28 14l6 8-6 8"/>`,
    data: `<ellipse class="iconStroke" cx="22" cy="12" rx="12" ry="5"/><path class="iconStroke" d="M10 12v20c0 3 24 3 24 0V12M10 22c0 3 24 3 24 0"/>`,
    infra: `<path class="iconStroke" d="M12 29h20M14 22h16M16 15h12M18 8h8"/>`,
    cache: `<path class="iconStroke" d="M12 13h20v18H12zM17 18h10M17 25h6"/>`,
    ktor: `<path class="iconStroke" d="M10 24l8-12 8 12-8 8zM26 24l6-9 6 9-6 7z"/>`,
    spring: `<path class="iconStroke" d="M11 28c9 6 22 0 22-11-10 0-18 3-22 11zM20 27c1-5 5-9 11-10"/>`,
    virtual: `<path class="iconStroke" d="M11 29c2-11 6-16 11-16s9 5 11 16M14 29h16M18 21h8"/>`,
    testing: `<path class="iconStroke" d="M12 23l7 7 14-16"/>`,
    utils: `<path class="iconStroke" d="M22 9v26M9 22h26M14 14l16 16M30 14L14 30"/>`,
    examples: `<path class="iconStroke" d="M14 10l18 12-18 12z"/>`,
  };
  const glyph = glyphs[id] ?? `<circle class="iconStroke" cx="22" cy="22" r="12"/>`;
  return `<g transform="translate(${x},${y})"><rect class="iconBadge" x="0" y="0" width="44" height="44" rx="10" fill="${color}" stroke="${color}"/>${glyph}</g>`;
}

function route(from, to, points, label, color) {
  return { from, to, points, label, color };
}

function routeSvg(item) {
  const stroke = paletteFor(item.color)[1];
  const d = item.points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]},${point[1]}`).join(" ");
  const middle = item.points[Math.floor(item.points.length / 2)];
  return `<g data-route="${esc(item.from)}->${esc(item.to)}"><path class="route" data-from="${esc(item.from)}" data-to="${esc(item.to)}" d="${d}" stroke="${stroke}"/><text class="routeLabel" x="${middle[0]}" y="${middle[1] - 10}">${esc(item.label)}</text></g>`;
}

function legendSvg(x, y) {
  return `<g><rect class="legend" x="${x}" y="${y}" width="450" height="58" rx="8"/><line x1="${x + 24}" y1="${y + 24}" x2="${x + 88}" y2="${y + 24}" stroke="#2563eb" stroke-width="2.4" marker-end="url(#arrow)"/><text class="legendText" x="${x + 105}" y="${y + 29}">dependency / responsibility flow</text><line x1="${x + 24}" y1="${y + 44}" x2="${x + 88}" y2="${y + 44}" stroke="#9333ea" stroke-width="2.4" marker-end="url(#arrow)"/><text class="legendText" x="${x + 105}" y="${y + 49}">version or runtime alignment</text></g>`;
}

function paletteFor(key) {
  const colors = {
    blue: ["#eff6ff", "#2563eb"],
    green: ["#f0fdf4", "#16a34a"],
    teal: ["#f0fdfa", "#0d9488"],
    amber: ["#fff7ed", "#ea580c"],
    pink: ["#fdf2f8", "#db2777"],
    purple: ["#faf5ff", "#9333ea"],
    olive: ["#f7fee7", "#65a30d"],
    gray: ["#f9fafb", "#6b7280"],
  };
  return colors[key] ?? colors.gray;
}

function ensureDir(dir) {
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
  if (!statSync(dirname(dir)).isDirectory()) throw new Error(`Invalid parent for ${dir}`);
}

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
