# Module bluetape4k-javers-persistence-redis

[Javers](https://javers.org) 는 객체에 대한 Audit과 Diff 를 지원해주는 Framework 입니다. 기본적으로 메모리, MongoDB, JDBC 에 Audit 정보를 저장할 수 있는 기능을 제공합니다.

Bluetape4k Javers 에서는 위의 저장소 이외에, Redis 와 Kafka 를 통해 Event Sourcing 방식으로 CDC 를 지원할 수 있도록 해줍니다. 유사한 기능으로 Hibernate Enver 가 있습니다

## 참고 자료

- [Javers](https://javers.org)
- [Javers Feature Overview](https://javers.org/features)
- [Javers VS Envers Comparison](https://javers.org/blog/2017/12/javers-vs-envers-comparision.html)
- [Using JaVers for Data Model Auditing in Spring Data](https://www.baeldung.com/spring-data-javers-audit)
- [Spring Data에서 데이터 모델 감사를 위해 JaVers 사용](https://recordsoflife.tistory.com/486)
