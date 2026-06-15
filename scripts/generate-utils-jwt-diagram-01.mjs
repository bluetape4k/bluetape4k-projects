#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-jwt-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/utils-jwt-diagram-01.png";
const W = 2200;
const H = 1320;
const colors = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  blue: "#2563EB",
  green: "#16A34A",
  teal: "#0D9488",
  orange: "#EA580C",
  purple: "#7C3AED",
  amber: "#D97706",
  rose: "#E11D48",
  gray: "#64748B",
};

const evidence = [
  "utils/jwt/README.md",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/JwtProvider.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/AbstractJwtProvider.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/DefaultJwtProvider.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/provider/JwtParserSupport.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/composer/JwtComposer.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/KeyChainRepository.kt",
  "utils/jwt/src/main/kotlin/io/bluetape4k/jwt/reader/JwtReader.kt",
];

for (const file of evidence) {
  if (!existsSync(file)) throw new Error(`Missing source evidence: ${file}`);
}

const readme = readFileSync("utils/jwt/README.md", "utf8");
if (!/JWT Create and Verify Flow[\s\S]*utils-jwt-diagram-01\.png/.test(readme)) {
  throw new Error("README JWT flow diagram slot not found");
}

const cards = {
  provider: { x: 120, y: 205, w: 390, h: 170, fill: "#EFF6FF", stroke: colors.blue, icon: "provider", title: "JwtProvider", kicker: "entrypoint", lines: ["composer() / compose()", "parse() / tryParse()", "rotate() / forcedRotate()"] },
  lock: { x: 640, y: 205, w: 370, h: 170, fill: "#F0FDFA", stroke: colors.teal, icon: "lock", title: "AbstractJwtProvider", kicker: "thread-safe gateway", lines: ["ReentrantLock protects", "currentKeyChain selection", "Composer gets explicit key"] },
  repository: { x: 1160, y: 205, w: 420, h: 170, fill: "#FFF7ED", stroke: colors.orange, icon: "store", title: "KeyChainRepository", kicker: "current + history", lines: ["current() for signing", "findOrNull(kid) for verify", "InMemory or Redis backing"] },
  rotation: { x: 1710, y: 205, w: 360, h: 170, fill: "#FFFBEB", stroke: colors.amber, icon: "rotate", title: "Rotation Timer", kicker: "DefaultJwtProvider", lines: ["initial rotate()", "checks every 60 seconds", "keeps previous keys"] },

  dsl: { x: 120, y: 540, w: 390, h: 180, fill: "#F8FAFC", stroke: colors.gray, icon: "dsl", title: "Builder or DSL", kicker: "caller input", lines: ["header(...)", "claim(...)", "issuer / subject / exp", "optional compression"] },
  composer: { x: 640, y: 535, w: 370, h: 190, fill: "#EFF6FF", stroke: colors.blue, icon: "compose", title: "JwtComposer", kicker: "composition state", lines: ["stores custom headers", "stores custom claims", "guards reserved names", "defaults iat when absent"] },
  builder: { x: 1160, y: 535, w: 420, h: 190, fill: "#F0FDF4", stroke: colors.green, icon: "sign", title: "jjwt Builder", kicker: "compact signing", lines: ["adds kid and typ=JWT", "signs with private key", "compresses if configured", "outputs compact JWS"] },
  token: { x: 1710, y: 540, w: 360, h: 180, fill: "#FAF5FF", stroke: colors.purple, icon: "token", title: "JWT String", kicker: "header.payload.signature", lines: ["URL-safe compact form", "kid identifies key", "can travel across nodes"] },

  incoming: { x: 120, y: 890, w: 390, h: 180, fill: "#FAF5FF", stroke: colors.purple, icon: "token", title: "Incoming JWT", kicker: "parse(jwtString)", lines: ["same compact JWS", "header exposes kid + alg", "claims require signature"] },
  parser: { x: 640, y: 885, w: 370, h: 190, fill: "#F8FAFC", stroke: colors.gray, icon: "parser", title: "currentJwtParser()", kicker: "provider cache", lines: ["ConcurrentHashMap cache", "jjwt parser instance", "built with key locator", "DEF/GZIP supported"] },
  locator: { x: 1160, y: 885, w: 420, h: 190, fill: "#FFF7ED", stroke: colors.orange, icon: "locate", title: "Key Locator", kicker: "kid based verification", lines: ["requires kid header", "finds matching KeyChain", "checks alg equality", "returns public key"] },
  reader: { x: 1710, y: 885, w: 360, h: 190, fill: "#ECFDF5", stroke: colors.green, icon: "reader", title: "JwtReader", kicker: "typed read API", lines: ["Claims delegate", "header<T>(key)", "claim<T>(name)", "isExpired / expiredTtl"] },
};

const edges = [
  { id: "providerLock", color: colors.blue, from: "provider", to: "lock", d: "M510 290 L640 290", label: { x: 575, y: 262, text: "serializes", w: 96 } },
  { id: "lockRepo", color: colors.teal, from: "lock", to: "repository", d: "M1010 290 L1160 290", label: { x: 1085, y: 262, text: "current key", w: 108 } },
  { id: "rotationRepo", color: colors.amber, from: "rotation", to: "repository", d: "M1710 290 L1580 290", label: { x: 1645, y: 262, text: "rotate", w: 78 } },
  { id: "providerComposer", color: colors.blue, from: "provider", to: "composer", d: "M315 375 L315 440 L825 440 L825 535", label: { x: 545, y: 414, text: "compose path", w: 118 } },
  { id: "dslComposer", color: colors.gray, from: "dsl", to: "composer", d: "M510 630 L640 630", label: { x: 575, y: 602, text: "configure", w: 92 } },
  { id: "composerBuilder", color: colors.blue, from: "composer", to: "builder", d: "M1010 630 L1160 630", label: { x: 1085, y: 602, text: "apply rules", w: 104 } },
  { id: "repoBuilder", color: colors.orange, from: "repository", to: "builder", d: "M1370 375 L1370 535", label: { x: 1456, y: 456, text: "private key", w: 104 } },
  { id: "builderToken", color: colors.green, from: "builder", to: "token", d: "M1580 630 L1710 630", label: { x: 1645, y: 602, text: "compact", w: 84 } },
  { id: "tokenIncoming", color: colors.purple, from: "token", to: "incoming", d: "M1890 720 L1890 790 L315 790 L315 890", label: { x: 1110, y: 762, text: "distributed use", w: 134 } },
  { id: "incomingParser", color: colors.purple, from: "incoming", to: "parser", d: "M510 980 L640 980", label: { x: 575, y: 952, text: "parse", w: 70 } },
  { id: "parserLocator", color: colors.gray, from: "parser", to: "locator", d: "M1010 980 L1160 980", label: { x: 1085, y: 952, text: "key lookup", w: 104 } },
  { id: "repoLocator", color: colors.orange, from: "repository", to: "locator", d: "M1370 375 L1370 430 L2110 430 L2110 1180 L1370 1180 L1370 1075", label: { x: 2038, y: 812, text: "public key by kid", w: 140 } },
  { id: "locatorReader", color: colors.green, from: "locator", to: "reader", d: "M1580 980 L1710 980", label: { x: 1645, y: 952, text: "verified JWS", w: 112 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="22" markerHeight="18" refX="20" refY="9" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 22 18"><path d="M2 2 L20 9 L2 16 Z" fill="${color}"/></marker>`;
}

function icon(type, x, y, color) {
  const cx = x + 42;
  const cy = y + 48;
  const base = `<rect x="${x + 20}" y="${y + 26}" width="50" height="50" rx="12" fill="${color}"/>`;
  if (type === "provider") return `${base}<path d="M${cx - 15} ${cy - 9} H${cx + 15} M${cx - 15} ${cy} H${cx + 15} M${cx - 15} ${cy + 9} H${cx + 15}" stroke="#fff" stroke-width="3.5" stroke-linecap="round"/>`;
  if (type === "lock") return `${base}<rect x="${cx - 15}" y="${cy - 1}" width="30" height="18" rx="4" fill="none" stroke="#fff" stroke-width="3"/><path d="M${cx - 9} ${cy - 1} V${cy - 9} C${cx - 9} ${cy - 18}, ${cx + 9} ${cy - 18}, ${cx + 9} ${cy - 9} V${cy - 1}" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round"/>`;
  if (type === "store") return `${base}<ellipse cx="${cx}" cy="${cy - 12}" rx="16" ry="7" fill="none" stroke="#fff" stroke-width="3"/><path d="M${cx - 16} ${cy - 12} V${cy + 12} C${cx - 16} ${cy + 20}, ${cx + 16} ${cy + 20}, ${cx + 16} ${cy + 12} V${cy - 12} M${cx - 16} ${cy} C${cx - 16} ${cy + 8}, ${cx + 16} ${cy + 8}, ${cx + 16} ${cy}" fill="none" stroke="#fff" stroke-width="3"/>`;
  if (type === "rotate") return `${base}<path d="M${cx + 13} ${cy - 4} A15 15 0 1 0 ${cx + 6} ${cy + 12}" fill="none" stroke="#fff" stroke-width="3.3" stroke-linecap="round"/><path d="M${cx + 13} ${cy - 4} L${cx + 13} ${cy - 15} L${cx + 3} ${cy - 10}" fill="none" stroke="#fff" stroke-width="3.3" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (type === "dsl") return `${base}<path d="M${cx - 14} ${cy - 10} L${cx - 4} ${cy} L${cx - 14} ${cy + 10} M${cx + 14} ${cy - 10} L${cx + 4} ${cy} L${cx + 14} ${cy + 10}" fill="none" stroke="#fff" stroke-width="3.3" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (type === "compose") return `${base}<path d="M${cx - 14} ${cy - 12} H${cx + 8} L${cx + 15} ${cy - 5} V${cy + 14} H${cx - 14} Z" fill="none" stroke="#fff" stroke-width="3" stroke-linejoin="round"/><path d="M${cx - 7} ${cy + 1} H${cx + 8} M${cx - 7} ${cy + 9} H${cx + 8}" stroke="#fff" stroke-width="2.8" stroke-linecap="round"/>`;
  if (type === "sign") return `${base}<path d="M${cx - 14} ${cy + 12} L${cx + 13} ${cy - 15} M${cx - 1} ${cy - 3} L${cx + 10} ${cy + 8} M${cx - 8} ${cy + 4} L${cx + 1} ${cy + 13}" stroke="#fff" stroke-width="3.3" stroke-linecap="round"/>`;
  if (type === "token") return `${base}<rect x="${cx - 17}" y="${cy - 13}" width="34" height="26" rx="5" fill="none" stroke="#fff" stroke-width="3"/><circle cx="${cx - 8}" cy="${cy}" r="2.8" fill="#fff"/><circle cx="${cx}" cy="${cy}" r="2.8" fill="#fff"/><circle cx="${cx + 8}" cy="${cy}" r="2.8" fill="#fff"/>`;
  if (type === "parser") return `${base}<path d="M${cx - 15} ${cy - 12} H${cx + 15} V${cy + 12} H${cx - 15} Z M${cx - 7} ${cy - 3} H${cx + 7} M${cx - 7} ${cy + 6} H${cx + 3}" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (type === "locate") return `${base}<circle cx="${cx - 5}" cy="${cy - 4}" r="11" fill="none" stroke="#fff" stroke-width="3"/><path d="M${cx + 4} ${cy + 5} L${cx + 15} ${cy + 16}" stroke="#fff" stroke-width="3.4" stroke-linecap="round"/>`;
  if (type === "reader") return `${base}<path d="M${cx - 15} ${cy - 13} H${cx + 15} V${cy + 13} H${cx - 15} Z M${cx - 8} ${cy - 4} H${cx + 8} M${cx - 8} ${cy + 5} H${cx + 8}" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`;
  return `${base}`;
}

function card(id) {
  const c = cards[id];
  const textX = c.x + 94;
  return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  ${icon(c.icon, c.x, c.y, c.stroke)}
  <text class="kicker" x="${textX}" y="${c.y + 38}">${esc(c.kicker)}</text>
  <text class="cardTitle" x="${textX}" y="${c.y + 68}">${esc(c.title)}</text>
  ${c.lines.map((line, i) => `<text class="detail" x="${textX}" y="${c.y + 102 + i * 23}">${esc(line)}</text>`).join("\n  ")}
</g>`;
}

function label({ x, y, text, w }) {
  return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 15})"><rect width="${w}" height="30" rx="8"/><text x="${w / 2}" y="20" text-anchor="middle">${esc(text)}</text></g>`;
}

function nums(d) {
  return d.match(/-?\d+(?:\.\d+)?/g).map(Number);
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
    if (!touches(cards[e.from], start)) throw new Error(`${e.id} start`);
    if (!touches(cards[e.to], end)) throw new Error(`${e.id} end`);
    for (const s of segs(e.d)) {
      for (const [id, c] of Object.entries(cards)) {
        if ((id === e.from || id === e.to) && (touches(c, s.a) || touches(c, s.b))) continue;
        if (hits(s, c)) throw new Error(`${e.id} crosses ${id}`);
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

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="JWT create and verify flow">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.lane{stroke:#94A3B8;stroke-width:1.8;stroke-dasharray:12 8}.laneTitle{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.providerLane{fill:#F3F8FF}.composeLane{fill:#F7FEE7}.verifyLane{fill:#FDF4FF}.card{filter:url(#softShadow);stroke-width:2}.kicker{font-family:"Comic Mono";font-size:12.8px;fill:${colors.muted}}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:13.2px;fill:${colors.muted}}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="36" y="30" width="${W - 72}" height="${H - 66}" rx="8"/>
<text class="title" x="78" y="88">JWT Create and Verify Flow</text>
<text class="subtitle" x="82" y="120">Provider, Composer, Parser, and KeyChain repository responsibilities from current source: sign with the current private key, verify by kid with the matching public key.</text>
<rect class="lane providerLane" x="82" y="165" width="2028" height="250" rx="8"/><text class="laneTitle" x="110" y="192">provider and key lifecycle</text>
<rect class="lane composeLane" x="82" y="485" width="2028" height="285" rx="8"/><text class="laneTitle" x="110" y="512">create path</text>
<rect class="lane verifyLane" x="82" y="835" width="2028" height="285" rx="8"/><text class="laneTitle" x="110" y="862">verify and read path</text>
<g id="edges">${edges.map((e) => `<path class="edge" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
${Object.keys(cards).map(card).join("\n")}
</svg>`;

for (const e of edges) {
  if (!svg.includes(`id="arrow-${e.id}"`)) throw new Error(`missing marker ${e.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
