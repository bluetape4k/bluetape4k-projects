#!/usr/bin/env node
import {writeFileSync} from "node:fs";

const out = "docs/images/readme-diagrams/infra-opentelemetry-diagram-02.svg";
const W = 1360;
const H = 820;
const C = {
    bg: "#ffffff",
    ink: "#111827",
    sub: "#4b5563",
    muted: "#6b7280",
    line: "#d1d5db",
    blue: "#2563eb",
    green: "#16a34a",
    purple: "#9333ea",
    orange: "#ea580c",
    teal: "#0d9488",
    grayFill: "#f9fafb",
    grayStroke: "#e5e7eb",
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
    redFill: "#fef2f2",
    redStroke: "#fecaca",
};

const esc = (s) =>
    String(s).replace(/[&<>"']/g, (ch) => ({"&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&apos;"})[ch]);

const svg = [];
const add = (s = "") => svg.push(s);

function text(x, y, value, size = 14, color = C.ink, weight = 400, anchor = "start") {
    add(`<text x="${x}" y="${y}" fill="${color}" font-size="${size}" font-weight="${weight}" text-anchor="${anchor}">${esc(value)}</text>`);
}

function rows(x, y, values, size = 13, color = C.sub, gap = 18) {
    values.forEach((value, i) => text(x, y + i * gap, value, size, color, 500));
}

function layer({x, y, w, h, title, subtitle, fill, stroke}) {
    add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="16" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>`);
    text(x + 20, y + 31, title, 15, C.ink, 700);
    text(x + 20, y + 53, subtitle, 12, C.muted, 500);
}

function card({id, x, y, w, h, title, subtitle, body, fill, stroke}) {
    add(`<g id="${id}">`);
    add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="11" fill="${fill}" stroke="${stroke}" stroke-width="1.9"/>`);
    text(x + 18, y + 30, title, 16, C.ink, 700);
    if (subtitle) text(x + 18, y + 52, subtitle, 12, C.muted, 600);
    rows(x + 18, y + (subtitle ? 78 : 58), body, 13, C.sub, 18);
    add(`</g>`);
}

function arrow(id, d, color, marker, width = 3.4, dash = "") {
    add(`<path id="${id}" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" ${dash ? `stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})" stroke-linecap="round" stroke-linejoin="round"/>`);
}

function label(x, y, value, color) {
    const w = value.length * 7 + 18;
    add(`<rect x="${x - w / 2}" y="${y - 17}" width="${w}" height="24" rx="7" fill="${C.bg}" opacity="0.96"/>`);
    text(x, y, value, 12, color, 700, "middle");
}

add(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">`);
add(`<style>
  text { font-family: 'Architects Daughter', 'Comic Mono', 'Comic Sans MS', sans-serif; letter-spacing: 0; }
</style>`);
add(`<defs>
  ${["blue", "green", "purple", "orange", "teal"].map((name) => {
    const color = C[name];
    return `<marker id="arrow-${name}" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse">
      <path d="M 0 0 L 14 7 L 0 14 Z" fill="${color}" stroke="${color}" stroke-width="0" stroke-dasharray="none"/>
    </marker>`;
}).join("\n")}
</defs>`);
add(`<rect width="${W}" height="${H}" fill="${C.bg}"/>`);
text(W / 2, 43, "OpenTelemetry Component Overview", 28, C.ink, 700, "middle");
text(W / 2, 72, "Application instrumentation enters bluetape4k helpers, then the OTel SDK ships traces and metrics to exporters", 14, C.muted, 500, "middle");

layer({
    x: 56,
    y: 118,
    w: 232,
    h: 580,
    title: "Application",
    subtitle: "instrumented code",
    fill: C.grayFill,
    stroke: C.grayStroke
});
layer({
    x: 324,
    y: 118,
    w: 316,
    h: 580,
    title: "bluetape4k helpers",
    subtitle: "Kotlin DSL and lifecycle guards",
    fill: "#f8fafc",
    stroke: "#cbd5e1"
});
layer({
    x: 676,
    y: 118,
    w: 300,
    h: 580,
    title: "OpenTelemetry SDK",
    subtitle: "providers, processors, readers",
    fill: "#fff7ed",
    stroke: "#fed7aa"
});
layer({
    x: 1012,
    y: 118,
    w: 292,
    h: 580,
    title: "Export targets",
    subtitle: "logging, memory, external backends",
    fill: "#f0fdfa",
    stroke: "#99f6e4"
});

card({
    id: "app",
    x: 86,
    y: 214,
    w: 172,
    h: 138,
    title: "Service code",
    subtitle: "manual spans",
    body: ["Tracer.withSpan", "Span.use", "Flow.traced"],
    fill: "#ffffff",
    stroke: C.grayStroke,
});
card({
    id: "webflux",
    x: 86,
    y: 464,
    w: 172,
    h: 118,
    title: "WebFlux app",
    subtitle: "legacy helper",
    body: ["createTracingWebFilter", "registers Reactor hook"],
    fill: C.redFill,
    stroke: C.redStroke,
});
card({
    id: "helpers",
    x: 362,
    y: 184,
    w: 240,
    h: 142,
    title: "Trace lifecycle helpers",
    subtitle: "blocking + suspend",
    body: ["set OK / ERROR", "preserve cancellation", "always end Span"],
    fill: C.blueFill,
    stroke: C.blueStroke,
});
card({
    id: "sdkdsl",
    x: 362,
    y: 374,
    w: 240,
    h: 142,
    title: "SDK builder DSL",
    subtitle: "setup helpers",
    body: ["openTelemetrySdk", "sdkTracerProvider", "sdkMeterProvider"],
    fill: C.greenFill,
    stroke: C.greenStroke,
});
card({
    id: "attrs",
    x: 362,
    y: 566,
    w: 240,
    h: 82,
    title: "Attribute DSL",
    body: ["attributes { } and Map.toAttributes()"],
    fill: C.purpleFill,
    stroke: C.purpleStroke,
});
card({
    id: "trace-sdk",
    x: 714,
    y: 184,
    w: 224,
    h: 142,
    title: "Trace SDK",
    subtitle: "provider + processors",
    body: ["SdkTracerProvider", "SimpleSpanProcessor", "BatchSpanProcessor"],
    fill: C.orangeFill,
    stroke: C.orangeStroke,
});
card({
    id: "metric-sdk",
    x: 714,
    y: 374,
    w: 224,
    h: 142,
    title: "Metric SDK",
    subtitle: "provider + readers",
    body: ["SdkMeterProvider", "PeriodicMetricReader", "InMemoryMetricReader"],
    fill: C.orangeFill,
    stroke: C.orangeStroke,
});
card({
    id: "context",
    x: 714,
    y: 566,
    w: 224,
    h: 82,
    title: "Context bridge",
    body: ["Context.asContextElement for coroutines"],
    fill: C.purpleFill,
    stroke: C.purpleStroke,
});
card({
    id: "trace-export",
    x: 1048,
    y: 184,
    w: 218,
    h: 142,
    title: "Span exporters",
    subtitle: "trace output",
    body: ["LoggingSpanExporter", "SpanExporter.composite", "vararg export helper"],
    fill: "#ffffff",
    stroke: C.tealStroke,
});
card({
    id: "metric-export",
    x: 1048,
    y: 374,
    w: 218,
    h: 142,
    title: "Metric exporters",
    subtitle: "metric output",
    body: ["LoggingMetricExporter", "InMemoryMetricExporter", "reader-driven export"],
    fill: "#ffffff",
    stroke: C.tealStroke,
});
card({
    id: "external",
    x: 1048,
    y: 566,
    w: 218,
    h: 82,
    title: "External backend",
    body: ["OTel collector or vendor exporter", "configured by the application"],
    fill: C.tealFill,
    stroke: C.tealStroke,
});

arrow("app-to-helpers", "M 258 282 H 362", C.blue, "arrow-blue");
label(310, 270, "manual API", C.blue);
arrow("helpers-to-trace", "M 602 255 H 714", C.blue, "arrow-blue");
arrow("sdk-to-trace", "M 602 429 H 658 V 306 H 714", C.green, "arrow-green");
arrow("sdk-to-metric", "M 602 445 H 714", C.green, "arrow-green");
arrow("attrs-to-context", "M 602 607 H 714", C.purple, "arrow-purple");
arrow("trace-to-export", "M 938 255 H 1048", C.orange, "arrow-orange");
arrow("metric-to-export", "M 938 445 H 1048", C.orange, "arrow-orange");
arrow("export-to-external", "M 1157 516 V 566", C.teal, "arrow-teal");
arrow("webflux-to-sdk", "M 258 540 H 676", C.orange, "arrow-orange", 3.0, "7 5");
label(468, 528, "legacy WebFlux", C.orange);

add(`<g transform="translate(84, 746)">`);
text(0, 0, "Legend", 13, C.ink, 700);
add(`<line x1="76" y1="-4" x2="118" y2="-4" stroke="${C.blue}" stroke-width="3.4" marker-end="url(#arrow-blue)"/>`);
text(132, 0, "span lifecycle", 12, C.muted, 600);
add(`<line x1="278" y1="-4" x2="320" y2="-4" stroke="${C.green}" stroke-width="3.4" marker-end="url(#arrow-green)"/>`);
text(334, 0, "SDK setup", 12, C.muted, 600);
add(`<line x1="468" y1="-4" x2="510" y2="-4" stroke="${C.orange}" stroke-width="3.0" stroke-dasharray="7 5" marker-end="url(#arrow-orange)"/>`);
text(524, 0, "legacy/optional instrumentation", 12, C.muted, 600);
add(`</g>`);

add(`</svg>`);

writeFileSync(out, svg.join("\n"));
console.log(out);
