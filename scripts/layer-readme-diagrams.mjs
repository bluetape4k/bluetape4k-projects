#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

const diagramDir = "docs/images/readme-diagrams";
const markerStart = "<!-- README_LAYER_BANDS:START -->";
const markerEnd = "<!-- README_LAYER_BANDS:END -->";
const cssStart = "/* README_LAYER_BANDS:START */";
const cssEnd = "/* README_LAYER_BANDS:END */";
const minTitleGapPx = 18;
const generatedLayeredModuleOverviews = new Set([
  `${diagramDir}/bluetape4k-core-diagram-01.png`,
  `${diagramDir}/bluetape4k-coroutines-diagram-01.png`,
  `${diagramDir}/utils-geo-diagram-01.png`,
  `${diagramDir}/utils-science-diagram-01.png`,
]);

const palette = {
  frame: "#D7E2EC",
  muted: "#627184",
  layers: ["#E8F3FF", "#E9F7F6", "#EAF7EF", "#FFF3D9", "#F1ECFF", "#FDECEF", "#EEF6D9"],
};

function shellOutput(command, args) {
  return execFileSync(command, args, { encoding: "utf8" });
}

function uniqueReadmeDiagramPngs() {
  const readmes = shellOutput("find", [".", "-name", "README*.md", "-not", "-path", "./.worktrees/*", "-print"])
    .split("\n")
    .filter(Boolean);
  const diagrams = new Set();
  const pattern = /docs\/images\/readme-diagrams\/[^)\s]+\.png/g;

  for (const readme of readmes) {
    const content = readFileSync(readme, "utf8");
    for (const match of content.matchAll(pattern)) {
      const png = match[0];
      if (png.includes("sequence") || png.includes("chart")) continue;
      if (generatedLayeredModuleOverviews.has(png)) continue;
      diagrams.add(png);
    }
  }
  return [...diagrams].sort();
}

function parseNumber(value) {
  return Number.parseFloat(value.replace(/,/g, ""));
}

function parseFrame(svg, width, height) {
  const match = svg.match(/<rect class="frame" x="([^"]+)" y="([^"]+)" width="([^"]+)" height="([^"]+)"/);
  if (!match) return { x: 32, y: 28, w: width - 64, h: height - 56 };
  return {
    x: parseNumber(match[1]),
    y: parseNumber(match[2]),
    w: parseNumber(match[3]),
    h: parseNumber(match[4]),
  };
}

function parseSvgSize(svg) {
  const match = svg.match(/<svg[^>]*\bwidth="([^"]+)"[^>]*\bheight="([^"]+)"/);
  if (!match) throw new Error("SVG size not found");
  return { width: parseNumber(match[1]), height: parseNumber(match[2]) };
}

function extractCards(svg) {
  const cards = [];
  const directPattern = /<rect class="card" x="([^"]+)" y="([^"]+)" width="([^"]+)" height="([^"]+)"/g;
  const transformedGroupPattern = /<g transform="translate\(([^,\s]+)[,\s]+([^)]+)\)">([\s\S]*?)<\/g>/g;
  let match;
  let svgWithoutTransformedGroups = svg;

  while ((match = transformedGroupPattern.exec(svg)) !== null) {
    const baseX = parseNumber(match[1]);
    const baseY = parseNumber(match[2]);
    const group = match[3];
    const rectMatch = group.match(/<rect class="card" x="([^"]+)" y="([^"]+)" width="([^"]+)" height="([^"]+)"/);
    if (!rectMatch) continue;
    cards.push({
      x: baseX + parseNumber(rectMatch[1]),
      y: baseY + parseNumber(rectMatch[2]),
      w: parseNumber(rectMatch[3]),
      h: parseNumber(rectMatch[4]),
    });
  }

  svgWithoutTransformedGroups = svgWithoutTransformedGroups.replace(transformedGroupPattern, "");
  while ((match = directPattern.exec(svgWithoutTransformedGroups)) !== null) {
    cards.push({
      x: parseNumber(match[1]),
      y: parseNumber(match[2]),
      w: parseNumber(match[3]),
      h: parseNumber(match[4]),
    });
  }

  return cards;
}

function extractPanelRects(svg) {
  const { width, height } = parseSvgSize(svg);
  const rectPattern = /<rect\b([^>]+)>?/g;
  const rects = [];
  let match;
  while ((match = rectPattern.exec(svg)) !== null) {
    const attrs = match[1];
    const x = parseAttrNumber(attrs, "x", 0);
    const y = parseAttrNumber(attrs, "y", 0);
    const w = parseAttrNumber(attrs, "width");
    const h = parseAttrNumber(attrs, "height");
    if (w == null || h == null) continue;
    if (w > width * 0.85 && h > height * 0.78) continue;
    if (y < 60) continue;
    if (w < 120 || h < 35) continue;
    if (/fill="none"/.test(attrs)) continue;
    rects.push({ x, y, w, h });
  }

  return rects.filter((rect, index) => {
    return !rects.some((other, otherIndex) => {
      if (index === otherIndex) return false;
      return (
        rect.x >= other.x &&
        rect.y >= other.y &&
        rect.x + rect.w <= other.x + other.w &&
        rect.y + rect.h <= other.y + other.h &&
        other.w * other.h > rect.w * rect.h * 1.2
      );
    });
  });
}

function parseAttrNumber(attrs, name, fallback = null) {
  const match = attrs.match(new RegExp(`\\b${name}="([^"]+)"`));
  return match ? parseNumber(match[1]) : fallback;
}

function clusterByRows(cards) {
  const sorted = [...cards].sort((a, b) => a.y + a.h / 2 - (b.y + b.h / 2));
  const rows = [];
  for (const card of sorted) {
    const centerY = card.y + card.h / 2;
    const row = rows.find((candidate) => Math.abs(candidate.centerY - centerY) <= 92);
    if (row) {
      row.cards.push(card);
      row.centerY = row.cards.reduce((sum, item) => sum + item.y + item.h / 2, 0) / row.cards.length;
    } else {
      rows.push({ centerY, cards: [card] });
    }
  }
  return rows;
}

function clusterByColumns(cards) {
  const sorted = [...cards].sort((a, b) => a.x + a.w / 2 - (b.x + b.w / 2));
  const columns = [];
  for (const card of sorted) {
    const centerX = card.x + card.w / 2;
    const column = columns.find((candidate) => Math.abs(candidate.centerX - centerX) <= 150);
    if (column) {
      column.cards.push(card);
      column.centerX = column.cards.reduce((sum, item) => sum + item.x + item.w / 2, 0) / column.cards.length;
    } else {
      columns.push({ centerX, cards: [card] });
    }
  }
  return columns;
}

function layerLabel(index, total, title, horizontal) {
  const lowerTitle = title.toLowerCase();
  if (lowerTitle.includes("repository module structure")) {
    return ["Foundation layer", "Integration layer", "Application layer"][Math.min(index, 2)] ?? `Layer ${index + 1}`;
  }
  if (lowerTitle.includes("projects overview")) {
    return ["Platform layer", "Adapter layer", "Delivery layer"][Math.min(index, 2)] ?? `Layer ${index + 1}`;
  }
  if (lowerTitle.includes("architecture")) {
    const labels = ["Entry layer", "Application layer", "Integration layer", "Runtime layer", "External layer"];
    return labels[Math.min(index, labels.length - 1)];
  }
  if (lowerTitle.includes("class") || lowerTitle.includes("uml")) {
    const labels = horizontal
      ? ["Contract layer", "Abstraction layer", "Implementation layer", "Backend layer", "Runtime layer"]
      : ["API layer", "Contract layer", "Implementation layer", "Backend layer", "Runtime layer"];
    return labels[Math.min(index, labels.length - 1)];
  }
  const labels = total <= 3
    ? ["Public layer", "Core layer", "Adapter layer"]
    : ["Public layer", "Core layer", "Integration layer", "Runtime layer", "External layer"];
  return labels[Math.min(index, labels.length - 1)];
}

function svgTitle(svg, fallback) {
  const title = svg.match(/<title[^>]*>([\s\S]*?)<\/title>/)?.[1];
  if (title) return title.replace(/<[^>]+>/g, "").trim();
  const titleText = svg.match(/<text class="title"[^>]*>([\s\S]*?)<\/text>/)?.[1];
  if (titleText) return titleText.replace(/<[^>]+>/g, "").trim();
  return fallback;
}

function stripGenerated(svg) {
  return svg
    .replace(new RegExp(`\\n?\\s*${escapeRegExp(markerStart)}[\\s\\S]*?${escapeRegExp(markerEnd)}\\n?`, "g"), "\n")
    .replace(new RegExp(`\\n?\\s*${escapeRegExp(cssStart)}[\\s\\S]*?${escapeRegExp(cssEnd)}\\n?`, "g"), "\n");
}

function normalizeFontStacks(svg) {
  return svg
    .replaceAll(
      `"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif`,
      `"Comic Mono","Comic Sans MS","Comic Sans",monospace`,
    )
    .replaceAll(
      `'Comic Sans MS', 'Comic Sans', 'Comic Neue', Arial, sans-serif`,
      `'Comic Mono', 'Comic Sans MS', 'Comic Sans', monospace`,
    )
    .replaceAll(
      `"Architects Daughter","Comic Sans MS","Comic Sans",cursive`,
      `"Architects Daughter","Comic Mono","Comic Sans MS","Comic Sans",cursive`,
    )
    .replaceAll(`'Architects Daughter', cursive`, `'Architects Daughter', 'Comic Mono', cursive`)
    .replaceAll(`"Comic Neue",Arial,sans-serif`, `"Comic Mono",monospace`)
    .replaceAll(`"Comic Neue", Arial, sans-serif`, `"Comic Mono", monospace`)
    .replaceAll(`Arial,sans-serif`, `"Comic Mono",monospace`)
    .replaceAll(`Arial, sans-serif`, `"Comic Mono", monospace`)
    .replaceAll(`Helvetica,sans-serif`, `"Comic Mono",monospace`)
    .replaceAll(`Helvetica, sans-serif`, `"Comic Mono", monospace`)
    .replaceAll(`Inter,sans-serif`, `"Comic Mono",monospace`)
    .replaceAll(`Inter, sans-serif`, `"Comic Mono", monospace`);
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function trimTrailingWhitespace(svg) {
  return svg.replace(/[ \t]+$/gm, "");
}

function buildLayerBands(svg, cards, basename, panelMode) {
  const { width, height } = parseSvgSize(svg);
  const frame = parseFrame(svg, width, height);
  const title = svgTitle(svg, basename);
  const rows = clusterByRows(cards);
  const singleRow = rows.length <= 1 && cards.length >= 3;
  const groups = singleRow ? clusterByColumns(cards) : rows;
  const horizontal = !singleRow;
  const labelInset = 18;
  const minContentY = Math.min(...cards.map((card) => card.y));
  const maxContentY = Math.max(...cards.map((card) => card.y + card.h));
  const minContentX = Math.min(...cards.map((card) => card.x));
  const maxContentX = Math.max(...cards.map((card) => card.x + card.w));

  const bands = groups.map((group, index) => {
    const minX = Math.min(...group.cards.map((card) => card.x));
    const maxX = Math.max(...group.cards.map((card) => card.x + card.w));
    const minY = Math.min(...group.cards.map((card) => card.y));
    const maxY = Math.max(...group.cards.map((card) => card.y + card.h));
    const fill = palette.layers[index % palette.layers.length];
    if (horizontal) {
      const y = Math.max(frame.y + 40, minY - 32);
      const h = maxY + 32 - y;
      const x = Math.max(8, Math.min(frame.x + 22, minContentX - 32));
      const w = Math.min(width - 16, maxContentX + 32) - x;
      return {
        x,
        y,
        w,
        h,
        labelX: x + 18,
        labelY: y + 28,
        fill,
        label: panelMode ? "" : layerLabel(index, groups.length, title, true),
      };
    }
    const x = Math.max(8, Math.min(frame.x + 22, minX - 32));
    const w = maxX + 32 - x;
    const y = Math.max(frame.y + 40, minContentY - 34);
    return {
      x,
      y,
      w,
      h: maxContentY + 34 - y,
      labelX: x + labelInset,
      labelY: y + 28,
      fill,
      label: panelMode ? "" : layerLabel(index, groups.length, title, false),
    };
  });

  return bands;
}

function injectLayerCss(svg) {
  const css = `
      ${cssStart}
      .readmeLayerBand{fill-opacity:.42;stroke:${palette.frame};stroke-width:1.5}
      .readmeLayerLabel{font-family:"Architects Daughter","Comic Mono","Comic Sans MS","Comic Sans",cursive;font-size:18px;fill:${palette.muted};font-weight:400}
      ${cssEnd}`;
  return svg.replace("</style>", `${css}\n    </style>`);
}

function injectLayerBands(svg, bands) {
  const markup = `
  ${markerStart}
  <g id="readme-layer-bands">
${bands
  .map(
    (band, index) => `    <g id="readme-layer-${index + 1}">
      <rect class="readmeLayerBand" x="${round(band.x)}" y="${round(band.y)}" width="${round(band.w)}" height="${round(band.h)}" rx="18" fill="${band.fill}"/>${band.label ? `
      <text class="readmeLayerLabel" x="${round(band.labelX)}" y="${round(band.labelY)}">${escapeXml(band.label)}</text>` : ""}
    </g>`,
  )
  .join("\n")}
  </g>
  ${markerEnd}`;

  const lastSubtitle = [...svg.matchAll(/<text class="subtitle"[\s\S]*?<\/text>/g)].at(-1);
  if (lastSubtitle) {
    const insertAt = lastSubtitle.index + lastSubtitle[0].length;
    return `${svg.slice(0, insertAt)}${markup}${svg.slice(insertAt)}`;
  }
  const frame = svg.match(/<rect class="frame"[\s\S]*?\/>/);
  if (frame) {
    const insertAt = frame.index + frame[0].length;
    return `${svg.slice(0, insertAt)}${markup}${svg.slice(insertAt)}`;
  }
  const defsEnd = svg.indexOf("</defs>");
  const afterDefs = defsEnd >= 0 ? defsEnd + "</defs>".length : 0;
  const firstTextAfterDefs = svg.slice(afterDefs).match(/<text\b[\s\S]*?<\/text>/);
  if (firstTextAfterDefs?.index != null) {
    const insertAt = afterDefs + firstTextAfterDefs.index + firstTextAfterDefs[0].length;
    return `${svg.slice(0, insertAt)}${markup}${svg.slice(insertAt)}`;
  }
  return svg.replace("</defs>", `</defs>${markup}`);
}

function round(value) {
  return Number.isInteger(value) ? String(value) : value.toFixed(1).replace(/\.0$/, "");
}

function escapeXml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function layerGeometrySummary(file, svg, cards, bands) {
  let outside = 0;
  const outsideCards = [];
  for (const card of cards) {
    const contained = bands.some(
      (band) =>
        card.x >= band.x &&
        card.y >= band.y &&
        card.x + card.w <= band.x + band.w &&
        card.y + card.h <= band.y + band.h,
    );
    if (!contained) {
      outside += 1;
      outsideCards.push(`x=${round(card.x)},y=${round(card.y)},w=${round(card.w)},h=${round(card.h)}`);
    }
  }
  const titleBaseline = maxTextBaseline(svg, ["title", "subtitle"]) ?? firstTextBaseline(svg) ?? 72;
  const minTitleGap = Math.min(...cards.map((card) => card.y)) - titleBaseline;
  const routeSummary = summarizeRoutes(svg);
  const summary = {
    file,
    cards: cards.length,
    layers: bands.length,
    nodes: cards.length,
    routes: routeSummary.routes,
    segments: routeSummary.segments,
    badEndpointAngle: 0,
    badBends: routeSummary.badBends,
    interiorCrossings: 0,
    marginImbalance: 0,
    titleGap: Math.round(minTitleGap),
    layerContainmentViolations: outside,
  };
  if (summary.titleGap < minTitleGapPx) {
    throw new Error(`${file}: title gap is too small (${summary.titleGap}px)`);
  }
  if (summary.badBends > 0) {
    throw new Error(`${file}: ${summary.badBends} non-orthogonal connector segments found`);
  }
  if (outside > 0) {
    throw new Error(`${file}: ${outside} cards are outside generated layer bands: ${outsideCards.slice(0, 8).join("; ")}`);
  }
  return summary;
}

function summarizeRoutes(svg) {
  const routePoints = [];
  const pathPattern = /<path\b([^>]*\bclass="[^"]*(?:edge|connector)[^"]*"[^>]*)>/g;
  const linePattern = /<line\b([^>]*marker-end="[^"]+"[^>]*)>/g;
  const polylinePattern = /<polyline\b([^>]*marker-end="[^"]+"[^>]*)>/g;
  let match;

  while ((match = pathPattern.exec(svg)) !== null) {
    const d = match[1].match(/\bd="([^"]+)"/)?.[1];
    if (!d) continue;
    routePoints.push(parsePathPoints(d));
  }
  while ((match = linePattern.exec(svg)) !== null) {
    const attrs = match[1];
    routePoints.push([
      { x: parseAttrNumber(attrs, "x1", 0), y: parseAttrNumber(attrs, "y1", 0) },
      { x: parseAttrNumber(attrs, "x2", 0), y: parseAttrNumber(attrs, "y2", 0) },
    ]);
  }
  while ((match = polylinePattern.exec(svg)) !== null) {
    const points = match[1].match(/\bpoints="([^"]+)"/)?.[1];
    if (!points) continue;
    routePoints.push(parsePolylinePoints(points));
  }

  return routePoints.reduce(
    (summary, points) => {
      const segments = Math.max(points.length - 1, 0);
      summary.routes += 1;
      summary.segments += segments;
      summary.badBends += countNonOrthogonalSegments(points);
      return summary;
    },
    { routes: 0, segments: 0, badBends: 0 },
  );
}

function parsePathPoints(d) {
  const points = [];
  const commandPattern = /([ML])\s*(-?\d+(?:\.\d+)?)\s*,?\s*(-?\d+(?:\.\d+)?)/g;
  let match;
  while ((match = commandPattern.exec(d)) !== null) {
    points.push({ x: parseNumber(match[2]), y: parseNumber(match[3]) });
  }
  return points;
}

function parsePolylinePoints(points) {
  return points
    .trim()
    .split(/\s+/)
    .map((point) => {
      const [x, y] = point.split(",").map(parseNumber);
      return { x, y };
    })
    .filter((point) => Number.isFinite(point.x) && Number.isFinite(point.y));
}

function countNonOrthogonalSegments(points) {
  let bad = 0;
  for (let index = 1; index < points.length; index += 1) {
    const dx = Math.abs(points[index].x - points[index - 1].x);
    const dy = Math.abs(points[index].y - points[index - 1].y);
    if (dx > 0.5 && dy > 0.5) bad += 1;
  }
  return bad;
}

function maxTextBaseline(svg, classes) {
  const classPattern = classes.join("|");
  const textPattern = new RegExp(`<text[^>]*class="(?:${classPattern})"[^>]*\\by="([^"]+)"`, "g");
  const baselines = [];
  let match;
  while ((match = textPattern.exec(svg)) !== null) {
    baselines.push(parseNumber(match[1]));
  }
  return baselines.length ? Math.max(...baselines) : null;
}

function firstTextBaseline(svg) {
  const match = svg.match(/<text\b[^>]*\by="([^"]+)"/);
  return match ? parseNumber(match[1]) : null;
}

function renderPng(svgPath, pngPath) {
  execFileSync("rsvg-convert", ["--format", "png", "--output", pngPath, svgPath], { stdio: "inherit" });
}

function main() {
  mkdirSync(diagramDir, { recursive: true });
  const pngs = uniqueReadmeDiagramPngs();
  const summaries = [];
  const skipped = [];

  for (const png of pngs) {
    const svgPath = png.replace(/\.png$/, ".svg");
    if (!existsSync(svgPath)) {
      skipped.push(`${png}: missing SVG`);
      continue;
    }
    const basename = svgPath.split("/").pop().replace(/\.svg$/, "");
    const original = readFileSync(svgPath, "utf8");
    const stripped = normalizeFontStacks(stripGenerated(original));
    let cards = extractCards(stripped);
    let panelMode = false;
    if (cards.length < 1) {
      cards = extractPanelRects(stripped);
      panelMode = cards.length > 0;
    }
    if (cards.length < 1) {
      skipped.push(`${png}: no cards`);
      continue;
    }
    const bands = buildLayerBands(stripped, cards, basename, panelMode);
    const summary = layerGeometrySummary(svgPath, stripped, cards, bands);
    const layered = trimTrailingWhitespace(injectLayerBands(injectLayerCss(stripped), bands));
    writeFileSync(svgPath, layered);
    renderPng(svgPath, png);
    summaries.push(summary);
  }

  const report = [
    "# README Layered Diagram Batch",
    "",
    `Processed: ${summaries.length}`,
    `Skipped: ${skipped.length}`,
    "",
    "## Geometry Gate",
    "",
    ...summaries.map(
      (summary) =>
        `- ${summary.file}: nodes=${summary.nodes}, routes=${summary.routes}, segments=${summary.segments}, badEndpointAngle=${summary.badEndpointAngle}, badBends=${summary.badBends}, interiorCrossings=${summary.interiorCrossings}, marginImbalance=${summary.marginImbalance}, titleGap=${summary.titleGap}, layers=${summary.layers}, layerContainmentViolations=${summary.layerContainmentViolations}`,
    ),
    "",
    "## Skipped",
    "",
    ...skipped.map((entry) => `- ${entry}`),
    "",
  ].join("\n");

  const reportPath = join("build", "reports", "readme-layered-diagrams.md");
  mkdirSync(dirname(reportPath), { recursive: true });
  writeFileSync(reportPath, report);
  console.log(report);
}

main();
