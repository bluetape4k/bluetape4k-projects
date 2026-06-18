#!/usr/bin/env node

import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-redisson-diagram-02.svg";
const W = 1700;
const H = 1260;

const esc = (s) =>
  String(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

const lines = [];
const add = (s) => lines.push(s);

function textLines(items, x, y, cls = "body", gap = 25, anchor = "middle") {
  items.forEach((line, i) => {
    add(`<text class="${cls}" x="${x}" y="${y + i * gap}" text-anchor="${anchor}">${esc(line)}</text>`);
  });
}

function card(id, x, y, w, h, tone, title, body = []) {
  add(`<g id="${id}" class="card ${tone}">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="18"/>`);
  add(`<text class="cardTitle" x="${x + w / 2}" y="${y + 39}" text-anchor="middle">${esc(title)}</text>`);
  textLines(body, x + w / 2, y + 74, "cardBody", 25);
  add(`</g>`);
}

function section(x, y, w, h, title, subtitle) {
  add(`<g class="section">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="22"/>`);
  add(`<text class="sectionTitle" x="${x + 34}" y="${y + 48}">${esc(title)}</text>`);
  add(`<text class="sectionSub" x="${x + 34}" y="${y + 80}">${esc(subtitle)}</text>`);
  add(`</g>`);
}

function edge(d, cls = "edge", marker = "arrow", label = "", lx = 0, ly = 0) {
  add(`<path class="${cls}" d="${d}" marker-end="url(#${marker})"/>`);
  if (label) {
    add(`<rect class="labelBg" x="${lx - 38}" y="${ly - 18}" width="76" height="27" rx="8"/>`);
    add(`<text class="edgeLabel" x="${lx}" y="${ly}" text-anchor="middle">${esc(label)}</text>`);
  }
}

function decision(id, cx, cy, w, h, title, body = []) {
  const pts = `${cx},${cy - h / 2} ${cx + w / 2},${cy} ${cx},${cy + h / 2} ${cx - w / 2},${cy}`;
  add(`<g id="${id}" class="decision">`);
  add(`<polygon points="${pts}"/>`);
  add(`<text class="cardTitle" x="${cx}" y="${cy - 6}" text-anchor="middle">${esc(title)}</text>`);
  textLines(body, cx, cy + 20, "cardBody", 22);
  add(`</g>`);
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
  .canvas{fill:#F7FAFC}.frame{fill:#FFFFFF;stroke:#D5E0EA;stroke-width:3}
  .title{font-size:47px;font-weight:700;fill:#172033}
  .subtitle{font-family:"Comic Mono",monospace;font-size:19px;font-weight:700;fill:#526174}
  .section rect{fill:#FFFFFF;stroke:#D5E0EA;stroke-width:2.4}
  .sectionTitle{font-size:29px;font-weight:700}.sectionSub{font-family:"Comic Mono",monospace;font-size:15px;font-weight:700;fill:#66788D}
  .card rect{filter:url(#shadow);stroke-width:3;fill:#FFFFFF}
  .blue rect{fill:#EAF3FF;stroke:#3B82F6}.green rect{fill:#EAF9F0;stroke:#21A366}
  .teal rect{fill:#E8FAF8;stroke:#139B91}.amber rect{fill:#FFF4D8;stroke:#D4860B}
  .purple rect{fill:#F2EAFF;stroke:#8B5CF6}.pink rect{fill:#FFEAF1;stroke:#DB2777}
  .gray rect{fill:#F4F6F8;stroke:#64748B}.orange rect{fill:#FFF0E4;stroke:#F05A1A}
  .cardTitle{font-size:27px;font-weight:700}.cardBody{font-family:"Comic Mono",monospace;font-size:15px;font-weight:700;fill:#526174}
  .decision polygon{filter:url(#shadow);fill:#FFF7ED;stroke:#F05A1A;stroke-width:3}
  .edge{fill:none;stroke:#344052;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgeBlue{fill:none;stroke:#2563EB;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgeGreen{fill:none;stroke:#15803D;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgeTeal{fill:none;stroke:#0F8B87;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgeAmber{fill:none;stroke:#C47A00;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgePink{fill:none;stroke:#BE185D;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .edgeDashed{fill:none;stroke:#BE185D;stroke-width:3.8;stroke-dasharray:11 8;stroke-linecap:round;stroke-linejoin:round}
  .labelBg{fill:#FFFFFF;stroke:#D6E3EF;stroke-width:1.4}.edgeLabel{font-family:"Comic Mono",monospace;font-size:14px;font-weight:700;fill:#526174}
  .note{font-family:"Comic Mono",monospace;font-size:15px;font-weight:700;fill:#526174}
</style>`);
add(`<rect class="canvas" width="${W}" height="${H}"/>`);
add(`<rect class="frame" x="36" y="36" width="${W - 72}" height="${H - 72}" rx="22"/>`);
add(`<text class="title" x="84" y="96">Batch and Transaction Processing Flow</text>`);
add(`<text class="subtitle" x="86" y="132">Batch queues commands and executes once; transaction commits on normal exit and rolls back before rethrowing failures.</text>`);

section(88, 188, 700, 960, "Batch DSL", "Same command path for sync and suspend variants.");
section(912, 188, 700, 960, "Transaction DSL", "Same action contract; only commit/rollback call style differs.");

card("batch-entry", 162, 302, 552, 112, "blue", "Batch entrypoints", ["withBatch / withSuspendedBatch", "receiver: RBatch"]);
card("batch-create", 218, 486, 440, 104, "green", "createBatch(options)", ["RedissonClient creates RBatch"]);
card("batch-action", 218, 666, 440, 104, "teal", "action(RBatch)", ["queue async Redis commands"]);
card("batch-execute", 218, 842, 440, 104, "amber", "execute once", ["execute()", "or executeAsync().await()"]);
card("batch-result", 506, 962, 214, 82, "orange", "BatchResult", ["one Redis round trip"]);

edge("M 438 414 L 438 486", "edgeBlue");
edge("M 438 590 L 438 666", "edgeGreen");
edge("M 438 770 L 438 842", "edgeTeal");
edge("M 658 894 L 714 894 L 714 962", "edgeAmber");

card("tx-entry", 972, 302, 552, 112, "purple", "Transaction entrypoints", ["withTransaction / withSuspendedTransaction", "receiver: RTransaction"]);
card("tx-create", 1028, 486, 440, 104, "green", "createTransaction(options)", ["RedissonClient creates RTransaction"]);
card("tx-action", 1028, 666, 440, 104, "teal", "action(RTransaction)", ["run caller commands"]);
decision("tx-decision", 1248, 864, 250, 122, "action throws?", ["normal vs exception"]);
card("tx-commit", 974, 960, 326, 108, "green", "Commit path", ["commit or commitAsync", "returns success"]);
card("tx-rollback", 1368, 960, 262, 108, "pink", "Rollback path", ["rollback attempt", "rethrow original"]);

edge("M 1248 414 L 1248 486", "edgeBlue");
edge("M 1248 590 L 1248 666", "edgeGreen");
edge("M 1248 770 L 1248 803", "edgeTeal");
edge("M 1123 864 L 1123 922 L 1137 922 L 1137 960", "edgeGreen", "arrow", "no", 1058, 914);
edge("M 1373 864 L 1499 864 L 1499 960", "edgeDashed", "arrow", "yes", 1546, 842);
add(`<rect class="labelBg" x="210" y="1170" width="1280" height="40" rx="12"/>`);
add(`<text class="note" x="850" y="1195" text-anchor="middle">Batch sends queued commands at execute time; transaction rollback failures are ignored while the original failure is rethrown.</text>`);

add(`</svg>`);
writeFileSync(out, `${lines.join("\n")}\n`);
console.log(out);
