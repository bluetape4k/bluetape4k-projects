#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/cache-cache-lettuce-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/cache-cache-lettuce-diagram-02.png";
const W = 2300, H = 1080;
const files = {
  readme: "cache/cache-lettuce/README.ko.md",
  factories: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/LettuceCaches.kt",
  jcache: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt",
  suspendJCache: "cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCache.kt",
  nearJCache: "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt",
  suspendNearJCache: "cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCache.kt",
};
for (const f of Object.values(files)) if (!existsSync(f)) throw new Error(`Missing source evidence: ${f}`);
function need(file, pattern, label) { if (!pattern.test(readFileSync(file, "utf8"))) throw new Error(`Expected ${label} in ${file}`); }
need(files.readme, /JCache 기반 NearCache[\s\S]*cache-cache-lettuce-diagram-02\.png/, "README ko diagram slot");
need(files.factories, /nearJCache\([\s\S]*val backCache = jcache[\s\S]*SuspendNearJCache[\s\S]*CaffeineSuspendJCache/, "Lettuce factory wiring");
need(files.jcache, /class LettuceJCache[\s\S]*LettuceMap[\s\S]*dispatchEvent/, "Lettuce JCache back cache");
need(files.suspendJCache, /class LettuceSuspendJCache[\s\S]*SuspendJCache/, "Suspend JCache adapter");
need(files.nearJCache, /class NearJCache[\s\S]*frontCache[\s\S]*backCache[\s\S]*getDeeply/, "NearJCache two-tier behavior");
need(files.suspendNearJCache, /class SuspendNearJCache[\s\S]*frontCache[\s\S]*backCache[\s\S]*withoutListener/, "SuspendNearJCache two-tier behavior");

const C = { ink:"#0F172A", muted:"#475569", canvas:"#F8FAFC", frame:"#FFFFFF", line:"#CBD5E1",
  blue:["#EFF6FF","#2563EB","#1D4ED8"], teal:["#F0FDFA","#0D9488","#0F766E"], green:["#F0FDF4","#16A34A","#15803D"], orange:["#FFF7ED","#EA580C","#C2410C"], pink:["#FDF2F8","#DB2777","#BE185D"], slate:["#F8FAFC","#64748B","#475569"] };
function esc(v){return String(v).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;");}
function defs(){return Object.entries(C).filter(([,v])=>Array.isArray(v)).map(([n,[,,d]])=>`<marker id="open-${n}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 19 11 L 2 20" fill="none" stroke="${d}" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"/></marker>`).join("");}
function card({id,x,y,w,h,c,title,sub,lines}){const [f,s]=C[c];return `<g id="${id}"><rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${f}" stroke="${s}"/><text class="cardTitle" x="${x+28}" y="${y+48}">${esc(title)}</text><text class="sub" x="${x+34}" y="${y+78}">${esc(sub)}</text><path d="M${x} ${y+98}H${x+w}" stroke="${s}" opacity=".5"/>${lines.map((l,i)=>`<text class="line" x="${x+34}" y="${y+134+i*30}">${esc(l)}</text>`).join("")}</g>`}
function edge(points,c,label,at,from,to){const [,,d]=C[c];const path=points.map((p,i)=>`${i?"L":"M"}${p[0]} ${p[1]}`).join(" ");const q=at??points[Math.floor(points.length/2)];return `<g data-from="${from}" data-to="${to}"><path class="edge dashed" d="${path}" stroke="${d}" marker-end="url(#open-${c})"/><text class="edgeLabel" x="${q[0]+8}" y="${q[1]-8}">${esc(label)}</text></g>`}
const body=[
  `<metadata data-allow-grid="true"/>`,
  card({id:"Factory",x:95,y:215,w:620,h:245,c:"green",title:"LettuceCaches factory",sub:"module entrypoint",lines:["nearJCache(): LettuceJCache back cache", "suspendNearJCache(): Caffeine front + LettuceSuspendJCache back", "codec defaults to LettuceBinaryCodecs.default()"]}),
  card({id:"NearJCache",x:850,y:200,w:620,h:270,c:"blue",title:"NearJCache<K,V>",sub:"cache-core blocking two-tier type",lines:["implements JCache<K,V> by backCache", "get() reads local front only", "getDeeply() reads back and fills front", "put/remove/replace update front then back"]}),
  card({id:"SuspendNearJCache",x:1600,y:200,w:600,h:270,c:"teal",title:"SuspendNearJCache<K,V>",sub:"cache-core coroutine two-tier type",lines:["implements SuspendJCache<K,V> by backCache", "get() delegates to getDeeply()", "Flow reads expose local front entries", "withoutListener() handles listener-free backends"]}),
  card({id:"Config",x:185,y:710,w:580,h:255,c:"orange",title:"NearJCacheConfig DSL",sub:"configuration boundary",lines:["cacheName identifies Redis hash/front cache", "frontCacheConfiguration defaults to Caffeine", "isSynchronous + syncRemoteTimeout tune event delivery"]}),
  card({id:"Back",x:915,y:710,w:560,h:255,c:"slate",title:"LettuceJCache back",sub:"Redis hash storage",lines:["hset/hget/hdel through LettuceMap", "dispatches CREATED/UPDATED/REMOVED events", "close() releases resources without deleting data"]}),
  card({id:"Front",x:1620,y:710,w:520,h:255,c:"pink",title:"Local front cache",sub:"Caffeine-backed local tier",lines:["fast local reads", "back-cache listener propagates peer changes", "clearLocal() keeps Redis data intact"]}),
  edge([[715,335],[850,335]],"blue","creates blocking",[735,318],"Factory","NearJCache"),
  edge([[405,215],[405,194],[419,180],[1886,180],[1900,194],[1900,200]],"teal","creates coroutine",[1130,187],"Factory","SuspendNearJCache"),
  edge([[1160,470],[1160,710]],"slate","delegates to back",null,"NearJCache","Back"),
  edge([[1900,470],[1900,710]],"pink","uses front",null,"SuspendNearJCache","Front"),
  edge([[475,460],[475,710]],"orange","DSL config",null,"Factory","Config"),
  edge([[1475,835],[1620,835]],"pink","events fill/evict",null,"Back","Front"),
];
const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Lettuce JCache near cache structure"><defs><filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity=".10"/></filter>${defs()}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${C.canvas}}.frame{fill:${C.frame};stroke:${C.line};stroke-width:1.5;filter:url(#shadow)}.title,.cardTitle{font-family:"Architects Daughter";fill:${C.ink}}.title{font-size:46px}.cardTitle{font-size:28px}.subtitle,.sub,.line,.edgeLabel{font-family:"Comic Mono";fill:${C.muted}}.subtitle{font-size:15.5px}.sub,.edgeLabel{font-size:13.5px}.line{font-size:14px}.card{stroke-width:2;filter:url(#shadow)}.edge{fill:none;stroke-width:3.4;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}</style></defs><rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W-68}" height="${H-64}" rx="8"/><text class="title" x="76" y="90">Lettuce JCache NearCache Structure</text><text class="subtitle" x="80" y="124">How cache-lettuce wires cache-core JCache near-cache abstractions to Redis-backed LettuceJCache and a local Caffeine front cache.</text>${body.join("\n")}</svg>`;
writeFileSync(svgPath, `${svg.replace(/[ \t]+$/gm, "")}\n`);
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`,[svgPath,"-o",pngPath,"-s","2"],{stdio:"inherit"});
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
