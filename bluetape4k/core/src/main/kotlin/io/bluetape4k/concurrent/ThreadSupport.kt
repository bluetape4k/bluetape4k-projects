package io.bluetape4k.concurrent

import io.bluetape4k.support.requireNotBlank

/**
 * 시스템 스레드 그룹을 반환합니다.
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
 * @param predicate 조건 함수
 * @return 조건을 만족하는 활성 스레드 그룹 목록
 */
fun findThreadGroups(predicate: (ThreadGroup) -> Boolean): List<ThreadGroup> =
    findThreadGroups(getSystemThreadGroup(), true, predicate)


/**
 * 주어진 스레드 그룹(및 하위 그룹)에서 [predicate]를 만족하는 활성 스레드 그룹을 모두 찾습니다.
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
 * @param threadId 검색할 스레드 ID
 * @param threadGroup 검색 기준 스레드 그룹 (기본값: 시스템 스레드 그룹)
 * @return 조건을 만족하는 스레드. 없으면 `null`을 반환합니다.
 */
fun findThreadByThreadId(threadId: Long, threadGroup: ThreadGroup = getSystemThreadGroup()): Thread? =
    findThreads(threadGroup) { it.threadId() == threadId }.firstOrNull()

/**
 * 시스템 스레드 그룹을 제외한 모든 활성 스레드 그룹을 반환합니다.
 *
 * @return 활성 스레드 그룹 목록
 */
fun getAllThreadGroups(): List<ThreadGroup> = findThreadGroups { true }

/**
 * 모든 활성 스레드를 반환합니다.
 *
 * @return 활성 스레드 목록
 */
fun getAllThreads(): List<Thread> = findThreads { true }
