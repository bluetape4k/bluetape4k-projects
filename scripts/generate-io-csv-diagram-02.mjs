#!/usr/bin/env node

import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/io-csv-diagram-02.svg";
const W = 2240;
const H = 1500;

const esc = (s) =>
  String(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

const lines = [];
const add = (s) => lines.push(s);

function textLines(items, x, y, cls = "body", gap = 27, anchor = "middle") {
  items.forEach((line, i) => {
    add(`<text class="${cls}" x="${x}" y="${y + i * gap}" text-anchor="${anchor}">${esc(line)}</text>`);
  });
}

function card(id, x, y, w, h, tone, title, body = []) {
  add(`<g id="${id}" class="card ${tone}">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="14"/>`);
  add(`<text class="cardTitle" x="${x + w / 2}" y="${y + 46}" text-anchor="middle">${esc(title)}</text>`);
  add(`<line class="divider" x1="${x + 34}" y1="${y + 68}" x2="${x + w - 34}" y2="${y + 68}"/>`);
  textLines(body, x + w / 2, y + 96, "body", 25);
  add(`</g>`);
}

function lane(id, x, y, w, h, title) {
  add(`<g id="${id}" class="lane">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="16"/>`);
  add(`<text class="laneTitle" x="${x + 42}" y="${y + 68}">${esc(title)}</text>`);
  add(`</g>`);
}

function arrow(id, x1, y1, x2, y2, cls, marker, label = "", lx = 0, ly = 0) {
  add(`<path id="${id}" class="arrow ${cls}" d="M ${x1} ${y1} L ${x2} ${y2}" marker-end="url(#${marker})"/>`);
  if (label) {
    add(`<rect class="labelBg" x="${lx - 128}" y="${ly - 20}" width="256" height="31" rx="9"/>`);
    add(`<text class="edgeLabel" x="${lx}" y="${ly}" text-anchor="middle">${esc(label)}</text>`);
  }
}

add(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="CSV TSV processing flow">`);
add(`<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%">
    <feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#203040" flood-opacity="0.10"/>
  </filter>
  <marker id="flowArrowTeal" markerWidth="15" markerHeight="15" refX="13" refY="7.5" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 1 1 L 14 7.5 L 1 14 Z" fill="#0F9B93" stroke="#0F9B93" stroke-width="1" stroke-dasharray="none"/>
  </marker>
  <marker id="flowArrowBlue" markerWidth="15" markerHeight="15" refX="13" refY="7.5" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 1 1 L 14 7.5 L 1 14 Z" fill="#2563EB" stroke="#2563EB" stroke-width="1" stroke-dasharray="none"/>
  </marker>
  <marker id="flowArrowOrange" markerWidth="15" markerHeight="15" refX="13" refY="7.5" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 1 1 L 14 7.5 L 1 14 Z" fill="#EA580C" stroke="#EA580C" stroke-width="1" stroke-dasharray="none"/>
  </marker>
</defs>`);

add(`<style>
  svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
  .canvas{fill:#F7FAFC}.frame{fill:#FFFFFF;stroke:#D5E0EA;stroke-width:3;filter:url(#shadow)}
  .title{font-family:"Architects Daughter";font-size:54px;font-weight:700;fill:#172033}
  .subtitle{font-family:"Comic Mono";font-size:18px;font-weight:700;fill:#526174}
  .settings rect,.semantics rect{filter:url(#shadow);stroke-width:3}
  .settings rect{fill:#F5ECFF;stroke:#9B3FFF}.semantics rect{fill:#F8FAFC;stroke:#64748B}
  .settingsTitle,.semanticsTitle{font-family:"Architects Daughter";font-size:31px;font-weight:700;fill:#172033}
  .settingsBody,.semanticsBody{font-family:"Comic Mono";font-size:15.5px;font-weight:700;fill:#526174}
  .lane rect{fill:#FFFFFF;stroke:#D1DBE8;stroke-width:2.5;filter:url(#shadow)}
  .laneTitle{font-family:"Architects Daughter";font-size:34px;font-weight:700;fill:#172033}
  .card rect{filter:url(#shadow);stroke-width:3;fill:#FFFFFF}
  .teal rect{fill:#E8FAF8;stroke:#0F9B93}.blue rect{fill:#EAF3FF;stroke:#2563EB}
  .orange rect{fill:#FFF0E4;stroke:#F05A1A}.green rect{fill:#EAF9F0;stroke:#21A366}
  .gray rect{fill:#F4F6F8;stroke:#64748B}.purple rect{fill:#F3EAFF;stroke:#8B5CF6}
  .cardTitle{font-family:"Architects Daughter";font-size:29px;font-weight:700;fill:#172033}
  .body{font-family:"Comic Mono";font-size:15px;font-weight:700;fill:#526174}
  .divider{stroke:#CBD5E1;stroke-width:1.5}
  .arrow{fill:none;stroke-width:5;stroke-linecap:round;stroke-linejoin:round}
  .tealLine{stroke:#0F9B93}.blueLine{stroke:#2563EB}.orangeLine{stroke:#EA580C}
  .labelBg{fill:#FFFFFF;stroke:#D6E3EF;stroke-width:1.4;opacity:.96}
  .edgeLabel{font-family:"Comic Mono";font-size:14px;font-weight:700;fill:#526174}
</style>`);

add(`<rect class="canvas" width="${W}" height="${H}"/>`);
add(`<rect class="frame" x="38" y="34" width="${W - 76}" height="${H - 68}" rx="22"/>`);
add(`<text class="title" x="90" y="104">CSV/TSV Processing Flow</text>`);
add(`<text class="subtitle" x="92" y="140">The self-implemented lexer powers V1 Sequence reads, coroutine Flow reads, V2 Flow DSL reads, and writer paths.</text>`);

add(`<g class="settings">`);
add(`<rect x="210" y="202" width="1820" height="126" rx="14"/>`);
add(`<text class="settingsTitle" x="1120" y="255" text-anchor="middle">CsvSettings / TsvSettings</text>`);
add(`<text class="settingsBody" x="1120" y="294" text-anchor="middle">delimiter, quote, CR/LF, trimValues, null policy, BOM detection, max column and field limits</text>`);
add(`</g>`);

lane("lane-v1", 120, 408, 620, 820, "V1 sync read");
lane("lane-flow", 810, 408, 620, 820, "Flow read paths");
lane("lane-writer", 1500, 408, 620, 820, "Writer output");

card("v1-input", 184, 530, 492, 132, "teal", "File / InputStream", [
  "caller charset; UTF-8 can use Okio",
]);
card("v1-reader", 184, 710, 492, 154, "teal", "Csv/TsvRecordReader", [
  "returns lazy Sequence<T>",
  "skipHeaders stores HeaderIndex",
]);
card("v1-lexer", 184, 920, 492, 172, "teal", "CsvLexer / TsvLexer", [
  "RFC 4180 state machine",
  "ArrayRecord per parsed row",
  "field and column limits",
]);

card("flow-input", 874, 530, 492, 132, "blue", "InputStream / Path", [
  "Suspend reader or FlowCsvReader",
]);
card("flow-channel", 874, 710, 492, 154, "blue", "channelFlow on Dispatchers.IO", [
  "ensureActive before each row",
  "collector sees parser exceptions",
]);
card("flow-output", 874, 920, 492, 172, "blue", "Flow<T> / Flow<CsvRow>", [
  "records emitted in order",
  "V2 converts Record to CsvRow",
  "cold stream starts on collect",
]);

card("writer-input", 1564, 530, 492, 132, "orange", "Rows or Flow<Iterable<*>>", [
  "headers plus transformed row values",
]);
card("writer-family", 1564, 710, 492, 154, "orange", "RecordWriter family", [
  "Csv/TsvRecordWriter",
  "Suspend writer and FlowCsvWriter",
]);
card("writer-engine", 1564, 920, 492, 172, "orange", "DelimitedWriter engine", [
  "quote, escape, line separator rules",
  "null is an unquoted empty field",
  "UTF-8 file path uses OkioDelimitedWriter",
]);

arrow("v1-a", 430, 662, 430, 710, "tealLine", "flowArrowTeal");
arrow("v1-b", 430, 864, 430, 920, "tealLine", "flowArrowTeal");
arrow("flow-a", 1120, 662, 1120, 710, "blueLine", "flowArrowBlue");
arrow("flow-b", 1120, 864, 1120, 920, "blueLine", "flowArrowBlue");
arrow("writer-a", 1810, 662, 1810, 710, "orangeLine", "flowArrowOrange");
arrow("writer-b", 1810, 864, 1810, 920, "orangeLine", "flowArrowOrange");

add(`<g class="semantics">`);
add(`<rect x="220" y="1288" width="1800" height="88" rx="12"/>`);
add(`<text class="semanticsTitle" x="400" y="1342" text-anchor="middle">Value semantics</text>`);
add(`<text class="semanticsBody" x="1160" y="1343" text-anchor="middle">null writes as an unquoted empty field; empty string writes quoted and reads back as an empty string.</text>`);
add(`</g>`);

add(`</svg>`);
writeFileSync(out, `${lines.join("\n")}\n`);
console.log(out);
