#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-states-diagram-04.svg";
const pngPath = "docs/images/readme-diagrams/utils-states-diagram-04.png";
const W = 1540;
const H = 780;
const colors = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  blue: "#2563EB",
  green: "#16A34A",
  orange: "#EA580C",
  purple: "#9333EA",
  gray: "#64748B",
};

const nodes = {
  start: { x: 150, y: 420, w: 24, h: 24, fill: colors.ink, stroke: colors.ink, title: "start" },
  locked: { x: 285, y: 330, w: 370, h: 190, fill: "#EFF6FF", stroke: colors.blue, title: "LOCKED" },
  unlocked: { x: 930, y: 330, w: 370, h: 190, fill: "#ECFDF5", stroke: colors.green, title: "UNLOCKED" },
  note: { x: 550, y: 610, w: 440, h: 76, fill: "#FAF5FF", stroke: colors.purple, title: "No final states: transitions can continue forever" },
};

const edges = [
  { id: "startLocked", color: colors.gray, from: "start", to: "locked", d: "M174 432 L285 432", label: { x: 230, y: 407, text: "initial", w: 72 } },
  { id: "coinUnlock", color: colors.blue, from: "locked", to: "unlocked", d: "M655 380 L930 380", label: { x: 792, y: 353, text: "Coin", w: 58 } },
  { id: "pushLock", color: colors.green, from: "unlocked", to: "locked", d: "M930 470 L655 470", label: { x: 792, y: 503, text: "Push", w: 58 } },
  { id: "coinStay", color: colors.orange, from: "unlocked", to: "unlocked", d: "M1115 330 L1115 215 L1355 215 L1355 425 L1300 425", label: { x: 1235, y: 189, text: "Coin again", w: 98 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12 Z" fill="${color}"/></marker>`;
}

function stateNode(id, lines) {
  const n = nodes[id];
  const cx = n.x + n.w / 2;
  return `<g id="${id}"><rect class="card" x="${n.x}" y="${n.y}" width="${n.w}" height="${n.h}" rx="8" fill="${n.fill}" stroke="${n.stroke}"/>
  <text class="stateTitle" x="${cx}" y="${n.y + 70}" text-anchor="middle">${esc(n.title)}</text>
  ${lines.map((l, i) => `<text class="detail" x="${cx}" y="${n.y + 112 + i * 24}" text-anchor="middle">${esc(l)}</text>`).join("\n  ")}</g>`;
}

function noteNode() {
  const n = nodes.note;
  return `<g id="note"><rect class="note" x="${n.x}" y="${n.y}" width="${n.w}" height="${n.h}" rx="8" fill="${n.fill}" stroke="${n.stroke}"/>
  <text class="noteText" x="${n.x + n.w / 2}" y="${n.y + 46}" text-anchor="middle">${esc(n.title)}</text></g>`;
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

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Turnstile Simple FSM">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.card,.note{filter:url(#softShadow);stroke-width:2}.stateTitle{font-family:"Architects Daughter";font-size:36px;fill:${colors.ink}}.detail,.noteText{font-family:"Comic Mono";font-size:15px;fill:${colors.muted}}.start{fill:${colors.ink}}.edge{fill:none;stroke-width:3.4;stroke-linecap:round;stroke-linejoin:round}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="1464" height="714" rx="8"/>
<text class="title" x="78" y="86">Turnstile Simple FSM</text>
<text class="subtitle" x="82" y="118">A two-state machine: a coin unlocks the gate, a push locks it again, and extra coins keep it unlocked.</text>
<g id="edges">${edges.map((e) => `<path class="edge" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
<circle id="start" class="start" cx="${nodes.start.x + 12}" cy="${nodes.start.y + 12}" r="12"/>
${stateNode("locked", ["initial state", "waits for Coin"])}
${stateNode("unlocked", ["allows Push", "extra Coin is ignored"])}
${noteNode()}
</svg>`;

for (const e of edges) {
  if (!svg.includes(`id="arrow-${e.id}"`) || !svg.includes(`fill="${e.color}"`)) throw new Error(`marker color ${e.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
