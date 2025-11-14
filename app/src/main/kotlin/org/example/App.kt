package org.example

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message

suspend fun main(args: Array<String>) {
  val modelPath =
    requireNotNull(args.getOrNull(0)) { "Model path must be provided as the first argument." }

  val engine = Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU))
  engine.initialize()

  engine.use { engine ->
    val conversationConfig =
      ConversationConfig(systemMessage = Message.of("You are a helpful assistant."))

    engine.createConversation(conversationConfig).use { conversation ->
      while (true) {
        print(">>> ")
        conversation.sendMessageAsync(Message.of(readln())).collect { print(YELLOW + it + RESET) }
      }
    }
  }
}

// ANSI color codes
const val RESET = "\u001B[0m"
const val YELLOW = "\u001B[33m"
