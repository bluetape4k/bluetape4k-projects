#!/usr/bin/env node

import {writeFileSync} from "node:fs";

const out = "docs/images/readme-diagrams/infra-redisson-diagram-01.svg";
const W = 1900;
const H = 1320;

const esc = (s) =>
    String(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

const lines = [];
const add = (s) => lines.push(s);

function textLines(items, x, y, cls = "small", gap = 28, anchor = "middle") {
    items.forEach((line, i) => {
        add(`<text class="${cls}" x="${x}" y="${y + i * gap}" text-anchor="${anchor}">${esc(line)}</text>`);
    });
}

function card(id, x, y, w, h, tone, title, body = [], opts = {}) {
    const rx = opts.rx ?? 18;
    add(`<g id="${id}" class="card ${tone}">`);
    add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}"/>`);
    add(`<text class="cardTitle" x="${x + w / 2}" y="${y + 38}" text-anchor="middle">${esc(title)}</text>`);
    if (body.length) {
        textLines(body, x + w / 2, y + 72, "cardBody", 25);
    }
    add(`</g>`);
}

function section(x, y, w, h, title, subtitle = "") {
    add(`<g class="section">`);
    add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="20"/>`);
    add(`<text class="sectionTitle" x="${x + 34}" y="${y + 48}">${esc(title)}</text>`);
    if (subtitle) add(`<text class="sectionSub" x="${x + 34}" y="${y + 80}">${esc(subtitle)}</text>`);
    add(`</g>`);
}

function path(id, d, cls = "edge", marker = "arrow", label = "", lx = 0, ly = 0) {
    add(`<path id="${id}" class="${cls}" d="${d}" marker-end="url(#${marker})"/>`);
    if (label) {
        add(`<rect class="labelBg" x="${lx - 96}" y="${ly - 19}" width="192" height="28" rx="8"/>`);
        add(`<text class="edgeLabel" x="${lx}" y="${ly}" text-anchor="middle">${esc(label)}</text>`);
    }
}

add(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}">`);
add(`<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%">
    <feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#203040" flood-opacity="0.10"/>
  </filter>
  <marker id="arrow" markerWidth="13" markerHeight="13" refX="11" refY="6.5" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 1.5 1.5 L 11 6.5 L 1.5 11.5 Z" fill="context-stroke" stroke="context-stroke" stroke-width="1"/>
  </marker>
  <marker id="openArrow" markerWidth="13" markerHeight="13" refX="11" refY="6.5" orient="auto" markerUnits="userSpaceOnUse">
    <path d="M 2 2 L 11 6.5 L 2 11" fill="none" stroke="context-stroke" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/>
  </marker>
</defs>`);
add(`<style>
  text{font-family:"Architects Daughter","Comic Mono",monospace;letter-spacing:0;fill:#263447}
  .canvas{fill:#F7FAFC}
  .frame{fill:#FFFFFF;stroke:#D5E0EA;stroke-width:3}
  .title{font-size:48px;font-weight:700;fill:#172033}
  .subtitle{font-family:"Comic Mono",monospace;font-size:20px;font-weight:700;fill:#526174}
  .section rect{fill:#FFFFFF;stroke:#D5E0EA;stroke-width:2.4}
  .sectionTitle{font-size:28px;font-weight:700;fill:#263447}
  .sectionSub{font-family:"Comic Mono",monospace;font-size:15px;font-weight:700;fill:#66788D}
  .card rect{filter:url(#shadow);stroke-width:3;fill:#FFFFFF}
  .blue rect{fill:#EAF3FF;stroke:#3B82F6}.green rect{fill:#EAF9F0;stroke:#21A366}
  .teal rect{fill:#E8FAF8;stroke:#139B91}.amber rect{fill:#FFF4D8;stroke:#D4860B}
  .purple rect{fill:#F2EAFF;stroke:#8B5CF6}.pink rect{fill:#FFEAF1;stroke:#DB2777}
  .gray rect{fill:#F4F6F8;stroke:#64748B}.orange rect{fill:#FFF0E4;stroke:#F05A1A}
  .cardTitle{font-size:27px;font-weight:700}.cardBody{font-family:"Comic Mono",monospace;font-size:15px;font-weight:700;fill:#526174}
  .note{font-family:"Comic Mono",monospace;font-size:16px;font-weight:700;fill:#526174}
  .edge{fill:none;stroke:#344052;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgeBlue{fill:none;stroke:#2563EB;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgeGreen{fill:none;stroke:#15803D;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgeTeal{fill:none;stroke:#0F8B87;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgeAmber{fill:none;stroke:#C47A00;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgePink{fill:none;stroke:#BE185D;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgeDashed{fill:none;stroke:#7C3AED;stroke-width:3.6;stroke-dasharray:10 8;stroke-linecap:round;stroke-linejoin:round}
  .labelBg{fill:#FFFFFF;stroke:#D6E3EF;stroke-width:1.4}.edgeLabel{font-family:"Comic Mono",monospace;font-size:14px;font-weight:700;fill:#526174}
</style>`);
add(`<rect class="canvas" width="${W}" height="${H}"/>`);
add(`<rect class="frame" x="36" y="36" width="${W - 72}" height="${H - 72}" rx="22"/>`);
add(`<text class="title" x="84" y="96">Redisson Codec Selection Map</text>`);
add(`<text class="subtitle" x="86" y="132">Read this as a selection model: Default is Fory, compression is opt-in, and map codecs use CompositeCodec.</text>`);

card("factory", 760, 184, 380, 118, "blue", "RedissonCodecs", ["lazy constants", "use-case factories"]);

section(90, 360, 410, 760, "Serializers", "Raw value codecs exposed as constants.");
section(530, 360, 410, 760, "Compression", "Wrappers around a selected value codec.");
section(970, 360, 410, 760, "Map composition", "String key codec plus value codec.");
section(1410, 360, 400, 760, "Use-case factories", "Reader-facing names return source constants.");

card("default", 135, 458, 320, 112, "green", "Default = Fory", ["plain Fory", "fallback: Kryo5"]);
card("fastfory", 135, 616, 320, 126, "purple", "FastFory", ["faster schema mode", "volatile cache only"]);
card("json", 135, 796, 320, 126, "amber", "JSON codecs", ["Jackson3 / Fastjson2", "safe factories allow-list"]);
card("builtin", 135, 968, 320, 104, "gray", "Built-ins", ["Kryo5, JDK", "String, numeric"]);

card("wrap", 570, 458, 330, 118, "teal", "Wrapper classes", ["Lz4Codec, ZstdCodec", "GzipCodec"]);
card("snappy", 570, 632, 330, 112, "teal", "SnappyCodecV2", ["Redisson wrapper", "same value-codec idea"]);
card("compressed", 570, 816, 330, 136, "teal", "Compressed constants", ["LZ4Fory / ZstdFory", "Snappy* / Gzip*"]);
card("compression-note", 570, 990, 330, 92, "pink", "Opt-in compression", ["choose it explicitly"]);

card("composite", 1010, 458, 330, 120, "orange", "CompositeCodec", ["String key codec", "value codec twice"]);
card("mapvalue", 1010, 650, 330, 126, "orange", "Map constants", ["ForyComposite", "LZ4ForyComposite"]);
card("near", 1010, 850, 330, 136, "teal", "NearCache default", ["RedissonNearCache", "uses LZ4Fory"]);

card("general", 1452, 444, 320, 76, "green", "forGeneral()", ["Fory"]);
card("small", 1452, 532, 320, 76, "gray", "forSmallValue()", ["Kryo5"]);
card("cache", 1452, 620, 320, 82, "teal", "forCache()", ["LZ4Fory; value cache"]);
card("cachemap", 1452, 718, 320, 82, "orange", "forCacheMap()", ["LZ4ForyComposite"]);
card("high", 1452, 816, 320, 88, "purple", "forHighThroughput()", ["LZ4FastFory; volatile only"]);
card("archive", 1452, 922, 320, 76, "amber", "forArchival()", ["ZstdFory"]);
card("compat", 1452, 1010, 320, 76, "gray", "forCompatibility()", ["JDK"]);

path("p-to-serial", "M 840 302 L 840 330 L 295 330 L 295 458", "edgeGreen", "arrow");
path("p-to-compression", "M 900 302 L 900 330 L 735 330 L 735 458", "edgeTeal", "arrow");
path("p-to-composite", "M 960 302 L 960 330 L 1175 330 L 1175 458", "edgeAmber", "arrow");
path("p-to-usecase", "M 1020 302 L 1020 330 L 1790 330 L 1790 482 L 1772 482", "edgeBlue", "arrow");

path("p-fory-wrap", "M 455 514 L 570 514", "edgeTeal", "openArrow");
path("p-fast-wrap", "M 455 678 L 515 678 L 515 690 L 570 690", "edgeDashed", "openArrow");
path("p-wrap-out", "M 735 576 L 735 816", "edgeTeal", "arrow");
path("p-composite-map", "M 1175 578 L 1175 650", "edgeAmber", "arrow");
path("p-map-near", "M 1175 776 L 1175 850", "edgeTeal", "arrow");

add(`<rect class="labelBg" x="132" y="1148" width="1636" height="42" rx="12"/>`);
add(`<text class="note" x="950" y="1175" text-anchor="middle">FastFory can read legacy Fory through fallback, but Fory cannot read FastFory data; keep FastFory for volatile caches.</text>`);

add(`</svg>`);
writeFileSync(out, `${lines.join("\n")}\n`);
console.log(out);
