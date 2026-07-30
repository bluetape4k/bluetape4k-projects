#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-math-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/utils-math-diagram-01.png";
const W = 2060;
const H = 1280;
const colors = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  blue: "#2563EB",
  green: "#16A34A",
  teal: "#0D9488",
  orange: "#EA580C",
  purple: "#7C3AED",
  pink: "#DB2777",
  amber: "#D97706",
  gray: "#64748B",
};

const evidence = [
  "utils/math/README.md",
  "utils/math/src/main/kotlin/io/bluetape4k/math/Descriptives.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/DoubleHistogram.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/RandomSupport.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/interpolation/Interpolator.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/integration/Integrator.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/equation/Equator.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/special/SpecialFunctions.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/linear/MatrixSupport.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/ml/clustering/Clustering.kt",
];

for (const file of evidence) {
  if (!existsSync(file)) throw new Error(`Missing source evidence: ${file}`);
}

const readme = readFileSync("utils/math/README.md", "utf8");
if (!/Feature Structure[\s\S]*utils-math-diagram-01\.png/.test(readme)) {
  throw new Error("README Feature Structure slot not found");
}

const cards = {
  api: { x: 115, y: 170, w: 1785, h: 175, fill: "#F8FAFC", stroke: colors.gray, icon: "api", title: "bluetape4k-math", kicker: "Kotlin helper module", lines: ["Kotlin extension functions and small wrappers", "built on Apache Commons Math3", "groups numerical APIs by reader task"] },
  stats: { x: 115, y: 510, w: 390, h: 190, fill: "#EFF6FF", stroke: colors.blue, icon: "stats", title: "Statistics", kicker: "core package", lines: ["Descriptives", "Double/BigDecimal statistics", "histograms, rank, correlation"] },
  random: { x: 565, y: 510, w: 390, h: 190, fill: "#F0FDF4", stroke: colors.green, icon: "random", title: "Sampling", kicker: "RandomSupport.kt", lines: ["randomFirst / random", "randomDistinct", "weighted coin and dice"] },
  calculus: { x: 1015, y: 510, w: 430, h: 190, fill: "#F0FDFA", stroke: colors.teal, icon: "curve", title: "Curves and Area", kicker: "interpolation + integration", lines: ["Linear, Spline, Loess, Akima", "Romberg, Simpson, Trapezoid", "MidPoint integrator"] },
  solving: { x: 1510, y: 510, w: 390, h: 190, fill: "#FFF7ED", stroke: colors.orange, icon: "root", title: "Equation Solving", kicker: "equation package", lines: ["Bisection, Brent, Secant", "Pegasus, Ridders", "Regula Falsi"] },
  special: { x: 115, y: 820, w: 390, h: 190, fill: "#FAF5FF", stroke: colors.purple, icon: "sigma", title: "Special Functions", kicker: "special + commons", lines: ["Gamma, Beta, Factorial", "Harmonic", "primes, combinations"] },
  algebra: { x: 565, y: 820, w: 390, h: 190, fill: "#FDF2F8", stroke: colors.pink, icon: "matrix", title: "Linear Algebra", kicker: "linear package", lines: ["matrix/vector helpers", "field and real variants", "Commons Math bridge"] },
  geometry: { x: 1015, y: 820, w: 430, h: 190, fill: "#FFFBEB", stroke: colors.amber, icon: "geometry", title: "Geometry and Transform", kicker: "geometry + transform", lines: ["1D/2D/3D vectors", "spherical geometry", "transform helpers"] },
  ml: { x: 1510, y: 820, w: 390, h: 190, fill: "#F8FAFC", stroke: colors.gray, icon: "cluster", title: "ML Utilities", kicker: "ml package", lines: ["K-Means clustering", "DoublePoint helpers", "distance measures"] },
  foundation: { x: 345, y: 1068, w: 1330, h: 124, fill: "#F8FAFC", stroke: colors.gray, icon: "foundation", title: "Apache Commons Math3 Foundation", kicker: "external dependency", lines: ["the module mostly exposes Kotlin-friendly entry points over stable Commons Math primitives"] },
};

const edges = [
  { id: "apiStats", color: colors.blue, from: "api", to: "stats", d: "M310 345 L310 510", label: { x: 404, y: 421, text: "descriptive data", w: 136 } },
  { id: "apiRandom", color: colors.green, from: "api", to: "random", d: "M760 345 L760 510", label: { x: 840, y: 421, text: "sampling", w: 90 } },
  { id: "apiCalculus", color: colors.teal, from: "api", to: "calculus", d: "M1230 345 L1230 510", label: { x: 1330, y: 421, text: "numeric methods", w: 136 } },
  { id: "apiSolving", color: colors.orange, from: "api", to: "solving", d: "M1705 345 L1705 510", label: { x: 1794, y: 421, text: "root finding", w: 112 } },
  { id: "statsSpecial", color: colors.purple, from: "stats", to: "special", d: "M310 700 L310 820", label: { x: 394, y: 763, text: "functions", w: 92 } },
  { id: "randomAlgebra", color: colors.pink, from: "random", to: "algebra", d: "M760 700 L760 820", label: { x: 844, y: 763, text: "data vectors", w: 104 } },
  { id: "calculusGeometry", color: colors.amber, from: "calculus", to: "geometry", d: "M1230 700 L1230 820", label: { x: 1324, y: 763, text: "coordinates", w: 108 } },
  { id: "solvingMl", color: colors.gray, from: "solving", to: "ml", d: "M1705 700 L1705 820", label: { x: 1788, y: 763, text: "distance", w: 82 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="22" markerHeight="18" refX="20" refY="9" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 22 18"><path d="M2 2 L20 9 L2 16 Z" fill="${color}"/></marker>`;
}

function icon(type, x, y, color) {
  const cx = x + 42;
  const cy = y + 48;
  const base = `<rect x="${x + 20}" y="${y + 26}" width="50" height="50" rx="12" fill="${color}"/>`;
  if (type === "stats") return `${base}<path d="M${cx - 15} ${cy + 13} V${cy - 2} M${cx} ${cy + 13} V${cy - 14} M${cx + 15} ${cy + 13} V${cy - 7}" stroke="#fff" stroke-width="4" stroke-linecap="round"/>`;
  if (type === "random") return `${base}<rect x="${cx - 14}" y="${cy - 14}" width="28" height="28" rx="5" fill="none" stroke="#fff" stroke-width="3"/><circle cx="${cx - 7}" cy="${cy - 7}" r="2.6" fill="#fff"/><circle cx="${cx + 7}" cy="${cy}" r="2.6" fill="#fff"/><circle cx="${cx - 7}" cy="${cy + 7}" r="2.6" fill="#fff"/>`;
  if (type === "curve") return `${base}<path d="M${cx - 16} ${cy + 12} C${cx - 5} ${cy - 18}, ${cx + 4} ${cy + 18}, ${cx + 16} ${cy - 12}" fill="none" stroke="#fff" stroke-width="3.5" stroke-linecap="round"/>`;
  if (type === "root") return `${base}<path d="M ${cx - 16} ${cy + 7} L ${cx - 9} ${cy + 7} Q ${cx - 5} ${cy + 7} ${cx - 3.8} ${cy + 3.19} L ${cx + 1} ${cy - 12} L ${cx + 6.88} ${cy + 8.16} Q ${cx + 8} ${cy + 12} ${cx + 12} ${cy + 12} L ${cx + 17} ${cy + 12}" fill="none" stroke="#fff" stroke-width="3.3" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (type === "sigma") return `${base}<path d="M${cx + 13} ${cy - 15} H${cx - 13} L${cx + 2} ${cy} L${cx - 13} ${cy + 15} H${cx + 13}" fill="none" stroke="#fff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (type === "matrix") return `${base}<path d="M${cx - 16} ${cy - 14} H${cx + 16} M${cx - 16} ${cy} H${cx + 16} M${cx - 16} ${cy + 14} H${cx + 16} M${cx - 6} ${cy - 16} V${cy + 16} M${cx + 7} ${cy - 16} V${cy + 16}" stroke="#fff" stroke-width="2.5"/>`;
  if (type === "geometry") return `${base}<path d="M${cx - 15} ${cy + 13} L${cx} ${cy - 15} L${cx + 15} ${cy + 13} Z" fill="none" stroke="#fff" stroke-width="3.4" stroke-linejoin="round"/><circle cx="${cx}" cy="${cy + 4}" r="4" fill="#fff"/>`;
  if (type === "cluster") return `${base}<circle cx="${cx - 10}" cy="${cy - 5}" r="6" fill="#fff"/><circle cx="${cx + 9}" cy="${cy - 10}" r="6" fill="#fff"/><circle cx="${cx + 3}" cy="${cy + 11}" r="6" fill="#fff"/><path d="M${cx - 5} ${cy - 7} L${cx + 4} ${cy - 9} M${cx - 6} ${cy} L${cx - 1} ${cy + 7} M${cx + 8} ${cy - 4} L${cx + 5} ${cy + 5}" stroke="#fff" stroke-width="2.6"/>`;
  if (type === "foundation") return `${base}<path d="M ${cx - 16} ${cy + 12} L ${cx + 16} ${cy + 12} L ${cx - 4.2} ${cy + 12} Q ${cx - 11} ${cy + 12} ${cx - 11} ${cy + 5.2} L ${cx - 11} ${cy - 1} Q ${cx - 11} ${cy - 5} ${cx - 7} ${cy - 5} L ${cx - 6} ${cy - 5} Q ${cx - 2} ${cy - 5} ${cx - 2} ${cy - 1} L ${cx - 2} ${cy + 8} Q ${cx - 2} ${cy + 12} ${cx + 2} ${cy + 12} L ${cx} ${cy + 12} Q ${cx + 4} ${cy + 12} ${cx + 4} ${cy + 8} L ${cx + 4} ${cy - 9} Q ${cx + 4} ${cy - 13} ${cx + 8} ${cy - 13} L ${cx + 9} ${cy - 13} Q ${cx + 13} ${cy - 13} ${cx + 13} ${cy - 9} L ${cx + 13} ${cy + 12}" fill="none" stroke="#fff" stroke-width="3" stroke-linejoin="round" stroke-linecap="round"/>`;
  return `${base}<path d="M${cx - 16} ${cy} H${cx + 16} M${cx} ${cy - 16} V${cy + 16}" stroke="#fff" stroke-width="3.5" stroke-linecap="round"/>`;
}

function card(id) {
  const c = cards[id];
  const textX = c.x + 94;
  return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  ${icon(c.icon, c.x, c.y, c.stroke)}
  <text class="kicker" x="${textX}" y="${c.y + 38}">${esc(c.kicker)}</text>
  <text class="cardTitle" x="${textX}" y="${c.y + 68}">${esc(c.title)}</text>
  ${c.lines.map((line, i) => `<text class="detail" x="${textX}" y="${c.y + 104 + i * 24}">${esc(line)}</text>`).join("\n  ")}
</g>`;
}

function label({ x, y, text, w }) {
  return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 15})"><rect width="${w}" height="30" rx="8"/><text x="${w / 2}" y="20" text-anchor="middle">${esc(text)}</text></g>`;
}

function nums(d) {
  return d.match(/-?\d+(?:\.\d+)?/g).map(Number);
}

function segs(d) {
  const n = nums(d);
  const pts = [];
  for (let i = 0; i < n.length; i += 2) pts.push({ x: n[i], y: n[i + 1] });
  return pts.slice(1).map((p, i) => ({ a: pts[i], b: p }));
}

function touches(b, p) {
  const onX = p.x >= b.x - 0.1 && p.x <= b.x + b.w + 0.1;
  const onY = p.y >= b.y - 0.1 && p.y <= b.y + b.h + 0.1;
  return ((Math.abs(p.x - b.x) < 0.1 || Math.abs(p.x - (b.x + b.w)) < 0.1) && onY) ||
    ((Math.abs(p.y - b.y) < 0.1 || Math.abs(p.y - (b.y + b.h)) < 0.1) && onX);
}

function hits(s, b, pad = 8) {
  const box = { x: b.x + pad, y: b.y + pad, w: b.w - pad * 2, h: b.h - pad * 2 };
  const minX = Math.min(s.a.x, s.b.x);
  const maxX = Math.max(s.a.x, s.b.x);
  const minY = Math.min(s.a.y, s.b.y);
  const maxY = Math.max(s.a.y, s.b.y);
  if (s.a.x === s.b.x) return s.a.x > box.x && s.a.x < box.x + box.w && maxY > box.y && minY < box.y + box.h;
  if (s.a.y === s.b.y) return s.a.y > box.y && s.a.y < box.y + box.h && maxX > box.x && minX < box.x + box.w;
  return false;
}

function validate() {
  for (const e of edges) {
    const n = nums(e.d);
    const start = { x: n[0], y: n[1] };
    const end = { x: n[n.length - 2], y: n[n.length - 1] };
    if (!touches(cards[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(cards[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) {
      for (const [id, c] of Object.entries(cards)) {
        if ((id === e.from || id === e.to) && (touches(c, s.a) || touches(c, s.b))) continue;
        if (hits(s, c)) throw new Error(`${e.id} crosses ${id}`);
      }
    }
  }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" data-layout="feature-map" data-allow-grid="true" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Math module feature structure">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.lane{fill:#F3F8FF;stroke:#94A3B8;stroke-width:1.8;stroke-dasharray:12 8}.laneTitle{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.card{filter:url(#softShadow);stroke-width:2}.kicker{font-family:"Comic Mono";font-size:12.8px;fill:${colors.muted}}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:13.2px;fill:${colors.muted}}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 8}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/>
<text class="title" x="78" y="88">Math Feature Structure</text>
<text class="subtitle" x="82" y="120">A task-oriented map of bluetape4k-math: statistics, random sampling, numeric methods, algebra, geometry, and ML helpers over Apache Commons Math3.</text>
<rect class="lane" x="70" y="455" width="1875" height="592" rx="8" fill="#DBEAFE" stroke="#93C5FD" fill-opacity="1"/><text class="laneTitle" x="98" y="442">reader-facing feature groups backed by concrete package/file families</text>
<g id="edges">${edges.map((e) => `<path class="edge route${e.dashed ? " dashed" : ""}" data-from="${e.from}" data-to="${e.to}" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
${Object.keys(cards).map(card).join("\n")}
</svg>`;

for (const e of edges) {
  if (!svg.includes(`id="arrow-${e.id}"`)) throw new Error(`missing marker ${e.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
