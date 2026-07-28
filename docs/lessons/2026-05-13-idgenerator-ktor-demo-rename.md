# idgenerator Ktor demo rename

## 배경

PR #421 merge 이후 review에서 nested Ktor example이 `:idgenerator-ktor`로 등록된 것을 확인했다.
다른 runnable example module은 demo-oriented naming convention을 사용한다. 해당 module도
publishing과 Kover에서 project directory 기준으로 제외되는 다른 example 옆에 있다.

## 결정

- `examples/ktor/idgenerator-ktor`를 `examples/ktor/idgenerator-ktor-demo`로 rename한다.
- Gradle project name이 `:idgenerator-ktor-demo`가 되도록
  `includeModules("examples/ktor", false, false)`를 유지한다.
- Examples workflow와 Nightly build exclusion command를 새 task path로 갱신한다.
- README와 lesson command example을 `:idgenerator-ktor-demo`로 갱신한다.

## 결과

Ktor idgenerator example은 directory와 Gradle task name 모두에서 명시적인 demo suffix를 갖는다.
Project directory가 계속 `examples/**` 아래에 있으므로 root Gradle sample/project-dir filtering은
publishing과 Kover에서 이 module을 제외한다.

## 검증 근거

```bash
./gradlew -q projects | rg "idgenerator-ktor-demo"
```

예상 결과: `:idgenerator-ktor-demo`가 등록된다.

```bash
repo-test-summary -- ./gradlew :idgenerator-ktor-demo:compileKotlin :idgenerator-ktor-demo:test --parallel
```

예상 결과: Ktor demo test가 통과한다.

## 향후 지침

Runnable example module을 추가할 때는 의도적인 user-facing 이유가 없다면 directory와 Gradle project name에
`-demo`를 포함한다. Rename 후에는 workflow task path, Nightly exclusion, README command snippet,
lesson evidence string을 같은 변경에서 함께 갱신한다.
