# Gradle Dependabot 보안 업데이트 그룹에서 처리 불가 의존성 분리

## 맥락

이슈 [#1832](https://github.com/bluetape4k/bluetape4k-projects/issues/1832)에서 추적한 `dependency_not_found`는 Dependabot 의존성 그래프를 만들 때 좌표를 찾지 못한 오류다. 기준 HEAD에서는 재현되지 않았지만, 실제 Gradle Dependabot 실행 [#39536903535](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/39536903535)은 보안 업데이트 그룹 `gradle`에서 `org.bouncycastle:bcprov-jdk18on`의 `security_update_not_possible` 오류로 실패했다. 로그의 `lowest-non-vulnerable-version`은 `null`이었고 새 보안 PR은 생성되지 않았다.

이 오류는 의존성 그래프에서 좌표를 찾지 못한 `dependency_not_found`와 다르다. 그룹에 속한 좌표 하나의 업데이트 불가 오류가 발생하면 그룹 결과 전체를 별도로 조사해야 한다.

## 결정

- Gradle 보안 업데이트 그룹을 `.github/dependabot.yml`에 명시하고, 처리 불가 좌표는 `exclude-patterns`로 그룹에서 제외해 개별 업데이트 경로로 남긴다.
- 제외는 그룹 소속만 바꾸며 보안 alert를 무시하거나 비활성화하지 않는다.
- 설정 변경은 실제 Dependabot 실행에서 그룹 구성, 개별 좌표 결과, 다른 좌표의 업데이트 결과를 확인하기 전까지 해결로 판정하지 않는다.

## 결과

설정에 `gradle-security-updates` 그룹과 두 BouncyCastle 제외 좌표를 추가했다. 로컬 YAML 구조 검증은 그룹 설정 구조를 확인한다. PR CI는 변경 파일에 따른 저장소 검사를 실행하지만, 실제 Dependabot 그룹 구성과 개별 업데이트 결과는 검증하지 않는다. 실제 Dependabot 실행 및 alert 상태 확인 전까지 이슈 #1842는 열린 상태로 둔다.

## 재발 방지

Dependabot 로그의 오류 유형과 `dependency-groups` 내용을 함께 확인한다. `dependency_not_found`는 의존성 그래프에서 좌표를 찾지 못한 상태를, `security_update_not_possible`는 해당 업데이트 후보의 처리 가능성 문제를 가리킨다. 그룹 안의 단일 의존성이 처리를 막으면 GitHub 문서에 따라 해당 좌표를 그룹에서 제외하고 개별 처리 결과를 확인한다. 그룹 설정은 alert를 숨기는 `ignore` 규칙으로 대체하지 않는다.

## 검증

이 변경의 로컬 검증에서는 YAML 파싱과 그룹 범위·제외 좌표를 확인한다. 후보 설정을 사용한 실제 Dependabot 실행과 관련 alert 상태 read-back이 최종 증거다. 그 실행 전에는 이슈 #1842의 완료를 선언하지 않는다.

## 참고

- [Issue #1842](https://github.com/bluetape4k/bluetape4k-projects/issues/1842)
- [Dependabot 오류 해결](https://docs.github.com/en/code-security/reference/supply-chain-security/troubleshoot-dependabot/dependabot-errors)
- [Dependabot 그룹 설정](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#groups)
