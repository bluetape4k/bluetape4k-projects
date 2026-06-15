#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-states-diagram-05.svg";
const pngPath = "docs/images/readme-diagrams/utils-states-diagram-05.png";
const W = 1780;
const H = 820;
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
  pink: "#DB2777",
  purple: "#9333EA",
  gray: "#64748B",
};

const nodes = {
  start: { x: 110, y: 330, w: 24, h: 24, fill: colors.ink, stroke: colors.ink, title: "start" },
  created: { x: 210, y: 250, w: 250, h: 155, fill: "#EFF6FF", stroke: colors.blue, title: "CREATED" },
  paid: { x: 570, y: 250, w: 250, h: 155, fill: "#ECFDF5", stroke: colors.green, title: "PAID" },
  shipped: { x: 930, y: 250, w: 250, h: 155, fill: "#F0FDFA", stroke: colors.teal, title: "SHIPPED" },
  delivered: { x: 1290, y: 250, w: 250, h: 155, fill: "#FDF2F8", stroke: colors.pink, title: "DELIVERED" },
  cancelled: { x: 390, y: 555, w: 300, h: 145, fill: "#FAF5FF", stroke: colors.purple, title: "CANCELLED" },
  reject: { x: 920, y: 570, w: 420, h: 96, fill: "#FFF7ED", stroke: colors.orange, title: "After SHIPPED, Cancel is rejected" },
};

const edges = [
  { id: "startCreated", color: colors.gray, from: "start", to: "created", d: "M134 342 L210 342", label: { x: 172, y: 317, text: "initial", w: 72 } },
  { id: "pay", color: colors.blue, from: "created", to: "paid", d: "M460 328 L570 328", label: { x: 515, y: 302, text: "Pay", w: 54 } },
  { id: "ship", color: colors.green, from: "paid", to: "shipped", d: "M820 328 L930 328", label: { x: 875, y: 302, text: "Ship", w: 58 } },
  { id: "deliver", color: colors.teal, from: "shipped", to: "delivered", d: "M1180 328 L1290 328", label: { x: 1235, y: 302, text: "Deliver", w: 78 } },
  { id: "cancelCreated", color: colors.purple, from: "created", to: "cancelled", d: "M335 405 L335 485 L500 485 L500 555", label: { x: 418, y: 458, text: "Cancel", w: 74 } },
  { id: "cancelPaid", color: colors.purple, from: "paid", to: "cancelled", d: "M695 405 L695 485 L580 485 L580 555", label: { x: 638, y: 458, text: "Cancel", w: 74 } },
  { id: "cancelRejected", color: colors.orange, from: "shipped", to: "reject", d: "M1055 405 L1055 570", label: { x: 1146, y: 488, text: "Cancel?", w: 78 } },
];

const SHIFT_X = 65;
for (const n of Object.values(nodes)) {
  n.x += SHIFT_X;
}
for (const e of edges) {
  let coord = 0;
  e.d = e.d.replace(/-?\d+(?:\.\d+)?/g, (value) => {
    const shifted = coord % 2 === 0 ? Number(value) + SHIFT_X : Number(value);
    coord += 1;
    return Number.isInteger(shifted) ? String(shifted) : String(shifted);
  });
  e.label.x += SHIFT_X;
}

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12 Z" fill="${color}"/></marker>`;
}

function stateNode(id, lines, final = false) {
  const n = nodes[id];
  const cx = n.x + n.w / 2;
  const badge = final ? `<rect class="badge" x="${n.x + n.w - 88}" y="${n.y + 16}" width="66" height="28" rx="8"/><text class="badgeText" x="${n.x + n.w - 55}" y="${n.y + 35}" text-anchor="middle">final</text>` : "";
  return `<g id="${id}"><rect class="card" x="${n.x}" y="${n.y}" width="${n.w}" height="${n.h}" rx="8" fill="${n.fill}" stroke="${n.stroke}"/>
  ${badge}
  <text class="stateTitle" x="${cx}" y="${n.y + 65}" text-anchor="middle">${esc(n.title)}</text>
  ${lines.map((l, i) => `<text class="detail" x="${cx}" y="${n.y + 100 + i * 22}" text-anchor="middle">${esc(l)}</text>`).join("\n  ")}</g>`;
}

function rejectNode() {
  const n = nodes.reject;
  return `<g id="reject"><rect class="note" x="${n.x}" y="${n.y}" width="${n.w}" height="${n.h}" rx="8" fill="${n.fill}" stroke="${n.stroke}"/>
  <text class="noteText" x="${n.x + n.w / 2}" y="${n.y + 40}" text-anchor="middle">${esc(n.title)}</text>
  <text class="detail" x="${n.x + n.w / 2}" y="${n.y + 66}" text-anchor="middle">test asserts this transition fails</text></g>`;
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
  if (b.w <= 24 && b.h <= 24) {
    const cx = b.x + b.w / 2;
    const cy = b.y + b.h / 2;
    return Math.hypot(p.x - cx, p.y - cy) <= 13.5;
  }
  const onX = p.x >= b.x - 0.1 && p.x <= b.x + b.w + 0.1;
  const onY = p.y >= b.y - 0.1 && p.y <= b.y + b.h + 0.1;
  return ((Math.abs(p.x - b.x) < 0.1 || Math.abs(p.x - (b.x + b.w)) < 0.1) && onY) ||
    ((Math.abs(p.y - b.y) < 0.1 || Math.abs(p.y - (b.y + b.h)) < 0.1) && onX);
}

function hits(s, b, pad = 8) {
  if (b.w <= 24 && b.h <= 24) return false;
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
  const ids = Object.keys(nodes);
  for (const e of edges) {
    const n = nums(e.d);
    const start = { x: n[0], y: n[1] };
    const end = { x: n[n.length - 2], y: n[n.length - 1] };
    if (!touches(nodes[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(nodes[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) {
      for (const id of ids) {
        if ((id === e.from || id === e.to) && (touches(nodes[id], s.a) || touches(nodes[id], s.b))) continue;
        if (hits(s, nodes[id])) throw new Error(`${e.id} crosses ${id}`);
      }
    }
  }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Order One-Way FSM">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.card,.note{filter:url(#softShadow);stroke-width:2}.stateTitle{font-family:"Architects Daughter";font-size:31px;fill:${colors.ink}}.detail,.noteText{font-family:"Comic Mono";font-size:14px;fill:${colors.muted}}.badge{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.2}.badgeText{font-family:"Comic Mono";font-size:11.5px;fill:${colors.muted}}.start{fill:${colors.ink}}.edge{fill:none;stroke-width:3.3;stroke-linecap:round;stroke-linejoin:round}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="1704" height="754" rx="8"/>
<text class="title" x="78" y="86">Order One-Way FSM</text>
<text class="subtitle" x="82" y="118">Orders move forward from creation to delivery. Cancellation is only allowed before shipping.</text>
<g id="edges">${edges.map((e) => `<path class="edge" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
<circle id="start" class="start" cx="${nodes.start.x + 12}" cy="${nodes.start.y + 12}" r="12"/>
${stateNode("created", ["initial state", "can pay or cancel"])}
${stateNode("paid", ["can ship or cancel", "refund path still open"])}
${stateNode("shipped", ["can only deliver", "cancel is rejected"])}
${stateNode("delivered", ["successful terminal state"], true)}
${stateNode("cancelled", ["terminal cancellation state"], true)}
${rejectNode()}
</svg>`;

for (const e of edges) {
  if (!svg.includes(`id="arrow-${e.id}"`) || !svg.includes(`fill="${e.color}"`)) throw new Error(`marker color ${e.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
