#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-01.png";
const W = 2420;
const H = 1320;

const evidence = [
  "utils/rule-engine/README.md",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Rule.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Facts.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/RuleSet.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/RuleEngine.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/RuleEngineConfig.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/DefaultRule.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/DefaultRuleEngine.kt",
];

for (const file of evidence) {
  if (!existsSync(file)) throw new Error(`Missing source evidence: ${file}`);
}

function assertContains(file, pattern, label) {
  const source = readFileSync(file, "utf8");
  if (!pattern.test(source)) throw new Error(`Expected ${label} in ${file}`);
}

assertContains(evidence[0], /Concept Overview[\s\S]*utils-rule-engine-diagram-01\.png/, "README concept overview slot");
assertContains(evidence[1], /interface Rule[\s\S]*fun evaluate\(facts: Facts\): Boolean[\s\S]*fun execute\(facts: Facts\)/, "Rule contract");
assertContains(evidence[2], /class Facts[\s\S]*ConcurrentHashMap[\s\S]*operator fun set\(name: String, value: Any\?\)/, "Facts mutable state");
assertContains(evidence[3], /open class RuleSet[\s\S]*TreeSet<Rule>[\s\S]*override fun iterator/, "priority-ordered RuleSet");
assertContains(evidence[4], /interface RuleEngine[\s\S]*fun check\(rules: RuleSet, facts: Facts\)[\s\S]*fun fire\(rules: RuleSet, facts: Facts\)/, "RuleEngine entrypoints");
assertContains(evidence[5], /skipOnFirstAppliedRule[\s\S]*skipOnFirstFailedRule[\s\S]*priorityThreshold/, "RuleEngineConfig stop policies");
assertContains(evidence[6], /class DefaultRule[\s\S]*condition\.evaluate\(facts\)[\s\S]*actions\.forEach/, "DefaultRule condition and actions");
assertContains(evidence[7], /for \(rule in rules\)[\s\S]*rule\.evaluate\(facts\)[\s\S]*rule\.execute\(facts\)/, "DefaultRuleEngine fire loop");

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
  gray: "#64748B",
};

const tones = {
  rules: { fill: "#EFF6FF", stroke: colors.blue, dark: "#1D4ED8" },
  engine: { fill: "#F0FDFA", stroke: colors.teal, dark: "#0F766E" },
  facts: { fill: "#F0FDF4", stroke: colors.green, dark: "#15803D" },
  rule: { fill: "#FAF5FF", stroke: colors.purple, dark: "#6D28D9" },
  policy: { fill: "#FFF7ED", stroke: colors.orange, dark: "#C2410C" },
  output: { fill: "#F8FAFC", stroke: colors.gray, dark: "#475569" },
};

const cards = {
  ruleSet: {
    x: 120,
    y: 235,
    w: 520,
    h: 240,
    tone: "rules",
    icon: "stack",
    kicker: "RuleSet",
    title: "priority-sorted rules",
    lines: ["stores Rule in TreeSet", "lower priority number fires first", "registerProxy() adapts annotated objects"],
    foot: "Iterable<Rule> is the engine input",
  },
  facts: {
    x: 120,
    y: 720,
    w: 520,
    h: 250,
    tone: "facts",
    icon: "data",
    kicker: "Facts",
    title: "shared mutable working data",
    lines: ["ConcurrentHashMap-backed key/value store", "get<T>(), put(), set(), remove()", "null assignment removes a fact"],
    foot: "Condition reads it; Action mutates it",
  },
  engine: {
    x: 840,
    y: 330,
    w: 620,
    h: 380,
    tone: "engine",
    icon: "loop",
    kicker: "DefaultRuleEngine.fire()",
    title: "iterate, evaluate, execute",
    lines: ["1. notify engine listeners", "2. iterate RuleSet in priority order", "3. call beforeEvaluate listeners", "4. evaluate rule, then execute on match"],
    foot: "check() returns Rule -> Boolean without executing actions",
  },
  rule: {
    x: 1690,
    y: 240,
    w: 560,
    h: 280,
    tone: "rule",
    icon: "rule",
    kicker: "Rule / DefaultRule",
    title: "condition + action",
    lines: ["evaluate(facts): Boolean", "execute(facts)", "DefaultRule delegates to Condition and Action list"],
    foot: "Rule is Comparable by priority, then name",
  },
  policy: {
    x: 840,
    y: 820,
    w: 620,
    h: 210,
    tone: "policy",
    icon: "switch",
    kicker: "RuleEngineConfig + listeners",
    title: "execution boundaries",
    lines: ["priorityThreshold can stop the loop", "skipOnFirstApplied/Failed/NonTriggered", "RuleListener and RuleEngineListener wrap events"],
  },
  outcome: {
    x: 1690,
    y: 720,
    w: 560,
    h: 250,
    tone: "output",
    icon: "result",
    kicker: "runtime outcome",
    title: "facts updated or rule skipped",
    lines: ["true condition -> actions mutate Facts", "false condition -> optional non-triggered stop", "exception -> optional failed-rule stop"],
    foot: "the caller keeps the same Facts instance",
  },
};

const edges = [
  { id: "rules-to-engine", from: "ruleSet", to: "engine", tone: "rules", points: [[640, 360], [840, 360]], label: "sorted RuleSet", labelAt: [675, 333] },
  { id: "facts-to-engine", from: "facts", to: "engine", tone: "facts", points: [[640, 845], [740, 845], [740, 595], [840, 595]], label: "Facts input", labelAt: [660, 818] },
  { id: "engine-to-rule", from: "engine", to: "rule", tone: "engine", points: [[1460, 455], [1690, 455]], label: "evaluate / execute", labelAt: [1505, 428] },
  { id: "engine-to-policy", from: "engine", to: "policy", tone: "policy", dashed: true, points: [[1150, 710], [1150, 820]], label: "stop policy", labelAt: [1170, 775] },
  { id: "rule-to-outcome", from: "rule", to: "outcome", tone: "rule", points: [[1970, 520], [1970, 720]], label: "condition true", labelAt: [1990, 628] },
  { id: "outcome-to-facts", from: "outcome", to: "facts", tone: "facts", points: [[1690, 845], [1555, 845], [1555, 1110], [380, 1110], [380, 970]], label: "actions mutate facts", labelAt: [860, 1083] },
];

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function icon(kind, x, y, tone) {
  const t = tones[tone];
  const cx = x + 46;
  const cy = y + 50;
  const box = `<rect x="${x + 22}" y="${y + 26}" width="54" height="54" rx="13" fill="${t.dark}"/>`;
  if (kind === "stack") return `${box}<path d="M${cx - 16} ${cy - 13} H${cx + 16} M${cx - 16} ${cy} H${cx + 16} M${cx - 16} ${cy + 13} H${cx + 16}" stroke="#fff" stroke-width="4" stroke-linecap="round"/>`;
  if (kind === "data") return `${box}<path d="M${cx - 16} ${cy - 12} C${cx - 16} ${cy - 21}, ${cx + 16} ${cy - 21}, ${cx + 16} ${cy - 12} V${cy + 12} C${cx + 16} ${cy + 21}, ${cx - 16} ${cy + 21}, ${cx - 16} ${cy + 12} Z M${cx - 16} ${cy} C${cx - 16} ${cy + 9}, ${cx + 16} ${cy + 9}, ${cx + 16} ${cy}" fill="none" stroke="#fff" stroke-width="3"/>`;
  if (kind === "loop") return `${box}<path d="M${cx - 16} ${cy - 4} C${cx - 8} ${cy - 18}, ${cx + 14} ${cy - 18}, ${cx + 16} ${cy - 2} M${cx + 16} ${cy - 2} L${cx + 7} ${cy - 7} M${cx + 16} ${cy - 2} L${cx + 10} ${cy + 6} M${cx + 16} ${cy + 5} C${cx + 8} ${cy + 18}, ${cx - 14} ${cy + 18}, ${cx - 16} ${cy + 2} M${cx - 16} ${cy + 2} L${cx - 7} ${cy + 7} M${cx - 16} ${cy + 2} L${cx - 10} ${cy - 6}" fill="none" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (kind === "rule") return `${box}<path d="M${cx - 16} ${cy - 12} H${cx + 16} M${cx - 16} ${cy} H${cx + 4} M${cx - 16} ${cy + 12} H${cx + 16}" stroke="#fff" stroke-width="3.3" stroke-linecap="round"/><path d="M${cx + 10} ${cy - 4} L${cx + 17} ${cy + 3} L${cx + 6} ${cy + 14}" fill="none" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>`;
  if (kind === "switch") return `${box}<path d="M${cx - 15} ${cy - 10} H${cx + 15} M${cx - 15} ${cy + 10} H${cx + 15}" stroke="#fff" stroke-width="3.2" stroke-linecap="round"/><circle cx="${cx - 4}" cy="${cy - 10}" r="5" fill="#fff"/><circle cx="${cx + 8}" cy="${cy + 10}" r="5" fill="#fff"/>`;
  return `${box}<path d="M${cx - 15} ${cy - 4} L${cx - 4} ${cy + 8} L${cx + 16} ${cy - 13}" fill="none" stroke="#fff" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>`;
}

function card(id) {
  const c = cards[id];
  const t = tones[c.tone];
  const textX = c.x + 104;
  return `<g id="${id}">
  <rect class="card" x="${c.x}" y="${c.y}" width="${c.w}" height="${c.h}" rx="8" fill="${t.fill}" stroke="${t.stroke}"/>
  ${icon(c.icon, c.x, c.y, c.tone)}
  <text class="kicker" x="${textX}" y="${c.y + 39}">${esc(c.kicker)}</text>
  <text class="cardTitle" x="${textX}" y="${c.y + 72}">${esc(c.title)}</text>
  <path class="divider" d="M${c.x} ${c.y + 100}H${c.x + c.w}" stroke="${t.dark}"/>
  ${c.lines.map((line, i) => `<text class="body" x="${c.x + 28}" y="${c.y + 136 + i * 25}">${esc(line)}</text>`).join("\n  ")}
  ${c.foot ? `<path class="divider" d="M${c.x} ${c.y + c.h - 48}H${c.x + c.w}" stroke="${t.dark}"/><text class="foot" x="${c.x + 28}" y="${c.y + c.h - 18}">${esc(c.foot)}</text>` : ""}
</g>`;
}

function markerDefs() {
  return Object.entries(tones).map(([name, tone]) => `<marker id="arrow-${name}" markerWidth="24" markerHeight="18" refX="22" refY="9" orient="auto" markerUnits="userSpaceOnUse"><path d="M2 2 L22 9 L2 16 Z" fill="${tone.dark}" stroke="${tone.dark}" stroke-width="1" stroke-dasharray="none"/></marker>`).join("\n  ");
}

function pathD(points) {
  return points.map((p, i) => `${i === 0 ? "M" : "L"}${p[0]} ${p[1]}`).join(" ");
}

function edge(e) {
  const tone = tones[e.tone];
  const w = Math.max(112, e.label.length * 8.2 + 28);
  return `<g id="${e.id}">
  <path class="edge ${e.dashed ? "dashed" : ""}" d="${pathD(e.points)}" stroke="${tone.dark}" marker-end="url(#arrow-${e.tone})"/>
  <rect class="edgeLabelBg" x="${e.labelAt[0] - 10}" y="${e.labelAt[1] - 18}" width="${w}" height="26" rx="6"/>
  <text class="edgeLabel" x="${e.labelAt[0]}" y="${e.labelAt[1]}">${esc(e.label)}</text>
</g>`;
}

function pointTouchesBox(c, [x, y]) {
  const inX = x >= c.x - 0.1 && x <= c.x + c.w + 0.1;
  const inY = y >= c.y - 0.1 && y <= c.y + c.h + 0.1;
  return ((Math.abs(x - c.x) < 0.1 || Math.abs(x - (c.x + c.w)) < 0.1) && inY) ||
    ((Math.abs(y - c.y) < 0.1 || Math.abs(y - (c.y + c.h)) < 0.1) && inX);
}

function segments(points) {
  return points.slice(1).map((p, i) => ({ a: points[i], b: p }));
}

function segmentHitsBox(seg, c, pad = 6) {
  const box = { x: c.x + pad, y: c.y + pad, w: c.w - pad * 2, h: c.h - pad * 2 };
  const minX = Math.min(seg.a[0], seg.b[0]);
  const maxX = Math.max(seg.a[0], seg.b[0]);
  const minY = Math.min(seg.a[1], seg.b[1]);
  const maxY = Math.max(seg.a[1], seg.b[1]);
  if (seg.a[0] === seg.b[0]) return seg.a[0] > box.x && seg.a[0] < box.x + box.w && maxY > box.y && minY < box.y + box.h;
  if (seg.a[1] === seg.b[1]) return seg.a[1] > box.y && seg.a[1] < box.y + box.h && maxX > box.x && minX < box.x + box.w;
  throw new Error(`Non-orthogonal segment: ${JSON.stringify(seg)}`);
}

function properCross(a, b) {
  if (a.a[1] === a.b[1] && b.a[0] === b.b[0]) {
    const y = a.a[1];
    const x = b.a[0];
    return x > Math.min(a.a[0], a.b[0]) && x < Math.max(a.a[0], a.b[0]) &&
      y > Math.min(b.a[1], b.b[1]) && y < Math.max(b.a[1], b.b[1]);
  }
  if (a.a[0] === a.b[0] && b.a[1] === b.b[1]) return properCross(b, a);
  return false;
}

function validateGeometry() {
  for (const e of edges) {
    if (!pointTouchesBox(cards[e.from], e.points[0])) throw new Error(`${e.id} start does not touch ${e.from}`);
    if (!pointTouchesBox(cards[e.to], e.points[e.points.length - 1])) throw new Error(`${e.id} end does not touch ${e.to}`);
    for (const seg of segments(e.points)) {
      for (const [id, c] of Object.entries(cards)) {
        if (id === e.from || id === e.to) continue;
        if (segmentHitsBox(seg, c)) throw new Error(`${e.id} crosses card ${id}`);
      }
    }
  }
  for (let i = 0; i < edges.length; i += 1) {
    for (let j = i + 1; j < edges.length; j += 1) {
      for (const a of segments(edges[i].points)) {
        for (const b of segments(edges[j].points)) {
          if (properCross(a, b)) throw new Error(`${edges[i].id} crosses ${edges[j].id}`);
        }
      }
    }
  }
}

validateGeometry();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="rule engine concept overview">
<defs>
  <filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${colors.muted}}
    .lane{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.6;stroke-dasharray:12 8}.laneTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}
    .card{stroke-width:1.9;filter:url(#softShadow)}.kicker{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}
    .cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${colors.ink}}.body{font-family:"Comic Mono";font-size:13.4px;fill:#334155}
    .foot{font-family:"Comic Mono";font-size:12.6px;fill:${colors.muted}}.divider{stroke-width:1.15;opacity:.45}
    .edge{fill:none;stroke-width:3.55;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:10 8}
    .edgeLabelBg{fill:#FFFFFF;stroke:#E2E8F0;stroke-width:1;opacity:.95}.edgeLabel{font-family:"Comic Mono";font-size:12.7px;fill:#334155}
  </style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 64}" rx="8"/>
<text class="title" x="74" y="88">Rule Engine Concept Overview</text>
<text class="subtitle" x="78" y="121">Rules are ordered by RuleSet, DefaultRuleEngine evaluates each condition against shared Facts, and matching actions update the same Facts instance.</text>
<rect class="lane" x="82" y="185" width="640" height="845" rx="8"/><text class="laneTitle" x="110" y="222">inputs</text>
<rect class="lane" x="780" y="185" width="730" height="905" rx="8"/><text class="laneTitle" x="808" y="222">engine loop</text>
<rect class="lane" x="1630" y="185" width="680" height="845" rx="8"/><text class="laneTitle" x="1658" y="222">rule contract and result</text>
<g id="edges">${edges.map(edge).join("\n")}</g>
<g id="cards">${Object.keys(cards).map(card).join("\n")}</g>
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
