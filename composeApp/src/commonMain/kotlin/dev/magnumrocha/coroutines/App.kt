package dev.magnumrocha.coroutines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.magnumrocha.coroutines.core.TaskBucket
import dev.magnumrocha.coroutines.core.TasksProcessor
import dev.magnumrocha.coroutines.core.TasksProcessorImpl
import dev.magnumrocha.coroutines.core.TasksProducer
import dev.magnumrocha.coroutines.core.TasksProducerImpl
import dev.magnumrocha.coroutines.core.TasksState
import dev.magnumrocha.coroutines.theme.ApplicationTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Duration.Companion.seconds

private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private val tasksProcessor: TasksProcessor = TasksProcessorImpl(
    initialDispatchers = listOf(
        Dispatchers.Main,
        Dispatchers.Default,
        Dispatchers.IO
    ),
    scope = appScope
)

private val tasksProducer: TasksProducer = TasksProducerImpl(
    timeInterval = 2.seconds,
    scope = appScope
)

@Composable
fun App(
    viewModel: AppViewModel = viewModel { AppViewModel(tasksProducer, tasksProcessor) }
) = ApplicationTheme {

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val dispatchers by viewModel.dispatchers.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.safeContentPadding().fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DispatchersList(
                dispatchers = dispatchers,
                modifier = Modifier.fillMaxWidth()
            )

            TasksList(
                tasks = tasks,
                onDispatcherSelected = viewModel::assignDispatcher,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            TasksGenerationButton(
                onStart = { viewModel.startTasksGeneration() },
                onStop = { viewModel.stopTasksGeneration() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DispatchersList(
    dispatchers: List<DispatchersUpdate>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Dispatchers", style = MaterialTheme.typography.headlineSmall)

        for (dispatcher in dispatchers) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dispatcher.name,
                    maxLines = 2,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
                Text(
                    text = dispatcher.amount.toString(),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }
}

@Composable
private fun TasksList(
    tasks: List<TasksState>,
    onDispatcherSelected: (TaskBucket, CoroutineDispatcher) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Tasks", style = MaterialTheme.typography.headlineSmall)
        }

//        item {
//            TasksBucketItem(
//                taskState = TasksState(
//                    tasks = TaskBucket(id = "0", type = TaskType.CPU_INTENSIVE, amount = 10),
//                    state = null,
//                    amountProcessed = 0
//                ),
//                onDispatcherSelected = onDispatcherSelected,
//            )
//        }

        items(
            items = tasks,
            key = { it.tasks.id }
        ) { taskState ->
            TasksBucketItem(
                taskState = taskState,
                onDispatcherSelected = onDispatcherSelected
            )
            if (taskState != tasks.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun TasksBucketItem(
    taskState: TasksState,
    onDispatcherSelected: (TaskBucket, CoroutineDispatcher) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDispatcherSelected by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(taskState.tasks.id + ": " + taskState.tasks.type.name)

            val amount = if (taskState.amountProcessed > 0) {
                taskState.amountProcessed.toString() + " / " + taskState.tasks.amount.toString()
            } else {
                taskState.tasks.amount.toString()
            }

            Text(
                text = amount,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    onDispatcherSelected(taskState.tasks, Dispatchers.Main)
                    isDispatcherSelected = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inversePrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                enabled = !isDispatcherSelected,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Main", style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = {
                    onDispatcherSelected(taskState.tasks, Dispatchers.Default)
                    isDispatcherSelected = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                enabled = !isDispatcherSelected,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Default", style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = {
                    onDispatcherSelected(taskState.tasks, Dispatchers.IO)
                    isDispatcherSelected = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                enabled = !isDispatcherSelected,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("IO", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TasksGenerationButton(
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isStated by remember { mutableStateOf(false) }

    Button(
        modifier = modifier,
        onClick = {
            isStated = !isStated
            if (isStated) onStart() else onStop()
        }
    ) {
        Text(if (isStated) "Stop" else "Start")
    }
}
