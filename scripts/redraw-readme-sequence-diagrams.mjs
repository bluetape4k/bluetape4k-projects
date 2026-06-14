#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import { basename, join } from "node:path";

const ROOT = process.cwd();
const OUT = join(ROOT, "docs/images/readme-diagrams");
const rsvg = "/opt/homebrew/bin/rsvg-convert";

const palette = {
  blue: ["#E8F3FF", "#5B8DEF", "#4F83BF"],
  green: ["#EAF7EF", "#58A978", "#3E9868"],
  teal: ["#E9F7F6", "#45A7A1", "#2E8F89"],
  amber: ["#FFF3D9", "#D6A441", "#B9851B"],
  pink: ["#FDECEF", "#DC6B82", "#C94D68"],
  purple: ["#F1ECFF", "#8A72D6", "#755BC6"],
  gray: ["#F2F5F9", "#9AA8B8", "#758297"],
};
const colors = Object.keys(palette);

const files = readdirSync(OUT)
  .filter((file) => file.endsWith(".svg") && file.includes("sequence") && !file.endsWith("-sketch.svg") && !file.endsWith("-graphviz.svg"))
  .sort();

let redrawn = 0;
let alreadyCompliant = 0;
let skipped = 0;
let totalMessages = 0;
let totalBranches = 0;

for (const file of files) {
  const path = join(OUT, file);
  const svg = readFileSync(path, "utf8");
  const model = parseSequence(svg, file);
  if (!model) {
    skipped += 1;
    continue;
  }

  const branchEventCount = model.events.filter((event) => event.type === "branch").length;
  const branchPillCount = (svg.match(/branchPill/g) || []).length;
  if (!model.forceRedraw
      && branchPillCount === branchEventCount
      && isApprovedSequence(svg)
      && !hasAwkwardSequenceText(svg)
      && !hasEmptyBranchRows(svg)
      && !hasViewBoxOverflow(svg)
      && !hasSequenceLabelCrossings(svg)
      && !hasSequenceBranchLifelineCrossings(svg)) {
    alreadyCompliant += 1;
    continue;
  }

  const rendered = renderSequence(model);
  const svgPath = join(OUT, file);
  const pngPath = svgPath.replace(/\.svg$/, ".png");
  writeFileSync(svgPath, rendered);
  execFileSync(rsvg, ["--format=png", "--output", pngPath, svgPath], { stdio: "inherit" });
  redrawn += 1;
  totalMessages += model.events.filter((event) => event.type === "message").length;
  totalBranches += model.events.filter((event) => event.type === "branch").length;
  console.log(`${basename(pngPath)} participants=${model.participants.length} messages=${model.events.filter((event) => event.type === "message").length}`);
}

console.log(`redraw-sequence: files=${files.length} redrawn=${redrawn} alreadyCompliant=${alreadyCompliant} skippedNonSequence=${skipped} messages=${totalMessages} branches=${totalBranches}`);

function parseSequence(svg, file) {
  const special = specialSequenceModel(file);
  if (special) return special;
  if (!/<line[^>]*class="[^"]*lifeline/.test(svg)) return parseFlowLikeSequence(svg, file);
  const participants = extractParticipants(svg);
  if (participants.length < 2) return null;
  const events = extractEvents(svg, participants);
  if (events.filter((event) => event.type === "message").length < 2) return null;
  return {
    file,
    title: extractTitle(svg) || titleFromFile(file),
    subtitle: "Source-checked interaction flow with compact labels, numbered calls, and readable branch semantics.",
    participants,
    events,
  };
}

function extractParticipants(svg) {
  const rects = [...svg.matchAll(/<rect\b[^>]*class="[^"]*card[^"]*"[^>]*>/g)]
    .map((match) => ({ tag: match[0], index: match.index, ...attrs(match[0]) }))
    .filter((rect) => rect.y >= 120 && rect.y <= 280 && rect.w >= 120 && rect.w <= 420 && rect.h >= 46 && rect.h <= 110);
  const participantRects = [...svg.matchAll(/<rect\b[^>]*class="[^"]*participant[^"]*"[^>]*>/g)]
    .map((match) => ({ tag: match[0], index: match.index, ...attrs(match[0]) }))
    .filter((rect) => rect.y >= 120 && rect.y <= 280 && rect.w >= 120 && rect.w <= 420 && rect.h >= 46 && rect.h <= 110);
  rects.push(...participantRects);
  const texts = extractTexts(svg);
  return [...svg.matchAll(/<line\b[^>]*class="[^"]*lifeline[^"]*"[^>]*>/g)]
    .map((match) => {
      const line = { tag: match[0], index: match.index, ...attrs(match[0]) };
      const rect = rects
        .filter((candidate) => candidate.index < line.index && line.x1 >= candidate.x - 2 && line.x1 <= candidate.x + candidate.w + 2)
        .sort((a, b) => b.index - a.index)[0];
      if (!rect) return null;
      const label = texts
        .filter((text) => text.index > rect.index && text.index < line.index && text.x >= rect.x - 5 && text.x <= rect.x + rect.w + 5)
        .map((text) => text.text)
        .join(" ")
        .trim();
      return {
        id: slug(label || `participant-${line.x1}`),
        label: normalizeParticipant(label || `Participant ${line.x1}`),
        oldX: line.x1,
        color: colors[Math.min(colors.length - 1, Math.max(0, rects.indexOf(rect))) % colors.length],
      };
    })
    .filter(Boolean)
    .filter((participant, index, all) => all.findIndex((item) => Math.abs(item.oldX - participant.oldX) < 2) === index)
    .sort((a, b) => a.oldX - b.oldX);
}

function extractEvents(svg, participants) {
  const pathMatches = [...svg.matchAll(/<path\b[^>]*class="[^"]*(?:line|dashed|seq|seqReturn|sequenceEdge|call[A-Z][A-Za-z]*|return[A-Z][A-Za-z]*)[^"]*"[^>]*\bd="([^"]+)"[^>]*>/g)]
    .map((match) => ({ tag: match[0], d: match[1], index: match.index, points: parsePath(match[1]) }))
    .filter((path) => path.points.length >= 2);
  const texts = extractTexts(svg);
  const events = [];

  for (const text of texts) {
    const branch = text.text.match(/^(ALT|ELSE|OPT|PAR|AND|LOOP|WHEN)\b\s*:?\s*(.+)$/i);
    if (branch) {
      const keyword = branch[1].toLowerCase();
      const rest = branch[2].trim();
      if (keyword === "par" && /^(allel|ticipants)\b/i.test(rest)) continue;
      if (keyword === "and" && /^and\b/i.test(rest)) continue;
      events.push({ type: "branch", index: text.index, label: `${keyword} ${rest}`.trim() });
      continue;
    }

    const numeric = text.text.match(/^\d+\.\s*(.+)$/);
    const edgeLabel = /(?:edgeLabel|messageLabel|label)/.test(text.tag) && !/normal lifecycle|success|error|cancellation/i.test(text.text);
    if (!numeric && !edgeLabel) continue;
    const path = pathMatches.find((candidate) => candidate.index > text.index && candidate.index - text.index < 700);
    if (!path) continue;
    const first = path.points[0];
    const last = path.points.at(-1);
    const from = nearestParticipant(participants, first.x);
    const to = nearestParticipant(participants, last.x);
    if (!from || !to) continue;
    const rawLabel = cleanLabel(numeric ? numeric[1] : text.text);
    if (!rawLabel) continue;
    events.push({
      type: "message",
      index: text.index,
      label: rawLabel,
      from: from.id,
      to: to.id,
      return: /dashed|seqReturn|return/i.test(path.tag) || first.x > last.x || /return|response|result|success|failure|exception|error|completed|validated|value/i.test(rawLabel),
    });
  }

  if (events.filter((event) => event.type === "message").length === 0) {
    events.push(...extractApprovedMessageGroups(svg, participants));
  }

  return coalesceBranches(events.sort((a, b) => a.index - b.index));
}

function extractApprovedMessageGroups(svg, participants) {
  const events = [];
  for (const match of svg.matchAll(/<g id="m\d+">([\s\S]*?)<\/g>/g)) {
    const body = match[1];
    const pathMatch = body.match(/<path\b[^>]*class="[^"]*seq(?:Return)?[^"]*"[^>]*\bd="([^"]+)"[^>]*>/);
    if (!pathMatch) continue;
    const points = parsePath(pathMatch[1]);
    if (points.length < 2) continue;
    const first = points[0];
    const last = points.at(-1);
    const labelTexts = [...body.matchAll(/<text\b([^>]*)>([\s\S]*?)<\/text>/g)]
      .map((text) => decode(text[2]))
      .filter((text) => text && !/^\d+$/.test(text));
    const label = cleanLabel(labelTexts.join(" "));
    if (!label) continue;
    events.push({
      type: "message",
      index: match.index,
      from: nearestParticipant(participants, first.x).id,
      to: nearestParticipant(participants, last.x).id,
      label,
      return: /seqReturn/.test(pathMatch[0]) || first.x > last.x || /return|response|result|success|failure|exception|error|completed|validated|value/i.test(label),
    });
  }
  return events;
}

function parseFlowLikeSequence(svg, file) {
  if (!/Sequence/i.test(`${file} ${extractTitle(svg)}`)) return null;
  const cards = extractFlowCards(svg);
  if (cards.length < 3) return null;
  const events = [];
  for (let index = 1; index < cards.length; index += 1) {
    events.push({
      type: "message",
      index,
      label: flowMessageLabel(cards[index]),
      from: cards[index - 1].id,
      to: cards[index].id,
      return: index > Math.ceil(cards.length * 0.6),
    });
  }
  return {
    file,
    title: extractTitle(svg) || titleFromFile(file),
    subtitle: "Source-checked interaction flow with sequence lifelines, numbered calls, and compact labels.",
    participants: cards.map((card, index) => ({
      id: card.id,
      label: card.title,
      oldX: 100 + index * 260,
      color: colors[index % colors.length],
    })),
    events,
  };
}

function extractFlowCards(svg) {
  const groupPattern = /<g(?:\s+[^>]*)?>([\s\S]*?)<\/g>/g;
  const cards = [];
  let match;
  while ((match = groupPattern.exec(svg))) {
    const body = match[1];
    const rectTag = body.match(/<rect\b[^>]*class="[^"]*card[^"]*"[^>]*>/)?.[0];
    if (!rectTag) continue;
    const rect = attrs(rectTag);
    if ([rect.x, rect.y, rect.w, rect.h].some((value) => Number.isNaN(value))) continue;
    const labels = [...body.matchAll(/<text\b([^>]*)>([\s\S]*?)<\/text>/g)].map((text) => decode(text[2])).filter(Boolean);
    if (labels.length === 0) continue;
    cards.push({
      id: slug(labels[0]),
      title: labels[0],
      details: labels.slice(1),
      x: rect.x,
      y: rect.y,
    });
  }
  return cards
    .toSorted((a, b) => (a.y - b.y) || (a.x - b.x))
    .filter((card, index, all) => all.findIndex((item) => item.id === card.id) === index);
}

function flowMessageLabel(card) {
  return cleanLabel(card.details.find((detail) => detail.length <= 34) || `enter ${card.title}`);
}

function coalesceBranches(events) {
  const result = [];
  let previousBranch = "";
  for (const event of events) {
    if (event.type === "branch") {
      if (event.label === previousBranch) continue;
      previousBranch = event.label;
    } else {
      previousBranch = "";
    }
    result.push(event);
  }
  return result;
}

function renderSequence(model) {
  const events = pruneEmptyBranches(model.events);
  const messageEvents = events.filter((event) => event.type === "message");
  const maxLabelLines = Math.max(1, ...messageEvents.map((event) => wrapMessageLabel(event.label, 58).length));
  const gap = Math.max(420, Math.min(860, Math.max(...messageEvents.map((event) => event.label.length * 7.4 + 210))));
  const side = 140;
  const participantW = 230;
  const width = Math.max(1240, side * 2 + participantW + (model.participants.length - 1) * gap);
  const headerY = 170;
  const headerH = 78;
  const topMessageY = 330;
  const rowStep = maxLabelLines > 1 ? 116 : 88;
  const branchStep = 58;
  const bodyRows = events.reduce((sum, event) => sum + (event.type === "branch" ? branchStep : rowStep), 0);
  const footerY = topMessageY + bodyRows + 40;
  const height = footerY + 118;
  const centerStart = side + participantW / 2;
  const centers = model.participants.map((_, index) => centerStart + index * gap);
  const participantById = new Map(model.participants.map((participant, index) => [participant.id, { ...participant, x: centers[index] }]));
  const lifelineBottom = footerY - 24;
  const yByParticipant = new Map(model.participants.map((participant) => [participant.id, []]));

  let y = topMessageY;
  const body = [];
  for (const [index, participant] of model.participants.entries()) {
    body.push(renderParticipant(participant, centers[index] - participantW / 2, headerY, participantW, headerH, lifelineBottom));
  }

  for (const event of events) {
    if (event.type === "branch") {
      body.push(renderBranch(event.label, side - 28, y - 22, width - (side - 28) * 2, centers));
      y += branchStep;
      continue;
    }
    yByParticipant.get(event.from)?.push(y);
    yByParticipant.get(event.to)?.push(y);
    y += rowStep;
  }

  for (const participant of model.participants) {
    const ys = yByParticipant.get(participant.id) || [];
    if (ys.length === 0) continue;
    const first = Math.min(...ys) - 8;
    const last = Math.max(...ys) + 20;
    body.push(renderActivation(participantById.get(participant.id).x, first, Math.max(52, last - first), participant.color));
  }

  y = topMessageY;
  let number = 1;
  for (const event of events) {
    if (event.type === "branch") {
      y += branchStep;
      continue;
    }
    const from = participantById.get(event.from);
    const to = participantById.get(event.to);
    const color = event.return ? "teal" : colors[(number - 1) % (colors.length - 1)];
    body.push(renderMessage(`m${number}`, from.x, to.x, y, event.label, color, event.return, number));
    y += rowStep;
    number += 1;
  }

  body.push(renderFooter(side, footerY, width - side * 2, "bluetape4k-projects - github.com/bluetape4k/bluetape4k-projects"));
  return base(width, height, model.title, model.subtitle, body.join("\n"));
}

function pruneEmptyBranches(events) {
  return events.filter((event, index) => {
    if (event.type !== "branch") return true;
    const next = events[index + 1];
    return next?.type === "message";
  });
}

function base(width, height, title, subtitle, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title)}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.10"/></filter>
  ${Object.entries(palette).map(([name, value]) => `<marker id="seqArrow-${name}" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="${value[2]}"/></marker>`).join("\n  ")}
  <style>
    .canvas{fill:#F6F9FC}.frame{fill:#fff;stroke:#C7D7E7;stroke-width:3;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:#22344A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#536476}
    .card{filter:url(#shadow);stroke-width:2}.cardTitle{font-family:"Architects Daughter";font-size:24px;fill:#22344A}.detail{font-family:"Comic Mono";font-size:13px;fill:#42556B}
    .lifeline{stroke:#A7B4C3;stroke-width:1.8;stroke-dasharray:8 8}.seq{fill:none;stroke-width:3;stroke-linecap:round}.seqReturn{fill:none;stroke-width:2.7;stroke-linecap:round;stroke-dasharray:8 7}
    .labelPill{fill:#fff;stroke:#D6E3EF;stroke-width:1.4}.altBox{fill:#FFFFFF;fill-opacity:.42;stroke:#D6A441;stroke-width:1.8;stroke-dasharray:8 8}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="32" y="28" width="${width - 64}" height="${height - 56}" rx="30"/>
<text class="title" x="68" y="84">${esc(title)}</text>
<text class="subtitle" x="72" y="116">${esc(subtitle)}</text>
${body}
</svg>
`;
}

function renderParticipant(participant, x, y, w, h, bottom) {
  const [fill, stroke] = palette[participant.color] || palette.gray;
  const lines = wrapIdentifier(participant.label, 18).slice(0, 2);
  const start = lines.length > 1 ? y + 30 : y + 45;
  return `<g id="seq-${participant.id}"><rect class="card" x="${round(x)}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}"/>${lines.map((line, index) => `<text class="cardTitle" x="${round(x + w / 2)}" y="${start + index * 24}" text-anchor="middle">${esc(line)}</text>`).join("")}<line class="lifeline" x1="${round(x + w / 2)}" y1="${y + h}" x2="${round(x + w / 2)}" y2="${bottom}"/></g>`;
}

function renderActivation(x, y, h, color) {
  const [fill, stroke] = palette[color] || palette.gray;
  return `<rect x="${round(x - 8)}" y="${round(y)}" width="16" height="${round(h)}" rx="5" fill="${fill}" stroke="${stroke}" stroke-width="1.4"/>`;
}

function renderBranch(label, x, y, w, centers) {
  const safe = label.replace(/^(alt|else|opt|par|and|loop|when)\s+/i, (match) => match.trim().toLowerCase() + " ");
  const maxGapW = centers.length > 1 ? Math.max(170, centers[1] - centers[0] - 40) : 320;
  const pillW = Math.min(maxGapW, Math.max(170, Math.min(360, safe.length * 7.2 + 42)));
  const lines = wrap(safe, Math.max(18, Math.floor((pillW - 42) / 7.2))).slice(0, 2);
  const pillH = lines.length > 1 ? 42 : 26;
  const pillY = y - (lines.length > 1 ? 22 : 14);
  const textStart = lines.length > 1 ? pillY + 13 : y - 1;
  const firstGapMid = centers.length > 1 ? (centers[0] + centers[1]) / 2 : x + 24 + pillW / 2;
  const pillX = Math.min(x + w - pillW - 24, Math.max(x + 24, firstGapMid - pillW / 2));
  const labelText = lines.map((line, index) => `<text class="detail" x="${round(pillX + pillW / 2)}" y="${round(textStart + index * 15)}" text-anchor="middle" dominant-baseline="middle">${esc(line)}</text>`).join("");
  return `<g><rect class="altBox" x="${round(x)}" y="${round(y)}" width="${round(w)}" height="46" rx="16"/><rect class="labelPill branchPill" x="${round(pillX)}" y="${round(pillY)}" width="${round(pillW)}" height="${pillH}" rx="7"/>${labelText}</g>`;
}

function renderMessage(id, fromX, toX, y, label, color, isReturn, number) {
  if (Math.abs(fromX - toX) < 2) return renderSelfMessage(id, fromX, y, label, color, number);
  const stroke = palette[color]?.[2] || palette.gray[2];
  const available = Math.max(260, Math.abs(toX - fromX) - 28);
  const maxChars = Math.max(34, Math.floor((available - 108) / 7.3));
  const lines = wrapMessageLabel(label, maxChars).slice(0, 2);
  const pillH = lines.length > 1 ? 42 : 26;
  const pillW = Math.max(230, Math.min(available, Math.max(...lines.map((line) => line.length)) * 7.3 + 108));
  const mid = (fromX + toX) / 2;
  const pillX = mid - pillW / 2;
  const pillY = y - pillH - 17;
  const textStart = lines.length > 1 ? pillY + 13 : pillY + pillH / 2 + 1;
  const text = lines.map((line, index) => `<text class="detail" x="${round(mid + 18)}" y="${round(textStart + index * 15)}" text-anchor="middle" dominant-baseline="middle">${esc(line)}</text>`).join("");
  const kind = isReturn ? "seqReturn" : "seq";
  return `<g id="${id}"><rect class="labelPill" x="${round(pillX)}" y="${round(pillY)}" width="${round(pillW)}" height="${pillH}" rx="7"/><circle cx="${round(pillX + 19)}" cy="${round(pillY + pillH / 2)}" r="12" fill="${stroke}"/><text class="detail" x="${round(pillX + 19)}" y="${round(pillY + pillH / 2 + 1)}" text-anchor="middle" dominant-baseline="middle" style="fill:#fff;font-size:12px">${number}</text>${text}<path class="${kind}" d="M${round(fromX)} ${round(y)} L${round(toX)} ${round(y)}" stroke="${stroke}" marker-end="url(#seqArrow-${color})"/></g>`;
}

function renderSelfMessage(id, x, y, label, color, number) {
  const stroke = palette[color]?.[2] || palette.gray[2];
  const lines = wrapMessageLabel(label, 52).slice(0, 2);
  const pillW = Math.max(210, Math.max(...lines.map((line) => line.length)) * 7.3 + 92);
  const pillH = lines.length > 1 ? 42 : 26;
  const pillX = x + 110;
  const pillY = y - pillH - 17;
  const textStart = lines.length > 1 ? pillY + 13 : pillY + pillH / 2 + 1;
  const text = lines.map((line, index) => `<text class="detail" x="${round(pillX + pillW / 2 + 18)}" y="${round(textStart + index * 15)}" text-anchor="middle" dominant-baseline="middle">${esc(line)}</text>`).join("");
  return `<g id="${id}"><rect class="labelPill" x="${round(pillX)}" y="${round(pillY)}" width="${round(pillW)}" height="${pillH}" rx="7"/><circle cx="${round(pillX + 19)}" cy="${round(pillY + pillH / 2)}" r="12" fill="${stroke}"/><text class="detail" x="${round(pillX + 19)}" y="${round(pillY + pillH / 2 + 1)}" text-anchor="middle" dominant-baseline="middle" style="fill:#fff;font-size:12px">${number}</text>${text}<path class="seq" d="M${round(x)} ${round(y)} L${round(x + 90)} ${round(y)} L${round(x + 90)} ${round(y + 28)} L${round(x + 18)} ${round(y + 28)}" stroke="${stroke}" marker-end="url(#seqArrow-${color})"/></g>`;
}

function wrapMessageLabel(label, maxChars) {
  const text = String(label).replace(/\s+/g, " ").trim();
  if (text.length <= maxChars) return [text];
  return rebalanceShortTail(wrap(text, maxChars));
}

function rebalanceShortTail(lines) {
  if (lines.length < 2) return lines;
  const result = [...lines];
  for (let index = 1; index < result.length; index += 1) {
    if (result[index].length > 3) continue;
    const previous = result[index - 1];
    const split = previous.lastIndexOf(" ");
    if (split <= 0) continue;
    result[index] = `${previous.slice(split + 1)} ${result[index]}`.trim();
    result[index - 1] = previous.slice(0, split);
  }
  return result.filter(Boolean);
}

function renderFooter(x, y, w, text) {
  return `<g><rect x="${x}" y="${y}" width="${w}" height="42" rx="12" fill="#FFFFFF" stroke="#D6E3EF" stroke-width="1.6"/><text class="detail" x="${x + w / 2}" y="${y + 26}" text-anchor="middle">${esc(text)}</text></g>`;
}

function extractTexts(svg) {
  return [...svg.matchAll(/<text\b([^>]*)>([\s\S]*?)<\/text>/g)].map((match) => ({
    index: match.index,
    tag: match[0],
    x: Number(match[1].match(/\bx="([-\d.]+)"/)?.[1] ?? Number.NaN),
    y: Number(match[1].match(/\by="([-\d.]+)"/)?.[1] ?? Number.NaN),
    text: decode(cleanTags(match[2])),
  })).filter((text) => text.text);
}

function extractTitle(svg) {
  return decode(svg.match(/aria-label="([^"]+)"/)?.[1] || svg.match(/<text[^>]*class="[^"]*title[^"]*"[^>]*>([\s\S]*?)<\/text>/)?.[1] || "");
}

function attrs(tag) {
  const get = (name) => Number(tag.match(new RegExp(`\\b${name}="([-\\d.]+)"`))?.[1]);
  return {
    x: get("x"),
    y: get("y"),
    w: get("width"),
    h: get("height"),
    x1: get("x1"),
    y1: get("y1"),
    x2: get("x2"),
    y2: get("y2"),
  };
}

function attrNumber(tag, name) {
  return Number(tag.match(new RegExp(`\\b${name}="([-\\d.]+)"`))?.[1]);
}

function parsePath(d) {
  const numbers = d.match(/-?\d*\.?\d+(?:e[-+]?\d+)?/gi)?.map(Number) || [];
  const points = [];
  for (let index = 0; index + 1 < numbers.length; index += 2) {
    points.push({ x: numbers[index], y: numbers[index + 1] });
  }
  return points;
}

function nearestParticipant(participants, x) {
  return participants.toSorted((a, b) => Math.abs(a.oldX - x) - Math.abs(b.oldX - x))[0];
}

function normalizeParticipant(value) {
  return String(value).replace(/\s+/g, " ").trim().replace(/^map\s+/i, "Map ");
}

function cleanLabel(value) {
  return String(value)
    .replace(/\s+/g, " ")
    .replace(/\(([^)]{38,})\)/g, "")
    .trim();
}

function titleFromFile(file) {
  return file.replace(/\.svg$/, "").split("-").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function wrap(text, max) {
  const words = String(text).split(/\s+/);
  const lines = [];
  let line = "";
  for (const word of words) {
    if ((line + " " + word).trim().length > max && line) {
      lines.push(line);
      line = word;
    } else {
      line = (line + " " + word).trim();
    }
  }
  if (line) lines.push(line);
  return lines;
}

function wrapIdentifier(text, max) {
  if (String(text).includes(" ")) return wrap(text, max);
  const parts = String(text).match(/[A-Z]?[a-z0-9]+|[A-Z]+(?=[A-Z]|$)/g) || [String(text)];
  const lines = [];
  let line = "";
  for (const part of parts) {
    if ((line + part).length > max && line) {
      lines.push(line);
      line = part;
    } else {
      line += part;
    }
  }
  if (line) lines.push(line);
  return lines;
}

function isApprovedSequence(svg) {
  return /seqArrow-blue/.test(svg)
    && /class="labelPill"/.test(svg)
    && /<circle\b/.test(svg)
    && !/id="openArrow"|class="line"|class="dashed"/.test(svg)
    && !/<rect[^>]*class="[^"]*card[^"]*"[^>]*rx="(?:1[6-9]|[2-9]\d)"/.test(svg);
}

function hasAwkwardSequenceText(svg) {
  for (const match of svg.matchAll(/<g id="m\d+">([\s\S]*?)<\/g>/g)) {
    const labels = [...match[1].matchAll(/<text\b([^>]*)>([\s\S]*?)<\/text>/g)]
      .filter((text) => !/fill:#fff/.test(text[1]))
      .map((text) => decode(cleanTags(text[2])).trim())
      .filter((text) => text && !/^\d+$/.test(text));
    if (labels.length > 1 && labels.at(-1).length <= 3) return true;
  }
  return false;
}

function hasEmptyBranchRows(svg) {
  return /branchPill/.test(svg) && (svg.match(/class="altBox"/g) || []).length > (svg.match(/<path class="seq/g) || []).length / 2;
}

function specialSequenceModel(file) {
  const models = {
    "io-okio-sequence-01.svg": {
      title: "Compression Sink One-Shot Flow",
      subtitle: "CompressableSink buffers writes and compresses once during close.",
      participants: ["Caller", "CompressableSink", "Plain Buffer", "Compressor", "Delegate Sink"],
      events: [
        ["Caller", "CompressableSink", "write(source, byteCount)"],
        ["CompressableSink", "Plain Buffer", "copy plaintext bytes"],
        ["Caller", "CompressableSink", "close()"],
        ["CompressableSink", "Compressor", "compress buffered payload"],
        ["Compressor", "CompressableSink", "compressed bytes", true],
        ["CompressableSink", "Delegate Sink", "write compressed payload"],
        ["CompressableSink", "Delegate Sink", "close delegate"],
        ["CompressableSink", "Caller", "close complete", true],
      ],
    },
    "io-okio-sequence-02.svg": {
      title: "Streaming Compression Sink Flow",
      subtitle: "StreamingCompressSink pushes bytes through a compression stream as data arrives.",
      participants: ["Caller", "StreamingCompressSink", "Compression Stream", "Compressor", "Delegate Sink"],
      events: [
        ["Caller", "StreamingCompressSink", "write(source, byteCount)"],
        ["StreamingCompressSink", "Compression Stream", "write chunk"],
        ["Compression Stream", "Compressor", "compress incrementally"],
        ["Compressor", "Delegate Sink", "flush compressed segment"],
        ["Caller", "StreamingCompressSink", "flush()"],
        ["StreamingCompressSink", "Delegate Sink", "flush delegate"],
        ["Caller", "StreamingCompressSink", "close()"],
        ["StreamingCompressSink", "Compression Stream", "finish stream"],
        ["StreamingCompressSink", "Delegate Sink", "close delegate"],
      ],
    },
    "io-okio-sequence-03.svg": {
      title: "Decompression Source One-Shot Flow",
      subtitle: "DecompressableSource decodes the delegate source once and serves reads from cache.",
      participants: ["Caller", "DecompressableSource", "Delegate Source", "Compressor", "Decoded Buffer"],
      events: [
        ["Caller", "DecompressableSource", "read(sink, byteCount)"],
        ["DecompressableSource", "Delegate Source", "read compressed bytes"],
        ["Delegate Source", "DecompressableSource", "compressed payload", true],
        ["DecompressableSource", "Compressor", "decompress payload"],
        ["Compressor", "Decoded Buffer", "store decoded bytes"],
        ["DecompressableSource", "Decoded Buffer", "copy requested bytes"],
        ["DecompressableSource", "Caller", "bytes read", true],
        ["Caller", "DecompressableSource", "next read()"],
        ["DecompressableSource", "Decoded Buffer", "serve cached decoded bytes"],
      ],
    },
    "io-okio-sequence-04.svg": {
      title: "Tink Encryption and Compression Flow",
      subtitle: "Sink decorators compose compression before encryption while keeping delegate writes ordered.",
      participants: ["Caller", "CompressableSink", "Compressed Buffer", "TinkEncryptSink", "Delegate Sink"],
      events: [
        ["Caller", "CompressableSink", "write plaintext"],
        ["CompressableSink", "Compressed Buffer", "buffer and compress"],
        ["Caller", "CompressableSink", "close()"],
        ["CompressableSink", "TinkEncryptSink", "write compressed bytes"],
        ["TinkEncryptSink", "TinkEncryptSink", "encrypt chunk"],
        ["TinkEncryptSink", "Delegate Sink", "write ciphertext"],
        ["CompressableSink", "TinkEncryptSink", "close encrypting sink"],
        ["TinkEncryptSink", "Delegate Sink", "close delegate"],
      ],
    },
  };
  const model = models[file];
  if (!model) return null;
  return {
    file,
    forceRedraw: true,
    title: model.title,
    subtitle: model.subtitle,
    participants: model.participants.map((label, index) => ({ id: slug(label), label, oldX: 100 + index * 360, color: colors[index % colors.length] })),
    events: model.events.map((event, index) => ({
      type: "message",
      index,
      from: slug(event[0]),
      to: slug(event[1]),
      label: event[2],
      return: Boolean(event[3]),
    })),
  };
}

function hasViewBoxOverflow(svg) {
  const size = svg.match(/viewBox="0 0 ([\d.]+) ([\d.]+)"/);
  if (!size) return false;
  const width = Number(size[1]);
  const height = Number(size[2]);
  const maxX = [];
  const maxY = [];
  for (const tag of svg.match(/<(?:rect|line|text)\b[^>]*>/g) || []) {
    const values = attrs(tag);
    if (!Number.isNaN(values.x) && !Number.isNaN(values.w)) maxX.push(values.x + values.w);
    if (!Number.isNaN(values.y) && !Number.isNaN(values.h)) maxY.push(values.y + values.h);
    for (const name of ["x", "x1", "x2"]) if (!Number.isNaN(values[name])) maxX.push(values[name]);
    for (const name of ["y", "y1", "y2"]) if (!Number.isNaN(values[name])) maxY.push(values[name]);
    if (tag.startsWith("<text") && !Number.isNaN(values.y)) maxY.push(values.y + 22);
  }
  for (const path of svg.match(/<path\b[^>]*\bd="([^"]+)"/g) || []) {
    for (const point of parsePath(path.match(/\bd="([^"]+)"/)?.[1] || "")) {
      maxX.push(point.x);
      maxY.push(point.y);
    }
  }
  return Math.max(...maxX, 0) > width + 2 || Math.max(...maxY, 0) > height + 2;
}

function hasSequenceLabelCrossings(svg) {
  const labels = [...svg.matchAll(/<rect[^>]*class="[^"]*labelPill[^"]*"[^>]*>/g)]
    .map((match) => ({ x: attrNumber(match[0], "x"), y: attrNumber(match[0], "y"), w: attrNumber(match[0], "width"), h: attrNumber(match[0], "height") }))
    .filter((rect) => [rect.x, rect.y, rect.w, rect.h].every((value) => !Number.isNaN(value)));
  const paths = [...svg.matchAll(/<path[^>]*class="[^"]*seq(?:Return)?[^"]*"[^>]*\bd="([^"]+)"[^>]*>/g)]
    .map((match) => parsePath(match[1]))
    .filter((points) => points.length >= 2);
  for (const points of paths) {
    for (let index = 1; index < points.length; index += 1) {
      const a = points[index - 1];
      const b = points[index];
      for (const label of labels) {
        if (segmentHitsRect(a, b, label)) return true;
      }
    }
  }
  return false;
}

function hasSequenceBranchLifelineCrossings(svg) {
  const labels = [...svg.matchAll(/<rect[^>]*class="[^"]*branchPill[^"]*"[^>]*>/g)]
    .map((match) => ({ x: attrNumber(match[0], "x"), y: attrNumber(match[0], "y"), w: attrNumber(match[0], "width"), h: attrNumber(match[0], "height") }))
    .filter((rect) => [rect.x, rect.y, rect.w, rect.h].every((value) => !Number.isNaN(value)));
  if (labels.length === 0) return false;
  const lifelines = [...svg.matchAll(/<line[^>]*class="[^"]*lifeline[^"]*"[^>]*>/g)]
    .map((match) => {
      const tag = match[0];
      return [
        { x: attrNumber(tag, "x1"), y: attrNumber(tag, "y1") },
        { x: attrNumber(tag, "x2"), y: attrNumber(tag, "y2") },
      ];
    })
    .filter((points) => points.every((point) => !Number.isNaN(point.x) && !Number.isNaN(point.y)));
  for (const [a, b] of lifelines) {
    for (const label of labels) {
      if (segmentHitsRect(a, b, label)) return true;
    }
  }
  return false;
}

function segmentHitsRect(a, b, rect) {
  if (Math.abs(a.y - b.y) < 0.5) {
    return a.y > rect.y && a.y < rect.y + rect.h && Math.max(a.x, b.x) > rect.x && Math.min(a.x, b.x) < rect.x + rect.w;
  }
  if (Math.abs(a.x - b.x) < 0.5) {
    return a.x > rect.x && a.x < rect.x + rect.w && Math.max(a.y, b.y) > rect.y && Math.min(a.y, b.y) < rect.y + rect.h;
  }
  return false;
}

function slug(value) {
  return String(value).toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "") || "participant";
}

function cleanTags(value) {
  return String(value || "").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
}

function decode(value) {
  return cleanTags(value).replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"');
}

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function round(value) {
  return Math.round(value * 10) / 10;
}
