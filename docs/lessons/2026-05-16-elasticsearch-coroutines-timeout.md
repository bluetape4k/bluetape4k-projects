# Elasticsearch Coroutines Timeout

## Context

Nightly full verification for the Memgraph CI fix passed `graphdb-memgraph`,
but failed in `Test / Infra (search-messaging)`. The failing module was
`:bluetape4k-elasticsearch:test`, specifically `ElasticsearchCoroutinesTest`.

## Decision or Finding

The failing tests were bounded by `runTest(timeout = 30.seconds)`. On GitHub
Actions, Elasticsearch container I/O plus index refresh calls can exceed that
limit under runner load. Sibling Elasticsearch coroutine integration tests
already use 60-second timeouts for similar external I/O paths.

## Outcome

`ElasticsearchCoroutinesTest` now uses 60-second `runTest` timeouts for setup,
teardown, and CRUD/search integration tests. This keeps the tests bounded while
allowing realistic CI latency.

## Verification

- Local targeted class: `:bluetape4k-elasticsearch:test --tests io.bluetape4k.elasticsearch.coroutines.ElasticsearchCoroutinesTest` passed in 31 seconds.
- GitHub Nightly full attempt 1 and failed-job rerun both failed before this
  change with `UncompletedCoroutinesError: After waiting for 30s`.

## Future Guidance

For coroutine tests that call real containers or remote-style clients, avoid
30-second `runTest` limits unless the underlying operation is deterministic and
purely local. Match sibling integration tests that already account for CI
runner latency.
