package dev.brahmkshatriya.echo.common.helpers

import dev.brahmkshatriya.echo.common.helpers.PagedData.Companion.empty
import dev.brahmkshatriya.echo.common.helpers.PagedData.Concat
import dev.brahmkshatriya.echo.common.helpers.PagedData.Continuous
import dev.brahmkshatriya.echo.common.helpers.PagedData.Single
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A class that represents a paged data source.
 *
 * It is be used to load data in chunks. If the data is not continuous, use [Single].
 * ```kotlin
 * val data = PagedData.Single { api.loadTracks() }
 * ```
 *
 * If the data is continuous, use [Continuous].
 * ```kotlin
 * val data = PagedData.Continuous { continuation ->
 *     val (tracks, nextContinuation) = api.loadTracks(continuation)
 *     Page(tracks, nextContinuation)
 * }
 * ```
 *
 * If the data is empty, you can use [empty].
 * ```kotlin
 * val data = PagedData.empty<Track>()
 * ```
 *
 * If you want to concatenate multiple data sources, use [Concat].
 * ```kotlin
 * val data = PagedData.Concat(
 *     PagedData.Single { api.loadTracks() },
 *     PagedData.Continuous { api.loadTracksPage(it) }
 *     // Add more sources here
 * )
 * ```
 * @param T The type of data
 */
sealed class PagedData<T : Any> {

    /**
     * To cleat the cache of paged data
     */
    abstract fun clear()

    /**
     * To load the first page of data
     *
     * @return A list of the first page of data
     */
    abstract suspend fun loadFirst(): List<T>

    /**
     * To load all the data
     *
     * @return A list of all the data
     */
    abstract suspend fun loadAll(): List<T>

    abstract suspend fun loadList(continuation: String?): Page<T>
    abstract fun invalidate(continuation: String?)

    private var loaded = false
    private var lastContinuation: String? = null
    suspend fun loadNext(): List<T>? {
        if (loaded) return null
        val page = loadList(lastContinuation)
        loaded = page.continuation == null
        lastContinuation = page.continuation
        return page.data
    }

    /**
     * To load the next page of data
     *
     * @return A list of the next page of data
     */
    fun hasNext() = !loaded

    abstract fun <R : Any> map(block: (T) -> R): PagedData<R>

    /**
     * A class representing a single page of data.
     *
     * @param load The function to load the list of data
     */
    class Single<T : Any>(
        val load: suspend () -> List<T>
    ) : PagedData<T>() {
        private var loaded = false
        val items = mutableListOf<T>()
        suspend fun loadList(): List<T> {
            if (loaded) return items
            items.addAll(load())
            loaded = true
            return items
        }

        override fun clear() {
            items.clear()
            loaded = false
        }

        /**
         * To load all the data
         *
         * @return A list of all the data
         */
        override suspend fun loadFirst() = loadList()

        /**
         * To load all the data
         *
         * @return A list of all the data
         */
        override suspend fun loadAll() = loadList()
        override suspend fun loadList(continuation: String?): Page<T> {
            return Page(loadList(), null)
        }
        override fun invalidate(continuation: String?) { clear() }
        override fun <R : Any> map(block: (T) -> R): PagedData<R> {
            return Single { load().map(block) }
        }
    }

    /**
     * A class representing a continuous page of data.
     *
     * This class is used to load data in chunks.
     * It takes a function that loads a page of data, with the given continuation token
     * and returns a [Page] with the next continuation token.
     *
     * If next continuation token is `null`, it means there is no more data to load.
     * The initial call to load will have a `null` continuation token.
     *
     * Example for using with an API that uses Strings as continuation tokens:
     * ```kotlin
     * val data = PagedData.Continuous { continuation ->
     *   val (tracks, nextCont) = api.loadTracks(continuation)
     *   Page(tracks, nextCont)
     * }
     * ```
     *
     * Example for using with an API that uses Integers as continuation tokens:
     * ```kotlin
     * val totalTracks = api.getTotalTracks()
     * val data = PagedData.Continuous { continuation ->
     *    val contInt = continuation?.toIntOrNull() ?: 0
     *    val tracks = api.loadTracks(contInt)
     *    val nextContinuation = contInt + 10
     *    if (nextContinuation >= totalTracks) Page(tracks, null)
     *    else Page(tracks, nextContinuation)
     * }
     * ```
     * @param load The function to load a [Page] for a given continuation token
     */
    class Continuous<T : Any>(
        val load: suspend (continuation: String?) -> Page<T>,
    ) : PagedData<T>() {

        private val itemMap = mutableMapOf<String?, Page<T>>()
        override suspend fun loadList(continuation: String?): Page<T> {
            val page = itemMap.getOrPut(continuation) {
                val (data, cont) = load(continuation)
                Page(data, cont)
            }
            return page
        }

        override fun invalidate(continuation: String?) { itemMap.remove(continuation) }
        override fun clear() = itemMap.clear()

        override suspend fun loadFirst() = loadList(null).data

        override suspend fun loadAll(): List<T> {
            val list = mutableListOf<T>()
            val init = loadList(null)
            list.addAll(init.data)
            var cont = init.continuation
            while (cont != null) {
                val page = load(cont)
                list.addAll(page.data)
                cont = page.continuation
            }
            return list
        }

        override fun <R : Any> map(block: (T) -> R): PagedData<R> {
            return Continuous { continuation ->
                val (data, cont) = load(continuation)
                Page(data.map(block), cont)
            }
        }
    }

    /**
     * A class representing a concatenation of multiple data sources.
     *
     * This class is used to concatenate multiple data sources into a single source.
     * It takes multiple [PagedData] sources and loads them one after the other.
     *
     * Example:
     * ```kotlin
     * val data = PagedData.Concat(
     *     PagedData.Single { api.loadShelves() },
     *     PagedData.Continuous { api.loadShelvesPage(it) },
     *     // Add more sources here
     * )
     * ```
     * @param sources The list of [PagedData] sources to concatenate
     */
    class Concat<T : Any>(
        private vararg val sources: PagedData<T>
    ) : PagedData<T>() {
        init {
            require(sources.isNotEmpty()) { "Concat must have at least one source" }
        }

        override fun clear() = sources.forEach { it.clear() }
        override suspend fun loadFirst(): List<T> = sources.first().loadFirst()
        override suspend fun loadAll(): List<T> = sources.flatMap { it.loadAll() }

        override fun <R : Any> map(block: (T) -> R): PagedData<R> {
            return Concat(*sources.map { it.map(block) }.toTypedArray())
        }

        private fun splitContinuation(continuation: String?): Pair<Int, String?> {
            if (continuation == null) return 0 to null
            val index = continuation.substringBefore("_").toIntOrNull() ?: -1
            val token = continuation.substringAfter("_")
            return index to token
        }

        private fun combine(index: Int, token: String?): String {
            return "${index}_${token ?: ""}"
        }

        override suspend fun loadList(continuation: String?): Page<T> {
            val (index, token) = splitContinuation(continuation)
            val source = sources.getOrNull(index) ?: return Page(emptyList(), null)
            val page = source.loadList(token)
            if (page.continuation != null) Page(page.data, combine(index, page.continuation))
            else Page(page.data, combine(index + 1, null))
            return page
        }

        override fun invalidate(continuation: String?) {
            val (index, token) = splitContinuation(continuation)
            val source = sources.getOrNull(index)
            source?.invalidate(token)
        }
    }

    class Suspend<T : Any>(
        private val getter: suspend () -> PagedData<T>
    ) : PagedData<T>() {
        private val mutex = Mutex()
        private var _data: PagedData<T>? = null
        private suspend fun data(): PagedData<T> {
            return _data ?: mutex.withLock { getter() }.also { _data = it }
        }

        override fun clear() {
            _data?.clear()
        }

        override suspend fun loadFirst(): List<T> {
            return data().loadFirst()
        }

        override suspend fun loadAll(): List<T> {
            return data().loadAll()
        }

        override fun <R : Any> map(block: (T) -> R): PagedData<R> {
            return Suspend { data().map(block) }
        }

        override suspend fun loadList(continuation: String?): Page<T> {
            return data().loadList(continuation)
        }

        override fun invalidate(continuation: String?) {
            _data?.invalidate(continuation)
        }
    }

    companion object {
        fun <T : Any> empty() = Single<T> { emptyList() }
    }
}