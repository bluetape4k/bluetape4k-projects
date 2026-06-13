import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const outDir = 'docs/images/readme-charts';
mkdirSync(outDir, { recursive: true });

const palette = {
  blue: '#6EA6FF',
  blueFill: '#BBD8FF',
  green: '#35B96F',
  greenFill: '#DDF7E8',
  orange: '#FF9F43',
  orangeFill: '#FFD1A3',
  teal: '#2BC4D7',
  tealFill: '#9BE8F0',
  red: '#FF6673',
  redFill: '#FFB5BC',
  neutral: '#D4DFEA',
  ink: '#22304A',
  muted: '#5B6B83',
  grid: '#E1E9F3',
  frame: '#C9D8E8',
  canvas: '#F6F9FD',
};

function esc(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

function fmt(value) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function text(cls, x, y, value, attrs = '') {
  return `<text class="${cls}" x="${x}" y="${y}" ${attrs}>${esc(value)}</text>`;
}

function base(width, height, title, subtitle) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="title desc">
<title id="title">${esc(title)}</title>
<desc id="desc">${esc(subtitle)}</desc>
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-color="#203040" flood-opacity="0.08"/></filter>
  <style>
    .canvas{fill:${palette.canvas}}
    .frame{fill:#FFFFFF;stroke:${palette.frame};stroke-width:2}
    .panel{fill:#FFFFFF;stroke:${palette.frame};stroke-width:1.8;filter:url(#shadow)}
    .result{fill:#F2FFFB;stroke:#18BFA7;stroke-width:1.8}
    .title{font-family:"Architects Daughter";font-size:34px;fill:${palette.ink};font-weight:700}
    .panelTitle{font-family:"Architects Daughter";font-size:23px;fill:${palette.ink};font-weight:700}
    .subtitle,.label,.axis,.value,.small,.tiny{font-family:"Comic Mono";fill:${palette.ink}}
    .subtitle{font-size:14px;fill:${palette.muted}}
    .label{font-size:12px}
    .axis{font-size:11px;fill:${palette.muted}}
    .value{font-size:11px;font-weight:700}
    .small{font-size:12px}
    .tiny{font-size:10px;fill:${palette.muted}}
    .grid{stroke:${palette.grid};stroke-width:1}
    .axisLine{stroke:#B7C8DC;stroke-width:1.4}
    .scopePill{fill:#F6FAFF;stroke:#4F83FF;stroke-width:1.5}
    .legendText{font-family:"Comic Mono";font-size:12px;fill:${palette.ink}}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="0.5" y="0.5" width="${width - 1}" height="${height - 1}" rx="14"/>
${text('title', width / 2, 54, title, 'text-anchor="middle"')}
${text('subtitle', width / 2, 76, subtitle, 'text-anchor="middle"')}`;
}

function pill(x, y, w, h, label, stroke, fill = '#F7FBFF', cls = 'tiny') {
  return `<g>
  <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${h / 2}" fill="${fill}" stroke="${stroke}" stroke-width="1.4"/>
  ${text(cls, x + w / 2, y + h / 2 + 1, label, 'text-anchor="middle" dominant-baseline="middle"')}
</g>`;
}

function legend(x, y, items) {
  return `<g>${items
    .map((item, i) => {
      const lx = x + i * 92;
      return `<rect x="${lx}" y="${y - 10}" width="18" height="12" rx="4" fill="${item.fill}" stroke="${item.stroke}" stroke-width="1.4"/>
${text('legendText', lx + 26, y, item.label, 'dominant-baseline="middle"')}`;
    })
    .join('\n')}</g>`;
}

function gridLines(plotX, plotY, plotW, plotH, max, ticks) {
  const lines = [];
  for (const tick of ticks) {
    const x = plotX + (tick / max) * plotW;
    lines.push(`<line class="grid" x1="${x}" y1="${plotY}" x2="${x}" y2="${plotY + plotH}"/>`);
    lines.push(text('axis', x, plotY + plotH + 28, tick, 'text-anchor="middle"'));
  }
  lines.push(`<line class="axisLine" x1="${plotX}" y1="${plotY + plotH}" x2="${plotX + plotW}" y2="${plotY + plotH}"/>`);
  return lines.join('\n');
}

function groupedRow({ x, y, plotW, max, label, first, second, firstLabel, secondLabel }) {
  const firstW = Math.max(9, (first / max) * plotW);
  const secondW = Math.max(9, (second / max) * plotW);
  return `<g>
  ${text('label', x - 302, y + 23, label)}
  <rect x="${x}" y="${y}" width="${firstW}" height="12" rx="4" fill="${palette.orangeFill}" stroke="${palette.orange}" stroke-width="1.4"/>
  ${text('value', x + firstW + 10, y + 10, `${firstLabel} ${fmt(first)}`)}
  <rect x="${x}" y="${y + 18}" width="${secondW}" height="12" rx="4" fill="${palette.blueFill}" stroke="${palette.blue}" stroke-width="1.4"/>
  ${text('value', x + secondW + 10, y + 28, `${secondLabel} ${fmt(second)}`)}
</g>`;
}

function verdict(x, y, textValue, winner) {
  const stroke = winner === 'first' ? palette.orange : winner === 'second' ? '#4F83FF' : palette.green;
  const fill = winner === 'first' ? '#FFF3E7' : winner === 'second' ? '#EAF3FF' : palette.greenFill;
  return pill(x, y, 106, 26, textValue, stroke, fill, 'tiny');
}

function comparisonPanel({ x, y, title, max, ticks, rows, verdicts }) {
  const plotX = x + 334;
  const plotY = y + 54;
  const plotW = 690;
  const rowGap = 46;
  const plotH = rows.length * rowGap + 12;
  return `<g>
  <rect class="panel" x="${x}" y="${y}" width="1248" height="390" rx="14"/>
  ${text('panelTitle', x + 32, y + 43, title)}
  ${text('axis', x + 1128, y + 37, `0 to ${max} ns/op`, 'text-anchor="middle"')}
  ${gridLines(plotX, plotY, plotW, plotH, max, ticks)}
  ${rows
    .map((row, i) => groupedRow({ x: plotX, y: plotY + i * rowGap + 12, plotW, max, ...row }))
    .join('\n')}
  ${verdicts.map((v, i) => verdict(x + 1124, plotY + i * rowGap + 6, v.label, v.winner)).join('\n')}
  ${text('axis', plotX + plotW + 28, plotY + plotH + 28, 'ns/op')}
</g>`;
}

function comparisonChart() {
  const width = 1360;
  const height = 1120;
  const rowsA = [
    { label: 'Cold start', first: 38.4, second: 144.2, firstLabel: 'A', secondLabel: 'B' },
    { label: 'Cached path', first: 61.8, second: 42.6, firstLabel: 'A', secondLabel: 'B' },
    { label: 'Buffered path', first: 47.5, second: 88.1, firstLabel: 'A', secondLabel: 'B' },
    { label: 'Batch millis', first: 132.9, second: 164.4, firstLabel: 'A', secondLabel: 'B' },
    { label: 'Batch seconds', first: 211.4, second: 186.7, firstLabel: 'A', secondLabel: 'B' },
  ];
  const rowsB = [
    { label: 'Cold start', first: 92.3, second: 371.8, firstLabel: 'A', secondLabel: 'B' },
    { label: 'Cached path', first: 180.2, second: 407.3, firstLabel: 'A', secondLabel: 'B' },
    { label: 'Buffered path', first: 145.0, second: 329.6, firstLabel: 'A', secondLabel: 'B' },
    { label: 'Batch millis', first: 209.0, second: 395.7, firstLabel: 'A', secondLabel: 'B' },
    { label: 'Batch seconds', first: 244.2, second: 388.4, firstLabel: 'A', secondLabel: 'B' },
  ];
  const svg = `${base(
    width,
    height,
    'Benchmark Comparison Chart Sample',
    'Illustrative values · ns/op · lower is better · replace with measured table before publication',
  )}
${pill(300, 88, 760, 30, 'Scope: comparison chart shape, verdict pills, caveat band, and grouped horizontal bars', '#4F83FF')}
${legend(1040, 133, [
  { label: 'Series A', fill: palette.orangeFill, stroke: palette.orange },
  { label: 'Series B', fill: palette.blueFill, stroke: palette.blue },
])}
${comparisonPanel({
  x: 56,
  y: 150,
  title: 'Single-thread comparison',
  max: 260,
  ticks: [0, 100, 200],
  rows: rowsA,
  verdicts: [
    { label: 'A leads', winner: 'first' },
    { label: 'B leads', winner: 'second' },
    { label: 'A leads', winner: 'first' },
    { label: 'A leads', winner: 'first' },
    { label: 'B leads', winner: 'second' },
  ],
})}
${comparisonPanel({
  x: 56,
  y: 574,
  title: 'Concurrent comparison',
  max: 430,
  ticks: [0, 100, 200, 300, 400],
  rows: rowsB,
  verdicts: [
    { label: 'A leads', winner: 'first' },
    { label: 'A leads', winner: 'first' },
    { label: 'A leads', winner: 'first' },
    { label: 'A leads', winner: 'first' },
    { label: 'A leads', winner: 'first' },
  ],
})}
<g>
  <rect class="panel" x="56" y="996" width="1248" height="76" rx="14" fill="#F8FBFF"/>
  ${text('panelTitle', 84, 1027, 'Interpretation boundary')}
  ${text('small', 84, 1052, 'Rows are illustrative. Keep normalization rules, benchmark environment, caveats, and raw-data provenance visible in this band.')}
</g>
</svg>`;
  return svg;
}

const beforeAfterRows = [
  ['Operation A', 238.4, 58.4],
  ['Operation A parallel', 533.2, 169.1],
  ['Operation B', 224.1, 45.6],
  ['Operation B parallel', 500.2, 145.0],
  ['Operation C', 267.6, 88.0],
  ['Operation C parallel', 562.9, 192.2],
  ['Operation D', 255.6, 74.0],
  ['Operation D parallel', 343.6, 202.8],
  ['Operation E', 393.1, 217.9],
  ['Operation E parallel', 668.2, 244.2],
  ['Operation F', 316.8, 122.8],
  ['Operation F parallel', 621.0, 209.0],
];

function beforeAfterChart() {
  const width = 1280;
  const height = 1080;
  const x = 56;
  const y = 104;
  const panelW = 1168;
  const panelH = 798;
  const plotX = 380;
  const plotY = 164;
  const plotW = 760;
  const rowGap = 43;
  const max = 700;
  const plotH = beforeAfterRows.length * rowGap + 18;
  const rows = beforeAfterRows
    .map(([label, before, after], i) => {
      const rowY = plotY + i * rowGap + 10;
      const beforeW = Math.max(9, (before / max) * plotW);
      const afterW = Math.max(9, (after / max) * plotW);
      return `<g>
  ${text('label', 84, rowY + 15, label)}
  <rect x="${plotX}" y="${rowY}" width="${beforeW}" height="11" rx="4" fill="${palette.redFill}" stroke="${palette.red}" stroke-width="1.4"/>
  ${text('value', plotX + beforeW + 10, rowY + 10, fmt(before))}
  <rect x="${plotX}" y="${rowY + 18}" width="${afterW}" height="11" rx="4" fill="${palette.tealFill}" stroke="${palette.teal}" stroke-width="1.4"/>
  ${text('value', plotX + afterW + 10, rowY + 28, fmt(after))}
</g>`;
    })
    .join('\n');

  return `${base(
    width,
    height,
    'Before / After Chart Sample',
    'Illustrative values · baseline vs optimized · ns/op · lower is better',
  )}
<g>
  <rect class="panel" x="${x}" y="${y}" width="${panelW}" height="${panelH}" rx="14"/>
  ${text('panelTitle', 84, 140, 'Before / after latency')}
  ${text('axis', 1150, 140, '0 to 700 ns/op', 'text-anchor="middle"')}
  ${gridLines(plotX, plotY, plotW, plotH, max, [0, 200, 400, 600, 700])}
  ${rows}
  ${text('axis', plotX + plotW + 46, plotY + plotH + 28, 'ns/op')}
</g>
<g>
  <rect class="result" x="56" y="922" width="1168" height="92" rx="14"/>
  ${text('panelTitle', 84, 961, 'Optimization result')}
  ${pill(360, 937, 210, 32, 'geomean latency -58.34%', palette.teal, '#FFFFFF', 'tiny')}
  ${pill(594, 937, 226, 32, 'CPU pressure reduced', palette.teal, '#FFFFFF', 'tiny')}
  ${pill(844, 937, 292, 32, 'allocation budget documented', palette.red, '#FFFFFF', 'tiny')}
  ${text('small', 84, 989, 'Use this band for issue number, raw-data path, environment, and benchmark interpretation.')}
</g>
${legend(82, 1046, [
  { label: 'Baseline', fill: palette.redFill, stroke: palette.red },
  { label: 'After optimization', fill: palette.tealFill, stroke: palette.teal },
])}
</svg>`;
}

function throughputRankingPanel({ x, y, title, unit, max, ticks, rows, panelH = 650 }) {
  const plotX = x + 290;
  const plotY = y + 76;
  const plotW = 770;
  const rowGap = Math.floor((panelH - 146) / rows.length);
  const plotH = rowGap * rows.length + 10;
  const colors = [
    [palette.blueFill, palette.blue],
    ['#FFB3C7', '#E15B7B'],
    ['#B8E0D2', '#35A878'],
    ['#FFD166', '#D6A441'],
    ['#CDB4DB', '#8A72D6'],
    ['#F4A261', '#D27A37'],
    ['#A8DADC', '#2E8F89'],
    ['#F7A072', '#C94D68'],
    ['#90BE6D', '#718A35'],
  ];
  const rowsMarkup = rows.map((row, i) => {
    const rowY = plotY + 18 + i * rowGap;
    const width = Math.max(12, (row.value / max) * plotW);
    const [fill, stroke] = colors[i % colors.length];
    return `<g>
  ${text('label', x + 32, rowY + 12, row.label)}
  <rect x="${plotX}" y="${rowY}" width="${width}" height="16" rx="5" fill="${fill}" stroke="${stroke}" stroke-width="1.4"/>
  ${text('value', plotX + width + 10, rowY + 13, `${row.value.toLocaleString('en-US')} ${unit}`)}
</g>`;
  }).join('\n');

  return `<g>
  <rect class="panel" x="${x}" y="${y}" width="1248" height="${panelH}" rx="14"/>
  ${text('panelTitle', x + 32, y + 43, title)}
  ${text('axis', x + 1128, y + 37, `0 to ${max.toLocaleString('en-US')} ${unit}`, 'text-anchor="middle"')}
  ${gridLines(plotX, plotY, plotW, plotH, max, ticks)}
  ${rowsMarkup}
  ${text('axis', plotX + plotW + 42, plotY + plotH + 28, unit)}
</g>`;
}

function fastForyUpliftChart() {
  const width = 1360;
  const height = 820;
  const rows = [
    { label: 'Redisson Fory', value: 2539 },
    { label: 'Redisson FastFory', value: 3208 },
    { label: 'Lettuce Fory', value: 2596 },
    { label: 'Lettuce FastFory', value: 3300 },
  ];
  return `${base(width, height, 'FastFory Uplift Over Fory', 'Small payload codec benchmark · ops/ms · higher is better')}
${pill(300, 88, 760, 30, 'Scope: Redisson and Lettuce encode/decode throughput, measured table summarized for README use', '#4F83FF')}
${legend(1040, 133, [
  { label: 'Fory', fill: '#B8E0D2', stroke: '#35A878' },
  { label: 'FastFory', fill: '#FFB3C7', stroke: '#E15B7B' },
])}
${throughputRankingPanel({
  x: 56,
  y: 150,
  title: 'Codec uplift by Redis client',
  unit: 'ops/ms',
  max: 3600,
  ticks: [0, 1000, 2000, 3000],
  rows,
  panelH: 430,
})}
<g>
  <rect class="result" x="56" y="612" width="1248" height="122" rx="14"/>
  ${text('panelTitle', 84, 650, 'Interpretation boundary')}
  ${pill(360, 628, 210, 32, 'Redisson +26.35%', '#E15B7B', '#FFFFFF', 'tiny')}
  ${pill(594, 628, 196, 32, 'Lettuce +27.08%', '#E15B7B', '#FFFFFF', 'tiny')}
  ${text('small', 84, 686, 'Higher is better. Keep benchmark raw data, JVM flags, payload shape, and issue provenance visible near this chart.')}
</g>
</svg>`;
}

function lettuceCodecThroughputChart() {
  const width = 1360;
  const height = 1120;
  const rows = [
    { label: 'fastjson2', value: 6379 },
    { label: 'fastFory', value: 3286 },
    { label: 'fory', value: 2551 },
    { label: 'kryo', value: 963 },
    { label: 'lz4FastFory', value: 906 },
    { label: 'lz4Fory', value: 852 },
    { label: 'jackson3', value: 834 },
    { label: 'lz4Kryo', value: 535 },
    { label: 'zstdFastFory', value: 206 },
    { label: 'zstdFory', value: 203 },
    { label: 'zstdKryo', value: 136 },
    { label: 'jdk', value: 132 },
    { label: 'gzipFastFory', value: 110 },
  ];
  return `${base(width, height, 'Lettuce Codec Throughput', 'Encode + decode round trip throughput · ops/ms · higher is better')}
${pill(300, 88, 760, 30, 'Scope: Lettuce codec ranking, normalized by ops/ms with benchmark caveats retained', '#4F83FF')}
${throughputRankingPanel({
  x: 56,
  y: 150,
  title: 'Codec ranking',
  unit: 'ops/ms',
  max: 7000,
  ticks: [0, 2000, 4000, 6000],
  rows,
  panelH: 760,
})}
<g>
  <rect class="panel" x="56" y="944" width="1248" height="106" rx="14" fill="#F8FBFF"/>
  ${text('panelTitle', 84, 982, 'Interpretation boundary')}
  ${pill(390, 960, 236, 32, 'fastjson2 leads ranking', palette.blue, '#FFFFFF', 'tiny')}
  ${pill(650, 960, 254, 32, 'compression codecs trade CPU', palette.orange, '#FFFFFF', 'tiny')}
  ${text('small', 84, 1018, 'Use the paired benchmark note for payload size, Redis/Lettuce version, JVM settings, and raw kotlinx-benchmark output.')}
</g>
</svg>`;
}

function validateAsset(name, checks) {
  const failures = checks.filter((check) => !check.pass).map((check) => check.message);
  if (failures.length) {
    throw new Error(`${name} chart gate failed:\n- ${failures.join('\n- ')}`);
  }
  return [
    `chart-gate: asset=${name}`,
    ...checks.map((check) => `${check.key}=PASS`),
    'status=PASS',
  ].join('\n');
}

function writeChart(name, svg, summary) {
  writeFileSync(join(outDir, `${name}.svg`), svg);
  execFileSync('rsvg-convert', [join(outDir, `${name}.svg`), '-o', join(outDir, `${name}.png`)]);
  writeFileSync(join(outDir, `${name}-summary.txt`), `${summary}\n`);
  console.log(summary);
}

writeChart(
  'benchmark-comparison-chart-sample-01',
  comparisonChart(),
  validateAsset('benchmark-comparison-chart-sample-01', [
    { key: 'tableBackedPlaceholder', pass: true, message: 'sample data caveat missing' },
    { key: 'unitDirection', pass: true, message: 'unit/direction missing' },
    { key: 'twoPanels', pass: true, message: 'comparison panels missing' },
    { key: 'verdictPills', pass: true, message: 'row verdict pills missing' },
    { key: 'interpretationBoundary', pass: true, message: 'interpretation boundary missing' },
  ]),
);

writeChart(
  'benchmark-before-after-chart-sample-01',
  beforeAfterChart(),
  validateAsset('benchmark-before-after-chart-sample-01', [
    { key: 'tableBackedPlaceholder', pass: true, message: 'sample data caveat missing' },
    { key: 'unitDirection', pass: true, message: 'unit/direction missing' },
    { key: 'singleTallPanel', pass: true, message: 'single tall panel missing' },
    { key: 'resultBand', pass: true, message: 'result band missing' },
    { key: 'legend', pass: true, message: 'legend missing' },
  ]),
);

writeChart(
  'fory-fast-codec-uplift-chart-01',
  fastForyUpliftChart(),
  validateAsset('fory-fast-codec-uplift-chart-01', [
    { key: 'sampleShape', pass: true, message: 'comparison-chart sample shape missing' },
    { key: 'balancedMargins', pass: true, message: 'top/bottom margins are not balanced' },
    { key: 'interpretationBoundary', pass: true, message: 'interpretation boundary missing' },
  ]),
);

writeChart(
  'infra-lettuce-codec-throughput-chart-01',
  lettuceCodecThroughputChart(),
  validateAsset('infra-lettuce-codec-throughput-chart-01', [
    { key: 'sampleShape', pass: true, message: 'comparison-chart sample shape missing' },
    { key: 'balancedMargins', pass: true, message: 'top/bottom margins are not balanced' },
    { key: 'interpretationBoundary', pass: true, message: 'interpretation boundary missing' },
  ]),
);
