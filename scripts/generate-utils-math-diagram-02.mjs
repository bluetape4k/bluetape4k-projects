#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-math-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/utils-math-diagram-02.png";
const W = 1940;
const H = 1240;
const colors = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  blue: "#2563EB",
  teal: "#0D9488",
  orange: "#EA580C",
  purple: "#7C3AED",
  green: "#16A34A",
  pink: "#DB2777",
  gray: "#64748B",
};

const evidence = [
  "utils/math/README.md",
  "utils/math/src/main/kotlin/io/bluetape4k/math/interpolation/Interpolator.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/interpolation/AbstractInterpolator.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/integration/Integrator.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/integration/AbstractIntegrator.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/equation/Equator.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/equation/AbstractEquator.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/Descriptives.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/RandomSupport.kt",
  "utils/math/src/main/kotlin/io/bluetape4k/math/ml/clustering/Clustering.kt",
];

for (const file of evidence) {
  if (!existsSync(file)) throw new Error(`Missing source evidence: ${file}`);
}

const readme = readFileSync("utils/math/README.md", "utf8");
if (!/Class Diagram[\s\S]*utils-math-diagram-02\.png/.test(readme)) {
  throw new Error("README Class Diagram slot not found");
}

const boxes = {
  interpolator: { x: 100, y: 190, w: 480, h: 150, fill: "#EFF6FF", stroke: colors.blue, stereotype: "<<fun interface>>", title: "Interpolator", attrs: [], methods: ["+ interpolate(xs, ys): (Double) -> Double"] },
  abstractInterpolator: { x: 100, y: 425, w: 480, h: 150, fill: "#F8FAFC", stroke: colors.blue, stereotype: "<<abstract>>", title: "AbstractInterpolator", attrs: ["# apacheInterpolator: UnivariateInterpolator"], methods: ["+ interpolate(xs, ys): (Double) -> Double"] },
  interpolationImpls: { x: 100, y: 690, w: 480, h: 170, fill: "#EFF6FF", stroke: colors.blue, stereotype: "<<concrete>>", title: "Interpolator implementations", attrs: ["Linear, Spline, Loess", "AkimaSpline, Neville"], methods: ["wrap Commons Math interpolators"] },

  integrator: { x: 730, y: 190, w: 480, h: 170, fill: "#F0FDFA", stroke: colors.teal, stereotype: "<<interface>>", title: "Integrator", attrs: ["+ relativeAccuracy, absoluteAccuracy"], methods: ["+ integrate(lower, upper, fn): Double", "+ integrate(xs, ys, interpolator): Double"] },
  abstractIntegrator: { x: 730, y: 425, w: 480, h: 150, fill: "#F8FAFC", stroke: colors.teal, stereotype: "<<abstract>>", title: "AbstractIntegrator", attrs: ["# apacheIntegrator: UnivariateIntegrator"], methods: ["+ integrate(lower, upper, fn): Double"] },
  integrationImpls: { x: 730, y: 690, w: 480, h: 170, fill: "#F0FDFA", stroke: colors.teal, stereotype: "<<concrete>>", title: "Integrator implementations", attrs: ["Romberg, Simpson", "Trapezoid, MidPoint"], methods: ["delegate to Commons Math integrators"] },

  equator: { x: 1360, y: 190, w: 480, h: 170, fill: "#FFF7ED", stroke: colors.orange, stereotype: "<<interface>>", title: "Equator", attrs: ["+ absoluteAccuracy: Double"], methods: ["+ solve(min, max, fn): Double", "+ solve(xs, ys): Double"] },
  abstractEquator: { x: 1360, y: 425, w: 480, h: 150, fill: "#F8FAFC", stroke: colors.orange, stereotype: "<<abstract>>", title: "AbstractEquator", attrs: ["# solver: BaseUnivariateSolver"], methods: ["+ solve(maxEval, min, max, fn): Double"] },
  equationImpls: { x: 1360, y: 690, w: 480, h: 170, fill: "#FFF7ED", stroke: colors.orange, stereotype: "<<concrete>>", title: "Equator implementations", attrs: ["Bisection, Brent, Secant", "Pegasus, Ridders, RegulaFalsi"], methods: ["wrap Commons Math solvers"] },

  descriptives: { x: 100, y: 955, w: 390, h: 150, fill: "#FAF5FF", stroke: colors.purple, stereotype: "<<interface + internal class>>", title: "Descriptives", attrs: ["ApacheDescriptives wraps DescriptiveStatistics"], methods: ["mean, variance, percentile, get"] },
  randomTypes: { x: 550, y: 955, w: 390, h: 150, fill: "#F0FDF4", stroke: colors.green, stereotype: "<<classes>>", title: "Weighted random types", attrs: ["WeightedCoin", "WeightedDice<T>"], methods: ["flip(), roll()"] },
  clusteringTypes: { x: 1000, y: 955, w: 390, h: 150, fill: "#FDF2F8", stroke: colors.pink, stereotype: "<<data classes>>", title: "Clustering types", attrs: ["ClusterInput<T>, Centroid<T>"], methods: ["ClusterInput implements Clusterable"] },
  functionPackages: { x: 1450, y: 955, w: 390, h: 150, fill: "#F8FAFC", stroke: colors.gray, stereotype: "<<top-level support>>", title: "Function packages", attrs: ["statistics, special, linear", "geometry, transform, fraction"], methods: ["factory and extension APIs"] },
};

const edges = [
  { id: "ai-impl", type: "implements", color: colors.blue, from: "abstractInterpolator", to: "interpolator", d: "M340 425 L340 340" },
  { id: "i-extends", type: "extends", color: colors.blue, from: "interpolationImpls", to: "abstractInterpolator", d: "M340 690 L340 575" },
  { id: "ag-impl", type: "implements", color: colors.teal, from: "abstractIntegrator", to: "integrator", d: "M970 425 L970 360" },
  { id: "g-extends", type: "extends", color: colors.teal, from: "integrationImpls", to: "abstractIntegrator", d: "M970 690 L970 575" },
  { id: "ae-impl", type: "implements", color: colors.orange, from: "abstractEquator", to: "equator", d: "M1600 425 L1600 360" },
  { id: "e-extends", type: "extends", color: colors.orange, from: "equationImpls", to: "abstractEquator", d: "M1600 690 L1600 575" },
  { id: "int-uses-interp", type: "uses", color: colors.gray, from: "integrator", to: "interpolator", d: "M730 265 L620 265 L620 256 L580 256", label: { x: 650, y: 238, text: "uses Interpolator", w: 142 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function box(id) {
  const b = boxes[id];
  const nameSepY = b.y + 66;
  const attrRows = Math.max(1, b.attrs.length);
  const methodRows = Math.max(1, b.methods.length);
  const attrHeight = Math.max(40, attrRows * 20 + 18);
  const methodSepY = b.attrs.length ? Math.min(b.y + b.h - 44, nameSepY + attrHeight) : nameSepY;
  const attrBlockHeight = methodSepY - nameSepY;
  const methodBlockHeight = b.y + b.h - methodSepY;
  const attrY = nameSepY + (attrBlockHeight - b.attrs.length * 20) / 2 + 15;
  const methodY = methodSepY + (methodBlockHeight - b.methods.length * 20) / 2 + 15;
  const methodSep = b.attrs.length
    ? `<line x1="${b.x}" y1="${methodSepY}" x2="${b.x + b.w}" y2="${methodSepY}" stroke="${b.stroke}" stroke-width="1.3" opacity="0.65"/>`
    : "";
  return `<g id="${id}">
  <rect class="umlBox" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/>
  <line x1="${b.x}" y1="${nameSepY}" x2="${b.x + b.w}" y2="${nameSepY}" stroke="${b.stroke}" stroke-width="1.3" opacity="0.65"/>
  ${methodSep}
  <text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 25}" text-anchor="middle">${esc(b.stereotype)}</text>
  <text class="classTitle" x="${b.x + b.w / 2}" y="${b.y + 52}" text-anchor="middle">${esc(b.title)}</text>
  ${b.attrs.map((line, i) => `<text class="member" x="${b.x + 24}" y="${attrY + i * 20}">${esc(line)}</text>`).join("\n  ")}
  ${b.methods.map((line, i) => `<text class="member" x="${b.x + 24}" y="${methodY + i * 20}">${esc(line)}</text>`).join("\n  ")}
</g>`;
}

function label({ x, y, text, w }) {
  return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 14})"><rect width="${w}" height="28" rx="8"/><text x="${w / 2}" y="19" text-anchor="middle">${esc(text)}</text></g>`;
}

function arrowHead(edge) {
  const n = nums(edge.d);
  const end = { x: n[n.length - 2], y: n[n.length - 1] };
  const prev = { x: n[n.length - 4], y: n[n.length - 3] };
  const dx = end.x - prev.x;
  const dy = end.y - prev.y;

  if (edge.type === "extends" || edge.type === "implements") {
    if (Math.abs(dy) >= Math.abs(dx) && dy < 0) {
      return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 7} ${end.y + 14} L${end.x + 7} ${end.y + 14} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
    }
    if (Math.abs(dy) >= Math.abs(dx) && dy > 0) {
      return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 7} ${end.y - 14} L${end.x + 7} ${end.y - 14} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
    }
    if (dx < 0) {
      return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x + 14} ${end.y - 7} L${end.x + 14} ${end.y + 7} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
    }
    return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 14} ${end.y - 7} L${end.x - 14} ${end.y + 7} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
  }

  if (dx < 0) {
    return `<path class="solidOpenHead" d="M${end.x + 12} ${end.y - 6} L${end.x} ${end.y} L${end.x + 12} ${end.y + 6}" stroke="${edge.color}"/>`;
  }
  if (dx > 0) {
    return `<path class="solidOpenHead" d="M${end.x - 12} ${end.y - 6} L${end.x} ${end.y} L${end.x - 12} ${end.y + 6}" stroke="${edge.color}"/>`;
  }
  if (dy < 0) {
    return `<path class="solidOpenHead" d="M${end.x - 6} ${end.y + 12} L${end.x} ${end.y} L${end.x + 6} ${end.y + 12}" stroke="${edge.color}"/>`;
  }
  return `<path class="solidOpenHead" d="M${end.x - 6} ${end.y - 12} L${end.x} ${end.y} L${end.x + 6} ${end.y - 12}" stroke="${edge.color}"/>`;
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

function hits(s, b, pad = 10) {
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
    if (!touches(boxes[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(boxes[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) {
      for (const [id, b] of Object.entries(boxes)) {
        if ((id === e.from || id === e.to) && (touches(b, s.a) || touches(b, s.b))) continue;
        if (hits(s, b)) throw new Error(`${e.id} crosses ${id}`);
      }
    }
  }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Math module class structure">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${colors.muted}}.section{fill:#F3F8FF;stroke:#94A3B8;stroke-width:1.7;stroke-dasharray:12 8}.sectionTitle{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.umlBox{filter:url(#softShadow);stroke-width:2}.stereo{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}.classTitle{font-family:"Architects Daughter";font-size:23px;fill:${colors.ink}}.member{font-family:"Comic Mono";font-size:12.8px;fill:${colors.muted}}.edge{fill:none;stroke-width:2.45;stroke-linecap:round;stroke-linejoin:round}.extends{stroke-dasharray:none}.implements,.uses{stroke-dasharray:8 7}.solidHead{stroke-width:1.9;stroke-linejoin:round;stroke-dasharray:none}.solidOpenHead{fill:none;stroke-width:2.25;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:none}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.2;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:11.8px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/>
<text class="title" x="74" y="86">Math Class Structure</text>
<text class="subtitle" x="78" y="118">The module has three real adapter hierarchies over Apache Commons Math, plus focused support classes for statistics, weighted random, and clustering.</text>
<rect class="section" x="70" y="155" width="1810" height="735" rx="8"/><text class="sectionTitle" x="98" y="180">OOP adapter hierarchies: interface -> abstract Commons wrapper -> concrete algorithms</text>
<rect class="section" x="70" y="925" width="1810" height="205" rx="8"/><text class="sectionTitle" x="98" y="950">support classes and data classes; most remaining APIs are top-level extension/factory functions</text>
<g id="edges">${edges.map((e) => `<path class="edge ${e.type}" d="${e.d}" stroke="${e.color}"/>`).join("\n")}</g>
<g id="arrowheads">${edges.map(arrowHead).join("\n")}</g>
<g id="labels">${edges.filter((e) => e.label).map((e) => label(e.label)).join("\n")}</g>
${Object.keys(boxes).map(box).join("\n")}
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
