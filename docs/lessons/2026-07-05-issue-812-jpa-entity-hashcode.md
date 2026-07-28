# 이슈 #812 교훈

## 배경

`AbstractJpaEntity`는 transient entity를 `equalProperties`로 비교했지만, `id == null`일
때 `System.identityHashCode(this)`를 반환했다.

## 결정

Transient entity에는 Hibernate가 resolve한 entity class hash를 사용하고, persisted
entity에는 identifier 기반 hashing을 유지한다.

## 결과

동등한 transient entity는 이제 같은 hash bucket에 들어가므로, persistence가
identifier를 부여하기 전에도 hash-based collection이 이를 하나의 logical element로
다룬다.

## 향후 지침

Entity equality에 transient business-signature path가 있으면 hash-based collection
regression test를 추가한다. Business field로 equal이 될 수 있는 object에는 identity
hash를 사용하지 않는다.
