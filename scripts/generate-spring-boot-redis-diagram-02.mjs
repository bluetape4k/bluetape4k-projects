#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "spring-boot/redis/README.md",
  "spring-boot/redis/README.ko.md",
  "spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupport.kt",
  "spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt",
  "spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt",
  "spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializer.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /ReactiveRedisTemplate Serialization Flow[\s\S]*spring-boot-redis-diagram-02\.png/, "README reactive serialization flow slot");
assertContains(sources[2], /redisSerializationContextOf[\s\S]*key\(StringRedisSerializer\.UTF_8\)[\s\S]*hashValue\(valueSerializer\)/, "String-key context helper");
assertContains(sources[3], /val LZ4Kryo[\s\S]*RedisBinarySerializer\(BinarySerializers\.LZ4Kryo\)/, "LZ4Kryo serializer factory");
assertContains(sources[4], /override fun serialize\(t: Any\?\)[\s\S]*serializer\.serialize\(it\)[\s\S]*override fun deserialize/, "binary serializer write/read");
assertContains(sources[5], /override fun serialize\(value: ByteArray\?\)[\s\S]*compressor\.compress\(it\)[\s\S]*compressor\.decompress/, "compress-only serializer write/read");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  slate: ["#F8FAFC", "#64748B", "#475569"],
  violet: ["#F5F3FF", "#7C3AED", "#6D28D9"],
};

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function markerDefs() {
  return Object.entries(palette).map(([name, [, , dark]]) => `
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 3 3 L 19 11 L 3 19 Z" fill="${dark}"/></marker>`).join("\n");
}

function card({ id, x, y, w, h, color, title, kicker, lines = [], footer = "" }) {
  const [fill, stroke, dark] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="kicker" x="${x + 24}" y="${y + 31}">${esc(kicker)}</text>
  <text class="cardTitle" x="${x + 24}" y="${y + 65}">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 86}H${x + w}" stroke="${dark}"/>
  ${lines.map((line, index) => `<text class="body" x="${x + 24}" y="${y + 119 + index * 24}">${esc(line)}</text>`).join("\n")}
  ${footer ? `<path class="divider" d="M${x} ${y + h - 48}H${x + w}" stroke="${dark}"/><text class="foot" x="${x + 24}" y="${y + h - 18}">${esc(footer)}</text>` : ""}
</g>`;
}

function laneLabel({ x, y, text, color }) {
  const [, , dark] = palette[color];
  return `<text class="laneLabel" x="${x}" y="${y}" fill="${dark}">${esc(text)}</text>`;
}

function layer({ id, x, y, w, h, color, title }) {
  const [fill, stroke, dark] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="layer" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="layerTitle" x="${x + 22}" y="${y + 29}" fill="${dark}">${esc(title)}</text>
</g>`;
}

function edge({ from, to, points, color, dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  const labelWidth = label ? Math.max(110, label.length * 8 + 24) : 0;
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>
  ${label ? `<rect class="edgeLabelBg" x="${p[0] - 8}" y="${p[1] - 17}" width="${labelWidth}" height="24" rx="4"/><text class="edgeLabel" x="${p[0]}" y="${p[1]}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2700;
const height = 1460;
const body = [
  layer({ id: "ConfigLayer", x: 76, y: 165, w: 2420, h: 330, color: "teal", title: "configuration path" }),
  layer({ id: "WriteLayer", x: 76, y: 615, w: 2420, h: 320, color: "amber", title: "write path" }),
  layer({ id: "ReadLayer", x: 76, y: 1050, w: 2420, h: 320, color: "blue", title: "read path" }),
  card({
    id: "ConfigCode",
    x: 110,
    y: 220,
    w: 430,
    h: 230,
    color: "teal",
    kicker: "Application @Bean",
    title: "ReactiveRedisTemplate config",
    lines: ["redisSerializationContextOf(...)", "valueSerializer = LZ4Kryo", "or builder { value(...); hashValue(...) }"],
    footer: "supplies the context before template construction",
  }),
  card({
    id: "ContextBuilder",
    x: 700,
    y: 200,
    w: 520,
    h: 270,
    color: "green",
    kicker: "RedisSerializationContextSupport",
    title: "RedisSerializationContext<K,V>",
    lines: ["key -> StringRedisSerializer.UTF_8", "value -> RedisSerializer<V>", "hashKey -> StringRedisSerializer.UTF_8", "hashValue -> RedisSerializer<V>"],
    footer: "default serializer is optional",
  }),
  card({
    id: "ReactiveTemplate",
    x: 1380,
    y: 220,
    w: 470,
    h: 230,
    color: "blue",
    kicker: "Spring Data Redis",
    title: "ReactiveRedisTemplate",
    lines: ["created with connection factory", "uses context slots per operation", "returns reactive publisher results"],
    footer: "template does not choose binary format by itself",
  }),
  card({
    id: "RedisConnection",
    x: 2040,
    y: 220,
    w: 410,
    h: 230,
    color: "slate",
    kicker: "Redis connection",
    title: "Redis wire protocol",
    lines: ["stores ByteArray payloads", "keys are String UTF-8 in helper overload", "values come from selected serializer"],
    footer: "Redis sees bytes, not object graphs",
  }),

  card({
    id: "DomainValue",
    x: 110,
    y: 680,
    w: 380,
    h: 210,
    color: "amber",
    kicker: "caller value",
    title: "Domain object",
    lines: ["Any value passed to opsForValue()", "or hash value operation", "null serializes to emptyByteArray"],
    footer: "example: TestBean",
  }),
  card({
    id: "WriteSlot",
    x: 610,
    y: 680,
    w: 420,
    h: 210,
    color: "green",
    kicker: "context slot",
    title: "value / hashValue serializer",
    lines: ["selected from RedisBinarySerializers", "same slot used by reactive operations", "key serializer stays separate"],
    footer: "source of the write codec",
  }),
  card({
    id: "LZ4Kryo",
    x: 1150,
    y: 680,
    w: 420,
    h: 210,
    color: "violet",
    kicker: "lazy singleton",
    title: "RedisBinarySerializers.LZ4Kryo",
    lines: ["wraps BinarySerializers.LZ4Kryo", "created as RedisBinarySerializer", "combines Kryo encoding and LZ4"],
    footer: "one of many factory constants",
  }),
  card({
    id: "WriteCodec",
    x: 1690,
    y: 680,
    w: 420,
    h: 210,
    color: "pink",
    kicker: "RedisBinarySerializer",
    title: "serialize(t)",
    lines: ["serializer.serialize(t)", "binary serializer owns object encoding", "compression is inside combined serializer"],
    footer: "returns ByteArray",
  }),
  card({
    id: "StoredBytes",
    x: 2230,
    y: 680,
    w: 260,
    h: 210,
    color: "slate",
    kicker: "Redis payload",
    title: "ByteArray",
    lines: ["written through connection", "opaque to Redis", "decoded only by matching serializer"],
    footer: "stored value",
  }),

  card({
    id: "StoredBytesRead",
    x: 2230,
    y: 1110,
    w: 260,
    h: 210,
    color: "slate",
    kicker: "Redis payload",
    title: "ByteArray",
    lines: ["loaded by reactive command", "same bytes written earlier", "may be emptyByteArray"],
    footer: "wire value",
  }),
  card({
    id: "ReadCodec",
    x: 1690,
    y: 1110,
    w: 420,
    h: 210,
    color: "pink",
    kicker: "RedisBinarySerializer",
    title: "deserialize(bytes)",
    lines: ["serializer.deserialize(bytes)", "decompresses when combined codec needs it", "rebuilds the object graph"],
    footer: "returns Any?",
  }),
  card({
    id: "ReactiveResult",
    x: 1110,
    y: 1110,
    w: 460,
    h: 210,
    color: "blue",
    kicker: "reactive result",
    title: "Mono / Flux value",
    lines: ["template maps ByteArray to V", "caller observes typed result", "serializer mismatch fails at decode time"],
    footer: "same context closes the loop",
  }),
  card({
    id: "CompressOnly",
    x: 110,
    y: 1110,
    w: 760,
    h: 210,
    color: "amber",
    kicker: "ByteArray alternative",
    title: "RedisCompressSerializer path",
    lines: ["RedisBinarySerializers.LZ4 / Gzip / Snappy / Zstd create compression-only serializers", "use when application value type is already ByteArray", "serialize -> compressor.compress, deserialize -> compressor.decompress"],
    footer: "not the object serializer path used by LZ4Kryo",
  }),

  edge({ from: "ConfigCode", to: "ContextBuilder", points: [[540, 335], [700, 335]], color: "teal", label: "builds", labelAt: [590, 314] }),
  edge({ from: "ContextBuilder", to: "ReactiveTemplate", points: [[1220, 335], [1380, 335]], color: "teal", label: "constructor argument", labelAt: [1255, 314] }),
  edge({ from: "ReactiveTemplate", to: "RedisConnection", points: [[1850, 335], [2040, 335]], color: "teal", label: "uses", labelAt: [1910, 314] }),

  edge({ from: "DomainValue", to: "WriteSlot", points: [[490, 785], [610, 785]], color: "amber", label: "operation value", labelAt: [512, 764] }),
  edge({ from: "WriteSlot", to: "LZ4Kryo", points: [[1030, 785], [1150, 785]], color: "amber", label: "selected codec", labelAt: [1052, 764] }),
  edge({ from: "LZ4Kryo", to: "WriteCodec", points: [[1570, 785], [1690, 785]], color: "amber", label: "delegates", labelAt: [1594, 764] }),
  edge({ from: "WriteCodec", to: "StoredBytes", points: [[2110, 785], [2230, 785]], color: "amber", label: "ByteArray", labelAt: [2142, 764] }),
  edge({ from: "StoredBytes", to: "RedisConnection", points: [[2360, 680], [2360, 450]], color: "amber", dashed: true, label: "stored in Redis", labelAt: [2378, 575] }),

  edge({ from: "RedisConnection", to: "StoredBytesRead", points: [[2450, 335], [2535, 335], [2535, 1215], [2490, 1215]], color: "blue", dashed: true, label: "loaded bytes", labelAt: [2506, 1010] }),
  edge({ from: "StoredBytesRead", to: "ReadCodec", points: [[2230, 1215], [2110, 1215]], color: "blue", label: "ByteArray", labelAt: [2142, 1194] }),
  edge({ from: "ReadCodec", to: "ReactiveResult", points: [[1690, 1215], [1570, 1215]], color: "blue", label: "decoded object", labelAt: [1588, 1194] }),
  edge({ from: "ReactiveResult", to: "WriteSlot", points: [[1340, 1110], [1340, 1015], [820, 1015], [820, 890]], color: "blue", dashed: true, label: "same serializer slot", labelAt: [890, 996] }),
  edge({ from: "CompressOnly", to: "WriteSlot", points: [[490, 1110], [490, 1000], [820, 1000], [820, 890]], color: "amber", dashed: true, label: "ByteArray-only option", labelAt: [520, 980] }),
];

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="ReactiveRedisTemplate Serialization Flow Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .layer{fill-opacity:.30;stroke-width:1.6;stroke-dasharray:10 8}.layerTitle{font-family:"Comic Mono";font-size:15px;font-weight:700;letter-spacing:0}
    .title{font-family:"Architects Daughter";font-size:47px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .card{stroke-width:1.8;filter:url(#shadow)}.kicker{font-family:"Comic Mono";font-size:14px;fill:#475569}.cardTitle{font-family:"Architects Daughter";font-size:25px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:14px;fill:#334155}.foot{font-family:"Comic Mono";font-size:13px;fill:#475569}.divider{stroke-width:1.1;opacity:.42}
    .laneLabel{font-family:"Comic Mono";font-size:16px;font-weight:700;letter-spacing:0}
    .edge{fill:none;stroke-width:3.6;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabelBg{fill:#FFFFFF;stroke:#E2E8F0;stroke-width:.8;opacity:.94}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">ReactiveRedisTemplate Serialization Flow</text>
<text class="subtitle" x="76" y="120">How bluetape4k RedisSerializationContext helpers bind serializer slots, then encode writes and decode reads for Spring Data Redis.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "spring-boot-redis-diagram-02.svg");
const pngPath = join(OUT, "spring-boot-redis-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated spring-boot-redis-diagram-02.svg/png");
