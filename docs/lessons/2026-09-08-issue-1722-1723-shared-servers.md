# 인프라 통합 테스트의 공용 Server 재사용

## 배경과 사용자 보완 요청

OpenFGA·Qdrant infra 모듈의 첫 구현은 테스트 내부에 GenericContainer를 직접 구성했다. 사용자는 새 #1722/#1723을 검토하고 공용 Testcontainers Server를 구현해서 사용하도록 요청했다. 앞으로 새 infra 통합 테스트를 추가할 때 공용 Server 제공 여부와 선행 이슈를 먼저 확인한다.

## 결정과 검증

공식 OpenFGAContainer/QdrantContainer 2.0.5를 재사용하고, SDK 중립 Server가 HTTP/gRPC endpoint와 수명을 소유한다. SDK 의미 검증은 infra 테스트에 남기고 공용 모듈은 최소 대표 작업으로 연결·준비 상태·종료를 확인한다. 57개 Kotlin 테스트와 65개 Python 계약, 두 이미지 게이트 각각 1/1이 통과했다.

## 드러난 잘못된 가정과 예방

- API 검토에서 상위 컨테이너의 playground exposed port를 제거할 수 없다고 판단했으나, 실제 jar의 `setExposedPorts(List)`로 교체할 수 있었다. 공개 getter/setter의 실제 바이너리 API를 먼저 확인한다.
- 이미지 이름 getter를 순수 설정 조회라고 가정했으나 `dockerImageName`은 실제 pull을 수행했다. 가상 custom-tag를 사용한 테스트가 404로 실패했다. 설정 검증에 존재하지 않는 이미지 태그를 사용하지 않는다.
- 표준 readiness 통과만으로 gRPC endpoint 검증을 갈음하지 않는다. 대표 workload에서 실제 mapped gRPC 도달성을 확인하고 TCP 연결과 RPC 의미 검증을 구분한다.
- Server 추가 시 source/test만 보지 않는다. image-gate family/release 수, shard 분배, 한영 README, Nightly의 명시 클래스 목록을 함께 검사한다.
- 기존 Jib Docker build 선행 작업은 configuration cache를 지원하지 않는다. 해당 테스트 명령에는 `--no-configuration-cache`를 사용하며 실제 테스트 실패와 캐시 경고를 분리한다.
- Kotlin 테스트 구현 중 긴 함수와 부정확한 JSON 문자열 구성이 발견되었다. REST 요청은 구조화된 body로 작성하고 수명·endpoint·작업 검증을 작은 private 함수로 나누어 신규 detekt 진단을 제거한다.

## 남은 범위

기존 testcontainers의 변경하지 않은 파일에서 detekt 진단 46개가 남아 있다. 이번 수정 파일의 신규 진단은 0이다. 발행·병합은 별도 권한과 최신 CI 검증을 따른다.
