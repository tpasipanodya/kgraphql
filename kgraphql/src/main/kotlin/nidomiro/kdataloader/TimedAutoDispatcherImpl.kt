package nidomiro.kdataloader

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import nidomiro.kdataloader.statistics.SimpleStatisticsCollector
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class TimedAutoDispatcherImpl<K, R>(
    options: TimedAutoDispatcherDataLoaderOptions<K, R>,
    batchLoader: BatchLoader<K, R>,
    parent: Job? = null,
    propagateables: List<() -> CoroutineContext.Element>
) : SimpleDataLoaderImpl<K, R>(options, SimpleStatisticsCollector(), batchLoader, propagateables), CoroutineScope {

    private val autoChannel = Channel<Unit>()
    override val coroutineContext = Job(parent)

    val dataLoaderDispatcher = newSingleThreadContext("CounterContext")

    init {
        val context = propagateables.fold(
            CoroutineName("TimedAutoDispatcherImpl:init"),
            CoroutineContext::plus
        )
        launch(context) {
            var job: Job? = null
            while (true) {
                autoChannel.receive()
//                println("TimedAutoDispatcherImpl:message")
                if (job?.isActive == true) job.cancelAndJoin()
                job = launch(CoroutineName("TimedAutoDispatcherImpl:autoChannel") + coroutineContext) {
                    delay(options.waitInterval)
                    if (isActive) launch {
                        dispatch()
                    }
                }
            }
        }
    }

    suspend fun cancel() {
        coroutineContext.cancel()
        autoChannel.close()
        dispatch()
    }

    override suspend fun loadAsync(key: K): Deferred<R> {
        return super.loadAsync(key).also { autoChannel.send(Unit) }
    }

    override suspend fun loadManyAsync(vararg keys: K): Deferred<List<R>> {
        return super.loadManyAsync(*keys).also { autoChannel.send(Unit) }
    }

    override suspend fun clear(key: K) {
        super.clear(key).also { autoChannel.send(Unit) }
    }

    override suspend fun clearAll() {
        super.clearAll().also { autoChannel.send(Unit) }
    }

    override suspend fun prime(key: K, value: R) {
        super.prime(key, value).also { autoChannel.send(Unit) }
    }

    override suspend fun prime(key: K, value: Throwable){
        super.prime(key, value).also { autoChannel.send(Unit) }
    }

}
