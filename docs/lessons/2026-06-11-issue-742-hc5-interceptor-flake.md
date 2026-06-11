# Issue #742: HC5 async interceptor ordering tests

## Context

`AsyncClientInterceptors` recorded request interceptor and execution interceptor
events into one shared list while issuing many async requests. The old assertion
compared the entire list to a serialized expected sequence.

## Decision

Keep the production behavior unchanged and assert interceptor ordering per
execution id. Cross-request interleaving is valid for async execution and should
not be part of the test contract.

## Verification

- Focused `AsyncClientInterceptors` test.
- Full `:bluetape4k-http:test` module test.

## Future Guard

When a test records events from concurrent or async requests, group by the
request/execution identity first. Assert the per-request invariant unless the
feature explicitly promises a global ordering contract.
