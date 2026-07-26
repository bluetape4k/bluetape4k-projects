#!/usr/bin/env node
import {writeFileSync} from "node:fs";

const out = "docs/images/readme-diagrams/infra-opentelemetry-sequence-01.svg";
const W = 1680;
const H = 1380;
const c = {
    bg: "#F8FAFC",
    ink: "#1F2937",
    muted: "#52616B",
    border: "#D6E2ED",
    life: "#A8B8C8",
    call: "#4F83BF",
    trace: "#2F9E6B",
    context: "#D08A2D",
    work: "#C15A7A",
    result: "#2E9C9B",
    error: "#C15A5A",
};

const participants = [
    {id: "caller", x: 100, w: 190, title: "Caller", role: "suspend service", fill: "#DBEAFE", stroke: "#3B82F6"},
    {id: "tracer", x: 360, w: 190, title: "Tracer", role: "withSpan DSL", fill: "#DCFCE7", stroke: "#22C55E"},
    {id: "builder", x: 620, w: 210, title: "SpanBuilder", role: "configured span", fill: "#FEF3C7", stroke: "#D97706"},
    {id: "span", x: 900, w: 190, title: "Span", role: "status + end", fill: "#CCFBF1", stroke: "#14B8A6"},
    {
        id: "context",
        x: 1160,
        w: 210,
        title: "OTel Context",
        role: "coroutine element",
        fill: "#F3E8FF",
        stroke: "#9333EA"
    },
    {id: "work", x: 1440, w: 190, title: "Coroutine Work", role: "user block", fill: "#FCE7F3", stroke: "#DB2777"},
];

const xs = Object.fromEntries(participants.map((p) => [p.id, p.x + p.w / 2]));
const lines = [];
const esc = (s) => s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
const approx = (s) => Math.max(150, s.length * 8.2 + 56);
const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));

function marker(id, color) {
    return `<marker id="${id}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse"><path d="M 0 0 L 10 5 L 0 10 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`;
}

function pill(n, text, color, x, y, w = approx(text)) {
    const px = clamp(x, 56, W - w - 56);
    lines.push(`<rect x="${px}" y="${y - 17}" width="${w}" height="34" rx="17" class="pill" stroke="${color}"/>`);
    lines.push(`<circle cx="${px + 24}" cy="${y}" r="13" fill="${color}"/>`);
    lines.push(`<text x="${px + 24}" y="${y + 4}" text-anchor="middle" class="badge">${n}</text>`);
    lines.push(`<text x="${px + 48}" y="${y + 5}" class="msg" fill="${color}">${esc(text)}</text>`);
}

function participant(p) {
    lines.push(`<rect x="${p.x}" y="160" width="${p.w}" height="76" rx="12" fill="${p.fill}" stroke="${p.stroke}" stroke-width="2.4"/>`);
    lines.push(`<text x="${p.x + p.w / 2}" y="190" text-anchor="middle" class="participant">${esc(p.title)}</text>`);
    lines.push(`<text x="${p.x + p.w / 2}" y="214" text-anchor="middle" class="role">${esc(p.role)}</text>`);
    lines.push(`<line x1="${p.x + p.w / 2}" y1="236" x2="${p.x + p.w / 2}" y2="1260" class="lifeline"/>`);
}

function msg(n, from, to, y, text, color, dashed = false, labelX = null, w = null) {
    const a = xs[from];
    const b = xs[to];
    const start = a < b ? a + 11 : a - 11;
    const end = a < b ? b - 11 : b + 11;
    const markerId = `arrow${n}`;
    lines.push(marker(markerId, color));
    const dash = dashed ? ` stroke-dasharray="10 8"` : "";
    lines.push(`<path d="M ${start} ${y} L ${end} ${y}" fill="none" stroke="${color}" stroke-width="${dashed ? 2.8 : 3.2}"${dash} marker-end="url(#${markerId})"/>`);
    const tw = w ?? approx(text);
    const lx = labelX ?? (Math.min(start, end) + Math.abs(end - start) / 2 - tw / 2);
    pill(n, text, color, lx, y - 34, tw);
}

function selfMsg(n, id, y, text, color, labelX = null, w = null) {
    const x = xs[id] + 14;
    const markerId = `arrowSelf${n}`;
    lines.push(marker(markerId, color));
    lines.push(`<path d="M ${x} ${y} L ${x + 82} ${y} L ${x + 82} ${y + 38} L ${x + 8} ${y + 38}" fill="none" stroke="${color}" stroke-width="3.0" marker-end="url(#${markerId})"/>`);
    pill(n, text, color, labelX ?? x + 42, y - 30, w ?? approx(text));
}

lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
lines.push(`<title id="title">span lifecycle in coroutine context</title>`);
lines.push(`<desc id="desc">Tracer.withSpan creates a SpanBuilder, starts one Span, installs it into the coroutine OpenTelemetry context, runs work, records completion or failure, and always ends the Span.</desc>`);
lines.push(`<defs><style>
  .title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}
  .subtitle,.msg,.role,.note,.footer{font-family:"Comic Mono";fill:${c.muted}}
  .subtitle{font-size:18px}.participant{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}
  .role{font-size:13px}.msg{font-size:14px}.note{font-size:13px}.footer{font-size:14px;fill:#60727d}
  .frame{fill:#FEFEFC;stroke:#546E7A;stroke-width:2.5}.inner{fill:#FFFFFF;stroke:${c.border};stroke-width:2}
  .lifeline{stroke:${c.life};stroke-width:2;stroke-dasharray:8 8}
  .activation{rx:6;stroke-width:1.6}.pill{fill:#FFFFFF;stroke-width:1.5}.badge{font-family:"Comic Mono";font-size:12px;font-weight:700;fill:#FFFFFF}
  .alt{fill:#FFFFFF;fill-opacity:.12;stroke:#78909C;stroke-width:2.6;stroke-dasharray:12 8}
</style></defs>`);
lines.push(`<rect x="24" y="24" width="${W - 48}" height="${H - 48}" rx="22" class="frame"/>`);
lines.push(`<text x="${W / 2}" y="78" text-anchor="middle" class="title">Span Lifecycle in a Coroutine Context</text>`);
lines.push(`<text x="${W / 2}" y="110" text-anchor="middle" class="subtitle">One Span is installed into coroutine OTel context, then ended after normal, cancelled, or failed work.</text>`);
lines.push(`<rect x="58" y="140" width="${W - 116}" height="1168" rx="18" class="inner"/>`);
participants.forEach(participant);

lines.push(`<rect x="${xs.tracer - 8}" y="300" width="16" height="862" rx="6" fill="#DCFCE7" stroke="#22C55E" class="activation"/>`);
lines.push(`<rect x="${xs.builder - 8}" y="382" width="16" height="125" rx="6" fill="#FEF3C7" stroke="#D97706" class="activation"/>`);
lines.push(`<rect x="${xs.span - 8}" y="502" width="16" height="660" rx="6" fill="#CCFBF1" stroke="#14B8A6" class="activation"/>`);
lines.push(`<rect x="${xs.context - 8}" y="640" width="16" height="260" rx="6" fill="#F3E8FF" stroke="#9333EA" class="activation"/>`);
lines.push(`<rect x="${xs.work - 8}" y="720" width="16" height="130" rx="6" fill="#FCE7F3" stroke="#DB2777" class="activation"/>`);

msg(1, "caller", "tracer", 320, 'withSpan("operation") { ... }', c.call, false, 190, 300);
msg(2, "tracer", "builder", 410, "spanBuilder(name).apply(configure)", c.trace, false, 465, 330);
msg(3, "builder", "span", 520, "startSpan()", c.context, false, 700, 180);
msg(4, "span", "context", 650, "storeInContext + asContextElement", c.context, false, 945, 340);
msg(5, "context", "work", 730, "withContext(...)", c.work, false, 1205, 210);
msg(6, "work", "span", 850, "block returns value", c.result, true, 1120, 220);

lines.push(`<rect x="96" y="930" width="${W - 192}" height="232" rx="15" class="alt"/>`);
lines.push(`<rect x="118" y="954" width="240" height="30" rx="15" fill="#F0FDF4" stroke="${c.trace}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="974" class="note">normal completion</text>`);
lines.push(`<line x1="96" y1="1016" x2="${W - 96}" y2="1016" stroke="#78909C" stroke-width="2.2" stroke-dasharray="12 8"/>`);
lines.push(`<rect x="118" y="1038" width="260" height="30" rx="15" fill="#F8FAFC" stroke="${c.muted}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="1058" class="note">cancellation</text>`);
lines.push(`<line x1="96" y1="1092" x2="${W - 96}" y2="1092" stroke="#78909C" stroke-width="2.2" stroke-dasharray="12 8"/>`);
lines.push(`<rect x="118" y="1114" width="250" height="30" rx="15" fill="#FEF2F2" stroke="${c.error}" stroke-width="1.6"/>`);
lines.push(`<text x="140" y="1134" class="note">other Throwable</text>`);

selfMsg(7, "span", 965, "setStatus(OK)", c.trace, 1040, 185);
selfMsg(8, "span", 1048, "leave status UNSET", c.muted, 1040, 230);
selfMsg(9, "span", 1128, "recordException + ERROR", c.error, 1015, 265);
msg(10, "span", "caller", 1210, "end() in finally, then return or rethrow", c.result, true, 560, 360);

lines.push(`<rect x="96" y="1252" width="${W - 192}" height="48" rx="12" fill="#FFFFFF" stroke="${c.border}" stroke-width="1.6"/>`);
lines.push(`<text x="${W / 2}" y="1282" text-anchor="middle" class="footer">waitTimeout overloads stay for compatibility; the current implementation ends the Span immediately.</text>`);
lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
