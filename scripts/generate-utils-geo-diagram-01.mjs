#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/geo/README.md",
  "utils/geo/src/main/kotlin/io/bluetape4k/geohash/GeoHash.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geohash/queries/GeoHashBoundingBoxQuery.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geohash/queries/GeoHashCircleQuery.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/google/GoogleGeoService.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingMapService.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geoip2/finder/GeoipFinder.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geoip2/DatabaseReaderExtensions.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Module Overview[\s\S]*utils-geo-diagram-01\.png/, "README overview slot");
assertContains(sources[0], /Geocode[\s\S]*GeoHash[\s\S]*GeoIP2/, "README three-feature contract");
assertContains(sources[1], /class GeoHash[\s\S]*toBase32\(\)[\s\S]*getAdjacent|class GeoHash[\s\S]*toBase32\(\)/, "GeoHash base32 surface");
assertContains(sources[2], /class GeoHashBoundingBoxQuery[\s\S]*isIntersection180Meridian[\s\S]*getSearchHashes/, "GeoHash bounding box query");
assertContains(sources[3], /requireZeroOrPositiveNumber[\s\S]*class GeoHashCircleQuery[\s\S]*VincentyGeodesy/, "GeoHash circle query");
assertContains(sources[4], /object GoogleGeoService[\s\S]*GOOGLE_GEOCODE_API_KEY[\s\S]*GeoApiContext/, "Google geocode service");
assertContains(sources[5], /object BingMapService[\s\S]*BING_GEOCODE_API_KEY[\s\S]*Feign[\s\S]*CoroutineFeign/, "Bing Feign clients");
assertContains(sources[6], /interface GeoipFinder[\s\S]*findAddress\(ipAddress: InetAddress\)/, "GeoIP finder contract");
assertContains(sources[7], /DatabaseReader\.tryFindCity[\s\S]*DatabaseReader\.tryFindCountry/, "MaxMind reader extensions");

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
  <text class="kicker" x="${x + 24}" y="${y + 32}">${esc(kicker)}</text>
  <text class="cardTitle" x="${x + 24}" y="${y + 70}">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 92}H${x + w}" stroke="${dark}"/>
  ${lines.map((line, index) => `<text class="body" x="${x + 24}" y="${y + 128 + index * 26}">${esc(line)}</text>`).join("\n")}
  ${footer ? `<path class="divider" d="M${x} ${y + h - 48}H${x + w}" stroke="${dark}"/><text class="foot" x="${x + 24}" y="${y + h - 18}">${esc(footer)}</text>` : ""}
</g>`;
}

function lane({ x, y, w, h, title }) {
  return `<g>
  <rect class="lane" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/>
  <text class="laneTitle" x="${x + 30}" y="${y + 44}">${esc(title)}</text>
</g>`;
}

function edge({ from, to, points, color, dashed = false, label = "", labelAt }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const p = labelAt ?? points[Math.floor(points.length / 2)];
  const labelWidth = label ? Math.max(110, label.length * 8 + 24) : 0;
  return `<g data-from="${esc(from)}" data-to="${esc(to)}">
  <path class="edge ${dashed ? "dashed" : ""}" d="${d}" stroke="${dark}" marker-end="url(#arrow-${color})"/>
  ${label ? `<rect class="edgeLabelBg" x="${p[0] - 8}" y="${p[1] - 17}" width="${labelWidth}" height="24" rx="4"/><text class="edgeLabel" x="${p[0]}" y="${p[1]}">${esc(label)}</text>` : ""}
</g>`;
}

const width = 2800;
const height = 1880;

const lanes = [
  lane({ x: 110, y: 340, w: 800, h: 1130, title: "GeoHash: in-process spatial indexing" }),
  lane({ x: 1000, y: 340, w: 800, h: 1130, title: "Geocode: provider-backed address lookup" }),
  lane({ x: 1890, y: 340, w: 800, h: 1130, title: "GeoIP2: MaxMind database lookup" }),
  lane({ x: 110, y: 1530, w: 2580, h: 280, title: "Optional runtime boundaries" }),
];

const nodes = [
  card({
    id: "Module",
    x: 820,
    y: 150,
    w: 1160,
    h: 155,
    color: "slate",
    kicker: "bluetape4k-geo",
    title: "Consolidated module for Geocode, GeoHash, and GeoIP2",
    lines: ["Former utils/geocode, utils/geohash, and utils/geoip2 surfaces now ship together."],
  }),
  card({
    id: "WGS84",
    x: 180,
    y: 430,
    w: 660,
    h: 210,
    color: "green",
    kicker: "geohash package",
    title: "WGS84Point + GeoHash",
    lines: ["lat/lon encode to bit precision", "Base32 output up to 12 chars", "bounding box retained for containment"],
  }),
  card({
    id: "GeoHashQuery",
    x: 180,
    y: 760,
    w: 660,
    h: 230,
    color: "teal",
    kicker: "queries package",
    title: "Bounding box and circle search",
    lines: ["GeoHashBoundingBoxQuery builds search hashes", "180-meridian boxes split into east/west searches", "GeoHashCircleQuery wraps radius with Vincenty geodesy"],
  }),
  card({
    id: "GeoHashUse",
    x: 180,
    y: 1110,
    w: 660,
    h: 210,
    color: "blue",
    kicker: "consumer output",
    title: "Spatial key set",
    lines: ["Base32 key for storage/search", "neighbors and adjacent cells", "WKT query box for diagnostics"],
  }),
  card({
    id: "GeocodeApi",
    x: 1070,
    y: 430,
    w: 660,
    h: 210,
    color: "amber",
    kicker: "geocode package",
    title: "Address finder contracts",
    lines: ["GeocodeAddressFinder returns Address?", "SuspendGeocodeAddressFinder adds coroutine lookup", "Geocode and Address are provider-neutral models"],
  }),
  card({
    id: "Providers",
    x: 1070,
    y: 760,
    w: 660,
    h: 230,
    color: "pink",
    kicker: "google + bing packages",
    title: "Provider adapters",
    lines: ["GoogleGeoService lazy-loads GeoApiContext", "BingMapService builds Feign and CoroutineFeign clients", "API keys come from environment variables"],
  }),
  card({
    id: "ProviderOutput",
    x: 1070,
    y: 1110,
    w: 660,
    h: 210,
    color: "violet",
    kicker: "consumer output",
    title: "Resolved address data",
    lines: ["coordinates to administrative address", "provider models mapped into bluetape4k Address", "sync or suspend client path"],
  }),
  card({
    id: "GeoipApi",
    x: 1960,
    y: 430,
    w: 660,
    h: 210,
    color: "violet",
    kicker: "geoip2 package",
    title: "GeoipFinder contract",
    lines: ["findAddress(InetAddress): Address?", "city and country finder variants", "typed Address for administrative location"],
  }),
  card({
    id: "MaxMindReader",
    x: 1960,
    y: 760,
    w: 660,
    h: 230,
    color: "blue",
    kicker: "DatabaseReader extensions",
    title: "Safe MaxMind reader calls",
    lines: ["tryFindCity(ip) returns Result<CityResponse>", "tryFindCountry(ip) returns Result<CountryResponse>", "lookup errors stay in Result failure"],
  }),
  card({
    id: "GeoipOutput",
    x: 1960,
    y: 1110,
    w: 660,
    h: 210,
    color: "green",
    kicker: "consumer output",
    title: "IP location data",
    lines: ["IP to city/country details", "latitude and longitude when database contains it", "local mmdb file controls freshness"],
  }),
  card({
    id: "PureKotlin",
    x: 180,
    y: 1590,
    w: 520,
    h: 180,
    color: "green",
    kicker: "GeoHash",
    title: "No external service",
    lines: ["pure Kotlin spatial indexing"],
  }),
  card({
    id: "MapApis",
    x: 870,
    y: 1590,
    w: 780,
    h: 180,
    color: "pink",
    kicker: "Geocode",
    title: "Google + Bing provider clients",
    lines: ["Google Maps Services", "Bing Maps REST via Feign"],
  }),
  card({
    id: "MaxMind",
    x: 1820,
    y: 1590,
    w: 720,
    h: 180,
    color: "blue",
    kicker: "GeoIP2",
    title: "MaxMind local database",
    lines: ["GeoIP2 DatabaseReader", "local mmdb files"],
  }),
];

const edges = [
  edge({ from: "Module", to: "WGS84", points: [[1260, 305], [1260, 325], [510, 325], [510, 430]], color: "green", label: "pure coordinate utilities", labelAt: [235, 318] }),
  edge({ from: "Module", to: "GeocodeApi", points: [[1400, 305], [1400, 430]], color: "amber", label: "address lookup APIs", labelAt: [1430, 370] }),
  edge({ from: "Module", to: "GeoipApi", points: [[1540, 305], [1540, 325], [2290, 325], [2290, 430]], color: "violet", label: "IP lookup APIs", labelAt: [2260, 318] }),
  edge({ from: "WGS84", to: "GeoHashQuery", points: [[510, 640], [510, 760]], color: "green", label: "encoded cells", labelAt: [535, 710] }),
  edge({ from: "GeoHashQuery", to: "GeoHashUse", points: [[510, 990], [510, 1110]], color: "teal", label: "search hashes", labelAt: [535, 1060] }),
  edge({ from: "GeocodeApi", to: "Providers", points: [[1400, 640], [1400, 760]], color: "amber", label: "delegates by provider", labelAt: [1425, 710] }),
  edge({ from: "Providers", to: "ProviderOutput", points: [[1400, 990], [1400, 1110]], color: "pink", label: "mapped response", labelAt: [1425, 1060] }),
  edge({ from: "GeoipApi", to: "MaxMindReader", points: [[2290, 640], [2290, 760]], color: "violet", label: "DatabaseReader", labelAt: [2315, 710] }),
  edge({ from: "MaxMindReader", to: "GeoipOutput", points: [[2290, 990], [2290, 1110]], color: "blue", label: "Result unwrapped by finder", labelAt: [2315, 1060] }),
  edge({ from: "GeoHashUse", to: "PureKotlin", points: [[510, 1320], [510, 1590]], color: "green", dashed: true }),
  edge({ from: "ProviderOutput", to: "MapApis", points: [[1400, 1320], [1400, 1590]], color: "pink", dashed: true }),
  edge({ from: "GeoipOutput", to: "MaxMind", points: [[2290, 1320], [2290, 1590]], color: "blue", dashed: true }),
];

const svg = `<svg data-intent="Explain the utils/geo module overview from current README and source packages." data-evidence="${esc(sources.join("; "))}" data-source-read="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Geo Module Overview">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}
    .frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:58px;fill:#0F172A}
    .subtitle{font-family:"Comic Mono";font-size:17px;fill:#475569}
    .lane{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5}
    .laneTitle{font-family:"Comic Mono";font-size:15px;font-weight:700;fill:#64748B}
    .card{filter:url(#shadow);stroke-width:2}
    .kicker{font-family:"Comic Mono";font-size:14px;fill:#475569;font-weight:700}
    .cardTitle{font-family:"Architects Daughter";font-size:28px;fill:#0F172A}
    .body{font-family:"Comic Mono";font-size:16px;fill:#334155}
    .foot{font-family:"Comic Mono";font-size:14px;fill:#475569}
    .divider{stroke-width:1;opacity:.35}
    .edge{fill:none;stroke-width:3.6;stroke-linecap:round;stroke-linejoin:round}
    .dashed{stroke-dasharray:10 10}
    .edgeLabelBg{fill:#FFFFFF;stroke:#E2E8F0;stroke-width:.8;opacity:.94}
    .edgeLabel{font-family:"Comic Mono";font-size:14px;fill:#334155}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="36" y="30" width="${width - 72}" height="${height - 60}" rx="8"/>
<text class="title" x="76" y="100">Geo Module Overview</text>
<text class="subtitle" x="80" y="134">A consolidated module with one pure spatial-index lane, one provider-backed geocoding lane, and one local GeoIP2 lookup lane.</text>
${lanes.join("\n")}
${nodes.join("\n")}
${edges.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-geo-diagram-01.svg");
const pngPath = join(OUT, "utils-geo-diagram-01.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--output-width", String(width * 2), "--output-height", String(height * 2)], { stdio: "inherit" });
console.log("Generated utils-geo-diagram-01.svg/png");
