#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-workflow-diagram-03.svg";
const pngPath = "docs/images/readme-diagrams/utils-workflow-diagram-03.png";

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
  teal: "#0D9488",
  purple: "#9333EA",
  orange: "#EA580C",
  pink: "#DB2777",
  lime: "#65A30D",
};

const cards = {
  dsl: { x: 95, y: 280, w: 350, h: 132, fill: "#EFF6FF", stroke: colors.blue, title: "Caller DSL" },
  sync: { x: 555, y: 190, w: 390, h: 126, fill: "#F0FDF4", stroke: colors.green, title: "Sync WorkFlow" },
  suspend: { x: 555, y: 420, w: 390, h: 126, fill: "#F0FDFA", stroke: colors.teal, title: "SuspendWorkFlow" },
  syncRuntime: { x: 1110, y: 190, w: 410, h: 126, fill: "#F7FEE7", stroke: colors.lime, title: "Virtual-thread runtime" },
  suspendRuntime: { x: 1110, y: 420, w: 410, h: 126, fill: "#FAF5FF", stroke: colors.purple, title: "Coroutine runtime" },
  families: { x: 330, y: 670, w: 430, h: 126, fill: "#FFF7ED", stroke: colors.orange, title: "Flow families" },
  report: { x: 930, y: 670, w: 430, h: 126, fill: "#FDF2F8", stroke: colors.pink, title: "WorkReport contract" },
};

const edges = [
  { id: "syncBuild", color: colors.green, from: "dsl", to: "sync", d: "M445 322 L500 322 L500 253 L555 253", label: { x: 503, y: 292, text: "sync builders", w: 122 } },
  { id: "suspendBuild", color: colors.teal, from: "dsl", to: "suspend", d: "M445 370 L500 370 L500 483 L555 483", label: { x: 506, y: 426, text: "suspend builders", w: 148 } },
  { id: "syncRuntime", color: colors.lime, from: "sync", to: "syncRuntime", d: "M945 253 L1110 253", label: { x: 1028, y: 228, text: "parallel uses", w: 112 } },
  { id: "suspendRuntime", color: colors.purple, from: "suspend", to: "suspendRuntime", d: "M945 483 L1110 483", label: { x: 1028, y: 458, text: "parallel uses", w: 112 } },
  { id: "dslFamilies", color: colors.orange, from: "dsl", to: "families", d: "M270 412 L270 733 L330 733", dashed: true, label: { x: 272, y: 610, text: "shared flow types", w: 148 } },
  { id: "runtimeReportA", color: colors.pink, from: "syncRuntime", to: "report", d: "M1520 253 L1580 253 L1580 733 L1360 733", label: { x: 1582, y: 506, text: "returns report", w: 128 } },
  { id: "runtimeReportB", color: colors.pink, from: "suspendRuntime", to: "report", d: "M1315 546 L1315 610 L1145 610 L1145 670", label: { x: 1232, y: 592, text: "same result", w: 108 } },
  { id: "familiesReport", color: colors.blue, from: "families", to: "report", d: "M760 733 L930 733", label: { x: 845, y: 708, text: "unified contract", w: 142 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12 Z" fill="${color}"/></marker>`;
}

function icon(kind, x, y, color) {
  if (kind === "dsl") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><path d="M22 4 C12 4 13 18 8 21 C13 24 12 38 22 38" fill="none"/><path d="M42 4 C52 4 51 18 56 21 C51 24 52 38 42 38" fill="none"/><circle cx="32" cy="21" r="4" fill="${color}" stroke="none"/></g>`;
  if (kind === "sync") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="7" y="10" width="50" height="34" rx="7" fill="#fff"/><path d="M18 27 H46 M38 19 L46 27 L38 35" fill="none"/></g>`;
  if (kind === "suspend") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="18" cy="28" r="9" fill="#fff"/><circle cx="42" cy="28" r="9" fill="#fff"/><path d="M27 28 H33" fill="none"/></g>`;
  if (kind === "runtime") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="30" cy="28" r="22" fill="#fff"/><path d="M30 12 V28 L43 36" fill="none"/></g>`;
  if (kind === "families") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="12" cy="28" r="7" fill="#fff"/><circle cx="32" cy="16" r="7" fill="#fff"/><circle cx="52" cy="28" r="7" fill="#fff"/><circle cx="32" cy="40" r="7" fill="#fff"/><path d="M19 26 L25 20 M39 20 L46 26 M45 31 L39 37 M25 36 L19 30" fill="none"/></g>`;
  return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="8" y="10" width="48" height="36" rx="7" fill="#fff"/><path d="M18 24 H44 M18 34 H38" fill="none"/><circle cx="46" cy="35" r="4" fill="${color}" stroke="none"/></g>`;
}

function card(id, lines, kind) {
  const c = cards[id];
  const cx = c.x + c.w / 2;
  const titleX = cx + 22;
  return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  ${icon(kind, c.x + 24, c.y + 24, c.stroke)}
  <text class="cardTitle" x="${titleX}" y="${c.y + 40}" text-anchor="middle">${esc(c.title)}</text>
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

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Workflow Execution Model">
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
<text class="title" x="78" y="86">Workflow Execution Model</text>
<text class="subtitle" x="82" y="118">The same workflow shape can execute as sync WorkFlow or suspend WorkFlow; runtime primitives differ, but results converge on WorkReport.</text>
<g id="edges">
${edges.map((edge) => `  <path class="edge${edge.dashed ? " dashed" : ""}" d="${edge.d}" stroke="${edge.color}" marker-end="url(#arrow-${edge.id})"/>`).join("\n")}
</g>
<g id="labels">
${edges.map((edge) => `  ${label(edge.label)}`).join("\n")}
</g>
${card("dsl", ["workflow / sequentialFlow", "suspendWorkflow variants"], "dsl")}
${card("sync", ["WorkFlow extends Work", "blocking API, virtual-thread friendly"], "sync")}
${card("suspend", ["SuspendWorkFlow extends SuspendWork", "cancellation-aware suspend API"], "suspend")}
${card("syncRuntime", ["StructuredTaskScope", "timeout returns Cancelled"], "runtime")}
${card("suspendRuntime", ["coroutineScope, async, awaitAll", "delay and CancellationException"], "suspend")}
${card("families", ["sequential, parallel, conditional", "repeat and retry"], "families")}
${card("report", ["Success / Failure / PartialSuccess", "Aborted / Cancelled"], "report")}
</svg>`;

for (const edge of edges) {
  if (!svg.includes(`id="arrow-${edge.id}"`) || !svg.includes(`fill="${edge.color}"`)) throw new Error(`marker color ${edge.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
