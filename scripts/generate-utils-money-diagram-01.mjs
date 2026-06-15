#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-money-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/utils-money-diagram-01.png";
const W = 2200;
const H = 1260;
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
  purple: "#9333EA",
  gray: "#64748B",
};

const cards = {
  currencyUnit: { x: 130, y: 260, w: 360, h: 150, fill: "#F8FAFC", stroke: colors.gray, kind: "interface", title: "CurrencyUnit", lines: ["currencyCode", "defaultFractionDigits", "provided by javax.money"] },
  monetaryAmount: { x: 640, y: 260, w: 380, h: 150, fill: "#F8FAFC", stroke: colors.gray, kind: "interface", title: "MonetaryAmount", lines: ["currency + number", "add/subtract/multiply", "with(conversion/rounding)"] },
  conversion: { x: 1150, y: 260, w: 360, h: 150, fill: "#F8FAFC", stroke: colors.gray, kind: "interface", title: "CurrencyConversion", lines: ["target currency", "applied via with(...)", "provider-backed rate"] },
  conversions: { x: 1660, y: 260, w: 360, h: 150, fill: "#F8FAFC", stroke: colors.gray, kind: "service", title: "MonetaryConversions", lines: ["getConversion(...)", "isConversionAvailable(...)", "providers: ECB, IMF"] },
  money: { x: 500, y: 510, w: 360, h: 150, fill: "#EFF6FF", stroke: colors.blue, kind: "class", title: "Money", lines: ["Moneta implementation", "BigDecimal-oriented", "created by moneyOf(...)"] },
  fastMoney: { x: 1020, y: 510, w: 360, h: 150, fill: "#ECFDF5", stroke: colors.green, kind: "class", title: "FastMoney", lines: ["Moneta implementation", "Long-oriented", "minor-unit helpers"] },
  currencySupport: { x: 130, y: 825, w: 330, h: 170, fill: "#F8FAFC", stroke: colors.gray, kind: "Kotlin file", title: "CurrencySupport", lines: ["currencyUnitOf(code)", "Locale cache", "KRW / USD / EUR / CNY / JPY"] },
  moneySupport: { x: 505, y: 825, w: 330, h: 170, fill: "#EFF6FF", stroke: colors.blue, kind: "Kotlin file", title: "MoneySupport", lines: ["moneyOf(...)", "Number.toMoney(...)", "inKRW / inUSD / inEUR"] },
  amountSupport: { x: 880, y: 825, w: 330, h: 170, fill: "#F0FDFA", stroke: colors.teal, kind: "Kotlin file", title: "MoneyAmountSupport", lines: ["MonetaryAmount factory", "operators + - * /", "round, convertTo, sum"] },
  fastSupport: { x: 1255, y: 825, w: 330, h: 170, fill: "#ECFDF5", stroke: colors.green, kind: "Kotlin file", title: "FastMoneySupport", lines: ["fastMoneyOf(...)", "minor-unit factories", "Number.toFastMoney(...)"] },
  convertor: { x: 1630, y: 825, w: 360, h: 170, fill: "#FFF7ED", stroke: colors.orange, kind: "object", title: "CurrencyConvertor", lines: ["lazy target conversions", "Default/KRW/USD/EUR/JPY", "getConversion(currency)"] },
  conversionSupport: { x: 1630, y: 1040, w: 360, h: 135, fill: "#FAF5FF", stroke: colors.purple, kind: "Kotlin file", title: "CurrencyConversionSupport", lines: ["String availability", "CurrencyUnit availability"] },
};

const edges = [
  { id: "moneyImpl", type: "implements", color: colors.blue, from: "money", to: "monetaryAmount", d: "M680 510 L680 410", label: { x: 772, y: 462, text: "implements", w: 106 } },
  { id: "fastImpl", type: "implements", color: colors.green, from: "fastMoney", to: "monetaryAmount", d: "M1200 510 L1200 450 L1038 450 L1038 335 L1020 335", label: { x: 1085, y: 432, text: "implements", w: 106 } },
  { id: "currencyFactory", type: "uses", color: colors.gray, from: "currencySupport", to: "currencyUnit", d: "M295 825 L295 410", label: { x: 365, y: 620, text: "creates/caches", w: 126 } },
  { id: "moneyFactory", type: "uses", color: colors.blue, from: "moneySupport", to: "money", d: "M670 825 L670 660", label: { x: 748, y: 744, text: "creates", w: 82 } },
  { id: "fastFactory", type: "uses", color: colors.green, from: "fastSupport", to: "fastMoney", d: "M1420 825 L1420 700 L1200 700 L1200 660", label: { x: 1310, y: 682, text: "creates", w: 82 } },
  { id: "amountOps", type: "uses", color: colors.teal, from: "amountSupport", to: "monetaryAmount", d: "M1045 825 L1045 735 L1000 735 L1000 410", label: { x: 1090, y: 718, text: "extensions", w: 104 } },
  { id: "convertorRates", type: "uses", color: colors.orange, from: "convertor", to: "conversions", d: "M1810 825 L1810 410", label: { x: 1900, y: 620, text: "queries", w: 78 } },
  { id: "convertorReturns", type: "uses", color: colors.orange, from: "convertor", to: "conversion", d: "M1630 910 L1605 910 L1605 335 L1510 335", label: { x: 1562, y: 620, text: "returns", w: 82 } },
  { id: "availability", type: "uses", color: colors.purple, from: "conversionSupport", to: "conversions", d: "M1990 1108 L2075 1108 L2075 335 L2020 335", label: { x: 2076, y: 704, text: "checks", w: 72 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, type, color) {
  if (type === "implements") {
    return `<marker id="arrow-${id}" markerWidth="22" markerHeight="18" refX="20" refY="9" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 22 18"><path d="M2 2 L20 9 L2 16 Z" fill="#FFFFFF" stroke="${color}" stroke-width="2.4"/></marker>`;
  }
  return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12" fill="none" stroke="${color}" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></marker>`;
}

function card(id) {
  const c = cards[id];
  const cx = c.x + c.w / 2;
  const lines = c.lines.map((line, i) => `<text class="member" x="${c.x + 24}" y="${c.y + 88 + i * 22}">${esc(line)}</text>`).join("\n  ");
  return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  <text class="stereo" x="${cx}" y="${c.y + 30}" text-anchor="middle">&lt;&lt;${esc(c.kind)}&gt;&gt;</text>
  <text class="cardTitle" x="${cx}" y="${c.y + 58}" text-anchor="middle">${esc(c.title)}</text>
  <line class="divider" x1="${c.x + 18}" y1="${c.y + 72}" x2="${c.x + c.w - 18}" y2="${c.y + 72}"/>
  ${lines}
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

function hits(s, b, pad = 8) {
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

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Money module API class structure">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.type, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.lane{stroke:#94A3B8;stroke-width:2.1;stroke-dasharray:12 8}.laneContracts{fill:#F3F8FF}.laneKotlin{fill:#FFFBEB}.laneTitle{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.card{filter:url(#softShadow);stroke-width:1.9}.stereo{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}.cardTitle{font-family:"Architects Daughter";font-size:23px;fill:${colors.ink}}.member{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.divider{stroke:${colors.line};stroke-width:1.2}.edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.implements,.uses{stroke-dasharray:9 8}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="2124" height="1194" rx="8"/>
<text class="title" x="78" y="88">Money API Class Structure</text>
<text class="subtitle" x="82" y="120">The module wraps JSR-354 and Moneta with Kotlin factories, operators, rounding, aggregation, and conversion helpers.</text>
<rect class="lane laneContracts" x="90" y="205" width="1970" height="540" rx="8"/><text class="laneTitle" x="118" y="232">JSR-354 contracts and Moneta implementations</text>
<rect class="lane laneKotlin" x="90" y="780" width="1970" height="420" rx="8"/><text class="laneTitle" x="118" y="807">bluetape4k-money Kotlin API surface</text>
<g id="edges">${edges.map((e) => `<path class="edge ${e.type}" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
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
