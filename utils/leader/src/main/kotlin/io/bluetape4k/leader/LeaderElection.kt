package io.bluetape4k.leader

/**
 * 여러 Process, Thread에서 같은 작업이 동시, 무작위로 실행되는 것을 방지하기 위해
 * Leader 를 선출되면 독점적으로 작업할 수 있도록 합니다.
 *
 * ```
 * val leaderElection = DefaultLeaderElection()
 * val result = leaderElection.runIfLeader("lockName") {
 *    // 리더로 선출되면 수행할 코드 블럭
 *    println("I'm a leader!")
 *    // 작업 결과
 *    "Hello, Leader!"
 *    // 리더로 선출되지 않으면 null 반환
 *    // 리더로 선출되면 작업 결과를 반환
 * }
 */
interface LeaderElection: AsyncLeaderElection {

    /**
     * 리더로 선출되면 [action]을 수행하고, 그렇지 않다면 수행하지 않습니다.
     *
     * @param lockName lock name - lock 획득에 성공하면 leader로 승격되는 것이다.
     * @param action leader 로 승격되면 수행할 코드 블럭
     * @return 작업 결과
     */
    fun <T> runIfLeader(lockName: String, action: () -> T): T

}
