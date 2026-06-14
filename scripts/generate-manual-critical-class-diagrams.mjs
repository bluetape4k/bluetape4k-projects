#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const OUT = join(process.cwd(), "docs/images/readme-diagrams");
const rsvg = "/opt/homebrew/bin/rsvg-convert";

const palette = {
  blue: ["#E8F3FF", "#5B8DEF", "#4F83BF"],
  green: ["#EAF7EF", "#58A978", "#3E9868"],
  teal: ["#E9F7F6", "#45A7A1", "#2E8F89"],
  amber: ["#FFF3D9", "#D6A441", "#B9851B"],
  pink: ["#FDECEF", "#DC6B82", "#C94D68"],
  purple: ["#F1ECFF", "#8A72D6", "#755BC6"],
  olive: ["#EEF6D9", "#8BA84D", "#718A35"],
  gray: ["#F2F5F9", "#9AA8B8", "#758297"],
  indigo: ["#EEF1FF", "#6477D8", "#4F63C7"],
  coral: ["#FFF0EA", "#E27D62", "#C8644B"],
  navy: ["#EAF0F8", "#617C9F", "#496581"],
};

mkdirSync(OUT, { recursive: true });

function okioAsyncHierarchy() {
  const nodes = [
    klass("sink", "interface", "SuspendedSink", ["write(Buffer, Long)", "flush()", "close()"], "green", 150, 185),
    klass("bufferedSink", "interface", "BufferedSuspendedSink", ["buffer: Buffer", "write(ByteString)", "writeUtf8(String)"], "amber", 520, 185),
    klass("source", "interface", "SuspendedSource", ["read(Buffer, Long)", "close()"], "teal", 890, 185),
    klass("bufferedSource", "interface", "BufferedSuspendedSource", ["buffer: Buffer", "exhausted()", "require(Long)"], "pink", 1260, 185),

    klass("fileSink", "class", "SuspendedFileChannelSink", ["AsynchronousFileChannel", "position: Long", "writeBuffer"], "purple", 220, 455),
    klass("socketSink", "class", "SuspendedSocketChannelSink", ["AsynchronousSocketChannel", "network write adapter"], "gray", 590, 455),
    klass("realSink", "class", "RealBufferedSuspendedSink", ["sink: SuspendedSink", "closed guard"], "green", 960, 455),
    klass("pipe", "class", "SuspendedPipe", ["sink: BufferedSuspendedSink", "source: BufferedSuspendedSource"], "blue", 1330, 455),

    klass("fileSource", "class", "SuspendedFileChannelSource", ["AsynchronousFileChannel", "position: Long", "readBuffer"], "olive", 220, 735),
    klass("socketSource", "class", "SuspendedSocketChannelSource", ["AsynchronousSocketChannel", "network read adapter"], "blue", 590, 735),
    klass("realSource", "class", "RealBufferedSuspendedSource", ["source: SuspendedSource", "closed guard"], "teal", 960, 735),
  ];
  const routes = [
    inherit("fileSink", "sink", "purple", [{ x: 390, y: 455 }, { x: 390, y: 365 }, { x: 320, y: 365 }, { x: 320, y: 317 }]),
    inherit("socketSink", "sink", "gray", [{ x: 760, y: 455 }, { x: 760, y: 345 }, { x: 320, y: 345 }, { x: 320, y: 317 }]),
    inherit("realSink", "bufferedSink", "green", [{ x: 1130, y: 455 }, { x: 1130, y: 365 }, { x: 690, y: 365 }, { x: 690, y: 317 }]),
    inherit("fileSource", "source", "olive", [{ x: 390, y: 735 }, { x: 390, y: 655 }, { x: 80, y: 655 }, { x: 80, y: 145 }, { x: 1060, y: 145 }, { x: 1060, y: 185 }]),
    inherit("socketSource", "source", "blue", [{ x: 760, y: 735 }, { x: 760, y: 635 }, { x: 120, y: 635 }, { x: 120, y: 160 }, { x: 1060, y: 160 }, { x: 1060, y: 185 }]),
    inherit("realSource", "bufferedSource", "teal", [{ x: 1130, y: 735 }, { x: 1130, y: 655 }, { x: 1740, y: 655 }, { x: 1740, y: 145 }, { x: 1430, y: 145 }, { x: 1430, y: 185 }]),
    dep("bufferedSink", "sink", "amber", [{ x: 520, y: 251 }, { x: 490, y: 251 }]),
    dep("bufferedSource", "source", "pink", [{ x: 1260, y: 251 }, { x: 1230, y: 251 }]),
    dep("pipe", "bufferedSink", "blue", [{ x: 1500, y: 455 }, { x: 1500, y: 340 }, { x: 690, y: 340 }, { x: 690, y: 317 }]),
    dep("pipe", "bufferedSource", "teal", [{ x: 1500, y: 455 }, { x: 1500, y: 317 }]),
  ];
  write("io-okio-diagram-03", 1880, 1060, "Coroutines Async I/O Hierarchy", "Sink and Source contracts stay at the top; concrete file, socket, buffering, and pipe adapters attach through short colored routes.", nodes, routes);
}

function testcontainersHierarchy() {
  const nodes = [
    klass("aws", "abstract", "AwsEmulatorServer", ["awsEndpoint", "access/secret keys", "service emulator base"], "blue", 130, 185),
    klass("generic", "abstract", "GenericServer", ["useDefaultPort", "start()", "stop()"], "green", 780, 185),
    klass("postgres", "class", "PostgreSQLServer", ["PostgreSQL container", "withExtensions(...)"], "purple", 1430, 185),

    klass("mini", "class", "MiniStackServer", ["S3/SQS focused emulator", "withServices() no-op"], "amber", 70, 455),
    klass("floci", "class", "FlociServer", ["AWS-compatible endpoint", "lightweight emulator"], "pink", 70, 665),
    klass("localstack", "class", "LocalStackServer", ["legacy localstack wrapper", "deprecated path"], "olive", 70, 875),

    klass("mysql", "class", "MySQL8Server", ["MySQL 8 container", "getDataSource()"], "blue", 610, 455),
    klass("kafka", "class", "KafkaServer", ["bootstrapServers", "Kafka broker container"], "green", 990, 455),
    klass("redis", "class", "RedisServer", ["host", "port"], "pink", 610, 675),
    klass("mailpit", "class", "MailpitServer", ["smtpPort", "uiUrl"], "amber", 990, 675),
    klass("http", "class", "BluetapeHttpServer", ["url", "httpbin/jsonplaceholder"], "gray", 610, 895),
    klass("webflux", "class", "BluetapeWebfluxServer", ["url", "reactive HTTP test server"], "teal", 990, 895),

    klass("pgvector", "class", "PgvectorServer", ["pgvector extension", "vector test database"], "indigo", 1430, 455),
    klass("postgis", "class", "PostgisServer", ["PostGIS extension", "spatial test database"], "olive", 1430, 675),
  ];
  const routes = [
    inherit("mini", "aws", "amber", [{ x: 240, y: 455 }, { x: 240, y: 317 }]),
    inherit("floci", "aws", "pink", [{ x: 70, y: 731 }, { x: 50, y: 731 }, { x: 50, y: 335 }, { x: 240, y: 335 }, { x: 240, y: 317 }]),
    inherit("localstack", "aws", "olive", [{ x: 240, y: 875 }, { x: 240, y: 820 }, { x: 430, y: 820 }, { x: 430, y: 335 }, { x: 240, y: 335 }, { x: 240, y: 317 }]),

    inherit("mysql", "generic", "blue", [{ x: 780, y: 455 }, { x: 780, y: 370 }, { x: 950, y: 370 }, { x: 950, y: 317 }]),
    inherit("kafka", "generic", "green", [{ x: 1160, y: 455 }, { x: 1160, y: 350 }, { x: 950, y: 350 }, { x: 950, y: 317 }]),
    inherit("redis", "generic", "pink", [{ x: 610, y: 741 }, { x: 540, y: 741 }, { x: 540, y: 337 }, { x: 950, y: 337 }, { x: 950, y: 317 }]),
    inherit("mailpit", "generic", "amber", [{ x: 1330, y: 741 }, { x: 1370, y: 741 }, { x: 1370, y: 337 }, { x: 950, y: 337 }, { x: 950, y: 317 }]),
    inherit("http", "generic", "gray", [{ x: 610, y: 961 }, { x: 500, y: 961 }, { x: 500, y: 325 }, { x: 950, y: 325 }, { x: 950, y: 317 }]),
    inherit("webflux", "generic", "teal", [{ x: 1330, y: 961 }, { x: 1415, y: 961 }, { x: 1415, y: 325 }, { x: 950, y: 325 }, { x: 950, y: 317 }]),

    inherit("postgres", "generic", "purple", [{ x: 1430, y: 251 }, { x: 1120, y: 251 }]),
    inherit("pgvector", "postgres", "indigo", [{ x: 1600, y: 455 }, { x: 1600, y: 317 }]),
    inherit("postgis", "postgres", "olive", [{ x: 1770, y: 741 }, { x: 1820, y: 741 }, { x: 1820, y: 335 }, { x: 1600, y: 335 }, { x: 1600, y: 317 }]),
  ];
  write("testing-testcontainers-diagram-01", 1900, 1220, "Supported Container Class Diagram", "Supported test containers are grouped by inheritance family so AWS emulators, generic services, and PostgreSQL extensions stay readable.", nodes, routes);
}

function klass(id, stereotype, title, members, color, x, y, w = 340, h = 132) {
  return { id, stereotype, title, members, color, x, y, w, h };
}

function inherit(from, to, color, points) {
  return { from, to, color, points, kind: "inherit" };
}

function dep(from, to, color, points) {
  return { from, to, color, points, kind: "dependency" };
}

function write(file, width, height, title, subtitle, nodes, routes) {
  const svgPath = join(OUT, `${file}.svg`);
  const pngPath = join(OUT, `${file}.png`);
  const svg = base(width, height, title, subtitle, `${routes.map(renderRoute).join("\n")}\n${nodes.map(renderCard).join("\n")}\n${legend(width - 405, height - 110)}\n${footer(180, height - 92, width - 640, "bluetape4k-projects class diagrams - github.com/bluetape4k/bluetape4k-projects")}`);
  writeFileSync(svgPath, `${svg.trimEnd()}\n`);
  execFileSync(rsvg, ["--format=png", "--output", pngPath, svgPath], { stdio: "inherit" });
  console.log(`${file}.png`);
}

function renderRoute(route) {
  const color = palette[route.color]?.[2] || palette.gray[2];
  const d = route.points.map((point, index) => `${index === 0 ? "M" : "L"}${point.x} ${point.y}`).join(" ");
  const dash = route.kind === "dependency" ? ` stroke-dasharray="8 7"` : "";
  return `<path class="${route.kind}" data-from="${route.from}" data-to="${route.to}" d="${d}" stroke="${color}"${dash}/>`;
}

function renderCard(node) {
  const [fill, stroke] = palette[node.color] || palette.gray;
  const titleLines = wrapIdentifier(node.title, 24).slice(0, 2);
  const memberLines = node.members.flatMap((line) => wrap(line, 34)).slice(0, 3);
  const titleStart = titleLines.length > 1 ? node.y + 38 : node.y + 47;
  const headerBottom = titleLines.length > 1 ? node.y + 64 : node.y + 56;
  const memberStart = lowerCompartmentBaselineStart(headerBottom, node.y + node.h, memberLines.length, 17);
  return `<g id="${node.id}">
  <rect class="classCard" x="${node.x}" y="${node.y}" width="${node.w}" height="${node.h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="stereo" x="${node.x + node.w / 2}" y="${node.y + 19}" text-anchor="middle">${esc(node.stereotype)}</text>
  ${titleLines.map((line, index) => `<text class="classTitle" x="${node.x + node.w / 2}" y="${titleStart + index * 22}" text-anchor="middle">${esc(line)}</text>`).join("\n")}
  <line x1="${node.x}" y1="${headerBottom}" x2="${node.x + node.w}" y2="${headerBottom}" stroke="${stroke}" stroke-width="1.4"/>
  ${memberLines.map((line, index) => `<text class="member" x="${node.x + 18}" y="${memberStart + index * 17}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function lowerCompartmentBaselineStart(headerBottom, cardBottom, lineCount, lineH) {
  if (lineCount <= 0) return Math.round((headerBottom + cardBottom) / 2);
  return Math.round((headerBottom + cardBottom) / 2 + 4 - ((lineCount - 1) * lineH) / 2);
}

function legend(x, y) {
  return `<g>
  <rect x="${x}" y="${y}" width="250" height="54" rx="12" fill="#FFFFFF" stroke="#D6E3EF" stroke-width="1.6"/>
  <path class="inherit" d="M${x + 18} ${y + 18} L${x + 76} ${y + 18}" stroke="#3E9868"/>
  <text class="detail" x="${x + 90}" y="${y + 23}">inheritance / implementation</text>
  <path class="dependency" d="M${x + 18} ${y + 39} L${x + 76} ${y + 39}" stroke="#758297" stroke-dasharray="8 7"/>
  <text class="detail" x="${x + 90}" y="${y + 44}">dependency / usage</text>
</g>`;
}

function footer(x, y, w, value) {
  return `<g><rect x="${x}" y="${y}" width="${w}" height="42" rx="12" fill="#FFFFFF" stroke="#D6E3EF" stroke-width="1.6"/><text class="detail" x="${x + w / 2}" y="${y + 26}" text-anchor="middle">${esc(value)}</text></g>`;
}

function base(width, height, title, subtitle, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title)}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/></filter>
  <marker id="inheritArrow" markerWidth="8.5" markerHeight="8" refX="8" refY="4" orient="auto" markerUnits="strokeWidth"><path d="M 1 1 L 8 4 L 1 7 Z" fill="#fff" stroke="context-stroke" stroke-width="1.25"/></marker>
  <marker id="depArrow" markerWidth="8" markerHeight="8" refX="7.4" refY="4" orient="auto" markerUnits="strokeWidth"><path d="M 1 1 L 7.4 4 L 1 7 Z" fill="context-stroke"/></marker>
  <style>
    .canvas{fill:#F6F9FC}.frame{fill:#fff;stroke:#C7D7E7;stroke-width:3;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:#22344A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#536476}
    .classCard{filter:url(#shadow);stroke-width:2}.classTitle{font-family:"Architects Daughter";font-size:23px;fill:#22344A}.stereo{font-family:"Comic Mono";font-size:10px;fill:#627184}.member{font-family:"Comic Mono";font-size:12px;fill:#102033}
    .inherit{fill:none;stroke-width:2.55;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#inheritArrow)}
    .dependency{fill:none;stroke-width:2.35;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#depArrow)}
    .detail{font-family:"Comic Mono";font-size:13px;fill:#42556B}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="32" y="28" width="${width - 64}" height="${height - 56}" rx="30"/>
<text class="title" x="68" y="84">${esc(title)}</text>
<text class="subtitle" x="72" y="116">${esc(subtitle)}</text>
${body}
</svg>`;
}

function wrap(value, max) {
  const words = String(value).split(/\s+/);
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

function wrapIdentifier(value, max) {
  const text = String(value);
  if (text.includes(" ")) return wrap(text, max);
  const parts = text.match(/[A-Z]?[a-z0-9]+|[A-Z]+(?=[A-Z]|$)/g) || [text];
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

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

okioAsyncHierarchy();
testcontainersHierarchy();
