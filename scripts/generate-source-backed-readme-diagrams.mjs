#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { basename, dirname, join, relative } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";
const ONLY = new Set((process.env.DIAGRAM_ONLY ?? "").split(",").map((item) => item.trim()).filter(Boolean));
const GENERATOR_WITH_MODELS = join(ROOT, "scripts/generate-visual-audit-diagram-reworks.mjs");

const palette = {
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  purple: ["#FAF5FF", "#9333EA", "#7E22CE"],
  olive: ["#F7FEE7", "#65A30D", "#4D7C0F"],
  gray: ["#F9FAFB", "#6B7280", "#4B5563"],
};
const colorCycle = ["blue", "green", "teal", "amber", "pink", "purple", "olive", "gray"];

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function walkFiles(dir, predicate, limit = Number.POSITIVE_INFINITY, out = []) {
  if (!existsSync(dir) || out.length >= limit) return out;
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    if (out.length >= limit) break;
    if ([".git", "build", ".gradle", "node_modules", ".idea"].includes(entry.name)) continue;
    const path = join(dir, entry.name);
    if (entry.isDirectory()) walkFiles(path, predicate, limit, out);
    else if (predicate(path)) out.push(path);
  }
  return out;
}

function findReadmes() {
  return walkFiles(ROOT, (path) => /^README(?:\..+)?\.md$/.test(basename(path)), Number.POSITIVE_INFINITY)
    .filter((path) => !path.includes("/build/"));
}

function sourceModelNames() {
  if (!existsSync(GENERATOR_WITH_MODELS)) return new Set();
  const text = readFileSync(GENERATOR_WITH_MODELS, "utf8");
  return new Set([...text.matchAll(/^  "([^"]+)": \{/gm)].map((match) => match[1]));
}

function readmeDiagramRefs() {
  const refs = new Map();
  for (const readme of findReadmes()) {
    const text = readFileSync(readme, "utf8");
    const re = /!\[([^\]]*)\]\(([^)]*readme-diagrams\/([^)\s]+?\.png))\)/g;
    let match;
    while ((match = re.exec(text))) {
      const name = match[3].replace(/\.png$/, "");
      if (name.includes("sequence") || name.includes("chart") || name.startsWith("utils-states-")) continue;
      if (!refs.has(name)) refs.set(name, []);
      refs.get(name).push({
        readme,
        alt: match[1],
        path: match[2],
      });
    }
  }
  return refs;
}

function chooseEvidence(items) {
  return [...items].sort((a, b) => scoreReadme(a.readme) - scoreReadme(b.readme))[0];
}

function scoreReadme(path) {
  let score = 0;
  if (basename(path) === "README.md") score -= 20;
  if (path.includes("/docs/")) score += 10;
  score += path.split("/").length;
  return score;
}

function cleanTitle(value, fallback, readmeTitle = "") {
  const genericAlt = /^(?:uml|diagram|architecture|overview|component|class structure|state diagram)(?:\s+diagram)?$/i;
  const source = genericAlt.test(String(value || "").trim()) ? readmeTitle : value;
  const cleaned = String(source || fallback)
    .replace(/\s+diagram$/i, "")
    .replace(/\s+chart$/i, "")
    .replace(/^Module\s+/i, "")
    .replace(/\s+/g, " ")
    .trim();
  return cleaned || fallback;
}

function readReadmeFacts(readme) {
  const text = readFileSync(readme, "utf8");
  const mainTitle = text.match(/^#\s+(.+)/m)?.[1] ?? "";
  const headings = [];
  const bullets = [];
  for (const raw of text.split(/\r?\n/)) {
    const heading = raw.match(/^#{2,4}\s+(.+)/);
    if (heading) headings.push(sanitizeFact(heading[1]));
    const bullet = raw.match(/^\s*(?:[-*]|\d+\.)\s+(.+)/);
    if (bullet) bullets.push(sanitizeFact(bullet[1]));
  }
  return {
    title: sanitizeFact(mainTitle),
    headings: unique(headings).filter(Boolean).slice(0, 6),
    bullets: unique(bullets).filter(Boolean).slice(0, 10),
  };
}

function sanitizeFact(value) {
  return String(value)
    .replace(/!\[[^\]]*]\([^)]*\)/g, "")
    .replace(/\[[^\]]*]\([^)]*\)/g, (match) => match.replace(/^\[|\]\([^)]*\)$/g, ""))
    .replace(/[`*_#>|]/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, 74);
}

function unique(values) {
  return [...new Set(values.map((item) => item.trim()).filter(Boolean))];
}

function sourceFacts(moduleDir) {
  const srcMain = join(moduleDir, "src/main/kotlin");
  const ktFiles = walkFiles(srcMain, (path) => path.endsWith(".kt"), 240);
  const packages = new Map();
  const classes = [];
  for (const file of ktFiles) {
    const rel = relative(srcMain, file);
    const parts = rel.split("/");
    const packagePart = parts.includes("bluetape4k")
      ? parts.slice(parts.indexOf("bluetape4k") + 1, -1)[0]
      : parts.slice(0, -1)[0];
    if (packagePart) packages.set(packagePart, (packages.get(packagePart) ?? 0) + 1);
    const text = readFileSync(file, "utf8");
    for (const match of text.matchAll(/\b(?:class|interface|object|enum class|data class|sealed class)\s+([A-Z][A-Za-z0-9_]*)/g)) {
      classes.push(match[1]);
      if (classes.length >= 28) break;
    }
  }
  const topPackages = [...packages.entries()].sort((a, b) => b[1] - a[1]).map(([name, count]) => `${name} package (${count})`);
  const publicTypes = unique(classes).slice(0, 8);
  return {
    ktCount: ktFiles.length,
    topPackages: topPackages.slice(0, 8),
    publicTypes,
  };
}

function runtimeFacts(moduleDir, readmeText) {
  const buildFile = ["build.gradle.kts", "build.gradle"].map((name) => join(moduleDir, name)).find(existsSync);
  const combined = `${readmeText}\n${buildFile ? readFileSync(buildFile, "utf8") : ""}`;
  const facts = [];
  const probes = [
    ["Spring Boot", /spring-boot|springframework/i],
    ["Ktor runtime", /\bktor\b/i],
    ["Coroutine APIs", /coroutine|Flow<|suspend/i],
    ["Reactive bridge", /reactor|Mono<|Flux<|reactive/i],
    ["Redis backend", /redis|lettuce|redisson/i],
    ["Kafka messaging", /kafka|streams/i],
    ["Serialization", /jackson|json|protobuf|avro|csv|serializer/i],
    ["Metrics/tracing", /micrometer|opentelemetry|observation|prometheus/i],
    ["Test support", /junit|testcontainers|mock|assert/i],
    ["Virtual threads", /virtual\s*thread|jdk21|jdk25/i],
  ];
  for (const [label, regex] of probes) {
    if (regex.test(combined)) facts.push(label);
  }
  return unique(facts).slice(0, 5);
}

function wrapDetail(text, max = 34) {
  const words = String(text).split(/\s+/);
  const lines = [];
  let current = "";
  for (const word of words) {
    const next = current ? `${current} ${word}` : word;
    if (next.length > max && current) {
      lines.push(current);
      current = word;
    } else {
      current = next;
    }
  }
  if (current) lines.push(current);
  return lines.slice(0, 2);
}

function card(id, x, y, w, h, title, details, color) {
  const [fill, stroke] = palette[color] ?? palette.gray;
  const detailLines = details.flatMap((line) => wrapDetail(line, Math.max(22, Math.floor(w / 10)))).slice(0, 3);
  const startY = y + 34;
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="cardTitle" x="${x + w / 2}" y="${startY}" text-anchor="middle">${esc(title)}</text>
${detailLines.map((line, index) => `  <text class="detail" x="${x + w / 2}" y="${startY + 28 + index * 19}" text-anchor="middle">${esc(line)}</text>`).join("\n")}
</g>`;
}

function panel(x, y, w, h, title) {
  return `<g><rect class="panel" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/><text class="panelTitle" x="${x + 28}" y="${y + 38}">${esc(title)}</text></g>`;
}

function route(fromId, toId, x1, y1, x2, y2, color) {
  const stroke = palette[color]?.[2] ?? palette.gray[2];
  const midY = Math.round((y1 + y2) / 2);
  return `<path class="route" data-from="${esc(fromId)}" data-to="${esc(toId)}" d="M${x1} ${y1} L${x1} ${midY} L${x2} ${midY} L${x2} ${y2}" stroke="${stroke}"/>`;
}

function svgBase(width, height, title, subtitle, body, model) {
  const evidence = model.evidence.join("; ");
  return `<svg data-intent="${esc(model.intent)}" data-evidence="${esc(evidence)}" data-source-read="${esc(evidence)}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title)}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  <marker id="arrow" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="context-stroke"/></marker>
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .panel{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5}.panelTitle{font-family:"Architects Daughter";font-size:24px;fill:#0F172A;paint-order:stroke;stroke:#fff;stroke-width:4px;stroke-linejoin:round}
    .card{filter:url(#shadow);stroke-width:1.7}.cardTitle{font-family:"Architects Daughter";font-size:22px;fill:#0F172A}.detail{font-family:"Comic Mono";font-size:13px;fill:#475569}
    .route{fill:none;stroke-width:2.8;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="84">${esc(title)}</text>
<text class="subtitle" x="76" y="116">${esc(subtitle)}</text>
${body}
</svg>
`;
}

function fallbackItems(items, fallback) {
  return items.length > 0 ? items : fallback;
}

function buildCards(items, prefix, y, colors, titleFactory = (value) => value) {
  const width = 300;
  const gap = 38;
  const total = items.length * width + (items.length - 1) * gap;
  const start = Math.round((1500 - total) / 2);
  return items.map((item, index) => ({
    id: `${prefix}${index}`,
    x: start + index * (width + gap),
    y,
    w: width,
    h: 92,
    title: titleFactory(item, index),
    details: [item],
    color: colors[index % colors.length],
  }));
}

function renderSourceBackedDiagram(name, evidence) {
  const readme = evidence.readme;
  const moduleDir = dirname(readme);
  const readmeText = readFileSync(readme, "utf8");
  const readmeFacts = readReadmeFacts(readme);
  const title = cleanTitle(evidence.alt, name.replaceAll("-", " "), readmeFacts.title);
  const src = sourceFacts(moduleDir);
  const runtime = runtimeFacts(moduleDir, readmeText);
  const moduleLabel = relative(ROOT, moduleDir) || "root";

  const contractItems = fallbackItems(
    [...readmeFacts.headings, ...readmeFacts.bullets].filter((item) => !/diagram|chart|license|language/i.test(item)).slice(0, 4),
    [title, `${moduleLabel} README`, "public usage contract"],
  ).slice(0, 4);
  const sourceItems = fallbackItems(
    [...src.topPackages, ...src.publicTypes.map((item) => `${item} type`)].slice(0, 4),
    [`${src.ktCount} Kotlin source files`, "module public source", "README-backed API surface"],
  ).slice(0, 4);
  const runtimeItems = fallbackItems(runtime, ["Public API boundary", "Configuration surface", "Test/readme usage path"]).slice(0, 4);

  const contractCards = buildCards(contractItems, "contract", 230, ["blue", "green", "teal", "amber"], (value, index) => index === 0 ? "README contract" : "Reader feature");
  const sourceCards = buildCards(sourceItems, "source", 510, ["purple", "teal", "pink", "olive"], (value) => value.replace(/\s*\(\d+\)$/, "").replace(/\s+package.+$/, "Source package"));
  const runtimeCards = buildCards(runtimeItems, "runtime", 790, ["green", "amber", "purple", "pink"], (value) => value);
  const cards = [...contractCards, ...sourceCards, ...runtimeCards];

  const body = [
    panel(82, 160, 1336, 190, "README-derived contract"),
    panel(82, 440, 1336, 190, "Current source surface"),
    panel(82, 720, 1336, 190, "Runtime or usage boundary"),
    ...cards.map((item) => card(item.id, item.x, item.y, item.w, item.h, item.title, item.details, item.color)),
  ];

  for (let index = 0; index < Math.min(contractCards.length, sourceCards.length); index++) {
    const from = contractCards[index];
    const to = sourceCards[index];
    body.push(route(from.id, to.id, from.x + from.w / 2, from.y + from.h, to.x + to.w / 2, to.y, from.color));
  }
  for (let index = 0; index < Math.min(sourceCards.length, runtimeCards.length); index++) {
    const from = sourceCards[index];
    const to = runtimeCards[index];
    body.push(route(from.id, to.id, from.x + from.w / 2, from.y + from.h, to.x + to.w / 2, to.y, from.color));
  }

  const model = {
    intent: `Regenerate ${title} from the current README and source package surface for ${moduleLabel}.`,
    evidence: [relative(ROOT, readme), relative(ROOT, moduleDir), `${relative(ROOT, moduleDir)}/src/main/kotlin`],
  };
  return svgBase(1500, 1000, title, `${moduleLabel}: README headings plus current Kotlin source surface`, body.join("\n"), model);
}

function main() {
  const refs = readmeDiagramRefs();
  const covered = sourceModelNames();
  const includeModeled = process.env.INCLUDE_MODELED === "1";
  const targets = [...refs.keys()].filter((name) => includeModeled || !covered.has(name)).sort();
  const selected = targets.filter((name) => ONLY.size === 0 || ONLY.has(name));
  for (const name of selected) {
    const evidence = chooseEvidence(refs.get(name));
    const svg = renderSourceBackedDiagram(name, evidence);
    const svgPath = join(OUT, `${name}.svg`);
    const pngPath = join(OUT, `${name}.png`);
    writeFileSync(svgPath, svg);
    execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
  }
  console.log(`Regenerated ${selected.length} source-backed README diagrams.`);
}

main();
