# Module bluetape4k-hibernate-reactive

[hibernate-reactive](https://hibernate.org/reactive/)를 Kotlin에서 사용할 때 유용한 기능을 제공합니다.

## 참고

* [Hibernate Reactive 1.1 Reference Document](https://hibernate.org/reactive/documentation/1.1/reference/html_single/)
* [Hibernate Reactive - Getting Started Guide](https://thorben-janssen.com/hibernate-reactive-getting-started-guide/)

## FIXME

`hibernate-reactive` 최신 버전인 `1.1.9.Final` 은 `hibernate` 5.6.+ 를 사용하고 있고, `javax` 기반이다. (jakarta 기반이 아님)

그래서 현재 `hibernate 6.2+` 와 `hibernate-reactive 2.0.0-Beta2` 를 가지고 테스트 중인데, 초기화 중에 `SqlStatementLogger` 에서 예외가 발생하여 문제를
해결해야 한다 
