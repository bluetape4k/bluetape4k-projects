# 실패 handler의 예외 우선순위를 런타임 간 고정한다

관련 이슈: https://github.com/bluetape4k/bluetape4k-projects/issues/1700

## 실패한 가정과 근거

JDK21은 handler 예외가 원래 실패를 대체했고 JDK25는 handler 실패를 버렸다.

## 결정

원래 작업 실패를 주 예외로 유지하고 handler의 별도 실패는 suppressed에 보존한다. 동일 객체 재전파는 self-suppression에서 제외한다.

## 재발 방지

같은 공통 테스트를 JDK21과 JDK25에서 실행한다. handler 안의 assertion은 삼켜질 수 있으므로 관찰값을 저장하고 handler 밖에서 검증한다.

## 검증

이 PR의 회귀 테스트에서 수정 전 동작 실패를 확인했다. 수정 후 테스트와 관련 모듈 검증 결과는 PR의 `DoD Status`에 기록한다.

## test fixture의 배포 의존성 검증

공통 fixture의 junit5 의존성은 Gradle 테스트에서는 통과했지만 Maven POM에서 junit5 → virtualthread-api → junit5 순환을 만들었다. 테스트 전용 variant라도 배포 모델의 optional 의존성에 포함될 수 있으므로 컴파일 성공만으로 의존성 경계를 판단하지 않는다. fixture에서 실제 필요한 assertions 모듈만 사용하고, 공유 fixture를 추가할 때 POM과 Gradle module metadata 생성·검증을 함께 실행한다.
