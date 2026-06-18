#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-bucket4j-diagram-01";
const W = 1880;
const H = 1160;
const c = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  sync: "#2563EB",
  local: "#EA580C",
  coroutine: "#0D9488",
  distributed: "#9333EA",
  result: "#16A34A",
  bucket: "#64748B",
  slate: "#334155",
};

const sources = [
  "infra/bucket4j/README.md",
  "infra/bucket4j/README.ko.md",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ratelimit/RateLimiter.kt",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ratelimit/SuspendRateLimiter.kt",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ratelimit/RateLimitResult.kt",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ratelimit/local/LocalRateLimiter.kt",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ratelimit/local/LocalSuspendRateLimiter.kt",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ratelimit/distributed/DistributedRateLimiter.kt",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ratelimit/distributed/DistributedSuspendRateLimiter.kt",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/local/LocalBucketProvider.kt",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/local/LocalSuspendBucketProvider.kt",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/BucketProxyProvider.kt",
  "infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/AsyncBucketProxyProvider.kt",
];
for (const source of sources) {
  if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
}
if (!/Bucket4j Integration Class Diagram[\s\S]*infra-bucket4j-diagram-01\.png/.test(readFileSync(sources[0], "utf8"))) {
  throw new Error("README diagram slot not found");
}

const boxes = {
  rateLimiter: {
    x: 290, y: 150, w: 340, h: 150, fill: "#EFF6FF", stroke: c.sync,
    stereo: "interface", title: "RateLimiter<K>", members: ["+ consume(key, tokens)", "returns RateLimitResult"],
  },
  result: {
    x: 760, y: 130, w: 360, h: 170, fill: "#F0FDF4", stroke: c.result,
    stereo: "data contract", title: "RateLimitResult", members: ["status: CONSUMED / REJECTED / ERROR", "availableTokens, consumedTokens", "retryAfter + diagnostics"],
  },
  suspendRateLimiter: {
    x: 1270, y: 150, w: 390, h: 150, fill: "#ECFDF5", stroke: c.coroutine,
    stereo: "interface", title: "SuspendRateLimiter<K>", members: ["+ suspend consume(key, tokens)", "propagates cancellation"],
  },
  localRateLimiter: {
    x: 80, y: 370, w: 340, h: 164, fill: "#FFF7ED", stroke: c.local,
    stereo: "class", title: "LocalRateLimiter", members: ["- LocalBucketProvider", "+ immediate tryConsume", "+ single-probe result"],
  },
  distributedRateLimiter: {
    x: 470, y: 370, w: 380, h: 164, fill: "#EFF6FF", stroke: c.sync,
    stereo: "class", title: "DistributedRateLimiter", members: ["- BucketProxyProvider", "+ Redis-backed proxy", "+ no extra token read"],
  },
  localSuspendRateLimiter: {
    x: 890, y: 370, w: 390, h: 164, fill: "#ECFDF5", stroke: c.coroutine,
    stereo: "class", title: "LocalSuspendRateLimiter", members: ["- LocalSuspendBucketProvider", "+ coroutine-friendly local bucket", "+ cancellation passes through"],
  },
  distributedSuspendRateLimiter: {
    x: 1325, y: 370, w: 430, h: 186, fill: "#FAF5FF", stroke: c.distributed,
    stereo: "class", title: "DistributedSuspendRateLimiter", members: ["- AsyncBucketProxyProvider", "+ await async proxy result", "+ optional operation timeout", "+ cancellation passes through"],
  },
  localBucketProvider: {
    x: 80, y: 650, w: 340, h: 170, fill: "#FFF7ED", stroke: c.local,
    stereo: "provider", title: "LocalBucketProvider", members: ["extends AbstractLocalBucketProvider", "key-prefix cache", "lock-free local bucket"],
  },
  bucketProxyProvider: {
    x: 470, y: 650, w: 380, h: 170, fill: "#EFF6FF", stroke: c.sync,
    stereo: "provider", title: "BucketProxyProvider", members: ["ProxyManager<ByteArray>", "prefix + UTF-8 key", "builds remote proxy only"],
  },
  localSuspendBucketProvider: {
    x: 890, y: 650, w: 390, h: 170, fill: "#ECFDF5", stroke: c.coroutine,
    stereo: "provider", title: "LocalSuspendBucketProvider", members: ["extends AbstractLocalBucketProvider", "SuspendLocalBucket per key", "64-bit math + millis time"],
  },
  asyncBucketProxyProvider: {
    x: 1325, y: 650, w: 430, h: 170, fill: "#FAF5FF", stroke: c.distributed,
    stereo: "provider", title: "AsyncBucketProxyProvider", members: ["AsyncProxyManager<ByteArray>", "prefix + UTF-8 key", "builds async proxy only"],
  },
  localBucket: {
    x: 80, y: 920, w: 340, h: 122, fill: "#F8FAFC", stroke: c.bucket,
    stereo: "Bucket4j primitive", title: "LocalBucket", members: ["tryConsumeAndReturnRemaining()"],
  },
  bucketProxy: {
    x: 470, y: 920, w: 380, h: 122, fill: "#F8FAFC", stroke: c.bucket,
    stereo: "Bucket4j primitive", title: "BucketProxy", members: ["remote tryConsume probe"],
  },
  suspendLocalBucket: {
    x: 890, y: 920, w: 390, h: 122, fill: "#F8FAFC", stroke: c.bucket,
    stereo: "bluetape4k primitive", title: "SuspendLocalBucket", members: ["delay-based waiting helpers"],
  },
  asyncBucketProxy: {
    x: 1325, y: 920, w: 430, h: 122, fill: "#F8FAFC", stroke: c.bucket,
    stereo: "Bucket4j primitive", title: "AsyncBucketProxy", members: ["CompletableFuture probe"],
  },
};

const edges = [
  { cls: "realize", d: "M 250 370 V 328 H 410 V 300", color: c.sync, marker: "hollowBlue", label: null },
  { cls: "realize", d: "M 660 370 V 328 H 510 V 300", color: c.sync, marker: "hollowBlue", label: null },
  { cls: "realize", d: "M 1085 370 V 328 H 1378 V 300", color: c.coroutine, marker: "hollowTeal", label: null },
  { cls: "realize", d: "M 1540 370 V 300", color: c.coroutine, marker: "hollowTeal", label: null },
  { cls: "assoc", d: "M 630 215 H 760", color: c.result, marker: "openGreen", label: ["returns", 695, 203] },
  { cls: "assoc", d: "M 1270 215 H 1120", color: c.result, marker: "openGreen", label: ["returns", 1195, 203] },
  { cls: "assoc", d: "M 250 534 V 650", color: c.local, marker: "openOrange", label: ["uses", 286, 592] },
  { cls: "assoc", d: "M 660 534 V 650", color: c.sync, marker: "openBlue", label: ["uses", 696, 592] },
  { cls: "assoc", d: "M 1085 534 V 650", color: c.coroutine, marker: "openTeal", label: ["uses", 1125, 592] },
  { cls: "assoc", d: "M 1540 556 V 650", color: c.distributed, marker: "openPurple", label: ["uses", 1582, 610] },
  { cls: "uses", d: "M 250 820 V 920", color: c.local, marker: "openOrange", label: ["resolves", 292, 874] },
  { cls: "uses", d: "M 660 820 V 920", color: c.sync, marker: "openBlue", label: ["resolves", 704, 874] },
  { cls: "uses", d: "M 1085 820 V 920", color: c.coroutine, marker: "openTeal", label: ["resolves", 1130, 874] },
  { cls: "uses", d: "M 1540 820 V 920", color: c.distributed, marker: "openPurple", label: ["resolves", 1588, 874] },
];

function esc(value) {
  return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function box(id) {
  const b = boxes[id];
  const dividerY = b.y + 76;
  const memberStart = dividerY + 27;
  return `<g id="${id}">
  <rect class="card" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/>
  <text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 28}" text-anchor="middle">${esc(b.stereo)}</text>
  <text class="cardTitle" x="${b.x + b.w / 2}" y="${b.y + 58}" text-anchor="middle">${esc(b.title)}</text>
  <line class="divider" x1="${b.x + 24}" y1="${dividerY}" x2="${b.x + b.w - 24}" y2="${dividerY}"/>
  ${b.members.map((member, index) => `<text class="member" x="${b.x + 28}" y="${memberStart + index * 22}">${esc(member)}</text>`).join("")}
</g>`;
}

function label(item) {
  if (!item) return "";
  const [text, x, y] = item;
  return `<text class="label" x="${x}" y="${y}" text-anchor="middle">${esc(text)}</text>`;
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Bucket4j integration class diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="118%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity=".11"/></filter>
  <marker id="openBlue" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.sync}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openOrange" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.local}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openTeal" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.coroutine}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openPurple" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.distributed}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openGreen" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.result}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="hollowBlue" viewBox="0 0 18 16" markerWidth="16" markerHeight="14" refX="16" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 16 8 L 1 15 Z" fill="${c.frame}" stroke="${c.sync}" stroke-width="2" stroke-dasharray="none"/></marker>
  <marker id="hollowTeal" viewBox="0 0 18 16" markerWidth="16" markerHeight="14" refX="16" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 16 8 L 1 15 Z" fill="${c.frame}" stroke="${c.coroutine}" stroke-width="2" stroke-dasharray="none"/></marker>
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.6;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:${c.ink}}.subtitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}
    .card{filter:url(#shadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:21px;fill:${c.ink}}
    .stereo{font-family:"Comic Mono";font-size:12px;fill:#64748B}.member{font-family:"Comic Mono";font-size:12.3px;fill:${c.slate}}
    .divider{stroke:rgba(15,23,42,.17);stroke-width:1.2}.assoc{fill:none;stroke-width:3.0;stroke-linecap:round;stroke-linejoin:round}
    .uses{fill:none;stroke-width:2.5;stroke-dasharray:8 7;stroke-linecap:round;stroke-linejoin:round}
    .realize{fill:none;stroke-width:2.4;stroke-dasharray:8 7;stroke-linecap:round;stroke-linejoin:round}
    .label{font-family:"Comic Mono";font-size:11.6px;fill:${c.slate};paint-order:stroke;stroke:${c.frame};stroke-width:5px;stroke-linejoin:round}
    .legend{font-family:"Comic Mono";font-size:12px;fill:${c.muted}}
  </style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="10"/>
<text class="title" x="72" y="84">Bucket4j Integration Class Diagram</text>
<text class="subtitle" x="76" y="116">Facade APIs validate requests, resolve key-based buckets, and return a stable bluetape4k result contract.</text>
<g>
${edges.map((e) => `  <path class="${e.cls}" d="${e.d}" stroke="${e.color}" marker-end="url(#${e.marker})"/>`).join("\n")}
</g>
<g>${edges.map((e) => label(e.label)).join("")}</g>
${Object.keys(boxes).map(box).join("")}
<g transform="translate(80 1096)">
  <path class="assoc" d="M 0 0 H 58" stroke="${c.sync}" marker-end="url(#openBlue)"/><text class="legend" x="72" y="5">has/reference</text>
  <path class="uses" d="M 214 0 H 272" stroke="${c.distributed}" marker-end="url(#openPurple)"/><text class="legend" x="286" y="5">uses / resolves</text>
  <path class="realize" d="M 455 0 H 513" stroke="${c.sync}" marker-end="url(#hollowBlue)"/><text class="legend" x="528" y="5">implements</text>
</g>
</svg>`;

writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
