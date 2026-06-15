#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/idgenerators/README.md",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/flake/Flake.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/utils/node/NodeIdentifier.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/utils/node/MacAddressNodeIdentifier.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Flake \(Boundary-style 128-bit\)[\s\S]*utils-idgenerators-diagram-07\.png/, "README Flake layout slot");
assertContains(sources[1], /class Flake[\s\S]*IdGenerator<ByteArray>/, "Flake generator contract");
assertContains(sources[1], /ID_SIZE_BYTES = 16[\s\S]*NODE_ID_BYTES = 6/, "Flake byte constants");
assertContains(sources[1], /putLong\(currentTime\)[\s\S]*put\(nodeId\)[\s\S]*putShort\(sequence\.toShort\(\)\)/, "Flake byte packing order");
assertContains(sources[1], /asHexString[\s\S]*asBase62String[\s\S]*asComponentString/, "Flake render helpers");
assertContains(sources[1], /MAX_SEQ = 0xFFFF[\s\S]*ReentrantLock[\s\S]*atomic\(0\)/, "Flake state management");
assertContains(sources[2], /interface NodeIdentifier/, "NodeIdentifier contract");
assertContains(sources[3], /class MacAddressNodeIdentifier[\s\S]*NodeIdentifier/, "MacAddress node identifier");

const width = 3000;
const height = 1080;
const barX = 200;
const barY = 330;
const barW = 2600;
const byteW = barW / 16;
const barH = 180;

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

const colors = {
  blue: { fill: "#EFF6FF", stroke: "#2563EB", dark: "#1D4ED8" },
  green: { fill: "#F0FDF4", stroke: "#16A34A", dark: "#15803D" },
  amber: { fill: "#FFF7ED", stroke: "#EA580C", dark: "#C2410C" },
  violet: { fill: "#F5F3FF", stroke: "#7C3AED", dark: "#6D28D9" },
  slate: { fill: "#F8FAFC", stroke: "#64748B", dark: "#475569" },
};

function segment({ x, y, w, color, label, title, subtitle }) {
  return `<g>
  <rect x="${x}" y="${y}" width="${w}" height="${barH}" rx="8" fill="${color.fill}" stroke="${color.stroke}" stroke-width="3"/>
  <text class="segmentBits" x="${x + w / 2}" y="${y + 50}" text-anchor="middle">${esc(label)}</text>
  <text class="segmentTitle" x="${x + w / 2}" y="${y + 98}" text-anchor="middle">${esc(title)}</text>
  <text class="segmentRange" x="${x + w / 2}" y="${y + 136}" text-anchor="middle">${esc(subtitle)}</text>
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

const segments = [
  segment({
    x: barX,
    y: barY,
    w: byteW * 8,
    color: colors.blue,
    label: "8 bytes",
    title: "timestamp",
    subtitle: "clock.millis() as Long",
  }),
  segment({
    x: barX + byteW * 8,
    y: barY,
    w: byteW * 6,
    color: colors.green,
    label: "6 bytes",
    title: "node id",
    subtitle: "NodeIdentifier default: MAC",
  }),
  segment({
    x: barX + byteW * 14,
    y: barY,
    w: byteW * 2,
    color: colors.amber,
    label: "2 bytes",
    title: "sequence",
    subtitle: "0..65535 per millisecond",
  }),
];

const cards = [
  card({
    x: 230,
    y: 690,
    w: 590,
    h: 250,
    color: colors.blue,
    title: "Timestamp packing",
    lines: ["putLong(currentTime)", "clock.millis() source", "stored first for ordering", "part of 16-byte ID"],
  }),
  card({
    x: 900,
    y: 690,
    w: 590,
    h: 250,
    color: colors.green,
    title: "Node source",
    lines: ["NODE_ID_BYTES = 6", "NodeIdentifier.get()", "MacAddressNodeIdentifier default", "copied after timestamp"],
  }),
  card({
    x: 1570,
    y: 690,
    w: 590,
    h: 250,
    color: colors.amber,
    title: "Sequence state",
    lines: ["putShort(sequence)", "atomic sequence counter", "MAX_SEQ = 0xFFFF", "waits for next millis on overflow"],
  }),
  card({
    x: 2240,
    y: 690,
    w: 590,
    h: 250,
    color: colors.slate,
    title: "Render helpers",
    lines: ["nextId(): ByteArray", "nextIdAsString(): Base62", "asHexString(id)", "asComponentString(id)"],
  }),
];

const guides = [
  `<path class="guide" d="M${barX + byteW * 4} ${barY + barH} V640 H525 V690" stroke="${colors.blue.dark}"/>`,
  `<path class="guide" d="M${barX + byteW * 11} ${barY + barH} V640 H1195 V690" stroke="${colors.green.dark}"/>`,
  `<path class="guide" d="M${barX + byteW * 15} ${barY + barH} V640 H1865 V690" stroke="${colors.amber.dark}"/>`,
  `<path class="guide" d="M${barX + byteW * 15} ${barY + barH} V640 H2535 V690" stroke="${colors.slate.dark}"/>`,
];

const bitTicks = [
  { x: barX, label: "127" },
  { x: barX + byteW * 8 - 34, label: "64" },
  { x: barX + byteW * 8 + 34, label: "63" },
  { x: barX + byteW * 14 - 34, label: "16" },
  { x: barX + byteW * 14 + 34, label: "15" },
  { x: barX + barW, label: "0" },
].map(({ x, label }) => `<g><path class="tick" d="M${x} ${barY - 18}V${barY + barH + 18}"/><text class="tickText" x="${x}" y="${barY - 34}" text-anchor="middle">${label}</text></g>`).join("\n");

const svg = `<svg data-intent="Explain Flake 128-bit byte layout from current source packing order." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Flake Layout">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}
    .frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:58px;fill:#0F172A}
    .subtitle{font-family:"Comic Mono";font-size:18px;fill:#475569}
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
<text class="title" x="76" y="104">Flake Layout</text>
<text class="subtitle" x="80" y="142">128-bit byte array: 8-byte timestamp + 6-byte node id + 2-byte sequence.</text>
<text class="bitLabel" x="${barX}" y="${barY - 86}">Most significant bit</text>
<text class="bitLabel" x="${barX + barW}" y="${barY - 86}" text-anchor="end">Least significant bit</text>
${bitTicks}
${segments.join("\n")}
${guides.join("\n")}
${cards.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-idgenerators-diagram-07.svg");
const pngPath = join(OUT, "utils-idgenerators-diagram-07.png");
writeFileSync(svgPath, svg);
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--output-width", String(width * 2), "--output-height", String(height * 2)], { stdio: "inherit" });
console.log("Generated utils-idgenerators-diagram-07.svg/png");
