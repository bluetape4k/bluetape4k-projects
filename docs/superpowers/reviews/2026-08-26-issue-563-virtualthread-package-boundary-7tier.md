# Issue #563 7-Tier 코드 리뷰

| Tier | 검토 영역 | 결과 | 근거 |
|---|---|---|---|
| T1 | 요구사항·경계 | PASS | core owner 유지, API Java 21 compatibility island 유지 |
| T2 | 아키텍처·의존성 | PASS | API를 core로 역이동하지 않고 `.api` subpackage로 분리해 cycle 회피 |
| T3 | API·ABI | WATCH | 이전 API package 소비자는 import 변경 후 재컴파일 필요 |
| T4 | 구현·ServiceLoader | PASS | JDK21/JDK25 provider import와 descriptor를 `.api` 계약으로 갱신 |
| T5 | 테스트·검증 | PASS | API boundary test, module descriptor/package scan, `validate-modules` exit 0 |
| T6 | 문서·운영 | PASS | 양언어 README, CHANGELOG, WIP, migration 명세 기록 |
| T7 | 전달·회귀 | WATCH | upstream 지원 PR merge와 새 snapshot graph 소비 검증은 후속 gate |

## 판정

```text
P0=0
P1=0
P2=1  (기존 package를 참조하는 외부 소비자 재컴파일 필요)
P3=0
```

upstream 지원 PR은 자체 compile/test/module validation 기준으로 READY다.
graph 후속 PR은 이 PR이 merge되고 새 snapshot이 소비 가능해진 뒤 exact-head
base를 갱신해야 한다. 전체 stacked train의 merge 승인은 별도 최종 gate다.
