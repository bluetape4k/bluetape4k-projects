# Module bluetape4k-geocode

## 개요

Reverse Geocode 서비스를 제공하는 곳은 대표적으로 Google 과 Microsoft 의 Bing 이 있다.

Google 은 자체 Library 인
[Google Map Services Java](https://github.com/googlemaps/google-maps-services-java)를 통해 다양한 기능을 제공하고,
Microsoft 는 Mobile 용 [maps-sdk](https://learn.microsoft.com/en-us/bingmaps/sdk-native/) 가 제공하지만 서버용은 없습니다. 대신
Bing의 [Bing Maps REST Services](https://learn.microsoft.com/en-us/bingmaps/rest-services/) 를 통해 지원합니다.

## 참고

### Google

* [GeoIP2 Java API](https://maxmind.github.io/GeoIP2-java/)

### Bing

* [Bing Maps REST Services](https://learn.microsoft.com/en-us/bingmaps/rest-services/)

## TODO

- 캐시 적용하기 (호출횟수 최소화)
- Resilience4j 를 활용한 resilience 적용하기
