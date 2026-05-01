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

  Engine.setNativeMinLogSeverity(LogSeverity.ERROR) // silence noisy log for the TUI.
  val engine = Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU()))
  engine.initialize()
  val conversation = engine.createConversation(ConversationConfig(systemInstruction = Contents.of("No emoji.")))

  runMosaicBlocking {
    var messages by remember { mutableStateOf(mutableListOf<String>()) }
    var currentResponse by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }

    val screen = LocalTerminalState.current
    val screenSize = screen.size
    val screenWidth = screenSize.columns
    val screenHeight = screenSize.rows

    Box(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.width(screenWidth)) {
        messages.forEachIndexed { index, message ->
          if (index % 2 == 0) {
            Row(
              modifier = Modifier.width(screenWidth),
              horizontalArrangement = Arrangement.End,
            ) {
              Text(
                message,
                modifier = Modifier.border().padding(1, 0),
              )
            }
          } else {
            Text(
              message,
              modifier = Modifier.border().padding(1, 0),
            )
          }
        }

        if (currentResponse != "") {
          Text(
            "${currentResponse}",
            modifier = Modifier.border().padding(1, 0),
          )
        }

        TextInput(
          input,
          modifier = Modifier.fillMaxWidth().border().padding(1, 0),
          onEnter = {
            if (input != "") {

              val message = Message.of(input)

              val callback = object : MessageCallback {
                override fun onMessage(message: Message) {
                  currentResponse += message.toString()
                }
                override fun onDone() {
                  messages.add(currentResponse.trim())
                  currentResponse = ""
                }
                override fun onError(throwable: Throwable) {}
              }

              conversation.sendMessageAsync(message, callback)

              messages.add(input)
              input = ""
            }
          }
        ) {
          input = it
        }
      }
    }

    LaunchedEffect(Unit) {
      awaitCancellation()
    }
  }
}

// https://github.com/JakeWharton/mosaic/issues/274#issuecomment-2770730596
@Composable
fun TextInput(
    value: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    background: Color = Color.Unspecified,
    textStyle: TextStyle = TextStyle.Unspecified,
    onEnter: (() -> Unit) = {},
    onTextChanged: ((String) -> Unit),
) {
    Text(
        value = value,
        color = color,
        background = background,
        textStyle = textStyle,
        modifier = modifier.onKeyEvent { event ->
            when {
                event == KeyEvent("c", ctrl = true) -> return@onKeyEvent false
                event.key.toCharArray().singleOrNull() != null -> onTextChanged(value + event.key)
                event == KeyEvent("Backspace") -> onTextChanged(value.dropLast(1))
                event == KeyEvent("Enter") -> onEnter()
                else -> return@onKeyEvent false
            }
            true
        },
    )
}
