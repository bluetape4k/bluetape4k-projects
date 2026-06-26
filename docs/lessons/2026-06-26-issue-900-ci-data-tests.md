# Lessons Learned - CI Data Tests (2026-06-26)

Related issue: #900
Affected workflow: `.github/workflows/ci.yml`

## L1: Path filters need matching test jobs and summary needs

### Problem

The CI workflow compiled data modules during the build job, but it had no `data` path-filter output and no `Test / Data`
job. Direct `data/**` changes could therefore merge without data-module test coverage in PR CI.

### Lesson

When a CI path group is introduced, wire the whole chain in the same change: `changes.outputs`, the paths-filter entry,
the test job, coverage aggregation `needs`, and final CI status `needs`. A missing downstream `needs` entry can make a
new test job invisible to summary gates.

### Future guard

Workflow coverage fixes should validate both syntax and wiring: run `actionlint`, grep for escaped `${{ }}` quotes, and
check that new jobs appear in coverage and CI status dependencies.
