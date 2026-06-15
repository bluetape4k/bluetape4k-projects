#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";
const evidence = ["README.md", "settings.gradle.kts"];

for (const file of evidence) {
  if (!existsSync(join(ROOT, file))) throw new Error(`Missing evidence: ${file}`);
}

const settings = readFileSync(join(ROOT, "settings.gradle.kts"), "utf8");
for (const includeRoot of ["bluetape4k", "cache", "data", "infra", "io", "ktor", "spring-boot", "testing", "utils", "virtualthread"]) {
  if (!settings.includes(`includeModules("${includeRoot}"`)) {
    throw new Error(`settings.gradle.kts no longer includes ${includeRoot}`);
  }
}

function modules(root) {
  const dir = join(ROOT, root);
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .filter((entry) => existsSync(join(dir, entry.name, "build.gradle.kts")))
    .map((entry) => entry.name)
    .sort();
}

const roots = {
  "bluetape4k/": modules("bluetape4k"),
  "io/": modules("io"),
  "data/": modules("data"),
  "infra/": modules("infra"),
  "cache/": modules("cache"),
  "spring-boot/": modules("spring-boot"),
  "ktor/": modules("ktor"),
  "testing/": modules("testing"),
  "utils/": modules("utils"),
  "virtualthread/": modules("virtualthread"),
};
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

function markerDefs() {
  return Object.entries(palette)
    .map(([name, [, , dark]]) => `<marker id="arrow-${name}" markerWidth="6" markerHeight="6" refX="5.4" refY="3" orient="auto" markerUnits="userSpaceOnUse"><path d="M .7 .7 L 5.4 3 L .7 5.3 Z" fill="${dark}"/></marker>`)
    .join("\n");
}

function icon(x, y, color, kind) {
  const glyphs = {
    folder: `<path d="M12 18h10l3 4h11v15H12z"/><path d="M12 22h24"/>`,
    gradle: `<path d="M14 29c3-11 13-16 23-11-1 5-5 8-11 8h-6"/><path d="M14 29c-2 5 1 9 6 9h11"/><circle cx="19" cy="33" r="1.5"/><circle cx="29" cy="33" r="1.5"/>`,
    publish: `<path d="M24 36V14M17 21l7-7 7 7"/><path d="M14 36h20"/>`,
    app: `<rect x="13" y="13" width="22" height="22" rx="4"/><path d="M18 20h12M18 27h8"/>`,
    split: `<circle cx="16" cy="24" r="5"/><circle cx="32" cy="16" r="5"/><circle cx="32" cy="32" r="5"/><path d="M20 22l8-4M20 26l8 4"/>`,
  };
  return `<g class="icon" transform="translate(${x},${y})"><rect width="46" height="46" rx="10" fill="${color}" stroke="${color}"/><g fill="none" stroke="#FFFFFF" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">${glyphs[kind]}</g></g>`;
}

function compactItems(items, visible = 6) {
  const shown = items.slice(0, visible);
  const remaining = items.length - shown.length;
  return remaining > 0 ? [...shown, `+${remaining} more`] : shown;
}

function wrapModules(items, maxChars = 56, maxLines = 3) {
  const lines = [];
  let current = "";
  for (const item of items) {
    const next = current ? `${current}, ${item}` : item;
    if (current && next.length > maxChars) {
      lines.push(current);
      current = item;
    } else {
      current = next;
    }
  }
  if (current) {
    lines.push(current);
  }
  return lines.slice(0, maxLines);
}

function rootCard({ id, x, y, w, h, color, kind = "folder", title, subtitle, items, badge }) {
  const [fill, stroke] = palette[color];
  const compactVisible = h <= 140 ? 5 : w > 700 ? 9 : 7;
  const maxLines = h <= 150 ? 2 : h <= 180 ? 3 : 4;
  const maxChars = Math.max(26, Math.floor((w - 60) / 8.6));
  const lines = wrapModules(compactItems(items, compactVisible), maxChars, maxLines);
  return `<g id="${esc(id)}" class="rootCard">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  ${icon(x + 20, y + 20, stroke, kind)}
  <text class="cardTitle" x="${x + 82}" y="${y + 42}">${esc(title)}</text>
  <text class="badge" x="${x + w - 24}" y="${y + 42}" text-anchor="end">${esc(badge)}</text>
  <text class="subtitle2" x="${x + 82}" y="${y + 68}">${esc(subtitle)}</text>
  ${lines.map((line, index) => `<text class="moduleLine" x="${x + 28}" y="${y + 106 + index * 24}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function band(x, y, w, h, title, detail) {
  return `<g><rect class="band" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/><text class="bandTitle" x="${x + 26}" y="${y + 38}">${esc(title)}</text><text class="bandDetail" x="${x + 26}" y="${y + 64}">${esc(detail)}</text></g>`;
}

function route(from, to, points, color) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  return `<path class="route" data-from="${esc(from)}" data-to="${esc(to)}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>`;
}

const width = 3000;
const height = 1400;
const body = [
  `<rect class="canvas" width="${width}" height="${height}"/>`,
  `<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>`,
  `<text class="title" x="72" y="86">Repository Module Structure</text>`,
  `<text class="subtitle" x="76" y="120">Gradle include roots from settings.gradle.kts, grouped for README navigation.</text>`,
  band(82, 160, 2836, 350, "Published library roots", "Libraries released from this repository; choose by domain and import through the BOM/catalog."),
  band(82, 545, 2836, 470, "Runtime, utility, and test roots", "Framework adapters, virtual-thread runtime selection, testing support, and utility modules."),
  band(82, 1050, 2836, 175, "Moved to sibling repositories", "README keeps these ecosystem links visible while this repo stays focused on shared JVM libraries."),
  rootCard({ id: "foundation", x: 125, y: 240, w: 600, h: 190, color: "blue", kind: "publish", title: "bluetape4k/", subtitle: "foundation + BOM", items: roots["bluetape4k/"], badge: `${roots["bluetape4k/"].length}` }),
  rootCard({ id: "io", x: 780, y: 240, w: 760, h: 190, color: "teal", title: "io/", subtitle: "serialization, codecs, clients", items: roots["io/"], badge: `${roots["io/"].length}` }),
  rootCard({ id: "data", x: 1595, y: 240, w: 530, h: 190, color: "purple", title: "data/", subtitle: "database adapters", items: roots["data/"], badge: `${roots["data/"].length}` }),
  rootCard({ id: "cache", x: 2180, y: 240, w: 630, h: 190, color: "pink", title: "cache/", subtitle: "cache APIs/backends", items: roots["cache/"], badge: `${roots["cache/"].length}` }),
  rootCard({ id: "infra", x: 125, y: 625, w: 760, h: 175, color: "olive", title: "infra/", subtitle: "messaging, Redis, telemetry", items: roots["infra/"], badge: `${roots["infra/"].length}` }),
  rootCard({ id: "spring", x: 940, y: 625, w: 600, h: 175, color: "green", kind: "app", title: "spring-boot/", subtitle: "Spring Boot 4 line", items: roots["spring-boot/"], badge: `${roots["spring-boot/"].length}` }),
  rootCard({ id: "ktor", x: 1595, y: 625, w: 520, h: 175, color: "blue", kind: "app", title: "ktor/", subtitle: "Ktor 3 server helpers", items: roots["ktor/"], badge: `${roots["ktor/"].length}` }),
  rootCard({ id: "testing", x: 2170, y: 625, w: 620, h: 175, color: "amber", kind: "gradle", title: "testing/", subtitle: "test support", items: roots["testing/"], badge: `${roots["testing/"].length}` }),
  rootCard({ id: "utils", x: 245, y: 825, w: 860, h: 180, color: "amber", title: "utils/", subtitle: "domain utilities", items: roots["utils/"], badge: `${roots["utils/"].length}` }),
  rootCard({ id: "vt", x: 1160, y: 825, w: 620, h: 160, color: "purple", kind: "app", title: "virtualthread/", subtitle: "API + JDK adapters", items: roots["virtualthread/"], badge: `${roots["virtualthread/"].length}` }),
  rootCard({ id: "siblings", x: 420, y: 1125, w: 2140, h: 72, color: "gray", kind: "split", title: "standalone repos", subtitle: "published outside this Gradle build", items: [], badge: "external" }),
];

const svg = `<svg data-intent="Recreate the root README Module Structure diagram as a Gradle include-root map, distinct from the overview diagram." data-evidence="${esc(evidence.join("; "))}; module directories counted from build.gradle.kts files" data-source-read="${esc(evidence.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Repository Module Structure">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .band{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5}.bandTitle{font-family:"Architects Daughter";font-size:26px;fill:#0F172A}.bandDetail{font-family:"Comic Mono";font-size:14px;fill:#64748B}
    .card{filter:url(#shadow);stroke-width:1.8}.cardTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}.badge{font-family:"Comic Mono";font-size:15px;fill:#475569}.subtitle2{font-family:"Comic Mono";font-size:14px;fill:#64748B}.moduleLine{font-family:"Comic Mono";font-size:14px;fill:#334155}
    .route{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}
  </style>
</defs>
${body.join("\n")}
</svg>
`;

const svgPath = join(OUT, "root-readme-en-diagram-01.svg");
const pngPath = join(OUT, "root-readme-en-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated root-readme-en-diagram-01.svg/png");
