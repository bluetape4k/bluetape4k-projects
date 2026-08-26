# Issue #563: split package는 소유권과 Java compatibility를 함께 보아야 한다

## 배경

`virtualthread-api`는 Java 21 호환 섬이고 `core`는 Java 25 기본 모듈이다.
API 타입을 core로 옮기면 Java 21 모듈이 Java 25 bytecode를 요구하게 되고,
core 타입을 API로 옮기면 core dependency cycle이 생긴다. 두 JAR이 같은
패키지를 가지는 상태는 classpath에서는 숨겨질 수 있지만 module-path의
`java --validate-modules`에서 즉시 드러난다.

## 배운 점

- split package의 해결은 단순 파일 이동이 아니라 published owner와 target
  Java version을 동시에 고정해야 한다.
- API subpackage 경계는 Java 21 compatibility와 core utility ABI를 모두
  보존하면서 automatic module 충돌을 제거한다.
- ServiceLoader descriptor 파일명은 interface package의 일부이므로 source
  import와 함께 원자적으로 갱신해야 한다.
- binary package migration은 bridge를 남기지 않는 한 재컴파일 계약이다.
  README와 release note에 이를 명시해야 한다.

## 검증

- `:bluetape4k-virtualthread-api:test`
- `:bluetape4k-virtualthread-jdk21:test`
- `:bluetape4k-virtualthread-jdk25:test`
- `:bluetape4k-core:test`
- 생성 `bluetape4k-core-2.0.0.jar`와
  `bluetape4k-virtualthread-api-2.0.0.jar`의
  `java --validate-modules` exit 0
