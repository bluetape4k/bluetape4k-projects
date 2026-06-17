#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-03.svg";
const pngPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-03.png";
const W = 2180;
const H = 1260;

const files = [
  "utils/rule-engine/README.md",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/RuleEngine.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/SuspendRuleEngine.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/RuleEngineConfig.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/RuleListener.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/RuleEngineListener.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/DefaultRuleEngine.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/DefaultSuspendRuleEngine.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/InferenceRuleEngine.kt",
];

for (const file of files) if (!existsSync(file)) throw new Error(`Missing source evidence: ${file}`);
function need(file, pattern, label) {
  if (!pattern.test(readFileSync(file, "utf8"))) throw new Error(`Expected ${label} in ${file}`);
}
need(files[0], /Rule Engine Class Diagram[\s\S]*utils-rule-engine-diagram-03\.png/, "README diagram slot");
need(files[1], /interface RuleEngine[\s\S]*fun check\(rules: RuleSet, facts: Facts\)[\s\S]*fun fire\(rules: RuleSet, facts: Facts\)/, "RuleEngine");
need(files[2], /interface SuspendRuleEngine[\s\S]*suspend fun check[\s\S]*suspend fun fire/, "SuspendRuleEngine");
need(files[3], /data class RuleEngineConfig[\s\S]*skipOnFirstAppliedRule[\s\S]*priorityThreshold/, "RuleEngineConfig");
need(files[6], /open class DefaultRuleEngine[\s\S]*: RuleEngine[\s\S]*for \(rule in rules\)/, "DefaultRuleEngine");
need(files[7], /open class DefaultSuspendRuleEngine[\s\S]*: SuspendRuleEngine[\s\S]*CancellationException/, "DefaultSuspendRuleEngine");
need(files[8], /class InferenceRuleEngine[\s\S]*: RuleEngine[\s\S]*do \{[\s\S]*selectCandidates/, "InferenceRuleEngine");

const c = {
  ink: "#0F172A", muted: "#475569", canvas: "#F8FAFC", frame: "#FFFFFF", line: "#CBD5E1",
  blue: "#2563EB", teal: "#0D9488", orange: "#EA580C", purple: "#7C3AED", green: "#16A34A", gray: "#64748B",
};

const boxes = {
  ruleEngine: { x: 120, y: 215, w: 500, h: 220, fill: "#EFF6FF", stroke: c.blue, stereotype: "<<interface>>", title: "RuleEngine", attrs: ["+ config: RuleEngineConfig", "+ ruleListeners", "+ ruleEngineListeners"], methods: ["+ check(ruleSet, facts)", "+ fire(ruleSet, facts)"] },
  defaultEngine: { x: 120, y: 575, w: 500, h: 250, fill: "#EFF6FF", stroke: c.blue, stereotype: "<<class>>", title: "DefaultRuleEngine", attrs: ["- CopyOnWriteArrayList listeners", "+ config"], methods: ["+ fire(ruleSet, facts)", "+ check(ruleSet, facts)", "# doFire(ruleSet, facts)"] },
  inference: { x: 120, y: 935, w: 500, h: 190, fill: "#FAF5FF", stroke: c.purple, stereotype: "<<class>>", title: "InferenceRuleEngine", attrs: ["- delegate: DefaultRuleEngine"], methods: ["+ fire() loops selected candidates", "+ check() delegates"] },
  config: { x: 820, y: 215, w: 520, h: 250, fill: "#FFF7ED", stroke: c.orange, stereotype: "<<data class>>", title: "RuleEngineConfig", attrs: ["+ skipOnFirstAppliedRule", "+ skipOnFirstFailedRule", "+ skipOnFirstNonTriggeredRule", "+ priorityThreshold"], methods: ["+ DEFAULT", "+ require threshold >= 0"] },
  ruleListener: { x: 820, y: 575, w: 520, h: 230, fill: "#F0FDF4", stroke: c.green, stereotype: "<<interface>>", title: "RuleListener", attrs: [], methods: ["+ beforeEvaluate(rule, facts): Boolean", "+ afterEvaluate(rule, facts, result)", "+ beforeExecute(rule, facts)", "+ afterExecute(rule, facts, exception)"] },
  engineListener: { x: 820, y: 925, w: 520, h: 200, fill: "#F8FAFC", stroke: c.gray, stereotype: "<<interface>>", title: "RuleEngineListener", attrs: [], methods: ["+ beforeEvaluate(rules, facts)", "+ afterExecute(rules, facts)"] },
  suspendEngine: { x: 1575, y: 215, w: 500, h: 210, fill: "#F0FDFA", stroke: c.teal, stereotype: "<<interface>>", title: "SuspendRuleEngine", attrs: ["+ config: RuleEngineConfig"], methods: ["+ suspend check(rules, facts)", "+ suspend fire(rules, facts)"] },
  defaultSuspend: { x: 1575, y: 575, w: 500, h: 250, fill: "#F0FDFA", stroke: c.teal, stereotype: "<<class>>", title: "DefaultSuspendRuleEngine", attrs: ["+ config"], methods: ["+ suspend fire()", "+ suspend check()", "+ rethrows CancellationException"] },
};

const edges = [
  { id: "default-implements", from: "defaultEngine", to: "ruleEngine", type: "implements", color: c.blue, d: "M370 575 L370 435" },
  { id: "inference-implements", from: "inference", to: "ruleEngine", type: "implements", color: c.purple, d: "M120 1030 L80 1030 L80 325 L120 325" },
  { id: "suspend-implements", from: "defaultSuspend", to: "suspendEngine", type: "implements", color: c.teal, d: "M1825 575 L1825 425" },
  { id: "default-listeners", from: "defaultEngine", to: "ruleListener", type: "has", color: c.green, d: "M620 700 L820 700", label: { x: 700, y: 672, text: "per-rule events", w: 132 } },
  { id: "default-engine-listeners", from: "defaultEngine", to: "engineListener", type: "has", color: c.gray, d: "M620 785 L700 785 L700 1025 L820 1025", label: { x: 705, y: 920, text: "engine events", w: 122 } },
];

function esc(v) { return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;"); }
function box(id) {
  const b = boxes[id]; const sep = b.y + 66; const attrH = Math.max(40, Math.max(1, b.attrs.length) * 20 + 18); const sep2 = b.attrs.length ? sep + attrH : sep;
  return `<g id="${id}"><rect class="umlBox" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/>
  <line x1="${b.x}" y1="${sep}" x2="${b.x + b.w}" y2="${sep}" stroke="${b.stroke}" stroke-width="1.3" opacity=".65"/>${b.attrs.length ? `<line x1="${b.x}" y1="${sep2}" x2="${b.x + b.w}" y2="${sep2}" stroke="${b.stroke}" stroke-width="1.3" opacity=".65"/>` : ""}
  <text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 25}" text-anchor="middle">${esc(b.stereotype)}</text><text class="classTitle" x="${b.x + b.w / 2}" y="${b.y + 52}" text-anchor="middle">${esc(b.title)}</text>
  ${b.attrs.map((line, i) => `<text class="member" x="${b.x + 24}" y="${sep + 25 + i * 20}">${esc(line)}</text>`).join("\n  ")}
  ${b.methods.map((line, i) => `<text class="member" x="${b.x + 24}" y="${sep2 + 25 + i * 20}">${esc(line)}</text>`).join("\n  ")}</g>`;
}
function nums(d) { return d.match(/-?\d+(?:\.\d+)?/g).map(Number); }
function arrow(e) {
  const n = nums(e.d); const end = { x: n[n.length - 2], y: n[n.length - 1] }; const prev = { x: n[n.length - 4], y: n[n.length - 3] }; const dx = end.x - prev.x; const dy = end.y - prev.y;
  if (e.type === "implements") {
    if (Math.abs(dy) >= Math.abs(dx) && dy < 0) return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 8} ${end.y + 16} L${end.x + 8} ${end.y + 16} Z" fill="#FFFFFF" stroke="${e.color}"/>`;
    if (dx < 0) return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x + 16} ${end.y - 8} L${end.x + 16} ${end.y + 8} Z" fill="#FFFFFF" stroke="${e.color}"/>`;
    return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 16} ${end.y - 8} L${end.x - 16} ${end.y + 8} Z" fill="#FFFFFF" stroke="${e.color}"/>`;
  }
  if (dx > 0) return `<path class="solidOpenHead" d="M${end.x - 13} ${end.y - 7} L${end.x} ${end.y} L${end.x - 13} ${end.y + 7}" stroke="${e.color}"/>`;
  if (dx < 0) return `<path class="solidOpenHead" d="M${end.x + 13} ${end.y - 7} L${end.x} ${end.y} L${end.x + 13} ${end.y + 7}" stroke="${e.color}"/>`;
  return `<path class="solidOpenHead" d="M${end.x - 7} ${end.y - 13} L${end.x} ${end.y} L${end.x + 7} ${end.y - 13}" stroke="${e.color}"/>`;
}
function label(l) { return `<g class="edgeLabel" transform="translate(${l.x - l.w / 2} ${l.y - 14})"><rect width="${l.w}" height="28" rx="8"/><text x="${l.w / 2}" y="19" text-anchor="middle">${esc(l.text)}</text></g>`; }

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="rule engine class diagram"><defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/></filter><style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${c.muted}}.section{fill:#F3F8FF;stroke:#94A3B8;stroke-width:1.7;stroke-dasharray:12 8}.sectionTitle{font-family:"Comic Mono";font-size:13px;fill:${c.muted}}.umlBox{filter:url(#softShadow);stroke-width:2}.stereo{font-family:"Comic Mono";font-size:12.2px;fill:${c.muted}}.classTitle{font-family:"Architects Daughter";font-size:23px;fill:${c.ink}}.member{font-family:"Comic Mono";font-size:12.8px;fill:${c.muted}}.edge{fill:none;stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round}.implements{stroke-dasharray:8 7}.solidHead{stroke-width:1.9;stroke-linejoin:round;stroke-dasharray:none}.solidOpenHead{fill:none;stroke-width:2.25;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:none}.edgeLabel rect{fill:#fff;stroke:${c.line};stroke-width:1.2;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:11.8px;fill:${c.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/>
<text class="title" x="74" y="86">Rule Engine Class Diagram</text><text class="subtitle" x="78" y="118">Synchronous, coroutine, and forward-chaining engines share RuleEngineConfig policies; DefaultRuleEngine also exposes listener hooks around each rule.</text>
<rect class="section" x="74" y="150" width="2028" height="1010" rx="8"/><text class="sectionTitle" x="102" y="176">engine contracts, concrete implementations, policies, and listener extension points</text>
<g id="edges">${edges.map((e) => `<path class="edge ${e.type}" d="${e.d}" stroke="${e.color}"/>`).join("\n")}</g><g id="arrowheads">${edges.map(arrow).join("\n")}</g><g id="labels">${edges.filter((e) => e.label).map((e) => label(e.label)).join("\n")}</g>${Object.keys(boxes).map(box).join("\n")}</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
