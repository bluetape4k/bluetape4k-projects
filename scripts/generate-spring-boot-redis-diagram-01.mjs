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
  "spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt",
  "spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializer.kt",
  "spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt",
  "spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupport.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Redis Serializer Class Hierarchy[\s\S]*spring-boot-redis-diagram-01\.png/, "README serializer hierarchy slot");
assertContains(sources[2], /class RedisBinarySerializer[\s\S]*:\s*RedisSerializer<Any>[\s\S]*BinarySerializer[\s\S]*serialize\(t:\s*Any\?\)/, "RedisBinarySerializer contract");
assertContains(sources[3], /class RedisCompressSerializer[\s\S]*:\s*RedisSerializer<ByteArray>[\s\S]*Compressor[\s\S]*compressor\.compress/, "RedisCompressSerializer contract");
assertContains(sources[4], /object RedisBinarySerializers[\s\S]*val Jdk[\s\S]*val LZ4[\s\S]*val ZstdFory/, "RedisBinarySerializers factory constants");
assertContains(sources[5], /redisSerializationContext[\s\S]*redisSerializationContextOf[\s\S]*StringRedisSerializer\.UTF_8/, "RedisSerializationContext helpers");

const palette = {
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  slate: ["#F8FAFC", "#64748B", "#475569"],
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
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 3 3 L 19 11 L 3 19 Z" fill="${dark}"/></marker>
  <marker id="triangle-${name}" markerWidth="24" markerHeight="22" refX="20" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 3 3 L 20 11 L 3 19 Z" fill="#FFFFFF" stroke="${dark}" stroke-width="2"/></marker>`).join("\n");
}

function classBox({ id, x, y, w, h, color, stereotype, title, attrs = [], methods = [] }) {
  const [fill, stroke, dark] = palette[color];
  const attrY = y + 76;
  const methodY = attrY + 34 + Math.max(24, attrs.length * 22);
  return `<g id="${esc(id)}">
  <rect class="classBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="stereotype" x="${x + w / 2}" y="${y + 28}" text-anchor="middle">${esc(stereotype)}</text>
  <text class="classTitle" x="${x + w / 2}" y="${y + 58}" text-anchor="middle">${esc(title)}</text>
  <path class="divider" d="M${x} ${attrY}H${x + w}" stroke="${dark}"/>
  ${attrs.map((line, index) => `<text class="member" x="${x + 24}" y="${attrY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
  <path class="divider" d="M${x} ${methodY}H${x + w}" stroke="${dark}"/>
  ${methods.map((line, index) => `<text class="member" x="${x + 24}" y="${methodY + 26 + index * 22}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function edge({ from, to, points, color, marker = "arrow", dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  const labelWidth = label ? Math.max(120, label.length * 8 + 24) : 0;
  const labelX = p[0] + 8;
  const labelY = p[1] - 8;
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#${marker}-${color})"/>
  ${label ? `<rect class="edgeLabelBg" x="${labelX - 8}" y="${labelY - 17}" width="${labelWidth}" height="24" rx="4"/><text class="edgeLabel" x="${labelX}" y="${labelY}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2400;
const height = 1480;
const body = [
  classBox({
    id: "RedisSerializer",
    x: 820,
    y: 230,
    w: 650,
    h: 215,
    color: "slate",
    stereotype: "<<Spring Data interface>>",
    title: "RedisSerializer<T>",
    attrs: ["serialize(value): ByteArray", "deserialize(bytes): T?"],
    methods: ["Used by RedisTemplate and ReactiveRedisTemplate", "Key, value, hashKey, hashValue slots"],
  }),
  classBox({
    id: "RedisBinarySerializer",
    x: 240,
    y: 610,
    w: 620,
    h: 290,
    color: "green",
    stereotype: "<<class>>",
    title: "RedisBinarySerializer",
    attrs: ["serializer: BinarySerializer", "implements RedisSerializer<Any>"],
    methods: ["serialize(t): serializer.serialize(t)", "null serialize -> emptyByteArray", "deserialize(bytes): serializer.deserialize(bytes)", "factory invoke(bs: BinarySerializer)"],
  }),
  classBox({
    id: "RedisCompressSerializer",
    x: 1440,
    y: 610,
    w: 620,
    h: 290,
    color: "blue",
    stereotype: "<<class>>",
    title: "RedisCompressSerializer",
    attrs: ["compressor: Compressor", "implements RedisSerializer<ByteArray>"],
    methods: ["serialize(value): compressor.compress(value)", "null serialize -> emptyByteArray", "deserialize(bytes): compressor.decompress(bytes)", "default invoke(): Compressors.LZ4"],
  }),
  classBox({
    id: "RedisBinarySerializers",
    x: 215,
    y: 1065,
    w: 720,
    h: 255,
    color: "amber",
    stereotype: "<<object factory>>",
    title: "RedisBinarySerializers",
    attrs: ["lazy singleton constants", "object serializers and compression-only serializers"],
    methods: ["Jdk / Kryo / Fory", "Gzip/LZ4/Snappy/Zstd compressors", "Gzip/LZ4/Snappy/Zstd x Jdk/Kryo/Fory combos"],
  }),
  classBox({
    id: "ContextSupport",
    x: 1290,
    y: 1065,
    w: 800,
    h: 255,
    color: "teal",
    stereotype: "<<DSL functions>>",
    title: "RedisSerializationContextSupport",
    attrs: ["redisSerializationContext { ... }", "redisSerializationContextOf(...)"],
    methods: ["sets key/value/hash serializers", "String key overload uses StringRedisSerializer.UTF_8", "feeds RedisSerializationContext for ReactiveRedisTemplate"],
  }),
  edge({ from: "RedisBinarySerializer", to: "RedisSerializer", points: [[550, 610], [550, 515], [1030, 515], [1030, 445]], color: "green", marker: "triangle", dashed: true, label: "implements Any", labelAt: [650, 502] }),
  edge({ from: "RedisCompressSerializer", to: "RedisSerializer", points: [[1750, 610], [1750, 515], [1260, 515], [1260, 445]], color: "blue", marker: "triangle", dashed: true, label: "implements ByteArray", labelAt: [1450, 502] }),
  edge({ from: "RedisBinarySerializers", to: "RedisBinarySerializer", points: [[575, 1065], [575, 900]], color: "amber", dashed: true, label: "creates object serializers", labelAt: [593, 995] }),
  edge({ from: "RedisBinarySerializers", to: "RedisCompressSerializer", points: [[935, 1244], [1120, 1244], [1120, 755], [1440, 755]], color: "amber", dashed: true, label: "creates compression serializers", labelAt: [1138, 1006] }),
  edge({ from: "ContextSupport", to: "RedisSerializer", points: [[1690, 1065], [1690, 980], [2140, 980], [2140, 330], [1470, 330]], color: "teal", dashed: true, label: "accepts serializers", labelAt: [2158, 700] }),
  edge({ from: "RedisBinarySerializers", to: "ContextSupport", points: [[575, 1320], [575, 1390], [1690, 1390], [1690, 1320]], color: "teal", dashed: true, label: "typical value serializer", labelAt: [940, 1377] }),
];

const svg = `<svg data-intent="Explain Spring Boot Redis serializer class hierarchy for README diagram 01." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Redis Serializer Class Hierarchy Diagram">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:46px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .classBox{stroke-width:1.8;filter:url(#shadow)}.stereotype{font-family:"Comic Mono";font-size:14px;fill:#475569}.classTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}
    .member{font-family:"Comic Mono";font-size:14px;fill:#334155}.divider{stroke-width:1.1;opacity:.45}
    .edge{fill:none;stroke-width:3.6;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabelBg{fill:#FFFFFF;stroke:#E2E8F0;stroke-width:.8;opacity:.94}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#475569}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="86">Redis Serializer Class Hierarchy</text>
<text class="subtitle" x="76" y="120">Spring Data Redis serializer implementations backed by bluetape4k binary serializers, compressors, lazy factories, and RedisSerializationContext DSL helpers.</text>
${body.join("\n")}
</svg>`;

const svgPath = join(OUT, "spring-boot-redis-diagram-01.svg");
const pngPath = join(OUT, "spring-boot-redis-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated spring-boot-redis-diagram-01.svg/png");
