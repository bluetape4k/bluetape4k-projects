#!/usr/bin/env node
import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";

const out = "docs/images/readme-diagrams/infra-kafka-logback-diagram-01";
const W = 1880;
const H = 1080;
const c = {
    ink: "#0F172A",
    muted: "#475569",
    canvas: "#F8FAFC",
    frame: "#FFFFFF",
    line: "#CBD5E1",
    logback: "#64748B",
    appender: "#2563EB",
    key: "#EA580C",
    exporter: "#9333EA",
    handler: "#0D9488",
    kafka: "#16A34A",
    slate: "#334155",
};

const sources = [
    "infra/kafka-logback/README.md",
    "infra/kafka-logback/README.ko.md",
    "infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/AbstractKafkaAppender.kt",
    "infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/KafkaAppender.kt",
    "infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/KafkaExporter.kt",
    "infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/DefaultKafkaExporter.kt",
    "infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/ExportExceptionHandler.kt",
    "infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/keyprovider/KafkaKeyProvider.kt",
    "infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/keyprovider/AbstractKafkaKeyProvider.kt",
];
for (const source of sources) {
    if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
}
if (!/Kafka Logback Class Structure[\s\S]*infra-kafka-logback-diagram-01\.png/.test(readFileSync(sources[0], "utf8"))) {
    throw new Error("README diagram slot not found");
}

const boxes = {
    logbackBase: {
        x: 80,
        y: 155,
        w: 360,
        h: 160,
        fill: "#F8FAFC",
        stroke: c.logback,
        stereo: "Logback APIs",
        title: "Appender Infrastructure",
        members: ["UnsynchronizedAppenderBase", "AppenderAttachable<E>", "Encoder<E>"],
    },
    abstractAppender: {
        x: 545,
        y: 135,
        w: 430,
        h: 200,
        fill: "#EFF6FF",
        stroke: c.appender,
        stereo: "abstract class",
        title: "AbstractKafkaAppender<E>",
        members: ["bootstrapServers, topic, acks", "keyProvider: KafkaKeyProvider", "exporter: KafkaExporter", "encoder: Encoder<E>"],
    },
    kafkaAppender: {
        x: 545,
        y: 425,
        w: 430,
        h: 210,
        fill: "#EFF6FF",
        stroke: c.appender,
        stereo: "class",
        title: "KafkaAppender<E>",
        members: ["defer Kafka client logs", "encode event -> ProducerRecord", "export(..., exceptionHandler)", "flush/close producer on stop"],
    },
    producer: {
        x: 545,
        y: 795,
        w: 430,
        h: 160,
        fill: "#F0FDF4",
        stroke: c.kafka,
        stereo: "Kafka client",
        title: "KafkaProducer",
        members: ["Producer<ByteArray, ByteArray>", "send(record, callback)", "flush() / close()"],
    },
    keyApi: {
        x: 80,
        y: 410,
        w: 360,
        h: 150,
        fill: "#FFF7ED",
        stroke: c.key,
        stereo: "interface",
        title: "KafkaKeyProvider<E>",
        members: ["+ get(event): ByteArray?", "null means unkeyed record"],
    },
    keyBase: {
        x: 80,
        y: 610,
        w: 360,
        h: 170,
        fill: "#FFF7ED",
        stroke: c.key,
        stereo: "abstract class",
        title: "AbstractKafkaKeyProvider",
        members: ["ContextAwareBase + LifeCycle", "resets errorWasShown", "base for context-aware keys"],
    },
    keyImpls: {
        x: 80,
        y: 830,
        w: 360,
        h: 170,
        fill: "#FFF7ED",
        stroke: c.key,
        stereo: "classes",
        title: "Key Provider Implementations",
        members: ["Hostname / Logger / Thread", "ContextName extend base", "Null implements interface"],
    },
    exporterApi: {
        x: 1090,
        y: 155,
        w: 360,
        h: 150,
        fill: "#FAF5FF",
        stroke: c.exporter,
        stereo: "interface",
        title: "KafkaExporter",
        members: ["+ export(producer, record, event)", "returns immediate success flag"],
    },
    defaultExporter: {
        x: 1090,
        y: 425,
        w: 360,
        h: 166,
        fill: "#FAF5FF",
        stroke: c.exporter,
        stereo: "class",
        title: "DefaultKafkaExporter",
        members: ["producer.send(record)", "callback delegates failures", "buffer/timeout -> false"],
    },
    handlerApi: {
        x: 1485,
        y: 395,
        w: 260,
        h: 142,
        fill: "#ECFDF5",
        stroke: c.handler,
        stereo: "fun interface",
        title: "ExportExceptionHandler",
        members: ["+ handle(event, error)", "custom fallback contract"],
    },
    appenderHandler: {
        x: 1485,
        y: 605,
        w: 260,
        h: 144,
        fill: "#ECFDF5",
        stroke: c.handler,
        stereo: "KafkaAppender owned",
        title: "Fallback Handler",
        members: ["warn on exception", "append to fallback appenders"],
    },
    fallback: {
        x: 1485, y: 830, w: 260, h: 150, fill: "#F8FAFC", stroke: c.logback,
        stereo: "Logback", title: "Attached Appenders", members: ["appendLoopOnAppenders", "fallback on export error"],
    },
};

const edges = [
    {cls: "extends", d: "M 545 225 H 440", color: c.logback, marker: "hollowSlate", label: null},
    {cls: "extends", d: "M 760 425 V 335", color: c.appender, marker: "hollowBlue", label: null},
    {
        cls: "assoc",
        d: "M 545 286 H 500 V 475 H 440",
        color: c.key,
        marker: "openOrange",
        label: ["keyProvider", 492, 450]
    },
    {cls: "assoc", d: "M 975 260 H 1090", color: c.exporter, marker: "openPurple", label: ["exporter", 1032, 248]},
    {cls: "realize", d: "M 260 610 V 560", color: c.key, marker: "hollowOrange", label: null},
    {cls: "extends", d: "M 260 830 V 780", color: c.key, marker: "hollowOrange", label: null},
    {cls: "realize", d: "M 1270 425 V 305", color: c.exporter, marker: "hollowPurple", label: null},
    {cls: "assoc", d: "M 975 522 H 1090", color: c.exporter, marker: "openPurple", label: ["exports", 1032, 510]},
    {cls: "assoc", d: "M 760 635 V 795", color: c.kafka, marker: "openGreen", label: ["owns producer", 820, 718]},
    {cls: "uses", d: "M 1450 508 H 1485", color: c.handler, marker: "openTeal", label: null},
    {cls: "realize", d: "M 1615 605 V 537", color: c.handler, marker: "hollowTeal", label: null},
    {cls: "assoc", d: "M 1615 749 V 830", color: c.handler, marker: "openTeal", label: ["fallback", 1660, 795]},
    {
        cls: "uses",
        d: "M 1090 575 H 1015 V 855 H 975",
        color: c.kafka,
        marker: "openGreen",
        label: ["send()", 1036, 625]
    },
];

function esc(value) {
    return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function box(id) {
    const b = boxes[id];
    const dividerY = b.y + 76;
    const memberStart = dividerY + 27;
    return `<g id="${id}">
  <rect class="card" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/>
  <text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 28}" text-anchor="middle">${esc(b.stereo)}</text>
  <text class="cardTitle" x="${b.x + b.w / 2}" y="${b.y + 58}" text-anchor="middle">${esc(b.title)}</text>
  <line class="divider" x1="${b.x + 24}" y1="${dividerY}" x2="${b.x + b.w - 24}" y2="${dividerY}"/>
  ${b.members.map((member, index) => `<text class="member" x="${b.x + 28}" y="${memberStart + index * 22}">${esc(member)}</text>`).join("")}
</g>`;
}

function label(item) {
    if (!item) return "";
    const [text, x, y] = item;
    const width = Math.max(58, text.length * 7.2 + 18);
    return `<g class="edgeLabel"><rect x="${x - width / 2}" y="${y - 15}" width="${width}" height="22" rx="6"/><text class="label" x="${x}" y="${y}" text-anchor="middle">${esc(text)}</text></g>`;
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Kafka Logback class structure">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="118%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity=".11"/></filter>
  <marker id="openBlue" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.appender}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openOrange" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.key}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openPurple" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.exporter}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openTeal" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.handler}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openGreen" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.kafka}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="hollowBlue" viewBox="0 0 18 16" markerWidth="16" markerHeight="14" refX="16" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 16 8 L 1 15 Z" fill="${c.frame}" stroke="${c.appender}" stroke-width="2" stroke-dasharray="none"/></marker>
  <marker id="hollowOrange" viewBox="0 0 18 16" markerWidth="16" markerHeight="14" refX="16" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 16 8 L 1 15 Z" fill="${c.frame}" stroke="${c.key}" stroke-width="2" stroke-dasharray="none"/></marker>
  <marker id="hollowPurple" viewBox="0 0 18 16" markerWidth="16" markerHeight="14" refX="16" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 16 8 L 1 15 Z" fill="${c.frame}" stroke="${c.exporter}" stroke-width="2" stroke-dasharray="none"/></marker>
  <marker id="hollowTeal" viewBox="0 0 18 16" markerWidth="16" markerHeight="14" refX="16" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 16 8 L 1 15 Z" fill="${c.frame}" stroke="${c.handler}" stroke-width="2" stroke-dasharray="none"/></marker>
  <marker id="hollowSlate" viewBox="0 0 18 16" markerWidth="16" markerHeight="14" refX="16" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 16 8 L 1 15 Z" fill="${c.frame}" stroke="${c.logback}" stroke-width="2" stroke-dasharray="none"/></marker>
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.6;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:${c.ink}}.subtitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}
    .card{filter:url(#shadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:21px;fill:${c.ink}}
    .stereo{font-family:"Comic Mono";font-size:12px;fill:#64748B}.member{font-family:"Comic Mono";font-size:12.3px;fill:${c.slate}}
    .divider{stroke:rgba(15,23,42,.17);stroke-width:1.2}.assoc{fill:none;stroke-width:3.0;stroke-linecap:round;stroke-linejoin:round}
    .uses,.realize{fill:none;stroke-width:2.5;stroke-dasharray:8 7;stroke-linecap:round;stroke-linejoin:round}
    .extends{fill:none;stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round}
    .edgeLabel rect{fill:${c.frame};stroke:${c.line};stroke-width:1;opacity:.96}
    .label{font-family:"Comic Mono";font-size:11.6px;fill:${c.ink};stroke:none}
    .legend{font-family:"Comic Mono";font-size:12px;fill:${c.muted}}
  </style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="10"/>
<text class="title" x="72" y="84">Kafka Logback Class Structure</text>
<text class="subtitle" x="76" y="116">KafkaAppender owns Logback integration while key selection, export, and fallback behavior stay pluggable.</text>
<g>
${edges.map((e) => `  <path class="${e.cls}" d="${e.d}" stroke="${e.color}" marker-end="url(#${e.marker})"/>`).join("\n")}
</g>
<g>${edges.map((e) => label(e.label)).join("")}</g>
${Object.keys(boxes).map(box).join("")}
<g transform="translate(80 1028)">
  <path class="extends" d="M 0 0 H 58" stroke="${c.appender}" marker-end="url(#hollowBlue)"/><text class="legend" x="72" y="5">extends</text>
  <path class="realize" d="M 170 0 H 228" stroke="${c.exporter}" marker-end="url(#hollowPurple)"/><text class="legend" x="242" y="5">implements</text>
  <path class="assoc" d="M 385 0 H 443" stroke="${c.kafka}" marker-end="url(#openGreen)"/><text class="legend" x="457" y="5">has / uses</text>
</g>
</svg>`;

writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
