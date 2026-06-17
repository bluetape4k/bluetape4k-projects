#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
const svgPath="docs/images/readme-diagrams/utils-rule-engine-diagram-06.svg",pngPath="docs/images/readme-diagrams/utils-rule-engine-diagram-06.png";
const W=2300,H=880;
const files=["utils/rule-engine/README.md","utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/DefaultRuleEngine.kt","utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/DefaultSuspendRuleEngine.kt","utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/core/InferenceRuleEngine.kt","utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/support/ActivationRuleGroup.kt","utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/support/ConditionalRuleGroup.kt","utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/support/UnitRuleGroup.kt"];
for(const f of files) if(!existsSync(f)) throw new Error(`Missing source evidence: ${f}`);
function need(f,p,l){if(!p.test(readFileSync(f,"utf8"))) throw new Error(`Expected ${l} in ${f}`)}
need(files[0],/Rule Engine Selection Guide[\s\S]*utils-rule-engine-diagram-06\.png/,"README slot");
need(files[1],/open class DefaultRuleEngine[\s\S]*skipOnFirstAppliedRule/,"DefaultRuleEngine");
need(files[2],/DefaultSuspendRuleEngine[\s\S]*CancellationException/,"DefaultSuspendRuleEngine");
need(files[3],/class InferenceRuleEngine[\s\S]*selectCandidates[\s\S]*delegate\.doFire/,"InferenceRuleEngine");
const C={ink:"#0F172A",muted:"#475569",canvas:"#F8FAFC",frame:"#FFFFFF",line:"#CBD5E1",blue:"#2563EB",green:"#16A34A",teal:"#0D9488",orange:"#EA580C",purple:"#7C3AED",gray:"#64748B"};
const rows=[
  ["Need ordinary sync rules","DefaultRuleEngine",["RuleSet + Facts, listeners, skip policies","Stops by priority threshold or configured skip flags"],"#EFF6FF",C.blue],
  ["Need suspend rules","DefaultSuspendRuleEngine",["Coroutine API, cancellation-safe checks","Rethrows CancellationException instead of swallowing it"],"#F0FDFA",C.teal],
  ["Need fact chaining","InferenceRuleEngine",["Select matching rules, execute, then re-evaluate","Use when actions can unlock later rules"],"#FAF5FF",C.purple],
  ["Exclusive child rules","ActivationRuleGroup",["First evaluated child wins","Use for mutually exclusive alternatives"],"#F0FDF4",C.green],
  ["Prerequisite gate","ConditionalRuleGroup",["Highest-priority child gates dependent matches","Use when one rule must pass before the rest"],"#FFF7ED",C.orange],
  ["All children must pass","UnitRuleGroup",["All evaluate() calls pass before any action","Use for required rule bundles"],"#F8FAFC",C.gray],
];
function esc(v){return String(v).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;")}
function row(r,i){const col=i%3,row=Math.floor(i/3),x=105+col*730,y=210+row*315;return `<g><rect class="row" x="${x}" y="${y}" width="650" height="245" rx="8" fill="${r[3]}" stroke="${r[4]}"/><text class="scenario" x="${x+28}" y="${y+48}">${esc(r[0])}</text><text class="choice" x="${x+28}" y="${y+100}">${esc(r[1])}</text><path class="divider" d="M${x} ${y+124}H${x+650}" stroke="${r[4]}"/>${r[2].map((line,j)=>`<text class="detail" x="${x+28}" y="${y+164+j*28}">${esc(line)}</text>`).join("")}</g>`}
const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="rule engine selection guide"><defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/></filter><style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${C.canvas}}.frame{fill:${C.frame};stroke:${C.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:46px;fill:${C.ink}}.subtitle,.detail{font-family:"Comic Mono";fill:${C.muted}}.subtitle{font-size:15.5px}.scenario{font-family:"Comic Mono";font-size:15px;fill:${C.muted}}.choice{font-family:"Architects Daughter";font-size:27px;fill:${C.ink}}.detail{font-size:14px}.row{stroke-width:1.9;filter:url(#softShadow)}.divider{stroke-width:1.1;opacity:.42}</style></defs><rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W-68}" height="${H-64}" rx="8"/><text class="title" x="74" y="88">Rule Engine Selection Guide</text><text class="subtitle" x="78" y="121">Choose the smallest rule-engine shape that matches execution semantics; composite groups are still single Rule instances to the engine.</text>${rows.map(row).join("")}</svg>`;
writeFileSync(svgPath,svg.replace(/[ \t]+$/gm,""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`,[svgPath,"-o",pngPath,"-s","2"],{stdio:"inherit"});
console.log(`Generated ${svgPath}`);console.log(`Generated ${pngPath}`);
