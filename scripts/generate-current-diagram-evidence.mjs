#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const OUT = join(process.cwd(), "docs/images/readme-diagrams");
const WORKLIST = process.env.KIND_WORKLIST || "/tmp/bluetape4k-projects-diagram-redraw/kind-worklist.tsv";

const rows = readFileSync(WORKLIST, "utf8").split(/\r?\n/).filter(Boolean)
  .map((row) => row.split("\t"))
  .filter(([kind]) => kind !== "class");

let generated = 0;
for (const [kind, file] of rows) {
  const svgPath = join(OUT, `${file}.svg`);
  if (!existsSync(svgPath)) throw new Error(`missing SVG: ${svgPath}`);
  const svg = readFileSync(svgPath, "utf8");
  const nodes = extractNodes(svg);
  const routes = extractRoutes(svg, nodes);
  if (nodes.length === 0) throw new Error(`${file}: no cards found for evidence`);
  const dot = renderDot(file, kind, nodes, routes);
  const dotPath = join(OUT, `${file}.dot`);
  writeFileSync(dotPath, dot);
  execFileSync("neato", ["-n2", "-Tplain", dotPath, "-o", join(OUT, `${file}.plain`)], { stdio: "inherit" });
  execFileSync("neato", ["-n2", "-Tsvg", dotPath, "-o", join(OUT, `${file}-graphviz.svg`)], { stdio: "inherit" });
  execFileSync("neato", ["-n2", "-Tpng", dotPath, "-o", join(OUT, `${file}-graphviz.png`)], { stdio: "inherit" });
  generated += 1;
  console.log(`${file}: evidence kind=${kind} nodes=${nodes.length} routes=${routes.length}`);
}

console.log(`current-diagram-evidence: files=${generated}`);

function extractNodes(svg) {
  const nodes = [];
  const groupPattern = /<g(?:\s+id="([^"]+)")?[^>]*>([\s\S]*?)<\/g>/g;
  let match;
  while ((match = groupPattern.exec(svg)) != null) {
    const body = match[2];
    const rect = body.match(/<rect class="(?:card|classCard)" x="([^"]+)" y="([^"]+)" width="([^"]+)" height="([^"]+)"/);
    if (!rect) continue;
    const label = text(body.match(/<text class="(?:cardTitle|classTitle)"[^>]*>([\s\S]*?)<\/text>/)?.[1]) || `node ${nodes.length + 1}`;
    nodes.push({
      id: sanitize(match[1] || label || `node_${nodes.length}`),
      label,
      x: Number(rect[1]),
      y: Number(rect[2]),
      w: Number(rect[3]),
      h: Number(rect[4]),
    });
  }
  return nodes;
}

function extractRoutes(svg, nodes) {
  const routes = [];
  for (const match of svg.matchAll(/<path class="(?:flow|inherit|dependency|line)"[^>]*d="([^"]+)"/g)) {
    const points = [...match[1].matchAll(/[ML]\s*([\d.]+)\s+([\d.]+)/g)].map((item) => ({ x: Number(item[1]), y: Number(item[2]) }));
    if (points.length < 2) continue;
    const from = nearestNode(points[0], nodes);
    const to = nearestNode(points.at(-1), nodes, from?.id);
    if (!from || !to || from.id === to.id) continue;
    routes.push({ from: from.id, to: to.id });
  }
  return uniqueRoutes(routes);
}

function renderDot(file, kind, nodes, routes) {
  const lines = [
    "digraph G {",
    `  graph [layout=neato, splines=ortho, outputorder=edgesfirst, label="${escDot(file)} ${kind} evidence", labelloc=t, fontsize=18, fontname="Comic Mono"];`,
    "  node [shape=box, style=\"rounded,filled\", fillcolor=\"#F7FBFF\", color=\"#9AA8B8\", fontname=\"Comic Mono\", fontsize=10, margin=\"0.10,0.08\"];",
    "  edge [color=\"#758297\", arrowsize=0.65, penwidth=1.6];",
  ];
  for (const node of nodes) {
    const cx = (node.x + node.w / 2) / 72;
    const cy = -(node.y + node.h / 2) / 72;
    const w = Math.max(1.0, node.w / 72);
    const h = Math.max(0.55, node.h / 72);
    lines.push(`  ${node.id} [label="${escDot(node.label)}", pos="${cx.toFixed(3)},${cy.toFixed(3)}!", width="${w.toFixed(3)}", height="${h.toFixed(3)}"];`);
  }
  for (const route of routes) {
    lines.push(`  ${route.from} -> ${route.to};`);
  }
  lines.push("}");
  return `${lines.join("\n")}\n`;
}

function nearestNode(point, nodes, exceptId = null) {
  return nodes
    .filter((node) => node.id !== exceptId)
    .map((node) => ({ node, distance: distanceToRect(point, node) }))
    .sort((a, b) => a.distance - b.distance)[0]?.node;
}

function distanceToRect(point, rect) {
  const dx = Math.max(rect.x - point.x, 0, point.x - (rect.x + rect.w));
  const dy = Math.max(rect.y - point.y, 0, point.y - (rect.y + rect.h));
  return Math.hypot(dx, dy);
}

function uniqueRoutes(routes) {
  const seen = new Set();
  return routes.filter((route) => {
    const key = `${route.from}->${route.to}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function sanitize(value) {
  const id = String(value).replace(/[^A-Za-z0-9_]+/g, "_").replace(/^_+|_+$/g, "");
  return /^[A-Za-z_]/.test(id) ? id : `node_${id}`;
}

function text(value) {
  return String(value ?? "")
    .replace(/<[^>]+>/g, "")
    .replaceAll("&amp;", "&")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&quot;", '"')
    .replace(/\s+/g, " ")
    .trim();
}

function escDot(value) {
  return String(value ?? "").replaceAll("\\", "\\\\").replaceAll('"', '\\"');
}
