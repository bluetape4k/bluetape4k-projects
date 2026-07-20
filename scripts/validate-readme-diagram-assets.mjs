#!/usr/bin/env node

import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const DIAGRAM_DIR = join(ROOT, "docs/images/readme-diagrams");
const REPORT = process.env.DIAGRAM_VALIDATION_REPORT || "/tmp/bluetape4k-projects-diagram-validation-report.json";
const MIN_CLEARANCE = 8;

const files = readdirSync(DIAGRAM_DIR)
  .filter((file) => file.endsWith(".svg"))
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
  const sourcePurposeFailures = validateSourcePurpose(svg, file, title, kind, cards, routes);
  fileFailures.push(...sourcePurposeFailures);

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

  const decisionFailures = validateDecisionFlowLabels(svg);
  fileFailures.push(...decisionFailures);

  if (kind === "flow-state" && cards.length >= 3 && routes.length === 0) {
    fileFailures.push("flow diagram has no connectors");
  }

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

  const textOverflows = countCardTextOverflows(svg);
  if (textOverflows > 0) fileFailures.push(`card text overflows=${textOverflows}`);

  const tangentRoutes = countTangentOrGluedRoutes(paths, cards);
  if (tangentRoutes > 0) fileFailures.push(`0-degree/glued connector attachments=${tangentRoutes}`);

  const intrusions = countConnectorIntrusions(paths, cards);
  if (intrusions > 0) fileFailures.push(`connector/card clearance violations=${intrusions}`);

  const disconnectedEndpoints = countDisconnectedRouteEndpoints(routes, cards);
  if (disconnectedEndpoints > 0) fileFailures.push(`disconnected/floating connector endpoints=${disconnectedEndpoints}`);

  const looseEndpoints = countLooseRouteEndpoints(routes, cards);
  if (looseEndpoints > 0) fileFailures.push(`loose connector endpoints=${looseEndpoints}`);

  const tangentEndpointAngles = countTangentEndpointAngles(routes, cards);
  if (tangentEndpointAngles > 0) fileFailures.push(`tangent endpoint angles=${tangentEndpointAngles}`);

  const routeConflicts = countRouteConflicts(routes, kind);
  if (routeConflicts > 0) {
    fileFailures.push(`${kind === "class" ? "undifferentiated" : "avoidable"} route-route conflicts=${routeConflicts}`);
  }

  if (kind === "class") {
    const reducible = countReducibleClassRoutes(routes, cards);
    if (reducible > 0) fileFailures.push(`class routes with reducible segment count=${reducible}`);
  }

  const labelConflicts = countLabelConflicts(labels, paths, cards, layers);
  if (labelConflicts > 0) fileFailures.push(`label/route/card/layer conflicts=${labelConflicts}`);

  const layerGutterConflicts = countLayerGutterConflicts(layers, cards, paths, labels);
  if (layerGutterConflicts > 0) fileFailures.push(`layer label gutter conflicts=${layerGutterConflicts}`);

  const footerFailures = validateFooterInsideFrame(svg, labels);
  fileFailures.push(...footerFailures);

  const marginFailures = validateContentMargins(svg, kind, cards, routes, layers, labels);
  fileFailures.push(...marginFailures);
  const layerMarginFailures = validateLayerInnerMargins(kind, cards, layers, labels);
  fileFailures.push(...layerMarginFailures);

  if (kind === "chart") {
    fileFailures.push(...validateChartFrameMargins(svg));
    fileFailures.push(...validateChartFooterMeaning(svg));
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
    ["Geometry evidence", /\bGeometry\s*:/i],
    ["validation evidence", /\b(?:validation|gate)\s*[:=]\s*(?:PASS|FAIL|SKIP)\b/i],
    ["repo-only filler footer", /\bbluetape4k-projects\s*-\s*github\.com\/bluetape4k\/bluetape4k-projects\b/i],
  ];
  for (const [label, pattern] of banned) {
    if (pattern.test(svg)) failures.push(`internal footer/evidence text in final SVG: ${label}`);
  }
  return failures;
}

function validateDecisionFlowLabels(svg) {
  if (!/data-layout="decision-flow"/.test(svg)) return [];
  const diamondCount = (svg.match(/<polygon[^>]*class="[^"]*card-shape[^"]*"/g) || []).length;
  if (diamondCount === 0) return [];
  const text = cleanText(svg);
  const labels = text.match(/\b(?:yes|no|fallback|priority|default)\b/gi) || [];
  if (labels.length < Math.min(2, diamondCount)) {
    return [`decision flow missing branch labels diamonds=${diamondCount} labels=${labels.length}`];
  }
  return [];
}

function validateSourcePurpose(svg, file, title, kind, cards, routes) {
  const failures = [];
  if (!["architecture", "module", "flow-state"].includes(kind)) return failures;

  const intent = svg.match(/\bdata-intent="([^"]+)"/)?.[1]?.trim() || "";
  const evidence = svg.match(/\bdata-evidence="([^"]+)"/)?.[1]?.trim() || "";
  const sourceRead = svg.match(/\bdata-source-read="([^"]+)"/)?.[1]?.trim() || "";
  const diagramOne = /-diagram-01\.svg$/i.test(file);
  const purposeTitle = /Overview|overview|Flow|flow|Structure|structure|Architecture|architecture|Component|component|Runtime|runtime|Processing|processing|Pipeline|pipeline|Selection|selection/i.test(`${file} ${title}`);

  if (purposeTitle || diagramOne) {
    if (intent.length < 90) {
      failures.push("missing source-derived diagram intent");
    }
    if (!evidence || evidence.split(/[,;|]/).map((item) => item.trim()).filter(Boolean).length < 2) {
      failures.push("missing source evidence for diagram purpose");
    }
    if (!sourceRead || !/README|src\/main|src\/test|build\.gradle|settings\.gradle|application\.ya?ml|\.kt/i.test(sourceRead)) {
      failures.push("missing inspected source/readme marker");
    }
  }

  if ((/Flow|Processing|Pipeline|Selection|Runtime|Request Routing/i.test(title) || kind === "flow-state") && cards.length >= 3 && routes.length < 2) {
    failures.push("purpose-bearing flow lacks enough route semantics");
  }

  if (/Overview|Architecture|Structure|Component/i.test(title) && cards.length >= 5 && routes.length === 0) {
    failures.push("overview/architecture is an unconnected inventory");
  }

  if (/Selection/i.test(title) && !/data-layout="decision-flow"/.test(svg)) {
    failures.push("selection explanation is not modeled as decision flow");
  }

  return failures;
}

function extractCards(svg) {
  const cards = [];
  for (const group of transformedGroups(svg)) {
    const rectTag = [...group.body.matchAll(/<rect[^>]*>/g)]
      .map((match) => match[0])
      .find((tag) => /class="[^"]*(?:card|classCard|card-shape|soft|chip|note)[^"]*"/i.test(tag));
    const rect = rectTag ? {
      x: attrNumber(rectTag, "x"),
      y: attrNumber(rectTag, "y"),
      w: attrNumber(rectTag, "width"),
      h: attrNumber(rectTag, "height"),
    } : polygonRect(group.body);
    if (!rect || [rect.x, rect.y, rect.w, rect.h].some((value) => Number.isNaN(value))) continue;
    if ((rect.w > 500 && rect.h > 160) || rect.h > 300) continue;
    const labels = [...group.body.matchAll(/<text[^>]*>([\s\S]*?)<\/text>/g)].map((match) => cleanText(match[1])).filter(Boolean);
    cards.push({
      id: group.id || labels[0] || `card-${cards.length + 1}`,
      label: labels[0] || group.id || `card-${cards.length + 1}`,
      x: group.dx + rect.x,
      y: group.dy + rect.y,
      w: rect.w,
      h: rect.h,
    });
  }

  const groupPattern = /<g(?:\s+[^>]*)?>([\s\S]*?)<\/g>/g;
  let groupMatch;
  while ((groupMatch = groupPattern.exec(svg))) {
    const groupOpen = groupMatch[0].match(/^<g(?:\s+[^>]*)?>/)?.[0] || "";
    const transform = groupOpen.match(/transform="translate\(([-\d.]+)[ ,]([-\d.]+)\)"/);
    const groupId = groupOpen.match(/\bid="([^"]+)"/)?.[1] || "";
    if (transform && groupId) continue;
    if (/\bid="(?:layer|panel)-[^"]*"/.test(groupOpen)) continue;
    const body = groupMatch[1];
    if (!/class="[^"]*(?:card|classCard|card-shape|soft|chip|note)[^"]*"/i.test(body)) continue;
    const dx = transform ? Number(transform[1]) : 0;
    const dy = transform ? Number(transform[2]) : 0;
    const rectTag = [...body.matchAll(/<rect[^>]*>/g)]
      .map((match) => match[0])
      .find((tag) => /class="[^"]*(?:card|classCard|card-shape|soft|chip|note)[^"]*"/i.test(tag));
    const rect = rectTag ? {
      x: attrNumber(rectTag, "x"),
      y: attrNumber(rectTag, "y"),
      w: attrNumber(rectTag, "width"),
      h: attrNumber(rectTag, "height"),
    } : polygonRect(body) || circleRect(body);
    if (!rect) continue;
    if ([rect.x, rect.y, rect.w, rect.h].some((value) => Number.isNaN(value))) continue;
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

  return uniqueRects(cards.filter((card) => card.w >= 40 && card.h >= 24));
}

function countCardTextOverflows(svg) {
  let count = 0;
  for (const group of extractCardGroups(svg)) {
    const rect = group.rect;
    const textTags = [...group.body.matchAll(/<text([^>]*)>([\s\S]*?)<\/text>/g)];
    for (const match of textTags) {
      const attrs = match[1];
      const textValue = cleanText(match[2]);
      if (!textValue) continue;
      const x = attrNumber(attrs, "x") + group.dx;
      if (Number.isNaN(x)) continue;
      const className = attrs.match(/\bclass="([^"]*)"/)?.[1] || "";
      const estimated = estimateTextWidth(textValue, className);
      const pad = /\bcardTitle\b/.test(className) ? 28 : 34;
      const allowedLeft = rect.x + pad;
      const allowedRight = rect.x + rect.w - pad;
      const anchor = attrs.match(/\btext-anchor="([^"]+)"/)?.[1] || "start";
      let textLeft;
      let textRight;
      if (anchor === "middle") {
        textLeft = x - estimated / 2;
        textRight = x + estimated / 2;
      } else if (anchor === "end") {
        textLeft = x - estimated;
        textRight = x;
      } else {
        textLeft = x;
        textRight = x + estimated;
      }
      if (textLeft < allowedLeft - 2 || textRight > allowedRight + 2) count += 1;
    }
  }
  return count;
}

function extractCardGroups(svg) {
  const groups = [];
  const groupPattern = /<g([^>]*)>([\s\S]*?)<\/g>/g;
  let match;
  while ((match = groupPattern.exec(svg))) {
    const attrs = match[1];
    const body = match[2];
    if (/\bid="(?:layer|panel)-[^"]*"/.test(attrs)) continue;
    const transform = attrs.match(/transform="translate\(([-\d.]+)[ ,]([-\d.]+)\)"/);
    const dx = transform ? Number(transform[1]) : 0;
    const dy = transform ? Number(transform[2]) : 0;
    const rectTag = [...body.matchAll(/<rect[^>]*>/g)]
      .map((rectMatch) => rectMatch[0])
      .find((tag) => /class="[^"]*(?:card|classCard|card-shape|soft|chip|note)[^"]*"/i.test(tag));
    if (!rectTag) continue;
    const rect = rectFromTag(rectTag);
    if (!rect || rect.w < 40 || rect.h < 24 || (rect.w > 500 && rect.h > 160) || rect.h > 300) continue;
    groups.push({ attrs, body, dx, dy, rect: { ...rect, x: rect.x + dx, y: rect.y + dy } });
  }
  return groups;
}

function estimateTextWidth(value, className) {
  const text = String(value || "");
  let width = 0;
  for (const char of text) {
    if (/[A-Z]/.test(char)) width += 8.1;
    else if (/[a-z0-9]/.test(char)) width += 7.2;
    else if (/\s/.test(char)) width += 4.2;
    else width += 6.8;
  }
  if (/\bcardTitle\b|\bclassTitle\b|\bpanelTitle\b/.test(className)) return width * 1.35;
  if (/\bdetail\b|\bmember\b|\bcaption\b/.test(className)) return width * 1.05;
  return width;
}

function transformedGroups(svg) {
  return [...svg.matchAll(/<g([^>]*)transform="translate\(([-\d.]+)[ ,]([-\d.]+)\)"([^>]*)>([\s\S]*?)<\/g>/g)]
    .map((match) => ({
      id: `${match[1]} ${match[4]}`.match(/\bid="([^"]+)"/)?.[1] || "",
      dx: Number(match[2]),
      dy: Number(match[3]),
      body: match[5],
    }));
}

function uniqueRects(rects) {
  const seen = new Set();
  return rects.filter((rect) => {
    const key = `${rect.id}:${Math.round(rect.x)}:${Math.round(rect.y)}:${Math.round(rect.w)}:${Math.round(rect.h)}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function polygonRect(body) {
  const tag = body.match(/<polygon[^>]*class="[^"]*card-shape[^"]*"[^>]*>/i)?.[0]
    || body.match(/<polygon[^>]*points="[^"]+"[^>]*class="[^"]*card-shape[^"]*"[^>]*>/i)?.[0];
  const pointText = tag?.match(/\bpoints="([^"]+)"/)?.[1];
  if (!pointText) return null;
  const numbers = pointText.match(/-?\d*\.?\d+(?:e[-+]?\d+)?/g)?.map(Number) || [];
  const xs = [];
  const ys = [];
  for (let index = 0; index < numbers.length; index += 2) {
    xs.push(numbers[index]);
    ys.push(numbers[index + 1]);
  }
  const minX = Math.min(...xs);
  const minY = Math.min(...ys);
  return { x: minX, y: minY, w: Math.max(...xs) - minX, h: Math.max(...ys) - minY };
}

function circleRect(body) {
  const tag = body.match(/<circle[^>]*class="[^"]*(?:card|soft|chip|note)[^"]*"[^>]*>/i)?.[0];
  if (!tag) return null;
  const cx = attrNumber(tag, "cx");
  const cy = attrNumber(tag, "cy");
  const r = attrNumber(tag, "r");
  if ([cx, cy, r].some((value) => Number.isNaN(value))) return null;
  return { x: cx - r, y: cy - r, w: r * 2, h: r * 2 };
}

function attrNumber(tag, name) {
  return Number(tag.match(new RegExp(`\\b${name}="([-\\d.]+)"`))?.[1]);
}

function extractRoutes(svg) {
  return [...svg.matchAll(/<path([^>]*)\bd="([^"]+)"([^>]*)>/g)]
    .map((match) => {
      const attrs = `${match[1]} ${match[3]}`;
      const className = attrs.match(/\bclass="([^"]+)"/)?.[1] || "";
      if (!/\b(?:route|flow|inherit|dependency|line|Line|connector|dashed|implLine|inheritLine)\b/.test(className)) return null;
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
  if (!hasFixedSequenceArrowMarkers(svg)) failures.push("sequence missing explicit fixed-size color arrow markers");
  if (/id="openArrow"|marker-end="url\(#openArrow\)"/.test(svg)) failures.push("sequence still uses old openArrow marker");
  if (/<path[^>]*class="(?:line|dashed)"/.test(svg)) failures.push("sequence still uses old line/dashed path classes");
  if (!hasSequenceMessageLabelPills(svg)) failures.push("sequence missing message label pills");
  if ((svg.match(/<path[^>]*class="[^"]*seq(?:Return)?[^"]*"/g) || []).length > 2 && !/<circle\b/.test(svg)) {
    failures.push("sequence missing numbered message badges");
  }
  if (/<rect[^>]*class="[^"]*card[^"]*"[^>]*rx="(?:1[6-9]|[2-9]\d)"/.test(svg)) {
    failures.push("sequence participant headers use pill-radius cards");
  }
  const altOpacities = [
    ...[...svg.matchAll(/(?:\.altBox|\.alt)\{[^}]*fill-opacity\s*:\s*([0-9.]+)/g)].map((match) => Number(match[1])),
    ...[...svg.matchAll(/<rect\b[^>]*class="[^"]*\b(?:altBox|alt)\b[^"]*"[^>]*\bfill-opacity="([0-9.]+)"/g)].map((match) => Number(match[1])),
  ].filter((value) => Number.isFinite(value));
  if (altOpacities.some((value) => value > 0.16)) {
    failures.push("sequence alt/else/loop region fill-opacity must stay near-transparent (<= .16)");
  }
  const labelCrossings = countSequenceLabelCrossings(svg);
  if (labelCrossings > 0) failures.push(`sequence label/path intersections=${labelCrossings}`);
  const branchCrossings = countSequenceBranchLifelineCrossings(svg);
  if (branchCrossings > 0) failures.push(`sequence branch label/lifeline intersections=${branchCrossings}`);
  return failures;
}

function hasFixedSequenceArrowMarkers(svg) {
  const markers = [...svg.matchAll(/<marker\b([^>]*)>([\s\S]*?)<\/marker>/g)];
  if (markers.length === 0) return false;
  return markers.every(([, attrs, body]) => {
    const width = attrNumber(attrs, "markerWidth");
    const height = attrNumber(attrs, "markerHeight");
    const solidHead = /<path\b[^>]*\bfill="(?!none)[^"]+"[^>]*\/?\s*>/.test(body);
    return Number.isFinite(width) && Number.isFinite(height) && width >= 5 && height >= 5 && solidHead;
  });
}

function hasSequenceMessageLabelPills(svg) {
  const labelBackgrounds = [...svg.matchAll(/<rect\b[^>]*class="([^"]+)"[^>]*>/g)]
    .filter((match) => /(?:^|\s)(?:labelPill|label|pill|badge)(?:\s|$)/i.test(match[1]));
  const numberedBadges = (svg.match(/<circle\b/g) || []).length;
  const visibleNumbers = (svg.match(/>\s*\d+\s*<\/text>/g) || []).length;
  return labelBackgrounds.length > 0 && numberedBadges > 0 && visibleNumbers > 0;
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
  return /data-allow-grid="true"|data-layout="decision-flow"|allowGridLayout/i.test(svg);
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
  const titleMatch = scope.match(/<text[^>]*class="[^"]*(?:panelTitle|layerTitle|layer-title|groupTitle)[^"]*"[^>]*>([\s\S]*?)<\/text>/)
    || findNearbyLayerTitle(svg, layer);
  if (!titleMatch) return null;
  const tag = titleMatch[0].match(/^<text[^>]*>/)?.[0] || titleMatch.tag || "";
  const className = tag.match(/\bclass="([^"]*)"/)?.[1] || "";
  const x = attrNumber(tag, "x");
  const y = attrNumber(tag, "y");
  if (Number.isNaN(x) || Number.isNaN(y)) return null;
  const text = cleanText(titleMatch[1] || titleMatch.text || "");
  const estimatedWidth = Math.max(96, Math.min(layer.w - 24, text.length * 12 + 18));
  return {
    x: Math.max(layer.x, x - 4),
    y: Math.max(layer.y, y - 31),
    w: estimatedWidth,
    h: 42,
    titleClass: className,
  };
}

function findNearbyLayerTitle(svg, layer) {
  for (const match of svg.matchAll(/<text[^>]*class="[^"]*(?:panelTitle|layerTitle|layer-title|groupTitle)[^"]*"[^>]*>([\s\S]*?)<\/text>/g)) {
    const tag = match[0].match(/^<text[^>]*>/)?.[0] || "";
    const x = attrNumber(tag, "x");
    const y = attrNumber(tag, "y");
    if (Number.isNaN(x) || Number.isNaN(y)) continue;
    if (x >= layer.x - 8 && x <= layer.x + layer.w + 8 && y >= layer.y - 8 && y <= layer.y + Math.min(72, layer.h)) {
      return { 0: match[0], 1: match[1], tag, text: match[1] };
    }
  }
  return null;
}

function extractLabelBoxes(svg) {
  const labels = [];
  const transformedSpans = [];
  for (const match of svg.matchAll(/<g[^>]*transform="translate\(([-\d.]+)[ ,]([-\d.]+)\)"[^>]*>([\s\S]*?)<\/g>/g)) {
    transformedSpans.push([match.index || 0, (match.index || 0) + match[0].length]);
    const dx = Number(match[1]);
    const dy = Number(match[2]);
    for (const rectMatch of match[3].matchAll(/<rect[^>]*class="([^"]*)"[^>]*>/g)) {
      const className = rectMatch[1];
      if (!/\b(?:labelPill|branchPill|note|pill)\b/i.test(className)) continue;
      const rect = rectFromTag(rectMatch[0]);
      if (!rect || rect.w < 18 || rect.h < 12) continue;
      labels.push({ ...rect, x: dx + rect.x, y: dy + rect.y, className });
    }
  }

  for (const match of svg.matchAll(/<rect[^>]*class="([^"]*)"[^>]*>/g)) {
    const index = match.index || 0;
    if (transformedSpans.some(([start, end]) => index >= start && index < end)) continue;
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
    } else if (command === "Q") {
      i += 2;
      x = Number(tokens[i++]);
      y = Number(tokens[i++]);
      points.push({ x, y });
    } else if (command === "q") {
      i += 2;
      x += Number(tokens[i++]);
      y += Number(tokens[i++]);
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
      if (rectsOverlap(cards[i], cards[j], 0)) count += 1;
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

function countLooseRouteEndpoints(routes, cards) {
  let count = 0;
  for (const route of routes) {
    const points = simplifyCollinearPoints(route.points);
    if (points.length < 2) continue;
    const first = points[0];
    const last = points.at(-1);
    if (route.from || route.to) {
      if (!cards.some((card) => pointOnBoundary(first, card, 6))) count += 1;
      if (!cards.some((card) => pointOnBoundary(last, card, 6))) count += 1;
      continue;
    }
    if (!cards.some((card) => pointOnBoundary(first, card, 6)) && nearAnyCardBoundary(first, cards, 64)) count += 1;
    if (!cards.some((card) => pointOnBoundary(last, card, 6)) && nearAnyCardBoundary(last, cards, 64)) count += 1;
  }
  return count;
}

function nearAnyCardBoundary(point, cards, threshold) {
  return cards.some((card) => distanceToRectBoundary(point, card) <= threshold);
}

function distanceToRectBoundary(point, rect) {
  const clampedX = Math.max(rect.x, Math.min(point.x, rect.x + rect.w));
  const clampedY = Math.max(rect.y, Math.min(point.y, rect.y + rect.h));
  if (point.x >= rect.x && point.x <= rect.x + rect.w && point.y >= rect.y && point.y <= rect.y + rect.h) {
    return Math.min(
      Math.abs(point.x - rect.x),
      Math.abs(point.x - (rect.x + rect.w)),
      Math.abs(point.y - rect.y),
      Math.abs(point.y - (rect.y + rect.h)),
    );
  }
  return Math.hypot(point.x - clampedX, point.y - clampedY);
}

function countTangentEndpointAngles(routes, cards) {
  let count = 0;
  for (const route of routes) {
    const points = simplifyCollinearPoints(route.points);
    if (points.length < 2) continue;
    if (endpointSegmentIsTangent(points[0], points[1], cards)) count += 1;
    if (endpointSegmentIsTangent(points.at(-1), points.at(-2), cards)) count += 1;
  }
  return count;
}

function endpointSegmentIsTangent(endpoint, next, cards) {
  const direction = segmentDirection(endpoint, next);
  if (direction === "point" || direction === "diagonal") return false;
  for (const card of cards) {
    const sides = boundarySides(endpoint, card, 2);
    if (sides.length === 0) continue;
    if (sides.some((side) => (side === "top" || side === "bottom") && direction === "horizontal")) return true;
    if (sides.some((side) => (side === "left" || side === "right") && direction === "vertical")) return true;
  }
  return false;
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

function countReducibleClassRoutes(routes, cards) {
  let count = 0;
  for (const route of routes) {
    const points = simplifyCollinearPoints(route.points);
    if (points.length <= 2) continue;
    const source = route.from ? findCardByRouteId(cards, route.from) : cards.find((card) => pointOnBoundary(points[0], card, 7));
    const target = route.to ? findCardByRouteId(cards, route.to) : cards.find((card) => pointOnBoundary(points.at(-1), card, 7) && !sameRect(card, source || {}));
    if (!source || !target || sameRect(source, target)) continue;
    const minimal = minimalClearClassRouteSegments(source, target, cards);
    if (minimal > 0 && points.length - 1 > minimal) count += 1;
  }
  return count;
}

function minimalClearClassRouteSegments(source, target, cards) {
  if (hasClearStraightBoundaryRoute(source, target, cards)) return 1;
  const candidates = orthogonalOneBendClassRoutes(source, target);
  for (const route of candidates) {
    if (route.length === 3 && route.every(Boolean) && routeClearOfOtherCards(route, source, target, cards)) return 2;
  }
  return 0;
}

function hasClearStraightBoundaryRoute(source, target, cards) {
  const horizontalGap = target.x >= source.x + source.w || source.x >= target.x + target.w;
  if (horizontalGap) {
    const sourceRight = source.x + source.w <= target.x;
    const minY = Math.max(source.y + MIN_CLEARANCE + 14, target.y + MIN_CLEARANCE + 14);
    const maxY = Math.min(source.y + source.h - MIN_CLEARANCE - 14, target.y + target.h - MIN_CLEARANCE - 14);
    if (minY <= maxY) {
      const yValues = [rectCenter(source).y, rectCenter(target).y, (minY + maxY) / 2, minY, maxY].map((value) => clamp(value, minY, maxY));
      for (const y of yValues) {
        const a = { x: sourceRight ? source.x + source.w : source.x, y };
        const b = { x: sourceRight ? target.x : target.x + target.w, y };
        if (segmentClearOfOtherCards(a, b, source, target, cards)) return true;
      }
    }
  }

  const verticalGap = target.y >= source.y + source.h || source.y >= target.y + target.h;
  if (verticalGap) {
    const sourceBelow = source.y + source.h <= target.y;
    const minX = Math.max(source.x + MIN_CLEARANCE + 28, target.x + MIN_CLEARANCE + 28);
    const maxX = Math.min(source.x + source.w - MIN_CLEARANCE - 28, target.x + target.w - MIN_CLEARANCE - 28);
    if (minX <= maxX) {
      const xValues = [rectCenter(source).x, rectCenter(target).x, (minX + maxX) / 2, minX, maxX].map((value) => clamp(value, minX, maxX));
      for (const x of xValues) {
        const a = { x, y: sourceBelow ? source.y + source.h : source.y };
        const b = { x, y: sourceBelow ? target.y : target.y + target.h };
        if (segmentClearOfOtherCards(a, b, source, target, cards)) return true;
      }
    }
  }
  return false;
}

function orthogonalOneBendClassRoutes(source, target) {
  const sourceCenter = rectCenter(source);
  const targetCenter = rectCenter(target);
  const sourceSide = sourceCenter.x <= targetCenter.x ? "right" : "left";
  const targetSide = sourceSide === "right" ? "left" : "right";
  const horizontalStart = sideBoundaryPoint(source, sourceSide, clamp(targetCenter.y, source.y + 32, source.y + source.h - 32));
  const horizontalEnd = sideBoundaryPoint(target, targetSide, clamp(horizontalStart.y, target.y + 32, target.y + target.h - 32));

  const sourceVerticalSide = sourceCenter.y <= targetCenter.y ? "bottom" : "top";
  const targetVerticalSide = sourceVerticalSide === "bottom" ? "top" : "bottom";
  const verticalStart = sideBoundaryPoint(source, sourceVerticalSide, clamp(targetCenter.x, source.x + 48, source.x + source.w - 48));
  const verticalEnd = sideBoundaryPoint(target, targetVerticalSide, clamp(verticalStart.x, target.x + 48, target.x + target.w - 48));

  return [
    simplifyCollinearPoints([horizontalStart, { x: horizontalEnd.x, y: horizontalStart.y }, horizontalEnd]),
    simplifyCollinearPoints([verticalStart, { x: verticalStart.x, y: verticalEnd.y }, verticalEnd]),
  ];
}

function sideBoundaryPoint(rect, side, value) {
  if (side === "left") return { x: rect.x, y: value };
  if (side === "right") return { x: rect.x + rect.w, y: value };
  if (side === "top") return { x: value, y: rect.y };
  return { x: value, y: rect.y + rect.h };
}

function routeClearOfOtherCards(points, source, target, cards) {
  for (let index = 1; index < points.length; index += 1) {
    const a = points[index - 1];
    const b = points[index];
    if (segmentRunsAlongRectEdge(a, b, source, 3) || segmentRunsAlongRectEdge(a, b, target, 3)) return false;
    if (!segmentClearOfOtherCards(a, b, source, target, cards)) return false;
  }
  return true;
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
      if (segmentsConflict(segments[i], segments[j]) && !allowedDistinctRightAngleCrossing(segments[i], segments[j])) count += 1;
    }
  }
  return count;
}

function allowedDistinctRightAngleCrossing(first, second) {
  if (!routesAreVisuallyDistinct(first.route, second.route)) return false;
  return segmentsCrossAtRightAngle(first.a, first.b, second.a, second.b);
}

function routesAreVisuallyDistinct(first, second) {
  const classDiffers = routeFamily(first.className) !== routeFamily(second.className);
  const strokeDiffers = first.stroke && second.stroke && first.stroke !== second.stroke;
  const dashDiffers = Boolean(first.dash) !== Boolean(second.dash) || first.dash !== second.dash;
  return classDiffers || strokeDiffers || dashDiffers;
}

function segmentsCrossAtRightAngle(a, b, c, d) {
  const firstDir = segmentDirection(a, b);
  const secondDir = segmentDirection(c, d);
  if (!((firstDir === "horizontal" && secondDir === "vertical") || (firstDir === "vertical" && secondDir === "horizontal"))) return false;
  if (firstDir === "horizontal") {
    return strictlyBetween(c.x, a.x, b.x) && strictlyBetween(a.y, c.y, d.y);
  }
  return strictlyBetween(a.x, c.x, d.x) && strictlyBetween(c.y, a.y, b.y);
}

function strictlyBetween(value, first, second) {
  const min = Math.min(first, second);
  const max = Math.max(first, second);
  return value > min + 1 && value < max - 1;
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
  const footerLabels = labels.filter((label) => /\b(?:footer|note)\b/i.test(label.className) && label.y > frame.y + frame.h * 0.72);
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

function validateChartFooterMeaning(svg) {
  const failures = [];
  const textNodes = [...svg.matchAll(/<text([^>]*)>([\s\S]*?)<\/text>/g)]
    .map((match) => ({
      className: match[1].match(/\bclass="([^"]*)"/)?.[1] || "",
      text: cleanText(match[2]),
    }))
    .filter((item) => item.text);
  const tinyFooterTexts = textNodes
    .filter((item) => /\b(?:tiny|small)\b/.test(item.className))
    .map((item) => item.text);
  for (const value of tinyFooterTexts) {
    if (/^bluetape4k-projects\s*-\s*github\.com\/bluetape4k\/bluetape4k-projects$/i.test(value)) {
      failures.push("chart footer has repo-only filler text");
    }
    if (/\b(?:geometry=PASS|chart-gate|sampleShape=PASS|frameMargins|footerInnerBottom)\b/i.test(value)) {
      failures.push("chart footer contains validation/internal evidence text");
    }
  }
  const hasSource = textNodes.some((item) => /\bSource\s*:/.test(item.text));
  const hasInterpretation = textNodes.some((item) => /\b(?:higher is better|lower is better|caveat|caveats|split scales|grouped bars|Panels are separated|benchmark mode|Unit\/direction)\b/i.test(item.text));
  if (!hasSource && /throughput|benchmark|latency/i.test(cleanText(svg))) {
    failures.push("chart footer/source data provenance missing");
  }
  if (!hasInterpretation) {
    failures.push("chart footer lacks unit/direction or interpretation cue");
  }
  return [...new Set(failures)];
}

function validateContentMargins(svg, kind, cards, routes, layers, labels) {
  if (kind === "chart" || kind === "sequence") return [];
  const frameTag = svg.match(/<rect[^>]*class="[^"]*frame[^"]*"[^>]*>/)?.[0];
  const frame = frameTag ? rectFromTag(frameTag) : null;
  if (!frame) return [];

  const footerCutoff = frame.y + frame.h * 0.78;
  const nonFooterLabels = labels.filter((label) => label.y < footerCutoff);
  const routePoints = routes.flatMap((route) => route.points);
  const bodyRects = [
    ...layers,
    ...cards,
    ...nonFooterLabels,
    ...routePoints.map((point) => ({ x: point.x, y: point.y, w: 0, h: 0 })),
  ].filter((rect) => rect && Number.isFinite(rect.x) && Number.isFinite(rect.y) && Number.isFinite(rect.w) && Number.isFinite(rect.h));

  if (bodyRects.length < 2) return [];

  const bounds = unionRects(bodyRects);
  const contentTop = Math.max(frame.y + 118, firstBodyYFromSvg(svg, frame));
  const footerTop = Math.min(footerTopFromSvg(svg, frame), labels
    .filter((label) => /\b(?:note|pill)\b/i.test(label.className) && label.y > footerCutoff)
    .map((label) => label.y)
    .reduce((min, y) => Math.min(min, y), frame.y + frame.h - 24));
  const contentBottom = Math.min(frame.y + frame.h - 24, footerTop - 24);
  const contentArea = {
    x: frame.x + 24,
    y: contentTop,
    w: Math.max(1, frame.w - 48),
    h: Math.max(1, contentBottom - contentTop),
  };
  const margins = {
    left: Math.round(bounds.x - contentArea.x),
    right: Math.round(contentArea.x + contentArea.w - (bounds.x + bounds.w)),
    top: Math.round(bounds.y - contentArea.y),
    bottom: Math.round(contentArea.y + contentArea.h - (bounds.y + bounds.h)),
  };

  const frameMargins = {
    left: Math.round(bounds.x - frame.x),
    right: Math.round(frame.x + frame.w - (bounds.x + bounds.w)),
    top: Math.round(bounds.y - frame.y),
    bottom: Math.round(frame.y + frame.h - (bounds.y + bounds.h)),
  };
  const allowedX = Math.max(28, Math.round(contentArea.w * 0.04));
  const allowedY = Math.max(24, Math.round(contentArea.h * 0.04));
  const failures = [];
  if (Math.min(frameMargins.left, frameMargins.right, frameMargins.top, frameMargins.bottom) < -8) {
    failures.push(`content outside frame=${frameMargins.left}/${frameMargins.right}/${frameMargins.top}/${frameMargins.bottom}`);
  }
  if (margins.left >= -8 && margins.right >= -8 && Math.abs(margins.left - margins.right) > allowedX) {
    failures.push(`content horizontal margin imbalance=${margins.left}/${margins.right} allowed=${allowedX}`);
  }
  if (margins.top >= -8 && margins.bottom >= -8 && Math.abs(margins.top - margins.bottom) > allowedY) {
    failures.push(`content vertical margin imbalance=${margins.top}/${margins.bottom} allowed=${allowedY}`);
  }
  return failures;
}

function validateLayerInnerMargins(kind, cards, layers, labels) {
  if (!["architecture", "module", "flow-state"].includes(kind) || layers.length === 0) return [];
  const failures = [];
  for (const layer of layers) {
    const containedCards = cards.filter((card) => rectInside(card, layer));
    if (containedCards.length < 2) continue;
    const layerLabels = labels.filter((label) => rectInside(label, layer) && label.y < layer.y + layer.h * 0.85);
    const minCardY = Math.min(...containedCards.map((card) => card.y));
    const gutter = layerGutter(layer);
    const sideLayerTitle = /\blayer-title\b/.test(gutter.titleClass || "") || (!layer.titleBox && /\blayer\b/i.test(layer.className || ""));
    const topTitle = !sideLayerTitle && gutter.y <= layer.y + 72 && gutter.x <= layer.x + Math.max(48, layer.w * 0.35);
    const bodyArea = layerBodyArea(layer, gutter, topTitle);
    const bounds = unionRects([...containedCards, ...layerLabels.filter((label) => !rectInside(label, gutter))]);
    const margins = {
      left: Math.round(bounds.x - bodyArea.x),
      right: Math.round(bodyArea.x + bodyArea.w - (bounds.x + bounds.w)),
      top: Math.round(bounds.y - bodyArea.y),
      bottom: Math.round(bodyArea.y + bodyArea.h - (bounds.y + bounds.h)),
    };
    const allowedX = Math.max(24, Math.round(bodyArea.w * 0.05));
    const allowedY = Math.max(18, Math.round(bodyArea.h * 0.08));
    if (Math.min(margins.left, margins.right) < -8) {
      failures.push(`layer content outside body=${Math.round(layer.x)}/${Math.round(layer.y)} margins=${margins.left}/${margins.right}`);
    } else if (Math.abs(margins.left - margins.right) > allowedX) {
      failures.push(`layer horizontal margin imbalance=${Math.round(layer.x)}/${Math.round(layer.y)} ${margins.left}/${margins.right} allowed=${allowedX}`);
    }
    if (Math.min(margins.top, margins.bottom) < -8) {
      failures.push(`layer vertical content outside body=${Math.round(layer.x)}/${Math.round(layer.y)} margins=${margins.top}/${margins.bottom}`);
    } else if (Math.abs(margins.top - margins.bottom) > allowedY) {
      failures.push(`layer vertical margin imbalance=${Math.round(layer.x)}/${Math.round(layer.y)} ${margins.top}/${margins.bottom} allowed=${allowedY}`);
    }
  }
  return failures;
}

function layerBodyArea(layer, gutter, topTitle) {
  if (topTitle) {
    const y = gutter.y + gutter.h + 8;
    return {
      x: layer.x + 28,
      y,
      w: Math.max(1, layer.w - 56),
      h: Math.max(1, layer.y + layer.h - y - 22),
    };
  }

  const labelGutter = Math.min(260, Math.max(116, gutter.w + 22));
  return {
    x: layer.x + labelGutter,
    y: layer.y + 24,
    w: Math.max(1, layer.w - labelGutter - 28),
    h: Math.max(1, layer.h - 48),
  };
}

function firstBodyYFromSvg(svg, frame) {
  const subtitleMatches = [...svg.matchAll(/<text[^>]*class="[^"]*subtitle[^"]*"[^>]*>/g)]
    .map((match) => attrNumber(match[0], "y"))
    .filter((value) => !Number.isNaN(value));
  if (subtitleMatches.length === 0) return frame.y + 118;
  return Math.max(...subtitleMatches) + 38;
}

function footerTopFromSvg(svg, frame) {
  const cutoff = frame.y + frame.h * 0.72;
  return [...svg.matchAll(/<text[^>]*y="([\d.]+)"[^>]*>([\s\S]*?)<\/text>/g)]
    .map((match) => ({ y: Number(match[1]), text: stripTags(match[2]) }))
    .filter((item) => Number.isFinite(item.y) && item.y > cutoff)
    .filter((item) => /\b(?:github\.com\/bluetape4k|bluetape4k-projects|bluetape4k)\b/i.test(item.text))
    .map((item) => item.y)
    .reduce((min, y) => Math.min(min, y), frame.y + frame.h - 24);
}

function stripTags(value) {
  return String(value || "").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
}

function unionRects(rects) {
  const minX = Math.min(...rects.map((rect) => rect.x));
  const minY = Math.min(...rects.map((rect) => rect.y));
  const maxX = Math.max(...rects.map((rect) => rect.x + rect.w));
  const maxY = Math.max(...rects.map((rect) => rect.y + rect.h));
  return { x: minX, y: minY, w: maxX - minX, h: maxY - minY };
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

function pointOnBoundary(point, rect, tolerance = 0.5) {
  const inX = point.x >= rect.x - tolerance && point.x <= rect.x + rect.w + tolerance;
  const inY = point.y >= rect.y - tolerance && point.y <= rect.y + rect.h + tolerance;
  return inX && inY && boundarySides(point, rect, tolerance).length > 0;
}

function boundarySides(point, rect, tolerance = 0.5) {
  const inX = point.x >= rect.x - tolerance && point.x <= rect.x + rect.w + tolerance;
  const inY = point.y >= rect.y - tolerance && point.y <= rect.y + rect.h + tolerance;
  if (!inX || !inY) return [];
  const sides = [];
  if (near(point.x, rect.x, tolerance)) sides.push("left");
  if (near(point.x, rect.x + rect.w, tolerance)) sides.push("right");
  if (near(point.y, rect.y, tolerance)) sides.push("top");
  if (near(point.y, rect.y + rect.h, tolerance)) sides.push("bottom");
  return sides;
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

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}
