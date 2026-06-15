#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-workflow-diagram-06.svg";
const pngPath = "docs/images/readme-diagrams/utils-workflow-diagram-06.png";
const W = 1680;
const H = 900;
const colors = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  blue: "#2563EB",
  green: "#16A34A",
  orange: "#EA580C",
  pink: "#DB2777",
  purple: "#9333EA",
  teal: "#0D9488",
};
const nodes = {
  start: { x: 635, y: 190, w: 410, h: 116, fill: "#EFF6FF", stroke: colors.blue, title: "Conditional flow" },
  decision: { x: 675, y: 350, w: 330, h: 170, fill: "#FAF5FF", stroke: colors.purple, title: "Predicate true?" },
  then: { x: 220, y: 560, w: 360, h: 118, fill: "#F0FDF4", stroke: colors.green, title: "Then work" },
  noop: { x: 660, y: 560, w: 360, h: 118, fill: "#FFF7ED", stroke: colors.orange, title: "Default Success" },
  otherwise: { x: 1100, y: 560, w: 360, h: 118, fill: "#FDF2F8", stroke: colors.pink, title: "Otherwise work" },
  report: { x: 580, y: 725, w: 520, h: 114, fill: "#F0FDFA", stroke: colors.teal, title: "Branch WorkReport" },
};
const edges = [
  { id: "evaluate", color: colors.blue, from: "start", to: "decision", d: "M840 306 L840 350", label: { x: 910, y: 330, text: "evaluate", w: 82 } },
  { id: "yes", color: colors.green, from: "decision", to: "then", d: "M675 435 L520 435 L520 560", label: { x: 560, y: 410, text: "true", w: 58 } },
  { id: "missing", color: colors.orange, from: "decision", to: "noop", d: "M840 520 L840 560", dashed: true, label: { x: 930, y: 540, text: "no branch", w: 94 } },
  { id: "no", color: colors.pink, from: "decision", to: "otherwise", d: "M1005 435 L1160 435 L1160 560", label: { x: 1120, y: 410, text: "false", w: 62 } },
  { id: "thenReport", color: colors.green, from: "then", to: "report", d: "M400 678 L400 782 L580 782", label: { x: 476, y: 760, text: "returns", w: 78 } },
  { id: "noopReport", color: colors.orange, from: "noop", to: "report", d: "M840 678 L840 725", dashed: true, label: { x: 920, y: 704, text: "success", w: 82 } },
  { id: "otherwiseReport", color: colors.pink, from: "otherwise", to: "report", d: "M1280 678 L1280 782 L1100 782", label: { x: 1204, y: 760, text: "returns", w: 78 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}
function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12 Z" fill="${color}"/></marker>`;
}
function icon(kind, x, y, color) {
  if (kind === "branch") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><path d="M28 7 L51 28 L28 49 L5 28 Z" fill="#fff"/><path d="M28 17 V39 M18 28 H38" fill="none"/></g>`;
  if (kind === "work") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="8" y="12" width="48" height="34" rx="7" fill="#fff"/><path d="M18 24 H46 M18 34 H38" fill="none"/></g>`;
  if (kind === "report") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="8" y="10" width="48" height="36" rx="7" fill="#fff"/><path d="M18 24 H44 M18 34 H38" fill="none"/><circle cx="46" cy="35" r="4" fill="${color}" stroke="none"/></g>`;
  if (kind === "success") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M18 29 L26 37 L40 19" fill="none"/></g>`;
  return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M24 17 L40 28 L24 39 Z" fill="${color}" stroke="none"/></g>`;
}
function rectCard(id, lines, kind) {
  const n = nodes[id];
  const cx = n.x + n.w / 2;
  return `<g id="${id}"><rect class="card" x="${n.x}" y="${n.y}" width="${n.w}" height="${n.h}" rx="8" fill="${n.fill}" stroke="${n.stroke}"/>
  ${icon(kind, n.x + 24, n.y + 24, n.stroke)}
  <text class="cardTitle" x="${cx + 24}" y="${n.y + 42}" text-anchor="middle">${esc(n.title)}</text>
  ${lines.map((l, i) => `<text class="detail" x="${cx}" y="${n.y + 78 + i * 19}" text-anchor="middle">${esc(l)}</text>`).join("\n  ")}</g>`;
}
function diamond(id, lines) {
  const n = nodes[id];
  const cx = n.x + n.w / 2;
  const cy = n.y + n.h / 2;
  const pts = `${cx},${n.y} ${n.x + n.w},${cy} ${cx},${n.y + n.h} ${n.x},${cy}`;
  return `<g id="${id}"><polygon class="card" points="${pts}" fill="${n.fill}" stroke="${n.stroke}"/>
  <text class="cardTitle" x="${cx}" y="${cy - 8}" text-anchor="middle">${esc(n.title)}</text>
  ${lines.map((l, i) => `<text class="detail" x="${cx}" y="${cy + 23 + i * 19}" text-anchor="middle">${esc(l)}</text>`).join("\n  ")}</g>`;
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
function crosses(a, b) {
  const av = a.a.x === a.b.x;
  const bv = b.a.x === b.b.x;
  if (av === bv) return false;
  const v = av ? a : b;
  const h = av ? b : a;
  const x = v.a.x;
  const y = h.a.y;
  const c = x > Math.min(h.a.x, h.b.x) && x < Math.max(h.a.x, h.b.x) && y > Math.min(v.a.y, v.b.y) && y < Math.max(v.a.y, v.b.y);
  return c && ![a.a, a.b, b.a, b.b].some((p) => p.x === x && p.y === y);
}
function validate() {
  const ids = Object.keys(nodes);
  for (const e of edges) {
    const n = nums(e.d);
    const start = { x: n[0], y: n[1] };
    const end = { x: n[n.length - 2], y: n[n.length - 1] };
    if (!touches(nodes[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(nodes[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) for (const id of ids) {
      if ((id === e.from || id === e.to) && (touches(nodes[id], s.a) || touches(nodes[id], s.b))) continue;
      if (hits(s, nodes[id])) throw new Error(`${e.id} crosses ${id}`);
    }
  }
  for (let i = 0; i < edges.length; i++) {
    for (let j = i + 1; j < edges.length; j++) {
      for (const a of segs(edges[i].d)) {
        for (const b of segs(edges[j].d)) {
          if (crosses(a, b)) throw new Error(`Line crossing: ${edges[i].id} x ${edges[j].id}`);
        }
      }
    }
  }
}
validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Conditional Flow Branching">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.card{filter:url(#softShadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:13.5px;fill:${colors.muted}}.icon{stroke-width:2.4;stroke-linecap:round;stroke-linejoin:round}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 8}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="1604" height="834" rx="8"/>
<text class="title" x="78" y="86">Conditional Flow Branching</text>
<text class="subtitle" x="82" y="118">The predicate selects exactly one branch. Sync returns branch reports; suspend converts non-cancellation exceptions to Failure.</text>
<g id="edges">${edges.map((e) => `<path class="edge${e.dashed ? " dashed" : ""}" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
${rectCard("start", ["condition { ctx -> ... }", "shared WorkContext"], "play")}
${diamond("decision", ["WorkContext -> Boolean"])}
${rectCard("then", ["execute selected true branch"], "work")}
${rectCard("otherwise", ["optional false branch"], "work")}
${rectCard("noop", ["when otherwise is absent"], "success")}
${rectCard("report", ["branch report returned", "or default success"], "report")}
</svg>`;
for (const e of edges) {
  if (!svg.includes(`id="arrow-${e.id}"`) || !svg.includes(`fill="${e.color}"`)) throw new Error(`marker color ${e.id}`);
}
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
