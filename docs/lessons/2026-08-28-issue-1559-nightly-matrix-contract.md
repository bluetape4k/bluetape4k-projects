# Nightly matrix 누락을 SNAPSHOT 게이트에서 차단한다

## 맥락

Milestone `2.0.0`의 P1 이슈 [#1559](https://github.com/bluetape4k/bluetape4k-projects/issues/1559)는
전체 Nightly가 성공하지 않은 상태에서 SNAPSHOT 발행이 시작될 수 있는 릴리스 게이트
위험을 다룬다. 기존 `publish-snapshot.yml` 검사는
`Test / Infra (` 같은 prefix마다 job이 하나 이상 있는지만 확인했다. 따라서 matrix
shard 하나가 사라져도 같은 prefix의 다른 shard가 성공하면 발행 조건을 통과할 수
있었다.

현재 Nightly는 고정 job 14개와 matrix job 29개를 만든다. matrix는
`Test / Infra` 4개, `Test / Data` 2개, `Test / Spring Boot` 3개,
`Test / Testcontainers` 20개 shard로 구성된다.

## 결정

- `scripts/nightly_matrix_contract.json`을 Nightly matrix의 명시적인 기준 계약으로
  둔다. 계약에는 workflow 경로와 네 그룹의 정확한 shard 목록을 기록한다.
- `scripts/validate_nightly_matrix.py`가 성공한 job 이름을 계약과 집합으로 비교한다.
  누락, 추가, 중복, 비성공 matrix job을 각각 오류로 보고하고, 기존 14개 고정 필수
  job과 run의 workflow 경로·`develop` branch·정확한 `head_sha`도 계속 확인한다.
- `publish-snapshot.yml`은 검증 대상 run의 `head_sha`가 유효한지 먼저 확인한 뒤,
  GitHub Contents API에서 그 SHA의 계약과 검증기를 raw로 읽는다. 검증 결과가
  `publish_eligible=true`일 때만 해당 SHA를 checkout하고 SNAPSHOT을 발행한다.
- 현재 workflow의 matrix 그룹과 계약 목록의 정렬은
  `scripts/test_release_workflow_policy.py`에서 회귀 계약으로 고정한다. 누락·추가·이름
  변경·비성공 shard를 거부하는 테스트도 함께 둔다.

## 결과

prefix 존재 여부 검사가 정확한 29개 matrix job 집합 검사로 바뀌었다. shard가 누락되거나
추가되거나 이름이 바뀌면 발행 job에 `true`가 전달되지 않는다. 검증기와 계약을
검증 대상 Nightly의 동일 SHA에서 읽으므로, 발행 대상 소스와 게이트 규칙의 ref가
엇갈리지 않는다.

기존 run은 새 계약 파일을 해당 SHA에 포함하지 않으므로 이 게이트를 통과하지 못한다.
변경이 통합된 뒤 새 Nightly를 성공시킨 다음 SNAPSHOT workflow를 실행해야 한다.
이는 이전 run을 재사용해 계약 검사를 우회하지 않는 fail-closed 동작이다.

## 검증

- `python3 -B -m unittest scripts/test_release_workflow_policy.py -v`: 19개 테스트 통과.
- `python3 -B -m py_compile scripts/validate_nightly_matrix.py scripts/test_release_workflow_policy.py`:
  통과.
- `git diff --check`: 통과.
- `actionlint 1.7.12`로 `.github/workflows/publish-snapshot.yml`과
  `.github/workflows/nightly-tests.yml` 검사: 오류 없음.
- [Nightly run #33062911098](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/33062911098)은
  `completed/success`, `.github/workflows/nightly-tests.yml`, `develop`,
  `5d0c22dab9169821fdaa75c321c2b1d627b2eb41`이었다. API pagination 결과 43개 job이
  모두 성공했고, 새 검증기에 동일 SHA를 기대값으로 전달해 `publish_eligible=true`를
  얻었다.

## 놓친 점과 주의사항

- matrix 그룹을 추가·삭제·이름 변경할 때 Nightly workflow와 계약 JSON을 같은 변경에서
  갱신해야 한다. 정책 테스트가 두 목록의 차이를 실패로 보고한다.
- GitHub API의 jobs 응답은 페이지 배열일 수 있으므로 검증기는 모든 페이지의 `jobs`를
  평탄화한다. 새 API 응답 형태를 도입하면 이 경계를 먼저 회귀 테스트한다.
- `workflow_run` 이벤트는 이벤트의 `head_sha`와 검증 run의 SHA가 같아야 한다. 수동
  실행은 선택한 완료 run의 branch와 workflow 경로를 별도로 확인한다.

## 향후 지침

- SNAPSHOT 발행 전에 prefix 기반 존재 검사나 수동 override를 다시 추가하지 않는다.
- Nightly matrix를 바꾸면 계약, 검증기 테스트, `actionlint`, 그리고 성공한 full
  Nightly run을 함께 확인한다.
- 이 게이트가 통합된 뒤에는 새 full Nightly의 고정 14개와 matrix 29개가 모두
  성공했다는 API 증거가 있어야 릴리스 train의 다음 이슈를 진행한다.

## 문서 SPW 감사

- SPW-01: PASS — 대상 독자는 릴리스·CI 유지보수자이며, 근거는 `#1559`,
  `.github/workflows/publish-snapshot.yml`, `.github/workflows/nightly-tests.yml`,
  `scripts/nightly_matrix_contract.json`, `scripts/validate_nightly_matrix.py`,
  정책 테스트와 run `33062911098`이다. 현재 ref에 대한 미확인 외부 주장은 포함하지
  않았다.
- SPW-02: PASS — 맥락, 결정, 결과, 검증, 주의사항, 향후 가드를 포함한다.
- SPW-03: PASS — 한국어 기술 문체를 사용하고 commands, identifiers, URL, SHA, 수치를
  원문 그대로 보존했다. `prefix`, `shard`, `head_sha`, `fail-closed`는 코드·릴리스
  경계 용어로 일관되게 사용했다.
- SPW-04: PASS — matrix 수(4/2/3/20), 고정 job 수(14), 전체 job 수(43), run 상태와
  SHA를 workflow·계약·실시간 API 결과와 대조했다.
- SPW-05: PASS — 최종 Markdown을 다시 읽어 링크, code token, 명령, 검증 범위와
  기존 run 재사용 제한을 확인했다.

## 한국어 자연스러움 감사

- KO-01: PASS — 식별자, 링크, 명령, 수치, SHA와 검증 불확실성을 보존했다.
- KO-02: PASS — 영향도 표현 대신 누락·추가·중복·비성공 job과 `publish_eligible` 결과를
  기술했다.
- KO-03: PASS — 번역투와 기계적인 나열을 줄이고 원인과 게이트 동작을 직접 서술했다.
- KO-04: PASS — 동일 개념에 `matrix job`, `shard`, `검증 대상 run` 용어를 일관되게
  적용했다.
- KO-05: PASS — 장식적 비유나 홍보 표현을 사용하지 않았다.
- KO-06: PASS — 제목, 본문, 링크, code token, 검증 목록을 읽어 확인했다. 이 lesson은
  단일 한국어 locale 문서이므로 대응 locale은 해당하지 않는다.
- KO-07: PASS — `audit-korean-terms.mjs` 실행 결과를 확인하고 의도하지 않은 용어
  충돌이 없음을 확인했다.
