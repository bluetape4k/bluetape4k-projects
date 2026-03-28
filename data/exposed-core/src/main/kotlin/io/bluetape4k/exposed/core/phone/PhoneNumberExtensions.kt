package io.bluetape4k.exposed.core.phone

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

/**
 * [PhoneNumber] 객체를 저장하는 전화번호 컬럼을 테이블에 등록한다.
 *
 * DB에는 E.164 형식 문자열로 저장되며, 읽을 때는 [PhoneNumber] 객체로 변환된다.
 *
 * **의존성**: `com.googlecode.libphonenumber:libphonenumber`가 런타임에 필요하다.
 *
 * @param name 컬럼 이름
 * @param defaultRegion 전화번호 파싱 시 사용할 기본 국가 코드 (기본값: "KR")
 * @return [PhoneNumber] 타입의 [Column]
 */
fun Table.phoneNumber(name: String, defaultRegion: String = "KR"): Column<PhoneNumber> =
    registerColumn(name, PhoneNumberColumnType(defaultRegion))

/**
 * 전화번호를 E.164 형식 문자열로 저장하는 컬럼을 테이블에 등록한다.
 *
 * 입력된 전화번호 문자열을 E.164 형식으로 정규화하여 저장하며,
 * 읽을 때도 E.164 형식 문자열 그대로 반환한다.
 *
 * **의존성**: `com.googlecode.libphonenumber:libphonenumber`가 런타임에 필요하다.
 *
 * @param name 컬럼 이름
 * @param defaultRegion 전화번호 파싱 시 사용할 기본 국가 코드 (기본값: "KR")
 * @return [String] 타입의 [Column]
 */
fun Table.phoneNumberString(name: String, defaultRegion: String = "KR"): Column<String> =
    registerColumn(name, PhoneNumberStringColumnType(defaultRegion))
