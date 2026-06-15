#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/mutiny/README.md",
  "utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/UniSupport.kt",
  "utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/MultiSupport.kt",
  "utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/CoroutineSupport.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Mutiny Type Diagram[\s\S]*utils-mutiny-diagram-01\.png/, "README type diagram slot");
assertContains(sources[1], /voidUni[\s\S]*nullUni[\s\S]*uniOf[\s\S]*CompletionStage<T>\.asUni[\s\S]*Future<T>\.asUni/, "Uni helper surface");
assertContains(sources[2], /multiOf[\s\S]*multiRangeOf[\s\S]*Iterable<T>\.asMulti[\s\S]*Sequence<T>\.asMulti[\s\S]*MultiRepetition\.deferUni/, "Multi helper surface");
assertContains(sources[3], /CoroutineScope\.asUni[\s\S]*async[\s\S]*asUni\(\)/, "Coroutine interop surface");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  slate: ["#F8FAFC", "#64748B", "#475569"],
  violet: ["#F5F3FF", "#7C3AED", "#6D28D9"],
};

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function markerDefs() {
  return Object.entries(palette).map(([name, [, , dark]]) => `
  <marker id="arrow-${name}" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto" markerUnits="strokeWidth"><path d="M .9 .9 L 7 4 L .9 7.1 Z" fill="${dark}"/></marker>`).join("\n");
}

function card({ id, x, y, w, h, color, kicker, title, lines = [], footer = "" }) {
  const [fill, stroke, dark] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="kicker" x="${x + 22}" y="${y + 31}">${esc(kicker)}</text>
  <text class="cardTitle" x="${x + 22}" y="${y + 65}">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 86}H${x + w}" stroke="${dark}"/>
  ${lines.map((line, index) => `<text class="body" x="${x + 22}" y="${y + 118 + index * 24}">${esc(line)}</text>`).join("\n")}
  ${footer ? `<path class="divider" d="M${x} ${y + h - 46}H${x + w}" stroke="${dark}"/><text class="foot" x="${x + 22}" y="${y + h - 17}">${esc(footer)}</text>` : ""}
</g>`;
}

function edge({ from, to, points, color, dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0]}" y="${p[1]}">${esc(label)}</text>` : ""}
</g>`;
}

function validateNoCardOverlap(cards, minGap = 18) {
  for (let i = 0; i < cards.length; i++) {
    for (let j = i + 1; j < cards.length; j++) {
      const a = cards[i];
      const b = cards[j];
      const separated =
        a.x + a.w + minGap <= b.x ||
        b.x + b.w + minGap <= a.x ||
        a.y + a.h + minGap <= b.y ||
        b.y + b.h + minGap <= a.y;
      if (!separated) throw new Error(`Card overlap or insufficient gap: ${a.id} vs ${b.id}`);
    }
  }
}

function validateEndpointTouchesCard(point, card, edgeName, sideName, tolerance = 6) {
  const [x, y] = point;
  const onLeft = Math.abs(x - card.x) <= tolerance && y >= card.y - tolerance && y <= card.y + card.h + tolerance;
  const onRight = Math.abs(x - (card.x + card.w)) <= tolerance && y >= card.y - tolerance && y <= card.y + card.h + tolerance;
  const onTop = Math.abs(y - card.y) <= tolerance && x >= card.x - tolerance && x <= card.x + card.w + tolerance;
  const onBottom = Math.abs(y - (card.y + card.h)) <= tolerance && x >= card.x - tolerance && x <= card.x + card.w + tolerance;
  if (!(onLeft || onRight || onTop || onBottom)) {
    throw new Error(`${edgeName} ${sideName} endpoint does not touch ${card.id}: (${x}, ${y})`);
  }
}

function validateEdgeEndpoints(edgeSpecs, cards) {
  const byId = Object.fromEntries(cards.map((card) => [card.id, card]));
  for (const edgeSpec of edgeSpecs) {
    validateEndpointTouchesCard(edgeSpec.points[0], byId[edgeSpec.from], `${edgeSpec.from}->${edgeSpec.to}`, "source");
    validateEndpointTouchesCard(edgeSpec.points[edgeSpec.points.length - 1], byId[edgeSpec.to], `${edgeSpec.from}->${edgeSpec.to}`, "target");
  }
}

const width = 2600;
const height = 1540;
const validationCards = [
  { id: "Mutiny", x: 770, y: 205, w: 980, h: 215 },
  { id: "Uni", x: 150, y: 555, w: 600, h: 260 },
  { id: "Multi", x: 1850, y: 555, w: 600, h: 260 },
  { id: "UniSupport", x: 150, y: 955, w: 600, h: 265 },
  { id: "CoroutineSupport", x: 930, y: 955, w: 650, h: 265 },
  { id: "MultiSupport", x: 1850, y: 955, w: 600, h: 265 },
];
const body = [
  card({
    id: "Mutiny",
    x: 770,
    y: 205,
    w: 980,
    h: 215,
    color: "slate",
    kicker: "SmallRye Mutiny",
    title: "Two primary reactive types",
    lines: ["Uni models at most one item", "Multi models a stream of items", "bluetape4k adds Kotlin-friendly creation and interop helpers"],
    footer: "the module does not replace Mutiny; it wraps common Kotlin entry points",
  }),
  card({
    id: "Uni",
    x: 150,
    y: 555,
    w: 600,
    h: 260,
    color: "blue",
    kicker: "0 or 1 item",
    title: "Uni<T>",
    lines: ["single async result", "null, void, item, supplier, failure", "CompletionStage and Future bridge", "onEach side-effect keeps the item"],
    footer: "closest mental model: Mono",
  }),
  card({
    id: "Multi",
    x: 1850,
    y: 555,
    w: 600,
    h: 260,
    color: "violet",
    kicker: "0 or more items",
    title: "Multi<T>",
    lines: ["stream of ordered items", "vararg, range, iterable, sequence, stream", "primitive arrays and progressions", "repeat helpers defer Uni or CompletionStage"],
    footer: "closest mental model: Flux",
  }),
  card({
    id: "UniSupport",
    x: 150,
    y: 955,
    w: 600,
    h: 265,
    color: "green",
    kicker: "UniSupport.kt",
    title: "Uni helper surface",
    lines: ["voidUni(), nullUni(), uniOf(...)", "uniFailureOf(...)", "uniCompletionStageOf(...)", "uniFutureOf(..., timeout)"],
    footer: "creation functions return new Uni instances",
  }),
  card({
    id: "CoroutineSupport",
    x: 930,
    y: 955,
    w: 650,
    h: 265,
    color: "teal",
    kicker: "CoroutineSupport.kt",
    title: "Coroutine interop",
    lines: ["CoroutineScope.asUni { suspend block }", "runs async { block(...) } in current scope", "uses kotlinx.coroutines.mutiny.asUni()", "block failures become failed Uni"],
    footer: "bridge is Uni-oriented, not Multi-oriented",
  }),
  card({
    id: "MultiSupport",
    x: 1850,
    y: 955,
    w: 600,
    h: 265,
    color: "amber",
    kicker: "MultiSupport.kt",
    title: "Multi helper surface",
    lines: ["multiOf(...), multiRangeOf(...)", "Iterable/Sequence/Stream.asMulti()", "Int/Long/Float/Double arrays", "MultiRepetition.deferUni/deferCompletionStage"],
    footer: "stream sources keep order and lazy behavior",
  }),
  edge({ from: "Mutiny", to: "Uni", points: [[1020, 420], [1020, 485], [450, 485], [450, 555]], color: "blue", label: "single-result side", labelAt: [620, 466] }),
  edge({ from: "Mutiny", to: "Multi", points: [[1500, 420], [1500, 485], [2150, 485], [2150, 555]], color: "violet", label: "stream side", labelAt: [1770, 466] }),
  edge({ from: "UniSupport", to: "Uni", points: [[450, 955], [450, 815]], color: "green", dashed: true, label: "creates and converts", labelAt: [468, 890] }),
  edge({ from: "CoroutineSupport", to: "Uni", points: [[930, 1075], [830, 1075], [830, 885], [450, 885], [450, 815]], color: "teal", dashed: true, label: "suspend block -> Uni", labelAt: [845, 1048] }),
  edge({ from: "MultiSupport", to: "Multi", points: [[2150, 955], [2150, 815]], color: "amber", dashed: true, label: "creates streams", labelAt: [2168, 890] }),
  edge({ from: "Uni", to: "Multi", points: [[750, 685], [1850, 685]], color: "slate", dashed: true, label: "different item cardinality", labelAt: [1170, 664] }),
];

validateNoCardOverlap(validationCards);
validateEdgeEndpoints([
  { from: "Mutiny", to: "Uni", points: [[1020, 420], [1020, 485], [450, 485], [450, 555]] },
  { from: "Mutiny", to: "Multi", points: [[1500, 420], [1500, 485], [2150, 485], [2150, 555]] },
  { from: "UniSupport", to: "Uni", points: [[450, 955], [450, 815]] },
  { from: "CoroutineSupport", to: "Uni", points: [[930, 1075], [830, 1075], [830, 885], [450, 885], [450, 815]] },
  { from: "MultiSupport", to: "Multi", points: [[2150, 955], [2150, 815]] },
  { from: "Uni", to: "Multi", points: [[750, 685], [1850, 685]] },
], validationCards);

const svg = `<svg data-intent="Explain Mutiny Uni and Multi type surface for README diagram 01." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Mutiny Type Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:47px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .card{stroke-width:1.8;filter:url(#shadow)}.kicker{font-family:"Comic Mono";font-size:14px;fill:#475569}.cardTitle{font-family:"Architects Daughter";font-size:25px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:14px;fill:#334155}.foot{font-family:"Comic Mono";font-size:13px;fill:#475569}.divider{stroke-width:1.1;opacity:.42}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">Mutiny Type Diagram</text>
<text class="subtitle" x="76" y="120">bluetape4k-mutiny keeps SmallRye Mutiny's Uni and Multi model, then adds Kotlin-friendly creation, conversion, and coroutine interop helpers.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-mutiny-diagram-01.svg");
const pngPath = join(OUT, "utils-mutiny-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated utils-mutiny-diagram-01.svg/png");
