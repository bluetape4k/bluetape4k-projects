# 이슈 #741 CodeQL Kotlin 2.4

## 배경

CodeQL Java/Kotlin extractor support가 따라오기 전에 Kotlin 2.4.0이 들어왔다. 같은
head에서 Nightly와 Publish Snapshot은 green이었지만, CodeQL만 `java-kotlin` axis에서
실패했다.

## 교훈

security scanner에 language support-window mismatch가 있으면 영향받지 않는 scanner
axis는 계속 실행하고 incompatible axis만 비활성화한다. scanner lag를 맞추기 위해
project toolchain을 downgrade하지 않는다.

## 후속 가드

CodeQL `java-kotlin`을 다시 활성화할 때는 `assemble`처럼 진짜 compile-only Gradle
command를 사용한다. 이 repository에서는 custom `Test` task가 여전히 task graph에
들어올 수 있으므로 `build -x test`만으로는 충분하지 않다.

## 증거

- CodeQL run `27250113912`: `actions`와 `python`은 통과했고 `java-kotlin`은 실패했다.
- Nightly run `27299345368`: 같은 SHA에서 성공했다.
- Publish Snapshot run `27299715400`: 같은 SHA에서 성공했다.
- local workflow validation: `actionlint .github/workflows/codeql.yml`.
