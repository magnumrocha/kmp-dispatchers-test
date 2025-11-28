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

    private val _tasksUpdates = MutableSharedFlow<TasksState>()
    private val _dispatchersUpdates = MutableStateFlow(initialDispatchers.associateWith { 0 })

    override fun observeTasksUpdates(): Flow<TasksState> = _tasksUpdates
    override fun observeDispatchersUpdates(): Flow<Map<CoroutineDispatcher, Int>> = _dispatchersUpdates

    override fun enqueue(tasksBucket: TaskBucket, dispatcher: CoroutineDispatcher) {
        scope.launch {
            _tasksUpdates.emit(TasksState(tasksBucket, TasksProcessState.IDLE, 0))
        }
        addToDispatcher(dispatcher, tasksBucket.amount)
        launchTasksOnDispatcher(dispatcher, tasksBucket)
    }

    private fun addToDispatcher(dispatcher: CoroutineDispatcher, taskAmount: Int) {
        _dispatchersUpdates.update {
            it.toMutableMap().apply {
                put(dispatcher, get(dispatcher)?.plus(taskAmount) ?: taskAmount)
            }
        }
    }

    private fun launchTasksOnDispatcher(dispatcher: CoroutineDispatcher, tasksBucket: TaskBucket) {
        scope.launch(dispatcher) {
            _tasksUpdates.emit(TasksState(tasksBucket, TasksProcessState.PROCESSING, 0))
            for (taskNumber in 1..tasksBucket.amount) {
                debugLog(TAG, "Processing task $taskNumber of type ${tasksBucket.type}")
                when (tasksBucket.type) {
                    TaskType.UI_UPDATE -> fakeUITaskProcess()
                    TaskType.CPU_INTENSIVE -> fakeIntensiveCPUTasksProcess()
                    TaskType.BLOCK -> fakeIOTasksProcess()
                }
                _tasksUpdates.emit(TasksState(tasksBucket, TasksProcessState.PROCESSING, taskNumber))
                _dispatchersUpdates.update { it.toMutableMap().apply { put(dispatcher, get(dispatcher)?.minus(1) ?: 0) } }
            }
            _tasksUpdates.emit(TasksState(tasksBucket, TasksProcessState.DONE, tasksBucket.amount))
        }
    }

    private suspend fun fakeUITaskProcess() {
        delay(10.milliseconds)
    }

    private suspend fun fakeIntensiveCPUTasksProcess() {
        delay(100.milliseconds)
    }

    private fun fakeIOTasksProcess() {
        threadSleep(200.milliseconds)
    }
}
