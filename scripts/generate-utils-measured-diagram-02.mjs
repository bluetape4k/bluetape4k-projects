#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/measured/README.md",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Units.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/TypeAliases.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Motion.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Area.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Volume.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/EnergyPower.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Unit Composition Flow[\s\S]*utils-measured-diagram-02\.png/, "README unit composition slot");
assertContains(sources[1], /Number\.times\(unit: T\)[\s\S]*A\.times\(other: B\)[\s\S]*A\.div\(other: B\)[\s\S]*Measure<A>\.times[\s\S]*Measure<A>\.div/, "generic unit and measure operators");
assertContains(sources[1], /timesRatioByDenominator[\s\S]*divProductByLeft/, "inverse composition operators");
assertContains(sources[2], /typealias Velocity[\s\S]*typealias Acceleration/, "motion aliases");
assertContains(sources[3], /metersPerSecond[\s\S]*kilometersPerHour[\s\S]*metersPerSecondSquared/, "motion constructors");
assertContains(sources[4], /timesLengthToArea[\s\S]*Measure<Area>/, "length-to-area specialization");
assertContains(sources[5], /areaTimesLengthToVolume[\s\S]*volumeDivAreaToLength[\s\S]*volumeDivLengthToArea/, "volume specializations");
assertContains(sources[6], /powerTimesTimeToEnergy[\s\S]*energyDivTimeToPower/, "energy specializations");

const palette = {
  slate: ["#F8FAFC", "#64748B", "#475569"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
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
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 3 3 L 19 11 L 3 19 Z" fill="${dark}"/></marker>`).join("\n");
}

function card({ id, x, y, w, h, color, kicker, title, lines = [], footer = "" }) {
  const [fill, stroke, dark] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="kicker" x="${x + 22}" y="${y + 30}">${esc(kicker)}</text>
  <text class="cardTitle" x="${x + 22}" y="${y + 64}">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 84}H${x + w}" stroke="${dark}"/>
  ${lines.map((line, index) => `<text class="body" x="${x + 22}" y="${y + 116 + index * 24}">${esc(line)}</text>`).join("\n")}
  ${footer ? `<path class="divider" d="M${x} ${y + h - 44}H${x + w}" stroke="${dark}"/><text class="foot" x="${x + 22}" y="${y + h - 16}">${esc(footer)}</text>` : ""}
</g>`;
}

function laneLabel({ x, y, text }) {
  return `<text class="lane" x="${x}" y="${y}">${esc(text)}</text>`;
}

function edge({ from, to, points, color, dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  const labelWidth = label ? Math.max(110, label.length * 8 + 24) : 0;
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>
  ${label ? `<rect class="edgeLabelBg" x="${p[0] - 8}" y="${p[1] - 17}" width="${labelWidth}" height="24" rx="4"/><text class="edgeLabel" x="${p[0]}" y="${p[1]}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2700;
const height = 1680;

const edges = [
  edge({ from: "UnitConstants", to: "UnitArithmetic", points: [[620, 342], [745, 342]], color: "green", label: "A / B", labelAt: [654, 318] }),
  edge({ from: "UnitArithmetic", to: "AliasUnits", points: [[1185, 342], [1310, 342]], color: "green", label: "UnitsRatio", labelAt: [1215, 318] }),
  edge({ from: "NumberAndUnits", to: "MeasureInputs", points: [[620, 722], [745, 722]], color: "blue", label: "Number.times", labelAt: [635, 698] }),
  edge({ from: "MeasureInputs", to: "GenericMeasureOps", points: [[1185, 722], [1310, 722]], color: "blue", label: "Measure / Measure", labelAt: [1200, 698] }),
  edge({ from: "GenericMeasureOps", to: "SpeedResult", points: [[1750, 722], [1875, 722]], color: "blue", label: "typed ratio", labelAt: [1775, 698] }),
  edge({ from: "SpeedResult", to: "RecoveredLength", points: [[2095, 870], [2095, 948]], color: "teal", dashed: true, label: "* Time", labelAt: [2116, 920] }),
  edge({ from: "ShapeInputs", to: "ShapeOps", points: [[620, 1132], [745, 1132]], color: "violet", label: "specialized *", labelAt: [630, 1108] }),
  edge({ from: "ShapeOps", to: "ShapeResults", points: [[1185, 1132], [1310, 1132]], color: "violet", label: "Area / Volume", labelAt: [1205, 1108] }),
  edge({ from: "PowerInputs", to: "EnergyOps", points: [[1750, 1392], [1875, 1392]], color: "amber", label: "W * s", labelAt: [1775, 1368] }),
  edge({ from: "EnergyOps", to: "Presentation", points: [[2315, 1392], [2525, 1392], [2525, 880]], color: "amber", dashed: true, label: "convert result", labelAt: [2545, 1130] }),
  edge({ from: "SpeedResult", to: "Presentation", points: [[2315, 722], [2390, 722]], color: "teal", dashed: true, label: "render result", labelAt: [2328, 698] }),
];

const nodes = [
  laneLabel({ x: 106, y: 204, text: "Unit composition" }),
  card({
    id: "UnitConstants",
    x: 140,
    y: 245,
    w: 480,
    h: 195,
    color: "green",
    kicker: "base unit constants",
    title: "Length.meters / Time.seconds",
    lines: ["Units carry suffix and ratio", "m ratio = 1.0", "s ratio = 1000.0"],
    footer: "constants are immutable unit values",
  }),
  card({
    id: "UnitArithmetic",
    x: 745,
    y: 245,
    w: 440,
    h: 195,
    color: "green",
    kicker: "Units.kt",
    title: "A.div(B)",
    lines: ["creates UnitsRatio<A,B>", "suffix = numerator/denominator", "ratio = A.ratio / B.ratio"],
    footer: "A.times(B) creates UnitsProduct<A,B>",
  }),
  card({
    id: "AliasUnits",
    x: 1310,
    y: 245,
    w: 495,
    h: 195,
    color: "teal",
    kicker: "TypeAliases.kt + Motion.kt",
    title: "Velocity / Acceleration",
    lines: ["Velocity = UnitsRatio<Length, Time>", "Acceleration = UnitsRatio<Length, Square<Time>>", "MotionUnits exposes m/s, km/hr, m/s^2"],
    footer: "aliases preserve generic unit structure",
  }),
  laneLabel({ x: 106, y: 584, text: "Value composition" }),
  card({
    id: "NumberAndUnits",
    x: 140,
    y: 625,
    w: 480,
    h: 195,
    color: "blue",
    kicker: "Number extensions",
    title: "10 * meters, 2 * seconds",
    lines: ["Number.times(unit)", "Number.meters()", "Number.seconds()"],
    footer: "each call creates Measure<T>",
  }),
  card({
    id: "MeasureInputs",
    x: 745,
    y: 625,
    w: 440,
    h: 195,
    color: "blue",
    kicker: "typed values",
    title: "Measure<Length> and Measure<Time>",
    lines: ["amount is stored as Double", "units keeps the exact type", "conversion uses ratio"],
    footer: "values are immutable",
  }),
  card({
    id: "GenericMeasureOps",
    x: 1310,
    y: 625,
    w: 440,
    h: 195,
    color: "blue",
    kicker: "generic operators",
    title: "Measure<A>.div(Measure<B>)",
    lines: ["amount / other.amount", "units / other.units", "returns Measure<UnitsRatio<A,B>>"],
    footer: "same pattern for product units",
  }),
  card({
    id: "SpeedResult",
    x: 1875,
    y: 625,
    w: 440,
    h: 245,
    color: "teal",
    kicker: "example result",
    title: "Measure<Velocity>",
    lines: ["10.meters() / 2.seconds()", "speed in m/s = 5.0", "speed * 5.seconds() restores Length"],
    footer: "ratio-by-denominator returns numerator unit",
  }),
  card({
    id: "RecoveredLength",
    x: 1875,
    y: 948,
    w: 440,
    h: 135,
    color: "teal",
    kicker: "inverse composition",
    title: "Measure<Length>",
    lines: ["(A/B) * B -> A"],
  }),
  card({
    id: "Presentation",
    x: 2390,
    y: 660,
    w: 250,
    h: 220,
    color: "slate",
    kicker: "output",
    title: "convert",
    lines: ["`in` target unit", "`as` target unit", "toHuman()"],
    footer: "README example prints distance",
  }),
  laneLabel({ x: 106, y: 994, text: "Domain specializations" }),
  card({
    id: "ShapeInputs",
    x: 140,
    y: 1035,
    w: 480,
    h: 235,
    color: "violet",
    kicker: "Area.kt + Volume.kt",
    title: "Length, Area, Volume",
    lines: ["Length * Length", "Area * Length", "Volume / Area", "Volume / Length"],
    footer: "specialized overloads return domain types",
  }),
  card({
    id: "ShapeOps",
    x: 745,
    y: 1035,
    w: 440,
    h: 235,
    color: "violet",
    kicker: "canonical conversion",
    title: "meters and meters2",
    lines: ["length uses Length.meters", "area uses Area.meters2", "volume uses Volume.cubicMeters"],
    footer: "source converts before arithmetic",
  }),
  card({
    id: "ShapeResults",
    x: 1310,
    y: 1035,
    w: 440,
    h: 235,
    color: "violet",
    kicker: "typed result",
    title: "Area or Volume",
    lines: ["Measure<Area>", "Measure<Volume>", "division recovers Length or Area"],
    footer: "domain type is clearer than raw UnitsProduct",
  }),
  card({
    id: "PowerInputs",
    x: 1310,
    y: 1325,
    w: 440,
    h: 190,
    color: "amber",
    kicker: "EnergyPower.kt",
    title: "Power and Time",
    lines: ["Power * Time", "Time * Power", "Energy / Time"],
  }),
  card({
    id: "EnergyOps",
    x: 1875,
    y: 1325,
    w: 440,
    h: 190,
    color: "amber",
    kicker: "canonical conversion",
    title: "watts and seconds",
    lines: ["W * s -> J", "J / s -> W", "kWh display via conversion"],
  }),
];

const svg = `<svg data-intent="Explain measured unit composition flow from the current README and Kotlin operator sources." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Unit Composition Flow">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:48px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .lane{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}.card{stroke-width:1.8;filter:url(#shadow)}
    .kicker{font-family:"Comic Mono";font-size:14px;fill:#475569}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:14px;fill:#334155}.foot{font-family:"Comic Mono";font-size:13px;fill:#475569}.divider{stroke-width:1.1;opacity:.42}
    .edge{fill:none;stroke-width:3.6;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabelBg{fill:#FFFFFF;stroke:#E2E8F0;stroke-width:.8;opacity:.94}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#334155}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="88">Unit Composition Flow</text>
<text class="subtitle" x="76" y="122">Values become typed Measure instances; unit and measure operators compose ratios/products, while domain specializations return Area, Volume, Energy, or restored base units.</text>
${edges.join("\n")}
${nodes.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-measured-diagram-02.svg");
const pngPath = join(OUT, "utils-measured-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated utils-measured-diagram-02.svg/png");
