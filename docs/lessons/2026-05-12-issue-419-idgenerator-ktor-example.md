# Issue #419 idgenerator Ktor 예제 작업 기록

## 배경

#419는 idgenerator 예제 중 Ktor 범위를 Spring Boot 예제 이슈에서 분리한 작업이다.

## 결정

- 실행 가능한 Ktor 예제는 `examples/ktor/idgenerator-ktor`에 둔다.
- `settings.gradle.kts`에는 `includeModules("examples/ktor", false, false)`를 추가해 `:idgenerator-ktor`로 등록한다.
- 명시적 `/ids/{type}` route와 generic `/idgen/{type}` route를 모두 제공하되, 뒤에서는 하나의 registry를 공유한다.
- `includeModules("examples/ktor", false, false)`처럼 project path에 `examples`가 빠질 수 있으므로, 배포/Kover 제외 판정은 Gradle path만 보지 않고 `projectDir`가 `examples/**` 아래인지도 확인한다.

## 결과

새 모듈은 UUID v4, UUID v7, ULID, KSUID, Snowflake, Flake 단건/배치 endpoint를 제공한다.

## 검증

```bash
./gradlew :idgenerator-ktor:compileKotlin :idgenerator-ktor:test --parallel
```

결과: 테스트 6개 통과.

## 다음 작업자를 위한 메모

중첩 예제를 custom project name으로 등록할 때는 root Gradle의 `project.path.contains("examples")` 기반 필터가 더 이상 예제 모듈을 잡지 못할 수 있다. 새 예제 모듈을 추가하면 배포, Kover, 집계 task 제외 조건을 함께 확인해야 한다.
