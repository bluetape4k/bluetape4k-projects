# 이슈 459 K3s workload example

## 배경

Issue #459는 node listing을 넘어 실제 K3s API server를 대상으로 Kubernetes workload resource를
보여주는 실용적인 `K3sServer` integration-test example을 요청했다.

## 결정

`K3sServerTest`를 확장하지 않고 `testing/testcontainers` 아래에 전용
`K3sWorkloadExampleTest`를 추가한다. 새 file은 기본 server smoke test와 workload example을
분리하고, privileged runner 실행에는 기존 `@Tag("k8s")`/`k8sTest` 경로를 사용한다.

## 결과

Example은 ConfigMap CRUD, Deployment-backed Service readiness, Secret value decoding을 다룬다.
각 test는 Kubernetes client를 닫고, stale resource가 사라질 때까지 기다리는 idempotent pre-cleanup을
수행하며, singleton K3s container를 안전하게 재사용할 수 있도록 `try/finally`로 생성 resource를
정리한다.

## 검증

- `./gradlew :bluetape4k-testcontainers:compileTestKotlin --no-daemon --console=plain`
  passed.
- `./gradlew :bluetape4k-testcontainers:k8sTest --tests 'io.bluetape4k.testcontainers.infra.K3sWorkloadExampleTest' --no-daemon --no-configuration-cache --console=plain`
  3 tests executed로 통과.

## 다음번 가이드

K3s example을 추가할 때 long-running 또는 privileged Docker scenario는 `@Tag("k8s")` 아래에
유지하고, Docker가 지원되는 local 환경에서는 전용 `k8sTest` task를 실행한다. Singleton-container
safety를 위해 wait-backed pre-cleanup과 `finally` cleanup을 함께 사용한다.
