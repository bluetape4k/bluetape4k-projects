#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const OUT = join(process.cwd(), "docs/images/readme-diagrams");
const rsvg = "/opt/homebrew/bin/rsvg-convert";

const palette = {
  blue: ["#E8F3FF", "#5B8DEF", "#4F83BF"],
  green: ["#EAF7EF", "#58A978", "#3E9868"],
  teal: ["#E9F7F6", "#45A7A1", "#2E8F89"],
  amber: ["#FFF3D9", "#D6A441", "#B9851B"],
  pink: ["#FDECEF", "#DC6B82", "#C94D68"],
  purple: ["#F1ECFF", "#8A72D6", "#755BC6"],
  gray: ["#F2F5F9", "#9AA8B8", "#758297"],
};

const diagrams = [
  {
    file: "io-fastjson2-diagram-01",
    title: "Fastjson2 Class Relationships",
    subtitle: "Graphviz-ranked class model: serializer contract, JSON/JSONB runtime APIs, and JSONObject extensions.",
    rankdir: "TB",
    nodes: [
      node("jsonSerializer", "JsonSerializer", "interface", ["+serialize(graph)", "+deserialize(bytes, clazz)"], "blue"),
      node("fastjsonSerializer", "FastjsonSerializer", "class", ["implements JsonSerializer", "JSONB bytes + JSON text"], "green"),
      node("jsonb", "JSONB", "external object", ["+toBytes(graph)", "+parseObject(bytes, type)"], "amber"),
      node("json", "JSON", "external object", ["+parseObject(text, type)"], "purple"),
      node("jsonObjectExtensions", "JSONObjectExtensions", "extension file", ["JSONObject.readValueOrNull()", "JSONObject.readValueOrNull(key)"], "teal"),
      node("jsonObject", "JSONObject", "external class", ["extension receiver"], "gray"),
      node("jsonReaderFeature", "JSONReader.Feature", "external enum", ["vararg feature controls"], "pink"),
    ],
    edges: [
      edge("fastjsonSerializer", "jsonSerializer", "implements", "inherit", "green", { dotFrom: "jsonSerializer", dotTo: "fastjsonSerializer", reverseRoute: true }),
      edge("fastjsonSerializer", "jsonb", "bytes", "use", "amber"),
      edge("fastjsonSerializer", "json", "text", "use", "purple"),
      edge("jsonObjectExtensions", "jsonObject", "receiver", "use", "teal"),
      edge("jsonObjectExtensions", "jsonReaderFeature", "features", "use", "pink"),
    ],
    ranks: [
      ["jsonSerializer", "jsonObject"],
      ["fastjsonSerializer", "jsonObjectExtensions", "jsonReaderFeature"],
      ["jsonb", "json"],
    ],
  },
  {
    file: "io-jackson2-diagram-01",
    title: "Jackson2 Class Relationships",
    subtitle: "Graphviz-ranked class model: contracts first, core mapper support second, format adapters at the edge.",
    rankdir: "TB",
    nodes: [
      node("jsonSerializer", "JsonSerializer", "interface", ["+serialize(graph)", "+deserialize(bytes, clazz)"], "blue"),
      node("jacksonSerializer", "JacksonSerializer", "open class", ["implements JsonSerializer", "uses ObjectMapper"], "green"),
      node("jackson", "Jackson", "object", ["defaultJsonMapper", "createTypedJsonMapper(...)"], "purple"),
      node("mapperSupport", "JsonMapperSupport", "extension file", ["jsonMapper { ... }", "ObjectMapper read/write helpers"], "teal"),
      node("objectMapper", "ObjectMapper / JsonMapper", "Jackson core type", ["readValue/writeValue", "module registration"], "gray"),
      node("textSerializers", "CSV/YAML/TOML serializers", "text adapters", ["extend JacksonSerializer", "format-specific mappers"], "amber"),
      node("binarySerializers", "CBOR/Ion/Smile serializers", "binary adapters", ["extend JacksonSerializer", "binary dataformats"], "pink"),
      node("uuidCryptoMask", "UUID/Crypto/Mask modules", "Jackson modules", ["custom serializers", "contextual handlers"], "purple"),
    ],
    edges: [
      edge("jacksonSerializer", "jsonSerializer", "implements", "inherit", "green", { dotFrom: "jsonSerializer", dotTo: "jacksonSerializer", reverseRoute: true }),
      edge("jacksonSerializer", "objectMapper", "mapper", "use", "gray"),
      edge("jackson", "objectMapper", "creates", "use", "purple"),
      edge("mapperSupport", "objectMapper", "extends", "use", "teal"),
      edge("textSerializers", "jacksonSerializer", "extends", "inherit", "amber", { dotFrom: "jacksonSerializer", dotTo: "textSerializers", reverseRoute: true }),
      edge("binarySerializers", "jacksonSerializer", "extends", "inherit", "pink", { dotFrom: "jacksonSerializer", dotTo: "binarySerializers", reverseRoute: true }),
      edge("uuidCryptoMask", "objectMapper", "registers", "use", "purple"),
    ],
    ranks: [
      ["jsonSerializer", "jackson", "mapperSupport"],
      ["jacksonSerializer", "objectMapper"],
      ["textSerializers", "binarySerializers", "uuidCryptoMask"],
    ],
  },
];

for (const diagram of diagrams) {
  render(diagram);
}

function render(diagram) {
  mkdirSync(OUT, { recursive: true });
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
  console.log(`${diagram.file}: dot/plain/sketch/final generated nodes=${diagram.nodes.length} edges=${diagram.edges.length}`);
}

function toDot(diagram) {
  const lines = [
    "digraph G {",
    `  graph [rankdir=${diagram.rankdir}, splines=ortho, nodesep=0.95, ranksep=1.25, outputorder=edgesfirst];`,
    "  node [shape=box, style=\"rounded,filled\", fontname=\"Comic Mono\", fontsize=11, margin=\"0.16,0.10\", width=2.7, height=1.0];",
    "  edge [fontname=\"Comic Mono\", fontsize=10, arrowsize=0.7, penwidth=1.8];",
  ];
  for (const item of diagram.nodes) {
    const color = palette[item.color] || palette.gray;
    lines.push(`  ${item.id} [label="${escDot(item.label)}", fillcolor="${color[0]}", color="${color[1]}"];`);
  }
  for (const group of diagram.ranks) {
    lines.push(`  { rank=same; ${group.join("; ")}; }`);
  }
  for (const item of diagram.edges) {
    const color = palette[item.color] || palette.gray;
    const style = item.kind === "inherit" ? "solid" : "dashed";
    const dotFrom = item.dotFrom || item.from;
    const dotTo = item.dotTo || item.to;
    const attrs = [
      `color="${color[2]}"`,
      `fontcolor="${color[2]}"`,
      `style="${style}"`,
      `arrowhead="none"`,
      item.kind === "inherit" ? "weight=12" : null,
      item.constraint === false ? "constraint=false" : null,
    ];
    lines.push(`  ${dotFrom} -> ${dotTo} [${attrs.filter(Boolean).join(", ")}];`);
  }
  lines.push("}");
  return `${lines.join("\n")}\n`;
}

function parsePlain(plain, diagram) {
  const graph = plain.match(/^graph\s+\S+\s+([\d.]+)\s+([\d.]+)/m);
  const graphW = Number(graph?.[1] || 1);
  const graphH = Number(graph?.[2] || 1);
  const nodes = new Map();
  const edges = [];

  for (const line of plain.split(/\r?\n/)) {
    const parts = line.match(/(?:[^\s"]+|"[^"]*")+/g) || [];
    if (parts[0] === "node") {
      const id = parts[1];
      const model = diagram.nodes.find((item) => item.id === id);
      nodes.set(id, {
        ...model,
        cx: Number(parts[2]),
        cy: Number(parts[3]),
        gw: Number(parts[4]),
        gh: Number(parts[5]),
      });
    } else if (parts[0] === "edge") {
      const from = parts[1];
      const to = parts[2];
      const count = Number(parts[3]);
      const points = [];
      for (let index = 0; index < count; index += 1) {
        points.push({ x: Number(parts[4 + index * 2]), y: Number(parts[5 + index * 2]) });
      }
      const model = diagram.edges.find((item) => (item.dotFrom || item.from) === from && (item.dotTo || item.to) === to);
      edges.push({ ...model, points });
    }
  }
  return { graphW, graphH, nodes: [...nodes.values()], edges };
}

function toFinalSvg(diagram, layout) {
  const scale = 142;
  const marginX = 118;
  const marginTop = 165;
  const cardW = 330;
  const cardH = 132;
  const contentW = layout.graphW * scale + cardW + marginX * 2;
  const contentH = layout.graphH * scale + cardH + marginTop + 145;
  const width = Math.max(1460, Math.ceil(contentW));
  const height = Math.max(760, Math.ceil(contentH));
  const positioned = new Map(layout.nodes.map((item) => {
    const x = marginX + item.cx * scale;
    const y = marginTop + (layout.graphH - item.cy) * scale;
    return [item.id, { ...item, x: Math.round(x), y: Math.round(y), w: cardW, h: cardH }];
  }));
  applyDiagramNudges(diagram.file, positioned);

  const routeOf = (point) => ({
    x: marginX + point.x * scale,
    y: marginTop + (layout.graphH - point.y) * scale,
  });
  const edgeSvg = layout.edges.map((item) => renderEdge(item, positioned, routeOf)).join("\n");
  const nodeSvg = [...positioned.values()].map(renderClassCard).join("\n");
  return base(width, height, diagram.title, diagram.subtitle, `${edgeSvg}\n${nodeSvg}\n${legend(width - 415, height - 108)}\n${footer(180, height - 90, width - 640, "Graphviz evidence: .dot, .plain, and -graphviz.svg define ranks, order, route sides, and endpoints.")}`);
}

function applyDiagramNudges(file, positioned) {
  if (file === "io-fastjson2-diagram-01") {
    moveNode(positioned, "jsonb", 36, 0);
    moveNode(positioned, "json", -36, 0);
  }
}

function moveNode(positioned, id, dx, dy) {
  const node = positioned.get(id);
  if (!node) return;
  node.x += dx;
  node.y += dy;
}

function renderEdge(edge, positioned, routeOf) {
  const from = positioned.get(edge.from);
  const to = positioned.get(edge.to);
  const color = palette[edge.color]?.[2] || palette.gray[2];
  if (edge.kind === "inherit") {
    return renderInheritanceEdge(edge, from, to, color);
  }
  const points = shortestDependencyRoute(from, to).map((point) => ({
    x: Math.round(point.x),
    y: Math.round(point.y),
  }));
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point.x} ${point.y}`).join(" ");
  const cls = edge.kind === "inherit" ? "inherit" : "dependency";
  const dash = edge.kind === "inherit" ? "" : ` stroke-dasharray="8 7"`;
  return `<path class="${cls}" data-from="${edge.from}" data-to="${edge.to}" d="${d}" stroke="${color}"${dash}/>`;
}

function renderInheritanceEdge(edge, from, to, color) {
  const fromCenter = center(from);
  const toCenter = center(to);
  const source = { x: Math.round(fromCenter.x), y: from.y };
  const targetPort = targetInheritancePort(fromCenter, to);
  const baseMidY = Math.round((source.y + targetPort.y) / 2);
  const laneOffset = fromCenter.x < toCenter.x - 40 ? -18 : fromCenter.x > toCenter.x + 40 ? 18 : 0;
  const midY = baseMidY + laneOffset;
  const points = Math.abs(source.x - targetPort.x) < 14
    ? [source, targetPort]
    : [source, { x: source.x, y: midY }, { x: targetPort.x, y: midY }, targetPort];
  const d = dedupeRoute(points).map((point, index) => `${index === 0 ? "M" : "L"}${Math.round(point.x)} ${Math.round(point.y)}`).join(" ");
  return `<path class="inherit" data-from="${edge.from}" data-to="${edge.to}" d="${d}" stroke="${color}"/>`;
}

function targetInheritancePort(sourceCenter, target) {
  const targetCenter = center(target);
  const delta = sourceCenter.x - targetCenter.x;
  const portShift = Math.max(-target.w * 0.28, Math.min(target.w * 0.28, delta * 0.42));
  return {
    x: Math.round(targetCenter.x + portShift),
    y: target.y + target.h,
  };
}

function shortestDependencyRoute(from, to) {
  const fc = center(from);
  const tc = center(to);
  const horizontalGap = from.x + from.w <= to.x || to.x + to.w <= from.x;
  const verticalGap = from.y + from.h <= to.y || to.y + to.h <= from.y;

  if (horizontalGap && rangesOverlap(from.y, from.y + from.h, to.y, to.y + to.h)) {
    const y = sharedHorizontalY(from, to);
    if (y != null) {
      return dedupeRoute([
        pointOnSide(from, fc.x < tc.x ? "right" : "left", y),
        pointOnSide(to, fc.x < tc.x ? "left" : "right", y),
      ]);
    }
  }

  if (verticalGap && rangesOverlap(from.x, from.x + from.w, to.x, to.x + to.w)) {
    const x = sharedVerticalX(from, to);
    if (x != null) {
      return dedupeRoute([
        pointOnSide(from, fc.y < tc.y ? "bottom" : "top", x),
        pointOnSide(to, fc.y < tc.y ? "top" : "bottom", x),
      ]);
    }
  }

  const verticalFirst = Math.abs(fc.y - tc.y) >= Math.abs(fc.x - tc.x) * 0.72;
  if (verticalFirst) {
    const start = pointOnSide(from, fc.y < tc.y ? "bottom" : "top", clamp(tc.x, from.x + 42, from.x + from.w - 42));
    const end = pointOnSide(to, fc.y < tc.y ? "top" : "bottom", clamp(start.x, to.x + 42, to.x + to.w - 42));
    return routeWithOneLane(start, end, "vertical");
  }

  const start = pointOnSide(from, fc.x < tc.x ? "right" : "left", clamp(tc.y, from.y + 34, from.y + from.h - 34));
  const end = pointOnSide(to, fc.x < tc.x ? "left" : "right", clamp(start.y, to.y + 34, to.y + to.h - 34));
  return routeWithOneLane(start, end, "horizontal");
}

function routeWithOneLane(start, end, mode) {
  if (Math.abs(start.x - end.x) <= 1 || Math.abs(start.y - end.y) <= 1) {
    return dedupeRoute([start, end]);
  }
  if (mode === "vertical") {
    const midY = Math.round((start.y + end.y) / 2);
    return dedupeRoute([start, { x: start.x, y: midY }, { x: end.x, y: midY }, end]);
  }
  const midX = Math.round((start.x + end.x) / 2);
  return dedupeRoute([start, { x: midX, y: start.y }, { x: midX, y: end.y }, end]);
}

function pointOnSide(rect, side, value) {
  if (side === "left") return { x: rect.x, y: clamp(value, rect.y + 30, rect.y + rect.h - 30), side };
  if (side === "right") return { x: rect.x + rect.w, y: clamp(value, rect.y + 30, rect.y + rect.h - 30), side };
  if (side === "top") return { x: clamp(value, rect.x + 42, rect.x + rect.w - 42), y: rect.y, side };
  return { x: clamp(value, rect.x + 42, rect.x + rect.w - 42), y: rect.y + rect.h, side: "bottom" };
}

function rangesOverlap(a1, a2, b1, b2) {
  return Math.max(a1, b1) <= Math.min(a2, b2);
}

function overlapCenter(a1, a2, b1, b2) {
  return Math.round((Math.max(a1, b1) + Math.min(a2, b2)) / 2);
}

function sharedVerticalX(from, to) {
  const min = Math.max(from.x + 42, to.x + 42);
  const max = Math.min(from.x + from.w - 42, to.x + to.w - 42);
  if (min <= max) return Math.round((min + max) / 2);
  return null;
}

function sharedHorizontalY(from, to) {
  const min = Math.max(from.y + 30, to.y + 30);
  const max = Math.min(from.y + from.h - 30, to.y + to.h - 30);
  if (min <= max) return Math.round((min + max) / 2);
  return null;
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function renderClassCard(node) {
  const [fill, stroke] = palette[node.color] || palette.gray;
  const titleLines = wrapIdentifier(node.label, 23).slice(0, 2);
  const memberLines = node.members.flatMap((line) => wrap(line, 32)).slice(0, 3);
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
    .inherit{fill:none;stroke-width:2.45;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#inheritArrow)}
    .dependency{fill:none;stroke-width:2.25;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#depArrow)}
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

function boundaryPoint(rect, toward) {
  const port = boundaryPort(rect, toward);
  return { x: port.x, y: port.y };
}

function boundaryPort(rect, toward) {
  const c = center(rect);
  const dx = toward.x - c.x;
  const dy = toward.y - c.y;
  if (Math.abs(dx) / rect.w > Math.abs(dy) / rect.h) {
    return { x: dx >= 0 ? rect.x + rect.w : rect.x, y: Math.round(c.y), side: dx >= 0 ? "right" : "left" };
  }
  return { x: Math.round(c.x), y: dy >= 0 ? rect.y + rect.h : rect.y, side: dy >= 0 ? "bottom" : "top" };
}

function center(rect) {
  return { x: rect.x + rect.w / 2, y: rect.y + rect.h / 2 };
}

function node(id, label, stereotype, members, color) {
  return { id, label, stereotype, members, color };
}

function edge(from, to, label, kind, color, options = {}) {
  return { from, to, label, kind, color, ...options };
}

function footer(x, y, w, text) {
  return `<g><rect x="${x}" y="${y}" width="${w}" height="42" rx="12" fill="#FFFFFF" stroke="#D6E3EF" stroke-width="1.6"/><text class="detail" x="${x + w / 2}" y="${y + 26}" text-anchor="middle">${esc(text)}</text></g>`;
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

function firstDistinct(points, fallback) {
  return points.find((point) => Number.isFinite(point.x) && Number.isFinite(point.y)) || fallback;
}

function lastDistinct(points, fallback) {
  return [...points].reverse().find((point) => Number.isFinite(point.x) && Number.isFinite(point.y)) || fallback;
}

function dedupeRoute(points) {
  const result = [];
  for (const point of points) {
    const previous = result.at(-1);
    if (!previous || Math.abs(previous.x - point.x) > 1 || Math.abs(previous.y - point.y) > 1) {
      result.push(point);
    }
  }
  return result;
}

function wrap(text, max) {
  const words = String(text).split(/\s+/);
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

function wrapIdentifier(text, max) {
  if (String(text).includes(" ")) return wrap(text, max);
  const parts = String(text).match(/[A-Z]?[a-z0-9]+|[A-Z]+(?=[A-Z]|$)/g) || [String(text)];
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

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function escDot(value) {
  return String(value ?? "").replaceAll("\\", "\\\\").replaceAll('"', '\\"');
}
