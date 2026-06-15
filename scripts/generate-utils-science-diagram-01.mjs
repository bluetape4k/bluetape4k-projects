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
  "utils/science/src/main/kotlin/io/bluetape4k/science/coords/BoundingBox.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/projection/Projections.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/shapefile/ShapefileReader.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/geometry/GeometryOperations.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/ShapefileImportService.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/exposed/schema/SpatialTables.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/exposed/schema/NetCdfTables.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Integrated Module Overview[\s\S]*utils-science-diagram-01\.png/, "README integrated overview slot");
assertContains(sources[0], /GIS Coordinate Conversion[\s\S]*Shapefile Processing[\s\S]*JTS Geometry Operations[\s\S]*PostGIS Data Pipeline[\s\S]*NetCDF Metadata Catalog/, "README five-domain overview");
assertContains(sources[3], /utmToWgs84[\s\S]*wgs84ToUtm[\s\S]*fun transform\(sourceCrs/, "projection transforms");
assertContains(sources[4], /fun loadShape[\s\S]*ShapeRecord[\s\S]*fun loadShapeAsync/, "shapefile reader surface");
assertContains(sources[6], /class ShapefileImportService[\s\S]*importShapefile[\s\S]*SpatialFeatureTable\.batchInsert[\s\S]*PGgeometry\(wkt\)/, "PostGIS shapefile import pipeline");
assertContains(sources[7], /class NetCdfCatalogService[\s\S]*registerFile[\s\S]*importGridValues[\s\S]*VariableAxisMap[\s\S]*CoordinateReprojector/, "NetCDF catalog import pipeline");
assertContains(sources[8], /object SpatialLayerTable[\s\S]*object SpatialFeatureTable/, "spatial schema");
assertContains(sources[9], /object NetCdfFileTable[\s\S]*object NetCdfGridValueTable[\s\S]*object NetCdfImportProgressTable/, "NetCDF schema");

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

function band({ x, y, w, h, title }) {
  return `<g>
  <rect class="band" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/>
  <text class="bandTitle" x="${x + 20}" y="${y + 34}">${esc(title)}</text>
</g>`;
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

function validateEndpointTouchesCard(point, card, edgeName, sideName, tolerance = 6) {
  const [x, y] = point;
  const onLeft = Math.abs(x - card.x) <= tolerance && y >= card.y - tolerance && y <= card.y + card.h + tolerance;
  const onRight = Math.abs(x - (card.x + card.w)) <= tolerance && y >= card.y - tolerance && y <= card.y + card.h + tolerance;
  const onTop = Math.abs(y - card.y) <= tolerance && x >= card.x - tolerance && x <= card.x + card.w + tolerance;
  const onBottom = Math.abs(y - (card.y + card.h)) <= tolerance && x >= card.x - tolerance && x <= card.x + card.w + tolerance;
  if (!(onLeft || onRight || onTop || onBottom)) {
    throw new Error(`${edgeName} ${sideName} endpoint does not touch ${card.id}: (${x}, ${y})`);
  }
}

function validateEdgeEndpoints(edgeSpecs, cards) {
  const byId = Object.fromEntries(cards.map((card) => [card.id, card]));
  for (const edgeSpec of edgeSpecs) {
    validateEndpointTouchesCard(edgeSpec.points[0], byId[edgeSpec.from], `${edgeSpec.from}->${edgeSpec.to}`, "source");
    validateEndpointTouchesCard(edgeSpec.points[edgeSpec.points.length - 1], byId[edgeSpec.to], `${edgeSpec.from}->${edgeSpec.to}`, "target");
  }
}

const width = 3000;
const height = 1880;

const bands = [
  band({ x: 90, y: 200, w: 2820, h: 300, title: "Coordinates and CRS transforms" }),
  band({ x: 90, y: 560, w: 2820, h: 360, title: "Shapefile to PostGIS pipeline" }),
  band({ x: 90, y: 980, w: 2820, h: 360, title: "NetCDF catalog and grid import pipeline" }),
  band({ x: 90, y: 1400, w: 2820, h: 270, title: "External libraries and storage boundaries" }),
];

const edges = [
  edge({ from: "Coords", to: "Projection", points: [[650, 350], [790, 350]], color: "green", label: "GeoLocation + UTM zone", labelAt: [660, 326] }),
  edge({ from: "Projection", to: "ProjectedOutput", points: [[1240, 350], [1380, 350]], color: "green", label: "Proj4J transform", labelAt: [1260, 326] }),
  edge({ from: "ShpFiles", to: "ShapeReader", points: [[520, 735], [660, 735]], color: "blue", label: ".shp + .dbf", labelAt: [540, 710] }),
  edge({ from: "ShapeReader", to: "ShapeModels", points: [[1070, 735], [1210, 735]], color: "blue", label: "domain models", labelAt: [1090, 710] }),
  edge({ from: "ShapeModels", to: "ImportService", points: [[1620, 735], [1760, 735]], color: "teal", label: "JTS geometry + attrs", labelAt: [1640, 710] }),
  edge({ from: "ImportService", to: "SpatialTables", points: [[2170, 735], [2310, 735]], color: "teal", label: "batch insert", labelAt: [2190, 710] }),
  edge({ from: "NetCdfFile", to: "NetCdfService", points: [[520, 1155], [660, 1155]], color: "amber", label: ".nc path", labelAt: [550, 1130] }),
  edge({ from: "NetCdfService", to: "AxisReproject", points: [[1070, 1155], [1210, 1155]], color: "amber", label: "rank 1-4 slices", labelAt: [1085, 1130] }),
  edge({ from: "AxisReproject", to: "NetCdfTables", points: [[1620, 1155], [1760, 1155]], color: "pink", label: "metadata + grid values", labelAt: [1635, 1130] }),
  edge({ from: "NetCdfTables", to: "Progress", points: [[2170, 1155], [2310, 1155]], color: "pink", dashed: true, label: "lease and resume", labelAt: [2190, 1130] }),
];

const nodes = [
  card({
    id: "Coords",
    x: 190,
    y: 270,
    w: 460,
    h: 175,
    color: "green",
    kicker: "coords package",
    title: "Coordinate primitives",
    lines: ["GeoLocation.distanceTo()", "BoundingBox contains/intersects", "DMS/DM, Vector, UtmZone"],
  }),
  card({
    id: "Projection",
    x: 790,
    y: 270,
    w: 450,
    h: 175,
    color: "green",
    kicker: "projection package",
    title: "CrsRegistry + Projections",
    lines: ["wgs84ToUtm()", "utmToWgs84()", "transform(source, target, lon, lat)"],
  }),
  card({
    id: "ProjectedOutput",
    x: 1380,
    y: 270,
    w: 430,
    h: 175,
    color: "teal",
    kicker: "projection output",
    title: "WGS84 / UTM / EPSG pairs",
    lines: ["GeoLocation result", "Pair(easting, northing)", "arbitrary EPSG coordinate pair"],
  }),
  card({
    id: "ShpFiles",
    x: 190,
    y: 655,
    w: 330,
    h: 160,
    color: "blue",
    kicker: "input",
    title: "Shapefile set",
    lines: [".shp geometry", ".dbf attributes"],
  }),
  card({
    id: "ShapeReader",
    x: 660,
    y: 635,
    w: 410,
    h: 200,
    color: "blue",
    kicker: "shapefile package",
    title: "loadShape / loadShapeAsync",
    lines: ["GeoTools ShapefileReader", "DbaseFileReader attributes", "Dispatchers.IO async wrapper"],
  }),
  card({
    id: "ShapeModels",
    x: 1210,
    y: 635,
    w: 410,
    h: 200,
    color: "teal",
    kicker: "shape + geometry",
    title: "Shape models and JTS",
    lines: ["ShapeHeader, ShapeRecord", "BoundingBox per record", "GeometryOperations helpers"],
  }),
  card({
    id: "ImportService",
    x: 1760,
    y: 635,
    w: 410,
    h: 200,
    color: "teal",
    kicker: "exposed service",
    title: "ShapefileImportService",
    lines: ["creates SpatialLayerRecord", "WKTWriter -> PGgeometry", "batchInsert per chunk"],
  }),
  card({
    id: "SpatialTables",
    x: 2310,
    y: 635,
    w: 430,
    h: 200,
    color: "pink",
    kicker: "PostGIS schema",
    title: "Spatial tables",
    lines: ["SpatialLayerTable metadata", "SpatialFeatureTable geom", "properties stored as JSONB"],
  }),
  card({
    id: "NetCdfFile",
    x: 190,
    y: 1075,
    w: 330,
    h: 160,
    color: "amber",
    kicker: "input",
    title: "NetCDF file",
    lines: ["variables", "dimensions"],
  }),
  card({
    id: "NetCdfService",
    x: 660,
    y: 1045,
    w: 410,
    h: 220,
    color: "amber",
    kicker: "exposed service",
    title: "NetCdfCatalogService",
    lines: ["registerFile(filePath)", "importGridValues(fileId, variable)", "rank 1 to 4 variable support"],
    footer: "latest source implements import flow",
  }),
  card({
    id: "AxisReproject",
    x: 1210,
    y: 1045,
    w: 410,
    h: 220,
    color: "amber",
    kicker: "internal helpers",
    title: "Axis map and reprojection",
    lines: ["VariableAxisMap finds time/level/lat/lon", "CoordinateReprojector normalizes CRS", "NaN and _FillValue are skipped"],
  }),
  card({
    id: "NetCdfTables",
    x: 1760,
    y: 1045,
    w: 410,
    h: 220,
    color: "pink",
    kicker: "NetCDF schema",
    title: "File and grid tables",
    lines: ["NetCdfFileTable metadata JSONB", "NetCdfGridValueTable value cells", "POINT location for spatial cells"],
  }),
  card({
    id: "Progress",
    x: 2310,
    y: 1045,
    w: 430,
    h: 220,
    color: "violet",
    kicker: "import progress",
    title: "Lease and resume state",
    lines: ["NetCdfImportProgressTable", "heartbeat lease TTL", "lastSliceIdx resume", "COMPLETED no-op guard"],
  }),
  card({
    id: "Proj4J",
    x: 790,
    y: 1440,
    w: 360,
    h: 145,
    color: "green",
    kicker: "external",
    title: "Proj4J / EPSG",
    lines: ["CRS transforms"],
  }),
  card({
    id: "GeoTools",
    x: 1240,
    y: 1440,
    w: 360,
    h: 145,
    color: "blue",
    kicker: "external",
    title: "GeoTools",
    lines: ["Shapefile and DBF I/O"],
  }),
  card({
    id: "JTS",
    x: 1690,
    y: 1440,
    w: 360,
    h: 145,
    color: "teal",
    kicker: "external",
    title: "JTS Core",
    lines: ["Geometry operations"],
  }),
  card({
    id: "PostGIS",
    x: 2310,
    y: 1440,
    w: 430,
    h: 145,
    color: "pink",
    kicker: "database",
    title: "Exposed + PostGIS",
    lines: ["geometry columns and JSONB attributes"],
  }),
  card({
    id: "NetcdfJava",
    x: 190,
    y: 1440,
    w: 430,
    h: 145,
    color: "amber",
    kicker: "external",
    title: "netCDF-Java",
    lines: ["NetcdfFiles and NetcdfDatasets"],
  }),
];

const validationCards = [
  { id: "Coords", x: 190, y: 270, w: 460, h: 175 },
  { id: "Projection", x: 790, y: 270, w: 450, h: 175 },
  { id: "ProjectedOutput", x: 1380, y: 270, w: 430, h: 175 },
  { id: "ShpFiles", x: 190, y: 655, w: 330, h: 160 },
  { id: "ShapeReader", x: 660, y: 635, w: 410, h: 200 },
  { id: "ShapeModels", x: 1210, y: 635, w: 410, h: 200 },
  { id: "ImportService", x: 1760, y: 635, w: 410, h: 200 },
  { id: "SpatialTables", x: 2310, y: 635, w: 430, h: 200 },
  { id: "NetCdfFile", x: 190, y: 1075, w: 330, h: 160 },
  { id: "NetCdfService", x: 660, y: 1045, w: 410, h: 220 },
  { id: "AxisReproject", x: 1210, y: 1045, w: 410, h: 220 },
  { id: "NetCdfTables", x: 1760, y: 1045, w: 410, h: 220 },
  { id: "Progress", x: 2310, y: 1045, w: 430, h: 220 },
  { id: "NetcdfJava", x: 190, y: 1440, w: 430, h: 145 },
  { id: "Proj4J", x: 790, y: 1440, w: 360, h: 145 },
  { id: "GeoTools", x: 1240, y: 1440, w: 360, h: 145 },
  { id: "JTS", x: 1690, y: 1440, w: 360, h: 145 },
  { id: "PostGIS", x: 2310, y: 1440, w: 430, h: 145 },
];

validateNoCardOverlap(validationCards);
validateEdgeEndpoints([
  { from: "Coords", to: "Projection", points: [[650, 350], [790, 350]] },
  { from: "Projection", to: "ProjectedOutput", points: [[1240, 350], [1380, 350]] },
  { from: "ShpFiles", to: "ShapeReader", points: [[520, 735], [660, 735]] },
  { from: "ShapeReader", to: "ShapeModels", points: [[1070, 735], [1210, 735]] },
  { from: "ShapeModels", to: "ImportService", points: [[1620, 735], [1760, 735]] },
  { from: "ImportService", to: "SpatialTables", points: [[2170, 735], [2310, 735]] },
  { from: "NetCdfFile", to: "NetCdfService", points: [[520, 1155], [660, 1155]] },
  { from: "NetCdfService", to: "AxisReproject", points: [[1070, 1155], [1210, 1155]] },
  { from: "AxisReproject", to: "NetCdfTables", points: [[1620, 1155], [1760, 1155]] },
  { from: "NetCdfTables", to: "Progress", points: [[2170, 1155], [2310, 1155]] },
], validationCards);

const svg = `<svg data-intent="Explain bluetape4k-science integrated module overview from current README and source." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Integrated Module Overview">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:48px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .band{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.2;opacity:.70}.bandTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}
    .card{stroke-width:1.8;filter:url(#shadow)}.kicker{font-family:"Comic Mono";font-size:13px;fill:#475569}.cardTitle{font-family:"Architects Daughter";font-size:23px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:13.5px;fill:#334155}.foot{font-family:"Comic Mono";font-size:12.5px;fill:#475569}.divider{stroke-width:1.1;opacity:.42}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#334155;paint-order:stroke;stroke:#FFFFFF;stroke-width:3px;stroke-linejoin:round}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="88">Integrated Module Overview</text>
<text class="subtitle" x="76" y="122">Five README domains are implemented as coordinate primitives, CRS transforms, Shapefile/JTS processing, PostGIS storage, and a current NetCDF registration plus grid import pipeline.</text>
${bands.join("\n")}
${edges.join("\n")}
${nodes.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-science-diagram-01.svg");
const pngPath = join(OUT, "utils-science-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated utils-science-diagram-01.svg/png");
