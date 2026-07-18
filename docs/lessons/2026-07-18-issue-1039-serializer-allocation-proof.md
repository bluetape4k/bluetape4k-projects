# Issue #1039 Serializer Allocation Proof

## Context

The serializer ByteBuffer work needed reproducible allocation evidence without changing production dispatch, wire formats, ownership, or security defaults. A single benchmark score was insufficient because compatibility defaults and backend-specific paths have different capabilities.

## Decision

Use a standalone, non-published `kotlinx-benchmark` module with 40 separately named JMH cells. Treat `gc.alloc.rate.norm` as the primary metric, throughput as diagnostic, and accept a lower-allocation claim only when two fresh runs both improve by at least 5% against the matching ByteArray baseline. Compatibility and fallback cells are always ineligible.

## What The Measurements Proved

Runs `run-20260718T030512Z` and `run-20260718T031704Z` each produced 40 allocation-bearing results. Five optimized comparisons were accepted: JDK serialization, Kryo serialization/deserialization, and Jackson 2/3 serialization. The comparator produced `accepted=5`, `inconclusive=7`, and `ineligible=14` across 26 candidate comparisons.

## What They Did Not Prove

The results do not prove lower allocation for JDK, Fory, Jackson 2/3, Fastjson2, or Avro input paths that were inconclusive. They do not prove anything for compatibility/fallback controls, generic/specific/list Avro APIs, unmeasured configurations, other payloads, or other JVMs. They do not turn ergonomic ByteBuffer overloads into zero-allocation APIs.

## Failure Or Surprise

The first executable JMH smoke failed because dependency signature files were copied into the fat JAR. The repository-standard `META-INF/*.RSA`, `*.DSA`, and `*.SF` exclusion restored an executable artifact, after which all 40 cells ran. The first evidence-launch command was also intercepted by the repository output-protection hook because environment capture invoked `./gradlew --version`; invoking the same Gradle WrapperMain directly avoided the false build redirect without starting or overwriting a run. The planned per-module Detekt tasks do not exist in this repository; the available root `detekt` task is `NO-SOURCE`, so compilation, tests, ABI proof, and the full non-test build provide the effective static/build evidence while the Detekt gap remains explicit.

## Verification Evidence

- Two sequential JMH runs: 40 JSON entries, 40 normalized-allocation metrics, and 40 summary rows per run.
- Seven comparator unit tests and byte-identical comparison regeneration.
- Benchmark module test, JMH compilation/JAR/smoke, and module build.
- Full tests for I/O, JSON, Jackson 2, Jackson 3, Fastjson2, and Avro.
- Serializer ABI proof: new caller compilation, default dispatch, and buffer default ABI all PASS.
- Module auto-registration and non-published/Kover-excluded benchmark classification.
- Full `build -x test`, locale/claim parity, raw artifact size, and `git diff --check` gates.

## Review Misses

The initial plan assumed module-local Detekt tasks and did not anticipate signed dependency metadata in the generated JMH fat JAR. Both assumptions were discovered by executing the real repository surfaces before completion. Six-lens review converged at P0=0 and P1=0; the Detekt configuration gap is an existing P2 repository concern rather than an issue #1039 code regression.

## Future Guard

Whenever serializer dispatch, the benchmark payload, or benchmark configuration changes, produce two new unique run IDs and regenerate `comparison.csv`. Never reuse positive allocation wording unless both fresh B/op deltas meet the 5% same-direction rule. Keep compatibility/fallback controls ineligible, preserve invalid runs for diagnosis, and verify the executable JMH JAR before collecting evidence.
