#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-workflow-diagram-05.svg";
const pngPath = "docs/images/readme-diagrams/utils-workflow-diagram-05.png";
const W = 1680;
const H = 900;
const colors = {
  ink: "#0F172A", muted: "#475569", canvas: "#F8FAFC", frame: "#FFFFFF", line: "#CBD5E1",
  blue: "#2563EB", green: "#16A34A", teal: "#0D9488", orange: "#EA580C", pink: "#DB2777", purple: "#9333EA",
};
const cards = {
  start: { x: 635, y: 170, w: 410, h: 116, fill: "#EFF6FF", stroke: colors.blue, title: "Parallel flow starts" },
  policy: { x: 635, y: 325, w: 410, h: 124, fill: "#FAF5FF", stroke: colors.purple, title: "ParallelPolicy" },
  all: { x: 260, y: 535, w: 410, h: 132, fill: "#F0FDF4", stroke: colors.green, title: "ALL policy" },
  any: { x: 1010, y: 535, w: 410, h: 132, fill: "#F0FDFA", stroke: colors.teal, title: "ANY policy" },
  timeout: { x: 70, y: 315, w: 330, h: 118, fill: "#FFF7ED", stroke: colors.orange, title: "Timeout boundary" },
  report: { x: 605, y: 735, w: 470, h: 118, fill: "#FDF2F8", stroke: colors.pink, title: "WorkReport priority" },
};
const edges = [
  { id: "startPolicy", color: colors.blue, from: "start", to: "policy", d: "M840 286 L840 325", label: { x: 928, y: 309, text: "shared context", w: 126 } },
  { id: "all", color: colors.green, from: "policy", to: "all", d: "M635 387 L465 387 L465 535", label: { x: 530, y: 362, text: "ALL", w: 56 } },
  { id: "any", color: colors.teal, from: "policy", to: "any", d: "M1045 387 L1215 387 L1215 535", label: { x: 1150, y: 362, text: "ANY", w: 56 } },
  { id: "allReport", color: colors.green, from: "all", to: "report", d: "M465 667 L465 794 L605 794", label: { x: 534, y: 772, text: "collect reports", w: 132 } },
  { id: "anyReport", color: colors.teal, from: "any", to: "report", d: "M1215 667 L1215 794 L1075 794", label: { x: 1146, y: 772, text: "winner or failures", w: 148 } },
  { id: "timeoutReport", color: colors.orange, from: "timeout", to: "report", d: "M160 433 L160 820 L605 820", dashed: true, label: { x: 285, y: 798, text: "Cancelled", w: 96 } },
  { id: "startTimeout", color: colors.orange, from: "start", to: "timeout", d: "M635 228 L235 228 L235 315", dashed: true, label: { x: 420, y: 205, text: "deadline", w: 86 } },
];

function esc(v) { return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;"); }
function marker(id, color) { return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12 Z" fill="${color}"/></marker>`; }
function icon(kind, x, y, color) {
  if (kind === "policy") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><path d="M28 7 L51 28 L28 49 L5 28 Z" fill="#fff"/><path d="M18 24 H38 M18 33 H38" fill="none"/></g>`;
  if (kind === "fork") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="12" cy="28" r="7" fill="#fff"/><circle cx="50" cy="16" r="7" fill="#fff"/><circle cx="50" cy="40" r="7" fill="#fff"/><path d="M19 28 H31 C38 28 38 16 43 16 M31 28 C38 28 38 40 43 40" fill="none"/></g>`;
  if (kind === "clock") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M28 13 V28 L41 36" fill="none"/></g>`;
  if (kind === "report") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="8" y="10" width="48" height="36" rx="7" fill="#fff"/><path d="M18 24 H44 M18 34 H38" fill="none"/><circle cx="46" cy="35" r="4" fill="${color}" stroke="none"/></g>`;
  return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M24 17 L40 28 L24 39 Z" fill="${color}" stroke="none"/></g>`;
}
function card(id, lines, kind) {
  const c = cards[id], cx = c.x + c.w / 2;
  return `<g id="${id}"><rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  ${icon(kind, c.x + 24, c.y + 24, c.stroke)}
  <text class="cardTitle" x="${cx + 24}" y="${c.y + 40}" text-anchor="middle">${esc(c.title)}</text>
  ${lines.map((l, i) => `<text class="detail" x="${cx}" y="${c.y + 78 + i * 19}" text-anchor="middle">${esc(l)}</text>`).join("\n  ")}</g>`;
}
function label({ x, y, text, w }) { return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 15})"><rect width="${w}" height="30" rx="8"/><text x="${w / 2}" y="20" text-anchor="middle">${esc(text)}</text></g>`; }
function nums(d) { return d.match(/-?\d+(?:\.\d+)?/g).map(Number); }
function segs(d) { const n = nums(d), pts = []; for (let i = 0; i < n.length; i += 2) pts.push({ x: n[i], y: n[i + 1] }); return pts.slice(1).map((p, i) => ({ a: pts[i], b: p })); }
function touches(b, p) { const onX = p.x >= b.x - 0.1 && p.x <= b.x + b.w + 0.1, onY = p.y >= b.y - 0.1 && p.y <= b.y + b.h + 0.1; return ((Math.abs(p.x - b.x) < 0.1 || Math.abs(p.x - (b.x + b.w)) < 0.1) && onY) || ((Math.abs(p.y - b.y) < 0.1 || Math.abs(p.y - (b.y + b.h)) < 0.1) && onX); }
function hits(s, b, pad = 8) { const box = { x: b.x + pad, y: b.y + pad, w: b.w - pad * 2, h: b.h - pad * 2 }; const minX = Math.min(s.a.x, s.b.x), maxX = Math.max(s.a.x, s.b.x), minY = Math.min(s.a.y, s.b.y), maxY = Math.max(s.a.y, s.b.y); if (s.a.x === s.b.x) return s.a.x > box.x && s.a.x < box.x + box.w && maxY > box.y && minY < box.y + box.h; if (s.a.y === s.b.y) return s.a.y > box.y && s.a.y < box.y + box.h && maxX > box.x && minX < box.x + box.w; return false; }
function crosses(a, b) { const av = a.a.x === a.b.x, bv = b.a.x === b.b.x; if (av === bv) return false; const v = av ? a : b, h = av ? b : a; const x = v.a.x, y = h.a.y; const c = x > Math.min(h.a.x, h.b.x) && x < Math.max(h.a.x, h.b.x) && y > Math.min(v.a.y, v.b.y) && y < Math.max(v.a.y, v.b.y); return c && ![a.a, a.b, b.a, b.b].some((p) => p.x === x && p.y === y); }
function validate() {
  const ids = Object.keys(cards);
  for (const e of edges) {
    const n = nums(e.d), start = { x: n[0], y: n[1] }, end = { x: n[n.length - 2], y: n[n.length - 1] };
    if (!touches(cards[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(cards[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) for (const id of ids) {
      if ((id === e.from || id === e.to) && (touches(cards[id], s.a) || touches(cards[id], s.b))) continue;
      if (hits(s, cards[id])) throw new Error(`${e.id} crosses ${id}`);
    }
  }
  for (let i = 0; i < edges.length; i++) for (let j = i + 1; j < edges.length; j++) for (const a of segs(edges[i].d)) for (const b of segs(edges[j].d)) if (crosses(a, b)) throw new Error(`Line crossing: ${edges[i].id} x ${edges[j].id}`);
}
validate();
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Parallel Flow Policy">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.card{filter:url(#softShadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:13.5px;fill:${colors.muted}}.icon{stroke-width:2.4;stroke-linecap:round;stroke-linejoin:round}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 8}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="1604" height="834" rx="8"/>
<text class="title" x="78" y="86">Parallel Flow Policy</text>
<text class="subtitle" x="82" y="118">ALL waits with fail-fast semantics; ANY returns the first Success. Timeout and non-success reports still become WorkReport outcomes.</text>
<g id="edges">${edges.map((e) => `<path class="edge${e.dashed ? " dashed" : ""}" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
${card("start", ["shared WorkContext", "same input to every fork"], "play")}
${card("policy", ["ALL waits for all", "ANY races for first Success"], "policy")}
${card("all", ["StructuredTaskScope fail-fast", "failure cancels siblings"], "fork")}
${card("any", ["first Success wins", "non-success keeps racing"], "fork")}
${card("timeout", ["deadline exceeded", "returns Cancelled"], "clock")}
${card("report", ["Aborted > Cancelled > Failure", "otherwise Success"], "report")}
</svg>`;
for (const e of edges) if (!svg.includes(`id="arrow-${e.id}"`) || !svg.includes(`fill="${e.color}"`)) throw new Error(`marker color ${e.id}`);
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
