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

assertContains(sources[0], /Mutiny Processing Flow[\s\S]*utils-mutiny-diagram-02\.png/, "README processing flow slot");
assertContains(sources[1], /Uni<T>\.onEach[\s\S]*subscribeAsCompletionStage|CompletionStage<T>\.asUni/, "Uni processing helpers");
assertContains(sources[2], /Multi<T>\.onEach[\s\S]*collect\(\)\.asList|MultiRepetition\.deferUni/, "Multi processing helpers");
assertContains(sources[3], /CoroutineScope\.asUni[\s\S]*async[\s\S]*asUni\(\)/, "Coroutine processing bridge");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  violet: ["#F5F3FF", "#7C3AED", "#6D28D9"],
  slate: ["#F8FAFC", "#64748B", "#475569"],
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

function lane({ x, y, text, color }) {
  const [, , dark] = palette[color];
  return `<text class="lane" x="${x}" y="${y}" fill="${dark}">${esc(text)}</text>`;
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

const width = 2700;
const height = 1540;
const validationCards = [
  { id: "UniSources", x: 110, y: 240, w: 430, h: 230 },
  { id: "UniHelpers", x: 700, y: 240, w: 460, h: 230 },
  { id: "UniPipeline", x: 1320, y: 240, w: 460, h: 230 },
  { id: "UniTerminal", x: 1940, y: 240, w: 520, h: 230 },
  { id: "MultiSources", x: 110, y: 655, w: 430, h: 250 },
  { id: "MultiHelpers", x: 700, y: 655, w: 460, h: 250 },
  { id: "MultiPipeline", x: 1320, y: 655, w: 460, h: 250 },
  { id: "MultiTerminal", x: 1940, y: 655, w: 520, h: 250 },
  { id: "CoroutineBlock", x: 420, y: 1095, w: 520, h: 220 },
  { id: "CoroutineBridge", x: 1110, y: 1095, w: 520, h: 220 },
  { id: "InteropResult", x: 1800, y: 1095, w: 520, h: 220 },
];
const body = [
  lane({ x: 95, y: 205, text: "Uni processing lane", color: "blue" }),
  card({ id: "UniSources", x: 110, y: 240, w: 430, h: 230, color: "blue", kicker: "source", title: "single result inputs", lines: ["item, supplier, failure", "CompletionStage", "Future with timeout"], footer: "0 or 1 item intent" }),
  card({ id: "UniHelpers", x: 700, y: 240, w: 460, h: 230, color: "green", kicker: "UniSupport.kt", title: "Uni creation helpers", lines: ["uniOf(...)", "uniFailureOf(...)", "uniCompletionStageOf(...)", "uniFutureOf(...)"], footer: "creates Uni<T>" }),
  card({ id: "UniPipeline", x: 1320, y: 240, w: 460, h: 230, color: "teal", kicker: "Mutiny pipeline", title: "Uni<T> processing", lines: ["onEach { callback }", "onItem().invoke(...)", "failure propagates downstream"], footer: "item is passed through unchanged" }),
  card({ id: "UniTerminal", x: 1940, y: 240, w: 520, h: 230, color: "slate", kicker: "terminal boundary", title: "await or CompletionStage", lines: ["await().atMost(...)", "await().indefinitely()", "subscribeAsCompletionStage()"], footer: "caller observes T, null, or failure" }),

  lane({ x: 95, y: 620, text: "Multi processing lane", color: "violet" }),
  card({ id: "MultiSources", x: 110, y: 655, w: 430, h: 250, color: "violet", kicker: "source", title: "stream inputs", lines: ["vararg and range", "Iterable, Sequence, Stream", "arrays and progressions", "deferred Uni or CompletionStage repeats"], footer: "0 or more items intent" }),
  card({ id: "MultiHelpers", x: 700, y: 655, w: 460, h: 250, color: "amber", kicker: "MultiSupport.kt", title: "Multi creation helpers", lines: ["multiOf(...)", "multiRangeOf(...)", "asMulti() conversions", "deferUni / deferCompletionStage"], footer: "creates Multi<T> or UniRepeat<T>" }),
  card({ id: "MultiPipeline", x: 1320, y: 655, w: 460, h: 250, color: "teal", kicker: "Mutiny pipeline", title: "Multi<T> processing", lines: ["onEach { callback }", "onItem().invoke(...)", "source order is preserved", "callback failure fails stream"], footer: "items continue downstream" }),
  card({ id: "MultiTerminal", x: 1940, y: 655, w: 520, h: 250, color: "slate", kicker: "terminal boundary", title: "collect or subscribe", lines: ["collect().asList()", "await().indefinitely()", "subscriber consumes stream"], footer: "caller observes list, stream, or failure" }),

  lane({ x: 95, y: 1060, text: "Coroutine interop lane", color: "teal" }),
  card({ id: "CoroutineBlock", x: 420, y: 1095, w: 520, h: 220, color: "teal", kicker: "CoroutineScope", title: "suspend block", lines: ["CoroutineScope.asUni { ... }", "runs async in current scope", "does not change receiver scope"], footer: "source is coroutine code" }),
  card({ id: "CoroutineBridge", x: 1110, y: 1095, w: 520, h: 220, color: "teal", kicker: "kotlinx.coroutines.mutiny", title: "async(...).asUni()", lines: ["Deferred result becomes Uni", "block exception becomes failed Uni", "cancellation follows coroutine machinery"], footer: "bridge targets Uni<T>" }),
  card({ id: "InteropResult", x: 1800, y: 1095, w: 520, h: 220, color: "blue", kicker: "Mutiny result", title: "Uni<T>", lines: ["continues through normal Uni pipeline", "can use onEach", "can await or subscribe"], footer: "same terminal surface as UniSupport" }),

  edge({ from: "UniSources", to: "UniHelpers", points: [[540, 355], [700, 355]], color: "blue", label: "wrapped by", labelAt: [585, 334] }),
  edge({ from: "UniHelpers", to: "UniPipeline", points: [[1160, 355], [1320, 355]], color: "green", label: "produces Uni", labelAt: [1200, 334] }),
  edge({ from: "UniPipeline", to: "UniTerminal", points: [[1780, 355], [1940, 355]], color: "teal", label: "terminal choice", labelAt: [1810, 334] }),
  edge({ from: "MultiSources", to: "MultiHelpers", points: [[540, 780], [700, 780]], color: "violet", label: "converted by", labelAt: [585, 759] }),
  edge({ from: "MultiHelpers", to: "MultiPipeline", points: [[1160, 780], [1320, 780]], color: "amber", label: "produces Multi", labelAt: [1195, 759] }),
  edge({ from: "MultiPipeline", to: "MultiTerminal", points: [[1780, 780], [1940, 780]], color: "teal", label: "terminal choice", labelAt: [1810, 759] }),
  edge({ from: "CoroutineBlock", to: "CoroutineBridge", points: [[940, 1205], [1110, 1205]], color: "teal", label: "async", labelAt: [1000, 1184] }),
  edge({ from: "CoroutineBridge", to: "InteropResult", points: [[1630, 1205], [1800, 1205]], color: "teal", label: "asUni", labelAt: [1695, 1184] }),
  edge({ from: "InteropResult", to: "UniPipeline", points: [[2060, 1095], [2060, 990], [2520, 990], [2520, 530], [1550, 530], [1550, 470]], color: "blue", dashed: true, label: "uses Uni pipeline", labelAt: [2120, 971] }),
];

validateNoCardOverlap(validationCards);
validateEdgeEndpoints([
  { from: "UniSources", to: "UniHelpers", points: [[540, 355], [700, 355]] },
  { from: "UniHelpers", to: "UniPipeline", points: [[1160, 355], [1320, 355]] },
  { from: "UniPipeline", to: "UniTerminal", points: [[1780, 355], [1940, 355]] },
  { from: "MultiSources", to: "MultiHelpers", points: [[540, 780], [700, 780]] },
  { from: "MultiHelpers", to: "MultiPipeline", points: [[1160, 780], [1320, 780]] },
  { from: "MultiPipeline", to: "MultiTerminal", points: [[1780, 780], [1940, 780]] },
  { from: "CoroutineBlock", to: "CoroutineBridge", points: [[940, 1205], [1110, 1205]] },
  { from: "CoroutineBridge", to: "InteropResult", points: [[1630, 1205], [1800, 1205]] },
  { from: "InteropResult", to: "UniPipeline", points: [[2060, 1095], [2060, 990], [2520, 990], [2520, 530], [1550, 530], [1550, 470]] },
], validationCards);

const svg = `<svg data-intent="Explain Mutiny processing flow for README diagram 02." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Mutiny Processing Flow Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:47px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .card{stroke-width:1.8;filter:url(#shadow)}.kicker{font-family:"Comic Mono";font-size:14px;fill:#475569}.cardTitle{font-family:"Architects Daughter";font-size:25px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:14px;fill:#334155}.foot{font-family:"Comic Mono";font-size:13px;fill:#475569}.divider{stroke-width:1.1;opacity:.42}
    .lane{font-family:"Comic Mono";font-size:16px;font-weight:700;letter-spacing:0}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">Mutiny Processing Flow</text>
<text class="subtitle" x="76" y="120">Inputs become Uni or Multi through bluetape4k helpers, pass through Mutiny item callbacks, and finish at await, collect, or subscription boundaries.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-mutiny-diagram-02.svg");
const pngPath = join(OUT, "utils-mutiny-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated utils-mutiny-diagram-02.svg/png");
