# TODO Issue Registration Design Spec

> Date: 2026-04-24  
> Scope: `TODO.md`, GitHub Issues backlog  
> Status: Draft

---

## 1. Brainstorming

### 1.1 Problem & Constraints Recap

사용자는 `TODO.md`에 쌓여 있는 항목 중 실제 작업 가능한 후보를 골라 **GitHub issue로 등록**해 달라고 요청했다. 단, `TODO.md`는 단순 작업 목록이 아니라 다음이 혼재되어 있다.

- 한 PR로 닫을 수 있는 실행 항목
- 여러 PR로 쪼개야 하는 중대형 주제
- 아직 탐색/검토가 먼저인 아이디어 풀

이번 설계에서 확정된 제약은 다음과 같다.

1. **작은 항목은 전부 issue로 등록한다.**
2. **큰 항목은 Epic issue로 등록한다.**
3. **제목과 본문은 모두 한국어로 작성한다.**
4. **TODO.md를 그대로 복붙하지 않고, 실행 가능한 단위로 재구성한다.**
5. **이번 패스에서는 label 체계 개편, TODO.md 구조 개편, 광범위한 백로그 정리는 하지 않는다.**

### 1.2 Approaches Compared

#### Approach A: TODO 항목 전량 issue화

- 장점: `TODO.md` 전체가 즉시 GitHub 백로그가 된다.
- 단점: 탐색 메모와 장기 아이디어까지 issue가 되어 노이즈가 커진다.

#### Approach B: 작은 항목 전부 issue화 + 큰 항목은 Epic으로 등록 + 탐색 항목은 TODO 유지 ✅ 채택

- 장점: 즉시 실행 가능한 작업은 바로 추적할 수 있고, 큰 주제도 Epic으로 잃지 않는다.
- 장점: `TODO.md`를 백로그 인덱스로 유지하면서 GitHub issue는 실행 단위 중심으로 정리된다.
- 단점: 분류 판단이 필요하다.

#### Approach C: 우선순위 상위 몇 개만 issue화

- 장점: 가장 보수적이고 빠르다.
- 단점: 사용자가 요청한 “바로 실행 가능한 것 전부” 기준과 맞지 않는다.

### 1.3 Selected Approach

**Approach B**를 채택한다.

핵심 원칙은 다음과 같다.

- **작은 issue**: 가능하면 한 PR로 닫을 수 있는 범위
- **Epic issue**: 여러 PR 또는 phase로 나뉘는 큰 주제
- **보류 항목**: 조사/분해가 먼저 필요한 넓은 항목은 `TODO.md`에 유지

---

## 2. Detailed Design

### 2.1 Selection Rules

#### Small issue로 등록하는 기준

다음 조건을 만족하면 small issue로 등록한다.

- 작업 범위가 비교적 명확하다.
- 완료 조건을 체크리스트로 작성할 수 있다.
- 가능하면 한 PR로 닫을 수 있다.
- 테스트/README/KDoc/검증 기준을 issue 본문에 명시할 수 있다.

#### Epic issue로 등록하는 기준

다음 조건이면 Epic으로 등록한다.

- 한 번에 끝낼 수 없는 중대형 작업이다.
- phase 또는 하위 issue로 분해가 필요하다.
- 전략적 방향과 후속 작업 추적이 더 중요하다.

#### 이번 패스에서 보류하는 기준

다음 항목은 이번에 issue로 등록하지 않는다.

- 신규 모듈 아이디어 풀
- 문서화/보안/품질처럼 범위가 광범위한 묶음
- 먼저 조사/분해/우선순위화가 필요한 항목

### 2.2 Candidate Mapping

#### Small issue 후보

검증 결과 `core 모듈 Deprecated 정리`는 2026-04-17 기준 완료 처리되어 이번 issue 등록 범위에서 제외한다.

1. `utils/science — NetCdf 지원 완성`
2. `examples/jpa-querydsl-demo — QueryDSL 쿼리 완성`
3. `io 모듈 레거시 정리`
4. `infra 모듈 정리`
5. `testing/testcontainers — HazelcastServer 수정`
6. `Spring Boot 3 / 4 동기화 유지`
7. `Redis Codec — ForyFast 지원 추가`

#### Epic issue 후보

1. `[Epic] x-obsoleted 처리 계획`
2. `[Epic] Javers + Exposed = Event Sourcing / CQRS / DDD`

#### 이번 패스 보류 항목

- `모듈 신규 추가 검토`
- `문서화 개선`
- `빌드 / CI 개선`
- `보안`
- `성능 / 품질`

### 2.3 Issue Body Shape

#### Small issue 본문 구조

- 배경
- 작업 범위
- 완료 기준
- 참고 위치

공통 완료 기준에는 가능한 범위에서 다음을 포함한다.

- 코드 변경 완료
- 관련 테스트 통과
- 변경 모듈 `README.md` + `README.ko.md` 동기화
- 필요 시 `docs/testlogs/YYYY-MM.md` 기록

#### Epic issue 본문 구조

- 목표
- 포함할 하위 주제
- 이번 패스 제외 범위
- 예상 후속 issue
- 참고 위치

Epic은 즉시 구현 체크리스트보다 **분해와 추적**에 초점을 둔다.

### 2.4 Registration Workflow

실제 등록은 다음 순서로 진행한다.

1. 기존 GitHub issue를 확인해 중복 가능성을 점검한다.
2. `TODO.md` 문맥을 바탕으로 각 issue 제목/본문을 한국어로 작성한다.
3. small issue를 먼저 등록한다.
4. Epic issue를 등록한다.
5. 필요하면 Epic 본문에 관련 small issue 후보를 연결한다.

이번 패스에서는 다음은 하지 않는다.

- GitHub project/label/milestone 체계 재설계
- TODO.md 대규모 구조 개편
- 보류 항목까지 억지로 issue화

---

## 3. Resolved Decisions

이번 brainstorming에서 아래가 확정되었다.

- issue 크기 기준: **가능하면 한 issue = 한 PR**
- issue 개수 기준: **바로 실행 가능한 것 전부**
- 큰 항목 처리: **Epic issue로 함께 등록**
- issue 언어: **제목/본문 모두 한국어**

---

## 4. Success Criteria

이번 작업이 성공으로 간주되려면 다음을 만족해야 한다.

1. small issue 후보가 한국어 제목/본문으로 모두 등록된다.
2. 큰 주제 2개가 Epic issue로 등록된다.
3. 보류 항목은 issue로 억지 등록하지 않고 `TODO.md`에 남긴다.
4. 중복 issue가 없도록 사전 확인을 수행한다.
5. 각 issue 본문에 배경, 범위, 완료 기준, 참고 위치가 포함된다.

---

## 5. Out of Scope

이번 작업 범위에 포함하지 않는다.

- `TODO.md` 전체 재작성
- 신규 모듈 후보 전수 issue화
- label/milestone/project board 설계
- issue 등록 이후의 실제 구현 착수
