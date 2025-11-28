package dev.magnumrocha.coroutines.core

data class TaskBucket(
    val id: String,
    val type: TaskType,
    val amount: Int
)

enum class TaskType {
    UI_UPDATE, CPU_INTENSIVE, BLOCK
}

enum class TasksProcessState {
    IDLE, PROCESSING, DONE
}

data class TasksState(
    val tasks: TaskBucket,
    val state: TasksProcessState?,
    val amountProcessed: Int
)
