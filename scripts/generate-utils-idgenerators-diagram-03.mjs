#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/idgenerators/README.md",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/SnowflakeSupport.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/SnowflakeId.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/sequencer/DefaultSequencer.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/snowflake/sequencer/GlobalSequencer.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Snowflake Bit Layout[\s\S]*utils-idgenerators-diagram-03\.png/, "README bit layout slot");
assertContains(sources[1], /TOTAL_BITS = 64[\s\S]*TIMESTAMP_BITS = 42[\s\S]*MACHINE_BITS = 10[\s\S]*SEQUENCE_BITS = 12/, "bit constants");
assertContains(sources[1], /EPOCH = 1420070400000L/, "custom epoch");
assertContains(sources[1], /makeId[\s\S]*timestamp - EPOCH[\s\S]*machineId shl MACHINE_ID_SHIFT[\s\S]*sequence/, "makeId packing");
assertContains(sources[1], /parseSnowflakeId[\s\S]*id ushr TIME_STAMP_SHIFT[\s\S]*MAX_MACHINE_ID_BIT[\s\S]*MAX_SEQUENCE_BIT/, "parse unpacking");
assertContains(sources[2], /data class SnowflakeId[\s\S]*timestamp: Long[\s\S]*machineId: Int[\s\S]*sequence: Int/, "SnowflakeId fields");
assertContains(sources[3], /sequence >= MAX_SEQUENCE[\s\S]*sequence = 0/, "Default sequence rollover");
assertContains(sources[4], /GlobalSequencer[\s\S]*machineId[\s\S]*sequence/, "Global sequence fields");

const width = 3000;
const height = 1700;
const barX = 170;
const barY = 430;
const barW = 2660;
const bitW = barW / 64;
const barH = 190;

const fields = [
  {
    name: "timestamp",
    bits: 42,
    x: barX,
    w: bitW * 42,
    fill: "#E0F2FE",
    stroke: "#0284C7",
    dark: "#0369A1",
    range: "bits 63..22",
    note: "milliseconds since custom epoch",
    cardX: 190,
    cardY: 760,
    cardW: 760,
  },
  {
    name: "machineId",
    bits: 10,
    x: barX + bitW * 42,
    w: bitW * 10,
    fill: "#DCFCE7",
    stroke: "#16A34A",
    dark: "#15803D",
    range: "bits 21..12",
    note: "0..1023 machine slot",
    cardX: 1120,
    cardY: 760,
    cardW: 560,
  },
  {
    name: "sequence",
    bits: 12,
    x: barX + bitW * 52,
    w: bitW * 12,
    fill: "#FEF3C7",
    stroke: "#D97706",
    dark: "#B45309",
    range: "bits 11..0",
    note: "0..4095 per millisecond",
    cardX: 1850,
    cardY: 760,
    cardW: 760,
  },
];

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function segment(field) {
  const cx = field.x + field.w / 2;
  const cardCx = field.cardX + field.cardW / 2;
  return `<g>
  <rect x="${field.x}" y="${barY}" width="${field.w}" height="${barH}" rx="8" fill="${field.fill}" stroke="${field.stroke}" stroke-width="3"/>
  <text class="segmentBits" x="${cx}" y="${barY + 58}" text-anchor="middle">${field.bits} bits</text>
  <text class="segmentTitle" x="${cx}" y="${barY + 112}" text-anchor="middle">${esc(field.name)}</text>
  <text class="segmentRange" x="${cx}" y="${barY + 152}" text-anchor="middle">${esc(field.range)}</text>
  <path class="guide" d="M${cx} ${barY + barH} V${field.cardY - 36} H${cardCx} V${field.cardY}" stroke="${field.dark}"/>
</g>`;
}

function card({ x, y, w, h, color, title, lines }) {
  return `<g>
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${color.fill}" stroke="${color.stroke}"/>
  <text class="cardTitle" x="${x + w / 2}" y="${y + 54}" text-anchor="middle">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 82}H${x + w}" stroke="${color.dark}"/>
  ${lines.map((line, index) => `<text class="cardText" x="${x + 28}" y="${y + 122 + index * 34}">${esc(line)}</text>`).join("\n")}
</g>`;
}

const colors = {
  blue: { fill: "#EFF6FF", stroke: "#2563EB", dark: "#1D4ED8" },
  green: { fill: "#F0FDF4", stroke: "#16A34A", dark: "#15803D" },
  amber: { fill: "#FFF7ED", stroke: "#EA580C", dark: "#C2410C" },
  slate: { fill: "#F8FAFC", stroke: "#64748B", dark: "#475569" },
  violet: { fill: "#F5F3FF", stroke: "#7C3AED", dark: "#6D28D9" },
};

const cards = [
  card({
    x: 190,
    y: 760,
    w: 760,
    h: 240,
    color: colors.blue,
    title: "Timestamp",
    lines: ["TIMESTAMP_BITS = 42", "stored as timestamp - EPOCH", "EPOCH = 2015-01-01T00:00:00Z", "parse: (id >>> 22) + EPOCH"],
  }),
  card({
    x: 1120,
    y: 760,
    w: 560,
    h: 240,
    color: colors.green,
    title: "Machine",
    lines: ["MACHINE_BITS = 10", "MAX_MACHINE_ID = 1024", "mask = 1023", "parse: (id >>> 12) & mask"],
  }),
  card({
    x: 1850,
    y: 760,
    w: 760,
    h: 240,
    color: colors.amber,
    title: "Sequence",
    lines: ["SEQUENCE_BITS = 12", "MAX_SEQUENCE = 4096", "mask = 4095", "rolls over on next millisecond"],
  }),
  card({
    x: 310,
    y: 1130,
    w: 1120,
    h: 270,
    color: colors.violet,
    title: "Packing",
    lines: ["makeId(timestamp, machineId, sequence)", "((timestamp - EPOCH) << 22)", "| (machineId << 12)", "| sequence"],
  }),
  card({
    x: 1570,
    y: 1130,
    w: 1120,
    h: 270,
    color: colors.slate,
    title: "Result",
    lines: ["SnowflakeId(timestamp, machineId, sequence)", "value: Long by makeId(...)", "valueAsString: base-36 Long string", "DefaultSequencer uses ReentrantLock"],
  }),
];

const ticks = [63, 22, 21, 12, 11, 0].map((bit, index) => {
  const x = index === 0 ? barX : index === 5 ? barX + barW : barX + bitW * (64 - bit - (index % 2));
  return `<g><path class="tick" d="M${x} ${barY - 18}V${barY + barH + 18}"/><text class="tickText" x="${x}" y="${barY - 34}" text-anchor="middle">${bit}</text></g>`;
}).join("\n");

const svg = `<svg data-intent="Explain the Snowflake 64-bit layout from current constants and make/parse functions." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Snowflake Bit Layout">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}
    .frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:58px;fill:#0F172A}
    .subtitle{font-family:"Comic Mono";font-size:18px;fill:#475569}
    .bitLabel{font-family:"Comic Mono";font-size:18px;fill:#475569}
    .segmentBits{font-family:"Comic Mono";font-size:28px;fill:#0F172A;font-weight:700}
    .segmentTitle{font-family:"Architects Daughter";font-size:38px;fill:#0F172A}
    .segmentRange{font-family:"Comic Mono";font-size:18px;fill:#334155}
    .card{filter:url(#shadow);stroke-width:2}
    .cardTitle{font-family:"Architects Daughter";font-size:34px;fill:#0F172A}
    .cardText{font-family:"Comic Mono";font-size:18px;fill:#334155}
    .divider{stroke-width:1;opacity:.36}
    .guide{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round;opacity:.78}
    .tick{stroke:#94A3B8;stroke-width:1.5;stroke-dasharray:6 8}
    .tickText{font-family:"Comic Mono";font-size:17px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="36" y="30" width="${width - 72}" height="${height - 60}" rx="8"/>
<text class="title" x="76" y="104">Snowflake Bit Layout</text>
<text class="subtitle" x="80" y="142">64-bit Long = 42-bit timestamp + 10-bit machine id + 12-bit sequence, packed by SnowflakeSupport.makeId.</text>
<text class="bitLabel" x="${barX}" y="${barY - 86}">Most significant bit</text>
<text class="bitLabel" x="${barX + barW}" y="${barY - 86}" text-anchor="end">Least significant bit</text>
${ticks}
${fields.map(segment).join("\n")}
${cards.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-idgenerators-diagram-03.svg");
const pngPath = join(OUT, "utils-idgenerators-diagram-03.png");
writeFileSync(svgPath, svg);
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--output-width", String(width * 2), "--output-height", String(height * 2)], { stdio: "inherit" });
console.log("Generated utils-idgenerators-diagram-03.svg/png");
