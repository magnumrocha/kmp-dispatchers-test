package dev.magnumrocha.coroutines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.magnumrocha.coroutines.core.TaskBucket
import dev.magnumrocha.coroutines.core.TasksProcessState
import dev.magnumrocha.coroutines.core.TasksProcessor
import dev.magnumrocha.coroutines.core.TasksProducer
import dev.magnumrocha.coroutines.core.TasksState
import dev.magnumrocha.coroutines.core.debugLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.collections.map

data class DispatchersUpdate(
    val name: String,
    val amount: Int
)

class AppViewModel(
    private val tasksProducer: TasksProducer,
    private val tasksProcessor: TasksProcessor
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<TasksState>>(emptyList())
    val tasks: StateFlow<List<TasksState>> = _tasks.asStateFlow()

    val dispatchers: StateFlow<List<DispatchersUpdate>> = tasksProcessor.observeDispatchersUpdates()
        .onEach { debugLog("dispatchers", "Dispatchers update: $it") }
        .map {
            it.map { (dispatcher, amount) ->
                DispatchersUpdate(name = dispatcher.toString(), amount = amount)
            }
        }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(), emptyList())

    init {
        tasksProducer.observeTasks()
            .onEach { debugLog("tasks", "New task: $it") }
            .onEach { tasks ->
                _tasks.update { it + TasksState(tasks = tasks, state = null, amountProcessed = 0) }
            }
            .launchIn(viewModelScope)

        tasksProcessor.observeTasksUpdates()
            .onEach { update ->
                _tasks.update { tasks ->
                    tasks.mapNotNull {
                        if (it.tasks == update.tasks) {
                            if (update.state == TasksProcessState.DONE) null
                            else it.copy(state = update.state, amountProcessed = update.amountProcessed)
                        } else {
                            it
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun startTasksGeneration() {
        debugLog("tasks", "Starting tasks...")
        tasksProducer.start()
    }

    fun stopTasksGeneration() {
        debugLog("tasks", "Stopping tasks...")
        tasksProducer.stop()
    }

    fun assignDispatcher(taskBucket: TaskBucket, dispatcher: CoroutineDispatcher) {
        tasksProcessor.enqueue(taskBucket, dispatcher)
    }

    override fun onCleared() {
        tasksProducer.stop()
        super.onCleared()
    }
}
