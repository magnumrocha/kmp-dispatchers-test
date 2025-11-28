package dev.magnumrocha.coroutines.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.random.Random
import kotlin.time.Duration

interface TasksProducer {
    fun observeTasks(): Flow<TaskBucket>

    fun start()
    fun stop()
}

@OptIn(ExperimentalAtomicApi::class)
class TasksProducerImpl(
    private val timeInterval: Duration,
    scope: CoroutineScope
) : TasksProducer {
    private val tasksNumber = AtomicInt(0)
    private val state = MutableStateFlow(false)
    private val _tasksFlow = MutableSharedFlow<TaskBucket>()

    init {
        scope.launch {
            state.collectLatest { isEnabled ->
                if (isEnabled) {
                    while (true) {
                        _tasksFlow.emit(createRandomTasks())
                        delay(timeInterval)
                    }
                }
            }
        }
    }

    override fun observeTasks(): Flow<TaskBucket> = _tasksFlow

    override fun start() { state.value = true }
    override fun stop() { state.value = false }

    private fun createRandomTasks(): TaskBucket {
        val randomInt = Random.nextInt(1, 100)
        val randomTaskType = TaskType.entries[Random.nextInt(0, TaskType.entries.size)]
        return TaskBucket(
            id = "#${tasksNumber.incrementAndFetch()}",
            type = randomTaskType,
            amount = randomInt
        )
    }
}
