#!/usr/bin/env node

import { writeFileSync } from "node:fs";

const out = "docs/images/readme-diagrams/infra-resilience4j-diagram-01.svg";
const W = 2320;
const H = 1600;

const esc = (s) =>
  String(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

const lines = [];
const add = (s) => lines.push(s);

function textLines(items, x, y, cls = "member", gap = 29, anchor = "start") {
  items.forEach((line, i) => {
    add(`<text class="${cls}" x="${x}" y="${y + i * gap}" text-anchor="${anchor}">${esc(line)}</text>`);
  });
}

function classBox(id, x, y, w, h, tone, title, members = [], opts = {}) {
  add(`<g id="${id}" class="classBox ${tone}">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12"/>`);
  if (opts.stereotype) {
    add(`<text class="stereo" x="${x + w / 2}" y="${y + 34}" text-anchor="middle">${esc(opts.stereotype)}</text>`);
    add(`<text class="classTitle" x="${x + w / 2}" y="${y + 70}" text-anchor="middle">${esc(title)}</text>`);
    add(`<line class="divider" x1="${x}" y1="${y + 92}" x2="${x + w}" y2="${y + 92}"/>`);
    textLines(members, x + 28, y + 125, "member", 29);
  } else {
    add(`<text class="classTitle" x="${x + w / 2}" y="${y + 48}" text-anchor="middle">${esc(title)}</text>`);
    add(`<line class="divider" x1="${x}" y1="${y + 70}" x2="${x + w}" y2="${y + 70}"/>`);
    textLines(members, x + 28, y + 103, "member", 29);
  }
  add(`</g>`);
}

function noteBox(id, x, y, w, h, tone, title, body = []) {
  add(`<g id="${id}" class="noteBox ${tone}">`);
  add(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12"/>`);
  add(`<text class="noteTitle" x="${x + 28}" y="${y + 42}">${esc(title)}</text>`);
  textLines(body, x + 28, y + 76, "noteText", 25);
  add(`</g>`);
}

function edge(id, d, cls, marker, label = "", lx = 0, ly = 0) {
  add(`<path id="${id}" class="${cls}" d="${d}" marker-end="url(#${marker})"/>`);
  if (label) {
    add(`<rect class="labelBg" x="${lx - 112}" y="${ly - 21}" width="224" height="30" rx="9"/>`);
    add(`<text class="edgeLabel" x="${lx}" y="${ly}" text-anchor="middle">${esc(label)}</text>`);
  }
}

add(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Resilience4j coroutine class structure">`);
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
  .clusterLabel{font-family:"Comic Mono";font-size:17px;font-weight:700;fill:#66788D}
  .classBox rect,.noteBox rect{filter:url(#shadow);stroke-width:3;fill:#FFFFFF}
  .blue rect{fill:#EAF3FF;stroke:#3B82F6}.green rect{fill:#EAF9F0;stroke:#21A366}
  .teal rect{fill:#E8FAF8;stroke:#139B91}.amber rect{fill:#FFF4D8;stroke:#D4860B}
  .purple rect{fill:#F2EAFF;stroke:#8B5CF6}.pink rect{fill:#FFEAF1;stroke:#DB2777}
  .gray rect{fill:#F4F6F8;stroke:#64748B}.orange rect{fill:#FFF0E4;stroke:#F05A1A}
  .stereo{font-family:"Comic Mono";font-size:14px;font-weight:700;fill:#66788D}
  .classTitle{font-family:"Architects Daughter";font-size:27px;font-weight:700;fill:#172033}
  .member{font-family:"Comic Mono";font-size:15px;font-weight:700;fill:#526174}
  .noteTitle{font-family:"Architects Daughter";font-size:25px;font-weight:700;fill:#172033}
  .noteText{font-family:"Comic Mono";font-size:14.5px;font-weight:700;fill:#526174}
  .divider{stroke:#CBD5E1;stroke-width:1.5}
  .uses{fill:none;stroke:#2563EB;stroke-width:3.8;stroke-dasharray:10 8;stroke-linecap:round;stroke-linejoin:round}
  .usesGreen{fill:none;stroke:#15803D;stroke-width:3.8;stroke-dasharray:10 8;stroke-linecap:round;stroke-linejoin:round}
  .ref{fill:none;stroke:#F05A1A;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .impl{fill:none;stroke:#8B5CF6;stroke-width:3.8;stroke-dasharray:10 8;stroke-linecap:round;stroke-linejoin:round}
  .create{fill:none;stroke:#344052;stroke-width:4;stroke-linecap:round;stroke-linejoin:round}
  .labelBg{fill:#FFFFFF;stroke:#D6E3EF;stroke-width:1.4;opacity:.96}
  .edgeLabel{font-family:"Comic Mono";font-size:13.5px;font-weight:700;fill:#526174}
  .legendText{font-family:"Comic Mono";font-size:13.5px;font-weight:700;fill:#526174}
</style>`);

add(`<rect class="canvas" width="${W}" height="${H}"/>`);
add(`<rect class="frame" x="38" y="34" width="${W - 76}" height="${H - 68}" rx="22"/>`);
add(`<text class="title" x="86" y="100">Resilience4j Coroutine Class Structure</text>`);
add(`<text class="subtitle" x="88" y="134">Source-backed class map: suspend entrypoints, decorator builders, cache contracts, and cancellation-safe fallback behavior.</text>`);

add(`<text class="clusterLabel" x="112" y="184">External Resilience4j policy types used by coroutine wrappers</text>`);
noteBox("policies", 112, 208, 700, 174, "gray", "Policy primitives", [
  "CircuitBreaker / Retry / RateLimiter",
  "Bulkhead / TimeLimiter",
  "Cache<K,V> facade"
]);
noteBox("extensions", 892, 208, 560, 174, "blue", "Coroutine extension functions", [
  "withXxx(policy) { suspend block }",
  "decorateSuspendFunction1 / BiFunction",
  "Retry preserves CancellationException"
]);
noteBox("fallback", 1578, 208, 560, 174, "pink", "Fallback helpers", [
  "recover(...) and andThen(...)",
  "typed exception fallback",
  "CancellationException is rethrown"
]);

classBox("decorators", 800, 470, 720, 220, "gray", "SuspendDecorators", [
  "+ ofSupplier() / ofRunnable()",
  "+ ofFunction1() / ofFunction2()",
  "+ returns a decorator builder"
], { stereotype: "<<object>>" });

add(`<text class="clusterLabel" x="112" y="762">Decorator builder classes returned by SuspendDecorators</text>`);
classBox("supplier", 112, 812, 540, 216, "blue", "DecoratorForSuspendSupplier<T>", [
  "+ withCircuitBreaker(policy)",
  "+ withRetry(policy) / withFallback(...)",
  "+ withCache(Cache<K,T>)",
  "+ decorate() / invoke()"
]);
classBox("function1", 890, 812, 540, 216, "green", "DecoratorForSuspendFunction1<T,R>", [
  "+ withXxx(policy)",
  "+ withCache(Cache<T,R>)",
  "+ withSuspendCache(SuspendCache<T,R>)",
  "+ decorate() / invoke(input)"
]);
classBox("function2", 1668, 812, 540, 216, "teal", "DecoratorForSuspendFunction2<T,U,R>", [
  "+ withCircuitBreaker(policy)",
  "+ withRetry(policy) / withRateLimit(policy)",
  "+ withBulkhead(policy) / withTimeLimiter(policy)",
  "+ decorate() / invoke(t, u)"
]);

add(`<text class="clusterLabel" x="112" y="1120">Cache contract and direct JCache-backed implementation</text>`);
classBox("r4jcache", 112, 1160, 420, 192, "pink", "Cache<K,V>", [
  "+ computeIfAbsent(key)",
  "Resilience4j facade",
  "no public backing JCache accessor"
], { stereotype: "<<external>>" });
classBox("suspendcache", 652, 1160, 472, 222, "purple", "SuspendCache<K,V>", [
  "+ computeIfAbsent(key, loader)",
  "+ containsKey(key)",
  "+ metrics / eventPublisher",
  "+ of(jcache)"
], { stereotype: "<<interface>>" });
classBox("suspendcacheimpl", 1252, 1160, 456, 222, "purple", "SuspendCacheImpl<K,V>", [
  "- keyLocks: ConcurrentHashMap",
  "+ jcache: javax.cache.Cache",
  "+ emits hit / miss / error events",
  "+ per-key Mutex on misses"
]);
classBox("jcache", 1836, 1160, 372, 192, "orange", "javax.cache.Cache<K,V>", [
  "+ containsKey(key)",
  "+ get(key) / put(key, value)",
  "blocking provider boundary"
], { stereotype: "<<external>>" });

edge("e-ext-policy", "M 1172 382 L 1172 432 L 460 432 L 460 382", "uses", "openArrow", "uses policies", 680, 421);
edge("e-fallback", "M 1452 295 L 1578 295", "usesGreen", "openArrow");
edge("e-decorators-supplier", "M 880 690 L 880 732 L 382 732 L 382 812", "create", "openArrow", "creates", 600, 714);
edge("e-decorators-function1", "M 1160 690 L 1160 812", "create", "openArrow", "creates", 1242, 756);
edge("e-decorators-function2", "M 1440 690 L 1440 732 L 1938 732 L 1938 812", "create", "openArrow", "creates", 1712, 714);

edge("e-supplier-policy", "M 382 812 L 382 512 L 812 512", "uses", "openArrow", "wraps supplier", 566, 538);
edge("e-function1-policy", "M 1160 812 L 1160 690", "usesGreen", "openArrow");
edge("e-function2-policy", "M 1938 812 L 1938 512 L 1520 512", "usesGreen", "openArrow", "wraps bi-function", 1748, 538);

edge("e-function1-cache", "M 890 930 L 760 930 L 760 1160", "usesGreen", "openArrow");
edge("e-supplier-cache", "M 382 1028 L 382 1072 L 74 1072 L 74 1246 L 112 1246", "uses", "openArrow");
edge("e-impl", "M 1252 1261 L 1124 1261", "impl", "hollowTriangle", "implements", 1190, 1238);
edge("e-jcache", "M 1708 1261 L 1836 1261", "ref", "openArrow", "has reference", 1772, 1240);

add(`<g id="legend">`);
add(`<path class="uses" d="M 112 1480 H 178" marker-end="url(#openArrow)"/><text class="legendText" x="194" y="1486">uses</text>`);
add(`<path class="impl" d="M 290 1480 H 356" marker-end="url(#hollowTriangle)"/><text class="legendText" x="376" y="1486">implements</text>`);
add(`<path class="ref" d="M 540 1480 H 606" marker-end="url(#openArrow)"/><text class="legendText" x="622" y="1486">has/reference</text>`);
add(`</g>`);

add(`</svg>`);
writeFileSync(out, `${lines.join("\n")}\n`);
console.log(out);
