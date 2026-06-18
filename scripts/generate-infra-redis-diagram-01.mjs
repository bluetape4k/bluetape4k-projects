import { readFileSync, writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-redis-diagram-01.svg";
const W = 1680;
const H = 1160;

const redisIcon = Buffer.from(
  readFileSync("/Users/debop/work/bluetape4k/bluetape4k-wiki/docs/icons/redis/redis-logo.svg", "utf8"),
).toString("base64");

const c = {
  ink: "#1F2937",
  muted: "#52616B",
  canvas: "#F7FAFC",
  frame: "#FFFFFF",
  border: "#D5E1EC",
  blue: "#356FEA",
  green: "#229E5D",
  teal: "#0F9B8E",
  amber: "#D97706",
  orange: "#EA580C",
  pink: "#DB2777",
};

const lines = [];
const esc = (s) => s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

function marker(id, color, open = false) {
  const fill = open ? "none" : color;
  lines.push(
    `<marker id="${id}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="13" markerHeight="13" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 9 5 L 1 9" fill="${fill}" stroke="${color}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>`,
  );
}

function textLines(x, y, items, cls = "sub", anchor = "middle", gap = 22) {
  items.forEach((item, i) => {
    lines.push(`<text x="${x}" y="${y + i * gap}" text-anchor="${anchor}" class="${cls}">${esc(item)}</text>`);
  });
}

function card({ x, y, w, h, fill, stroke, title, sub = [], icon = false, dashed = false }) {
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="16" fill="${fill}" stroke="${stroke}" stroke-width="2.8"${dashed ? ' stroke-dasharray="10 8"' : ""}/>`);
  const tx = icon ? x + 88 : x + w / 2;
  if (icon) {
    lines.push(`<image x="${x + 28}" y="${y + 28}" width="48" height="48" href="data:image/svg+xml;base64,${redisIcon}"/>`);
  }
  lines.push(`<text x="${tx}" y="${y + 42}" text-anchor="${icon ? "start" : "middle"}" class="cardTitle">${esc(title)}</text>`);
  textLines(tx, y + 72, sub, "sub", icon ? "start" : "middle", 22);
}

function layer(x, y, w, h, title, sub) {
  lines.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="20" fill="#FFFFFF" stroke="${c.border}" stroke-width="2.2"/>`);
  lines.push(`<text x="${x + 28}" y="${y + 42}" class="layerTitle">${esc(title)}</text>`);
  if (sub) lines.push(`<text x="${x + 28}" y="${y + 68}" class="layerSub">${esc(sub)}</text>`);
}

function path(id, d, color, dashed = false, width = 4.2) {
  const dash = dashed ? ' stroke-dasharray="10 8"' : "";
  lines.push(`<path d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"${dash} marker-end="url(#${id})"/>`);
}

lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
lines.push(`<title id="title">Redis umbrella module dependency structure</title>`);
lines.push(`<desc id="desc">bluetape4k-redis exports Lettuce and Redisson; Spring Data Redis serializers remain a separate dependency choice.</desc>`);
lines.push(`<defs><style>
  .title{font-family:"Architects Daughter";font-size:46px;fill:${c.ink}}
  .subtitle,.sub,.layerSub,.note{font-family:"Comic Mono";fill:${c.muted}}
  .subtitle{font-size:18px}.layerTitle{font-family:"Architects Daughter";font-size:25px;fill:${c.ink}}
  .layerSub{font-size:14px}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:${c.ink}}
  .sub{font-size:15px}.note{font-size:14px}.code{font-family:"Comic Mono";font-size:16px;fill:#334155}
</style></defs>`);
marker("arrowBlue", c.blue);
marker("arrowGreen", c.green);
marker("arrowTeal", c.teal);
marker("arrowOrange", c.orange, true);
marker("arrowPink", c.pink, true);

lines.push(`<rect width="${W}" height="${H}" fill="${c.canvas}"/>`);
lines.push(`<rect x="30" y="28" width="${W - 60}" height="${H - 56}" rx="24" fill="${c.frame}" stroke="${c.border}" stroke-width="2.4"/>`);
lines.push(`<text x="${W / 2}" y="82" text-anchor="middle" class="title">Redis Umbrella Module Dependency Structure</text>`);
lines.push(`<text x="${W / 2}" y="116" text-anchor="middle" class="subtitle">The umbrella artifact exports Lettuce and Redisson only; Spring Data Redis serializers are selected separately.</text>`);

layer(72, 160, 1536, 250, "Dependency chosen by an application", "Use the full bundle for migration compatibility, or depend on only one client module.");
card({
  x: 130,
  y: 270,
  w: 360,
  h: 110,
  fill: "#EBF2FF",
  stroke: c.blue,
  title: "bluetape4k-redis",
  sub: ["umbrella artifact", "api(project(:lettuce, :redisson))"],
});
card({
  x: 595,
  y: 270,
  w: 360,
  h: 110,
  fill: "#F0FDF4",
  stroke: c.green,
  title: "client-only choice",
  sub: ["depend on lettuce or redisson", "when the other client is unused"],
});
card({
  x: 1060,
  y: 270,
  w: 400,
  h: 110,
  fill: "#FFF7ED",
  stroke: c.orange,
  title: "Spring Redis serializers",
  sub: ["separate module", "not exported by the umbrella"],
  dashed: true,
});

layer(72, 440, 1536, 350, "Exported Redis client modules", "Both included modules talk to the same Redis backend but expose different client models.");
card({
  x: 160,
  y: 570,
  w: 440,
  h: 164,
  fill: "#ECFDF5",
  stroke: c.green,
  title: "bluetape4k-lettuce",
  sub: ["RedisClient and cached connections", "sync / async / coroutine commands", "RedisFuture.awaitSuspending()", "binary, JSON, and Protobuf codecs"],
});
card({
  x: 650,
  y: 570,
  w: 440,
  h: 164,
  fill: "#F3E8FF",
  stroke: "#8B5CF6",
  title: "bluetape4k-redisson",
  sub: ["redissonClient DSL", "RFuture coroutine adapters", "map/cache helpers", "RedissonNearCache"],
});
card({
  x: 1140,
  y: 570,
  w: 370,
  h: 142,
  fill: "#FFF7ED",
  stroke: c.orange,
  title: "spring-boot/redis",
  sub: ["RedisBinarySerializers", "redisSerializationContext()", "use explicitly with templates"],
  dashed: true,
});

layer(72, 850, 1536, 190, "Runtime backend boundary", "The dependency choice changes client APIs, not the Redis server contract.");
card({
  x: 445,
  y: 922,
  w: 790,
  h: 100,
  fill: "#FFF1F2",
  stroke: c.pink,
  title: "Redis server / cache backend",
  sub: ["keys, maps, streams, locks, and cached values", "template data shares the same Redis runtime"],
  icon: true,
});

path("arrowBlue", "M 310 380 L 310 430 L 380 430 L 380 566", c.blue);
path("arrowBlue", "M 310 380 L 310 422 L 870 422 L 870 566", c.blue);
path("arrowGreen", "M 775 380 L 775 526 L 505 526 L 505 566", c.green, true, 3.4);
path("arrowGreen", "M 775 380 L 775 566", c.green, true, 3.4);
path("arrowOrange", "M 1260 380 L 1260 566", c.orange, true, 3.4);
path("arrowGreen", "M 380 734 L 380 814 L 700 814 L 700 918", c.green);
path("arrowTeal", "M 870 734 L 870 814 L 860 814 L 860 918", c.teal);
path("arrowOrange", "M 1325 712 L 1325 820 L 980 820 L 980 918", c.orange, true, 3.2);

lines.push(`<rect x="350" y="1076" width="980" height="42" rx="15" fill="#FFFFFF" stroke="${c.border}" stroke-width="1.8"/>`);
lines.push(`<text x="${W / 2}" y="1103" text-anchor="middle" class="note">Rule of thumb: use the umbrella for compatibility; use direct modules for leaner dependency graphs.</text>`);
lines.push(`</svg>`);

writeFileSync(out, `${lines.join("\n")}\n`);
console.log(`wrote ${out}`);
