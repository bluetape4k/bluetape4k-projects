#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const outDir = "docs/images/readme-charts";
const rsvgConvert = "/opt/homebrew/bin/rsvg-convert";

mkdirSync(outDir, { recursive: true });

const frameMargin = 34;
const innerBottomMargin = 34;
const bodyNoteGap = 28;
const noteFooterGap = 16;
const noteH = 58;
const footerH = 50;

const colors = {
  ink: "#21334A",
  muted: "#536476",
  canvas: "#F7F9FC",
  frame: "#D6E2ED",
  grid: "#D7E2EC",
  axis: "#AEBFD1",
  blue: ["#E7F1FF", "#5A85D6"],
  green: ["#EAF7ED", "#58A978"],
  teal: ["#E6F7F5", "#38A69E"],
  amber: ["#FFF3D8", "#D6A441"],
  purple: ["#F1ECFF", "#8A72D6"],
  pink: ["#FCE7F3", "#DB7890"],
  red: ["#FFE7EC", "#D85D74"],
  olive: ["#EEF6D9", "#8BA84D"],
  gray: ["#F5F7FA", "#8FA1B3"],
};

const series = [colors.blue, colors.green, colors.teal, colors.amber, colors.purple, colors.pink, colors.olive, colors.red];

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function fmt(value, digits = 0) {
  return Number(value).toLocaleString("en-US", {
    maximumFractionDigits: digits,
    minimumFractionDigits: Number.isInteger(value) ? 0 : Math.min(digits, 2),
  });
}

function text(cls, x, y, value, attrs = "") {
  return `<text class="${cls}" x="${x}" y="${y}" ${attrs}>${esc(value)}</text>`;
}

function base(width, height, title, subtitle) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="title desc">
<title id="title">${esc(title)}</title>
<desc id="desc">${esc(subtitle)}</desc>
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="7" stdDeviation="8" flood-color="#203040" flood-opacity="0.10"/></filter>
  <style>
    .canvas{fill:${colors.canvas}}
    .frame{fill:#FFFFFF;stroke:${colors.frame};stroke-width:2}
    .panel{fill:#FFFFFF;stroke:${colors.frame};stroke-width:2;filter:url(#shadow)}
    .note{fill:#F7F9FC;stroke:${colors.grid};stroke-width:1.6}
    .title{font-family:"Architects Daughter";font-size:42px;fill:${colors.ink};font-weight:700}
    .subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}
    .panelTitle{font-family:"Architects Daughter";font-size:25px;fill:${colors.ink};font-weight:700}
    .label{font-family:"Architects Daughter";font-size:21px;fill:${colors.ink};font-weight:700}
    .body{font-family:"Comic Mono";font-size:14px;fill:#34465B}
    .small{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}
    .tiny{font-family:"Comic Mono";font-size:11px;fill:#657386}
    .value{font-family:"Comic Mono";font-size:13px;fill:#26384F;font-weight:700}
    .grid{stroke:${colors.grid};stroke-width:1;stroke-dasharray:4 7}
    .axis{stroke:${colors.axis};stroke-width:1.6}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="${frameMargin}" y="${frameMargin}" width="${width - frameMargin * 2}" height="${height - frameMargin * 2}" rx="26"/>
${text("title", 70, 82, title)}
${text("subtitle", 72, 116, subtitle)}`;
}

function footer(y, width, source, rule) {
  return `<g>
  <rect class="note" x="94" y="${y}" width="${width - 188}" height="50" rx="16"/>
  ${text("small", width / 2, y + 18, `Source: ${source}`, 'text-anchor="middle" dominant-baseline="middle"')}
  ${text("tiny", width / 2, y + 36, rule, 'text-anchor="middle" dominant-baseline="middle"')}
</g>`;
}

function legend(x, y, items) {
  return `<g>${items.map((item, index) => {
    const [fill, stroke] = item.color ?? series[index % series.length];
    const lx = x + index * item.step;
    return `<rect x="${lx}" y="${y - 12}" width="20" height="13" rx="4" fill="${fill}" stroke="${stroke}" stroke-width="1.5"/>
${text("small", lx + 30, y - 3, item.label, 'dominant-baseline="middle"')}`;
  }).join("\n")}</g>`;
}

function axis(plotX, plotY, plotW, plotH, max, ticks, suffix = "") {
  const lines = ticks.map((tick) => {
    const x = plotX + (tick / max) * plotW;
    return `<line class="grid" x1="${x}" y1="${plotY}" x2="${x}" y2="${plotY + plotH}"/>
${text("tiny", x, plotY + plotH + 25, `${fmt(tick)}${suffix}`, 'text-anchor="middle"')}`;
  });
  lines.push(`<line class="axis" x1="${plotX}" y1="${plotY + plotH}" x2="${plotX + plotW}" y2="${plotY + plotH}"/>`);
  return lines.join("\n");
}

function bar(plotX, y, plotW, max, value, color, label, options = {}) {
  const [fill, stroke] = color;
  const h = options.height ?? 18;
  const w = Math.max(value > 0 ? 8 : 0, (value / max) * plotW);
  const digits = options.digits ?? 0;
  const unit = options.unit ?? "";
  const valueX = Math.min(plotX + plotW - 4, plotX + w + 12);
  const anchor = valueX >= plotX + plotW - 4 ? 'text-anchor="end"' : "";
  return `<g>
  <rect x="${plotX}" y="${y}" width="${plotW}" height="${h}" rx="6" fill="#EEF4F9" stroke="${colors.grid}" stroke-width="1.4"/>
  <rect x="${plotX}" y="${y}" width="${w}" height="${h}" rx="6" fill="${fill}" stroke="${stroke}" stroke-width="1.5"/>
  ${text("value", valueX, y + h / 2 + 1, `${label}${fmt(value, digits)}${unit}`, `${anchor} dominant-baseline="middle"`)}
</g>`;
}

function rankingChart({ name, title, subtitle, source, unit, max, ticks, rows, note, width = 1720, rowGap = 48, top = 170, plotX = 560 }) {
  const plotW = width - plotX - 330;
  const plotH = rows.length * rowGap + 10;
  const plotBottom = top + plotH + 45;
  const bottom = bottomBands(plotBottom, noteH, 720);
  const height = bottom.height;
  const body = rows.map((row, index) => {
    const y = top + index * rowGap + 10;
    return `<g>
  ${text("label", 96, y + 14, row.label, 'dominant-baseline="middle"')}
  ${row.detail ? text("small", 98, y + 34, row.detail) : ""}
  ${bar(plotX, y, plotW, max, row.value, row.color ?? series[index % series.length], "", { unit: ` ${unit}`, digits: row.digits ?? 0 })}
</g>`;
  }).join("\n");
  const svg = `${base(width, height, title, subtitle)}
<g>
  <rect class="panel" x="70" y="142" width="${width - 140}" height="${bottom.noteY - 166}" rx="22"/>
  ${text("panelTitle", 96, 184, "Measured ranking")}
  ${text("small", width - 190, 184, `0 to ${fmt(max)} ${unit}`, 'text-anchor="end"')}
  ${axis(plotX, top, plotW, plotH, max, ticks)}
  ${body}
</g>
<g>
  <rect class="note" x="94" y="${bottom.noteY}" width="${width - 188}" height="${noteH}" rx="16"/>
  ${text("small", width / 2, bottom.noteY + 23, note, 'text-anchor="middle" dominant-baseline="middle"')}
  ${text("tiny", width / 2, bottom.noteY + 42, "Higher is better. Keep units, benchmark mode, and source table visible with the chart.", 'text-anchor="middle" dominant-baseline="middle"')}
</g>
${footer(bottom.footerY, width, source, "bluetape4k-projects - github.com/bluetape4k/bluetape4k-projects")}
</svg>`;
  writeChart(name, svg, `${name}: rows=${rows.length} max=${max} unit=${unit}`);
}

function groupedBarsChart({ name, title, subtitle, source, unit, max, ticks, groups, seriesLabels, note, width = 1760 }) {
  const rowGap = 80;
  const top = 190;
  const plotX = 570;
  const plotW = width - plotX - 270;
  const plotH = groups.length * rowGap + 10;
  const plotBottom = top + plotH + 45;
  const bottom = bottomBands(plotBottom, 60);
  const height = bottom.height;
  const bars = groups.map((group, groupIndex) => {
    const groupY = top + groupIndex * rowGap + 12;
    return `<g>
  ${text("label", 100, groupY + 18, group.label, 'dominant-baseline="middle"')}
  ${group.detail ? text("small", 102, groupY + 42, group.detail) : ""}
  ${group.values.map((value, index) => bar(plotX, groupY + index * 22, plotW, max, value, series[index % series.length], `${seriesLabels[index]} `, { unit: ` ${unit}`, digits: group.digits ?? 0, height: 15 })).join("\n")}
</g>`;
  }).join("\n");
  const svg = `${base(width, height, title, subtitle)}
${legend(width - 650, 110, seriesLabels.map((label) => ({ label, step: 205 })))}
<g>
  <rect class="panel" x="70" y="150" width="${width - 140}" height="${bottom.noteY - 174}" rx="22"/>
  ${text("panelTitle", 100, 194, "Source table rows")}
  ${text("small", width - 188, 194, `0 to ${fmt(max)} ${unit}`, 'text-anchor="end"')}
  ${axis(plotX, top, plotW, plotH, max, ticks)}
  ${bars}
</g>
<g>
  <rect class="note" x="94" y="${bottom.noteY}" width="${width - 188}" height="60" rx="16"/>
  ${text("small", width / 2, bottom.noteY + 23, note, 'text-anchor="middle" dominant-baseline="middle"')}
  ${text("tiny", width / 2, bottom.noteY + 43, "Use grouped rows only when each bar shares the same unit and benchmark mode.", 'text-anchor="middle" dominant-baseline="middle"')}
</g>
${footer(bottom.footerY, width, source, "bluetape4k-projects - github.com/bluetape4k/bluetape4k-projects")}
</svg>`;
  writeChart(name, svg, `${name}: groups=${groups.length} series=${seriesLabels.length} max=${max} unit=${unit}`);
}

function splitPanelsChart({ name, title, subtitle, source, panels, note, width = 1800 }) {
  const maxRows = Math.max(...panels.map((panel) => panel.rows.length));
  const rowGap = 105;
  const panelTop = 150;
  const panelH = Math.max(430, maxRows * rowGap + 155);
  const bottom = bottomBands(panelTop + panelH, noteH);
  const height = bottom.height;
  const panelWidth = (width - 180) / panels.length;
  const body = panels.map((panel, panelIndex) => {
    const x = 70 + panelIndex * (panelWidth + 40);
    const plotX = x + 300;
    const plotY = panelTop + 92;
    const plotW = panelWidth - 370;
    const plotH = rowGap * panel.rows.length + 8;
    return `<g>
  <rect class="panel" x="${x}" y="${panelTop}" width="${panelWidth}" height="${panelH}" rx="22"/>
  ${text("panelTitle", x + 28, panelTop + 42, panel.title)}
  ${text("small", x + panelWidth - 28, panelTop + 42, `0 to ${fmt(panel.max, panel.digits ?? 0)} ${panel.unit}`, 'text-anchor="end"')}
  ${axis(plotX, plotY, plotW, plotH, panel.max, panel.ticks)}
  ${panel.rows.map((row, index) => {
    const y = plotY + index * rowGap + 13;
    return `<g>
  ${text("label", x + 30, y + 12, row.label, 'dominant-baseline="middle"')}
  ${row.detail ? text("small", x + 32, y + 35, row.detail) : ""}
  ${bar(plotX, y, plotW, panel.max, row.value, row.color ?? series[index % series.length], "", { unit: ` ${panel.unit}`, digits: panel.digits ?? 0 })}
</g>`;
  }).join("\n")}
</g>`;
  }).join("\n");
  const svg = `${base(width, height, title, subtitle)}
${body}
<g>
  <rect class="note" x="94" y="${bottom.noteY}" width="${width - 188}" height="${noteH}" rx="16"/>
  ${text("small", width / 2, bottom.noteY + 23, note, 'text-anchor="middle" dominant-baseline="middle"')}
  ${text("tiny", width / 2, bottom.noteY + 42, "Split panels are intentional: one chart would compress slower paths into unreadable slivers.", 'text-anchor="middle" dominant-baseline="middle"')}
</g>
${footer(bottom.footerY, width, source, "bluetape4k-projects - github.com/bluetape4k/bluetape4k-projects")}
</svg>`;
  writeChart(name, svg, `${name}: panels=${panels.length}`);
}

function writeChart(name, svg, summary) {
  const svgPath = join(outDir, `${name}.svg`);
  const pngPath = join(outDir, `${name}.png`);
  const normalized = normalizeSvg(svg);
  const geometry = validateFrameMargins(name, normalized);
  writeFileSync(svgPath, normalized);
  execFileSync(rsvgConvert, [svgPath, "-o", pngPath]);
  writeFileSync(join(outDir, `${name}-summary.txt`), `${summary}\n${geometry}\n`);
  console.log(`${summary}; ${geometry}`);
}

function normalizeSvg(svg) {
  return `${svg.split("\n").map((line) => line.trimEnd()).join("\n")}\n`;
}

function bottomBands(contentBottom, interpretationH, minHeight = 0) {
  const naturalNoteY = contentBottom + bodyNoteGap;
  const naturalFooterY = naturalNoteY + interpretationH + noteFooterGap;
  const naturalHeight = naturalFooterY + footerH + innerBottomMargin + frameMargin;
  const height = Math.max(naturalHeight, minHeight);
  const footerY = height - frameMargin - innerBottomMargin - footerH;
  const noteY = footerY - noteFooterGap - interpretationH;
  if (noteY < naturalNoteY) {
    throw new Error(`bottom band overlap: noteY=${noteY}, required>=${naturalNoteY}`);
  }
  return { noteY, footerY, height };
}

function validateFrameMargins(name, svg) {
  const svgMatch = svg.match(/<svg[^>]+width="(\d+)"[^>]+height="(\d+)"/);
  const frameMatch = svg.match(/<rect class="frame" x="([\d.]+)" y="([\d.]+)" width="([\d.]+)" height="([\d.]+)"/);
  if (!svgMatch || !frameMatch) throw new Error(`${name}: missing SVG/frame geometry`);

  const width = Number(svgMatch[1]);
  const height = Number(svgMatch[2]);
  const frameX = Number(frameMatch[1]);
  const frameY = Number(frameMatch[2]);
  const frameW = Number(frameMatch[3]);
  const frameH = Number(frameMatch[4]);
  const margins = {
    left: frameX,
    right: width - frameX - frameW,
    top: frameY,
    bottom: height - frameY - frameH,
  };
  if (Object.values(margins).some((value) => value !== frameMargin)) {
    throw new Error(`${name}: frame margin imbalance ${JSON.stringify(margins)}`);
  }

  const noteMatches = [...svg.matchAll(/<rect class="note" x="([\d.]+)" y="([\d.]+)" width="([\d.]+)" height="([\d.]+)"/g)];
  const footerRect = noteMatches.at(-1);
  if (!footerRect) throw new Error(`${name}: missing footer note geometry`);
  const footerY = Number(footerRect[2]);
  const footerHeight = Number(footerRect[4]);
  const footerInnerBottom = height - frameMargin - footerY - footerHeight;
  if (footerInnerBottom !== innerBottomMargin) {
    throw new Error(`${name}: footer inner bottom margin ${footerInnerBottom}, expected ${innerBottomMargin}`);
  }

  return `geometry=PASS frameMargins(L/R/T/B)=${margins.left}/${margins.right}/${margins.top}/${margins.bottom} footerInnerBottom=${footerInnerBottom}`;
}

function compactPanel({ x, y, w, h, title, unit, max, ticks, rows }) {
  const plotX = x + 205;
  const plotY = y + 78;
  const plotW = w - 270;
  const rowGap = Math.floor((h - 138) / rows.length);
  const plotH = rowGap * rows.length + 8;
  return `<g>
  <rect class="panel" x="${x}" y="${y}" width="${w}" height="${h}" rx="22"/>
  ${text("panelTitle", x + 28, y + 42, title)}
  ${text("small", x + w - 28, y + 42, `0 to ${fmt(max)} ${unit}`, 'text-anchor="end"')}
  ${axis(plotX, plotY, plotW, plotH, max, ticks)}
  ${rows.map((row, index) => {
    const rowY = plotY + index * rowGap + 12;
    return `<g>
  ${text("body", x + 30, rowY + 9, row.label, 'dominant-baseline="middle"')}
  ${bar(plotX, rowY, plotW, max, row.value, row.color ?? series[index % series.length], "", { unit: ` ${unit}`, height: 13 })}
</g>`;
  }).join("\n")}
</g>`;
}

function idGeneratorsChart() {
  const width = 1840;
  const panelW = 820;
  const panelH = 455;
  const bottom = bottomBands(650 + panelH, noteH);
  const height = bottom.height;
  const singleSmall = [
    { label: "UUID V7", value: 429584, color: colors.green },
    { label: "ULID", value: 270825, color: colors.teal },
    { label: "UUID V4", value: 105645, color: colors.blue },
    { label: "KSUID ms", value: 59896, color: colors.amber },
    { label: "KSUID sec", value: 53884, color: colors.purple },
    { label: "Flake", value: 52840, color: colors.olive },
    { label: "Snowflake", value: 40972, color: colors.red },
  ];
  const singleLarge = [
    { label: "UUID V7", value: 4278, color: colors.green },
    { label: "ULID", value: 2553, color: colors.teal },
    { label: "UUID V4", value: 1037, color: colors.blue },
    { label: "KSUID ms", value: 582, color: colors.amber },
    { label: "KSUID sec", value: 521, color: colors.purple },
    { label: "Flake", value: 478, color: colors.olive },
    { label: "Snowflake", value: 410, color: colors.red },
  ];
  const concurrentSmall = [
    { label: "UUID V7", value: 83431, color: colors.green },
    { label: "Flake", value: 32011, color: colors.olive },
    { label: "UUID V4", value: 30217, color: colors.blue },
    { label: "Snowflake", value: 27016, color: colors.red },
    { label: "KSUID sec", value: 25810, color: colors.purple },
    { label: "KSUID ms", value: 25768, color: colors.amber },
    { label: "ULID", value: 22580, color: colors.teal },
  ];
  const concurrentLarge = [
    { label: "UUID V7", value: 795, color: colors.green },
    { label: "Flake", value: 317, color: colors.olive },
    { label: "UUID V4", value: 290, color: colors.blue },
    { label: "Snowflake", value: 253, color: colors.red },
    { label: "KSUID sec", value: 252, color: colors.purple },
    { label: "KSUID ms", value: 241, color: colors.amber },
    { label: "ULID", value: 223, color: colors.teal },
  ];
  const svg = `${base(
    width,
    height,
    "ID Generators Throughput",
    "Single-thread and concurrent benchmark tables · batch=100 and batch=10000 · ops/s · higher is better",
  )}
${compactPanel({ x: 70, y: 150, w: panelW, h: panelH, title: "Single-thread batch=100", unit: "ops/s", max: 450000, ticks: [0, 150000, 300000, 450000], rows: singleSmall })}
${compactPanel({ x: 950, y: 150, w: panelW, h: panelH, title: "Concurrent batch=100", unit: "ops/s", max: 90000, ticks: [0, 30000, 60000, 90000], rows: concurrentSmall })}
${compactPanel({ x: 70, y: 650, w: panelW, h: panelH, title: "Single-thread batch=10000", unit: "ops/s", max: 4500, ticks: [0, 1500, 3000, 4500], rows: singleLarge })}
${compactPanel({ x: 950, y: 650, w: panelW, h: panelH, title: "Concurrent batch=10000", unit: "ops/s", max: 850, ticks: [0, 250, 500, 750], rows: concurrentLarge })}
<g>
  <rect class="note" x="94" y="${bottom.noteY}" width="${width - 188}" height="${noteH}" rx="16"/>
  ${text("small", width / 2, bottom.noteY + 23, "UUID V7 leads every table; batch=10000 drops all generators because uniqueness verification dominates the measured work.", 'text-anchor="middle" dominant-baseline="middle"')}
  ${text("tiny", width / 2, bottom.noteY + 42, "Four panels are intentional: mixing batch sizes or execution modes into one axis hides the benchmark meaning.", 'text-anchor="middle" dominant-baseline="middle"')}
</g>
${footer(bottom.footerY, width, "utils/idgenerators/Benchmark.md and Benchmark.ko.md summary tables", "bluetape4k-projects - github.com/bluetape4k/bluetape4k-projects")}
</svg>`;
  writeChart("idgenerators-throughput-chart-01", svg, "idgenerators-throughput-chart-01: panels=4 sourceTables=single/concurrent batch100/batch10000");
}

splitPanelsChart({
  name: "cache-lettuce-near-cache-throughput-chart-01",
  title: "Lettuce Near Cache Throughput",
  subtitle: "L1 hit path is separated from Redis/write paths · ops/ms · higher is better",
  source: "cache/cache-lettuce/README.ko.md near-cache throughput table",
  note: "L1 hit throughput is roughly four orders of magnitude above Redis-backed paths, so the chart uses split scales.",
  panels: [
    {
      title: "L1 hit by payload",
      unit: "ops/ms",
      max: 70000,
      ticks: [0, 35000, 70000],
      rows: [
        { label: "512 B", value: 65560, color: colors.green },
        { label: "4 KiB", value: 63458, color: colors.teal },
        { label: "16 KiB", value: 64580, color: colors.blue },
      ],
    },
    {
      title: "Remote/write paths",
      unit: "ops/ms",
      max: 5,
      ticks: [0, 2.5, 5],
      digits: 2,
      rows: [
        { label: "L2 hit", detail: "512 B / 4 KiB / 16 KiB", value: 4.07, color: colors.blue },
        { label: "L2 miss", detail: "best payload value", value: 4.21, color: colors.teal },
        { label: "Remove", detail: "single key", value: 4.24, color: colors.purple },
        { label: "Put", detail: "single key", value: 2.12, color: colors.amber },
        { label: "PutAll x100", detail: "batch write", value: 1.04, color: colors.red },
      ],
    },
  ],
});

groupedBarsChart({
  name: "data-r2dbc-pool-acquire-throughput-chart-01",
  title: "R2DBC Pool Acquire Throughput",
  subtitle: "Default vs high-concurrency profile · ops/s · higher is better",
  source: "data/r2dbc/README.ko.md pool acquire benchmark table",
  unit: "ops/s",
  max: 105000,
  ticks: [0, 25000, 50000, 75000, 100000],
  seriesLabels: ["default", "high"],
  note: "Fast H2 acquisition dominates the scale; slower delayed rows remain visible because both profiles share one unit.",
  groups: [
    { label: "H2 0 ms", values: [100200, 95423] },
    { label: "H2 1 ms", values: [6921, 6906] },
    { label: "H2 5 ms", values: [1430, 1439] },
    { label: "PostgreSQL 18 TC 0 ms", values: [16571, 16960] },
    { label: "PostgreSQL 18 TC 1 ms", values: [4271, 4695] },
    { label: "PostgreSQL 18 TC 5 ms", values: [1050, 1066] },
    { label: "MySQL 8.4 TC 0 ms", values: [9007, 8251] },
    { label: "MySQL 8.4 TC 1 ms", values: [4266, 4279] },
    { label: "MySQL 8.4 TC 5 ms", values: [918, 960] },
  ],
});

groupedBarsChart({
  name: "data-r2dbc-pool-contention-throughput-chart-01",
  title: "R2DBC Pool Contention Throughput",
  subtitle: "Throughput and rejection pressure under 10 ms / 50 ms work · ops/s · higher is not always healthier",
  source: "data/r2dbc/README.ko.md contention benchmark table",
  unit: "ops/s",
  max: 40000,
  ticks: [0, 10000, 20000, 30000, 40000],
  seriesLabels: ["default", "high"],
  note: "High-concurrency settings can report high throughput while producing large failed-acquire counts; interpretation must keep that caveat.",
  groups: [
    { label: "10 ms / maxSize 4", detail: "failed: 0 vs 150,669", values: [360, 38342] },
    { label: "10 ms / maxSize 8", detail: "failed: 0 vs 82,978", values: [733, 21530] },
    { label: "10 ms / maxSize 16", detail: "failed: 0 vs 0", values: [1476, 1477] },
    { label: "50 ms / maxSize 4", detail: "failed: 0 vs 150,891", values: [76, 37763] },
    { label: "50 ms / maxSize 8", detail: "failed: 0 vs 82,893", values: [155, 20810] },
    { label: "50 ms / maxSize 16", detail: "failed: 0 vs 0", values: [313, 310] },
  ],
});

idGeneratorsChart();

rankingChart({
  name: "infra-lettuce-codec-throughput-chart-01",
  title: "Lettuce Codec Throughput",
  subtitle: "Encode/decode round trip throughput · ops/ms · higher is better",
  source: "infra/lettuce/README.ko.md codec benchmark table",
  unit: "ops/ms",
  max: 7000,
  ticks: [0, 2000, 4000, 6000],
  note: "fastjson2 leads raw throughput; compressed codecs trade throughput for payload-size pressure.",
  rows: [
    { label: "fastjson2", value: 6379, color: colors.green },
    { label: "FastFory", value: 3286, color: colors.pink },
    { label: "Fory", value: 2551, color: colors.teal },
    { label: "Kryo", value: 963, color: colors.blue },
    { label: "LZ4 FastFory", value: 906, color: colors.amber },
    { label: "LZ4 Fory", value: 852, color: colors.purple },
    { label: "Jackson3", value: 834, color: colors.olive },
    { label: "LZ4 Kryo", value: 535, color: colors.red },
    { label: "Zstd FastFory", value: 206, color: colors.pink },
    { label: "Zstd Fory", value: 203, color: colors.teal },
    { label: "Zstd Kryo", value: 136, color: colors.blue },
    { label: "JDK", value: 132, color: colors.gray },
    { label: "Gzip FastFory", value: 110, color: colors.amber },
  ],
});

rankingChart({
  name: "infra-lettuce-connection-throughput-chart-01",
  title: "Lettuce Connection Throughput",
  subtitle: "Incremental connection and pipeline tuning · ops/s · higher is better",
  source: "infra/lettuce/README.ko.md connection tuning benchmark table",
  unit: "ops/s",
  max: 85000,
  ticks: [0, 25000, 50000, 75000],
  note: "Integrated pipeline + awaitAll is the only step that materially changes throughput beyond socket/client-resource tuning.",
  rows: [
    { label: "baseline", value: 31847, color: colors.gray },
    { label: "shared resources", value: 32154, color: colors.blue },
    { label: "pipeline SET+GET", value: 40816, color: colors.teal },
    { label: "SocketOptions", value: 46728, color: colors.amber },
    { label: "pipeline + awaitAll", value: 81967, color: colors.green },
  ],
});

rankingChart({
  name: "infra-redisson-codec-throughput-chart-01",
  title: "Redisson Codec Throughput",
  subtitle: "Codec encode/decode throughput · ops/ms · higher is better",
  source: "infra/redisson/README.ko.md codec benchmark table",
  unit: "ops/ms",
  max: 3300,
  ticks: [0, 1000, 2000, 3000],
  note: "FastFory/Fory dominate Redisson codec throughput; compression variants preserve size tradeoffs at lower speed.",
  rows: [
    { label: "FastFory", value: 3084, color: colors.pink },
    { label: "Fory", value: 2504, color: colors.teal },
    { label: "fastjson2", value: 1928, color: colors.green },
    { label: "Kryo5", value: 1225, color: colors.blue },
    { label: "LZ4 FastFory", value: 829, color: colors.amber },
    { label: "LZ4 Fory", value: 774, color: colors.purple },
    { label: "LZ4 Kryo5", value: 518, color: colors.red },
    { label: "Jackson3", value: 474, color: colors.olive },
    { label: "Zstd Fory", value: 196, color: colors.teal },
    { label: "Zstd FastFory", value: 193, color: colors.pink },
    { label: "Zstd Kryo5", value: 139, color: colors.blue },
    { label: "JDK", value: 128, color: colors.gray },
    { label: "Gzip FastFory", value: 108, color: colors.amber },
  ],
});

rankingChart({
  name: "infra-redisson-batch-throughput-chart-01",
  title: "Redisson Batch Throughput",
  subtitle: "Optimization ladder · ops/s · higher is better",
  source: "infra/redisson/README.ko.md batch benchmark table",
  unit: "ops/s",
  max: 100000,
  ticks: [0, 25000, 50000, 75000, 100000],
  note: "Batch size, StringCodec, and pooled keys are the main throughput levers.",
  rows: [
    { label: "baseline", value: 11737, color: colors.gray },
    { label: "warmup", value: 16025, color: colors.blue },
    { label: "RBatch pipelining", value: 28571, color: colors.teal },
    { label: "megabatch", value: 78125, color: colors.amber },
    { label: "StringCodec + KEY_POOL", value: 92592, color: colors.green },
  ],
});

splitPanelsChart({
  name: "io-serializer-throughput-chart-01",
  title: "Binary Serializer Throughput",
  subtitle: "Serializer throughput split by payload shape · ops/s · higher is better",
  source: "io/io/src/test/kotlin/io/bluetape4k/io/benchmark/README.md serializer benchmark tables",
  note: "Fory leads both payload shapes, but ByteArray payloads lower every serializer score enough to warrant a separate panel.",
  panels: [
    {
      title: "No ByteArray payload",
      unit: "ops/s",
      max: 320000,
      ticks: [0, 100000, 200000, 300000],
      rows: [
        { label: "Fory", value: 305821, color: colors.green },
        { label: "Kryo", value: 81823, color: colors.teal },
        { label: "Jackson", value: 39510, color: colors.blue },
        { label: "JDK", value: 22249, color: colors.gray },
      ],
    },
    {
      title: "ByteArray 4096",
      unit: "ops/s",
      max: 65000,
      ticks: [0, 20000, 40000, 60000],
      rows: [
        { label: "Fory", value: 59192, color: colors.green },
        { label: "Kryo", value: 29329, color: colors.teal },
        { label: "JDK", value: 8431, color: colors.gray },
        { label: "Jackson", value: 4323, color: colors.blue },
      ],
    },
  ],
});

rankingChart({
  name: "io-compressor-throughput-chart-01",
  title: "Compressor Throughput",
  subtitle: "Same-condition compressor benchmark · ops/s · higher is better",
  source: "io/io/README.ko.md compressor benchmark summary",
  unit: "ops/s",
  max: 8500,
  ticks: [0, 2000, 4000, 6000, 8000],
  note: "Snappy and LZ4 are the throughput-oriented choices; Zstd/GZip/Deflate are shown as slower compression tradeoffs.",
  rows: [
    { label: "Snappy", value: 8073, color: colors.green },
    { label: "LZ4", value: 6769, color: colors.teal },
    { label: "Zstd", value: 5103, color: colors.blue },
    { label: "GZip", value: 1195, color: colors.amber },
    { label: "Deflate", value: 1084, color: colors.red },
  ],
});

rankingChart({
  name: "io-fast-serializer-throughput-chart-01",
  title: "Fast Serializer Throughput",
  subtitle: "Fast path serializers vs factory defaults · ops/s · higher is better",
  source: "io/io/README.ko.md fast serializer benchmark summary",
  unit: "ops/s",
  max: 125000,
  ticks: [0, 30000, 60000, 90000, 120000],
  note: "Dedicated fast serializer instances expose the fast path more clearly than factory defaults.",
  rows: [
    { label: "ForyBinarySerializer.fast", value: 116000, color: colors.green },
    { label: "KryoBinarySerializer.fast", value: 68000, color: colors.teal },
    { label: "BinarySerializers.Fory", value: 68000, color: colors.blue },
    { label: "BinarySerializers.Kryo", value: 34000, color: colors.purple },
    { label: "JDK", value: 8431, color: colors.gray },
    { label: "Jackson", value: 4323, color: colors.amber },
  ],
});

splitPanelsChart({
  name: "io-http-cache-throughput-chart-01",
  title: "HTTP Client Cache Throughput",
  subtitle: "Cached paths and no-cache baselines use separate scales · ops/s · higher is better",
  source: "io/http/README.ko.md HTTP client cache benchmark table",
  note: "In-memory cache throughput is orders of magnitude above uncached HTTP clients, so no-cache baselines need their own panel.",
  panels: [
    {
      title: "Cached paths",
      unit: "ops/s",
      max: 850000,
      ticks: [0, 250000, 500000, 750000],
      rows: [
        { label: "HC5 + InMemoryCache", detail: "x1233", value: 813906, color: colors.green },
        { label: "OkHttp + DiskLruCache", detail: "x53", value: 35359, color: colors.teal },
      ],
    },
    {
      title: "No-cache baselines",
      unit: "ops/s",
      max: 720,
      ticks: [0, 240, 480, 720],
      rows: [
        { label: "HC5 Classic", detail: "x1", value: 682, color: colors.gray },
        { label: "HC5 VirtualThread", value: 668, color: colors.blue },
        { label: "OkHttp3", value: 661, color: colors.amber },
      ],
    },
  ],
});
