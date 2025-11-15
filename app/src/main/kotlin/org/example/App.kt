package org.example

import com.jakewharton.mosaic.ui.*
import com.jakewharton.mosaic.*
import com.jakewharton.mosaic.layout.*
import com.jakewharton.mosaic.modifier.Modifier
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import com.google.ai.edge.litertlm.*



fun main(args: Array<String>) {
  val modelPath =
      requireNotNull(args.getOrNull(0)) { "Model path must be provided as the first argument." }


  runMosaicBlocking {
    var response by remember { mutableStateOf("") }

    val screen = LocalTerminalState.current
    val screenSize = screen.size
    val screenWidth = screenSize.columns
    val screenHeight = screenSize.rows

    Box(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.width(screenWidth)) {
        Row(
          modifier = Modifier.width(screenWidth),
          horizontalArrangement = Arrangement.End,
        ) {
          Text(
            "Tell me a joke",
            modifier = Modifier.border().padding(1, 0),
          )
        }
        if (response != "") {
          Text(
            "${response}",
            modifier = Modifier.border().padding(1, 0),
          )
        }
      }
    }

    // LaunchedEffect(Unit) {
    //   awaitCancellation()
    // }

    LaunchedEffect(Unit) {
      Engine.setNativeMinLogServerity(LogSeverity.ERROR) // silence noisy log for the TUI.
      val engine = Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU))
      engine.use { engine ->
        engine.initialize()
        engine.createConversation().use { conversation ->
          conversation.sendMessageAsync(Message.of("Tell me a joke.")).collect { response += it.toString() }
        }
      }
    }
  }
}
