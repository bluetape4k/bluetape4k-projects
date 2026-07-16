#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/science/README.md",
  "utils/science/src/main/kotlin/io/bluetape4k/science/exposed/schema/SpatialTables.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/exposed/schema/PoiTable.kt",
  "utils/science/src/main/kotlin/io/bluetape4k/science/exposed/schema/NetCdfTables.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /PostGIS \+ NetCDF Database Schema[\s\S]*utils-science-diagram-03\.png/, "README database schema slot");
assertContains(sources[1], /object SpatialLayerTable[\s\S]*name = varchar[\s\S]*object SpatialFeatureTable[\s\S]*layerId = reference[\s\S]*geoGeometry/, "spatial tables");
assertContains(sources[2], /object PoiTable[\s\S]*location = geoPoint[\s\S]*properties = jsonb/, "POI table");
assertContains(sources[3], /object NetCdfFileTable[\s\S]*object NetCdfGridValueTable[\s\S]*fileId = reference[\s\S]*object NetCdfGridValueIndexes[\s\S]*object NetCdfImportProgressTable[\s\S]*uniqueIndex/, "NetCDF tables and indexes");

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

function table({ id, x, y, w, color, title, subtitle, rows }) {
  const [fill, stroke, dark] = palette[color];
  const rowH = 30;
  const h = 96 + rows.length * rowH;
  return `<g id="${esc(id)}">
  <rect class="table" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="kicker" x="${x + 22}" y="${y + 30}">${esc(subtitle)}</text>
  <text class="tableTitle" x="${x + 22}" y="${y + 66}">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 86}H${x + w}" stroke="${dark}"/>
  ${rows.map((row, index) => `<text class="col" x="${x + 24}" y="${y + 116 + index * rowH}">${esc(row)}</text>`).join("\n")}
</g>`;
}

function note({ id, x, y, w, h, color, title, lines }) {
  const [fill, stroke] = palette[color];
  return `<g id="${esc(id)}">
  <rect class="note" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="noteTitle" x="${x + 22}" y="${y + 38}">${esc(title)}</text>
  ${lines.map((line, index) => `<text class="noteLine" x="${x + 22}" y="${y + 72 + index * 24}">${esc(line)}</text>`).join("\n")}
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

function validateEdgeEndpoints(edges, cards) {
  const byId = Object.fromEntries(cards.map((card) => [card.id, card]));
  for (const edgeSpec of edges) {
    validateEndpointTouchesCard(edgeSpec.points[0], byId[edgeSpec.from], `${edgeSpec.from}->${edgeSpec.to}`, "source");
    validateEndpointTouchesCard(edgeSpec.points[edgeSpec.points.length - 1], byId[edgeSpec.to], `${edgeSpec.from}->${edgeSpec.to}`, "target");
  }
}

const width = 3100;
const height = 1740;

const validationCards = [
  { id: "SpatialLayer", x: 160, y: 280, w: 900, h: 96 + 6 * 30 },
  { id: "SpatialFeature", x: 160, y: 660, w: 900, h: 96 + 6 * 30 },
  { id: "Poi", x: 160, y: 1040, w: 900, h: 96 + 5 * 30 },
  { id: "NetCdfFile", x: 1430, y: 280, w: 1150, h: 96 + 6 * 30 },
  { id: "NetCdfGrid", x: 1380, y: 700, w: 740, h: 96 + 6 * 30 },
  { id: "NetCdfProgress", x: 2240, y: 700, w: 640, h: 96 + 8 * 30 },
  { id: "IndexNote", x: 1400, y: 1190, w: 700, h: 150 },
  { id: "LeaseNote", x: 2240, y: 1270, w: 640, h: 110 },
];

const bands = [
  band({ x: 90, y: 180, w: 1120, h: 1340, title: "Spatial / PostGIS schema" }),
  band({ x: 1320, y: 180, w: 1690, h: 1340, title: "NetCDF catalog and import schema" }),
];

const edges = [
  edge({ from: "SpatialLayer", to: "SpatialFeature", points: [[650, 560], [650, 660]], color: "teal", label: "1 layer -> many features", labelAt: [675, 615] }),
  edge({ from: "NetCdfFile", to: "NetCdfGrid", points: [[2005, 560], [2005, 625], [1750, 625], [1750, 700]], color: "pink", label: "grid values", labelAt: [1810, 604] }),
  edge({ from: "NetCdfFile", to: "NetCdfProgress", points: [[2005, 560], [2005, 625], [2560, 625], [2560, 700]], color: "violet", label: "import lease", labelAt: [2470, 604] }),
  edge({ from: "NetCdfGrid", to: "IndexNote", points: [[1750, 976], [1750, 1190]], color: "pink", dashed: true, label: "partial unique indexes", labelAt: [1774, 1084] }),
  edge({ from: "NetCdfProgress", to: "LeaseNote", points: [[2560, 1036], [2560, 1270]], color: "violet", dashed: true, label: "status lifecycle", labelAt: [2584, 1160] }),
];

const nodes = [
  table({
    id: "SpatialLayer",
    x: 160,
    y: 280,
    w: 900,
    color: "teal",
    subtitle: "LongIdTable spatial_layers",
    title: "SpatialLayerTable",
    rows: [
      "PK id",
      "name varchar(255) unique",
      "description text nullable, source_file varchar(1024) nullable",
      "srid int default 4326, geometry_type varchar(50) nullable",
      "bbox_min_x/y, bbox_max_x/y double nullable",
      "record_count int default 0, created_at, updated_at",
    ],
  }),
  table({
    id: "SpatialFeature",
    x: 160,
    y: 660,
    w: 900,
    color: "teal",
    subtitle: "LongIdTable spatial_features",
    title: "SpatialFeatureTable",
    rows: [
      "PK id",
      "FK layer_id -> spatial_layers.id",
      "feature_type varchar(50)",
      "geom PostGIS GEOMETRY",
      "properties JSONB Map<String, Any?>",
      "name varchar(255) nullable, created_at, updated_at",
    ],
  }),
  table({
    id: "Poi",
    x: 160,
    y: 1040,
    w: 900,
    color: "blue",
    subtitle: "LongIdTable poi",
    title: "PoiTable",
    rows: [
      "PK id",
      "name varchar(255), category varchar(100) nullable",
      "location PostGIS POINT",
      "properties JSONB Map<String, Any?>",
      "created_at, updated_at",
    ],
  }),
  table({
    id: "NetCdfFile",
    x: 1430,
    y: 280,
    w: 1150,
    color: "pink",
    subtitle: "LongIdTable netcdf_files",
    title: "NetCdfFileTable",
    rows: [
      "PK id",
      "filename varchar(255), file_path varchar(1024), file_size long",
      "variables JSONB List<NetCdfVariableInfo>",
      "dimensions JSONB Map<String, Int>, global_attrs JSONB",
      "bbox PostGIS POLYGON nullable",
      "time_start, time_end timestamp nullable, created_at, updated_at",
    ],
  }),
  table({
    id: "NetCdfGrid",
    x: 1380,
    y: 700,
    w: 740,
    color: "pink",
    subtitle: "LongIdTable netcdf_grid_values",
    title: "NetCdfGridValueTable",
    rows: [
      "PK id",
      "FK file_id -> netcdf_files.id",
      "variable_name varchar(255)",
      "location PostGIS POINT nullable",
      "time_idx int default 0, level_idx int default 0",
      "value double, attrs JSONB nullable",
    ],
  }),
  table({
    id: "NetCdfProgress",
    x: 2240,
    y: 700,
    w: 640,
    color: "violet",
    subtitle: "LongIdTable netcdf_import_progress",
    title: "NetCdfImportProgressTable",
    rows: [
      "PK id",
      "FK file_id -> netcdf_files.id",
      "variable_name varchar(255)",
      "status enum PENDING/IN_PROGRESS/COMPLETED/FAILED",
      "last_slice_idx long nullable",
      "lease_expires_at timestamp nullable",
      "error_message text nullable",
      "unique(file_id, variable_name)",
    ],
  }),
  note({
    id: "IndexNote",
    x: 1400,
    y: 1190,
    w: 700,
    h: 150,
    color: "slate",
    title: "Grid uniqueness",
    lines: [
      "location present: file + var + time + level",
      "+ MD5(ST_AsBinary(location))",
      "location null: file + var + time + level",
      "both indexes are partial unique indexes",
    ],
  }),
  note({
    id: "LeaseNote",
    x: 2240,
    y: 1270,
    w: 640,
    h: 110,
    color: "slate",
    title: "Import control",
    lines: [
      "heartbeat lease prevents duplicate active import",
      "lastSliceIdx resumes rank 3/4 imports",
    ],
  }),
];

validateNoCardOverlap(validationCards);
validateEdgeEndpoints([
  { from: "SpatialLayer", to: "SpatialFeature", points: [[650, 560], [650, 660]] },
  { from: "NetCdfFile", to: "NetCdfGrid", points: [[2005, 560], [2005, 625], [1750, 625], [1750, 700]] },
  { from: "NetCdfFile", to: "NetCdfProgress", points: [[2005, 560], [2005, 625], [2560, 625], [2560, 700]] },
  { from: "NetCdfGrid", to: "IndexNote", points: [[1750, 976], [1750, 1190]] },
  { from: "NetCdfProgress", to: "LeaseNote", points: [[2560, 1036], [2560, 1270]] },
], validationCards);

const svg = `<svg data-intent="Explain PostGIS and NetCDF database schema from current source tables." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="PostGIS + NetCDF Database Schema">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:48px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .band{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.2;opacity:.72}.bandTitle{font-family:"Architects Daughter";font-size:27px;fill:#0F172A}
    .table,.note{stroke-width:1.8;filter:url(#shadow)}.kicker{font-family:"Comic Mono";font-size:13px;fill:#475569}.tableTitle,.noteTitle{font-family:"Architects Daughter";font-size:24px;fill:#0F172A}
    .col,.noteLine{font-family:"Comic Mono";font-size:13.5px;fill:#334155}.divider{stroke-width:1.1;opacity:.42}
    .edge{fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:9 7}.edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#334155}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="88">PostGIS + NetCDF Database Schema</text>
<text class="subtitle" x="76" y="122">Spatial tables store layers/features/POIs in PostGIS geometry columns; NetCDF tables store file metadata, grid values, and resumable import lease state.</text>
${bands.join("\n")}
${edges.join("\n")}
${nodes.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-science-diagram-03.svg");
const pngPath = join(OUT, "utils-science-diagram-03.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
console.log("Generated utils-science-diagram-03.svg/png");
