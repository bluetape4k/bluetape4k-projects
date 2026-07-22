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
