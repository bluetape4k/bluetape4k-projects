#!/usr/bin/env node
import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";

const out = "docs/images/readme-diagrams/data-jdbc-diagram-02";
const W = 1540;
const H = 1020;
const c = {
    ink: "#0F172A",
    muted: "#475569",
    canvas: "#F8FAFC",
    frame: "#FFFFFF",
    line: "#CBD5E1",
    blue: "#2563EB",
    green: "#16A34A",
    orange: "#EA580C",
    purple: "#9333EA",
};

const sources = [
    "data/jdbc/README.md",
    "data/jdbc/README.ko.md",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceExtensions.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensions.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementExtensions.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PrepareStatementSupport.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetExtensions.kt",
    "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensions.kt",
];
for (const source of sources) {
    if (!existsSync(source)) throw new Error(`Missing source evidence: ${source}`);
}
if (!/Core API Structure[\s\S]*data-jdbc-diagram-02\.png/.test(readFileSync(sources[0], "utf8"))) {
    throw new Error("README diagram slot not found");
}

const top = [
    {
        key: "ds",
        x: 110,
        y: 235,
        w: 295,
        h: 92,
        fill: "#EFF6FF",
        stroke: c.blue,
        title: "DataSource",
        sub: "connection provider"
    },
    {
        key: "conn",
        x: 435,
        y: 235,
        w: 295,
        h: 92,
        fill: "#F0FDF4",
        stroke: c.green,
        title: "Connection",
        sub: "statement + transaction scope"
    },
    {
        key: "stmt",
        x: 760,
        y: 235,
        w: 295,
        h: 92,
        fill: "#FFF7ED",
        stroke: c.orange,
        title: "PreparedStatement",
        sub: "parameter binding + batch"
    },
    {
        key: "rs",
        x: 1085,
        y: 235,
        w: 295,
        h: 92,
        fill: "#FAF5FF",
        stroke: c.purple,
        title: "ResultSet",
        sub: "typed reads + row mapping"
    },
];

const groups = [
    {
        x: 110, y: 500, w: 295, h: 230, fill: "#EFF6FF", stroke: c.blue, title: "DataSource extensions",
        lines: ["withConnect { Connection }", "withStatement { Statement }", "runQuery(sql) { ResultSet }", "executeUpdate / executeInsert", "executeBatch(sql...)"],
    },
    {
        x: 435, y: 500, w: 295, h: 230, fill: "#F0FDF4", stroke: c.green, title: "Connection extensions",
        lines: ["withStatement { Statement }", "runQuery(sql) { ResultSet }", "executeUpdate / generated keys", "preparedStatement(...) DSL", "batch and large batch helpers"],
    },
    {
        x: 760, y: 500, w: 295, h: 230, fill: "#FFF7ED", stroke: c.orange, title: "Prepared APIs",
        lines: ["executeQuery(sql, params)", "executeUpdate(sql, params)", "executeUpdateWithGeneratedKeys", "arguments { string[1] = ... }", "ArgumentSetter wrappers"],
    },
    {
        x: 1085, y: 500, w: 295, h: 230, fill: "#FAF5FF", stroke: c.purple, title: "ResultSet APIs",
        lines: ["getXxxOrNull(label/index)", "mapFirst / mapSingle", "toList / toSet / toMap", "sequence / forEach / filterMap", "singleInt / metadata helpers"],
    },
];

function esc(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function marker(id, color) {
    return `<marker id="${id}" markerWidth="14" markerHeight="14" refX="12" refY="7" orient="auto" markerUnits="userSpaceOnUse"><path d="M 1 1 L 13 7 L 1 13 Z" fill="${color}" stroke="${color}" stroke-dasharray="none"/></marker>`;
}

function topNode(n) {
    return `<g><rect class="card node" x="${n.x}" y="${n.y}" width="${n.w}" height="${n.h}" rx="8" fill="${n.fill}" stroke="${n.stroke}"/><text class="nodeTitle" x="${n.x + n.w / 2}" y="${n.y + 38}" text-anchor="middle">${esc(n.title)}</text><text class="nodeSub" x="${n.x + n.w / 2}" y="${n.y + 68}" text-anchor="middle">${esc(n.sub)}</text></g>`;
}

function codeParts(text) {
    const tokens = text.split(/(\s+|[{}()=|./,\[\]]+)/).filter(Boolean);
    return tokens.map((token, index) => {
        const next = tokens.slice(index + 1).find((part) => !/^\s+$/.test(part)) ?? "";
        let color = "#0F172A";
        if (/^(false|true|null)$/.test(token)) color = "#C2410C";
        else if (/^(vararg|val|fun|return)$/.test(token)) color = "#9333EA";
        else if (/^(Connection|Statement|ResultSet|PreparedStatement|Int|Long|String)$/.test(token)) color = "#7C3AED";
        else if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(token) && next.startsWith("(")) color = "#2563EB";
        else if (/^[{}()=|./,\[\]]+$/.test(token)) color = "#64748B";
        return {token, color};
    });
}

function codeLine(text, centerX, y) {
    const parts = codeParts(text);
    const tokenWidth = (token) => {
        if (/^\s+$/.test(token)) return token.length * 3.8;
        if (/^[{}()=|./,\[\]]+$/.test(token)) return token.length * 5.25;
        return token.length * 6.75;
    };
    const width = parts.reduce((sum, part) => sum + tokenWidth(part.token), 0);
    let x = centerX - width / 2;
    return `<g>${parts.map((part) => {
        const item = `<text class="code" x="${x}" y="${y}"><tspan class="syntax-token" fill="${part.color}">${esc(part.token)}</tspan></text>`;
        x += tokenWidth(part.token);
        return item;
    }).join("")}</g>`;
}

function groupCard(g) {
    return `<g><rect class="card node" x="${g.x}" y="${g.y}" width="${g.w}" height="${g.h}" rx="8" fill="${g.fill}" stroke="${g.stroke}"/><text class="cardTitle" x="${g.x + g.w / 2}" y="${g.y + 38}" text-anchor="middle">${esc(g.title)}</text><line x1="${g.x}" y1="${g.y + 58}" x2="${g.x + g.w}" y2="${g.y + 58}" stroke="${g.stroke}" stroke-opacity=".35"/>${g.lines.map((line, i) => {
        const y = g.y + 88 + i * 27;
        return codeLine(line, g.x + g.w / 2, y);
    }).join("")}</g>`;
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="bluetape4k JDBC core API structure">
<defs><filter id="shadow" x="-8%" y="-8%" width="116%" height="120%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".11"/></filter>${marker("arrowBlue", c.blue)}${marker("arrowGreen", c.green)}<style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.6;filter:url(#shadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}.subtitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}.lane{fill:#F8FAFC;stroke:${c.line};stroke-width:1.5}.laneTitle{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}.node{filter:url(#shadow);stroke-width:1.9}.nodeTitle{font-family:"Architects Daughter";font-size:25px;fill:${c.ink}}.nodeSub{font-family:"Comic Mono";font-size:12.5px;fill:#64748B}.cardTitle{font-family:"Architects Daughter";font-size:21px;fill:${c.ink}}.code{font-family:"Comic Mono";font-size:12px;font-weight:700}.syntax-token{fill:${c.ink}}.flow{fill:none;stroke:${c.blue};stroke-width:4;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowBlue)}.support{fill:none;stroke:${c.green};stroke-width:3.2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}.small{font-family:"Comic Mono";font-size:12.2px;fill:${c.muted}}</style></defs>
<rect class="canvas" width="${W}" height="${H}"/>
<rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="10"/>
<text class="title" x="72" y="86">bluetape4k-jdbc Core API Structure</text>
<text class="subtitle" x="76" y="118">Kotlin extension groups follow JDBC receivers and keep JDBC exceptions visible.</text>
<rect class="layer lane" x="82" y="160" width="${W - 164}" height="225" rx="10"/>
<text class="laneTitle" x="112" y="201">JDBC object path wrapped by extension APIs</text>
${top.map(topNode).join("")}
<path class="flow" d="M405 281 L435 281"/><path class="flow" d="M730 281 L760 281"/><path class="flow" d="M1055 281 L1085 281"/>
<rect class="layer lane" x="82" y="420" width="${W - 164}" height="385" rx="10"/>
<text class="laneTitle" x="112" y="462">Extension groups by receiver</text>
${groups.map(groupCard).join("")}
<path class="support" d="M258 500 L258 327"/><path class="support" d="M583 500 L583 327"/><path class="support" d="M908 500 L908 327"/><path class="support" d="M1233 500 L1233 327"/>
<rect class="layer lane" x="82" y="840" width="${W - 164}" height="86" rx="10"/>
<text class="laneTitle" x="112" y="886">Behavior contracts</text>
<text class="small" x="350" y="866">Resource scope: Connection, Statement, PreparedStatement, and ResultSet are closed with use {}</text>
<text class="small" x="350" y="890">Transaction scope: commit on success, rollback on failure, then restore autoCommit/isolation/readOnly</text>
<text class="small" x="350" y="914">Type-safe DSLs: setter/getter tokens wrap JDBC label/index overloads without hiding driver exceptions</text>
</svg>`;

writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
