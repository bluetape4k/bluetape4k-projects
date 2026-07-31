import assert from "node:assert/strict";
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { spawnSync } from "node:child_process";
import test from "node:test";
import { fileURLToPath } from "node:url";

const scriptsDir = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = dirname(scriptsDir);
const validator = join(scriptsDir, "validate-readme-diagram-assets.mjs");

test("validator skips card-like groups without shape geometry", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(join(diagramDir, "geometry-less-card.svg"), `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600">
  <title>Geometry-less card group</title>
  <g>
    <path class="card" d="M 40 40 L 120 40" />
  </g>
</svg>
`, "utf8");

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  assert.doesNotMatch(result.stderr, /TypeError/);
  assert.match(result.stderr, /readme diagram validation: total=1 failed=0 report=/);

  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(validation.failed, 0);
  assert.equal(validation.rows[0].cards, 0);
});

test("validator excludes decorative icon lines from diagram routes", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(join(diagramDir, "decorative-icon-line.svg"), `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600">
  <title>Decorative icon line</title>
  <rect x="24" y="24" width="752" height="552" class="frame" />
  <rect x="72" y="170" width="300" height="350" class="source-panel" />
  <rect x="428" y="170" width="300" height="350" class="sink-panel" />
  <g id="source"><rect class="card" x="100" y="300" width="100" height="80" /></g>
  <g id="target"><rect class="card" x="600" y="300" width="100" height="80" /></g>
  <path class="flow-green" data-from="source" data-to="target" d="M 200 340 H 600" />
  <g transform="translate(340 200)" aria-hidden="true">
    <path d="M -10 -8 H 10" class="icon-line-green" />
    <path d="M -8 8 H 8" class="icon-line" />
  </g>
</svg>
`, "utf8");

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(validation.failed, 0);
  assert.equal(validation.rows[0].cards, 2);
  assert.equal(validation.rows[0].paths, 1);
  assert.deepEqual(validation.rows[0].failures, []);
});

test("validator preserves relationship endpoint checks across Q and q bends", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(join(diagramDir, "quadratic-routes.svg"), `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 440 240">
  <title>Quadratic route endpoint validation</title>
  <g id="source"><rect class="card" x="40" y="40" width="100" height="80" /></g>
  <g id="target"><rect class="card" x="300" y="40" width="100" height="80" /></g>
  <path class="route" data-from="source" data-to="target" d="M 140 70 Q 220 10 280 70" />
  <path class="route" data-from="target" data-to="source" d="M 300 90 q -80 60 -140 0" />
</svg>
`, "utf8");

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 1, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.rows[0].paths, 2);
  assert.ok(validation.rows[0].failures.includes("disconnected/floating connector endpoints=2"));
});

test("validator keeps large routed nodes in endpoint geometry", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(join(diagramDir, "large-routed-node.svg"), `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 500">
  <title>Large routed node</title>
  <g id="source"><rect class="card" x="40" y="40" width="600" height="350" /></g>
  <g id="target"><rect class="card" x="800" y="160" width="120" height="100" /></g>
  <path class="route" data-from="source" data-to="target" d="M 640 210 H 800" />
</svg>
`, "utf8");

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.rows[0].cards, 2);
  assert.deepEqual(validation.rows[0].failures, []);
});

test("canonical infra Lettuce diagram has no card text overflow", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const diagramName = "infra-lettuce-diagram-01.svg";
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(
    join(diagramDir, diagramName),
    readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
    "utf8",
  );

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(validation.failed, 0);
  assert.deepEqual(validation.rows[0].failures, []);
});

test("canonical bluetape4k core overview has balanced content margins", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const diagramName = "bluetape4k-core-diagram-01.svg";
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(
    join(diagramDir, diagramName),
    readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
    "utf8",
  );

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(validation.failed, 0);
  assert.deepEqual(validation.rows[0].failures, []);
});

test("canonical foundation diagram validation slice passes", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  const diagramNames = [
    "bluetape4k-bom-diagram-01.svg",
    "bluetape4k-coroutines-diagram-01.svg",
    "bluetape4k-coroutines-diagram-03.svg",
    "bluetape4k-logging-diagram-01.svg",
    "bluetape4k-logging-diagram-02.svg",
  ];
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  for (const diagramName of diagramNames) {
    writeFileSync(
      join(diagramDir, diagramName),
      readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
      "utf8",
    );
  }

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 5);
  assert.equal(validation.failed, 0);
  assert.deepEqual(validation.rows.flatMap((row) => row.failures), []);
});

test("canonical cache diagram validation slice passes", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  const diagramNames = [
    "cache-cache-core-diagram-01.svg",
    "cache-cache-core-diagram-02.svg",
    "cache-cache-core-diagram-03.svg",
    "cache-cache-core-diagram-04.svg",
    "cache-cache-core-diagram-05.svg",
    "cache-cache-core-diagram-06.svg",
    "cache-cache-hazelcast-diagram-02.svg",
    "cache-cache-lettuce-diagram-01.svg",
    "cache-cache-lettuce-diagram-02.svg",
    "cache-cache-lettuce-diagram-04.svg",
    "cache-hibernate-cache-lettuce-diagram-02.svg",
  ];
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  for (const diagramName of diagramNames) {
    writeFileSync(
      join(diagramDir, diagramName),
      readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
      "utf8",
    );
  }

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 11);
  assert.equal(validation.failed, 0);
  assert.deepEqual(validation.rows.flatMap((row) => row.failures), []);
});

test("canonical IO client diagram validation slice passes", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  const diagramNames = [
    "io-feign-diagram-01.svg",
    "io-feign-diagram-03.svg",
    "io-http-diagram-01.svg",
    "io-http-diagram-02.svg",
    "io-http-diagram-03.svg",
    "io-http-diagram-04.svg",
    "io-http-diagram-05.svg",
    "io-retrofit2-diagram-01.svg",
    "io-retrofit2-diagram-02.svg",
    "io-vertx-diagram-01.svg",
    "io-vertx-diagram-02.svg",
  ];
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  for (const diagramName of diagramNames) {
    writeFileSync(
      join(diagramDir, diagramName),
      readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
      "utf8",
    );
  }

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, diagramNames.length);
  assert.equal(validation.failed, 0);
  assert.deepEqual(validation.rows.flatMap((row) => row.failures), []);
});

test("canonical infrastructure diagram validation slice passes", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  const diagramNames = [
    "infra-bucket4j-diagram-01.svg",
    "infra-elasticsearch-diagram-02.svg",
    "infra-kafka-logback-diagram-01.svg",
    "infra-lettuce-diagram-02.svg",
    "infra-micrometer-diagram-02.svg",
    "infra-micrometer-diagram-03.svg",
    "infra-nats-diagram-01.svg",
    "infra-opentelemetry-diagram-03.svg",
    "infra-pulsar-diagram-01.svg",
    "infra-redisson-diagram-01.svg",
    "infra-redisson-diagram-02.svg",
  ];
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  for (const diagramName of diagramNames) {
    writeFileSync(
      join(diagramDir, diagramName),
      readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
      "utf8",
    );
  }

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, diagramNames.length);
  assert.equal(validation.failed, 0);
  assert.deepEqual(validation.rows.flatMap((row) => row.failures), []);
});

test("canonical server core diagram validation slice passes", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  const diagramNames = [
    "ktor-core-architecture-01.svg",
    "ktor-observability-component-01.svg",
    "ktor-resilience4j-flow-01.svg",
    "spring-boot-core-diagram-01.svg",
    "spring-boot-core-diagram-02.svg",
  ];
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  for (const diagramName of diagramNames) {
    writeFileSync(
      join(diagramDir, diagramName),
      readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
      "utf8",
    );
  }

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, diagramNames.length);
  assert.equal(validation.failed, 0);
  assert.deepEqual(validation.rows.flatMap((row) => row.failures), []);
});

test("canonical Okio async hierarchy has balanced content margins", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const diagramName = "io-okio-diagram-03.svg";
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(
    join(diagramDir, diagramName),
    readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
    "utf8",
  );

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(validation.failed, 0);
  assert.deepEqual(validation.rows[0].failures, []);
});

test("canonical Okio compression factory has balanced content margins", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const diagramName = "io-okio-diagram-04.svg";
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(
    join(diagramDir, diagramName),
    readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
    "utf8",
  );

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(validation.failed, 0);
  assert.deepEqual(validation.rows[0].failures, []);
});

test("canonical Ktor OpenAPI route helpers have balanced content margins", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const diagramName = "ktor-openapi-routes-01.svg";
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(
    join(diagramDir, diagramName),
    readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
    "utf8",
  );

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.notEqual(result.status, null, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(
    validation.rows[0].failures.some((failure) => failure.startsWith("content vertical margin imbalance=")),
    false,
    validation.rows[0].failures.join("\n"),
  );
});

test("canonical utils math feature structure exposes routed content bounds", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const diagramName = "utils-math-diagram-01.svg";
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(
    join(diagramDir, diagramName),
    readFileSync(join(repositoryRoot, "docs/images/readme-diagrams", diagramName), "utf8"),
    "utf8",
  );

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: { ...process.env, DIAGRAM_VALIDATION_REPORT: report },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(validation.failed, 0);
  assert.equal(validation.rows[0].cards, 10);
  assert.equal(validation.rows[0].paths, 8);
  assert.deepEqual(validation.rows[0].failures, []);
});

test("canonical bluetape4k core overview uses explicit color arrow markers", () => {
  const svg = readFileSync(
    join(repositoryRoot, "docs/images/readme-diagrams/bluetape4k-core-diagram-01.svg"),
    "utf8",
  );

  assert.doesNotMatch(svg, /context-stroke/);
  assert.doesNotMatch(svg, /markerUnits="strokeWidth"/);

  const markerColors = new Map(
    [...svg.matchAll(/<marker id="(arrow-[^"]+)"[^>]*><path[^>]*fill="([^"]+)"/g)]
      .map((match) => [match[1], match[2]]),
  );
  const routes = [...svg.matchAll(/<path class="route(?: dashed)?"[^>]*stroke="([^"]+)"[^>]*marker-end="url\(#([^)]+)\)"/g)];

  assert.equal(routes.length, 6);
  for (const [, stroke, marker] of routes) {
    assert.equal(markerColors.get(marker), stroke);
  }
});

test("target filter validates only exact requested filenames", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(join(diagramDir, "selected.svg"), `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600">
  <title>Selected exact target</title>
</svg>
`, "utf8");
  writeFileSync(join(diagramDir, "unselected.svg"), `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600">
  <title>Unselected invalid flow</title>
  <g id="a"><rect class="card" x="40" y="40" width="100" height="80" /></g>
  <g id="b"><rect class="card" x="200" y="40" width="100" height="80" /></g>
  <g id="c"><rect class="card" x="360" y="40" width="100" height="80" /></g>
</svg>
`, "utf8");

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: {
      ...process.env,
      DIAGRAM_VALIDATION_REPORT: report,
      DIAGRAM_VALIDATION_TARGETS: "selected.svg",
    },
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(validation.failed, 0);
  assert.equal(validation.rows[0].file, "selected.svg");
});

test("target filter fails when an exact filename is missing", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: {
      ...process.env,
      DIAGRAM_VALIDATION_REPORT: report,
      DIAGRAM_VALIDATION_TARGETS: "missing.svg",
    },
  });

  assert.equal(result.status, 1, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(validation.failed, 1);
  assert.deepEqual(validation.rows[0].failures, ["missing validation target: missing.svg"]);
});

test("target filter rejects duplicate filenames", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  writeFileSync(join(diagramDir, "duplicate.svg"), `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600">
  <title>Duplicate exact target</title>
</svg>
`, "utf8");

  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env: {
      ...process.env,
      DIAGRAM_VALIDATION_REPORT: report,
      DIAGRAM_VALIDATION_TARGETS: "duplicate.svg,duplicate.svg",
    },
  });

  assert.equal(result.status, 1, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 1);
  assert.equal(validation.failed, 1);
  assert.deepEqual(validation.rows[0].failures, ["duplicate validation target: duplicate.svg"]);
});

test("unset target filter preserves the default full scan", (context) => {
  const root = mkdtempSync(join(tmpdir(), "readme-diagram-validator-"));
  const diagramDir = join(root, "docs/images/readme-diagrams");
  const report = join(root, "diagram-validation-report.json");
  context.after(() => rmSync(root, { recursive: true, force: true }));

  mkdirSync(diagramDir, { recursive: true });
  for (const file of ["first.svg", "second.svg"]) {
    writeFileSync(join(diagramDir, file), `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600">
  <title>${file}</title>
</svg>
`, "utf8");
  }

  const env = { ...process.env, DIAGRAM_VALIDATION_REPORT: report };
  delete env.DIAGRAM_VALIDATION_TARGETS;
  const result = spawnSync(process.execPath, [validator], {
    cwd: root,
    encoding: "utf8",
    env,
  });

  assert.equal(result.status, 0, result.stderr);
  const validation = JSON.parse(readFileSync(report, "utf8"));
  assert.equal(validation.total, 2);
  assert.equal(validation.failed, 0);
  assert.deepEqual(validation.rows.map((row) => row.file), ["first.svg", "second.svg"]);
});
