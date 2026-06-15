#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-workflow-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/utils-workflow-diagram-01.png";

const W = 1680;
const H = 980;

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
  teal: "#0D9488",
};

const cards = {
  dsl: { x: 90, y: 385, w: 340, h: 120, fill: "#EFF6FF", stroke: colors.blue, title: "Workflow DSL" },
  context: { x: 560, y: 220, w: 360, h: 128, fill: "#FFF7ED", stroke: colors.orange, title: "WorkContext" },
  work: { x: 560, y: 530, w: 360, h: 128, fill: "#F0FDF4", stroke: colors.green, title: "Work / SuspendWork" },
  flow: { x: 1100, y: 385, w: 390, h: 132, fill: "#FAF5FF", stroke: colors.purple, title: "Flow implementations" },
  report: { x: 560, y: 765, w: 360, h: 128, fill: "#F0FDFA", stroke: colors.teal, title: "WorkReport outcomes" },
};

const edges = [
  {
    id: "builds",
    color: colors.blue,
    from: "dsl",
    to: "flow",
    d: "M430 445 L1100 445",
    label: { x: 760, y: 421, text: "builds orchestration graph", w: 226 },
  },
  {
    id: "adds",
    color: colors.blue,
    from: "dsl",
    to: "work",
    d: "M430 505 L500 505 L500 594 L560 594",
    dashed: true,
    label: { x: 470, y: 560, text: "adds execute blocks", w: 176 },
  },
  {
    id: "contextInput",
    color: colors.orange,
    from: "context",
    to: "work",
    d: "M740 348 L740 530",
    label: { x: 846, y: 487, text: "input state", w: 106 },
  },
  {
    id: "stateVisible",
    color: colors.orange,
    from: "context",
    to: "flow",
    d: "M920 284 L1040 284 L1040 385 L1100 385",
    dashed: true,
    label: { x: 1035, y: 256, text: "shared across run", w: 156 },
  },
  {
    id: "dispatch",
    color: colors.green,
    from: "flow",
    to: "work",
    d: "M1100 505 L1010 505 L1010 594 L920 594",
    label: { x: 1014, y: 635, text: "dispatches units", w: 144 },
  },
  {
    id: "returns",
    color: colors.teal,
    from: "work",
    to: "report",
    d: "M740 658 L740 765",
    label: { x: 814, y: 717, text: "returns result", w: 124 },
  },
  {
    id: "controls",
    color: colors.purple,
    from: "report",
    to: "flow",
    d: "M920 829 L1040 829 L1040 517 L1100 517",
    dashed: true,
    label: { x: 1124, y: 678, text: "controls next step", w: 158 },
  },
];

function esc(text) {
  return String(text)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function card(id, lines, icon) {
  const c = cards[id];
  const cx = c.x + c.w / 2;
  const iconSvg = iconFor(icon, c.x + 24, c.y + 24, c.stroke);
  const titleX = id === "flow" ? cx + 34 : id === "work" || id === "report" ? cx + 22 : cx;
  const detail = lines
    .map((line, i) => `<text class="detail" x="${cx}" y="${c.y + 78 + i * 19}" text-anchor="middle">${esc(line)}</text>`)
    .join("\n");

  return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  ${iconSvg}
  <text class="cardTitle" x="${titleX}" y="${c.y + 40}" text-anchor="middle">${esc(c.title)}</text>
  ${detail}
</g>`;
}

function iconFor(kind, x, y, color) {
  if (kind === "dsl") {
    return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}">
    <path d="M22 4 C12 4 13 18 8 21 C13 24 12 38 22 38" fill="none"/>
    <path d="M42 4 C52 4 51 18 56 21 C51 24 52 38 42 38" fill="none"/>
    <circle cx="32" cy="21" r="4" fill="${color}" stroke="none"/>
  </g>`;
  }
  if (kind === "context") {
    return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}">
    <ellipse cx="31" cy="9" rx="25" ry="8" fill="#FFFFFF"/>
    <path d="M6 9 V35 C6 40 56 40 56 35 V9" fill="#FFFFFF"/>
    <path d="M6 22 C6 27 56 27 56 22" fill="none"/>
    <ellipse cx="31" cy="9" rx="25" ry="8" fill="none"/>
  </g>`;
  }
  if (kind === "work") {
    return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}">
    <rect x="5" y="6" width="52" height="36" rx="7" fill="#FFFFFF"/>
    <path d="M20 17 L13 24 L20 31" fill="none"/>
    <path d="M42 17 L49 24 L42 31" fill="none"/>
    <path d="M35 14 L27 34" fill="none"/>
  </g>`;
  }
  if (kind === "flow") {
    return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}">
    <circle cx="13" cy="22" r="8" fill="#FFFFFF"/>
    <circle cx="49" cy="10" r="8" fill="#FFFFFF"/>
    <circle cx="49" cy="34" r="8" fill="#FFFFFF"/>
    <path d="M21 22 H32 C38 22 37 10 41 10" fill="none"/>
    <path d="M21 22 H32 C38 22 37 34 41 34" fill="none"/>
  </g>`;
  }
  return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}">
    <rect x="6" y="7" width="50" height="36" rx="7" fill="#FFFFFF"/>
    <path d="M17 19 H45 M17 30 H38" fill="none"/>
    <circle cx="47" cy="31" r="5" fill="${color}" stroke="none"/>
  </g>`;
}

function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="20" markerHeight="16" refX="18" refY="8" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 20 16">
    <path d="M2 2 L18 8 L2 14 Z" fill="${color}"/>
  </marker>`;
}

function label({ x, y, text, w }) {
  return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 15})">
  <rect width="${w}" height="30" rx="8"/>
  <text x="${w / 2}" y="20" text-anchor="middle">${esc(text)}</text>
</g>`;
}

function pathToSegments(d) {
  const nums = d.match(/-?\d+(?:\.\d+)?/g).map(Number);
  const pts = [];
  for (let i = 0; i < nums.length; i += 2) pts.push({ x: nums[i], y: nums[i + 1] });
  return pts.slice(1).map((p, i) => ({ a: pts[i], b: p }));
}

function touches(cardBox, p) {
  const onX = p.x >= cardBox.x - 0.1 && p.x <= cardBox.x + cardBox.w + 0.1;
  const onY = p.y >= cardBox.y - 0.1 && p.y <= cardBox.y + cardBox.h + 0.1;
  const onLeftRight = (Math.abs(p.x - cardBox.x) < 0.1 || Math.abs(p.x - (cardBox.x + cardBox.w)) < 0.1) && onY;
  const onTopBottom = (Math.abs(p.y - cardBox.y) < 0.1 || Math.abs(p.y - (cardBox.y + cardBox.h)) < 0.1) && onX;
  return onLeftRight || onTopBottom;
}

function segmentIntersectsBox(seg, box, pad = 0) {
  const minX = Math.min(seg.a.x, seg.b.x);
  const maxX = Math.max(seg.a.x, seg.b.x);
  const minY = Math.min(seg.a.y, seg.b.y);
  const maxY = Math.max(seg.a.y, seg.b.y);
  const b = { x: box.x + pad, y: box.y + pad, w: box.w - pad * 2, h: box.h - pad * 2 };
  if (seg.a.x === seg.b.x) {
    return seg.a.x > b.x && seg.a.x < b.x + b.w && maxY > b.y && minY < b.y + b.h;
  }
  if (seg.a.y === seg.b.y) {
    return seg.a.y > b.y && seg.a.y < b.y + b.h && maxX > b.x && minX < b.x + b.w;
  }
  return false;
}

function validateGeometry() {
  const ids = Object.keys(cards);
  for (let i = 0; i < ids.length; i++) {
    for (let j = i + 1; j < ids.length; j++) {
      const a = cards[ids[i]];
      const b = cards[ids[j]];
      const overlap = a.x < b.x + b.w + 8 && a.x + a.w + 8 > b.x && a.y < b.y + b.h + 8 && a.y + a.h + 8 > b.y;
      if (overlap) throw new Error(`Card overlap: ${ids[i]} ${ids[j]}`);
    }
  }

  for (const edge of edges) {
    const points = edge.d.match(/-?\d+(?:\.\d+)?/g).map(Number);
    const start = { x: points[0], y: points[1] };
    const end = { x: points[points.length - 2], y: points[points.length - 1] };
    if (!touches(cards[edge.from], start)) throw new Error(`${edge.id} start does not touch ${edge.from}`);
    if (!touches(cards[edge.to], end)) throw new Error(`${edge.id} end does not touch ${edge.to}`);

    for (const seg of pathToSegments(edge.d)) {
      for (const id of ids) {
        if (id === edge.from || id === edge.to) continue;
        if (segmentIntersectsBox(seg, cards[id], 6)) throw new Error(`${edge.id} crosses ${id}`);
      }
    }
  }
}

validateGeometry();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Workflow Concept Overview">
<defs>
  <filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${edges.map((edge) => marker(edge.id, edge.color)).join("\n  ")}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:${colors.canvas}}
    .frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}
    .subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}
    .card{filter:url(#softShadow);stroke-width:1.9}
    .cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}
    .detail{font-family:"Comic Mono";font-size:13.5px;fill:${colors.muted}}
    .icon{stroke-width:2.3;stroke-linecap:round;stroke-linejoin:round}
    .edge{fill:none;stroke-width:3.3;stroke-linecap:round;stroke-linejoin:round}
    .dashed{stroke-dasharray:9 8}
    .edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}
    .edgeLabel text{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}
    .legend text{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}
  </style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="38" y="30" width="1604" height="914" rx="8"/>
<text class="title" x="78" y="86">Workflow Concept Overview</text>
<text class="subtitle" x="82" y="118">DSL builders compose sync and suspend work units; each unit receives shared context and returns a WorkReport that drives flow control.</text>

<g id="edges">
${edges
  .map(
    (edge) =>
      `  <path class="edge${edge.dashed ? " dashed" : ""}" data-from="${edge.from}" data-to="${edge.to}" d="${edge.d}" stroke="${edge.color}" marker-end="url(#arrow-${edge.id})"/>`,
  )
  .join("\n")}
</g>
<g id="labels">
${edges.map((edge) => `  ${label(edge.label)}`).join("\n")}
</g>

${card("dsl", ["workflow { ... }", "sequential, parallel, retry"], "dsl")}
${card("context", ["ConcurrentHashMap-backed state", "get, set, compute, merge"], "context")}
${card("work", ["execute(context): WorkReport", "sync Work and suspend Work"], "work")}
${card("flow", ["Sequential, Parallel, Conditional", "Repeat and Retry strategies"], "flow")}
${card("report", ["Success / Failure / PartialSuccess", "Aborted and Cancelled terminate"], "report")}

<g class="legend" transform="translate(94 900)">
  <line x1="0" y1="0" x2="34" y2="0" stroke="${colors.blue}" stroke-width="3.3" marker-end="url(#arrow-builds)"/>
  <text x="48" y="5">definition</text>
  <line x1="158" y1="0" x2="192" y2="0" stroke="${colors.orange}" stroke-width="3.3" marker-end="url(#arrow-contextInput)"/>
  <text x="206" y="5">context</text>
  <line x1="304" y1="0" x2="338" y2="0" stroke="${colors.green}" stroke-width="3.3" marker-end="url(#arrow-dispatch)"/>
  <text x="352" y="5">execution</text>
  <line x1="468" y1="0" x2="502" y2="0" stroke="${colors.purple}" stroke-width="3.3" stroke-dasharray="9 8" marker-end="url(#arrow-controls)"/>
  <text x="516" y="5">flow decision</text>
</g>
</svg>
`;

for (const edge of edges) {
  if (!svg.includes(`marker-end="url(#arrow-${edge.id})"`)) throw new Error(`Missing marker reference for ${edge.id}`);
  if (!svg.includes(`id="arrow-${edge.id}"`) || !svg.includes(`fill="${edge.color}"`)) {
    throw new Error(`Arrow marker color mismatch for ${edge.id}`);
  }
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
