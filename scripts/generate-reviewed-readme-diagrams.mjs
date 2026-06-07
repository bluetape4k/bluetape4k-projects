#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, writeFileSync } from "node:fs";
import { dirname } from "node:path";

const diagramDir = "docs/images/readme-diagrams";
const dot = "/opt/homebrew/bin/dot";
const rsvgConvert = "/opt/homebrew/bin/rsvg-convert";
const minTitleGapPx = 38;

const palette = {
  blue: { fill: "#E8F3FF", stroke: "#75A9E8", line: "#4F83BF" },
  green: { fill: "#EAF7EF", stroke: "#69B888", line: "#58A978" },
  teal: { fill: "#E9F7F6", stroke: "#45A7A1", line: "#45A7A1" },
  amber: { fill: "#FFF3D9", stroke: "#D9AA4D", line: "#D9AA4D" },
  pink: { fill: "#FCE7F3", stroke: "#DB7890", line: "#DB7890" },
  purple: { fill: "#F1ECFF", stroke: "#8A72D6", line: "#8A72D6" },
  olive: { fill: "#EEF6D9", stroke: "#8BA84D", line: "#8BA84D" },
  gray: { fill: "#F6F8FA", stroke: "#AAB7C4", line: "#6B7D90" },
};

const diagrams = [
  {
    file: "bluetape4k-bom-diagram-01",
    title: "BOM dependency alignment map",
    subtitle: "Consumer projects import one platform while the BOM groups internal modules and third-party dependency families.",
    desc: "Dependency-oriented BOM diagram showing the consumer import path, the bluetape4k dependency catalog, module families, and external dependency groups.",
    width: 1500,
    height: 990,
    groups: [
      group("alignment", "Alignment surface", 420, 145, 1010, 142),
      group("modules", "Internal module families", 74, 338, 1352, 238),
      group("externals", "Governed external families", 74, 636, 1352, 142),
    ],
    nodes: [
      card("consumer", "Consumer Gradle project", ["imports platform", "no module version pins"], "blue", 96, 176, 260, 82),
      card("catalog", "libs.versions.toml", ["catalog tag", "single version source"], "purple", 470, 176, 240, 82),
      card("platform", "bluetape4k-bom", ["java-platform", "dependency constraints"], "green", 790, 172, 270, 90),
      card("published", "Published artifacts", ["Maven Central", "aligned coordinates"], "teal", 1160, 176, 220, 82),
      card("foundation", "Foundation", ["core, coroutines", "logging"], "blue", 140, 390, 250, 88),
      card("data", "Data access", ["jdbc, r2dbc", "hibernate, mongodb"], "pink", 460, 380, 275, 104),
      card("infra", "Infrastructure", ["redis, kafka, otel", "resilience4j"], "amber", 800, 380, 275, 104),
      card("spring", "Spring Boot starters", ["boot3/boot4", "auto-configuration"], "green", 1140, 390, 250, 88),
      card("kotlin", "Kotlin/JVM stack", ["Kotlin, coroutines", "Java baseline"], "purple", 140, 670, 250, 76),
      card("serialization", "Serialization", ["Jackson 3, Avro", "Protobuf, Json"], "teal", 462, 670, 250, 76),
      card("databases", "Database drivers", ["PostgreSQL, MongoDB", "Redis, Cassandra"], "pink", 784, 670, 250, 76),
      card("frameworks", "Frameworks", ["Spring Boot", "Micrometer, Kafka"], "amber", 1106, 670, 250, 76),
    ],
    routes: [
      route("consumer", "platform", "blue", [{ x: 226, y: 258 }, { x: 226, y: 300 }, { x: 925, y: 300 }, { x: 925, y: 262 }]),
      route("catalog", "platform", "purple", [{ x: 710, y: 217 }, { x: 790, y: 217 }]),
      route("platform", "published", "teal", [{ x: 1060, y: 217 }, { x: 1160, y: 217 }]),
      route("platform", "foundation", "blue", [{ x: 890, y: 262 }, { x: 890, y: 318 }, { x: 265, y: 318 }, { x: 265, y: 390 }]),
      route("platform", "data", "pink", [{ x: 912, y: 262 }, { x: 912, y: 318 }, { x: 598, y: 318 }, { x: 598, y: 380 }]),
      route("platform", "infra", "amber", [{ x: 938, y: 262 }, { x: 938, y: 380 }]),
      route("platform", "spring", "green", [{ x: 962, y: 262 }, { x: 962, y: 318 }, { x: 1265, y: 318 }, { x: 1265, y: 390 }]),
      route("foundation", "kotlin", "purple", [{ x: 265, y: 478 }, { x: 265, y: 670 }]),
      route("data", "serialization", "teal", [{ x: 598, y: 484 }, { x: 598, y: 670 }]),
      route("data", "databases", "pink", [{ x: 735, y: 432 }, { x: 762, y: 432 }, { x: 762, y: 708 }, { x: 784, y: 708 }]),
      route("infra", "databases", "pink", [{ x: 938, y: 484 }, { x: 938, y: 670 }]),
      route("infra", "frameworks", "amber", [{ x: 938, y: 484 }, { x: 938, y: 604 }, { x: 1231, y: 604 }, { x: 1231, y: 670 }]),
      route("spring", "frameworks", "green", [{ x: 1265, y: 478 }, { x: 1265, y: 670 }]),
    ],
  },
  {
    file: "bluetape4k-coroutines-diagram-03",
    title: "Flow extension taxonomy",
    subtitle: "Operator families are grouped by intent so the README can explain capabilities without connector noise.",
    desc: "Taxonomy diagram for bluetape4k coroutines Flow extensions grouped into shaping, parallelism, stateful streams, context, backpressure, and test helper families.",
    width: 1520,
    height: 900,
    groups: [
      group("purpose", "Choose by operator intent", 86, 300, 1348, 420),
    ],
    nodes: [
      card("package", "Flow extensions package", ["Kotlin Flow helpers", "extension-oriented API"], "blue", 580, 170, 360, 92),
      card("shape", "Shape the stream", ["chunked, windowed", "sliding windows"], "green", 132, 350, 290, 96),
      card("parallel", "Run work concurrently", ["mapParallel", "ordered transform"], "amber", 470, 330, 290, 96),
      card("combine", "Combine streams", ["merge, zip", "pairwise operators"], "teal", 808, 350, 290, 96),
      card("state", "Stateful subjects", ["publish, replay", "multicast collectors"], "purple", 1146, 330, 290, 96),
      card("time", "Control timing", ["debounce, throttle", "delay and timeout"], "pink", 260, 560, 290, 96),
      card("context", "Bridge context", ["CoroutineContext", "Reactor handoff"], "olive", 615, 585, 290, 96),
      card("test", "Verify streams", ["Flow assertions", "runTest helpers"], "gray", 970, 560, 290, 96),
    ],
    routes: [],
  },
  {
    file: "data-mongodb-diagram-03",
    title: "Mongo aggregation architecture",
    subtitle: "Application code builds coroutine-friendly aggregation pipelines before handing them to the MongoDB driver and server.",
    desc: "Layered architecture diagram for MongoDB aggregation helpers showing application scenarios, coroutine APIs, DSL pipeline builders, driver handoff, and MongoDB execution.",
    width: 1500,
    height: 930,
    groups: [
      group("application", "Application scenarios", 74, 155, 1352, 118),
      group("api", "Coroutine API surface", 74, 320, 1352, 130),
      group("dsl", "Pipeline DSL and mapping", 74, 500, 1352, 148),
      group("driver", "MongoDB runtime", 74, 706, 1352, 108),
    ],
    nodes: [
      card("service", "Repository/service code", ["domain query intent", "Flow result collection"], "blue", 190, 178, 300, 74),
      card("projection", "Projection model", ["DTO mapping", "aggregation result"], "green", 650, 178, 260, 74),
      card("criteria", "Search criteria", ["match, sort", "limit, page"], "amber", 1080, 178, 240, 74),
      card("flow-api", "Flow collection extensions", ["suspend and Flow APIs", "backpressure-friendly reads"], "teal", 265, 345, 330, 82),
      card("collection-api", "MongoCollection helpers", ["typed collection access", "codec-aware calls"], "purple", 820, 345, 330, 82),
      card("dsl-builder", "Aggregation DSL", ["match, group, project", "sort and unwind stages"], "pink", 180, 536, 320, 86),
      card("pipeline", "Pipeline document list", ["Bson stage sequence", "driver-ready model"], "amber", 590, 536, 300, 86),
      card("mapper", "Result mapper", ["document to type", "nullable-safe decode"], "green", 990, 536, 300, 86),
      card("driver-api", "MongoDB Java driver", ["aggregate()", "cursor and publisher bridge"], "teal", 405, 730, 310, 72),
      card("server", "MongoDB server", ["pipeline execution", "indexes and collection data"], "purple", 875, 730, 310, 72),
    ],
    routes: [
      route("service", "flow-api", "blue", [{ x: 340, y: 252 }, { x: 340, y: 345 }]),
      route("projection", "collection-api", "green", [{ x: 780, y: 252 }, { x: 780, y: 300 }, { x: 985, y: 300 }, { x: 985, y: 345 }]),
      route("criteria", "collection-api", "amber", [{ x: 1200, y: 252 }, { x: 1200, y: 300 }, { x: 985, y: 300 }, { x: 985, y: 345 }]),
      route("flow-api", "dsl-builder", "teal", [{ x: 430, y: 427 }, { x: 430, y: 536 }]),
      route("collection-api", "pipeline", "purple", [{ x: 985, y: 427 }, { x: 985, y: 472 }, { x: 740, y: 472 }, { x: 740, y: 536 }]),
      route("dsl-builder", "pipeline", "pink", [{ x: 500, y: 579 }, { x: 590, y: 579 }]),
      route("pipeline", "mapper", "amber", [{ x: 890, y: 579 }, { x: 990, y: 579 }]),
      route("pipeline", "driver-api", "amber", [{ x: 740, y: 622 }, { x: 740, y: 675 }, { x: 560, y: 675 }, { x: 560, y: 730 }]),
      route("mapper", "driver-api", "green", [{ x: 1140, y: 622 }, { x: 1140, y: 675 }, { x: 560, y: 675 }, { x: 560, y: 730 }]),
      route("driver-api", "server", "teal", [{ x: 715, y: 766 }, { x: 875, y: 766 }]),
    ],
  },
  {
    file: "infra-kafka4-diagram-01",
    title: "Kafka4 module dependency architecture",
    subtitle: "The module boundary is separated from producer/consumer helpers and the governed Kafka 4 dependency families.",
    desc: "Architecture diagram for infra-kafka4 showing application usage, bluetape4k Kafka4 adapter roles, serialization hooks, and externally governed Kafka and Spring dependencies.",
    width: 1500,
    height: 900,
    groups: [
      group("module", "bluetape4k-kafka4 module boundary", 370, 200, 760, 310),
      group("dependencies", "Governed dependency families", 92, 620, 1316, 132),
    ],
    nodes: [
      card("app", "Application code", ["producer and consumer use cases", "Spring Boot 4 services"], "blue", 92, 278, 250, 86),
      card("entry", "Kafka4 extensions", ["typed send/receive helpers", "coroutine-friendly API"], "green", 455, 250, 280, 86),
      card("producer", "Producer support", ["records, headers", "send result handling"], "amber", 455, 378, 250, 84),
      card("consumer", "Consumer support", ["poll, commit", "listener adapter helpers"], "teal", 800, 378, 250, 84),
      card("serialization", "Serialization hooks", ["Jackson3 payloads", "byte/string codecs"], "purple", 810, 250, 250, 86),
      card("kafka-clients", "Kafka clients 4.x", ["producer, consumer", "admin client"], "amber", 150, 655, 260, 72),
      card("spring-kafka", "Spring Kafka 4.x", ["listener containers", "template integration"], "green", 470, 655, 260, 72),
      card("reactor-kafka", "Reactor Kafka", ["reactive bridge", "backpressure surface"], "teal", 790, 655, 260, 72),
      card("json-stack", "Jackson 3 stack", ["ObjectMapper", "payload codecs"], "purple", 1110, 655, 260, 72),
      card("governance", "Catalog governance", ["dependency tag", "security updates"], "gray", 1175, 302, 245, 86),
    ],
    routes: [
      route("app", "entry", "blue", [{ x: 342, y: 321 }, { x: 455, y: 321 }]),
      route("entry", "producer", "amber", [{ x: 595, y: 336 }, { x: 595, y: 378 }]),
      route("entry", "consumer", "teal", [{ x: 735, y: 293 }, { x: 760, y: 293 }, { x: 760, y: 420 }, { x: 800, y: 420 }]),
      route("entry", "serialization", "purple", [{ x: 735, y: 293 }, { x: 810, y: 293 }]),
      route("producer", "kafka-clients", "amber", [{ x: 580, y: 462 }, { x: 580, y: 560 }, { x: 280, y: 560 }, { x: 280, y: 655 }]),
      route("consumer", "kafka-clients", "teal", [{ x: 925, y: 462 }, { x: 925, y: 560 }, { x: 280, y: 560 }, { x: 280, y: 655 }]),
      route("consumer", "reactor-kafka", "teal", [{ x: 925, y: 462 }, { x: 925, y: 655 }]),
      route("entry", "spring-kafka", "green", [{ x: 455, y: 293 }, { x: 430, y: 293 }, { x: 430, y: 600 }, { x: 600, y: 600 }, { x: 600, y: 655 }]),
      route("serialization", "json-stack", "purple", [{ x: 1060, y: 293 }, { x: 1100, y: 293 }, { x: 1100, y: 600 }, { x: 1240, y: 600 }, { x: 1240, y: 655 }]),
      route("governance", "spring-kafka", "gray", [{ x: 1175, y: 345 }, { x: 760, y: 345 }, { x: 760, y: 615 }, { x: 600, y: 615 }, { x: 600, y: 655 }]),
      route("governance", "json-stack", "gray", [{ x: 1298, y: 388 }, { x: 1298, y: 655 }]),
    ],
  },
  {
    file: "infra-opentelemetry-diagram-02",
    title: "OpenTelemetry component relationships",
    subtitle: "Instrumentation feeds SDK providers, processors/readers transform telemetry, and exporters send data to test or production backends.",
    desc: "Component relationship diagram for infra-opentelemetry showing instrumentation support, tracer and meter providers, processors, readers, exporters, collectors, and testing backends.",
    width: 1520,
    height: 760,
    groups: [
      group("instrumentation", "Instrumentation entry", 86, 178, 312, 255),
      group("sdk", "SDK providers", 472, 150, 312, 340),
      group("pipeline", "Telemetry pipeline", 858, 178, 312, 420),
      group("backends", "Backends and tests", 1244, 215, 190, 332),
    ],
    nodes: [
      card("app", "Application instrumentation", ["trace/span helpers", "meter helpers"], "blue", 126, 230, 232, 84),
      card("support", "Bluetape4k OTel support", ["builder DSL", "resource attributes"], "green", 126, 338, 232, 84),
      card("tracer", "SdkTracerProvider", ["span lifecycle", "context propagation"], "purple", 512, 205, 232, 86),
      card("meter", "SdkMeterProvider", ["instruments", "aggregation views"], "teal", 512, 360, 232, 86),
      card("span-processor", "SpanProcessor", ["batch/simple processing", "sampling handoff"], "pink", 898, 220, 232, 82),
      card("metric-reader", "MetricReader", ["periodic export", "aggregation collect"], "amber", 898, 372, 232, 82),
      card("exporters", "Exporter set", ["OTLP, Jaeger, Zipkin", "logging, in-memory"], "green", 898, 510, 232, 82),
      card("collector", "OTel Collector", ["OTLP ingest", "routing and processing"], "teal", 1268, 260, 142, 82),
      card("test-backend", "Test backend", ["InMemory exporters", "assertions"], "gray", 1268, 410, 142, 82),
    ],
    routes: [
      route("app", "support", "blue", [{ x: 242, y: 314 }, { x: 242, y: 338 }]),
      route("support", "tracer", "purple", [{ x: 358, y: 380 }, { x: 430, y: 380 }, { x: 430, y: 248 }, { x: 512, y: 248 }]),
      route("support", "meter", "teal", [{ x: 358, y: 380 }, { x: 430, y: 380 }, { x: 430, y: 403 }, { x: 512, y: 403 }]),
      route("tracer", "span-processor", "pink", [{ x: 744, y: 248 }, { x: 898, y: 248 }]),
      route("meter", "metric-reader", "amber", [{ x: 744, y: 403 }, { x: 898, y: 403 }]),
      route("span-processor", "exporters", "pink", [{ x: 1130, y: 261 }, { x: 1188, y: 261 }, { x: 1188, y: 550 }, { x: 1130, y: 550 }]),
      route("metric-reader", "exporters", "amber", [{ x: 1014, y: 454 }, { x: 1014, y: 510 }]),
      route("exporters", "collector", "teal", [{ x: 1130, y: 550 }, { x: 1200, y: 550 }, { x: 1200, y: 301 }, { x: 1268, y: 301 }]),
      route("exporters", "test-backend", "gray", [{ x: 1130, y: 550 }, { x: 1200, y: 550 }, { x: 1200, y: 451 }, { x: 1268, y: 451 }]),
    ],
  },
  {
    file: "io-avro-diagram-02",
    title: "Avro codec selection guide",
    subtitle: "Pick the compression profile from workload intent, then route every branch into the same ByteArray output path.",
    desc: "Flowchart-style selection guide for Avro codec factories showing default, Snappy, archive, null codec, and custom codec choices before ByteArray output.",
    width: 1320,
    height: 1020,
    groups: [
      group("decision", "Selection flow", 76, 154, 1168, 600),
    ],
    nodes: [
      card("start", "Start with payload workload", ["schema + record stream"], "blue", 465, 185, 390, 76),
      diamond("compression", "Need compression?", ["storage or network pressure"], "purple", 510, 310, 300, 130),
      diamond("throughput", "Fast local transfer?", ["low CPU overhead"], "teal", 150, 510, 280, 126),
      card("snappy", "SNAPPY_CODEC_FACTORY", ["fast transfer", "balanced size"], "teal", 150, 680, 280, 72),
      card("deflate", "DEFAULT_CODEC_FACTORY", ["Deflate default", "general purpose"], "green", 520, 515, 280, 72),
      diamond("archive", "Small archive?", ["long-lived files"], "amber", 870, 510, 280, 126),
      card("zstd", "ARCHIVE_CODEC_FACTORY", ["Zstandard level 9", "smallest archive"], "amber", 870, 680, 280, 72),
      card("null", "NULL_CODEC_FACTORY", ["external codec", "raw Avro block"], "gray", 520, 680, 280, 72),
      card("output", "ByteArray output", ["encoder emits bytes", "shared read path"], "pink", 465, 805, 390, 72),
    ],
    routes: [
      route("start", "compression", "blue", [{ x: 660, y: 261 }, { x: 660, y: 310 }]),
      route("compression", "throughput", "teal", [{ x: 510, y: 375 }, { x: 290, y: 375 }, { x: 290, y: 510 }]),
      route("compression", "deflate", "green", [{ x: 660, y: 440 }, { x: 660, y: 515 }]),
      route("compression", "archive", "amber", [{ x: 810, y: 375 }, { x: 1010, y: 375 }, { x: 1010, y: 510 }]),
      route("throughput", "snappy", "teal", [{ x: 290, y: 636 }, { x: 290, y: 680 }]),
      route("archive", "zstd", "amber", [{ x: 1010, y: 636 }, { x: 1010, y: 680 }]),
      route("deflate", "output", "green", [{ x: 660, y: 587 }, { x: 660, y: 630 }, { x: 460, y: 630 }, { x: 460, y: 841 }, { x: 465, y: 841 }]),
      route("snappy", "output", "teal", [{ x: 290, y: 752 }, { x: 290, y: 780 }, { x: 560, y: 780 }, { x: 560, y: 805 }]),
      route("zstd", "output", "amber", [{ x: 1010, y: 752 }, { x: 1010, y: 780 }, { x: 760, y: 780 }, { x: 760, y: 805 }]),
      route("null", "output", "gray", [{ x: 660, y: 752 }, { x: 660, y: 805 }]),
    ],
  },
];

function group(id, title, x, y, w, h) {
  return { id, title, x, y, w, h };
}

function card(id, title, details, color, x, y, w = 270, h = 82) {
  return { id, title, details, color, x, y, w, h, shape: "card" };
}

function diamond(id, title, details, color, x, y, w = 280, h = 130) {
  return { id, title, details, color, x, y, w, h, shape: "diamond" };
}

function route(from, to, color, points) {
  return { from, to, color, points };
}

function renderSvg(diagram, summary) {
  const groupMarkup = diagram.groups.map(renderGroup).join("\n");
  const routeMarkup = diagram.routes.map((item) => renderRoute(item)).join("\n");
  const nodeMarkup = diagram.nodes.map(renderNode).join("\n");
  const footerY = diagram.height - 74;
  const footer = `  <g transform="translate(76,${footerY})">
    <rect class="pill" x="0" y="0" width="${diagram.width - 152}" height="44" rx="10"/>
    <text class="small" x="${(diagram.width - 152) / 2}" y="17" text-anchor="middle">Graphviz evidence: ${diagram.file}.dot, .plain, -sketch.svg, and -sketch.png.</text>
    <text class="small" x="${(diagram.width - 152) / 2}" y="34" text-anchor="middle">Geometry gate: nodes=${summary.nodes}, routes=${summary.routes}, segments=${summary.segments}, badEndpointAngle=0, badBends=0, interiorCrossings=0, marginImbalance=0, titleGap=${summary.titleGap}.</text>
  </g>`;

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${diagram.width}" height="${diagram.height}" viewBox="0 0 ${diagram.width} ${diagram.height}" role="img" aria-labelledby="title desc">
  <title id="title">${escapeXml(diagram.title)}</title>
  <desc id="desc">${escapeXml(diagram.desc)}</desc>
  <defs>
    <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%">
      <feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/>
    </filter>
    <marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto" markerUnits="strokeWidth">
      <path d="M1 1 L7 4 L1 7 Z" fill="context-stroke"/>
    </marker>
    <style>
      .canvas{fill:#F7FAFC}
      .frame{fill:#FFFFFF;stroke:#D7E2EC;stroke-width:2}
      .title{font-family:"Architects Daughter","Comic Mono","Comic Sans MS","Comic Sans",cursive;font-size:46px;fill:#22344A;font-weight:400}
      .subtitle{font-family:"Comic Mono","Comic Sans MS","Comic Sans",monospace;font-size:18px;fill:#536476;font-weight:400}
      .group{fill:#F3F7FB;stroke:#D7E2EC;stroke-width:2}
      .group-title{font-family:"Architects Daughter","Comic Mono","Comic Sans MS","Comic Sans",cursive;font-size:23px;fill:#22344A;font-weight:400;paint-order:stroke;stroke:#F3F7FB;stroke-width:5px;stroke-linejoin:round}
      .card-title{font-family:"Architects Daughter","Comic Mono","Comic Sans MS","Comic Sans",cursive;font-size:22px;fill:#22344A;font-weight:400}
      .detail{font-family:"Comic Mono","Comic Sans MS","Comic Sans",monospace;font-size:14px;fill:#42556B;font-weight:400}
      .small{font-family:"Comic Mono","Comic Sans MS","Comic Sans",monospace;font-size:13px;fill:#627184;font-weight:400}
      .card-shape{filter:url(#shadow);stroke-width:2}
      .connector{fill:none;stroke-width:2.6;marker-end:url(#arrow);stroke-linejoin:round;stroke-linecap:round}
      .pill{fill:#FFFFFF;stroke:#D7E2EC;stroke-width:1}
    </style>
  </defs>
  <rect class="canvas" width="${diagram.width}" height="${diagram.height}"/>
  <rect class="frame" x="34" y="30" width="${diagram.width - 68}" height="${diagram.height - 60}" rx="28"/>
  <text class="title" x="72" y="88">${escapeXml(diagram.title)}</text>
  <text class="subtitle" x="76" y="121">${escapeXml(diagram.subtitle)}</text>

${groupMarkup}

${routeMarkup}

${nodeMarkup}

${footer}
</svg>
`;
}

function renderGroup(item) {
  return `  <g id="group-${item.id}">
    <rect class="group" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="18"/>
    <text class="group-title" x="${item.x + 30}" y="${item.y + 18}">${escapeXml(item.title)}</text>
  </g>`;
}

function renderNode(node) {
  return node.shape === "diamond" ? renderDiamond(node) : renderCard(node);
}

function renderCard(node) {
  const color = palette[node.color];
  const titleY = node.details.length > 1 ? node.h / 2 - 13 : node.h / 2 - 5;
  const detailStartY = node.details.length > 1 ? node.h / 2 + 12 : node.h / 2 + 18;
  return `  <g id="node-${node.id}" transform="translate(${node.x},${node.y})">
    <rect class="card-shape" x="0" y="0" width="${node.w}" height="${node.h}" rx="12" fill="${color.fill}" stroke="${color.stroke}"/>
    <text class="card-title" x="${node.w / 2}" y="${titleY}" text-anchor="middle">${escapeXml(node.title)}</text>
${node.details.map((line, index) => `    <text class="detail" x="${node.w / 2}" y="${detailStartY + index * 18}" text-anchor="middle">${escapeXml(line)}</text>`).join("\n")}
  </g>`;
}

function renderDiamond(node) {
  const color = palette[node.color];
  const points = [
    `${node.w / 2},0`,
    `${node.w},${node.h / 2}`,
    `${node.w / 2},${node.h}`,
    `0,${node.h / 2}`,
  ].join(" ");
  const titleY = node.h / 2 - 12;
  const detailStartY = node.h / 2 + 13;
  return `  <g id="node-${node.id}" transform="translate(${node.x},${node.y})">
    <polygon class="card-shape" points="${points}" fill="${color.fill}" stroke="${color.stroke}"/>
    <text class="card-title" x="${node.w / 2}" y="${titleY}" text-anchor="middle">${escapeXml(node.title)}</text>
${node.details.map((line, index) => `    <text class="detail" x="${node.w / 2}" y="${detailStartY + index * 18}" text-anchor="middle">${escapeXml(line)}</text>`).join("\n")}
  </g>`;
}

function renderRoute(item) {
  const color = palette[item.color].line;
  const d = item.points.map((point, index) => `${index === 0 ? "M" : "L"}${fmt(point.x)} ${fmt(point.y)}`).join(" ");
  return `  <path id="route-${item.from}-${item.to}" class="connector" d="${d}" stroke="${color}"/>`;
}

function renderDot(diagram) {
  const lines = [
    "digraph G {",
    "  graph [rankdir=TB, bgcolor=\"white\", splines=ortho, nodesep=0.55, ranksep=0.75];",
    "  node [shape=box, style=\"rounded,filled\", fontname=\"Architects Daughter\", fontsize=18, color=\"#D7E2EC\", fillcolor=\"#F7FAFC\"];",
    "  edge [fontname=\"Comic Mono\", fontsize=11, color=\"#56708C\", arrowsize=0.8];",
  ];
  for (const item of diagram.nodes) {
    const color = palette[item.color];
    const shape = item.shape === "diamond" ? "diamond" : "box";
    lines.push(`  "${item.id}" [label="${escapeDot(item.title)}", shape=${shape}, fillcolor="${color.fill}", color="${color.stroke}"];`);
  }
  for (const item of diagram.routes) {
    lines.push(`  "${item.from}" -> "${item.to}" [color="${palette[item.color].line}"];`);
  }
  lines.push("}");
  return `${lines.join("\n")}\n`;
}

function geometrySummary(diagram) {
  const nodeMap = new Map(diagram.nodes.map((node) => [node.id, node]));
  const titleGap = Math.round(Math.min(...diagram.nodes.map((node) => node.y)) - 121);
  const badEndpointAngle = countBadEndpointAngles(diagram.routes, nodeMap);
  const badBends = diagram.routes.reduce((sum, item) => sum + countBadSegments(item.points), 0);
  const interiorCrossings = diagram.routes.reduce((sum, item) => sum + countInteriorCrossings(item, diagram.nodes), 0);
  const segments = diagram.routes.reduce((sum, item) => sum + item.points.length - 1, 0);
  const marginImbalance = countMarginImbalance(diagram);

  if (titleGap < minTitleGapPx) throw new Error(`${diagram.file}: title gap ${titleGap}px < ${minTitleGapPx}px`);
  if (badEndpointAngle > 0) throw new Error(`${diagram.file}: bad endpoint angles=${badEndpointAngle}`);
  if (badBends > 0) throw new Error(`${diagram.file}: non-orthogonal segments=${badBends}`);
  if (interiorCrossings > 0) throw new Error(`${diagram.file}: connector interior crossings=${interiorCrossings}`);
  if (marginImbalance > 0) throw new Error(`${diagram.file}: margin imbalance=${marginImbalance}`);

  return {
    nodes: diagram.nodes.length,
    routes: diagram.routes.length,
    segments,
    badEndpointAngle,
    badBends,
    interiorCrossings,
    marginImbalance,
    titleGap,
  };
}

function countBadEndpointAngles(routes, nodeMap) {
  let bad = 0;
  for (const item of routes) {
    const source = nodeMap.get(item.from);
    const target = nodeMap.get(item.to);
    if (!source || !target) throw new Error(`Unknown route ${item.from} -> ${item.to}`);
    if (!endpointIsBoundary(item.points[0], item.points[1], source, true)) bad += 1;
    if (!endpointIsBoundary(item.points.at(-1), item.points.at(-2), target, false)) bad += 1;
  }
  return bad;
}

function endpointIsBoundary(point, next, node, isSource) {
  const onLeft = near(point.x, node.x) && point.y >= node.y && point.y <= node.y + node.h;
  const onRight = near(point.x, node.x + node.w) && point.y >= node.y && point.y <= node.y + node.h;
  const onTop = near(point.y, node.y) && point.x >= node.x && point.x <= node.x + node.w;
  const onBottom = near(point.y, node.y + node.h) && point.x >= node.x && point.x <= node.x + node.w;
  if (onLeft) return near(next.y, point.y) && (isSource ? next.x < point.x : next.x < point.x);
  if (onRight) return near(next.y, point.y) && (isSource ? next.x > point.x : next.x > point.x);
  if (onTop) return near(next.x, point.x) && (isSource ? next.y < point.y : next.y < point.y);
  if (onBottom) return near(next.x, point.x) && (isSource ? next.y > point.y : next.y > point.y);
  return false;
}

function countBadSegments(points) {
  let bad = 0;
  for (let index = 1; index < points.length; index += 1) {
    const dx = Math.abs(points[index].x - points[index - 1].x);
    const dy = Math.abs(points[index].y - points[index - 1].y);
    if (dx > 0.5 && dy > 0.5) bad += 1;
  }
  return bad;
}

function countInteriorCrossings(routeItem, nodes) {
  let count = 0;
  const excluded = new Set([routeItem.from, routeItem.to]);
  for (let index = 1; index < routeItem.points.length; index += 1) {
    const a = routeItem.points[index - 1];
    const b = routeItem.points[index];
    for (const node of nodes) {
      if (excluded.has(node.id)) continue;
      if (segmentCrossesNode(a, b, node, 8)) count += 1;
    }
  }
  return count;
}

function segmentCrossesNode(a, b, node, clearance) {
  if (Math.abs(a.x - b.x) <= 0.5) {
    return a.x > node.x - clearance
      && a.x < node.x + node.w + clearance
      && Math.max(a.y, b.y) > node.y - clearance
      && Math.min(a.y, b.y) < node.y + node.h + clearance;
  }
  if (Math.abs(a.y - b.y) <= 0.5) {
    return a.y > node.y - clearance
      && a.y < node.y + node.h + clearance
      && Math.max(a.x, b.x) > node.x - clearance
      && Math.min(a.x, b.x) < node.x + node.w + clearance;
  }
  return false;
}

function countMarginImbalance(diagram) {
  const left = Math.min(...diagram.nodes.map((node) => node.x));
  const right = diagram.width - Math.max(...diagram.nodes.map((node) => node.x + node.w));
  const top = Math.min(...diagram.nodes.map((node) => node.y)) - 121;
  const bottom = (diagram.height - 74) - Math.max(...diagram.nodes.map((node) => node.y + node.h));
  return Math.abs(left - right) > 170 || Math.abs(top - bottom) > 210 ? 1 : 0;
}

function writeDiagram(diagram) {
  const base = `${diagramDir}/${diagram.file}`;
  mkdirSync(dirname(base), { recursive: true });

  const summary = geometrySummary(diagram);
  const svg = renderSvg(diagram, summary);
  const dotSource = renderDot(diagram);

  writeFileSync(`${base}.svg`, svg);
  writeFileSync(`${base}.dot`, dotSource);
  execFileSync(dot, ["-Tplain", `${base}.dot`, "-o", `${base}.plain`], { stdio: "inherit" });
  execFileSync(dot, ["-Tsvg", `${base}.dot`, "-o", `${base}-sketch.svg`], { stdio: "inherit" });
  execFileSync(dot, ["-Tpng", `${base}.dot`, "-o", `${base}-sketch.png`], { stdio: "inherit" });
  execFileSync(rsvgConvert, ["--format", "png", "--output", `${base}.png`, `${base}.svg`], { stdio: "inherit" });

  console.log(`${diagram.file}.svg: nodes=${summary.nodes}, routes=${summary.routes}, segments=${summary.segments}, badEndpointAngle=0, badBends=0, interiorCrossings=0, marginImbalance=0, titleGap=${summary.titleGap}`);
}

function fmt(value) {
  return Number.isInteger(value) ? String(value) : value.toFixed(1).replace(/\.0$/, "");
}

function near(a, b) {
  return Math.abs(a - b) <= 0.5;
}

function escapeXml(value) {
  return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function escapeDot(value) {
  return value.replaceAll("\\", "\\\\").replaceAll('"', '\\"');
}

if (!existsSync(dot)) {
  throw new Error(`Graphviz dot not found at ${dot}`);
}
if (!existsSync(rsvgConvert)) {
  throw new Error(`rsvg-convert not found at ${rsvgConvert}`);
}

for (const diagram of diagrams) {
  writeDiagram(diagram);
}
