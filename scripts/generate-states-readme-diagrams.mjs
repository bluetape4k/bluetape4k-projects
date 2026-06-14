#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const outDir = join(ROOT, "docs/images/readme-diagrams");
const cairosvg = process.env.CAIROSVG ?? "cairosvg";

if (!existsSync(outDir)) mkdirSync(outDir, { recursive: true });

write("utils-states-diagram-01", conceptOverview());
write("utils-states-diagram-03", dslBuilder());
write("utils-states-diagram-04", turnstile());
write("utils-states-diagram-05", order());
write("utils-states-diagram-06", appointment());

console.log("states-readme-diagrams: diagrams=5 renderer=cairosvg sequence=preserved");

function write(name, svg) {
  const base = join(outDir, name);
  writeFileSync(`${base}.svg`, svg);
  execFileSync(cairosvg, [`${base}.svg`, "-o", `${base}.png`, "--scale", "2"], { stdio: "inherit" });
}

function conceptOverview() {
  const states = [
    box("dsl", 95, 210, 280, 102, "DSL Definition", ["initialState", "guarded transition"], "blue"),
    box("registry", 500, 210, 280, 102, "TransitionRegistry", ["exact keys", "parent keys"], "purple"),
    box("sync", 905, 165, 300, 102, "StateMachine", ["AtomicReference", "TransitionResult"], "green"),
    box("suspend", 905, 315, 300, 102, "SuspendStateMachine", ["Mutex", "StateFlow"], "teal"),
    box("reactive", 1320, 240, 300, 102, "Reactive Runtime", ["send(event)", "effects"], "amber"),
    box("consumer", 1320, 430, 300, 102, "Observers", ["StateFlow", "effects"], "pink"),
  ];
  const arrows = [
    arrow([[375, 261], [500, 261]], "builds"),
    arrow([[780, 250], [905, 216]], "resolves"),
    arrow([[780, 274], [905, 366]], "resolves"),
    arrow([[1205, 366], [1320, 291]], "extends"),
    arrow([[1470, 342], [1470, 430]], "emits"),
    arrow([[1055, 417], [1055, 560], [1470, 560], [1470, 532]], "observes", "teal"),
  ];
  return diagram({
    title: "States Architecture",
    subtitle: "Typed events move through registry resolution into sync, coroutine, or reactive runtimes.",
    intent: "Explain the states module architecture from Kotlin DSL declaration into transition registry resolution, synchronous transition execution, coroutine StateFlow observation, and optional reactive effect emission.",
    evidence: "utils/states/README.md, StateMachineDsl.kt, TransitionRegistry.kt, DefaultStateMachine.kt, SuspendStateMachine.kt",
    sourceRead: "utils/states/README.md;utils/states/src/main/kotlin/**/*.kt",
    width: 1720,
    height: 680,
    panels: [panel(55, 150, 735, 230, "Definition and resolution"), panel(860, 125, 390, 330, "Runtime engines"), panel(1280, 175, 390, 410, "Reactive observation")],
    states,
    arrows,
    footer: "Source-backed: exact transitions, parent transitions, guards, final-state checks, and StateFlow are modeled separately.",
  });
}

function dslBuilder() {
  const states = [
    box("builder", 90, 270, 280, 98, "stateMachine { }", ["initialState", "finalStates"], "blue"),
    box("transition", 470, 200, 290, 98, "transition()", ["from + event", "target state"], "purple"),
    box("parent", 470, 360, 290, 98, "state<Parent>()", ["nested transition", "ambiguity check"], "amber"),
    box("guard", 870, 200, 280, 98, "guard { }", ["optional predicate", "blocks invalid move"], "pink"),
    box("callback", 870, 360, 280, 98, "onTransition", ["prev, event, next", "side-effect hook"], "green"),
    box("machine", 1260, 280, 300, 112, "Machine Instance", ["sync or suspend", "TransitionResult"], "teal"),
  ];
  const arrows = [
    arrow([[370, 319], [470, 249]], "registers"),
    arrow([[370, 319], [470, 409]], "registers"),
    arrow([[760, 249], [870, 249]], "optional"),
    arrow([[760, 409], [870, 409]], "optional"),
    arrow([[1150, 249], [1260, 315]], "builds"),
    arrow([[1150, 409], [1260, 357]], "builds"),
  ];
  return diagram({
    title: "DSL Builder Structure",
    subtitle: "Builder calls collect exact and parent transitions, then construct sync or suspend machines.",
    intent: "Explain the states DSL builder structure by separating initial/final state configuration, exact transition registration, nested parent-state registration, guard predicates, transition callbacks, and machine construction.",
    evidence: "StateMachineDsl.kt, ReactiveStateMachineDsl.kt, README quick-start examples",
    sourceRead: "utils/states/README.md;utils/states/src/main/kotlin/io/bluetape4k/states/core/StateMachineDsl.kt;utils/states/src/main/kotlin/io/bluetape4k/states/reactive/ReactiveStateMachineDsl.kt",
    width: 1660,
    height: 720,
    panels: [panel(55, 220, 340, 190, "Entry"), panel(430, 155, 760, 350, "Rules"), panel(1220, 230, 380, 210, "Output")],
    states,
    arrows,
    footer: "Sequence transition flow remains in utils-states-sequence-01/02.",
  });
}

function turnstile() {
  return stateMachineDiagram({
    title: "Turnstile FSM",
    subtitle: "Coin unlocks the turnstile; push locks it again.",
    width: 1200,
    height: 640,
    intent: "Explain the simple turnstile finite state machine with two states, two events, an initial locked state, and a cyclic transition path.",
    evidence: "utils/states/README.md, Quick Start FSM examples",
    sourceRead: "utils/states/README.md;utils/states/src/test/kotlin/**/*.kt",
    nodes: [
      state("locked", 210, 265, 260, 110, "Locked", ["initial", "push rejected"], "blue"),
      state("unlocked", 730, 265, 260, 110, "Unlocked", ["coin accepted", "push opens"], "green"),
    ],
    arrows: [
      arrow([[120, 320], [210, 320]], "initial"),
      arrow([[470, 320], [730, 320]], "Coin"),
      arrow([[730, 360], [600, 455], [340, 455], [340, 375]], "Push", "green"),
    ],
  });
}

function order() {
  return stateMachineDiagram({
    title: "Order One-Way FSM",
    subtitle: "Linear fulfillment with cancellation only from Created.",
    width: 1600,
    height: 760,
    intent: "Explain the order finite state machine from Created through Paid, Shipped, and Delivered final state, with Cancel as a terminal branch from Created.",
    evidence: "utils/states/README.md, README quick-start order example",
    sourceRead: "utils/states/README.md",
    nodes: [
      state("created", 130, 270, 230, 100, "Created", ["initial"], "blue"),
      state("paid", 500, 270, 230, 100, "Paid", ["payment accepted"], "green"),
      state("shipped", 870, 270, 230, 100, "Shipped", ["carrier handoff"], "amber"),
      state("delivered", 1240, 270, 230, 100, "Delivered", ["final success"], "teal"),
      state("cancelled", 500, 515, 280, 96, "Cancelled", ["final stop"], "pink"),
    ],
    arrows: [
      arrow([[70, 320], [130, 320]], "initial"),
      arrow([[360, 320], [500, 320]], "Pay"),
      arrow([[730, 320], [870, 320]], "Ship"),
      arrow([[1100, 320], [1240, 320]], "Deliver"),
      arrow([[245, 370], [245, 563], [500, 563]], "Cancel", "pink"),
    ],
  });
}

function appointment() {
  return stateMachineDiagram({
    title: "Appointment Complex FSM",
    subtitle: "Clinic flow with guarded lifecycle branches and terminal outcomes.",
    width: 1720,
    height: 1000,
    intent: "Explain the clinic appointment finite state machine with pending request, confirmation, arrival, treatment, completion, no-show, and cancellation terminal branches.",
    evidence: "utils/states/README.md, clinic-appointment migration guide",
    sourceRead: "utils/states/README.md;clinic-appointment source references in README",
    nodes: [
      state("pending", 125, 430, 220, 90, "PENDING", ["initial"], "blue"),
      state("requested", 450, 430, 220, 90, "REQUESTED", ["requested"], "green"),
      state("confirmed", 775, 430, 220, 90, "CONFIRMED", ["accepted"], "purple"),
      state("checked", 775, 210, 220, 90, "CHECKED_IN", ["arrived"], "teal"),
      state("progress", 1140, 210, 245, 90, "IN_PROGRESS", ["treating"], "amber"),
      state("completed", 1140, 710, 245, 90, "COMPLETED", ["final success"], "green"),
      state("cancelled", 125, 710, 245, 90, "CANCELLED", ["final stop"], "pink"),
      state("noshow", 775, 710, 220, 90, "NO_SHOW", ["final"], "olive"),
    ],
    arrows: [
      arrow([[65, 475], [125, 475]], "initial"),
      arrow([[345, 475], [450, 475]], "Request"),
      arrow([[670, 475], [775, 475]], "Confirm"),
      arrow([[885, 430], [885, 300]], "Check in", "teal"),
      arrow([[995, 255], [1140, 255]], "Start"),
      arrow([[1262, 300], [1262, 710]], "Complete", "green"),
      arrow([[885, 520], [885, 710]], "No show", "olive"),
      arrow([[235, 520], [235, 710]], "Cancel", "pink"),
      arrow([[560, 520], [560, 855], [235, 855], [235, 800]], "Cancel", "pink"),
      arrow([[885, 520], [885, 855], [235, 855], [235, 800]], "Cancel", "pink"),
    ],
  });
}

function stateMachineDiagram({ title, subtitle, width, height, intent, evidence, sourceRead, nodes, arrows }) {
  return diagram({
    title,
    subtitle,
    width,
    height,
    intent,
    evidence,
    sourceRead,
    panels: [panel(55, 145, width - 110, height - 235, "State transitions")],
    states: nodes,
    arrows,
    footer: "Generated as direct SVG. Sequence diagrams are intentionally preserved.",
  });
}

function diagram({ title, subtitle, width, height, intent, evidence, sourceRead, panels, states, arrows, footer }) {
  const lines = [];
  lines.push(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="title desc" data-layout="state-machine" data-intent="${esc(intent)}" data-evidence="${esc(evidence)}" data-source-read="${esc(sourceRead)}">`);
  lines.push(`<title id="title">${esc(title)}</title>`);
  lines.push(`<desc id="desc">${esc(subtitle)}</desc>`);
  lines.push(styleBlock());
  lines.push(`<rect width="${width}" height="${height}" fill="#f8fafc"/>`);
  lines.push(`<rect x="30" y="30" width="${width - 60}" height="${height - 60}" rx="8" fill="#f8fafc" stroke="#cbd5e1" stroke-width="1.5"/>`);
  lines.push(`<text class="title" x="70" y="92">${esc(title)}</text>`);
  lines.push(`<text class="subtitle" x="70" y="124">${esc(subtitle)}</text>`);
  for (const item of panels) lines.push(`<rect class="panel" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="18"/><text class="panelTitle" x="${item.x + 24}" y="${item.y + 34}">${esc(item.title)}</text>`);
  lines.push(`<circle cx="86" cy="${states[0].y + states[0].h / 2}" r="10" fill="#172033"/>`);
  for (const item of arrows) lines.push(arrowSvg(item));
  for (const item of states) lines.push(stateSvg(item));
  lines.push(`<circle cx="${width - 88}" cy="${height - 118}" r="13" fill="none" stroke="#172033" stroke-width="3"/><circle cx="${width - 88}" cy="${height - 118}" r="8" fill="#172033"/>`);
  lines.push(`<text class="footer" x="70" y="${height - 44}">${esc(footer)}</text>`);
  lines.push(`</svg>`);
  return lines.join("\n");
}

function styleBlock() {
  return `<style>
    svg { font-family: "Architects Daughter", "Comic Mono", "Comic Sans MS", ui-sans-serif, system-ui, sans-serif; }
    .title { fill: #0f172a; font-size: 34px; font-weight: 800; letter-spacing: 0; }
    .subtitle { fill: #475569; font-size: 18px; font-weight: 500; }
    .panel { fill: #ffffff; stroke: #cbd5e1; stroke-width: 1.5; }
    .panelTitle { fill: #334155; font-size: 14px; font-weight: 800; letter-spacing: .2px; text-transform: uppercase; paint-order: stroke; stroke: #ffffff; stroke-width: 7px; stroke-linejoin: round; }
    .card { stroke-width: 1.7; filter: url(#cardShadow); }
    .stateTitle { fill: #0f172a; font-size: 17px; font-weight: 800; }
    .stateDetail { fill: #475569; font-size: 12px; font-weight: 600; }
    .iconBadge { stroke-width: 1.5; }
    .iconStroke { fill: none; stroke: #ffffff; stroke-width: 2.4; stroke-linecap: round; stroke-linejoin: round; }
    .iconFill { fill: #ffffff; }
    .arrow { fill: none; stroke-width: 2.6; stroke-linecap: round; stroke-linejoin: round; marker-end: url(#arrow); }
    .arrowLabelBg { fill: #ffffff; stroke: #cbd5e1; stroke-width: 1; }
    .arrowLabel { fill: #1e293b; font-size: 13px; font-weight: 800; text-anchor: middle; }
    .footer { fill: #64748b; font-size: 13px; font-weight: 600; }
  </style>
  <defs>
    <filter id="cardShadow" x="-8%" y="-12%" width="116%" height="130%">
      <feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0f172a" flood-opacity="0.10"/>
    </filter>
    <marker id="arrow" markerWidth="12" markerHeight="8" refX="10" refY="4" orient="auto">
      <path d="M0,0 L12,4 L0,8 Z" fill="#475569"/>
    </marker>
  </defs>`;
}

function panel(x, y, w, h, title) {
  return { x, y, w, h, title };
}

function box(id, x, y, w, h, title, details, color) {
  return state(id, x, y, w, h, title, details, color);
}

function state(id, x, y, w, h, title, details, color) {
  return { id, x, y, w, h, title, details, color };
}

function stateSvg(item) {
  const [fill, stroke] = paletteFor(item.color);
  const iconX = item.x + 20;
  const iconY = item.y + Math.max(18, item.h / 2 - 22);
  const textX = item.x + 80;
  const detailRows = item.details.map((detail, index) => `<text class="stateDetail" x="${textX}" y="${item.y + item.h / 2 + 18 + index * 18}">${esc(detail)}</text>`).join("");
  return `<g id="${esc(item.id)}"><rect class="card" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="8" fill="${fill}" stroke="${stroke}"/>${stateIconSvg(item.id, iconX, iconY, stroke)}<text class="stateTitle" x="${textX}" y="${item.y + item.h / 2 - 8}">${esc(item.title)}</text>${detailRows}</g>`;
}

function stateIconSvg(id, x, y, color) {
  const glyphs = {
    dsl: `<path class="iconStroke" d="M13 14l-5 8 5 8M31 14l5 8-5 8M25 11l-6 22"/>`,
    registry: `<path class="iconStroke" d="M12 12h20v20H12zM17 18h10M17 24h10"/>`,
    sync: `<path class="iconStroke" d="M22 10a12 12 0 1 1-9 4M13 14h8v8"/>`,
    suspend: `<path class="iconStroke" d="M14 15h16M14 22h16M14 29h16"/><circle class="iconFill" cx="30" cy="15" r="2"/>`,
    reactive: `<path class="iconStroke" d="M10 22h9M25 22h9M22 10v9M22 25v9"/><circle class="iconStroke" cx="22" cy="22" r="4"/>`,
    consumer: `<path class="iconStroke" d="M10 22c5-7 19-7 24 0-5 7-19 7-24 0z"/><circle class="iconFill" cx="22" cy="22" r="3"/>`,
    builder: `<path class="iconStroke" d="M12 14h20M12 22h20M12 30h12"/>`,
    transition: `<path class="iconStroke" d="M10 22h22M24 14l8 8-8 8"/>`,
    parent: `<path class="iconStroke" d="M11 13h12v12H11zM21 21h12v12H21z"/>`,
    guard: `<path class="iconStroke" d="M22 9l12 6v8c0 7-5 12-12 14-7-2-12-7-12-14v-8z"/>`,
    callback: `<path class="iconStroke" d="M12 14h20v16H12zM16 22h12"/>`,
    machine: `<path class="iconStroke" d="M14 12h16v20H14zM18 17h8M18 23h8M18 29h5"/>`,
  };
  const glyph = glyphs[id] ?? `<circle class="iconStroke" cx="22" cy="22" r="12"/><path class="iconStroke" d="M22 15v7l5 4"/>`;
  return `<g transform="translate(${x},${y})"><rect class="iconBadge" x="0" y="0" width="44" height="44" rx="10" fill="${color}" stroke="${color}"/>${glyph}</g>`;
}

function arrow(points, label, color = "blue") {
  return { points, label, color };
}

function arrowSvg(item) {
  const stroke = paletteFor(item.color)[1];
  const d = item.points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]},${point[1]}`).join(" ");
  const [labelX, labelY] = labelPoint(item.points);
  const labelWidth = Math.max(54, item.label.length * 8 + 18);
  return `<g><path class="route arrow" d="${d}" stroke="${stroke}"/><rect class="arrowLabelBg" x="${labelX - labelWidth / 2}" y="${labelY - 34}" width="${labelWidth}" height="22" rx="11"/><text class="arrowLabel" x="${labelX}" y="${labelY - 18}">${esc(item.label)}</text></g>`;
}

function labelPoint(points) {
  let best = [points[0], points[points.length - 1]];
  let bestLength = -1;
  for (let index = 1; index < points.length; index += 1) {
    const start = points[index - 1];
    const end = points[index];
    const length = Math.hypot(end[0] - start[0], end[1] - start[1]);
    if (length > bestLength) {
      best = [start, end];
      bestLength = length;
    }
  }
  return [(best[0][0] + best[1][0]) / 2, (best[0][1] + best[1][1]) / 2];
}

function paletteFor(key) {
  const colors = {
    blue: ["#eff6ff", "#2563eb"],
    green: ["#f0fdf4", "#16a34a"],
    teal: ["#f0fdfa", "#0d9488"],
    amber: ["#fff7ed", "#ea580c"],
    pink: ["#fdf2f8", "#db2777"],
    purple: ["#faf5ff", "#9333ea"],
    olive: ["#f7fee7", "#65a30d"],
    gray: ["#f9fafb", "#6b7280"],
  };
  return colors[key] ?? colors.gray;
}

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
