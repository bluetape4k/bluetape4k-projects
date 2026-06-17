#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";

const id = process.argv[2];
if (!id || !["01", "02", "03", "04"].includes(id)) {
  console.error("Usage: node scripts/generate-spring-boot-core-diagram.mjs <01|02|03|04>");
  process.exit(1);
}

const root = resolve(dirname(new URL(import.meta.url).pathname), "..");
const outDir = resolve(root, "docs/images/readme-diagrams");
mkdirSync(outDir, { recursive: true });

const font = "'Architects Daughter', 'Comic Mono', 'Helvetica Neue', Arial, sans-serif";

function esc(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function textLines(lines, x, y, opts = {}) {
  const {
    size = 15,
    weight = 400,
    fill = "#1f2937",
    anchor = "start",
    line = 19,
    cls = "",
  } = opts;
  return lines
    .map((item, idx) => {
      return `<text ${cls ? `class="${cls}" ` : ""}x="${x}" y="${y + idx * line}" text-anchor="${anchor}" font-size="${size}" font-weight="${weight}" fill="${fill}">${esc(item)}</text>`;
    })
    .join("\n");
}

function card({ x, y, w, h, title, lines, fill, stroke, accent, icon }) {
  const iconSvg = icon
    ? `<circle cx="${x + 25}" cy="${y + 27}" r="13" fill="${accent}" opacity="0.18"/>
       <text x="${x + 25}" y="${y + 32}" text-anchor="middle" font-size="15" font-weight="700" fill="${stroke}">${esc(icon)}</text>`
    : "";
  return `
  <g class="card" data-card="${esc(title)}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    ${iconSvg}
    <text x="${x + (icon ? 48 : 18)}" y="${y + 31}" font-size="18" font-weight="700" fill="#111827">${esc(title)}</text>
    ${textLines(lines, x + 18, y + 59, { size: 13.5, fill: "#4b5563", line: 18 })}
  </g>`;
}

function lane({ x, y, w, h, title, note, fill, stroke }) {
  return `
  <g class="lane" data-lane="${esc(title)}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" stroke-width="1.3" stroke-dasharray="7 5"/>
    <text x="${x + 18}" y="${y + 26}" font-size="14" font-weight="700" fill="${stroke}">${esc(title)}</text>
    <text x="${x + 18}" y="${y + 47}" font-size="12.5" fill="#6b7280">${esc(note)}</text>
  </g>`;
}

function line({ d, color, marker = "arrow-blue", width = 3.2, dash = "" }) {
  return `<path class="edge" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})"/>`;
}

function diagram01() {
  const width = 1360;
  const height = 820;
  const cards = [
    card({
      x: 64,
      y: 194,
      w: 360,
      h: 126,
      title: "Core Kotlin Extensions",
      lines: [
        "BeanFactory get/findBean operators",
        "Merged annotation lookup + copy",
        "PropertyResolver and profile annotations",
      ],
      fill: "#eff6ff",
      stroke: "#2563eb",
      accent: "#2563eb",
      icon: "{}",
    }),
    card({
      x: 64,
      y: 354,
      w: 360,
      h: 126,
      title: "Support Utilities",
      lines: [
        "API error response and exception helpers",
        "ExampleMatcher, DataBuffer, MessageBuilder",
        "Model, StopWatch, member/toString helpers",
      ],
      fill: "#f0fdfa",
      stroke: "#0f766e",
      accent: "#0f766e",
      icon: "+",
    }),
    card({
      x: 500,
      y: 194,
      w: 360,
      h: 132,
      title: "WebFlux Coroutine Bases",
      lines: [
        "Default, IO, and virtual-thread scopes",
        "SupervisorJob isolates sibling failures",
        "@PreDestroy cancels controller scope",
      ],
      fill: "#fff7ed",
      stroke: "#ea580c",
      accent: "#ea580c",
      icon: "~",
    }),
    card({
      x: 500,
      y: 366,
      w: 360,
      h: 132,
      title: "HTTP Client Helpers",
      lines: [
        "RestClient suspend verbs on Dispatchers.IO",
        "WebClient/WebTestClient verb extensions",
        "Flow and Publisher request-body overloads",
      ],
      fill: "#f5f3ff",
      stroke: "#7c3aed",
      accent: "#7c3aed",
      icon: "->",
    }),
    card({
      x: 936,
      y: 194,
      w: 360,
      h: 126,
      title: "Observation Boundary",
      lines: [
        "observeSpring and suspending variants",
        "Low/high cardinality key values",
        "Reactor + coroutine observation context",
      ],
      fill: "#ecfdf5",
      stroke: "#16a34a",
      accent: "#16a34a",
      icon: "o",
    }),
    card({
      x: 936,
      y: 354,
      w: 360,
      h: 126,
      title: "Application-Owned Export",
      lines: [
        "Prometheus and OTLP stay in Actuator config",
        "No custom exporter or global SDK mutation",
        "Boot app keeps backend ownership",
      ],
      fill: "#f8fafc",
      stroke: "#475569",
      accent: "#475569",
      icon: "*",
    }),
  ];

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Core capability map">
  <style>
    text { font-family: ${font}; dominant-baseline: alphabetic; }
    .subtitle { font-family: ${font}; }
  </style>
  <defs>
    <marker id="arrow-blue" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#2563eb" stroke="#2563eb" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-purple" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#7c3aed" stroke="#7c3aed" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-green" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#16a34a" stroke="#16a34a" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-gray" markerUnits="userSpaceOnUse" markerWidth="12" markerHeight="12" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#64748b" stroke="#64748b" stroke-width="0" stroke-dasharray="none"/>
    </marker>
  </defs>
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="64" y="62" font-size="30" font-weight="700" fill="#111827">Spring Boot Core Capability Map</text>
  <text class="subtitle" x="64" y="92" font-size="15" fill="#64748b">Source-backed module surface: Kotlin-friendly Spring APIs, coroutine runtime helpers, HTTP clients, and observability boundaries.</text>

  ${lane({ x: 42, y: 122, w: 404, h: 430, title: "DEVELOPER-FACING APIS", note: "Small Kotlin extensions used directly by application code.", fill: "#eff6ff", stroke: "#2563eb" })}
  ${lane({ x: 478, y: 122, w: 404, h: 430, title: "COROUTINE AND HTTP RUNTIME", note: "Runtime adapters that bind Spring APIs to coroutine usage.", fill: "#fff7ed", stroke: "#ea580c" })}
  ${lane({ x: 914, y: 122, w: 404, h: 430, title: "OBSERVABILITY OWNERSHIP", note: "Helpers create scopes; the application owns exporters.", fill: "#ecfdf5", stroke: "#16a34a" })}

  ${line({ d: "M424 258 L500 258", color: "#2563eb", marker: "arrow-blue" })}
  ${line({ d: "M424 417 L500 417", color: "#7c3aed", marker: "arrow-purple" })}
  ${line({ d: "M860 260 L936 260", color: "#16a34a", marker: "arrow-green" })}
  ${line({ d: "M1116 320 L1116 354", color: "#64748b", marker: "arrow-gray", width: 2.8, dash: "6 5" })}
  ${cards.join("\n")}

  <g class="boundary" data-boundary="spring-boot-app">
    <rect x="218" y="612" width="924" height="106" rx="10" fill="#f8fafc" stroke="#cbd5e1" stroke-width="1.5"/>
    <text x="680" y="652" text-anchor="middle" font-size="20" font-weight="700" fill="#111827">Spring Boot 4 Application</text>
    <text x="680" y="680" text-anchor="middle" font-size="14" fill="#475569">Imports the module, chooses which extension surface to use, and keeps concrete infrastructure configuration in the app.</text>
  </g>
  ${line({ d: "M244 480 L244 612", color: "#2563eb", marker: "arrow-blue", width: 2.8 })}
  ${line({ d: "M680 498 L680 612", color: "#7c3aed", marker: "arrow-purple", width: 2.8 })}
  ${line({ d: "M1116 480 L1116 612", color: "#16a34a", marker: "arrow-green", width: 2.8 })}

  <g class="legend" transform="translate(64 760)">
    <line x1="0" y1="0" x2="38" y2="0" stroke="#2563eb" stroke-width="3.2" marker-end="url(#arrow-blue)"/>
    <text x="52" y="5" font-size="13" fill="#475569">Kotlin API surface</text>
    <line x1="230" y1="0" x2="268" y2="0" stroke="#7c3aed" stroke-width="3.2" marker-end="url(#arrow-purple)"/>
    <text x="282" y="5" font-size="13" fill="#475569">Coroutine/HTTP adapter</text>
    <line x1="510" y1="0" x2="548" y2="0" stroke="#16a34a" stroke-width="3.2" marker-end="url(#arrow-green)"/>
    <text x="562" y="5" font-size="13" fill="#475569">Observation scope</text>
    <line x1="750" y1="0" x2="788" y2="0" stroke="#64748b" stroke-width="2.8" stroke-dasharray="6 5" marker-end="url(#arrow-gray)"/>
    <text x="802" y="5" font-size="13" fill="#475569">Application-owned config</text>
  </g>
</svg>`;
}

function smallCard({ x, y, w, h, title, lines, fill, stroke }) {
  return `
  <g class="card" data-card="${esc(title)}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    <text x="${x + w / 2}" y="${y + 31}" text-anchor="middle" font-size="17" font-weight="700" fill="#111827">${esc(title)}</text>
    ${textLines(lines, x + w / 2, y + 58, { size: 13, fill: "#4b5563", line: 17, anchor: "middle" })}
  </g>`;
}

function diagram02() {
  const width = 1440;
  const height = 900;
  const cards = [
    smallCard({
      x: 210,
      y: 202,
      w: 230,
      h: 92,
      title: "HTTP Request",
      lines: ["WebFlux server", "receives the call"],
      fill: "#eff6ff",
      stroke: "#2563eb",
    }),
    smallCard({
      x: 530,
      y: 202,
      w: 250,
      h: 92,
      title: "Handler Method",
      lines: ["suspend result or Flow", "declared by app code"],
      fill: "#f0fdfa",
      stroke: "#0f766e",
    }),
    smallCard({
      x: 870,
      y: 202,
      w: 260,
      h: 92,
      title: "Controller Base",
      lines: ["Default, IO, or VT", "CoroutineScope is delegated"],
      fill: "#fff7ed",
      stroke: "#ea580c",
    }),
    smallCard({
      x: 240,
      y: 456,
      w: 250,
      h: 100,
      title: "Default Scope",
      lines: ["Dispatchers.Default", "CPU-oriented work"],
      fill: "#fff7ed",
      stroke: "#ea580c",
    }),
    smallCard({
      x: 575,
      y: 456,
      w: 250,
      h: 100,
      title: "IO Scope",
      lines: ["Dispatchers.IO", "blocking bridge work"],
      fill: "#f5f3ff",
      stroke: "#7c3aed",
    }),
    smallCard({
      x: 910,
      y: 456,
      w: 250,
      h: 100,
      title: "Virtual Thread Scope",
      lines: ["Dispatchers.VT", "virtual-thread adapter"],
      fill: "#ecfdf5",
      stroke: "#16a34a",
    }),
    smallCard({
      x: 520,
      y: 710,
      w: 250,
      h: 96,
      title: "Service Work",
      lines: ["child coroutines run", "SupervisorJob isolates failures"],
      fill: "#f8fafc",
      stroke: "#475569",
    }),
    smallCard({
      x: 860,
      y: 710,
      w: 260,
      h: 96,
      title: "Response Bridge",
      lines: ["Flow/Publisher/Mono", "encoded by WebFlux"],
      fill: "#eff6ff",
      stroke: "#2563eb",
    }),
  ];

  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring WebFlux coroutine request flow">
  <style>
    text { font-family: ${font}; dominant-baseline: alphabetic; }
  </style>
  <defs>
    <marker id="arrow-blue" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#2563eb" stroke="#2563eb" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-orange" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#ea580c" stroke="#ea580c" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-gray" markerUnits="userSpaceOnUse" markerWidth="12" markerHeight="12" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#64748b" stroke="#64748b" stroke-width="0" stroke-dasharray="none"/>
    </marker>
  </defs>
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="70" y="62" font-size="30" font-weight="700" fill="#111827">Spring WebFlux + Coroutines Request Flow</text>
  <text x="70" y="92" font-size="15" fill="#64748b">A request enters Spring WebFlux, app code chooses the coroutine controller base, and the selected scope drives service work.</text>

  ${lane({ x: 46, y: 122, w: 1220, h: 210, title: "WEBFLUX ENTRY", note: "Spring routes the HTTP call to an application handler method.", fill: "#eff6ff", stroke: "#2563eb" })}
  ${lane({ x: 46, y: 376, w: 1220, h: 210, title: "CONTROLLER-OWNED COROUTINE SCOPE", note: "The abstract base class chooses the Dispatcher and owns cancellation on bean destruction.", fill: "#fff7ed", stroke: "#ea580c" })}
  ${lane({ x: 46, y: 630, w: 1220, h: 210, title: "APPLICATION WORK AND RESPONSE", note: "The service result flows back through WebFlux response encoding.", fill: "#f8fafc", stroke: "#64748b" })}

  ${line({ d: "M440 248 L530 248", color: "#2563eb", marker: "arrow-blue" })}
  ${line({ d: "M780 248 L870 248", color: "#2563eb", marker: "arrow-blue" })}
  ${line({ d: "M1000 294 L1000 354 L365 354 L365 456", color: "#ea580c", marker: "arrow-orange" })}
  ${line({ d: "M1000 294 L1000 354 L700 354 L700 456", color: "#ea580c", marker: "arrow-orange" })}
  ${line({ d: "M1000 294 L1000 354 L1035 354 L1035 456", color: "#ea580c", marker: "arrow-orange" })}
  ${line({ d: "M365 556 L365 604 L645 604 L645 710", color: "#64748b", marker: "arrow-gray", width: 2.8 })}
  ${line({ d: "M700 556 L700 604 L645 604 L645 710", color: "#64748b", marker: "arrow-gray", width: 2.8 })}
  ${line({ d: "M1035 556 L1035 604 L645 604 L645 710", color: "#64748b", marker: "arrow-gray", width: 2.8 })}
  ${line({ d: "M770 758 L860 758", color: "#2563eb", marker: "arrow-blue" })}

  ${cards.join("\n")}

  <g class="note" data-note="security-context">
    <rect x="1300" y="200" width="96" height="132" rx="8" fill="#fff7ed" stroke="#f59e0b" stroke-width="1.5"/>
    <text x="1348" y="232" text-anchor="middle" font-size="15" font-weight="700" fill="#92400e">Caveat</text>
    ${textLines(["Security", "context is", "manual"], 1348, 262, { size: 12.5, fill: "#78350f", line: 17, anchor: "middle" })}
  </g>
  ${line({ d: "M1130 248 L1300 248", color: "#64748b", marker: "arrow-gray", width: 2.5, dash: "6 5" })}

  <g class="legend" transform="translate(70 858)">
    <line x1="0" y1="0" x2="38" y2="0" stroke="#2563eb" stroke-width="3.2" marker-end="url(#arrow-blue)"/>
    <text x="52" y="5" font-size="13" fill="#475569">Request/response path</text>
    <line x1="260" y1="0" x2="298" y2="0" stroke="#ea580c" stroke-width="3.2" marker-end="url(#arrow-orange)"/>
    <text x="312" y="5" font-size="13" fill="#475569">Dispatcher choice</text>
    <line x1="500" y1="0" x2="538" y2="0" stroke="#64748b" stroke-width="2.8" marker-end="url(#arrow-gray)"/>
    <text x="552" y="5" font-size="13" fill="#475569">Scope-owned work</text>
  </g>
</svg>`;
}

function diagram03() {
  const width = 1440;
  const height = 860;
  const cards = [
    smallCard({
      x: 96,
      y: 230,
      w: 248,
      h: 96,
      title: "Application Code",
      lines: ["restClientOf(baseUrl)", "or existing RestClient"],
      fill: "#eff6ff",
      stroke: "#2563eb",
    }),
    smallCard({
      x: 420,
      y: 230,
      w: 248,
      h: 96,
      title: "Builder DSL",
      lines: ["baseUrl + builder block", "builds RestClient"],
      fill: "#f0fdfa",
      stroke: "#0f766e",
    }),
    smallCard({
      x: 744,
      y: 230,
      w: 248,
      h: 96,
      title: "Spring RestClient",
      lines: ["blocking client facade", "get/post/put/patch/delete"],
      fill: "#fff7ed",
      stroke: "#ea580c",
    }),
    smallCard({
      x: 420,
      y: 470,
      w: 248,
      h: 104,
      title: "Suspend Verb",
      lines: ["suspendGet/Post/Put/Patch", "suspendDelete"],
      fill: "#f5f3ff",
      stroke: "#7c3aed",
    }),
    smallCard({
      x: 744,
      y: 470,
      w: 248,
      h: 104,
      title: "Interruptible Block",
      lines: ["runInterruptible", "Dispatchers.IO"],
      fill: "#f8fafc",
      stroke: "#475569",
    }),
    smallCard({
      x: 1068,
      y: 470,
      w: 260,
      h: 104,
      title: "Response Decode",
      lines: ["retrieve().body(T::class.java)", "or toBodilessEntity()"],
      fill: "#ecfdf5",
      stroke: "#16a34a",
    }),
    smallCard({
      x: 1068,
      y: 620,
      w: 260,
      h: 96,
      title: "Coroutine Caller",
      lines: ["gets typed value", "or nullable value"],
      fill: "#eff6ff",
      stroke: "#2563eb",
    }),
  ];
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="RestClient coroutine DSL structure">
  <style>
    text { font-family: ${font}; dominant-baseline: alphabetic; }
  </style>
  <defs>
    <marker id="arrow-blue" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#2563eb" stroke="#2563eb" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-purple" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#7c3aed" stroke="#7c3aed" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-green" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#16a34a" stroke="#16a34a" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-gray" markerUnits="userSpaceOnUse" markerWidth="12" markerHeight="12" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#64748b" stroke="#64748b" stroke-width="0" stroke-dasharray="none"/>
    </marker>
  </defs>
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="76" y="62" font-size="30" font-weight="700" fill="#111827">RestClient Coroutine DSL Structure</text>
  <text x="76" y="92" font-size="15" fill="#64748b">The module keeps Spring RestClient blocking semantics explicit while offering coroutine-friendly suspend wrappers.</text>

  ${lane({ x: 54, y: 132, w: 320, h: 604, title: "CALLER", note: "Starts DSL; receives the result.", fill: "#eff6ff", stroke: "#2563eb" })}
  ${lane({ x: 388, y: 132, w: 320, h: 604, title: "DSL WRAPPER", note: "Builds clients and exposes suspend verbs.", fill: "#f5f3ff", stroke: "#7c3aed" })}
  ${lane({ x: 722, y: 132, w: 320, h: 604, title: "BLOCKING EXCHANGE", note: "Runs RestClient in Dispatchers.IO.", fill: "#f8fafc", stroke: "#64748b" })}
  ${lane({ x: 1056, y: 132, w: 320, h: 604, title: "RESULT", note: "Decodes body for the caller.", fill: "#ecfdf5", stroke: "#16a34a" })}

  ${line({ d: "M344 278 L420 278", color: "#2563eb", marker: "arrow-blue" })}
  ${line({ d: "M668 278 L744 278", color: "#2563eb", marker: "arrow-blue" })}
  ${line({ d: "M220 326 L220 522 L420 522", color: "#7c3aed", marker: "arrow-purple" })}
  ${line({ d: "M668 522 L744 522", color: "#7c3aed", marker: "arrow-purple" })}
  ${line({ d: "M868 470 L868 378 L868 326", color: "#64748b", marker: "arrow-gray", width: 2.8 })}
  ${line({ d: "M992 522 L1068 522", color: "#16a34a", marker: "arrow-green", width: 3.0 })}
  ${line({ d: "M1198 574 L1198 620", color: "#16a34a", marker: "arrow-green", width: 3.0 })}

  ${cards.join("\n")}

  <g class="note" data-note="cancellation">
    <rect x="744" y="616" width="248" height="96" rx="8" fill="#fff7ed" stroke="#f59e0b" stroke-width="1.5"/>
    <text x="868" y="646" text-anchor="middle" font-size="16" font-weight="700" fill="#92400e">Cancellation boundary</text>
    ${textLines(["Thread interruption works only", "when the request factory", "honors interruption."], 868, 672, { size: 12.5, fill: "#78350f", line: 16, anchor: "middle" })}
  </g>
  ${line({ d: "M1068 668 L992 668", color: "#64748b", marker: "arrow-gray", width: 2.5, dash: "6 5" })}

  <g class="legend" transform="translate(76 814)">
    <line x1="0" y1="0" x2="38" y2="0" stroke="#2563eb" stroke-width="3.2" marker-end="url(#arrow-blue)"/>
    <text x="52" y="5" font-size="13" fill="#475569">Construction path</text>
    <line x1="238" y1="0" x2="276" y2="0" stroke="#7c3aed" stroke-width="3.2" marker-end="url(#arrow-purple)"/>
    <text x="290" y="5" font-size="13" fill="#475569">Suspend wrapper path</text>
    <line x1="520" y1="0" x2="558" y2="0" stroke="#16a34a" stroke-width="3.0" marker-end="url(#arrow-green)"/>
    <text x="572" y="5" font-size="13" fill="#475569">Decoded result</text>
    <line x1="720" y1="0" x2="758" y2="0" stroke="#64748b" stroke-width="2.7" stroke-dasharray="6 5" marker-end="url(#arrow-gray)"/>
    <text x="772" y="5" font-size="13" fill="#475569">Blocking boundary note</text>
  </g>
</svg>`;
}

function diagram04() {
  const width = 1296;
  const height = 860;
  const cards = [
    smallCard({
      x: 86,
      y: 220,
      w: 248,
      h: 96,
      title: "Application Config",
      lines: ["subclasses AbstractWebClientConfig", "overrides resource knobs"],
      fill: "#eff6ff",
      stroke: "#2563eb",
    }),
    smallCard({
      x: 86,
      y: 390,
      w: 248,
      h: 96,
      title: "AbstractWebClientConfig",
      lines: ["declares Spring beans", "owns client resource wiring"],
      fill: "#f0fdfa",
      stroke: "#0f766e",
    }),
    smallCard({
      x: 86,
      y: 596,
      w: 248,
      h: 96,
      title: "Tuning Inputs",
      lines: ["threadCount, timeouts, SSL", "maxInMemorySize"],
      fill: "#fff7ed",
      stroke: "#ea580c",
    }),
    smallCard({
      x: 468,
      y: 204,
      w: 236,
      h: 88,
      title: "LoopResources",
      lines: ["web-client-thread-", "dedicated workers"],
      fill: "#f5f3ff",
      stroke: "#7c3aed",
    }),
    smallCard({
      x: 468,
      y: 344,
      w: 236,
      h: 88,
      title: "ReactorResourceFactory",
      lines: ["useGlobalResources = false", "shutdown timeout"],
      fill: "#f8fafc",
      stroke: "#475569",
    }),
    smallCard({
      x: 468,
      y: 484,
      w: 236,
      h: 88,
      title: "Client Connector",
      lines: ["SSL context", "connect/response timeout"],
      fill: "#ecfdf5",
      stroke: "#16a34a",
    }),
    smallCard({
      x: 468,
      y: 596,
      w: 236,
      h: 88,
      title: "ExchangeStrategies",
      lines: ["codec memory limit", "Spring codecs stay default"],
      fill: "#eff6ff",
      stroke: "#2563eb",
    }),
    smallCard({
      x: 882,
      y: 500,
      w: 288,
      h: 170,
      title: "WebClient Bean",
      lines: ["builder.clientConnector(connector)", "builder.exchangeStrategies(strategies)", "uses dedicated resources"],
      fill: "#ecfdf5",
      stroke: "#16a34a",
    }),
  ];
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="WebClient dedicated resource configuration">
  <style>
    text { font-family: ${font}; dominant-baseline: alphabetic; }
  </style>
  <defs>
    <marker id="arrow-blue" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#2563eb" stroke="#2563eb" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-green" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#16a34a" stroke="#16a34a" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-gray" markerUnits="userSpaceOnUse" markerWidth="12" markerHeight="12" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#64748b" stroke="#64748b" stroke-width="0" stroke-dasharray="none"/>
    </marker>
    <marker id="arrow-purple" markerUnits="userSpaceOnUse" markerWidth="14" markerHeight="14" viewBox="0 0 10 10" refX="9" refY="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 Z" fill="#7c3aed" stroke="#7c3aed" stroke-width="0" stroke-dasharray="none"/>
    </marker>
  </defs>
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="76" y="62" font-size="30" font-weight="700" fill="#111827">WebClient Dedicated Resource Configuration</text>
  <text x="76" y="92" font-size="15" fill="#64748b">AbstractWebClientConfig builds an isolated WebClient resource set from application-owned tuning inputs.</text>

  ${lane({ x: 54, y: 132, w: 310, h: 620, title: "CONFIG INPUT", note: "Application overrides and tuning values.", fill: "#eff6ff", stroke: "#2563eb" })}
  ${lane({ x: 430, y: 132, w: 318, h: 620, title: "BASE CONFIG BEANS", note: "Beans produced by AbstractWebClientConfig.", fill: "#f8fafc", stroke: "#64748b" })}
  ${lane({ x: 844, y: 132, w: 398, h: 620, title: "DEDICATED WEBCLIENT", note: "Final client uses isolated Reactor resources.", fill: "#ecfdf5", stroke: "#16a34a" })}

  ${line({ d: "M210 316 L210 390", color: "#2563eb", marker: "arrow-blue" })}
  ${line({ d: "M334 438 L390 438 L390 248 L468 248", color: "#7c3aed", marker: "arrow-purple" })}
  ${line({ d: "M334 438 L390 438 L390 388 L468 388", color: "#64748b", marker: "arrow-gray", width: 2.8 })}
  ${line({ d: "M334 438 L390 438 L390 528 L468 528", color: "#16a34a", marker: "arrow-green" })}
  ${line({ d: "M334 644 L468 644", color: "#2563eb", marker: "arrow-blue" })}
  ${line({ d: "M586 292 L586 344", color: "#7c3aed", marker: "arrow-purple" })}
  ${line({ d: "M586 432 L586 484", color: "#64748b", marker: "arrow-gray", width: 2.8 })}
  ${line({ d: "M704 528 L882 528", color: "#16a34a", marker: "arrow-green" })}
  ${line({ d: "M704 644 L882 644", color: "#2563eb", marker: "arrow-blue" })}

  ${cards.join("\n")}

  <g class="note" data-note="ssl">
    <rect x="912" y="322" width="246" height="104" rx="8" fill="#fff7ed" stroke="#f59e0b" stroke-width="1.5"/>
    <text x="1035" y="354" text-anchor="middle" font-size="16" font-weight="700" fill="#92400e">SSL override</text>
    ${textLines(["Default trust store is used.", "insecureSslContext() is", "development/test only."], 1035, 380, { size: 12.5, fill: "#78350f", line: 16, anchor: "middle" })}
  </g>
  ${line({ d: "M912 374 L704 528", color: "#64748b", marker: "arrow-gray", width: 2.5, dash: "6 5" })}

  <g class="legend" transform="translate(76 814)">
    <line x1="0" y1="0" x2="38" y2="0" stroke="#2563eb" stroke-width="3.2" marker-end="url(#arrow-blue)"/>
    <text x="52" y="5" font-size="13" fill="#475569">Application input</text>
    <line x1="238" y1="0" x2="276" y2="0" stroke="#7c3aed" stroke-width="3.2" marker-end="url(#arrow-purple)"/>
    <text x="290" y="5" font-size="13" fill="#475569">Dedicated loop resource</text>
    <line x1="540" y1="0" x2="578" y2="0" stroke="#16a34a" stroke-width="3.2" marker-end="url(#arrow-green)"/>
    <text x="592" y="5" font-size="13" fill="#475569">Connector/WebClient wiring</text>
    <line x1="850" y1="0" x2="888" y2="0" stroke="#64748b" stroke-width="2.7" stroke-dasharray="6 5" marker-end="url(#arrow-gray)"/>
    <text x="902" y="5" font-size="13" fill="#475569">Configuration caveat</text>
  </g>
</svg>`;
}

const svg = id === "01" ? diagram01() : id === "02" ? diagram02() : id === "03" ? diagram03() : diagram04();
const svgPath = resolve(outDir, `spring-boot-core-diagram-${id}.svg`);
const pngPath = resolve(outDir, `spring-boot-core-diagram-${id}.png`);
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync("/Users/debop/.local/bin/cairosvg", [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`generated ${svgPath}`);
console.log(`generated ${pngPath}`);
