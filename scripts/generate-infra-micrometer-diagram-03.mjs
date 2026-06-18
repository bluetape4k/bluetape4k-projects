import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-micrometer-diagram-03.svg";
const W = 1720;
const H = 980;

const c = {
  ink: "#1F2937",
  muted: "#52616B",
  border: "#D6E2ED",
  blue: "#2563EB",
  violet: "#8B5CF6",
  orange: "#EA580C",
  teal: "#0D9488",
  green: "#16A34A",
  pink: "#DB2777",
};

const lines = [];
const esc = (s) => s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

function marker(id, color) {
  lines.push(`<marker id="${id}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="13" markerHeight="13" orient="auto" markerUnits="userSpaceOnUse"><path d="M 0 0 L 10 5 L 0 10 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`);
}

function card(x, y, w, h, title, sub, fill, stroke) {
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" fill="${fill}" stroke="${stroke}" stroke-width="2.4"/>`);
  lines.push(`<text x="${x + w / 2}" y="${y + 38}" text-anchor="middle" class="cardTitle">${esc(title)}</text>`);
  lines.push(`<text x="${x + w / 2}" y="${y + 66}" text-anchor="middle" class="sub">${esc(sub)}</text>`);
}

function group(x, y, w, h, title, color) {
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="16" fill="#FFFFFF" stroke="${c.border}" stroke-width="2.2"/>`);
  lines.push(`<text x="${x + 24}" y="${y + 38}" class="groupTitle">${esc(title)}</text>`);
  lines.push(`<line x1="${x + 22}" y1="${y + 58}" x2="${x + w - 22}" y2="${y + 58}" stroke="${color}" stroke-width="2" stroke-dasharray="9 8" opacity=".5"/>`);
}

function path(d, color, markerId, dash = false) {
  lines.push(`<path d="${d}" fill="none" stroke="${color}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="11 9"` : ""} marker-end="url(#${markerId})"/>`);
}

lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
lines.push(`<title id="title">micrometer instrumentation component map</title>`);
lines.push(`<desc id="desc">Application work enters timer, observation, event, Retrofit, or Cache2k helpers; helpers build bounded tags or observation context and hand them to MeterRegistry or ObservationRegistry before export.</desc>`);
lines.push(`<defs><style>
  .title{font-family:"Architects Daughter";font-size:46px;fill:${c.ink}}
  .subtitle,.sub,.legend{font-family:"Comic Mono";fill:${c.muted}}
  .subtitle{font-size:18px}.groupTitle{font-family:"Architects Daughter";font-size:27px;fill:${c.ink}}
  .cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${c.ink}}
  .sub{font-size:14px}.legend{font-size:13px}
</style>`);
marker("arrow-blue", c.blue);
marker("arrow-violet", c.violet);
marker("arrow-orange", c.orange);
marker("arrow-teal", c.teal);
marker("arrow-green", c.green);
marker("arrow-pink", c.pink);
lines.push(`</defs>`);
lines.push(`<rect x="34" y="30" width="${W - 68}" height="${H - 60}" rx="22" fill="#FEFEFC" stroke="#CBD8E6" stroke-width="2.4"/>`);
lines.push(`<text x="82" y="90" class="title">Micrometer Instrumentation Component Map</text>`);
lines.push(`<text x="86" y="126" class="subtitle">Instrumentation helpers build bounded measurements, then cross an explicit MeterRegistry or ObservationRegistry boundary.</text>`);

group(82, 170, 300, 650, "Application work", c.blue);
card(118, 285, 228, 104, "Suspend / Flow", "recordSuspend, withTimer", "#EFF6FF", c.blue);
card(118, 438, 228, 104, "Operation block", "Observation scope", "#F5F3FF", c.violet);
card(118, 592, 228, 104, "Framework call", "Retrofit call or cache", "#FFF7ED", c.orange);

group(440, 170, 430, 650, "Module entry helpers", c.green);
card(486, 242, 338, 102, "TimerExtensions", "duration around suspend/Flow", "#EFF6FF", c.blue);
card(486, 382, 338, 102, "Observation wrappers", "start, scope, error, stop", "#F5F3FF", c.violet);
card(486, 522, 338, 102, "Event telemetry", "publish/consume contract", "#FDF2F8", c.pink);
card(486, 662, 338, 102, "Retrofit + Cache2k", "HTTP calls and cache stats", "#FFF7ED", c.orange);

group(928, 170, 350, 650, "Bounded metadata", c.teal);
card(970, 242, 266, 102, "Duration", "nanoTime or stopwatch", "#EFF6FF", c.blue);
card(970, 382, 266, 102, "Observation context", "reactor/coroutine propagation", "#F5F3FF", c.violet);
card(970, 522, 266, 102, "Safe event tags", "destination, outcome, ids", "#FDF2F8", c.pink);
card(970, 662, 266, 102, "HTTP/cache tags", "method, status, cache name", "#ECFDF5", c.teal);

group(1336, 170, 302, 650, "Registry boundary", c.green);
card(1376, 285, 222, 106, "MeterRegistry", "Timer, Gauge, counters", "#ECFEFF", c.teal);
card(1376, 470, 222, 106, "ObservationRegistry", "handlers observe context", "#F5F3FF", c.violet);
card(1376, 655, 222, 106, "Exported signals", "metrics and trace spans", "#F0FDF4", c.green);

path("M 346 337 H 486", c.blue, "arrow-blue");
path("M 824 293 H 970", c.blue, "arrow-blue");
path("M 1236 293 H 1376", c.blue, "arrow-blue");
path("M 346 490 H 486", c.violet, "arrow-violet");
path("M 824 433 H 970", c.violet, "arrow-violet");
path("M 1236 433 H 1356 V 523 H 1376", c.violet, "arrow-violet");
path("M 346 644 H 486", c.orange, "arrow-orange");
path("M 824 573 H 970", c.pink, "arrow-pink");
path("M 1236 573 H 1356 V 523 H 1376", c.pink, "arrow-pink", true);
path("M 824 713 H 970", c.teal, "arrow-teal");
path("M 1236 713 H 1328 V 338 H 1376", c.teal, "arrow-teal");
path("M 1598 338 H 1630 V 708 H 1598", c.green, "arrow-green");
path("M 1487 576 V 655", c.green, "arrow-green");

lines.push(`<rect x="120" y="865" width="750" height="50" rx="12" fill="#FFFFFF" stroke="${c.border}" stroke-width="1.6"/>`);
lines.push(`<text x="146" y="896" class="legend">Solid paths carry metric timing/tag data; dashed paths carry observation/event context.</text>`);
lines.push(`<text x="950" y="896" class="legend">No payloads, raw headers, secrets, or unbounded destination names cross into low-cardinality metrics.</text>`);
lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
