#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";

const sources = [
  "utils/geo/README.md",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/Geocode.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/Address.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/GeocodeAddressFinder.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/SuspendGeocodeAddressFinder.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/google/GoogleAddressFinder.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/google/GoogleAddress.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/google/GoogleGeoService.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingAddressFinder.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingAddress.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingMapService.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingMapModel.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geohash/GeoHash.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geohash/WGS84Point.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geohash/BoundingBox.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geohash/queries/GeoHashQuery.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geohash/queries/GeoHashBoundingBoxQuery.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geohash/queries/GeoHashCircleQuery.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geoip2/Geoip.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geoip2/finder/GeoipFinder.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geoip2/finder/GeoipCityFinder.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geoip2/finder/GeoipCountryFinder.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geoip2/Address.kt",
  "utils/geo/src/main/kotlin/io/bluetape4k/geoip2/GeoLocation.kt",
];

for (const source of sources) {
  if (!existsSync(join(ROOT, source))) throw new Error(`Missing evidence source: ${source}`);
}

function assertContains(source, pattern, label) {
  const text = readFileSync(join(ROOT, source), "utf8");
  if (!pattern.test(text)) throw new Error(`Expected ${label} in ${source}`);
}

assertContains(sources[0], /Class Diagram[\s\S]*utils-geo-diagram-02\.png/, "README class diagram slot");
assertContains(sources[1], /data class Geocode[\s\S]*val latitude: BigDecimal[\s\S]*fun parse[\s\S]*fun round/, "Geocode value object");
assertContains(sources[3], /interface GeocodeAddressFinder: SuspendGeocodeAddressFinder/, "sync finder extends suspend finder");
assertContains(sources[5], /class GoogleAddressFinder private constructor\(apiKey: String\): GeocodeAddressFinder[\s\S]*reverseGeocode[\s\S]*toAddress/, "Google reverse geocoder");
assertContains(sources[8], /class BingAddressFinder: GeocodeAddressFinder[\s\S]*client\.locations[\s\S]*toBingAddress/, "Bing reverse geocoder");
assertContains(sources[10], /object BingMapService[\s\S]*interface BingMapApi[\s\S]*interface BingMapCoroutineApi/, "Bing Feign service contracts");
assertContains(sources[12], /class GeoHash[\s\S]*var point: WGS84Point[\s\S]*var boundingBox: BoundingBox[\s\S]*significantBits/, "GeoHash state");
assertContains(sources[15], /interface GeoHashQuery[\s\S]*contains\(hash: GeoHash\)[\s\S]*getSearchHashes\(\): List<GeoHash>/, "GeoHashQuery contract");
assertContains(sources[16], /class GeoHashBoundingBoxQuery[\s\S]*: GeoHashQuery,[\s\S]*private val searchHashes/, "bounding box query");
assertContains(sources[17], /class GeoHashCircleQuery[\s\S]*: GeoHashQuery,[\s\S]*private val query: GeoHashBoundingBoxQuery/, "circle query delegates to bounding query");
assertContains(sources[18], /object Geoip[\s\S]*asnDatabase[\s\S]*cityDatabase[\s\S]*countryDatabase/, "Geoip database readers");
assertContains(sources[20], /class GeoipCityFinder: GeoipFinder[\s\S]*Geoip\.cityDatabase[\s\S]*Address\.fromCity/, "Geoip city finder");
assertContains(sources[21], /class GeoipCountryFinder: GeoipFinder[\s\S]*Geoip\.countryDatabase[\s\S]*Address\.fromCountry/, "Geoip country finder");
assertContains(sources[22], /data class Address[\s\S]*geoLocation: GeoLocation\?[\s\S]*fromCity[\s\S]*fromCountry/, "Geoip address mapping");

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
  return Object.entries(palette).map(([name, [, stroke, dark]]) => `
  <marker id="open-${name}" markerWidth="22" markerHeight="22" refX="19" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 3 3 L 19 11 L 3 19" fill="none" stroke="${dark}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/></marker>
  <marker id="inherit-${name}" markerWidth="24" markerHeight="22" refX="20" refY="11" orient="auto" markerUnits="userSpaceOnUse"><path d="M 3 3 L 20 11 L 3 19 Z" fill="#FFFFFF" stroke="${stroke}" stroke-width="2"/></marker>`).join("\n");
}

function column({ x, y, w, h, title, note }) {
  return `<g>
  <rect class="column" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/>
  <text class="columnTitle" x="${x + 24}" y="${y + 40}">${esc(title)}</text>
  <text class="columnNote" x="${x + 24}" y="${y + 72}">${esc(note)}</text>
</g>`;
}

function classBox({ id, x, y, w, h, color, kind, title, attrs = [], ops = [] }) {
  const [fill, stroke, dark] = palette[color];
  const attrY = y + 112;
  const opY = attrs.length ? attrY + attrs.length * 23 + 24 : y + 116;
  return `<g id="${esc(id)}">
  <rect class="classBox" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="kind" x="${x + w / 2}" y="${y + 30}" text-anchor="middle">${esc(kind)}</text>
  <text class="classTitle" x="${x + w / 2}" y="${y + 64}" text-anchor="middle">${esc(title)}</text>
  <path class="divider" d="M${x} ${y + 86}H${x + w}" stroke="${dark}"/>
  ${attrs.map((line, index) => `<text class="member" x="${x + 22}" y="${attrY + index * 23}">${esc(line)}</text>`).join("\n")}
  ${ops.length ? `<path class="divider" d="M${x} ${opY - 22}H${x + w}" stroke="${dark}"/>` : ""}
  ${ops.map((line, index) => `<text class="member" x="${x + 22}" y="${opY + index * 23}">${esc(line)}</text>`).join("\n")}
</g>`;
}

function edge({ from, to, points, color, type = "uses", label }) {
  const [, , dark] = palette[color];
  const d = points.map((point, index) => `${index === 0 ? "M" : "L"}${point[0]} ${point[1]}`).join(" ");
  const marker = type === "inherit" || type === "implements" ? `inherit-${color}` : `open-${color}`;
  const klass = type === "implements" || type === "uses" ? "edge dashed" : "edge";
  const path = `<path class="${klass}" data-from="${esc(from)}" data-to="${esc(to)}" d="${d}" stroke="${dark}" marker-end="url(#${marker})"/>`;
  if (!label) return path;
  const mid = points[Math.floor(points.length / 2)];
  const labelWidth = Math.max(96, label.length * 8 + 18);
  return `${path}
  <g>
    <rect class="edgeLabelBg" x="${mid[0] - labelWidth / 2}" y="${mid[1] - 24}" width="${labelWidth}" height="24" rx="5"/>
    <text class="edgeLabel" x="${mid[0]}" y="${mid[1] - 7}" text-anchor="middle">${esc(label)}</text>
  </g>`;
}

const width = 3460;
const height = 2220;

const columns = [
  column({ x: 80, y: 215, w: 1020, h: 1840, title: "GeoHash model and queries", note: "Value model plus query implementations for spatial search." }),
  column({ x: 1220, y: 215, w: 1020, h: 1840, title: "Geocode reverse lookup", note: "Geocode value enters sync/suspend Google and Bing finders." }),
  column({ x: 2360, y: 215, w: 1020, h: 1840, title: "GeoIP2 database lookup", note: "Local MaxMind readers feed city/country address mapping." }),
];

const boxes = [
  classBox({
    id: "WGS84Point",
    x: 145,
    y: 350,
    w: 390,
    h: 178,
    color: "green",
    kind: "data class",
    title: "WGS84Point",
    attrs: ["latitude: Double", "longitude: Double"],
  }),
  classBox({
    id: "BoundingBox",
    x: 650,
    y: 350,
    w: 390,
    h: 178,
    color: "green",
    kind: "data class",
    title: "BoundingBox",
    attrs: ["south/north latitude", "west/east longitude", "contains(), intersects()"],
  }),
  classBox({
    id: "GeoHash",
    x: 300,
    y: 660,
    w: 580,
    h: 238,
    color: "teal",
    kind: "class",
    title: "GeoHash",
    attrs: ["bits: Long", "point: WGS84Point", "boundingBox: BoundingBox", "significantBits: Byte"],
    ops: ["toBase32(), next(), prev(), ord()", "contains(point), within(hash)"],
  }),
  classBox({
    id: "GeoHashQuery",
    x: 300,
    y: 1035,
    w: 580,
    h: 240,
    color: "blue",
    kind: "<<interface>>",
    title: "GeoHashQuery",
    ops: ["contains(hash: GeoHash): Boolean", "contains(point: WGS84Point): Boolean", "getSearchHashes(): List<GeoHash>", "getWktBox(): String"],
  }),
  classBox({
    id: "GeoHashCircleQuery",
    x: 135,
    y: 1435,
    w: 430,
    h: 236,
    color: "blue",
    kind: "class",
    title: "GeoHashCircleQuery",
    attrs: ["center: WGS84Point", "radius: Double", "query: GeoHashBoundingBoxQuery"],
    ops: ["validates radius", "delegates contains/search/WKT"],
  }),
  classBox({
    id: "GeoHashBoundingBoxQuery",
    x: 630,
    y: 1435,
    w: 430,
    h: 236,
    color: "blue",
    kind: "class",
    title: "GeoHashBoundingBoxQuery",
    attrs: ["bbox: BoundingBox", "searchHashes: ArrayList<GeoHash>", "boundingBox: BoundingBox?"],
    ops: ["buildSearchHashes()", "expandSearch(centerHash, bbox)"],
  }),
  classBox({
    id: "Geocode",
    x: 1415,
    y: 350,
    w: 630,
    h: 230,
    color: "green",
    kind: "data class",
    title: "Geocode",
    attrs: ["latitude: BigDecimal", "longitude: BigDecimal"],
    ops: ["parse(\"lat,lon\")", "round(scale, roundingMode)", "toLatLng() extension"],
  }),
  classBox({
    id: "SuspendGeocodeAddressFinder",
    x: 1360,
    y: 690,
    w: 740,
    h: 154,
    color: "amber",
    kind: "<<interface>>",
    title: "SuspendGeocodeAddressFinder",
    ops: ["suspendFindAddress(geocode, language): Address?"],
  }),
  classBox({
    id: "GeocodeAddressFinder",
    x: 1360,
    y: 950,
    w: 740,
    h: 154,
    color: "amber",
    kind: "<<interface>>",
    title: "GeocodeAddressFinder",
    ops: ["findAddress(geocode, language): Address?"],
  }),
  classBox({
    id: "GoogleAddressFinder",
    x: 1265,
    y: 1245,
    w: 430,
    h: 232,
    color: "pink",
    kind: "class",
    title: "GoogleAddressFinder",
    attrs: ["context: GeoApiContext", "apiKey from GoogleGeoService"],
    ops: ["GeocodingApi.reverseGeocode()", "callback API wrapped for suspend"],
  }),
  classBox({
    id: "BingAddressFinder",
    x: 1765,
    y: 1245,
    w: 430,
    h: 232,
    color: "pink",
    kind: "class",
    title: "BingAddressFinder",
    attrs: ["client: BingMapApi", "asyncClient: BingMapCoroutineApi"],
    ops: ["client.locations()", "Location.toBingAddress()"],
  }),
  classBox({
    id: "GeoAddress",
    x: 1360,
    y: 1625,
    w: 740,
    h: 174,
    color: "violet",
    kind: "abstract class",
    title: "geocode.Address",
    attrs: ["country: String?", "city: String?"],
    ops: ["value equality by country + city"],
  }),
  classBox({
    id: "GoogleAddress",
    x: 1265,
    y: 1875,
    w: 430,
    h: 140,
    color: "violet",
    kind: "class",
    title: "GoogleAddress",
    attrs: ["placeId, detail, zip", "formattedAddress"],
  }),
  classBox({
    id: "BingAddress",
    x: 1765,
    y: 1875,
    w: 430,
    h: 140,
    color: "violet",
    kind: "class",
    title: "BingAddress",
    attrs: ["name, detail, zip", "formattedAddress"],
  }),
  classBox({
    id: "Geoip",
    x: 2520,
    y: 350,
    w: 700,
    h: 205,
    color: "slate",
    kind: "object",
    title: "Geoip",
    attrs: ["asnDatabase: DatabaseReader", "cityDatabase: DatabaseReader", "countryDatabase: DatabaseReader"],
    ops: ["lazy readers from bundled mmdb resources"],
  }),
  classBox({
    id: "GeoipFinder",
    x: 2520,
    y: 690,
    w: 700,
    h: 154,
    color: "green",
    kind: "<<interface>>",
    title: "GeoipFinder",
    ops: ["findAddress(ipAddress: InetAddress): Address?"],
  }),
  classBox({
    id: "GeoipCityFinder",
    x: 2410,
    y: 1010,
    w: 430,
    h: 205,
    color: "green",
    kind: "class",
    title: "GeoipCityFinder",
    ops: ["Geoip.cityDatabase.tryFindCity(ip)", "Address.fromCity(ip, response)"],
  }),
  classBox({
    id: "GeoipCountryFinder",
    x: 2900,
    y: 1010,
    w: 430,
    h: 205,
    color: "green",
    kind: "class",
    title: "GeoipCountryFinder",
    ops: ["Geoip.countryDatabase.tryFindCountry(ip)", "Address.fromCountry(ip, response)"],
  }),
  classBox({
    id: "GeoipAddress",
    x: 2520,
    y: 1390,
    w: 700,
    h: 238,
    color: "blue",
    kind: "data class",
    title: "geoip2.Address",
    attrs: ["ipAddress, city, country, continent", "geoLocation: GeoLocation?", "countryIsoCode: String?", "traits: Traits?"],
    ops: ["fromCity(ip, CityResponse)", "fromCountry(ip, CountryResponse)"],
  }),
  classBox({
    id: "GeoLocation",
    x: 2520,
    y: 1775,
    w: 700,
    h: 205,
    color: "teal",
    kind: "data class",
    title: "GeoLocation",
    attrs: ["latitude: Double", "longitude: Double", "timeZone, accuracyRadius"],
    ops: ["fromLocation(location)"],
  }),
];

const edges = [
  edge({ from: "GeoHash", to: "WGS84Point", points: [[445, 660], [445, 528]], color: "green", type: "has" }),
  edge({ from: "GeoHash", to: "BoundingBox", points: [[735, 660], [735, 528]], color: "green", type: "has" }),
  edge({ from: "GeoHashQuery", to: "GeoHash", points: [[300, 1155], [205, 1155], [205, 780], [300, 780]], color: "teal", type: "uses" }),
  edge({ from: "GeoHashQuery", to: "WGS84Point", points: [[300, 1105], [170, 1105], [170, 440], [145, 440]], color: "green", type: "uses" }),
  edge({ from: "GeoHashCircleQuery", to: "GeoHashQuery", points: [[350, 1435], [350, 1345], [508, 1345], [508, 1275]], color: "blue", type: "implements" }),
  edge({ from: "GeoHashBoundingBoxQuery", to: "GeoHashQuery", points: [[845, 1435], [845, 1345], [672, 1345], [672, 1275]], color: "blue", type: "implements" }),
  edge({ from: "GeoHashCircleQuery", to: "GeoHashBoundingBoxQuery", points: [[565, 1550], [630, 1550]], color: "teal", type: "uses" }),
  edge({ from: "GeoHashBoundingBoxQuery", to: "BoundingBox", points: [[845, 1435], [845, 1345], [1030, 1345], [1030, 500], [1040, 500]], color: "green", type: "has" }),
  edge({ from: "GeocodeAddressFinder", to: "SuspendGeocodeAddressFinder", points: [[1730, 950], [1730, 844]], color: "amber", type: "inherit" }),
  edge({ from: "GeocodeAddressFinder", to: "Geocode", points: [[1510, 950], [1510, 580]], color: "green", type: "uses" }),
  edge({ from: "GoogleAddressFinder", to: "GeocodeAddressFinder", points: [[1480, 1245], [1480, 1175], [1595, 1175], [1595, 1104]], color: "pink", type: "implements" }),
  edge({ from: "BingAddressFinder", to: "GeocodeAddressFinder", points: [[1980, 1245], [1980, 1175], [1865, 1175], [1865, 1104]], color: "pink", type: "implements" }),
  edge({ from: "GoogleAddressFinder", to: "GoogleAddress", points: [[1480, 1477], [1480, 1875]], color: "violet", type: "uses" }),
  edge({ from: "BingAddressFinder", to: "BingAddress", points: [[1980, 1477], [1980, 1875]], color: "violet", type: "uses" }),
  edge({ from: "GoogleAddress", to: "GeoAddress", points: [[1480, 1875], [1480, 1799]], color: "violet", type: "inherit" }),
  edge({ from: "BingAddress", to: "GeoAddress", points: [[1980, 1875], [1980, 1799]], color: "violet", type: "inherit" }),
  edge({ from: "GeoipCityFinder", to: "GeoipFinder", points: [[2625, 1010], [2625, 910], [2695, 910], [2695, 844]], color: "green", type: "implements" }),
  edge({ from: "GeoipCountryFinder", to: "GeoipFinder", points: [[3115, 1010], [3115, 910], [3045, 910], [3045, 844]], color: "green", type: "implements" }),
  edge({ from: "GeoipCityFinder", to: "Geoip", points: [[2625, 1010], [2625, 555]], color: "slate", type: "uses" }),
  edge({ from: "GeoipCountryFinder", to: "Geoip", points: [[3115, 1010], [3115, 555]], color: "slate", type: "uses" }),
  edge({ from: "GeoipCityFinder", to: "GeoipAddress", points: [[2625, 1215], [2625, 1390]], color: "blue", type: "uses" }),
  edge({ from: "GeoipCountryFinder", to: "GeoipAddress", points: [[3115, 1215], [3115, 1390]], color: "blue", type: "uses" }),
  edge({ from: "GeoipAddress", to: "GeoLocation", points: [[2870, 1628], [2870, 1775]], color: "teal", type: "has" }),
];

const svg = `<svg data-intent="Explain the current utils/geo class structure from source-backed class relationships." data-evidence="${esc(sources.join("; "))}" xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Geo Class Structure">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  ${markerDefs()}
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}
    .frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:58px;fill:#0F172A}
    .subtitle{font-family:"Comic Mono";font-size:17px;fill:#475569}
    .column{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5}
    .columnTitle{font-family:"Architects Daughter";font-size:30px;fill:#0F172A;paint-order:stroke;stroke:#FFFFFF;stroke-width:5px;stroke-linejoin:round}
    .columnNote{font-family:"Comic Mono";font-size:15px;fill:#64748B}
    .classBox{filter:url(#shadow);stroke-width:2}
    .kind{font-family:"Comic Mono";font-size:14px;fill:#475569;font-weight:700}
    .classTitle{font-family:"Architects Daughter";font-size:28px;fill:#0F172A}
    .member{font-family:"Comic Mono";font-size:15px;fill:#334155}
    .divider{stroke-width:1;opacity:.38}
    .edge{fill:none;stroke-width:3.6;stroke-linecap:round;stroke-linejoin:round}
    .dashed{stroke-dasharray:10 10}
    .edgeLabelBg{fill:#F8FAFC;stroke:#CBD5E1;stroke-width:1;opacity:.94}
    .edgeLabel{font-family:"Comic Mono";font-size:13px;fill:#334155;font-weight:700}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="36" y="30" width="${width - 72}" height="${height - 60}" rx="8"/>
<text class="title" x="76" y="100">Geo Class Structure</text>
<text class="subtitle" x="80" y="134">Current source relationships: inheritance, implementation, dependency, and contained value objects. No icons in this class diagram.</text>
${columns.join("\n")}
${edges.join("\n")}
${boxes.join("\n")}
</svg>`;

const svgPath = join(OUT, "utils-geo-diagram-02.svg");
const pngPath = join(OUT, "utils-geo-diagram-02.png");
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--output-width", String(width * 2), "--output-height", String(height * 2)], { stdio: "inherit" });
console.log("Generated utils-geo-diagram-02.svg/png");
