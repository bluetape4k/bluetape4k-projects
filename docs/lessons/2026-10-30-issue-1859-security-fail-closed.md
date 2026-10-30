# OWASP Security workflow의 실패 전파를 fail-closed로 유지한다

## 맥락

Issue [#1859](https://github.com/bluetape4k/bluetape4k-projects/issues/1859)의
`Security` workflow는 `dependencyCheckAnalyze`와 SARIF 업로드에
`continue-on-error: true`를 사용했다. 실제 scheduled run
[`40234771781`](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/40234771781)에서
OWASP task가 `FAILED`와 `BUILD FAILED`를 출력했지만 job conclusion은 `success`였다.
보고서가 생성되지 않거나 업로드가 실패해도 최종 security gate가 이를 독립적으로 판정하지
않았다.

## 결정

- OWASP scan과 SARIF upload의 `continue-on-error`를 제거해 step/job 결과를 보존한다.
- scan이 실패해도 보고서 수집 단계는 `if: always()`로 실행한다.
- 보고서가 하나도 없으면 명시적으로 실패하고, artifact upload도
  `if-no-files-found: error`로 설정한다.
- `gitleaks`와 `dependency-check`를 `needs`로 묶은 `security-status` job을 추가해 각 결과가
  `success`인지 확인하고, 실패·취소·업로드 실패를 최종 workflow 실패로 전파한다.
- 이 계약을 scan failure, upload failure, no-report 경로를 각각 고정하는 Python unittest로
  유지한다.

## 결과

보안 workflow는 취약점 scan 실패를 성공으로 은폐하지 않고, 산출물·SARIF 업로드 결과와
보고서 부재도 최종 상태에 반영한다. `gitleaks` 자체의 기존 full-history 검출 범위와
OWASP Gradle 설정은 변경하지 않았다.

## 검증

- 원본 workflow에서 새 계약 테스트 4개가 모두 실패하는 RED를 재현했다.
- 수정 후 `python3 -m unittest tests.test_catalog_workflows -v`가 139개 테스트 전체 통과했다.
- `python3 scripts/verify-catalog-workflows.py`가 `catalog workflow contract passed`를 반환했다.
- `actionlint .github/workflows/security.yml`, Python YAML parse, `git diff --check`가 통과했다.

## 향후 지침

1. 보안 scanner의 실패를 허용하려면 예외·범위·만료를 별도 문서화하고 최종 gate에서 성공으로
   재분류하지 않는다.
2. `if: always()`는 증거 수집을 위한 것이며 실패를 무시하는 `continue-on-error`와 함께 쓰지
   않는다.
3. artifact·SARIF 경로는 report 생성 여부와 업로드 결과를 모두 확인하고, no-report 경로를
   회귀 테스트로 유지한다.
