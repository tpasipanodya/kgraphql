package nidomiro.kdataloader.factories

import kotlinx.coroutines.Job
import nidomiro.kdataloader.*
import kotlin.coroutines.CoroutineContext

typealias DataLoaderFactoryMethod<K, R> =
        suspend (options: DataLoaderOptions<K, R>,
                 batchLoader: BatchLoader<K, R>,
                 parent: Job?,
                 propagateables: List<() -> CoroutineContext.Element>) -> DataLoader<K, R>

open class DataLoaderFactory<K, R>(
    @Suppress("MemberVisibilityCanBePrivate")
    protected val optionsFactory: () -> DataLoaderOptions<K, R>,
    @Suppress("MemberVisibilityCanBePrivate")
    protected val batchLoader: BatchLoader<K, R>,
    @Suppress("MemberVisibilityCanBePrivate")
    protected val cachePrimes: Map<K, ExecutionResult<R>>,
    protected val factoryMethod: DataLoaderFactoryMethod<K, R>
) {

    suspend fun constructNew(parent: Job?, propagateables: List<() -> CoroutineContext.Element>): DataLoader<K, R> {
        val dataLoader = factoryMethod(optionsFactory(), batchLoader, parent, propagateables)
        cachePrimes.forEach { (key, value) -> dataLoader.prime(key, value) }
        return dataLoader
    }
}
