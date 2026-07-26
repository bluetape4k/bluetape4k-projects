#!/usr/bin/env node
import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";

const out = "docs/images/readme-diagrams/data-mongodb-diagram-01";
const W = 1800;
const H = 1160;
const c = {
    ink: "#0F172A",
    muted: "#475569",
    canvas: "#F8FAFC",
    frame: "#FFFFFF",
    line: "#CBD5E1",
    blue: "#2563EB",
    green: "#16A34A",
    teal: "#0D9488",
    orange: "#EA580C",
    purple: "#9333EA",
    pink: "#DB2777",
    gray: "#64748B",
};

const sources = [
    "data/mongodb/README.md",
    "data/mongodb/README.ko.md",
    "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientSupport.kt",
    "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientProvider.kt",
    "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientExtensions.kt",
    "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensions.kt",
    "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensions.kt",
    "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensions.kt",
    "data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupport.kt",
];
for (const source of sources) {
    if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
}
if (!/Core Class Structure[\s\S]*data-mongodb-diagram-01\.png/.test(readFileSync(sources[0], "utf8"))) {
    throw new Error("README diagram slot not found");
}

const layers = [
    {
        x: 70,
        y: 165,
        w: 1660,
        h: 350,
        title: "Native coroutine driver types",
        note: "Runtime still belongs to MongoDB Kotlin Coroutine Driver: suspend CRUD and Flow-returning query APIs are not wrapped."
    },
    {
        x: 70,
        y: 555,
        w: 1660,
        h: 420,
        title: "bluetape4k additions",
        note: "Factories, cached clients, focused extensions, BSON helpers, and ordered pipeline stage builders."
    },
    {
        x: 70,
        y: 1010,
        w: 1660,
        h: 96,
        title: "Design contract",
        note: "Add only missing convenience. Keep native aggregate(), collection types, driver exceptions, and coroutine Flow behavior visible."
    },
];

const cards = {
    settings: {
        x: 120,
        y: 300,
        w: 330,
        h: 146,
        fill: "#EFF6FF",
        stroke: c.blue,
        title: "MongoClientSettings",
        tag: "<<driver>>",
        body: ["builder configures sockets", "connection string, codecs"]
    },
    client: {
        x: 535,
        y: 300,
        w: 330,
        h: 146,
        fill: "#F0FDF4",
        stroke: c.green,
        title: "MongoClient",
        tag: "<<coroutine driver>>",
        body: [{code: "listDatabaseNames()"}, {code: "startSession()"}]
    },
    database: {
        x: 950,
        y: 300,
        w: 330,
        h: 146,
        fill: "#F0FDFA",
        stroke: c.teal,
        title: "MongoDatabase",
        tag: "<<coroutine driver>>",
        body: [{code: "getCollection(name, Class)"}, {code: "listCollectionNames()"}]
    },
    collection: {
        x: 1365,
        y: 300,
        w: 330,
        h: 146,
        fill: "#FAF5FF",
        stroke: c.purple,
        title: "MongoCollection<T>",
        tag: "<<coroutine driver>>",
        body: ["native suspend CRUD", {code: "find(), aggregate()"}]
    },
    provider: {
        x: 120,
        y: 750,
        w: 340,
        h: 172,
        fill: "#EFF6FF",
        stroke: c.blue,
        title: "MongoClientProvider",
        tag: "<<object>>",
        body: [{code: "getOrCreate(url)"}, {code: "getOrCreate(settings)"}, "ConcurrentHashMap caches"]
    },
    factory: {
        x: 510,
        y: 750,
        w: 340,
        h: 172,
        fill: "#F0FDF4",
        stroke: c.green,
        title: "Client factory APIs",
        tag: "<<top-level functions>>",
        body: [{code: "mongoClient { ... }"}, {code: "mongoClientOf(url)"}, "delegates to MongoClient.create"]
    },
    extensions: {
        x: 900,
        y: 750,
        w: 360,
        h: 172,
        fill: "#F0FDFA",
        stroke: c.teal,
        title: "Mongo extension APIs",
        tag: "<<extension functions>>",
        body: [{code: "getCollectionOf<T>(name)"}, {code: "findFirst / exists / upsert"}, {code: "findAsFlow(filter, sort)"}]
    },
    dsl: {
        x: 1310,
        y: 750,
        w: 370,
        h: 172,
        fill: "#FFF7ED",
        stroke: c.orange,
        title: "BSON + pipeline DSL",
        tag: "<<builders>>",
        body: [{code: "documentOf { ... }"}, {code: "getAs<T>(key)"}, {code: "pipeline { add(stage) }"}]
    },
};

const flows = [
    {color: c.blue, d: "M450 373 L535 373", label: "create", x: 492, y: 353, dash: true, open: true},
    {color: c.green, d: "M865 373 L950 373", label: "database", x: 908, y: 353, dash: true, open: true},
    {color: c.teal, d: "M1280 373 L1365 373", label: "collection", x: 1322, y: 353, dash: true, open: true},
    {color: c.blue, d: "M290 750 L290 682 L660 682 L660 446", label: "caches", x: 468, y: 662, dash: true, open: true},
    {color: c.green, d: "M680 750 L680 446", label: "creates", x: 718, y: 610, dash: true, open: true},
    {color: c.teal, d: "M1080 750 L1080 446", label: "extends", x: 1118, y: 610, dash: true, open: true},
    {
        color: c.orange,
        d: "M1495 750 L1495 682 L1530 682 L1530 446",
        label: "feeds aggregate",
        x: 1518,
        y: 662,
        dash: true,
        open: true
    },
];

function esc(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function marker(color, open = false) {
    const id = `${open ? "open" : "arrow"}-${color.replace("#", "")}`;
    if (open) {
        return `<marker id="${id}" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 2 L 12 7 L 2 12" fill="none" stroke="${color}" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="none"/></marker>`;
    }
    return `<marker id="${id}" markerWidth="15" markerHeight="14" refX="13" refY="7" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 14 7 L 1 13 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`;
}

function codeParts(text) {
    const tokens = text.split(/(\s+|[{}()=|./,<>\[\]]+)/).filter(Boolean);
    return tokens.map((token, index) => {
        const next = tokens.slice(index + 1).find((part) => !/^\s+$/.test(part)) ?? "";
        let color = "#0F172A";
        if (/^(false|true|null)$/.test(token)) color = "#C2410C";
        else if (/^(val|fun|return|suspend|inline|object)$/.test(token)) color = "#9333EA";
        else if (/^(MongoClient|MongoDatabase|MongoCollection|Document|Bson|Class|Flow|String)$/.test(token)) color = "#7C3AED";
        else if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(token) && next.startsWith("(")) color = "#2563EB";
        else if (/^[{}()=|./,<>\[\]]+$/.test(token)) color = "#64748B";
        return {token, color};
    });
}

function codeLine(text, centerX, y) {
    const parts = codeParts(text);
    const tokenWidth = (token) => {
        if (/^\s+$/.test(token)) return token.length * 3.9;
        if (/^[{}()=|./,<>\[\]]+$/.test(token)) return token.length * 5.3;
        return token.length * 6.85;
    };
    const width = parts.reduce((sum, part) => sum + tokenWidth(part.token), 0);
    let x = centerX - width / 2;
    return `<g>${parts.map((part) => {
        const item = `<text class="code" x="${x}" y="${y}" fill="${part.color}">${esc(part.token)}</text>`;
        x += tokenWidth(part.token);
        return item;
    }).join("")}</g>`;
}

function card(key) {
    const b = cards[key];
    return `<g><rect class="card" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/><text class="tag" x="${b.x + b.w / 2}" y="${b.y + 32}" text-anchor="middle">${esc(b.tag)}</text><text class="cardTitle" x="${b.x + b.w / 2}" y="${b.y + 66}" text-anchor="middle">${esc(b.title)}</text><line x1="${b.x}" y1="${b.y + 84}" x2="${b.x + b.w}" y2="${b.y + 84}" stroke="${b.stroke}" stroke-opacity=".32"/>${b.body.map((line, index) => {
        const y = b.y + 110 + index * 25;
        if (typeof line === "object") return codeLine(line.code, b.x + b.w / 2, y);
        return `<text class="detail" x="${b.x + b.w / 2}" y="${y}" text-anchor="middle">${esc(line)}</text>`;
    }).join("")}</g>`;
}

function label(text, x, y) {
    const w = Math.max(78, text.length * 8 + 22);
    return `<g transform="translate(${x - w / 2} ${y - 14})"><rect width="${w}" height="27" rx="8" fill="#fff" stroke="${c.line}" opacity=".97"/><text class="edgeLabel" x="${w / 2}" y="18" text-anchor="middle">${esc(text)}</text></g>`;
}

const markerDefs = [...new Map(flows.flatMap((flow) => [
    [`${flow.color}-arrow`, marker(flow.color, false)],
    [`${flow.color}-open`, marker(flow.color, true)],
])).values()].join("");

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="bluetape4k MongoDB core class structure">
<defs><filter id="shadow" x="-8%" y="-8%" width="116%" height="118%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity=".11"/></filter>${markerDefs}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.6;filter:url(#shadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${c.ink}}.subtitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}.layer{fill:#F8FAFC;stroke:${c.line};stroke-width:1.5}.layerTitle{font-family:"Architects Daughter";font-size:24px;fill:${c.ink}}.layerNote{font-family:"Comic Mono";font-size:12.5px;fill:#64748B}.card{filter:url(#shadow);stroke-width:1.9}.tag{font-family:"Comic Mono";font-size:12.3px;font-weight:700;fill:#64748B}.cardTitle{font-family:"Architects Daughter";font-size:25px;fill:${c.ink}}.detail{font-family:"Comic Mono";font-size:13px;fill:${c.muted}}.code{font-family:"Comic Mono";font-size:12.6px;font-weight:700}.edge{fill:none;stroke-width:3.5;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:8 7}.edgeLabel{font-family:"Comic Mono";font-size:12.2px;fill:${c.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="10"/>
<text class="title" x="72" y="84">bluetape4k-mongodb Core Class Structure</text>
<text class="subtitle" x="76" y="116">Thin convenience layer over MongoDB Kotlin Coroutine Driver: no duplicate CRUD wrappers.</text>
${layers.map((layer) => `<g><rect class="layer" x="${layer.x}" y="${layer.y}" width="${layer.w}" height="${layer.h}" rx="10"/><text class="layerTitle" x="${layer.x + 34}" y="${layer.y + 48}">${esc(layer.title)}</text><text class="layerNote" x="${layer.x + 34}" y="${layer.y + 82}">${esc(layer.note)}</text></g>`).join("")}
<g>${flows.map((flow) => `<path class="edge${flow.dash ? " dashed" : ""}" d="${flow.d}" stroke="${flow.color}" marker-end="url(#${flow.open ? "open" : "arrow"}-${flow.color.replace("#", "")})"/>`).join("")}</g>
<g>${flows.map((flow) => label(flow.label, flow.x, flow.y)).join("")}</g>
${Object.keys(cards).map(card).join("")}
<text class="detail" x="335" y="1050">Native driver remains the runtime model.</text>
<text class="detail" x="735" y="1050">Cached clients are registered with ShutdownQueue.</text>
<text class="detail" x="1140" y="1050">Extension APIs compose existing driver calls.</text>
<text class="detail" x="1480" y="1050">Pipeline helpers build List&lt;Bson&gt;.</text>
</svg>`;

writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
