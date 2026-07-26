#!/usr/bin/env node

import {execFileSync} from "node:child_process";
import {readdirSync, readFileSync, writeFileSync} from "node:fs";
import {join} from "node:path";

const DIAGRAM_DIR = "docs/images/readme-diagrams";
const RSVGC = "/opt/homebrew/bin/rsvg-convert";
const MIN_CLEARANCE = 8;

const files = readdirSync(DIAGRAM_DIR)
    .filter((file) => file.endsWith(".svg"))
    .sort();

let modernized = 0;
let straightened = 0;

for (const file of files) {
    const svgPath = join(DIAGRAM_DIR, file);
    const original = readFileSync(svgPath, "utf8");
    if (!/(?:original Mermaid source|Source-checked .*?(?:relationships|flow|diagram|rendering))/i.test(original)) continue;

    const kind = classify(file, extractTitle(original));
    let svg = modernizeSubtitle(original, kind);
    if (kind === "class") {
        svg = colorizeLegacyClassRoutes(svg);
    }
    if (["architecture", "module", "flow-state"].includes(kind)) {
        const result = straightenAvoidableDoglegs(svg);
        svg = result.svg;
        straightened += result.count;
    }

    if (svg !== original) {
        writeFileSync(svgPath, cleanSvg(svg));
        execFileSync(RSVGC, ["--format", "png", "--output", svgPath.replace(/\.svg$/, ".png"), svgPath], {stdio: "inherit"});
        modernized += 1;
    }
}

function cleanSvg(svg) {
    return `${svg.replace(/[ \t]+$/gm, "").trimEnd()}\n`;
}

function colorizeLegacyClassRoutes(svg) {
    const colors = ["#3E9868", "#4F83BF", "#2E8F89", "#B9851B", "#C94D68", "#755BC6", "#718A35"];
    let index = 0;
    return svg
        .replace(/(<marker id="openArrow"[^>]*><path d="M 0\.5 0\.5 L 4\.5 2\.5 L 0\.5 4\.5" fill="none" stroke=")[^"]+("[^>]*><\/marker>)/, "$1context-stroke$2")
        .replace(/(<marker id="inherit"[^>]*><path d="M 0\.25 0\.5 L 4\.75 2\.5 L 0\.25 4\.5 Z" fill="#ffffff" stroke=")[^"]+("[^>]*><\/marker>)/, "$1context-stroke$2")
        .replace(/<path\b([^>]*class="[^"]*(?:line|dashed|inheritLine|implLine)[^"]*"[^>]*)>/g, (tag, attrs) => {
            const color = colors[index % colors.length];
            index += 1;
            return `<path${withRouteStroke(attrs, color)}/>`;
        });
}

function withRouteStroke(attrs, color) {
    let next = attrs.replace(/\s*\/\s*$/, "");
    next = next.replace(/\s*\/\s+(?=style=)/, " ");
    if (/\bstyle="/.test(next)) {
        next = next.replace(/\bstyle="([^"]*)"/, (_match, style) => `style="${style.replace(/;?$/, ";")}stroke:${color}"`);
    } else {
        next += ` style="stroke:${color}"`;
    }
    if (!/\bdata-route-color=/.test(next)) next += ` data-route-color="${color}"`;
    return next;
}

console.log(`legacy-readme-diagrams: modernized=${modernized} straightenedDoglegs=${straightened}`);

function modernizeSubtitle(svg, kind) {
    const subtitle = subtitleFor(kind);
    return svg
        .replace(/(<text[^>]*class="[^"]*title[^"]*"[^>]*>)([^<]*?)\bOv\.(<\/text>)/i, "$1$2Overview$3")
        .replace(/<text class="subtitle" x="70" y="(?:104|118|130)">[^<]*(?:original Mermaid source|Source-checked [^<]+)<\/text>/i, `<text class="subtitle" x="70" y="104">${subtitle}</text>`)
        .replace(/<text class="subtitle" x="70" y="(?:104|118|130)">([^<]*)<\/text>/i, (_match, text) => {
            if (!/(?:original Mermaid source|Source-checked .*?(?:relationships|flow|diagram|rendering))/i.test(text)) return _match;
            return `<text class="subtitle" x="70" y="104">${subtitle}</text>`;
        });
}

function subtitleFor(kind) {
    if (kind === "sequence") return "Shows the main calls, branches, and returned values for this flow.";
    if (kind === "class") return "Source-checked class relationships with readable compartments and routed inheritance.";
    if (kind === "flow-state") return "Source-checked lifecycle flow with grouped phases and explicit transitions.";
    if (kind === "module") return "Source-checked module relationships grouped by API and implementation responsibility.";
    if (kind === "architecture") return "Source-checked architecture relationships with grouped responsibilities and routed connectors.";
    return "Source-checked README diagram refreshed with stable SVG/PNG rendering.";
}

function straightenAvoidableDoglegs(svg) {
    const cards = extractCards(svg);
    let count = 0;
    const nextSvg = svg.replace(/<path\b[^>]*\bd="([^"]+)"[^>]*>/g, (tag, d) => {
        if (!/class="[^"]*(?:flow|line|Line|connector|dashed)[^"]*"/.test(tag)) return tag;
        const points = simplifyCollinearPoints(parsePath(d));
        if (points.length <= 2 || !hasDirectionChange(points)) return tag;

        const source = cards.find((card) => pointOnBoundary(points[0], card));
        const target = cards.find((card) => pointOnBoundary(points.at(-1), card));
        if (!source || !target || sameRect(source, target)) return tag;

        const direct = clearStraightSegment(source, target, cards);
        if (!direct) return tag;
        count += 1;
        return tag.replace(/\bd="[^"]+"/, `d="M ${fmt(direct.a.x)} ${fmt(direct.a.y)} L ${fmt(direct.b.x)} ${fmt(direct.b.y)}"`);
    });
    return {svg: nextSvg, count};
}

function extractTitle(svg) {
    return svg.match(/<text[^>]*class="[^"]*title[^"]*"[^>]*>([\s\S]*?)<\/text>/)?.[1]?.replace(/<[^>]+>/g, "").trim() || "";
}

function classify(file, title) {
    const text = `${file} ${title}`;
    if (/sequence/i.test(text)) return "sequence";
    if (/Class Structure|Structure Classes|Class Hierarchy|Class Diagram|Hierarchy|Classes\b|UML|Domain Model|Domain Classes|Interface and Implementations|API Structure|Type Diagram/i.test(text)) return "class";
    if (/Module Diagram|Module API Structure|Module Structure|Dependency Structure|Repository Module Structure|Dependency Diagram/i.test(text)) return "module";
    if (/Flow|Pipeline|Lifecycle|State|Retry|Processing|FSM|Algorithm|Selection|Transitions|Execution Model|Sequential|Parallel|Conditional|Repeat|DSL Builder/i.test(text)) return "flow-state";
    if (/Architecture|Overview|Layer|Stack|Topology|Integration|Observability|Component|Transport Layer/i.test(text)) return "architecture";
    return "unclassified";
}

function extractCards(svg) {
    const cards = [];
    const groupPattern = /<g(?:\s+[^>]*)?>([\s\S]*?)<\/g>/g;
    let groupMatch;
    while ((groupMatch = groupPattern.exec(svg))) {
        const groupOpen = groupMatch[0].match(/^<g(?:\s+[^>]*)?>/)?.[0] || "";
        const transform = groupOpen.match(/transform="translate\(([-\d.]+)[ ,]([-\d.]+)\)"/);
        if (!transform && !/\bid="[^"]+"/.test(groupOpen)) continue;
        if (/\bid="(?:layer|panel)-[^"]*"/.test(groupOpen)) continue;
        const body = groupMatch[1];
        if (!/class="[^"]*(?:card|soft|chip)[^"]*"/.test(body)) continue;
        const dx = transform ? Number(transform[1]) : 0;
        const dy = transform ? Number(transform[2]) : 0;
        const rectTag = [...body.matchAll(/<rect[^>]*>/g)]
            .map((match) => match[0])
            .find((tag) => /class="[^"]*(?:card|soft|chip)[^"]*"/.test(tag));
        if (!rectTag) continue;
        const rect = {
            x: dx + attrNumber(rectTag, "x"),
            y: dy + attrNumber(rectTag, "y"),
            w: attrNumber(rectTag, "width"),
            h: attrNumber(rectTag, "height"),
        };
        if ([rect.x, rect.y, rect.w, rect.h].some((value) => Number.isNaN(value))) continue;
        if ((rect.w > 500 && rect.h > 160) || rect.h > 300) continue;
        cards.push(rect);
    }
    return cards.filter((card) => card.w >= 40 && card.h >= 24);
}

function attrNumber(tag, name) {
    return Number(tag.match(new RegExp(`\\b${name}="([-\\d.]+)"`))?.[1]);
}

function parsePath(d) {
    const tokens = d.match(/[MLHVCSQTAZmlhvcsqtaz]|-?\d*\.?\d+(?:e[-+]?\d+)?/g) || [];
    const points = [];
    let x = 0;
    let y = 0;
    let command = "";
    for (let i = 0; i < tokens.length;) {
        const token = tokens[i++];
        if (/^[A-Za-z]$/.test(token)) {
            command = token;
            if (/Z/i.test(command)) break;
            continue;
        }
        i -= 1;
        if (command === "M" || command === "L") {
            x = Number(tokens[i++]);
            y = Number(tokens[i++]);
            points.push({x, y});
        } else if (command === "H") {
            x = Number(tokens[i++]);
            points.push({x, y});
        } else if (command === "V") {
            y = Number(tokens[i++]);
            points.push({x, y});
        } else if (command === "C") {
            i += 4;
            x = Number(tokens[i++]);
            y = Number(tokens[i++]);
            points.push({x, y});
        } else {
            break;
        }
    }
    return points;
}

function clearStraightSegment(source, target, cards) {
    const sourceCenter = rectCenter(source);
    const targetCenter = rectCenter(target);
    const horizontalGap = target.x >= source.x + source.w || source.x >= target.x + target.w;
    const verticalGap = target.y >= source.y + source.h || source.y >= target.y + target.h;
    const alignedY = Math.abs(sourceCenter.y - targetCenter.y) <= 18
        && source.y + MIN_CLEARANCE < sourceCenter.y
        && sourceCenter.y < source.y + source.h - MIN_CLEARANCE
        && target.y + MIN_CLEARANCE < targetCenter.y
        && targetCenter.y < target.y + target.h - MIN_CLEARANCE;
    if (horizontalGap && alignedY) {
        const y = round((sourceCenter.y + targetCenter.y) / 2);
        const a = {x: round(sourceCenter.x < targetCenter.x ? source.x + source.w : source.x), y};
        const b = {x: round(sourceCenter.x < targetCenter.x ? target.x : target.x + target.w), y};
        return segmentClearOfOtherCards(a, b, source, target, cards) ? {a, b} : null;
    }

    const alignedX = Math.abs(sourceCenter.x - targetCenter.x) <= 18
        && source.x + MIN_CLEARANCE < sourceCenter.x
        && sourceCenter.x < source.x + source.w - MIN_CLEARANCE
        && target.x + MIN_CLEARANCE < targetCenter.x
        && targetCenter.x < target.x + target.w - MIN_CLEARANCE;
    if (verticalGap && alignedX) {
        const x = round((sourceCenter.x + targetCenter.x) / 2);
        const a = {x, y: round(sourceCenter.y < targetCenter.y ? source.y + source.h : source.y)};
        const b = {x, y: round(sourceCenter.y < targetCenter.y ? target.y : target.y + target.h)};
        return segmentClearOfOtherCards(a, b, source, target, cards) ? {a, b} : null;
    }

    return null;
}

function segmentClearOfOtherCards(a, b, source, target, cards) {
    return cards.every((card) => sameRect(card, source) || sameRect(card, target) || !segmentIntersectsRectInterior(a, b, card, MIN_CLEARANCE));
}

function segmentIntersectsRectInterior(a, b, rect, pad) {
    const minX = rect.x - pad;
    const maxX = rect.x + rect.w + pad;
    const minY = rect.y - pad;
    const maxY = rect.y + rect.h + pad;
    if (near(a.y, b.y, 0.5)) {
        if (a.y <= minY || a.y >= maxY) return false;
        return Math.max(a.x, b.x) > minX && Math.min(a.x, b.x) < maxX;
    }
    if (near(a.x, b.x, 0.5)) {
        if (a.x <= minX || a.x >= maxX) return false;
        return Math.max(a.y, b.y) > minY && Math.min(a.y, b.y) < maxY;
    }
    return false;
}

function pointOnBoundary(point, rect) {
    const inX = point.x >= rect.x - 0.5 && point.x <= rect.x + rect.w + 0.5;
    const inY = point.y >= rect.y - 0.5 && point.y <= rect.y + rect.h + 0.5;
    return inX && inY && (near(point.x, rect.x, 0.5) || near(point.x, rect.x + rect.w, 0.5) || near(point.y, rect.y, 0.5) || near(point.y, rect.y + rect.h, 0.5));
}

function simplifyCollinearPoints(points) {
    const deduped = [];
    for (const point of points) {
        if (deduped.length === 0 || !samePoint(deduped.at(-1), point)) deduped.push(point);
    }
    const simplified = [];
    for (const point of deduped) {
        simplified.push(point);
        while (simplified.length >= 3) {
            const [a, b, c] = simplified.slice(-3);
            if (!sameDirection(a, b, c)) break;
            simplified.splice(simplified.length - 2, 1);
        }
    }
    return simplified;
}

function hasDirectionChange(points) {
    for (let index = 2; index < points.length; index += 1) {
        if (!sameDirection(points[index - 2], points[index - 1], points[index])) return true;
    }
    return false;
}

function sameDirection(a, b, c) {
    return (near(a.x, b.x, 0.5) && near(b.x, c.x, 0.5)) || (near(a.y, b.y, 0.5) && near(b.y, c.y, 0.5));
}

function samePoint(a, b) {
    return near(a.x, b.x, 0.5) && near(a.y, b.y, 0.5);
}

function sameRect(a, b) {
    return near(a.x, b.x, 0.5) && near(a.y, b.y, 0.5) && near(a.w, b.w, 0.5) && near(a.h, b.h, 0.5);
}

function rectCenter(rect) {
    return {x: rect.x + rect.w / 2, y: rect.y + rect.h / 2};
}

function round(value) {
    return Math.round(value * 10) / 10;
}

function fmt(value) {
    return Number.isInteger(value) ? String(value) : value.toFixed(1);
}

function near(left, right, tolerance) {
    return Math.abs(left - right) <= tolerance;
}
