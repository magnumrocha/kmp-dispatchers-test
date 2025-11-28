package dev.magnumrocha.coroutines.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class TasksProducerTest {

    @Test
    fun `tasksFlow should not emit any tasks if not started`() = runTest {
        // Arrange
        val tasksProducer = TasksProducerImpl(timeInterval = 1.milliseconds, scope = backgroundScope)

        // Act
        val tasks = mutableListOf<TaskBucket>()
        val job = launch {
            tasksProducer.observeTasks().collect { tasks.add(it) }
        }
        advanceTimeBy(100) // waiting a bit to ensure nothing happens...
        job.cancel()

        // Assert
        assertEquals(0, tasks.size, "There should be no tasks emitted before starting the producer.")
    }

    @Test
    fun `tasksFlow should emit tasks when started`() = runTest {
        // Arrange
        val tasksProducer = TasksProducerImpl(timeInterval = 1.milliseconds, scope = backgroundScope)

        // Act
        tasksProducer.start()

        // Assert
        val emittedTask = tasksProducer.observeTasks().firstOrNull()
        assertNotNull(emittedTask, "A task should be emitted after the producer starts.")
    }

    @Test
    fun `tasksFlow should stop emitting tasks after stop is called`() = runTest {
        // Arrange
        val tasksProducer = TasksProducerImpl(timeInterval = 1.milliseconds, scope = backgroundScope)

        // Act
        tasksProducer.start() // start task generation

        // Assert
        assertNotNull(tasksProducer.observeTasks().firstOrNull(), "A task should be emitted after the producer starts.")

        // Act
        tasksProducer.stop() // stop task generation

        // check that no tasks are emitted
        val tasks = mutableListOf<TaskBucket>()
        val job = launch {
            tasksProducer.observeTasks().collect { tasks.add(it) }
        }

        advanceTimeBy(1.milliseconds)
        job.cancel()

        // Assert
        assertEquals(0, tasks.size, "No tasks should be emitted after the producer is stopped.")
    }

    @Test
    fun `tasksFlow should emit tasks periodically based on the time interval`() = runTest {
        // Arrange
        val tasksProducer = TasksProducerImpl(timeInterval = 1.milliseconds, scope = backgroundScope)

        // Act
        tasksProducer.start()
        advanceTimeBy(1.milliseconds)

        // Assert
        val task1 = tasksProducer.observeTasks().firstOrNull()
        assertNotNull(task1, "A task should be emitted after the first interval.")

        // Act
        advanceTimeBy(1.milliseconds)
        val task2 = tasksProducer.observeTasks().firstOrNull()

        // Assert
        assertNotNull(task2, "A task should be emitted after the second interval.")
        assertTrue("check that the tasks are different") { task1 != task2 }
    }
}
