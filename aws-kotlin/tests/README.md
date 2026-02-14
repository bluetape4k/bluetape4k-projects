# Module bluetape4k-aws-kotlin-tests

AWS Kotlin SDK 모듈 테스트를 위한 공통 Test Support 라이브러리입니다.

## 주요 기능

- **LocalStack 연동 유틸**: 테스트 환경에서 AWS 서비스 에뮬레이션 지원
- **컨테이너 확장 함수**: LocalStack 설정/초기화 보조
- **통합 테스트 보조**: 서비스별 테스트 코드 중복 제거

## 의존성 추가

```kotlin
dependencies {
    testImplementation("io.bluetape4k:bluetape4k-aws-kotlin-tests:${version}")
}
```

## 주요 기능 상세

- `LocalStackContainerExtensions.kt`
