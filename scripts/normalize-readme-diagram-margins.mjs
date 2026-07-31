#!/usr/bin/env node

import {execFileSync} from "node:child_process";
import {existsSync, readFileSync, writeFileSync} from "node:fs";
import {join} from "node:path";

const ROOT = process.cwd();
const REPORT = process.env.DIAGRAM_VALIDATION_REPORT || "/tmp/bluetape4k-projects-diagram-validation-report.json";
const DIR = join(ROOT, "docs/images/readme-diagrams");
const RSVGC = "/opt/homebrew/bin/rsvg-convert";
const MAX_DELTA = 180;

const report = JSON.parse(readFileSync(REPORT, "utf8"));
let changed = 0;

for (const row of report.rows) {
    const contentFailures = row.failures.filter((failure) => /^content (?:horizontal|vertical) margin imbalance=/.test(failure));
    const layerFailures = row.failures.filter((failure) => /^layer (?:horizontal|vertical|content|vertical content)/.test(failure));
    if (contentFailures.length === 0 && layerFailures.length === 0) continue;

    const svgPath = join(DIR, row.file);
    if (!existsSync(svgPath)) continue;
    const original = readFileSync(svgPath, "utf8");
    const frame = frameRect(original);
    if (!frame) continue;

    let dx = 0;
    let dy = 0;
    for (const failure of contentFailures) {
        const match = failure.match(/^content (horizontal|vertical) margin imbalance=(-?\d+)\/(-?\d+)/);
        if (!match) {
            const outside = failure.match(/^content outside frame=(-?\d+)\/(-?\d+)\/(-?\d+)\/(-?\d+)/);
            if (!outside) continue;
            const left = Number(outside[1]);
            const right = Number(outside[2]);
            const top = Number(outside[3]);
            const bottom = Number(outside[4]);
            if (left < 0) dx += -left + 16;
            if (right < 0) dx -= -right + 16;
            if (top < 0) dy += -top + 16;
            if (bottom < 0) dy -= -bottom + 16;
            continue;
        }
        const a = Number(match[2]);
        const b = Number(match[3]);
        const delta = clamp(Math.round((b - a) / 2), -MAX_DELTA, MAX_DELTA);
        if (match[1] === "horizontal") dx += delta;
        else dy += delta;
    }

    let next = dx === 0 && dy === 0 ? original : translateBody(original, frame, dx, dy);
    next = normalizeLayers(next, row.failures);
    if (next === original) continue;

    writeFileSync(svgPath, `${next.trimEnd()}\n`);
    if (process.env.SKIP_DIAGRAM_PNG !== "true") {
        execFileSync(RSVGC, ["--format=png", "--output", svgPath.replace(/\.svg$/, ".png"), svgPath], {stdio: "inherit"});
    }
    changed += 1;
    console.log(`${row.file}: bodyShift=${dx}/${dy}`);
}

console.log(`normalize-readme-diagram-margins: changed=${changed}`);

function translateBody(svg, frame, dx, dy) {
    const footerCutoff = frame.y + frame.h * 0.72;
    const protectedGroupRanges = [];
    let next = svg.replace(/<g\b([^>]*)transform="translate\(([-\d.]+)[ ,]([-\d.]+)\)"([^>]*)>/g, (tag, before, xText, yText, after, offset) => {
        const className = `${before} ${after}`.match(/\bclass="([^"]*)"/)?.[1] || "";
        const id = `${before} ${after}`.match(/\bid="([^"]*)"/)?.[1] || "";
        const x = Number(xText);
        const y = Number(yText);
        if (!Number.isFinite(x) || !Number.isFinite(y)) return tag;
        if (isProtectedClass(className) || /^defs|^legend$|footer/i.test(id)) return tag;
        if (y < frame.y + 86 || y > footerCutoff) return tag;
        protectedGroupRanges.push(findGroupRange(svg, offset));
        return tag.replace(`translate(${xText},${yText})`, `translate(${fmt(x + dx)},${fmt(y + dy)})`)
            .replace(`translate(${xText} ${yText})`, `translate(${fmt(x + dx)} ${fmt(y + dy)})`);
    });

    next = replaceStandaloneTags(next, protectedGroupRanges, /<rect\b[^>]*>/g, (tag) => {
        const className = tag.match(/\bclass="([^"]*)"/)?.[1] || "";
        if (isProtectedClass(className)) return tag;
        const rect = rectFromTag(tag);
        if (!rect || rect.y < frame.y + 86 || rect.y > footerCutoff) return tag;
        return setAttr(setAttr(tag, "x", rect.x + dx), "y", rect.y + dy);
    });

    next = replaceStandaloneTags(next, protectedGroupRanges, /<text\b[^>]*>/g, (tag) => {
        const className = tag.match(/\bclass="([^"]*)"/)?.[1] || "";
        if (isProtectedClass(className)) return tag;
        const y = attrNumber(tag, "y");
        if (!Number.isFinite(y) || y < frame.y + 86 || y > footerCutoff) return tag;
        const x = attrNumber(tag, "x");
        let out = setAttr(tag, "y", y + dy);
        if (Number.isFinite(x)) out = setAttr(out, "x", x + dx);
        return out;
    });

    next = replaceStandaloneTags(next, protectedGroupRanges, /<path\b[^>]*\bd="[^"]+"[^>]*>/g, (tag) => {
        const d = tag.match(/\bd="([^"]+)"/)?.[1];
        if (!d) return tag;
        const box = pathBox(d);
        if (!box || box.y + box.h / 2 < frame.y + 86 || box.y + box.h / 2 > footerCutoff) return tag;
        return tag.replace(/\bd="[^"]+"/, `d="${shiftPath(d, dx, dy)}"`);
    });

    next = replaceStandaloneTags(next, protectedGroupRanges, /<line\b[^>]*>/g, (tag) => {
        const y1 = attrNumber(tag, "y1");
        const y2 = attrNumber(tag, "y2");
        if (!Number.isFinite(y1) || !Number.isFinite(y2)) return tag;
        const cy = (y1 + y2) / 2;
        if (cy < frame.y + 86 || cy > footerCutoff) return tag;
        return ["x1", "x2"].reduce((out, name) => {
            const value = attrNumber(out, name);
            return Number.isFinite(value) ? setAttr(out, name, value + dx) : out;
        }, ["y1", "y2"].reduce((out, name) => setAttr(out, name, attrNumber(out, name) + dy), tag));
    });

    next = replaceStandaloneTags(next, protectedGroupRanges, /<circle\b[^>]*>/g, (tag) => {
        const cy = attrNumber(tag, "cy");
        if (!Number.isFinite(cy) || cy < frame.y + 86 || cy > footerCutoff) return tag;
        const cx = attrNumber(tag, "cx");
        let out = setAttr(tag, "cy", cy + dy);
        if (Number.isFinite(cx)) out = setAttr(out, "cx", cx + dx);
        return out;
    });

    next = replaceStandaloneTags(next, protectedGroupRanges, /<polygon\b[^>]*points="[^"]+"[^>]*>/g, (tag) => {
        const points = tag.match(/\bpoints="([^"]+)"/)?.[1];
        if (!points) return tag;
        const box = pointsBox(points);
        if (!box || box.y + box.h / 2 < frame.y + 86 || box.y + box.h / 2 > footerCutoff) return tag;
        return tag.replace(/\bpoints="[^"]+"/, `points="${shiftPoints(points, dx, dy)}"`);
    });

    return next;
}

function normalizeLayers(svg, failures) {
    const adjustments = new Map();
    for (const failure of failures) {
        let match = failure.match(/^layer horizontal margin imbalance=(\d+)\/(\d+) (-?\d+)\/(-?\d+)/);
        if (match) {
            const key = `${match[1]}/${match[2]}`;
            const left = Number(match[3]);
            const right = Number(match[4]);
            const item = adjustments.get(key) || layerAdjustment(Number(match[1]), Number(match[2]));
            item.dw += clamp(Math.round(left - right), -MAX_DELTA, MAX_DELTA);
            adjustments.set(key, item);
            continue;
        }

        match = failure.match(/^layer vertical margin imbalance=(\d+)\/(\d+) (-?\d+)\/(-?\d+)/);
        if (match) {
            const key = `${match[1]}/${match[2]}`;
            const top = Number(match[3]);
            const bottom = Number(match[4]);
            const item = adjustments.get(key) || layerAdjustment(Number(match[1]), Number(match[2]));
            item.dh += clamp(Math.round(top - bottom), -MAX_DELTA, MAX_DELTA);
            adjustments.set(key, item);
            continue;
        }

        match = failure.match(/^layer content outside body=(\d+)\/(\d+) margins=(-?\d+)\/(-?\d+)/);
        if (match) {
            const key = `${match[1]}/${match[2]}`;
            const left = Number(match[3]);
            const right = Number(match[4]);
            const item = adjustments.get(key) || layerAdjustment(Number(match[1]), Number(match[2]));
            if (left < 0) {
                item.dx += left - 14;
                item.dw += -left + 14;
            }
            if (right < 0) item.dw += -right + 14;
            adjustments.set(key, item);
            continue;
        }

        match = failure.match(/^layer vertical content outside body=(\d+)\/(\d+) margins=(-?\d+)\/(-?\d+)/);
        if (match) {
            const key = `${match[1]}/${match[2]}`;
            const top = Number(match[3]);
            const bottom = Number(match[4]);
            const item = adjustments.get(key) || layerAdjustment(Number(match[1]), Number(match[2]));
            if (top < 0) {
                item.dy += top - 14;
                item.dh += -top + 14;
            }
            if (bottom < 0) item.dh += -bottom + 14;
            adjustments.set(key, item);
        }
    }

    let next = svg;
    for (const adjustment of adjustments.values()) {
        next = adjustLayerRect(next, adjustment);
        next = adjustLayerTitle(next, adjustment);
    }
    return next;
}

function layerAdjustment(x, y) {
    return {x, y, dx: 0, dy: 0, dw: 0, dh: 0};
}

function adjustLayerRect(svg, adjustment) {
    const rectPattern = /<rect\b[^>]*class="[^"]*(?:layer|panel|band)[^"]*"[^>]*>/g;
    return svg.replace(rectPattern, (tag) => {
        const rect = rectFromTag(tag);
        if (!rect) return tag;
        if (Math.abs(rect.x - adjustment.x) > 1 || Math.abs(rect.y - adjustment.y) > 1) return tag;
        return setAttr(setAttr(setAttr(setAttr(tag,
                        "x", rect.x + adjustment.dx),
                    "y", rect.y + adjustment.dy),
                "width", rect.w + adjustment.dw),
            "height", rect.h + adjustment.dh);
    });
}

function adjustLayerTitle(svg, adjustment) {
    const nearX = adjustment.x - 8;
    const farX = adjustment.x + 360;
    const nearY = adjustment.y - 8;
    const farY = adjustment.y + 96;
    return svg.replace(/<text\b[^>]*class="[^"]*(?:panelTitle|layerTitle|layer-title|panel-title)[^"]*"[^>]*>/g, (tag) => {
        const x = attrNumber(tag, "x");
        const y = attrNumber(tag, "y");
        if (!Number.isFinite(x) || !Number.isFinite(y)) return tag;
        if (x < nearX || x > farX || y < nearY || y > farY) return tag;
        return setAttr(setAttr(tag, "x", x + adjustment.dx), "y", y + adjustment.dy);
    });
}

function replaceStandaloneTags(svg, protectedRanges, pattern, replacer) {
    return svg.replace(pattern, (tag, ...args) => {
        const offset = args.at(-2);
        if (protectedRanges.some((range) => range && offset > range.start && offset < range.end)) return tag;
        return replacer(tag);
    });
}

function findGroupRange(svg, start) {
    const close = svg.indexOf("</g>", start);
    return close < 0 ? null : {start, end: close + 4};
}

function isProtectedClass(className) {
    return /\b(?:canvas|frame|title|subtitle|footer|small|tiny)\b/.test(className || "");
}

function frameRect(svg) {
    const tag = svg.match(/<rect[^>]*class="[^"]*frame[^"]*"[^>]*>/)?.[0];
    return tag ? rectFromTag(tag) : null;
}

function rectFromTag(tag) {
    const rect = {
        x: attrNumber(tag, "x"),
        y: attrNumber(tag, "y"),
        w: attrNumber(tag, "width"),
        h: attrNumber(tag, "height"),
    };
    return Object.values(rect).every(Number.isFinite) ? rect : null;
}

function attrNumber(tag, name) {
    return Number(tag.match(new RegExp(`\\b${name}="([-\\d.]+)"`))?.[1]);
}

function setAttr(tag, name, value) {
    const formatted = fmt(value);
    return tag.replace(new RegExp(`\\b${name}="[-\\d.]+"`), `${name}="${formatted}"`);
}

function pathBox(d) {
    const nums = d.match(/-?\d*\.?\d+(?:e[-+]?\d+)?/gi)?.map(Number) || [];
    if (nums.length < 2) return null;
    const xs = [];
    const ys = [];
    for (let index = 0; index + 1 < nums.length; index += 2) {
        xs.push(nums[index]);
        ys.push(nums[index + 1]);
    }
    return bounds(xs, ys);
}

function pointsBox(points) {
    const nums = points.match(/-?\d*\.?\d+/g)?.map(Number) || [];
    if (nums.length < 2) return null;
    const xs = [];
    const ys = [];
    for (let index = 0; index + 1 < nums.length; index += 2) {
        xs.push(nums[index]);
        ys.push(nums[index + 1]);
    }
    return bounds(xs, ys);
}

function bounds(xs, ys) {
    const minX = Math.min(...xs);
    const minY = Math.min(...ys);
    const maxX = Math.max(...xs);
    const maxY = Math.max(...ys);
    return {x: minX, y: minY, w: maxX - minX, h: maxY - minY};
}

function shiftPath(d, dx, dy) {
    let coord = 0;
    return d.replace(/-?\d*\.?\d+(?:e[-+]?\d+)?/gi, (value) => {
        const delta = coord % 2 === 0 ? dx : dy;
        coord += 1;
        return fmt(Number(value) + delta);
    });
}

function shiftPoints(points, dx, dy) {
    let coord = 0;
    return points.replace(/-?\d*\.?\d+/g, (value) => {
        const delta = coord % 2 === 0 ? dx : dy;
        coord += 1;
        return fmt(Number(value) + delta);
    });
}

function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
}

function fmt(value) {
    return Number(value.toFixed(2)).toString();
}
