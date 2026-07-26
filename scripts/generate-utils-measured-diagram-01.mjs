#!/usr/bin/env node

import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-measured-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/utils-measured-diagram-01.png";
const W = 2520;
const H = 1420;
const colors = {
    ink: "#0F172A",
    muted: "#475569",
    canvas: "#F8FAFC",
    frame: "#FFFFFF",
    line: "#CBD5E1",
    blue: "#2563EB",
    teal: "#0D9488",
    green: "#16A34A",
    orange: "#EA580C",
    purple: "#7C3AED",
    amber: "#D97706",
    gray: "#64748B",
};

const evidence = [
    "utils/measured/README.md",
    "utils/measured/src/main/kotlin/io/bluetape4k/measured/Units.kt",
    "utils/measured/src/main/kotlin/io/bluetape4k/measured/TypeAliases.kt",
    "utils/measured/src/main/kotlin/io/bluetape4k/measured/Length.kt",
    "utils/measured/src/main/kotlin/io/bluetape4k/measured/Time.kt",
    "utils/measured/src/main/kotlin/io/bluetape4k/measured/Mass.kt",
    "utils/measured/src/main/kotlin/io/bluetape4k/measured/Motion.kt",
    "utils/measured/src/main/kotlin/io/bluetape4k/measured/Temperature.kt",
];

for (const file of evidence) {
    if (!existsSync(file)) throw new Error(`Missing source evidence: ${file}`);
}

const readme = readFileSync("utils/measured/README.md", "utf8");
if (!/Class Diagram[\s\S]*utils-measured-diagram-01\.png/.test(readme)) {
    throw new Error("README class diagram slot not found");
}

const units = readFileSync("utils/measured/src/main/kotlin/io/bluetape4k/measured/Units.kt", "utf8");
for (const pattern of [
    /abstract class Units/,
    /class UnitsProduct/,
    /class UnitsRatio/,
    /class InverseUnits/,
    /class Measure<T: Units>/,
    /operator fun <T: Units> Number\.times/,
    /operator fun <A: Units, B: Units> A\.times/,
    /operator fun <A: Units, B: Units> A\.div/,
]) {
    if (!pattern.test(units)) throw new Error(`Expected source pattern not found: ${pattern}`);
}

const boxes = {
    units: {
        x: 155,
        y: 195,
        w: 520,
        h: 220,
        fill: "#EFF6FF",
        stroke: colors.blue,
        stereotype: "<<abstract class>>",
        title: "Units",
        attrs: ["+ suffix: String", "+ ratio: Double", "# spaceBetweenMagnitude"],
        methods: ["+ measureSuffix()", "+ equals() / hashCode()"]
    },
    measure: {
        x: 1040,
        y: 175,
        w: 560,
        h: 260,
        fill: "#F0FDFA",
        stroke: colors.teal,
        stereotype: "<<class>>",
        title: "Measure<T: Units>",
        attrs: ["+ amount: Double", "+ units: T"],
        methods: ["+ as(other): Measure<T>", "+ in(other): Double", "+ plus/minus/times/div", "+ toHuman(), compareTo()"]
    },
    concrete: {
        x: 135,
        y: 620,
        w: 560,
        h: 245,
        fill: "#EFF6FF",
        stroke: colors.blue,
        stereotype: "<<open subclasses>>",
        title: "Concrete unit families",
        attrs: ["Length, Time, Mass, Area, Volume", "Storage, BinarySize, Frequency", "Energy, Power, Pressure, GraphicsLength"],
        methods: ["companion constants define suffix and ratio", "Number extension functions create Measure<T>"]
    },
    product: {
        x: 815,
        y: 620,
        w: 460,
        h: 220,
        fill: "#FAF5FF",
        stroke: colors.purple,
        stereotype: "<<class>>",
        title: "UnitsProduct<A, B>",
        attrs: ["+ first: A", "+ second: B", "suffix: A*B or (A)^2"],
        methods: ["created by A * B"]
    },
    ratio: {
        x: 1380,
        y: 620,
        w: 460,
        h: 235,
        fill: "#ECFDF5",
        stroke: colors.green,
        stereotype: "<<class>>",
        title: "UnitsRatio<A, B>",
        attrs: ["+ numerator: A", "+ denominator: B", "+ reciprocal: UnitsRatio<B,A>"],
        methods: ["created by A / B", "ratio = A.ratio / B.ratio"]
    },
    inverse: {
        x: 1940,
        y: 620,
        w: 420,
        h: 220,
        fill: "#FFF7ED",
        stroke: colors.orange,
        stereotype: "<<class>>",
        title: "InverseUnits<T>",
        attrs: ["+ unit: T", "suffix: 1/unit", "ratio: 1.0 / unit.ratio"],
        methods: ["represents reciprocal unit"]
    },
    operators: {
        x: 1810,
        y: 180,
        w: 500,
        h: 220,
        fill: "#F8FAFC",
        stroke: colors.gray,
        stereotype: "<<top-level operators>>",
        title: "Unit arithmetic API",
        attrs: ["Number * Units", "A * B", "A / B", "Measure * Units"],
        methods: ["returns typed Measure", "preserves compound types"]
    },
    motion: {
        x: 1050,
        y: 1030,
        w: 520,
        h: 245,
        fill: "#ECFDF5",
        stroke: colors.green,
        stereotype: "<<typealiases + object>>",
        title: "MotionUnits",
        attrs: ["Velocity = Length / Time", "Acceleration = Length / Time^2"],
        methods: ["metersPerSecond", "kilometersPerHour", "metersPerSecondSquared"]
    },
    temperature: {
        x: 1715,
        y: 1030,
        w: 520,
        h: 245,
        fill: "#FFFBEB",
        stroke: colors.amber,
        stereotype: "<<special value model>>",
        title: "Temperature",
        attrs: ["TemperatureUnit enum", "TemperatureDelta value class"],
        methods: ["Kelvin / Celsius / Fahrenheit", "delta-aware conversion helpers"]
    },
};

const edges = [
    {
        id: "measure-has-units",
        type: "has",
        color: colors.teal,
        from: "measure",
        to: "units",
        d: "M1040 305 L675 305",
        label: {x: 822, y: 276, text: "has units: T", w: 112}
    },
    {
        id: "concrete-extends-units",
        type: "extends",
        color: colors.blue,
        from: "concrete",
        to: "units",
        d: "M415 620 L415 415",
        label: {x: 488, y: 548, text: "extends Units", w: 120}
    },
    {
        id: "product-extends-units",
        type: "extends",
        color: colors.purple,
        from: "product",
        to: "units",
        d: "M1045 620 L1045 520 L585 520 L585 415"
    },
    {
        id: "ratio-extends-units",
        type: "extends",
        color: colors.green,
        from: "ratio",
        to: "units",
        d: "M1610 620 L1610 500 L640 500 L640 415"
    },
    {
        id: "inverse-extends-units",
        type: "extends",
        color: colors.orange,
        from: "inverse",
        to: "units",
        d: "M2150 620 L2150 480 L675 480 L675 360"
    },
    {
        id: "motion-uses-ratio",
        type: "uses",
        color: colors.green,
        from: "motion",
        to: "ratio",
        d: "M1310 1030 L1310 930 L1610 930 L1610 855",
        label: {x: 1460, y: 902, text: "aliases ratio units", w: 142}
    },
    {
        id: "operators-create-measure",
        type: "uses",
        color: colors.gray,
        from: "operators",
        to: "measure",
        d: "M1810 290 L1600 290",
        label: {x: 1710, y: 262, text: "creates Measure", w: 132}
    },
];

function esc(v) {
    return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function box(id) {
    const b = boxes[id];
    const nameSepY = b.y + 66;
    const attrRows = Math.max(1, b.attrs.length);
    const attrHeight = Math.max(42, attrRows * 20 + 20);
    const methodSepY = b.y + 66 + attrHeight;
    const attrY = nameSepY + 24;
    const methodY = methodSepY + 25;
    return `<g id="${id}">
  <rect class="umlBox" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/>
  <line x1="${b.x}" y1="${nameSepY}" x2="${b.x + b.w}" y2="${nameSepY}" stroke="${b.stroke}" stroke-width="1.3" opacity="0.65"/>
  <line x1="${b.x}" y1="${methodSepY}" x2="${b.x + b.w}" y2="${methodSepY}" stroke="${b.stroke}" stroke-width="1.3" opacity="0.65"/>
  <text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 25}" text-anchor="middle">${esc(b.stereotype)}</text>
  <text class="classTitle" x="${b.x + b.w / 2}" y="${b.y + 52}" text-anchor="middle">${esc(b.title)}</text>
  ${b.attrs.map((line, i) => `<text class="member" x="${b.x + 24}" y="${attrY + i * 20}">${esc(line)}</text>`).join("\n  ")}
  ${b.methods.map((line, i) => `<text class="member" x="${b.x + 24}" y="${methodY + i * 20}">${esc(line)}</text>`).join("\n  ")}
</g>`;
}

function nums(d) {
    return d.match(/-?\d+(?:\.\d+)?/g).map(Number);
}

function label({x, y, text, w}) {
    return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 14})"><rect width="${w}" height="28" rx="8"/><text x="${w / 2}" y="19" text-anchor="middle">${esc(text)}</text></g>`;
}

function arrowHead(edge) {
    const n = nums(edge.d);
    const end = {x: n[n.length - 2], y: n[n.length - 1]};
    const prev = {x: n[n.length - 4], y: n[n.length - 3]};
    const dx = end.x - prev.x;
    const dy = end.y - prev.y;
    if (edge.type === "extends") {
        if (Math.abs(dy) >= Math.abs(dx) && dy < 0) return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 8} ${end.y + 16} L${end.x + 8} ${end.y + 16} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
        if (dx < 0) return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x + 16} ${end.y - 8} L${end.x + 16} ${end.y + 8} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
        return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 16} ${end.y - 8} L${end.x - 16} ${end.y + 8} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
    }
    if (dx < 0) return `<path class="solidOpenHead" d="M${end.x + 13} ${end.y - 7} L${end.x} ${end.y} L${end.x + 13} ${end.y + 7}" stroke="${edge.color}"/>`;
    if (dy < 0) return `<path class="solidOpenHead" d="M${end.x - 7} ${end.y + 13} L${end.x} ${end.y} L${end.x + 7} ${end.y + 13}" stroke="${edge.color}"/>`;
    return `<path class="solidOpenHead" d="M${end.x - 13} ${end.y - 7} L${end.x} ${end.y} L${end.x - 13} ${end.y + 7}" stroke="${edge.color}"/>`;
}

function segs(d) {
    const n = nums(d);
    const pts = [];
    for (let i = 0; i < n.length; i += 2) pts.push({x: n[i], y: n[i + 1]});
    return pts.slice(1).map((p, i) => ({a: pts[i], b: p}));
}

function touches(b, p) {
    const onX = p.x >= b.x - 0.1 && p.x <= b.x + b.w + 0.1;
    const onY = p.y >= b.y - 0.1 && p.y <= b.y + b.h + 0.1;
    return ((Math.abs(p.x - b.x) < 0.1 || Math.abs(p.x - (b.x + b.w)) < 0.1) && onY) ||
        ((Math.abs(p.y - b.y) < 0.1 || Math.abs(p.y - (b.y + b.h)) < 0.1) && onX);
}

function hits(s, b, pad = 10) {
    const box = {x: b.x + pad, y: b.y + pad, w: b.w - pad * 2, h: b.h - pad * 2};
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
        const start = {x: n[0], y: n[1]};
        const end = {x: n[n.length - 2], y: n[n.length - 1]};
        if (!touches(boxes[e.from], start)) throw new Error(`${e.id} start`);
        if (!touches(boxes[e.to], end)) throw new Error(`${e.id} end`);
        for (const s of segs(e.d)) {
            for (const [id, b] of Object.entries(boxes)) {
                if ((id === e.from || id === e.to) && (touches(b, s.a) || touches(b, s.b))) continue;
                if (hits(s, b)) throw new Error(`${e.id} crosses ${id}`);
            }
        }
    }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="measured class structure">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${colors.muted}}.section{fill:#F3F8FF;stroke:#94A3B8;stroke-width:1.7;stroke-dasharray:12 8}.sectionTitle{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.umlBox{filter:url(#softShadow);stroke-width:2}.stereo{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}.classTitle{font-family:"Architects Daughter";font-size:23px;fill:${colors.ink}}.member{font-family:"Comic Mono";font-size:12.8px;fill:${colors.muted}}.edge{fill:none;stroke-width:2.65;stroke-linecap:round;stroke-linejoin:round}.uses{stroke-dasharray:8 7}.solidHead{stroke-width:1.9;stroke-linejoin:round;stroke-dasharray:none}.solidOpenHead{fill:none;stroke-width:2.25;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:none}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.2;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:11.8px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/>
<text class="title" x="74" y="86">measured Class Structure</text>
<text class="subtitle" x="78" y="118">Units define conversion ratios, Measure keeps typed values, and product/ratio/inverse units preserve compound-unit structure.</text>
<rect class="section" x="78" y="145" width="2340" height="780" rx="8"/><text class="sectionTitle" x="106" y="170">core type model and compound unit classes</text>
<rect class="section" x="78" y="980" width="2340" height="345" rx="8"/><text class="sectionTitle" x="106" y="1005">concrete unit families and specialized helpers</text>
<g id="edges">${edges.map((e) => `<path class="edge ${e.type}" d="${e.d}" stroke="${e.color}"/>`).join("\n")}</g>
<g id="arrowheads">${edges.map(arrowHead).join("\n")}</g>
<g id="labels">${edges.filter((e) => e.label).map((e) => label(e.label)).join("\n")}</g>
${Object.keys(boxes).map(box).join("\n")}
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
