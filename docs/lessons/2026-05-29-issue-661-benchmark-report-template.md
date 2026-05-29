# Issue 661: Benchmark Report Template

## Context

Benchmark evidence was split across `docs/benchmarks`, module-local
`Benchmark.md` files, issue comments, and README charts.

## Decision

Use `docs/benchmarks/README.md` as the durable benchmark report index and
template. Reports should name scope, commands, environment, raw artifacts,
summary tables, chart artifacts, interpretation, and follow-up links.

## Outcome

- Added a benchmark report index and standard report shape.
- Marked existing benchmark reports with raw/chart artifact availability.
- Kept module-local benchmark documents lightweight; durable issue evidence
  belongs in `docs/benchmarks`.

## Verification

- Markdown-only change reviewed with `git diff --check`.
- Existing artifact links in `docs/benchmarks/README.md` were checked for file
  presence.

## Future Guidance

New performance PRs should link their issue comment to a `docs/benchmarks`
report when the result changes public guidance, defaults, or README charts.
