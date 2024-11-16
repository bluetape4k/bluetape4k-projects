package io.bluetape4k.apache

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

/**
 *  Reflection을 사용하여 객체의 속성정보를 문자열로 변환합니다.
 *
 *  ```
 *  val obj = MyClass()
 *  obj.reflectionToString() // MyClass@1234(a=1,b=2)
 *  ```
 */
fun Any?.reflectionToString(style: ToStringStyle = ToStringStyle.DEFAULT_STYLE): String {
    return if (this != null) ToStringBuilder.reflectionToString(this, style) else ""
}
