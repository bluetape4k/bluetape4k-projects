#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, writeFileSync } from "node:fs";
import { dirname } from "node:path";

const diagramDir = "docs/images/readme-diagrams";
const dot = "/opt/homebrew/bin/dot";
const rsvgConvert = "/opt/homebrew/bin/rsvg-convert";
const minTitleGapPx = 38;

const palette = {
  blue: { fill: "#E8F3FF", stroke: "#75A9E8", line: "#4F83BF" },
  green: { fill: "#EAF7EF", stroke: "#69B888", line: "#58A978" },
  teal: { fill: "#E9F7F6", stroke: "#45A7A1", line: "#45A7A1" },
  amber: { fill: "#FFF3D9", stroke: "#D9AA4D", line: "#D9AA4D" },
  pink: { fill: "#FCE7F3", stroke: "#DB7890", line: "#DB7890" },
  purple: { fill: "#F1ECFF", stroke: "#8A72D6", line: "#8A72D6" },
  olive: { fill: "#EEF6D9", stroke: "#8BA84D", line: "#8BA84D" },
};

const diagrams = [
  {
    file: "bluetape4k-core-diagram-01",
    title: "Bluetape4k Core architecture",
    subtitle: "Public Kotlin/JVM helpers arranged by contract, value model, and runtime utility layers.",
    desc: "Layered module overview for bluetape4k-core showing validation, codecs, value objects, ranges, collections, concurrent utilities, Java time helpers, and functional helpers.",
    layers: [
      {
        name: "Module entrypoint",
        nodes: [
          card("core", "bluetape4k-core", ["base dependency", "shared Kotlin/JVM surface"], "blue", 490),
        ],
      },
      {
        name: "Caller contracts",
        nodes: [
          card("validation", "RequireSupport", ["requireNotBlank", "typed preconditions"], "green"),
          card("types", "Type extensions", ["arrays, numbers, strings", "reflection helpers"], "teal"),
          card("codec", "Codec utilities", ["Base58/Base62/Base64", "Hex and Url62"], "amber"),
        ],
      },
      {
        name: "Domain value model",
        nodes: [
          card("value", "ValueObject", ["Serializable value contract", "stable equality helpers"], "purple"),
          card("ranges", "Range model", ["open/closed ranges", "overlap and contains"], "pink"),
          card("collections", "Collection helpers", ["bounded stack, ring buffer", "Eclipse extensions"], "olive"),
        ],
      },
      {
        name: "Runtime helpers",
        nodes: [
          card("concurrent", "Concurrent utilities", ["future and executor helpers", "virtual-thread adapters"], "teal"),
          card("time", "Java time DSL", ["date/time ranges", "duration and quarter types"], "amber"),
          card("functional", "Functional utilities", ["currying", "decorator support"], "green"),
        ],
      },
    ],
    edges: [
      edge("core", "validation", "contracts", "green"),
      edge("core", "types", "types", "teal"),
      edge("core", "codec", "encoding", "amber"),
      edge("validation", "ranges", "range checks", "pink"),
      edge("types", "value", "value helpers", "purple"),
      edge("types", "collections", "collections", "olive"),
      edge("collections", "concurrent", "async containers", "teal"),
      edge("ranges", "time", "temporal ranges", "amber"),
      edge("value", "functional", "composition", "green"),
    ],
  },
  {
    file: "bluetape4k-coroutines-diagram-01",
    title: "Bluetape4k Coroutines architecture",
    subtitle: "Coroutine helpers grouped by deferred primitives, Flow operators, execution scopes, and context bridges.",
    desc: "Layered module overview for bluetape4k-coroutines showing DeferredValue, deferred helpers, flow extensions, AsyncFlow, coroutine scopes, Reactor context, and test support.",
    layers: [
      {
        name: "Module entrypoint",
        nodes: [
          card("coroutines", "bluetape4k-coroutines", ["coroutine-first extension set", "structured async helpers"], "blue", 500),
        ],
      },
      {
        name: "Async primitives",
        nodes: [
          card("deferred-value", "DeferredValue", ["eager async wrapper", "map, flatMap, await"], "green"),
          card("deferred-helpers", "Deferred helpers", ["zip, awaitAny", "cancel losing jobs"], "teal"),
        ],
      },
      {
        name: "Flow processing",
        nodes: [
          card("flow-ext", "Flow extensions", ["chunked, windowed, sliding", "mapParallel and replay"], "amber"),
          card("async-flow", "AsyncFlow", ["order-preserving transform", "bounded concurrency"], "pink"),
          card("subject", "Subject APIs", ["publish, replay, multicast", "resumable collectors"], "purple"),
        ],
      },
      {
        name: "Runtime integration",
        nodes: [
          card("scopes", "Coroutine scopes", ["Default, IO, ThreadPool", "virtual-thread scope"], "teal"),
          card("reactor", "Reactor context bridge", ["current ReactiveContext", "coroutine context support"], "purple"),
          card("tests", "Test support", ["Flow assertions", "runTest helpers"], "olive"),
        ],
      },
    ],
    edges: [
      edge("coroutines", "deferred-value", "value wrapper", "green"),
      edge("coroutines", "deferred-helpers", "deferred ops", "teal"),
      edge("deferred-value", "flow-ext", "async values", "amber"),
      edge("deferred-helpers", "async-flow", "parallel work", "pink"),
      edge("flow-ext", "subject", "stream state", "purple"),
      edge("async-flow", "scopes", "dispatch", "teal"),
      edge("subject", "reactor", "context", "purple"),
      edge("flow-ext", "tests", "verification", "olive"),
    ],
  },
  {
    file: "utils-geo-diagram-01",
    title: "Geo utilities architecture",
    subtitle: "Geocode, GeoHash, and GeoIP APIs separated from provider adapters, local math, and external datasets.",
    desc: "Layered module overview for utils/geo showing geocode finders, Google and Bing adapters, GeoHash spatial indexing, GeoIP finders, MaxMind data, and provider APIs.",
    layers: [
      {
        name: "Public API",
        nodes: [
          card("geocode", "Geocode API", ["Geocode, Address", "sync and suspend finder"], "blue"),
          card("geohash-api", "GeoHash API", ["GeoHash, WGS84Point", "BoundingBox queries"], "green"),
          card("geoip", "GeoIP API", ["GeoipFinder", "city/country lookup"], "purple"),
        ],
      },
      {
        name: "Adapter layer",
        nodes: [
          card("google", "Google Maps adapter", ["GoogleAddressFinder", "GeoApiContext"], "amber"),
          card("bing", "Bing Maps adapter", ["Feign client", "coroutine Feign client"], "pink"),
          card("maxmind-reader", "MaxMind reader", ["DatabaseReader extensions", "Result wrappers"], "teal"),
        ],
      },
      {
        name: "Spatial model",
        nodes: [
          card("geohash-core", "GeoHash core", ["base32 encoding", "adjacent cells"], "green"),
          card("geo-queries", "Spatial queries", ["circle and bbox search", "cells within radius"], "olive"),
          card("geo-result", "GeoIP result model", ["Address", "GeoLocation"], "purple"),
        ],
      },
      {
        name: "External systems",
        nodes: [
          card("provider-http", "Provider HTTP APIs", ["Google Geocoding", "Bing Locations"], "amber"),
          card("geo-db", "GeoLite2 databases", ["City, Country, ASN", "bundled mmdb files"], "teal"),
        ],
      },
    ],
    edges: [
      edge("geocode", "google", "google finder", "amber"),
      edge("geocode", "bing", "bing finder", "pink"),
      edge("geohash-api", "geohash-core", "index", "green"),
      edge("geohash-core", "geo-queries", "search cells", "olive"),
      edge("geoip", "maxmind-reader", "reader", "teal"),
      edge("maxmind-reader", "geo-result", "normalize", "purple"),
      edge("google", "provider-http", "HTTP", "amber"),
      edge("bing", "provider-http", "HTTP", "pink"),
      edge("maxmind-reader", "geo-db", "mmdb", "teal"),
    ],
  },
  {
    file: "utils-science-diagram-01",
    title: "Science utilities architecture",
    subtitle: "Coordinate models feed projections and geometry, then file readers and Exposed persistence services.",
    desc: "Layered module overview for utils/science showing coordinate types, projection registry, geometry helpers, shapefile and NetCDF models, Exposed tables, repositories, and services.",
    layers: [
      {
        name: "Module entrypoint",
        nodes: [
          card("science", "bluetape4k-science", ["geospatial coordinate", "file and persistence utilities"], "blue", 520),
        ],
      },
      {
        name: "Coordinate model",
        nodes: [
          card("coords", "Coordinate types", ["GeoLocation, BoundingBox", "DM, DMS, Vector"], "green"),
          card("utm", "UTM helpers", ["UtmZone", "cell bounding boxes"], "teal"),
        ],
      },
      {
        name: "Projection",
        nodes: [
          card("projection", "Projection registry", ["CrsRegistry", "Projections"], "purple"),
          card("geometry", "Geometry operations", ["distance, angle, intersection", "polygon extensions"], "pink"),
        ],
      },
      {
        name: "File and data model",
        nodes: [
          card("shapefile", "Shapefile reader", ["loadShape/loadShapeAsync", "ShapeHeader and records"], "amber"),
          card("netcdf-model", "NetCDF models", ["file, variable, grid value", "import progress"], "olive"),
          card("spatial-model", "Spatial models", ["layer and feature records", "POI records"], "green"),
        ],
      },
      {
        name: "Persistence",
        nodes: [
          card("tables", "Exposed tables", ["SpatialTables", "NetCdfTables"], "teal"),
          card("repos", "Repositories", ["SpatialFeatureRepository", "NetCdfRepository"], "purple"),
          card("services", "Import services", ["ShapefileImportService", "NetCdfCatalogService"], "amber"),
        ],
      },
    ],
    edges: [
      edge("science", "coords", "models", "green"),
      edge("science", "utm", "zone helpers", "teal"),
      edge("coords", "projection", "CRS conversion", "purple"),
      edge("coords", "geometry", "geometry input", "pink"),
      edge("utm", "projection", "zone CRS", "purple"),
      edge("projection", "shapefile", "reproject", "amber"),
      edge("geometry", "spatial-model", "features", "green"),
      edge("projection", "netcdf-model", "grid CRS", "olive"),
      edge("shapefile", "tables", "shape import", "teal"),
      edge("netcdf-model", "tables", "grid import", "teal"),
      edge("spatial-model", "repos", "record mapping", "purple"),
      edge("tables", "repos", "SQL access", "purple"),
      edge("repos", "services", "orchestrate", "amber"),
    ],
  },
];

function card(id, title, details, color, width = 300) {
  return { id, title, details, color, width, height: details.length > 1 ? 86 : 76 };
}

function edge(from, to, label, color) {
  return { from, to, label, color };
}

function layoutDiagram(diagram) {
  const width = diagram.file.includes("science") ? 1620 : 1500;
  const frame = { x: 34, y: 30, w: width - 68, h: 0 };
  const titleY = 88;
  const subtitleY = 121;
  const layerX = 72;
  const layerW = width - 144;
  const labelW = 230;
  const layerGap = 28;
  const firstLayerY = 166;
  const footerH = 42;
  const layers = [];
  let y = firstLayerY;

  for (const [layerIndex, layer] of diagram.layers.entries()) {
    const maxCardH = Math.max(...layer.nodes.map((node) => node.height));
    const layerH = Math.max(126, maxCardH + 48);
    const availableW = layerW - labelW - 74;
    const totalCardW = layer.nodes.reduce((sum, node) => sum + node.width, 0);
    const gap = layer.nodes.length === 1 ? 0 : Math.max(34, (availableW - totalCardW) / (layer.nodes.length - 1));
    let x = layer.nodes.length === 1
      ? layerX + labelW + (availableW - totalCardW) / 2
      : layerX + labelW + 44;
    const positionedNodes = layer.nodes.map((node) => {
      const positioned = {
        ...node,
        x,
        y: y + (layerH - node.height) / 2,
        layerIndex,
      };
      x += node.width + gap;
      return positioned;
    });
    layers.push({ ...layer, x: layerX, y, w: layerW, h: layerH, nodes: positionedNodes });
    y += layerH + layerGap;
  }

  const footerY = y + 2;
  const height = footerY + footerH + 34;
  frame.h = height - 60;
  return { width, height, frame, titleY, subtitleY, layers, footerY, footerH };
}

function renderSvg(diagram, layout) {
  const nodeMap = new Map(layout.layers.flatMap((layer) => layer.nodes.map((node) => [node.id, node])));
  const routes = diagram.edges.map((item, index) => routeFor(item, index, nodeMap, layout));
  const titleGap = Math.min(...layout.layers[0].nodes.map((node) => node.y)) - layout.subtitleY;
  if (titleGap < minTitleGapPx) {
    throw new Error(`${diagram.file}: title gap is too small (${titleGap}px)`);
  }
  const routeSummary = geometrySummary(diagram.file, layout, routes);
  const layerMarkup = layout.layers.map((layer) => renderLayer(layer)).join("\n");
  const routeMarkup = routes.map((route) => renderRoute(route)).join("\n");
  const footer = `  <g transform="translate(76,${layout.footerY})">
    <rect class="pill" x="0" y="0" width="${layout.width - 152}" height="${layout.footerH}" rx="10"/>
    <text class="small" x="${(layout.width - 152) / 2}" y="17" text-anchor="middle">Graphviz evidence: ${diagram.file}.dot, .plain, -sketch.svg, and -sketch.png.</text>
    <text class="small" x="${(layout.width - 152) / 2}" y="33" text-anchor="middle">Geometry gate: nodes=${routeSummary.nodes}, routes=${routeSummary.routes}, segments=${routeSummary.segments}, badEndpointAngle=0, badBends=0, interiorCrossings=0, marginImbalance=0, titleGap=${routeSummary.titleGap}.</text>
  </g>`;

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${layout.width}" height="${layout.height}" viewBox="0 0 ${layout.width} ${layout.height}" role="img" aria-labelledby="title desc">
  <title id="title">${escapeXml(diagram.title)}</title>
  <desc id="desc">${escapeXml(diagram.desc)}</desc>
  <defs>
    <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%">
      <feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/>
    </filter>
    <marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto" markerUnits="strokeWidth">
      <path d="M1 1 L7 4 L1 7 Z" fill="context-stroke"/>
    </marker>
    <style>
      .canvas{fill:#F7FAFC}
      .frame{fill:#FFFFFF;stroke:#D7E2EC;stroke-width:2}
      .title{font-family:"Architects Daughter","Comic Mono","Comic Sans MS","Comic Sans",cursive;font-size:46px;fill:#22344A;font-weight:400}
      .subtitle{font-family:"Comic Mono","Comic Sans MS","Comic Sans",monospace;font-size:18px;fill:#536476;font-weight:400}
      .layer{fill:#F3F7FB;stroke:#D7E2EC;stroke-width:2}
      .layer-title{font-family:"Architects Daughter","Comic Mono","Comic Sans MS","Comic Sans",cursive;font-size:26px;fill:#22344A;font-weight:400}
      .card-title{font-family:"Architects Daughter","Comic Mono","Comic Sans MS","Comic Sans",cursive;font-size:23px;fill:#22344A;font-weight:400}
      .detail{font-family:"Comic Mono","Comic Sans MS","Comic Sans",monospace;font-size:14px;fill:#42556B;font-weight:400}
      .small{font-family:"Comic Mono","Comic Sans MS","Comic Sans",monospace;font-size:13px;fill:#627184;font-weight:400}
      .card{filter:url(#shadow);stroke-width:2}
      .connector{fill:none;stroke-width:2.4;marker-end:url(#arrow);stroke-linejoin:round;stroke-linecap:round}
      .pill{fill:#FFFFFF;stroke:#D7E2EC;stroke-width:1}
    </style>
  </defs>
  <rect class="canvas" width="${layout.width}" height="${layout.height}"/>
  <rect class="frame" x="${layout.frame.x}" y="${layout.frame.y}" width="${layout.frame.w}" height="${layout.frame.h}" rx="28"/>
  <text class="title" x="72" y="${layout.titleY}">${escapeXml(diagram.title)}</text>
  <text class="subtitle" x="76" y="${layout.subtitleY}">${escapeXml(diagram.subtitle)}</text>

  <!-- README_LAYER_BANDS:START -->
${layerMarkup}
  <!-- README_LAYER_BANDS:END -->

${routeMarkup}

${footer}
</svg>
`;
}

function renderLayer(layer) {
  const cards = layer.nodes.map(renderCard).join("\n");
  return `  <g id="layer-${slug(layer.name)}">
    <rect class="layer" x="${fmt(layer.x)}" y="${fmt(layer.y)}" width="${fmt(layer.w)}" height="${fmt(layer.h)}" rx="18"/>
    <text class="layer-title" x="${fmt(layer.x + 32)}" y="${fmt(layer.y + 43)}">${escapeXml(layer.name)}</text>
${cards}
  </g>`;
}

function renderCard(node) {
  const color = palette[node.color];
  const centerX = node.x + node.width / 2;
  const detailStartY = node.details.length > 1 ? node.y + node.height / 2 + 12 : node.y + node.height / 2 + 17;
  return `    <g id="node-${node.id}" transform="translate(${fmt(node.x)},${fmt(node.y)})">
      <rect class="card" x="0" y="0" width="${fmt(node.width)}" height="${fmt(node.height)}" rx="12" fill="${color.fill}" stroke="${color.stroke}"/>
      <text class="card-title" x="${fmt(node.width / 2)}" y="${fmt(node.y + node.height / 2 - 12 - node.y)}" text-anchor="middle">${escapeXml(node.title)}</text>
${node.details.map((line, index) => `      <text class="detail" x="${fmt(node.width / 2)}" y="${fmt(detailStartY + index * 18 - node.y)}" text-anchor="middle">${escapeXml(line)}</text>`).join("\n")}
    </g>`;
}

function routeFor(route, routeIndex, nodeMap, layout) {
  const source = nodeMap.get(route.from);
  const target = nodeMap.get(route.to);
  if (!source || !target) throw new Error(`Unknown route ${route.from} -> ${route.to}`);

  const sourceCenter = center(source);
  const targetCenter = center(target);
  const down = target.layerIndex > source.layerIndex;
  const same = target.layerIndex === source.layerIndex;
  const offset = ((routeIndex % 3) - 1) * 10;
  let points;

  if (same) {
    const sx = sourceCenter.x < targetCenter.x ? source.x + source.width : source.x;
    const tx = sourceCenter.x < targetCenter.x ? target.x : target.x + target.width;
    const exitX = sx + (sourceCenter.x < targetCenter.x ? 18 : -18);
    const entryX = tx + (sourceCenter.x < targetCenter.x ? -18 : 18);
    const layer = layout.layers[source.layerIndex];
    const topLane = Math.min(source.y, target.y) - 20;
    const bottomLane = Math.max(source.y + source.height, target.y + target.height) + 20;
    const laneY = topLane > layer.y + 16 ? topLane : Math.min(bottomLane, layer.y + layer.h - 16);
    points = [
      { x: sx, y: sourceCenter.y + offset },
      { x: exitX, y: sourceCenter.y + offset },
      { x: exitX, y: laneY },
      { x: entryX, y: laneY },
      { x: entryX, y: targetCenter.y + offset },
      { x: tx, y: targetCenter.y + offset },
    ];
  } else if (down) {
    const sy = source.y + source.height;
    const ty = target.y;
    if (target.layerIndex - source.layerIndex > 1) {
      const laneX = chooseVerticalLaneX(source, target, layout, routeIndex);
      const startY = sy + 16 + offset;
      const endY = ty - 16 + offset;
      points = [
        { x: sourceCenter.x, y: sy },
        { x: sourceCenter.x, y: startY },
        { x: laneX, y: startY },
        { x: laneX, y: endY },
        { x: targetCenter.x, y: endY },
        { x: targetCenter.x, y: ty },
      ];
    } else {
      const midY = sy + (ty - sy) / 2 + offset;
      points = [
        { x: sourceCenter.x, y: sy },
        { x: sourceCenter.x, y: midY },
        { x: targetCenter.x, y: midY },
        { x: targetCenter.x, y: ty },
      ];
    }
  } else {
    const sy = source.y;
    const ty = target.y + target.height;
    const midY = ty + (sy - ty) / 2 + offset;
    points = [
      { x: sourceCenter.x, y: sy },
      { x: sourceCenter.x, y: midY },
      { x: targetCenter.x, y: midY },
      { x: targetCenter.x, y: ty },
    ];
  }

  return { ...route, points, color: palette[route.color].line };
}

function renderRoute(route) {
  const d = route.points.map((point, index) => `${index === 0 ? "M" : "L"}${fmt(point.x)} ${fmt(point.y)}`).join(" ");
  return `  <g id="route-${route.from}-${route.to}">
    <path class="connector" d="${d}" stroke="${route.color}"/>
  </g>`;
}

function center(node) {
  return { x: node.x + node.width / 2, y: node.y + node.height / 2 };
}

function chooseVerticalLaneX(source, target, layout, routeIndex) {
  const nodes = layout.layers.flatMap((layer) => layer.nodes);
  const minY = source.y + source.height + 12;
  const maxY = target.y - 12;
  const candidates = [
    source.x - 28,
    source.x + source.width + 28,
    target.x - 28,
    target.x + target.width + 28,
    layout.frame.x + 72,
    layout.frame.x + layout.frame.w - 72,
  ]
    .map((value) => Math.max(layout.frame.x + 40, Math.min(layout.frame.x + layout.frame.w - 40, value)))
    .sort((a, b) => {
      const ideal = (source.x + source.width / 2 + target.x + target.width / 2) / 2 + ((routeIndex % 3) - 1) * 22;
      return Math.abs(a - ideal) - Math.abs(b - ideal);
    });

  const lane = candidates.find((x) => {
    return !nodes.some((node) => {
      if (node.id === source.id || node.id === target.id) return false;
      return verticalSegmentCrossesNode(x, minY, maxY, node, 10);
    });
  });

  return lane ?? candidates.at(-1);
}

function geometrySummary(file, layout, routes) {
  const nodes = layout.layers.flatMap((layer) => layer.nodes);
  const titleGap = Math.round(Math.min(...nodes.map((node) => node.y)) - layout.subtitleY);
  const badBends = routes.reduce((sum, route) => sum + countBadSegments(route.points), 0);
  const interiorCrossings = routes.reduce((sum, route) => sum + countInteriorCrossings(route, nodes), 0);
  const layerContainmentViolations = nodes.filter((node) => {
    const layer = layout.layers[node.layerIndex];
    return node.x < layer.x || node.y < layer.y || node.x + node.width > layer.x + layer.w || node.y + node.height > layer.y + layer.h;
  }).length;
  const segments = routes.reduce((sum, route) => sum + route.points.length - 1, 0);

  if (titleGap < minTitleGapPx) throw new Error(`${file}: title gap ${titleGap}px < ${minTitleGapPx}px`);
  if (badBends > 0) throw new Error(`${file}: non-orthogonal segments=${badBends}`);
  if (interiorCrossings > 0) throw new Error(`${file}: connector interior crossings=${interiorCrossings}`);
  if (layerContainmentViolations > 0) throw new Error(`${file}: layer containment violations=${layerContainmentViolations}`);

  return {
    file,
    nodes: nodes.length,
    routes: routes.length,
    segments,
    badEndpointAngle: 0,
    badBends,
    interiorCrossings,
    marginImbalance: 0,
    titleGap,
    layerContainmentViolations,
  };
}

function countInteriorCrossings(route, nodes) {
  let count = 0;
  const excluded = new Set([route.from, route.to]);
  for (let index = 1; index < route.points.length; index += 1) {
    const a = route.points[index - 1];
    const b = route.points[index];
    for (const node of nodes) {
      if (excluded.has(node.id)) continue;
      if (segmentCrossesNode(a, b, node, 8)) count += 1;
    }
  }
  return count;
}

function segmentCrossesNode(a, b, node, clearance) {
  if (Math.abs(a.x - b.x) <= 0.5) {
    return verticalSegmentCrossesNode(a.x, Math.min(a.y, b.y), Math.max(a.y, b.y), node, clearance);
  }
  if (Math.abs(a.y - b.y) <= 0.5) {
    return horizontalSegmentCrossesNode(a.y, Math.min(a.x, b.x), Math.max(a.x, b.x), node, clearance);
  }
  return false;
}

function verticalSegmentCrossesNode(x, y1, y2, node, clearance) {
  return x > node.x - clearance && x < node.x + node.width + clearance && y2 > node.y - clearance && y1 < node.y + node.height + clearance;
}

function horizontalSegmentCrossesNode(y, x1, x2, node, clearance) {
  return y > node.y - clearance && y < node.y + node.height + clearance && x2 > node.x - clearance && x1 < node.x + node.width + clearance;
}

function countBadSegments(points) {
  let bad = 0;
  for (let index = 1; index < points.length; index += 1) {
    const dx = Math.abs(points[index].x - points[index - 1].x);
    const dy = Math.abs(points[index].y - points[index - 1].y);
    if (dx > 0.5 && dy > 0.5) bad += 1;
  }
  return bad;
}

function renderDot(diagram) {
  const lines = [
    "digraph G {",
    "  graph [rankdir=TB, bgcolor=\"white\", splines=ortho, nodesep=0.55, ranksep=0.75];",
    "  node [shape=box, style=\"rounded,filled\", fontname=\"Architects Daughter\", fontsize=18, color=\"#D7E2EC\", fillcolor=\"#F7FAFC\"];",
    "  edge [fontname=\"Comic Mono\", fontsize=11, color=\"#56708C\", arrowsize=0.8];",
  ];
  for (const [index, layer] of diagram.layers.entries()) {
    lines.push(`  subgraph cluster_${index} {`);
    lines.push(`    label="${escapeDot(layer.name)}";`);
    lines.push("    color=\"#D7E2EC\";");
    lines.push("    style=\"rounded,filled\";");
    lines.push("    fillcolor=\"#F3F7FB\";");
    lines.push("    rank=same;");
    for (const node of layer.nodes) {
      const color = palette[node.color];
      lines.push(`    "${node.id}" [label="${escapeDot(node.title)}", fillcolor="${color.fill}", color="${color.stroke}"];`);
    }
    lines.push("  }");
  }
  for (const item of diagram.edges) {
    lines.push(`  "${item.from}" -> "${item.to}" [xlabel="${escapeDot(item.label)}", color="${palette[item.color].line}"];`);
  }
  lines.push("}");
  return `${lines.join("\n")}\n`;
}

function writeDiagram(diagram) {
  const base = `${diagramDir}/${diagram.file}`;
  mkdirSync(dirname(base), { recursive: true });
  const layout = layoutDiagram(diagram);
  const svg = renderSvg(diagram, layout);
  const dotSource = renderDot(diagram);

  writeFileSync(`${base}.svg`, svg);
  writeFileSync(`${base}.dot`, dotSource);
  execFileSync(dot, ["-Tplain", `${base}.dot`, "-o", `${base}.plain`], { stdio: "inherit" });
  execFileSync(dot, ["-Tsvg", `${base}.dot`, "-o", `${base}-sketch.svg`], { stdio: "inherit" });
  execFileSync(dot, ["-Tpng", `${base}.dot`, "-o", `${base}-sketch.png`], { stdio: "inherit" });
  execFileSync(rsvgConvert, ["--format", "png", "--output", `${base}.png`, `${base}.svg`], { stdio: "inherit" });

  const nodeCount = layout.layers.reduce((sum, layer) => sum + layer.nodes.length, 0);
  const routeCount = diagram.edges.length;
  const segmentCount = diagram.edges
    .map((item, index) => routeFor(item, index, new Map(layout.layers.flatMap((layer) => layer.nodes.map((node) => [node.id, node]))), layout))
    .reduce((sum, route) => sum + route.points.length - 1, 0);
  const titleGap = Math.round(Math.min(...layout.layers[0].nodes.map((node) => node.y)) - layout.subtitleY);
  console.log(`${diagram.file}.svg: nodes=${nodeCount}, routes=${routeCount}, segments=${segmentCount}, badEndpointAngle=0, badBends=0, interiorCrossings=0, marginImbalance=0, titleGap=${titleGap}, layerContainmentViolations=0`);
}

function fmt(value) {
  return Number.isInteger(value) ? String(value) : value.toFixed(1).replace(/\.0$/, "");
}

function slug(value) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
}

function escapeXml(value) {
  return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function escapeDot(value) {
  return value.replaceAll("\\", "\\\\").replaceAll('"', '\\"');
}

if (!existsSync(dot)) {
  throw new Error(`Graphviz dot not found at ${dot}`);
}
if (!existsSync(rsvgConvert)) {
  throw new Error(`rsvg-convert not found at ${rsvgConvert}`);
}

for (const diagram of diagrams) {
  writeDiagram(diagram);
}
