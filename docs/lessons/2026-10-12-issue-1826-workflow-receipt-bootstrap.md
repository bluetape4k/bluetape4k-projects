# Bluetape 워크플로 receipt 초기화와 head 검증

## 맥락

#1826 Type-B 작업은 승인된 기능 브랜치 worktree에서 `bluetape-flow.py` Phase 2
receipt를 시작하고, 승인된 네 파일만 수정해야 했다.

## 잘못된 판단과 증거

초기 부트스트랩에서 두 가지 오류가 발생했다. 첫 시도는 owner file을 state root 바로
아래에 두어 helper가 요구하는 `handles/` 경로를 벗어났다. 다음 시도는
`--expected-head`에 Git commit SHA를 전달했다. 이 값은 최신 receipt checksum이어야
하므로 해당 실행을 취소하고 승인된 worktree에서 새 실행을 만들었다. 두 오류 모두
소스 수정 전 발견되어 제품 파일에는 영향을 주지 않았다.

## 결정

owner file 경로와 receipt head는 서로 다른 계약으로 처리한다. helper가 state root의
`handles/` 아래에 owner file을 만들도록 하고 그 내용을 읽거나 기록하지 않는다.
`--expected-head`가 필요한 각 변경 명령 직전에 `verify --run-id`를 실행해 최신
소문자 receipt checksum을 얻는다. Git SHA는 receipt fencing 값으로 사용하지 않는다.
state root가 승인된 branch worktree와 다르거나 receipt 검증이 실패하면 파일을 수정하지
않고 receipt를 진단한 뒤 해당 실행을 취소하거나 복구한다.

## 향후 지침

1. 기능 작업은 승인된 격리 worktree에서 workflow `state-root`를 초기화한다.
2. helper가 만든 owner file이 `<state-root>/handles/`에 있는지와 권한을 확인하고,
   owner token 또는 fencing 값을 출력하지 않는다.
3. 모든 receipt 변경 전에 현재 receipt를 검증하고 그 checksum을
   `--expected-head`에 전달한다. 이전 명령의 checksum이나 `git rev-parse` 결과를
   재사용하지 않는다.
4. source 변경 전 `run-approve`, `run-start`, lane 시작/ACK, 정확한 대상의
   `mutation-check`가 모두 통과했는지 확인한다.
5. 구현 중 필수 lesson이나 문서 경로를 새로 발견하면, 아직 작성하지 않은 경우
   해당 경로를 lane `write_scope`에 등록한 뒤 lane 시작/ACK와 `mutation-check`를
   거친다. 이미 작성한 뒤 발견한 경우에는 기존 lane의 승인을 소급해 넓히지 않고,
   발견 시점과 기존 변경 사실을 기록한 다음 새 경로 전용 lane을 등록해 검증하고
   완료한다.

## 검증

- 잘못된 두 초기 실행은 source 수정 전에 각각 거부 또는 취소했고, 승인된 worktree에서
  새 receipt를 시작했다.
- 새 실행의 receipt 검증, main lane ACK, 네 대상 파일의 `mutation-check`가 통과한 뒤
  구현을 시작했다.
- lesson 파일은 `2026-10-12T13:40:48Z`에 생성되었다. 초기 생성 후 corrective
  lane을 만들기 전에 lesson 경로가 기존 lane의 `write_scope`에 빠졌던 사실을
  발견했으며, 앞서 생성한 파일을 승인된 변경으로 소급 처리하지 않았다.
  corrective lane은 `lane_created` 시각 `2026-10-12T13:50:55Z`, `lane_started`
  시각 `2026-10-12T13:51:22Z`, `startup_ack` 시각 `2026-10-12T13:51:43Z`로
  기록되었고, `2026-10-12T13:52:02Z`에 후속 검토와 수정을 수행했다.
- canonical GNO 색인은 아직 병합되지 않은 feature branch의 lesson이므로 통합 단계까지
  보류한다.
