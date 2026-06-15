#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-money-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/utils-money-diagram-02.png";
const W = 2040;
const H = 1100;
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
  amber: "#D97706",
  gray: "#64748B",
};

const cards = {
  inputs: { x: 110, y: 250, w: 340, h: 185, fill: "#EFF6FF", stroke: colors.blue, title: "Inputs", lines: ["Number or MonetaryAmount", "currency code / Locale", "Money vs FastMoney choice"], icon: "input" },
  currency: { x: 560, y: 250, w: 340, h: 185, fill: "#F8FAFC", stroke: colors.gray, title: "Currency lookup", lines: ["currencyUnitOf(...)", "ConcurrentHashMap cache", "KRW / USD / EUR / CNY / JPY"], icon: "currency" },
  create: { x: 1010, y: 250, w: 380, h: 185, fill: "#ECFDF5", stroke: colors.green, title: "Amount factories", lines: ["moneyOf -> Money", "fastMoneyOf -> FastMoney", "monetaryAmountOf -> default factory"], icon: "factory" },
  arithmetic: { x: 1520, y: 250, w: 360, h: 185, fill: "#F0FDFA", stroke: colors.teal, title: "Local operations", lines: ["+  -  *  /  unary -", "numberValue properties", "same-currency arithmetic"], icon: "ops" },
  rounding: { x: 1010, y: 555, w: 380, h: 170, fill: "#FFFBEB", stroke: colors.amber, title: "Rounding", lines: ["round(currency rule)", "defaultRound()", "returns same amount type"], icon: "round" },
  conversion: { x: 560, y: 555, w: 340, h: 170, fill: "#FFF7ED", stroke: colors.orange, title: "Exchange conversion", lines: ["convertTo(code/unit)", "CurrencyConvertor cache", "MonetaryConversions"], icon: "exchange" },
  providers: { x: 110, y: 555, w: 340, h: 170, fill: "#FAF5FF", stroke: colors.purple, title: "Rate providers", lines: ["ECB", "IMF", "availability checks"], icon: "cloud" },
  aggregation: { x: 1520, y: 555, w: 360, h: 170, fill: "#FDF2F8", stroke: "#DB2777", title: "Aggregation", lines: ["Collection<MonetaryAmount>.sum()", "empty -> zero amount", "convert each item to target"], icon: "sum" },
  result: { x: 420, y: 875, w: 1200, h: 130, fill: "#F8FAFC", stroke: colors.gray, title: "Result", lines: ["A typed MonetaryAmount in the requested currency, rounded or converted according to JSR-354 provider rules."], icon: "check" },
};

const edges = [
  { id: "inputCurrency", color: colors.blue, from: "inputs", to: "currency", d: "M450 342 L560 342", label: { x: 505, y: 315, text: "code/Locale", w: 104 } },
  { id: "currencyCreate", color: colors.gray, from: "currency", to: "create", d: "M900 342 L1010 342", label: { x: 955, y: 315, text: "CurrencyUnit", w: 108 } },
  { id: "createOps", color: colors.green, from: "create", to: "arithmetic", d: "M1390 342 L1520 342", label: { x: 1455, y: 315, text: "Money/FastMoney", w: 136 } },
  { id: "opsRound", color: colors.teal, from: "arithmetic", to: "rounding", d: "M1700 435 L1700 495 L1200 495 L1200 555", label: { x: 1510, y: 505, text: "needs scale", w: 96 } },
  { id: "opsAgg", color: "#DB2777", from: "arithmetic", to: "aggregation", d: "M1700 435 L1700 555", label: { x: 1785, y: 500, text: "collections", w: 104 } },
  { id: "providersConversion", color: colors.purple, from: "providers", to: "conversion", d: "M450 640 L560 640", label: { x: 505, y: 613, text: "rates", w: 70 } },
  { id: "createConversion", color: colors.orange, from: "create", to: "conversion", d: "M1200 435 L1200 485 L730 485 L730 555", label: { x: 965, y: 494, text: "convertTo", w: 92 } },
  { id: "conversionResult", color: colors.orange, from: "conversion", to: "result", d: "M730 725 L730 875", label: { x: 815, y: 802, text: "target currency", w: 132 } },
  { id: "roundResult", color: colors.amber, from: "rounding", to: "result", d: "M1200 725 L1200 875", label: { x: 1285, y: 802, text: "rounded", w: 84 } },
  { id: "aggResult", color: "#DB2777", from: "aggregation", to: "result", d: "M1700 725 L1700 805 L1460 805 L1460 875", label: { x: 1605, y: 815, text: "total", w: 66 } },
];

function esc(v) {
  return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
  return `<marker id="arrow-${id}" markerWidth="22" markerHeight="18" refX="20" refY="9" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 22 18"><path d="M2 2 L20 9 L2 16 Z" fill="${color}"/></marker>`;
}

function icon(type, x, y, color) {
  const cx = x + 38;
  const cy = y + 46;
  const base = `<rect x="${x + 18}" y="${y + 26}" width="48" height="48" rx="12" fill="${color}"/>`;
  if (type === "currency") return `${base}<text x="${cx}" y="${cy + 10}" text-anchor="middle" font-family="Comic Mono" font-size="28" font-weight="700" fill="#FFFFFF">$</text>`;
  if (type === "factory") return `${base}<path d="M${cx - 15} ${cy - 10} H${cx + 15} V${cy + 12} H${cx - 15} Z M${cx - 8} ${cy - 18} H${cx + 8}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (type === "ops") return `${base}<path d="M${cx - 14} ${cy} H${cx + 14} M${cx} ${cy - 14} V${cy + 14}" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/><circle cx="${cx + 14}" cy="${cy + 14}" r="3" fill="#FFFFFF"/>`;
  if (type === "round") return `${base}<path d="M${cx - 16} ${cy + 2} C${cx - 8} ${cy - 14}, ${cx + 8} ${cy - 14}, ${cx + 16} ${cy + 2}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/><path d="M${cx + 8} ${cy - 1} L${cx + 17} ${cy + 3} L${cx + 9} ${cy + 9}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (type === "exchange") return `${base}<path d="M${cx - 16} ${cy - 8} H${cx + 16} M${cx + 8} ${cy - 16} L${cx + 16} ${cy - 8} L${cx + 8} ${cy} M${cx + 16} ${cy + 9} H${cx - 16} M${cx - 8} ${cy + 1} L${cx - 16} ${cy + 9} L${cx - 8} ${cy + 17}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (type === "cloud") return `${base}<path d="M${cx - 18} ${cy + 8} H${cx + 15} C${cx + 22} ${cy + 8}, ${cx + 22} ${cy - 4}, ${cx + 13} ${cy - 4} C${cx + 10} ${cy - 17}, ${cx - 8} ${cy - 15}, ${cx - 9} ${cy - 4} C${cx - 20} ${cy - 6}, ${cx - 24} ${cy + 8}, ${cx - 18} ${cy + 8} Z" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linejoin="round"/>`;
  if (type === "sum") return `${base}<path d="M${cx - 13} ${cy - 14} H${cx + 14} L${cx - 2} ${cy} L${cx + 14} ${cy + 14} H${cx - 13}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (type === "check") return `${base}<path d="M${cx - 15} ${cy} L${cx - 5} ${cy + 10} L${cx + 17} ${cy - 12}" fill="none" stroke="#FFFFFF" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>`;
  return `${base}<path d="M${cx - 14} ${cy} H${cx + 14}" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>`;
}

function card(id) {
  const c = cards[id];
  const textX = c.x + (id === "result" ? 118 : 92);
  return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  ${icon(c.icon, c.x, c.y, c.stroke)}
  <text class="cardTitle" x="${textX}" y="${c.y + 56}">${esc(c.title)}</text>
  ${c.lines.map((line, i) => `<text class="detail" x="${textX}" y="${c.y + 92 + i * 24}">${esc(line)}</text>`).join("\n  ")}
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

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Money currency operation flow">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.lane{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.4;stroke-dasharray:10 8}.laneTitle{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.card{filter:url(#softShadow);stroke-width:2}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:13.4px;fill:${colors.muted}}.edge{fill:none;stroke-width:3.3;stroke-linecap:round;stroke-linejoin:round}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="1964" height="1034" rx="8"/>
<text class="title" x="78" y="88">Currency Operation Flow</text>
<text class="subtitle" x="82" y="120">From currency lookup to amount creation, local arithmetic, rounding, aggregation, and provider-backed exchange conversion.</text>
<rect class="lane" x="80" y="205" width="1880" height="555" rx="8"/><text class="laneTitle" x="108" y="232">operations provided by bluetape4k-money</text>
<g id="edges">${edges.map((e) => `<path class="edge" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
${Object.keys(cards).map(card).join("\n")}
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
