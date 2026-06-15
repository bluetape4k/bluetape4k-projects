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
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Length.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Time.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Mass.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Area.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Volume.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Storage.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/BinarySize.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Frequency.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/EnergyPower.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Pressure.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/GraphicsLength.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Motion.kt",
  "utils/measured/src/main/kotlin/io/bluetape4k/measured/Temperature.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Class Diagram[\s\S]*utils-measured-diagram-01\.png/, "README class diagram slot");
assertContains(sources[1], /abstract class Units[\s\S]*class UnitsProduct[\s\S]*class UnitsRatio[\s\S]*class InverseUnits[\s\S]*class Measure<T: Units>/, "core unit and measure classes");
assertContains(sources[1], /operator fun <T: Units> Number\.times[\s\S]*operator fun <A: Units, B: Units> A\.times[\s\S]*operator fun <A: Units, B: Units> A\.div/, "unit operator surface");
assertContains(sources[2], /typealias Velocity[\s\S]*typealias Acceleration/, "motion type aliases");
assertContains(sources[14], /object MotionUnits[\s\S]*metersPerSecond[\s\S]*metersPerSecondSquared/, "motion unit constants");
assertContains(sources[15], /enum class TemperatureUnit[\s\S]*value class TemperatureDelta/, "temperature value model");

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
  return Object.entries(palette).map(([name, [, stroke, dark]]) => `
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 3 3 L 19 11 L 3 19 Z" fill="${dark}"/></marker>
  <marker id="inherit-${name}" markerWidth="24" markerHeight="22" refX="20" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 3 3 L 20 11 L 3 19 Z" fill="#FFFFFF" stroke="${stroke}" stroke-width="2"/></marker>`).join("\n");
}

function classBox({ id, x, y, w, h, color, stereotype = "", title, attrs = [], methods = [] }) {
  const [fill, stroke] = palette[color];
  const titleY = stereotype ? y + 48 : y + 42;
  const attrStart = y + 106;
  const methodStart = y + 106 + Math.max(attrs.length, 1) * 24 + 35;
  return `<g id="${esc(id)}">
  <rect class="classBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  ${stereotype ? `<text class="stereo" x="${x + w / 2}" y="${y + 26}" text-anchor="middle">${esc(stereotype)}</text>` : ""}
  <text class="classTitle" x="${x + w / 2}" y="${titleY}" text-anchor="middle">${esc(title)}</text>
  <path class="compartment" d="M${x} ${y + 72}H${x + w}"/>
  ${attrs.map((line, index) => `<text class="member" x="${x + 24}" y="${attrStart + index * 24}">${esc(line)}</text>`).join("\n")}
  <path class="compartment" d="M${x} ${methodStart - 26}H${x + w}"/>
  ${methods.map((line, index) => `<text class="member" x="${x + 24}" y="${methodStart + index * 24}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function note({ id, x, y, w, h, color, title, lines }) {
  const [fill, stroke] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="note" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="noteTitle" x="${x + 22}" y="${y + 38}">${esc(title)}</text>
  ${lines.map((line, index) => `<text class="noteLine" x="${x + 22}" y="${y + 72 + index * 24}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function edge({ from, to, points, color = "slate", dashed = false, marker = "arrow", label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const markerEnd = marker === "inherit" ? `url(#inherit-${color})` : `url(#arrow-${color})`;
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  const labelWidth = label ? Math.max(110, label.length * 8 + 24) : 0;
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="${markerEnd}"/>
  ${label ? `<rect class="edgeLabelBg" x="${p[0] - 8}" y="${p[1] - 17}" width="${labelWidth}" height="24" rx="4"/><text class="edgeLabel" x="${p[0]}" y="${p[1]}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2600;
const height = 1600;
const nodes = [
  classBox({
    id: "Units",
    x: 480,
    y: 175,
    w: 620,
    h: 270,
    color: "blue",
    stereotype: "abstract class",
    title: "Units",
    attrs: ["+ suffix: String", "+ ratio: Double", "# spaceBetweenMagnitude: Boolean"],
    methods: ["+ toString(): String", "+ equals(other): Boolean", "+ hashCode(): Int"],
  }),
  classBox({
    id: "Measure",
    x: 1510,
    y: 155,
    w: 660,
    h: 315,
    color: "teal",
    title: "Measure<T: Units>",
    attrs: ["+ amount: Double", "+ units: T"],
    methods: ["+ as(other): Measure<T>", "+ in(other): Double", "+ plus/minus(other): Measure<T>", "+ toHuman(): String", "+ compareTo(other): Int"],
  }),
  classBox({
    id: "UnitsProduct",
    x: 140,
    y: 595,
    w: 500,
    h: 250,
    color: "violet",
    title: "UnitsProduct<A, B>",
    attrs: ["+ first: A", "+ second: B", "+ suffix: A*B", "+ ratio: A.ratio * B.ratio"],
    methods: ["created by A.times(B)"],
  }),
  classBox({
    id: "UnitsRatio",
    x: 720,
    y: 595,
    w: 500,
    h: 250,
    color: "green",
    title: "UnitsRatio<A, B>",
    attrs: ["+ numerator: A", "+ denominator: B", "+ reciprocal: UnitsRatio<B,A>", "+ ratio: A.ratio / B.ratio"],
    methods: ["created by A.div(B)"],
  }),
  classBox({
    id: "InverseUnits",
    x: 1300,
    y: 595,
    w: 500,
    h: 250,
    color: "amber",
    title: "InverseUnits<T>",
    attrs: ["+ unit: T", "+ suffix: 1/unit", "+ ratio: 1.0 / unit.ratio"],
    methods: ["represents reciprocal unit"],
  }),
  classBox({
    id: "ConcreteUnits",
    x: 140,
    y: 1020,
    w: 980,
    h: 330,
    color: "blue",
    title: "Open Units subclasses",
    attrs: ["Length, Time, Mass", "Area, Volume, GraphicsLength", "Storage, BinarySize, Frequency", "Energy, Power, Pressure"],
    methods: ["companion constants keep suffix and ratio", "Number extension functions create Measure<T>", "toHuman() picks practical display units"],
  }),
  classBox({
    id: "TemperatureModel",
    x: 1960,
    y: 1020,
    w: 500,
    h: 300,
    color: "amber",
    title: "Temperature model",
    attrs: ["TemperatureUnit enum", "TemperatureDelta value class", "Kelvin / Celsius / Fahrenheit"],
    methods: ["inKelvin()", "inCelsius()", "toHuman(unit)"],
  }),
  note({
    id: "MotionAliases",
    x: 1230,
    y: 1035,
    w: 620,
    h: 145,
    color: "slate",
    title: "Motion aliases",
    lines: ["Velocity = UnitsRatio<Length, Time>", "Acceleration = UnitsRatio<Length, Square<Time>>", "MotionUnits builds m/s, km/hr, and m/s^2 from unit operators"],
  }),
  note({
    id: "OperatorSurface",
    x: 1900,
    y: 595,
    w: 520,
    h: 250,
    color: "slate",
    title: "Operator surface",
    lines: ["Number * Units -> Measure<T>", "A * B -> UnitsProduct<A,B>", "A / B -> UnitsRatio<A,B>", "Measure operators preserve generic unit type"],
  }),
];

const edges = [
  edge({ from: "Measure", to: "Units", points: [[1510, 280], [1100, 280]], color: "teal", marker: "open", label: "has units: T", labelAt: [1250, 258] }),
  edge({ from: "UnitsProduct", to: "Units", points: [[390, 595], [390, 520], [640, 520], [640, 445]], color: "violet", marker: "inherit", label: "extends", labelAt: [420, 500] }),
  edge({ from: "UnitsRatio", to: "Units", points: [[970, 595], [970, 520], [790, 520], [790, 445]], color: "green", marker: "inherit" }),
  edge({ from: "InverseUnits", to: "Units", points: [[1550, 595], [1550, 520], [940, 520], [940, 445]], color: "amber", marker: "inherit", label: "extends", labelAt: [1240, 500] }),
  edge({ from: "ConcreteUnits", to: "Units", points: [[630, 1020], [630, 900], [710, 900], [710, 445]], color: "blue", marker: "inherit", label: "subclasses extend Units", labelAt: [650, 875] }),
  edge({ from: "MotionAliases", to: "UnitsRatio", points: [[1510, 1035], [1510, 940], [1080, 940], [1080, 845]], color: "green", dashed: true, marker: "open", label: "Velocity and Acceleration aliases", labelAt: [1180, 918] }),
  edge({ from: "OperatorSurface", to: "Measure", points: [[2160, 595], [2160, 470]], color: "slate", dashed: true, marker: "open", label: "typed results", labelAt: [2180, 540] }),
];

const svg = `<svg data-intent="Explain the measured module class structure from the current README and Kotlin sources." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="measured Class Structure">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:48px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .classBox,.note{stroke-width:1.8;filter:url(#shadow)}.stereo{font-family:"Comic Mono";font-size:14px;fill:#475569}
    .classTitle{font-family:"Architects Daughter";font-size:25px;fill:#0F172A}.compartment{stroke:#64748B;stroke-width:1;opacity:.38}
    .member{font-family:"Comic Mono";font-size:14px;fill:#334155}.noteTitle{font-family:"Architects Daughter";font-size:24px;fill:#0F172A}.noteLine{font-family:"Comic Mono";font-size:14px;fill:#334155}
    .edge{fill:none;stroke-width:3.6;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabelBg{fill:#FFFFFF;stroke:#E2E8F0;stroke-width:.8;opacity:.94}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#334155}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="88">measured Class Structure</text>
<text class="subtitle" x="76" y="122">Core model: Units define conversion ratios, Measure keeps a typed value, and compound units preserve type information for product and ratio arithmetic.</text>
${edges.join("\n")}
${nodes.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-measured-diagram-01.svg");
const pngPath = join(OUT, "utils-measured-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated utils-measured-diagram-01.svg/png");
