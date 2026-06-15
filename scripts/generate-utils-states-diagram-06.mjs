#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-states-diagram-06.svg";
const pngPath = "docs/images/readme-diagrams/utils-states-diagram-06.png";
const W = 2160;
const H = 1040;
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
  amber: "#D97706",
  gray: "#64748B",
};

const nodes = {
  start: { x: 95, y: 415, w: 24, h: 24, fill: colors.ink, stroke: colors.ink, title: "start" },
  pending: { x: 165, y: 350, w: 230, h: 135, fill: "#EFF6FF", stroke: colors.blue, title: "PENDING" },
  requested: { x: 475, y: 350, w: 230, h: 135, fill: "#ECFDF5", stroke: colors.green, title: "REQUESTED" },
  confirmed: { x: 785, y: 350, w: 230, h: 135, fill: "#F0FDFA", stroke: colors.teal, title: "CONFIRMED" },
  checkedIn: { x: 1095, y: 350, w: 230, h: 135, fill: "#FFF7ED", stroke: colors.orange, title: "CHECKED_IN" },
  inProgress: { x: 1405, y: 350, w: 230, h: 135, fill: "#FAF5FF", stroke: colors.purple, title: "IN_PROGRESS" },
  completed: { x: 1715, y: 350, w: 230, h: 135, fill: "#FDF2F8", stroke: colors.pink, title: "COMPLETED" },
  cancelWindow: { x: 205, y: 585, w: 1080, h: 78, fill: "#FAF5FF", stroke: colors.purple, title: "Cancellation window" },
  cancelled: { x: 390, y: 775, w: 300, h: 135, fill: "#FAF5FF", stroke: colors.purple, title: "CANCELLED" },
  noShow: { x: 770, y: 160, w: 260, h: 125, fill: "#FFF7ED", stroke: colors.amber, title: "NO_SHOW" },
  reject: { x: 1360, y: 775, w: 390, h: 135, fill: "#F8FAFC", stroke: colors.gray, title: "IN_PROGRESS rejects Cancel" },
};

const edges = [
  { id: "startPending", color: colors.gray, from: "start", to: "pending", d: "M119 427 L165 427", label: { x: 142, y: 402, text: "initial", w: 72 } },
  { id: "request", color: colors.blue, from: "pending", to: "requested", d: "M395 418 L475 418", label: { x: 435, y: 392, text: "Request", w: 84 } },
  { id: "confirm", color: colors.green, from: "requested", to: "confirmed", d: "M705 418 L785 418", label: { x: 745, y: 392, text: "Confirm", w: 84 } },
  { id: "checkIn", color: colors.teal, from: "confirmed", to: "checkedIn", d: "M1015 418 L1095 418", label: { x: 1055, y: 392, text: "CheckIn", w: 86 } },
  { id: "startTreatment", color: colors.orange, from: "checkedIn", to: "inProgress", d: "M1325 418 L1405 418", label: { x: 1365, y: 392, text: "Start", w: 62 } },
  { id: "complete", color: colors.pink, from: "inProgress", to: "completed", d: "M1635 418 L1715 418", label: { x: 1675, y: 392, text: "Complete", w: 90 } },
  { id: "cancelPending", color: colors.purple, from: "pending", to: "cancelWindow", d: "M280 485 L280 585" },
  { id: "cancelRequested", color: colors.purple, from: "requested", to: "cancelWindow", d: "M590 485 L590 585" },
  { id: "cancelConfirmed", color: colors.purple, from: "confirmed", to: "cancelWindow", d: "M900 485 L900 585" },
  { id: "cancelChecked", color: colors.purple, from: "checkedIn", to: "cancelWindow", d: "M1210 485 L1210 585" },
  { id: "cancelFinal", color: colors.purple, from: "cancelWindow", to: "cancelled", d: "M540 663 L540 775", label: { x: 640, y: 724, text: "Cancel -> final", w: 124 } },
  { id: "markNoShow", color: colors.amber, from: "confirmed", to: "noShow", d: "M900 350 L900 285", label: { x: 780, y: 315, text: "MarkNoShow", w: 118 } },
  { id: "rejectCancel", color: colors.gray, from: "inProgress", to: "reject", d: "M1520 485 L1520 775", label: { x: 1625, y: 680, text: "Cancel?", w: 78 } },
];

const SHIFT_X = 60;
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
  if (e.label) {
    e.label.x += SHIFT_X;
  }
}

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12 Z" fill="${color}"/></marker>`;
}

function node(id, lines, final = false) {
  const n = nodes[id];
  const cx = n.x + n.w / 2;
  const badge = final ? `<rect class="badge" x="${n.x + n.w - 86}" y="${n.y + 14}" width="64" height="26" rx="8"/><text class="badgeText" x="${n.x + n.w - 54}" y="${n.y + 32}" text-anchor="middle">final</text>` : "";
  return `<g id="${id}"><rect class="card" x="${n.x}" y="${n.y}" width="${n.w}" height="${n.h}" rx="8" fill="${n.fill}" stroke="${n.stroke}"/>
  ${badge}
  <text class="stateTitle" x="${cx}" y="${n.y + 58}" text-anchor="middle">${esc(n.title)}</text>
  ${lines.map((l, i) => `<text class="detail" x="${cx}" y="${n.y + 90 + i * 21}" text-anchor="middle">${esc(l)}</text>`).join("\n  ")}</g>`;
}

function cancelWindow() {
  const n = nodes.cancelWindow;
  const cx = n.x + n.w / 2;
  return `<g id="cancelWindow"><rect class="band" x="${n.x}" y="${n.y}" width="${n.w}" height="${n.h}" rx="8" fill="${n.fill}" stroke="${n.stroke}"/>
  <text class="bandTitle" x="${cx}" y="${n.y + 34}" text-anchor="middle">${esc(n.title)}</text>
  <text class="detail" x="${cx}" y="${n.y + 58}" text-anchor="middle">Cancel is accepted from PENDING, REQUESTED, CONFIRMED, and CHECKED_IN</text></g>`;
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

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Appointment Complex FSM">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.card,.band{filter:url(#softShadow);stroke-width:2}.band{fill-opacity:.72}.stateTitle{font-family:"Architects Daughter";font-size:25px;fill:${colors.ink}}.bandTitle{font-family:"Architects Daughter";font-size:22px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:13.2px;fill:${colors.muted}}.badge{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.2}.badgeText{font-family:"Comic Mono";font-size:11.5px;fill:${colors.muted}}.start{fill:${colors.ink}}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="2084" height="974" rx="8"/>
<text class="title" x="78" y="86">Appointment Complex FSM</text>
<text class="subtitle" x="82" y="118">A coroutine FSM for appointment intake, confirmation, check-in, treatment, terminal outcomes, and cancellation boundaries.</text>
<g id="edges">${edges.map((e) => `<path class="edge" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.filter((e) => e.label).map((e) => label(e.label)).join("\n")}</g>
<circle id="start" class="start" cx="${nodes.start.x + 12}" cy="${nodes.start.y + 12}" r="12"/>
${node("pending", ["initial request slot", "Cancel allowed"])}
${node("requested", ["waiting confirmation", "Cancel allowed"])}
${node("confirmed", ["can check in", "No-show or cancel"])}
${node("checkedIn", ["patient arrived", "Cancel still allowed"])}
${node("inProgress", ["treatment started", "Cancel rejected"])}
${node("completed", ["normal terminal outcome"], true)}
${cancelWindow()}
${node("cancelled", ["terminal cancellation", "from early states"], true)}
${node("noShow", ["via MarkNoShow", "from CONFIRMED"], true)}
${node("reject", ["assertRejects(Cancel)", "after treatment starts"])}
</svg>`;

for (const e of edges) {
  if (!svg.includes(`id="arrow-${e.id}"`) || !svg.includes(`fill="${e.color}"`)) throw new Error(`marker color ${e.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
