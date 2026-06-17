#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
const svgPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-04.svg";
const pngPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-04.png";
const W = 2380, H = 1240;
const files = [
  "utils/rule-engine/README.md",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/support/CompositeRule.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/support/ActivationRuleGroup.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/support/ConditionalRuleGroup.kt",
  "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/support/UnitRuleGroup.kt",
];
for (const f of files) if (!existsSync(f)) throw new Error(`Missing source evidence: ${f}`);
function need(f, p, label) { if (!p.test(readFileSync(f, "utf8"))) throw new Error(`Expected ${label} in ${f}`); }
need(files[0], /Composite Rules[\s\S]*utils-rule-engine-diagram-04\.png/, "README diagram slot");
need(files[1], /abstract class CompositeRule[\s\S]*protected open val rules[\s\S]*fun addRule\(rule: Any\)/, "CompositeRule");
need(files[2], /class ActivationRuleGroup[\s\S]*selectedRule[\s\S]*firstOrNull \{ it\.evaluate\(facts\) \}/, "ActivationRuleGroup");
need(files[3], /class ConditionalRuleGroup[\s\S]*conditionalRule[\s\S]*successfulEvaluations[\s\S]*Only one rule can have highest priority/, "ConditionalRuleGroup");
need(files[4], /open class UnitRuleGroup[\s\S]*rules\.all \{ it\.evaluate\(facts\) \}[\s\S]*rules\.forEach \{ it\.execute\(facts\) \}/, "UnitRuleGroup");
const C = { ink:"#0F172A", muted:"#475569", canvas:"#F8FAFC", frame:"#FFFFFF", line:"#CBD5E1", blue:"#2563EB", green:"#16A34A", teal:"#0D9488", orange:"#EA580C", purple:"#7C3AED", gray:"#64748B" };
const tones = { base:["#EFF6FF",C.blue,"#1D4ED8"], act:["#F0FDFA",C.teal,"#0F766E"], cond:["#FFF7ED",C.orange,"#C2410C"], unit:["#F0FDF4",C.green,"#15803D"], out:["#F8FAFC",C.gray,"#475569"] };
const cards = {
  composite:{x:120,y:210,w:610,h:270,tone:"base",title:"CompositeRule",kicker:"abstract Rule",lines:["extends AbstractRule", "stores rules in priority-sorted TreeSet", "addRule() accepts Rule or annotated object", "RuleProxy.asRule() adapts objects"],foot:"a group is itself a Rule"},
  input:{x:120,y:610,w:610,h:230,tone:"out",title:"member rules",kicker:"Rule children",lines:["rules are sorted by priority", "each child still evaluates against Facts", "each child action mutates the same Facts"],foot:"composition changes selection policy"},
  activation:{x:900,y:210,w:600,h:255,tone:"act",title:"ActivationRuleGroup",kicker:"first matching rule wins",lines:["evaluate: first rule whose condition is true", "execute: only selectedRule", "later matching rules are ignored"],foot:"use for exclusive alternatives"},
  conditional:{x:900,y:550,w:600,h:285,tone:"cond",title:"ConditionalRuleGroup",kicker:"highest-priority rule gates the group",lines:["evaluate gate rule first", "if gate passes, collect matching remaining rules", "execute gate, then successfulEvaluations", "throws if two rules share highest priority"],foot:"use for prerequisite + dependent rules"},
  unit:{x:900,y:920,w:600,h:230,tone:"unit",title:"UnitRuleGroup",kicker:"all-or-nothing group",lines:["evaluate: all child rules must pass", "execute: every child rule action", "empty group evaluates false"],foot:"use for required rule bundles"},
  result:{x:1685,y:520,w:560,h:300,tone:"out",title:"what the engine sees",kicker:"single Rule contract",lines:["RuleEngine only calls evaluate(facts)", "then execute(facts) if the group matches", "selection logic is hidden inside the group", "Facts instance is shared through children"],foot:"composite policies stay local"},
};
const edges = [
  ["composite","input","base",[[425,480],[425,610]],"owns rules",[445,555]],
  ["composite","activation","act",[[730,305],[900,305]],"variant",[770,278]],
  ["composite","conditional","cond",[[730,345],[800,345],[800,692],[900,692]],"variant",[812,500]],
  ["composite","unit","unit",[[730,385],[770,385],[770,1035],[900,1035]],"variant",[782,795]],
  ["activation","result","act",[[1500,338],[1590,338],[1590,600],[1685,600]],"first true",[1605,555]],
  ["conditional","result","cond",[[1500,692],[1685,692]],"gate + matches",[1530,665]],
  ["unit","result","unit",[[1500,1035],[1590,1035],[1590,740],[1685,740]],"all pass",[1605,907]],
];
function esc(v){return String(v).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;");}
function card(id){const d=cards[id],t=tones[d.tone];return `<g id="${id}"><rect class="card" x="${d.x}" y="${d.y}" width="${d.w}" height="${d.h}" rx="8" fill="${t[0]}" stroke="${t[1]}"/><text class="kicker" x="${d.x+24}" y="${d.y+34}">${esc(d.kicker)}</text><text class="cardTitle" x="${d.x+24}" y="${d.y+70}">${esc(d.title)}</text><path class="divider" d="M${d.x} ${d.y+94}H${d.x+d.w}" stroke="${t[2]}"/>${d.lines.map((l,i)=>`<text class="body" x="${d.x+24}" y="${d.y+130+i*25}">${esc(l)}</text>`).join("")}${d.foot?`<path class="divider" d="M${d.x} ${d.y+d.h-46}H${d.x+d.w}" stroke="${t[2]}"/><text class="foot" x="${d.x+24}" y="${d.y+d.h-17}">${esc(d.foot)}</text>`:""}</g>`}
function marker(){return Object.entries(tones).map(([k,t])=>`<marker id="arrow-${k}" markerWidth="24" markerHeight="18" refX="22" refY="9" orient="auto" markerUnits="userSpaceOnUse"><path d="M2 2 L22 9 L2 16 Z" fill="${t[2]}" stroke="${t[2]}"/></marker>`).join("")}
function path(points){return points.map((p,i)=>`${i?"L":"M"}${p[0]} ${p[1]}`).join(" ")}
function edge(e){const t=tones[e[2]],w=Math.max(96,e[4].length*8+26);return `<path class="edge" d="${path(e[3])}" stroke="${t[2]}" marker-end="url(#arrow-${e[2]})"/><rect class="edgeLabelBg" x="${e[5][0]-8}" y="${e[5][1]-18}" width="${w}" height="26" rx="6"/><text class="edgeLabel" x="${e[5][0]}" y="${e[5][1]}">${esc(e[4])}</text>`}
const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="composite rule groups"><defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/></filter>${marker()}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${C.canvas}}.frame{fill:${C.frame};stroke:${C.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:46px;fill:${C.ink}}.subtitle{font-family:"Comic Mono";font-size:15.5px;fill:${C.muted}}.card{stroke-width:1.9;filter:url(#softShadow)}.kicker{font-family:"Comic Mono";font-size:13px;fill:${C.muted}}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${C.ink}}.body{font-family:"Comic Mono";font-size:13.4px;fill:#334155}.foot{font-family:"Comic Mono";font-size:12.6px;fill:${C.muted}}.divider{stroke-width:1.15;opacity:.45}.edge{fill:none;stroke-width:3.4;stroke-linecap:round;stroke-linejoin:round}.edgeLabelBg{fill:#fff;stroke:#E2E8F0;stroke-width:1;opacity:.95}.edgeLabel{font-family:"Comic Mono";font-size:12.7px;fill:#334155}</style></defs><rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W-68}" height="${H-64}" rx="8"/><text class="title" x="74" y="88">Composite Rules</text><text class="subtitle" x="78" y="121">CompositeRule lets several child rules behave as one Rule; each concrete group changes only the child selection and execution policy.</text><g id="edges">${edges.map(edge).join("")}</g><g id="cards">${Object.keys(cards).map(card).join("")}</g></svg>`;
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm,""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath,"-o",pngPath,"-s","2"], {stdio:"inherit"});
console.log(`Generated ${svgPath}`); console.log(`Generated ${pngPath}`);
