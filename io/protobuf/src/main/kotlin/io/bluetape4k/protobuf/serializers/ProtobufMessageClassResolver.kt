package io.bluetape4k.protobuf.serializers

import com.google.protobuf.Message
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

internal class ProtobufMessageClassResolver {

    private companion object {
        const val MAX_CLASSES_PER_LOADER = 256
    }

    private val staleLoaders = ReferenceQueue<ClassLoader>()
    private val loaderCaches = ConcurrentHashMap<LoaderKey, LoaderBucket>()
    private val bootstrapCache = LoaderBucket()

    fun resolve(className: String, classLoader: ClassLoader?): Class<out Message> {
        expungeStaleLoaders()
        val bucket = bucketFor(classLoader)
        return bucket.resolve(className) { loadMessageClass(className, classLoader) }
    }

    private fun bucketFor(classLoader: ClassLoader?): LoaderBucket =
        if (classLoader == null) {
            bootstrapCache
        } else {
            loaderCaches.computeIfAbsent(LoaderKey(classLoader, staleLoaders)) { LoaderBucket() }
        }

    private fun loadMessageClass(className: String, classLoader: ClassLoader?): Class<out Message> {
        val resolved = Class.forName(className, false, classLoader)
        if (!Message::class.java.isAssignableFrom(resolved)) {
            throw SecurityException(
                "Resolved Protobuf class $className does not implement ${Message::class.java.name}."
            )
        }
        return resolved.asSubclass(Message::class.java)
    }

    private fun expungeStaleLoaders() {
        while (true) {
            val stale = staleLoaders.poll() as? LoaderKey ?: return
            loaderCaches.remove(stale)
        }
    }

    internal fun cacheSizeForTest(classLoader: ClassLoader?): Int =
        if (classLoader == null) {
            bootstrapCache.size()
        } else {
            loaderCaches.entries.singleOrNull { it.key.get() === classLoader }?.value?.size() ?: 0
        }

    internal fun seedCacheForTest(
        classLoader: ClassLoader?,
        entries: Map<String, Class<out Message>>,
    ) {
        bucketFor(classLoader).seedForTest(entries)
    }

    internal fun expungeStaleLoadersForTest() = expungeStaleLoaders()

    internal fun loaderBucketCountForTest(): Int = loaderCaches.size

    internal fun clearAndEnqueueLoaderKeyForTest(classLoader: ClassLoader): Boolean {
        val key = loaderCaches.keys.singleOrNull { it.get() === classLoader } ?: return false
        key.clear()
        return key.enqueue()
    }

    private class LoaderBucket {
        private val classes = LinkedHashMap<String, WeakReference<Class<out Message>>>()

        @Synchronized
        fun resolve(
            className: String,
            loader: () -> Class<out Message>,
        ): Class<out Message> {
            classes[className]?.get()?.let { return it }
            classes.entries.removeIf { it.value.get() == null }
            return insert(className, loader())
        }

        @Synchronized
        fun size(): Int = classes.size

        @Synchronized
        fun seedForTest(entries: Map<String, Class<out Message>>) {
            entries.forEach { (name, type) -> insert(name, type) }
        }

        private fun insert(className: String, type: Class<out Message>): Class<out Message> {
            if (classes.size >= MAX_CLASSES_PER_LOADER) classes.clear()
            classes[className] = WeakReference(type)
            return type
        }
    }

    private class LoaderKey(
        classLoader: ClassLoader,
        queue: ReferenceQueue<ClassLoader>,
    ): WeakReference<ClassLoader>(classLoader, queue) {
        private val identityHash = System.identityHashCode(classLoader)

        override fun hashCode(): Int = identityHash

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LoaderKey || identityHash != other.identityHash) return false
            val left = get() ?: return false
            return left === other.get()
        }
    }
}
