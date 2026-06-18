import { readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-redis-diagram-02.svg";
const W = 1700;
const H = 1180;
const redisIcon = Buffer.from(
  readFileSync("/Users/debop/work/bluetape4k/bluetape4k-wiki/docs/icons/redis/redis-logo.svg", "utf8"),
).toString("base64");

const c = {
  ink: "#1F2937",
  muted: "#52616B",
  border: "#D5E1EC",
  blue: "#356FEA",
  green: "#16A064",
  purple: "#8B5CF6",
  orange: "#EA580C",
  pink: "#DB2777",
  teal: "#0F9B8E",
};

const lines = [];
const esc = (s) => s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

function marker(id, color, open = false) {
  lines.push(`<marker id="${id}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 9 5 L 1 9" fill="${open ? "none" : color}" stroke="${color}" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>`);
}

function group(x, y, w, h, title, sub, color, dashed = false) {
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="18" fill="#FFFFFF" stroke="${color}" stroke-width="2.2"${dashed ? ' stroke-dasharray="10 8"' : ""}/>`);
  lines.push(`<text x="${x + 24}" y="${y + 36}" class="groupTitle">${esc(title)}</text>`);
  lines.push(`<text x="${x + 24}" y="${y + 62}" class="groupSub">${esc(sub)}</text>`);
}

function card({ x, y, w, h, title, lines: body, fill, stroke, kind = "object" }) {
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="13" fill="${fill}" stroke="${stroke}" stroke-width="2.2"/>`);
  lines.push(`<text x="${x + w / 2}" y="${y + 32}" text-anchor="middle" class="stereo">&lt;&lt;${esc(kind)}&gt;&gt;</text>`);
  lines.push(`<text x="${x + w / 2}" y="${y + 60}" text-anchor="middle" class="cardTitle">${esc(title)}</text>`);
  body.forEach((line, i) => {
    lines.push(`<text x="${x + 18}" y="${y + 91 + i * 21}" class="member">${esc(line)}</text>`);
  });
}

function note(x, y, w, h, title, body, color) {
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="14" fill="#FFFFFF" stroke="${color}" stroke-width="2.2"/>`);
  lines.push(`<text x="${x + 24}" y="${y + 37}" class="noteTitle">${esc(title)}</text>`);
  body.forEach((line, i) => lines.push(`<text x="${x + 24}" y="${y + 68 + i * 22}" class="note">${esc(line)}</text>`));
}

function edge(id, d, color, dashed = false, width = 3.8) {
  lines.push(`<path d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"${dashed ? ' stroke-dasharray="10 8"' : ""} marker-end="url(#${id})"/>`);
}

lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
lines.push(`<title id="title">Redis exported API surface</title>`);
lines.push(`<desc id="desc">The redis umbrella has no source classes; it exports Lettuce and Redisson APIs, while Spring Boot Redis serializers stay separate.</desc>`);
lines.push(`<defs><style>
  .title{font-family:"Architects Daughter";font-size:48px;fill:${c.ink}}
  .subtitle,.groupSub,.member,.note,.stereo{font-family:"Comic Mono";fill:${c.muted}}
  .subtitle{font-size:18px}.groupTitle{font-family:"Architects Daughter";font-size:25px;fill:${c.ink}}
  .groupSub{font-size:14px}.cardTitle{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}
  .stereo{font-size:12px}.member{font-size:14px}.noteTitle{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}.note{font-size:14px}
</style></defs>`);
marker("blue", c.blue);
marker("green", c.green);
marker("purple", c.purple);
marker("orange", c.orange, true);
marker("pink", c.pink, true);

lines.push(`<rect width="${W}" height="${H}" fill="#F7FAFC"/>`);
lines.push(`<rect x="30" y="28" width="${W - 60}" height="${H - 56}" rx="24" fill="#FFFFFF" stroke="${c.border}" stroke-width="2.4"/>`);
lines.push(`<text x="${W / 2}" y="82" text-anchor="middle" class="title">Redis Exported API Surface</text>`);
lines.push(`<text x="${W / 2}" y="116" text-anchor="middle" class="subtitle">bluetape4k-redis has no Kotlin sources; it preserves imports by exporting concrete Redis client modules.</text>`);

note(560, 154, 580, 96, "bluetape4k-redis umbrella", ["build.gradle.kts exports only Lettuce and Redisson", "no classes are inherited from this module"], c.blue);

group(80, 300, 720, 498, "Exported by umbrella: Lettuce", "Low-level Redis command access, codecs, data structures, and coroutine adapters.", c.green);
group(900, 300, 720, 498, "Exported by umbrella: Redisson", "Redisson client creation, codecs, coroutine adapters, cache helpers, and NearCache.", c.purple);
group(282, 840, 1136, 222, "Separate Spring Boot Redis serializer module", "Use this module explicitly when configuring RedisTemplate or ReactiveRedisTemplate.", c.orange, true);

card({
  x: 120,
  y: 392,
  w: 300,
  h: 170,
  title: "LettuceClients",
  kind: "object",
  fill: "#ECFDF5",
  stroke: c.green,
  lines: ["clientOf(), connect()", "commands(), asyncCommands()", "coroutinesCommands()", "cached connections"],
});
card({
  x: 460,
  y: 392,
  w: 300,
  h: 170,
  title: "Lettuce Codecs",
  kind: "objects",
  fill: "#F0FDF4",
  stroke: c.green,
  lines: ["LettuceBinaryCodecs", "LettuceJsonCodecs", "Int / Long codecs", "compression variants"],
});
card({
  x: 120,
  y: 596,
  w: 300,
  h: 162,
  title: "Coroutine adapters",
  kind: "extensions",
  fill: "#F0FDFA",
  stroke: c.teal,
  lines: ["RedisFuture.awaitSuspending()", "Collection<RedisFuture>.awaitAll()", "Iterable<RedisFuture>.sequence()"],
});
card({
  x: 460,
  y: 596,
  w: 300,
  h: 162,
  title: "Lettuce structures",
  kind: "classes",
  fill: "#F0FDFA",
  stroke: c.teal,
  lines: ["LettuceMap / SuspendMap", "Lock / Semaphore", "Bloom / Cuckoo filters", "HyperLogLog"],
});

card({
  x: 940,
  y: 392,
  w: 300,
  h: 170,
  title: "Redisson client DSL",
  kind: "functions",
  fill: "#F3E8FF",
  stroke: c.purple,
  lines: ["redissonClient {}", "redissonClientOf(config)", "YAML Config helpers", "high-concurrency defaults"],
});
card({
  x: 1280,
  y: 392,
  w: 300,
  h: 170,
  title: "RedissonCodecs",
  kind: "object",
  fill: "#FAF5FF",
  stroke: c.purple,
  lines: ["Kryo5 / Fory / Jdk", "LZ4 / Zstd / Gzip", "Jackson3 / Fastjson2", "cache codec helpers"],
});
card({
  x: 940,
  y: 596,
  w: 300,
  h: 162,
  title: "Coroutine adapters",
  kind: "extensions",
  fill: "#EEF2FF",
  stroke: "#6366F1",
  lines: ["Collection<RFuture>.awaitAll()", "Iterable<RFuture>.sequence()", "suspend batch / transaction helpers"],
});
card({
  x: 1280,
  y: 596,
  w: 300,
  h: 162,
  title: "Cache surfaces",
  kind: "classes",
  fill: "#EEF2FF",
  stroke: "#6366F1",
  lines: ["RedissonNearCache", "RedissonCacheConfig", "MapCacheSupport", "LocalCacheMapSupport"],
});

card({
  x: 342,
  y: 930,
  w: 330,
  h: 74,
  title: "RedisBinarySerializers",
  kind: "object",
  fill: "#FFF7ED",
  stroke: c.orange,
  lines: [],
});
card({
  x: 735,
  y: 930,
  w: 330,
  h: 74,
  title: "redisSerializationContext",
  kind: "function",
  fill: "#FFF7ED",
  stroke: c.orange,
  lines: [],
});
card({
  x: 1128,
  y: 930,
  w: 230,
  h: 74,
  title: "RedisTemplate",
  kind: "Spring API",
  fill: "#FFF7ED",
  stroke: c.orange,
  lines: [],
});

lines.push(`<rect x="642" y="1086" width="416" height="54" rx="16" fill="#FFF1F2" stroke="${c.pink}" stroke-width="2.2"/>`);
lines.push(`<image x="666" y="1095" width="36" height="36" href="data:image/svg+xml;base64,${redisIcon}"/>`);
lines.push(`<text x="724" y="1121" class="noteTitle">Redis runtime remains shared</text>`);

edge("blue", "M 650 250 L 840 250 L 840 520 L 804 520", c.blue);
edge("blue", "M 1050 250 L 860 250 L 860 520 L 896 520", c.blue);
edge("green", "M 270 562 L 270 586", c.green);
edge("green", "M 610 562 L 610 586", c.green);
edge("purple", "M 1090 562 L 1090 586", c.purple);
edge("purple", "M 1430 562 L 1430 586", c.purple);

lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
