#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-workflow-diagram-07.svg";
const pngPath = "docs/images/readme-diagrams/utils-workflow-diagram-07.png";
const W = 1680;
const H = 900;
const colors = {
  ink: "#0F172A", muted: "#475569", canvas: "#F8FAFC", frame: "#FFFFFF", line: "#CBD5E1",
  blue: "#2563EB", green: "#16A34A", teal: "#0D9488", orange: "#EA580C", pink: "#DB2777", purple: "#9333EA",
};
const nodes = {
  start: { x: 635, y: 165, w: 410, h: 116, fill: "#EFF6FF", stroke: colors.blue, title: "Repeat flow starts" },
  work: { x: 635, y: 320, w: 410, h: 118, fill: "#F0FDF4", stroke: colors.green, title: "Execute work" },
  guard: { x: 635, y: 480, w: 410, h: 118, fill: "#FFF7ED", stroke: colors.orange, title: "Stop guard" },
  gate: { x: 675, y: 660, w: 330, h: 170, fill: "#FAF5FF", stroke: colors.purple, title: "Repeat again?" },
  delay: { x: 180, y: 686, w: 360, h: 118, fill: "#FDF2F8", stroke: colors.pink, title: "Suspend delay" },
  result: { x: 1140, y: 686, w: 380, h: 118, fill: "#F0FDFA", stroke: colors.teal, title: "Last WorkReport" },
};
const edges = [
  { id: "startWork", color: colors.blue, from: "start", to: "work", d: "M840 281 L840 320", label: { x: 912, y: 304, text: "context", w: 76 } },
  { id: "workGuard", color: colors.green, from: "work", to: "guard", d: "M840 438 L840 480", label: { x: 912, y: 462, text: "report", w: 70 } },
  { id: "breakResult", color: colors.orange, from: "guard", to: "result", d: "M1045 539 L1320 539 L1320 686", label: { x: 1245, y: 516, text: "aborted / cancelled", w: 150 } },
  { id: "guardGate", color: colors.purple, from: "guard", to: "gate", d: "M840 598 L840 660", label: { x: 920, y: 630, text: "otherwise", w: 92 } },
  { id: "gateResult", color: colors.teal, from: "gate", to: "result", d: "M1005 745 L1140 745", label: { x: 1070, y: 718, text: "stop", w: 58 } },
  { id: "gateDelay", color: colors.pink, from: "gate", to: "delay", d: "M675 745 L540 745", label: { x: 608, y: 718, text: "repeat", w: 78 } },
  { id: "delayLoop", color: colors.pink, from: "delay", to: "work", d: "M360 686 L360 379 L635 379", dashed: true, label: { x: 450, y: 357, text: "next iteration", w: 118 } },
];

function esc(v) { return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;"); }
function marker(id, color) { return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12 Z" fill="${color}"/></marker>`; }
function icon(kind, x, y, color) {
  if (kind === "work") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="8" y="12" width="48" height="34" rx="7" fill="#fff"/><path d="M18 24 H46 M18 34 H38" fill="none"/></g>`;
  if (kind === "guard") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M17 17 L39 39 M39 17 L17 39" fill="none"/></g>`;
  if (kind === "delay") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M28 13 V28 L41 36" fill="none"/></g>`;
  if (kind === "report") return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="8" y="10" width="48" height="36" rx="7" fill="#fff"/><path d="M18 24 H44 M18 34 H38" fill="none"/><circle cx="46" cy="35" r="4" fill="${color}" stroke="none"/></g>`;
  return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M24 17 L40 28 L24 39 Z" fill="${color}" stroke="none"/></g>`;
}
function rectCard(id, lines, kind) {
  const n = nodes[id], cx = n.x + n.w / 2;
  const titleX = cx + 24;
  const titleAnchor = "middle";
  return `<g id="${id}"><rect class="card" x="${n.x}" y="${n.y}" width="${n.w}" height="${n.h}" rx="8" fill="${n.fill}" stroke="${n.stroke}"/>
  ${icon(kind, n.x + 24, n.y + 24, n.stroke)}
  <text class="cardTitle" x="${titleX}" y="${n.y + 42}" text-anchor="${titleAnchor}">${esc(n.title)}</text>
  ${lines.map((l, i) => `<text class="detail" x="${cx}" y="${n.y + 78 + i * 19}" text-anchor="middle">${esc(l)}</text>`).join("\n  ")}</g>`;
}
function diamond(id, lines) {
  const n = nodes[id], cx = n.x + n.w / 2, cy = n.y + n.h / 2;
  const pts = `${cx},${n.y} ${n.x + n.w},${cy} ${cx},${n.y + n.h} ${n.x},${cy}`;
  return `<g id="${id}"><polygon class="card" points="${pts}" fill="${n.fill}" stroke="${n.stroke}"/>
  <text class="cardTitle" x="${cx}" y="${cy - 8}" text-anchor="middle">${esc(n.title)}</text>
  ${lines.map((l, i) => `<text class="detail" x="${cx}" y="${cy + 23 + i * 19}" text-anchor="middle">${esc(l)}</text>`).join("\n  ")}</g>`;
}
function label({ x, y, text, w }) { return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 15})"><rect width="${w}" height="30" rx="8"/><text x="${w / 2}" y="20" text-anchor="middle">${esc(text)}</text></g>`; }
function nums(d) { return d.match(/-?\d+(?:\.\d+)?/g).map(Number); }
function segs(d) { const n = nums(d), pts = []; for (let i = 0; i < n.length; i += 2) pts.push({ x: n[i], y: n[i + 1] }); return pts.slice(1).map((p, i) => ({ a: pts[i], b: p })); }
function touches(b, p) { const onX = p.x >= b.x - 0.1 && p.x <= b.x + b.w + 0.1, onY = p.y >= b.y - 0.1 && p.y <= b.y + b.h + 0.1; return ((Math.abs(p.x - b.x) < 0.1 || Math.abs(p.x - (b.x + b.w)) < 0.1) && onY) || ((Math.abs(p.y - b.y) < 0.1 || Math.abs(p.y - (b.y + b.h)) < 0.1) && onX); }
function hits(s, b, pad = 8) { const box = { x: b.x + pad, y: b.y + pad, w: b.w - pad * 2, h: b.h - pad * 2 }; const minX = Math.min(s.a.x, s.b.x), maxX = Math.max(s.a.x, s.b.x), minY = Math.min(s.a.y, s.b.y), maxY = Math.max(s.a.y, s.b.y); if (s.a.x === s.b.x) return s.a.x > box.x && s.a.x < box.x + box.w && maxY > box.y && minY < box.y + box.h; if (s.a.y === s.b.y) return s.a.y > box.y && s.a.y < box.y + box.h && maxX > box.x && minX < box.x + box.w; return false; }
function crosses(a, b) { const av = a.a.x === a.b.x, bv = b.a.x === b.b.x; if (av === bv) return false; const v = av ? a : b, h = av ? b : a; const x = v.a.x, y = h.a.y; const c = x > Math.min(h.a.x, h.b.x) && x < Math.max(h.a.x, h.b.x) && y > Math.min(v.a.y, v.b.y) && y < Math.max(v.a.y, v.b.y); return c && ![a.a, a.b, b.a, b.b].some((p) => p.x === x && p.y === y); }
function validate() {
  const ids = Object.keys(nodes);
  for (const e of edges) {
    const n = nums(e.d), start = { x: n[0], y: n[1] }, end = { x: n[n.length - 2], y: n[n.length - 1] };
    if (!touches(nodes[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(nodes[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) for (const id of ids) {
      if ((id === e.from || id === e.to) && (touches(nodes[id], s.a) || touches(nodes[id], s.b))) continue;
      if (hits(s, nodes[id])) throw new Error(`${e.id} crosses ${id}`);
    }
  }
  for (let i = 0; i < edges.length; i++) for (let j = i + 1; j < edges.length; j++) for (const a of segs(edges[i].d)) for (const b of segs(edges[j].d)) if (crosses(a, b)) throw new Error(`Line crossing: ${edges[i].id} x ${edges[j].id}`);
}
validate();
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Repeat Flow Loop">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.card{filter:url(#softShadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:13.5px;fill:${colors.muted}}.icon{stroke-width:2.4;stroke-linecap:round;stroke-linejoin:round}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 8}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="1604" height="834" rx="8"/>
<text class="title" x="78" y="86">Repeat Flow Loop</text>
<text class="subtitle" x="82" y="118">Repeat executes at least once, stops on Aborted or Cancelled, and otherwise loops while the predicate and max iteration guard allow it.</text>
<g id="edges">${edges.map((e) => `<path class="edge${e.dashed ? " dashed" : ""}" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
${rectCard("start", ["initial WorkReport.Success", "iteration starts at zero"], "play")}
${rectCard("work", ["work.execute(context)", "exceptions become Failure"], "work")}
${rectCard("guard", ["Aborted / Cancelled", "return immediately"], "guard")}
${diamond("gate", ["iteration < maxIterations", "repeatPredicate(report)"])}
${rectCard("delay", ["suspend repeatDelay only", "before next iteration"], "delay")}
${rectCard("result", ["final report returned", "last executed work wins"], "report")}
</svg>`;
for (const e of edges) if (!svg.includes(`id="arrow-${e.id}"`) || !svg.includes(`fill="${e.color}"`)) throw new Error(`marker color ${e.id}`);
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
