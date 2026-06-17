#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";

const id = process.argv[2];
if (!["01", "02"].includes(id)) {
  console.error("Usage: node scripts/generate-spring-boot-redis-diagram.mjs 01|02");
  process.exit(1);
}

const root = resolve(dirname(new URL(import.meta.url).pathname), "..");
const outDir = resolve(root, "docs/images/readme-diagrams");
mkdirSync(outDir, { recursive: true });

const font = "'Architects Daughter', 'Comic Mono', 'Helvetica Neue', Arial, sans-serif";

function esc(value) {
  return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function lines(items, x, y, opts = {}) {
  const { size = 13, fill = "#475569", line = 18, anchor = "start", weight = 400 } = opts;
  return items.map((item, idx) => `<text x="${x}" y="${y + idx * line}" text-anchor="${anchor}" font-size="${size}" font-weight="${weight}" fill="${fill}">${esc(item)}</text>`).join("\n");
}

function classCard({ x, y, w, h, name, stereotype, attrs = [], ops = [], fill, stroke }) {
  const nameLines = String(name).split("\n");
  const headerH = 34 + nameLines.length * 21;
  const attrH = attrs.length ? Math.max(34, attrs.length * 18 + 18) : 0;
  const opY = y + headerH + attrH;
  return `
  <g class="card" data-card="${esc(nameLines.join(" "))}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    <text x="${x + w / 2}" y="${y + 21}" text-anchor="middle" font-size="12.5" fill="${stroke}">${esc(stereotype)}</text>
    ${lines(nameLines, x + w / 2, y + 44, { size: 17, weight: 700, fill: "#111827", line: 20, anchor: "middle" })}
    <line x1="${x}" y1="${y + headerH}" x2="${x + w}" y2="${y + headerH}" stroke="${stroke}" stroke-width="1.1" opacity="0.55"/>
    ${attrH ? `<line x1="${x}" y1="${opY}" x2="${x + w}" y2="${opY}" stroke="${stroke}" stroke-width="1.1" opacity="0.45"/>` : ""}
    ${attrs.length ? lines(attrs, x + 16, y + headerH + 23, { size: 12.8, fill: "#475569", line: 18 }) : ""}
    ${ops.length ? lines(ops, x + 16, opY + 23, { size: 12.8, fill: "#374151", line: 18 }) : ""}
  </g>`;
}

function flowCard({ x, y, w, h, title, body, fill, stroke }) {
  return `
  <g class="card" data-card="${esc(title)}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}" stroke-width="1.8"/>
    <rect x="${x}" y="${y}" width="10" height="${h}" rx="5" fill="${stroke}" opacity="0.9"/>
    <text x="${x + 26}" y="${y + 34}" font-size="18" font-weight="700" fill="#111827">${esc(title)}</text>
    ${lines(body, x + 26, y + 62, { size: 13.3, fill: "#475569", line: 19 })}
  </g>`;
}

function group({ x, y, w, h, title, note, fill, stroke }) {
  return `
  <g class="group" data-group="${esc(title)}">
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" stroke-width="1.3" stroke-dasharray="7 5"/>
    <text x="${x + 18}" y="${y + 26}" font-size="14" font-weight="700" fill="${stroke}">${esc(title)}</text>
    <text x="${x + 18}" y="${y + 47}" font-size="12.5" fill="#6b7280">${esc(note)}</text>
  </g>`;
}

function line({ d, color, marker, width = 2.6, dash = "" }) {
  return `<path class="edge" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"${dash ? ` stroke-dasharray="${dash}"` : ""} marker-end="url(#${marker})"/>`;
}

function defs() {
  return `
  <defs>
    <marker id="arrow-blue" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#2563eb" stroke="#2563eb" stroke-width="0" stroke-dasharray="none"/></marker>
    <marker id="arrow-green" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#16a34a" stroke="#16a34a" stroke-width="0" stroke-dasharray="none"/></marker>
    <marker id="arrow-orange" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#ea580c" stroke="#ea580c" stroke-width="0" stroke-dasharray="none"/></marker>
    <marker id="arrow-purple" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 0 0 L 10 5 L 0 10 Z" fill="#7c3aed" stroke="#7c3aed" stroke-width="0" stroke-dasharray="none"/></marker>
    <marker id="open-gray" markerUnits="userSpaceOnUse" markerWidth="13" markerHeight="13" viewBox="0 0 10 10" refX="9" refY="5" orient="auto"><path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="#64748b" stroke-width="1.7" stroke-dasharray="none" stroke-linecap="round" stroke-linejoin="round"/></marker>
    <marker id="hollow-blue" markerUnits="userSpaceOnUse" markerWidth="15" markerHeight="15" viewBox="0 0 12 12" refX="11" refY="6" orient="auto"><path d="M 1 1 L 11 6 L 1 11 Z" fill="#ffffff" stroke="#2563eb" stroke-width="1.5" stroke-dasharray="none"/></marker>
  </defs>`;
}

function diagram01() {
  const width = 1460;
  const height = 860;
  const cards = [
    classCard({ x: 355, y: 172, w: 330, h: 126, name: "RedisSerializer<T>", stereotype: "<<Spring Data interface>>", attrs: ["serialize(value): ByteArray", "deserialize(bytes): T?"], fill: "#eff6ff", stroke: "#2563eb" }),
    classCard({ x: 115, y: 382, w: 330, h: 172, name: "RedisBinarySerializer", stereotype: "<<RedisSerializer Any>>", attrs: ["BinarySerializer"], ops: ["serialize(null) -> emptyByteArray", "deserialize delegates to BinarySerializer"], fill: "#ecfdf5", stroke: "#16a34a" }),
    classCard({ x: 595, y: 382, w: 330, h: 172, name: "RedisCompressSerializer", stereotype: "<<RedisSerializer ByteArray>>", attrs: ["Compressor = LZ4 by default"], ops: ["compress ByteArray values", "decompress stored bytes"], fill: "#fff7ed", stroke: "#ea580c" }),
    classCard({ x: 1015, y: 382, w: 330, h: 172, name: "RedisBinarySerializers", stereotype: "<<object factory>>", attrs: ["lazy singleton serializers"], ops: ["Jdk, Kryo, Fory", "Gzip/LZ4/Snappy/Zstd variants", "compression-only serializers"], fill: "#f5f3ff", stroke: "#7c3aed" }),
    classCard({ x: 115, y: 634, w: 330, h: 128, name: "BinarySerializers", stereotype: "<<bluetape4k-io>>", attrs: ["Jdk/Kryo/Fory", "compressed binary combinations"], fill: "#f8fafc", stroke: "#64748b" }),
    classCard({ x: 595, y: 634, w: 330, h: 128, name: "Compressors", stereotype: "<<bluetape4k-io>>", attrs: ["GZip, LZ4, Snappy, Zstd"], fill: "#f8fafc", stroke: "#64748b" }),
    classCard({ x: 1015, y: 634, w: 330, h: 128, name: "Redis Context DSL", stereotype: "<<functions>>", attrs: ["redisSerializationContext {}", "redisSerializationContextOf(...)"], fill: "#eff6ff", stroke: "#2563eb" }),
  ];
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Spring Boot Redis serializer class structure">
  <style>text { font-family: ${font}; dominant-baseline: alphabetic; }</style>
  ${defs()}
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="72" y="62" font-size="30" font-weight="700" fill="#111827">Spring Boot Redis Serializer Class Structure</text>
  <text x="72" y="92" font-size="15" fill="#64748b">RedisSerializer adapters wrap bluetape4k binary serializers or compressors, while factory constants and context DSL wire them into Redis templates.</text>
  <text x="280" y="324" text-anchor="middle" font-size="14" font-weight="700" fill="#16a34a">object value adapter</text>
  <text x="760" y="324" text-anchor="middle" font-size="14" font-weight="700" fill="#ea580c">ByteArray compression adapter</text>
  <text x="1180" y="342" text-anchor="middle" font-size="14" font-weight="700" fill="#7c3aed">factory and context helpers</text>
  ${line({ d: "M280 382 L280 340 L465 340 L465 298", color: "#2563eb", marker: "hollow-blue", width: 2.2, dash: "7 5" })}
  ${line({ d: "M760 382 L760 340 L615 340 L615 298", color: "#2563eb", marker: "hollow-blue", width: 2.2, dash: "7 5" })}
  ${line({ d: "M280 634 L280 554", color: "#16a34a", marker: "arrow-green" })}
  ${line({ d: "M760 634 L760 554", color: "#ea580c", marker: "arrow-orange" })}
  ${line({ d: "M1180 554 L1180 634", color: "#2563eb", marker: "arrow-blue" })}
  ${cards.join("\n")}
  <g class="legend" transform="translate(72 822)">
    <line x1="0" y1="0" x2="38" y2="0" stroke="#2563eb" stroke-width="2.2" stroke-dasharray="7 5" marker-end="url(#hollow-blue)"/><text x="54" y="5" font-size="13" fill="#475569">implements RedisSerializer</text>
    <line x1="340" y1="0" x2="378" y2="0" stroke="#16a34a" stroke-width="2.6" marker-end="url(#arrow-green)"/><text x="394" y="5" font-size="13" fill="#475569">delegates object serialization</text>
    <line x1="660" y1="0" x2="698" y2="0" stroke="#ea580c" stroke-width="2.6" marker-end="url(#arrow-orange)"/><text x="714" y="5" font-size="13" fill="#475569">delegates compression</text>
    <line x1="940" y1="0" x2="978" y2="0" stroke="#2563eb" stroke-width="2.6" marker-end="url(#arrow-blue)"/><text x="994" y="5" font-size="13" fill="#475569">DSL entrypoint</text>
  </g>
</svg>`;
}

function diagram02() {
  const width = 1460;
  const height = 820;
  const cards = [
    flowCard({ x: 98, y: 230, w: 330, h: 120, title: "Configuration Code", body: ["redisSerializationContext {}", "or redisSerializationContextOf(...)", "choose key/value/hash serializers"], fill: "#eff6ff", stroke: "#2563eb" }),
    flowCard({ x: 555, y: 230, w: 350, h: 120, title: "Serialization Context", body: ["RedisSerializationContext<K, V>", "key, value, hashKey, hashValue", "built before template creation"], fill: "#ecfdf5", stroke: "#16a34a" }),
    flowCard({ x: 1032, y: 230, w: 330, h: 120, title: "ReactiveRedisTemplate", body: ["uses context for every operation", "String keys can use UTF_8 serializer", "values use chosen binary serializer"], fill: "#f5f3ff", stroke: "#7c3aed" }),
    flowCard({ x: 1032, y: 482, w: 330, h: 120, title: "Write Path", body: ["value -> RedisBinarySerializer", "BinarySerializer serializes/compresses", "Redis stores ByteArray"], fill: "#ecfdf5", stroke: "#16a34a" }),
    flowCard({ x: 555, y: 482, w: 350, h: 120, title: "Redis Storage", body: ["stored bytes are codec-specific", "compression-only variants keep ByteArray contract", "null serializes to emptyByteArray"], fill: "#f8fafc", stroke: "#64748b" }),
    flowCard({ x: 98, y: 482, w: 330, h: 120, title: "Decoded Read Result", body: ["ByteArray -> deserialize", "decompress when compressor is present", "returns domain value or ByteArray"], fill: "#fff7ed", stroke: "#ea580c" }),
  ];
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="ReactiveRedisTemplate serialization flow">
  <style>text { font-family: ${font}; dominant-baseline: alphabetic; }</style>
  ${defs()}
  <rect width="${width}" height="${height}" fill="#ffffff"/>
  <text x="72" y="62" font-size="30" font-weight="700" fill="#111827">ReactiveRedisTemplate Serialization Flow</text>
  <text x="72" y="92" font-size="15" fill="#64748b">The DSL builds a RedisSerializationContext; template operations then serialize writes and deserialize reads through the selected adapters.</text>
  ${group({ x: 60, y: 132, w: 406, h: 534, title: "APPLICATION CALL SITE", note: "Code selects serializers and receives decoded reads.", fill: "#eff6ff", stroke: "#2563eb" })}
  ${group({ x: 517, y: 132, w: 426, h: 534, title: "SERIALIZATION CONTEXT", note: "Context owns slots and Redis byte representation.", fill: "#ecfdf5", stroke: "#16a34a" })}
  ${group({ x: 994, y: 132, w: 406, h: 534, title: "TEMPLATE WRITE PATH", note: "Template applies the context to operations.", fill: "#f5f3ff", stroke: "#7c3aed" })}
  ${line({ d: "M428 290 L555 290", color: "#2563eb", marker: "arrow-blue", width: 3 })}
  ${line({ d: "M905 290 L1032 290", color: "#7c3aed", marker: "arrow-purple", width: 3 })}
  ${line({ d: "M1197 350 L1197 482", color: "#16a34a", marker: "arrow-green", width: 3 })}
  ${line({ d: "M1032 542 L905 542", color: "#16a34a", marker: "arrow-green", width: 3 })}
  ${line({ d: "M555 542 L428 542", color: "#ea580c", marker: "arrow-orange", width: 3 })}
  ${line({ d: "M263 482 L263 350", color: "#64748b", marker: "open-gray", width: 2.4, dash: "6 5" })}
  ${cards.join("\n")}
  <g class="legend" transform="translate(72 758)">
    <line x1="0" y1="0" x2="42" y2="0" stroke="#2563eb" stroke-width="3" marker-end="url(#arrow-blue)"/><text x="58" y="5" font-size="13" fill="#475569">context construction</text>
    <line x1="290" y1="0" x2="332" y2="0" stroke="#16a34a" stroke-width="3" marker-end="url(#arrow-green)"/><text x="348" y="5" font-size="13" fill="#475569">write serialization</text>
    <line x1="560" y1="0" x2="602" y2="0" stroke="#ea580c" stroke-width="3" marker-end="url(#arrow-orange)"/><text x="618" y="5" font-size="13" fill="#475569">read deserialization</text>
    <line x1="850" y1="0" x2="892" y2="0" stroke="#64748b" stroke-width="2.4" stroke-dasharray="6 5" marker-end="url(#open-gray)"/><text x="908" y="5" font-size="13" fill="#475569">read result returns</text>
  </g>
</svg>`;
}

const svg = id === "01" ? diagram01() : diagram02();
const svgPath = resolve(outDir, `spring-boot-redis-diagram-${id}.svg`);
const pngPath = resolve(outDir, `spring-boot-redis-diagram-${id}.png`);
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync("/Users/debop/.local/bin/cairosvg", [svgPath, "-o", pngPath, "-s", "2"], { stdio: "inherit" });
console.log(`generated ${svgPath}`);
console.log(`generated ${pngPath}`);
