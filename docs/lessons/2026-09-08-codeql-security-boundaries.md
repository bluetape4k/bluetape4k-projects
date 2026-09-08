# CodeQL 보안 경계는 실행 코드와 입력 데이터를 분리한다

## 맥락

정기 CodeQL 분석은 `develop`의
`23f60647ef53503a3dcbb9a7ac331abb83401db9`에서 다음 high 심각도 경고 세 건을
보고했다.

- [`py/redos` #52](https://github.com/bluetape4k/bluetape4k-projects/security/code-scanning/52):
  `scripts/testcontainers_image_gate.py`의 Kotlin 테스트 메서드 탐색 정규식
- [`actions/cache-poisoning/poisonable-step` #53](https://github.com/bluetape4k/bluetape4k-projects/security/code-scanning/53)
- [`actions/cache-poisoning/poisonable-step` #54](https://github.com/bluetape4k/bluetape4k-projects/security/code-scanning/54)

릴리스 두 경고는 `workflow_dispatch` 입력으로 선택한 release tag의 commit을
`verify-full-nightly` job에서 checkout한 뒤, 그 commit에 포함된 Python script를 기본 브랜치
권한과 cache write 권한으로 실행하는 경로를 가리켰다. tag가 저장소 내부 참조라는 사실만으로
workflow 실행 코드의 신뢰 경계가 보장되지는 않는다.

정규식은 `\s*`, 줄바꿈, 반복되는 `\s*`가 중첩돼 실패 입력에서 backtracking 경로가
급격히 증가했다. `@Test` 뒤에 줄바꿈 28개와 함수가 아닌 문자열을 넣은 bounded regression은
기존 구현에서 1초 안에 끝나지 않았다.

## 결정

release 검증 job은 `${{ github.workflow_sha }}`를 checkout해 workflow와 같은 trusted revision의
검증 script만 실행한다. 선택된 release commit SHA는 checkout 대상이 아니라 `TARGET_SHA` 데이터로
전달한다. tag가 여전히 그 SHA를 가리키는지는 trusted `resolve_release_target.py verify`가 검사하고,
실제 publication job만 검증된 SHA를 checkout한다.

정규식은 수평 공백과 줄바꿈을 구분하고 각 문자가 하나의 반복 경로에만 속하도록 바꾼다.
annotation 인자와 backtick 함수명도 줄 경계를 넘지 못하게 해 기존 탐색 범위를 유지한다.

## 결과

- release 검증 단계는 사용자가 선택한 release commit의 코드를 cache-write 권한으로 실행하지 않는다.
- release target의 immutable tag/SHA 검증과 exact-head Full Nightly 증거 확인은 유지된다.
- 병적인 실패 입력도 bounded subprocess test의 1초 제한 안에서 종료된다.
- workflow policy test는 검증 job이 다시 target SHA를 checkout하면 명시적으로 실패한다.

## 검증

- `scripts.test_testcontainers_image_gate`: 23개 통과
- `scripts.ci.resolve_release_target_test`: 9개 통과
- `scripts.test_release_workflow_policy`: 53개 통과
- `actionlint .github/workflows/release.yml`: 통과
- 변경 Python 파일 `py_compile`: 통과

`ruff` 전체 변경 파일 검사는 기존 파일에 있던 shebang permission, implicit concatenation,
`Optional`, import-order 규칙 위반 때문에 실패했다. 이번 변경에서 새 규칙 위반이 생기지 않았는지는
해당 기존 rule을 제외한 별도 검사로 확인한다.

## 놓친 점

처음 patch 경로를 integration checkout 기준으로 적용해 승인된 linked worktree 밖의 같은 파일 두 개를
일시적으로 수정했다. 즉시 해당 hunk만 되돌리고 integration checkout이 clean임을 확인한 뒤, 모든
후속 patch를 절대 worktree 경로로 제한했다. 공유 저장소에서 `workdir`은 command 실행 위치만 바꾸며
patch 대상 경로를 자동으로 바꾸지 않는다는 점을 작업 규칙으로 고정한다.

## 향후 지침

1. 권한이 높은 GitHub Actions job에서는 외부 입력이 고른 ref의 코드를 실행하지 말고 trusted workflow
   revision의 code와 검증 대상 data를 분리한다.
2. release target code 실행이 필요하면 cache write, secret, protected environment 같은 권한 경계를 먼저
   제거하거나 별도 job으로 격리한다.
3. 정규식 보안 수정은 매칭 결과뿐 아니라 adversarial non-match의 실행 시간 상한도 regression으로 고정한다.
4. linked worktree에서 patch할 때는 대상 파일을 절대 경로로 지정하고 integration checkout의 clean 상태를
   다시 확인한다.
