# 제출된 Gradle 의존성 그래프와 Dependabot의 선언 위치

## 맥락

이슈 [#1832](https://github.com/bluetape4k/bluetape4k-projects/issues/1832)는 Dependabot Gradle 보안 업데이트가 `dependency_not_found`와 `Job dependencies not found in the dependency snapshot`으로 중단되는 문제를 다룬다. 실패는 `469ff06cdf9ec9d2648dd7341707559fa62ab51f`에서 실행한 #38350740351, #38350922415와 `255078cf79c6544fe22ae7e7c5eb0323f84aa7ef`에서 실행한 #38667737423에서 확인됐다.

Dependency Submission 실행 #38350717022와 #38667705661은 각각 같은 기준 HEAD에서 성공했다. 각 artifact는 manifest 하나와 해결된 의존성 항목 2,068개를 담고, `com.rabbitmq:amqp-client`, Jackson, Netty 등 실패 로그의 좌표도 포함한다. 따라서 업로드한 artifact에서 해당 패키지 좌표가 빠졌다고 볼 근거는 없다. 다만 두 artifact 모두 manifest의 `file.source_location`을 `settings.gradle.kts`로 기록한다. 현재 열린 alert 31건도 모두 `settings.gradle.kts`를 manifest 경로로 보고한다.

Projects는 `settings.gradle.kts`에서 `bluetape4k-dependencies`의 고정 commit을 읽어 외부 `gradle/libs.versions.toml`을 내려받고, 이를 `bt4k` version catalog로 등록한다. 여러 빌드 스크립트는 `bt4kLibrary`와 `bt4kVersion` helper로 이 catalog를 참조한다. Gradle Dependency Graph 플러그인 FAQ는 Gradle이 의존성 선언의 정확한 파일 위치를 제공하지 않아 모든 의존성을 `settings.gradle.kts`에 귀속한다고 설명한다. GitHub도 Gradle 보안 업데이트가 제출된 dependency graph를 사용하고, manifest에서 수정할 위치를 찾지 못하는 전이 의존성에는 보안 업데이트 PR을 만들지 못할 수 있다고 설명한다.

이 증거는 패키지 자체가 artifact에 없다는 해석과 맞지 않는다. 제출 자료에는 패키지가 있지만 Dependabot이 수정 가능한 선언 위치와 연결하지 못하는 것으로 보인다. 내부 `dependency_not_found` 판정 로직은 공개 자료로 확인할 수 없으므로, 선언 위치와 외부 catalog를 원인으로 보는 것은 artifact·alert metadata·공식 문서에 근거한 추론이다. 10월 13일 첫 실패와 10월 15일 실패는 dependency submission이 끝나기 전에 시작됐고, 10월 13일 두 번째 실패는 제출 완료 후 시작됐다. 따라서 단순 실행 순서만으로 전체 실패를 설명할 수는 없다.

## 결정

- Dependency Submission workflow는 변경하지 않는다. 제출 실행이 성공했고 artifact에 실패 좌표가 들어 있음을 확인했다.
- Projects의 Dependabot 설정에 Gradle 항목을 두고 `open-pull-requests-limit: 0`으로 정기 버전 업데이트 PR을 막는다. security update를 위해 `target-branch`는 설정하지 않는다.
- `open-pull-requests-limit: 0`은 일반 버전 업데이트를 끄지만 보안 업데이트는 허용한다. Projects는 Gradle 항목에 `bluetape4k-dependabot-security-updates: enabled` 주석 marker를 명시해 중앙 `ignore` 블록을 생략한다. marker가 없는 다른 저장소의 zero-limit 설정은 기존 생성 규칙을 유지하며, opt-in 설정에서도 수동 ignore는 보존한다.
- 버전 업데이트 PR이 켜진 다른 downstream 설정에는 중앙 ignore 규칙이 계속 적용된다. 해당 규칙은 일치하는 보안 업데이트도 억제할 수 있으므로, 중앙 catalog를 갱신한 뒤 고정 참조를 하위 저장소에 전파하고 alert 상태를 따로 확인한다.
- Ignore는 취약점을 수정하거나 제출한 의존성 그래프를 바꾸지 않는다. Projects 설정 변경만으로 `dependency_not_found`가 해결됐다고 보고하지 않는다.

## 결과와 검증

- RabbitMQ 좌표는 중앙 저장소 생성 규칙에 포함된다. 새 생성기는 marker가 있는 Projects Gradle 항목에서만 중앙 ignore를 생략하고, Exposed를 포함한 marker 없는 downstream에는 기존 중앙 ignore를 유지한다.
- 초기 조사 시점의 alert 31건에서 확인한 고유 패키지 좌표 10개가 당시 Projects ignore 목록에 포함됐다. 이는 당시 차단 범위만 증명하며 alert 해소를 의미하지 않는다.
- 로컬 검증에서는 생성기 단위 테스트, Projects 설정과 생성기 출력의 parity, YAML parse, `git diff --check`를 확인한다. 이 검증은 GitHub Dependabot이 보안 PR을 만들거나 원래 실행 오류가 사라졌음을 증명하지 않는다.
- 이슈 #1832는 설정 변경 이후 Dependabot security update 실행이 `dependency_not_found` 없이 끝나는지 확인할 때까지 열린 상태로 둔다. 열린 보안 alert의 실제 수정 여부도 별도로 확인한다.

## 재발 방지

`dependency_not_found`가 나오면 오류 문자열만으로 업로드 누락을 단정하지 않는다. 같은 HEAD의 dependency-submission artifact에서 `sha`, `job.correlator`, manifest `source_location`, package coordinates를 확인하고, GitHub alert의 manifest 경로·relationship과 비교한다. 중앙 catalog가 소유한 패키지는 downstream ignore 정책과 실제 중앙 버전 갱신을 별도로 확인한다.

## 참고

- [GitHub supported ecosystems and repositories](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories)
- [GitHub dependency submission API](https://docs.github.com/en/rest/dependency-graph/dependency-submission)
- [Gradle dependency submission action](https://github.com/gradle/actions/blob/main/docs/dependency-submission.md)
- [Gradle dependency submission FAQ](https://github.com/actions/gradle-build-tools-actions/blob/main/docs/dependency-submission-faq.md)
- [GitHub controlling dependencies updated](https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/manage-your-dependency-security/controlling-dependencies-updated)
