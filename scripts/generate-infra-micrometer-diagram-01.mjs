import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-micrometer-diagram-01.svg";
const W = 1840;
const H = 1550;

const c = {
  ink: "#1F2937",
  muted: "#52616B",
  border: "#D6E2ED",
  blue: "#2563EB",
  violet: "#8B5CF6",
  orange: "#F97316",
  teal: "#0D9488",
  green: "#16A34A",
  slate: "#64748B",
};

const lines = [];
const esc = (s) => s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

function markerDefs() {
  lines.push(`<marker id="hollow-blue" viewBox="0 0 14 12" refX="13" refY="6" markerWidth="14" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 13 6 L 1 11 Z" fill="#FFFFFF" stroke="${c.blue}" stroke-width="2.4" stroke-dasharray="none"/></marker>`);
  lines.push(`<marker id="hollow-orange" viewBox="0 0 14 12" refX="13" refY="6" markerWidth="14" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 13 6 L 1 11 Z" fill="#FFFFFF" stroke="${c.orange}" stroke-width="2.4" stroke-dasharray="none"/></marker>`);
  lines.push(`<marker id="hollow-teal" viewBox="0 0 14 12" refX="13" refY="6" markerWidth="14" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 13 6 L 1 11 Z" fill="#FFFFFF" stroke="${c.teal}" stroke-width="2.4" stroke-dasharray="none"/></marker>`);
  lines.push(`<marker id="open-blue" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="10" markerHeight="10" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="${c.blue}" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>`);
  lines.push(`<marker id="open-green" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="10" markerHeight="10" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="${c.green}" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>`);
  lines.push(`<marker id="open-violet" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="10" markerHeight="10" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="${c.violet}" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>`);
}

function band(x, y, w, h, title, sub, color) {
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="16" fill="#FFFFFF" stroke="${c.border}" stroke-width="2.2"/>`);
  lines.push(`<text x="${x + 28}" y="${y + 42}" class="bandTitle">${esc(title)}</text>`);
  lines.push(`<text x="${x + 28}" y="${y + 70}" class="bandSub">${esc(sub)}</text>`);
  lines.push(`<line x1="${x + 24}" y1="${y + 88}" x2="${x + w - 24}" y2="${y + 88}" stroke="${color}" stroke-width="2" stroke-dasharray="9 8" opacity=".45"/>`);
}

function card(x, y, w, h, title, rows, fill, stroke, opts = {}) {
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" stroke-width="2.3"/>`);
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="48" rx="10" fill="${opts.header ?? fill}" opacity=".68"/>`);
  lines.push(`<text x="${x + w / 2}" y="${y + 32}" text-anchor="middle" class="cardTitle">${esc(title)}</text>`);
  rows.forEach((row, i) => {
    lines.push(`<text x="${x + 22}" y="${y + 74 + i * 25}" class="row">${esc(row)}</text>`);
  });
}

function pill(x, y, w, text, color) {
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="46" rx="23" fill="#FFFFFF" stroke="${color}" stroke-width="2" stroke-dasharray="9 7"/>`);
  lines.push(`<text x="${x + w / 2}" y="${y + 30}" text-anchor="middle" class="pillText">${esc(text)}</text>`);
}

function path(d, color, marker, dash = false, width = 3) {
  lines.push(`<path d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="10 8"` : ""} marker-end="url(#${marker})"/>`);
}

lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
lines.push(`<title id="title">micrometer core class structure</title>`);
lines.push(`<desc id="desc">Core bluetape4k-micrometer classes: Kotlin extension surfaces, Retrofit2 call adapter measurement chain, Cache2k binder, and key-value helpers.</desc>`);
lines.push(`<defs><style>
  .title{font-family:"Architects Daughter";font-size:46px;fill:${c.ink}}
  .subtitle,.bandSub,.row,.pillText,.legend{font-family:"Comic Mono";fill:${c.muted}}
  .subtitle{font-size:18px}.bandTitle{font-family:"Architects Daughter";font-size:28px;fill:${c.ink}}
  .bandSub{font-size:14px}.cardTitle{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}
  .row{font-size:14px}.pillText{font-size:15px}.legend{font-size:13px}
</style>`);
markerDefs();
lines.push(`</defs>`);
lines.push(`<rect x="36" y="30" width="${W - 72}" height="${H - 60}" rx="22" fill="#FEFEFC" stroke="#CBD8E6" stroke-width="2.4"/>`);
lines.push(`<text x="82" y="92" class="title">Micrometer Core Class Structure</text>`);
lines.push(`<text x="86" y="128" class="subtitle">Kotlin extensions wrap Micrometer APIs; Retrofit2 and Cache2k use concrete adapter and binder classes.</text>`);

lines.push(`<rect x="82" y="174" width="${W - 164}" height="124" rx="14" fill="#FFFFFF" stroke="${c.border}" stroke-width="2"/>`);
lines.push(`<text x="112" y="220" class="bandTitle">External APIs extended by this module</text>`);
pill(150, 242, 270, "Timer + Flow", c.blue);
pill(520, 242, 340, "Observation + Registry", c.violet);
pill(970, 242, 330, "Retrofit CallAdapter + Call", c.orange);
pill(1385, 242, 300, "CacheMeterBinder", c.teal);

band(82, 342, 515, 900, "Kotlin extension surfaces", "top-level extension functions and event data contracts", c.violet);
card(122, 454, 205, 112, "TimerExtensions", ["Timer.recordSuspend", "Flow.withTimer"], "#EFF6FF", c.blue);
card(352, 454, 205, 112, "RegistrySupport", ["observationRegistryOf", "start / createNotStarted"], "#F5F3FF", c.violet);
card(122, 604, 205, 112, "Observation Ext", ["withObservation", "tryObserve"], "#F5F3FF", c.violet);
card(352, 604, 205, 112, "CoroutineSupport", ["observeSuspending", "context propagation"], "#F5F3FF", c.violet);
card(122, 800, 435, 130, "EventTelemetry Support", ["observeEventPublish / observeEventConsume", "publish/consume outcomes", "cancellation rethrow: outcome=CANCELLED"], "#FDF2F8", "#DB2777");
card(122, 970, 435, 88, "EventTelemetry data model", ["Destination, Correlation, HighCardinality, Outcome"], "#FDF2F8", "#DB2777");

band(650, 342, 715, 900, "Retrofit2 metrics class cluster", "CallAdapter.Factory creates wrappers; collector builds stable tag sets", c.orange);
card(835, 420, 280, 112, "CallAdapter.Factory", ["external Retrofit base class", "get(returnType, annotations)"], "#FFF7ED", c.orange);
card(702, 590, 282, 120, "RetrofitMetricsFactory", ["extends CallAdapter.Factory", "builds collector per endpoint"], "#FFF7ED", c.orange);
card(1040, 590, 282, 120, "MeasuredCallAdapter", ["implements CallAdapter", "wraps returned Call"], "#FFF7ED", c.orange);
card(670, 760, 330, 96, "Micrometer Retrofit Factory", ["extends RetrofitMetricsFactory", "supplies Micrometer recorder"], "#FFF7ED", c.orange);
card(1040, 760, 282, 120, "MeasuredCall<T>", ["implements Call<T>", "records execute/enqueue"], "#FFF7ED", c.orange);
card(710, 930, 310, 120, "Retrofit Metrics Collector", ["method, status, outcome tags", "duration / exception paths"], "#FFF7ED", c.orange);
card(1080, 930, 220, 120, "Outcome", ["SUCCESS / CLIENT/SERVER", "UNKNOWN"], "#FFF7ED", c.orange);
card(702, 1145, 282, 84, "MetricsRecorder", ["<<fun interface>> recordTiming(...)"], "#FFFFFF", c.slate);
card(1040, 1145, 282, 84, "Micrometer Recorder", ["Timer cache + percentiles"], "#FFF7ED", c.orange);

band(1415, 342, 343, 900, "Cache and tag helpers", "Cache2k binder plus KeyValue validation utilities", c.teal);
card(1452, 520, 268, 130, "Cache2kCacheMetrics", ["extends CacheMeterBinder", "size, hits, misses", "load and expiry meters"], "#ECFDF5", c.teal);
card(1452, 750, 268, 112, "KeyValueSupport", ["keyValueOf / keyValuesOf", "blank-key validation"], "#F0FDF4", c.green);
card(1452, 970, 268, 112, "MeterRegistry", ["Timer, Gauge", "FunctionCounter"], "#ECFEFF", c.teal);

path("M 842 590 V 544", c.orange, "hollow-orange", false, 2.8);
path("M 835 760 V 710", c.orange, "hollow-orange", false, 2.8);
path("M 1220 590 V 546 H 980 V 532", c.orange, "open-blue", true, 2.8);
path("M 1181 710 V 760", c.blue, "open-blue", true, 2.8);
path("M 1181 880 V 930", c.blue, "open-blue", true, 2.8);
path("M 1020 990 H 1080", c.orange, "open-blue", true, 2.8);
path("M 865 1050 V 1145", c.blue, "open-blue", true, 2.8);
path("M 1040 1187 H 984", c.slate, "open-blue", true, 2.6);
path("M 1181 1145 V 1072 H 865", c.orange, "open-blue", false, 2.8);
path("M 1181 1229 V 1282 H 1586 V 1082", c.green, "open-green", true, 2.8);
path("M 1586 650 V 750", c.green, "open-green", true, 2.8);
path("M 1586 862 V 970", c.green, "open-green", true, 2.8);
path("M 1720 520 V 288", c.teal, "hollow-teal", false, 2.8);
path("M 455 716 V 800", c.violet, "open-violet", true, 2.8);
path("M 340 930 V 970", "#DB2777", "open-violet", true, 2.8);

lines.push(`<rect x="108" y="1390" width="1160" height="54" rx="12" fill="#FFFFFF" stroke="${c.border}" stroke-width="1.8"/>`);
lines.push(`<text x="134" y="1423" class="legend">solid hollow triangle = extends</text>`);
lines.push(`<text x="370" y="1423" class="legend">dashed hollow/open arrows = implements or uses</text>`);
lines.push(`<text x="710" y="1423" class="legend">Source-backed grouping: extensions, Retrofit measurement chain, Cache2k binder, tag helpers</text>`);
lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
