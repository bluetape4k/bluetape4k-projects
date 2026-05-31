# Lesson: #596 EtcdServer Testcontainers fixture

## Context

`bluetape4k-leader` #227 needs real etcd integration tests for `leader-etcd`.
`bluetape4k-testcontainers` already had `ConsulServer` and `ZooKeeperServer`,
but no reusable etcd launcher.

## Decision

Add `EtcdServer` as a small `GenericContainer` wrapper in the existing
`infra` package. Use the official etcd container guide defaults:
`gcr.io/etcd-development/etcd:v3.6.0`, client port `2379`, peer port `2380`,
and `/health` readiness.

## Outcome

`EtcdServer.Launcher.etcd` now provides a reusable singleton and exports
`testcontainers.etcd.*` properties. The primary user-facing helper is
`endpoint`, which is directly usable by jetcd clients.

## Verification

- PullMD research saved to
  `~/work/bluetape4k/bluetape4k-wiki/research/2026-05-22-issue-596-etcd-container-pullmd.md`
  and verified with GNO.
- `./gradlew :bluetape4k-testcontainers:compileKotlin :bluetape4k-testcontainers:compileTestKotlin --no-daemon --console=plain`
  passed.
- `./gradlew :bluetape4k-testcontainers:test --tests 'io.bluetape4k.testcontainers.infra.EtcdServerTest' --no-daemon --console=plain`
  passed; rerun with `--no-configuration-cache` after the smoke assertion update.

## Future Guidance

For new infrastructure launchers, add direct endpoint smoke coverage in addition
to `isRunning`. Use official container docs through PullMD, store the research
in shared wiki research, and GNO-index it before committing the implementation.
