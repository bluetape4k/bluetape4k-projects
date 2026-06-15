#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/idgenerators/README.md",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/Ksuid.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/KsuidGenerator.kt",
  "utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/BytesBase62.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /KSUID \(K-Sortable Unique ID\)[\s\S]*utils-idgenerators-diagram-06\.png/, "README KSUID layout slot");
assertContains(sources[1], /object Seconds[\s\S]*TIMESTAMP_LEN = 4[\s\S]*PAYLOAD_LEN = 16[\s\S]*MAX_ENCODED_LEN = 27[\s\S]*TOTAL_BYTES = TIMESTAMP_LEN \+ PAYLOAD_LEN/, "Seconds layout constants");
assertContains(sources[1], /object Millis[\s\S]*TIMESTAMP_LEN = 8[\s\S]*PAYLOAD_LEN = 12[\s\S]*MAX_ENCODED_LEN = 27[\s\S]*TOTAL_BYTES = TIMESTAMP_LEN \+ PAYLOAD_LEN/, "Millis layout constants");
assertContains(sources[1], /BytesBase62\.encode\(bytes\)[\s\S]*substring\(0, MAX_ENCODED_LEN\)/, "Base62 27-char rendering");
assertContains(sources[2], /class KsuidGenerator[\s\S]*Ksuid\.Generator = Ksuid\.Seconds[\s\S]*IdGenerator<String> by generator/, "KsuidGenerator adapter");
assertContains(sources[3], /object BytesBase62[\s\S]*encode/, "BytesBase62 encoder");

const width = 3100;
const height = 1740;
const barX = 220;
const barW = 2660;
const byteW = barW / 20;
const rowH = 170;

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
  violet: { fill: "#F5F3FF", stroke: "#7C3AED", dark: "#6D28D9" },
  slate: { fill: "#F8FAFC", stroke: "#64748B", dark: "#475569" },
};

function segment({ x, y, w, h, color, label, title, subtitle }) {
  return `<g>
  <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${color.fill}" stroke="${color.stroke}" stroke-width="3"/>
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

const rows = [
  {
    title: "Ksuid.Seconds",
    subtitle: "20 bytes = 4-byte second timestamp + 16-byte random payload",
    y: 330,
    timeLabel: "4 bytes",
    timeTitle: "seconds timestamp",
    timeSubtitle: "epoch offset from 2014-05-13",
    payloadLabel: "16 bytes",
    payloadTitle: "random payload",
    payloadSubtitle: "SecureRandom bytes",
    timeBytes: 4,
    timeColor: colors.blue,
    payloadColor: colors.amber,
  },
  {
    title: "Ksuid.Millis",
    subtitle: "20 bytes = 8-byte millisecond timestamp + 12-byte random payload",
    y: 1050,
    timeLabel: "8 bytes",
    timeTitle: "millis timestamp",
    timeSubtitle: "epoch offset from 2014-05-13",
    payloadLabel: "12 bytes",
    payloadTitle: "random payload",
    payloadSubtitle: "SecureRandom bytes",
    timeBytes: 8,
    timeColor: colors.violet,
    payloadColor: colors.green,
  },
];

const rowSvg = rows.map((row) => {
  const timeW = row.timeBytes * byteW;
  const boundaryHigh = row.timeBytes === 4 ? "128" : "96";
  const boundaryLow = row.timeBytes === 4 ? "127" : "95";
  const boundaryX = barX + timeW;
  const bitTicks = [
    { x: barX, label: "159" },
    { x: boundaryX - 34, label: boundaryHigh },
    { x: boundaryX + 34, label: boundaryLow },
    { x: barX + barW, label: "0" },
  ].map(({ x, label }) => `<g><path class="tick" d="M${x} ${row.y - 18}V${row.y + rowH + 18}"/><text class="tickText" x="${x}" y="${row.y - 34}" text-anchor="middle">${label}</text></g>`).join("\n");
  return `<g>
  <text class="bitLabel" x="${barX}" y="${row.y - 116}">Most significant bit</text>
  <text class="bitLabel" x="${barX + barW}" y="${row.y - 116}" text-anchor="end">Least significant bit</text>
  <text class="rowTitle" x="${barX}" y="${row.y - 76}">${esc(row.title)}</text>
  <text class="rowSubtitle" x="${barX + 420}" y="${row.y - 76}">${esc(row.subtitle)}</text>
  ${bitTicks}
  ${segment({ x: barX, y: row.y, w: timeW, h: rowH, color: row.timeColor, label: row.timeLabel, title: row.timeTitle, subtitle: row.timeSubtitle })}
  ${segment({ x: barX + timeW, y: row.y, w: barW - timeW, h: rowH, color: row.payloadColor, label: row.payloadLabel, title: row.payloadTitle, subtitle: row.payloadSubtitle })}
</g>`;
}).join("\n");

const cards = [
  card({
    x: 220,
    y: 570,
    w: 1200,
    h: 250,
    color: colors.blue,
    title: "Seconds timestamp",
    lines: ["Ksuid.Seconds.TIMESTAMP_LEN = 4", "timestamp = epoch seconds - 1_400_000_000", "bytes.writeInt(timestamp.toInt())", "placed before the random payload"],
  }),
  card({
    x: 1680,
    y: 570,
    w: 1200,
    h: 250,
    color: colors.amber,
    title: "Seconds payload",
    lines: ["Ksuid.Seconds.PAYLOAD_LEN = 16", "SecureRandom fills 16 payload bytes", "20 bytes total before Base62", "default KsuidGenerator uses this strategy"],
  }),
  card({
    x: 220,
    y: 1290,
    w: 1200,
    h: 250,
    color: colors.violet,
    title: "Millis timestamp",
    lines: ["Ksuid.Millis.TIMESTAMP_LEN = 8", "timestamp = epoch millis - 1_400_000_000_000", "bytes.writeLong(timestamp)", "switch here when ms precision matters"],
  }),
  card({
    x: 1680,
    y: 1290,
    w: 1200,
    h: 250,
    color: colors.green,
    title: "Millis payload",
    lines: ["Ksuid.Millis.PAYLOAD_LEN = 12", "SecureRandom fills 12 payload bytes", "20 bytes total before Base62", "all variants render substring(0, 27)"],
  }),
];

const guides = [
  `<path class="guide" d="M${barX + byteW * 2} ${rows[0].y + rowH} V540 H820 V570" stroke="${colors.blue.dark}"/>`,
  `<path class="guide" d="M${barX + byteW * 12} ${rows[0].y + rowH} V540 H2280 V570" stroke="${colors.amber.dark}"/>`,
  `<path class="guide" d="M${barX + byteW * 4} ${rows[1].y + rowH} V1260 H820 V1290" stroke="${colors.violet.dark}"/>`,
  `<path class="guide" d="M${barX + byteW * 14} ${rows[1].y + rowH} V1260 H2280 V1290" stroke="${colors.green.dark}"/>`,
];

const svg = `<svg data-intent="Explain KSUID second and millisecond layouts from current source constants." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="KSUID Layout">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}
    .frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:58px;fill:#0F172A}
    .subtitle,.rowSubtitle{font-family:"Comic Mono";font-size:18px;fill:#475569}
    .rowTitle{font-family:"Architects Daughter";font-size:34px;fill:#0F172A}
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
<text class="title" x="76" y="104">KSUID Layout</text>
<text class="subtitle" x="80" y="142">Two 20-byte layouts render to the same 27-character Base62 ID: seconds precision or milliseconds precision.</text>
${rowSvg}
${guides.join("\n")}
${cards.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-idgenerators-diagram-06.svg");
const pngPath = join(OUT, "utils-idgenerators-diagram-06.png");
writeFileSync(svgPath, svg);
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--output-width", String(width * 2), "--output-height", String(height * 2)], { stdio: "inherit" });
console.log("Generated utils-idgenerators-diagram-06.svg/png");
