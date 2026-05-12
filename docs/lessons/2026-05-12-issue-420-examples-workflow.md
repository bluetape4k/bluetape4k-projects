# Issue 420 Examples workflow 분리

## Context

#420은 예제 모듈을 Nightly의 라이브러리 회귀 테스트 범위에서 분리하고, 사용 예제 전용 GitHub Actions workflow로 검증하는 작업이다. 기존 `examples/*` 네 모듈과 후속 idgenerator 예제들이 대상이다.

## Decision

- `.github/workflows/examples.yml`을 새로 만들고 workflow 이름은 `Examples`로 둔다.
- 기존 `examples/*` 네 모듈은 Examples workflow에서 compile/test를 직접 실행한다.
- #419 Ktor 예제와 #416 Spring Boot 예제는 별도 PR 순서에 영향받지 않도록 모듈 파일이 있을 때만 task를 추가한다.
- Nightly의 root `build -x test`는 example module build task를 제외해 예제 compile을 Examples workflow로 옮긴다.
- 예제 coverage는 업로드하지 않고 test result artifact만 보관한다.

## Outcome

Examples workflow와 Nightly example 제외 로직을 추가했다.

## Verification Evidence

- `ruby -e 'require "yaml"; ...' .github/workflows/examples.yml .github/workflows/nightly-tests.yml` 성공.
- `./gradlew -q projects | rg "bluetape4k-examples|idgenerator-ktor|spring-boot-idgenerator-demo"`로 현재 branch의 기존 examples 4개 모듈만 확인.
- `./gradlew :bluetape4k-examples-coroutines-demo:compileKotlin :bluetape4k-examples-coroutines-demo:test :bluetape4k-examples-jpa-querydsl-demo:compileKotlin :bluetape4k-examples-jpa-querydsl-demo:test :bluetape4k-examples-redisson-demo:compileKotlin :bluetape4k-examples-redisson-demo:test :bluetape4k-examples-virtualthreads-demo:compileKotlin :bluetape4k-examples-virtualthreads-demo:test --parallel` 성공.
  - coroutines demo: 125 passing, 2 pending.
  - jpa-querydsl demo: 42 passing, 1 pending.
  - redisson demo: 89 passing.
  - virtualthreads demo: 32 passing.
- `./gradlew build -x test ... --dry-run | rg "bluetape4k-examples|idgenerator-ktor|spring-boot-idgenerator-demo"` 결과 match 없음으로 Nightly build exclusion 확인.

## Future Guidance

새 예제 모듈을 추가할 때는 Nightly가 아니라 `.github/workflows/examples.yml`에 등록한다. 다른 PR과 순서가 엮이는 예제 모듈은 파일 존재 여부를 확인한 뒤 task를 추가해 독립 PR을 깨지 않게 한다.
