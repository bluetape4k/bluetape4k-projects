# Issue #1466 image gate 결과 증거 보존 lesson

## 배경

PR #1463의 hosted CI run `32480088175`에서 Testcontainers image gate가
`0/52`로 실패했습니다. 52개 family 중 51개는 Gradle `returncode=0`이었지만
`blocked`로 집계됐고, K3s 한 family만 `exit 137` infrastructure failure였습니다.

## 원인과 결정

runner가 artifact에 저장할 stdout/stderr를 먼저 12,000자로 축약한 뒤,
축약본에서 `BUILD SUCCESSFUL` marker를 찾았습니다. Jib/Docker 진행 출력이 긴
family에서는 성공 marker가 축약 경계 뒤에 있어 정상 종료를 `blocked`로
오판했습니다.

판정은 원문 stdout/stderr에서 수행하고, artifact와 로그에는 기존의
bounded·redacted 출력을 계속 저장합니다. marker가 없는 `returncode=0`은
계속 `blocked`로 유지해 조기 종료나 불완전한 결과를 통과시키지 않습니다.

## 검증

- 긴 성공 출력 회귀 테스트: RED에서 `blocked` 재현, 수정 후 GREEN
- 기존 성공·product failure·infrastructure retry·timeout·redaction 테스트:
  PASS
- hosted CI 재실행: PENDING

## 후속 guard

실행 결과의 판정 입력과 보존 입력을 분리합니다. 출력 제한은 저장 경계에만
적용하고, 성공·실패 marker 검사는 원문을 사용해야 합니다.

## SPW 감사

- SPW-01: PASS — run URL, head, `0/52`, `exit 137`, 원인과 미검증 범위를 고정했습니다.
- SPW-02: PASS — 배경, 원인, 결정, 검증, 후속 guard를 포함했습니다.
- SPW-03: PASS — 한국어 기술 문체와 `returncode`, `BUILD SUCCESSFUL` 토큰을 보존했습니다.
- SPW-04: PASS — artifact JSON과 runner source/test를 대조했습니다.
- SPW-05: PASS — 최종 Markdown read-back과 `git diff --check`를 수행할 예정입니다.
