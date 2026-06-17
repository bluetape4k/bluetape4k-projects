#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/cache-cache-lettuce-diagram-03.svg";
const pngPath = "docs/images/readme-diagrams/cache-cache-lettuce-diagram-03.png";
const W = 2320;
const H = 980;

const files = {
  readme: "cache/cache-lettuce/README.md",
  scripts: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheScripts.kt",
  near: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt",
  suspendNear: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceSuspendNearCache.kt",
  jcache: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt",
  suspendManager: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendCacheManager.kt",
  suspendMemoizer: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceSuspendMemoizer.kt",
  asyncMemoizer: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceAsyncMemoizer.kt",
};
for (const f of Object.values(files)) if (!existsSync(f)) throw new Error(`Missing source evidence: ${f}`);
function need(file, pattern, label) {
  if (!pattern.test(readFileSync(file, "utf8"))) throw new Error(`Expected ${label} in ${file}`);
}
need(files.readme, /Performance \/ Stability Notes[\s\S]*cache-cache-lettuce-diagram-03\.png[\s\S]*EVALSHA[\s\S]*UNLINK[\s\S]*putAll/, "README stability section");
need(files.scripts, /EVALSHA[\s\S]*NOSCRIPT[\s\S]*COMPARE_AND_SET[\s\S]*RedisScript/, "shared CAS script");
need(files.near, /evalsha[\s\S]*RedisNoScriptException[\s\S]*commands\.unlink[\s\S]*registerTrackingKey/, "blocking stability paths");
need(files.suspendNear, /evalsha[\s\S]*RedisNoScriptException[\s\S]*commands\.unlink[\s\S]*CancellationException/, "suspend stability paths");
need(files.jcache, /close\(\)[\s\S]*clear\(\)[\s\S]*existingKeys[\s\S]*this\.map\.getAll/, "JCache close and batch existence check");
need(files.suspendManager, /withContext\(NonCancellable\)[\s\S]*cancellation\?\.let/, "non-cancellable suspend manager cleanup");
need(files.suspendMemoizer, /CompletableDeferred[\s\S]*CancellationException[\s\S]*inFlight\.remove\(key,\s*deferred\)/, "suspend memoizer recovery");
need(files.asyncMemoizer, /CompletableFuture[\s\S]*inFlight\.remove\(key,\s*promise\)/, "async memoizer race fix");

const C = {
  ink: "#0F172A", muted: "#475569", canvas: "#F8FAFC", frame: "#FFFFFF", line: "#CBD5E1",
  blue: ["#EFF6FF", "#2563EB"], teal: ["#F0FDFA", "#0D9488"], green: ["#F0FDF4", "#16A34A"],
  orange: ["#FFF7ED", "#EA580C"], pink: ["#FDF2F8", "#DB2777"], purple: ["#FAF5FF", "#7C3AED"],
  slate: ["#F8FAFC", "#64748B"],
};
const contracts = [
  ["CAS replace", "EVALSHA first", ["NearCacheScripts wraps Lua with SHA1", "NOSCRIPT falls back to full EVAL", "KEEPTTL preserves Redis expiry"], "blue"],
  ["Delete path", "UNLINK + SCAN", ["remove/removeAll avoid blocking DEL", "clearBack scans only cacheName:*", "front cache is cleared separately"], "teal"],
  ["JCache close", "resource close only", ["close() does not delete Redis hash data", "explicit clear() is required for data removal", "JSR-107 semantics stay intact"], "green"],
  ["Suspend cleanup", "NonCancellable close", ["manager closes remaining caches despite caller cancellation", "explicit cache CancellationException is rethrown after cleanup", "ordinary close failures are logged and skipped"], "orange"],
  ["Suspend memoizer", "failure is not cached", ["in-flight Deferred is removed in finally", "CancellationException is rethrown", "next call recomputes after failure"], "pink"],
  ["Async memoizer", "exact promise removal", ["inFlight.remove(key, promise)", "late completion cannot evict replacement promise", "single-flight remains per JVM key"], "purple"],
];
function esc(v) { return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;"); }
function card([title, subtitle, lines, color], i) {
  const [fill, stroke] = C[color];
  const col = i % 3, row = Math.floor(i / 3);
  const x = 95 + col * 735, y = 205 + row * 385;
  return `<g>
    <rect class="card" x="${x}" y="${y}" width="650" height="310" rx="8" fill="${fill}" stroke="${stroke}"/>
    <text class="contract" x="${x + 32}" y="${y + 56}">${esc(title)}</text>
    <text class="subtitle2" x="${x + 34}" y="${y + 92}">${esc(subtitle)}</text>
    <path d="M${x} ${y + 118}H${x + 650}" stroke="${stroke}" stroke-width="1.1" opacity=".55"/>
    ${lines.map((line, j) => `<text class="line" x="${x + 34}" y="${y + 166 + j * 44}">${esc(line)}</text>`).join("")}
  </g>`;
}
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Lettuce cache stability contracts">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity=".10"/></filter>
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:${C.canvas}}.frame{fill:${C.frame};stroke:${C.line};stroke-width:1.5;filter:url(#shadow)}
    .title,.contract{font-family:"Architects Daughter";fill:${C.ink}}.title{font-size:46px}.contract{font-size:31px}
    .subtitle,.subtitle2,.line{font-family:"Comic Mono";fill:${C.muted}}.subtitle{font-size:15.5px}.subtitle2{font-size:15px}.line{font-size:14.4px}
    .card{stroke-width:2;filter:url(#shadow)}
  </style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 64}" rx="8"/>
<text class="title" x="76" y="90">Lettuce Cache Stability Contracts</text>
<text class="subtitle" x="80" y="124">The runtime contracts that keep native near caches, JCache, and memoizers predictable under failure, cancellation, and Redis edge cases.</text>
${contracts.map(card).join("\n")}
</svg>`;
writeFileSync(svgPath, `${svg.replace(/[ \t]+$/gm, "")}\n`);
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
