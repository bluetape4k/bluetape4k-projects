package io.bluetape4k.protobuf

/** Protobuf 메시지 기본 타입 별칭입니다. */
typealias ProtoMessage = com.google.protobuf.Message

/** Protobuf `Any` 타입 별칭입니다. */
typealias ProtoAny = com.google.protobuf.Any
/** Protobuf `Empty` 타입 별칭입니다. */
typealias ProtoEmpty = com.google.protobuf.Empty

/** Protobuf `Money` 타입 별칭입니다. */
typealias ProtoMoney = com.google.type.Money

/** Protobuf `Date` 타입 별칭입니다. */
typealias ProtoDate = com.google.type.Date
/** Protobuf `TimeOfDay` 타입 별칭입니다. */
typealias ProtoTime = com.google.type.TimeOfDay
/** Protobuf `DateTime` 타입 별칭입니다. */
typealias ProtoDateTime = com.google.type.DateTime

/** Protobuf `Duration` 타입 별칭입니다. */
typealias ProtoDuration = com.google.protobuf.Duration
/** Protobuf `Timestamp` 타입 별칭입니다. */
typealias ProtoTimestamp = com.google.protobuf.Timestamp

/** 기본 `Empty` 인스턴스입니다. */
@JvmField
val PROTO_EMPTY: ProtoEmpty = ProtoEmpty.getDefaultInstance()

/** 기본 `Any` 인스턴스입니다. */
@JvmField
val PROTO_ANY: ProtoAny = ProtoAny.getDefaultInstance()
