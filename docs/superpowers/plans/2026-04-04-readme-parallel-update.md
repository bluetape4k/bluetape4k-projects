# README 병렬 최신화 구현 계획

> **For agentic workers:
** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (
`- [ ]`) syntax for tracking.

**Goal:** 12개 claude-session-driver 워커를 병렬 실행하여 bluetape4k-projects의 모든 모듈 README.md를 git 이력 기반으로 최신화한다.

**Architecture:
** 컨트롤러(Claude)가 SCRIPTS 경로의 launch-worker.sh / send-prompt.sh / wait-for-event.sh 를 순서대로 호출해 12개 워커를 동시에 띄운 뒤 stop 이벤트를 기다린다. 각 워커는 담당 모듈의 git log를 분석해 README 이후 변경사항만 반영한다.

**Tech Stack:** claude-session-driver 1.0.1 scripts, tmux, jq, git, Kotlin/Gradle (bluetape4k-projects)

---

## 공통 변수

```bash
SCRIPTS="/Users/debop/.claude/plugins/cache/superpowers-marketplace/claude-session-driver/1.0.1/scripts"
PROJECT="/Users/debop/work/bluetape4k/bluetape4k-projects"
```

---

## 워커별 프롬프트 템플릿

각 워커에게 보낼 프롬프트 패턴 (모듈 목록만 교체):

```
다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: /Users/debop/work/bluetape4k/bluetape4k-projects

담당 모듈:
<모듈 목록>

각 모듈에 대해 다음을 수행하라:
1. README.md의 마지막 git 커밋 날짜 확인:
   git -C <project> log -1 --format="%ci" -- <module>/README.md

2. 그 이후 모듈 변경사항 파악:
   git -C <project> log --oneline --since="<readme-date>" -- <module>/

3. 변경된 파일 목록 확인 후 README와 대조:
   - 새 클래스/함수/설정이 README에 없으면 추가
   - 삭제된 API가 README에 남아있으면 제거
   - 전면 재작성 금지 — 변경된 부분만 수정

4. README 수정 시 준수사항:
   - 설명은 한국어
   - 코드 예시는 현행 Kotlin API 기준
   - 기존 섹션 구조 유지

변경사항이 없는 모듈은 건너뛴다.
```

---

### Task 1: 사전 요구사항 확인

**Files:**

- 확인만 (수정 없음)

- [ ] **Step 1: tmux 설치 확인**

```bash
tmux -V
```

Expected: `tmux 3.x` 이상

- [ ] **Step 2: jq 설치 확인**

```bash
jq --version
```

Expected: `jq-1.x` 이상

- [ ] **Step 3: SCRIPTS 경로 확인**

```bash
ls "$SCRIPTS/launch-worker.sh"
```

Expected: 파일 존재 출력

---

### Task 2: W1 ~ W6 워커 병렬 실행 (1차 배치)

**Files:** 없음 (워커 실행만)

- [ ] **Step 1: W1 실행 — bluetape4k/ 코어 모듈**

```bash
R1=$("$SCRIPTS/launch-worker.sh" bt4k-w1 "$PROJECT")
S1=$(echo "$R1" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w1 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- bluetape4k/core
- bluetape4k/coroutines
- bluetape4k/logging

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

- [ ] **Step 2: W2 실행 — exposed 핵심 모듈**

```bash
R2=$("$SCRIPTS/launch-worker.sh" bt4k-w2 "$PROJECT")
S2=$(echo "$R2" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w2 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- data/exposed-core
- data/exposed-dao
- data/exposed-jdbc
- data/exposed-r2dbc
- data/exposed

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

- [ ] **Step 3: W3 실행 — exposed 캐시 연동 모듈**

```bash
R3=$("$SCRIPTS/launch-worker.sh" bt4k-w3 "$PROJECT")
S3=$(echo "$R3" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w3 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- data/exposed-jdbc-lettuce
- data/exposed-r2dbc-lettuce
- data/exposed-jdbc-redisson
- data/exposed-r2dbc-redisson

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

- [ ] **Step 4: W4 실행 — exposed DB 방언 모듈**

```bash
R4=$("$SCRIPTS/launch-worker.sh" bt4k-w4 "$PROJECT")
S4=$(echo "$R4" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w4 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- data/exposed-postgresql
- data/exposed-mysql8
- data/exposed-duckdb
- data/exposed-trino
- data/exposed-bigquery

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

- [ ] **Step 5: W5 실행 — exposed 직렬화/암호화 모듈**

```bash
R5=$("$SCRIPTS/launch-worker.sh" bt4k-w5 "$PROJECT")
S5=$(echo "$R5" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w5 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- data/exposed-jackson2
- data/exposed-jackson3
- data/exposed-fastjson2
- data/exposed-jasypt
- data/exposed-tink
- data/exposed-measured

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

- [ ] **Step 6: W6 실행 — Hibernate/기타 데이터 모듈**

```bash
R6=$("$SCRIPTS/launch-worker.sh" bt4k-w6 "$PROJECT")
S6=$(echo "$R6" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w6 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- data/hibernate
- data/hibernate-reactive
- data/hibernate-cache-lettuce
- data/cassandra
- data/jdbc
- data/mongodb
- data/r2dbc

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

---

### Task 3: W7 ~ W12 워커 병렬 실행 (2차 배치)

- [ ] **Step 1: W7 실행 — infra/lettuce, redisson, redis, cache**

```bash
R7=$("$SCRIPTS/launch-worker.sh" bt4k-w7 "$PROJECT")
S7=$(echo "$R7" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w7 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- infra/lettuce
- infra/redisson
- infra/redis
- infra/cache
- infra/cache-core

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

- [ ] **Step 2: W8 실행 — infra 나머지 모듈**

```bash
R8=$("$SCRIPTS/launch-worker.sh" bt4k-w8 "$PROJECT")
S8=$(echo "$R8" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w8 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- infra/cache-hazelcast
- infra/cache-redisson
- infra/cache-lettuce
- infra/kafka
- infra/bucket4j
- infra/resilience4j
- infra/micrometer
- infra/opentelemetry

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

- [ ] **Step 3: W9 실행 — io 직렬화 모듈**

```bash
R9=$("$SCRIPTS/launch-worker.sh" bt4k-w9 "$PROJECT")
S9=$(echo "$R9" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w9 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- io/io
- io/okio
- io/jackson2
- io/jackson3
- io/fastjson2
- io/json

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

- [ ] **Step 4: W10 실행 — io 통신/네트워크 모듈**

```bash
R10=$("$SCRIPTS/launch-worker.sh" bt4k-w10 "$PROJECT")
S10=$(echo "$R10" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w10 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- io/feign
- io/retrofit2
- io/grpc
- io/protobuf
- io/avro
- io/netty
- io/http
- io/csv
- io/tink
- io/crypto

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

- [ ] **Step 5: W11 실행 — aws, virtualthread**

```bash
R11=$("$SCRIPTS/launch-worker.sh" bt4k-w11 "$PROJECT")
S11=$(echo "$R11" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w11 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- aws/aws
- aws/aws-kotlin
- virtualthread/api
- virtualthread/jdk21
- virtualthread

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

- [ ] **Step 6: W12 실행 — spring-boot3/4**

```bash
R12=$("$SCRIPTS/launch-worker.sh" bt4k-w12 "$PROJECT")
S12=$(echo "$R12" | jq -r '.session_id')
"$SCRIPTS/send-prompt.sh" bt4k-w12 "다음 모듈들의 README.md를 최신화하라. 프로젝트 루트: $PROJECT

담당 모듈:
- spring-boot3/core
- spring-boot3/redis
- spring-boot3/r2dbc
- spring-boot3/mongodb
- spring-boot3/cassandra
- spring-boot3/exposed-jdbc
- spring-boot3/exposed-r2dbc
- spring-boot3/hibernate-lettuce
- spring-boot3/exposed-jdbc-demo
- spring-boot3/exposed-r2dbc-demo
- spring-boot3/cassandra-demo
- spring-boot3/hibernate-lettuce-demo
- spring-boot4/core
- spring-boot4/redis
- spring-boot4/r2dbc
- spring-boot4/mongodb
- spring-boot4/cassandra
- spring-boot4/exposed-jdbc
- spring-boot4/exposed-r2dbc
- spring-boot4/hibernate-lettuce
- spring-boot4/exposed-jdbc-demo
- spring-boot4/exposed-r2dbc-demo
- spring-boot4/cassandra-demo
- spring-boot4/hibernate-lettuce-demo

각 모듈에 대해:
1. README.md 마지막 커밋 날짜: git -C $PROJECT log -1 --format='%ci' -- <module>/README.md
2. 그 이후 변경사항: git -C $PROJECT log --oneline --since='<날짜>' -- <module>/
3. 신규/삭제 클래스·함수를 README에 반영 (변경된 부분만, 전면 재작성 금지)
4. 설명은 한국어, 코드 예시는 현행 Kotlin API 기준, 기존 섹션 구조 유지
변경없는 모듈은 건너뜀."
```

---

### Task 4: 1차 배치 (W1~W6) 완료 대기

- [ ] **Step 1: W1~W6 stop 이벤트 대기 (각 10분)**

```bash
"$SCRIPTS/wait-for-event.sh" "$S1" stop 600
"$SCRIPTS/wait-for-event.sh" "$S2" stop 600
"$SCRIPTS/wait-for-event.sh" "$S3" stop 600
"$SCRIPTS/wait-for-event.sh" "$S4" stop 600
"$SCRIPTS/wait-for-event.sh" "$S5" stop 600
"$SCRIPTS/wait-for-event.sh" "$S6" stop 600
```

Expected: 각 명령이 exit code 0으로 종료

- [ ] **Step 2: W1~W6 결과 확인**

```bash
for sid in "$S1" "$S2" "$S3" "$S4" "$S5" "$S6"; do
  echo "=== $sid ===" && "$SCRIPTS/read-turn.sh" "$sid" 2>/dev/null | tail -20
done
```

- [ ] **Step 3: W1~W6 정리**

```bash
"$SCRIPTS/stop-worker.sh" bt4k-w1 "$S1"
"$SCRIPTS/stop-worker.sh" bt4k-w2 "$S2"
"$SCRIPTS/stop-worker.sh" bt4k-w3 "$S3"
"$SCRIPTS/stop-worker.sh" bt4k-w4 "$S4"
"$SCRIPTS/stop-worker.sh" bt4k-w5 "$S5"
"$SCRIPTS/stop-worker.sh" bt4k-w6 "$S6"
```

---

### Task 5: 2차 배치 (W7~W12) 완료 대기

- [ ] **Step 1: W7~W12 stop 이벤트 대기**

```bash
"$SCRIPTS/wait-for-event.sh" "$S7" stop 600
"$SCRIPTS/wait-for-event.sh" "$S8" stop 600
"$SCRIPTS/wait-for-event.sh" "$S9" stop 600
"$SCRIPTS/wait-for-event.sh" "$S10" stop 600
"$SCRIPTS/wait-for-event.sh" "$S11" stop 600
"$SCRIPTS/wait-for-event.sh" "$S12" stop 600
```

- [ ] **Step 2: W7~W12 결과 확인**

```bash
for sid in "$S7" "$S8" "$S9" "$S10" "$S11" "$S12"; do
  echo "=== $sid ===" && "$SCRIPTS/read-turn.sh" "$sid" 2>/dev/null | tail -20
done
```

- [ ] **Step 3: W7~W12 정리**

```bash
"$SCRIPTS/stop-worker.sh" bt4k-w7 "$S7"
"$SCRIPTS/stop-worker.sh" bt4k-w8 "$S8"
"$SCRIPTS/stop-worker.sh" bt4k-w9 "$S9"
"$SCRIPTS/stop-worker.sh" bt4k-w10 "$S10"
"$SCRIPTS/stop-worker.sh" bt4k-w11 "$S11"
"$SCRIPTS/stop-worker.sh" bt4k-w12 "$S12"
```

---

### Task 6: 변경사항 커밋

- [ ] **Step 1: 변경된 README 목록 확인**

```bash
git -C "$PROJECT" diff --name-only | grep README.md
```

- [ ] **Step 2: 커밋**

```bash
git -C "$PROJECT" add $(git -C "$PROJECT" diff --name-only | grep README.md)
git -C "$PROJECT" commit -m "docs: 모든 모듈 README.md 최신화 (git 이력 기반)"
```

---

### Task 7: testlog.md 기록

- [ ] **Step 1: testlog.md에 작업 결과 기록**

`docs/testlog.md` 마지막 5행 읽은 후 아래 행 추가:

| 날짜         | 작업         | 대상           | 테스트 항목           | 결과   | 소요 | 비고          |
|------------|------------|--------------|------------------|------|----|-------------|
| 2026-04-04 | README 최신화 | 전체 모듈 (~70개) | README git 이력 반영 | ✅ 성공 | -  | 12 워커 병렬 실행 |
