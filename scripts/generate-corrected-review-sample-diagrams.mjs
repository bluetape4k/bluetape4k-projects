#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";
import { join } from "node:path";

const OUT = join(process.cwd(), "docs/images/readme-diagrams");
const rsvg = "/opt/homebrew/bin/rsvg-convert";

const colors = {
  blue: ["#E8F3FF", "#5B8DEF", "#4F83BF"],
  green: ["#EAF7EF", "#58A978", "#3E9868"],
  teal: ["#E9F7F6", "#45A7A1", "#2E8F89"],
  amber: ["#FFF3D9", "#D6A441", "#B9851B"],
  pink: ["#FDECEF", "#DC6B82", "#C94D68"],
  purple: ["#F1ECFF", "#8A72D6", "#755BC6"],
  olive: ["#EEF6D9", "#8BA84D", "#718A35"],
  gray: ["#F2F5F9", "#9AA8B8", "#758297"],
};

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
}

function wrap(text, max = 28) {
  const words = String(text).split(/\s+/);
  const lines = [];
  let line = "";
  for (const word of words) {
    if ((line + " " + word).trim().length > max && line) {
      lines.push(line);
      line = word;
    } else {
      line = (line + " " + word).trim();
    }
  }
  if (line) lines.push(line);
  return lines;
}

function wrapIdentifier(text, max = 18) {
  if (String(text).includes(" ")) return wrap(text, max);
  const parts = String(text).match(/[A-Z]?[a-z0-9]+|[A-Z]+(?=[A-Z]|$)/g) || [String(text)];
  const lines = [];
  let line = "";
  for (const part of parts) {
    if ((line + part).length > max && line) {
      lines.push(line);
      line = part;
    } else {
      line += part;
    }
  }
  if (line) lines.push(line);
  return lines;
}

function base(width, height, title, subtitle, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title)}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/></filter>
  <marker id="arrow" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="context-stroke"/></marker>
  <marker id="seqArrow-blue" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="${colors.blue[2]}"/></marker>
  <marker id="seqArrow-green" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="${colors.green[2]}"/></marker>
  <marker id="seqArrow-teal" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="${colors.teal[2]}"/></marker>
  <marker id="seqArrow-amber" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="${colors.amber[2]}"/></marker>
  <marker id="seqArrow-purple" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="${colors.purple[2]}"/></marker>
  <marker id="seqArrow-gray" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="${colors.gray[2]}"/></marker>
  <marker id="inherit" markerWidth="8" markerHeight="7" refX="7" refY="3.5" orient="auto" markerUnits="strokeWidth"><path d="M 1 1 L 7 3.5 L 1 6 Z" fill="#fff" stroke="context-stroke" stroke-width="1.4"/></marker>
  <style>
    .canvas{fill:#F6F9FC}.frame{fill:#fff;stroke:#C7D7E7;stroke-width:3;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:#22344A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#536476}
    .panel{fill:#F7FBFF;stroke:#D6E3EF;stroke-width:2}.panelTitle{font-family:"Architects Daughter";font-size:24px;fill:#31445A}
    .card{filter:url(#shadow);stroke-width:2}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:#22344A}.detail{font-family:"Comic Mono";font-size:13px;fill:#42556B}
    .classTitle{font-family:"Architects Daughter";font-size:23px;fill:#22344A}.stereo{font-family:"Comic Mono";font-size:10px;fill:#627184}.member{font-family:"Comic Mono";font-size:11px;fill:#102033}
    .flow{fill:none;stroke-width:2.8;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}.inherit{fill:none;stroke:#758297;stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#inherit)}.muted{stroke:#A7B4C3;stroke-dasharray:7 7}
    .lifeline{stroke:#A7B4C3;stroke-width:1.8;stroke-dasharray:8 8}.seq{fill:none;stroke-width:3;stroke-linecap:round}.seqReturn{fill:none;stroke-width:2.7;stroke-linecap:round;stroke-dasharray:8 7}
    .labelPill{fill:#fff;stroke:#D6E3EF;stroke-width:1.4}.altBox{fill:#FFFFFF;fill-opacity:.42;stroke:#D6A441;stroke-width:1.8;stroke-dasharray:8 8}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="32" y="28" width="${width - 64}" height="${height - 56}" rx="30"/>
<text class="title" x="68" y="84">${esc(title)}</text>
<text class="subtitle" x="72" y="116">${esc(subtitle)}</text>
${body}
</svg>
`;
}

function panel(x, y, w, h, title) {
  return `<g><rect class="panel" x="${x}" y="${y}" width="${w}" height="${h}" rx="22"/><text class="panelTitle" x="${x + 24}" y="${y + 34}">${esc(title)}</text></g>`;
}

function card(id, x, y, w, h, title, detail, color = "blue") {
  const [fill, stroke] = colors[color] || colors.gray;
  const lines = Array.isArray(detail) ? detail : wrap(detail || "", 30);
  const titleLines = wrapIdentifier(title, w > 280 ? 24 : 19).slice(0, 2);
  const titleLineH = 24;
  const detailLineH = 18;
  const gap = lines.length > 0 ? 12 : 0;
  const blockH = titleLines.length * titleLineH + gap + lines.length * detailLineH;
  const titleStart = Math.round(y + h / 2 - blockH / 2 + 18);
  const detailStart = Math.round(titleStart + titleLines.length * titleLineH + gap);
  const titleSvg = titleLines.map((line, i) => `<text class="cardTitle" x="${x + w / 2}" y="${titleStart + i * 24}" text-anchor="middle">${esc(line)}</text>`).join("");
  const detailSvg = lines.map((line, i) => `<text class="detail" x="${x + w / 2}" y="${detailStart + i * 18}" text-anchor="middle">${esc(line)}</text>`).join("\n");
  return `<g id="${id}"><rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="14" fill="${fill}" stroke="${stroke}"/>${titleSvg}${detailSvg}</g>`;
}

function classBox(id, x, y, w, h, title, stereo, members, color = "blue") {
  const [fill, stroke] = colors[color] || colors.gray;
  const titleLines = wrapIdentifier(title, w > 285 ? 22 : 17).slice(0, 2);
  const titleStart = titleLines.length > 1 ? y + 42 : y + 48;
  const headerBottom = titleLines.length > 1 ? y + 82 : y + 66;
  const lineH = 17;
  const memberStart = lowerCompartmentBaselineStart(headerBottom, y + h, members.length, lineH);
  const titleSvg = titleLines.map((line, i) => `<text class="classTitle" x="${x + w / 2}" y="${titleStart + i * 22}" text-anchor="middle">${esc(line)}</text>`).join("");
  const memberSvg = members.map((line, i) => `<text class="member" x="${x + 18}" y="${memberStart + i * lineH}">${esc(line)}</text>`).join("\n");
  return `<g id="${id}"><rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="7" fill="${fill}" stroke="${stroke}"/><line x1="${x}" y1="${headerBottom}" x2="${x + w}" y2="${headerBottom}" stroke="${stroke}" stroke-width="1.5"/><text class="stereo" x="${x + w / 2}" y="${y + 20}" text-anchor="middle">${esc(stereo)}</text>${titleSvg}${memberSvg}</g>`;
}

function lowerCompartmentBaselineStart(headerBottom, cardBottom, lineCount, lineH) {
  if (lineCount <= 0) return Math.round((headerBottom + cardBottom) / 2);
  const lowerCenter = (headerBottom + cardBottom) / 2;
  const averageBaselineOffset = ((lineCount - 1) * lineH) / 2;
  return Math.round(lowerCenter + 4 - averageBaselineOffset);
}

function route(d, color = "gray", klass = "flow") {
  return `<path class="${klass}" d="${d}" stroke="${colors[color]?.[2] || colors.gray[2]}"/>`;
}

function line(x1, y1, x2, y2, color = "gray") {
  return route(`M${x1} ${y1} L${x2} ${y2}`, color);
}

function inherit(points) {
  return `<path class="inherit" d="${points}"/>`;
}

function inheritRoute(points, color = "gray", dashed = false) {
  const dash = dashed ? "stroke-dasharray:8 7;" : "";
  return `<path class="inherit" d="${points}" style="stroke:${colors[color]?.[2] || colors.gray[2]};${dash}"/>`;
}

function dashedRoute(d, color = "gray") {
  return `<path class="flow" d="${d}" stroke="${colors[color]?.[2] || colors.gray[2]}" stroke-dasharray="9 8"/>`;
}

function footer(x, y, w, text) {
  return `<g><rect x="${x}" y="${y}" width="${w}" height="42" rx="12" fill="#FFFFFF" stroke="#D6E3EF" stroke-width="1.6"/><text class="detail" x="${x + w / 2}" y="${y + 26}" text-anchor="middle">${esc(text)}</text></g>`;
}

function participant(id, x, y, w, h, title, color = "blue", bottom = 900) {
  const [fill, stroke] = colors[color] || colors.gray;
  const titleLines = wrapIdentifier(title, 18).slice(0, 2);
  const titleStart = titleLines.length > 1 ? y + 30 : y + 45;
  const titleSvg = titleLines.map((line, i) => `<text class="cardTitle" x="${x + w / 2}" y="${titleStart + i * 24}" text-anchor="middle">${esc(line)}</text>`).join("");
  return `<g id="${id}"><rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}"/>${titleSvg}<line class="lifeline" x1="${x + w / 2}" y1="${y + h}" x2="${x + w / 2}" y2="${bottom}"/></g>`;
}

function sequenceMessage(id, fromX, toX, y, label, color = "blue", kind = "seq", number = "") {
  const stroke = colors[color]?.[2] || colors.gray[2];
  const markerColor = colors[color] ? color : "gray";
  const width = Math.max(190, label.length * 7.2 + 82);
  const mid = (fromX + toX) / 2;
  const pillY = y - 30;
  const pillX = mid - width / 2;
  const badge = number === "" ? "" : `<circle cx="${pillX + 19}" cy="${pillY}" r="12" fill="${stroke}"/><text class="detail" x="${pillX + 19}" y="${pillY + 1}" text-anchor="middle" dominant-baseline="middle" style="fill:#fff;font-size:12px">${esc(number)}</text>`;
  return `<g id="${id}"><rect class="labelPill" x="${pillX}" y="${pillY - 13}" width="${width}" height="26" rx="7"/>${badge}<text class="detail" x="${mid + 16}" y="${pillY + 1}" text-anchor="middle" dominant-baseline="middle">${esc(label)}</text><path class="${kind}" d="M${fromX} ${y} L${toX} ${y}" stroke="${stroke}" marker-end="url(#seqArrow-${markerColor})"/></g>`;
}

function activation(x, y, h, color = "gray") {
  const [fill, stroke] = colors[color] || colors.gray;
  return `<rect x="${x - 8}" y="${y}" width="16" height="${h}" rx="5" fill="${fill}" stroke="${stroke}" stroke-width="1.4"/>`;
}

function write(name, svg) {
  const svgPath = join(OUT, `${name}.svg`);
  const pngPath = join(OUT, `${name}.png`);
  writeFileSync(svgPath, svg);
  execFileSync(rsvg, ["--format=png", "--output", pngPath, svgPath], { stdio: "inherit" });
  console.log(`${name}.png`);
}

function okioHierarchy() {
  const body = [
    panel(82, 154, 1238, 1245, "Source family"),
    panel(1380, 154, 1238, 1245, "Sink family"),

    classBox("source", 535, 225, 330, 120, "Source", "interface, okio", ["+read(sink, byteCount)", "+close()"], "green"),
    classBox("input", 190, 430, 300, 120, "InputStreamSource", "class", ["-input: InputStream", "+read(...)"], "amber"),
    classBox("fsource", 900, 430, 300, 100, "ForwardingSource", "class", ["#delegate: Source"], "teal"),
    classBox("decomp", 125, 660, 285, 138, "DecompressableSource", "open class", ["-decoderBuffer: Buffer", "-compressor: Compressor"], "amber"),
    classBox("b64source", 445, 660, 285, 112, "AbstractBase64Source", "abstract", ["#decodeBase64Bytes(...)"], "olive"),
    classBox("tdec", 765, 660, 250, 112, "TinkDecryptSource", "open class", ["-encryptor: TinkEncryptor"], "pink"),
    classBox("daeaddec", 1045, 660, 250, 132, "DaeadChunkDecryptSource", "class", ["-daead: DeterministicAead", "-maxCiphertextLength"], "purple"),
    classBox("streamdec", 125, 925, 285, 132, "StreamingDecompressSource", "open class", ["-decompressingStream", "-compressor"], "pink"),
    classBox("okb64s", 445, 925, 250, 96, "OkioBase64Source", "class", ["base64 variant"], "green"),
    classBox("apb64s", 725, 917, 250, 112, "ApacheBase64Source", "class", ["base64 variant"], "olive"),

    classBox("sink", 1830, 225, 330, 128, "Sink", "interface, okio", ["+write(source, byteCount)", "+flush()", "+close()"], "blue"),
    classBox("output", 1488, 430, 300, 120, "OutputStreamSink", "class", ["-output: OutputStream", "+write(...)"], "purple"),
    classBox("fsink", 2200, 430, 300, 100, "ForwardingSink", "class", ["#delegate: Sink"], "pink"),
    classBox("compress", 1425, 660, 285, 138, "CompressableSink", "open class", ["-plainBuffer: Buffer", "-compressor: Compressor"], "green"),
    classBox("b64sink", 1745, 660, 285, 112, "AbstractBase64Sink", "abstract", ["#getEncodedBuffer(...)"], "blue"),
    classBox("tenc", 2065, 660, 250, 112, "TinkEncryptSink", "open class", ["-encryptor: TinkEncryptor"], "purple"),
    classBox("daeadenc", 2345, 660, 250, 132, "DaeadChunkEncryptSink", "class", ["-daead: DeterministicAead", "-chunkSize"], "teal"),
    classBox("streamcmp", 1425, 925, 285, 132, "StreamingCompressSink", "open class", ["-compressingStream", "-compressor"], "pink"),
    classBox("okb64k", 1745, 925, 250, 96, "OkioBase64Sink", "class", ["base64 variant"], "green"),
    classBox("apb64k", 2025, 925, 250, 96, "ApacheBase64Sink", "class", ["base64 variant"], "blue"),

    inherit("M340 430 L340 388 L650 388 L650 345"),
    inherit("M1050 430 L1050 388 L750 388 L750 345"),
    inherit("M268 660 L268 582 L960 582 L960 530"),
    inherit("M588 660 L588 594 L1020 594 L1020 530"),
    inherit("M890 660 L890 606 L1080 606 L1080 530"),
    inherit("M1170 660 L1170 618 L1140 618 L1140 530"),
    inherit("M268 925 L268 855 L268 798"),
    inherit("M570 925 L570 848 L535 848 L535 772"),
    inherit("M850 917 L850 860 L650 860 L650 772"),

    inherit("M1638 430 L1638 390 L1940 390 L1940 353"),
    inherit("M2350 430 L2350 390 L2050 390 L2050 353"),
    inherit("M1568 660 L1568 582 L2250 582 L2250 530"),
    inherit("M1888 660 L1888 594 L2315 594 L2315 530"),
    inherit("M2190 660 L2190 606 L2385 606 L2385 530"),
    inherit("M2470 660 L2470 618 L2445 618 L2445 530"),
    inherit("M1568 925 L1568 855 L1568 798"),
    inherit("M1870 925 L1870 848 L1835 848 L1835 772"),
    inherit("M2150 925 L2150 860 L1945 860 L1945 772"),
    footer(180, 1448, 2340, "Source-checked UML: every adapter either implements Source/Sink directly or extends the matching Forwarding family."),
  ].join("\n");
  write("io-okio-diagram-01", base(2700, 1530, "Sink / Source Adapter Hierarchy", "Okio adapters are split by family so inheritance routes stay short, source-accurate, and readable.", body));
}

function workflowOverview() {
  const body = [
    panel(86, 168, 1665, 385, "Definition policy layer"),
    card("definition", 720, 265, 330, 118, "WorkflowDefinition", "composition root for work order", "purple"),
    card("seq", 170, 245, 245, 86, "Sequential", "ordered work chain", "blue"),
    card("cond", 450, 205, 255, 86, "Conditional", "branch selector", "green"),
    card("par", 450, 390, 255, 86, "Parallel", "shared cancellable ctx", "amber"),
    card("rep", 1085, 205, 245, 86, "Repeat", "loop while valid", "pink"),
    card("retry", 1085, 390, 245, 86, "Retry", "bounded backoff", "teal"),
    card("policy", 1410, 300, 245, 90, "FlowPolicy", "stop / continue / retry", "gray"),

    panel(190, 635, 1455, 245, "Execution layer"),
    card("caller", 270, 715, 250, 92, "Caller", "context.Context", "blue"),
    card("runner", 720, 690, 330, 124, "Workflow runner", "Run(ctx) -> WorkReport", "purple"),
    card("work", 1245, 705, 310, 104, "Work / SuspendWork", "domain action", "amber"),

    panel(245, 1000, 1338, 205, "Result contract"),
    card("ctx", 335, 1072, 285, 88, "WorkContext", "shared mutable map", "blue"),
    card("report", 775, 1062, 320, 98, "WorkReport", "success / partial / failure", "green"),
    card("status", 1270, 1072, 250, 88, "Caller result", "completed report", "gray"),

    route("M578 291 L578 330 L720 330", "green"),
    route("M578 390 L578 350 L720 350", "amber"),
    route("M1208 291 L1208 315 L1050 315", "pink"),
    route("M1208 390 L1208 365 L1050 365", "teal"),
    route("M1050 345 L1410 345", "gray"),
    route("M885 383 L885 690", "purple"),
    route("M520 761 L720 761", "blue"),
    route("M1050 752 L1245 752", "purple"),
    route("M885 814 L885 965 L935 965 L935 1062", "green"),
    route("M805 814 L805 955 L478 955 L478 1072", "blue"),
    route("M1095 1111 L1270 1111", "gray"),
    footer(235, 1248, 1365, "Definitions stay in the policy layer. One runner lane executes work, updates context, and returns a single report."),
  ].join("\n");
  write("utils-workflow-diagram-01", base(1840, 1335, "Workflow Concept Overview", "Source-grounded workflow concepts arranged as policy, execution, and result contract layers.", body));
}

function lettuceContracts() {
  const body = [
    panel(70, 160, 320, 560, "CAS replace()"),
    panel(430, 160, 320, 560, "Bulk removal"),
    panel(790, 160, 320, 560, "JCache close()"),
    panel(1150, 160, 320, 560, "Memoizer recovery"),
    card("cas1", 105, 248, 250, 84, "EVALSHA CAS", "20-byte script digest", "blue"),
    card("cas2", 105, 388, 250, 84, "NOSCRIPT fallback", "retry with EVAL source", "amber"),
    card("cas3", 105, 528, 250, 84, "Same semantics", "transparent to callers", "green"),
    card("bulk1", 465, 248, 250, 84, "UNLINK keys", "non-blocking delete", "green"),
    card("bulk2", 465, 388, 250, 84, "Redis background free", "large values do not block", "pink"),
    card("bulk3", 465, 528, 250, 84, "O(1) round trip", "DEL semantics preserved", "teal"),
    card("jc1", 825, 248, 250, 84, "Close wrappers", "listeners and resources", "amber"),
    card("jc2", 825, 388, 250, 84, "Keep cache data", "JSR-107 contract", "purple"),
    card("jc3", 825, 528, 250, 84, "Explicit clear()", "only when caller asks", "green"),
    card("mem1", 1185, 248, 250, 84, "In-flight promise", "coordination only", "blue"),
    card("mem2", 1185, 388, 250, 84, "Failure/cancel", "remove exact promise", "pink"),
    card("mem3", 1185, 528, 250, 84, "Recompute next call", "failed value not cached", "green"),
    line(230, 332, 230, 388), line(230, 472, 230, 528), line(590, 332, 590, 388), line(590, 472, 590, 528), line(950, 332, 950, 388), line(950, 472, 950, 528), line(1310, 332, 1310, 388), line(1310, 472, 1310, 528),
    `<text class="detail" x="750" y="780" text-anchor="middle">Read top to bottom within each contract. Every path preserves caller-visible cache semantics without adding extra bottom whitespace.</text>`,
  ].join("\n");
  write("cache-cache-lettuce-diagram-03", base(1540, 840, "Lettuce Cache Stability Contracts", "Operational guarantees shared by NearCache, JCache, and memoizer implementations.", body));
}

function geoArchitecture() {
  const body = [
    panel(76, 160, 1620, 170, "Public API"),
    panel(76, 390, 1620, 170, "Adapter layer"),
    panel(76, 620, 1620, 170, "Spatial model"),
    panel(76, 850, 1620, 170, "External systems"),
    card("geocode", 215, 215, 330, 84, "Geocode API", "address and location lookup", "blue"),
    card("geohash", 725, 215, 330, 84, "GeoHash API", "hash, bbox, neighbors", "green"),
    card("geoip", 1235, 215, 330, 84, "GeoIP API", "city/country lookup", "purple"),
    card("maps", 300, 445, 460, 86, "Map provider adapters", "GoogleAddressFinder / Bing client", "amber"),
    card("hashcore", 725, 675, 330, 84, "GeoHash core", "base32 encoding and decoding", "green"),
    card("queries", 1120, 675, 330, 84, "Spatial queries", "circle and bbox search", "olive"),
    card("max", 1235, 445, 330, 84, "MaxMind reader", "DatabaseReader extensions", "teal"),
    card("providers", 300, 905, 460, 84, "Provider HTTP APIs", "Google / Bing location data", "amber"),
    card("datasets", 1235, 905, 330, 84, "GeoLite2 databases", "bundled mmdb files", "teal"),
    route("M380 299 L380 445", "blue"),
    route("M890 299 L890 675", "green"),
    route("M1400 299 L1400 445", "purple"),
    route("M530 531 L530 905", "amber"),
    route("M1400 529 L1400 560 L1605 560 L1605 947 L1565 947", "teal"),
    route("M1055 717 L1120 717", "green"),
    footer(205, 1062, 1360, "Architecture follows the JaVers-style layer stack: public APIs, adapters, spatial models, and external datasets."),
  ].join("\n");
  write("utils-geo-diagram-01", base(1770, 1145, "Geo Utilities Architecture", "Geocode, GeoHash, and GeoIP APIs are separated from provider adapters, spatial models, and datasets.", body));
}

function coreClassStructure() {
  const body = [
    panel(80, 160, 1540, 195, "Encoding contracts"),
    classBox("base58", 140, 215, 250, 116, "Base58", "object", ["+encode(data)", "+decode(text)"], "blue"),
    classBox("base62", 500, 215, 250, 116, "Base62", "object", ["+encode(number)", "+decode(text)"], "teal"),
    classBox("enc", 860, 215, 250, 116, "StringEncoder", "interface", ["+encode(bytes)", "+decode(text)"], "green"),
    classBox("hex", 1220, 215, 250, 116, "HexStringEncoder", "class", ["+encode(bytes)", "+decode(text)"], "amber"),
    panel(80, 430, 1540, 510, "Range model"),
    classBox("range", 670, 475, 300, 150, "Range", "interface", ["+first / +last", "+contains(value)", "+isEmpty()"], "amber"),
    classBox("cc", 150, 760, 270, 128, "ClosedClosedRange", "class", ["start inclusive", "end inclusive"], "green"),
    classBox("co", 510, 760, 270, 128, "ClosedOpenRange", "class", ["start inclusive", "end exclusive"], "blue"),
    classBox("oc", 870, 760, 270, 128, "OpenClosedRange", "class", ["start exclusive", "end inclusive"], "pink"),
    classBox("oo", 1230, 760, 270, 128, "OpenOpenRange", "class", ["start exclusive", "end exclusive"], "teal"),
    panel(80, 990, 1540, 205, "Value object support"),
    classBox("vo", 410, 1050, 300, 112, "ValueObject", "interface", ["+Serializable"], "pink"),
    classBox("avo", 940, 1040, 300, 140, "AbstractValueObject", "abstract", ["+equals(other)", "+hashCode()", "+equalProperties(*)"], "blue"),
    panel(620, 1260, 540, 180, "Validation helpers"),
    classBox("req", 740, 1315, 300, 118, "RequireSupport", "extension functions", ["requireNotNull/Empty", "requireGt/Ge/Lt/Le"], "purple"),
    inherit("M1220 273 L1110 273"),
    inherit("M285 760 L285 690 L715 690 L715 625"),
    inherit("M645 760 L645 720 L785 720 L785 625"),
    inherit("M1005 760 L1005 720 L855 720 L855 625"),
    inherit("M1365 760 L1365 690 L925 690 L925 625"),
    inherit("M940 1110 L710 1110"),
  ].join("\n");
  write("bluetape4k-core-diagram-02", base(1700, 1515, "Core Class Structure", "Related contracts are grouped by encoding, ranges, value objects, and validation helpers.", body));
}

function workflowSequenceSample() {
  const xs = {
    caller: 250,
    runner: 610,
    definition: 970,
    work: 1330,
    report: 1610,
  };
  const body = [
    participant("seq-caller", 150, 170, 200, 78, "Caller", "blue"),
    participant("seq-runner", 500, 170, 220, 78, "WorkflowRunner", "purple"),
    participant("seq-definition", 855, 170, 230, 78, "WorkflowDefinition", "green"),
    participant("seq-work", 1175, 170, 310, 78, "Work / SuspendWork", "amber"),
    participant("seq-report", 1510, 170, 200, 78, "WorkReport", "teal"),
    activation(xs.runner, 320, 575, "purple"),
    activation(xs.definition, 382, 90, "green"),
    activation(xs.work, 535, 125, "amber"),
    activation(xs.report, 725, 95, "teal"),
    sequenceMessage("m1", xs.caller, xs.runner, 320, "run(ctx, definition)", "blue", "seq", 1),
    sequenceMessage("m2", xs.runner, xs.definition, 390, "resolve next step", "purple", "seq", 2),
    sequenceMessage("m3", xs.definition, xs.runner, 460, "selected work order", "green", "seqReturn", 3),
    `<g><rect class="altBox" x="760" y="500" width="700" height="178" rx="18"/><rect class="labelPill" x="785" y="486" width="240" height="26" rx="7"/><text class="detail" x="905" y="500" text-anchor="middle" dominant-baseline="middle">alt retry policy allows execution</text></g>`,
    sequenceMessage("m4", xs.runner, xs.work, 570, "execute(ctx)", "amber", "seq", 4),
    sequenceMessage("m5", xs.work, xs.runner, 650, "work result or retry signal", "amber", "seqReturn", 5),
    sequenceMessage("m6", xs.runner, xs.report, 735, "build WorkReport", "teal", "seq", 6),
    sequenceMessage("m7", xs.report, xs.runner, 805, "success / partial / failure", "teal", "seqReturn", 7),
    sequenceMessage("m8", xs.runner, xs.caller, 885, "completed report", "blue", "seqReturn", 8),
    footer(260, 910, 1320, "Sequence sample keeps labels above arrows, lifelines as dashed stems, and alt semantics outside message paths."),
  ].join("\n");
  write("utils-workflow-sequence-sample-01", base(1860, 1055, "Workflow Execution Sequence", "Participants, lifelines, returns, and retry branch semantics without label or line overlap.", body));
}

function jcacheNearCache() {
  const body = [
    panel(88, 165, 1475, 240, "Coroutine JCache composition"),
    classBox("front", 150, 235, 335, 118, "CaffeineSuspendJCache", "front tier", ["local Caffeine AsyncCache", "low-latency reads"], "blue"),
    classBox("near", 630, 225, 360, 138, "SuspendNearJCache", "2-tier coordinator", ["read-through front/back", "optional listener propagation"], "green"),
    classBox("back", 1135, 235, 335, 118, "HazelcastSuspendJCache", "back tier", ["cluster JCache wrapper", "shared distributed data"], "purple"),
    line(485, 294, 630, 294, "blue"),
    line(990, 294, 1135, 294, "purple"),

    panel(88, 500, 1475, 255, "Factory paths"),
    card("without", 155, 580, 310, 90, "withoutListener(front, back)", "safe when back listener cannot serialize front cache", "amber"),
    card("invoke", 640, 565, 340, 112, "invoke(front, back)", "registers back-cache listener when provider supports it", "green"),
    card("provider", 1150, 580, 320, 90, "Provider DSL", "Hazelcast/Lettuce/Redisson factories compose tiers", "teal"),
    route("M310 353 L310 580", "blue"),
    route("M810 363 L810 565", "green"),
    route("M1302 353 L1302 580", "purple"),
    route("M465 625 L640 625", "amber"),
    route("M980 625 L1150 625", "teal"),

    panel(88, 850, 1475, 210, "Read/write semantics"),
    card("read", 185, 920, 290, 82, "Read path", "front hit or back load", "blue"),
    card("write", 620, 920, 290, 82, "Write path", "front + back update", "green"),
    card("invalidate", 1055, 920, 320, 82, "Invalidation", "listener or explicit clear", "pink"),
    route("M475 961 L620 961", "blue"),
    route("M910 961 L1055 961", "green"),
    footer(225, 1112, 1200, "Adjacent front/coordinator/back classes use straight connectors; policy and runtime details stay in separate layers."),
  ].join("\n");
  write("cache-cache-core-diagram-05", base(1650, 1205, "JCache NearCache Structure", "SuspendNearJCache composes a local Caffeine front tier with provider-backed suspend JCache implementations.", body));
}

function cacheHibernateNearCacheStructure() {
  const body = [
    panel(86, 165, 1400, 170, "Hibernate integration"),
    card("entity", 155, 220, 300, 82, "EntityManager / Session", "loads entities and collections", "blue"),
    card("region", 590, 210, 360, 102, "Hibernate Cache Region", "entity, collection, query, timestamps", "purple"),
    card("factory", 1090, 220, 300, 82, "RegionFactory", "creates access strategies", "green"),
    route("M455 261 L590 261", "blue"),
    route("M950 261 L1090 261", "purple"),

    panel(86, 450, 1400, 245, "2-tier Near Cache"),
    card("front", 170, 535, 320, 92, "Caffeine front tier", "hot keys in-process", "amber"),
    card("near", 625, 522, 340, 118, "NearCache access strategy", "coordinates hits, misses, and writes", "green"),
    card("redis", 1100, 535, 320, 92, "Lettuce Redis back tier", "shared distributed cache", "teal"),
    line(490, 581, 625, 581, "amber"),
    line(965, 581, 1100, 581, "teal"),
    route("M770 312 L770 522", "purple"),

    panel(86, 815, 1400, 195, "Consistency contract"),
    card("put", 155, 875, 300, 82, "put/update", "write-through to Redis", "blue"),
    card("evict", 590, 875, 300, 82, "evict/clear", "remove local and remote entries", "pink"),
    card("ttl", 1025, 875, 300, 82, "TTL / invalidation", "Redis state governs sharing", "gray"),
    route("M455 916 L590 916", "blue"),
    route("M890 916 L1025 916", "pink"),
    footer(205, 1058, 1165, "The diagram is layered by responsibility instead of a grid: Hibernate API, near-cache storage, then consistency semantics."),
  ].join("\n");
  write("cache-hibernate-cache-lettuce-diagram-01", base(1570, 1150, "Hibernate Lettuce Near Cache", "Hibernate cache regions use a local Caffeine tier and a shared Lettuce Redis tier through one access strategy.", body));
}

function cacheHibernateLayerStructure() {
  const body = [
    panel(86, 165, 1360, 150, "Hibernate contract layer"),
    card("spi", 190, 210, 320, 74, "Hibernate cache SPI", "RegionFactory + access strategies", "purple"),
    card("regions", 770, 210, 320, 74, "Region types", "entity, collection, natural-id, query", "blue"),
    route("M510 247 L770 247", "purple"),

    panel(86, 395, 1360, 175, "bluetape4k cache layer"),
    card("factory", 180, 445, 340, 108, "LettuceCacheRegionFactory", "builds configured cache regions", "green"),
    card("access", 610, 432, 380, 132, "Access strategy adapters", "read/write, non-strict, transactional contracts", "amber"),
    card("codec", 1090, 445, 290, 108, "Serializer / codec", "binary cache payload", "teal"),
    route("M520 499 L610 499", "green"),
    route("M990 499 L1090 499", "amber"),
    route("M350 284 L350 445", "purple"),
    route("M930 284 L930 432", "blue"),

    panel(86, 665, 1360, 170, "Storage layer"),
    card("front", 185, 720, 320, 80, "Caffeine local cache", "near hot-set", "amber"),
    card("redis", 610, 710, 350, 100, "Redis via Lettuce", "shared region data", "teal"),
    card("metrics", 1080, 720, 280, 80, "Metrics/logging", "operational visibility", "gray"),
    route("M800 564 L800 710", "amber"),
    route("M505 760 L610 760", "amber"),
    route("M960 760 L1080 760", "teal"),
    footer(205, 880, 1120, "Layer boundaries explain why Hibernate contracts stay above bluetape4k adapters and storage implementations."),
  ].join("\n");
  write("cache-hibernate-cache-lettuce-diagram-02", base(1530, 970, "Hibernate Cache Layer Structure", "A responsibility stack for Hibernate SPI, bluetape4k adapters, and concrete Caffeine/Redis storage.", body));
}

function dataCassandraApiStructure() {
  const body = [
    panel(86, 165, 1420, 210, "Driver entry points"),
    classBox("cql", 215, 220, 330, 118, "CqlSession", "DataStax driver", ["+execute(statement)", "+prepare(query)"], "blue"),
    classBox("builder", 965, 220, 330, 118, "StatementBuilders", "DSL factory", ["+selectFrom(...)", "+insertInto(...)"], "purple"),

    panel(86, 480, 1420, 230, "Coroutine and DSL extensions"),
    classBox("async", 215, 550, 330, 118, "AsyncCqlSession", "extension facade", ["+executeSuspending(...)", "+executeFlow(...)"], "green"),
    classBox("qb", 965, 540, 340, 145, "QueryBuilderExtensions", "extension functions", ["+whereEq(...)", "+orderBy(...)", "+bind(...)"], "amber"),
    route("M380 338 L380 550", "blue"),
    route("M1135 338 L1135 540", "purple"),

    panel(86, 820, 1420, 190, "Result handling"),
    card("row", 210, 880, 330, 84, "Rows and mapped values", "typed extraction helpers", "teal"),
    card("flow", 610, 880, 330, 84, "Flow<Row>", "streamed query results", "green"),
    card("statement", 1010, 880, 330, 84, "Bound statements", "driver-ready CQL", "amber"),
    route("M380 668 L380 880", "green"),
    route("M1135 685 L1135 790 L1175 790 L1175 880", "amber"),
    route("M545 609 L965 609", "gray"),
    footer(210, 1060, 1170, "AsyncCqlSession now sits directly under CqlSession, and QueryBuilderExtensions sits directly under StatementBuilders."),
  ].join("\n");
  write("data-cassandra-diagram-02", base(1590, 1150, "Cassandra Core API Structure", "Driver types stay above coroutine/session extensions and query-builder helpers to keep the layout short.", body));
}

function dataHibernateHierarchy() {
  const body = [
    panel(80, 160, 1640, 240, "JPA base contracts"),
    classBox("persistable", 180, 230, 310, 122, "Persistable", "interface", ["+getId()", "+isNew()"], "blue"),
    classBox("auditable", 690, 220, 340, 142, "Auditable", "interface", ["+createdAt / updatedAt", "+createdBy / updatedBy"], "green"),
    classBox("versioned", 1230, 230, 310, 122, "Versioned", "interface", ["+version"], "purple"),

    panel(80, 505, 1640, 310, "Entity hierarchy"),
    classBox("mapped", 160, 610, 330, 132, "AbstractJpaEntity", "mapped superclass", ["@MappedSuperclass", "id and equality contract"], "amber"),
    classBox("auditEntity", 610, 590, 370, 170, "AbstractAuditableJpaEntity", "mapped superclass", ["auditing fields", "Spring Data auditing hooks"], "green"),
    classBox("tenantEntity", 1100, 610, 360, 132, "TenantAwareJpaEntity", "optional superclass", ["tenant id", "multi-tenant boundary"], "teal"),
    inheritRoute("M315 610 L315 430 L335 430 L335 352", "blue"),
    inheritRoute("M795 590 L795 430 L860 430 L860 362", "green", true),
    inheritRoute("M1280 610 L1280 430 L1385 430 L1385 352", "purple", true),

    panel(80, 915, 1640, 260, "Concrete domain models"),
    classBox("aggregate", 210, 1000, 320, 126, "Domain aggregate", "entity", ["business identity", "relationships"], "pink"),
    classBox("lookup", 700, 1000, 320, 126, "Lookup entity", "entity", ["stable id", "read-mostly table"], "blue"),
    classBox("join", 1190, 1000, 320, 126, "Join / link entity", "entity", ["association table", "compact lifecycle"], "gray"),
    inheritRoute("M370 1000 L370 878 L325 878 L325 742", "pink"),
    inheritRoute("M860 1000 L860 850 L795 850 L795 760", "blue", true),
    inheritRoute("M1350 1000 L1350 850 L1280 850 L1280 742", "gray", true),
    footer(260, 1225, 1280, "Solid colored lines mark direct inheritance. Dashed colored lines mark optional interface or mapped-superclass relationships."),
  ].join("\n");
  write("data-hibernate-diagram-02", base(1800, 1320, "JPA Entity Class Hierarchy", "Class hierarchy is arranged top-down with colored solid and dashed relationships to avoid ambiguous overlaps.", body));
}

function dataHibernateConverters() {
  const body = [
    panel(86, 165, 1420, 165, "Domain attribute types"),
    card("domain", 150, 210, 320, 96, "Domain value objects", "Money, Id, status, flags", "blue"),
    card("time", 600, 210, 300, 96, "Time values", "Instant, LocalDate, Duration", "green"),
    card("json", 1030, 210, 320, 96, "Structured values", "JSON, arrays, custom payloads", "purple"),

    panel(86, 430, 1420, 250, "AttributeConverter layer"),
    classBox("conv", 185, 512, 320, 132, "AttributeConverter", "JPA contract", ["+convertToDatabaseColumn", "+convertToEntityAttribute"], "amber"),
    classBox("support", 610, 495, 350, 162, "bluetape4k converters", "converter classes", ["Enum/ValueObject converters", "JSON and temporal converters"], "green"),
    classBox("config", 1065, 512, 320, 132, "Hibernate type hints", "module config", ["dialect-aware mapping", "column definition"], "teal"),
    route("M310 306 L310 370 L700 370 L700 495", "blue"),
    route("M750 306 L750 495", "green"),
    route("M1190 306 L1190 512", "purple"),
    inheritRoute("M610 620 L555 620 L555 670 L345 670 L345 644", "amber", true),
    route("M960 580 L1065 580", "teal"),

    panel(86, 795, 1420, 175, "Database column shape"),
    card("scalar", 210, 850, 280, 78, "Scalar columns", "varchar, bigint, timestamp", "blue"),
    card("jsonb", 610, 850, 280, 78, "JSON columns", "json/jsonb document value", "purple"),
    card("enum", 1010, 850, 280, 78, "Enum/code columns", "stable persisted code", "green"),
    route("M735 657 L735 742 L520 742 L520 889 L490 889", "blue"),
    route("M785 657 L785 850", "purple"),
    route("M835 657 L835 742 L1150 742 L1150 850", "green"),
    footer(210, 982, 1165, "Converter cards are separated from layer labels; the horizontal regions now explain source type, converter, and column shape."),
  ].join("\n");
  write("data-hibernate-diagram-03", base(1590, 1110, "AttributeConverter Type Mapping", "Domain attributes pass through JPA converters before reaching stable database column shapes.", body));
}

function idGeneratorKtorLayered() {
  const body = [
    panel(86, 165, 1370, 160, "HTTP API layer"),
    card("routes", 160, 220, 320, 74, "Ktor routes", "explicit and generic endpoints", "blue"),
    card("serialization", 600, 220, 320, 74, "JSON response model", "typed id payloads", "green"),
    card("errors", 1040, 220, 300, 74, "Status handling", "validation and failures", "pink"),
    route("M480 257 L600 257", "blue"),
    route("M920 257 L1040 257", "green"),

    panel(86, 430, 1370, 190, "Application service layer"),
    card("service", 250, 500, 360, 86, "IdGenerator service", "selects generator by route", "purple"),
    card("registry", 850, 500, 360, 86, "Generator registry", "Snowflake, UUID, ULID families", "amber"),
    route("M320 294 L320 500", "blue"),
    route("M610 543 L850 543", "purple"),

    panel(86, 735, 1370, 190, "Generator runtime layer"),
    card("clock", 180, 805, 300, 82, "Clock and node id", "timestamp + worker identity", "teal"),
    card("algorithm", 610, 795, 340, 102, "ID algorithms", "monotonic, sortable, random", "green"),
    card("metrics", 1080, 805, 280, 82, "Observability", "latency and throughput probes", "gray"),
    route("M1030 586 L1030 805", "amber"),
    route("M480 846 L610 846", "teal"),
    route("M950 846 L1080 846", "green"),
    footer(205, 975, 1130, "Layered layout replaces the old grid: request handling, service selection, and generator runtime are visually separated."),
  ].join("\n");
  write("examples-ktor-idgenerator-ktor-demo-diagram-01", base(1540, 1065, "IdGenerator Ktor Demo Architecture", "Ktor routes delegate to a registry-backed generator service and return typed JSON responses.", body));
}

function idGeneratorSpringLayered() {
  const body = [
    panel(86, 165, 1370, 160, "Spring Web layer"),
    card("controller", 160, 220, 330, 74, "REST controllers", "explicit and generic endpoints", "blue"),
    card("dto", 610, 220, 300, 74, "Response DTOs", "id value and metadata", "green"),
    card("advice", 1040, 220, 300, 74, "Exception advice", "validation and status mapping", "pink"),
    route("M490 257 L610 257", "blue"),
    route("M910 257 L1040 257", "green"),

    panel(86, 430, 1370, 190, "Application service layer"),
    card("service", 250, 500, 360, 86, "IdGenerator service", "Spring-managed generator facade", "purple"),
    card("props", 850, 500, 360, 86, "Configuration properties", "node id and generator choices", "amber"),
    route("M325 294 L325 500", "blue"),
    route("M610 543 L850 543", "purple"),

    panel(86, 735, 1370, 190, "Generator runtime layer"),
    card("clock", 180, 805, 300, 82, "Clock source", "timestamp and sequence guard", "teal"),
    card("algorithm", 610, 795, 340, 102, "ID algorithms", "Snowflake, ULID, UUID variants", "green"),
    card("actuator", 1080, 805, 280, 82, "Actuator metrics", "latency and throughput", "gray"),
    route("M1030 586 L1030 805", "amber"),
    route("M480 846 L610 846", "teal"),
    route("M950 846 L1080 846", "green"),
    footer(205, 975, 1130, "Layered layout replaces the old grid and keeps Spring web concerns separate from generator mechanics."),
  ].join("\n");
  write("examples-spring-boot-idgenerator-spring-boot-demo-diagram-01", base(1540, 1065, "IdGenerator Spring Boot Demo Architecture", "Spring controllers delegate to a configured generator service and expose typed REST responses.", body));
}

okioHierarchy();
workflowOverview();
lettuceContracts();
geoArchitecture();
coreClassStructure();
workflowSequenceSample();
jcacheNearCache();
cacheHibernateNearCacheStructure();
cacheHibernateLayerStructure();
dataCassandraApiStructure();
dataHibernateHierarchy();
dataHibernateConverters();
idGeneratorKtorLayered();
idGeneratorSpringLayered();
