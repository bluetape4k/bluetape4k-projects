# Kover Coverage Policy

## 현재 상태

`bluetape4k-projects`는 Nightly에서 광범위한 Kover XML artifact를 생성하고 큰 matrix 전반의
coverage summary를 집계한다. 광범위한 repository-wide coverage gate는 강제하지 않는다.

## 정책

Status: report-only transition.

이 monorepo는 core library, infrastructure client, Spring Boot module, virtual-thread module,
Testcontainers support를 포함한다. Threshold는 하나의 aggregate gate가 아니라 module-by-module로
도입해야 한다.

## Threshold 계획

- Kover는 build gate가 아니라 trend signal로 다룬다.
- Nightly XML report와 기존 coverage artifact upload를 사용해 coverage regression을 식별한다.
- Module에 coverage repair가 필요하면 focused issue를 연다. Default enforcement mechanism으로
  failing threshold를 도입하지 않는다.
- `testing/testcontainers`는 matrix partitioning 때문에 coverage가 인위적으로 낮아 보일 수 있으므로
  threshold 변경 전에 artifact production check가 필요하다.
- Benchmark/generated/test fixture code는 명시적으로 제외된 상태를 유지해야 한다.

## CI/Nightly Contract

Nightly는 Kover XML artifact를 집계하고 trend visibility를 유지한다. 향후 issue가 해당 gate를
명시적으로 재도입하지 않는 한, CI와 Nightly는 어떤 module이 고정 coverage percentage보다 낮다는
이유만으로 실패하면 안 된다.
