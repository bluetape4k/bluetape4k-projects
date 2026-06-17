#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/cache-cache-hazelcast-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/cache-cache-hazelcast-diagram-02.png";
const W = 2180;
const H = 1220;
const colors = {
  ink: "#0F172A", muted: "#475569", canvas: "#F8FAFC", frame: "#FFFFFF", line: "#CBD5E1",
  blue: "#2563EB", green: "#16A34A", teal: "#0D9488", orange: "#EA580C", purple: "#7C3AED", gray: "#64748B",
};
const tones = {
  call: { fill: "#EFF6FF", stroke: colors.blue, dark: "#1D4ED8" },
  front: { fill: "#F0FDF4", stroke: colors.green, dark: "#15803D" },
  back: { fill: "#F0FDFA", stroke: colors.teal, dark: "#0F766E" },
  event: { fill: "#FFF7ED", stroke: colors.orange, dark: "#C2410C" },
  option: { fill: "#F5F3FF", stroke: colors.purple, dark: "#6D28D9" },
  note: { fill: "#F8FAFC", stroke: colors.gray, dark: "#475569" },
};

const sources = [
  "cache/cache-hazelcast/README.md",
  "cache/cache-hazelcast/README.ko.md",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCache.kt",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastSuspendNearCache.kt",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastEntryEventListener.kt",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastLocalCache.kt",
  "cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCache.kt",
];
for (const source of sources) if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
if (!/2-Tier NearCache Flow/.test(readFileSync(sources[0], "utf8"))) throw new Error("README flow section not found");
if (!/HazelcastNearCache Runtime Flow[\s\S]*cache-cache-hazelcast-diagram-02\.png/.test(readFileSync(sources[1], "utf8"))) {
  throw new Error("Korean README diagram slot not found");
}

const cards = {
  readCall: { x: 110, y: 240, w: 330, h: 160, tone: "call", kicker: "1 read call", title: "get(key)", lines: ["HazelcastNearCache", "or suspend get(key)", "key is validated"], foot: "entrypoint" },
  frontCheck: { x: 555, y: 240, w: 380, h: 160, tone: "front", kicker: "2 local check", title: "Caffeine front", lines: ["frontCache.get(key)", "hit returns immediately", "miss continues to IMap"], foot: "no network on hit" },
  backRead: { x: 1050, y: 240, w: 380, h: 160, tone: "back", kicker: "3 miss path", title: "Hazelcast IMap", lines: ["get / getAsync", "back hit/miss stats", "source of truth"], foot: "read-through backend" },
  populate: { x: 1545, y: 240, w: 390, h: 160, tone: "front", kicker: "4 refill", title: "frontCache.put", lines: ["store remote value locally", "return value to caller", "future reads hit front"], foot: "populate only when value exists" },

  writeCall: { x: 110, y: 595, w: 330, h: 160, tone: "call", kicker: "1 write call", title: "put/remove", lines: ["put / putAll", "remove / replace", "clearAll"], foot: "same sync/suspend surface" },
  frontUpdate: { x: 555, y: 595, w: 380, h: 160, tone: "front", kicker: "2 local update", title: "front first", lines: ["put/remove local entry", "clearLocal affects front only", "local member events ignored"], foot: "keeps caller-visible state current" },
  backWrite: { x: 1050, y: 595, w: 380, h: 160, tone: "back", kicker: "3 remote write", title: "IMap mutation", lines: ["set / delete", "putAll bulk write", "clearAll clears IMap"], foot: "write-through backend" },
  listener: { x: 1545, y: 595, w: 390, h: 160, tone: "event", kicker: "4 peer coherence", title: "EntryListener", lines: ["remote update/remove invalidates", "expiry invalidates local key", "ADD is ignored"], foot: "prevents stale peer front cache" },

  resilient: { x: 270, y: 910, w: 470, h: 175, tone: "event", kicker: "write-behind variant", title: "Resilient near-cache", lines: ["front updates immediately", "queue/channel writes IMap later", "retry + stale-read guards"], foot: "used when remote writes may fail" },
  jcache: { x: 870, y: 910, w: 470, h: 175, tone: "option", kicker: "factory caveat", title: "NearJCache mode", lines: ["same front/back shape", "listener-free factory path", "no peer propagation guarantee"], foot: "native NearCache is the coherent path" },
  close: { x: 1470, y: 910, w: 390, h: 175, tone: "note", kicker: "lifecycle", title: "close()", lines: ["remove IMap listener", "close front cache", "release local resources"], foot: "cleanup is explicit" },
};

const edges = [
  { points: [[440, 320], [555, 320]], tone: "call", label: "calls", labelAt: [498, 292] },
  { points: [[935, 320], [1050, 320]], tone: "back", label: "miss", labelAt: [992, 292] },
  { points: [[1430, 320], [1545, 320]], tone: "front", label: "refill", labelAt: [1488, 292] },

  { points: [[440, 675], [555, 675]], tone: "call", label: "writes", labelAt: [498, 647] },
  { points: [[935, 675], [1050, 675]], tone: "front", label: "front update", labelAt: [992, 647] },
  { points: [[1430, 675], [1545, 675]], tone: "event", label: "remote event", labelAt: [1488, 647] },

  { points: [[1740, 595], [1740, 475], [735, 475], [735, 400]], tone: "event", dashed: true, label: "invalidate peer front", labelAt: [1238, 447] },
  { points: [[275, 755], [275, 850], [505, 850], [505, 910]], tone: "event", label: "optional", labelAt: [390, 822] },
  { points: [[1240, 755], [1240, 850], [1105, 850], [1105, 910]], tone: "option", dashed: true, label: "factory", labelAt: [1170, 822] },
  { points: [[1740, 755], [1740, 910]], tone: "note", label: "cleanup", labelAt: [1785, 835] },
];

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function markerDefs() {
  return Object.entries(tones).map(([name, p]) =>
    `<marker id="arrow-${name}" markerWidth="22" markerHeight="16" refX="20" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M2 2 L20 8 L2 14 Z" fill="${p.dark}" stroke="${p.dark}" stroke-dasharray="none"/></marker>`
  ).join("\n");
}

function card(id) {
  const c = cards[id], p = tones[c.tone];
  return `<g id="${id}"><rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${p.fill}" stroke="${p.stroke}"/><text class="kicker" x="${c.x + 22}" y="${c.y + 28}">${esc(c.kicker)}</text><text class="cardTitle" x="${c.x + 22}" y="${c.y + 60}">${esc(c.title)}</text><path class="divider" d="M${c.x} ${c.y + 78}H${c.x + c.w}" stroke="${p.dark}"/>${c.lines.map((line, i) => `<text class="body" x="${c.x + 22}" y="${c.y + 106 + i * 21}">${esc(line)}</text>`).join("")}</g>`;
}

function pathD(points) {
  return points.map((point, i) => `${i ? "L" : "M"}${point[0]} ${point[1]}`).join(" ");
}

function edge(e) {
  const p = tones[e.tone];
  const w = Math.max(82, e.label.length * 8 + 24);
  return `<g><path class="edge ${e.dashed ? "dashed" : ""}" d="${pathD(e.points)}" stroke="${p.dark}" marker-end="url(#arrow-${e.tone})"/><g class="edgeLabel" transform="translate(${e.labelAt[0] - w / 2} ${e.labelAt[1] - 14})"><rect width="${w}" height="28" rx="8"/><text x="${w / 2}" y="19" text-anchor="middle">${esc(e.label)}</text></g></g>`;
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Hazelcast near-cache read write and invalidation flow">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/></filter>${markerDefs()}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:45px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:15px;fill:${colors.muted}}.lane{fill:#fff;stroke:#CBD5E1;stroke-width:1.45;stroke-dasharray:12 8}.laneTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.laneText{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}.card{stroke-width:1.9;filter:url(#softShadow)}.kicker{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}.cardTitle{font-family:"Architects Daughter";font-size:23px;fill:${colors.ink}}.body{font-family:"Comic Mono";font-size:12.5px;fill:#334155}.foot{font-family:"Comic Mono";font-size:11.8px;fill:${colors.muted}}.divider{stroke-width:1.1;opacity:.42}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:10 8}.edgeLabel rect{fill:#fff;stroke:#CBD5E1;stroke-width:1.15;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/>
<text class="title" x="72" y="86">Hazelcast NearCache Runtime Flow</text><text class="subtitle" x="76" y="119">Two ordinary lanes explain the default behavior: reads are read-through, writes are write-through, and remote IMap events invalidate peer front caches.</text>
<rect class="lane" x="65" y="165" width="2050" height="285" rx="8"/><text class="laneTitle" x="92" y="198">read-through lane</text><text class="laneText" x="92" y="222">front hit returns immediately; front miss reads IMap and refills the local Caffeine tier</text>
<rect class="lane" x="65" y="520" width="2050" height="285" rx="8"/><text class="laneTitle" x="92" y="553">write-through and coherence lane</text><text class="laneText" x="92" y="577">writes update local and remote tiers; remote member changes travel back through the EntryListener</text>
<rect class="lane" x="65" y="850" width="2050" height="250" rx="8"/><text class="laneTitle" x="92" y="883">side variants and lifecycle</text>
<g id="edges">${edges.map(edge).join("\n")}</g>
<g id="cards">${Object.keys(cards).map(card).join("\n")}</g>
</svg>`;

writeFileSync(svgPath, `${svg.replace(/[ \t]+$/gm, "")}\n`);
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
