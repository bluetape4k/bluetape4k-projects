#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/idgenerators/README.md",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/ULID.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/internal/Constants.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/internal/ULIDValue.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/internal/ULIDFactory.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/internal/Crockford.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /ULID \(Universally Unique Lexicographically Sortable Identifier\)[\s\S]*utils-idgenerators-diagram-04\.png/, "README ULID layout slot");
assertContains(sources[1], /interface ULID[\s\S]*mostSignificantBits[\s\S]*leastSignificantBits[\s\S]*timestamp/, "ULID contract");
assertContains(sources[2], /Mask58BitsCount = 5[\s\S]*TimestampOverflowMask[\s\S]*Mask16Bits/, "ULID bit masks");
assertContains(sources[3], /timestamp: Long[\s\S]*mostSignificantBits ushr 16[\s\S]*toString\(\)[\s\S]*buffer\.write\(timestamp, 10, 0\)/, "ULIDValue layout and string rendering");
assertContains(sources[4], /randomULID[\s\S]*randomHigh[\s\S]*randomLow[\s\S]*nextULID[\s\S]*timestamp shl 16/, "ULIDFactory random fields");
assertContains(sources[5], /CharArray\.write[\s\S]*Mask58BitsCount[\s\S]*EncodingChars/, "Crockford writer");

const width = 3000;
const height = 1900;
const barX = 170;
const barY = 420;
const barW = 2660;
const barH = 180;
const bitW = barW / 128;
const stringY = 780;
const stringH = 170;
const charW = barW / 26;

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

const colors = {
  blue: { fill: "#EFF6FF", stroke: "#2563EB", dark: "#1D4ED8" },
  amber: { fill: "#FFF7ED", stroke: "#EA580C", dark: "#C2410C" },
  green: { fill: "#F0FDF4", stroke: "#16A34A", dark: "#15803D" },
  slate: { fill: "#F8FAFC", stroke: "#64748B", dark: "#475569" },
  violet: { fill: "#F5F3FF", stroke: "#7C3AED", dark: "#6D28D9" },
};

function rectText({ x, y, w, h, color, bits, title, subtitle }) {
  return `<g>
  <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${color.fill}" stroke="${color.stroke}" stroke-width="3"/>
  <text class="segmentBits" x="${x + w / 2}" y="${y + 54}" text-anchor="middle">${esc(bits)}</text>
  <text class="segmentTitle" x="${x + w / 2}" y="${y + 104}" text-anchor="middle">${esc(title)}</text>
  <text class="segmentRange" x="${x + w / 2}" y="${y + 143}" text-anchor="middle">${esc(subtitle)}</text>
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

const bitSegments = [
  rectText({
    x: barX,
    y: barY,
    w: bitW * 48,
    h: barH,
    color: colors.blue,
    bits: "48 bits",
    title: "timestamp",
    subtitle: "mostSignificantBits >>> 16",
  }),
  rectText({
    x: barX + bitW * 48,
    y: barY,
    w: bitW * 80,
    h: barH,
    color: colors.amber,
    bits: "80 bits",
    title: "randomness",
    subtitle: "16 high bits + 64 low bits",
  }),
];

const stringSegments = [
  rectText({
    x: barX,
    y: stringY,
    w: charW * 10,
    h: stringH,
    color: colors.green,
    bits: "10 chars",
    title: "time string",
    subtitle: "write(timestamp, 10, offset 0)",
  }),
  rectText({
    x: barX + charW * 10,
    y: stringY,
    w: charW * 16,
    h: stringH,
    color: colors.violet,
    bits: "16 chars",
    title: "random string",
    subtitle: "80 random bits in Crockford Base32",
  }),
];

const cards = [
  card({
    x: 170,
    y: 1060,
    w: 1200,
    h: 290,
    color: colors.blue,
    title: "Timestamp bits",
    lines: ["48-bit millisecond timestamp", "stored in mostSignificantBits", "timestamp = msb >>> 16", "10 time chars render this field"],
  }),
  card({
    x: 1630,
    y: 1060,
    w: 1200,
    h: 290,
    color: colors.amber,
    title: "Randomness bits",
    lines: ["80-bit random payload", "randomHigh: 16 bits", "randomLow: 64 bits", "Factory writes both into ULIDValue", "monotonic variants increment payload"],
  }),
  card({
    x: 170,
    y: 1450,
    w: 1200,
    h: 250,
    color: colors.green,
    title: "Time string",
    lines: ["first 10 Crockford chars", "buffer.write(timestamp, 10, 0)", "50 char-slots encode 48 useful bits", "lexicographic order starts here"],
  }),
  card({
    x: 1630,
    y: 1450,
    w: 1200,
    h: 250,
    color: colors.violet,
    title: "Random string",
    lines: ["last 16 Crockford chars", "80 random bits total", "source code writes this as 8 + 8 chars", "not an 80-bit-to-8-char mapping"],
  }),
];

const guides = [
  `<path class="guide" d="M${barX + bitW * 24} ${barY + barH} V690 H90 V1010 H770 V1060" stroke="${colors.blue.dark}"/>`,
  `<path class="guide" d="M${barX + bitW * 88} ${barY + barH} V690 H2910 V1010 H2230 V1060" stroke="${colors.amber.dark}"/>`,
  `<path class="guide" d="M${barX + charW * 5} ${stringY + stringH} V1400 H770 V1450" stroke="${colors.green.dark}"/>`,
  `<path class="guide" d="M${barX + charW * 18} ${stringY + stringH} V1400 H2230 V1450" stroke="${colors.violet.dark}"/>`,
];

const bitTicks = [
  { x: barX, label: "127" },
  { x: barX + bitW * 48 - 34, label: "80" },
  { x: barX + bitW * 48 + 34, label: "79" },
  { x: barX + barW, label: "0" },
].map(({ x, label }) => `<g><path class="tick" d="M${x} ${barY - 18}V${barY + barH + 18}"/><text class="tickText" x="${x}" y="${barY - 34}" text-anchor="middle">${label}</text></g>`).join("\n");

const sample = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

const svg = `<svg data-intent="Explain ULID timestamp/randomness layout from current ULID source." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="ULID Layout">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}
    .frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:58px;fill:#0F172A}
    .subtitle,.sample{font-family:"Comic Mono";font-size:18px;fill:#475569}
    .sampleValue{font-family:"Comic Mono";font-size:34px;fill:#0F172A;font-weight:700;letter-spacing:0}
    .segmentBits{font-family:"Comic Mono";font-size:26px;fill:#0F172A;font-weight:700}
    .segmentTitle{font-family:"Architects Daughter";font-size:36px;fill:#0F172A}
    .segmentRange{font-family:"Comic Mono";font-size:17px;fill:#334155}
    .card{filter:url(#shadow);stroke-width:2}
    .cardTitle{font-family:"Architects Daughter";font-size:34px;fill:#0F172A}
    .cardText{font-family:"Comic Mono";font-size:18px;fill:#334155}
    .divider{stroke-width:1;opacity:.36}
    .guide{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round;opacity:.75}
    .bitLabel{font-family:"Comic Mono";font-size:18px;fill:#475569}
    .tick{stroke:#94A3B8;stroke-width:1.5;stroke-dasharray:6 8}
    .tickText{font-family:"Comic Mono";font-size:17px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="36" y="30" width="${width - 72}" height="${height - 60}" rx="8"/>
<text class="title" x="76" y="104">ULID Layout</text>
<text class="subtitle" x="80" y="142">128-bit value: 48-bit timestamp + 80-bit randomness; 26 Crockford chars render 10 time + 16 random chars.</text>
<text class="sample" x="${barX}" y="228">example</text>
<text class="sampleValue" x="${barX}" y="278">${sample}</text>
<text class="bitLabel" x="${barX}" y="${barY - 108}">Most significant bit</text>
<text class="bitLabel" x="${barX + barW}" y="${barY - 108}" text-anchor="end">Least significant bit</text>
${bitTicks}
${bitSegments.join("\n")}
${stringSegments.join("\n")}
${guides.join("\n")}
${cards.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-idgenerators-diagram-04.svg");
const pngPath = join(OUT, "utils-idgenerators-diagram-04.png");
writeFileSync(svgPath, svg);
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--output-width", String(width * 2), "--output-height", String(height * 2)], { stdio: "inherit" });
console.log("Generated utils-idgenerators-diagram-04.svg/png");
