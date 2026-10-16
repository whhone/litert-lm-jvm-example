package org.example

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EmbeddingEngine
import com.google.ai.edge.litertlm.EmbeddingEngineConfig
import com.google.ai.edge.litertlm.InputData
import java.io.File
import kotlin.math.sqrt

// ==============================================================================
// 1. Math & Helper Functions
// ==============================================================================

fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Double {
  var dot = 0.0
  var norm1 = 0.0
  var norm2 = 0.0
  for (i in v1.indices) {
    val a = v1[i].toDouble()
    val b = v2[i].toDouble()
    dot += a * b
    norm1 += a * a
    norm2 += b * b
  }
  if (norm1 == 0.0 || norm2 == 0.0) return 0.0
  return dot / (sqrt(norm1) * sqrt(norm2))
}

data class SingleBenchmarkResult(
  val modality: String,
  val dim: Int,
  val preview: List<Float>,
  val avgMs: Double,
  val embedding: FloatArray,
)

fun benchmarkSingle(
  engine: EmbeddingEngine,
  label: String,
  input: InputData,
  iterations: Int = 3,
): SingleBenchmarkResult {
  // Warmup run
  val warmupResp = engine.computeEmbedding(listOf(input))
  val vec = warmupResp.embedding
  val dim = vec.size
  val preview = vec.take(4)

  // Latency benchmark
  var totalNanos = 0L
  for (i in 0 until iterations) {
    val t0 = System.nanoTime()
    engine.computeEmbedding(listOf(input))
    totalNanos += (System.nanoTime() - t0)
  }

  val avgMs = (totalNanos.toDouble() / iterations) / 1_000_000.0
  return SingleBenchmarkResult(
    modality = label,
    dim = dim,
    preview = preview,
    avgMs = avgMs,
    embedding = vec,
  )
}

// ==============================================================================
// 2. Part 1: Single Embedding Generation & Benchmark
// ==============================================================================

fun runSingleEmbeddingsSection(
  engine: EmbeddingEngine,
  backendLabel: String,
  samples: Map<String, File>,
  iterations: Int = 3,
): Map<String, SingleBenchmarkResult> {
  println("\n" + "=".repeat(80))
  println("  PART 1: SINGLE EMBEDDING GENERATION & BENCHMARK [$backendLabel]")
  println("=".repeat(80))

  val results = mutableMapOf<String, SingleBenchmarkResult>()

  // 1. Text single embedding
  val sampleText = "LiteRT high-performance multimodal embeddings on Apple Silicon."
  println("\n[1] Single Text Embedding:")
  println("    Input: \"$sampleText\"")
  val rText = benchmarkSingle(engine, "Text", InputData.Text(sampleText), iterations)
  val textPreviewFormatted = rText.preview.map { String.format("%.4f", it) }
  println("    ✓ Output Dimension: ${rText.dim}")
  println("    ✓ Vector Preview:   $textPreviewFormatted ...")
  println(String.format("    ✓ Avg Latency:      %.2f ms", rText.avgMs))
  results["text"] = rText

  // 2. Vision single embedding
  val sampleImg = samples["apple_anchor"]!!
  println("\n[2] Single Vision (Image) Embedding:")
  println("    Input: ${sampleImg.name} (${sampleImg.length()} bytes)")
  val rImg = benchmarkSingle(engine, "Vision", InputData.Image(sampleImg.readBytes()), iterations)
  val imgPreviewFormatted = rImg.preview.map { String.format("%.4f", it) }
  println("    ✓ Output Dimension: ${rImg.dim}")
  println("    ✓ Vector Preview:   $imgPreviewFormatted ...")
  println(String.format("    ✓ Avg Latency:      %.2f ms", rImg.avgMs))
  results["vision"] = rImg

  // 3. Audio single embedding
  val sampleAud = samples["audio_anchor"]!!
  println("\n[3] Single Audio Embedding:")
  println("    Input: ${sampleAud.name} (${sampleAud.length()} bytes)")
  val rAud = benchmarkSingle(engine, "Audio", InputData.Audio(sampleAud.readBytes()), iterations)
  val audPreviewFormatted = rAud.preview.map { String.format("%.4f", it) }
  println("    ✓ Output Dimension: ${rAud.dim}")
  println("    ✓ Vector Preview:   $audPreviewFormatted ...")
  println(String.format("    ✓ Avg Latency:      %.2f ms", rAud.avgMs))
  results["audio"] = rAud

  return results
}

// ==============================================================================
// 3. Part 2: 3-Input Verification (2 Same Kind vs 1 Opposite)
// ==============================================================================

fun run3InputVerificationSection(
  engine: EmbeddingEngine,
  backendLabel: String,
  samples: Map<String, File>,
): Boolean {
  println("\n" + "=".repeat(80))
  println("  PART 2: 3-INPUT SIMILARITY VERIFICATION (2 Same Kind vs 1 Opposite) [$backendLabel]")
  println("=".repeat(80))

  var allCorrect = true

  // --------------------------------------------------------------------------
  // Test A: Text (3 Inputs: Anchor, Same Kind / Similar, Opposite / Different)
  // --------------------------------------------------------------------------
  println("\n[Test A] TEXT: 2 Same Kind vs 1 Opposite")
  val t1Anchor = "Apples are delicious and nutritious round fruits."
  val t2Similar = "A crisp, fresh red apple is great for a healthy snack."
  val t3Opposite = "Quantum computers process information using quantum bits and superposition."

  val embT1 = engine.computeEmbedding(listOf(InputData.Text(t1Anchor))).embedding
  val embT2 = engine.computeEmbedding(listOf(InputData.Text(t2Similar))).embedding
  val embT3 = engine.computeEmbedding(listOf(InputData.Text(t3Opposite))).embedding

  val simTSame = cosineSimilarity(embT1, embT2)
  val simTOpp = cosineSimilarity(embT1, embT3)
  val isTextCorrect = simTSame > simTOpp
  allCorrect = allCorrect && isTextCorrect

  println("  Input 1 (Anchor):        \"$t1Anchor\"")
  println("  Input 2 (Same Kind):     \"$t2Similar\"")
  println("  Input 3 (Opposite/Diff): \"$t3Opposite\"")
  println(String.format("  --> Similarity(Input 1, Input 2 [Same]):     %.4f  (Expected HIGH)", simTSame))
  println(String.format("  --> Similarity(Input 1, Input 3 [Opposite]): %.4f  (Expected LOW)", simTOpp))
  val verdictText = if (isTextCorrect) "✅ CORRECT (PASS)" else "❌ INCORRECT (FAIL)"
  println(String.format("  VERIFICATION [Sim(Same) > Sim(Opposite)]: %s (Δ = +%.4f)", verdictText, simTSame - simTOpp))

  // --------------------------------------------------------------------------
  // Test B: Vision (3 Inputs: Anchor Image, Same Kind Image, Opposite Image)
  // --------------------------------------------------------------------------
  println("\n[Test B] VISION: 2 Same Kind vs 1 Opposite")
  val v1Anchor = samples["apple_anchor"]!!
  val v2Similar = samples["apple_similar"]!!
  val v3Opposite = samples["car_opposite"]!!

  val embV1 = engine.computeEmbedding(listOf(InputData.Image(v1Anchor.readBytes()))).embedding
  val embV2 = engine.computeEmbedding(listOf(InputData.Image(v2Similar.readBytes()))).embedding
  val embV3 = engine.computeEmbedding(listOf(InputData.Image(v3Opposite.readBytes()))).embedding

  val simVSame = cosineSimilarity(embV1, embV2)
  val simVOpp = cosineSimilarity(embV1, embV3)
  val isVisionCorrect = simVSame > simVOpp
  allCorrect = allCorrect && isVisionCorrect

  println("  Input 1 (Anchor Image):        ${v1Anchor.name} (Apple)")
  println("  Input 2 (Same Kind Image):     ${v2Similar.name} (Apple Variant)")
  println("  Input 3 (Opposite/Diff Image): ${v3Opposite.name} (Blue Car)")
  println(String.format("  --> Similarity(Image 1, Image 2 [Same]):     %.4f  (Expected HIGH)", simVSame))
  println(String.format("  --> Similarity(Image 1, Image 3 [Opposite]): %.4f  (Expected LOW)", simVOpp))
  val verdictVis = if (isVisionCorrect) "✅ CORRECT (PASS)" else "❌ INCORRECT (FAIL)"
  println(String.format("  VERIFICATION [Sim(Same) > Sim(Opposite)]: %s (Δ = +%.4f)", verdictVis, simVSame - simVOpp))

  // --------------------------------------------------------------------------
  // Test C: Audio (3 Inputs: Anchor Audio, Same Kind Audio, Opposite Audio)
  // --------------------------------------------------------------------------
  println("\n[Test C] AUDIO: 2 Same Kind vs 1 Opposite")
  val a1Anchor = samples["audio_anchor"]!!
  val a2Similar = samples["audio_similar"]!!
  val a3Opposite = samples["audio_opposite"]!!

  val embA1 = engine.computeEmbedding(listOf(InputData.Audio(a1Anchor.readBytes()))).embedding
  val embA2 = engine.computeEmbedding(listOf(InputData.Audio(a2Similar.readBytes()))).embedding
  val embA3 = engine.computeEmbedding(listOf(InputData.Audio(a3Opposite.readBytes()))).embedding

  val simASame = cosineSimilarity(embA1, embA2)
  val simAOpp = cosineSimilarity(embA1, embA3)
  val isAudioCorrect = simASame > simAOpp
  allCorrect = allCorrect && isAudioCorrect

  println("  Input 1 (Anchor Audio):        ${a1Anchor.name} (Speech: 'Have a wonderful day')")
  println("  Input 2 (Same Kind Audio):     ${a2Similar.name} (Speech: 'Have a wonderful day' variant)")
  println("  Input 3 (Opposite/Diff Audio): ${a3Opposite.name} (Synthetic: 440Hz Pure Sine Tone)")
  println(String.format("  --> Similarity(Audio 1, Audio 2 [Same]):     %.4f  (Expected HIGH)", simASame))
  println(String.format("  --> Similarity(Audio 1, Audio 3 [Opposite]): %.4f  (Expected LOW)", simAOpp))
  val verdictAud = if (isAudioCorrect) "✅ CORRECT (PASS)" else "❌ INCORRECT (FAIL)"
  println(String.format("  VERIFICATION [Sim(Same) > Sim(Opposite)]: %s (Δ = +%.4f)", verdictAud, simASame - simAOpp))

  // --------------------------------------------------------------------------
  // Test D: Cross-Modal Verification (Text Query vs Matching Media vs Opposite)
  // --------------------------------------------------------------------------
  println("\n[Test D] CROSS-MODAL: 1 Text Query vs 1 Matching Media vs 1 Opposite Media")

  // Text vs Images
  val qApple = "A fresh red apple fruit"
  val embQApple = engine.computeEmbedding(listOf(InputData.Text(qApple))).embedding
  val simTvMatch = cosineSimilarity(embQApple, embV1)
  val simTvOpp = cosineSimilarity(embQApple, embV3)
  val isTvCorrect = simTvMatch > simTvOpp
  allCorrect = allCorrect && isTvCorrect

  println("  (1) Text Query: \"$qApple\"")
  println(String.format("      vs Matching Image (%s):   Score: %.4f", v1Anchor.name, simTvMatch))
  println(String.format("      vs Opposite Image (%s): Score: %.4f", v3Opposite.name, simTvOpp))
  val verdictTv = if (isTvCorrect) "✅ CORRECT (PASS)" else "❌ INCORRECT (FAIL)"
  println(String.format("      VERIFICATION: %s (Δ = +%.4f)", verdictTv, simTvMatch - simTvOpp))

  // Text vs Audios
  val qDay = "Wishing you to have a wonderful day"
  val embQDay = engine.computeEmbedding(listOf(InputData.Text(qDay))).embedding
  val simTaMatch = cosineSimilarity(embQDay, embA1)
  val simTaOpp = cosineSimilarity(embQDay, embA3)
  val isTaCorrect = simTaMatch > simTaOpp
  allCorrect = allCorrect && isTaCorrect

  println("  (2) Text Query: \"$qDay\"")
  println(String.format("      vs Matching Audio (%s): Score: %.4f", a1Anchor.name, simTaMatch))
  println(String.format("      vs Opposite Audio (%s):  Score: %.4f", a3Opposite.name, simTaOpp))
  val verdictTa = if (isTaCorrect) "✅ CORRECT (PASS)" else "❌ INCORRECT (FAIL)"
  println(String.format("      VERIFICATION: %s (Δ = +%.4f)", verdictTa, simTaMatch - simTaOpp))

  println("\n" + "-".repeat(80))
  val overallStatus = if (allCorrect) "✅ ALL 4 VERIFICATION TESTS PASSED (Text, Vision, Audio, Cross-Modal)!" else "❌ SOME TESTS FAILED!"
  println("  Overall Result [$backendLabel]: $overallStatus")
  println("-".repeat(80))

  return allCorrect
}

// ==============================================================================
// 4. Comparison Table
// ==============================================================================

fun printComparisonTable(
  cpuResults: Map<String, SingleBenchmarkResult>,
  gpuResults: Map<String, SingleBenchmarkResult>,
) {
  println("\n" + "=".repeat(80))
  println("  HARDWARE COMPARISON: CPU vs GPU (Apple Metal Acceleration)")
  println("=".repeat(80))
  println(String.format("%-10s | %-14s | %-14s | %-12s", "Modality", "CPU Latency", "GPU Latency", "Speedup"))
  println("-".repeat(80))
  for (key in listOf("text", "vision", "audio")) {
    val cpuMs = cpuResults[key]!!.avgMs
    val gpuMs = gpuResults[key]!!.avgMs
    val speedup = if (gpuMs > 0.0) cpuMs / gpuMs else 1.0
    val label = key.replaceFirstChar { it.uppercase() }
    println(String.format("%-10s | %10.2f ms   | %10.2f ms   | %8.2fx faster", label, cpuMs, gpuMs, speedup))
  }
  println("-".repeat(80))
}

// ==============================================================================
// 5. Sample & Model File Resolvers
// ==============================================================================

fun findSamplesDirectory(): File {
  val candidates = listOf(
    File("samples"),
    File("../samples"),
    File("Desktop/litert-lm-jvm-example/samples"),
    File(System.getProperty("user.home"), "Desktop/litert-lm-jvm-example/samples"),
    File(System.getProperty("user.home"), "Downloads/samples"),
  )
  for (c in candidates) {
    if (File(c, "vision").exists() && File(c, "audio").exists()) {
      return c.absoluteFile
    }
  }
  throw IllegalStateException("Could not find 'samples/' folder with vision and audio subfolders.")
}

fun resolveSampleFiles(samplesDir: File): Map<String, File> {
  val vDir = File(samplesDir, "vision")
  val aDir = File(samplesDir, "audio")
  val files = mapOf(
    "apple_anchor" to File(vDir, "apple.png"),
    "apple_similar" to File(vDir, "apple_similar.png"),
    "car_opposite" to File(vDir, "blue_car.png"),
    "audio_anchor" to File(aDir, "have_a_wonderful_day.wav"),
    "audio_similar" to File(aDir, "have_a_wonderful_day_similar.wav"),
    "audio_opposite" to File(aDir, "synthetic_tone_440hz.wav"),
  )
  for ((name, f) in files) {
    if (!f.exists()) {
      throw IllegalStateException("Missing sample file: ${f.absolutePath} ($name)")
    }
  }
  return files
}

fun resolveEmbeddingModelPath(providedPath: String?): String {
  if (!providedPath.isNullOrBlank()) {
    val direct = File(providedPath)
    if (direct.exists()) return direct.absolutePath

    val homeDir = System.getProperty("user.home")
    val homeRelative = if (providedPath.startsWith("~" + File.separator)) {
      File(homeDir, providedPath.removePrefix("~" + File.separator))
    } else {
      File(homeDir, providedPath)
    }
    if (homeRelative.exists()) return homeRelative.absolutePath
  }

  val candidates = listOf(
    File("embedding-gemma-v2.litertlm"),
  )
  for (c in candidates) {
    if (c.exists()) return c.absolutePath
  }
  throw IllegalArgumentException("Could not find embedding-gemma-v2.litertlm. Please provide path as first argument.")
}

// ==============================================================================
// 6. Main Entry Point
// ==============================================================================

fun runEmbeddingApp(args: Array<String>) {
  val rawModel = if (args.isNotEmpty()) args[0] else null
  val modelPath = resolveEmbeddingModelPath(rawModel)
  val mode = if (args.size > 1) args[1].lowercase() else "both"

  val samplesDir = findSamplesDirectory()
  val sampleFiles = resolveSampleFiles(samplesDir)

  println("=".repeat(80))
  println("  LiteRT-LM JVM Gemma Multimodal Embedding Evaluation")
  println("  Model:       $modelPath")
  println("  Samples Dir: ${samplesDir.absolutePath}")
  println("  Backend:     ${mode.uppercase()}")
  println("=".repeat(80))

  val backendsToRun = when (mode) {
    "cpu" -> listOf("cpu")
    "gpu" -> listOf("gpu")
    "npu" -> listOf("npu")
    "both", "all" -> listOf("cpu", "gpu")
    else -> {
      println("Unknown mode '$mode'. Choose from 'cpu', 'gpu', 'npu', or 'both'. Defaulting to 'both'.")
      listOf("cpu", "gpu")
    }
  }

  val benchRecords = mutableMapOf<String, Map<String, SingleBenchmarkResult>>()

  for (bName in backendsToRun) {
    val bLabel = bName.uppercase()
    val backendObj = when (bName) {
      "gpu" -> Backend.GPU()
      "cpu" -> Backend.CPU(threadCount = 4)
      "npu" -> {
        try {
          Backend.NPU()
        } catch (e: Throwable) {
          println("\n❌ NPU Backend Error: ${e.message}")
          if (System.getProperty("os.name").lowercase().contains("mac")) {
            println("ℹ️ Note: On macOS, LiteRT-LM does not currently support Apple Neural Engine (ANE) via NPU.")
            println("   Please use 'gpu' (Apple Metal), which is fully supported and delivers 2.5x - 3x acceleration.")
          }
          return
        }
      }
      else -> Backend.CPU(threadCount = 4)
    }

    val audioBackendObj = when (bName) {
      "gpu", "npu" -> Backend.CPU(threadCount = 4)
      else -> backendObj
    }

    val config = EmbeddingEngineConfig(
      modelPath = modelPath,
      backend = backendObj,
      visionBackend = backendObj,
      audioBackend = audioBackendObj,
    )

    try {
      EmbeddingEngine(config).use { engine ->
        engine.initialize()

        val singleResults = runSingleEmbeddingsSection(engine, bLabel, sampleFiles, iterations = 3)
        benchRecords[bName] = singleResults

        run3InputVerificationSection(engine, bLabel, sampleFiles)
      }
    } catch (e: Throwable) {
      println("\n❌ Engine Execution Error on $bLabel: ${e.message}")
      if (bName == "npu" && System.getProperty("os.name").lowercase().contains("mac")) {
        println("ℹ️ Note: On macOS, LiteRT-LM does not currently support Apple Neural Engine (ANE) via NPU.")
        println("   Please use 'gpu' (Apple Metal), which is fully supported and delivers 2.5x - 3x acceleration.")
      }
    }
  }

  if (benchRecords.containsKey("cpu") && benchRecords.containsKey("gpu")) {
    printComparisonTable(benchRecords["cpu"]!!, benchRecords["gpu"]!!)
  }
}

fun main(args: Array<String>) {
  runEmbeddingApp(args)
}
