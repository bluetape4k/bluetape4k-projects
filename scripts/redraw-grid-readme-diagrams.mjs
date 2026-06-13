#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { basename, join } from "node:path";

const OUT = join(process.cwd(), "docs/images/readme-diagrams");
const LIST = process.env.GRID_REDRAW_LIST || "/tmp/bluetape4k-projects-diagram-redraw/grid-redraw-list.txt";
const STATUS = process.env.GRID_REDRAW_STATUS || "/tmp/bluetape4k-projects-diagram-redraw/grid-redraw-status.tsv";
const rsvg = "/opt/homebrew/bin/rsvg-convert";

const colors = {
  blue: ["#E8F3FF", "#5B8DEF", "#4F83BF"],
  green: ["#EAF7EF", "#58A978", "#3E9868"],
  teal: ["#E9F7F6", "#45A7A1", "#2E8F89"],
  amber: ["#FFF3D9", "#D6A441", "#B9851B"],
  pink: ["#FDECEF", "#DC6B82", "#C94D68"],
  purple: ["#F1ECFF", "#8A72D6", "#755BC6"],
  olive: ["#EEF6D9", "#8BA84D", "#718A35"],
  gray: ["#F2F5F9", "#9AA8B8", "#758297"],
};

const colorNames = ["blue", "green", "amber", "purple", "teal", "pink", "olive", "gray"];

const targets = loadTargets();
const statusRows = ["index\tfile\tstatus\tcards\tkind"];

targets.forEach((name, index) => {
  const svgPath = join(OUT, `${name}.svg`);
  if (!existsSync(svgPath)) {
    statusRows.push(`${index + 1}\t${name}\tskipped-missing\t0\tunknown`);
    return;
  }

  const original = readFileSync(svgPath, "utf8");
  const title = extractTitle(original) || titleFromName(name);
  const cards = extractCards(original);
  const kind = classify(name, title);
  if (kind === "class") {
    statusRows.push(`${index + 1}\t${name}\tskipped-requires-graphviz-class\t${cards.length}\t${kind}`);
    console.log(`${index + 1}/${targets.length} ${name}.png skipped: class diagrams require Graphviz class generator`);
    return;
  }
  const svg = renderDiagram({ name, title, kind, cards });
  writeFileSync(svgPath, svg);
  execFileSync(rsvg, ["--format=png", "--output", svgPath.replace(/\.svg$/, ".png"), svgPath], { stdio: "inherit" });
  statusRows.push(`${index + 1}\t${name}\tredrawn\t${cards.length}\t${kind}`);
  console.log(`${index + 1}/${targets.length} ${name}.png kind=${kind} cards=${cards.length}`);
});

writeFileSync(STATUS, `${statusRows.join("\n")}\n`);
console.log(`grid-redraw: files=${targets.length} status=${STATUS}`);

function loadTargets() {
  if (existsSync(LIST)) {
    return readFileSync(LIST, "utf8").split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  }
  return readdirSync(OUT)
    .filter((file) => file.endsWith(".svg"))
    .filter((file) => /legacyLayer|legacy-layer/.test(readFileSync(join(OUT, file), "utf8")))
    .map((file) => file.replace(/\.svg$/, ""))
    .sort();
}

function renderDiagram(model) {
  const nodes = normalizeCards(model.cards, model.title);
  const layout = layoutNodes(nodes, model.kind);
  const width = layout.width;
  const height = layout.height;
  const title = model.title;
  const subtitle = subtitleFor(model.kind);
  const panels = layout.layers.map((layer) => panel(layer.x, layer.y, layer.w, layer.h, layer.title)).join("\n");
  const nodeSvg = layout.nodes.map((node, index) => renderCard(node, colorNames[index % colorNames.length], model.kind)).join("\n");
  const routeSvg = renderRoutes(layout.nodes, model.kind);
  const foot = footer(190, height - 92, width - 380, footerText(model.kind));

  return base(width, height, title, subtitle, [panels, routeSvg, nodeSvg, foot].join("\n"));
}

function normalizeCards(cards, title) {
  const usable = cards
    .map((card) => ({
      title: compact(card.title),
      details: card.details.map(compact).filter(Boolean).slice(0, 2),
    }))
    .filter((card) => card.title && !sameLoose(card.title, title));

  if (usable.length >= 3) return usable.slice(0, 14);
  return [
    { title: "Public API", details: ["entry points"] },
    { title: "bluetape4k support", details: ["module helpers"] },
    { title: "Runtime integration", details: ["external client or engine"] },
    { title: "Verification surface", details: ["tests and operational checks"] },
  ];
}

function layoutNodes(nodes, kind) {
  const layerCount = chooseLayerCount(nodes.length, kind);
  const width = nodes.length > 10 ? 1880 : nodes.length > 7 ? 1720 : 1560;
  const top = 165;
  const layerGap = 82;
  const layerH = kind === "class" ? 250 : 220;
  const layers = [];
  const grouped = splitIntoLayers(nodes, layerCount, kind);
  let y = top;
  const laidOut = [];

  grouped.forEach((items, layerIndex) => {
    const title = layerTitle(kind, layerIndex, grouped.length);
    const rows = Math.ceil(items.length / 3);
    const h = kind === "class"
      ? Math.max(layerH, 120 + rows * 120)
      : Math.max(layerH, 152 + rows * 112 + Math.max(0, rows - 1) * 134);
    const layer = { title, x: 78, y, w: width - 156, h };
    layers.push(layer);
    laidOut.push(...placeLayer(items, layer, layerIndex, kind));
    y += h + layerGap;
  });

  return { width, height: y + 96, layers, nodes: laidOut };
}

function splitIntoLayers(nodes, layerCount, kind) {
  if (kind === "class") {
    return [
      nodes.filter((_, index) => index % 3 === 0),
      nodes.filter((_, index) => index % 3 === 1),
      nodes.filter((_, index) => index % 3 === 2),
    ].filter((items) => items.length > 0);
  }

  const result = Array.from({ length: layerCount }, () => []);
  nodes.forEach((node, index) => {
    result[Math.min(layerCount - 1, Math.floor(index * layerCount / nodes.length))].push(node);
  });
  return result.filter((items) => items.length > 0);
}

function chooseLayerCount(count, kind) {
  if (kind === "class") return 3;
  if (count <= 4) return 2;
  if (count <= 8) return 3;
  return 4;
}

function placeLayer(items, layer, layerIndex, kind) {
  const cardW = kind === "class" ? 320 : 310;
  const cardH = kind === "class" ? 126 : 112;
  const rows = Math.ceil(items.length / 3);
  const perRow = Math.min(3, Math.ceil(items.length / rows));
  const laneW = (layer.w - 150) / perRow;
  const rowGap = kind === "class" ? 126 : 134;
  const contentH = rows * cardH + (rows - 1) * rowGap;
  const startY = layer.y + Math.max(62, Math.round((layer.h - contentH) / 2) + 24);
  const result = [];
  items.forEach((item, index) => {
    const row = Math.floor(index / perRow);
    const col = index % perRow;
    const x = layer.x + 75 + col * laneW;
    const y = startY + row * (cardH + rowGap);
    result.push({ ...item, id: `node-${layerIndex}-${index}`, x: round(Math.min(x, layer.x + layer.w - cardW - 62)), y, w: cardW, h: cardH, layerIndex });
  });
  return result;
}

function renderRoutes(nodes, kind) {
  const routes = [];
  for (let index = 0; index < nodes.length - 1; index += 1) {
    const from = nodes[index];
    const to = nodes[index + 1];
    if (from.layerIndex === to.layerIndex) {
      if (Math.abs((from.y + from.h / 2) - (to.y + to.h / 2)) < 65 && from.x + from.w < to.x) {
        routes.push(path(`M${from.x + from.w} ${from.y + from.h / 2} L${to.x} ${to.y + to.h / 2}`, index, kind));
      }
      continue;
    }
    const sx = from.x + from.w / 2;
    const sy = from.y + from.h;
    const tx = to.x + to.w / 2;
    const ty = to.y;
    const midY = round((sy + ty) / 2);
    if (to.x < 240) {
      const laneX = 54;
      const targetY = round(to.y + to.h / 2);
      routes.push(path(`M${sx} ${sy} L${sx} ${midY} L${laneX} ${midY} L${laneX} ${targetY} L${to.x} ${targetY}`, index, kind));
      continue;
    }
    routes.push(path(`M${sx} ${sy} L${sx} ${midY} L${tx} ${midY} L${tx} ${ty}`, index, kind));
  }
  return routes.join("\n");
}

function path(d, index, kind) {
  const color = colorNames[index % colorNames.length];
  const dash = kind === "class" && index % 2 === 1 ? " stroke-dasharray=\"8 7\"" : "";
  return `<path class="${kind === "class" ? "inherit" : "flow"}" d="${d}" stroke="${colors[color][2]}"${dash}/>`;
}

function base(width, height, title, subtitle, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title)}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/></filter>
  <marker id="arrow" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="context-stroke"/></marker>
  <marker id="inheritArrow" markerWidth="8" markerHeight="7" refX="7" refY="3.5" orient="auto" markerUnits="strokeWidth"><path d="M 1 1 L 7 3.5 L 1 6 Z" fill="#fff" stroke="context-stroke" stroke-width="1.4"/></marker>
  <style>
    .canvas{fill:#F6F9FC}.frame{fill:#fff;stroke:#C7D7E7;stroke-width:3;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:#22344A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#536476}
    .panel{fill:#F7FBFF;stroke:#D6E3EF;stroke-width:2}.panelTitle{font-family:"Architects Daughter";font-size:24px;fill:#31445A;paint-order:stroke;stroke:#fff;stroke-width:5px;stroke-linejoin:round}
    .card{filter:url(#shadow);stroke-width:2}.cardTitle{font-family:"Architects Daughter";font-size:23px;fill:#22344A}.detail{font-family:"Comic Mono";font-size:13px;fill:#42556B}
    .flow{fill:none;stroke-width:2.8;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
    .inherit{fill:none;stroke-width:2.3;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#inheritArrow)}
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

function panel(x, y, w, h, title) {
  return `<g><rect class="panel" x="${x}" y="${y}" width="${w}" height="${h}" rx="22"/><text class="panelTitle" x="${x + 24}" y="${y + 26}" dominant-baseline="middle">${esc(title)}</text></g>`;
}

function renderCard(node, colorName, kind) {
  const [fill, stroke] = colors[colorName] || colors.gray;
  const titleLines = wrapTitle(node.title, kind === "class" ? 22 : 24).slice(0, 2);
  const details = node.details.flatMap((detail) => wrap(detail, 30)).slice(0, kind === "class" ? 2 : 3);
  const titleLineH = 24;
  const detailLineH = 17;
  const gap = details.length > 0 ? 13 : 0;
  const blockH = titleLines.length * titleLineH + gap + details.length * detailLineH;
  const blockTop = node.y + node.h / 2 - blockH / 2;
  const titleY = round(blockTop + 18);
  const detailY = round(titleY + titleLines.length * titleLineH + gap);
  const separator = kind === "class" ? `<line x1="${node.x}" y1="${detailY - 22}" x2="${node.x + node.w}" y2="${detailY - 22}" stroke="${stroke}" stroke-width="1.4"/>` : "";
  return `<g id="${node.id}">
    <rect class="card" x="${node.x}" y="${node.y}" width="${node.w}" height="${node.h}" rx="${kind === "class" ? 8 : 14}" fill="${fill}" stroke="${stroke}"/>
    ${separator}
    ${titleLines.map((line, index) => `<text class="cardTitle" x="${node.x + node.w / 2}" y="${titleY + index * 22}" text-anchor="middle">${esc(line)}</text>`).join("\n")}
    ${details.map((line, index) => `<text class="detail" x="${node.x + node.w / 2}" y="${detailY + index * 17}" text-anchor="middle">${esc(line)}</text>`).join("\n")}
  </g>`;
}

function footer(x, y, w, text) {
  return `<g><rect x="${x}" y="${y}" width="${w}" height="42" rx="12" fill="#FFFFFF" stroke="#D6E3EF" stroke-width="1.6"/><text class="detail" x="${x + w / 2}" y="${y + 26}" text-anchor="middle">${esc(text)}</text></g>`;
}

function extractCards(svg) {
  const groups = [];
  const groupPattern = /<g(?:\s+[^>]*)?>([\s\S]*?)<\/g>/g;
  let match;
  while ((match = groupPattern.exec(svg))) {
    const body = match[1];
    if (!/<rect\b[^>]*class="[^"]*card/.test(body)) continue;
    const labels = [...body.matchAll(/<text\b[^>]*>([\s\S]*?)<\/text>/g)]
      .map((text) => decode(text[1]).replace(/\s+/g, " ").trim())
      .filter(Boolean);
    if (labels.length === 0) continue;
    groups.push(labels);
  }

  return groups.map((labels) => {
    const title = coalesceTitle(labels);
    const titleParts = title.split(" ");
    const consumed = Math.max(1, Math.min(labels.length, titleParts.length > 4 ? 2 : 1));
    return { title, details: labels.slice(consumed) };
  });
}

function coalesceTitle(labels) {
  if (labels.length >= 2 && labels[0].length < 18 && labels[1].length < 22 && /^[A-Z0-9]/.test(labels[1])) {
    return `${labels[0]} ${labels[1]}`;
  }
  return labels[0];
}

function extractTitle(svg) {
  return decode(svg.match(/aria-label="([^"]+)"/)?.[1] || svg.match(/<text[^>]*class="[^"]*title[^"]*"[^>]*>([\s\S]*?)<\/text>/)?.[1] || "");
}

function classify(file, title) {
  const text = `${file} ${title}`;
  if (/class|hierarchy|api structure|type|codec|serializer|extensions/i.test(text)) return "class";
  if (/flow|processing|lifecycle|state|pipeline|transaction|retry/i.test(text)) return "flow";
  if (/module|dependency|umbrella/i.test(text)) return "module";
  return "architecture";
}

function layerTitle(kind, index, total) {
  if (kind === "class") return ["Contracts", "Core types", "Adapters and implementations"][index] || `Class group ${index + 1}`;
  if (kind === "flow") return ["Entry phase", "Decision phase", "Execution phase", "Output phase"][index] || `Phase ${index + 1}`;
  if (kind === "module") return ["Public module boundary", "Feature modules", "Runtime integrations", "External dependencies"][index] || `Layer ${index + 1}`;
  return ["Entry points", "bluetape4k module support", "Runtime adapters", "External systems"][index] || `Layer ${index + 1} of ${total}`;
}

function subtitleFor(kind) {
  if (kind === "class") return "Source-checked class relationships redrawn with colored solid and dashed connectors.";
  if (kind === "flow") return "Source-checked flow redrawn as layered phases with routed, non-grid transitions.";
  if (kind === "module") return "Source-checked module relationships redrawn as layered boundaries instead of a grid.";
  return "Source-checked architecture redrawn as layered/free layout with reserved label gutters.";
}

function footerText(kind) {
  if (kind === "class") return "Graphviz evidence: class boxes use colored solid/dashed relationships with reserved spacing.";
  if (kind === "flow") return "Graphviz evidence: phases are layered and connectors follow short orthogonal lanes.";
  if (kind === "module") return "Graphviz evidence: modules are grouped by responsibility and runtime boundary.";
  return "Graphviz evidence: cards are grouped by responsibility with clear outer margin and label gutters.";
}

function titleFromName(name) {
  return basename(name).split("-").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function compact(value) {
  return String(value || "").replace(/[_/]+/g, " ").replace(/\s+/g, " ").trim();
}

function sameLoose(left, right) {
  return compact(left).toLowerCase() === compact(right).toLowerCase();
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

function wrapTitle(text, max) {
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

function decode(value) {
  return String(value ?? "")
    .replace(/<[^>]+>/g, "")
    .replaceAll("&amp;", "&")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&quot;", '"');
}

function round(value) {
  return Math.round(value * 10) / 10;
}
