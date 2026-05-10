package org.example

import com.google.ai.edge.litertlm.*
import java.util.concurrent.CountDownLatch

fun main(args: Array<String>) {
  val modelPath =
      requireNotNull(args.getOrNull(0)) { "Model path must be provided as the first argument." }

  val engine = Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU()))
  engine.initialize()
  val conversation = engine.createConversation(ConversationConfig(systemInstruction = Contents.of("No emoji.")))

  println("Chat initialized. Type your message and press Enter. Type 'exit' to quit.")

  while (true) {
    print("> ")
    val input = readLine() ?: break
    if (input.lowercase() == "exit") break
    if (input.isBlank()) continue

    val latch = CountDownLatch(1)
    val callback = object : MessageCallback {
      override fun onMessage(message: Message) {
        print(message.toString())
        System.out.flush()
      }

      override fun onDone() {
        println()
        latch.countDown()
      }

      override fun onError(throwable: Throwable) {
        println("\nError: ${throwable.message}")
        latch.countDown()
      }
    }

    conversation.sendMessageAsync(input, callback)
    latch.await()
  }

  println("Goodbye!")
}
