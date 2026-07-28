# #596 EtcdServer Testcontainers fixture

## 배경

`bluetape4k-leader` #227은 `leader-etcd`를 위한 실제 etcd integration test가 필요하다.
`bluetape4k-testcontainers`에는 이미 `ConsulServer`와 `ZooKeeperServer`가 있었지만 재사용 가능한 etcd
launcher는 없었다.

## 결정

기존 `infra` package에 작은 `GenericContainer` wrapper인 `EtcdServer`를 추가한다. Official etcd
container guide default인 `gcr.io/etcd-development/etcd:v3.6.0`, client port `2379`, peer port `2380`,
`/health` readiness를 사용한다.

## 결과

`EtcdServer.Launcher.etcd`는 reusable singleton을 제공하고 `testcontainers.etcd.*` property를 export한다.
Primary user-facing helper는 jetcd client가 직접 사용할 수 있는 `endpoint`다.

## 검증

- PullMD research를 `~/work/bluetape4k/bluetape4k-wiki/research/2026-05-22-issue-596-etcd-container-pullmd.md`에 저장하고 GNO로 검증.
- `./gradlew :bluetape4k-testcontainers:compileKotlin :bluetape4k-testcontainers:compileTestKotlin --no-daemon --console=plain` 통과.
- `./gradlew :bluetape4k-testcontainers:test --tests 'io.bluetape4k.testcontainers.infra.EtcdServerTest' --no-daemon --console=plain` 통과. Smoke assertion update 후 `--no-configuration-cache`로 rerun.

## 향후 가이드

새 infrastructure launcher에는 `isRunning` 외에 direct endpoint smoke coverage를 추가한다. Official
container doc은 PullMD로 확인하고, shared wiki research에 저장한 뒤 implementation commit 전에 GNO-index한다.
