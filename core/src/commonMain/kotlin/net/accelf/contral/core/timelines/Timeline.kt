package net.accelf.contral.core.timelines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.accelf.contral.core.router.PageComponent

val Timeline: PageComponent = { route, ctx ->
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val handlers = LocalTimelineHandlers.current

    Scaffold(
        scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
    ) {
        route.params["handler"]?.let { handlerName ->
            handlers[handlerName]?.invoke(route.params)?.let { handler ->
                var items by remember { mutableStateOf(emptyList<TimelineItem>()) }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier.padding(4.dp),
                            elevation = 4.dp,
                        ) {
                            item.render(ctx)
                        }
                    }

                    if (items.isEmpty()) {
                        item {
                            SideEffect {
                                scope.launch {
                                    items = runCatching { handler.initialFetch() }
                                        .getOrElse {
                                            snackbarHostState.showSnackbar(
                                                message = "error loading timeline: ${it.message}",
                                                duration = SnackbarDuration.Long,
                                            )
                                            return@launch
                                        }
                                }
                            }
                        }
                    } else {
                        item {
                            SideEffect {
                                scope.launch {
                                    val old = runCatching { handler.fetchNext(items.last()) }
                                        .getOrElse {
                                            snackbarHostState.showSnackbar(
                                                message = "error loading timeline: ${it.message}",
                                                duration = SnackbarDuration.Long,
                                            )
                                            return@launch
                                        }
                                    items = items + old
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(4.dp),
                                )
                            }
                        }
                    }
                }
            } ?: scope.launch {
                snackbarHostState.showSnackbar(
                    message = "handler $handlerName not found",
                    duration = SnackbarDuration.Indefinite,
                )
            }
        } ?: scope.launch {
            snackbarHostState.showSnackbar(
                message = "handler not specified",
                duration = SnackbarDuration.Indefinite,
            )
        }
    }
}
