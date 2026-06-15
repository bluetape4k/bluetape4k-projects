#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-states-diagram-03.svg";
const pngPath = "docs/images/readme-diagrams/utils-states-diagram-03.png";
const W = 1840;
const H = 1020;
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

const boxes = {
  code: { x: 90, y: 175, w: 640, h: 705, fill: "#F8FAFC", stroke: colors.gray, title: "DSL usage sketch" },
  entry: { x: 880, y: 185, w: 780, h: 150, fill: "#EFF6FF", stroke: colors.blue, title: "Entry functions" },
  state: { x: 880, y: 395, w: 360, h: 150, fill: "#ECFDF5", stroke: colors.green, title: "Machine settings" },
  transitions: { x: 1300, y: 395, w: 360, h: 150, fill: "#FFF7ED", stroke: colors.orange, title: "Transition registrations" },
  validate: { x: 880, y: 670, w: 360, h: 145, fill: "#FAF5FF", stroke: colors.purple, title: "Build-time validation" },
  runtime: { x: 1300, y: 670, w: 360, h: 145, fill: "#FDF2F8", stroke: colors.pink, title: "Runtime machine" },
};

const edges = [
  { id: "codeEntry", color: colors.blue, from: "code", to: "entry", d: "M730 250 L880 250", label: { x: 805, y: 224, text: "calls", w: 60 } },
  { id: "entryState", color: colors.green, from: "entry", to: "state", d: "M1055 335 L1055 395", label: { x: 1126, y: 365, text: "apply block", w: 100 } },
  { id: "entryTransitions", color: colors.orange, from: "entry", to: "transitions", d: "M1480 335 L1480 395", label: { x: 1568, y: 365, text: "collect rules", w: 110 } },
  { id: "stateValidate", color: colors.green, from: "state", to: "validate", d: "M1060 545 L1060 670", label: { x: 1136, y: 607, text: "known states", w: 112 } },
  { id: "transitionsValidate", color: colors.orange, from: "transitions", to: "validate", d: "M1480 545 L1480 600 L1160 600 L1160 670", label: { x: 1320, y: 574, text: "exact + parent", w: 118 } },
  { id: "validateRuntime", color: colors.purple, from: "validate", to: "runtime", d: "M1240 742 L1300 742", label: { x: 1270, y: 640, text: "freezes maps", w: 108 } },
];

const codeLines = [
  { indent: 0, tokens: [["fn", "stateMachine"], ["op", "<"], ["type", "OrderState"], ["op", ", "], ["type", "OrderEvent"], ["op", "> {" ]] },
  { indent: 1, tokens: [["prop", "initialState"], ["op", " = "], ["state", "Created"]] },
  { indent: 1, tokens: [["prop", "finalStates"], ["op", " = "], ["fn", "setOf"], ["op", "("], ["state", "Delivered"], ["op", ", "], ["state", "Cancelled"], ["op", ")"]] },
  { indent: 0, tokens: [] },
  { indent: 1, tokens: [["fn", "transition"], ["op", "("], ["state", "Created"], ["op", ", "], ["fn", "on"], ["op", "<"], ["type", "Pay"], ["op", ">(), "], ["param", "to"], ["op", " = "], ["state", "Paid"], ["op", ")"]] },
  { indent: 1, tokens: [["fn", "transition"], ["op", "("], ["fn", "state"], ["op", "<"], ["type", "ActiveOrder"], ["op", ">(), "], ["fn", "on"], ["op", "<"], ["type", "Cancel"], ["op", ">(), "], ["param", "to"], ["op", " = "], ["state", "Cancelled"], ["op", ") {"]] },
  { indent: 2, tokens: [["fn", "guard"], ["op", " { "], ["param", "state"], ["op", ", "], ["param", "event"], ["op", " -> "], ["param", "event"], ["op", "."], ["prop", "allowed"], ["op", " }"]] },
  { indent: 1, tokens: [["op", "}"]] },
  { indent: 0, tokens: [] },
  { indent: 1, tokens: [["fn", "onTransition"], ["op", " { "], ["param", "previous"], ["op", ", "], ["param", "event"], ["op", ", "], ["param", "next"], ["op", " ->"]] },
  { indent: 2, tokens: [["fn", "audit"], ["op", "("], ["param", "previous"], ["op", ", "], ["param", "event"], ["op", ", "], ["param", "next"], ["op", ")"]] },
  { indent: 1, tokens: [["op", "}"]] },
  { indent: 0, tokens: [["op", "}"]] },
  { indent: 0, tokens: [] },
  { indent: 0, note: "suspendStateMachine { ... } uses the same builder shape" },
  { indent: 0, note: "reactiveStateMachine { ... } adds effects and onState side effects" },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12 Z" fill="${color}"/></marker>`;
}

function card(id, lines) {
  const b = boxes[id];
  const cx = b.x + b.w / 2;
  return `<g id="${id}"><rect class="card" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/>
  <text class="cardTitle" x="${cx}" y="${b.y + 42}" text-anchor="middle">${esc(b.title)}</text>
  ${lines.map((l, i) => `<text class="detail" x="${cx}" y="${b.y + 78 + i * 22}" text-anchor="middle">${esc(l)}</text>`).join("\n  ")}</g>`;
}

function codePanel() {
  const b = boxes.code;
  const lines = codeLines.map((line, i) => {
    const y = b.y + 102 + i * 34;
    if (line.note) {
      return `<text class="code note" x="${b.x + 34}" y="${y}">${esc(line.note)}</text>`;
    }
    if (line.tokens.length === 0) {
      return `<text class="code mutedCode" x="${b.x + 34}" y="${y}"></text>`;
    }
    const x = b.x + 34 + line.indent * 34;
    const tokens = line.tokens.map(([cls, text]) => `<tspan class="${cls}">${esc(text)}</tspan>`).join("");
    return `<text class="code" x="${x}" y="${y}">${tokens}</text>`;
  }).join("\n  ");
  return `<g id="code"><rect class="card" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/>
  <text class="cardTitle" x="${b.x + 34}" y="${b.y + 48}">${esc(b.title)}</text>
  <line class="divider" x1="${b.x + 28}" y1="${b.y + 68}" x2="${b.x + b.w - 28}" y2="${b.y + 68}"/>
  ${lines}</g>`;
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
  const ids = Object.keys(boxes);
  for (const e of edges) {
    const n = nums(e.d);
    const start = { x: n[0], y: n[1] };
    const end = { x: n[n.length - 2], y: n[n.length - 1] };
    if (!touches(boxes[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(boxes[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) {
      for (const id of ids) {
        if ((id === e.from || id === e.to) && (touches(boxes[id], s.a) || touches(boxes[id], s.b))) continue;
        if (hits(s, boxes[id])) throw new Error(`${e.id} crosses ${id}`);
      }
    }
  }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="DSL Builder Structure">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.card{filter:url(#softShadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:25px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:14px;fill:${colors.muted}}.divider{stroke:${colors.line};stroke-width:1.2}.code{font-family:"Comic Mono";font-size:15px;fill:${colors.ink}}.fn{fill:${colors.blue}}.type{fill:${colors.purple}}.prop{fill:${colors.green}}.state{fill:${colors.orange}}.param{fill:${colors.teal}}.op{fill:${colors.ink}}.note{fill:${colors.muted};font-size:13px}.mutedCode{fill:${colors.muted}}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="1764" height="954" rx="8"/>
<text class="title" x="78" y="86">DSL Builder Structure</text>
<text class="subtitle" x="82" y="118">The DSL reads like a state-machine declaration, then builders freeze typed transition maps into the selected runtime.</text>
<g id="edges">${edges.map((e) => `<path class="edge" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
${codePanel()}
${card("entry", ["stateMachine { ... }", "suspendStateMachine { ... }", "reactiveStateMachine(scope) { ... }"])}
${card("state", ["initialState", "finalStates", "onTransition callback"])}
${card("transitions", ["transition(from, on<E>(), to)", "transition(state<Parent>(), on<E>(), to)", "guard { ... } / effect { ... }"])}
${card("validate", ["collect known states", "reject ambiguous parent matches", "copy maps before runtime"])}
${card("runtime", ["DefaultStateMachine", "SuspendStateMachine", "DefaultReactiveStateMachine"])}
</svg>`;

for (const e of edges) {
  if (!svg.includes(`id="arrow-${e.id}"`) || !svg.includes(`fill="${e.color}"`)) throw new Error(`marker color ${e.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
