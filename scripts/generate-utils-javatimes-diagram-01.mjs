#!/usr/bin/env node

import {execFileSync} from "node:child_process";
import {writeFileSync} from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-javatimes-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/utils-javatimes-diagram-01.png";
const W = 2040;
const H = 1080;
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
    pink: "#DB2777",
    purple: "#9333EA",
    amber: "#D97706",
    gray: "#64748B",
};

const cards = {
    foundation: {
        x: 110,
        y: 270,
        w: 350,
        h: 250,
        fill: "#EFF6FF",
        stroke: colors.blue,
        title: "java.time foundation",
        lines: ["Instant / ZonedDateTime", "LocalDateTime / Temporal", "core DSL: 5.days(), 3.hours()"],
        icon: "clock",
    },
    calendarPolicy: {
        x: 110,
        y: 610,
        w: 350,
        h: 210,
        fill: "#F8FAFC",
        stroke: colors.gray,
        title: "Calendar policy",
        lines: ["TimeCalendarConfig", "start/end offsets", "first day + fiscal year rules"],
        icon: "settings",
    },
    interval: {
        x: 590,
        y: 230,
        w: 350,
        h: 180,
        fill: "#ECFDF5",
        stroke: colors.green,
        title: "TemporalInterval",
        lines: ["half-open start..end", "contains / overlaps", "windowed + chunked"],
        icon: "brackets",
    },
    period: {
        x: 995,
        y: 230,
        w: 350,
        h: 180,
        fill: "#FFF7ED",
        stroke: colors.orange,
        title: "Period framework",
        lines: ["TimeBlock / TimeRange", "move / expand / shrink", "PeriodRelation"],
        icon: "block",
    },
    business: {
        x: 1400,
        y: 230,
        w: 350,
        h: 180,
        fill: "#FAF5FF",
        stroke: colors.purple,
        title: "Business calendars",
        lines: ["DateAdd include/exclude", "DateDiff elapsed units", "availability gaps"],
        icon: "calendar",
    },
    calendarRanges: {
        x: 590,
        y: 560,
        w: 350,
        h: 180,
        fill: "#F0FDFA",
        stroke: colors.teal,
        title: "Calendar ranges",
        lines: ["Year / Quarter / Month", "Week / Day / Hour / Minute", "collections + Flow ranges"],
        icon: "range",
    },
    temporalRanges: {
        x: 995,
        y: 560,
        w: 350,
        h: 180,
        fill: "#FDF2F8",
        stroke: colors.pink,
        title: "Temporal range DSL",
        lines: ["start..end syntax", "step progressions", "window / chunk / zip"],
        icon: "route",
    },
    coroutines: {
        x: 1400,
        y: 560,
        w: 350,
        h: 180,
        fill: "#FFFBEB",
        stroke: colors.amber,
        title: "Coroutine adapters",
        lines: ["range.asFlow()", "windowedFlow / chunkedFlow", "flowOfDayRange(...)"],
        icon: "flow",
    },
    outcomes: {
        x: 650,
        y: 840,
        w: 1040,
        h: 120,
        fill: "#F8FAFC",
        stroke: colors.gray,
        title: "Reader outcomes",
        lines: ["Model temporal spans, align them to calendar rules, split them into sequences or Flows, and calculate business-time answers."],
        icon: "target",
    },
};

const edges = [
    {
        id: "foundationInterval",
        color: colors.blue,
        d: "M460 330 L590 330",
        from: "foundation",
        to: "interval",
        label: {x: 525, y: 303, text: "builds on", w: 90}
    },
    {
        id: "foundationRanges",
        color: colors.blue,
        d: "M460 415 L480 415 L480 650 L590 650",
        from: "foundation",
        to: "calendarRanges",
        label: {x: 480, y: 540, text: "temporal types", w: 116}
    },
    {
        id: "policyBusiness",
        color: colors.gray,
        d: "M460 715 L500 715 L500 500 L1575 500 L1575 410",
        from: "calendarPolicy",
        to: "business",
        label: {x: 558, y: 500, text: "rules", w: 62}
    },
    {
        id: "topOutcome",
        color: colors.green,
        d: "M1750 320 L1885 320 L1885 900 L1690 900",
        from: "business",
        to: "outcomes",
        label: {x: 1844, y: 612, text: "answers", w: 82}
    },
    {
        id: "bottomOutcome",
        color: colors.teal,
        d: "M1575 740 L1575 795 L1180 795 L1180 840",
        from: "coroutines",
        to: "outcomes",
        label: {x: 1518, y: 804, text: "streams", w: 82}
    },
];

function esc(v) {
    return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function marker(id, color) {
    return `<marker id="arrow-${id}" markerWidth="20" markerHeight="16" refX="18" refY="8" orient="auto" markerUnits="userSpaceOnUse" viewBox="0 0 20 16"><path d="M2 2 L18 8 L2 14 Z" fill="${color}"/></marker>`;
}

function iconPath(type, x, y, color) {
    const cx = x + 38;
    const cy = y + 46;
    const base = `<rect x="${x + 18}" y="${y + 26}" width="48" height="48" rx="12" fill="${color}"/>`;
    if (type === "clock") {
        return `${base}<circle cx="${cx}" cy="${cy}" r="15" fill="none" stroke="#FFFFFF" stroke-width="3"/><path d="M${cx} ${cy - 9} L${cx} ${cy} L${cx + 9} ${cy + 5}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`;
    }
    if (type === "settings") {
        return `${base}<circle cx="${cx}" cy="${cy}" r="11" fill="none" stroke="#FFFFFF" stroke-width="3"/><path d="M${cx - 18} ${cy} H${cx - 13} M${cx + 13} ${cy} H${cx + 18} M${cx} ${cy - 18} V${cy - 13} M${cx} ${cy + 13} V${cy + 18}" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>`;
    }
    if (type === "brackets") {
        return `${base}<path d="M${cx - 12} ${cy - 14} H${cx - 20} V${cy + 14} H${cx - 12} M${cx + 12} ${cy - 14} H${cx + 20} V${cy + 14} H${cx + 12}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`;
    }
    if (type === "block") {
        return `${base}<path d="M${cx - 16} ${cy - 10} H${cx + 16} M${cx - 16} ${cy} H${cx + 16} M${cx - 16} ${cy + 10} H${cx + 16}" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>`;
    }
    if (type === "calendar") {
        return `${base}<rect x="${cx - 17}" y="${cy - 14}" width="34" height="30" rx="5" fill="none" stroke="#FFFFFF" stroke-width="3"/><path d="M${cx - 17} ${cy - 4} H${cx + 17} M${cx - 9} ${cy - 19} V${cy - 10} M${cx + 9} ${cy - 19} V${cy - 10}" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>`;
    }
    if (type === "range") {
        return `${base}<path d="M${cx - 17} ${cy + 8} C${cx - 7} ${cy - 12}, ${cx + 7} ${cy - 12}, ${cx + 17} ${cy + 8}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/><circle cx="${cx - 17}" cy="${cy + 8}" r="4" fill="#FFFFFF"/><circle cx="${cx + 17}" cy="${cy + 8}" r="4" fill="#FFFFFF"/>`;
    }
    if (type === "route") {
        return `${base}<path d="M${cx - 15} ${cy - 8} H${cx + 2} V${cy + 10} H${cx + 16}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/><circle cx="${cx - 17}" cy="${cy - 8}" r="4" fill="#FFFFFF"/><circle cx="${cx + 17}" cy="${cy + 10}" r="4" fill="#FFFFFF"/>`;
    }
    if (type === "flow") {
        return `${base}<path d="M${cx - 18} ${cy - 9} H${cx + 18} M${cx - 18} ${cy + 9} H${cx + 4}" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/><path d="M${cx + 8} ${cy + 1} L${cx + 18} ${cy + 9} L${cx + 8} ${cy + 17}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`;
    }
    return `${base}<path d="M${cx - 15} ${cy} H${cx + 15}" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>`;
}

function card(id) {
    const c = cards[id];
    const textX = c.x + (id === "outcomes" ? 118 : 92);
    const titleY = c.y + 56;
    const detailY = c.y + 92;
    return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${c.fill}" stroke="${c.stroke}"/>
  ${iconPath(c.icon, c.x, c.y, c.stroke)}
  <text class="cardTitle" x="${textX}" y="${titleY}">${esc(c.title)}</text>
  ${c.lines.map((line, i) => `<text class="detail" x="${textX}" y="${detailY + i * 24}">${esc(line)}</text>`).join("\n  ")}
</g>`;
}

function label({x, y, text, w}) {
    return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 15})"><rect width="${w}" height="30" rx="8"/><text x="${w / 2}" y="20" text-anchor="middle">${esc(text)}</text></g>`;
}

function nums(d) {
    return d.match(/-?\d+(?:\.\d+)?/g).map(Number);
}

function segs(d) {
    const n = nums(d);
    const pts = [];
    for (let i = 0; i < n.length; i += 2) pts.push({x: n[i], y: n[i + 1]});
    return pts.slice(1).map((p, i) => ({a: pts[i], b: p}));
}

function touches(b, p) {
    const onX = p.x >= b.x - 0.1 && p.x <= b.x + b.w + 0.1;
    const onY = p.y >= b.y - 0.1 && p.y <= b.y + b.h + 0.1;
    return ((Math.abs(p.x - b.x) < 0.1 || Math.abs(p.x - (b.x + b.w)) < 0.1) && onY) ||
        ((Math.abs(p.y - b.y) < 0.1 || Math.abs(p.y - (b.y + b.h)) < 0.1) && onX);
}

function hits(s, b, pad = 8) {
    const box = {x: b.x + pad, y: b.y + pad, w: b.w - pad * 2, h: b.h - pad * 2};
    const minX = Math.min(s.a.x, s.b.x);
    const maxX = Math.max(s.a.x, s.b.x);
    const minY = Math.min(s.a.y, s.b.y);
    const maxY = Math.max(s.a.y, s.b.y);
    if (s.a.x === s.b.x) return s.a.x > box.x && s.a.x < box.x + box.w && maxY > box.y && minY < box.y + box.h;
    if (s.a.y === s.b.y) return s.a.y > box.y && s.a.y < box.y + box.h && maxX > box.x && minX < box.x + box.w;
    return false;
}

function validate() {
    for (const e of edges) {
        const n = nums(e.d);
        const start = {x: n[0], y: n[1]};
        const end = {x: n[n.length - 2], y: n[n.length - 1]};
        if (!touches(cards[e.from], start)) throw new Error(`${e.id} start`);
        if (!touches(cards[e.to], end)) throw new Error(`${e.id} end`);
        for (const s of segs(e.d)) {
            for (const [id, c] of Object.entries(cards)) {
                if ((id === e.from || id === e.to) && (touches(c, s.a) || touches(c, s.b))) continue;
                if (hits(s, c)) throw new Error(`${e.id} crosses ${id}`);
            }
        }
    }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="javatimes Feature Overview">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
${edges.map((e) => marker(e.id, e.color)).join("\n")}
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:16px;fill:${colors.muted}}.card{filter:url(#softShadow);stroke-width:2}.module{fill:#EFF6FF;stroke:${colors.blue};stroke-width:2.4}.moduleHeader{fill:#DBEAFE;stroke:${colors.blue};stroke-width:1.7}.moduleTitle{font-family:"Comic Mono";font-size:13.5px;font-weight:700;fill:${colors.blue};letter-spacing:0}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.detail{font-family:"Comic Mono";font-size:13.4px;fill:${colors.muted}}.edge{fill:none;stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.25;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="38" y="30" width="1964" height="1014" rx="8"/>
<text class="title" x="78" y="88">javatimes Feature Overview</text>
<text class="subtitle" x="82" y="120">The module extends core java.time DSLs into intervals, period algebra, calendar-aware ranges, business-day math, and Flow-friendly iteration.</text>
<rect class="module" x="535" y="180" width="1285" height="615" rx="8"/>
<rect class="moduleHeader" x="558" y="195" width="420" height="34" rx="8"/>
<text class="moduleTitle" x="580" y="217">provided by bluetape4k-javatimes</text>
<g id="edges">${edges.map((e) => `<path class="edge" d="${e.d}" stroke="${e.color}" marker-end="url(#arrow-${e.id})"/>`).join("\n")}</g>
<g id="labels">${edges.map((e) => label(e.label)).join("\n")}</g>
${Object.keys(cards).map(card).join("\n")}
</svg>`;

for (const e of edges) {
    if (!svg.includes(`id="arrow-${e.id}"`) || !svg.includes(`fill="${e.color}"`)) throw new Error(`marker color ${e.id}`);
}

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
