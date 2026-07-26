#!/usr/bin/env node

import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-measured-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/utils-measured-diagram-02.png";
const W = 2840;
const H = 1760;

const files = {
    readme: "utils/measured/README.md",
    units: "utils/measured/src/main/kotlin/io/bluetape4k/measured/Units.kt",
    aliases: "utils/measured/src/main/kotlin/io/bluetape4k/measured/TypeAliases.kt",
    motion: "utils/measured/src/main/kotlin/io/bluetape4k/measured/Motion.kt",
    area: "utils/measured/src/main/kotlin/io/bluetape4k/measured/Area.kt",
    volume: "utils/measured/src/main/kotlin/io/bluetape4k/measured/Volume.kt",
    energy: "utils/measured/src/main/kotlin/io/bluetape4k/measured/EnergyPower.kt",
};

for (const file of Object.values(files)) {
    if (!existsSync(file)) throw new Error(`Missing source evidence: ${file}`);
}

function text(file) {
    return readFileSync(file, "utf8");
}

function requirePattern(file, pattern, label) {
    if (!pattern.test(text(file))) throw new Error(`Expected ${label} in ${file}`);
}

requirePattern(files.readme, /Unit Composition Flow[\s\S]*utils-measured-diagram-02\.png/, "README diagram slot");
requirePattern(files.units, /operator fun <T: Units> Number\.times\(unit: T\): Measure<T>/, "Number.times(unit)");
requirePattern(files.units, /operator fun <A: Units, B: Units> A\.times\(other: B\): UnitsProduct<A, B>/, "unit product operator");
requirePattern(files.units, /operator fun <A: Units, B: Units> A\.div\(other: B\): UnitsRatio<A, B>/, "unit ratio operator");
requirePattern(files.units, /operator fun <A: Units, B: Units> Measure<A>\.times\(other: Measure<B>\)/, "measure product operator");
requirePattern(files.units, /operator fun <A: Units, B: Units> Measure<A>\.div\(other: Measure<B>\)/, "measure ratio operator");
requirePattern(files.units, /timesRatioByDenominator[\s\S]*divProductByLeft/, "inverse compound operators");
requirePattern(files.aliases, /typealias Velocity[\s\S]*typealias Acceleration/, "motion typealiases");
requirePattern(files.motion, /metersPerSecond[\s\S]*kilometersPerHour[\s\S]*metersPerSecondSquared/, "motion unit factories");
requirePattern(files.area, /timesLengthToArea[\s\S]*Measure<Area>/, "area specialization");
requirePattern(files.volume, /areaTimesLengthToVolume[\s\S]*volumeDivAreaToLength[\s\S]*volumeDivLengthToArea/, "volume specialization");
requirePattern(files.energy, /powerTimesTimeToEnergy[\s\S]*energyDivTimeToPower/, "energy specialization");

const colors = {
    ink: "#0F172A",
    muted: "#475569",
    canvas: "#F8FAFC",
    frame: "#FFFFFF",
    line: "#CBD5E1",
    blue: "#2563EB",
    teal: "#0D9488",
    green: "#16A34A",
    purple: "#7C3AED",
    orange: "#EA580C",
    amber: "#D97706",
    gray: "#64748B",
};

const palette = {
    unit: {fill: "#F0FDF4", stroke: colors.green, dark: "#15803D"},
    measure: {fill: "#EFF6FF", stroke: colors.blue, dark: "#1D4ED8"},
    ratio: {fill: "#F0FDFA", stroke: colors.teal, dark: "#0F766E"},
    domain: {fill: "#FAF5FF", stroke: colors.purple, dark: "#6D28D9"},
    energy: {fill: "#FFF7ED", stroke: colors.orange, dark: "#C2410C"},
    output: {fill: "#F8FAFC", stroke: colors.gray, dark: "#475569"},
};

const cards = {
    baseUnits: {
        x: 140,
        y: 265,
        w: 510,
        h: 235,
        tone: "unit",
        kicker: "base unit constants",
        title: "Length.meters + Time.seconds",
        lines: ["each Units value owns suffix and ratio", "meters ratio = 1.0", "seconds ratio = 1000.0"],
        foot: "immutable unit values",
    },
    unitOps: {
        x: 790,
        y: 265,
        w: 500,
        h: 235,
        tone: "unit",
        kicker: "Units.kt",
        title: "A / B and A * B",
        lines: ["A.div(B) -> UnitsRatio<A,B>", "A.times(B) -> UnitsProduct<A,B>", "compound unit keeps typed operands"],
        foot: "unit-only composition",
    },
    aliases: {
        x: 1430,
        y: 265,
        w: 560,
        h: 235,
        tone: "ratio",
        kicker: "TypeAliases.kt + Motion.kt",
        title: "Velocity and Acceleration",
        lines: ["Velocity = UnitsRatio<Length, Time>", "Acceleration = UnitsRatio<Length, Square<Time>>", "m/s, km/hr, m/s^2 are concrete ratios"],
        foot: "motion names wrap generic ratios",
    },
    numberInputs: {
        x: 140,
        y: 645,
        w: 510,
        h: 235,
        tone: "measure",
        kicker: "Number extensions",
        title: "10 * meters, 2 * seconds",
        lines: ["Number.times(unit)", "Number.meters()", "Number.seconds()"],
        foot: "creates Measure<T>",
    },
    measures: {
        x: 790,
        y: 645,
        w: 500,
        h: 235,
        tone: "measure",
        kicker: "typed values",
        title: "Measure<Length> + Measure<Time>",
        lines: ["amount is Double", "units keeps exact generic type", "conversion uses unit ratios"],
        foot: "value and unit move together",
    },
    measureOps: {
        x: 1430,
        y: 645,
        w: 500,
        h: 235,
        tone: "measure",
        kicker: "generic operators",
        title: "Measure<A> / Measure<B>",
        lines: ["amount / other.amount", "units / other.units", "returns Measure<UnitsRatio<A,B>>"],
        foot: "same pattern for product units",
    },
    speed: {
        x: 2070,
        y: 645,
        w: 520,
        h: 255,
        tone: "ratio",
        kicker: "typed result",
        title: "Measure<Velocity>",
        lines: ["10.meters() / 2.seconds()", "speed in m/s = 5.0", "speed * 5.seconds() restores Length"],
        foot: "(A/B) * B -> A",
    },
    presentation: {
        x: 2070,
        y: 965,
        w: 520,
        h: 220,
        tone: "output",
        kicker: "presentation",
        title: "convert, `in`, `as`, toHuman()",
        lines: ["convert values into target units", "format concise human-readable output"],
        foot: "examples display typed results",
    },
    shapeInputs: {
        x: 140,
        y: 1280,
        w: 510,
        h: 260,
        tone: "domain",
        kicker: "Area.kt + Volume.kt",
        title: "Length, Area, Volume",
        lines: ["Length * Length", "Area * Length", "Volume / Area", "Volume / Length"],
        foot: "specialized overloads return domain units",
    },
    shapeOps: {
        x: 790,
        y: 1280,
        w: 500,
        h: 260,
        tone: "domain",
        kicker: "canonical conversion",
        title: "meters, meters2, cubicMeters",
        lines: ["convert operands to canonical bases", "perform arithmetic on canonical values", "return Area, Volume, Length, or Area"],
        foot: "domain result is clearer than raw product",
    },
    energyInputs: {
        x: 1430,
        y: 1280,
        w: 500,
        h: 260,
        tone: "energy",
        kicker: "EnergyPower.kt",
        title: "Power + Time",
        lines: ["Power * Time", "Time * Power", "Energy / Time"],
        foot: "W*s <-> J, J/s <-> W",
    },
    domainResults: {
        x: 2070,
        y: 1450,
        w: 520,
        h: 170,
        tone: "output",
        kicker: "domain results",
        title: "Area, Volume, Energy, Power",
        lines: ["specialized APIs hide compound internals", "callers receive reader-friendly measure types"],
    },
};

function esc(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function card(id) {
    const c = cards[id];
    const p = palette[c.tone];
    return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${p.fill}" stroke="${p.stroke}"/>
  <text class="kicker" x="${c.x + 24}" y="${c.y + 32}">${esc(c.kicker)}</text>
  <text class="cardTitle" x="${c.x + 24}" y="${c.y + 67}">${esc(c.title)}</text>
  <path class="divider" d="M${c.x} ${c.y + 88}H${c.x + c.w}" stroke="${p.dark}"/>
  ${c.lines.map((line, i) => `<text class="body" x="${c.x + 24}" y="${c.y + 122 + i * 25}">${esc(line)}</text>`).join("\n  ")}
  ${c.foot ? `<path class="divider" d="M${c.x} ${c.y + c.h - 46}H${c.x + c.w}" stroke="${p.dark}"/><text class="foot" x="${c.x + 24}" y="${c.y + c.h - 17}">${esc(c.foot)}</text>` : ""}
</g>`;
}

const edges = [
    {
        id: "unit-to-ops",
        from: "baseUnits",
        to: "unitOps",
        points: [[650, 370], [790, 370]],
        tone: "unit",
        label: "compose units",
        labelAt: [670, 343]
    },
    {
        id: "ops-to-aliases",
        from: "unitOps",
        to: "aliases",
        points: [[1290, 370], [1430, 370]],
        tone: "ratio",
        label: "ratio aliases",
        labelAt: [1315, 343]
    },
    {
        id: "number-to-measure",
        from: "numberInputs",
        to: "measures",
        points: [[650, 750], [790, 750]],
        tone: "measure",
        label: "Number.times",
        labelAt: [665, 723]
    },
    {
        id: "measure-to-ops",
        from: "measures",
        to: "measureOps",
        points: [[1290, 750], [1430, 750]],
        tone: "measure",
        label: "divide values",
        labelAt: [1312, 723]
    },
    {
        id: "ops-to-speed",
        from: "measureOps",
        to: "speed",
        points: [[1930, 750], [2070, 750]],
        tone: "ratio",
        label: "typed ratio",
        labelAt: [1958, 723]
    },
    {
        id: "speed-to-presentation",
        from: "speed",
        to: "presentation",
        points: [[2330, 900], [2330, 965]],
        tone: "ratio",
        dashed: true,
        label: "convert",
        labelAt: [2350, 940]
    },
    {
        id: "shape-to-shape-ops",
        from: "shapeInputs",
        to: "shapeOps",
        points: [[650, 1410], [790, 1410]],
        tone: "domain",
        label: "specialized * /",
        labelAt: [658, 1383]
    },
    {
        id: "shape-ops-to-results",
        from: "shapeOps",
        to: "domainResults",
        points: [[1290, 1410], [1360, 1410], [1360, 1585], [2070, 1585]],
        tone: "domain",
        label: "Area / Volume",
        labelAt: [1425, 1558]
    },
    {
        id: "energy-to-results",
        from: "energyInputs",
        to: "domainResults",
        points: [[1930, 1410], [1990, 1410], [1990, 1490], [2070, 1490]],
        tone: "energy",
        label: "Energy / Power",
        labelAt: [1968, 1462]
    },
    {
        id: "presentation-to-results",
        from: "presentation",
        to: "domainResults",
        points: [[2330, 1185], [2330, 1450]],
        tone: "output",
        dashed: true,
        label: "format too",
        labelAt: [2350, 1325]
    },
];

function markerDefs() {
    return Object.entries(palette).map(([name, p]) => `<marker id="arrow-${name}" markerWidth="24" markerHeight="18" refX="22" refY="9" orient="auto" markerUnits="userSpaceOnUse"><path d="M2 2 L22 9 L2 16 Z" fill="${p.dark}" stroke="${p.dark}" stroke-width="1" stroke-dasharray="none"/></marker>`).join("\n  ");
}

function pathD(points) {
    return points.map((p, i) => `${i === 0 ? "M" : "L"}${p[0]} ${p[1]}`).join(" ");
}

function edge(e) {
    const p = palette[e.tone];
    const labelWidth = Math.max(120, e.label.length * 8.2 + 28);
    return `<g id="${e.id}">
  <path class="edge ${e.dashed ? "dashed" : ""}" d="${pathD(e.points)}" stroke="${p.dark}" marker-end="url(#arrow-${e.tone})"/>
  <rect class="edgeLabelBg" x="${e.labelAt[0] - 10}" y="${e.labelAt[1] - 18}" width="${labelWidth}" height="26" rx="6"/>
  <text class="edgeLabel" x="${e.labelAt[0]}" y="${e.labelAt[1]}">${esc(e.label)}</text>
</g>`;
}

function lane({x, y, w, h, title}) {
    return `<rect class="laneBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/><text class="laneTitle" x="${x + 24}" y="${y + 34}">${esc(title)}</text>`;
}

function pointTouchesBox(c, [x, y]) {
    const inX = x >= c.x - 0.1 && x <= c.x + c.w + 0.1;
    const inY = y >= c.y - 0.1 && y <= c.y + c.h + 0.1;
    return ((Math.abs(x - c.x) < 0.1 || Math.abs(x - (c.x + c.w)) < 0.1) && inY) ||
        ((Math.abs(y - c.y) < 0.1 || Math.abs(y - (c.y + c.h)) < 0.1) && inX);
}

function segments(points) {
    return points.slice(1).map((p, i) => ({a: points[i], b: p}));
}

function segmentHitsBox(seg, c, pad = 4) {
    const box = {x: c.x + pad, y: c.y + pad, w: c.w - pad * 2, h: c.h - pad * 2};
    const minX = Math.min(seg.a[0], seg.b[0]);
    const maxX = Math.max(seg.a[0], seg.b[0]);
    const minY = Math.min(seg.a[1], seg.b[1]);
    const maxY = Math.max(seg.a[1], seg.b[1]);
    if (seg.a[0] === seg.b[0]) {
        return seg.a[0] > box.x && seg.a[0] < box.x + box.w && maxY > box.y && minY < box.y + box.h;
    }
    if (seg.a[1] === seg.b[1]) {
        return seg.a[1] > box.y && seg.a[1] < box.y + box.h && maxX > box.x && minX < box.x + box.w;
    }
    throw new Error(`Non-orthogonal segment ${JSON.stringify(seg)}`);
}

function properCross(a, b) {
    if (a.a[1] === a.b[1] && b.a[0] === b.b[0]) {
        const y = a.a[1];
        const x = b.a[0];
        return x > Math.min(a.a[0], a.b[0]) && x < Math.max(a.a[0], a.b[0]) &&
            y > Math.min(b.a[1], b.b[1]) && y < Math.max(b.a[1], b.b[1]);
    }
    if (a.a[0] === a.b[0] && b.a[1] === b.b[1]) return properCross(b, a);
    return false;
}

function validateGeometry() {
    for (const e of edges) {
        if (!pointTouchesBox(cards[e.from], e.points[0])) throw new Error(`${e.id} start does not touch ${e.from}`);
        if (!pointTouchesBox(cards[e.to], e.points[e.points.length - 1])) throw new Error(`${e.id} end does not touch ${e.to}`);
        for (const seg of segments(e.points)) {
            for (const [id, c] of Object.entries(cards)) {
                if (id === e.from || id === e.to) continue;
                if (segmentHitsBox(seg, c)) throw new Error(`${e.id} crosses card ${id}`);
            }
        }
    }
    for (let i = 0; i < edges.length; i += 1) {
        for (let j = i + 1; j < edges.length; j += 1) {
            for (const a of segments(edges[i].points)) {
                for (const b of segments(edges[j].points)) {
                    if (properCross(a, b)) throw new Error(`${edges[i].id} crosses ${edges[j].id}`);
                }
            }
        }
    }
}

validateGeometry();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="measured unit composition flow">
<defs>
  <filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${colors.muted}}
    .laneBox{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.6;stroke-dasharray:12 8}.laneTitle{font-family:"Architects Daughter";font-size:25px;fill:${colors.ink}}
    .card{stroke-width:1.9;filter:url(#softShadow)}.kicker{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}
    .cardTitle{font-family:"Architects Daughter";font-size:23px;fill:${colors.ink}}.body{font-family:"Comic Mono";font-size:13.2px;fill:#334155}
    .foot{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}.divider{stroke-width:1.15;opacity:.45}
    .edge{fill:none;stroke-width:3.55;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:10 8}
    .edgeLabelBg{fill:#FFFFFF;stroke:#E2E8F0;stroke-width:1;opacity:.95}.edgeLabel{font-family:"Comic Mono";font-size:12.7px;fill:#334155}
  </style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 64}" rx="8"/>
<text class="title" x="74" y="88">Unit Composition Flow</text>
<text class="subtitle" x="78" y="121">Unit operators compose typed unit descriptors; value operators compose Measure types, while domain overloads return readable Area, Volume, Energy, and Power results.</text>
${lane({x: 86, y: 212, w: 1970, h: 345, title: "1. compose unit descriptors before values exist"})}
${lane({x: 86, y: 592, w: 2570, h: 400, title: "2. compose measured values with generic operators"})}
${lane({x: 86, y: 1220, w: 2570, h: 420, title: "3. use domain overloads when raw compound types would be noisy"})}
<g id="edges">${edges.map(edge).join("\n")}</g>
<g id="cards">${Object.keys(cards).map(card).join("\n")}</g>
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
