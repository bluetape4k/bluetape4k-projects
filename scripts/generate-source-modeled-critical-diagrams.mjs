#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const OUT = join(process.cwd(), "docs/images/readme-diagrams");
const rsvg = "/opt/homebrew/bin/rsvg-convert";

const palette = {
  blue: ["#E8F3FF", "#5B8DEF", "#4F83BF"],
  green: ["#EAF7EF", "#58A978", "#3E9868"],
  teal: ["#E9F7F6", "#45A7A1", "#2E8F89"],
  amber: ["#FFF3D9", "#D6A441", "#B9851B"],
  pink: ["#FDECEF", "#DC6B82", "#C94D68"],
  purple: ["#F1ECFF", "#8A72D6", "#755BC6"],
  olive: ["#EEF6D9", "#8BA84D", "#718A35"],
  gray: ["#F2F5F9", "#9AA8B8", "#758297"],
};

mkdirSync(OUT, { recursive: true });

const diagrams = [
  r2dbcCoreApi(),
  kafkaLogbackClass(),
  grpcClass(),
  micrometerMetricFlow(),
  redisUmbrella(),
  redissonCodecMap(),
  redissonBatchTransaction(),
];

for (const diagram of diagrams) render(diagram);

function r2dbcCoreApi() {
  return {
    file: "data-r2dbc-diagram-02",
    kind: "class",
    width: 1580,
    height: 900,
    title: "R2DBC Core API Structure",
    subtitle: "Entry points, query builders, binding helpers, and coroutine fetch extensions are placed by call proximity.",
    source: "bluetape4k-projects / data:r2dbc - github.com/bluetape4k/bluetape4k-projects",
    nodes: [
      classCard("R2dbcClient", 110, 210, 320, 132, "R2dbcClient", "class", ["wraps DatabaseClient", "execute(sql) / execute(query)"], "blue"),
      classCard("ExecuteDsl", 110, 410, 320, 132, "Execute DSL", "extension functions", ["execute(sqlString)", "execute(query: Query)"], "green"),
      classCard("QueryBuilder", 575, 210, 320, 132, "QueryBuilder", "builder class", ["select / where / orderBy", "build(): Query"], "amber"),
      classCard("Query", 575, 410, 320, 132, "Query", "data class", ["sql: String", "parameters: Map<String, Any?>"], "teal"),
      classCard("DatabaseClientExtensions", 1015, 210, 410, 158, "DatabaseClientExtensions", "coroutine fetch API", ["flow(mapper)", "awaitSingle / awaitList", "awaitExists / awaitCount"], "purple"),
      classCard("BindSpecExtensions", 1015, 430, 410, 132, "BindSpecExtensions", "binding helpers", ["bindMap(params)", "bindIndexedMap(params)"], "pink"),
      note("r2dbcIntent", 340, 650, 900, 70, "Flow: QueryBuilder builds Query; Execute DSL uses Query; BindSpec feeds DatabaseClient fetch extensions.", "gray"),
    ],
    routes: [
      route("R2dbcClient", "ExecuteDsl", "M270 342 L270 410", "green", "inherit"),
      route("QueryBuilder", "Query", "M735 342 L735 410", "amber", "dependency"),
      route("ExecuteDsl", "Query", "M430 476 L575 476", "teal", "dependency"),
      route("Query", "BindSpecExtensions", "M895 476 L1015 476", "pink", "dependency"),
      route("BindSpecExtensions", "DatabaseClientExtensions", "M1220 430 L1220 368", "purple", "dependency"),
    ],
  };
}

function kafkaLogbackClass() {
  return {
    file: "infra-kafka-logback-diagram-01",
    kind: "class",
    width: 1500,
    height: 980,
    title: "Kafka Logback Class Structure",
    subtitle: "Appender, exporter, key provider, and fallback error handling are separated so dependencies stay short.",
    source: "bluetape4k-projects / infra:kafka-logback - github.com/bluetape4k/bluetape4k-projects",
    nodes: [
      classCard("AbstractKafkaAppender", 550, 170, 360, 136, "AbstractKafkaAppender", "abstract class", ["producer config", "topic, encoder, keyProvider", "option validation"], "teal"),
      classCard("KafkaAppender", 550, 360, 360, 148, "KafkaAppender", "class", ["drains Kafka-client log queue", "encodes event to ProducerRecord", "delegates send to exporter"], "purple"),
      classCard("KafkaExporter", 120, 560, 350, 116, "KafkaExporter", "interface", ["export producer record", "exception handler callback"], "blue"),
      classCard("DefaultKafkaExporter", 120, 740, 350, 116, "DefaultKafkaExporter", "class", ["KafkaProducer.send", "fire-and-forget callback"], "green"),
      classCard("KafkaKeyProvider", 990, 560, 350, 116, "KafkaKeyProvider", "interface", ["event -> ByteArray key"], "amber"),
      classCard("KeyProviderFamilies", 990, 740, 350, 116, "Key providers", "implementations", ["hostname, logger, thread", "context, null"], "olive"),
      classCard("ExportExceptionHandler", 550, 740, 360, 116, "ExportExceptionHandler", "fun interface", ["warn and fallback appender", "receives failed event"], "pink"),
    ],
    routes: [
      route("KafkaAppender", "AbstractKafkaAppender", "M730 360 L730 306", "teal", "inherit"),
      route("DefaultKafkaExporter", "KafkaExporter", "M295 740 L295 676", "blue", "inherit"),
      route("KeyProviderFamilies", "KafkaKeyProvider", "M1165 740 L1165 676", "amber", "inherit"),
      route("KafkaAppender", "KafkaExporter", "M550 438 L500 438 L500 618 L470 618", "blue", "dependency"),
      route("KafkaAppender", "KafkaKeyProvider", "M910 438 L960 438 L960 618 L990 618", "amber", "dependency"),
      route("KafkaAppender", "ExportExceptionHandler", "M730 508 L730 740", "pink", "dependency"),
    ],
  };
}

function grpcClass() {
  return {
    file: "io-grpc-diagram-01",
    kind: "class",
    width: 1420,
    height: 1040,
    title: "gRPC Class Hierarchy",
    subtitle: "Server lifecycle and client channel families are separated before in-process test helpers are connected.",
    source: "bluetape4k-projects / io:grpc - github.com/bluetape4k/bluetape4k-projects",
    nodes: [
      classCard("GrpcServer", 150, 180, 340, 126, "GrpcServer", "interface", ["start / stop / close", "isRunning / isShutdown"], "blue"),
      classCard("AbstractGrpcClient", 890, 180, 340, 126, "AbstractGrpcClient", "abstract class", ["ManagedChannel lifecycle", "close with shutdownNow fallback"], "green"),
      classCard("AbstractGrpcServer", 150, 395, 340, 142, "AbstractGrpcServer", "abstract class", ["ServerBuilder + services", "shutdown hook registration"], "teal"),
      classCard("AbstractGrpcInprocessClient", 890, 620, 340, 142, "AbstractGrpcInprocessClient", "abstract class", ["InProcessChannelBuilder", "serverName target"], "purple"),
      classCard("AbstractGrpcInprocessServer", 150, 620, 340, 142, "AbstractGrpcInprocessServer", "abstract class", ["InProcessServerBuilder", "same serverName contract"], "amber"),
      classCard("ServerSupport", 545, 395, 300, 126, "ServerSupport", "extensions", ["serverOf helpers", "interceptor utilities"], "pink"),
      classCard("ManagedChannelSupport", 890, 800, 340, 126, "ManagedChannelSupport", "extensions", ["managedChannel builders", "host/port validation"], "olive"),
    ],
    routes: [
      route("AbstractGrpcServer", "GrpcServer", "M320 395 L320 306", "blue", "inherit"),
      route("AbstractGrpcInprocessServer", "AbstractGrpcServer", "M320 620 L320 537", "teal", "inherit"),
      route("AbstractGrpcInprocessClient", "AbstractGrpcClient", "M1060 620 L1060 306", "green", "inherit"),
      route("AbstractGrpcInprocessServer", "AbstractGrpcInprocessClient", "M490 691 L890 691", "purple", "dependency"),
      route("AbstractGrpcServer", "ServerSupport", "M490 466 L545 466", "pink", "dependency"),
      route("AbstractGrpcClient", "ManagedChannelSupport", "M1230 243 L1300 243 L1300 863 L1230 863", "olive", "dependency"),
    ],
  };
}

function micrometerMetricFlow() {
  return {
    file: "infra-micrometer-diagram-02",
    kind: "flow",
    width: 1600,
    height: 1080,
    title: "Metric Collection Flow",
    subtitle: "Application operations enter helper families, then record into Micrometer registry or Observation context.",
    source: "bluetape4k-projects / infra:micrometer - github.com/bluetape4k/bluetape4k-projects",
    panels: [
      panel("appLayer", 72, 165, 300, 740, "Application operation"),
      panel("helperLayer", 430, 165, 390, 740, "bluetape4k helper"),
      panel("registryLayer", 880, 165, 300, 740, "Micrometer API"),
      panel("backendLayer", 1235, 165, 290, 740, "Recorded signal"),
    ],
    nodes: [
      card("suspendFlow", 105, 265, 235, 96, "Suspend or Flow work", ["business block", "Flow collection"], "blue"),
      card("clientWork", 105, 455, 235, 96, "HTTP or cache work", ["Retrofit Call", "Cache2k operation"], "amber"),
      card("eventWork", 105, 645, 235, 96, "Event publish/consume", ["operation + destination", "correlation metadata"], "green"),
      card("timerHelper", 470, 250, 310, 116, "Timer extensions", ["recordSuspend", "Flow.withTimer"], "blue"),
      card("binderHelper", 470, 430, 310, 116, "Instrumentation binders", ["MeasuredCallAdapter", "Cache2kCacheMetrics"], "amber"),
      card("observationHelper", 470, 625, 310, 136, "Observation helpers", ["withObservationContext", "observeEventPublish", "observeEventConsume"], "green"),
      card("meterRegistry", 915, 290, 230, 112, "MeterRegistry", ["Timer.record", "cache meters"], "blue"),
      card("observationRegistry", 915, 627, 230, 132, "ObservationRegistry", ["start/openScope/stop", "low/high cardinality keys"], "green"),
      card("timerSignal", 1270, 290, 220, 112, "Timer metrics", ["duration", "count"], "blue"),
      card("eventSignal", 1270, 627, 220, 132, "Observation signal", ["outcome tags", "trace/span bridge"], "green"),
    ],
    routes: [
      route("suspendFlow", "timerHelper", "M340 313 L470 313", "blue", "flow"),
      route("clientWork", "binderHelper", "M340 503 L470 503", "amber", "flow"),
      route("eventWork", "observationHelper", "M340 693 L470 693", "green", "flow"),
      route("timerHelper", "meterRegistry", "M780 346 L915 346", "blue", "flow"),
      route("binderHelper", "meterRegistry", "M780 488 L850 488 L850 374 L915 374", "amber", "flow"),
      route("observationHelper", "observationRegistry", "M780 693 L915 693", "green", "flow"),
      route("meterRegistry", "timerSignal", "M1145 346 L1270 346", "blue", "flow"),
      route("observationRegistry", "eventSignal", "M1145 693 L1270 693", "green", "flow"),
    ],
  };
}

function redisUmbrella() {
  return {
    file: "infra-redis-diagram-01",
    kind: "module",
    width: 1500,
    height: 1000,
    title: "Redis Umbrella Module Structure",
    subtitle: "The umbrella dependency bundles Lettuce and Redisson; Spring Data Redis serializers stay in a separate module family.",
    source: "bluetape4k-projects / infra:redis - github.com/bluetape4k/bluetape4k-projects",
    panels: [
      panel("choiceLayer", 72, 165, 1356, 160, "Dependency choice"),
      panel("clientLayer", 72, 365, 860, 310, "Bundled Redis clients"),
      panel("springLayer", 985, 365, 443, 310, "Separate Spring Data Redis"),
      panel("usageLayer", 72, 715, 1356, 170, "Reader guidance"),
    ],
    nodes: [
      card("umbrella", 245, 220, 1010, 74, "bluetape4k-redis umbrella", ["keeps existing full-bundle dependency working"], "blue"),
      card("lettuce", 125, 450, 350, 116, "bluetape4k-lettuce", ["async/coroutine Redis client", "high-performance binary codecs"], "green"),
      card("redisson", 565, 450, 350, 116, "bluetape4k-redisson", ["distributed objects", "NearCache, leader election"], "purple"),
      card("springRedis", 1035, 450, 330, 116, "spring-data-redis", ["RedisTemplate serializers", "ReactiveRedisTemplate context"], "amber"),
      note("redisGuidance", 245, 785, 1010, 58, "Use the umbrella for compatibility, choose Lettuce or Redisson directly for smaller dependencies, and use Spring Data Redis modules only for template serialization.", "gray"),
    ],
    routes: [
      route("umbrella", "lettuce", "M390 294 L390 450", "green", "flow"),
      route("umbrella", "redisson", "M740 294 L740 450", "purple", "flow"),
      route("springRedis", "redisGuidance", "M1200 566 L1200 785", "amber", "flow"),
      route("lettuce", "redisGuidance", "M300 566 L300 785", "green", "flow"),
      route("redisson", "redisGuidance", "M740 566 L740 785", "purple", "flow"),
    ],
  };
}

function redissonCodecMap() {
  return {
    file: "infra-redisson-diagram-01",
    kind: "class",
    width: 1760,
    height: 1040,
    title: "Redisson Codec Class Diagram",
    subtitle: "Factory methods expose serializer families, compression decorators, and composite map choices.",
    source: "bluetape4k-projects / infra:redisson - github.com/bluetape4k/bluetape4k-projects",
    nodes: [
      classCard("RedissonCodecs", 690, 170, 380, 122, "RedissonCodecs", "factory object", ["Default = Fory", "named serializer/compression choices"], "blue"),
      classCard("ForyCodec", 140, 390, 300, 124, "ForyCodec", "serializer codec", ["Fory bytes", "fallback to Kryo5"], "green"),
      classCard("FastForyCodec", 500, 390, 300, 124, "FastForyCodec", "serializer codec", ["SCHEMA_CONSISTENT", "volatile caches only"], "teal"),
      classCard("JsonCodecs", 860, 390, 300, 124, "JSON codecs", "serializer group", ["Jackson3Codec", "Fastjson2Codec"], "amber"),
      classCard("BuiltinCodecs", 1220, 390, 300, 124, "Built-in codecs", "Redisson codecs", ["Kryo5, JDK", "String, Int, Long, Double"], "gray"),
      classCard("CompressionWrappers", 320, 650, 430, 132, "Compression wrappers", "decorator codecs", ["Lz4Codec, ZstdCodec, GzipCodec", "SnappyCodecV2 wrappers"], "pink"),
      classCard("CompositeCodecs", 860, 650, 620, 132, "Composite codecs", "map/set choices", ["String key codec", "value codec from selected family"], "purple"),
      note("codecGuidance", 360, 850, 1040, 58, "Recommended path: choose Fory/LZ4Fory for internal cache speed, ZstdFory for ratio, Jackson3/Fastjson2 with allow-list for JSON boundaries.", "gray"),
    ],
    routes: [
      route("RedissonCodecs", "ForyCodec", "M760 292 L760 330 L290 330 L290 390", "green", "dependency"),
      route("RedissonCodecs", "FastForyCodec", "M820 292 L820 350 L650 350 L650 390", "teal", "dependency"),
      route("RedissonCodecs", "JsonCodecs", "M940 292 L940 390", "amber", "dependency"),
      route("RedissonCodecs", "BuiltinCodecs", "M1000 292 L1000 340 L1370 340 L1370 390", "gray", "dependency"),
      route("ForyCodec", "CompressionWrappers", "M290 514 L290 604 L535 604 L535 650", "pink", "dependency"),
      route("FastForyCodec", "CompressionWrappers", "M650 514 L650 650", "pink", "dependency"),
      route("JsonCodecs", "CompositeCodecs", "M1010 514 L1010 650", "purple", "dependency"),
      route("BuiltinCodecs", "CompositeCodecs", "M1370 514 L1370 650", "purple", "dependency"),
    ],
  };
}

function redissonBatchTransaction() {
  return {
    file: "infra-redisson-diagram-02",
    kind: "flow",
    width: 1600,
    height: 980,
    title: "Batch and Transaction Processing Flow",
    subtitle: "Batch reduces round trips; transaction commits on success and rolls back before rethrowing on failure.",
    source: "bluetape4k-projects / infra:redisson - github.com/bluetape4k/bluetape4k-projects",
    panels: [
      panel("entryLayer", 72, 170, 330, 645, "Entry DSL"),
      panel("workLayer", 430, 170, 650, 645, "Redisson work object"),
      panel("outcomeLayer", 1120, 170, 408, 645, "Outcome"),
    ],
    nodes: [
      card("batchCall", 125, 265, 240, 94, "withBatch", ["sync or suspended", "action: RBatch.()"], "blue"),
      card("rBatch", 455, 265, 240, 94, "RBatch", ["queue Redis commands", "BatchOptions"], "green"),
      card("batchExecute", 785, 265, 270, 94, "execute", ["execute()", "executeAsync().await()"], "teal"),
      card("batchResult", 1165, 265, 250, 94, "BatchResult", ["single network round trip", "result list"], "amber"),
      card("txCall", 125, 590, 240, 94, "withTransaction", ["sync or suspended", "action: RTransaction.()"], "purple"),
      card("rTx", 455, 590, 240, 94, "RTransaction", ["createTransaction", "TransactionOptions"], "green"),
      card("txCommit", 785, 555, 270, 94, "commit path", ["action completes", "commit / commitAsync"], "teal"),
      card("txRollback", 785, 690, 270, 94, "rollback path", ["action throws", "rollback then rethrow"], "pink"),
      card("txOutcome", 1165, 625, 250, 94, "Caller outcome", ["success returns", "original error preserved"], "amber"),
    ],
    routes: [
      route("batchCall", "rBatch", "M365 312 L455 312", "blue", "flow"),
      route("rBatch", "batchExecute", "M695 312 L785 312", "green", "flow"),
      route("batchExecute", "batchResult", "M1055 312 L1165 312", "teal", "flow"),
      route("txCall", "rTx", "M365 637 L455 637", "purple", "flow"),
      route("rTx", "txCommit", "M695 620 L740 620 L740 602 L785 602", "green", "flow"),
      route("rTx", "txRollback", "M695 660 L740 660 L740 737 L785 737", "pink", "flow"),
      route("txCommit", "txOutcome", "M1055 602 L1100 602 L1100 650 L1165 650", "teal", "flow"),
      route("txRollback", "txOutcome", "M1055 737 L1100 737 L1100 700 L1165 700", "pink", "flow"),
    ],
  };
}

function render(diagram) {
  const svgPath = join(OUT, `${diagram.file}.svg`);
  const pngPath = join(OUT, `${diagram.file}.png`);
  const dotPath = join(OUT, `${diagram.file}.dot`);
  const plainPath = join(OUT, `${diagram.file}.plain`);
  const sketchSvgPath = join(OUT, `${diagram.file}-graphviz.svg`);
  const sketchPngPath = join(OUT, `${diagram.file}-graphviz.png`);

  writeFileSync(dotPath, dotFor(diagram));
  writeFileSync(plainPath, execFileSync("dot", ["-Tplain", dotPath], { encoding: "utf8" }));
  writeFileSync(sketchSvgPath, execFileSync("dot", ["-Tsvg", dotPath], { encoding: "utf8" }));
  execFileSync("dot", ["-Tpng", dotPath, "-o", sketchPngPath], { stdio: "inherit" });
  writeFileSync(svgPath, cleanSvg(svgFor(diagram)));
  execFileSync(rsvg, ["--format=png", "--output", pngPath, svgPath], { stdio: "inherit" });
  console.log(`${diagram.file}: source-modeled nodes=${diagram.nodes.length} routes=${diagram.routes.length}`);
}

function dotFor(diagram) {
  const lines = [
    "digraph G {",
    "  graph [rankdir=TB, splines=ortho, nodesep=0.9, ranksep=1.1, outputorder=edgesfirst];",
    "  node [shape=box, style=\"rounded,filled\", fontname=\"Comic Mono\", fontsize=11, margin=\"0.16,0.10\"];",
    "  edge [fontname=\"Comic Mono\", fontsize=10, arrowsize=0.7, penwidth=1.8];",
  ];
  for (const item of diagram.nodes.filter((nodeItem) => nodeItem.shape !== "note")) {
    const color = palette[item.color] || palette.gray;
    lines.push(`  ${item.id} [label="${escDot(item.title)}", fillcolor="${color[0]}", color="${color[1]}"];`);
  }
  for (const item of diagram.routes) {
    const style = item.kind === "inherit" ? "solid" : "dashed";
    lines.push(`  ${item.from} -> ${item.to} [style="${style}", color="${(palette[item.color] || palette.gray)[2]}"];`);
  }
  lines.push("}");
  return `${lines.join("\n")}\n`;
}

function svgFor(diagram) {
  const body = [
    ...(diagram.panels || []).map(renderPanel),
    ...diagram.routes.map(renderRoute),
    ...diagram.nodes.map(renderNode),
    footer(120, diagram.height - 88, diagram.width - 240, diagram.source),
  ].join("\n");
  return base(diagram.width, diagram.height, diagram.title, diagram.subtitle, body);
}

function classCard(id, x, y, w, h, title, stereo, members, color) {
  return { id, x, y, w, h, title, stereo, members, color, shape: "class" };
}

function card(id, x, y, w, h, title, members, color) {
  return { id, x, y, w, h, title, members, color, shape: "card" };
}

function note(id, x, y, w, h, title, color) {
  return { id, x, y, w, h, title, members: [], color, shape: "note" };
}

function panel(id, x, y, w, h, title) {
  return { id, x, y, w, h, title };
}

function route(from, to, d, color, kind) {
  return { from, to, d, color, kind };
}

function renderPanel(item) {
  return `<g id="${item.id}"><rect class="panel" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="20"/><text class="panelTitle" x="${item.x + 28}" y="${item.y + 36}">${esc(item.title)}</text></g>`;
}

function renderNode(item) {
  if (item.shape === "class") return renderClassCard(item);
  if (item.shape === "note") return renderNote(item);
  return renderCard(item);
}

function renderClassCard(item) {
  const color = palette[item.color] || palette.gray;
  const headerH = 70;
  const lineH = 20;
  const memberBlockH = item.members.length * lineH;
  const memberStart = item.y + headerH + Math.max(22, Math.round((item.h - headerH - memberBlockH) / 2) + 12);
  return `<g id="${item.id}">
  <rect class="classCard" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="10" fill="${color[0]}" stroke="${color[1]}"/>
  <path d="M${item.x} ${item.y + headerH} L${item.x + item.w} ${item.y + headerH}" stroke="${color[1]}" stroke-width="1.4"/>
  <text class="stereo" x="${item.x + item.w / 2}" y="${item.y + 22}" text-anchor="middle">${esc(item.stereo)}</text>
  <text class="classTitle" x="${item.x + item.w / 2}" y="${item.y + 50}" text-anchor="middle">${esc(item.title)}</text>
  ${item.members.map((line, index) => `<text class="member" x="${item.x + 18}" y="${memberStart + index * lineH}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function renderCard(item) {
  const color = palette[item.color] || palette.gray;
  const blockH = 28 + item.members.length * 18;
  const titleY = Math.round(item.y + item.h / 2 - blockH / 2 + 20);
  return `<g id="${item.id}">
  <rect class="card" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="10" fill="${color[0]}" stroke="${color[1]}"/>
  <text class="cardTitle" x="${item.x + item.w / 2}" y="${titleY}" text-anchor="middle" dominant-baseline="middle">${esc(item.title)}</text>
  ${item.members.map((line, index) => `<text class="detail" x="${item.x + item.w / 2}" y="${titleY + 29 + index * 18}" text-anchor="middle" dominant-baseline="middle">${esc(line)}</text>`).join("\n")}
</g>`;
}

function renderNote(item) {
  const color = palette[item.color] || palette.gray;
  return `<g id="${item.id}">
  <rect class="note" x="${item.x}" y="${item.y}" width="${item.w}" height="${item.h}" rx="12" fill="#FFFFFF" stroke="${color[1]}"/>
  <text class="detail" x="${item.x + item.w / 2}" y="${item.y + item.h / 2 + 1}" text-anchor="middle" dominant-baseline="middle">${esc(item.title)}</text>
</g>`;
}

function renderRoute(item) {
  const color = (palette[item.color] || palette.gray)[2];
  const klass = item.kind === "inherit" ? "inherit" : item.kind === "flow" ? "flow" : "dependency";
  const dash = klass === "dependency" ? ` stroke-dasharray="8 7"` : "";
  return `<path class="${klass}" data-from="${item.from}" data-to="${item.to}" d="${item.d}" stroke="${color}"${dash}/>`;
}

function base(width, height, title, subtitle, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title)}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/></filter>
  <marker id="arrow" viewBox="0 0 5 5" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="context-stroke"/></marker>
  <marker id="inheritArrow" markerWidth="8" markerHeight="7" refX="7" refY="3.5" orient="auto" markerUnits="strokeWidth"><path d="M 1 1 L 7 3.5 L 1 6 Z" fill="#fff" stroke="context-stroke" stroke-width="1.4"/></marker>
  <style>
    .canvas{fill:#F6F9FC}.frame{fill:#fff;stroke:#C7D7E7;stroke-width:3;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:43px;fill:#22344A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#536476}
    .panel{fill:#F7FBFF;stroke:#D6E3EF;stroke-width:2}.panelTitle{font-family:"Architects Daughter";font-size:24px;fill:#31445A;paint-order:stroke;stroke:#F7FBFF;stroke-width:5px;stroke-linejoin:round}
    .card,.classCard{filter:url(#shadow);stroke-width:2}.cardTitle,.classTitle{font-family:"Architects Daughter";font-size:23px;fill:#22344A}.stereo{font-family:"Comic Mono";font-size:10px;fill:#627184}.member{font-family:"Comic Mono";font-size:13px;fill:#102033}.detail{font-family:"Comic Mono";font-size:13px;fill:#42556B}
    .note{stroke-width:1.6}.flow{fill:none;stroke-width:2.7;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}.dependency{fill:none;stroke-width:2.35;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}.inherit{fill:none;stroke-width:2.25;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#inheritArrow)}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="32" y="28" width="${width - 64}" height="${height - 56}" rx="30"/>
<text class="title" x="68" y="84">${esc(title)}</text>
<text class="subtitle" x="72" y="116">${esc(subtitle)}</text>
${body}
</svg>
`;
}

function footer(x, y, w, text) {
  return `<g><rect class="note" x="${x}" y="${y}" width="${w}" height="42" rx="12" fill="#FFFFFF" stroke="#D6E3EF"/><text class="detail" x="${x + w / 2}" y="${y + 26}" text-anchor="middle">${esc(text)}</text></g>`;
}

function cleanSvg(svg) {
  return `${svg.replace(/[ \t]+$/gm, "").trimEnd()}\n`;
}

function esc(value) {
  return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function escDot(value) {
  return String(value).replaceAll("\\", "\\\\").replaceAll('"', '\\"');
}
