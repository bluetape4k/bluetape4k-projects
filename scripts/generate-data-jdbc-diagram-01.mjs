#!/usr/bin/env node
import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";

const out = "docs/images/readme-diagrams/data-jdbc-diagram-01";
const W = 1680;
const H = 860;
const intent = "Explain how SQL intent and JDBC receivers enter bluetape4k helper paths for transactions, prepared execution, row mapping, and update counts.";
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
    pink: "#DB2777",
    purple: "#9333EA",
    gray: "#64748B",
    lime: "#65A30D",
};

const sources = [
    "data/jdbc/README.md",
    "data/jdbc/README.ko.md",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceExtensions.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensions.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementExtensions.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetExtensions.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensions.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensions.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/TransactionExtensions.kt",
];
for (const source of sources) {
    if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
}
if (!/Extension Function API Overview[\s\S]*data-jdbc-diagram-01\.png/.test(readFileSync(sources[0], "utf8"))) {
    throw new Error("README diagram slot not found");
}

const layers = [
    {
        x: 70,
        y: 165,
        w: 430,
        h: 620,
        title: "Caller contract",
        note: "SQL intent and receiver decide which helper path runs."
    },
    {
        x: 555,
        y: 165,
        w: 560,
        h: 620,
        title: "JDBC helper path",
        note: "Extensions keep resource scope, binding, batch, and transaction code short."
    },
    {
        x: 1170,
        y: 165,
        w: 440,
        h: 620,
        title: "Returned value",
        note: "Queries map rows; updates and batches return affected counts."
    },
];

const cards = {
    sql: {
        x: 125,
        y: 300,
        w: 320,
        h: 112,
        fill: "#EFF6FF",
        stroke: c.blue,
        title: "SQL + parameters",
        body: ["query/update text", {code: "params | batch rows"}]
    },
    receiver: {
        x: 125,
        y: 585,
        w: 320,
        h: 112,
        fill: "#F0FDF4",
        stroke: c.green,
        title: "DataSource / Connection",
        body: [{code: "withConnect { conn }"}, "delegates to Connection APIs"]
    },
    tx: {
        x: 625,
        y: 280,
        w: 420,
        h: 112,
        fill: "#FAF5FF",
        stroke: c.purple,
        title: "Transaction scope",
        body: [{code: "autoCommit = false"}, "commit or rollback + restore"]
    },
    prepared: {
        x: 625,
        y: 455,
        w: 420,
        h: 122,
        fill: "#F0FDFA",
        stroke: c.teal,
        title: "Prepared execution",
        body: [{code: "prepareStatement(sql)"}, "bind params, execute query/update"]
    },
    batch: {
        x: 625,
        y: 640,
        w: 420,
        h: 112,
        fill: "#FFF7ED",
        stroke: c.orange,
        title: "Batch helpers",
        body: [{code: "addBatch()"}, {code: "IntArray | LongArray"}]
    },
    result: {
        x: 1225,
        y: 272,
        w: 330,
        h: 112,
        fill: "#FDF2F8",
        stroke: c.pink,
        title: "ResultSet access",
        body: [{code: "getXxxOrNull()"}, "label/index operators"]
    },
    mapper: {
        x: 1225,
        y: 452,
        w: 330,
        h: 112,
        fill: "#F7FEE7",
        stroke: c.lime,
        title: "Row mapping",
        body: [{code: "mapFirst / mapSingle"}, {code: "toList / toMap / sequence"}]
    },
    counts: {
        x: 1225,
        y: 612,
        w: 330,
        h: 112,
        fill: "#F9FAFB",
        stroke: c.gray,
        title: "Update counts",
        body: ["affected rows", "generated keys optional"]
    },
};

const flows = [
    [c.blue, "M445 356 L543 356 Q555 356 555 368 L555 488 Q555 500 567 500 L625 500", "SQL", 510, 337],
    [c.green, "M445 641 L523 641 Q535 641 535 629 L535 552 Q535 540 547 540 L625 540", "receiver", 490, 622],
    [c.purple, "M835 392 L835 455", "transaction boundary", 945, 425, true],
    [c.teal, "M1045 516 L1128 516 Q1140 516 1140 504 L1140 340 Q1140 328 1152 328 L1225 328", "query", 1090, 478],
    [c.pink, "M1390 384 L1390 452", "typed rows", 1470, 417],
    [c.orange, "M1045 696 L1132 696 Q1140 696 1140 688 L1140 676 Q1140 668 1148 668 L1225 668", "update", 1090, 645],
];

function esc(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function marker(color) {
    const id = `arrow-${color.replace("#", "")}`;
    return `<marker id="${id}" markerWidth="15" markerHeight="14" refX="13" refY="7" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 14 7 L 1 13 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`;
}

function codeParts(text) {
    const tokens = text.split(/(\s+|[{}()=|./,]+)/).filter(Boolean);
    return tokens.map((token, index) => {
        const next = tokens.slice(index + 1).find((part) => !/^\s+$/.test(part)) ?? "";
        let color = "#0F172A";
        if (/^(false|true|null)$/.test(token)) color = "#C2410C";
        else if (/^(vararg|val|fun|return)$/.test(token)) color = "#9333EA";
        else if (/^(IntArray|LongArray|Connection|ResultSet|Statement|PreparedStatement)$/.test(token)) color = "#7C3AED";
        else if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(token) && next.startsWith("(")) color = "#2563EB";
        else if (/^[{}()=|./,]+$/.test(token)) color = "#64748B";
        return {token, color};
    });
}

function codeLine(text, centerX, y) {
    const parts = codeParts(text);
    const tokenWidth = (token) => {
        if (/^\s+$/.test(token)) return token.length * 3.8;
        if (/^[{}()=|./,]+$/.test(token)) return token.length * 5.4;
        return token.length * 6.95;
    };
    const width = parts.reduce((sum, part) => sum + tokenWidth(part.token), 0);
    let x = centerX - width / 2;
    return `<g>${parts.map((part) => {
        const item = `<text class="code" x="${x}" y="${y}"><tspan class="syntax-token" fill="${part.color}">${esc(part.token)}</tspan></text>`;
        x += tokenWidth(part.token);
        return item;
    }).join("")}</g>`;
}

function card(key) {
    const b = cards[key];
    return `<g><rect class="card" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/><text class="cardTitle" x="${b.x + b.w / 2}" y="${b.y + 42}" text-anchor="middle">${esc(b.title)}</text>${b.body.map((line, index) => {
        const y = b.y + 76 + index * 23;
        if (typeof line === "object") {
            const text = line.code;
            return codeLine(text, b.x + b.w / 2, y);
        }
        return `<text class="detail" x="${b.x + b.w / 2}" y="${y}" text-anchor="middle">${esc(line)}</text>`;
    }).join("")}</g>`;
}

function label(text, x, y) {
    const w = Math.max(82, text.length * 8 + 22);
    return `<g transform="translate(${x - w / 2} ${y - 14})"><rect width="${w}" height="27" rx="8" fill="#fff" stroke="${c.line}" opacity=".96"/><text class="edgeLabel" x="${w / 2}" y="18" text-anchor="middle">${esc(text)}</text></g>`;
}

const defs = [...new Set(flows.map((flow) => flow[0]))].map(marker).join("");
const sourceEvidence = sources.join("; ");
const svg = `<svg data-intent="${esc(intent)}" data-evidence="${esc(sourceEvidence)}" data-source-read="${esc(sourceEvidence)}" xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="JDBC Extension API Overview">
<defs><filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>${defs}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.6;filter:url(#shadow)}.title{font-family:"Architects Daughter";font-size:44px;fill:${c.ink}}.subtitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}.layer{fill:#F8FAFC;stroke:${c.line};stroke-width:1.5}.layerBadge{fill:#FFFFFF;stroke:${c.line};stroke-width:1.3}.layerTitle{font-family:"Architects Daughter";font-size:24px;fill:${c.ink}}.layerNote{font-family:"Comic Mono";font-size:12.5px;fill:#64748B}.card{filter:url(#shadow);stroke-width:1.9}.cardTitle{font-family:"Architects Daughter";font-size:25px;fill:${c.ink}}.detail{font-family:"Comic Mono";font-size:13px;fill:${c.muted}}.code{font-family:"Comic Mono";font-size:12.6px;font-weight:700}.syntax-token{fill:${c.ink}}.edge{fill:none;stroke-width:3.6;stroke-linecap:round;stroke-linejoin:round}.dashed{stroke-dasharray:8 7}.edgeLabel{font-family:"Comic Mono";font-size:12.2px;fill:${c.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="10"/>
<text class="title" x="72" y="84">JDBC Extension API Overview</text>
<text class="subtitle" x="76" y="116">A caller path from SQL and JDBC receivers to prepared execution, row mapping, and update counts.</text>
${layers.map((layer) => `<g><rect class="layer" x="${layer.x}" y="${layer.y}" width="${layer.w}" height="${layer.h}" rx="10"/><rect class="layerBadge" x="${layer.x + 28}" y="${layer.y + 24}" width="${Math.min(layer.w - 56, layer.title.length * 14 + 58)}" height="36" rx="8"/><text class="layerTitle" x="${layer.x + 48}" y="${layer.y + 50}">${esc(layer.title)}</text><text class="layerNote" x="${layer.x + 28}" y="${layer.y + 95}">${esc(layer.note)}</text></g>`).join("")}
<g>${flows.map((flow) => `<path class="edge${flow[5] ? " dashed" : ""}" d="${flow[1]}" stroke="${flow[0]}" marker-end="url(#arrow-${flow[0].replace("#", "")})"/>`).join("")}</g>
<g>${flows.map((flow) => label(flow[2], flow[3], flow[4])).join("")}</g>
${Object.keys(cards).map(card).join("")}
</svg>`;

writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
