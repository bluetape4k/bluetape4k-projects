#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-states-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/utils-states-diagram-02.png";
const W = 2040;
const H = 1400;
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

const cards = {
  base: { x: 820, y: 145, w: 400, h: 172, fill: "#F8FAFC", stroke: colors.gray, title: "BaseStateMachine<S, E>", kind: "interface" },
  syncApi: { x: 115, y: 420, w: 440, h: 150, fill: "#EFF6FF", stroke: colors.blue, title: "StateMachine<S, E>", kind: "interface" },
  suspendApi: { x: 780, y: 420, w: 480, h: 150, fill: "#ECFDF5", stroke: colors.green, title: "SuspendStateMachineInterface<S, E>", kind: "interface" },
  reactiveApi: { x: 1485, y: 420, w: 440, h: 150, fill: "#F0FDFA", stroke: colors.teal, title: "ReactiveStateMachine<S, E, F>", kind: "interface" },
  syncImpl: { x: 115, y: 675, w: 440, h: 150, fill: "#EFF6FF", stroke: colors.blue, title: "DefaultStateMachine<S, E>", kind: "class" },
  suspendImpl: { x: 800, y: 675, w: 440, h: 150, fill: "#ECFDF5", stroke: colors.green, title: "SuspendStateMachine<S, E>", kind: "class" },
  reactiveImpl: { x: 1485, y: 675, w: 440, h: 150, fill: "#F0FDFA", stroke: colors.teal, title: "DefaultReactiveStateMachine<S, E, F>", kind: "class" },
  registry: { x: 800, y: 925, w: 440, h: 142, fill: "#FAF5FF", stroke: colors.purple, title: "TransitionRegistry<S, E>", kind: "internal class" },
  key: { x: 90, y: 1150, w: 360, h: 145, fill: "#FFF7ED", stroke: colors.orange, title: "TransitionKey<S, E>", kind: "data class" },
  parentKey: { x: 545, y: 1150, w: 360, h: 145, fill: "#FFF7ED", stroke: colors.orange, title: "ParentTransitionKey<S, E>", kind: "data class" },
  target: { x: 1000, y: 1150, w: 360, h: 145, fill: "#FFF7ED", stroke: colors.orange, title: "TransitionTarget<S, E>", kind: "data class" },
  result: { x: 1455, y: 1150, w: 360, h: 145, fill: "#FDF2F8", stroke: colors.pink, title: "TransitionResult<S, E>", kind: "data class" },
};

const edges = [
  { id: "syncBase", type: "extends", color: colors.blue, from: "syncApi", to: "base", d: "M335 420 L335 360 L920 360 L920 317", label: { x: 628, y: 338, text: "extends", w: 82 } },
  { id: "suspendBase", type: "extends", color: colors.green, from: "suspendApi", to: "base", d: "M1020 420 L1020 317", label: { x: 1090, y: 372, text: "extends", w: 82 } },
  { id: "reactiveBase", type: "extends", color: colors.teal, from: "reactiveApi", to: "base", d: "M1705 420 L1705 360 L1120 360 L1120 317", label: { x: 1412, y: 338, text: "extends", w: 82 } },
  { id: "syncImplApi", type: "implements", color: colors.blue, from: "syncImpl", to: "syncApi", d: "M335 675 L335 570", label: { x: 420, y: 624, text: "implements", w: 106 } },
  { id: "suspendImplApi", type: "implements", color: colors.green, from: "suspendImpl", to: "suspendApi", d: "M1020 675 L1020 570", label: { x: 1112, y: 624, text: "implements", w: 106 } },
  { id: "reactiveImplApi", type: "implements", color: colors.teal, from: "reactiveImpl", to: "reactiveApi", d: "M1705 675 L1705 570", label: { x: 1800, y: 624, text: "implements", w: 106 } },
  { id: "syncRegistry", type: "has", color: colors.blue, from: "syncImpl", to: "registry", d: "M335 825 L335 875 L910 875 L910 925", label: { x: 622, y: 852, text: "has registry", w: 112 } },
  { id: "suspendRegistry", type: "has", color: colors.green, from: "suspendImpl", to: "registry", d: "M1020 825 L1020 925", label: { x: 1114, y: 874, text: "has registry", w: 112 } },
  { id: "reactiveRegistry", type: "has", color: colors.teal, from: "reactiveImpl", to: "registry", d: "M1705 825 L1705 875 L1130 875 L1130 925", label: { x: 1418, y: 852, text: "has registry", w: 112 } },
  { id: "registryKey", type: "has", color: colors.purple, from: "registry", to: "key", d: "M895 1067 L895 1125 L270 1125 L270 1150", label: { x: 582, y: 1104, text: "exact", w: 64 } },
  { id: "registryParent", type: "has", color: colors.purple, from: "registry", to: "parentKey", d: "M980 1067 L980 1137 L725 1137 L725 1150", label: { x: 852, y: 1124, text: "parent", w: 76 } },
  { id: "registryTarget", type: "has", color: colors.purple, from: "registry", to: "target", d: "M1100 1067 L1100 1125 L1180 1125 L1180 1150", label: { x: 1140, y: 1104, text: "target", w: 72 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, type, color) {
  if (type === "extends" || type === "implements") {
    return `<marker id="arrow-${id}" markerWidth="22" markerHeight="18" refX="20" refY="9" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 22 18"><path d="M2 2 L20 9 L2 16 Z" fill="#FFFFFF" stroke="${color}" stroke-width="2.4"/></marker>`;
  }
  return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12" fill="none" stroke="${color}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/></marker>`;
}

function card(id, members) {
  const c = cards[id];
  const cx = c.x + c.w / 2;
  const memberLines = members.map((line, i) => `<text class="member" x="${c.x + 26}" y="${c.y + 88 + i * 20}">${esc(line)}</text>`).join("\n  ");
  return `<g id="${id}"><rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  <text class="stereo" x="${cx}" y="${c.y + 30}" text-anchor="middle">&lt;&lt;${esc(c.kind)}&gt;&gt;</text>
  <text class="cardTitle" x="${cx}" y="${c.y + 58}" text-anchor="middle">${esc(c.title)}</text>
  <line class="divider" x1="${c.x + 18}" y1="${c.y + 72}" x2="${c.x + c.w - 18}" y2="${c.y + 72}"/>
  ${memberLines}</g>`;
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

function crosses(a, b) {
  const av = a.a.x === a.b.x;
  const bv = b.a.x === b.b.x;
  if (av === bv) return false;
  const v = av ? a : b;
  const h = av ? b : a;
  const x = v.a.x;
  const y = h.a.y;
  const crossing = x > Math.min(h.a.x, h.b.x) && x < Math.max(h.a.x, h.b.x) &&
    y > Math.min(v.a.y, v.b.y) && y < Math.max(v.a.y, v.b.y);
  return crossing && ![a.a, a.b, b.a, b.b].some((p) => p.x === x && p.y === y);
}

function validate() {
  const ids = Object.keys(cards);
  for (const e of edges) {
    const n = nums(e.d);
    const start = { x: n[0], y: n[1] };
    const end = { x: n[n.length - 2], y: n[n.length - 1] };
    if (!touches(cards[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(cards[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) {
      for (const id of ids) {
        if ((id === e.from || id === e.to) && (touches(cards[id], s.a) || touches(cards[id], s.b))) continue;
        if (hits(s, cards[id])) throw new Error(`${e.id} crosses ${id}`);
      }
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

const members = {
  base: ["+ currentState: S", "+ initialState: S", "+ finalStates: Set<S>", "+ canTransition(event): Boolean", "+ allowedEvents(): Set<Class<out E>>"],
  syncApi: ["+ transition(event): TransitionResult", "Thread-safe via AtomicReference CAS"],
  suspendApi: ["+ suspend transition(event): TransitionResult", "+ stateFlow: StateFlow<S>", "Serialized with Mutex"],
  reactiveApi: ["+ send(event): TransitionResult", "+ stateFlow: StateFlow<S>", "+ effects: Flow<F>", "+ close()"],
  syncImpl: ["- currentState: AtomicReference<S>", "- registry: TransitionRegistry", "throws on CAS collision"],
  suspendImpl: ["- mutex: Mutex", "- stateFlow: MutableStateFlow<S>", "- registry: TransitionRegistry"],
  reactiveImpl: ["- mutex: Mutex", "- effects: MutableSharedFlow<F>", "- sideEffects + lifecycle Job"],
  registry: ["+ resolve(state, event): TransitionMatch?", "+ allowedEvents(state): Set<Class<out E>>", "exact transitions win before parent matches"],
  key: ["state + eventType"],
  parentKey: ["stateType + eventType", "nested state-family match"],
  target: ["state", "guard: ((S, E) -> Boolean)?"],
  result: ["previousState", "event", "currentState"],
};

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="States Class Structure">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.type, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.card{filter:url(#softShadow);stroke-width:1.9}.stereo{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}.cardTitle{font-family:"Architects Daughter";font-size:23px;fill:${colors.ink}}.member{font-family:"Comic Mono";font-size:13.2px;fill:${colors.muted}}.divider{stroke:${colors.line};stroke-width:1.2}.edge{fill:none;stroke-width:3.0;stroke-linecap:round;stroke-linejoin:round}.implements{stroke-dasharray:9 8}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="1964" height="1334" rx="8"/>
<text class="title" x="78" y="86">States Class Structure</text>
<text class="subtitle" x="82" y="118">Sync, suspend, and reactive machines share the read-only FSM contract while keeping different transition APIs and concurrency models.</text>
<g id="edges">${edges.map((e) => `<path class="edge${e.type === "implements" ? " implements" : ""}" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
${Object.keys(cards).map((id) => card(id, members[id])).join("\n")}
</svg>`;

for (const e of edges) {
  if (!svg.includes(`id="arrow-${e.id}"`)) throw new Error(`missing marker ${e.id}`);
  if ((e.type === "has" && !svg.includes(`stroke="${e.color}" stroke-width="2.4"`)) ||
      ((e.type === "extends" || e.type === "implements") && !svg.includes(`stroke="${e.color}" stroke-width="2.4"`))) {
    throw new Error(`marker color ${e.id}`);
  }
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
