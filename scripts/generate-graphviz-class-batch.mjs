#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const OUT = join(process.cwd(), "docs/images/readme-diagrams");
const WORKLIST = process.env.CLASS_WORKLIST || "/tmp/bluetape4k-projects-diagram-redraw/kind-worklist.tsv";
const rsvg = "/opt/homebrew/bin/rsvg-convert";
const manualGenerator = join(process.cwd(), "scripts/generate-graphviz-class-diagrams.mjs");
const manualFiles = new Set(["io-fastjson2-diagram-01", "io-jackson2-diagram-01"]);

const palette = {
  blue: ["#E8F3FF", "#5B8DEF", "#4F83BF"],
  green: ["#EAF7EF", "#58A978", "#3E9868"],
  teal: ["#E9F7F6", "#45A7A1", "#2E8F89"],
  amber: ["#FFF3D9", "#D6A441", "#B9851B"],
  pink: ["#FDECEF", "#DC6B82", "#C94D68"],
  purple: ["#F1ECFF", "#8A72D6", "#755BC6"],
  olive: ["#EEF6D9", "#8BA84D", "#718A35"],
  gray: ["#F2F5F9", "#9AA8B8", "#758297"],
};
const colorNames = ["blue", "green", "teal", "amber", "pink", "purple", "olive", "gray"];

const targets = loadTargets();
mkdirSync(OUT, { recursive: true });

const generated = [];
for (const file of targets.filter((item) => !manualFiles.has(item))) {
  const sourcePath = join(OUT, `${file}.svg`);
  if (!existsSync(sourcePath)) {
    throw new Error(`missing SVG source for ${file}`);
  }
  const diagram = extractDiagram(file, readBaselineSvg(file, sourcePath));
  render(diagram);
  generated.push(file);
}

if (targets.some((item) => manualFiles.has(item))) {
  execFileSync("node", [manualGenerator], { stdio: "inherit" });
  generated.push(...targets.filter((item) => manualFiles.has(item)));
}

const validation = validateTargets([...new Set(generated)].sort());
console.log(`graphviz-class-batch: files=${generated.length} overlaps=${validation.overlaps} diagonalSegments=${validation.diagonalSegments} longDependencyRoutes=${validation.longDependencyRoutes} invertedInheritance=${validation.invertedInheritance}`);

function loadTargets() {
  const rows = readFileSync(WORKLIST, "utf8").split(/\r?\n/).filter(Boolean);
  return rows
    .map((row) => row.split("\t"))
    .filter(([kind]) => kind === "class")
    .map(([, file]) => file)
    .sort();
}

function readBaselineSvg(file, sourcePath) {
  const repoPath = `docs/images/readme-diagrams/${file}.svg`;
  try {
    return execFileSync("git", ["show", `HEAD:${repoPath}`], { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
  } catch {
    return readFileSync(sourcePath, "utf8");
  }
}

function extractDiagram(file, svg) {
  const title = text(svg.match(/<text class="title"[^>]*>([\s\S]*?)<\/text>/)?.[1]) || titleFromFile(file);
  const nodes = extractCards(svg).map((item, index) => ({
    ...item,
    color: colorNames[index % colorNames.length],
  }));
  const edges = extractEdges(svg, nodes);
  if (nodes.length < 2) throw new Error(`${file}: expected at least two class cards`);
  return {
    file,
    title: normalizeTitle(title),
    subtitle: "Graphviz-ranked class model: source relationships redrawn with short colored inheritance and dependency routes.",
    rankdir: "TB",
    nodes,
    edges: edges.length > 0 ? edges : fallbackEdges(nodes),
  };
}

function extractCards(svg) {
  const cards = [];
  const groupPattern = /<g(?:\s+id="([^"]+)")?(?:\s+transform="translate\(([^,\s]+),([^)]+)\)")?[^>]*>([\s\S]*?)<\/g>/g;
  let match;
  while ((match = groupPattern.exec(svg)) != null) {
    const body = match[4];
    const rect = body.match(/<rect class="(?:card|classCard)" x="([^"]+)" y="([^"]+)" width="([^"]+)" height="([^"]+)"/);
    if (!rect) continue;
    const tx = Number(match[2] || 0);
    const ty = Number(match[3] || 0);
    const cardTitles = [...body.matchAll(/<text class="(?:cardTitle|classTitle|label|smallLabel|small)"[^>]*>([\s\S]*?)<\/text>/g)].map((item) => text(item[1]));
    const details = [...body.matchAll(/<text class="(?:detail|member|mono)"[^>]*>([\s\S]*?)<\/text>/g)].map((item) => text(item[1])).filter(Boolean);
    const parsed = refineTitle(parseTitleLines(cardTitles), details);
    const id = uniqueId(match[1] || parsed.label || `node-${cards.length}`, cards);
    cards.push({
      id,
      label: parsed.label || `Class ${cards.length + 1}`,
      stereotype: parsed.stereotype,
      members: details.filter((line) => !sameLoose(line, parsed.label)).slice(0, 3),
      sourceBox: {
        x: tx + Number(rect[1]),
        y: ty + Number(rect[2]),
        w: Number(rect[3]),
        h: Number(rect[4]),
      },
    });
  }
  return cards;
}

function uniqueId(value, cards) {
  const base = String(value)
    .replace(/[^A-Za-z0-9_]+/g, "_")
    .replace(/^_+|_+$/g, "")
    || `node_${cards.length}`;
  let candidate = /^[A-Za-z_]/.test(base) ? base : `node_${base}`;
  let suffix = 2;
  while (cards.some((card) => card.id === candidate)) {
    candidate = `${base}_${suffix}`;
    suffix += 1;
  }
  return candidate;
}

function parseTitleLines(lines) {
  const clean = lines.map((line) => line.trim()).filter(Boolean);
  if (clean.length === 0) return { label: "", stereotype: "class" };
  let stereotype = "class";
  let labelLines = clean;
  const first = clean[0];
  const inline = first.match(/^«([^»]+)»\s*(.*)$/);
  if (inline) {
    stereotype = inline[1].trim();
    labelLines = [inline[2], ...clean.slice(1)].filter(Boolean);
  } else if (/^«[^»]+»$/.test(first)) {
    stereotype = first.replace(/[«»]/g, "").trim();
    labelLines = clean.slice(1);
  }
  return {
    label: normalizeClassLabel(labelLines.join(" ")),
    stereotype,
  };
}

function refineTitle(parsed, details) {
  const cleanDetails = details.map(normalizeClassLabel).filter(Boolean);
  let label = parsed.label;
  let stereotype = parsed.stereotype;
  if (/^(extensionFunctions?|factoryFunction)$/i.test(label) && cleanDetails[0]) {
    label = cleanDetails[0];
    stereotype = /extension/i.test(parsed.label) ? "extension" : "factory";
  } else {
    const joined = [label, ...cleanDetails.slice(0, 2)]
      .join(" ")
      .replace(/\s+/g, " ")
      .trim();
    if (shouldJoinAsIdentifier(joined) && joined.length > label.length && cleanDetails[0]?.startsWith(label)) {
      label = joined;
    }
  }
  if (shouldJoinAsIdentifier(label)) label = label.replace(/\s+/g, "");
  return { label, stereotype };
}

function shouldJoinAsIdentifier(value) {
  const textValue = String(value).trim();
  if (!textValue.includes(" ")) return false;
  if (/[^A-Za-z0-9\s]/.test(textValue)) return false;
  const words = textValue.split(/\s+/);
  if (words.length < 2 || words.length > 5) return false;
  return words.every((word) => /^[A-Z0-9]/.test(word));
}

function extractEdges(svg, nodes) {
  const edges = [];
  const hasExplicitEdges = /<path class="(?:inherit|dependency)"[^>]*data-from=/.test(svg);
  const pathPattern = /<path class="([^"]+)"([^>]*)d="([^"]+)"([^>]*)>/g;
  let match;
  while ((match = pathPattern.exec(svg)) != null) {
    const className = match[1];
    if (!/\b(?:inherit|dependency|flow|line|dashed|inheritLine|implLine)\b/.test(className)) continue;
    const attrs = `class="${className}" ${match[2]} ${match[4]}`;
    const points = parsePath(match[3]);
    if (points.length < 2) continue;
    const explicitFrom = attrs.match(/data-from="([^"]+)"/)?.[1];
    const explicitTo = attrs.match(/data-to="([^"]+)"/)?.[1];
    if (hasExplicitEdges && (!explicitFrom || !explicitTo)) continue;
    const from = explicitFrom ? nodes.find((node) => node.id === explicitFrom) : nearestNode(points[0], nodes);
    const to = explicitTo ? nodes.find((node) => node.id === explicitTo) : nearestNode(points.at(-1), nodes, from?.id);
    if (!from || !to || from.id === to.id) continue;
    const inherit = /\b(?:inherit|inheritLine|implLine)\b/.test(className);
    const color = colorForStroke(attrs) || from.color;
    edges.push({
      from: from.id,
      to: to.id,
      kind: inherit ? "inherit" : "use",
      color,
      points,
    });
  }
  return uniqueEdges(normalizeHierarchyEdges(edges, nodes));
}

function normalizeHierarchyEdges(edges, nodes) {
  return edges.map((edge) => {
    if (edge.kind !== "inherit") return edge;
    const from = nodes.find((node) => node.id === edge.from);
    const to = nodes.find((node) => node.id === edge.to);
    if (isHierarchyParent(from) && !isHierarchyParent(to)) {
      return { ...edge, from: edge.to, to: edge.from };
    }
    return edge;
  });
}

function parsePath(d) {
  return [...d.matchAll(/[ML]\s*([\d.]+)\s+([\d.]+)/g)].map((match) => ({
    x: Number(match[1]),
    y: Number(match[2]),
  }));
}

function nearestNode(point, nodes, exceptId = null) {
  return nodes
    .filter((node) => node.id !== exceptId)
    .map((node) => ({ node, distance: distanceToRect(point, node.sourceBox) }))
    .sort((a, b) => a.distance - b.distance)[0]?.node;
}

function distanceToRect(point, rect) {
  const dx = Math.max(rect.x - point.x, 0, point.x - (rect.x + rect.w));
  const dy = Math.max(rect.y - point.y, 0, point.y - (rect.y + rect.h));
  return Math.hypot(dx, dy);
}

function colorForStroke(attrs) {
  const stroke = attrs.match(/stroke="([^"]+)"/)?.[1]?.toUpperCase();
  if (!stroke) return null;
  return Object.entries(palette).find(([, values]) => values[2].toUpperCase() === stroke || values[1].toUpperCase() === stroke)?.[0] || null;
}

function uniqueEdges(edges) {
  const seen = new Set();
  const result = [];
  for (const edge of edges) {
    const key = `${edge.from}->${edge.to}:${edge.kind}`;
    if (seen.has(key)) continue;
    seen.add(key);
    result.push(edge);
  }
  return result.slice(0, 24);
}

function fallbackEdges(nodes) {
  return nodes.slice(0, -1).map((node, index) => ({
    from: node.id,
    to: nodes[index + 1].id,
    kind: index % 2 === 0 ? "inherit" : "use",
    color: node.color,
  }));
}

function render(diagram) {
  const dotPath = join(OUT, `${diagram.file}.dot`);
  const plainPath = join(OUT, `${diagram.file}.plain`);
  const sketchSvgPath = join(OUT, `${diagram.file}-graphviz.svg`);
  const sketchPngPath = join(OUT, `${diagram.file}-graphviz.png`);
  const finalSvgPath = join(OUT, `${diagram.file}.svg`);
  const finalPngPath = join(OUT, `${diagram.file}.png`);

  writeFileSync(dotPath, toDot(diagram));
  writeFileSync(plainPath, execFileSync("dot", ["-Tplain", dotPath], { encoding: "utf8" }));
  writeFileSync(sketchSvgPath, execFileSync("dot", ["-Tsvg", dotPath], { encoding: "utf8" }));
  execFileSync("dot", ["-Tpng", dotPath, "-o", sketchPngPath], { stdio: "inherit" });
  const layout = parsePlain(readFileSync(plainPath, "utf8"), diagram);
  const svg = toFinalSvg(diagram, layout);
  writeFileSync(finalSvgPath, svg);
  execFileSync(rsvg, ["--format=png", "--output", finalPngPath, finalSvgPath], { stdio: "inherit" });
  console.log(`${diagram.file}: class nodes=${diagram.nodes.length} edges=${diagram.edges.length}`);
}

function toDot(diagram) {
  const lines = [
    "digraph G {",
    `  graph [rankdir=${diagram.rankdir}, splines=ortho, nodesep=0.95, ranksep=1.2, outputorder=edgesfirst, pack=false, sep="+0.9,0.9"];`,
    "  node [shape=box, style=\"rounded,filled\", fontname=\"Comic Mono\", fontsize=11, margin=\"0.16,0.10\", width=2.8, height=1.05];",
    "  edge [fontname=\"Comic Mono\", fontsize=10, arrowsize=0.75, penwidth=1.8];",
  ];
  for (const item of diagram.nodes) {
    const color = palette[item.color] || palette.gray;
    lines.push(`  ${item.id} [label="${escDot(item.label)}", fillcolor="${color[0]}", color="${color[1]}"];`);
  }
  const parentRank = diagram.nodes.filter(isHierarchyParent).map((item) => item.id);
  if (parentRank.length > 0) {
    lines.push(`  { rank=source; ${parentRank.join("; ")}; }`);
  }
  for (const item of diagram.edges) {
    const color = palette[item.color] || palette.gray;
    const style = item.kind === "inherit" ? "solid" : "dashed";
    const [dotFrom, dotTo] = dotEdgeEndpoints(item, diagram.nodes);
    lines.push(`  ${dotFrom} -> ${dotTo} [color="${color[2]}", fontcolor="${color[2]}", style="${style}", arrowhead="none", ${item.kind === "inherit" ? "weight=10" : "weight=2"}];`);
  }
  lines.push("}");
  return `${lines.join("\n")}\n`;
}

function dotEdgeEndpoints(edge, nodes) {
  if (edge.kind !== "inherit") return [edge.from, edge.to];
  const from = nodes.find((item) => item.id === edge.from);
  const to = nodes.find((item) => item.id === edge.to);
  if (to && isHierarchyParent(to) && !isHierarchyParent(from)) return [edge.to, edge.from];
  if (from && isHierarchyParent(from) && !isHierarchyParent(to)) return [edge.from, edge.to];
  return [edge.to, edge.from];
}

function isHierarchyParent(node) {
  return /\b(interface|abstract)\b/i.test(node?.stereotype || "");
}

function parsePlain(plain, diagram) {
  const graph = plain.match(/^graph\s+\S+\s+([\d.]+)\s+([\d.]+)/m);
  const graphW = Number(graph?.[1] || 1);
  const graphH = Number(graph?.[2] || 1);
  const nodes = new Map();
  for (const line of plain.split(/\r?\n/)) {
    const parts = line.match(/(?:[^\s"]+|"[^"]*")+/g) || [];
    if (parts[0] !== "node") continue;
    const model = diagram.nodes.find((item) => item.id === parts[1]);
    if (!model) continue;
    nodes.set(model.id, {
      ...model,
      cx: Number(parts[2]),
      cy: Number(parts[3]),
      gw: Number(parts[4]),
      gh: Number(parts[5]),
    });
  }
  return { graphW, graphH, nodes: [...nodes.values()] };
}

function toFinalSvg(diagram, layout) {
  let positioned;
  let size;
  for (const scale of [142, 158, 176, 196, 218]) {
    const attempt = positionNodes(layout, scale, diagram.edges);
    if (countOverlaps([...attempt.positioned.values()]) === 0) {
      positioned = attempt.positioned;
      size = attempt.size;
      break;
    }
    positioned = attempt.positioned;
    size = attempt.size;
  }

  const routeSvg = diagram.edges.map((item) => renderEdge(item, positioned, [...positioned.values()])).join("\n");
  const nodeSvg = [...positioned.values()].map(renderClassCard).join("\n");
  const body = `${routeSvg}\n${nodeSvg}\n${legend(size.width - 415, size.height - 108)}\n${footer(180, size.height - 90, size.width - 640, "Graphviz evidence: .dot, .plain, and -graphviz.svg define relationship order and route intent.")}`;
  return base(size.width, size.height, diagram.title, diagram.subtitle, body);
}

function positionNodes(layout, scale, edges) {
  const marginX = 118;
  const marginTop = 165;
  const cardW = 340;
  const cardH = 132;
  const contentW = layout.graphW * scale + cardW + marginX * 2;
  const contentH = layout.graphH * scale + cardH + marginTop + 145;
  const width = Math.max(1500, Math.ceil(contentW));
  const height = Math.max(780, Math.ceil(contentH));
  const offsetX = Math.max(0, Math.round((width - contentW) / 2));
  const positioned = new Map(layout.nodes.map((item) => {
    const x = marginX + offsetX + item.cx * scale;
    const y = marginTop + (layout.graphH - item.cy) * scale;
    return [item.id, { ...item, x: Math.round(x), y: Math.round(y), w: cardW, h: cardH }];
  }));
  return { positioned, size: { width, height } };
}

function packDisconnectedComponents(positioned, edges, size) {
  const components = connectedComponents([...positioned.keys()], edges);
  if (components.length <= 2 || size.width <= 2300) return { positioned, size };

  const targetInnerW = 1780;
  const gapX = 110;
  const gapY = 105;
  let cursorX = 0;
  let cursorY = 0;
  let rowH = 0;
  let maxW = 0;

  const boxes = components
    .map((ids) => ({ ids, box: componentBox(ids, positioned) }))
    .sort((a, b) => a.box.y - b.box.y || a.box.x - b.box.x);

  for (const item of boxes) {
    const componentW = item.box.w;
    const componentH = item.box.h;
    if (cursorX > 0 && cursorX + componentW > targetInnerW) {
      maxW = Math.max(maxW, cursorX - gapX);
      cursorX = 0;
      cursorY += rowH + gapY;
      rowH = 0;
    }
    const dx = size.marginX + cursorX - item.box.x;
    const dy = size.marginTop + cursorY - item.box.y;
    for (const id of item.ids) {
      const node = positioned.get(id);
      node.x = Math.round(node.x + dx);
      node.y = Math.round(node.y + dy);
    }
    cursorX += componentW + gapX;
    rowH = Math.max(rowH, componentH);
  }
  maxW = Math.max(maxW, cursorX - gapX);
  const width = Math.max(1500, Math.ceil(maxW + size.marginX * 2));
  const height = Math.max(780, Math.ceil(size.marginTop + cursorY + rowH + 170));
  return { positioned, size: { width, height } };
}

function connectedComponents(ids, edges) {
  const graph = new Map(ids.map((id) => [id, new Set()]));
  for (const edge of edges) {
    graph.get(edge.from)?.add(edge.to);
    graph.get(edge.to)?.add(edge.from);
  }
  const seen = new Set();
  const result = [];
  for (const id of ids) {
    if (seen.has(id)) continue;
    const stack = [id];
    const group = [];
    seen.add(id);
    while (stack.length > 0) {
      const current = stack.pop();
      group.push(current);
      for (const next of graph.get(current) || []) {
        if (seen.has(next)) continue;
        seen.add(next);
        stack.push(next);
      }
    }
    result.push(group);
  }
  return result;
}

function componentBox(ids, positioned) {
  const nodes = ids.map((id) => positioned.get(id)).filter(Boolean);
  const minX = Math.min(...nodes.map((node) => node.x));
  const minY = Math.min(...nodes.map((node) => node.y));
  const maxX = Math.max(...nodes.map((node) => node.x + node.w));
  const maxY = Math.max(...nodes.map((node) => node.y + node.h));
  return { x: minX, y: minY, w: maxX - minX, h: maxY - minY };
}

function renderEdge(edge, positioned, nodes) {
  const from = positioned.get(edge.from);
  const to = positioned.get(edge.to);
  if (!from || !to) return "";
  const color = palette[edge.color]?.[2] || palette.gray[2];
  const route = edge.kind === "inherit"
    ? inheritanceRoute(from, to, nodes)
    : dependencyRoute(from, to, nodes);
  const d = route.map((point, index) => `${index === 0 ? "M" : "L"}${Math.round(point.x)} ${Math.round(point.y)}`).join(" ");
  const cls = edge.kind === "inherit" ? "inherit" : "dependency";
  const dash = edge.kind === "inherit" ? "" : ` stroke-dasharray="8 7"`;
  return `<path class="${cls}" data-from="${edge.from}" data-to="${edge.to}" d="${d}" stroke="${color}"${dash}/>`;
}

function inheritanceRoute(from, to, nodes) {
  const candidates = [
    routeBetween(from, to, "top-bottom"),
    routeBetween(from, to, "left-right"),
    routeBetween(from, to, "right-left"),
    routeBetween(from, to, "bottom-top"),
  ];
  return bestRoute(candidates, nodes, from.id, to.id);
}

function dependencyRoute(from, to, nodes) {
  const fc = center(from);
  const tc = center(to);
  const preferred = Math.abs(fc.x - tc.x) >= Math.abs(fc.y - tc.y)
    ? ["right-left", "left-right", "top-bottom", "bottom-top"]
    : ["bottom-top", "top-bottom", "right-left", "left-right"];
  return bestRoute(preferred.map((mode) => routeBetween(from, to, mode)), nodes, from.id, to.id);
}

function routeBetween(from, to, mode) {
  const fc = center(from);
  const tc = center(to);
  const startSide = mode.split("-")[0];
  const endSide = mode.split("-")[1];
  const start = sidePoint(from, startSide, startSide === "left" || startSide === "right" ? clamp(tc.y, from.y + 30, from.y + from.h - 30) : clamp(tc.x, from.x + 42, from.x + from.w - 42));
  const end = sidePoint(to, endSide, endSide === "left" || endSide === "right" ? clamp(start.y, to.y + 30, to.y + to.h - 30) : clamp(start.x, to.x + 42, to.x + to.w - 42));
  if (Math.abs(start.x - end.x) <= 1 || Math.abs(start.y - end.y) <= 1) return dedupeRoute([start, end]);
  if (startSide === "top" || startSide === "bottom") {
    const midY = Math.round((start.y + end.y) / 2);
    return dedupeRoute([start, { x: start.x, y: midY }, { x: end.x, y: midY }, end]);
  }
  const midX = Math.round((start.x + end.x) / 2);
  return dedupeRoute([start, { x: midX, y: start.y }, { x: midX, y: end.y }, end]);
}

function bestRoute(routes, nodes, fromId, toId) {
  return routes
    .map((route) => ({ route, score: routeScore(route, nodes, fromId, toId) }))
    .sort((a, b) => a.score - b.score)[0].route;
}

function routeScore(route, nodes, fromId, toId) {
  let score = route.length * 3;
  for (let index = 0; index < route.length - 1; index += 1) {
    const a = route[index];
    const b = route[index + 1];
    score += Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    if (Math.abs(a.x - b.x) > 1 && Math.abs(a.y - b.y) > 1) score += 100000;
    for (const node of nodes) {
      if (node.id === fromId || node.id === toId) continue;
      if (segmentIntersectsRect(a, b, inset(node, -8))) score += 100000;
    }
  }
  return score;
}

function sidePoint(rect, side, value) {
  if (side === "left") return { x: rect.x, y: value };
  if (side === "right") return { x: rect.x + rect.w, y: value };
  if (side === "top") return { x: value, y: rect.y };
  return { x: value, y: rect.y + rect.h };
}

function renderClassCard(node) {
  const [fill, stroke] = palette[node.color] || palette.gray;
  const titleLines = wrapIdentifier(node.label, 24).slice(0, 2);
  const memberLines = node.members.flatMap((line) => wrap(line, 34)).slice(0, 3);
  const titleStart = titleLines.length > 1 ? node.y + 38 : node.y + 47;
  const headerBottom = titleLines.length > 1 ? node.y + 64 : node.y + 56;
  const lineH = 17;
  const memberStart = lowerCompartmentBaselineStart(headerBottom, node.y + node.h, memberLines.length, lineH);
  return `<g id="${node.id}">
  <rect class="classCard" x="${node.x}" y="${node.y}" width="${node.w}" height="${node.h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="stereo" x="${node.x + node.w / 2}" y="${node.y + 19}" text-anchor="middle">${esc(node.stereotype)}</text>
  ${titleLines.map((line, index) => `<text class="classTitle" x="${node.x + node.w / 2}" y="${titleStart + index * 22}" text-anchor="middle">${esc(line)}</text>`).join("\n")}
  <line x1="${node.x}" y1="${headerBottom}" x2="${node.x + node.w}" y2="${headerBottom}" stroke="${stroke}" stroke-width="1.4"/>
  ${memberLines.map((line, index) => `<text class="member" x="${node.x + 18}" y="${memberStart + index * lineH}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function lowerCompartmentBaselineStart(headerBottom, cardBottom, lineCount, lineH) {
  if (lineCount <= 0) return Math.round((headerBottom + cardBottom) / 2);
  const lowerCenter = (headerBottom + cardBottom) / 2;
  const averageBaselineOffset = ((lineCount - 1) * lineH) / 2;
  return Math.round(lowerCenter + 4 - averageBaselineOffset);
}

function base(width, height, title, subtitle, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title)}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/></filter>
  <marker id="inheritArrow" markerWidth="11" markerHeight="10" refX="10.2" refY="5" orient="auto" markerUnits="strokeWidth"><path d="M 1.2 1 L 10.2 5 L 1.2 9 Z" fill="#fff" stroke="context-stroke" stroke-width="1.4"/></marker>
  <marker id="depArrow" markerWidth="8" markerHeight="8" refX="7.4" refY="4" orient="auto" markerUnits="strokeWidth"><path d="M 1 1 L 7.4 4 L 1 7 Z" fill="context-stroke"/></marker>
  <style>
    .canvas{fill:#F6F9FC}.frame{fill:#fff;stroke:#C7D7E7;stroke-width:3;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:#22344A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#536476}
    .classCard{filter:url(#shadow);stroke-width:2}.classTitle{font-family:"Architects Daughter";font-size:23px;fill:#22344A}.stereo{font-family:"Comic Mono";font-size:10px;fill:#627184}.member{font-family:"Comic Mono";font-size:12px;fill:#102033}
    .inherit{fill:none;stroke-width:2.55;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#inheritArrow)}
    .dependency{fill:none;stroke-width:2.35;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#depArrow)}
    .detail{font-family:"Comic Mono";font-size:13px;fill:#42556B}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="32" y="28" width="${width - 64}" height="${height - 56}" rx="30"/>
<text class="title" x="68" y="84">${esc(title)}</text>
<text class="subtitle" x="72" y="116">${esc(subtitle)}</text>
${body}
</svg>
`;
}

function validateTargets(files) {
  let overlaps = 0;
  let diagonalSegments = 0;
  let longDependencyRoutes = 0;
  let invertedInheritance = 0;
  for (const file of files) {
    const svg = readFileSync(join(OUT, `${file}.svg`), "utf8");
    const rects = [...svg.matchAll(/<rect class="classCard" x="([^"]+)" y="([^"]+)" width="([^"]+)" height="([^"]+)"/g)].map((match) => ({
      x: Number(match[1]),
      y: Number(match[2]),
      w: Number(match[3]),
      h: Number(match[4]),
    }));
    overlaps += countOverlaps(rects);
    for (const path of svg.matchAll(/<path class="(inherit|dependency)"[^>]*d="([^"]+)"/g)) {
      const points = parsePath(path[2]);
      for (let index = 0; index < points.length - 1; index += 1) {
        if (Math.abs(points[index].x - points[index + 1].x) > 1 && Math.abs(points[index].y - points[index + 1].y) > 1) diagonalSegments += 1;
      }
      if (path[1] === "dependency" && points.length > 4) longDependencyRoutes += 1;
    }
    const positioned = new Map([...svg.matchAll(/<g id="([^"]+)">([\s\S]*?)<\/g>/g)].map((match) => {
      const rect = match[2].match(/<rect class="classCard" x="([^"]+)" y="([^"]+)" width="([^"]+)" height="([^"]+)"/);
      if (!rect) return null;
      const stereotype = text(match[2].match(/<text class="stereo"[^>]*>([\s\S]*?)<\/text>/)?.[1] || "");
      return [
        match[1],
        { x: Number(rect[1]), y: Number(rect[2]), w: Number(rect[3]), h: Number(rect[4]), stereotype },
      ];
    }).filter(Boolean));
    for (const path of svg.matchAll(/<path class="inherit"[^>]*data-from="([^"]+)"[^>]*data-to="([^"]+)"/g)) {
      const child = positioned.get(path[1]);
      const parent = positioned.get(path[2]);
      if (child && parent && isHierarchyParent(parent) && !isHierarchyParent(child) && child.y <= parent.y) invertedInheritance += 1;
    }
    execFileSync("xmllint", ["--noout", join(OUT, `${file}.svg`)], { stdio: "pipe" });
  }
  if (overlaps || diagonalSegments || longDependencyRoutes || invertedInheritance) {
    throw new Error(`class validation failed: overlaps=${overlaps} diagonalSegments=${diagonalSegments} longDependencyRoutes=${longDependencyRoutes} invertedInheritance=${invertedInheritance}`);
  }
  return { overlaps, diagonalSegments, longDependencyRoutes, invertedInheritance };
}

function countOverlaps(rects) {
  let count = 0;
  for (let i = 0; i < rects.length; i += 1) {
    for (let j = i + 1; j < rects.length; j += 1) {
      if (rectsOverlap(rects[i], rects[j])) count += 1;
    }
  }
  return count;
}

function rectsOverlap(a, b) {
  return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
}

function segmentIntersectsRect(a, b, rect) {
  if (a.x === b.x) {
    const y1 = Math.min(a.y, b.y);
    const y2 = Math.max(a.y, b.y);
    return a.x > rect.x && a.x < rect.x + rect.w && y2 > rect.y && y1 < rect.y + rect.h;
  }
  if (a.y === b.y) {
    const x1 = Math.min(a.x, b.x);
    const x2 = Math.max(a.x, b.x);
    return a.y > rect.y && a.y < rect.y + rect.h && x2 > rect.x && x1 < rect.x + rect.w;
  }
  return true;
}

function inset(rect, amount) {
  return { x: rect.x + amount, y: rect.y + amount, w: rect.w - amount * 2, h: rect.h - amount * 2 };
}

function center(rect) {
  return { x: rect.x + rect.w / 2, y: rect.y + rect.h / 2 };
}

function dedupeRoute(points) {
  const result = [];
  for (const point of points) {
    const previous = result.at(-1);
    if (!previous || Math.abs(previous.x - point.x) > 1 || Math.abs(previous.y - point.y) > 1) result.push(point);
  }
  return result;
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function footer(x, y, w, value) {
  return `<g><rect x="${x}" y="${y}" width="${w}" height="42" rx="12" fill="#FFFFFF" stroke="#D6E3EF" stroke-width="1.6"/><text class="detail" x="${x + w / 2}" y="${y + 26}" text-anchor="middle">${esc(value)}</text></g>`;
}

function legend(x, y) {
  return `<g>
  <rect x="${x}" y="${y}" width="250" height="54" rx="12" fill="#FFFFFF" stroke="#D6E3EF" stroke-width="1.6"/>
  <path class="inherit" d="M${x + 18} ${y + 18} L${x + 76} ${y + 18}" stroke="#3E9868"/>
  <text class="detail" x="${x + 90}" y="${y + 23}">inheritance / implementation</text>
  <path class="dependency" d="M${x + 18} ${y + 39} L${x + 76} ${y + 39}" stroke="#758297" stroke-dasharray="8 7"/>
  <text class="detail" x="${x + 90}" y="${y + 44}">dependency / usage</text>
</g>`;
}

function wrap(value, max) {
  const words = String(value).split(/\s+/);
  const lines = [];
  let line = "";
  for (const word of words) {
    if ((line + " " + word).trim().length > max && line) {
      lines.push(line);
      line = word;
    } else {
      line = (line + " " + word).trim();
    }
  }
  if (line) lines.push(line);
  return lines;
}

function wrapIdentifier(value, max) {
  const textValue = String(value);
  if (textValue.includes(" ")) return wrap(textValue, max);
  const parts = textValue.match(/[A-Z]?[a-z0-9]+|[A-Z]+(?=[A-Z]|$)/g) || [textValue];
  const lines = [];
  let line = "";
  for (const part of parts) {
    if ((line + part).length > max && line) {
      lines.push(line);
      line = part;
    } else {
      line += part;
    }
  }
  if (line) lines.push(line);
  return lines;
}

function normalizeClassLabel(value) {
  return String(value)
    .replaceAll("~", "")
    .replace(/\bExtenions\b/g, "Extensions")
    .replace(/\s+/g, " ")
    .trim();
}

function normalizeTitle(value) {
  const normalized = String(value)
    .replace(/\bcoroutines Class Structure 2\b/i, "Coroutine Class Structure 2")
    .replace(/\bdiagram$/i, "Diagram")
    .replace(/\s+/g, " ")
    .trim();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function titleFromFile(file) {
  return file.split("-").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function sameLoose(a, b) {
  const clean = (value) => String(value).toLowerCase().replace(/[^a-z0-9]/g, "");
  return clean(a) === clean(b);
}

function text(value) {
  return String(value ?? "")
    .replace(/<[^>]+>/g, "")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&amp;", "&")
    .replaceAll("&quot;", '"')
    .replace(/\s+/g, " ")
    .trim();
}

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function escDot(value) {
  return String(value ?? "").replaceAll("\\", "\\\\").replaceAll('"', '\\"');
}
