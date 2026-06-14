#!/usr/bin/env node

import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const DIAGRAM_DIR = join(ROOT, "docs/images/readme-diagrams");
const REPORT = process.env.DIAGRAM_VALIDATION_REPORT || "/tmp/bluetape4k-projects-diagram-validation-report.json";
const MIN_CLEARANCE = 8;

const files = readdirSync(DIAGRAM_DIR)
  .filter((file) => file.endsWith(".svg") && !file.endsWith("-sketch.svg") && !file.endsWith("-graphviz.svg"))
  .sort();

const failures = [];
const rows = [];

for (const file of files) {
  const svg = readFileSync(join(DIAGRAM_DIR, file), "utf8");
  const title = extractTitle(svg);
  const kind = classify(file, title);
  const cards = extractCards(svg);
  const hasPanels = hasLayerOrPanelBands(svg);
  const routes = extractRoutes(svg);
  const paths = routes.map((route) => route.points);
  const layers = extractLayerBoxes(svg);
  const labels = extractLabelBoxes(svg);
  const fileFailures = [];
  const overflow = measureViewBoxOverflow(svg);

  if (/generated from README Mermaid|UML compartments with vertical inheritance and dependency direction/i.test(svg)) {
    fileFailures.push("legacy empty Mermaid shell residue");
  }

  if (/\bOv\./i.test(title)) {
    fileFailures.push("abbreviated title label");
  }

  if (overflow.overflow) {
    fileFailures.push(`content outside viewBox maxX=${overflow.maxX} maxY=${overflow.maxY} viewBox=${overflow.width}x${overflow.height}`);
  }

  const bannedFooter = validateNoInternalFooterEvidence(svg);
  fileFailures.push(...bannedFooter);

  if (kind !== "sequence" && kind !== "chart" && kind !== "class") {
    if (looksLikeRelationshipGrid(cards, paths, layers) && !hasExplicitGridException(svg)) {
      fileFailures.push(hasPanels ? "relationship-heavy grid layout hidden by panels" : "relationship-heavy grid layout");
    }
  }

  if (kind === "sequence") {
    const sequenceFailures = validateSequenceStyle(svg);
    fileFailures.push(...sequenceFailures);
  }

  const cardOverlaps = countCardOverlaps(cards);
  if (cardOverlaps > 0) fileFailures.push(`card overlaps=${cardOverlaps}`);

  const tangentRoutes = countTangentOrGluedRoutes(paths, cards);
  if (tangentRoutes > 0) fileFailures.push(`0-degree/glued connector attachments=${tangentRoutes}`);

  const intrusions = countConnectorIntrusions(paths, cards);
  if (intrusions > 0) fileFailures.push(`connector/card clearance violations=${intrusions}`);

  const disconnectedEndpoints = countDisconnectedRouteEndpoints(routes, cards);
  if (disconnectedEndpoints > 0) fileFailures.push(`disconnected/floating connector endpoints=${disconnectedEndpoints}`);

  const routeConflicts = countRouteConflicts(routes, kind);
  if (routeConflicts > 0) {
    fileFailures.push(`${kind === "class" ? "undifferentiated" : "avoidable"} route-route conflicts=${routeConflicts}`);
  }

  const labelConflicts = countLabelConflicts(labels, paths, cards, layers);
  if (labelConflicts > 0) fileFailures.push(`label/route/card/layer conflicts=${labelConflicts}`);

  const layerGutterConflicts = countLayerGutterConflicts(layers, cards, paths, labels);
  if (layerGutterConflicts > 0) fileFailures.push(`layer label gutter conflicts=${layerGutterConflicts}`);

  const footerFailures = validateFooterInsideFrame(svg, labels);
  fileFailures.push(...footerFailures);

  if (kind === "chart") {
    fileFailures.push(...validateChartFrameMargins(svg));
  }

  if (["architecture", "module", "flow-state"].includes(kind)) {
    const avoidableDoglegs = countAvoidableDoglegs(paths, cards, layers);
    if (avoidableDoglegs > 0) fileFailures.push(`avoidable doglegs=${avoidableDoglegs}`);
  }

  const row = { file, title, kind, cards: cards.length, paths: paths.length, hasPanels, failures: fileFailures };
  rows.push(row);
  if (fileFailures.length > 0) failures.push(row);
}

writeFileSync(REPORT, `${JSON.stringify({ total: rows.length, failed: failures.length, rows }, null, 2)}\n`);

for (const row of failures) {
  console.log(`${row.file}\t${row.kind}\t${row.failures.join("; ")}\t${row.title}`);
}
console.error(`readme diagram validation: total=${rows.length} failed=${failures.length} report=${REPORT}`);

if (failures.length > 0) {
  process.exit(1);
}

function extractTitle(svg) {
  const aria = svg.match(/aria-label="([^"]+)"/)?.[1];
  const textTitle = [...svg.matchAll(/<text[^>]*class="[^"]*(?:title|diagram-title)[^"]*"[^>]*>([\s\S]*?)<\/text>/g)]
    .map((match) => cleanText(match[1]))
    .find(Boolean);
  return textTitle || aria || "";
}

function classify(file, title) {
  const text = `${file} ${title}`;
  if (/chart|benchmark|latency|throughput/i.test(text)) return "chart";
  if (/Class Structure|Structure Classes|Class Hierarchy|Class Diagram|Hierarchy|Classes\b|UML|Domain Model|Domain Classes|Interface and Implementations|API Structure|Type Diagram/i.test(text)) return "class";
  if (/sequence/i.test(text)) return "sequence";
  if (/Module Diagram|Module API Structure|Module Structure|Dependency Structure|Repository Module Structure|Dependency Diagram|Module Co\./i.test(text)) return "module";
  if (/Flow|Pipeline|Lifecycle|State|Retry|Processing|FSM|Algorithm|Selection|Transitions|Execution Model|Sequential|Parallel|Conditional|Repeat/i.test(text)) return "flow-state";
  if (/Architecture|Overview|Layer|Stack|Topology|Integration|Observability|Component/i.test(text)) return "architecture";
  return "unclassified";
}

function validateNoInternalFooterEvidence(svg) {
  const failures = [];
  const banned = [
    ["Source truth", /\bSource\s+truth\b/i],
    ["Source thruth", /\bSource\s+thruth\b/i],
    ["Graphviz evidence", /\bGraphviz\s+evidence\b/i],
    ["Geometry evidence", /\bGeometry\s*:/i],
    ["validation evidence", /\b(?:validation|gate)\s*[:=]\s*(?:PASS|FAIL|SKIP)\b/i],
  ];
  for (const [label, pattern] of banned) {
    if (pattern.test(svg)) failures.push(`internal footer/evidence text in final SVG: ${label}`);
  }
  return failures;
}

function extractCards(svg) {
  const cards = [];
  const groupPattern = /<g(?:\s+[^>]*)?>([\s\S]*?)<\/g>/g;
  let groupMatch;
  while ((groupMatch = groupPattern.exec(svg))) {
    const groupOpen = groupMatch[0].match(/^<g(?:\s+[^>]*)?>/)?.[0] || "";
    const transform = groupOpen.match(/transform="translate\(([-\d.]+)[ ,]([-\d.]+)\)"/);
    if (!transform && !/\bid="[^"]+"/.test(groupOpen)) continue;
    if (/\bid="(?:layer|panel)-[^"]*"/.test(groupOpen)) continue;
    const body = groupMatch[1];
    if (!/class="[^"]*(?:card|classCard|soft|chip)[^"]*"/i.test(body)) continue;
    const dx = transform ? Number(transform[1]) : 0;
    const dy = transform ? Number(transform[2]) : 0;
    const rectTag = [...body.matchAll(/<rect[^>]*>/g)]
      .map((match) => match[0])
      .find((tag) => /class="[^"]*(?:card|classCard|soft|chip)[^"]*"/i.test(tag));
    if (!rectTag) continue;
    const rect = {
      x: attrNumber(rectTag, "x"),
      y: attrNumber(rectTag, "y"),
      w: attrNumber(rectTag, "width"),
      h: attrNumber(rectTag, "height"),
    };
    if ([rect.x, rect.y, rect.w, rect.h].some((value) => Number.isNaN(value))) continue;
    const groupId = groupOpen.match(/\bid="([^"]+)"/)?.[1] || "";
    const labels = [...body.matchAll(/<text[^>]*>([\s\S]*?)<\/text>/g)].map((match) => cleanText(match[1])).filter(Boolean);
    if ((rect.w > 500 && rect.h > 160) || rect.h > 300) continue;
    cards.push({
      id: groupId || labels[0] || `card-${cards.length + 1}`,
      label: labels[0] || groupId || `card-${cards.length + 1}`,
      x: dx + rect.x,
      y: dy + rect.y,
      w: rect.w,
      h: rect.h,
    });
  }

  return cards.filter((card) => card.w >= 40 && card.h >= 24);
}

function attrNumber(tag, name) {
  return Number(tag.match(new RegExp(`\\b${name}="([-\\d.]+)"`))?.[1]);
}

function extractRoutes(svg) {
  return [...svg.matchAll(/<path([^>]*)\bd="([^"]+)"([^>]*)>/g)]
    .map((match) => {
      const attrs = `${match[1]} ${match[3]}`;
      const className = attrs.match(/\bclass="([^"]+)"/)?.[1] || "";
      if (!/\b(?:flow|inherit|dependency|line|Line|connector|dashed|implLine|inheritLine)\b/.test(className)) return null;
      const points = parsePath(match[2]);
      if (points.length < 2) return null;
      return {
        points,
        className,
        stroke: routeStroke(attrs),
        dash: attrs.match(/\bstroke-dasharray="([^"]+)"/)?.[1] || "",
        from: attrs.match(/\bdata-from="([^"]+)"/)?.[1] || "",
        to: attrs.match(/\bdata-to="([^"]+)"/)?.[1] || "",
      };
    })
    .filter(Boolean);
}

function routeStroke(attrs) {
  const styleStroke = attrs.match(/\bstyle="[^"]*\bstroke\s*:\s*([^;"]+)/)?.[1];
  const attrStroke = attrs.match(/\bstroke="([^"]+)"/)?.[1];
  return (styleStroke || attrStroke || "").trim().toLowerCase();
}

function validateSequenceStyle(svg) {
  const failures = [];
  if (!/seqArrow-blue/.test(svg)) failures.push("sequence missing explicit 5x5 color arrow markers");
  if (/id="openArrow"|marker-end="url\(#openArrow\)"/.test(svg)) failures.push("sequence still uses old openArrow marker");
  if (/<path[^>]*class="(?:line|dashed)"/.test(svg)) failures.push("sequence still uses old line/dashed path classes");
  if (!/class="labelPill"/.test(svg)) failures.push("sequence missing message label pills");
  if ((svg.match(/<path[^>]*class="[^"]*seq(?:Return)?[^"]*"/g) || []).length > 2 && !/<circle\b/.test(svg)) {
    failures.push("sequence missing numbered message badges");
  }
  if (/<rect[^>]*class="[^"]*card[^"]*"[^>]*rx="(?:1[6-9]|[2-9]\d)"/.test(svg)) {
    failures.push("sequence participant headers use pill-radius cards");
  }
  const labelCrossings = countSequenceLabelCrossings(svg);
  if (labelCrossings > 0) failures.push(`sequence label/path intersections=${labelCrossings}`);
  const branchCrossings = countSequenceBranchLifelineCrossings(svg);
  if (branchCrossings > 0) failures.push(`sequence branch label/lifeline intersections=${branchCrossings}`);
  return failures;
}

function countSequenceLabelCrossings(svg) {
  const labels = [...svg.matchAll(/<rect[^>]*class="[^"]*labelPill[^"]*"[^>]*>/g)]
    .map((match) => ({
      x: attrNumber(match[0], "x"),
      y: attrNumber(match[0], "y"),
      w: attrNumber(match[0], "width"),
      h: attrNumber(match[0], "height"),
    }))
    .filter((rect) => [rect.x, rect.y, rect.w, rect.h].every((value) => !Number.isNaN(value)));
  const sequencePaths = [...svg.matchAll(/<path[^>]*class="[^"]*seq(?:Return)?[^"]*"[^>]*\bd="([^"]+)"[^>]*>/g)]
    .map((match) => parsePath(match[1]))
    .filter((points) => points.length >= 2);
  let count = 0;
  for (const points of sequencePaths) {
    for (let index = 1; index < points.length; index += 1) {
      const a = points[index - 1];
      const b = points[index];
      for (const label of labels) {
        if (segmentIntersectsRectInterior(a, b, label, 0)) count += 1;
      }
    }
  }
  return count;
}

function countSequenceBranchLifelineCrossings(svg) {
  const labels = [...svg.matchAll(/<rect[^>]*class="[^"]*branchPill[^"]*"[^>]*>/g)]
    .map((match) => ({
      x: attrNumber(match[0], "x"),
      y: attrNumber(match[0], "y"),
      w: attrNumber(match[0], "width"),
      h: attrNumber(match[0], "height"),
    }))
    .filter((rect) => [rect.x, rect.y, rect.w, rect.h].every((value) => !Number.isNaN(value)));
  if (labels.length === 0) return 0;
  const lifelines = [...svg.matchAll(/<line[^>]*class="[^"]*lifeline[^"]*"[^>]*>/g)]
    .map((match) => {
      const tag = match[0];
      return [
        { x: attrNumber(tag, "x1"), y: attrNumber(tag, "y1") },
        { x: attrNumber(tag, "x2"), y: attrNumber(tag, "y2") },
      ];
    })
    .filter((points) => points.every((point) => !Number.isNaN(point.x) && !Number.isNaN(point.y)));
  let count = 0;
  for (const points of lifelines) {
    const [a, b] = points;
    for (const label of labels) {
      if (segmentIntersectsRectInterior(a, b, label, 0)) count += 1;
    }
  }
  return count;
}

function measureViewBoxOverflow(svg) {
  const viewBox = svg.match(/viewBox="0 0 ([\d.]+) ([\d.]+)"/);
  const width = viewBox ? Number(viewBox[1]) : Number(svg.match(/\bwidth="([\d.]+)"/)?.[1] || 0);
  const height = viewBox ? Number(viewBox[2]) : Number(svg.match(/\bheight="([\d.]+)"/)?.[1] || 0);
  if (!width || !height) return { overflow: false, width, height, maxX: 0, maxY: 0 };
  const xs = [];
  const ys = [];
  for (const tag of svg.match(/<(?:rect|line|text)\b[^>]*>/g) || []) {
    for (const name of ["x", "x1", "x2"]) {
      const value = attrNumber(tag, name);
      if (!Number.isNaN(value)) xs.push(value);
    }
    for (const name of ["y", "y1", "y2"]) {
      const value = attrNumber(tag, name);
      if (!Number.isNaN(value)) ys.push(value);
    }
    const x = attrNumber(tag, "x");
    const y = attrNumber(tag, "y");
    const w = attrNumber(tag, "width");
    const h = attrNumber(tag, "height");
    if (!Number.isNaN(x) && !Number.isNaN(w)) xs.push(x + w);
    if (!Number.isNaN(y) && !Number.isNaN(h)) ys.push(y + h);
    if (tag.startsWith("<text") && !Number.isNaN(y)) ys.push(y + 24);
  }
  for (const match of svg.matchAll(/<path[^>]*\bd="([^"]+)"/g)) {
    for (const point of parsePath(match[1])) {
      xs.push(point.x);
      ys.push(point.y);
    }
  }
  const maxX = Math.round(Math.max(0, ...xs) * 10) / 10;
  const maxY = Math.round(Math.max(0, ...ys) * 10) / 10;
  return { overflow: maxX > width + 2 || maxY > height + 2, width, height, maxX, maxY };
}

function hasLayerOrPanelBands(svg) {
  return /class="[^"]*(?:group|[Ll]ayer|panel)[^"]*"/.test(svg);
}

function hasExplicitGridException(svg) {
  return /data-allow-grid="true"|allowGridLayout/i.test(svg);
}

function extractLayerBoxes(svg) {
  const layers = [];
  for (const match of svg.matchAll(/<rect[^>]*class="([^"]*)"[^>]*>/g)) {
    const className = match[1];
    if (!/\b(?:group|layer|layerBand|panel)\b/i.test(className)) continue;
    if (/\b(?:card|classCard|participant|labelPill|pill)\b/i.test(className)) continue;
    const rect = rectFromTag(match[0]);
    if (!rect || rect.w < 160 || rect.h < 70) continue;
    layers.push({ ...rect, className, titleBox: findLayerTitleBox(svg, match.index || 0, rect) });
  }
  return layers;
}

function findLayerTitleBox(svg, startIndex, layer) {
  const groupEnd = svg.indexOf("</g>", startIndex);
  const scope = svg.slice(startIndex, groupEnd > startIndex ? groupEnd : Math.min(svg.length, startIndex + 600));
  const titleMatch = scope.match(/<text[^>]*class="[^"]*(?:panelTitle|layerTitle|groupTitle)[^"]*"[^>]*>([\s\S]*?)<\/text>/);
  if (!titleMatch) return null;
  const tag = titleMatch[0].match(/^<text[^>]*>/)?.[0] || "";
  const x = attrNumber(tag, "x");
  const y = attrNumber(tag, "y");
  if (Number.isNaN(x) || Number.isNaN(y)) return null;
  const text = cleanText(titleMatch[1]);
  const estimatedWidth = Math.max(96, Math.min(layer.w - 24, text.length * 12 + 18));
  return {
    x: Math.max(layer.x, x - 4),
    y: Math.max(layer.y, y - 31),
    w: estimatedWidth,
    h: 42,
  };
}

function extractLabelBoxes(svg) {
  const labels = [];
  for (const match of svg.matchAll(/<rect[^>]*class="([^"]*)"[^>]*>/g)) {
    const className = match[1];
    if (!/\b(?:labelPill|branchPill|note|pill)\b/i.test(className)) continue;
    const rect = rectFromTag(match[0]);
    if (!rect || rect.w < 18 || rect.h < 12) continue;
    labels.push({ ...rect, className });
  }
  return labels;
}

function rectFromTag(tag) {
  const rect = {
    x: attrNumber(tag, "x"),
    y: attrNumber(tag, "y"),
    w: attrNumber(tag, "width"),
    h: attrNumber(tag, "height"),
  };
  return [rect.x, rect.y, rect.w, rect.h].some((value) => Number.isNaN(value)) ? null : rect;
}

function parsePath(d) {
  const tokens = d.match(/[MLHVCSQTAZmlhvcsqtaz]|-?\d*\.?\d+(?:e[-+]?\d+)?/g) || [];
  const points = [];
  let x = 0;
  let y = 0;
  let command = "";
  for (let i = 0; i < tokens.length;) {
    const token = tokens[i++];
    if (/^[A-Za-z]$/.test(token)) {
      command = token;
      if (/Z/i.test(command)) break;
      continue;
    }
    i -= 1;
    if (command === "M" || command === "L") {
      x = Number(tokens[i++]);
      y = Number(tokens[i++]);
      points.push({ x, y });
    } else if (command === "H") {
      x = Number(tokens[i++]);
      points.push({ x, y });
    } else if (command === "V") {
      y = Number(tokens[i++]);
      points.push({ x, y });
    } else if (command === "C") {
      i += 4;
      x = Number(tokens[i++]);
      y = Number(tokens[i++]);
      points.push({ x, y });
    } else {
      break;
    }
  }
  return points;
}

function looksLikeRelationshipGrid(cards, paths, layers) {
  if (cards.length < 6 || paths.length === 0) return false;
  if (layers.length >= 3 && cards.every((card) => layers.some((layer) => rectInside(card, layer)))) return false;
  const xs = clusteredCount(cards.map((card) => card.x), 18);
  const ys = clusteredCount(cards.map((card) => card.y), 18);
  if (xs < 3 || ys < 2) return false;
  const routeDensity = paths.reduce((sum, points) => sum + Math.max(0, points.length - 1), 0);
  if (layers.length >= 2 && routeDensity < cards.length) return false;
  return routeDensity >= Math.max(4, Math.ceil(cards.length / 2));
}

function clusteredCount(values, tolerance) {
  const clusters = [];
  for (const value of values.toSorted((a, b) => a - b)) {
    const cluster = clusters.find((item) => Math.abs(item - value) <= tolerance);
    if (cluster === undefined) clusters.push(value);
  }
  return clusters.length;
}

function countCardOverlaps(cards) {
  let count = 0;
  for (let i = 0; i < cards.length; i += 1) {
    for (let j = i + 1; j < cards.length; j += 1) {
      if (rectsOverlap(cards[i], cards[j], 2)) count += 1;
    }
  }
  return count;
}

function countTangentOrGluedRoutes(paths, cards) {
  let count = 0;
  for (const points of paths) {
    for (let i = 1; i < points.length; i += 1) {
      const a = points[i - 1];
      const b = points[i];
      for (const card of cards) {
        if (segmentRunsAlongRectEdge(a, b, card, 3)) count += 1;
      }
    }
  }
  return count;
}

function countConnectorIntrusions(paths, cards) {
  let count = 0;
  for (const points of paths) {
    for (let i = 1; i < points.length; i += 1) {
      const a = points[i - 1];
      const b = points[i];
      for (const card of cards) {
        if (segmentIntersectsRectInterior(a, b, card, MIN_CLEARANCE) && !segmentEndpointTouchesCard(a, b, card)) count += 1;
      }
    }
  }
  return count;
}

function countDisconnectedRouteEndpoints(routes, cards) {
  let count = 0;
  for (const route of routes) {
    if (!route.from && !route.to) continue;
    const first = route.points[0];
    const last = route.points.at(-1);
    if (route.from) {
      const source = findCardByRouteId(cards, route.from);
      if (source && !pointOnBoundary(first, source, 7)) count += 1;
    }
    if (route.to) {
      const target = findCardByRouteId(cards, route.to);
      if (target && !pointOnBoundary(last, target, 7)) count += 1;
    }
  }
  return count;
}

function findCardByRouteId(cards, value) {
  const normalized = normalizeId(value);
  return cards.find((card) => normalizeId(card.id) === normalized || normalizeId(card.label) === normalized);
}

function normalizeId(value) {
  return String(value || "").replace(/[^A-Za-z0-9]+/g, "").toLowerCase();
}

function countAvoidableDoglegs(paths, cards, layers = []) {
  const blockers = [...cards, ...layers.map((layer) => layerGutter(layer))];
  let count = 0;
  for (const points of paths) {
    const simplified = simplifyCollinearPoints(points);
    if (simplified.length <= 2 || !hasDirectionChange(simplified)) continue;
    const source = cards.find((card) => pointOnBoundary(simplified[0], card));
    const target = cards.find((card) => pointOnBoundary(simplified.at(-1), card));
    if (!source || !target || sameRect(source, target)) continue;
    if (hasClearStraightRoute(source, target, blockers)) count += 1;
  }
  return count;
}

function countRouteConflicts(routes, kind) {
  let count = 0;
  const segments = routes.flatMap((route, routeIndex) => {
    const points = route.points;
    const result = [];
    for (let index = 1; index < points.length; index += 1) {
      result.push({ routeIndex, route, a: points[index - 1], b: points[index] });
    }
    return result;
  });
  for (let i = 0; i < segments.length; i += 1) {
    for (let j = i + 1; j < segments.length; j += 1) {
      if (segments[i].routeIndex === segments[j].routeIndex) continue;
      if (kind === "class" && routesAreVisuallyDistinct(segments[i].route, segments[j].route)) continue;
      if (segmentsConflict(segments[i], segments[j])) count += 1;
    }
  }
  return count;
}

function routesAreVisuallyDistinct(first, second) {
  const classDiffers = routeFamily(first.className) !== routeFamily(second.className);
  const strokeDiffers = first.stroke && second.stroke && first.stroke !== second.stroke;
  const dashDiffers = Boolean(first.dash) !== Boolean(second.dash) || first.dash !== second.dash;
  return classDiffers || strokeDiffers || dashDiffers;
}

function routeFamily(className) {
  if (/\b(?:inherit|inheritLine|implLine)\b/.test(className)) return "inherit";
  if (/\bdependency\b/.test(className)) return "dependency";
  return className || "route";
}

function countLabelConflicts(labels, paths, cards, layers) {
  let count = 0;
  const protectedLabels = labels.filter((label) => !/\bnote\b/i.test(label.className));
  for (const label of protectedLabels) {
    for (const card of cards) {
      if (rectsOverlap(label, card, 1)) count += 1;
    }
    for (const layer of layers) {
      const gutter = layerGutter(layer);
      if (rectsOverlap(label, gutter, 1)) count += 1;
    }
    for (const points of paths) {
      for (let index = 1; index < points.length; index += 1) {
        if (segmentIntersectsRectInterior(points[index - 1], points[index], label, 1)) count += 1;
      }
    }
  }
  return count;
}

function countLayerGutterConflicts(layers, cards, paths, labels) {
  let count = 0;
  for (const layer of layers) {
    const gutter = layerGutter(layer);
    for (const card of cards) {
      if (rectInside(card, layer) && rectsOverlap(card, gutter, 0)) count += 1;
    }
    for (const label of labels) {
      if (rectInside(label, layer) && rectsOverlap(label, gutter, 0)) count += 1;
    }
    for (const points of paths) {
      for (let index = 1; index < points.length; index += 1) {
        if (segmentIntersectsRectInterior(points[index - 1], points[index], gutter, 0)) count += 1;
      }
    }
  }
  return count;
}

function validateFooterInsideFrame(svg, labels) {
  const failures = [];
  const frameTag = svg.match(/<rect[^>]*class="[^"]*frame[^"]*"[^>]*>/)?.[0];
  const frame = frameTag ? rectFromTag(frameTag) : null;
  if (!frame) return failures;
  const footerLabels = labels.filter((label) => /\b(?:note|pill)\b/i.test(label.className) && label.y > frame.y + frame.h * 0.72);
  for (const footer of footerLabels) {
    if (!rectInside(footer, inset(frame, -2))) failures.push("footer outside frame");
    if (Math.abs((footer.x + footer.w / 2) - (frame.x + frame.w / 2)) > Math.max(48, frame.w * 0.12)) {
      failures.push("footer is not horizontally centered in frame");
    }
  }
  return [...new Set(failures)];
}

function validateChartFrameMargins(svg) {
  const failures = [];
  const svgMatch = svg.match(/<svg[^>]+width="([\d.]+)"[^>]+height="([\d.]+)"/);
  const frameTag = svg.match(/<rect[^>]*class="[^"]*frame[^"]*"[^>]*>/)?.[0];
  if (!svgMatch || !frameTag) return failures;
  const width = Number(svgMatch[1]);
  const height = Number(svgMatch[2]);
  const frame = rectFromTag(frameTag);
  if (!frame) return failures;
  const margins = {
    left: frame.x,
    right: width - frame.x - frame.w,
    top: frame.y,
    bottom: height - frame.y - frame.h,
  };
  if (Math.abs(margins.left - margins.right) > 2 || Math.abs(margins.top - margins.bottom) > 2) {
    failures.push(`chart frame margin imbalance=${margins.left}/${margins.right}/${margins.top}/${margins.bottom}`);
  }
  return failures;
}

function layerGutter(layer) {
  if (layer.titleBox) return layer.titleBox;
  return {
    x: layer.x,
    y: layer.y,
    w: Math.min(layer.w, 260),
    h: Math.min(56, Math.max(40, layer.h * 0.24)),
  };
}

function rectInside(rect, outer) {
  return rect.x >= outer.x - 0.5
    && rect.y >= outer.y - 0.5
    && rect.x + rect.w <= outer.x + outer.w + 0.5
    && rect.y + rect.h <= outer.y + outer.h + 0.5;
}

function hasClearStraightRoute(source, target, cards) {
  const sourceCenter = rectCenter(source);
  const targetCenter = rectCenter(target);
  const horizontalGap = target.x >= source.x + source.w || source.x >= target.x + target.w;
  const verticalGap = target.y >= source.y + source.h || source.y >= target.y + target.h;
  const alignedY = Math.abs(sourceCenter.y - targetCenter.y) <= 18
    && source.y + MIN_CLEARANCE < sourceCenter.y
    && sourceCenter.y < source.y + source.h - MIN_CLEARANCE
    && target.y + MIN_CLEARANCE < targetCenter.y
    && targetCenter.y < target.y + target.h - MIN_CLEARANCE;
  if (horizontalGap && alignedY) {
    const y = (sourceCenter.y + targetCenter.y) / 2;
    const startX = sourceCenter.x < targetCenter.x ? source.x + source.w : source.x;
    const endX = sourceCenter.x < targetCenter.x ? target.x : target.x + target.w;
    return segmentClearOfOtherCards({ x: startX, y }, { x: endX, y }, source, target, cards);
  }

  const alignedX = Math.abs(sourceCenter.x - targetCenter.x) <= 18
    && source.x + MIN_CLEARANCE < sourceCenter.x
    && sourceCenter.x < source.x + source.w - MIN_CLEARANCE
    && target.x + MIN_CLEARANCE < targetCenter.x
    && targetCenter.x < target.x + target.w - MIN_CLEARANCE;
  if (verticalGap && alignedX) {
    const x = (sourceCenter.x + targetCenter.x) / 2;
    const startY = sourceCenter.y < targetCenter.y ? source.y + source.h : source.y;
    const endY = sourceCenter.y < targetCenter.y ? target.y : target.y + target.h;
    return segmentClearOfOtherCards({ x, y: startY }, { x, y: endY }, source, target, cards);
  }

  return false;
}

function segmentClearOfOtherCards(a, b, source, target, cards) {
  return cards.every((card) => sameRect(card, source) || sameRect(card, target) || !segmentIntersectsRectInterior(a, b, card, MIN_CLEARANCE));
}

function simplifyCollinearPoints(points) {
  const deduped = [];
  for (const point of points) {
    if (deduped.length === 0 || !samePoint(deduped.at(-1), point)) deduped.push(point);
  }
  const simplified = [];
  for (const point of deduped) {
    simplified.push(point);
    while (simplified.length >= 3) {
      const [a, b, c] = simplified.slice(-3);
      if (!sameDirection(a, b, c)) break;
      simplified.splice(simplified.length - 2, 1);
    }
  }
  return simplified;
}

function hasDirectionChange(points) {
  for (let index = 2; index < points.length; index += 1) {
    if (!sameDirection(points[index - 2], points[index - 1], points[index])) return true;
  }
  return false;
}

function sameDirection(a, b, c) {
  return (near(a.x, b.x, 0.5) && near(b.x, c.x, 0.5)) || (near(a.y, b.y, 0.5) && near(b.y, c.y, 0.5));
}

function samePoint(a, b) {
  return near(a.x, b.x, 0.5) && near(a.y, b.y, 0.5);
}

function segmentRunsAlongRectEdge(a, b, rect, tolerance) {
  if (near(a.x, b.x, tolerance)) {
    const x = a.x;
    if (!near(x, rect.x, tolerance) && !near(x, rect.x + rect.w, tolerance)) return false;
    return rangesOverlap(a.y, b.y, rect.y, rect.y + rect.h, 8);
  }
  if (near(a.y, b.y, tolerance)) {
    const y = a.y;
    if (!near(y, rect.y, tolerance) && !near(y, rect.y + rect.h, tolerance)) return false;
    return rangesOverlap(a.x, b.x, rect.x, rect.x + rect.w, 8);
  }
  return false;
}

function segmentIntersectsRectInterior(a, b, rect, pad) {
  const minX = rect.x - pad;
  const maxX = rect.x + rect.w + pad;
  const minY = rect.y - pad;
  const maxY = rect.y + rect.h + pad;
  if (near(a.y, b.y, 0.5)) {
    if (a.y <= minY || a.y >= maxY) return false;
    return Math.max(a.x, b.x) > minX && Math.min(a.x, b.x) < maxX;
  }
  if (near(a.x, b.x, 0.5)) {
    if (a.x <= minX || a.x >= maxX) return false;
    return Math.max(a.y, b.y) > minY && Math.min(a.y, b.y) < maxY;
  }
  return false;
}

function segmentEndpointTouchesCard(a, b, card) {
  return pointOnBoundary(a, card) || pointOnBoundary(b, card);
}

function pointOnBoundary(point, rect) {
  const inX = point.x >= rect.x - 0.5 && point.x <= rect.x + rect.w + 0.5;
  const inY = point.y >= rect.y - 0.5 && point.y <= rect.y + rect.h + 0.5;
  return inX && inY && (near(point.x, rect.x, 0.5) || near(point.x, rect.x + rect.w, 0.5) || near(point.y, rect.y, 0.5) || near(point.y, rect.y + rect.h, 0.5));
}

function rectsOverlap(a, b, pad) {
  return a.x < b.x + b.w + pad && a.x + a.w + pad > b.x && a.y < b.y + b.h + pad && a.y + a.h + pad > b.y;
}

function segmentsConflict(first, second) {
  const firstDir = segmentDirection(first.a, first.b);
  const secondDir = segmentDirection(second.a, second.b);
  if (firstDir === "point" || secondDir === "point" || firstDir === "diagonal" || secondDir === "diagonal") return false;
  if (firstDir === secondDir) {
    if (firstDir === "horizontal" && !near(first.a.y, second.a.y, 0.5)) return false;
    if (firstDir === "vertical" && !near(first.a.x, second.a.x, 0.5)) return false;
    return rangesOverlap(
      firstDir === "horizontal" ? first.a.x : first.a.y,
      firstDir === "horizontal" ? first.b.x : first.b.y,
      firstDir === "horizontal" ? second.a.x : second.a.y,
      firstDir === "horizontal" ? second.b.x : second.b.y,
      10,
    );
  }
  const horizontal = firstDir === "horizontal" ? first : second;
  const vertical = firstDir === "vertical" ? first : second;
  const x = vertical.a.x;
  const y = horizontal.a.y;
  return insideOpen(x, horizontal.a.x, horizontal.b.x) && insideOpen(y, vertical.a.y, vertical.b.y);
}

function segmentDirection(a, b) {
  if (near(a.x, b.x, 0.5) && near(a.y, b.y, 0.5)) return "point";
  if (near(a.x, b.x, 0.5)) return "vertical";
  if (near(a.y, b.y, 0.5)) return "horizontal";
  return "diagonal";
}

function insideOpen(value, a, b) {
  const min = Math.min(a, b);
  const max = Math.max(a, b);
  return value > min + 0.5 && value < max - 0.5;
}

function inset(rect, pad) {
  return { x: rect.x + pad, y: rect.y + pad, w: rect.w - pad * 2, h: rect.h - pad * 2 };
}

function rangesOverlap(a1, a2, b1, b2, minimum) {
  const left = Math.max(Math.min(a1, a2), Math.min(b1, b2));
  const right = Math.min(Math.max(a1, a2), Math.max(b1, b2));
  return right - left > minimum;
}

function sameRect(a, b) {
  return near(a.x, b.x, 0.5) && near(a.y, b.y, 0.5) && near(a.w, b.w, 0.5) && near(a.h, b.h, 0.5);
}

function rectCenter(rect) {
  return { x: rect.x + rect.w / 2, y: rect.y + rect.h / 2 };
}

function cleanText(value) {
  return String(value || "").replace(/<[^>]+>/g, " ").replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/\s+/g, " ").trim();
}

function near(left, right, tolerance) {
  return Math.abs(left - right) <= tolerance;
}
