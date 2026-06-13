#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const outDir = "docs/images/readme-diagrams";
mkdirSync(outDir, { recursive: true });

const palette = {
  ink: "#22344A",
  body: "#34465B",
  muted: "#627184",
  frame: "#D7E2EC",
  canvas: "#F6F9FC",
  neutral: "#758297",
  request: "#5B8DEF",
  service: "#45A7A1",
  event: "#58A978",
  metrics: "#D6A441",
  trace: "#8A72D6",
  response: "#DC6B82",
  cardBlue: "#E8F3FF",
  cardTeal: "#E9F7F6",
  cardGreen: "#EAF7EF",
  cardAmber: "#FFF3D9",
  cardPurple: "#F1ECFF",
  cardRose: "#FDECEF",
  cardWhite: "#FFFFFF",
};

const fontCss = `
      .title{font-family:"Architects Daughter";font-size:42px;fill:${palette.ink};font-weight:400}
      .subtitle{font-family:"Comic Mono";font-size:16px;fill:${palette.muted};font-weight:400}
      .cardTitle,.participantTitle{font-family:"Architects Daughter";font-size:23px;fill:${palette.ink};font-weight:400}
      .body,.messageLabel,.edgeLabel,.note,.legend{font-family:"Comic Mono";fill:${palette.body};font-weight:400}
      .body{font-size:14px}.messageLabel{font-size:13px}.edgeLabel{font-size:12px}.note{font-size:13px}.legend{font-size:12px}
`;

function escapeXml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function markerDefs(colors, kind) {
  const marker = kind === "sequence"
    ? { w: 8, h: 8, refX: 7, refY: 4, path: "M 1 1 L 7 4 L 1 7 Z" }
    : { w: 5, h: 5, refX: 4.5, refY: 2.5, path: "M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" };
  return colors
    .map(({ id, color }) => `
    <marker id="${id}" viewBox="0 0 ${marker.w} ${marker.h}" markerWidth="${marker.w}" markerHeight="${marker.h}" refX="${marker.refX}" refY="${marker.refY}" orient="auto" markerUnits="strokeWidth">
      <path d="${marker.path}" fill="${color}"/>
    </marker>`)
    .join("");
}

function textBlock(lines, x, centerY, className, lineHeight = 22, anchor = "middle") {
  const startY = centerY - ((lines.length - 1) * lineHeight) / 2;
  return lines
    .map(
      (line, index) =>
        `<text class="${className}" x="${x}" y="${startY + index * lineHeight}" text-anchor="${anchor}" dominant-baseline="middle">${escapeXml(line)}</text>`,
    )
    .join("\n");
}

function card(node) {
  const centerX = node.x + node.w / 2;
  const titleLines = node.titleLines ?? [node.title];
  const bodyLines = node.bodyLines ?? [];
  const titleHeight = titleLines.length * 26;
  const bodyHeight = bodyLines.length * 20;
  const gap = bodyLines.length > 0 ? 18 : 0;
  const total = titleHeight + gap + bodyHeight;
  const top = node.y + node.h / 2 - total / 2;
  const titleCenter = top + titleHeight / 2;
  const bodyCenter = top + titleHeight + gap + bodyHeight / 2;

  return `
  <g id="${node.id}">
    <rect class="card" x="${node.x}" y="${node.y}" width="${node.w}" height="${node.h}" rx="16" fill="${node.fill}" stroke="${node.stroke}"/>
    ${textBlock(titleLines, centerX, titleCenter, "cardTitle", 26)}
    ${bodyLines.length > 0 ? textBlock(bodyLines, centerX, bodyCenter, "body", 20) : ""}
  </g>`;
}

function participant(node) {
  return `
  <g id="${node.id}">
    <rect class="participant" x="${node.x}" y="${node.y}" width="${node.w}" height="${node.h}" rx="10" fill="${node.fill}" stroke="${node.stroke}"/>
    ${textBlock(node.titleLines ?? [node.title], node.x + node.w / 2, node.y + node.h / 2, "participantTitle", 24)}
    <line class="lifeline" x1="${node.x + node.w / 2}" y1="${node.y + node.h}" x2="${node.x + node.w / 2}" y2="${node.lifelineBottom}" />
  </g>`;
}

function routePath(points) {
  return points.map((point, index) => `${index === 0 ? "M" : "L"}${point.x} ${point.y}`).join(" ");
}

function labelBox(label) {
  const w = label.w ?? Math.max(150, label.text.length * 7.3 + 26);
  const h = label.h ?? 26;
  const x = label.x - w / 2;
  const y = label.y - h / 2;
  return `
    <rect class="labelPill" x="${x}" y="${y}" width="${w}" height="${h}" rx="7"/>
    <text class="${label.className ?? "edgeLabel"}" x="${label.x}" y="${label.y + 1}" text-anchor="middle" dominant-baseline="middle">${escapeXml(label.text)}</text>`;
}

function drawRoutes(routes) {
  return routes
    .map((route) => {
      const marker = route.marker ? ` marker-end="url(#${route.marker})"` : "";
      const dash = route.dash ? ` stroke-dasharray="${route.dash}"` : "";
      const label = route.label ? labelBox(route.label) : "";
      return `
  <g id="${route.id}">
    <path class="${route.className ?? "edge"}" d="${routePath(route.points)}" stroke="${route.color}"${marker}${dash}/>
    ${label}
  </g>`;
    })
    .join("\n");
}

function lineRoute({ id, from, to, y, label, color = palette.request, marker = "arrowRequest", dash }) {
  return {
    id,
    from,
    to,
    color,
    marker,
    dash,
    points: [{ x: from.cx, y }, { x: to.cx, y }],
    label: label ? { text: label, x: (from.cx + to.cx) / 2, y: y - 20 } : undefined,
  };
}

function returnRoute({ id, from, to, y, label, color = palette.response }) {
  return lineRoute({ id, from, to, y, label, color, marker: "arrowResponse", dash: "8 7" });
}

function boundarySide(point, node) {
  const eps = 0.01;
  const centerX = node.x + node.w / 2;
  if (
    node.lifelineBottom &&
    Math.abs(point.x - centerX) < eps &&
    point.y >= node.y + node.h - eps &&
    point.y <= node.lifelineBottom + eps
  ) return "lifeline";
  if (Math.abs(point.x - node.x) < eps && point.y >= node.y - eps && point.y <= node.y + node.h + eps) return "left";
  if (Math.abs(point.x - (node.x + node.w)) < eps && point.y >= node.y - eps && point.y <= node.y + node.h + eps) return "right";
  if (Math.abs(point.y - node.y) < eps && point.x >= node.x - eps && point.x <= node.x + node.w + eps) return "top";
  if (Math.abs(point.y - (node.y + node.h)) < eps && point.x >= node.x - eps && point.x <= node.x + node.w + eps) return "bottom";
  return "inside";
}

function segmentOrientation(a, b) {
  if (a.x === b.x && a.y !== b.y) return "vertical";
  if (a.y === b.y && a.x !== b.x) return "horizontal";
  return "diagonal";
}

function pointRectDistance(point, rect) {
  const dx = Math.max(rect.x - point.x, 0, point.x - (rect.x + rect.w));
  const dy = Math.max(rect.y - point.y, 0, point.y - (rect.y + rect.h));
  return Math.hypot(dx, dy);
}

function segmentIntersectsRect(a, b, rect, clearance = 0) {
  const r = {
    x: rect.x - clearance,
    y: rect.y - clearance,
    w: rect.w + clearance * 2,
    h: rect.h + clearance * 2,
  };
  if (segmentOrientation(a, b) === "horizontal") {
    const minX = Math.min(a.x, b.x);
    const maxX = Math.max(a.x, b.x);
    return a.y >= r.y && a.y <= r.y + r.h && maxX > r.x && minX < r.x + r.w;
  }
  if (segmentOrientation(a, b) === "vertical") {
    const minY = Math.min(a.y, b.y);
    const maxY = Math.max(a.y, b.y);
    return a.x >= r.x && a.x <= r.x + r.w && maxY > r.y && minY < r.y + r.h;
  }
  return true;
}

function validateGeometry(diagram) {
  const nodes = new Map(diagram.nodes.map((node) => [node.id, node]));
  let badEndpointAngle = 0;
  let badBends = 0;
  let interiorCrossings = 0;
  let clearanceViolations = 0;
  let layerContainmentViolations = 0;
  let segments = 0;

  for (const route of diagram.routes) {
    if (!route.from || !route.to) continue;
    const from = nodes.get(route.from);
    const to = nodes.get(route.to);
    const start = route.points[0];
    const end = route.points[route.points.length - 1];
    const startSide = boundarySide(start, from);
    const endSide = boundarySide(end, to);
    const firstOrientation = segmentOrientation(route.points[0], route.points[1]);
    const lastOrientation = segmentOrientation(route.points[route.points.length - 2], route.points[route.points.length - 1]);
    const expectedStart = startSide === "left" || startSide === "right" || startSide === "lifeline" ? "horizontal" : "vertical";
    const expectedEnd = endSide === "left" || endSide === "right" || endSide === "lifeline" ? "horizontal" : "vertical";

    if (startSide === "inside" || endSide === "inside" || firstOrientation !== expectedStart || lastOrientation !== expectedEnd) {
      badEndpointAngle += 1;
    }

    for (let index = 0; index < route.points.length - 1; index += 1) {
      const a = route.points[index];
      const b = route.points[index + 1];
      const orientation = segmentOrientation(a, b);
      segments += 1;
      if (orientation === "diagonal") badBends += 1;

      for (const node of diagram.nodes) {
        if (node.id === route.from || node.id === route.to) continue;
        if (segmentIntersectsRect(a, b, node, 0)) interiorCrossings += 1;
        if (segmentIntersectsRect(a, b, node, 8)) {
          const farEnough = pointRectDistance(a, node) >= 8 || pointRectDistance(b, node) >= 8;
          if (farEnough) clearanceViolations += 1;
        }
      }
    }
  }

  const marginBlocks = diagram.layers?.length
    ? diagram.layers.map((layer) => ({ x: layer.x, y: layer.y, w: layer.w, h: layer.h }))
    : diagram.nodes.map((node) => ({ x: node.x, y: node.y, w: node.w, h: node.lifelineBottom ? node.lifelineBottom - node.y : node.h }));
  const minX = Math.min(...marginBlocks.map((block) => block.x));
  const maxX = Math.max(...marginBlocks.map((block) => block.x + block.w));
  const minY = Math.min(...marginBlocks.map((block) => block.y));
  const maxY = Math.max(...marginBlocks.map((block) => block.y + block.h));
  const left = minX - diagram.frame.x;
  const right = diagram.frame.x + diagram.frame.w - maxX;
  const top = minY - diagram.bodyTop;
  const bottom = diagram.bodyBottom - maxY;
  const marginImbalance = Math.round(Math.max(Math.abs(left - right), Math.abs(top - bottom)));
  const titleGap = Math.round(minY - diagram.subtitleY);
  const sequenceParticipantGap = diagram.kind === "sequence"
    ? Math.round(Math.min(...diagram.nodes
        .map((node) => node.x + node.w / 2)
        .sort((a, b) => a - b)
        .slice(1)
        .map((x, index, sortedTail) => x - (index === 0 ? Math.min(...diagram.nodes.map((node) => node.x + node.w / 2)) : sortedTail[index - 1]))))
    : 0;

  if (diagram.layers?.length) {
    const layers = new Map(diagram.layers.map((layer) => [layer.id, layer]));
    for (const node of diagram.nodes) {
      if (!node.layer) continue;
      const layer = layers.get(node.layer);
      if (!layer) {
        layerContainmentViolations += 1;
        continue;
      }
      const contains =
        node.x >= layer.x &&
        node.y >= layer.y &&
        node.x + node.w <= layer.x + layer.w &&
        node.y + node.h <= layer.y + layer.h;
      if (!contains) layerContainmentViolations += 1;
    }
  }

  const summary = {
    nodes: diagram.nodes.length,
    routes: diagram.routes.length,
    segments,
    badEndpointAngle,
    badBends,
    interiorCrossings,
    nodeOverlaps: countNodeOverlaps(diagram.nodes),
    clearanceViolations,
    laneClearance: clearanceViolations,
    layerContainmentViolations,
    marginImbalance,
    margins: { left, right, top, bottom },
    titleGap,
    sequenceParticipantGap,
  };

  const failures = [];
  if (badEndpointAngle !== 0) failures.push("bad endpoint angle");
  if (badBends !== 0) failures.push("non-orthogonal bend");
  if (interiorCrossings !== 0) failures.push("connector crosses non-endpoint box");
  if (summary.nodeOverlaps !== 0) failures.push("node overlaps");
  if (clearanceViolations !== 0) failures.push("connector lacks 8px clearance");
  if (layerContainmentViolations !== 0) failures.push("node outside assigned layer");
  if (diagram.kind === "sequence" && diagram.nodes.length >= 6 && diagram.width < 1600) failures.push("complex sequence width < 1600");
  if (diagram.kind === "sequence" && diagram.nodes.length >= 6 && sequenceParticipantGap < 190) failures.push(`participant gap ${sequenceParticipantGap} < 190`);
  if (titleGap < diagram.minTitleGap) failures.push(`title gap ${titleGap} < ${diagram.minTitleGap}`);
  if (marginImbalance > diagram.maxMarginImbalance) failures.push(`margin imbalance ${marginImbalance} > ${diagram.maxMarginImbalance}`);

  return { summary, failures };
}

function countNodeOverlaps(nodes) {
  let count = 0;
  for (let leftIndex = 0; leftIndex < nodes.length; leftIndex += 1) {
    for (let rightIndex = leftIndex + 1; rightIndex < nodes.length; rightIndex += 1) {
      const a = nodes[leftIndex];
      const b = nodes[rightIndex];
      if (a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y) count += 1;
    }
  }
  return count;
}

function shiftDiagramY(diagram, delta) {
  return {
    ...diagram,
    layers: diagram.layers?.map((layer) => ({ ...layer, y: layer.y + delta })),
    nodes: diagram.nodes.map((node) => ({
      ...node,
      y: node.y + delta,
      lifelineBottom: node.lifelineBottom ? node.lifelineBottom + delta : node.lifelineBottom,
    })),
    routes: diagram.routes.map((route) => ({
      ...route,
      points: route.points.map((point) => ({ ...point, y: point.y + delta })),
    })),
  };
}

function dotFor(diagram) {
  const rows = [
    `digraph "${diagram.file}" {`,
    `  graph [rankdir=${diagram.layers?.length ? "TB" : "LR"}, bgcolor="#F6F9FC", splines=ortho, nodesep=0.7, ranksep=1.0];`,
    "  node [shape=box, style=\"rounded,filled\", color=\"#D7E2EC\", fillcolor=\"#FFFFFF\", fontname=\"Architects Daughter\"];",
    "  edge [color=\"#758297\", arrowsize=0.8, fontname=\"Comic Mono\"];",
  ];
  const writtenNodes = new Set();
  if (diagram.layers?.length) {
    for (const layer of diagram.layers) {
      rows.push(`  subgraph cluster_${layer.id} {`);
      rows.push(`    label="${layer.label.replaceAll('"', '\\"')}";`);
      rows.push("    color=\"#D7E2EC\";");
      rows.push("    style=\"rounded\";");
      rows.push("    rank=same;");
      for (const node of diagram.nodes.filter((candidate) => candidate.layer === layer.id)) {
        const label = node.dotLabel ?? node.title ?? (node.titleLines ?? []).join(" ");
        rows.push(`    ${node.id} [label="${label.replaceAll('"', '\\"')}", fillcolor="${node.fill}", color="${node.stroke}"];`);
        writtenNodes.add(node.id);
      }
      rows.push("  }");
    }
  }
  for (const node of diagram.nodes.filter((candidate) => !writtenNodes.has(candidate.id))) {
    const label = node.dotLabel ?? node.title ?? (node.titleLines ?? []).join(" ");
    rows.push(`  ${node.id} [label="${label.replaceAll('"', '\\"')}", fillcolor="${node.fill}", color="${node.stroke}"];`);
  }
  for (const route of diagram.routes) {
    if (!route.from || !route.to) continue;
    const style = route.dash ? ", style=dashed" : "";
    rows.push(`  ${route.from} -> ${route.to} [label="${route.dotLabel ?? route.label?.text ?? ""}", color="${route.color}"${style}];`);
  }
  rows.push("}");
  return rows.join("\n");
}

function renderSvg(diagram) {
  const markerColors = [
    { id: "arrowNeutral", color: palette.neutral },
    { id: "arrowRequest", color: palette.request },
    { id: "arrowService", color: palette.service },
    { id: "arrowEvent", color: palette.event },
    { id: "arrowMetrics", color: palette.metrics },
    { id: "arrowTrace", color: palette.trace },
    { id: "arrowResponse", color: palette.response },
  ];

  const nodeMarkup = diagram.kind === "sequence"
    ? diagram.nodes.map(participant).join("\n")
    : diagram.nodes.map(card).join("\n");
  const layerStyle = (diagram.layers ?? []).length > 0
    ? `
      .layerBand{fill:#FFFFFF;stroke:${palette.frame};stroke-width:1.4;opacity:.72}
      .layerLabel{font-family:"Architects Daughter";font-size:18px;fill:${palette.muted};font-weight:400;paint-order:stroke;stroke:#fff;stroke-width:5px;stroke-linejoin:round}`
    : "";
  const layerMarkup = (diagram.layers ?? []).length > 0
    ? `\n  ${(diagram.layers ?? [])
      .map((layer) => `<g id="${layer.id}"><rect class="layerBand" x="${layer.x}" y="${layer.y}" width="${layer.w}" height="${layer.h}" rx="18"/><text class="layerLabel" x="${layer.x + 20}" y="${layer.y + 18}" dominant-baseline="middle">${escapeXml(layer.label)}</text></g>`)
      .join("\n  ")}`
    : "";

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${diagram.width}" height="${diagram.height}" viewBox="0 0 ${diagram.width} ${diagram.height}" role="img" aria-labelledby="${diagram.file}-title ${diagram.file}-desc">
  <title id="${diagram.file}-title">${escapeXml(diagram.title)}</title>
  <desc id="${diagram.file}-desc">${escapeXml(diagram.subtitle)}</desc>
  <defs>
    <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="7" stdDeviation="8" flood-color="#1f2937" flood-opacity="0.10"/></filter>
    ${markerDefs(markerColors, diagram.kind)}
    <style>
      .canvas{fill:${palette.canvas}}.frame{fill:#FFFFFF;stroke:${palette.frame};stroke-width:2}
      .card,.participant{filter:url(#shadow);stroke-width:2}${layerStyle}
      .lifeline{stroke:${palette.neutral};stroke-width:1.8;stroke-dasharray:7 8}
      .edge{stroke-width:2.4;fill:none}
      .sequenceEdge{stroke-width:2.3;fill:none}
      .labelPill{fill:#FFFFFF;stroke:${palette.frame};stroke-width:1;opacity:.94}
      ${fontCss}
    </style>
  </defs>
  <rect class="canvas" width="${diagram.width}" height="${diagram.height}"/>
  <rect class="frame" x="${diagram.frame.x}" y="${diagram.frame.y}" width="${diagram.frame.w}" height="${diagram.frame.h}" rx="26"/>
  <text class="title" x="66" y="82">${escapeXml(diagram.title)}</text>
  <text class="subtitle" x="70" y="${diagram.subtitleY}">${escapeXml(diagram.subtitle)}</text>${layerMarkup}
  ${diagram.kind === "sequence" ? nodeMarkup + "\n" + drawRoutes(diagram.routes.map((route) => ({ ...route, className: "sequenceEdge" }))) : drawRoutes(diagram.routes) + "\n" + nodeMarkup}
  <text class="note" x="${diagram.width / 2}" y="${diagram.height - 30}" text-anchor="middle">${escapeXml("Graphviz DOT/plain/sketch evidence is stored next to this README PNG/SVG asset.")}</text>
</svg>
`;
  return svg.replace(/[ \t]+$/gm, "");
}

function generate(diagram) {
  const dotPath = join(outDir, `${diagram.file}.dot`);
  const plainPath = join(outDir, `${diagram.file}.plain`);
  const sketchSvgPath = join(outDir, `${diagram.file}-sketch.svg`);
  const sketchPngPath = join(outDir, `${diagram.file}-sketch.png`);
  const svgPath = join(outDir, `${diagram.file}.svg`);
  const pngPath = join(outDir, `${diagram.file}.png`);

  writeFileSync(dotPath, dotFor(diagram));
  execFileSync("dot", ["-Tplain", "-o", plainPath, dotPath], { stdio: "inherit" });
  execFileSync("dot", ["-Tsvg", "-o", sketchSvgPath, dotPath], { stdio: "inherit" });
  execFileSync("dot", ["-Tpng", "-o", sketchPngPath, dotPath], { stdio: "inherit" });

  const { summary, failures } = validateGeometry(diagram);
  const margins = `${summary.margins.left}/${summary.margins.right}/${summary.margins.top}/${summary.margins.bottom}`;
  const sequenceGap = diagram.kind === "sequence" ? `, sequenceParticipantGap=${summary.sequenceParticipantGap}` : "";
  const summaryText = `${diagram.file}: nodes=${summary.nodes}, routes=${summary.routes}, segments=${summary.segments}, badEndpointAngle=${summary.badEndpointAngle}, badBends=${summary.badBends}, interiorCrossings=${summary.interiorCrossings}, nodeOverlaps=${summary.nodeOverlaps}, laneClearance=${summary.laneClearance}, clearanceViolations=${summary.clearanceViolations}, layerContainmentViolations=${summary.layerContainmentViolations}, margins=${margins}, marginImbalance=${summary.marginImbalance}, titleGap=${summary.titleGap}${sequenceGap}`;
  console.log(summaryText);
  if (failures.length > 0) {
    throw new Error(`${diagram.file} failed geometry gates: ${failures.join(", ")}`);
  }

  writeFileSync(svgPath, renderSvg(diagram));
  execFileSync("rsvg-convert", ["--format", "png", "--output", pngPath, svgPath], { stdio: "inherit" });
}

function architectureSpring() {
  const layers = [
    { id: "entry", label: "Entry layer", x: 54, y: 150, w: 1312, h: 140 },
    { id: "application", label: "Application work layer", x: 54, y: 310, w: 1312, h: 140 },
    { id: "observability", label: "Observation layer", x: 54, y: 470, w: 1312, h: 160 },
    { id: "export", label: "Export layer", x: 54, y: 650, w: 1312, h: 160 },
  ];
  const nodes = [
    { id: "client", layer: "entry", title: "HTTP Client", bodyLines: ["POST order event", "GET scrape output"], x: 120, y: 178, w: 245, h: 88, fill: palette.cardBlue, stroke: palette.request },
    { id: "controller", layer: "entry", titleLines: ["Spring MVC", "Controller"], bodyLines: ["/orders/{orderId}/events", "X-Request-Id header"], x: 440, y: 170, w: 280, h: 112, fill: palette.cardTeal, stroke: palette.service },
    { id: "service", layer: "application", title: "OrderEventService", bodyLines: ["local publish + consume", "returns accepted JSON"], x: 570, y: 334, w: 320, h: 96, fill: palette.cardGreen, stroke: palette.event },
    { id: "springObs", layer: "observability", title: "observeSpring", bodyLines: ["orders.http.publish", "HTTP/service boundary"], x: 185, y: 500, w: 290, h: 100, fill: palette.cardTeal, stroke: palette.service },
    { id: "registry", layer: "observability", title: "ObservationRegistry", bodyLines: ["Micrometer handlers", "metrics + optional spans"], x: 545, y: 500, w: 310, h: 100, fill: palette.cardWhite, stroke: palette.neutral },
    { id: "eventObs", layer: "observability", titleLines: ["Event", "Telemetry"], bodyLines: ["event.publish", "event.consume"], x: 925, y: 494, w: 300, h: 112, fill: palette.cardGreen, stroke: palette.event },
    { id: "actuator", layer: "export", titleLines: ["Actuator", "Prometheus"], bodyLines: ["/actuator/prometheus", "Spring owns endpoint"], x: 770, y: 682, w: 285, h: 112, fill: palette.cardAmber, stroke: palette.metrics },
    { id: "otlp", layer: "export", titleLines: ["OTLP Collector", "optional"], bodyLines: ["enabled by Spring config", "not required for tests"], x: 1110, y: 682, w: 250, h: 112, fill: palette.cardPurple, stroke: palette.trace },
  ];
  const routes = [
    { id: "client-controller", from: "client", to: "controller", color: palette.request, marker: "arrowRequest", points: [{ x: 365, y: 222 }, { x: 440, y: 222 }] },
    { id: "controller-service", from: "controller", to: "service", color: palette.service, marker: "arrowService", points: [{ x: 580, y: 282 }, { x: 580, y: 304 }, { x: 730, y: 304 }, { x: 730, y: 334 }] },
    { id: "service-observe-spring", from: "service", to: "springObs", color: palette.service, marker: "arrowService", points: [{ x: 650, y: 430 }, { x: 650, y: 455 }, { x: 330, y: 455 }, { x: 330, y: 500 }] },
    { id: "service-registry", from: "service", to: "registry", color: palette.neutral, marker: "arrowNeutral", points: [{ x: 730, y: 430 }, { x: 730, y: 500 }] },
    { id: "service-event-telemetry", from: "service", to: "eventObs", color: palette.event, marker: "arrowEvent", points: [{ x: 810, y: 430 }, { x: 810, y: 455 }, { x: 1075, y: 455 }, { x: 1075, y: 494 }] },
    { id: "springObs-registry", from: "springObs", to: "registry", color: palette.service, marker: "arrowService", points: [{ x: 475, y: 550 }, { x: 545, y: 550 }] },
    { id: "eventObs-registry", from: "eventObs", to: "registry", color: palette.event, marker: "arrowEvent", points: [{ x: 925, y: 550 }, { x: 855, y: 550 }] },
    { id: "registry-actuator", from: "registry", to: "actuator", color: palette.metrics, marker: "arrowMetrics", points: [{ x: 700, y: 600 }, { x: 700, y: 650 }, { x: 912, y: 650 }, { x: 912, y: 682 }] },
    { id: "registry-otlp", from: "registry", to: "otlp", color: palette.trace, marker: "arrowTrace", dash: "8 7", points: [{ x: 790, y: 600 }, { x: 790, y: 650 }, { x: 1235, y: 650 }, { x: 1235, y: 682 }] },
  ];
  return shiftDiagramY({
    file: "examples-spring-boot-observability-spring-boot-demo-architecture-01",
    kind: "architecture",
    title: "Spring Boot Observability Demo Architecture",
    subtitle: "Layered request, application, observation, and export responsibilities.",
    width: 1420,
    height: 940,
    frame: { x: 32, y: 28, w: 1356, h: 864 },
    subtitleY: 116,
    bodyTop: 150,
    bodyBottom: 850,
    minTitleGap: 54,
    maxMarginImbalance: 90,
    layers,
    nodes,
    routes: routes.map(({ label, ...route }) => route),
  }, 20);
}

function architectureKtor() {
  const layers = [
    { id: "entry", label: "Entry layer", x: 54, y: 150, w: 1312, h: 140 },
    { id: "platform", label: "Ktor platform layer", x: 54, y: 310, w: 1312, h: 140 },
    { id: "application", label: "Application event layer", x: 54, y: 470, w: 1312, h: 160 },
    { id: "export", label: "Export layer", x: 54, y: 650, w: 1312, h: 160 },
  ];
  const nodes = [
    { id: "client", layer: "entry", title: "HTTP Client", bodyLines: ["POST order event", "GET /metrics"], x: 120, y: 178, w: 245, h: 88, fill: palette.cardBlue, stroke: palette.request },
    { id: "routes", layer: "entry", title: "Ktor Routes", bodyLines: ["/orders/{orderId}/events", "/metrics + /health"], x: 440, y: 170, w: 280, h: 104, fill: palette.cardTeal, stroke: palette.service },
    { id: "core", layer: "platform", titleLines: ["bluetape4k", "Ktor Core"], bodyLines: ["JSON + errors", "health baseline"], x: 260, y: 326, w: 260, h: 112, fill: palette.cardWhite, stroke: palette.neutral },
    { id: "observability", layer: "platform", titleLines: ["Ktor", "Observability"], bodyLines: ["correlation + logging", "metrics + optional tracing"], x: 610, y: 326, w: 320, h: 112, fill: palette.cardAmber, stroke: palette.metrics },
    { id: "service", layer: "application", titleLines: ["OrderEvent", "TelemetryService"], bodyLines: ["publish + consume", "uses ObservationRegistry"], x: 420, y: 494, w: 300, h: 112, fill: palette.cardGreen, stroke: palette.event },
    { id: "eventObs", layer: "application", titleLines: ["Event", "Telemetry"], bodyLines: ["event.publish", "event.consume"], x: 780, y: 494, w: 300, h: 112, fill: palette.cardGreen, stroke: palette.event },
    { id: "registry", layer: "export", titleLines: ["Prometheus", "MeterRegistry"], bodyLines: ["application-owned", "scraped by route"], x: 760, y: 682, w: 300, h: 112, fill: palette.cardAmber, stroke: palette.metrics },
    { id: "otel", layer: "export", titleLines: ["OpenTelemetry SDK", "optional"], bodyLines: ["server spans", "null disables tracing"], x: 1110, y: 682, w: 250, h: 112, fill: palette.cardPurple, stroke: palette.trace },
  ];
  const routes = [
    { id: "client-routes", from: "client", to: "routes", color: palette.request, marker: "arrowRequest", points: [{ x: 365, y: 222 }, { x: 440, y: 222 }] },
    { id: "routes-core", from: "routes", to: "core", color: palette.neutral, marker: "arrowNeutral", points: [{ x: 510, y: 274 }, { x: 510, y: 304 }, { x: 390, y: 304 }, { x: 390, y: 326 }] },
    { id: "routes-observability", from: "routes", to: "observability", color: palette.metrics, marker: "arrowMetrics", points: [{ x: 630, y: 274 }, { x: 630, y: 304 }, { x: 770, y: 304 }, { x: 770, y: 326 }] },
    { id: "routes-service", from: "routes", to: "service", color: palette.event, marker: "arrowEvent", points: [{ x: 560, y: 274 }, { x: 560, y: 494 }] },
    { id: "routes-registry", from: "routes", to: "registry", color: palette.metrics, marker: "arrowMetrics", points: [{ x: 720, y: 222 }, { x: 1100, y: 222 }, { x: 1100, y: 650 }, { x: 910, y: 650 }, { x: 910, y: 682 }] },
    { id: "observability-registry", from: "observability", to: "registry", color: palette.metrics, marker: "arrowMetrics", points: [{ x: 930, y: 382 }, { x: 1120, y: 382 }, { x: 1120, y: 650 }, { x: 910, y: 650 }, { x: 910, y: 682 }] },
    { id: "observability-otel", from: "observability", to: "otel", color: palette.trace, marker: "arrowTrace", dash: "8 7", points: [{ x: 930, y: 366 }, { x: 1235, y: 366 }, { x: 1235, y: 682 }] },
    { id: "service-eventObs", from: "service", to: "eventObs", color: palette.event, marker: "arrowEvent", points: [{ x: 720, y: 550 }, { x: 780, y: 550 }] },
    { id: "eventObs-registry", from: "eventObs", to: "registry", color: palette.event, marker: "arrowEvent", points: [{ x: 930, y: 606 }, { x: 930, y: 682 }] },
  ];
  return shiftDiagramY({
    file: "examples-ktor-observability-ktor-demo-architecture-01",
    kind: "architecture",
    title: "Ktor Observability Demo Architecture",
    subtitle: "Layered entry, platform, application event, and export responsibilities.",
    width: 1420,
    height: 940,
    frame: { x: 32, y: 28, w: 1356, h: 864 },
    subtitleY: 116,
    bodyTop: 150,
    bodyBottom: 850,
    minTitleGap: 54,
    maxMarginImbalance: 90,
    layers,
    nodes,
    routes: routes.map(({ label, ...route }) => route),
  }, 20);
}

function sequenceSpring() {
  const xs = [90, 340, 610, 880, 1145, 1365];
  const titles = [
    ["Client"],
    ["Spring MVC", "Controller"],
    ["observeSpring"],
    ["Event", "Telemetry"],
    ["Actuator", "Prometheus"],
    ["OTLP", "optional"],
  ];
  const colors = [palette.request, palette.service, palette.service, palette.event, palette.metrics, palette.trace];
  const fills = [palette.cardBlue, palette.cardTeal, palette.cardTeal, palette.cardGreen, palette.cardAmber, palette.cardPurple];
  const nodes = xs.map((x, index) => ({
    id: ["client", "controller", "springObs", "eventObs", "actuator", "otlp"][index],
    title: titles[index].join(" "),
    titleLines: titles[index],
    x,
    y: 165,
    w: index === 4 ? 175 : index === 5 ? 155 : 165,
    h: 74,
    lifelineBottom: 720,
    fill: fills[index],
    stroke: colors[index],
  }));
  for (const node of nodes) node.cx = node.x + node.w / 2;
  const byId = new Map(nodes.map((node) => [node.id, node]));
  const routes = [
    lineRoute({ id: "post-order", from: byId.get("client"), to: byId.get("controller"), y: 295, label: "POST /orders/{orderId}/events" }),
    lineRoute({ id: "observe-work", from: byId.get("controller"), to: byId.get("springObs"), y: 355, label: "observe HTTP service work", color: palette.service, marker: "arrowService" }),
    lineRoute({ id: "publish", from: byId.get("springObs"), to: byId.get("eventObs"), y: 415, label: "event.publish", color: palette.event, marker: "arrowEvent" }),
    lineRoute({ id: "consume", from: byId.get("springObs"), to: byId.get("eventObs"), y: 475, label: "event.consume", color: palette.event, marker: "arrowEvent" }),
    returnRoute({ id: "service-response", from: byId.get("springObs"), to: byId.get("controller"), y: 535, label: "OrderEventResponse" }),
    returnRoute({ id: "json-response", from: byId.get("controller"), to: byId.get("client"), y: 595, label: "200 JSON" }),
    lineRoute({ id: "scrape", from: byId.get("client"), to: byId.get("actuator"), y: 650, label: "GET /actuator/prometheus", color: palette.metrics, marker: "arrowMetrics" }),
    lineRoute({ id: "traces", from: byId.get("springObs"), to: byId.get("otlp"), y: 325, label: "traces when configured", color: palette.trace, marker: "arrowTrace", dash: "8 7" }),
  ];
  return {
    file: "examples-spring-boot-observability-spring-boot-demo-sequence-01",
    kind: "sequence",
    title: "Spring Boot Observability Demo Sequence",
    subtitle: "HTTP work, event observations, Prometheus scrape output, and optional trace export.",
    width: 1620,
    height: 820,
    frame: { x: 32, y: 28, w: 1556, h: 744 },
    subtitleY: 116,
    bodyTop: 145,
    bodyBottom: 730,
    minTitleGap: 49,
    maxMarginImbalance: 120,
    nodes,
    routes: routes.map((route) => ({ ...route, from: route.from.id, to: route.to.id })),
  };
}

function sequenceKtor() {
  const xs = [90, 350, 625, 900, 1140, 1360];
  const ids = ["client", "ktor", "observability", "eventObs", "prom", "otel"];
  const titles = [["Client"], ["Ktor", "Routes"], ["Ktor", "Observability"], ["Event", "Telemetry"], ["Prometheus", "Registry"], ["OpenTelemetry", "optional"]];
  const colors = [palette.request, palette.service, palette.metrics, palette.event, palette.metrics, palette.trace];
  const fills = [palette.cardBlue, palette.cardTeal, palette.cardAmber, palette.cardGreen, palette.cardAmber, palette.cardPurple];
  const nodes = xs.map((x, index) => ({
    id: ids[index],
    title: titles[index].join(" "),
    titleLines: titles[index],
    x,
    y: 165,
    w: index === 2 ? 180 : index === 4 || index === 5 ? 170 : 165,
    h: 74,
    lifelineBottom: 720,
    fill: fills[index],
    stroke: colors[index],
  }));
  for (const node of nodes) node.cx = node.x + node.w / 2;
  const byId = new Map(nodes.map((node) => [node.id, node]));
  const routes = [
    lineRoute({ id: "post-order", from: byId.get("client"), to: byId.get("ktor"), y: 295, label: "POST /orders/{orderId}/events" }),
    lineRoute({ id: "plugins", from: byId.get("ktor"), to: byId.get("observability"), y: 355, label: "correlation + logging + metrics", color: palette.metrics, marker: "arrowMetrics" }),
    lineRoute({ id: "publish", from: byId.get("ktor"), to: byId.get("eventObs"), y: 415, label: "observe event.publish", color: palette.event, marker: "arrowEvent" }),
    lineRoute({ id: "consume", from: byId.get("ktor"), to: byId.get("eventObs"), y: 475, label: "observe event.consume", color: palette.event, marker: "arrowEvent" }),
    lineRoute({ id: "timers", from: byId.get("eventObs"), to: byId.get("prom"), y: 535, label: "Micrometer timers", color: palette.metrics, marker: "arrowMetrics" }),
    lineRoute({ id: "spans", from: byId.get("observability"), to: byId.get("otel"), y: 325, label: "server spans when configured", color: palette.trace, marker: "arrowTrace", dash: "8 7" }),
    returnRoute({ id: "json-response", from: byId.get("ktor"), to: byId.get("client"), y: 595, label: "200 JSON" }),
    lineRoute({ id: "scrape-request", from: byId.get("client"), to: byId.get("ktor"), y: 650, label: "GET /metrics", color: palette.metrics, marker: "arrowMetrics" }),
    lineRoute({ id: "scrape-registry", from: byId.get("ktor"), to: byId.get("prom"), y: 625, label: "scrape()", color: palette.metrics, marker: "arrowMetrics" }),
  ];
  return {
    file: "examples-ktor-observability-ktor-demo-sequence-01",
    kind: "sequence",
    title: "Ktor Observability Demo Sequence",
    subtitle: "Application-owned metrics routing, event observations, and optional OpenTelemetry spans.",
    width: 1620,
    height: 820,
    frame: { x: 32, y: 28, w: 1556, h: 744 },
    subtitleY: 116,
    bodyTop: 145,
    bodyBottom: 730,
    minTitleGap: 49,
    maxMarginImbalance: 120,
    nodes,
    routes: routes.map((route) => ({ ...route, from: route.from.id, to: route.to.id })),
  };
}

for (const diagram of [architectureSpring(), sequenceSpring(), architectureKtor(), sequenceKtor()]) {
  generate(diagram);
}
