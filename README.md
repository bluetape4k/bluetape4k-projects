# Bluetape4k Projects

Kotlin 언어로 JVM 환경에서 개발할 때 사용할 공용 라이브러리

## 소개

Kotlin 언어를 배우고, 사용하면서, Backend 개발에 자주 사용하는 기술, Coroutines 등 기존 라이브러리가 제공하지 않는 기능 들을 개발해 왔습니다.

1. Kotlin 의 장점을 최대화 할 수 있는 추천할 만한 코딩 스타일을 제공할 수 있는 기능을 제공합니다.
    - `bluetape4k-core` 의 assertions, required 같은 기능
    - `kommins-units` 의 단위를 표현하는 value class 제공

2. 기존 Java 라이브러리를 무지성으로 사용하지 않고, 좀 더 효과적으로 사용할 수 있도록 개선한 기능을 제공합니다.
    - `bluetape4k-core` 의 LZ4, Zstd 등 압축 기능 개선
    - `bluetape4k-redis` 의 lettuce, redisson 용 Codec 제공 (공식 Codec 보다 성능이 월등함)

3. 테스트를 좀 더 완성도 있게 하기 위한 기능을 제공합니다.
    - `bluetape4k-junit5` 다양한 테스트 기법을 Junit5 기반으로 제공합니다.
    - `bluetape4k-testcontainers` 다양한 서비스들을 테스트 환경에서 사용할 수 있도록 합니다.

3. Kotlin Coroutines 등 Async/Non-Blocking 방식의 개발을 지원하는 기능을 제공합니다.
    - `bluetape4k-coroutines` Coroutine 을 사용할 때 유용한 기능을 제공합니다.
    - `bluetape4k-feigh`, `bluetape4k-retrofit2` 등은 HTTP 통신 시 async/non-blocking을 위해 Coroutines 을 사용하도록 합니다

4. AWS SDK 사용 시 성능을 위해 개선한 기능을 제공합니다.
    - `bluetape4k-aws-xxxx` AWS Java SDK 사용 시 Async/Non-Blocking 방식으로 사용할 수 있도록 합니다.
    - `bluetape4k-aws-s3` 는 S3 사용 시 Async/Non-Blocking 방식, TransferManager 를 사용하여, 대용량 파일 전송 시 성능을 향상할 수 있습니다.

5. AWS Kotlin SDK 를 사용을 편리하게 하기 위한 기능을 제공합니다.
    - `bluetape4k-aws-kotlin-xxxx` AWS Kotlin SDK 를 사용할 때 유용하고, 고성능을 지원할 수 있도록 Coroutines 를 활용하는 기능 및 예제를 제공합니다.

6. MSA의 필수인 Resilience4j 에 대한 Kotlin Coroutines 지원을 강화했습니다.
    - `bluetape4k-resilience4j` Resilience4j 를 사용할 때 Kotlin Coroutines 를 사용할 수 있도록 지원합니다.
    - 또한 Coroutines 용 Cache를 추가하여, Coroutines 환경에서도 API 호출 결과를 캐싱할 수 있도록 지원합니다.

7. Redis 를 댜양한 방식에서 사용할 수 있도록 지원합니다.
    - `bluetape4k-redis`는 Lettuce, Redisson 용 고성능 Codec 을 제공합니다.
    - Redisson의 다양한 Lock 기능을 Coroutines 환경에서도 사용할 수 있도록 지원합니다.
    - Redis를 분산 캐시로만 사용하는 것이 아니라, Near Cache로 사용할 수 있도록 하여 더욱 성능을 높힐 수 있도록 합니다.

그 외 현업에서 마주쳤던 많은 문제를 해결하는 과정에서 필요로 하는 기능 들을 제공합니다.

앞으로도 필요한 기능들이 있다면 Issue 에 제안 주시기 바랍니다.

## 배포 방법

버전 확인은 `gradle.properties` 파일에서 확인

```properties
projectGroup=io.bluetape4k
baseVersion=0.0.1
snapshotVersion=-SNAPSHOT
```

SNAPSHOT 배포 시에는 다음과 같이 간단하게 Maven에 배포합니다.

```bash
$ ./gradlew publishMavenPublicationToMavenRepository
```

RELEASE 배포 시에는 다음과 같이 `snapshotVersion` 정보를 없애고, Maven에 배포합니다.

```bash
$ ./gradlew publishMavenPublicationToMavenRepository -PsnapshotVersion=
```
