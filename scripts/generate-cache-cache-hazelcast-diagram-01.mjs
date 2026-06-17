#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/cache-cache-hazelcast-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/cache-cache-hazelcast-diagram-01.png";
const W = 2440;
const H = 1400;
const colors = { ink: "#0F172A", muted: "#475569", canvas: "#F8FAFC", frame: "#FFFFFF", line: "#CBD5E1", blue: "#2563EB", teal: "#0D9488", green: "#16A34A", orange: "#EA580C", purple: "#7C3AED", gray: "#64748B" };

const sources = [
  "cache/cache-hazelcast/README.md",
  "cache/cache-hazelcast/README.ko.md",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCache.kt",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastSuspendNearCache.kt",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastLocalCache.kt",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastEntryEventListener.kt",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCache.kt",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastSuspendNearJCache.kt",
];
for (const source of sources) if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
const readme = readFileSync(sources[0], "utf8");
if (!/HazelcastNearCache Class Hierarchy[\s\S]*cache-cache-hazelcast-diagram-01\.png/.test(readme)) {
  throw new Error("README diagram slot not found");
}

const boxes = {
  blockingOps: { x: 100, y: 175, w: 455, h: 170, fill: "#EFF6FF", stroke: colors.blue, stereo: "<<interface>>", title: "NearCacheOperations<K,V>", attrs: ["blocking cache contract"], methods: ["get, put, remove, clear", "local/back/cache stats"] },
  suspendOps: { x: 680, y: 175, w: 455, h: 170, fill: "#F0FDFA", stroke: colors.teal, stereo: "<<interface>>", title: "SuspendNearCacheOperations<K,V>", attrs: ["suspend cache contract"], methods: ["suspend get/put/remove", "same semantic surface"] },
  local: { x: 1260, y: 175, w: 455, h: 170, fill: "#F8FAFC", stroke: colors.gray, stereo: "<<interface>>", title: "HazelcastLocalCache<V>", attrs: ["front-cache abstraction"], methods: ["getIfPresent, put", "invalidate, estimatedSize"] },
  caffeine: { x: 1860, y: 175, w: 455, h: 170, fill: "#F8FAFC", stroke: colors.gray, stereo: "<<class>>", title: "CaffeineHazelcastLocalCache<V>", attrs: ["Caffeine Cache<String,V>"], methods: ["expire/size/stat options"] },
  near: { x: 100, y: 545, w: 500, h: 220, fill: "#EFF6FF", stroke: colors.blue, stereo: "<<class>>", title: "HazelcastNearCache<V>", attrs: ["front: HazelcastLocalCache<V>", "back: IMap<String,V>", "listenerId"], methods: ["read-through on miss", "write-through put/remove"] },
  suspendNear: { x: 690, y: 545, w: 500, h: 220, fill: "#F0FDFA", stroke: colors.teal, stereo: "<<class>>", title: "HazelcastSuspendNearCache<V>", attrs: ["front + IMap async bridge", "EntryListener invalidation"], methods: ["awaits IMap async calls", "same write-through policy"] },
  listener: { x: 1310, y: 545, w: 430, h: 190, fill: "#FFF7ED", stroke: colors.orange, stereo: "<<listener>>", title: "HazelcastEntryEventListener", attrs: ["listens to IMap changes"], methods: ["invalidates front cache", "removes updated/deleted keys"] },
  config: { x: 1860, y: 545, w: 455, h: 190, fill: "#F5F3FF", stroke: colors.purple, stereo: "<<data class + DSL>>", title: "HazelcastNearCacheConfig", attrs: ["cacheName, maxLocalSize", "expireAfterWrite/access"], methods: ["validates nonblank name", "validates positive sizes"] },
  resilient: { x: 255, y: 945, w: 520, h: 215, fill: "#ECFDF5", stroke: colors.green, stereo: "<<write-behind class>>", title: "ResilientHazelcastNearCache<V>", attrs: ["front updated immediately", "LinkedBlockingQueue writer"], methods: ["retry IMap mutations", "tombstones avoid stale reads"] },
  resilientSuspend: { x: 900, y: 945, w: 520, h: 215, fill: "#ECFDF5", stroke: colors.green, stereo: "<<suspend write-behind>>", title: "ResilientHazelcastSuspendNearCache<V>", attrs: ["front updated immediately", "Channel-based writer"], methods: ["coroutine consumer", "same stale-read guards"] },
  nearJcache: { x: 1545, y: 945, w: 360, h: 215, fill: "#FFF7ED", stroke: colors.orange, stereo: "<<JCache adapter>>", title: "NearJCache<K,V>", attrs: ["Caffeine + IMap"], methods: ["implements JCache Cache", "listener-backed direct mode"] },
  suspendJcache: { x: 1975, y: 945, w: 360, h: 215, fill: "#FFF7ED", stroke: colors.orange, stereo: "<<JCache adapter>>", title: "SuspendNearJCache<K,V>", attrs: ["Caffeine + IMap"], methods: ["suspend facade", "factory uses withoutListener"] },
};

const edges = [
  { id: "near-impl", type: "implements", color: colors.blue, from: "near", to: "blockingOps", d: "M350 545 L350 345" },
  { id: "suspend-impl", type: "implements", color: colors.teal, from: "suspendNear", to: "suspendOps", d: "M940 545 L940 345" },
  { id: "caffeine-impl", type: "implements", color: colors.gray, from: "caffeine", to: "local", d: "M1860 260 L1715 260" },
  { id: "near-has-local", type: "has", color: colors.gray, from: "near", to: "local", d: "M600 655 L635 655 L635 155 L1260 155 L1260 260", label: { x: 900, y: 182, text: "front cache", w: 96 } },
  { id: "suspend-has-local", type: "has", color: colors.gray, from: "suspendNear", to: "local", d: "M1190 622 L1240 622 L1240 430 L1335 430 L1335 345", label: { x: 1240, y: 465, text: "front cache", w: 96 } },
  { id: "listener-to-front", type: "uses", color: colors.orange, from: "listener", to: "local", d: "M1525 545 L1525 345", label: { x: 1628, y: 455, text: "invalidate", w: 92 } },
  { id: "near-uses-config", type: "uses", color: colors.purple, from: "near", to: "config", d: "M600 700 L635 700 L635 1250 L2360 1250 L2360 640 L2315 640", label: { x: 1435, y: 1222, text: "configured by", w: 118 } },
  { id: "resilient-extends", type: "extends", color: colors.green, from: "resilient", to: "near", d: "M515 945 L515 765" },
  { id: "resilient-suspend-extends", type: "extends", color: colors.green, from: "resilientSuspend", to: "suspendNear", d: "M1160 945 L1160 765" },
  { id: "jcache-uses-local", type: "has", color: colors.orange, from: "nearJcache", to: "local", d: "M1725 945 L1725 800 L1690 800 L1690 370 L1645 370 L1645 345" },
  { id: "sjcache-uses-local", type: "has", color: colors.orange, from: "suspendJcache", to: "local", d: "M2155 945 L2155 880 L1800 880 L1800 325 L1715 325" },
];

function esc(v) { return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;"); }
function box(id) {
  const b = boxes[id], sep1 = b.y + 66, sep2 = b.y + 118;
  return `<g id="${id}"><rect class="umlBox" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/><line x1="${b.x}" y1="${sep1}" x2="${b.x + b.w}" y2="${sep1}" stroke="${b.stroke}"/><line x1="${b.x}" y1="${sep2}" x2="${b.x + b.w}" y2="${sep2}" stroke="${b.stroke}"/><text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 25}" text-anchor="middle">${esc(b.stereo)}</text><text class="classTitle" x="${b.x + b.w / 2}" y="${b.y + 52}" text-anchor="middle">${esc(b.title)}</text>${b.attrs.map((line, i) => `<text class="member" x="${b.x + 24}" y="${b.y + 91 + i * 20}">${esc(line)}</text>`).join("")}${b.methods.map((line, i) => `<text class="member" x="${b.x + 24}" y="${b.y + 145 + i * 20}">${esc(line)}</text>`).join("")}</g>`;
}
function nums(d) { return d.match(/-?\d+(?:\.\d+)?/g).map(Number); }
function arrowHead(e) {
  const n = nums(e.d), end = { x: n[n.length - 2], y: n[n.length - 1] }, prev = { x: n[n.length - 4], y: n[n.length - 3] };
  const dx = end.x - prev.x, dy = end.y - prev.y;
  if (e.type === "extends" || e.type === "implements") {
    if (Math.abs(dy) >= Math.abs(dx) && dy < 0) return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 8} ${end.y + 16} L${end.x + 8} ${end.y + 16} Z" fill="#fff" stroke="${e.color}"/>`;
    if (dx < 0) return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x + 16} ${end.y - 8} L${end.x + 16} ${end.y + 8} Z" fill="#fff" stroke="${e.color}"/>`;
    return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 16} ${end.y - 8} L${end.x - 16} ${end.y + 8} Z" fill="#fff" stroke="${e.color}"/>`;
  }
  if (dx < 0) return `<path class="solidOpenHead" d="M${end.x + 13} ${end.y - 7} L${end.x} ${end.y} L${end.x + 13} ${end.y + 7}" stroke="${e.color}"/>`;
  if (dx > 0) return `<path class="solidOpenHead" d="M${end.x - 13} ${end.y - 7} L${end.x} ${end.y} L${end.x - 13} ${end.y + 7}" stroke="${e.color}"/>`;
  if (dy < 0) return `<path class="solidOpenHead" d="M${end.x - 7} ${end.y + 13} L${end.x} ${end.y} L${end.x + 7} ${end.y + 13}" stroke="${e.color}"/>`;
  return `<path class="solidOpenHead" d="M${end.x - 7} ${end.y - 13} L${end.x} ${end.y} L${end.x + 7} ${end.y - 13}" stroke="${e.color}"/>`;
}
function label(l) { return `<g class="edgeLabel" transform="translate(${l.x - l.w / 2} ${l.y - 14})"><rect width="${l.w}" height="28" rx="8"/><text x="${l.w / 2}" y="19" text-anchor="middle">${esc(l.text)}</text></g>`; }

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Hazelcast NearCache class hierarchy">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/></filter><style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${colors.muted}}.section{fill:#F3F8FF;stroke:#94A3B8;stroke-width:1.7;stroke-dasharray:12 8}.sectionTitle{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.umlBox{filter:url(#softShadow);stroke-width:2}.stereo{font-family:"Comic Mono";font-size:12px;fill:${colors.muted}}.classTitle{font-family:"Architects Daughter";font-size:22px;fill:${colors.ink}}.member{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}.edge{fill:none;stroke-width:2.55;stroke-linecap:round;stroke-linejoin:round}.extends,.has{stroke-dasharray:none}.implements,.uses{stroke-dasharray:8 7}.solidHead{stroke-width:1.9;stroke-linejoin:round;stroke-dasharray:none}.solidOpenHead{fill:none;stroke-width:2.25;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:none}.edgeLabel rect{fill:#fff;stroke:${colors.line};stroke-width:1.2;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:11.8px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/>
<text class="title" x="74" y="86">Hazelcast NearCache Class Hierarchy</text><text class="subtitle" x="78" y="118">Hazelcast near-cache implementations share the same front-cache abstraction, IMap back store, listener invalidation, and write-through or write-behind variants.</text>
<rect class="section" x="62" y="145" width="2315" height="1155" rx="8"/><text class="sectionTitle" x="90" y="170">class relationships from cache/cache-hazelcast source; class diagrams intentionally omit icons</text>
<g id="edges">${edges.map((e) => `<path class="edge ${e.type}" d="${e.d}" stroke="${e.color}"/>`).join("\n")}</g><g id="arrowheads">${edges.map(arrowHead).join("\n")}</g><g id="labels">${edges.filter((e) => e.label).map((e) => label(e.label)).join("\n")}</g>${Object.keys(boxes).map(box).join("\n")}</svg>`;

writeFileSync(svgPath, `${svg.replace(/[ \t]+$/gm, "")}\n`);
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
