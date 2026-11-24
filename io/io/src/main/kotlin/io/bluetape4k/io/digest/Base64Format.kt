package io.bluetape4k.io.digest

/** Base64 alphabet과 padding 정책을 함께 지정합니다. */
enum class Base64Format {
    /** `+`와 `/` alphabet을 사용하고 padding을 포함합니다. */
    STANDARD_PADDED,

    /** `+`와 `/` alphabet을 사용하고 padding을 생략합니다. */
    STANDARD_UNPADDED,

    /** `-`와 `_` alphabet을 사용하고 padding을 포함합니다. */
    URL_SAFE_PADDED,

    /** `-`와 `_` alphabet을 사용하고 padding을 생략합니다. */
    URL_SAFE_UNPADDED,
}
