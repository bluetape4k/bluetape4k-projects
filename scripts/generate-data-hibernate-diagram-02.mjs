#!/usr/bin/env node

import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";

const out = "docs/images/readme-diagrams/data-hibernate-diagram-02";
const W = 2340, H = 1085;
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
    purple: "#7C3AED",
    gray: "#64748B"
};
const sources = ["data/hibernate/README.md", "data/hibernate/README.ko.md", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/PersistenceObject.kt", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/AbstractPersistenceObject.kt", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/JpaEntity.kt", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/AbstractJpaEntity.kt", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/IntJpaEntity.kt", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/LongJpaEntity.kt", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/UuidJpaEntity.kt", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/JpaTreeEntity.kt", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/AbstractJpaTreeEntity.kt", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/IntJpaTreeEntity.kt", "data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/model/LongJpaTreeEntity.kt"];
for (const s of sources) if (!existsSync(s)) throw new Error(`Missing source evidence: ${s}`);
if (!/JPA Entity Class Hierarchy[\s\S]*data-hibernate-diagram-02\.png/.test(readFileSync(sources[0], "utf8"))) throw new Error("README diagram slot not found");

const boxes = {
    po: {
        x: 115,
        y: 200,
        w: 410,
        h: 160,
        fill: "#F8FAFC",
        stroke: c.gray,
        st: "<<interface>>",
        title: "PersistenceObject",
        attrs: ["isPersisted: Boolean"],
        methods: ["extends Serializable"]
    },
    apo: {
        x: 115,
        y: 500,
        w: 410,
        h: 170,
        fill: "#F8FAFC",
        stroke: c.gray,
        st: "<<abstract class>>",
        title: "AbstractPersistenceObject",
        attrs: ["isPersisted = false"],
        methods: ["extends AbstractValueObject"]
    },
    jpa: {
        x: 630,
        y: 200,
        w: 410,
        h: 160,
        fill: "#EFF6FF",
        stroke: c.blue,
        st: "<<interface>>",
        title: "JpaEntity<ID>",
        attrs: ["id: ID?", "identifier: ID"],
        methods: ["checkNotNull(id)"]
    },
    tree: {
        x: 1440,
        y: 200,
        w: 520,
        h: 170,
        fill: "#F0FDFA",
        stroke: c.teal,
        st: "<<interface>>",
        title: "JpaTreeEntity<T>",
        attrs: ["parent: T?", "children: MutableSet<T>"],
        methods: ["addChildren(...)", "removeChildren(...)"]
    },
    absJpa: {
        x: 630,
        y: 500,
        w: 410,
        h: 180,
        fill: "#EFF6FF",
        stroke: c.blue,
        st: "<<abstract class>>",
        title: "AbstractJpaEntity<ID>",
        attrs: ["isPersisted = id != null"],
        methods: ["equals uses id when persisted", "transient uses value signature"]
    },
    absTree: {
        x: 1440,
        y: 500,
        w: 520,
        h: 180,
        fill: "#F0FDFA",
        stroke: c.teal,
        st: "<<abstract class>>",
        title: "AbstractJpaTreeEntity<T,ID>",
        attrs: ["parent @ManyToOne", "children @OneToMany"],
        methods: ["no @MappedSuperclass", "QueryDSL limitation note"]
    },
    int: {
        x: 220,
        y: 830,
        w: 340,
        h: 170,
        fill: "#ECFDF5",
        stroke: c.green,
        st: "<<MappedSuperclass>>",
        title: "IntJpaEntity",
        attrs: ["id: Int?", "@GeneratedValue IDENTITY"],
        methods: ["simple integer id base"]
    },
    long: {
        x: 600,
        y: 830,
        w: 340,
        h: 170,
        fill: "#ECFDF5",
        stroke: c.green,
        st: "<<MappedSuperclass>>",
        title: "LongJpaEntity",
        attrs: ["id: Long?", "@GeneratedValue IDENTITY"],
        methods: ["simple long id base"]
    },
    uuid: {
        x: 980,
        y: 830,
        w: 340,
        h: 170,
        fill: "#ECFDF5",
        stroke: c.green,
        st: "<<MappedSuperclass>>",
        title: "UuidJpaEntity",
        attrs: ["id: UUID?", "Uuid.V7.nextId()"],
        methods: ["BINARY(16) column"]
    },
    intTree: {
        x: 1370,
        y: 830,
        w: 420,
        h: 170,
        fill: "#FFF7ED",
        stroke: c.orange,
        st: "<<MappedSuperclass>>",
        title: "IntJpaTreeEntity<T>",
        attrs: ["id: Int?", "redeclares parent/children"],
        methods: ["inherits tree helpers"]
    },
    longTree: {
        x: 1800,
        y: 830,
        w: 420,
        h: 170,
        fill: "#FFF7ED",
        stroke: c.orange,
        st: "<<MappedSuperclass>>",
        title: "LongJpaTreeEntity<T>",
        attrs: ["id: Long?", "redeclares parent/children"],
        methods: ["inherits tree helpers"]
    },
};
const edges = [
    ["implements", c.gray, "M320 500 L320 360"],
    ["extends", c.blue, "M630 590 L525 590"],
    ["implements", c.blue, "M835 500 L835 360"],
    ["extends", c.teal, "M1440 590 L1040 590"],
    ["implements", c.teal, "M1700 500 L1700 370"],
    ["extends", c.blue, "M390 830 L390 760 L740 760 L740 680"],
    ["extends", c.blue, "M770 830 L770 680"],
    ["extends", c.blue, "M1150 830 L1150 760 L930 760 L930 680"],
    ["extends", c.teal, "M1580 830 L1580 755 L1605 755 L1605 680"],
    ["extends", c.teal, "M2010 830 L2010 755 L1795 755 L1795 680"],
];

function esc(v) {
    return String(v).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function box(id) {
    const b = boxes[id], s1 = b.y + 62, s2 = b.y + 112;
    return `<g><rect class="umlBox" x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="8" fill="${b.fill}" stroke="${b.stroke}"/><line x1="${b.x}" y1="${s1}" x2="${b.x + b.w}" y2="${s1}" stroke="${b.stroke}"/><line x1="${b.x}" y1="${s2}" x2="${b.x + b.w}" y2="${s2}" stroke="${b.stroke}"/><text class="stereo" x="${b.x + b.w / 2}" y="${b.y + 23}" text-anchor="middle">${esc(b.st)}</text><text class="classTitle" x="${b.x + b.w / 2}" y="${b.y + 49}" text-anchor="middle">${esc(b.title)}</text>${b.attrs.map((l, i) => `<text class="member" x="${b.x + 22}" y="${b.y + 86 + i * 20}">${esc(l)}</text>`).join("")}${b.methods.map((l, i) => `<text class="member" x="${b.x + 22}" y="${b.y + 138 + i * 20}">${esc(l)}</text>`).join("")}</g>`;
}

function nums(d) {
    return d.match(/-?\d+(?:\.\d+)?/g).map(Number);
}

function head(e) {
    const n = nums(e[2]), end = {x: n[n.length - 2], y: n[n.length - 1]},
        prev = {x: n[n.length - 4], y: n[n.length - 3]}, dx = end.x - prev.x, dy = end.y - prev.y;
    if (e[0] === "extends" || e[0] === "implements") {
        if (Math.abs(dy) >= Math.abs(dx) && dy < 0) return `<path class="hollow" d="M${end.x} ${end.y} L${end.x - 9} ${end.y + 17} L${end.x + 9} ${end.y + 17} Z" stroke="${e[1]}"/>`;
        if (dx < 0) return `<path class="hollow" d="M${end.x} ${end.y} L${end.x + 17} ${end.y - 9} L${end.x + 17} ${end.y + 9} Z" stroke="${e[1]}"/>`;
        return `<path class="hollow" d="M${end.x} ${end.y} L${end.x - 17} ${end.y - 9} L${end.x - 17} ${end.y + 9} Z" stroke="${e[1]}"/>`;
    }
    return "";
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="JPA entity class hierarchy"><defs><filter id="softShadow" x="-8%" y="-10%" width="116%" height="124%"><feDropShadow dx="0" dy="5" stdDeviation="5" flood-color="#0F172A" flood-opacity=".10"/></filter><style>svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}.canvas{fill:${c.canvas}}.frame{fill:${c.frame};stroke:${c.line};stroke-width:1.5;filter:url(#softShadow)}.title{font-family:"Architects Daughter";font-size:42px;fill:${c.ink}}.subtitle,.sectionTitle{font-family:"Comic Mono";font-size:15px;fill:${c.muted}}.section{fill:#F3F8FF;stroke:#94A3B8;stroke-width:1.6;stroke-dasharray:12 8}.umlBox{filter:url(#softShadow);stroke-width:2}.stereo{font-family:"Comic Mono";font-size:12px;fill:${c.muted}}.classTitle{font-family:"Architects Daughter";font-size:22px;fill:${c.ink}}.member{font-family:"Comic Mono";font-size:12.5px;fill:${c.muted}}.edge{fill:none;stroke-width:2.45;stroke-linecap:round;stroke-linejoin:round}.extends{stroke-dasharray:none}.implements{stroke-dasharray:8 7}.hollow{fill:#fff;stroke-width:1.9;stroke-linejoin:round;stroke-dasharray:none}</style></defs><rect class="canvas" width="${W}" height="${H}"/><rect class="frame" x="34" y="30" width="${W - 68}" height="${H - 66}" rx="8"/><text class="title" x="76" y="86">JPA Entity Class Hierarchy</text><text class="subtitle" x="78" y="118">Mapped entity bases separate simple ID entities from self-referencing tree entities.</text><rect class="section" x="62" y="140" width="${W - 124}" height="895" rx="8"/><text class="sectionTitle" x="90" y="165">extends = solid hollow triangle, implements = dashed hollow triangle</text><g>${edges.map(e => `<path class="edge ${e[0]}" d="${e[2]}" stroke="${e[1]}"/>`).join("\n")}</g><g>${edges.map(head).join("\n")}</g>${Object.keys(boxes).map(box).join("\n")}</svg>`;
writeFileSync(`${out}.svg`, svg.replace(/[ \t]+$/gm, ""));
execFileSync(`${process.env.HOME}/.local/bin/cairosvg`, [`${out}.svg`, "-o", `${out}.png`, "-s", "2"], {stdio: "inherit"});
console.log(`Generated ${out}.svg`);
console.log(`Generated ${out}.png`);
