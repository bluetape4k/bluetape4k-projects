#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-jwt-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/utils-jwt-diagram-02.png";
const W = 2600;
const H = 1380;
const colors = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  blue: "#2563EB",
  teal: "#0D9488",
  orange: "#EA580C",
  purple: "#7C3AED",
  green: "#16A34A",
  rose: "#E11D48",
  amber: "#D97706",
  gray: "#64748B",
};

const evidence = [
  "utils/jwt/README.md",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/JwtProvider.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/AbstractJwtProvider.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/DefaultJwtProvider.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/FixedJwtProvider.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/cache/JCacheJwtProvider.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/cache/RedissonJwtProvider.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/KeyChainRepository.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/AbstractKeyChainRepository.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/inmemory/InMemoryKeyChainRepository.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/redis/RedisKeyChainRepository.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/KeyChain.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/composer/JwtComposer.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/composer/JwtComposerDsl.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/reader/JwtReader.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/reader/JwtReaderDto.kt",
];

for (const file of evidence) {
  if (!existsSync(file)) throw new Error(`Missing source evidence: ${file}`);
}

const readme = readFileSync("utils/jwt/README.md", "utf8");
if (!/Class Diagram[\s\S]*utils-jwt-diagram-02\.png/.test(readme)) {
  throw new Error("README Class Diagram slot not found");
}

const boxes = {
  provider: { x: 100, y: 180, w: 460, h: 195, fill: "#EFF6FF", stroke: colors.blue, stereotype: "<<interface>>", title: "JwtProvider", attrs: ["+ signatureAlgorithm"], methods: ["+ composer(), compose()", "+ parse(), tryParse()", "+ rotate(), forcedRotate()", "+ findKeyChain(kid)"] },
  abstractProvider: { x: 100, y: 480, w: 460, h: 160, fill: "#F8FAFC", stroke: colors.blue, stereotype: "<<abstract>>", title: "AbstractJwtProvider", attrs: ["# lock: ReentrantLock"], methods: ["+ composer(keyChain?)", "+ compose(keyChain?, builder)"] },
  defaultProvider: { x: 70, y: 760, w: 420, h: 175, fill: "#EFF6FF", stroke: colors.blue, stereotype: "<<class>>", title: "DefaultJwtProvider", attrs: ["- repository: KeyChainRepository", "- timer: Timer"], methods: ["+ rotate()", "+ findKeyChain(kid)"] },
  fixedProvider: { x: 535, y: 760, w: 395, h: 175, fill: "#EFF6FF", stroke: colors.blue, stereotype: "<<class>>", title: "FixedJwtProvider", attrs: ["- current: KeyChain"], methods: ["+ currentKeyChain()", "+ rotate(): unsupported"] },
  jcacheProvider: { x: 70, y: 1025, w: 420, h: 175, fill: "#F5F3FF", stroke: colors.purple, stereotype: "<<decorator>>", title: "JCacheJwtProvider", attrs: ["- delegate: JwtProvider", "- cache: Cache<String, JwtReaderDto>"], methods: ["+ tryParse(jwtString)"] },
  redissonProvider: { x: 535, y: 1025, w: 395, h: 175, fill: "#F5F3FF", stroke: colors.purple, stereotype: "<<decorator>>", title: "RedissonJwtProvider", attrs: ["- delegate: JwtProvider", "- cache: RMapCache<...>"], methods: ["+ tryParse(jwtString)"] },

  repository: { x: 1840, y: 180, w: 500, h: 195, fill: "#FFF7ED", stroke: colors.orange, stereotype: "<<interface>>", title: "KeyChainRepository", attrs: ["+ capacity"], methods: ["+ current()", "+ findOrNull(kid)", "+ rotate(keyChain)", "+ forcedRotate(keyChain)"] },
  abstractRepository: { x: 1840, y: 480, w: 500, h: 175, fill: "#F8FAFC", stroke: colors.orange, stereotype: "<<abstract>>", title: "AbstractKeyChainRepository", attrs: ["# cachedCurrent: KeyChain?", "- timer: Timer"], methods: ["# doLoadCurrent()", "# changeCurrent(keyChain)"] },
  inMemoryRepository: { x: 1580, y: 760, w: 450, h: 175, fill: "#FFF7ED", stroke: colors.orange, stereotype: "<<class>>", title: "InMemoryKeyChainRepository", attrs: ["- keyChainStore: ConcurrentLinkedDeque"], methods: ["+ rotate()", "+ forcedRotate()"] },
  redisRepository: { x: 2090, y: 760, w: 440, h: 175, fill: "#FFF7ED", stroke: colors.orange, stereotype: "<<class>>", title: "RedisKeyChainRepository", attrs: ["- keyChainStore: RDeque<KeyChainDto>"], methods: ["+ rotate()", "+ forcedRotate()"] },

  factory: { x: 750, y: 180, w: 455, h: 175, fill: "#F0FDFA", stroke: colors.teal, stereotype: "<<object>>", title: "JwtProviderFactory", attrs: ["default(), fixed()"], methods: ["jcached(), redissonCached()"] },
  keyChain: { x: 1115, y: 760, w: 330, h: 175, fill: "#FFFBEB", stroke: colors.amber, stereotype: "<<value object>>", title: "KeyChain", attrs: ["+ algorithm", "+ keyPair", "+ id, createdAt"], methods: ["+ isExpired", "+ expiredAt"] },
  composer: { x: 1115, y: 1025, w: 370, h: 175, fill: "#ECFDF5", stroke: colors.green, stereotype: "<<class>>", title: "JwtComposer", attrs: ["- keyChain: KeyChain", "- headers, claims"], methods: ["+ header(), claim()", "+ compose()"] },
  dsl: { x: 1540, y: 1025, w: 360, h: 175, fill: "#ECFDF5", stroke: colors.green, stereotype: "<<DSL class>>", title: "JwtComposerDsl", attrs: ["- composer: JwtComposer", "+ exp / nbf / iat props"], methods: ["+ composeJwt(...)", "+ compose()"] },
  reader: { x: 1985, y: 1025, w: 350, h: 175, fill: "#F0FDF4", stroke: colors.green, stereotype: "<<class>>", title: "JwtReader", attrs: ["- jws: Jws<Claims>"], methods: ["+ header<T>(key)", "+ claim<T>(name)", "+ isExpired"] },
};

const edges = [
  { id: "abstract-implements-provider", type: "implements", color: colors.blue, from: "abstractProvider", to: "provider", d: "M330 480 L330 375" },
  { id: "default-extends-abstract", type: "extends", color: colors.blue, from: "defaultProvider", to: "abstractProvider", d: "M280 760 L280 700 L330 700 L330 640" },
  { id: "fixed-extends-abstract", type: "extends", color: colors.blue, from: "fixedProvider", to: "abstractProvider", d: "M732 760 L732 680 L575 680 L575 560 L560 560" },
  { id: "jcache-extends-abstract", type: "extends", color: colors.purple, from: "jcacheProvider", to: "abstractProvider", d: "M280 1025 L280 960 L515 960 L515 640" },
  { id: "redisson-extends-abstract", type: "extends", color: colors.purple, from: "redissonProvider", to: "abstractProvider", d: "M732 1025 L732 965 L995 965 L995 560 L560 560" },
  { id: "jcache-delegates-provider", type: "uses", color: colors.purple, from: "jcacheProvider", to: "provider", d: "M70 1112 L55 1112 L55 278 L100 278", label: { x: 128, y: 675, text: "decorators delegate", w: 142 } },

  { id: "abstractrepo-implements-repo", type: "implements", color: colors.orange, from: "abstractRepository", to: "repository", d: "M2090 480 L2090 375" },
  { id: "inmemory-extends-abstractrepo", type: "extends", color: colors.orange, from: "inMemoryRepository", to: "abstractRepository", d: "M1805 760 L1805 705 L2090 705 L2090 655" },
  { id: "redis-extends-abstractrepo", type: "extends", color: colors.orange, from: "redisRepository", to: "abstractRepository", d: "M2310 760 L2310 705 L2090 705 L2090 655" },

  { id: "factory-creates-provider", type: "uses", color: colors.teal, from: "factory", to: "provider", d: "M750 267 L560 267", label: { x: 654, y: 239, text: "creates", w: 78 } },
  { id: "dsl-wraps-composer", type: "has", color: colors.green, from: "dsl", to: "composer", d: "M1540 1112 L1485 1112", label: { x: 1512, y: 1084, text: "wraps", w: 70 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function box(id) {
  const b = boxes[id];
  const nameSepY = b.y + 66;
  const attrRows = Math.max(1, b.attrs.length);
  const attrHeight = Math.max(38, attrRows * 20 + 18);
  const methodSepY = b.attrs.length ? Math.min(b.y + b.h - 42, nameSepY + attrHeight) : nameSepY;
  const attrBlockHeight = methodSepY - nameSepY;
  const methodBlockHeight = b.y + b.h - methodSepY;
  const attrY = b.attrs.length ? nameSepY + (attrBlockHeight - b.attrs.length * 20) / 2 + 15 : nameSepY + 24;
  const methodY = methodSepY + (methodBlockHeight - b.methods.length * 20) / 2 + 15;
  const methodSep = b.attrs.length
    ? `<line x1="${b.x}" y1="${methodSepY}" x2="${b.x + b.w}" y2="${methodSepY}" stroke="${b.stroke}" stroke-width="1.3" opacity="0.65"/>`
    : "";
  return `<g id="${id}">
  <rect class="umlBox" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/>
  <line x1="${b.x}" y1="${nameSepY}" x2="${b.x + b.w}" y2="${nameSepY}" stroke="${b.stroke}" stroke-width="1.3" opacity="0.65"/>
  ${methodSep}
  <text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 25}" text-anchor="middle">${esc(b.stereotype)}</text>
  <text class="classTitle" x="${b.x + b.w / 2}" y="${b.y + 52}" text-anchor="middle">${esc(b.title)}</text>
  ${b.attrs.map((line, i) => `<text class="member" x="${b.x + 24}" y="${attrY + i * 20}">${esc(line)}</text>`).join("\n  ")}
  ${b.methods.map((line, i) => `<text class="member" x="${b.x + 24}" y="${methodY + i * 20}">${esc(line)}</text>`).join("\n  ")}
</g>`;
}

function label({ x, y, text, w }) {
  return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 14})"><rect width="${w}" height="28" rx="8"/><text x="${w / 2}" y="19" text-anchor="middle">${esc(text)}</text></g>`;
}

function nums(d) {
  return d.match(/-?\d+(?:\.\d+)?/g).map(Number);
}

function arrowHead(edge) {
  const n = nums(edge.d);
  const end = { x: n[n.length - 2], y: n[n.length - 1] };
  const prev = { x: n[n.length - 4], y: n[n.length - 3] };
  const dx = end.x - prev.x;
  const dy = end.y - prev.y;
  if (edge.type === "extends" || edge.type === "implements") {
    if (Math.abs(dy) >= Math.abs(dx) && dy < 0) return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 8} ${end.y + 16} L${end.x + 8} ${end.y + 16} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
    if (Math.abs(dy) >= Math.abs(dx) && dy > 0) return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 8} ${end.y - 16} L${end.x + 8} ${end.y - 16} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
    if (dx < 0) return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x + 16} ${end.y - 8} L${end.x + 16} ${end.y + 8} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
    return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 16} ${end.y - 8} L${end.x - 16} ${end.y + 8} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
  }
  if (dx < 0) return `<path class="solidOpenHead" d="M${end.x + 13} ${end.y - 7} L${end.x} ${end.y} L${end.x + 13} ${end.y + 7}" stroke="${edge.color}"/>`;
  if (dx > 0) return `<path class="solidOpenHead" d="M${end.x - 13} ${end.y - 7} L${end.x} ${end.y} L${end.x - 13} ${end.y + 7}" stroke="${edge.color}"/>`;
  if (dy < 0) return `<path class="solidOpenHead" d="M${end.x - 7} ${end.y + 13} L${end.x} ${end.y} L${end.x + 7} ${end.y + 13}" stroke="${edge.color}"/>`;
  return `<path class="solidOpenHead" d="M${end.x - 7} ${end.y - 13} L${end.x} ${end.y} L${end.x + 7} ${end.y - 13}" stroke="${edge.color}"/>`;
}

function segs(d) {
  const n = nums(d);
  const pts = [];
  for (let i = 0; i < n.length; i += 2) pts.push({ x: n[i], y: n[i + 1] });
  return pts.slice(1).map((p, i) => ({ a: pts[i], b: p }));
}

function touches(b, p) {
  const onX = p.x >= b.x - 0.1 && p.x <= b.x + b.w + 0.1;
  const onY = p.y >= b.y - 0.1 && p.y <= b.y + b.h + 0.1;
  return ((Math.abs(p.x - b.x) < 0.1 || Math.abs(p.x - (b.x + b.w)) < 0.1) && onY) ||
    ((Math.abs(p.y - b.y) < 0.1 || Math.abs(p.y - (b.y + b.h)) < 0.1) && onX);
}

function hits(s, b, pad = 10) {
  const box = { x: b.x + pad, y: b.y + pad, w: b.w - pad * 2, h: b.h - pad * 2 };
  const minX = Math.min(s.a.x, s.b.x);
  const maxX = Math.max(s.a.x, s.b.x);
  const minY = Math.min(s.a.y, s.b.y);
  const maxY = Math.max(s.a.y, s.b.y);
  if (s.a.x === s.b.x) return s.a.x > box.x && s.a.x < box.x + box.w && maxY > box.y && minY < box.y + box.h;
  if (s.a.y === s.b.y) return s.a.y > box.y && s.a.y < box.y + box.h && maxX > box.x && minX < box.x + box.w;
  return false;
}

function isEndpoint(p, s) {
  return (Math.abs(p.x - s.a.x) < 0.1 && Math.abs(p.y - s.a.y) < 0.1) ||
    (Math.abs(p.x - s.b.x) < 0.1 && Math.abs(p.y - s.b.y) < 0.1);
}

function crosses(a, b) {
  const ah = a.a.y === a.b.y;
  const av = a.a.x === a.b.x;
  const bh = b.a.y === b.b.y;
  const bv = b.a.x === b.b.x;
  if (!((ah && bv) || (av && bh))) return false;
  const h = ah ? a : b;
  const v = av ? a : b;
  const p = { x: v.a.x, y: h.a.y };
  const hMin = Math.min(h.a.x, h.b.x);
  const hMax = Math.max(h.a.x, h.b.x);
  const vMin = Math.min(v.a.y, v.b.y);
  const vMax = Math.max(v.a.y, v.b.y);
  const inside = p.x > hMin + 0.1 && p.x < hMax - 0.1 && p.y > vMin + 0.1 && p.y < vMax - 0.1;
  return inside && !isEndpoint(p, a) && !isEndpoint(p, b);
}

function validate() {
  for (const e of edges) {
    const n = nums(e.d);
    const start = { x: n[0], y: n[1] };
    const end = { x: n[n.length - 2], y: n[n.length - 1] };
    if (!touches(boxes[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(boxes[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) {
      for (const [id, b] of Object.entries(boxes)) {
        if ((id === e.from || id === e.to) && (touches(b, s.a) || touches(b, s.b))) continue;
        if (hits(s, b)) throw new Error(`${e.id} crosses ${id}`);
      }
    }
  }
  for (let i = 0; i < edges.length; i++) {
    for (let j = i + 1; j < edges.length; j++) {
      for (const a of segs(edges[i].d)) {
        for (const b of segs(edges[j].d)) {
          if (crosses(a, b)) throw new Error(`${edges[i].id} crosses ${edges[j].id}`);
        }
      }
    }
  }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="JWT module class structure">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${colors.muted}}.section{stroke:#94A3B8;stroke-width:1.7;stroke-dasharray:12 8}.providerSection{fill:#F3F8FF}.repoSection{fill:#FFF7ED}.supportSection{fill:#F8FAFC}.sectionTitle{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.umlBox{filter:url(#softShadow);stroke-width:2}.stereo{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}.classTitle{font-family:"Architects Daughter";font-size:23px;fill:${colors.ink}}.member{font-family:"Comic Mono";font-size:12.6px;fill:${colors.muted}}.edge{fill:none;stroke-width:2.45;stroke-linecap:round;stroke-linejoin:round}.extends,.has{stroke-dasharray:none}.implements,.uses{stroke-dasharray:8 7}.solidHead{stroke-width:1.9;stroke-linejoin:round;stroke-dasharray:none}.solidOpenHead{fill:none;stroke-width:2.25;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:none}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.2;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:11.8px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/>
<text class="title" x="74" y="86">JWT Class Structure</text>
<text class="subtitle" x="78" y="118">Provider implementations, cache decorators, KeyChain repositories, and compose/read support types from the current jwt source.</text>
<rect class="section providerSection" x="50" y="145" width="930" height="1085" rx="8"/><text class="sectionTitle" x="78" y="170">Provider contract, concrete providers, and parse-result cache decorators</text>
<rect class="section repoSection" x="1510" y="145" width="1050" height="820" rx="8"/><text class="sectionTitle" x="1538" y="170">KeyChain storage contract and distributed/in-memory implementations</text>
<rect class="section supportSection" x="1040" y="990" width="1520" height="240" rx="8"/><text class="sectionTitle" x="1068" y="1015">Composition, signing key, and reader support types</text>
<g id="edges">${edges.map((e) => `<path class="edge ${e.type}" d="${e.d}" stroke="${e.color}"/>`).join("\n")}</g>
<g id="arrowheads">${edges.map(arrowHead).join("\n")}</g>
<g id="labels">${edges.filter((e) => e.label).map((e) => label(e.label)).join("\n")}</g>
${Object.keys(boxes).map(box).join("\n")}
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
