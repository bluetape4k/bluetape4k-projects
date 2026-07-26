#!/usr/bin/env node
import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-05.svg",
    pngPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-05.png";
const W = 1780, H = 1480;
const files = ["utils/rule-engine/README.md", "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/InferenceRuleEngine.kt", "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/DefaultRuleEngine.kt", "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Facts.kt", "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/RuleSet.kt"];
for (const f of files) if (!existsSync(f)) throw new Error(`Missing source evidence: ${f}`);

function need(f, p, l) {
    if (!p.test(readFileSync(f, "utf8"))) throw new Error(`Expected ${l} in ${f}`)
}

need(files[0], /InferenceRuleEngine \(Forward Chaining\)[\s\S]*utils-rule-engine-diagram-05\.png/, "README slot");
need(files[1], /do \{[\s\S]*selectedRules = selectCandidates\(rules, facts\)[\s\S]*delegate\.doFire\(RuleSet\(selectedRules\), facts\)[\s\S]*\} while \(selectedRules\.isNotEmpty\(\)\)/, "forward chaining loop");
need(files[1], /private fun selectCandidates[\s\S]*rules\.filter \{ it\.evaluate\(facts\) \}/, "candidate selection");
need(files[2], /internal open fun doFire[\s\S]*rule\.execute\(facts\)/, "delegate doFire");
const C = {
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
    gray: "#64748B"
};
const tones = {
    input: ["#EFF6FF", C.blue, "#1D4ED8"],
    loop: ["#F0FDFA", C.teal, "#0F766E"],
    exec: ["#FAF5FF", C.purple, "#6D28D9"],
    facts: ["#F0FDF4", C.green, "#15803D"],
    stop: ["#FFF7ED", C.orange, "#C2410C"],
    out: ["#F8FAFC", C.gray, "#475569"]
};
const cards = {
    start: {
        x: 610,
        y: 190,
        w: 560,
        h: 205,
        tone: "input",
        title: "InferenceRuleEngine.fire()",
        kicker: "entrypoint",
        lines: ["accepts RuleSet + Facts", "keeps DefaultRuleEngine delegate", "starts with all configured rules"]
    },
    select: {
        x: 610,
        y: 480,
        w: 560,
        h: 225,
        tone: "loop",
        title: "selectCandidates()",
        kicker: "evaluation pass",
        lines: ["evaluate every rule against current Facts", "keep only rules that return true", "TreeSet preserves priority order"]
    },
    any: {
        x: 640,
        y: 790,
        w: 500,
        h: 195,
        tone: "stop",
        title: "any candidates?",
        kicker: "loop condition",
        lines: ["empty set stops the chain", "non-empty set executes one cycle"]
    },
    fire: {
        x: 610,
        y: 1070,
        w: 560,
        h: 235,
        tone: "exec",
        title: "delegate.doFire()",
        kicker: "execution pass",
        lines: ["wrap selected rules as RuleSet", "execute selected rules in priority order", "actions mutate the same Facts"]
    },
    facts: {
        x: 80,
        y: 1045,
        w: 390,
        h: 245,
        tone: "facts",
        title: "Facts changes",
        kicker: "working memory",
        lines: ["new or changed facts may satisfy more rules", "same Facts instance flows into next cycle", "no snapshot is created"]
    },
    done: {
        x: 1260,
        y: 790,
        w: 390,
        h: 220,
        tone: "out",
        title: "no candidates remain",
        kicker: "terminal state",
        lines: ["last Facts state is the result", "engine returns to caller", "check() still delegates once"]
    },
};
const edges = [
    ["start", "select", "input", [[890, 395], [890, 480]], "start selection", [910, 440]],
    ["select", "any", "loop", [[890, 705], [890, 790]], "selectedRules", [910, 750]],
    ["any", "fire", "exec", [[890, 985], [890, 1070]], "non-empty", [910, 1030]],
    ["fire", "facts", "facts", [[610, 1188], [470, 1188]], "actions mutate", [486, 1158]],
    ["facts", "select", "facts", [[275, 1045], [275, 585], [610, 585]], "re-evaluate", [300, 805]],
    ["any", "done", "stop", [[1140, 887], [1260, 887]], "empty", [1168, 858]],
];

function esc(v) {
    return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;")
}

function marker() {
    return Object.entries(tones).map(([k, t]) => `<marker id="arrow-${k}" markerWidth="24" markerHeight="18" refX="22" refY="9" orient="auto" markerUnits="userSpaceOnUse"><path d="M2 2 L22 9 L2 16 Z" fill="${t[2]}"/></marker>`).join("")
}

function card(id) {
    const d = cards[id], t = tones[d.tone];
    return `<g id="${id}"><rect class="card" x="${d.x}" y="${d.y}" width="${d.w}" height="${d.h}" rx="8" fill="${t[0]}" stroke="${t[1]}"/><text class="kicker" x="${d.x + 24}" y="${d.y + 34}">${esc(d.kicker)}</text><text class="cardTitle" x="${d.x + 24}" y="${d.y + 70}">${esc(d.title)}</text><path class="divider" d="M${d.x} ${d.y + 94}H${d.x + d.w}" stroke="${t[2]}"/>${d.lines.map((l, i) => `<text class="body" x="${d.x + 24}" y="${d.y + 130 + i * 25}">${esc(l)}</text>`).join("")}</g>`
}

function path(p) {
    return p.map((q, i) => `${i ? "L" : "M"}${q[0]} ${q[1]}`).join(" ")
}

function edge(e) {
    const t = tones[e[2]], w = Math.max(94, e[4].length * 8 + 26);
    return `<path class="edge" d="${path(e[3])}" stroke="${t[2]}" marker-end="url(#arrow-${e[2]})"/><rect class="edgeLabelBg" x="${e[5][0] - 8}" y="${e[5][1] - 18}" width="${w}" height="26" rx="6"/><text class="edgeLabel" x="${e[5][0]}" y="${e[5][1]}">${esc(e[4])}</text>`
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="InferenceRuleEngine forward chaining"><defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/></filter>${marker()}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${C.canvas}}.frame{fill:${C.frame};stroke:${C.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:46px;fill:${C.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${C.muted}}.card{stroke-width:1.9;filter:url(#softShadow)}.kicker{font-family:"Comic Mono";font-size:13px;fill:${C.muted}}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${C.ink}}.body{font-family:"Comic Mono";font-size:13.4px;fill:#334155}.divider{stroke-width:1.15;opacity:.45}.edge{fill:none;stroke-width:3.5;stroke-linecap:round;stroke-linejoin:round}.edgeLabelBg{fill:#fff;stroke:#E2E8F0;stroke-width:1;opacity:.95}.edgeLabel{font-family:"Comic Mono";font-size:12.7px;fill:#334155}</style></defs><rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 64}" rx="8"/><text class="title" x="74" y="88">InferenceRuleEngine Forward Chaining</text><text class="subtitle" x="78" y="121">The engine repeatedly selects currently matching rules, delegates one execution cycle, and re-evaluates until no rule remains applicable.</text><g id="edges">${edges.map(edge).join("")}</g><g id="cards">${Object.keys(cards).map(card).join("")}</g></svg>`;
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
