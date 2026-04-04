package io.bluetape4k.concurrent

import io.bluetape4k.support.requireNotBlank

/**
 * 시스템 스레드 그룹을 반환합니다.
 *
 * 현재 스레드 그룹의 부모를 루트까지 거슬러 올라가 최상위 시스템 스레드 그룹을 반환합니다.
 *
 * ```kotlin
 * val systemGroup = getSystemThreadGroup()
 * println(systemGroup.name) // "system"
 * ```
 *
 * @return 시스템 스레드 그룹
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
 * 시스템 스레드 그룹 기준으로 재귀 탐색을 수행합니다.
 *
 * ```kotlin
 * // 이름이 "worker"로 시작하는 스레드 그룹 검색
 * val groups = findThreadGroups { it.name.startsWith("worker") }
 * groups.forEach { println(it.name) }
 * ```
 *
 * @param predicate 조건 함수
 * @return 조건을 만족하는 활성 스레드 그룹 목록
 */
fun findThreadGroups(predicate: (ThreadGroup) -> Boolean): List<ThreadGroup> =
    findThreadGroups(getSystemThreadGroup(), true, predicate)


/**
 * 주어진 스레드 그룹(및 하위 그룹)에서 [predicate]를 만족하는 활성 스레드 그룹을 모두 찾습니다.
 *
 * ```kotlin
 * val parentGroup = ThreadGroup("parent")
 * val childGroup = ThreadGroup(parentGroup, "child-worker")
 *
 * // parentGroup 하위에서 "child"로 시작하는 스레드 그룹 검색
 * val groups = findThreadGroups(parentGroup, recurse = true) { it.name.startsWith("child") }
 * println(groups.size)       // 1
 * println(groups[0].name)    // "child-worker"
 * ```
 *
 * @param threadGroup 검색 기준 스레드 그룹
 * @param recurse `true`이면 하위 그룹까지 재귀적으로 검색합니다
 * @param predicate 조건 함수
 * @return 조건을 만족하는 활성 스레드 그룹 목록
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

/**
 * 이름이 [name]인 스레드 그룹을 모두 찾습니다.
 *
 * ```kotlin
 * val group = ThreadGroup("my-workers")
 * val thread = Thread(group, { Thread.sleep(100) }, "t1").also { it.start() }
 *
 * val found = findThreadGroupsByName("my-workers")
 * println(found.isNotEmpty())    // true
 * println(found[0].name)         // "my-workers"
 *
 * thread.join()
 * ```
 *
 * @param name 검색할 스레드 그룹 이름
 * @return 이름이 일치하는 활성 스레드 그룹 목록
 */
fun findThreadGroupsByName(name: String): List<ThreadGroup> {
    name.requireNotBlank("name")
    return findThreadGroups { it.name == name }
}

/**
 * 주어진 스레드 그룹(및 하위 그룹)에서 [predicate]를 만족하는 활성 스레드를 모두 찾습니다.
 *
 * ```kotlin
 * val group = ThreadGroup("io-workers")
 * val thread = Thread(group, { Thread.sleep(200) }, "io-1").also { it.start() }
 *
 * // 데몬이 아닌 스레드 검색
 * val threads = findThreads(group, recurse = false) { !it.isDaemon }
 * println(threads.any { it.name == "io-1" }) // true
 *
 * thread.join()
 * ```
 *
 * @param threadGroup 검색 기준 스레드 그룹
 * @param recurse `true`이면 하위 그룹까지 재귀적으로 검색합니다
 * @param predicate 조건 함수
 * @return 조건을 만족하는 활성 스레드 목록
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
 * 지정한 스레드 그룹 이름에 속하면서 이름이 [threadName]인 활성 스레드를 찾습니다.
 *
 * ```kotlin
 * val group = ThreadGroup("my-group")
 * val thread = Thread(group, { Thread.sleep(200) }, "my-thread").also { it.start() }
 *
 * val found = findThreadByName("my-thread", "my-group")
 * println(found.isNotEmpty())    // true
 * println(found[0].name)         // "my-thread"
 *
 * thread.join()
 * ```
 *
 * @param threadName 검색할 스레드 이름
 * @param threadGroupName 검색할 스레드 그룹 이름
 * @return 조건을 만족하는 스레드 목록. 없으면 빈 목록을 반환합니다.
 */
fun findThreadByName(threadName: String, threadGroupName: String): List<Thread> =
    findThreadGroups { it.name == threadGroupName }
        .flatMap { group ->
            findThreads(group, false) { it.name == threadName }
        }

/**
 * 이름이 [threadName]인 활성 스레드를 찾습니다.
 *
 * ```kotlin
 * val thread = Thread({ Thread.sleep(200) }, "search-target").also { it.start() }
 *
 * val found = findThreadByName("search-target")
 * println(found.isNotEmpty())  // true
 * println(found[0].name)       // "search-target"
 *
 * thread.join()
 * ```
 *
 * @param threadName 검색할 스레드 이름
 * @param threadGroup 검색 기준 스레드 그룹 (기본값: 시스템 스레드 그룹)
 * @return 이름이 일치하는 스레드 목록. 없으면 빈 목록을 반환합니다.
 */
fun findThreadByName(threadName: String, threadGroup: ThreadGroup = getSystemThreadGroup()): List<Thread> {
    threadName.requireNotBlank("name")
    return findThreads(threadGroup, false) { it.name == threadName }
}

/**
 * 지정한 스레드 그룹에서 ID가 [threadId]인 활성 스레드를 찾습니다.
 *
 * ```kotlin
 * val thread = Thread({ Thread.sleep(200) }, "id-search").also { it.start() }
 * val id = thread.threadId()
 *
 * val found = findThreadByThreadId(id)
 * println(found?.name) // "id-search"
 *
 * thread.join()
 * ```
 *
 * @param threadId 검색할 스레드 ID
 * @param threadGroup 검색 기준 스레드 그룹 (기본값: 시스템 스레드 그룹)
 * @return 조건을 만족하는 스레드. 없으면 `null`을 반환합니다.
 */
fun findThreadByThreadId(threadId: Long, threadGroup: ThreadGroup = getSystemThreadGroup()): Thread? =
    findThreads(threadGroup) { it.threadId() == threadId }.firstOrNull()

/**
 * 시스템 스레드 그룹을 제외한 모든 활성 스레드 그룹을 반환합니다.
 *
 * ```kotlin
 * val groups = getAllThreadGroups()
 * println(groups.map { it.name }) // ["main", "InnocuousThreadGroup", ...]
 * ```
 *
 * @return 활성 스레드 그룹 목록
 */
fun getAllThreadGroups(): List<ThreadGroup> = findThreadGroups { true }

/**
 * 모든 활성 스레드를 반환합니다.
 *
 * ```kotlin
 * val threads = getAllThreads()
 * println(threads.map { it.name }) // ["main", "Signal Dispatcher", "Reference Handler", ...]
 * ```
 *
 * @return 활성 스레드 목록
 */
fun getAllThreads(): List<Thread> = findThreads { true }
