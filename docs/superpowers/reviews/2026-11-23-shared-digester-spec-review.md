# 공통 Digest API 설계 통합 리뷰

## 검토 범위

- 대상: [공통 Digest API 설계](../specs/2026-11-23-shared-digester-design.md)
- 검토한 명세 SHA-256: `8e32eb0e2d541a48cab71302fe4ab69b2e6825872a761db15a788d4a7237b6c5`
- 관점: 성능, 안정성, 보안, 개발자 API/JVM ABI, 운영·이전, 소비자 사용성의 독립 리뷰 6건과 통합 비평 1건
- 리뷰는 모두 읽기 전용으로 수행했다. 명세 승인 전이므로 구현 테스트와 벤치마크는 실행하지 않았다.

## 최종 판정

**사용자 명세 검토 단계로 전달 가능.** 최종 6개 독립 리뷰와 통합 비평 모두 같은 명세 해시를 확인했고 P0=0, P1=0, P2=0, P3=0으로 판정했다. 통합 비평은 추가 필수 수정을 요구하지 않았다.

<!-- Inline verdict: PASS; P0=0, P1=0. -->

| 관점 | 독립 검토 역할 | 판정 | 확인한 경계 |
|---|---|---|---|
| 성능 | `test-engineer` | 통과, P0/P1/P2/P3=0 | 동일 입력의 직접 one-shot, API one-shot, 새 세션 증분, 재사용 세션 경로와 측정 조건 |
| 안정성 | `verifier` | 통과, P0/P1/P2/P3=0 | 독립 세션, `digest()`/`digestHex()` reset, 예외 전파와 이전 rollback |
| 보안 | `code-reviewer` | 통과, P0/P1/P2/P3=0 | 엄격한 Base64 canonical 검증, 비교 방식, zeroization 한계, MAC·서명 경계 |
| 개발자 API/JVM ABI | `code-reviewer` | 통과, P0/P1/P2/P3=0 | Kotlin/JVM 시그니처, Java overload, `TinkDigester` 호환성 |
| 운영·이전 | `planner` | 통과, P0/P1/P2/P3=0 | Projects 우선 순서, composite-build 대체, 외부 artifact 한계와 rollback |
| 소비자 사용성 | `analyst` | 통과, P0/P1/P2/P3=0 | 29개 파일·35개 호출, framing·출력 보존, API 예제와 인증 용도 구분 |
| 통합 비평 | `critic` | 통과, P0/P1/P2/P3=0 | 각 절의 계약 일관성, 작업 승인 경계, 누락 위험 |

## 이전 지적과 처리

| 지적 | 처리 |
|---|---|
| 초기 인벤토리가 24개 파일·6개 모듈로 실제 범위를 과소 집계했다. | Workshop production 코드를 다시 확인해 9개 모듈·29개 파일·35개 호출로 수정하고 파일별 mapping을 명세에 기록했다. |
| 호출자가 raw bytes, UTF-8, URL-safe unpadded Base64, 증분 입력을 구분할 예제가 부족했다. | 각 입력·출력 형태의 Kotlin 예제를 추가하고 두 URL-safe unpadded fingerprint에 형식을 명시했다. |
| 키 없는 digest와 인증 기능의 선택 경계가 분명하지 않았다. | 공유 비밀 기반 인증에는 keyed MAC, 공개 검증에는 서명을 사용한다고 명시했다. |
| `DigestSession.digestHex()`의 reset 의미가 API 본문에 빠졌다. | `digestHex()`가 현재 상태를 소비하고 초기 상태로 되돌린다고 API 계약과 재사용 조건에 명시했다. |
| 세션의 내부 상태가 안전하게 지워진다고 오해할 수 있었다. | `clear`/`close` 및 secure erasure를 보장하지 않는다고 명시하고 호출자 소유 배열의 zeroization 책임을 적었다. |
| 레거시 `TinkDigester.matches(String, String)`와 새 strict Base64 matcher의 차이가 숨겨져 있었다. | 기존 decoder 계약을 유지하고 canonical 표현 검증에는 새 API를 쓰도록 명시했으며 호환 테스트를 acceptance에 추가했다. |
| 벤치마크 비교 비용과 기록 위치가 모호했다. | 벤치마크 파일, JMH 모드·warmup·measurement, 입력 크기, 네 측정 경로, 세션 생성 비용 경계, 실행 명령과 결과 경로를 고정했다. |

## 통합 판단과 남은 승인 경계

`io/io`가 공통 `Digester`와 JDK 구현을 소유하고, `io/tink`의 `TinkDigester`는 기존 표면을 유지하는 JDK 기반 호환 어댑터로 둔다. 실제 Tink MAC은 키 없는 SHA-256과 다른 primitive이므로 별도의 unkeyed digest backend로 취급하지 않는다. 근거는 [Google Tink MAC 문서](https://developers.google.com/tink/mac)다.

Workshop 이전은 Projects API 구현·검증 이후에 진행한다. 원격 `2.2.0-SNAPSHOT` artifact가 없으므로 로컬 검증은 composite-build substitution으로 확인하고, 기본 checkout 또는 외부 CI 검증이 끝났다고 주장하지 않는다. Publication, PR, merge, tag, release는 이번 검토 범위에 포함되지 않는다.

**구현과 런타임 검증은 아직 시작하지 않았다.** 다음 단계는 사용자가 위 명세를 검토하는 것이다. 승인 후 별도 구현 계획을 작성하고 Projects API를 먼저 구현·검증한 뒤 Workshop 소비자를 이전한다.

## Writer DoD

- `SPW-01`: 검토 목적, 독자, 한국어 범위, 명세 해시와 근거 출처를 고정했다.
- `SPW-02`: 검토 범위, 심각도별 판정, 근거, 지적 처리, 남은 승인 경계를 기록했다.
- `SPW-03`: 한국어 기술 문체와 용어를 확인했고, 영어는 코드·API·역할명·URL에 한정했다.
- `SPW-04`: 최종 리뷰 결과와 명세의 수치·결정·경계가 같은지 대조했다.
- `SPW-05`: Markdown을 다시 읽었고 제목, 표, 링크, 코드 토큰을 확인했다. 용어 감사는 0건이다.
