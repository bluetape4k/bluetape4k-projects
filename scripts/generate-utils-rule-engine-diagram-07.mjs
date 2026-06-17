#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";

const svgPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-07.svg";
const pngPath = "docs/images/readme-diagrams/utils-rule-engine-diagram-07.png";
const W = 2360;
const H = 980;

const files = {
  readme: "utils/rule-engine/README.md",
  janino: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/engines/janino/JaninoCondition.kt",
  spel: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/engines/spel/SpelCondition.kt",
  mvel: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/engines/mvel2/MvelRule.kt",
  groovy: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/engines/groovy/GroovyAction.kt",
  nullSafe: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/engines/groovy/NullSafeBinding.kt",
  kotlin: "utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/engines/kotlinscript/KotlinScriptEngine.kt",
};

for (const f of Object.values(files)) {
  if (!existsSync(f)) throw new Error(`Missing source evidence: ${f}`);
}

function need(file, pattern, label) {
  if (!pattern.test(readFileSync(file, "utf8"))) throw new Error(`Expected ${label} in ${file}`);
}

need(files.readme, /Script Engine Selection Guide[\s\S]*utils-rule-engine-diagram-07\.png/, "README diagram slot");
need(files.readme, /Script Engine Comparison[\s\S]*Janino[\s\S]*Bytecode[\s\S]*Kotlin Script[\s\S]*slow cold start/, "README comparison");
need(files.janino, /ExpressionEvaluator[\s\S]*cook\(expression\)[\s\S]*evaluator\.evaluate/, "Janino bytecode evaluator");
need(files.spel, /StandardEvaluationContext[\s\S]*setVariables[\s\S]*setBeanResolver/, "SpEL bean resolver");
need(files.mvel, /class MvelRule[\s\S]*MvelCondition[\s\S]*MvelAction/, "MVEL rule DSL");
need(files.groovy, /GroovyShell[\s\S]*NullSafeBinding[\s\S]*GString/, "Groovy runtime features");
need(files.nullSafe, /MissingPropertyException[\s\S]*return if \(hasVariable\(name\)\)/, "Groovy null-safe binding");
need(files.kotlin, /BasicJvmScriptingHost[\s\S]*providedProperties[\s\S]*host\.eval/, "Kotlin script host");

const C = {
  ink: "#0F172A",
  muted: "#475569",
  canvas: "#F8FAFC",
  frame: "#FFFFFF",
  line: "#CBD5E1",
  blue: "#2563EB",
  teal: "#0D9488",
  green: "#16A34A",
  orange: "#EA580C",
  purple: "#7C3AED",
  pink: "#DB2777",
  gray: "#64748B",
};

const rows = [
  ["Price / threshold checks", "Janino", "Java subset", "Bytecode evaluator; fastest repeated checks.", "#EFF6FF", C.blue],
  ["Spring bean references", "SpEL", "Spring EL", "EvaluationContext + BeanResolver.", "#F0FDFA", C.teal],
  ["Discount / tier formulas", "MVEL2 or Groovy", "Dynamic expr", "Short business-authored expressions.", "#F0FDF4", C.green],
  ["Collection transforms", "Groovy", "Groovy script", "Closures, ranges, branching, script actions.", "#FFF7ED", C.orange],
  ["Optional / missing facts", "Groovy", "Null-safe facts", "NullSafeBinding enables Elvis/safe navigation.", "#FDF2F8", C.pink],
  ["Type-sensitive logic", "Kotlin Script", "Kotlin", "Full Kotlin syntax; slower cold start.", "#FAF5FF", C.purple],
];

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function card([scenario, engine, lang, detail, fill, stroke], index) {
  const col = index % 3;
  const row = Math.floor(index / 3);
  const x = 100 + col * 750;
  const y = 225 + row * 310;
  return `<g>
    <rect class="card" x="${x}" y="${y}" width="690" height="250" rx="8" fill="${fill}" stroke="${stroke}"/>
    <rect x="${x}" y="${y}" width="14" height="250" rx="7" fill="${stroke}"/>
    <text class="scenario" x="${x + 42}" y="${y + 50}">${esc(scenario)}</text>
    <text class="engine" x="${x + 42}" y="${y + 101}">${esc(engine)}</text>
    <text class="tag" x="${x + 42}" y="${y + 142}">${esc(lang)}</text>
    <text class="detail" x="${x + 42}" y="${y + 190}">${esc(detail)}</text>
  </g>`;
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="script engine selection guide">
  <defs>
    <filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%">
      <feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/>
    </filter>
    <style>
      svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
      .canvas{fill:${C.canvas}}
      .frame{fill:${C.frame};stroke:${C.line};stroke-width:1.5;filter:url(#softShadow)}
      .title{font-family:"Architects Daughter";font-size:46px;fill:${C.ink}}
      .subtitle,.detail,.tag{font-family:"Comic Mono";fill:${C.muted}}
      .subtitle{font-size:15.4px}
      .scenario{font-family:"Comic Mono";font-size:17px;fill:${C.muted}}
      .engine{font-family:"Architects Daughter";font-size:30px;fill:${C.ink}}
      .tag{font-size:15px}
      .detail{font-size:15.6px}
      .card{stroke-width:2;filter:url(#softShadow)}
      .note{font-family:"Comic Mono";font-size:13.6px;fill:${C.muted}}
    </style>
  </defs>
  <rect class="canvas" width="${W}" height="${H}"/>
  <rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 64}" rx="8"/>
  <text class="title" x="76" y="92">Script Engine Selection Guide</text>
  <text class="subtitle" x="80" y="126">Pick the scripting engine by authoring model first, then by runtime cost. All choices still produce Rule instances for the same engine pipeline.</text>
  ${rows.map(card).join("")}
  <text class="note" x="110" y="890">Janino favors throughput, SpEL favors Spring integration, Groovy favors expressive rules and null-safe facts, Kotlin Script favors type-aware scripts despite slower cold start.</text>
</svg>`;

writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`Generated ${svgPath}`);
console.log(`Generated ${pngPath}`);
