package io.bluetape4k.io.serializer

/**
 * Describes the trust boundary expected by a serialization codec.
 *
 * These profiles are documentation and configuration vocabulary for codecs
 * that differ in how much serialized data can influence deserialization.
 */
enum class SerializationTrustProfile(
    /**
     * Stable display name used in public documentation.
     */
    val displayName: String,
) {
    /**
     * Data is read only from a fully trusted internal boundary.
     */
    TRUSTED_INTERNAL("TrustedInternal"),

    /**
     * Dynamic type loading is allowed only for configured package prefixes,
     * class names, or object input filters.
     */
    ALLOW_LISTED_TYPES("AllowListedTypes"),

    /**
     * The caller supplies the target type statically; serialized data does not
     * choose the class to instantiate.
     */
    NO_DYNAMIC_TYPE_LOADING("NoDynamicTypeLoading"),

    /**
     * Legacy allow-all behavior is enabled explicitly for migration.
     */
    UNSAFE_LEGACY_COMPATIBILITY("UnsafeLegacyCompatibility"),
}
