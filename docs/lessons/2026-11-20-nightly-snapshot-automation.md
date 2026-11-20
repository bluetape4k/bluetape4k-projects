# Nightly 성공 후 Snapshot 자동 발행 연결 복원 교훈

## 맥락

`publish-snapshot.yml`은 2026-08-31의 commit `7eceeaa78`에서
`workflow_run` 기반 Nightly 연계를 지원했지만, 2026-09-01의 commit
`5920d4037e14ed21145f358acd3fcd90516eea51`에서 수동 발행 전용으로 바뀌었다.
따라서 2026-10-14의 마지막 Snapshot run `38551584769` 이후 성공한 Nightly가
Snapshot을 시작하지 않았다. 마지막 자동 Snapshot run은
`33227159196`(2026-08-29)이며, 최신 확인 Nightly run
`44645952553`(2026-11-20)도 Snapshot 후속 run을 만들지 않았다.

변경 당시의 문서와 PR 기록은 이 정책 전환을 의도된 manual-only handoff로
기록했지만, 현재 운영 기대와 Issue #1907의 복원 요구는 성공한 Nightly 후
자동 발행을 다시 보장하는 것이다.

## 원인

워크플로에서 `workflow_run` 트리거와 성공 조건이 제거되었고, checkout·exact
head 검증·summary가 모두 `github.sha`에 고정되었다. 그 결과 schedule로 실행된
Nightly의 성공 여부와 source SHA가 Snapshot workflow의 입력이 되지 않았다.

수동 발행만 통과하는 정책 테스트도 이 회귀를 허용했다. 특히 trigger 종류,
`workflow_run.conclusion`, source SHA 선택을 함께 검사하지 않아 자동 연결이
사라져도 테스트가 녹색이었다.

## 결정

- `workflow_run`을 `Nightly`의 `completed` 이벤트와 `develop` branch로 복원한다.
- `workflow_dispatch` 수동 복구 경로를 유지한다.
- publish job은 수동 실행 또는 Nightly conclusion이 `success`인 경우에만 실행한다.
- 자동 실행은 `github.event.workflow_run.head_sha`, 수동 실행은 `github.sha`를
  checkout·exact-head 검증·summary에 동일하게 사용한다.
- 보호된 `maven-central-release` environment, 다섯 개의 Maven Central secret,
  action commit pin, metadata 검증, 실제 publication command는 변경하지 않는다.
- 정책 테스트가 두 trigger, 성공 guard, branch filter, source identity를
  fail-closed로 검사하도록 복원한다.

## 검증

| 검사 | 결과 |
|---|---|
| TDD RED | manual-only workflow에 자동 trigger 계약을 먼저 적용한 뒤 정책 suite가 실패했다(8 failures). |
| TDD GREEN | `python3 -m unittest scripts/test_release_workflow_policy.py -v` — 54 tests, OK |
| Python 문법 | `python3 -m py_compile scripts/test_release_workflow_policy.py` — 통과 |
| YAML 정책 | `scripts/test_release_workflow_policy.py`가 action pin, secret, guard, exact SHA와 publication command를 통과시켰다. |
| 실제 발행 | Maven Central Snapshot dispatch는 실행하지 않았다. PR exact-head CI와 merge 후 Nightly read-back이 필요하다. |

## 재발 방지

Nightly 성공 후 Snapshot이 기대되는 동안에는 manual-only 정책으로 전환할 때
운영 기대와 issue/문서를 함께 갱신해야 한다. 자동 연결을 유지하는 경우 이
정책 테스트의 trigger·conclusion·source SHA 계약을 제거하지 않는다. PR 병합 후
다음 Nightly에서 `workflow_run` Snapshot run의 event, source SHA, conclusion,
publication 결과를 다시 읽어 자동 연결을 확인한다.
