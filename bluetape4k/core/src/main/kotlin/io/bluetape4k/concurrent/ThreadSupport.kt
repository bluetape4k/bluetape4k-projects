package io.bluetape4k.concurrent

import io.bluetape4k.support.requireNotBlank

/**
 * Gets the system thread group.
 *
 * @return the system thread group
 */
fun getSystemThreadGroup(): ThreadGroup {
    var group: ThreadGroup = Thread.currentThread().threadGroup
    while (group.parent != null) {
        group = group.parent
    }
    return group
}

/**
 * [predicate]를 만족하는 활성화된 스레드 그룹을 모두 찾습니다.
 *
 * @param predicate the predicate
 * @return An unmodifiable {@link Collection} of active thread groups matching the given predicate
 */
fun findThreadGroups(predicate: (ThreadGroup) -> Boolean): List<ThreadGroup> {
    return findThreadGroups(getSystemThreadGroup(), true, predicate)
}


/**
 * Finds all active thread groups which match the given predicate and which is a subgroup of the given thread group (or one of its subgroups).
 *
 * @param threadGroup the thread group
 * @param recurse if {@code true} then evaluate the predicate recursively on all thread groups in all subgroups of the given group
 * @param predicate the predicate
 * @return An unmodifiable {@link Collection} of active thread groups which match the given predicate and which is a subgroup of the given thread group
 */
fun findThreadGroups(
    threadGroup: ThreadGroup = getSystemThreadGroup(),
    recurse: Boolean = true,
    predicate: (ThreadGroup) -> Boolean,
): List<ThreadGroup> {
    var count = threadGroup.activeGroupCount()
    var threadGroups: Array<ThreadGroup?>
    do {
        threadGroups = Array(count + count / 2 + 1) { null }
        count = threadGroup.enumerate(threadGroups, recurse)
    } while (count >= threadGroups.size)

    return threadGroups.filterNotNull().take(count).filter(predicate)
}

fun findThreadGroupsByName(name: String): List<ThreadGroup> {
    name.requireNotBlank("name")
    return findThreadGroups { it.name == name }
}

/**
 * Finds all active threads which match the given predicate and which belongs to the given thread group (or one of its subgroups).
 *
 * @param threadGroup the thread group
 * @param recurse if {@code true} then evaluate the predicate recursively on all threads in all subgroups of the given group
 * @param predicate the predicate
 * @return An unmodifiable {@link Collection} of active threads which match the given predicate and which belongs to the given thread group
 */
fun findThreads(
    threadGroup: ThreadGroup = getSystemThreadGroup(),
    recurse: Boolean = true,
    predicate: (Thread) -> Boolean,
): List<Thread> {
    var count = threadGroup.activeCount()
    var threads: Array<Thread?>
    do {
        threads = Array(count + count / 2 + 1) { null }
        count = threadGroup.enumerate(threads, recurse)
        //return value of enumerate() must be strictly less than the array size according to javadoc
    } while (count >= threads.size)

    return threads.filterNotNull().take(count).filter(predicate)
}

/**
 * Finds active threads with the specified name if they belong to a thread group with the specified group name.
 *
 * @param threadName The thread name
 * @param threadGroupName The thread group name
 * @return The threads which belongs to a thread group with the specified group name and the thread's name match the specified name,
 * An empty collection is returned if no such thread exists. The collection returned is always unmodifiable.
 */
fun findThreadByName(threadName: String, threadGroupName: String): List<Thread> {
    return findThreadGroups { it.name == threadGroupName }
        .flatMap { group ->
            findThreads(group, false) { it.name == threadName }
        }
}

/**
 * Finds active threads with the specified name.
 *
 * @param threadName The thread name
 * @return The threads with the specified name or an empty collection if no such thread exists. The collection returned is always unmodifiable.
 */
fun findThreadByName(threadName: String, threadGroup: ThreadGroup = getSystemThreadGroup()): List<Thread> {
    threadName.requireNotBlank("name")
    return findThreads(threadGroup, false) { it.name == threadName }
}

/**
 * Finds the active thread with the specified id if it belongs to the specified thread group.
 *
 * @param threadId The thread id
 * @param threadGroup The thread group
 * @return The thread which belongs to a specified thread group and the thread's id match the specified id.
 * `null` is returned if no such thread exists
 */
fun findThreadByThreadId(threadId: Long, threadGroup: ThreadGroup = getSystemThreadGroup()): Thread? {
    return findThreads(threadGroup) { it.threadId() == threadId }.firstOrNull()
}

/**
 * Gets all active thread groups excluding the system thread group (A thread group is active if it has been not destroyed).
 *
 * @return all thread groups excluding the system thread group. The collection returned is always unmodifiable.
 */
fun getAllThreadGroups(): List<ThreadGroup> {
    return findThreadGroups { true }
}

/**
 * Gets all active threads (A thread is active if it has been started and has not yet died).
 *
 * @return all active threads. The collection returned is always unmodifiable.
 */
fun getAllThreads(): List<Thread> {
    return findThreads { true }
}
