#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-javatimes-diagram-03.svg";
const pngPath = "docs/images/readme-diagrams/utils-javatimes-diagram-03.png";
const W = 1560;
const H = 2100;
const colors = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  axis: "#94A3B8",
  a: "#2563EB",
  b: "#EA580C",
  green: "#16A34A",
  teal: "#0D9488",
  purple: "#9333EA",
  gray: "#64748B",
};

const groups = [
  {
    title: "outside or boundary only",
    x: 90,
    y: 230,
    w: 1380,
    rows: [
      { name: "After", b: [15, 175], note: "A starts after B ends", flag: "no overlap" },
      { name: "StartTouching", b: [15, 250], note: "A.start == B.end", flag: "no overlap" },
      { name: "EndTouching", b: [530, 765], note: "A.end == B.start", flag: "no overlap" },
      { name: "Before", b: [590, 785], note: "A ends before B starts", flag: "no overlap" },
      { name: "NoRelation", b: null, note: "both periods are not comparable", flag: "no overlap" },
    ],
  },
  {
    title: "A contains B",
    x: 90,
    y: 845,
    w: 1380,
    rows: [
      { name: "EnclosingStartTouching", b: [250, 405], note: "same start, B ends inside A", flag: "overlap" },
      { name: "Enclosing", b: [325, 455], note: "B is fully inside A", flag: "overlap" },
      { name: "EnclosingEndTouching", b: [390, 530], note: "B starts inside, same end", flag: "overlap" },
      { name: "ExactMatch", b: [250, 530], note: "A and B have identical bounds", flag: "overlap" },
    ],
  },
  {
    title: "B contains A or cuts into A",
    x: 90,
    y: 1390,
    w: 1380,
    rows: [
      { name: "InsideStartTouching", b: [250, 790], note: "same start, B extends after A", flag: "overlap" },
      { name: "Inside", b: [25, 790], note: "A is fully inside B", flag: "overlap" },
      { name: "InsideEndTouching", b: [25, 530], note: "B starts before A, same end", flag: "overlap" },
      { name: "StartInside", b: [25, 390], note: "A.start is inside B only", flag: "overlap" },
      { name: "EndInside", b: [390, 790], note: "A.end is inside B only", flag: "overlap" },
    ],
  },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function pill(x, y, text, color, w = 110) {
  return `<g transform="translate(${x} ${y})"><rect width="${w}" height="34" rx="10" fill="#FFFFFF" stroke="${color}" stroke-width="1.6"/><text class="pillText" x="${w / 2}" y="23" text-anchor="middle" fill="${color}">${esc(text)}</text></g>`;
}

function periodBar(x, y, w, color, label, dashed = false) {
  const dash = dashed ? ` stroke-dasharray="8 7"` : "";
  return `<g><line x1="${x}" y1="${y}" x2="${x + w}" y2="${y}" stroke="${color}" stroke-width="18" stroke-linecap="round"${dash}/><circle cx="${x}" cy="${y}" r="7" fill="#FFFFFF" stroke="${color}" stroke-width="3"/><circle cx="${x + w}" cy="${y}" r="7" fill="#FFFFFF" stroke="${color}" stroke-width="3"/><text class="barLabel" x="${x + w / 2}" y="${y - 20}" text-anchor="middle" fill="${color}">${esc(label)}</text></g>`;
}

function unknownBounds(x, y, w) {
  return `<g><rect x="${x}" y="${y}" width="${w}" height="30" rx="8" fill="#F8FAFC" stroke="${colors.b}" stroke-width="2" stroke-dasharray="9 7"/><text class="barLabel" x="${x + w / 2}" y="${y + 20}" text-anchor="middle" fill="${colors.b}">B bounds unavailable</text></g>`;
}

function row(g, row, i) {
  const rowY = g.y + 88 + i * 96;
  const scale = 1;
  const baseX = g.x + 195;
  const axisX = g.x + 260;
  const axisW = g.w - 340;
  const aStart = axisX + 250 * scale;
  const aEnd = axisX + 530 * scale;
  const bStart = row.b ? axisX + row.b[0] * scale : axisX + 60;
  const bEnd = row.b ? axisX + row.b[1] * scale : axisX + 760;
  const flagColor = row.flag === "overlap" ? colors.green : colors.gray;
  const graphEnd = Math.max(aEnd, bEnd);
  const pillX = g.x + 1230;
  const axisEnd = pillX - 22;
  return `<g>
    <text class="relation" x="${baseX}" y="${rowY + 9}" text-anchor="end">${esc(row.name)}</text>
    <line class="axis" x1="${axisX}" y1="${rowY}" x2="${axisEnd}" y2="${rowY}"/>
    <text class="tick" x="${aStart}" y="${rowY + 34}" text-anchor="middle">A.start</text>
    <text class="tick" x="${aEnd}" y="${rowY + 34}" text-anchor="middle">A.end</text>
    ${periodBar(aStart, rowY - 11, aEnd - aStart, colors.a, "A")}
    ${row.b ? periodBar(bStart, rowY + 19, bEnd - bStart, colors.b, "B") : unknownBounds(bStart, rowY + 15, bEnd - bStart)}
    ${pill(pillX, rowY - 30, row.flag, flagColor, 120)}
    <text class="note" x="${axisX}" y="${rowY + 62}">${esc(row.note)}</text>
  </g>`;
}

function group(g) {
  const h = 105 + g.rows.length * 96;
  return `<g>
    <rect class="group" x="${g.x}" y="${g.y}" width="${g.w}" height="${h}" rx="8"/>
    <text class="groupTitle" x="${g.x + 28}" y="${g.y + 44}">${esc(g.title)}</text>
    <text class="hint" x="${g.x + 28}" y="${g.y + 70}">Result of: A relationWith B</text>
    ${g.rows.map((r, i) => row(g, r, i)).join("\n")}
  </g>`;
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="PeriodRelation timeline comparison">
<defs>
<filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
<style>
svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}
.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}
.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}
.summary{fill:#EFF6FF;stroke:${colors.a};stroke-width:1.8;filter:url(#softShadow)}
.summaryTitle{font-family:"Architects Daughter";font-size:25px;fill:${colors.ink}}
.summaryText{font-family:"Comic Mono";font-size:14px;fill:${colors.muted}}
.group{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}
.groupTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}
.hint,.note,.tick{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}
.relation{font-family:"Comic Mono";font-size:14px;font-weight:700;fill:${colors.ink}}
.axis{stroke:${colors.axis};stroke-width:1.4;stroke-dasharray:8 7}
.barLabel{font-family:"Comic Mono";font-size:12px;font-weight:700}
.pillText{font-family:"Comic Mono";font-size:12px;font-weight:700}
</style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="38" y="30" width="1484" height="2034" rx="8"/>
<text class="title" x="78" y="88">PeriodRelation - How Two Periods Relate</text>
<text class="subtitle" x="82" y="120">A relationWith B compares A.start/A.end against B.start/B.end; overlapWith excludes only NoRelation, After, StartTouching, EndTouching, and Before.</text>
<rect class="summary" x="90" y="155" width="1380" height="58" rx="8"/>
<text class="summaryTitle" x="120" y="192">Read every row with A fixed</text>
<text class="summaryText" x="525" y="190">Blue bar is the receiver period A. Orange bar is the argument period B. Names are the enum returned by source code.</text>
${groups.map(group).join("\n")}
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
