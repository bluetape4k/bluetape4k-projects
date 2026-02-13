package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SelectorsTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    fun <T> Flow<T>.flowOnStandardTestDispatcher(testScope: TestScope): Flow<T> =
        flowOn(StandardTestDispatcher(testScope.testScheduler))

    private data class State(
        val isLoading: Boolean,
        val items: List<String>,
        val searchTerm: String?,
        val title: String,
        val error: Throwable?,
        val isRefreshing: Boolean,
        val subtitle: String,
        val unreadCount: Int,
    ) {
        companion object {
            val INITIAL = State(
                isLoading = true,
                items = emptyList(),
                searchTerm = null,
                title = "Loading...",
                error = null,
                isRefreshing = false,
                subtitle = "Loading...",
                unreadCount = 0
            )
        }
    }

    fun <T, R> Flow<T>.scanSkipFirst(
        initial: R,
        operation: suspend (acc: R, value: T) -> R,
    ): Flow<R> = scan(initial, operation).drop(1)

    private val zeroToTen = List(10) { it.toString() }

    private val reducer: (acc: State, value: Int) -> State = { state, action ->
        when (action) {
            // items
            0    -> state.copy(items = zeroToTen)
            // loading
            1    -> state.copy(isLoading = !state.isLoading)
            // loading
            2    -> state.copy(isLoading = !state.isLoading)
            // searchTerm
            3    -> state.copy(searchTerm = "4")
            // loading
            4    -> state.copy(isLoading = !state.isLoading)
            // items
            5    -> state.copy(items = state.items + "11")
            // title
            6    -> state.copy(title = "Title")
            // loading
            7    -> state.copy(isLoading = !state.isLoading)
            // error
            8    -> state.copy(error = Throwable("Error"))
            // subtitle
            9    -> state.copy(subtitle = "Subtitle")
            // loading
            10   -> state.copy(isLoading = !state.isLoading)
            // unreadCount
            11   -> state.copy(unreadCount = state.unreadCount + 1)
            // isRefreshing
            12   -> state.copy(isRefreshing = !state.isRefreshing)
            // subtitle
            13   -> state.copy(subtitle = "Subtitle 2")
            // isRefreshing
            14   -> state.copy(isRefreshing = !state.isRefreshing)
            // unreadCount
            15   -> state.copy(unreadCount = state.unreadCount + 1)
            else -> error("Unknown action")
        }
    }


    @Test
    fun `select 1`() = runTest {
        val progression = 0..1_000 step 10
        val flow = progression.asFlow()
            .log("flow")
            .select { it.toString().length }   // 숫자의 자릿수가 변할 때만 emit 된다.
            .log("selected")

        flow.test {
            awaitItem() shouldBeEqualTo 1
            awaitItem() shouldBeEqualTo 2
            awaitItem() shouldBeEqualTo 3
            awaitItem() shouldBeEqualTo 4
            awaitComplete()
        }
    }

    @Test
    fun `select 2`() = runTest {
        var searchTermCount = 0
        var itemsCount = 0
        var projectorCount = 0

        val flow = flowRangeOf(0, 6)
            .flowOnStandardTestDispatcher(this)
            .onEach { delay(100) }
            .log("source")
            .scanSkipFirst(State.INITIAL, reducer)
            .log("skipFirst")
            .select(
                selector1 = {
                    log.trace { "select1. searchTerm=${it.searchTerm}" }
                    searchTermCount++
                    it.searchTerm
                },
                selector2 = {
                    log.trace { "select2. items=${it.items}" }
                    itemsCount++
                    it.items
                },
                projector = { searchTerm, items ->
                    log.trace { "projector. searchTerm=$searchTerm, items=$items" }
                    projectorCount++
                    items.filter { it.contains(searchTerm ?: "") }
                }
            )
            .log("selected")

        flow.test {
            awaitItem() shouldBeEqualTo zeroToTen       // 0 - items
            awaitItem() shouldBeEqualTo listOf("4")     // 3 - searchTerm
            awaitComplete()
        }

        searchTermCount shouldBeEqualTo 6   // 0..5
        itemsCount shouldBeEqualTo 6    // 0..5
        projectorCount shouldBeEqualTo 3    // [0, 3, 5]
    }

    @Test
    fun `select 3`() = runTest {
        var searchTermCount = 0
        var itemsCount = 0
        var titleCount = 0
        var projectorCount = 0

        val flow = flowRangeOf(0, 8)
            .flowOnStandardTestDispatcher(this)
            .onEach { delay(100) }
            .log("source")
            .scanSkipFirst(State.INITIAL, reducer)
            .log("skipFirst")
            .select(
                selector1 = {
                    log.trace { "select1. searchTerm=${it.searchTerm}" }
                    searchTermCount++
                    it.searchTerm
                },
                selector2 = {
                    log.trace { "select2. items=${it.items}" }
                    itemsCount++
                    it.items
                },
                selector3 = {
                    log.trace { "select3. title=${it.title}" }
                    titleCount++
                    it.title
                },
                projector = { searchTerm, items, title ->
                    log.trace { "projector. searchTerm=$searchTerm, items=$items, title=$title" }
                    projectorCount++
                    items
                        .filter { it.contains(searchTerm ?: "") }
                        .map { "$it # $title" }
                }
            )
            .log("selected")

        flow.test {
            awaitItem() shouldBeEqualTo zeroToTen.map { "$it # Loading..." }       // 0 - items
            awaitItem() shouldBeEqualTo listOf("4 # Loading...")     // 3 - searchTerm
            awaitItem() shouldBeEqualTo listOf("4 # Title")     // 6 -title
            awaitComplete()
        }

        searchTermCount shouldBeEqualTo 8   // 0..7
        itemsCount shouldBeEqualTo 8    // 0..7
        titleCount shouldBeEqualTo 8
        projectorCount shouldBeEqualTo 4    // [0, 3, 5, 6]
    }

    @Test
    fun `select 4`() = runTest {
        var searchTermCount = 0
        var itemsCount = 0
        var titleCount = 0
        var subtitleCount = 0
        var projectorCount = 0

        val flow = flowRangeOf(0, 11)
            .flowOnStandardTestDispatcher(this)
            .onEach { delay(100) }
            .log("source")
            .scanSkipFirst(State.INITIAL, reducer)
            .log("skipFirst")
            .select(
                selector1 = {
                    log.trace { "select1. searchTerm=${it.searchTerm}" }
                    searchTermCount++
                    it.searchTerm
                },
                selector2 = {
                    log.trace { "select2. items=${it.items}" }
                    itemsCount++
                    it.items
                },
                selector3 = {
                    log.trace { "select3. title=${it.title}" }
                    titleCount++
                    it.title
                },
                selector4 = {
                    log.trace { "select4. subtitle=${it.subtitle}" }
                    subtitleCount++
                    it.subtitle
                },
                projector = { searchTerm, items, title, subtitle ->
                    log.trace { "projector. searchTerm=$searchTerm, items=$items, title=$title, subtitle=$subtitle," }
                    projectorCount++
                    items
                        .filter { it.contains(searchTerm ?: "") }
                        .map { "$it # $title ~ $subtitle" }
                }
            )
            .log("selected")

        flow.test {
            awaitItem() shouldBeEqualTo zeroToTen.map { "$it # Loading... ~ Loading..." }       // 0 - items
            awaitItem() shouldBeEqualTo listOf("4 # Loading... ~ Loading...")     // 3 - searchTerm
            awaitItem() shouldBeEqualTo listOf("4 # Title ~ Loading...")     // 6 - title
            awaitItem() shouldBeEqualTo listOf("4 # Title ~ Subtitle")     // 9 - subtitle
            awaitComplete()
        }

        searchTermCount shouldBeEqualTo 11   // 0..10
        itemsCount shouldBeEqualTo 11    // 0..10
        titleCount shouldBeEqualTo 11    // 0..10
        subtitleCount shouldBeEqualTo 11    // 0..10
        projectorCount shouldBeEqualTo 5    // [0, 3, 5, 6, 9]
    }


    @Test
    fun `select 5`() = runTest {
        var searchTermCount = 0
        var itemsCount = 0
        var titleCount = 0
        var subtitleCount = 0
        var unreadCountCount = 0
        var projectorCount = 0

        val flow = flowRangeOf(0, 16)
            .flowOnStandardTestDispatcher(this)
            .onEach { delay(100) }
            .log("source")
            .scanSkipFirst(State.INITIAL, reducer)
            .log("skipFirst")
            .select(
                selector1 = {
                    log.trace { "select1. searchTerm=${it.searchTerm}" }
                    searchTermCount++
                    it.searchTerm
                },
                selector2 = {
                    log.trace { "select2. items=${it.items}" }
                    itemsCount++
                    it.items
                },
                selector3 = {
                    log.trace { "select3. title=${it.title}" }
                    titleCount++
                    it.title
                },
                selector4 = {
                    log.trace { "select4. subtitle=${it.subtitle}" }
                    subtitleCount++
                    it.subtitle
                },
                selector5 = {
                    log.trace { "selec5. unreadCount=${it.unreadCount}" }
                    unreadCountCount++
                    it.unreadCount
                },
                projector = { searchTerm, items, title, subtitle, unreadCount ->
                    log.trace { "projector. searchTerm=$searchTerm, items=$items, title=$title, subtitle=$subtitle, unreadCount=$unreadCount" }
                    projectorCount++
                    items
                        .filter { it.contains(searchTerm ?: "") }
                        .map { "$it # $title ~ $subtitle # $unreadCount" }
                }
            )
            .log("selected")

        flow.test {
            awaitItem() shouldBeEqualTo zeroToTen.map { "$it # Loading... ~ Loading... # 0" }       // 0 - items
            awaitItem() shouldBeEqualTo listOf("4 # Loading... ~ Loading... # 0")     // 3 - searchTerm
            awaitItem() shouldBeEqualTo listOf("4 # Title ~ Loading... # 0")     // 6 - title
            awaitItem() shouldBeEqualTo listOf("4 # Title ~ Subtitle # 0")     // 9 - subtitle
            awaitItem() shouldBeEqualTo listOf("4 # Title ~ Subtitle # 1")     // 11 - unreadCount
            awaitItem() shouldBeEqualTo listOf("4 # Title ~ Subtitle 2 # 1")     // 13 - subtitle
            awaitItem() shouldBeEqualTo listOf("4 # Title ~ Subtitle 2 # 2")     // 15 - unreadCount
            awaitComplete()
        }

        searchTermCount shouldBeEqualTo 16   // 0..15
        itemsCount shouldBeEqualTo 16   // 0..15
        titleCount shouldBeEqualTo 16   // 0..15
        subtitleCount shouldBeEqualTo 16   // 0..15
        unreadCountCount shouldBeEqualTo 16   // 0..15
        projectorCount shouldBeEqualTo 8    // [0, 3, 5, 6, 9, 11, 13, 15]
    }
}
