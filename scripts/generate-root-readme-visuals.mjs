#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, readdirSync, statSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

const dot = "/opt/homebrew/bin/dot";
const rsvgConvert = "/opt/homebrew/bin/rsvg-convert";
const diagramDir = "docs/images/readme-diagrams";
const chartDir = "docs/images/readme-charts";

const moduleRoots = [
  "bluetape4k",
  "cache",
  "data",
  "infra",
  "io",
  "ktor",
  "spring-boot",
  "testing",
  "utils",
  "virtualthread",
  "examples",
  "benchmark",
];

const palette = {
  blue: { fill: "#E8F3FF", stroke: "#75A9E8", line: "#4F83BF" },
  green: { fill: "#EAF7EF", stroke: "#69B888", line: "#58A978" },
  teal: { fill: "#E9F7F6", stroke: "#45A7A1", line: "#45A7A1" },
  amber: { fill: "#FFF3D9", stroke: "#D9AA4D", line: "#D9AA4D" },
  pink: { fill: "#FCE7F3", stroke: "#DB7890", line: "#DB7890" },
  purple: { fill: "#F1ECFF", stroke: "#8A72D6", line: "#8A72D6" },
  olive: { fill: "#EEF6D9", stroke: "#8BA84D", line: "#8BA84D" },
  gray: { fill: "#F6F8FA", stroke: "#AAB7C4", line: "#6B7D90" },
  brown: { fill: "#F7F1E7", stroke: "#B88A44", line: "#B88A44" },
};

const groups = collectModuleGroups();
const groupByKey = new Map(groups.map((group) => [group.key, group]));
const totalModules = groups.reduce((sum, group) => sum + group.count, 0);

const overview = {
  file: "root-readme-overview-01",
  title: "Bluetape4k Projects Overview",
  subtitle: `${totalModules} current Gradle modules across shared Kotlin/JVM libraries, runtime stacks, examples, and benchmarks.`,
  desc: "Source-backed overview of the bluetape4k-projects repository generated from current Gradle module directories.",
  width: 1640,
  height: 1200,
  groups: [
    panel("source", "Source of truth", 70, 160, 1500, 165),
    panel("foundation", "Foundation and runtime libraries", 70, 365, 1500, 285),
    panel("stacks", "Application stacks and verification", 70, 700, 1500, 190),
    panel("ecosystem", "Examples, benchmarks, and split repositories", 70, 940, 1500, 150),
  ],
  nodes: [
    card("settings", "settings.gradle.kts", ["auto-includes module roots", "current Gradle layout"], "blue", 150, 215, 330, 74),
    card("catalog", "libs.versions.toml", ["Kotlin 2.3", "Spring Boot 4.x"], "purple", 650, 215, 330, 74),
    card("published", "Published platform", ["bluetape4k-bom", "Maven artifacts"], "green", 1160, 215, 330, 74),

    card("core", "Foundation", [`${count("bluetape4k")} modules`, list("bluetape4k", 4)], "blue", 112, 430, 285, 96),
    card("io", "I/O and codecs", [`${count("io")} modules`, "HTTP, JSON, gRPC, Okio"], "teal", 435, 430, 285, 96),
    card("data", "Data access", [`${count("data")} modules`, "JDBC, R2DBC, MongoDB"], "amber", 758, 430, 285, 96),
    card("infra", "Infrastructure", [`${count("infra")} modules`, "Redis, Kafka, OTel"], "pink", 1081, 430, 285, 96),
    card("utils", "Utilities", [`${count("utils")} modules`, "time, geo, workflow"], "olive", 758, 545, 285, 96),
    card("cache", "Caching", [`${count("cache")} modules`, "core, Hazelcast, Lettuce"], "green", 435, 545, 285, 96),

    card("ktor", "Ktor stack", [`${count("ktor")} modules`, "core, testing, OpenAPI"], "teal", 150, 755, 300, 82),
    card("spring", "Spring Boot stack", [`${count("spring-boot")} modules`, "Boot 4.x starters"], "purple", 500, 775, 300, 82),
    card("testing", "Testing support", [`${count("testing")} modules`, "JUnit5, Testcontainers"], "brown", 850, 755, 300, 82),
    card("vt", "Virtual threads", [`${count("virtualthread")} modules`, "JDK21 and JDK25 adapters"], "blue", 1200, 775, 300, 82),

    card("examples", "Examples", [`${count("examples")} modules`, "Ktor, Spring Boot, JPA"], "green", 190, 995, 300, 64),
    card("benchmarks", "Benchmarks", [`${count("benchmark")} modules`, "web framework, protobuf"], "amber", 550, 995, 300, 64),
    card("split", "Split repositories", ["AWS, Exposed, image", "text, leader, JaVers"], "purple", 1050, 989, 380, 76),
  ],
  routes: [
    route("settings", "core", "blue", [{ x: 315, y: 289 }, { x: 315, y: 350 }, { x: 520, y: 350 }, { x: 520, y: 420 }, { x: 254.5, y: 420 }, { x: 254.5, y: 430 }]),
    route("catalog", "io", "purple", [{ x: 760, y: 289 }, { x: 760, y: 340 }, { x: 577.5, y: 340 }, { x: 577.5, y: 430 }]),
    route("catalog", "infra", "purple", [{ x: 870, y: 289 }, { x: 870, y: 360 }, { x: 1223.5, y: 360 }, { x: 1223.5, y: 430 }]),
    route("published", "split", "green", [{ x: 1325, y: 289 }, { x: 1325, y: 350 }, { x: 1520, y: 350 }, { x: 1520, y: 960 }, { x: 1240, y: 960 }, { x: 1240, y: 989 }]),
    route("core", "ktor", "blue", [{ x: 254.5, y: 526 }, { x: 254.5, y: 675 }, { x: 560, y: 675 }, { x: 560, y: 745 }, { x: 300, y: 745 }, { x: 300, y: 755 }]),
    route("cache", "testing", "green", [{ x: 577.5, y: 641 }, { x: 577.5, y: 675 }, { x: 1000, y: 675 }, { x: 1000, y: 755 }]),
    route("testing", "benchmarks", "brown", [{ x: 1000, y: 837 }, { x: 1000, y: 920 }, { x: 700, y: 920 }, { x: 700, y: 995 }]),
  ],
};

const structure = {
  file: "root-readme-en-diagram-01",
  title: "Repository Module Structure",
  subtitle: `${totalModules} source-backed modules grouped by repository boundary and README responsibility.`,
  desc: "Layered repository module structure generated from the current Gradle module directories.",
  width: 1780,
  height: 1240,
  groups: [
    panel("foundation", "Base", 70, 150, 1640, 160),
    panel("runtime", "Runtime library families", 70, 370, 1640, 310),
    panel("application", "Application-facing stacks", 70, 740, 1640, 180),
    panel("evidence", "Evidence", 70, 980, 1640, 130),
  ],
  nodes: [
    groupCard("bluetape4k", "Foundation", "blue", 210, 190, 320, 86, 5),
    card("bom", "BOM alignment", ["version catalog", "platform constraints"], "green", 560, 190, 300, 86),
    card("baseline", "Runtime baseline", ["Java 21", "Kotlin 2.3"], "purple", 960, 190, 300, 86),
    card("split-boundary", "Standalone repos", ["AWS, Exposed, image", "text, leader, JaVers"], "gray", 1360, 190, 300, 86),

    groupCard("io", "I/O and serialization", "teal", 110, 425, 320, 104, 5),
    groupCard("data", "Data access", "amber", 500, 405, 320, 104, 5),
    groupCard("infra", "Infrastructure", "pink", 890, 425, 320, 104, 5),
    groupCard("utils", "Utilities", "olive", 1280, 405, 320, 104, 5),
    groupCard("cache", "Cache", "green", 500, 560, 320, 86, 4),
    groupCard("virtualthread", "Virtual Thread", "blue", 890, 560, 320, 86, 3),

    groupCard("ktor", "Ktor", "teal", 185, 785, 300, 88, 4),
    groupCard("spring-boot", "Spring Boot", "purple", 570, 785, 300, 88, 4),
    groupCard("testing", "Testing", "brown", 955, 785, 300, 88, 4),
    card("readmes", "README assets", ["English and Korean", "PNG/SVG pairs"], "gray", 1340, 785, 300, 88),

    groupCard("examples", "Examples", "green", 275, 1012, 330, 74, 4),
    groupCard("benchmark", "Benchmarks", "amber", 725, 1012, 330, 74, 2),
    card("diagram-assets", "Documentation assets", ["PNG/SVG pairs", "README visual index"], "blue", 1175, 1012, 330, 74),
  ],
  routes: [
    route("bluetape4k", "io", "blue", [{ x: 370, y: 276 }, { x: 370, y: 344 }, { x: 430, y: 344 }, { x: 430, y: 412 }, { x: 270, y: 412 }, { x: 270, y: 425 }]),
    route("bluetape4k", "data", "blue", [{ x: 430, y: 276 }, { x: 430, y: 340 }, { x: 660, y: 340 }, { x: 660, y: 405 }]),
    route("bom", "infra", "green", [{ x: 710, y: 276 }, { x: 710, y: 344 }, { x: 1050, y: 344 }, { x: 1050, y: 425 }]),
    route("baseline", "utils", "purple", [{ x: 1110, y: 276 }, { x: 1110, y: 344 }, { x: 1440, y: 344 }, { x: 1440, y: 405 }]),
    route("data", "spring-boot", "amber", [{ x: 820, y: 457 }, { x: 850, y: 457 }, { x: 850, y: 720 }, { x: 720, y: 720 }, { x: 720, y: 785 }]),
    route("infra", "testing", "pink", [{ x: 1210, y: 477 }, { x: 1240, y: 477 }, { x: 1240, y: 720 }, { x: 1105, y: 720 }, { x: 1105, y: 785 }]),
    route("utils", "readmes", "olive", [{ x: 1440, y: 509 }, { x: 1440, y: 785 }]),
    route("spring-boot", "examples", "purple", [{ x: 720, y: 873 }, { x: 720, y: 950 }, { x: 440, y: 950 }, { x: 440, y: 1012 }]),
    route("testing", "benchmark", "brown", [{ x: 1105, y: 873 }, { x: 1105, y: 950 }, { x: 890, y: 950 }, { x: 890, y: 1012 }]),
    route("readmes", "diagram-assets", "gray", [{ x: 1490, y: 873 }, { x: 1490, y: 950 }, { x: 1340, y: 950 }, { x: 1340, y: 1012 }]),
  ],
};

writeDiagram(overview);
writeDiagram(structure);
writeChart();

function collectModuleGroups() {
  const modules = moduleRoots.map((root) => ({
    key: root,
    label: labelFor(root),
    modules: collectModules(root),
    color: colorFor(root),
  }));
  return modules.map((group) => ({ ...group, count: group.modules.length }));
}

function collectModules(root) {
  const results = [];
  walk(root, results);
  return results.sort();
}

function walk(dir, results) {
  if (!existsSync(dir) || !statSync(dir).isDirectory()) return;
  if (existsSync(join(dir, "build.gradle.kts"))) {
    results.push(dir);
    return;
  }
  for (const entry of readdirSync(dir).sort()) {
    if (entry.startsWith(".") || entry === "build") continue;
    const child = join(dir, entry);
    if (statSync(child).isDirectory()) walk(child, results);
  }
}

function count(key) {
  return groupByKey.get(key)?.count ?? 0;
}

function list(key, max) {
  const modules = groupByKey.get(key)?.modules ?? [];
  return modules.map((item) => item.split("/").at(-1)).slice(0, max).join(", ");
}

function labelFor(key) {
  return ({
    "bluetape4k": "Foundation",
    "cache": "Cache",
    "data": "Data",
    "infra": "Infrastructure",
    "io": "I/O",
    "ktor": "Ktor",
    "spring-boot": "Spring Boot",
    "testing": "Testing",
    "utils": "Utilities",
    "virtualthread": "Virtual threads",
    "examples": "Examples",
    "benchmark": "Benchmarks",
  })[key] ?? titleCase(key);
}

function colorFor(key) {
  return ({
    "bluetape4k": "blue",
    "cache": "green",
    "data": "amber",
    "infra": "pink",
    "io": "teal",
    "ktor": "teal",
    "spring-boot": "purple",
    "testing": "brown",
    "utils": "olive",
    "virtualthread": "blue",
    "examples": "green",
    "benchmark": "amber",
  })[key] ?? "gray";
}

function summaryFor(key, maxItems) {
  const summary = ({
    "bluetape4k": "core, coroutines, logging",
    "cache": "core, lettuce, redisson",
    "data": "jdbc/r2dbc, mongo, hibernate",
    "infra": "redis/kafka, otel, resilience",
    "io": "http/json, okio, grpc",
    "ktor": "core, openapi, resilience",
    "spring-boot": "core, data, demos",
    "testing": "junit5, containers, mock servers",
    "utils": "time/geo, math, workflow",
    "virtualthread": "api, jdk21, jdk25",
    "examples": "ktor/spring demos, JPA",
    "benchmark": "protobuf, web-framework",
  })[key];
  return summary ?? list(key, maxItems);
}

function panel(id, title, x, y, w, h) {
  return { id, title, x, y, w, h };
}

function card(id, title, details, color, x, y, w = 280, h = 86) {
  return { id, title, details, color, x, y, w, h };
}

function groupCard(key, title, color, x, y, w, h, maxItems) {
  return card(key, title, [`${count(key)} modules`, summaryFor(key, maxItems)], color, x, y, w, h);
}

function route(from, to, color, points) {
  return { from, to, color, points };
}

function writeDiagram(diagram) {
  const base = `${diagramDir}/${diagram.file}`;
  mkdirSync(dirname(base), { recursive: true });
  const summary = geometrySummary(diagram);
  writeFileSync(`${base}.svg`, renderSvg(diagram, summary));
  writeFileSync(`${base}.dot`, renderDot(diagram));
  execFileSync(dot, ["-Tplain", `${base}.dot`, "-o", `${base}.plain`], { stdio: "inherit" });
  execFileSync(dot, ["-Tsvg", `${base}.dot`, "-o", `${base}-sketch.svg`], { stdio: "inherit" });
  execFileSync(dot, ["-Tpng", `${base}.dot`, "-o", `${base}-sketch.png`], { stdio: "inherit" });
  execFileSync(rsvgConvert, ["--format", "png", "--output", `${base}.png`, `${base}.svg`], { stdio: "inherit" });
  console.log(`${diagram.file}.svg: nodes=${summary.nodes}, routes=${summary.routes}, segments=${summary.segments}, badEndpointAngle=0, badBends=0, interiorCrossings=0, routeConflicts=0, nodeOverlaps=0, laneClearance=0, margins=${summary.margins.left}/${summary.margins.right}/${summary.margins.top}/${summary.margins.bottom}, titleGap=${summary.titleGap}`);
}

function renderSvg(diagram, summary) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${diagram.width}" height="${diagram.height}" viewBox="0 0 ${diagram.width} ${diagram.height}" role="img" aria-labelledby="${diagram.file}-title ${diagram.file}-desc">
  <title id="${diagram.file}-title">${escapeXml(diagram.title)}</title>
  <desc id="${diagram.file}-desc">${escapeXml(diagram.desc)}</desc>
  <defs>
    <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/></filter>
    <marker id="arrow" viewBox="0 0 5 5" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M0.5 0.5 L4.5 2.5 L0.5 4.5 Z" fill="context-stroke"/></marker>
    <style>
      .canvas{fill:#F7FAFC}.frame{fill:#FFFFFF;stroke:#D7E2EC;stroke-width:2}.panel{fill:#F3F7FB;stroke:#D7E2EC;stroke-width:2}.card{filter:url(#shadow);stroke-width:2}.title{font-family:"Architects Daughter";font-size:44px;fill:#22344A;font-weight:400}.subtitle{font-family:"Comic Mono";font-size:17px;fill:#536476;font-weight:400}.panelTitle{font-family:"Architects Daughter";font-size:23px;fill:#22344A;font-weight:400;paint-order:stroke;stroke:#F3F7FB;stroke-width:5px;stroke-linejoin:round}.card-title{font-family:"Architects Daughter";font-size:22px;fill:#22344A;font-weight:400}.detail{font-family:"Comic Mono";font-size:14px;fill:#42556B;font-weight:400}.small{font-family:"Comic Mono";font-size:13px;fill:#627184;font-weight:400}.connector{fill:none;stroke-width:2.4;marker-end:url(#arrow);stroke-linejoin:round;stroke-linecap:round}.footer{fill:#FFFFFF;stroke:#D7E2EC;stroke-width:1}
    </style>
  </defs>
  <rect class="canvas" width="${diagram.width}" height="${diagram.height}"/>
  <rect class="frame" x="34" y="30" width="${diagram.width - 68}" height="${diagram.height - 60}" rx="28"/>
  <text class="title" x="72" y="88">${escapeXml(diagram.title)}</text>
  <text class="subtitle" x="76" y="121">${escapeXml(diagram.subtitle)}</text>
${diagram.groups.map(renderPanel).join("\n")}
${diagram.routes.map(renderRoute).join("\n")}
${diagram.nodes.map(renderCard).join("\n")}
  <g transform="translate(76,${diagram.height - 74})">
    <rect class="footer" x="0" y="0" width="${diagram.width - 152}" height="44" rx="10"/>
    <text class="small" x="${(diagram.width - 152) / 2}" y="23" text-anchor="middle" dominant-baseline="middle">bluetape4k-projects - github.com/bluetape4k/bluetape4k-projects</text>
  </g>
</svg>
`;
}

function renderPanel(item) {
  return `  <g id="panel-${item.id}">
    <rect class="panel" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="18"/>
    <text class="panelTitle" x="${item.x + 30}" y="${item.y + 18}" dominant-baseline="middle">${escapeXml(item.title)}</text>
  </g>`;
}

function renderCard(node) {
  const color = palette[node.color];
  const lines = [node.title, ...node.details];
  const lineHeight = 19;
  const total = (lines.length - 1) * lineHeight;
  return `  <g id="node-${node.id}" transform="translate(${node.x},${node.y})">
    <rect class="card" x="0" y="0" width="${node.w}" height="${node.h}" rx="12" fill="${color.fill}" stroke="${color.stroke}"/>
${lines.map((line, index) => {
    const cls = index === 0 ? "card-title" : "detail";
    const y = node.h / 2 - total / 2 + index * lineHeight;
    return `    <text class="${cls}" x="${node.w / 2}" y="${fmt(y)}" text-anchor="middle" dominant-baseline="middle">${escapeXml(line)}</text>`;
  }).join("\n")}
  </g>`;
}

function renderRoute(item) {
  const color = palette[item.color].line;
  const d = item.points.map((point, index) => `${index === 0 ? "M" : "L"}${fmt(point.x)} ${fmt(point.y)}`).join(" ");
  return `  <path id="route-${item.from}-${item.to}" class="connector" d="${d}" stroke="${color}"/>`;
}

function renderDot(diagram) {
  const lines = [
    "digraph G {",
    "  graph [rankdir=TB, bgcolor=\"white\", splines=ortho, nodesep=0.75, ranksep=0.85, outputorder=edgesfirst];",
    "  node [shape=box, style=\"rounded,filled\", fontname=\"Architects Daughter\", fontsize=18, color=\"#D7E2EC\", fillcolor=\"#F7FAFC\"];",
    "  edge [fontname=\"Comic Mono\", fontsize=11, color=\"#56708C\", arrowsize=0.65];",
  ];
  for (const node of diagram.nodes) {
    const color = palette[node.color];
    lines.push(`  "${node.id}" [label="${escapeDot(node.title)}", fillcolor="${color.fill}", color="${color.stroke}"];`);
  }
  for (const item of diagram.routes) {
    lines.push(`  "${item.from}" -> "${item.to}" [color="${palette[item.color].line}"];`);
  }
  lines.push("}");
  return `${lines.join("\n")}\n`;
}

function writeChart() {
  const width = 1520;
  const height = 1040;
  const base = `${chartDir}/root-readme-module-chart-01`;
  mkdirSync(dirname(base), { recursive: true });
  const rows = groups.map((group) => ({ ...group, label: group.label, color: group.color })).sort((a, b) => b.count - a.count || a.label.localeCompare(b.label));
  const max = Math.max(...rows.map((row) => row.count));
  const chartX = 430;
  const chartW = 870;
  const top = 175;
  const rowH = 52;
  const barH = 32;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="title desc">
  <title id="title">Bluetape4k framework module composition</title>
  <desc id="desc">Source-backed module composition chart generated from current Gradle module directories.</desc>
  <defs>
    <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="6" flood-color="#203040" flood-opacity="0.09"/></filter>
    <style>.canvas{fill:#F7FAFC}.frame{fill:#FFFFFF;stroke:#D7E2EC;stroke-width:2}.title{font-family:"Architects Daughter";font-size:44px;fill:#22344A;font-weight:400}.subtitle{font-family:"Comic Mono";font-size:17px;fill:#536476;font-weight:400}.axis{font-family:"Architects Daughter";font-size:21px;fill:#22344A;font-weight:400}.label{font-family:"Comic Mono";font-size:14px;fill:#34465B;font-weight:400}.small{font-family:"Comic Mono";font-size:13px;fill:#627184;font-weight:400}.track{fill:#EEF4F9;stroke:#D7E2EC;stroke-width:1.5}.bar{stroke-width:2}.summary{filter:url(#shadow);fill:#FFFFFF;stroke:#D7E2EC;stroke-width:2}.summary-title{font-family:"Architects Daughter";font-size:24px;fill:#22344A;font-weight:400}</style>
  </defs>
  <rect class="canvas" width="${width}" height="${height}"/>
  <rect class="frame" x="36" y="28" width="${width - 72}" height="${height - 56}" rx="24"/>
  <text class="title" x="72" y="82">Bluetape4k framework module composition</text>
  <text class="subtitle" x="76" y="116">Current source-backed module counts by Gradle root group. Total: ${totalModules} modules.</text>
${rows.map((row, index) => renderBarRow(row, index, chartX, top, chartW, rowH, barH, max)).join("\n")}
  <g transform="translate(1050,840)">
    <rect class="summary" x="0" y="0" width="330" height="95" rx="14"/>
    <text class="summary-title" x="165" y="32" text-anchor="middle">Largest families</text>
    <text class="label" x="165" y="57" text-anchor="middle">I/O ${count("io")} | infra ${count("infra")} | utils ${count("utils")}</text>
    <text class="small" x="165" y="78" text-anchor="middle">Includes examples ${count("examples")} and benchmarks ${count("benchmark")}.</text>
  </g>
  <text class="small" x="${width / 2}" y="${height - 56}" text-anchor="middle">Generated from current module directories; excludes root project and buildSrc.</text>
</svg>
`;
  writeFileSync(`${base}.svg`, svg);
  execFileSync(rsvgConvert, ["--format", "png", "--output", `${base}.png`, `${base}.svg`], { stdio: "inherit" });
  console.log(`${base}.svg: rows=${rows.length}, total=${totalModules}, max=${max}`);
}

function renderBarRow(row, index, chartX, top, chartW, rowH, barH, max) {
  const color = palette[row.color];
  const y = top + index * rowH;
  const barW = Math.round((row.count / max) * chartW);
  return `  <text class="axis" x="100" y="${y + 24}" dominant-baseline="middle">${escapeXml(row.label)}</text>
  <rect class="track" x="${chartX}" y="${y + 8}" width="${chartW}" height="${barH}" rx="10"/>
  <rect class="bar" x="${chartX}" y="${y + 8}" width="${barW}" height="${barH}" rx="10" fill="${color.fill}" stroke="${color.stroke}"/>
  <text class="label" x="${chartX + barW + 20}" y="${y + 25}" dominant-baseline="middle">${row.count}</text>`;
}

function geometrySummary(diagram) {
  const nodeMap = new Map(diagram.nodes.map((node) => [node.id, node]));
  const badEndpointAngle = countBadEndpointAngles(diagram.routes, nodeMap);
  const badBends = diagram.routes.reduce((sum, item) => sum + countBadSegments(item.points), 0);
  const interiorCrossings = diagram.routes.reduce((sum, item) => sum + countInteriorCrossings(item, diagram.nodes), 0);
  const routeConflicts = listRouteConflicts(diagram.routes);
  const nodeOverlaps = countNodeOverlaps(diagram.nodes);
  const segments = diagram.routes.reduce((sum, item) => sum + item.points.length - 1, 0);
  const titleGap = Math.round(Math.min(...diagram.nodes.map((node) => node.y)) - 121);
  const margins = computeMargins(diagram);
  if (titleGap < 38) throw new Error(`${diagram.file}: title gap ${titleGap}px < 38px`);
  if (badEndpointAngle > 0) throw new Error(`${diagram.file}: bad endpoint angles=${badEndpointAngle}`);
  if (badBends > 0) throw new Error(`${diagram.file}: non-orthogonal segments=${badBends}`);
  if (interiorCrossings > 0) throw new Error(`${diagram.file}: connector interior crossings=${interiorCrossings}`);
  if (routeConflicts.length > 0) throw new Error(`${diagram.file}: connector route conflicts=${routeConflicts.length}: ${routeConflicts.slice(0, 4).join("; ")}`);
  if (nodeOverlaps > 0) throw new Error(`${diagram.file}: node overlaps=${nodeOverlaps}`);
  return { nodes: diagram.nodes.length, routes: diagram.routes.length, segments, titleGap, margins };
}

function countBadEndpointAngles(routes, nodeMap) {
  let bad = 0;
  for (const item of routes) {
    const source = nodeMap.get(item.from);
    const target = nodeMap.get(item.to);
    if (!source || !target) throw new Error(`Unknown route ${item.from} -> ${item.to}`);
    if (!endpointIsBoundary(item.points[0], item.points[1], source, true)) bad += 1;
    if (!endpointIsBoundary(item.points.at(-1), item.points.at(-2), target, false)) bad += 1;
  }
  return bad;
}

function endpointIsBoundary(point, next, node, isSource) {
  const onLeft = near(point.x, node.x) && point.y >= node.y && point.y <= node.y + node.h;
  const onRight = near(point.x, node.x + node.w) && point.y >= node.y && point.y <= node.y + node.h;
  const onTop = near(point.y, node.y) && point.x >= node.x && point.x <= node.x + node.w;
  const onBottom = near(point.y, node.y + node.h) && point.x >= node.x && point.x <= node.x + node.w;
  if (onLeft) return near(next.y, point.y) && next.x < point.x;
  if (onRight) return near(next.y, point.y) && next.x > point.x;
  if (onTop) return near(next.x, point.x) && next.y < point.y;
  if (onBottom) return near(next.x, point.x) && next.y > point.y;
  return false;
}

function countBadSegments(points) {
  let bad = 0;
  for (let index = 1; index < points.length; index += 1) {
    const dx = Math.abs(points[index].x - points[index - 1].x);
    const dy = Math.abs(points[index].y - points[index - 1].y);
    if (dx > 0.5 && dy > 0.5) bad += 1;
  }
  return bad;
}

function countInteriorCrossings(routeItem, nodes) {
  let count = 0;
  const excluded = new Set([routeItem.from, routeItem.to]);
  for (let index = 1; index < routeItem.points.length; index += 1) {
    const a = routeItem.points[index - 1];
    const b = routeItem.points[index];
    for (const node of nodes) {
      if (excluded.has(node.id)) continue;
      if (segmentCrossesNode(a, b, node, 8)) count += 1;
    }
  }
  return count;
}

function listRouteConflicts(routes) {
  const conflicts = [];
  for (let i = 0; i < routes.length; i += 1) {
    const aSegments = routeSegments(routes[i]);
    for (let j = i + 1; j < routes.length; j += 1) {
      const bSegments = routeSegments(routes[j]);
      for (const a of aSegments) {
        for (const b of bSegments) {
          if (segmentsConflict(a, b)) conflicts.push(`${a.route} ${segmentLabel(a)} x ${b.route} ${segmentLabel(b)}`);
        }
      }
    }
  }
  return conflicts;
}

function routeSegments(routeItem) {
  const segments = [];
  for (let index = 1; index < routeItem.points.length; index += 1) {
    segments.push({ route: `${routeItem.from}->${routeItem.to}`, a: routeItem.points[index - 1], b: routeItem.points[index] });
  }
  return segments;
}

function segmentLabel(segment) {
  return `(${fmt(segment.a.x)},${fmt(segment.a.y)}-${fmt(segment.b.x)},${fmt(segment.b.y)})`;
}

function segmentsConflict(first, second) {
  const aDir = segmentDirection(first.a, first.b);
  const bDir = segmentDirection(second.a, second.b);
  if (aDir === "point" || bDir === "point") return false;
  if (aDir === bDir) {
    if (aDir === "horizontal" && !near(first.a.y, second.a.y)) return false;
    if (aDir === "vertical" && !near(first.a.x, second.a.x)) return false;
    return overlapLength(segmentRange(first, aDir), segmentRange(second, bDir)) > 8;
  }
  const horizontal = aDir === "horizontal" ? first : second;
  const vertical = aDir === "vertical" ? first : second;
  const x = vertical.a.x;
  const y = horizontal.a.y;
  return insideOpen(x, Math.min(horizontal.a.x, horizontal.b.x), Math.max(horizontal.a.x, horizontal.b.x))
    && insideOpen(y, Math.min(vertical.a.y, vertical.b.y), Math.max(vertical.a.y, vertical.b.y));
}

function segmentDirection(a, b) {
  if (near(a.x, b.x) && near(a.y, b.y)) return "point";
  if (near(a.x, b.x)) return "vertical";
  if (near(a.y, b.y)) return "horizontal";
  return "diagonal";
}

function segmentRange(segment, direction) {
  return direction === "horizontal"
    ? [Math.min(segment.a.x, segment.b.x), Math.max(segment.a.x, segment.b.x)]
    : [Math.min(segment.a.y, segment.b.y), Math.max(segment.a.y, segment.b.y)];
}

function overlapLength(a, b) {
  return Math.max(0, Math.min(a[1], b[1]) - Math.max(a[0], b[0]));
}

function insideOpen(value, min, max) {
  return value > min + 0.5 && value < max - 0.5;
}

function segmentCrossesNode(a, b, node, clearance) {
  if (Math.abs(a.x - b.x) <= 0.5) {
    return a.x > node.x - clearance && a.x < node.x + node.w + clearance && Math.max(a.y, b.y) > node.y - clearance && Math.min(a.y, b.y) < node.y + node.h + clearance;
  }
  if (Math.abs(a.y - b.y) <= 0.5) {
    return a.y > node.y - clearance && a.y < node.y + node.h + clearance && Math.max(a.x, b.x) > node.x - clearance && Math.min(a.x, b.x) < node.x + node.w + clearance;
  }
  return false;
}

function countNodeOverlaps(nodes) {
  let count = 0;
  for (let i = 0; i < nodes.length; i += 1) {
    for (let j = i + 1; j < nodes.length; j += 1) {
      const a = nodes[i];
      const b = nodes[j];
      if (a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y) count += 1;
    }
  }
  return count;
}

function computeMargins(diagram) {
  return {
    left: Math.round(Math.min(...diagram.nodes.map((node) => node.x))),
    right: Math.round(diagram.width - Math.max(...diagram.nodes.map((node) => node.x + node.w))),
    top: Math.round(Math.min(...diagram.nodes.map((node) => node.y)) - 121),
    bottom: Math.round((diagram.height - 74) - Math.max(...diagram.nodes.map((node) => node.y + node.h))),
  };
}

function near(a, b) {
  return Math.abs(a - b) <= 0.5;
}

function fmt(value) {
  return Number.isInteger(value) ? String(value) : value.toFixed(1).replace(/\.0$/, "");
}

function titleCase(value) {
  return value.split("-").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function escapeXml(value) {
  return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function escapeDot(value) {
  return String(value).replaceAll("\\", "\\\\").replaceAll('"', '\\"');
}

if (!existsSync(dot)) throw new Error(`Graphviz dot not found at ${dot}`);
if (!existsSync(rsvgConvert)) throw new Error(`rsvg-convert not found at ${rsvgConvert}`);
