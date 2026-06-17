#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-02.svg";
const pngPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-02.png";
const W = 2520;
const H = 1360;

const files = {
  readme: "utils/rule-engine/README.md",
  rule: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Rule.kt",
  facts: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Facts.kt",
  condition: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Condition.kt",
  action: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Action.kt",
  ruleSet: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/RuleSet.kt",
  abstractRule: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/AbstractRule.kt",
  defaultRule: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/DefaultRule.kt",
  dsl: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/RuleDsl.kt",
  proxy: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/RuleProxy.kt",
};

for (const file of Object.values(files)) {
  if (!existsSync(file)) throw new Error(`Missing source evidence: ${file}`);
}

function requirePattern(file, pattern, label) {
  if (!pattern.test(readFileSync(file, "utf8"))) throw new Error(`Expected ${label} in ${file}`);
}

requirePattern(files.readme, /Core Class Diagram[\s\S]*utils-rule-engine-diagram-02\.png/, "README core class diagram slot");
requirePattern(files.rule, /interface Rule: Comparable<Rule>[\s\S]*fun evaluate\(facts: Facts\): Boolean[\s\S]*fun execute\(facts: Facts\)/, "Rule interface");
requirePattern(files.facts, /class Facts[\s\S]*ConcurrentHashMap[\s\S]*fun <T> get\(name: String\): T\?[\s\S]*operator fun set/, "Facts API");
requirePattern(files.condition, /fun interface Condition[\s\S]*fun evaluate\(facts: Facts\): Boolean[\s\S]*val TRUE/, "Condition interface");
requirePattern(files.action, /fun interface Action[\s\S]*fun execute\(facts: Facts\)/, "Action interface");
requirePattern(files.ruleSet, /open class RuleSet[\s\S]*TreeSet<Rule>[\s\S]*fun register\(rule: Rule\)/, "RuleSet");
requirePattern(files.abstractRule, /abstract class AbstractRule[\s\S]*: Rule[\s\S]*override fun evaluate/, "AbstractRule");
requirePattern(files.defaultRule, /open class DefaultRule[\s\S]*val condition: Condition[\s\S]*val actions: List<Action>[\s\S]*\): AbstractRule/, "DefaultRule");
requirePattern(files.dsl, /class RuleBuilder[\s\S]*internal fun build\(\): DefaultRule[\s\S]*fun rule\(setup: RuleBuilder\.\(\) -> Unit\): DefaultRule/, "Rule DSL builder");
requirePattern(files.proxy, /class RuleProxy[\s\S]*InvocationHandler[\s\S]*fun asRule\(rule: Any\): Rule[\s\S]*fun Any\.asRule\(\): Rule/, "RuleProxy");

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

const boxes = {
  facts: { x: 120, y: 220, w: 470, h: 235, fill: "#F0FDF4", stroke: colors.green, stereotype: "<<class>>", title: "Facts", attrs: ["- facts: ConcurrentHashMap<String, Any?>"], methods: ["+ get<T>(name): T?", "+ set(name, value)", "+ put/remove/containsKey"] },
  condition: { x: 720, y: 220, w: 430, h: 190, fill: "#EFF6FF", stroke: colors.blue, stereotype: "<<fun interface>>", title: "Condition", attrs: ["+ TRUE / FALSE"], methods: ["+ evaluate(facts): Boolean"] },
  action: { x: 1230, y: 220, w: 430, h: 190, fill: "#EFF6FF", stroke: colors.blue, stereotype: "<<fun interface>>", title: "Action", attrs: [], methods: ["+ execute(facts): Unit"] },
  rule: { x: 1800, y: 190, w: 560, h: 265, fill: "#FAF5FF", stroke: colors.purple, stereotype: "<<interface>>", title: "Rule", attrs: ["+ name: String", "+ description: String", "+ priority: Int"], methods: ["+ evaluate(facts): Boolean", "+ execute(facts)", "+ compareTo(other)"] },
  ruleSet: { x: 120, y: 590, w: 500, h: 225, fill: "#F8FAFC", stroke: colors.gray, stereotype: "<<class>>", title: "RuleSet", attrs: ["+ rules: TreeSet<Rule>"], methods: ["+ register(rule)", "+ registerProxy(ruleObject)", "+ iterator(): Iterator<Rule>"] },
  ruleProxy: { x: 760, y: 570, w: 500, h: 235, fill: "#FFF7ED", stroke: colors.orange, stereotype: "<<InvocationHandler>>", title: "RuleProxy", attrs: ["- target: Any", "- conditionMethod", "- actionMethodBeans"], methods: ["+ asRule(ruleObject): Rule", "+ invoke(proxy, method, args)"] },
  abstractRule: { x: 1800, y: 560, w: 560, h: 220, fill: "#FAF5FF", stroke: colors.purple, stereotype: "<<abstract class>>", title: "AbstractRule", attrs: ["+ name / description / priority"], methods: ["+ evaluate(facts): Boolean = false", "+ execute(facts): Unit", "+ equals/hashCode by name"] },
  ruleBuilder: { x: 760, y: 895, w: 500, h: 225, fill: "#F0FDFA", stroke: colors.teal, stereotype: "<<DSL builder>>", title: "RuleBuilder", attrs: ["+ condition: Condition", "+ actions: MutableList<Action>"], methods: ["+ condition { ... }", "+ action { ... }", "+ build(): DefaultRule"] },
  defaultRule: { x: 1800, y: 880, w: 560, h: 250, fill: "#FAF5FF", stroke: colors.purple, stereotype: "<<class>>", title: "DefaultRule", attrs: ["+ condition: Condition", "+ actions: List<Action>"], methods: ["+ evaluate(facts) = condition.evaluate(facts)", "+ execute(facts) runs each action"] },
};

const edges = [
  { id: "abstract-implements-rule", from: "abstractRule", to: "rule", type: "implements", color: colors.purple, d: "M2080 560 L2080 455" },
  { id: "default-extends-abstract", from: "defaultRule", to: "abstractRule", type: "extends", color: colors.purple, d: "M2080 880 L2080 780" },
  { id: "default-has-condition", from: "defaultRule", to: "condition", type: "has", color: colors.blue, d: "M1800 950 L1440 950 L1440 445 L935 445 L935 410", label: { x: 1234, y: 418, text: "has one condition", w: 138 } },
  { id: "default-has-actions", from: "defaultRule", to: "action", type: "has", color: colors.blue, d: "M1940 880 L1940 840 L1510 840 L1510 410", label: { x: 1532, y: 704, text: "has actions", w: 106 } },
  { id: "builder-creates-default", from: "ruleBuilder", to: "defaultRule", type: "uses", color: colors.teal, d: "M1260 1005 L1800 1005", label: { x: 1450, y: 978, text: "builds DefaultRule", w: 148 } },
];

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function box(id) {
  const b = boxes[id];
  const nameSepY = b.y + 67;
  const attrHeight = Math.max(40, Math.max(1, b.attrs.length) * 20 + 20);
  const methodSepY = b.attrs.length ? nameSepY + attrHeight : nameSepY;
  const attrY = nameSepY + 25;
  const methodY = methodSepY + 25;
  return `<g id="${id}">
  <rect class="umlBox" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/>
  <line x1="${b.x}" y1="${nameSepY}" x2="${b.x + b.w}" y2="${nameSepY}" stroke="${b.stroke}" stroke-width="1.3" opacity="0.65"/>
  ${b.attrs.length ? `<line x1="${b.x}" y1="${methodSepY}" x2="${b.x + b.w}" y2="${methodSepY}" stroke="${b.stroke}" stroke-width="1.3" opacity="0.65"/>` : ""}
  <text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 25}" text-anchor="middle">${esc(b.stereotype)}</text>
  <text class="classTitle" x="${b.x + b.w / 2}" y="${b.y + 53}" text-anchor="middle">${esc(b.title)}</text>
  ${b.attrs.map((line, i) => `<text class="member" x="${b.x + 24}" y="${attrY + i * 20}">${esc(line)}</text>`).join("\n  ")}
  ${b.methods.map((line, i) => `<text class="member" x="${b.x + 24}" y="${methodY + i * 20}">${esc(line)}</text>`).join("\n  ")}
</g>`;
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
    if (dx > 0) return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x - 16} ${end.y - 8} L${end.x - 16} ${end.y + 8} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
    return `<path class="solidHead" d="M${end.x} ${end.y} L${end.x + 16} ${end.y - 8} L${end.x + 16} ${end.y + 8} Z" fill="#FFFFFF" stroke="${edge.color}"/>`;
  }
  if (dx > 0) return `<path class="solidOpenHead" d="M${end.x - 13} ${end.y - 7} L${end.x} ${end.y} L${end.x - 13} ${end.y + 7}" stroke="${edge.color}"/>`;
  if (dx < 0) return `<path class="solidOpenHead" d="M${end.x + 13} ${end.y - 7} L${end.x} ${end.y} L${end.x + 13} ${end.y + 7}" stroke="${edge.color}"/>`;
  if (dy < 0) return `<path class="solidOpenHead" d="M${end.x - 7} ${end.y + 13} L${end.x} ${end.y} L${end.x + 7} ${end.y + 13}" stroke="${edge.color}"/>`;
  return `<path class="solidOpenHead" d="M${end.x - 7} ${end.y - 13} L${end.x} ${end.y} L${end.x + 7} ${end.y - 13}" stroke="${edge.color}"/>`;
}

function label({ x, y, text, w }) {
  return `<g class="edgeLabel" transform="translate(${x - w / 2} ${y - 14})"><rect width="${w}" height="28" rx="8"/><text x="${w / 2}" y="19" text-anchor="middle">${esc(text)}</text></g>`;
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

function properCross(a, b) {
  if (a.a.y === a.b.y && b.a.x === b.b.x) {
    return b.a.x > Math.min(a.a.x, a.b.x) && b.a.x < Math.max(a.a.x, a.b.x) &&
      a.a.y > Math.min(b.a.y, b.b.y) && a.a.y < Math.max(b.a.y, b.b.y);
  }
  if (a.a.x === a.b.x && b.a.y === b.b.y) return properCross(b, a);
  return false;
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
        if (id === e.from || id === e.to) continue;
        if (hits(s, b)) throw new Error(`${e.id} crosses ${id}`);
      }
    }
  }
  for (let i = 0; i < edges.length; i += 1) {
    for (let j = i + 1; j < edges.length; j += 1) {
      for (const a of segs(edges[i].d)) {
        for (const b of segs(edges[j].d)) {
          if (properCross(a, b)) throw new Error(`${edges[i].id} crosses ${edges[j].id}`);
        }
      }
    }
  }
}

validate();

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="rule engine core class diagram">
<defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${colors.canvas}}.frame{fill:${colors.frame};stroke:${colors.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${colors.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${colors.muted}}.section{fill:#F3F8FF;stroke:#94A3B8;stroke-width:1.7;stroke-dasharray:12 8}.sectionTitle{font-family:"Comic Mono";font-size:13px;fill:${colors.muted}}.umlBox{filter:url(#softShadow);stroke-width:2}.stereo{font-family:"Comic Mono";font-size:12.2px;fill:${colors.muted}}.classTitle{font-family:"Architects Daughter";font-size:23px;fill:${colors.ink}}.member{font-family:"Comic Mono";font-size:12.8px;fill:${colors.muted}}.edge{fill:none;stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round}.implements,.uses{stroke-dasharray:8 7}.extends,.has{stroke-dasharray:none}.solidHead{stroke-width:1.9;stroke-linejoin:round;stroke-dasharray:none}.solidOpenHead{fill:none;stroke-width:2.25;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:none}.edgeLabel rect{fill:#FFFFFF;stroke:${colors.line};stroke-width:1.2;opacity:.96}.edgeLabel text{font-family:"Comic Mono";font-size:11.8px;fill:${colors.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/>
<text class="title" x="74" y="86">Rule Engine Core Class Diagram</text>
<text class="subtitle" x="78" y="118">Rule is the executable contract; DefaultRule binds one Condition and many Actions, while RuleSet, Facts, DSL, and proxy adapters feed the engine.</text>
<rect class="section" x="74" y="145" width="2320" height="1038" rx="8"/><text class="sectionTitle" x="102" y="170">core rule model, creation helpers, and facts passed through evaluate/execute</text>
<g id="edges">${edges.map((e) => `<path class="edge ${e.type}" d="${e.d}" stroke="${e.color}"/>`).join("\n")}</g>
<g id="arrowheads">${edges.map(arrowHead).join("\n")}</g>
<g id="labels">${edges.filter((e) => e.label).map((e) => label(e.label)).join("\n")}</g>
${Object.keys(boxes).map(box).join("\n")}
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
