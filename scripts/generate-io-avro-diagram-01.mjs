#!/usr/bin/env node

import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/io-avro-diagram-01.svg";
const W = 2320;
const H = 1420;

const esc = (s) =>
  String(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

const lines = [];
const add = (s) => lines.push(s);

function textLines(items, x, y, cls = "member", gap = 28, anchor = "start") {
  items.forEach((line, i) => {
    add(`<text class="${cls}" x="${x}" y="${y + i * gap}" text-anchor="${anchor}">${esc(line)}</text>`);
  });
}

function classBox(id, x, y, w, h, tone, stereotype, title, members = []) {
  add(`<g id="${id}" class="classBox ${tone}">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12"/>`);
  add(`<text class="stereo" x="${x + w / 2}" y="${y + 34}" text-anchor="middle">${esc(stereotype)}</text>`);
  add(`<text class="classTitle" x="${x + w / 2}" y="${y + 70}" text-anchor="middle">${esc(title)}</text>`);
  add(`<line class="divider" x1="${x}" y1="${y + 92}" x2="${x + w}" y2="${y + 92}"/>`);
  textLines(members, x + 30, y + 125);
  add(`</g>`);
}

function supportBox(id, x, y, w, h, tone, title, members = []) {
  add(`<g id="${id}" class="supportBox ${tone}">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12"/>`);
  add(`<text class="supportTitle" x="${x + w / 2}" y="${y + 48}" text-anchor="middle">${esc(title)}</text>`);
  add(`<line class="divider" x1="${x}" y1="${y + 70}" x2="${x + w}" y2="${y + 70}"/>`);
  textLines(members, x + 30, y + 103);
  add(`</g>`);
}

function edge(id, d, cls, marker, label = "", lx = 0, ly = 0) {
  add(`<path id="${id}" class="${cls}" d="${d}" marker-end="url(#${marker})"/>`);
  if (label) {
    add(`<rect class="labelBg" x="${lx - 102}" y="${ly - 20}" width="204" height="30" rx="9"/>`);
    add(`<text class="edgeLabel" x="${lx}" y="${ly}" text-anchor="middle">${esc(label)}</text>`);
  }
}

add(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Avro serializer class structure">`);
add(`<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%">
    <feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#203040" flood-opacity="0.10"/>
  </filter>
  <marker id="openArrow" markerWidth="15" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 2 2 L 12 7 L 2 12" fill="none" stroke="context-stroke" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/>
  </marker>
  <marker id="hollowTriangle" markerWidth="18" markerHeight="16" refX="16" refY="8" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 2 2 L 16 8 L 2 14 Z" fill="#FFFFFF" stroke="context-stroke" stroke-width="2.4" stroke-linejoin="round" stroke-dasharray="none"/>
  </marker>
</defs>`);
add(`<style>
  svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
  .canvas{fill:#F7FAFC}.frame{fill:#FFFFFF;stroke:#D5E0EA;stroke-width:3;filter:url(#shadow)}
  .title{font-family:"Architects Daughter";font-size:50px;font-weight:700;fill:#172033}
  .subtitle{font-family:"Comic Mono";font-size:18px;font-weight:700;fill:#526174}
  .sectionLabel{font-family:"Comic Mono";font-size:17px;font-weight:700;fill:#66788D}
  .classBox rect,.supportBox rect{filter:url(#shadow);stroke-width:3;fill:#FFFFFF}
  .blue rect{fill:#EAF3FF;stroke:#3B82F6}.green rect{fill:#EAF9F0;stroke:#21A366}
  .teal rect{fill:#E8FAF8;stroke:#139B91}.amber rect{fill:#FFF4D8;stroke:#D4860B}
  .purple rect{fill:#F2EAFF;stroke:#8B5CF6}.pink rect{fill:#FFEAF1;stroke:#DB2777}
  .gray rect{fill:#F4F6F8;stroke:#64748B}.orange rect{fill:#FFF0E4;stroke:#F05A1A}
  .stereo{font-family:"Comic Mono";font-size:14px;font-weight:700;fill:#66788D}
  .classTitle{font-family:"Architects Daughter";font-size:25px;font-weight:700;fill:#172033}
  .supportTitle{font-family:"Architects Daughter";font-size:26px;font-weight:700;fill:#172033}
  .member{font-family:"Comic Mono";font-size:15px;font-weight:700;fill:#526174}
  .divider{stroke:#CBD5E1;stroke-width:1.5}
  .impl{fill:none;stroke-width:3.8;stroke-dasharray:10 8;stroke-linecap:round;stroke-linejoin:round}
  .uses{fill:none;stroke-width:3.8;stroke-linecap:round;stroke-linejoin:round}
  .blueLine{stroke:#2563EB}.greenLine{stroke:#15803D}.tealLine{stroke:#0F8B87}.orangeLine{stroke:#EA580C}.purpleLine{stroke:#7C3AED}
  .labelBg{fill:#FFFFFF;stroke:#D6E3EF;stroke-width:1.4;opacity:.96}
  .edgeLabel{font-family:"Comic Mono";font-size:13.5px;font-weight:700;fill:#526174}
  .legendText{font-family:"Comic Mono";font-size:13.5px;font-weight:700;fill:#526174}
</style>`);

add(`<rect class="canvas" width="${W}" height="${H}"/>`);
add(`<rect class="frame" x="38" y="34" width="${W - 76}" height="${H - 68}" rx="22"/>`);
add(`<text class="title" x="86" y="100">Avro Serializer Class Structure</text>`);
add(`<text class="subtitle" x="88" y="134">Three public serializer contracts map to DataFile-based implementations with shared codec, Base64, and failure behavior.</text>`);

add(`<text class="sectionLabel" x="106" y="204">Public serializer interfaces</text>`);
classBox("generic-iface", 110, 240, 620, 190, "blue", "<<interface>>", "AvroGenericRecordSerializer", [
  "+ serialize(schema, GenericRecord?)",
  "+ deserialize(schema, ByteArray?)",
  "+ serializeAsString(...) / deserializeFromString(...)"
]);
classBox("specific-iface", 850, 240, 620, 190, "green", "<<interface>>", "AvroSpecificRecordSerializer", [
  "+ serialize(SpecificRecord?)",
  "+ deserialize(bytes, Class<T>)",
  "+ serializeList(...) / deserializeList(...)"
]);
classBox("reflect-iface", 1590, 240, 620, 190, "teal", "<<interface>>", "AvroReflectSerializer", [
  "+ serialize(graph: T?)",
  "+ deserialize(bytes, Class<T>)",
  "+ reified deserialize helpers"
]);

add(`<text class="sectionLabel" x="106" y="556">Default implementations</text>`);
classBox("generic-impl", 110, 592, 620, 226, "blue", "<<class>>", "DefaultAvroGenericRecordSerializer", [
  "- codecFactory: CodecFactory",
  "+ GenericDatumWriter / GenericDatumReader",
  "+ schema is required at call site",
  "+ failure => null, logged"
]);
classBox("specific-impl", 850, 592, 620, 226, "green", "<<class>>", "DefaultAvroSpecificRecordSerializer", [
  "- codecFactory: CodecFactory",
  "+ SpecificDatumWriter / SpecificDatumReader",
  "+ single record and list APIs",
  "+ failure => null or emptyList(), logged"
]);
classBox("reflect-impl", 1590, 592, 620, 226, "teal", "<<class>>", "DefaultAvroReflectSerializer", [
  "- codecFactory: CodecFactory",
  "+ ReflectDatumWriter / ReflectDatumReader",
  "+ per-class schema cache",
  "+ failure => null, logged"
]);

add(`<text class="sectionLabel" x="106" y="970">Shared support contracts</text>`);
supportBox("failure", 110, 1010, 560, 188, "pink", "Failure and string contract", [
  "Base64 wrappers live on interfaces",
  "invalid Base64 returns null",
  "deserialization failures do not escape"
]);
supportBox("datafile", 780, 1010, 760, 188, "gray", "Avro DataFile container", [
  "DataFileWriter + DataFileReader",
  "SeekableByteArrayInput for reads",
  "serializer constructors accept CodecFactory"
]);
supportBox("reflect-cache", 1650, 1010, 560, 188, "amber", "Reflect schema cache", [
  "ConcurrentHashMap<Class<*>, Schema>",
  "ReflectData.get().getSchema(clazz)",
  "used only by reflection serializer"
]);

edge("impl-generic", "M 420 592 L 420 430", "impl blueLine", "hollowTriangle", "implements", 530, 512);
edge("impl-specific", "M 1160 592 L 1160 430", "impl greenLine", "hollowTriangle", "implements", 1270, 512);
edge("impl-reflect", "M 1900 592 L 1900 430", "impl tealLine", "hollowTriangle", "implements", 2010, 512);

edge("generic-datafile", "M 420 818 L 420 928 L 1060 928 L 1060 1010", "uses blueLine", "openArrow", "uses DataFile", 724, 912);
edge("specific-datafile", "M 1160 818 L 1160 1010", "uses greenLine", "openArrow");
edge("reflect-datafile", "M 1900 818 L 1900 928 L 1300 928 L 1300 1010", "uses tealLine", "openArrow", "uses DataFile", 1600, 912);
edge("reflect-cache-edge", "M 1900 818 L 1900 1010", "uses orangeLine", "openArrow", "schemaOf(clazz)", 2020, 926);
edge("iface-contract", "M 110 382 L 70 382 L 70 930 L 250 930 L 250 1010", "uses purpleLine", "openArrow", "string wrappers", 168, 910);

add(`<g id="legend">`);
add(`<path class="impl blueLine" d="M 110 1288 H 178" marker-end="url(#hollowTriangle)"/><text class="legendText" x="198" y="1294">implements</text>`);
add(`<path class="uses orangeLine" d="M 344 1288 H 412" marker-end="url(#openArrow)"/><text class="legendText" x="432" y="1294">uses support</text>`);
add(`</g>`);

add(`</svg>`);
writeFileSync(out, `${lines.join("\n")}\n`);
console.log(out);
