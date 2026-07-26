#!/usr/bin/env node

import {execFileSync} from "node:child_process";
import {writeFileSync} from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-workflow-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/utils-workflow-diagram-02.png";

const W = 1680;
const H = 980;
const colors = {
    ink: "#0F172A",
    muted: "#475569",
    canvas: "#F8FAFC",
    frame: "#FFFFFF",
    line: "#CBD5E1",
    blue: "#2563EB",
    green: "#16A34A",
    pink: "#DB2777",
    orange: "#EA580C",
    purple: "#9333EA",
    teal: "#0D9488",
    lime: "#65A30D",
    gray: "#64748B",
};

const cards = {
    execute: {x: 610, y: 170, w: 380, h: 118, fill: "#EFF6FF", stroke: colors.blue, title: "Work executes"},
    success: {x: 1130, y: 360, w: 360, h: 112, fill: "#F0FDF4", stroke: colors.green, title: "Success"},
    failure: {x: 610, y: 380, w: 380, h: 112, fill: "#FDF2F8", stroke: colors.pink, title: "Failure"},
    cancelled: {x: 150, y: 210, w: 350, h: 112, fill: "#FAF5FF", stroke: colors.purple, title: "Cancelled"},
    aborted: {x: 150, y: 430, w: 350, h: 112, fill: "#FFF7ED", stroke: colors.orange, title: "Aborted"},
    strategy: {x: 610, y: 590, w: 380, h: 132, fill: "#F0FDFA", stroke: colors.teal, title: "ErrorStrategy?"},
    stop: {x: 500, y: 780, w: 320, h: 108, fill: "#FFF1F2", stroke: colors.pink, title: "Return Failure"},
    partial: {x: 1000, y: 780, w: 380, h: 108, fill: "#F7FEE7", stroke: colors.lime, title: "PartialSuccess"},
};

const edges = [
    {
        id: "success",
        color: colors.green,
        from: "execute",
        to: "success",
        d: "M990 229 L1080 229 L1080 416 L1130 416",
        label: {x: 1090, y: 300, text: "COMPLETED", w: 116},
    },
    {
        id: "failure",
        color: colors.pink,
        from: "execute",
        to: "failure",
        d: "M800 288 L800 380",
        label: {x: 884, y: 340, text: "FAILED", w: 82},
    },
    {
        id: "aborted",
        color: colors.orange,
        from: "execute",
        to: "aborted",
        d: "M610 255 L560 255 L560 486 L500 486",
        label: {x: 548, y: 378, text: "ABORTED", w: 94},
    },
    {
        id: "cancelled",
        color: colors.purple,
        from: "execute",
        to: "cancelled",
        d: "M610 220 L540 220 L540 266 L500 266",
        dashed: true,
        label: {x: 538, y: 202, text: "CANCELLED", w: 104},
    },
    {
        id: "strategy",
        color: colors.teal,
        from: "failure",
        to: "strategy",
        d: "M800 492 L800 590",
        label: {x: 884, y: 542, text: "strategy decides", w: 146},
    },
    {
        id: "stop",
        color: colors.pink,
        from: "strategy",
        to: "stop",
        d: "M720 722 L660 780",
        label: {x: 638, y: 744, text: "STOP", w: 64},
    },
    {
        id: "continue",
        color: colors.lime,
        from: "strategy",
        to: "partial",
        d: "M900 722 L1060 780",
        label: {x: 1020, y: 744, text: "CONTINUE", w: 104},
    },
];

function esc(text) {
    return String(text).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
    return `<marker id="arrow-${id}" markerWidth="18" markerHeight="14" refX="16" refY="7" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 18 14"><path d="M2 2 L16 7 L2 12 Z" fill="${color}"/></marker>`;
}

function card(id, lines, icon) {
    const c = cards[id];
    const cx = c.x + c.w / 2;
    const titleX = id === "success" ? cx + 10 : id === "aborted" || id === "cancelled" ? cx + 16 : cx + 24;
    const detail = lines.map((line, i) => `<text class="detail" x="${cx}" y="${c.y + 76 + i * 19}" text-anchor="middle">${esc(line)}</text>`).join("\n");
    return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  ${iconFor(icon, c.x + 24, c.y + 24, c.stroke)}
  <text class="cardTitle" x="${titleX}" y="${c.y + 40}" text-anchor="middle">${esc(c.title)}</text>
  ${detail}
</g>`;
}

function iconFor(kind, x, y, color) {
    if (kind === "run") {
        return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="22" fill="#fff"/><path d="M24 17 L40 28 L24 39 Z" fill="${color}" stroke="none"/></g>`;
    }
    if (kind === "ok") {
        return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="23" fill="#fff"/><path d="M16 29 L25 38 L42 18" fill="none"/></g>`;
    }
    if (kind === "fail") {
        return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="23" fill="#fff"/><path d="M18 18 L38 38 M38 18 L18 38" fill="none"/></g>`;
    }
    if (kind === "break") {
        return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="7" y="9" width="42" height="38" rx="7" fill="#fff"/><path d="M17 19 H38 M17 29 H30 M17 39 H34" fill="none"/></g>`;
    }
    if (kind === "cancel") {
        return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><circle cx="28" cy="28" r="23" fill="#fff"/><path d="M28 13 V28 L42 36" fill="none"/></g>`;
    }
    if (kind === "choice") {
        return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><path d="M28 6 L50 28 L28 50 L6 28 Z" fill="#fff"/><path d="M19 24 H37 M19 33 H37" fill="none"/></g>`;
    }
    return `<g class="icon" transform="translate(${x} ${y})" stroke="${color}"><rect x="7" y="10" width="44" height="36" rx="7" fill="#fff"/><path d="M18 24 H40 M18 34 H34" fill="none"/></g>`;
}

function label({x, y, text, w}) {
    return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 15})"><rect width="${w}" height="30" rx="8"/><text x="${w / 2}" y="20" text-anchor="middle">${esc(text)}</text></g>`;
}

function nums(d) {
    return d.match(/-?\d+(?:\.\d+)?/g).map(Number);
}

function pathSegments(d) {
    const n = nums(d);
    const pts = [];
    for (let i = 0; i < n.length; i += 2) pts.push({x: n[i], y: n[i + 1]});
    return pts.slice(1).map((p, i) => ({a: pts[i], b: p}));
}

function touches(box, p) {
    const onX = p.x >= box.x - 0.1 && p.x <= box.x + box.w + 0.1;
    const onY = p.y >= box.y - 0.1 && p.y <= box.y + box.h + 0.1;
    return ((Math.abs(p.x - box.x) < 0.1 || Math.abs(p.x - (box.x + box.w)) < 0.1) && onY) ||
        ((Math.abs(p.y - box.y) < 0.1 || Math.abs(p.y - (box.y + box.h)) < 0.1) && onX);
}

function segmentHitsBox(seg, box, pad = 8) {
    const b = {x: box.x + pad, y: box.y + pad, w: box.w - pad * 2, h: box.h - pad * 2};
    const minX = Math.min(seg.a.x, seg.b.x);
    const maxX = Math.max(seg.a.x, seg.b.x);
    const minY = Math.min(seg.a.y, seg.b.y);
    const maxY = Math.max(seg.a.y, seg.b.y);
    if (seg.a.x === seg.b.x) return seg.a.x > b.x && seg.a.x < b.x + b.w && maxY > b.y && minY < b.y + b.h;
    if (seg.a.y === seg.b.y) return seg.a.y > b.y && seg.a.y < b.y + b.h && maxX > b.x && minX < b.x + b.w;
    return false;
}

function segmentsCross(a, b) {
    const aVertical = a.a.x === a.b.x;
    const bVertical = b.a.x === b.b.x;
    if (aVertical === bVertical) return false;
    const v = aVertical ? a : b;
    const h = aVertical ? b : a;
    const x = v.a.x;
    const y = h.a.y;
    const vMinY = Math.min(v.a.y, v.b.y);
    const vMaxY = Math.max(v.a.y, v.b.y);
    const hMinX = Math.min(h.a.x, h.b.x);
    const hMaxX = Math.max(h.a.x, h.b.x);
    const crosses = x > hMinX && x < hMaxX && y > vMinY && y < vMaxY;
    if (!crosses) return false;
    return ![
        a.a, a.b, b.a, b.b,
    ].some((p) => p.x === x && p.y === y);
}

function validate() {
    const ids = Object.keys(cards);
    for (let i = 0; i < ids.length; i++) {
        for (let j = i + 1; j < ids.length; j++) {
            const a = cards[ids[i]], b = cards[ids[j]];
            if (a.x < b.x + b.w + 12 && a.x + a.w + 12 > b.x && a.y < b.y + b.h + 12 && a.y + a.h + 12 > b.y) {
                throw new Error(`Card overlap: ${ids[i]} ${ids[j]}`);
            }
        }
    }
    for (const edge of edges) {
        const n = nums(edge.d);
        const start = {x: n[0], y: n[1]};
        const end = {x: n[n.length - 2], y: n[n.length - 1]};
        if (!touches(cards[edge.from], start)) throw new Error(`${edge.id} start does not touch ${edge.from}`);
        if (!touches(cards[edge.to], end)) throw new Error(`${edge.id} end does not touch ${edge.to}`);
        for (const seg of pathSegments(edge.d)) {
            for (const id of ids) {
                if ((id === edge.from && (touches(cards[id], seg.a) || touches(cards[id], seg.b))) ||
                    (id === edge.to && (touches(cards[id], seg.a) || touches(cards[id], seg.b)))) continue;
                if (segmentHitsBox(seg, cards[id])) throw new Error(`${edge.id} crosses ${id}`);
            }
        }
    }
    for (let i = 0; i < edges.length; i++) {
        for (let j = i + 1; j < edges.length; j++) {
            for (const a of pathSegments(edges[i].d)) {
                for (const b of pathSegments(edges[j].d)) {
                    if (segmentsCross(a, b)) throw new Error(`Line crossing: ${edges[i].id} x ${edges[j].id}`);
                }
            }
        }
    }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="WorkReport State Flow">
<defs>
  <filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${edges.map((edge) => marker(edge.id, edge.color)).join("\n  ")}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}
    .card{filter:url(#softShadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:13.5px;fill:${colors.muted}}
    .icon{stroke-width:2.4;stroke-linecap:round;stroke-linejoin:round}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 8}
    .edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.5px;fill:${colors.muted}}
  </style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="38" y="30" width="1604" height="914" rx="8"/>
<text class="title" x="78" y="86">WorkReport State Flow</text>
<text class="subtitle" x="82" y="118">Failure is the only state governed by ErrorStrategy; Aborted and Cancelled bypass strategy and terminate immediately.</text>
<g id="edges">
${edges.map((edge) => `  <path class="edge${edge.dashed ? " dashed" : ""}" data-from="${edge.from}" data-to="${edge.to}" d="${edge.d}" stroke="${edge.color}" marker-end="url(#arrow-${edge.id})"/>`).join("\n")}
</g>
<g id="labels">
${edges.map((edge) => `  ${label(edge.label)}`).join("\n")}
</g>
${card("execute", ["work.execute(context)", "returns exactly one report"], "run")}
${card("success", ["continue to next work", "final report if no more work"], "ok")}
${card("failure", ["exception or explicit failure", "strategy chooses exit path"], "fail")}
${card("aborted", ["internal early exit", "ignores ErrorStrategy"], "break")}
${card("cancelled", ["timeout or coroutine cancel", "terminal external stop"], "cancel")}
${card("strategy", ["STOP returns Failure", "CONTINUE accumulates failure"], "choice")}
${card("stop", ["fail fast", "returns immediately"], "fail")}
${card("partial", ["one or more failures", "returned after final work"], "report")}
</svg>
`;

for (const edge of edges) {
    if (!svg.includes(`marker-end="url(#arrow-${edge.id})"`)) throw new Error(`Missing marker ${edge.id}`);
    if (!svg.includes(`id="arrow-${edge.id}"`) || !svg.includes(`fill="${edge.color}"`)) throw new Error(`Marker color mismatch ${edge.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
