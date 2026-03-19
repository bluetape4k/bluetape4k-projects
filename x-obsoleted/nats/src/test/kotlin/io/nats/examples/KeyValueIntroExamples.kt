package io.nats.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.api.keyValueConfiguration
import io.bluetape4k.support.toUtf8String
import io.nats.client.Connection
import io.nats.client.JetStreamApiException
import io.nats.client.KeyValue
import io.nats.client.KeyValueManagement
import io.nats.client.PushSubscribeOptions
import io.nats.client.api.KeyValueConfiguration
import io.nats.client.api.KeyValueEntry
import io.nats.client.api.KeyValueStatus
import io.nats.client.api.KeyValueWatchOption
import io.nats.client.api.KeyValueWatcher
import org.junit.jupiter.api.Test

class KeyValueIntroExamples: AbstractNatsTest() {

    companion object: KLogging() {
        private const val KEY_VALUE_NAME = "profiles"
    }

    @Test
    fun `key-value capability in nats`() {
        getConnection().use { nc: Connection ->

            // ### Bucket basics
            // A key-value (KV) bucket is created by specifying a bucket name.
            // Java returns a KeyValueStatus object upon creation
            val kvm: KeyValueManagement = nc.keyValueManagement()
            val kvc: KeyValueConfiguration = keyValueConfiguration {
                this.name(KEY_VALUE_NAME)
            }
            val kvStatus: KeyValueStatus = kvm.create(kvc)
            log.debug { "kv status=$kvStatus" }

            // Retrieve the Key Value context once the bucket is created.
            val kv: KeyValue = nc.keyValue(KEY_VALUE_NAME)

            // As one would expect, the `KeyValue` interface provides the
            // standard `Put` and `Get` methods. However, unlike most KV
            // stores, a revision number of the entry is tracked.
            kv.put("sue.color", "blue")
            var entry = kv.get("sue.color")
            println("key=${entry.key}, revision=${entry.revision}, value=${entry.valueAsString}")

            kv.put("sue.color", "green")
            entry = kv.get("sue.color")
            println("key=${entry.key}, revision=${entry.revision}, value=${entry.valueAsString}")

            // A revision number is useful when you need to enforce [optimistic
            // concurrency control][occ] on a specific key-value entry. In short,
            // if there are multiple actors attempting to put a new value for a
            // key concurrently, we want to prevent the "last writer wins" behavior
            // which is non-deterministic. To guard against this, we can use the
            // `kv.Update` method and specify the expected revision. Only if this
            // matches on the server, will the value be updated.
            // [occ]: https://en.wikipedia.org/wiki/Optimistic_concurrency_control
            try {
                kv.update("sue.color", "red", 1)
            } catch (e: JetStreamApiException) {
                log.error(e) { "Fail to update key-value." }
            }

            val lastRevision = entry.revision
            kv.update("sue.color", "red", lastRevision)
            entry = kv.get("sue.color")
            println("key=${entry.key}, revision=${entry.revision}, value=${entry.valueAsString}")

            // ### Stream abstraction
            // Before moving on, it is important to understand that a KV bucket is
            // light abstraction over a standard stream. This is by design since it
            // enables some powerful features which we will observe in a minute.
            //
            // **How exactly is a KV bucket modeled as a stream?**
            // When one is created, internally, a stream is created using the `KV_`
            // prefix as convention. Appropriate stream configuration are used that
            // are optimized for the KV access patterns, so you can ignore the
            // details.
            val jsm = nc.jetStreamManagement()

            val streamNames = jsm.streamNames
            println("stream names=$streamNames")

            // Since it is a normal stream, we can create a consumer and
            // fetch messages.
            // If we look at the subject, we will notice that first token is a
            // special reserved prefix, the second token is the bucket name, and
            // remaining suffix is the actualy key. The bucket name is inherently
            // a namespace for all keys and thus there is no concern for conflict
            // across buckets. This is different from what we need to do for a stream
            // which is to bind a set of _public_ subjects to a stream.
            val js = nc.jetStream()

            val pso = PushSubscribeOptions.builder().stream("KV_profiles").build()
            val sub = js.subscribe(">", pso)

            var m = sub.nextMessage(100)
            println("${m.subject} ${m.metaData().streamSequence()} -> ${m.data.toUtf8String()}")

            // Let's put a new value for this key and see what we get from the subscription.
            kv.put("sue.color", "yellow")
            m = sub.nextMessage(100)
            println("${m.subject} ${m.metaData().streamSequence()} -> ${m.data.toUtf8String()}")

            // Unsurprisingly, we get the new updated value as a message.
            // Since it's KV interface, we should be able to delete a key as well.
            // Does this result in a new message?
            kv.delete("sue.color")
            m = sub.nextMessage(100)
            println("${m.subject} ${m.metaData().streamSequence()} -> ${m.data.toUtf8String()}")

            // 🤔 That is useful to get a message that something happened to that key,
            // and that this is considered a new revision.
            // However, how do we know if the new value was set to be `nil` or the key
            // was deleted?
            // To differentiate, delete-based messages contain a header. Notice the `KV-Operation: DEL`
            // header.
            println("Headers:")
            m.headers.keySet().forEach { key ->
                println("  $key, ${m.headers.getFirst(key)}")
            }

            // ### Watching for changes
            // Although one could subscribe to the stream directly, it is more convenient
            // to use a `KeyValueWatcher` which provides a deliberate API and types for tracking
            // changes over time. Notice that we can use a wildcard which we will come back to..
            val watcher = object: KeyValueWatcher {
                override fun watch(entry: KeyValueEntry) {
                    println("Watcher: ${entry.key} ${entry.revision} -> ${entry.valueAsString}")
                }

                override fun endOfData() {
                    println("Watcher: Received End of Data Signal")
                }
            }

            kv.watch("sue.*", watcher, KeyValueWatchOption.IGNORE_DELETE)

            // Even though we deleted the key, of course we can put a new value.
            // In java, there are a variety of put signatures also, so here just put a string
            kv.put("sue.color", "purple")

            // To finish this short intro, since we know that keys are subjects under the covers, if we
            // put another key, we can observe the change through the watcher. One other detail to call out
            // is notice the revision for this *new* key is not `1`. It relies on the underlying stream's
            // message sequence number to indicate the _revision_. The guarantee being that it is always
            // monotonically increasing, but numbers will be shared across keys (like subjects) rather
            // than sequence numbers relative to each key.
            kv.put("sue.food", "pizza")

            // Sleep this thread a little so the program has time
            // to receive all the messages before the program quits.
            Thread.sleep(1000)
        }
    }
}
