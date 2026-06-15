#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-workflow-diagram-04.svg";
const pngPath = "docs/images/readme-diagrams/utils-workflow-diagram-04.png";

const W = 1680;
const H = 940;
const colors = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  blue: "#2563EB",
  green: "#16A34A",
  pink: "#DB2777",
  teal: "#0D9488",
  orange: "#EA580C",
  lime: "#65A30D",
  purple: "#9333EA",
};

const cards = {
  start: { x: 110, y: 245, w: 330, h: 124, fill: "#EFF6FF", stroke: colors.blue, title: "Start sequential flow" },
  execute: { x: 540, y: 245, w: 360, h: 124, fill: "#F0FDF4", stroke: colors.green, title: "Execute next Work" },
  inspect: { x: 1000, y: 245, w: 360, h: 124, fill: "#F0FDFA", stroke: colors.teal, title: "Inspect WorkReport" },
  success: { x: 1210, y: 495, w: 350, h: 118, fill: "#F7FEE7", stroke: colors.lime, title: "Success path" },
  failure: { x: 780, y: 495, w: 350, h: 118, fill: "#FDF2F8", stroke: colors.pink, title: "Failure path" },
  stop: { x: 290, y: 735, w: 350, h: 118, fill: "#FFF1F2", stroke: colors.pink, title: "STOP strategy" },
  cont: { x: 760, y: 735, w: 380, h: 118, fill: "#FFF7ED", stroke: colors.orange, title: "CONTINUE strategy" },
  final: { x: 1240, y: 735, w: 350, h: 118, fill: "#FAF5FF", stroke: colors.purple, title: "Final result" },
};

const edges = [
  { id: "startExecute", color: colors.blue, from: "start", to: "execute", d: "M440 307 L540 307", label: { x: 490, y: 236, text: "initial context", w: 126 } },
  { id: "executeInspect", color: colors.green, from: "execute", to: "inspect", d: "M900 307 L1000 307", label: { x: 950, y: 282, text: "report", w: 72 } },
  { id: "noFailure", color: colors.lime, from: "inspect", to: "success", d: "M1360 307 L1455 307 L1455 495", label: { x: 1488, y: 407, text: "not failure", w: 104 } },
  { id: "failure", color: colors.pink, from: "inspect", to: "failure", d: "M1180 369 L1180 450 L955 450 L955 495", label: { x: 1066, y: 425, text: "Failure", w: 82 } },
  { id: "stop", color: colors.pink, from: "failure", to: "stop", d: "M780 554 L660 554 L660 794 L640 794", label: { x: 668, y: 674, text: "STOP", w: 64 } },
  { id: "continue", color: colors.orange, from: "failure", to: "cont", d: "M955 613 L955 735", label: { x: 1028, y: 676, text: "CONTINUE", w: 104 } },
  { id: "successFinal", color: colors.lime, from: "success", to: "final", d: "M1385 613 L1385 735", label: { x: 1458, y: 676, text: "no failures", w: 96 } },
  { id: "partialFinal", color: colors.orange, from: "cont", to: "final", d: "M1140 794 L1240 794", label: { x: 1190, y: 769, text: "has failures", w: 108 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12 Z" fill="${color}"/></marker>`;
}

function icon(kind, x, y, color) {
  if (kind === "play") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M24 17 L40 28 L24 39 Z" fill="${color}" stroke="none"/></g>`;
  if (kind === "work") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="7" y="10" width="50" height="34" rx="7" fill="#fff"/><path d="M18 27 H45 M37 19 L45 27 L37 35" fill="none"/></g>`;
  if (kind === "inspect") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="9" y="10" width="42" height="36" rx="7" fill="#fff"/><path d="M18 23 H42 M18 33 H35" fill="none"/><circle cx="44" cy="34" r="4" fill="${color}" stroke="none"/></g>`;
  if (kind === "ok") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M17 29 L25 37 L42 18" fill="none"/></g>`;
  if (kind === "fail") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M18 18 L38 38 M38 18 L18 38" fill="none"/></g>`;
  if (kind === "loop") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><path d="M43 20 A17 17 0 1 0 43 38" fill="none"/><path d="M42 14 L44 22 L36 20" fill="none"/></g>`;
  return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="8" y="10" width="48" height="36" rx="7" fill="#fff"/><path d="M18 24 H44 M18 34 H38" fill="none"/></g>`;
}

function card(id, lines, kind) {
  const c = cards[id];
  const cx = c.x + c.w / 2;
  return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  ${icon(kind, c.x + 24, c.y + 24, c.stroke)}
  <text class="cardTitle" x="${cx + 24}" y="${c.y + 40}" text-anchor="middle">${esc(c.title)}</text>
  ${lines.map((line, i) => `<text class="detail" x="${cx}" y="${c.y + 78 + i * 19}" text-anchor="middle">${esc(line)}</text>`).join("\n  ")}
</g>`;
}

function label({ x, y, text, w }) {
  return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 15})"><rect width="${w}" height="30" rx="8"/><text x="${w / 2}" y="20" text-anchor="middle">${esc(text)}</text></g>`;
}

function nums(d) {
  return d.match(/-?\d+(?:\.\d+)?/g).map(Number);
}

function segments(d) {
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

function hits(seg, b, pad = 8) {
  const box = { x: b.x + pad, y: b.y + pad, w: b.w - pad * 2, h: b.h - pad * 2 };
  const minX = Math.min(seg.a.x, seg.b.x), maxX = Math.max(seg.a.x, seg.b.x);
  const minY = Math.min(seg.a.y, seg.b.y), maxY = Math.max(seg.a.y, seg.b.y);
  if (seg.a.x === seg.b.x) return seg.a.x > box.x && seg.a.x < box.x + box.w && maxY > box.y && minY < box.y + box.h;
  if (seg.a.y === seg.b.y) return seg.a.y > box.y && seg.a.y < box.y + box.h && maxX > box.x && minX < box.x + box.w;
  return false;
}

function crosses(a, b) {
  const av = a.a.x === a.b.x, bv = b.a.x === b.b.x;
  if (av === bv) return false;
  const v = av ? a : b, h = av ? b : a;
  const x = v.a.x, y = h.a.y;
  const crossing = x > Math.min(h.a.x, h.b.x) && x < Math.max(h.a.x, h.b.x) && y > Math.min(v.a.y, v.b.y) && y < Math.max(v.a.y, v.b.y);
  return crossing && ![a.a, a.b, b.a, b.b].some((p) => p.x === x && p.y === y);
}

function validate() {
  const ids = Object.keys(cards);
  for (const edge of edges) {
    const n = nums(edge.d), start = { x: n[0], y: n[1] }, end = { x: n[n.length - 2], y: n[n.length - 1] };
    if (!touches(cards[edge.from], start)) throw new Error(`${edge.id} start`);
    if (!touches(cards[edge.to], end)) throw new Error(`${edge.id} end`);
    for (const seg of segments(edge.d)) for (const id of ids) {
      if ((id === edge.from || id === edge.to) && (touches(cards[id], seg.a) || touches(cards[id], seg.b))) continue;
      if (hits(seg, cards[id])) throw new Error(`${edge.id} crosses ${id}`);
    }
  }
  for (let i = 0; i < edges.length; i++) for (let j = i + 1; j < edges.length; j++) {
    for (const a of segments(edges[i].d)) for (const b of segments(edges[j].d)) if (crosses(a, b)) throw new Error(`Line crossing: ${edges[i].id} x ${edges[j].id}`);
  }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Sequential Flow Error Strategy">
<defs>
  <filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${edges.map((edge) => marker(edge.id, edge.color)).join("\n  ")}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}
    .card{filter:url(#softShadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:13.5px;fill:${colors.muted}}
    .icon{stroke-width:2.4;stroke-linecap:round;stroke-linejoin:round}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 8}
    .edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}
  </style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="38" y="30" width="1604" height="874" rx="8"/>
<text class="title" x="78" y="86">Sequential Flow Error Strategy</text>
<text class="subtitle" x="82" y="118">Sequential flows run work in order; STOP returns the first failure, while CONTINUE accumulates failures and keeps running later work.</text>
<g id="edges">
${edges.map((edge) => `  <path class="edge${edge.dashed ? " dashed" : ""}" d="${edge.d}" stroke="${edge.color}" marker-end="url(#arrow-${edge.id})"/>`).join("\n")}
</g>
<g id="labels">
${edges.map((edge) => `  ${label(edge.label)}`).join("\n")}
</g>
${card("start", ["initial WorkContext", "ordered work list"], "play")}
${card("execute", ["calls execute(context)", "exceptions become Failure"], "work")}
${card("inspect", ["Success continues", "Failure checks strategy"], "inspect")}
${card("success", ["advance in order", "or finish with Success"], "ok")}
${card("failure", ["only Failure uses strategy", "Aborted/Cancelled exit earlier"], "fail")}
${card("stop", ["return first Failure", "flow exits immediately"], "fail")}
${card("cont", ["record failed report", "continue remaining work"], "loop")}
${card("final", ["Success if no failures", "PartialSuccess if accumulated"], "report")}
</svg>`;

for (const edge of edges) {
  if (!svg.includes(`id="arrow-${edge.id}"`) || !svg.includes(`fill="${edge.color}"`)) throw new Error(`marker color ${edge.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
