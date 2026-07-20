#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const evidence = ["README.md", "README.ko.md", "settings.gradle.kts"];
for (const file of evidence) {
  if (!existsSync(join(ROOT, file))) throw new Error(`Missing evidence: ${file}`);
}

function moduleNames(group) {
  const dir = join(ROOT, group);
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .filter((entry) => existsSync(join(dir, entry.name, "build.gradle.kts")))
    .map((entry) => entry.name)
    .sort();
}

const groups = {
  foundation: moduleNames("bluetape4k"),
  io: moduleNames("io"),
  data: moduleNames("data"),
  infra: moduleNames("infra"),
  cache: moduleNames("cache"),
  spring: moduleNames("spring-boot"),
  ktor: moduleNames("ktor"),
  testing: moduleNames("testing"),
  utils: moduleNames("utils"),
  virtualthread: moduleNames("virtualthread"),
};

const readme = readFileSync(join(ROOT, "README.md"), "utf8");
const readmeEvidenceChecks = [
  ["Kotlin 2.3", /\*\*Kotlin\*\*:\s*2\.3/],
  ["Java toolchain", /\*\*Java\*\*:\s*21/],
  ["Spring Boot 4", /Spring Boot\*\*:\s*4\.x/],
  ["split-repository ecosystem modules", /Split-repository ecosystem modules/],
];
for (const [label, pattern] of readmeEvidenceChecks) {
  if (!pattern.test(readme)) throw new Error(`README evidence missing: ${label}`);
}

const palette = {
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  purple: ["#FAF5FF", "#9333EA", "#7E22CE"],
  olive: ["#F7FEE7", "#65A30D", "#4D7C0F"],
  gray: ["#F8FAFC", "#64748B", "#475569"],
};

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function wrap(text, max = 34) {
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
  return lines;
}

function markerDefs() {
  return Object.entries(palette)
    .map(([name, [, , dark]]) => `<marker id="arrow-${name}" markerWidth="14" markerHeight="14" refX="13" refY="7" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 13 7 L 1 13 Z" fill="${dark}"/></marker>`)
    .join("\n");
}

function icon(x, y, color, kind) {
  const glyphs = {
    core: `<path d="M18 14h12l6 10-6 10H18l-6-10z"/><path d="M19 24h10"/>`,
    io: `<path d="M15 17h18v14H15z"/><path d="M12 24h6M30 24h6M20 17v-5M28 17v-5M20 36v-5M28 36v-5"/>`,
    data: `<ellipse cx="24" cy="15" rx="11" ry="5"/><path d="M13 15v18c0 3 5 5 11 5s11-2 11-5V15"/><path d="M13 24c0 3 5 5 11 5s11-2 11-5"/>`,
    app: `<rect x="13" y="13" width="22" height="22" rx="4"/><path d="M18 20h12M18 27h8"/>`,
    test: `<path d="M21 11v10l-7 12a4 4 0 0 0 3 6h14a4 4 0 0 0 3-6l-7-12V11"/><path d="M18 11h12M18 31h12"/>`,
    split: `<circle cx="16" cy="24" r="5"/><circle cx="32" cy="16" r="5"/><circle cx="32" cy="32" r="5"/><path d="M20 22l8-4M20 26l8 4"/>`,
  };
  return `<g class="icon" transform="translate(${x},${y})"><rect width="48" height="48" rx="10" fill="${color}" stroke="${color}"/><g fill="none" stroke="#FFFFFF" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">${glyphs[kind]}</g></g>`;
}

function card({ id, x, y, w, h, color, iconKind, title, count, details }) {
  const [fill, stroke] = palette[color];
  const lines = details.flatMap((line) => wrap(line, Math.floor((w - 52) / 9.3))).slice(0, 4);
  return `<g id="${esc(id)}" class="cardGroup">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  ${icon(x + 22, y + 22, stroke, iconKind)}
  <text class="cardTitle" x="${x + 84}" y="${y + 42}">${esc(title)}</text>
  <text class="count" x="${x + w - 32}" y="${y + 42}" text-anchor="end">${esc(count)}</text>
  ${lines.map((line, index) => `<text class="detail" x="${x + 28}" y="${y + 88 + index * 22}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function route(from, to, d, color) {
  const [, , dark] = palette[color];
  return `<path class="route" data-from="${esc(from)}" data-to="${esc(to)}" aria-label="${esc(from)} to ${esc(to)}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>`;
}

function pill(x, y, label, color) {
  const [fill, stroke] = palette[color];
  return `<g><rect class="tag" x="${x}" y="${y}" width="130" height="46" rx="8" fill="${fill}" stroke="${stroke}"/><text class="tagText" x="${x + 65}" y="${y + 29}" text-anchor="middle">${esc(label)}</text></g>`;
}

const width = 2200;
const height = 1360;
const center = { x: 850, y: 470, w: 500, h: 300 };

const cards = [
  card({
    id: "foundation",
    x: 140,
    y: 275,
    w: 500,
    h: 190,
    color: "blue",
    iconKind: "core",
    title: "Foundation",
    count: `${groups.foundation.length} modules`,
    details: ["annotations, bom, core, coroutines, logging", "contracts, Kotlin utilities, diagnostics, version alignment"],
  }),
  card({
    id: "dataCache",
    x: 140,
    y: 795,
    w: 500,
    h: 210,
    color: "pink",
    iconKind: "data",
    title: "Data and cache",
    count: `${groups.data.length + groups.cache.length} modules`,
    details: ["JDBC, R2DBC, MongoDB, Cassandra, Hibernate", "JCache, NearCache, Hazelcast, Lettuce, Redisson"],
  }),
  card({
    id: "ioInfra",
    x: 1560,
    y: 275,
    w: 500,
    h: 220,
    color: "teal",
    iconKind: "io",
    title: "I/O + infra",
    count: `${groups.io.length + groups.infra.length} modules`,
    details: ["HTTP, Feign, gRPC, JSON, Netty, Okio, Protobuf", "Redis, Kafka, OTel, Micrometer, NATS, Pulsar, Resilience4j"],
  }),
  card({
    id: "apps",
    x: 1560,
    y: 770,
    w: 500,
    h: 210,
    color: "green",
    iconKind: "app",
    title: "App runtimes",
    count: `${groups.spring.length + groups.ktor.length + groups.virtualthread.length} modules`,
    details: ["Spring Boot 4 integrations", "Ktor 3 server helpers", "Virtual thread API, JDK 21 and JDK 25 adapters"],
  }),
  card({
    id: "testing",
    x: 570,
    y: 1020,
    w: 430,
    h: 170,
    color: "amber",
    iconKind: "test",
    title: "Testing",
    count: `${groups.testing.length} modules`,
    details: ["assertions, JUnit5 extensions", "mock web servers, Testcontainers"],
  }),
  card({
    id: "utilities",
    x: 1110,
    y: 1020,
    w: 430,
    h: 170,
    color: "purple",
    iconKind: "core",
    title: "Utilities",
    count: `${groups.utils.length} modules`,
    details: ["geo, id generators, time, math, money", "rules, state machines, workflows"],
  }),
  card({
    id: "splitRepos",
    x: 690,
    y: 210,
    w: 820,
    h: 120,
    color: "gray",
    iconKind: "split",
    title: "Standalone ecosystem repos",
    count: "moved out",
    details: ["AWS, image, text, leader election, JaVers, Exposed, graph"],
  }),
];

const body = [
  `<rect class="canvas" width="${width}" height="${height}"/>`,
  `<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>`,
  `<text class="title" x="72" y="84">Bluetape4k Projects Overview</text>`,
  `<text class="subtitle" x="76" y="118">A Kotlin/JVM backend toolkit repo: foundation libraries, data/cache, I/O/infra, app runtimes, utilities, and test support.</text>`,
  `<g id="repo" class="cardGroup">`,
  `<rect class="card focus" x="${center.x}" y="${center.y}" width="${center.w}" height="${center.h}" rx="8"/>`,
  icon(center.x + 36, center.y + 34, palette.olive[1], "core"),
  `<text class="focusTitle" x="${center.x + 110}" y="${center.y + 66}">This repository</text>`,
  `<text class="focusName" x="${center.x + 110}" y="${center.y + 108}">bluetape4k-projects</text>`,
  `<text class="focusDetail" x="${center.x + 42}" y="${center.y + 154}">Shared Kotlin/JVM libraries published for reuse.</text>`,
  `<text class="focusDetail" x="${center.x + 42}" y="${center.y + 184}">Use the BOM/catalog for version alignment.</text>`,
  `<text class="focusDetail" x="${center.x + 42}" y="${center.y + 212}">Pick only the module families needed by the application.</text>`,
  pill(center.x + 44, center.y + 226, "Kotlin 2.3", "purple"),
  pill(center.x + 184, center.y + 226, "Java 21", "amber"),
  pill(center.x + 324, center.y + 226, "Spring Boot 4", "green"),
  `</g>`,
  ...cards,
  route("foundation", "repo", "M 640 410 L 731 410 Q 745 410 745 424 L 745 546 Q 745 560 759 560 L 850 560", "blue"),
  route("dataCache", "repo", "M 640 900 L 731 900 Q 745 900 745 886 L 745 664 Q 745 650 759 650 L 850 650", "pink"),
  route("ioInfra", "repo", "M 1560 415 L 1469 415 Q 1455 415 1455 429 L 1455 546 Q 1455 560 1441 560 L 1350 560", "teal"),
  route("apps", "repo", "M 1560 875 L 1469 875 Q 1455 875 1455 861 L 1455 664 Q 1455 650 1441 650 L 1350 650", "green"),
  route("repo", "testing", "M 990 770 L 990 881 Q 990 895 976 895 L 799 895 Q 785 895 785 909 L 785 1020", "amber"),
  route("repo", "utilities", "M 1210 770 L 1210 881 Q 1210 895 1224 895 L 1311 895 Q 1325 895 1325 909 L 1325 1020", "purple"),
  route("splitRepos", "repo", "M 1100 330 L 1100 470", "gray"),
  `<g class="legend" transform="translate(102,1220)">
  <text class="legendTitle" x="0" y="0">How to read this overview</text>
  <text class="legendText" x="0" y="32">Center is the repository boundary. Side cards are module families currently included by settings.gradle.kts. Top card is the split-repo ecosystem kept out of this repo.</text>
</g>`,
];

const svg = `<svg data-intent="Recreate the root README overview as a repository boundary and module-family map, not a uniform layered inventory." data-evidence="${esc(evidence.join("; "))}; module directories counted from settings.gradle.kts groups" data-source-read="${esc(evidence.join("; "))}" data-layout="responsibility-map" data-allow-grid="true" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Bluetape4k Projects Overview">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .focus{fill:#F7FEE7;stroke:#65A30D;stroke-width:2.2;filter:url(#shadow)}.focusTitle{font-family:"Comic Mono";font-size:17px;fill:#475569}.focusName{font-family:"Architects Daughter";font-size:34px;fill:#0F172A}.focusDetail{font-family:"Comic Mono";font-size:15px;fill:#334155}
    .card{filter:url(#shadow);stroke-width:1.8}.cardTitle{font-family:"Architects Daughter";font-size:29px;fill:#0F172A}.count{font-family:"Comic Mono";font-size:15px;fill:#475569}.detail{font-family:"Comic Mono";font-size:15px;fill:#334155}
    .tag{stroke-width:1.5}.tagText{font-family:"Comic Mono";font-size:14px;fill:#334155}.route{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}
    .legendTitle{font-family:"Architects Daughter";font-size:25px;fill:#0F172A}.legendText{font-family:"Comic Mono";font-size:15px;fill:#475569}
  </style>
</defs>
${body.join("\n")}
</svg>
`;

const svgPath = join(OUT, "root-readme-overview-01.svg");
const pngPath = join(OUT, "root-readme-overview-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated root-readme-overview-01.svg/png");
