package dev.magnumrocha.coroutines

import dev.magnumrocha.coroutines.core.TaskBucket
import dev.magnumrocha.coroutines.core.TaskType
import dev.magnumrocha.coroutines.core.TasksProcessState
import dev.magnumrocha.coroutines.core.TasksProcessor
import dev.magnumrocha.coroutines.core.TasksProducer
import dev.magnumrocha.coroutines.core.TasksState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTests {

    private val testDispatcher = StandardTestDispatcher()

    // mocks
    private lateinit var fakeTasksProducer: FakeTasksProducer
    private lateinit var fakeTasksProcessor: FakeTasksProcessor

    private lateinit var viewModel: AppViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTasksProducer = FakeTasksProducer()
        fakeTasksProcessor = FakeTasksProcessor()
        viewModel = AppViewModel(fakeTasksProducer, fakeTasksProcessor)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tasks should be empty initially`() = runTest {
        assertEquals(emptyList(), viewModel.tasks.value)
    }

    @Test
    fun `startTasksGeneration should call start on producer`() {
        // Act
        viewModel.startTasksGeneration()

        // Assert
        assertEquals(true, fakeTasksProducer.isStarted)
    }

    @Test
    fun `stopTasksGeneration should call stop on producer`() {
        // Act
        viewModel.stopTasksGeneration()

        // Assert
        assertEquals(true, fakeTasksProducer.isStopped)
    }

    @Test
    fun `assignDispatcher should call enqueue on processor`() {
        // Arrange
        val bucket = TaskBucket("1", TaskType.UI_UPDATE, 1)

        // Act
        viewModel.assignDispatcher(bucket, Dispatchers.Main)

        // Assert
        assertEquals(bucket, fakeTasksProcessor.lastEnqueuedTask)
        assertEquals(Dispatchers.Main, fakeTasksProcessor.lastEnqueuedDispatcher)
    }

    @Test
    fun `tasks should update when producer emits new task`() = runTest {
        // Arrange
        val bucket = TaskBucket("1", TaskType.UI_UPDATE, 1)
        fakeTasksProducer.emitTask(bucket)

        // Act
        testDispatcher.scheduler.advanceUntilIdle() // advance scheduler to allow the Flow collection

        // Assert
        val expectedTaskState = TasksState(bucket, null, 0)
        assertEquals(1, viewModel.tasks.value.size)
        assertEquals(expectedTaskState, viewModel.tasks.value.first())
    }

    @Test
    fun `tasks should update when processor emits updates`() = runTest {
        // 1. Emit a task from TasksProducer so it exists in the list
        val bucket = TaskBucket("1", TaskType.UI_UPDATE, 1)
        fakeTasksProducer.emitTask(bucket)
        testDispatcher.scheduler.advanceUntilIdle()

        // 2. Processor emits an update for that task
        val updatedState = TasksState(bucket, TasksProcessState.PROCESSING, 1)
        fakeTasksProcessor.emitTaskUpdate(updatedState)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(updatedState, viewModel.tasks.value.first())
    }

    @Test
    fun `tasks should be removed when state is DONE`() = runTest {
        // 1. Emit a task
        val bucket = TaskBucket("1", TaskType.UI_UPDATE, 1)
        fakeTasksProducer.emitTask(bucket)
        testDispatcher.scheduler.advanceUntilIdle()

        // 2. Update to DONE
        val doneState = TasksState(bucket, TasksProcessState.DONE, 1)
        fakeTasksProcessor.emitTaskUpdate(doneState)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.tasks.value.size)
    }

    @Test
    fun `dispatchers should update when processor emits dispatcher updates`() = runTest {
        val update = mapOf<CoroutineDispatcher, Int>(Dispatchers.Main to 5)

        // launch a collector to activate SharingStarted.WhileSubscribed
        val job = launch {
            viewModel.dispatchers.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        fakeTasksProcessor.emitDispatchersUpdate(update)

        // advance scheduler to process the emission
        testDispatcher.scheduler.advanceUntilIdle()

        val expected = listOf(DispatchersUpdate(Dispatchers.Main.toString(), 5))
        assertEquals(expected, viewModel.dispatchers.value)

        job.cancel()
    }

    class FakeTasksProducer : TasksProducer {
        var isStarted = false
        var isStopped = false
        private val _tasks = MutableSharedFlow<TaskBucket>()

        override fun observeTasks(): Flow<TaskBucket> = _tasks

        override fun start() {
            isStarted = true
        }

        override fun stop() {
            isStopped = true
        }

        suspend fun emitTask(bucket: TaskBucket) {
            _tasks.emit(bucket)
        }
    }

    class FakeTasksProcessor : TasksProcessor {
        var lastEnqueuedTask: TaskBucket? = null
        var lastEnqueuedDispatcher: CoroutineDispatcher? = null

        private val _tasksUpdates = MutableSharedFlow<TasksState>()
        private val _dispatchersUpdates = MutableStateFlow<Map<CoroutineDispatcher, Int>>(emptyMap())

        override fun observeTasksUpdates(): Flow<TasksState> = _tasksUpdates
        override fun observeDispatchersUpdates(): Flow<Map<CoroutineDispatcher, Int>> = _dispatchersUpdates

        override fun enqueue(tasksBucket: TaskBucket, dispatcher: CoroutineDispatcher) {
            lastEnqueuedTask = tasksBucket
            lastEnqueuedDispatcher = dispatcher
        }

        suspend fun emitTaskUpdate(state: TasksState) {
            _tasksUpdates.emit(state)
        }

        suspend fun emitDispatchersUpdate(update: Map<CoroutineDispatcher, Int>) {
            _dispatchersUpdates.emit(update)
        }
    }
}
