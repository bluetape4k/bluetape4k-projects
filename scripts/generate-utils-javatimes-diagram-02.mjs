#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-javatimes-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/utils-javatimes-diagram-02.png";
const W = 2200;
const H = 1280;
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
  purple: "#9333EA",
  gray: "#64748B",
};

const cards = {
  iPeriod: { x: 900, y: 150, w: 400, h: 150, fill: "#F8FAFC", stroke: colors.gray, title: "ITimePeriod", kind: "interface", lines: ["+ start / end", "+ duration", "+ move(offset)", "+ relation helpers"] },
  timePeriod: { x: 900, y: 390, w: 400, h: 155, fill: "#EFF6FF", stroke: colors.blue, title: "TimePeriod", kind: "class", lines: ["- _start / _end", "+ setup(start, end)", "+ copy(offset)", "+ reset()"] },
  iBlock: { x: 210, y: 390, w: 400, h: 155, fill: "#ECFDF5", stroke: colors.green, title: "ITimeBlock", kind: "interface", lines: ["+ duration", "+ durationFromStart()", "+ durationFromEnd()"] },
  iRange: { x: 1590, y: 390, w: 400, h: 155, fill: "#F0FDFA", stroke: colors.teal, title: "ITimeRange", kind: "interface", lines: ["+ expandStartTo()", "+ expandEndTo()", "+ shrinkStartTo()", "+ shrinkEndTo()"] },
  timeBlock: { x: 210, y: 670, w: 400, h: 155, fill: "#ECFDF5", stroke: colors.green, title: "TimeBlock", kind: "class", lines: ["start + duration model", "updates end from duration", "readonly AnyTime support"] },
  timeRange: { x: 1590, y: 670, w: 400, h: 155, fill: "#F0FDFA", stroke: colors.teal, title: "TimeRange", kind: "class", lines: ["start + end model", "expand / shrink boundaries", "copy with offset"] },
  iContainer: { x: 900, y: 670, w: 400, h: 155, fill: "#FFF7ED", stroke: colors.orange, title: "ITimePeriodContainer", kind: "interface", lines: ["extends ITimePeriod", "MutableList<ITimePeriod>", "+ sortByStart/End", "+ containsPeriod()"] },
  container: { x: 900, y: 910, w: 400, h: 155, fill: "#FFF7ED", stroke: colors.orange, title: "TimePeriodContainer", kind: "class", lines: ["extends TimePeriod", "delegates MutableList", "start=min / end=max", "move() shifts all periods"] },
  iCollection: { x: 420, y: 1085, w: 360, h: 135, fill: "#FAF5FF", stroke: colors.purple, title: "ITimePeriodCollection", kind: "interface", lines: ["inside / overlap", "intersection / relation"] },
  iChain: { x: 1420, y: 1085, w: 360, h: 135, fill: "#FAF5FF", stroke: colors.purple, title: "ITimePeriodChain", kind: "interface", lines: ["head / last", "assertSpaceBefore/After"] },
};

const edges = [
  { id: "timePeriodImpl", type: "implements", color: colors.blue, from: "timePeriod", to: "iPeriod", d: "M1100 390 L1100 300", label: { x: 1180, y: 346, text: "implements", w: 106 } },
  { id: "iBlockExtends", type: "extends", color: colors.green, from: "iBlock", to: "iPeriod", d: "M410 390 L410 340 L980 340 L980 300", label: { x: 695, y: 317, text: "extends", w: 82 } },
  { id: "iRangeExtends", type: "extends", color: colors.teal, from: "iRange", to: "iPeriod", d: "M1790 390 L1790 340 L1220 340 L1220 300", label: { x: 1505, y: 317, text: "extends", w: 82 } },
  { id: "timeBlockExtends", type: "extends", color: colors.green, from: "timeBlock", to: "timePeriod", d: "M610 748 L760 748 L760 475 L900 475", label: { x: 760, y: 610, text: "extends", w: 82 } },
  { id: "timeBlockImpl", type: "implements", color: colors.green, from: "timeBlock", to: "iBlock", d: "M410 670 L410 545", label: { x: 500, y: 610, text: "implements", w: 106 } },
  { id: "timeRangeExtends", type: "extends", color: colors.teal, from: "timeRange", to: "timePeriod", d: "M1590 748 L1440 748 L1440 475 L1300 475", label: { x: 1440, y: 610, text: "extends", w: 82 } },
  { id: "timeRangeImpl", type: "implements", color: colors.teal, from: "timeRange", to: "iRange", d: "M1790 670 L1790 545", label: { x: 1888, y: 610, text: "implements", w: 106 } },
  { id: "containerImpl", type: "implements", color: colors.orange, from: "container", to: "iContainer", d: "M1100 910 L1100 825", label: { x: 1195, y: 868, text: "implements", w: 106 } },
  { id: "collectionExtends", type: "extends", color: colors.purple, from: "iCollection", to: "iContainer", d: "M600 1085 L600 870 L980 870 L980 825", label: { x: 700, y: 934, text: "extends", w: 82 } },
  { id: "chainExtends", type: "extends", color: colors.purple, from: "iChain", to: "iContainer", d: "M1600 1085 L1600 870 L1220 870 L1220 825", label: { x: 1500, y: 934, text: "extends", w: 82 } },
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

function card(id) {
  const c = cards[id];
  const cx = c.x + c.w / 2;
  const lines = c.lines.map((line, i) => `<text class="member" x="${c.x + 26}" y="${c.y + 88 + i * 20}">${esc(line)}</text>`).join("\n  ");
  return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  <text class="stereo" x="${cx}" y="${c.y + 30}" text-anchor="middle">&lt;&lt;${esc(c.kind)}&gt;&gt;</text>
  <text class="cardTitle" x="${cx}" y="${c.y + 58}" text-anchor="middle">${esc(c.title)}</text>
  <line class="divider" x1="${c.x + 18}" y1="${c.y + 72}" x2="${c.x + c.w - 18}" y2="${c.y + 72}"/>
  ${lines}
</g>`;
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
  for (const e of edges) {
    const n = nums(e.d);
    const start = { x: n[0], y: n[1] };
    const end = { x: n[n.length - 2], y: n[n.length - 1] };
    if (!touches(cards[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(cards[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) {
      for (const [id, c] of Object.entries(cards)) {
        if ((id === e.from || id === e.to) && (touches(c, s.a) || touches(c, s.b))) continue;
        if (hits(s, c)) throw new Error(`${e.id} crosses ${id}`);
      }
    }
  }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Period Framework Class Hierarchy">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.type, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.lane{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.4;stroke-dasharray:10 8}.laneTitle{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.card{filter:url(#softShadow);stroke-width:1.9}.stereo{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}.cardTitle{font-family:"Architects Daughter";font-size:23px;fill:${colors.ink}}.member{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.divider{stroke:${colors.line};stroke-width:1.2}.edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.implements{stroke-dasharray:9 8}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="2124" height="1214" rx="8"/>
<text class="title" x="78" y="88">Class Hierarchy — Period Framework</text>
<text class="subtitle" x="82" y="120">Single periods share TimePeriod behavior; containers reuse that period contract while managing many ITimePeriod instances.</text>
<rect class="lane" x="160" y="315" width="1880" height="560" rx="8"/><text class="laneTitle" x="188" y="344">single period model</text>
<rect class="lane" x="360" y="850" width="1480" height="380" rx="8"/><text class="laneTitle" x="388" y="878">container model</text>
<g id="edges">${edges.map((e) => `<path class="edge${e.type === "implements" ? " implements" : ""}" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
${Object.keys(cards).map(card).join("\n")}
</svg>`;

for (const e of edges) {
  if (!svg.includes(`id="arrow-${e.id}"`)) throw new Error(`missing marker ${e.id}`);
  if (!svg.includes(`stroke="${e.color}" stroke-width="2.4"`)) throw new Error(`marker color ${e.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
