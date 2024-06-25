package nidomiro.kdataloader

import kotlinx.coroutines.*
import nidomiro.kdataloader.statistics.SimpleStatisticsCollector
import nidomiro.kdataloader.statistics.StatisticsCollector
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

open class SimpleDataLoaderImpl<K, R>(
    override val options: DataLoaderOptions<K, R>,
    private val statisticsCollector: StatisticsCollector,
    private val batchLoader: BatchLoader<K, R>,
    private val propagateables: List<() -> CoroutineContext.Element>
) : DataLoader<K, R> {
    constructor(options: DataLoaderOptions<K, R>,
                batchLoader: BatchLoader<K, R>,
                propagateables: List<() -> CoroutineContext.Element>) : this(
        options,
        SimpleStatisticsCollector(),
        batchLoader,
        propagateables
    )

    constructor(batchLoader: BatchLoader<K, R>, propagateables: List<() -> CoroutineContext.Element>) : this(DataLoaderOptions(), batchLoader, propagateables)

    private suspend fun dataLoaderScope() = CoroutineScope(Dispatchers.Default)

    private val queue: LoaderQueue<K, R> = DefaultLoaderQueueImpl()

    @Suppress("DeferredResultUnused")
    override suspend fun loadAsync(key: K): Deferred<R> {
        statisticsCollector.incLoadAsyncMethodCalledAsync()
        statisticsCollector.incObjectsRequestedAsync()

        return internalLoadAsync(key)
    }

    @Suppress("DeferredResultUnused")
    private suspend fun internalLoadAsync(key: K): Deferred<R> {
        val block: suspend (key: K) -> CompletableDeferred<R> = {
            val newDeferred = CompletableDeferred<R>()
            queue.enqueue(key, newDeferred)
            if (options.batchMode == BatchMode.LoadImmediately) {
                dispatch()
            }
            newDeferred
        }

        return if (options.cache != null) {
            options.cache!!.getOrCreate(key, block, { statisticsCollector.incCacheHitCountAsync() })
        } else {
            block(key)
        }
    }

    @Suppress("DeferredResultUnused")
    override suspend fun loadManyAsync(vararg keys: K): Deferred<List<R>> {
        statisticsCollector.incLoadManyAsyncMethodCalledAsync()
        statisticsCollector.incObjectsRequestedAsync(keys.size.toLong())

        val deferreds = keys.map { internalLoadAsync(it) }

        return  CoroutineScope(Dispatchers.Default).async {
            return@async deferreds.map { it.await() }
        }
    }

    @Suppress("DeferredResultUnused")
    override suspend fun dispatch() {
        val context = propagateables.fold(Dispatchers.Default, CoroutineContext::plus)
       dataLoaderScope().launch(context) {
            statisticsCollector.incDispatchMethodCalledAsync()

            val queueEntries = if (options.cache != null) {
                queue.getAllItemsAsList().distinctBy { it.key }
            } else {
                queue.getAllItemsAsList()
            }

            queueEntries
                .batchIfNeeded(options.batchMode)
                .forEach {
                    executeDispatchOnQueueEntries(it)
                }
        }
    }

    private fun List<LoaderQueueEntry<K, CompletableDeferred<R>>>.batchIfNeeded(batchMode: BatchMode) =
        if (batchMode is BatchMode.LoadInBatch && batchMode.batchSize != null) {
            this.chunked(batchMode.batchSize)
        } else {
            listOf(this)
        }

    private suspend fun executeDispatchOnQueueEntries(queueEntries: List<LoaderQueueEntry<K, CompletableDeferred<R>>>) {
        val keys = queueEntries.map { it.key }
        if (keys.isNotEmpty()) {
            executeBatchLoader(keys, queueEntries)
        }
    }

    @Suppress("DeferredResultUnused")
    private suspend fun executeBatchLoader(
        keys: List<K>,
        queueEntries: List<LoaderQueueEntry<K, CompletableDeferred<R>>>
    ) {
        statisticsCollector.incBatchCallsExecutedAsync()
        try {
            batchLoader(keys).forEachIndexed { i, result ->
                val queueEntry = queueEntries[i]
                handleSingleBatchLoaderResult(result, queueEntry)
            }
        } catch (e: Throwable) {
            handleCompleteBatchLoaderFailure(queueEntries, e)
        }
    }

    private suspend fun handleSingleBatchLoaderResult(
        result: ExecutionResult<R>,
        queueEntry: LoaderQueueEntry<K, CompletableDeferred<R>>
    ) {
        when (result) {
            is ExecutionResult.Success -> queueEntry.value.complete(result.value)
            is ExecutionResult.Failure -> {
                queueEntry.value.completeExceptionally(result.throwable)
                if (!options.cacheExceptions) {
                    clear(queueEntry.key)
                }
            }

        }
    }

    private suspend fun handleCompleteBatchLoaderFailure(
        queueEntries: List<LoaderQueueEntry<K, CompletableDeferred<R>>>,
        e: Throwable
    ) {
        queueEntries.forEach {
            clear(it.key)
            it.value.completeExceptionally(e)
        }
    }

    @Suppress("DeferredResultUnused")
    override suspend fun clear(key: K) {
        statisticsCollector.incClearMethodCalledAsync()

        options.cache?.clear(key)
    }

    @Suppress("DeferredResultUnused")
    override suspend fun clearAll() {
        statisticsCollector.incClearAllMethodCalledAsync()
        options.cache?.clear()
    }

    @Suppress("DeferredResultUnused")
    override suspend fun prime(key: K, value: R) {
        statisticsCollector.incPrimeMethodCalledAsync()
        options.cache?.getOrCreate(key) {
            CompletableDeferred(value)
        }
    }

    @Suppress("DeferredResultUnused")
    override suspend fun prime(key: K, value: Throwable) {
        statisticsCollector.incPrimeMethodCalledAsync()
        options.cache?.getOrCreate(key) {
            CompletableDeferred<R>().apply {
                completeExceptionally(value)
            }
        }
    }

    override suspend fun createStatisticsSnapshot() = statisticsCollector.createStatisticsSnapshot()

}
