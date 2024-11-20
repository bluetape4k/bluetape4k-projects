# Module bluetape4k-micrometer

## 개요

Application 성능 측정을 수행하는 [micrometer.io](http://micrometer.io/) 에서 제공하지 않는 라이브러리에 대한 성능 측정 정보를 제공합니다.

## 사용법

Retrofit2 에 대한 성능 측정을 수행하기 위해 다음과 같이 설정 할 수 있습니다.

```kotlin
val registry = SimpleMeterRegistry()
val factory = MicrometerRetrofitMetricsFactory.create(registry)
val bintrayApi = createRetrofit(factory).service<BintrayApi>()


// MicrometerRetrofitMetricsFactory 를 이용하여 Retrofit2의 성능을 측정합니다.
private fun createRetrofit(factory: CallAdapter.Factory): Retrofit {
    return retrofitOf(TestService.BintrayApiBaseUrl) {
        callFactory(asyncClientCallFactoryOf())
        addConverterFactory(getDefaultConverterFactory())
        addCallAdapterFactory(factory)

        if (isPresentRetrofitAdapterRxJava2()) {
            addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
        }
        if (classIsPresent("com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory")) {
            addCallAdapterFactory(ReactorCallAdapterFactory.createAsync())
        }
    }
}
```
