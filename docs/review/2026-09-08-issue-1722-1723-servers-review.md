# 공용 Server 설계·계획 검토

## 범위와 근거

#1722/#1723 설계 및 계획, 공식 Testcontainers 2.0.5 소스, ConsulServer/ChromaDBServer, image-gate manifest와 Nightly 등록 계약을 검토했다.

| 관점 | 설계 | 계획 | 조치 |
|---|---|---|---|
| 성능 | P0/P1 없음 | P0/P1 없음 | 추가 polling 없이 공식 readiness와 Launcher 재사용 |
| 안정성 | P0/P1 없음 | 순차 실행 P1 해소 | 각 Server/infra 모듈을 별도 invocation --no-parallel |
| 보안 | P2 | P2/P3 | Docker binding과 인증 없는 테스트 전용 제약 문서화, secret export 없음 |
| 운영 | readiness/등록 P1 해소 | P2 | gRPC TCP 도달성, family53/release49/shard 분배와 Nightly 등록, image gate 실제 실행 |
| 개발자/API | playground P1 반박 | P2 | 실제 javap에서 setExposedPorts(List) 확인, getter 중복 방지, exact dependency alias |
| 호출자 | P2 | 순차 실행 P1 해소 | 명시적/공유 수명 예시와 기존 pagination/named-vector/UUID/오류 회귀 보존 |

독립 리뷰: server_inventory(성능·안정성·운영·호출자 개별 검토), server_docs(보안), openfga_impl(API). 메인 통합은 중복 지적을 합치고 현재 파일 및 실제 2.0.5 jar API로 확인했다. 실행 명령 재조회로 오래된 계획을 참조한 지적도 정정했다.

## 작성 검증

SPW-01~05 PASS: 한국어 엔지니어 대상, 이슈/현재 소스 근거, 설계→수용 기준→계획 연결, 기술 토큰 보존, 최종 Markdown 재조회. Korean terminology audit PASS. 코드 리뷰와 실행 결과는 구현 후 추가한다.

## 판정

설계·계획 P0=0/P1=0. 구현·테스트·코드 리뷰는 PENDING.
