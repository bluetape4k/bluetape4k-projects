#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const OUT = join(process.cwd(), "docs/images/readme-diagrams");
const DOT = "/opt/homebrew/bin/dot";
const RSVG = "/opt/homebrew/bin/rsvg-convert";

const colors = {
  blue: ["#E8F3FF", "#5B8DEF", "#4F83BF"],
  green: ["#EAF7EF", "#58A978", "#3E9868"],
  teal: ["#E9F7F6", "#45A7A1", "#2E8F89"],
  amber: ["#FFF3D9", "#D6A441", "#B9851B"],
  pink: ["#FDECEF", "#DC6B82", "#C94D68"],
  purple: ["#F1ECFF", "#8A72D6", "#755BC6"],
  olive: ["#EEF6D9", "#8BA84D", "#718A35"],
  gray: ["#F2F5F9", "#9AA8B8", "#758297"],
};

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function dotEsc(value) {
  return String(value ?? "").replaceAll("\\", "\\\\").replaceAll('"', '\\"');
}

function wrap(text, max = 28) {
  const words = String(text).split(/\s+/);
  const lines = [];
  let line = "";
  for (const word of words) {
    if ((line + " " + word).trim().length > max && line) {
      lines.push(line);
      line = word;
    } else {
      line = (line + " " + word).trim();
    }
  }
  if (line) lines.push(line);
  return lines;
}

function base(width, height, title, subtitle, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title)}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/></filter>
  <marker id="arrow" markerWidth="6" markerHeight="6" refX="5.4" refY="3" orient="auto" markerUnits="strokeWidth"><path d="M 0.7 0.7 L 5.4 3 L 0.7 5.3 Z" fill="context-stroke"/></marker>
  <marker id="inherit" markerWidth="11" markerHeight="10" refX="10" refY="5" orient="auto" markerUnits="strokeWidth"><path d="M 1 1 L 10 5 L 1 9 Z" fill="#FFFFFF" stroke="context-stroke" stroke-width="1.4"/></marker>
  <style>
    .canvas{fill:#F6F9FC}.frame{fill:#fff;stroke:#C7D7E7;stroke-width:3.2;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:#22344A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#536476}
    .panel{fill:#F7FBFF;stroke:#D6E3EF;stroke-width:2}.panelTitle{font-family:"Architects Daughter";font-size:24px;fill:#31445A}
    .card{filter:url(#shadow);stroke-width:2}.cardTitle{font-family:"Architects Daughter";font-size:23px;fill:#22344A}.detail{font-family:"Comic Mono";font-size:13px;fill:#42556B}
    .classTitle{font-family:"Architects Daughter";font-size:22px;fill:#22344A}.stereo{font-family:"Comic Mono";font-size:10px;fill:#627184}.member{font-family:"Comic Mono";font-size:11px;fill:#102033}
    .flow{fill:none;stroke-width:2.8;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
    .dashed{stroke-dasharray:9 8}.inherit{fill:none;stroke-width:2.3;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#inherit)}
    .small{font-family:"Comic Mono";font-size:12px;fill:#627184}.bitLabel{font-family:"Architects Daughter";font-size:22px;fill:#22344A}.bitMeta{font-family:"Comic Mono";font-size:13px;fill:#42556B}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="30"/>
<text class="title" x="72" y="86">${esc(title)}</text>
<text class="subtitle" x="76" y="118">${esc(subtitle)}</text>
${body}
</svg>
`;
}

function panel(x, y, w, h, title) {
  return `<g><rect class="panel" x="${x}" y="${y}" width="${w}" height="${h}" rx="22"/><text class="panelTitle" x="${x + 24}" y="${y + 34}">${esc(title)}</text></g>`;
}

function card(id, x, y, w, h, title, details = [], color = "blue") {
  const [fill, stroke] = colors[color] || colors.gray;
  const lines = Array.isArray(details) ? details : wrap(details, 30);
  const titleLines = wrap(title, w > 300 ? 25 : 20).slice(0, 2);
  const titleH = titleLines.length * 23;
  const detailH = lines.length * 18;
  const gap = lines.length ? 12 : 0;
  const total = titleH + gap + detailH;
  const firstTitle = Math.round(y + h / 2 - total / 2 + 18);
  const firstDetail = firstTitle + titleH + gap;
  const titleSvg = titleLines.map((line, index) => `<text class="cardTitle" x="${x + w / 2}" y="${firstTitle + index * 23}" text-anchor="middle">${esc(line)}</text>`).join("");
  const detailSvg = lines.map((line, index) => `<text class="detail" x="${x + w / 2}" y="${firstDetail + index * 18}" text-anchor="middle">${esc(line)}</text>`).join("");
  return `<g id="${id}"><rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="14" fill="${fill}" stroke="${stroke}"/>${titleSvg}${detailSvg}</g>`;
}

function state(id, x, y, w, h, title, details = [], color = "blue") {
  return card(id, x, y, w, h, title, details, color);
}

function classBox(id, x, y, w, h, title, stereo, members, color = "blue") {
  const [fill, stroke] = colors[color] || colors.gray;
  const titleLines = wrap(title, w > 290 ? 24 : 19).slice(0, 2);
  const headerBottom = y + (titleLines.length > 1 ? 82 : 68);
  const firstTitle = y + (titleLines.length > 1 ? 42 : 48);
  const lineH = 17;
  const firstMember = Math.round((headerBottom + y + h) / 2 + 4 - ((members.length - 1) * lineH) / 2);
  const titleSvg = titleLines.map((line, index) => `<text class="classTitle" x="${x + w / 2}" y="${firstTitle + index * 22}" text-anchor="middle">${esc(line)}</text>`).join("");
  const memberSvg = members.map((line, index) => `<text class="member" x="${x + 18}" y="${firstMember + index * lineH}">${esc(line)}</text>`).join("");
  return `<g id="${id}"><rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="7" fill="${fill}" stroke="${stroke}"/><line x1="${x}" y1="${headerBottom}" x2="${x + w}" y2="${headerBottom}" stroke="${stroke}" stroke-width="1.5"/><text class="stereo" x="${x + w / 2}" y="${y + 20}" text-anchor="middle">${esc(stereo)}</text>${titleSvg}${memberSvg}</g>`;
}

function path(d, color = "gray", klass = "flow") {
  return `<path class="${klass}" d="${d}" stroke="${colors[color]?.[2] || colors.gray[2]}"/>`;
}

function label(x, y, text, color = "gray", w = 150) {
  const [, stroke] = colors[color] || colors.gray;
  return `<g><rect x="${x - w / 2}" y="${y - 15}" width="${w}" height="30" rx="8" fill="#FFFFFF" stroke="${stroke}" stroke-width="1.2"/><text class="detail" x="${x}" y="${y + 1}" text-anchor="middle" dominant-baseline="middle">${esc(text)}</text></g>`;
}

function footer(x, y, w, text) {
  return `<g><rect x="${x}" y="${y}" width="${w}" height="42" rx="12" fill="#FFFFFF" stroke="#D6E3EF" stroke-width="1.5"/><text class="small" x="${x + w / 2}" y="${y + 25}" text-anchor="middle">${esc(text)}</text></g>`;
}

function bitSegment(x, y, w, h, title, bits, range, color) {
  const [fill, stroke] = colors[color] || colors.gray;
  return `<g><rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}"/>
    <text class="bitLabel" x="${x + w / 2}" y="${y + 48}" text-anchor="middle">${esc(title)}</text>
    <text class="bitMeta" x="${x + w / 2}" y="${y + 78}" text-anchor="middle">${esc(bits)}</text>
    <text class="bitMeta" x="${x + w / 2}" y="${y + h - 24}" text-anchor="middle">${esc(range)}</text>
  </g>`;
}

function dotGraph(title, nodes, edges = [], rankdir = "LR") {
  const lines = [
    "digraph G {",
    `  graph [rankdir=${rankdir}, splines=ortho, nodesep=0.7, ranksep=0.85, fontname="Comic Mono", label="${dotEsc(title)}"];`,
    '  node [shape=box, style="rounded,filled", fontname="Architects Daughter", fontsize=18, color="#D7E2EC", fillcolor="#F7FAFC"];',
    '  edge [fontname="Comic Mono", fontsize=11, color="#56708C", arrowsize=0.8];',
  ];
  nodes.forEach((node) => lines.push(`  "${dotEsc(node)}";`));
  edges.forEach(([from, to, label = ""]) => lines.push(`  "${dotEsc(from)}" -> "${dotEsc(to)}" [label="${dotEsc(label)}"];`));
  lines.push("}");
  return `${lines.join("\n")}\n`;
}

function write(name, svg, dotSource) {
  mkdirSync(OUT, { recursive: true });
  const basePath = join(OUT, name);
  writeFileSync(`${basePath}.svg`, svg);
  execFileSync(RSVG, ["--format=png", "--output", `${basePath}.png`, `${basePath}.svg`], { stdio: "inherit" });
  if (dotSource) {
    writeFileSync(`${basePath}.dot`, dotSource);
    execFileSync(DOT, ["-Tplain", `${basePath}.dot`, "-o", `${basePath}.plain`], { stdio: "inherit" });
    execFileSync(DOT, ["-Tsvg", `${basePath}.dot`, "-o", `${basePath}-graphviz.svg`], { stdio: "inherit" });
    execFileSync(DOT, ["-Tpng", `${basePath}.dot`, "-o", `${basePath}-graphviz.png`], { stdio: "inherit" });
  }
  console.log(`${name}.png`);
}

function measuredUnitComposition() {
  const body = [
    panel(80, 158, 1440, 176, "Unit definitions"),
    panel(80, 382, 1440, 188, "Measure operators"),
    panel(80, 620, 1440, 172, "Compound results"),
    card("amount", 130, 218, 260, 82, "Number", ["10, 5, 1.5"], "blue"),
    card("units", 480, 208, 300, 102, "Units", ["suffix + ratio", "Length.meters, Time.seconds"], "green"),
    card("measure", 895, 208, 300, 102, "Measure<T>", ["amount + units", "conversion via ratio"], "purple"),
    card("format", 1280, 218, 190, 82, "toHuman()", ["readable output"], "teal"),
    card("unitProduct", 180, 432, 310, 92, "UnitsProduct", ["A * B", "ratio = A.ratio * B.ratio"], "amber"),
    card("unitRatio", 625, 432, 310, 92, "UnitsRatio", ["A / B", "ratio = A.ratio / B.ratio"], "pink"),
    card("inverse", 1070, 432, 300, 92, "InverseUnits", ["1 / unit", "reciprocal ratio"], "olive"),
    card("area", 170, 682, 260, 78, "Area", ["Length * Length"], "amber"),
    card("velocity", 505, 682, 290, 78, "Velocity", ["Length / Time"], "pink"),
    card("accel", 870, 682, 300, 78, "Acceleration", ["Length / Time²"], "purple"),
    card("energy", 1245, 682, 240, 78, "Energy / Power", ["domain-specific ops"], "green"),
    path("M390 259 L480 259", "green"),
    path("M780 259 L895 259", "purple"),
    path("M1195 259 L1280 259", "teal"),
    path("M330 310 L330 432", "amber"),
    path("M630 310 L630 432", "pink"),
    path("M1045 310 L1045 356 L1220 356 L1220 432", "olive"),
    path("M335 524 L335 682", "amber"),
    path("M780 524 L780 600 L650 600 L650 682", "pink"),
    path("M1170 524 L1170 602 L1020 602 L1020 682", "purple"),
    path("M1270 524 L1270 586 L1365 586 L1365 682", "green"),
    label(435, 392, "operator fun times", "amber", 180),
    label(790, 392, "operator fun div", "pink", 170),
    footer(140, 830, 1320, "Source truth: Units.kt composes suffix/ratio, and Measure operators return typed compound Measure values."),
  ].join("\n");
  write("utils-measured-diagram-02", base(1600, 920, "Unit Composition Flow", "Composable Units and Measure operators produce typed compound units without grid layout.", body), dotGraph("Unit Composition Flow", ["Number", "Units", "Measure", "UnitsProduct", "UnitsRatio", "InverseUnits", "Area", "Velocity", "Acceleration"], [["Number", "Units", "*"], ["Units", "Measure", "wrap"], ["Units", "UnitsProduct", "*"], ["Units", "UnitsRatio", "/"], ["Units", "InverseUnits", "reciprocal"], ["UnitsProduct", "Area", "Length*Length"], ["UnitsRatio", "Velocity", "Length/Time"], ["UnitsRatio", "Acceleration", "Length/Time^2"]]));
}

function snowflakeBitLayout() {
  const x = 130;
  const y = 270;
  const h = 150;
  const widths = [70, 790, 210, 240];
  const body = [
    `<text class="detail" x="120" y="190">64-bit signed Long layout · high bit stays zero; timestamp is milliseconds since EPOCH=2015-01-01.</text>`,
    bitSegment(x, y, widths[0], h, "0", "sign bit", "bit 63", "gray"),
    bitSegment(x + widths[0], y, widths[1], h, "timestamp", "41 bits", "bits 62..22", "blue"),
    bitSegment(x + widths[0] + widths[1], y, widths[2], h, "machineId", "10 bits", "bits 21..12", "green"),
    bitSegment(x + widths[0] + widths[1] + widths[2], y, widths[3], h, "sequence", "12 bits", "bits 11..0", "amber"),
    `<g><line x1="${x}" y1="${y + h + 38}" x2="${x + widths.reduce((a, b) => a + b, 0)}" y2="${y + h + 38}" stroke="#8FA3B8" stroke-width="2"/>
      <text class="bitMeta" x="${x}" y="${y + h + 70}" text-anchor="middle">63</text>
      <text class="bitMeta" x="${x + widths[0]}" y="${y + h + 70}" text-anchor="middle">62</text>
      <text class="bitMeta" x="${x + widths[0] + widths[1]}" y="${y + h + 70}" text-anchor="middle">21</text>
      <text class="bitMeta" x="${x + widths[0] + widths[1] + widths[2]}" y="${y + h + 70}" text-anchor="middle">11</text>
      <text class="bitMeta" x="${x + widths.reduce((a, b) => a + b, 0)}" y="${y + h + 70}" text-anchor="middle">0</text>
    </g>`,
    card("parse", 185, 560, 310, 96, "parseSnowflakeId(id)", ["timestamp = id >>> 22 + EPOCH", "machineId = bits 21..12"], "purple"),
    card("make", 640, 560, 310, 96, "makeId(...)", ["timestamp shift 22", "machine shift 12"], "teal"),
    card("capacity", 1095, 560, 310, 96, "Capacity", ["1,024 machines", "4,096 IDs/ms/machine"], "pink"),
    footer(160, 730, 1280, "Source truth: SnowflakeSupport.kt uses TIME_STAMP_SHIFT=22, MACHINE_ID_SHIFT=12, MAX_MACHINE_ID=1024, MAX_SEQUENCE=4096."),
  ].join("\n");
  write("utils-idgenerators-diagram-03", base(1600, 820, "Snowflake Bit Layout", "The 64-bit ID is rendered as a horizontal bit array with explicit bit ranges.", body), dotGraph("Snowflake Bit Layout", ["sign", "timestamp", "machineId", "sequence", "makeId", "parseSnowflakeId"], [["timestamp", "makeId", "shl 22"], ["machineId", "makeId", "shl 12"], ["sequence", "makeId"], ["makeId", "parseSnowflakeId", "round-trip"]]));
}

function virtualThreadJdk21Uml() {
  const body = [
    panel(80, 158, 2060, 190, "API contracts from virtualthread-api"),
    panel(80, 395, 2060, 380, "JDK 21 implementation module"),
    classBox("vtr", 145, 215, 300, 112, "VirtualThreadRuntime", "interface", ["+runtimeName: String", "+threadFactory(prefix)", "+executorService()"], "blue"),
    classBox("provider", 500, 205, 430, 132, "StructuredTaskScopeProvider", "interface", ["+withAll(...)", "+withAny(...)", "+withSupervised(...)"], "green"),
    classBox("all", 990, 215, 330, 112, "StructuredTaskScopeAll", "interface", ["+fork(task)", "+join()", "+throwIfFailed()"], "teal"),
    classBox("any", 1380, 215, 330, 112, "StructuredTaskScopeAny", "interface", ["+fork(task)", "+join()", "+result()"], "amber"),
    classBox("sup", 1770, 215, 280, 112, "Supervised", "interface", ["+results", "+join()"], "purple"),
    classBox("jruntime", 150, 455, 300, 128, "Jdk21VirtualThreadRuntime", "final class", ["+runtimeName = jdk21", "+priority = 21", "+isSupported()"], "blue"),
    classBox("jprovider", 520, 445, 430, 148, "Jdk21StructuredTaskScopeProvider", "class", ["+withAll()", "+withAny()", "+withSupervised()"], "green"),
    classBox("jall", 1000, 455, 330, 128, "Jdk21AllScope", "private class", ["wraps ShutdownOnFailure", "implements fail-fast API"], "teal"),
    classBox("jany", 1390, 455, 330, 128, "Jdk21AnyScope", "private class", ["wraps ShutdownOnSuccess", "first successful result"], "amber"),
    classBox("jsup", 1780, 455, 280, 128, "Jdk21SupervisedScope", "private class", ["wraps custom scope", "separates results"], "purple"),
    classBox("subtask", 1040, 650, 250, 92, "Jdk21Subtask", "private class", ["wraps Subtask<T>"], "gray"),
    classBox("scope", 1580, 650, 360, 92, "Jdk21SupervisedTaskScope", "private class", ["extends StructuredTaskScope<T>"], "pink"),
    path("M300 455 L300 327", "blue", "inherit"),
    path("M735 445 L735 390 L715 390 L715 337", "green", "inherit"),
    path("M1165 455 L1165 390 L1155 390 L1155 327", "teal", "inherit"),
    path("M1555 455 L1555 390 L1545 390 L1545 327", "amber", "inherit"),
    path("M1920 455 L1920 390 L1910 390 L1910 327", "purple", "inherit"),
    path("M1165 650 L1165 583", "gray"),
    path("M1760 650 L1760 616 L1920 616 L1920 583", "pink"),
    footer(220, 818, 1760, "Source truth: JDK21 provider wraps Java 21 StructuredTaskScope variants and exposes the API contracts through ServiceLoader."),
  ].join("\n");
  write("virtualthread-jdk21-diagram-01", base(2220, 920, "JDK21 Virtual Thread UML", "Interfaces stay above Java 21 implementations; each UML arrow terminates on the real parent boundary.", body), dotGraph("JDK21 Virtual Thread UML", ["VirtualThreadRuntime", "StructuredTaskScopeProvider", "StructuredTaskScopeAll", "StructuredTaskScopeAny", "StructuredTaskScopeSupervised", "Jdk21VirtualThreadRuntime", "Jdk21StructuredTaskScopeProvider", "Jdk21AllScope", "Jdk21AnyScope", "Jdk21SupervisedScope"], [["Jdk21VirtualThreadRuntime", "VirtualThreadRuntime"], ["Jdk21StructuredTaskScopeProvider", "StructuredTaskScopeProvider"], ["Jdk21AllScope", "StructuredTaskScopeAll"], ["Jdk21AnyScope", "StructuredTaskScopeAny"], ["Jdk21SupervisedScope", "StructuredTaskScopeSupervised"]], "BT"));
}

function stateDiagrams() {
  const turnstile = [
    panel(80, 160, 1040, 360, "Two-state FSM"),
    state("locked", 170, 285, 300, 112, "Locked", ["initial state", "push is rejected"], "blue"),
    state("unlocked", 730, 285, 300, 112, "Unlocked", ["coin accepted", "next push rotates"], "green"),
    path("M470 320 L730 320", "green"),
    label(600, 285, "Coin", "green", 120),
    path("M730 375 L470 375", "amber"),
    label(600, 420, "Push", "amber", 120),
    footer(150, 585, 900, "No grid: the reciprocal transitions use two separated direct horizontal lanes."),
  ].join("\n");
  write("utils-states-diagram-04", base(1200, 700, "Turnstile FSM", "Coin unlocks the gate; push locks it again through short separated transition lanes.", turnstile), dotGraph("Turnstile FSM", ["Locked", "Unlocked"], [["Locked", "Unlocked", "Coin"], ["Unlocked", "Locked", "Push"]]));

  const order = [
    panel(80, 160, 1520, 290, "Happy path"),
    panel(80, 500, 1520, 150, "Terminal shortcut"),
    state("created", 130, 265, 220, 100, "Created", ["initial"], "blue"),
    state("paid", 500, 265, 220, 100, "Paid", ["payment accepted"], "green"),
    state("shipped", 870, 265, 220, 100, "Shipped", ["carrier handoff"], "amber"),
    state("delivered", 1240, 265, 220, 100, "Delivered", ["final success"], "teal"),
    state("cancelled", 500, 535, 290, 88, "Cancelled", ["final stop from Created"], "pink"),
    path("M350 315 L500 315", "green"),
    path("M720 315 L870 315", "amber"),
    path("M1090 315 L1240 315", "teal"),
    path("M240 365 L240 579 L500 579", "pink"),
    label(425, 282, "Pay", "green", 120),
    label(795, 282, "Ship", "amber", 120),
    label(1165, 282, "Deliver", "teal", 130),
    label(370, 548, "Cancel", "pink", 120),
    footer(180, 720, 1320, "One-way order flow stays left-to-right, with cancellation in a reserved terminal band instead of a grid."),
  ].join("\n");
  write("utils-states-diagram-05", base(1680, 820, "Order One-Way FSM", "Linear business progress is horizontal; the cancellation branch is isolated below the main path.", order), dotGraph("Order One-Way FSM", ["Created", "Paid", "Shipped", "Delivered", "Cancelled"], [["Created", "Paid", "Pay"], ["Paid", "Shipped", "Ship"], ["Shipped", "Delivered", "Deliver"], ["Created", "Cancelled", "Cancel"]]));
}

function cassandraLayered() {
  const body = [
    panel(80, 155, 1580, 145, "Application intent"),
    panel(80, 350, 1580, 210, "bluetape4k Cassandra extensions"),
    panel(80, 620, 1580, 190, "DataStax driver model"),
    panel(80, 870, 1580, 125, "Cassandra runtime"),
    card("repo", 170, 205, 310, 78, "Repository code", ["domain query intent"], "blue"),
    card("admin", 670, 205, 300, 78, "Admin tasks", ["keyspace + version checks"], "purple"),
    card("entity", 1220, 205, 310, 78, "Entity mapping", ["row to domain object"], "green"),
    card("session", 230, 430, 330, 96, "CqlSession DSL", ["cqlSession { }", "contact points"], "teal"),
    card("async", 610, 430, 330, 96, "Coroutine queries", ["executeSuspending", "prepareSuspending"], "blue"),
    card("qb", 965, 430, 310, 96, "QueryBuilder helpers", ["terms, relations", "statements"], "pink"),
    card("row", 1305, 430, 310, 96, "Row / Gettable", ["typed value access", "toMap helpers"], "amber"),
    card("cql", 250, 685, 330, 112, "CqlSession / AsyncCqlSession", ["driver sessions", "sync + async execution"], "teal"),
    card("stmt", 975, 710, 290, 72, "Statement model", ["Simple / Bound / Batch"], "amber"),
    card("result", 1315, 710, 290, 72, "Row / ResultSet", ["driver data shape"], "green"),
    card("cluster", 610, 902, 900, 82, "Cassandra cluster", ["keyspaces, tables, indexes"], "purple"),
    path("M325 283 L325 320 L500 320 L500 430", "teal"),
    path("M820 283 L820 340 L545 340 L545 430", "purple"),
    path("M1375 283 L1375 430", "amber"),
    path("M395 526 L395 685", "teal"),
    path("M775 526 L775 610 L465 610 L465 685", "blue"),
    path("M1120 526 L1120 710", "pink"),
    path("M1460 526 L1460 710", "green"),
    path("M415 797 L415 842 L750 842 L750 902", "teal"),
    path("M1120 782 L1120 902", "amber"),
    path("M1460 782 L1460 902", "green"),
    footer(190, 1030, 1360, "Layered, not snake-shaped: each extension family drops to the driver/runtime layer through the nearest clear lane."),
  ].join("\n");
  write("data-cassandra-diagram-01", base(1740, 1120, "Cassandra Data Access Layer", "Application code, bluetape4k extensions, driver types, and Cassandra runtime stay in clear responsibility bands.", body), dotGraph("Cassandra Data Access Layer", ["Repository", "CqlSession DSL", "Coroutine queries", "Row/Gettable", "QueryBuilder", "CqlSession", "Statements", "ResultSet", "Cassandra"], [["Repository", "CqlSession DSL"], ["Repository", "Coroutine queries"], ["Coroutine queries", "CqlSession"], ["QueryBuilder", "Statements"], ["Row/Gettable", "ResultSet"], ["CqlSession", "Cassandra"], ["Statements", "Cassandra"], ["ResultSet", "Cassandra"]], "TB"));
}

function lettuceNearCacheStack() {
  const body = [
    panel(80, 155, 1580, 165, "Public cache contracts"),
    panel(80, 370, 1580, 215, "JCache NearCache adapters"),
    panel(80, 640, 1580, 180, "Local and Redis tiers"),
    card("nearOps", 160, 210, 330, 82, "NearCacheOperations", ["blocking contract"], "blue"),
    card("suspendOps", 555, 210, 360, 82, "SuspendNearCacheOperations", ["coroutine contract"], "green"),
    card("jcache", 980, 210, 280, 82, "JCache", ["standard cache API"], "purple"),
    card("suspendJCache", 1325, 210, 260, 82, "SuspendJCache", ["suspend API"], "teal"),
    classBox("near", 210, 425, 330, 112, "NearJCache", "class", ["front: LettuceLocalCache", "back: LettuceJCache"], "blue"),
    classBox("snear", 640, 425, 360, 112, "SuspendNearJCache", "class", ["front: LettuceLocalCache", "back: LettuceSuspendJCache"], "green"),
    card("factory", 1100, 425, 330, 112, "LettuceCaches", ["nearJCache()", "suspendNearJCache()"], "amber"),
    card("front", 250, 690, 330, 82, "LettuceCaffeineLocalCache", ["L1 Caffeine", "listener invalidation"], "teal"),
    card("redis", 705, 690, 330, 82, "Lettuce JCache backend", ["L2 Redis", "TTL + codec"], "purple"),
    card("tracking", 1160, 690, 330, 82, "TrackingInvalidationListener", ["peer invalidation", "front-cache clear"], "pink"),
    path("M375 425 L375 292", "blue", "inherit"),
    path("M820 425 L820 352 L735 352 L735 292", "green", "inherit"),
    path("M875 425 L875 336 L1120 336 L1120 292", "purple"),
    path("M900 425 L900 346 L1455 346 L1455 292", "teal"),
    path("M375 537 L375 690", "teal"),
    path("M820 537 L820 690", "purple"),
    path("M1000 480 L1100 480", "amber"),
    path("M1035 731 L1160 731", "pink"),
    path("M580 731 L705 731", "purple"),
    footer(170, 862, 1400, "Stack form keeps contract, adapter, and storage tiers separate; adjacent cards use direct connectors instead of snake routing."),
  ].join("\n");
  write("cache-cache-lettuce-diagram-01", base(1740, 960, "JCache NearCache Structure", "Lettuce NearCache is shown as a contract-to-adapter-to-tier stack with short direct relationships.", body), dotGraph("JCache NearCache Structure", ["NearCacheOperations", "SuspendNearCacheOperations", "NearJCache", "SuspendNearJCache", "LettuceCaches", "LocalCache", "LettuceJCache", "TrackingInvalidationListener"], [["NearJCache", "NearCacheOperations"], ["SuspendNearJCache", "SuspendNearCacheOperations"], ["LettuceCaches", "NearJCache"], ["LettuceCaches", "SuspendNearJCache"], ["NearJCache", "LocalCache"], ["NearJCache", "LettuceJCache"], ["SuspendNearJCache", "LocalCache"], ["SuspendNearJCache", "LettuceJCache"], ["TrackingInvalidationListener", "LocalCache"]], "TB"));
}

function opentelemetryTracePropagation() {
  const body = [
    panel(80, 155, 420, 430, "Service A"),
    panel(570, 220, 500, 260, "Carrier boundary"),
    panel(1140, 155, 420, 500, "Service B"),
    panel(570, 710, 500, 155, "Telemetry backend"),
    card("spanA", 145, 230, 290, 88, "withSpan / useSpan", ["creates parent Span", "Context.current()"], "blue"),
    card("coroutine", 145, 382, 290, 88, "Coroutine Context", ["asContextElement()", "withSpanContext()"], "green"),
    card("inject", 650, 280, 340, 88, "ContextPropagators", ["inject trace context", "W3C traceparent"], "purple"),
    card("carrier", 670, 420, 300, 70, "HTTP / Kafka carrier", ["headers carry trace IDs"], "amber"),
    card("extract", 1205, 410, 290, 88, "Extracted Context", ["remote parent", "trace continuity"], "teal"),
    card("spanB", 1205, 550, 290, 88, "Child Span", ["server/consumer work", "same trace"], "green"),
    card("export", 645, 780, 350, 64, "SpanExporter / Collector", ["joined trace view"], "pink"),
    path("M290 318 L290 382", "green"),
    path("M435 426 L535 426 L535 324 L650 324", "purple"),
    path("M820 368 L820 420", "amber"),
    path("M970 455 L1205 455", "teal"),
    path("M1350 498 L1350 550", "green"),
    path("M1350 638 L1350 680 L995 680 L995 780", "pink"),
    path("M290 470 L290 680 L645 680 L645 780", "pink"),
    label(470, 382, "inject", "purple", 110),
    label(1080, 420, "extract", "teal", 110),
    footer(145, 910, 1350, "Free placement around the carrier keeps propagation routes short; no grid or forced snake path is used."),
  ].join("\n");
  write("infra-opentelemetry-diagram-03", base(1640, 1000, "Distributed Trace Propagation", "Trace context moves through explicit propagators and carriers before the downstream service creates a child span.", body), dotGraph("Distributed Trace Propagation", ["Service A Span", "Coroutine Context", "ContextPropagators", "HTTP/Kafka carrier", "Extracted Context", "Child Span", "Exporter"], [["Service A Span", "Coroutine Context"], ["Coroutine Context", "ContextPropagators", "inject"], ["ContextPropagators", "HTTP/Kafka carrier"], ["HTTP/Kafka carrier", "Extracted Context", "extract"], ["Extracted Context", "Child Span"], ["Child Span", "Exporter"]], "LR"));
}

function main() {
  measuredUnitComposition();
  snowflakeBitLayout();
  virtualThreadJdk21Uml();
  stateDiagrams();
  cassandraLayered();
  lettuceNearCacheStack();
  opentelemetryTracePropagation();
}

main();
