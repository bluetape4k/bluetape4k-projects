#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const DIR = join(ROOT, "docs/images/readme-diagrams");
const rsvg = "/opt/homebrew/bin/rsvg-convert";

let changed = 0;

for (const file of readdirSync(DIR).filter((name) => name.endsWith(".svg") && !name.endsWith("-sketch.svg") && !name.endsWith("-graphviz.svg")).sort()) {
  const path = join(DIR, file);
  const svg = readFileSync(path, "utf8");
  const box = measure(svg);
  if (!box.width || !box.height || !box.overflow) continue;
  const nextWidth = Math.ceil(Math.max(box.width, box.maxX + 72));
  const nextHeight = Math.ceil(Math.max(box.height, box.maxY + 72));
  let next = svg
    .replace(/\bwidth="[\d.]+"/, `width="${nextWidth}"`)
    .replace(/\bheight="[\d.]+"/, `height="${nextHeight}"`)
    .replace(/viewBox="0 0 [\d.]+ [\d.]+"/, `viewBox="0 0 ${nextWidth} ${nextHeight}"`);
  next = next.replace(/<rect class="canvas" width="[\d.]+" height="[\d.]+"\/>/, `<rect class="canvas" width="${nextWidth}" height="${nextHeight}"/>`);
  next = next.replace(/<rect class="frame" x="32" y="28" width="[\d.]+" height="[\d.]+" rx="([^"]+)"\/>/, `<rect class="frame" x="32" y="28" width="${nextWidth - 64}" height="${nextHeight - 56}" rx="$1"/>`);
  writeFileSync(path, next);
  execFileSync(rsvg, ["--format=png", "--output", path.replace(/\.svg$/, ".png"), path], { stdio: "inherit" });
  changed += 1;
  console.log(`${file} viewBox ${box.width}x${box.height} -> ${nextWidth}x${nextHeight}`);
}

console.log(`expand-overflowing-readme-diagrams: changed=${changed}`);

function measure(svg) {
  const viewBox = svg.match(/viewBox="0 0 ([\d.]+) ([\d.]+)"/);
  const width = viewBox ? Number(viewBox[1]) : Number(svg.match(/\bwidth="([\d.]+)"/)?.[1] || 0);
  const height = viewBox ? Number(viewBox[2]) : Number(svg.match(/\bheight="([\d.]+)"/)?.[1] || 0);
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
    const numbers = match[1].match(/-?\d*\.?\d+(?:e[-+]?\d+)?/gi)?.map(Number) || [];
    for (let index = 0; index + 1 < numbers.length; index += 2) {
      xs.push(numbers[index]);
      ys.push(numbers[index + 1]);
    }
  }
  const maxX = Math.round(Math.max(0, ...xs) * 10) / 10;
  const maxY = Math.round(Math.max(0, ...ys) * 10) / 10;
  return { width, height, maxX, maxY, overflow: maxX > width + 2 || maxY > height + 2 };
}

function attrNumber(tag, name) {
  return Number(tag.match(new RegExp(`\\b${name}="([-\\d.]+)"`))?.[1]);
}
