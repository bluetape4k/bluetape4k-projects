#!/usr/bin/env node
import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-opentelemetry-diagram-01.svg";
const W = 1360;
const H = 960;
const C = {
  bg: "#ffffff",
  ink: "#111827",
  sub: "#4b5563",
  muted: "#6b7280",
  blue: "#2563eb",
  green: "#16a34a",
  purple: "#9333ea",
  orange: "#ea580c",
  teal: "#0d9488",
  grayFill: "#f9fafb",
  grayStroke: "#d1d5db",
  blueFill: "#eff6ff",
  blueStroke: "#bfdbfe",
  greenFill: "#f0fdf4",
  greenStroke: "#bbf7d0",
  purpleFill: "#faf5ff",
  purpleStroke: "#ddd6fe",
  orangeFill: "#fff7ed",
  orangeStroke: "#fed7aa",
  tealFill: "#f0fdfa",
  tealStroke: "#99f6e4",
};

const esc = (s) =>
  String(s).replace(/[&<>"']/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&apos;" })[ch]);

const svg = [];
const add = (s = "") => svg.push(s);

function text(x, y, value, size = 14, color = C.ink, weight = 400, anchor = "start", extra = "") {
  add(`<text x="${x}" y="${y}" fill="${color}" font-size="${size}" font-weight="${weight}" text-anchor="${anchor}" ${extra}>${esc(value)}</text>`);
}

function rows(x, y, values, size = 13, color = C.sub, gap = 19) {
  values.forEach((value, i) => text(x, y + i * gap, value, size, color, 500));
}

function classCard({ id, x, y, w, h, title, stereotype, attrs, methods, fill, stroke }) {
  add(`<g id="${id}">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="9" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>`);
  add(`<line x1="${x}" y1="${y + 58}" x2="${x + w}" y2="${y + 58}" stroke="${stroke}" stroke-width="1.2"/>`);
  const methodY = y + 58 + attrs.length * 19 + 18;
  add(`<line x1="${x}" y1="${methodY}" x2="${x + w}" y2="${methodY}" stroke="${stroke}" stroke-width="1.2"/>`);
  if (stereotype) text(x + w / 2, y + 23, stereotype, 12, C.muted, 600, "middle");
  text(x + w / 2, y + 45, title, 17, C.ink, 700, "middle");
  rows(x + 18, y + 82, attrs, 12.5, C.sub, 19);
  rows(x + 18, methodY + 25, methods, 12.5, C.sub, 19);
  add(`</g>`);
}

function note({ x, y, w, h, title, body, fill, stroke }) {
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="11" fill="${fill}" stroke="${stroke}" stroke-width="1.6"/>`);
  text(x + 18, y + 30, title, 15, C.ink, 700);
  rows(x + 18, y + 57, body, 12.5, C.sub, 18);
}

function dep(id, d, color, label, lx, ly) {
  add(`<path id="${id}" d="${d}" fill="none" stroke="${color}" stroke-width="2.7" stroke-dasharray="7 5" marker-end="url(#open-${id})" stroke-linecap="round" stroke-linejoin="round"/>`);
  add(`<marker id="open-${id}" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 1 1 L 10 6 L 1 11" fill="none" stroke="${color}" stroke-width="2.4" stroke-dasharray="none" stroke-linecap="round" stroke-linejoin="round"/>
  </marker>`);
  if (label) {
    const w = label.length * 7 + 16;
    add(`<rect x="${lx - w / 2}" y="${ly - 17}" width="${w}" height="24" rx="7" fill="${C.bg}" opacity="0.96"/>`);
    text(lx, ly, label, 12, color, 700, "middle");
  }
}

add(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">`);
add(`<style>
  text { font-family: 'Architects Daughter', 'Comic Mono', 'Comic Sans MS', sans-serif; letter-spacing: 0; }
</style>`);
add(`<rect width="${W}" height="${H}" fill="${C.bg}"/>`);
text(W / 2, 43, "OpenTelemetry Core Class Structure", 28, C.ink, 700, "middle");
text(W / 2, 72, "Kotlin extension groups wrap OpenTelemetry SDK types without adding a new runtime object model", 14, C.muted, 500, "middle");

note({
  x: 70,
  y: 112,
  w: 1220,
  h: 70,
  title: "Reading rule",
  body: ["Dashed open arrows mean dependency/use: each function group adds Kotlin extension functions to, or builds, the OTel type it points to."],
  fill: C.grayFill,
  stroke: C.grayStroke,
});

classCard({
  id: "otel",
  x: 86,
  y: 228,
  w: 260,
  h: 208,
  title: "OpenTelemetry",
  stereotype: "<<OTel API / SDK>>",
  attrs: ["+ GlobalOpenTelemetry", "+ OpenTelemetrySdkBuilder"],
  methods: ["+ tracerBuilder(name)", "+ meterBuilder(name)", "+ propagating(props)"],
  fill: C.grayFill,
  stroke: C.grayStroke,
});
classCard({
  id: "trace-types",
  x: 386,
  y: 228,
  w: 260,
  h: 208,
  title: "Tracer / Span",
  stereotype: "<<OTel trace API>>",
  attrs: ["+ Tracer", "+ SpanBuilder", "+ Span"],
  methods: ["+ startSpan()", "+ makeCurrent()", "+ end()"],
  fill: C.grayFill,
  stroke: C.grayStroke,
});
classCard({
  id: "context-flow",
  x: 686,
  y: 228,
  w: 260,
  h: 208,
  title: "Context / Flow",
  stereotype: "<<coroutine bridge>>",
  attrs: ["+ Context", "+ CoroutineContext", "+ Flow<T>"],
  methods: ["+ asContextElement()", "+ collect()", "+ channelFlow()"],
  fill: C.grayFill,
  stroke: C.grayStroke,
});
classCard({
  id: "metrics-attrs",
  x: 986,
  y: 228,
  w: 260,
  h: 208,
  title: "Metrics / Attributes",
  stereotype: "<<OTel common API>>",
  attrs: ["+ AttributesBuilder", "+ SdkMeterProviderBuilder"],
  methods: ["+ build()", "+ registerMetricReader()", "+ put(key, value)"],
  fill: C.grayFill,
  stroke: C.grayStroke,
});

classCard({
  id: "support",
  x: 86,
  y: 590,
  w: 260,
  h: 212,
  title: "OpenTelemetrySupport.kt",
  stereotype: "<<top-level extensions>>",
  attrs: ["+ globalOpenTelemetry", "+ NoopOpenTelemetry"],
  methods: ["+ openTelemetrySdk { }", "+ openTelemetrySdkGlobal { }", "+ OpenTelemetry.tracer(...)"],
  fill: C.blueFill,
  stroke: C.blueStroke,
});
classCard({
  id: "trace-support",
  x: 386,
  y: 590,
  w: 260,
  h: 212,
  title: "Trace Support",
  stereotype: "<<span lifecycle>>",
  attrs: ["+ noopTraceProvider", "+ InvalidSpanContext"],
  methods: ["+ Tracer.startSpan(...)", "+ Span.use { }", "+ Tracer.withSpan(...)"],
  fill: C.greenFill,
  stroke: C.greenStroke,
});
classCard({
  id: "coroutine-support",
  x: 686,
  y: 590,
  w: 260,
  h: 212,
  title: "Coroutine / Flow Support",
  stereotype: "<<context propagation>>",
  attrs: ["+ Span.useSuspending", "+ Flow.traced"],
  methods: ["+ withSpanContext(...)", "+ Flow.tracedCollect(...)", "+ withOtelContext { }"],
  fill: C.purpleFill,
  stroke: C.purpleStroke,
});
classCard({
  id: "dsl-support",
  x: 986,
  y: 590,
  w: 260,
  h: 212,
  title: "Common / Metrics DSL",
  stereotype: "<<builder helpers>>",
  attrs: ["+ AttributeKey helpers", "+ NoopMeterProvider"],
  methods: ["+ attributes { }", "+ Map.toAttributes()", "+ sdkMeterProvider { }"],
  fill: C.orangeFill,
  stroke: C.orangeStroke,
});

dep("support-otel", "M 216 590 V 436", C.blue, "builds / registers", 218, 512);
dep("trace-tracer", "M 516 590 V 436", C.green, "manages span", 520, 512);
dep("coro-trace", "M 746 590 V 512 H 586 V 436", C.purple, "uses span", 648, 500);
dep("coro-context", "M 816 590 V 436", C.purple, "propagates", 820, 512);
dep("dsl-metrics", "M 1116 590 V 436", C.orange, "builds", 1120, 512);

add(`<g transform="translate(86, 900)">`);
text(0, 0, "Relationship", 13, C.ink, 700);
add(`<line x1="110" y1="-4" x2="154" y2="-4" stroke="${C.green}" stroke-width="2.7" stroke-dasharray="7 5"/>`);
add(`<path d="M 154 -9 L 164 -4 L 154 1" fill="none" stroke="${C.green}" stroke-width="2.4" stroke-dasharray="none" stroke-linecap="round" stroke-linejoin="round"/>`);
text(176, 0, "dependency / extension receiver, not inheritance", 12, C.muted, 600);
add(`</g>`);

add(`</svg>`);

writeFileSync(out, svg.join("\n"));
console.log(out);
