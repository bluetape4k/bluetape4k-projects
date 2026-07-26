#!/usr/bin/env node
import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";

const out = "docs/images/readme-diagrams/data-r2dbc-diagram-02";
const W = 1880;
const H = 1080;
const c = {
    ink: "#0F172A",
    muted: "#475569",
    canvas: "#F8FAFC",
    frame: "#FFFFFF",
    line: "#CBD5E1",
    blue: "#2563EB",
    teal: "#0D9488",
    green: "#16A34A",
    orange: "#EA580C",
    purple: "#9333EA",
    pink: "#DB2777",
    red: "#DC2626",
    slate: "#334155",
};

const sources = [
    "data/r2dbc/README.md",
    "data/r2dbc/README.ko.md",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/R2dbcClient.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Execute.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Insert.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Update.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Delete.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/Query.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/Filter.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/QueryBuilder.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderSupport.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/DatabaseClientSupport.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/ParameterSupport.kt",
    "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/ReadableSupport.kt",
];
for (const source of sources) {
    if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
}
if (!/Core API Class Structure[\s\S]*data-r2dbc-diagram-02\.png/.test(readFileSync(sources[0], "utf8"))) {
    throw new Error("README diagram slot not found");
}

const boxes = {
    databaseClient: {
        x: 90, y: 170, w: 315, h: 130, fill: "#EFF6FF", stroke: c.blue,
        stereo: "Spring class", title: "DatabaseClient", members: ["+ sql(...): ExecuteSpec", "+ connectionFactory"],
    },
    r2dbcClient: {
        x: 690, y: 145, w: 430, h: 184, fill: "#F0FDFA", stroke: c.teal,
        stereo: "class", title: "R2dbcClient", members: [
            "+ databaseClient: DatabaseClient",
            "+ entityTemplate: R2dbcEntityTemplate",
            "+ mappingConverter: MappingR2dbcConverter",
        ],
    },
    entityTemplate: {
        x: 1420, y: 126, w: 360, h: 112, fill: "#F0FDF4", stroke: c.green,
        stereo: "Spring class", title: "R2dbcEntityTemplate", members: ["+ insert/update/delete domain operations"],
    },
    mappingConverter: {
        x: 1420, y: 295, w: 360, h: 112, fill: "#FAF5FF", stroke: c.purple,
        stereo: "Spring class", title: "MappingR2dbcConverter", members: ["+ read(domainType, row, metadata)"],
    },
    queryBuilder: {
        x: 90,
        y: 480,
        w: 345,
        h: 178,
        fill: "#FFF7ED",
        stroke: c.orange,
        stereo: "class",
        title: "QueryBuilder",
        members: ["- params: MutableMap", "- filters: Filter.Group", "+ build(): Query", "+ buildCount(): Query"],
    },
    query: {
        x: 90,
        y: 770,
        w: 345,
        h: 190,
        fill: "#FFFBEB",
        stroke: c.orange,
        stereo: "data class",
        title: "Query",
        members: ["+ sqlBuffer: StringBuilder", "+ parameters: Map", "+ sql: String", "+ used by execute/matching"],
    },
    filter: {
        x: 555, y: 480, w: 310, h: 120, fill: "#FDF2F8", stroke: c.pink,
        stereo: "sealed class", title: "Filter", members: ["+ countLeaves(): Int"],
    },
    filterGroup: {
        x: 500, y: 765, w: 260, h: 142, fill: "#FDF2F8", stroke: c.pink,
        stereo: "data class", title: "Filter.Group", members: ["+ operator: String", "+ filters: MutableList"],
    },
    filterWhere: {
        x: 780, y: 765, w: 260, h: 142, fill: "#FDF2F8", stroke: c.pink,
        stereo: "data class", title: "Filter.Where", members: ["+ where: String"],
    },
    bindApi: {
        x: 1060,
        y: 480,
        w: 335,
        h: 166,
        fill: "#FAF5FF",
        stroke: c.purple,
        stereo: "interface",
        title: "BindSpec<T>",
        members: ["+ bind(index/name, value)", "+ bindNull(...)", "+ fetch(): RowsFetchSpec"],
    },
    bindImpl: {
        x: 1060,
        y: 765,
        w: 335,
        h: 160,
        fill: "#FAF5FF",
        stroke: c.purple,
        stereo: "internal class",
        title: "BindSpecImpl<T>",
        members: ["- namedParameters", "- indexedParameters", "+ maps rows through converter"],
    },
    rowsFetch: {
        x: 1435, y: 785, w: 300, h: 134, fill: "#EFF6FF", stroke: c.blue,
        stereo: "Spring interface", title: "RowsFetchSpec<T>", members: ["+ all() / one()", "+ await and Flow helpers"],
    },
    crudApi: {
        x: 1445, y: 485, w: 380, h: 210, fill: "#FFF7ED", stroke: c.orange,
        stereo: "interfaces", title: "CRUD Spec Families", members: [
            "InsertIntoSpec -> Values / Key",
            "UpdateTableSpec -> Values",
            "SetterSpec <- UpdateValuesSpec",
            "DeleteTableSpec -> Value",
            "impl classes build SQL specs",
        ],
    },
};

const edges = [
    {cls: "assoc", d: "M 690 237 H 405", color: c.blue, marker: "openBlue", label: ["databaseClient", 545, 225]},
    {cls: "assoc", d: "M 1120 195 H 1420", color: c.green, marker: "openGreen", label: ["entityTemplate", 1270, 183]},
    {
        cls: "assoc",
        d: "M 1120 285 H 1250 V 351 H 1420",
        color: c.purple,
        marker: "openPurple",
        label: ["mappingConverter", 1268, 339]
    },
    {cls: "uses", d: "M 435 568 H 555", color: c.pink, marker: "openPink", label: ["filters", 495, 556]},
    {cls: "uses", d: "M 265 658 V 770", color: c.orange, marker: "openOrange", label: ["builds", 302, 718]},
    {cls: "extends", d: "M 630 765 V 600", color: c.pink, marker: "hollowPink", label: null},
    {cls: "extends", d: "M 910 765 V 682 H 710 V 600", color: c.pink, marker: "hollowPink", label: null},
    {cls: "realize", d: "M 1228 765 V 646", color: c.purple, marker: "hollowPurple", label: null},
    {cls: "uses", d: "M 1395 842 H 1435", color: c.blue, marker: "openBlue", label: ["fetch()", 1415, 830]},
];

function esc(value) {
    return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function box(id) {
    const b = boxes[id];
    const dividerY = b.y + 76;
    const memberStart = dividerY + 27;
    return `<g id="${id}">
  <rect class="card" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/>
  <text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 28}" text-anchor="middle">${esc(b.stereo)}</text>
  <text class="cardTitle" x="${b.x + b.w / 2}" y="${b.y + 58}" text-anchor="middle">${esc(b.title)}</text>
  <line class="divider" x1="${b.x + 24}" y1="${dividerY}" x2="${b.x + b.w - 24}" y2="${dividerY}"/>
  ${b.members.map((member, index) => `<text class="member" x="${b.x + 28}" y="${memberStart + index * 22}">${esc(member)}</text>`).join("")}
</g>`;
}

function label(item) {
    if (!item) return "";
    const [text, x, y] = item;
    return `<text class="label" x="${x}" y="${y}" text-anchor="middle">${esc(text)}</text>`;
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="R2DBC core API class structure">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="118%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity=".11"/></filter>
  <marker id="openBlue" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.blue}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openGreen" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.green}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openOrange" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.orange}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openPurple" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.purple}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="openPink" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 10 6 L 2 10" fill="none" stroke="${c.pink}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>
  <marker id="hollowPink" viewBox="0 0 18 16" markerWidth="16" markerHeight="14" refX="16" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 16 8 L 1 15 Z" fill="${c.frame}" stroke="${c.pink}" stroke-width="2" stroke-dasharray="none"/></marker>
  <marker id="hollowPurple" viewBox="0 0 18 16" markerWidth="16" markerHeight="14" refX="16" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 16 8 L 1 15 Z" fill="${c.frame}" stroke="${c.purple}" stroke-width="2" stroke-dasharray="none"/></marker>
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.6;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:${c.ink}}.subtitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}
    .card{filter:url(#shadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}
    .stereo{font-family:"Comic Mono";font-size:12px;fill:#64748B}.member{font-family:"Comic Mono";font-size:12.5px;fill:${c.slate}}
    .divider{stroke:rgba(15,23,42,.17);stroke-width:1.2}.assoc{fill:none;stroke-width:3.0;stroke-linecap:round;stroke-linejoin:round}
    .uses{fill:none;stroke-width:2.5;stroke-dasharray:8 7;stroke-linecap:round;stroke-linejoin:round}
    .realize{fill:none;stroke-width:2.4;stroke-dasharray:8 7;stroke-linecap:round;stroke-linejoin:round}
    .extends{fill:none;stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round}
    .label{font-family:"Comic Mono";font-size:11.6px;fill:${c.slate}}
    .legend{font-family:"Comic Mono";font-size:12px;fill:${c.muted}}
  </style>
</defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="10"/>
<text class="title" x="72" y="84">R2DBC Core API Class Structure</text>
<text class="subtitle" x="76" y="116">UML view of the holder, query model, typed execution spec, and CRUD spec families.</text>
<g>
${edges.map((e) => `  <path class="${e.cls}" d="${e.d}" stroke="${e.color}" marker-end="url(#${e.marker})"/>`).join("\n")}
</g>
<g>${edges.map((e) => label(e.label)).join("")}</g>
${Object.keys(boxes).map(box).join("")}
<g transform="translate(90 990)">
  <path class="assoc" d="M 0 0 H 58" stroke="${c.blue}" marker-end="url(#openBlue)"/><text class="legend" x="72" y="5">has/reference</text>
  <path class="uses" d="M 214 0 H 272" stroke="${c.purple}" marker-end="url(#openPurple)"/><text class="legend" x="286" y="5">uses / builds</text>
  <path class="realize" d="M 455 0 H 513" stroke="${c.purple}" marker-end="url(#hollowPurple)"/><text class="legend" x="528" y="5">implements</text>
  <path class="extends" d="M 675 0 H 733" stroke="${c.pink}" marker-end="url(#hollowPink)"/><text class="legend" x="748" y="5">extends</text>
</g>
</svg>`;

writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
