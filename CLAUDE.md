# CLAUDE.md — bluetape4k-projects

Bluetape4k 코어 라이브러리. 공유 Kotlin/JVM 백엔드 라이브러리 컬렉션.
Kotlin idiom 극대화, Java 라이브러리 개선, Coroutines 기반 비동기/논블로킹 개발 지원.
Gradle 멀티모듈; `settings.gradle.kts` 가 서브디렉토리를 `bluetape4k-{dirname}` 으로 자동 등록.

## Build Commands

```bash
repo-status                      # git status 요약
repo-diff                        # 파일별 변경 수
repo-test-summary -- ./gradlew :module:test   # 테스트 출력 요약

./gradlew clean build
./gradlew build -x test
./gradlew :bluetape4k-coroutines:build
./gradlew test --tests "io.bluetape4k.io.CompressorTest"
./gradlew detekt
./gradlew publishBluetape4kPublicationToBluetape4kRepository           # SNAPSHOT
./gradlew publishBluetape4kPublicationToBluetape4kRepository -PsnapshotVersion=  # RELEASE
```

## Module Groups

> Full list: `.claude/references/module-groups.md`

| Group             | Description                                                         |
|-------------------|---------------------------------------------------------------------|
| `bluetape4k/`     | `core`, `coroutines`, `logging`, `bom`                              |
| `data/`           | `exposed-*`, `hibernate`, `mongodb`, `jdbc`, `r2dbc`, `cassandra`   |
| `infra/`          | `lettuce`, `redisson`, `kafka`, `pulsar`, `resilience4j`, `cache-*` |
| `spring-boot3/4/` | WebFlux+Coroutines, Exposed repos, Spring Batch                     |
| `virtualthread/`  | `api`, `jdk21`, `jdk25` — always update both together               |

## Build Configuration

- **JVM Toolchain**: Java 21 · **Kotlin**: 2.3 · **Gradle**: ZGC daemon, 4–8 GB heap, parallel build
- `buildSrc/Libs.kt` — dependency versions · `gradle.properties` — `baseVersion=1.7.0`

## Important Notes

- **jar source extraction**: `.claude/lib-sources/<library-name>/` — never `/tmp/` or project source tree
- **atomicfu**: class property level only — never method-local variables
- **Detekt**: disabled in `exposed-jdbc-tests`
- **virtualthread-api**: `virtualthread/api` 변경 시 반드시 `jdk21` + `jdk25` 모두 업데이트
- **mock-web-server** 변경 → `./gradlew :bluetape4k-mock-web-server:jibDockerBuild --no-configuration-cache`
- **mock-webflux-server** 변경 → `./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild --no-configuration-cache`
- **Publishing**: GitHub Packages Maven; `workshop/` + `examples/` 제외

## Design Patterns (프로젝트 특이사항)

- **Auditable**: UPDATE 작업은 항상 `auditedUpdate*` 사용
- KDoc full reference: `.claude/references/design-patterns.md`
