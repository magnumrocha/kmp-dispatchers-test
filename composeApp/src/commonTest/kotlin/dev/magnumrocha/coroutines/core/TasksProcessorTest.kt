package dev.magnumrocha.coroutines.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
class TasksProcessorTest {

    @Test
    fun `enqueue should emit TasksState with IDLE state`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        val processor = TasksProcessorImpl(listOf(dispatcher), scope)
        val taskBucket = TaskBucket("#1", TaskType.UI_UPDATE, 2)

        val updates = mutableListOf<TasksState>()
        val job = scope.launch {
            processor.observeTasksUpdates().collect { updates.add(it) }
        }

        // Act
        processor.enqueue(taskBucket, dispatcher)
        advanceUntilIdle()

        // Assert
        val initialState = updates.first()

        assertEquals(TasksProcessState.IDLE, initialState.state)
        assertEquals(taskBucket, initialState.tasks)
        assertEquals(0, initialState.amountProcessed)
        job.cancel()
    }

    @Test
    fun `enqueue should update dispatchers with task counts`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        val processor = TasksProcessorImpl(listOf(dispatcher), scope)
        val taskBucket = TaskBucket("#1", TaskType.CPU_INTENSIVE, 5)

        // Act
        processor.enqueue(taskBucket, dispatcher)
        val dispatcherUpdates = processor.observeDispatchersUpdates().first()

        // Assert
        assertEquals(5, dispatcherUpdates[dispatcher])
    }

    @Test
    fun `enqueue processes tasks and updates tasksStates correctly`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        val processor = TasksProcessorImpl(listOf(dispatcher), scope)
        val taskBucket = TaskBucket("#1", TaskType.UI_UPDATE, 3)

        val tasksStates = mutableListOf<TasksState>()
        val job = scope.launch {
            processor.observeTasksUpdates().collect { tasksStates.add(it) }
        }

        // Act
        processor.enqueue(taskBucket, dispatcher)
        advanceUntilIdle()

        // Assert
        assertEquals(6, tasksStates.size) // IDLE, Processing (x3), DONE
        assertEquals(TasksProcessState.IDLE, tasksStates[0].state)
        assertEquals(TasksProcessState.PROCESSING, tasksStates[1].state) // amountProcessed == 0
        assertEquals(TasksProcessState.PROCESSING, tasksStates[2].state) // amountProcessed == 1
        assertEquals(TasksProcessState.PROCESSING, tasksStates[3].state) // amountProcessed == 2
        assertEquals(TasksProcessState.PROCESSING, tasksStates[4].state) // amountProcessed == 3
        assertEquals(TasksProcessState.DONE, tasksStates[5].state)
        job.cancel()
    }

    @Test
    fun `dispatcher task count decreases correctly after processing`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        val processor = TasksProcessorImpl(listOf(dispatcher), scope)
        val taskBucket = TaskBucket("#1", TaskType.UI_UPDATE, 4)

        val dispatcherCounts = mutableListOf<Map<CoroutineDispatcher, Int>>()
        val job = scope.launch {
            processor.observeDispatchersUpdates().collect { dispatcherCounts.add(it) }
        }

        // Act
        processor.enqueue(taskBucket, dispatcher)
        advanceUntilIdle()

        // Assert
        assertEquals(5, dispatcherCounts.size) // initial (0 -> zero) + 4 task processing steps
        assertEquals(0, dispatcherCounts.last()[dispatcher]) // no tasks left in dispatcher
        job.cancel()
    }
}
