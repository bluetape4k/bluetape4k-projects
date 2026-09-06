# #1639 Fory wire compatibility 검증 lesson

## 맥락

Fory/Fory Kotlin이 `1.6.0`에서 `1.7.1`로 올라간 뒤 Kotlin metadata와 Redis 저장 바이트 경계를 검증해야 했다. 기존 `issue-754` fixture는 Fory `1.3.0`으로 생성되어 있어 이번 변경 구간의 증거로 사용할 수 없다.

## 결정

- 현재 catalog의 Fory/Fory Kotlin `1.7.1`을 유지하고 production serializer는 변경하지 않는다.
- Kotlin `2.4.10` data class의 nullable/default/collection/nested/value-class 사례를 current serializer에서 직접 검증한다.
- Fory `1.6.0`으로 생성한 bytes는 binary fixture와 manifest로 고정하고, 생성 version·payload·hash를 함께 검증한다.
- malformed/truncated input은 작은 fixture와 제한 시간 assertion으로 실패 경계를 확인한다.
- FastFory(`SCHEMA_CONSISTENT`)와 Fory(`COMPATIBLE`)의 wire compatibility를 약속하지 않는다. Redis 검증은 실제 `RedisSerializer`가 생성하는 저장 바이트와 round-trip 범위로 한정한다.

## 결과와 검증 기록

- Fory `1.6.0` fixture는 241 bytes이며 SHA-256은
  `4e672fb8d1b5cad5cd66537461ce714f4027b794a8be32e54d8337f6cddac819`이다.
- Fory `1.7.1`은 `1.6.0` fixture를 같은 Kotlin payload로 복원했다.
- 현재 `1.7.1` 쓰기 결과가 fixture와 동일할 수도 있으므로 byte 차이를 호환성
  조건으로 두지 않았다. version-pinned fixture 읽기와 current round-trip을 독립적으로
  검증한다.
- `ForyWireCompatibilityTest`: 4 passing, skipped/failures/errors 0.
- `RedisForyWireCompatibilityTest`: Fory/LZ4Fory/FastFory 3 passing,
  skipped/failures/errors 0. 실제 Redis 서버나 Testcontainers는 필요하지 않았다.
- 전체 module 검증에서는 `bluetape4k-io` 1,302개와
  `bluetape4k-spring-boot-redis` 102개 테스트가 실패·오류·제외 없이 통과했다.
  두 module의 detekt를 변경 후 다시 실행해 `BUILD SUCCESSFUL`을 확인했다.
- 한국어 문서 3개 용어 감사 findings 0과 `git diff --check` 통과를 확인했다.
- 올바른 Spring Boot Redis project path는
  `:bluetape4k-spring-boot-redis`이다. `:bluetape4k-redis`는 `infra/redis` 모듈이므로
  후속 검증에서 혼동하지 않는다.

## 재발 방지 guard

의존성 호환성 이슈에서는 기존 fixture의 serializer version을 먼저 manifest에서 읽고, 목표 version 구간을 증명하지 못하면 새 fixture를 만든다. current write/read, old fixture read, FastFory mode boundary를 하나의 assertion으로 합치지 않고 별도 테스트로 유지한다.
