package dev.magnumrocha.coroutines.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

interface TasksProcessor {
    fun observeTasksUpdates(): Flow<TasksState>
    fun observeDispatchersUpdates(): Flow<Map<CoroutineDispatcher, Int>>

    fun enqueue(tasksBucket: TaskBucket, dispatcher: CoroutineDispatcher)
}

@OptIn(ExperimentalCoroutinesApi::class)
class TasksProcessorImpl(
    initialDispatchers: List<CoroutineDispatcher>,
    private val scope: CoroutineScope
): TasksProcessor {
    private companion object {
        const val TAG = "TasksProcessor"
    }

    private val tasksUpdates = MutableSharedFlow<TasksState>()
    private val dispatchersUpdatesCounter = MutableStateFlow(initialDispatchers.associateWith { 0 })

    override fun observeTasksUpdates(): Flow<TasksState> = tasksUpdates
    override fun observeDispatchersUpdates(): Flow<Map<CoroutineDispatcher, Int>> = dispatchersUpdatesCounter

    override fun enqueue(tasksBucket: TaskBucket, dispatcher: CoroutineDispatcher) {
        scope.launch {
            tasksUpdates.emit(TasksState(tasksBucket, TasksProcessState.IDLE, 0))
        }
        addToDispatcher(dispatcher, tasksBucket.amount)
        launchTasksOnDispatcher(dispatcher, tasksBucket)
    }

    private fun addToDispatcher(dispatcher: CoroutineDispatcher, taskAmount: Int) {
        dispatchersUpdatesCounter.update {
            it.toMutableMap().apply {
                put(dispatcher, get(dispatcher)?.plus(taskAmount) ?: taskAmount)
            }
        }
    }

    private fun launchTasksOnDispatcher(dispatcher: CoroutineDispatcher, tasksBucket: TaskBucket) {
        scope.launch(dispatcher) {
            tasksUpdates.emit(TasksState(tasksBucket, TasksProcessState.PROCESSING, 0))
            for (taskNumber in 1..tasksBucket.amount) {
                debugLog(TAG, "Processing task $taskNumber of type ${tasksBucket.type}")
                when (tasksBucket.type) {
                    TaskType.UI_UPDATE -> fakeUITaskProcess()
                    TaskType.CPU_INTENSIVE -> fakeIntensiveCPUTasksProcess()
                    TaskType.BLOCK -> fakeIOTasksProcess()
                }
                tasksUpdates.emit(TasksState(tasksBucket, TasksProcessState.PROCESSING, taskNumber))
                dispatchersUpdatesCounter.update { it.toMutableMap().apply { put(dispatcher, get(dispatcher)?.minus(1) ?: 0) } }
            }
            tasksUpdates.emit(TasksState(tasksBucket, TasksProcessState.DONE, tasksBucket.amount))
        }
    }

    private suspend fun fakeUITaskProcess() {
        val randomLayoutBounds = List(100) { Random.nextInt(0, 1000) }
        randomLayoutBounds.chunked(2).forEach { (width, height) ->
            // simulate layout measurement and drawing
            val area = width * height
            val aspect = width.toFloat() / height.toFloat()
            debugLog(TAG, "Drawing a rectangle of area $area and aspect ratio $aspect")
            delay(1.milliseconds)
        }
    }

    private fun fakeIntensiveCPUTasksProcess() {
        val randomList = List(1_000_000) { Random.nextInt() }
        randomList.sorted()
    }

    private fun fakeIOTasksProcess() {
        threadSleep(200.milliseconds)
    }
}
