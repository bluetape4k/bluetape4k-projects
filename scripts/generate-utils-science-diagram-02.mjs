#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/science/README.md",
  "utils/science/src/main/kotlin/io/bluetape4k/science/coords/GeoLocation.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/coords/UtmZone.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/coords/UtmZoneSupport.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/projection/CrsRegistry.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/projection/Projections.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Coordinate Transformation Flow[\s\S]*utils-science-diagram-02\.png/, "README coordinate flow slot");
assertContains(sources[3], /fun utmZoneOf\(latitude[\s\S]*fun utmZoneOf\(location[\s\S]*fun UtmZone\.boundingBox/, "UTM zone support");
assertContains(sources[4], /internal object CrsRegistry[\s\S]*ConcurrentHashMap[\s\S]*getCrs\(epsgCode[\s\S]*getCrsFromProj4/, "CRS cache registry");
assertContains(sources[5], /fun utmToWgs84[\s\S]*southernHemisphereSuffix[\s\S]*BasicCoordinateTransform[\s\S]*fun wgs84ToUtm[\s\S]*fun transform\(sourceCrs/, "projection flow functions");

const palette = {
  slate: ["#F8FAFC", "#64748B", "#475569"],
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  violet: ["#F5F3FF", "#7C3AED", "#6D28D9"],
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
  <marker id="arrow-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 3 3 L 19 11 L 3 19 Z" fill="${dark}"/></marker>`).join("\n");
}

function card({ id, x, y, w, h, color, kicker, title, lines = [], footer = "" }) {
  const [fill, stroke, dark] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="kicker" x="${x + 22}" y="${y + 30}">${esc(kicker)}</text>
  <text class="cardTitle" x="${x + 22}" y="${y + 64}">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 84}H${x + w}" stroke="${dark}"/>
  ${lines.map((line, index) => `<text class="body" x="${x + 22}" y="${y + 116 + index * 24}">${esc(line)}</text>`).join("\n")}
  ${footer ? `<path class="divider" d="M${x} ${y + h - 44}H${x + w}" stroke="${dark}"/><text class="foot" x="${x + 22}" y="${y + h - 16}">${esc(footer)}</text>` : ""}
</g>`;
}

function lane({ x, y, text }) {
  return `<text class="lane" x="${x}" y="${y}">${esc(text)}</text>`;
}

function edge({ from, to, points, color, dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>
  ${label ? `<text class="edgeLabel" x="${p[0]}" y="${p[1]}">${esc(label)}</text>` : ""}
</g>`;
}

function validateNoCardOverlap(cards, minGap = 18) {
  for (let i = 0; i < cards.length; i++) {
    for (let j = i + 1; j < cards.length; j++) {
      const a = cards[i];
      const b = cards[j];
      const separated =
        a.x + a.w + minGap <= b.x ||
        b.x + b.w + minGap <= a.x ||
        a.y + a.h + minGap <= b.y ||
        b.y + b.h + minGap <= a.y;
      if (!separated) {
        throw new Error(`Card overlap or insufficient gap: ${a.id} vs ${b.id}`);
      }
    }
  }
}

const width = 3000;
const height = 1500;

const edges = [
  edge({ from: "WgsInput", to: "ZoneDetect", points: [[560, 350], [700, 350]], color: "green", label: "utmZoneOf(location)", labelAt: [585, 326] }),
  edge({ from: "ZoneDetect", to: "UtmCrs", points: [[1100, 350], [1240, 350]], color: "green", label: "proj4 params", labelAt: [1125, 326] }),
  edge({ from: "UtmCrs", to: "TransformForward", points: [[1640, 350], [1780, 350]], color: "green", label: "cached CRS", labelAt: [1665, 326] }),
  edge({ from: "TransformForward", to: "UtmOutput", points: [[2180, 350], [2320, 350]], color: "green", label: "dst.x / dst.y", labelAt: [2205, 326] }),
  edge({ from: "UtmInput", to: "ReverseCrs", points: [[560, 760], [700, 760]], color: "blue", label: "zone + south", labelAt: [590, 736] }),
  edge({ from: "ReverseCrs", to: "TransformReverse", points: [[1100, 760], [1240, 760]], color: "blue", label: "UTM -> WGS84", labelAt: [1125, 736] }),
  edge({ from: "TransformReverse", to: "WgsOutput", points: [[1640, 760], [1780, 760]], color: "blue", label: "dst.y / dst.x", labelAt: [1665, 736] }),
  edge({ from: "ArbitraryInput", to: "RegistryPair", points: [[560, 1168], [700, 1168]], color: "violet", label: "source + target EPSG", labelAt: [575, 1144] }),
  edge({ from: "RegistryPair", to: "TransformAny", points: [[1100, 1168], [1240, 1168]], color: "violet", label: "two CRS objects", labelAt: [1125, 1144] }),
  edge({ from: "TransformAny", to: "PairOutput", points: [[1640, 1168], [1780, 1168]], color: "violet", label: "Pair(x, y)", labelAt: [1665, 1144] }),
];

const nodes = [
  lane({ x: 92, y: 210, text: "WGS84 to UTM" }),
  card({
    id: "WgsInput",
    x: 140,
    y: 265,
    w: 420,
    h: 170,
    color: "green",
    kicker: "input",
    title: "GeoLocation",
    lines: ["latitude: -90..90", "longitude: -180..180", "source CRS is EPSG:4326"],
  }),
  card({
    id: "ZoneDetect",
    x: 700,
    y: 265,
    w: 400,
    h: 170,
    color: "green",
    kicker: "coords",
    title: "UTM zone detection",
    lines: ["longitude zone 1..60", "latitude band C..X", "I and O are excluded"],
  }),
  card({
    id: "UtmCrs",
    x: 1240,
    y: 265,
    w: 400,
    h: 205,
    color: "green",
    kicker: "Projections.kt",
    title: "build UTM CRS",
    lines: ["+proj=utm +zone=n", "+datum=WGS84 +units=m", "+south when band < N"],
    footer: "CrsRegistry.getCrsFromProj4()",
  }),
  card({
    id: "TransformForward",
    x: 1780,
    y: 265,
    w: 400,
    h: 170,
    color: "teal",
    kicker: "Proj4J",
    title: "BasicCoordinateTransform",
    lines: ["src = (longitude, latitude)", "dst = UTM coordinate", "transform(src, dst)"],
  }),
  card({
    id: "UtmOutput",
    x: 2320,
    y: 265,
    w: 350,
    h: 170,
    color: "teal",
    kicker: "output",
    title: "Pair(easting, northing)",
    lines: ["meters in UTM zone", "returned by wgs84ToUtm()"],
  }),
  lane({ x: 92, y: 620, text: "UTM to WGS84" }),
  card({
    id: "UtmInput",
    x: 140,
    y: 675,
    w: 420,
    h: 170,
    color: "blue",
    kicker: "input",
    title: "UTM coordinate + zone",
    lines: ["easting, northing", "UtmZone(longitudeZone, band)", "validates zone and latitude band"],
  }),
  card({
    id: "ReverseCrs",
    x: 700,
    y: 675,
    w: 400,
    h: 205,
    color: "blue",
    kicker: "Projections.kt",
    title: "resolve source and target CRS",
    lines: ["UTM proj4 params", "WGS84 from EPSG:4326", "same +south rule"],
    footer: "CrsRegistry caches both",
  }),
  card({
    id: "TransformReverse",
    x: 1240,
    y: 675,
    w: 400,
    h: 170,
    color: "blue",
    kicker: "Proj4J",
    title: "BasicCoordinateTransform",
    lines: ["src = (easting, northing)", "dst = WGS84 coordinate", "transform(src, dst)"],
  }),
  card({
    id: "WgsOutput",
    x: 1780,
    y: 675,
    w: 400,
    h: 170,
    color: "teal",
    kicker: "output",
    title: "GeoLocation",
    lines: ["latitude = dst.y", "longitude = dst.x", "returned by utmToWgs84()"],
  }),
  lane({ x: 92, y: 1028, text: "Arbitrary EPSG transform" }),
  card({
    id: "ArbitraryInput",
    x: 140,
    y: 1083,
    w: 420,
    h: 170,
    color: "violet",
    kicker: "input",
    title: "EPSG source and target",
    lines: ["sourceCrs string", "targetCrs string", "lon and lat input values"],
  }),
  card({
    id: "RegistryPair",
    x: 700,
    y: 1083,
    w: 400,
    h: 170,
    color: "violet",
    kicker: "CrsRegistry",
    title: "getCrs(source) + getCrs(target)",
    lines: ["CRSFactory.createFromName()", "ConcurrentHashMap cache", "internal Proj4J CRS objects"],
  }),
  card({
    id: "TransformAny",
    x: 1240,
    y: 1083,
    w: 400,
    h: 170,
    color: "violet",
    kicker: "Projections.kt",
    title: "transform(source, target, lon, lat)",
    lines: ["src coordinate = (lon, lat)", "dst coordinate from Proj4J", "returns target CRS coordinate pair"],
  }),
  card({
    id: "PairOutput",
    x: 1780,
    y: 1083,
    w: 350,
    h: 170,
    color: "pink",
    kicker: "output",
    title: "Pair(x, y)",
    lines: ["target CRS coordinates", "no domain wrapper added"],
  }),
  card({
    id: "CrsRegistry",
    x: 2250,
    y: 1040,
    w: 520,
    h: 200,
    color: "slate",
    kicker: "shared implementation detail",
    title: "CrsRegistry cache",
    lines: ["CRSFactory creates CoordinateReferenceSystem", "keys are EPSG codes or proj4 params", "clearCache() is available for tests"],
    footer: "Proj4J types stay internal",
  }),
];

validateNoCardOverlap([
  { id: "WgsInput", x: 140, y: 265, w: 420, h: 170 },
  { id: "ZoneDetect", x: 700, y: 265, w: 400, h: 170 },
  { id: "UtmCrs", x: 1240, y: 265, w: 400, h: 205 },
  { id: "TransformForward", x: 1780, y: 265, w: 400, h: 170 },
  { id: "UtmOutput", x: 2320, y: 265, w: 350, h: 170 },
  { id: "UtmInput", x: 140, y: 675, w: 420, h: 170 },
  { id: "ReverseCrs", x: 700, y: 675, w: 400, h: 205 },
  { id: "TransformReverse", x: 1240, y: 675, w: 400, h: 170 },
  { id: "WgsOutput", x: 1780, y: 675, w: 400, h: 170 },
  { id: "ArbitraryInput", x: 140, y: 1083, w: 420, h: 170 },
  { id: "RegistryPair", x: 700, y: 1083, w: 400, h: 170 },
  { id: "TransformAny", x: 1240, y: 1083, w: 400, h: 170 },
  { id: "PairOutput", x: 1780, y: 1083, w: 350, h: 170 },
  { id: "CrsRegistry", x: 2250, y: 1040, w: 520, h: 200 },
]);

const svg = `<svg data-intent="Explain coordinate transformation flow from current README and projection source." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Coordinate Transformation Flow">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:48px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .lane{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}.card{stroke-width:1.8;filter:url(#shadow)}
    .kicker{font-family:"Comic Mono";font-size:13px;fill:#475569}.cardTitle{font-family:"Architects Daughter";font-size:23px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:13.5px;fill:#334155}.foot{font-family:"Comic Mono";font-size:12.5px;fill:#475569}.divider{stroke-width:1.1;opacity:.42}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#334155}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="88">Coordinate Transformation Flow</text>
<text class="subtitle" x="76" y="122">The projection package wraps Proj4J with typed WGS84/UTM helpers, UTM zone detection, southern-hemisphere handling, and a shared CRS cache.</text>
${edges.join("\n")}
${nodes.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-science-diagram-02.svg");
const pngPath = join(OUT, "utils-science-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated utils-science-diagram-02.svg/png");
