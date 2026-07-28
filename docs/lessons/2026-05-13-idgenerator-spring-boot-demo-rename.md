# idgenerator Spring Boot demo rename

## 배경

Spring Boot idgenerator example은 원래 `spring-boot/idgenerator-demo` 아래에 있었고 Gradle task는
`:bluetape4k-spring-boot-idgenerator-demo`였다. Ktor example이 `idgenerator-ktor-demo`로
rename된 뒤 Spring Boot example도 같은 subject-first naming style이 필요했다.

## 결정

- Example을 `examples/spring-boot/idgenerator-spring-boot-demo`로 이동한다.
- Gradle task가 `:idgenerator-spring-boot-demo`가 되도록 nested Spring Boot example을
  `includeModules("examples/spring-boot", false, false)`로 등록한다.
- Examples workflow, Nightly exclusion, README command snippet, lesson을 새 path와 task name으로 갱신한다.

## 결과

두 idgenerator example은 서로 맞는 runnable example 이름을 사용한다.

- `:idgenerator-ktor-demo`
- `:idgenerator-spring-boot-demo`

둘 다 `examples/**` 아래에 있으므로 root Gradle sample filtering은 project directory 기준으로
publishing과 Kover에서 제외한다.

## 검증 근거

```bash
./gradlew -q projects | rg "idgenerator-(ktor|spring-boot)-demo"
```

예상 결과: 두 idgenerator demo project가 모두 등록된다.

```bash
repo-test-summary -- ./gradlew :idgenerator-spring-boot-demo:compileKotlin :idgenerator-spring-boot-demo:compileTestKotlin :idgenerator-spring-boot-demo:test --parallel
```

예상 결과: Spring Boot idgenerator demo test가 통과한다.

## 향후 지침

Framework integration을 비교하는 runnable example은 `idgenerator-ktor-demo`,
`idgenerator-spring-boot-demo`처럼 subject-first example name을 선호한다. Workflow task path와
README snippet은 Gradle project name과 같은 변경에서 동기화한다.
