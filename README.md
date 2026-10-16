# LiteRT-LM JVM Example

This repository demonstrates how to use the Google LiteRT-LM JVM library (`com.google.ai.edge.litertlm:litertlm-jvm`) in Kotlin for:
1. **Interactive LLM Chat** ([`App.kt`](app/src/main/kotlin/org/example/App.kt)): Text generation with LLMs such as Gemma 4.
2. **Multimodal Embeddings & Benchmarking** ([`EmbeddingApp.kt`](app/src/main/kotlin/org/example/EmbeddingApp.kt)): Multimodal embeddings across Text, Vision, and Audio, with similarity verification and CPU vs. GPU (Apple Metal) performance benchmarks.

---

## 🛠️ Build

```shell
# Linux / macOS
./gradlew installDist

# Windows PowerShell
.\gradlew.bat installDist
```

> **Note**: Avoid `./gradlew run` as it interleaves Gradle daemon output with the interactive terminal. Building with `installDist` and running the installed binary directly provides the best experience.

---

## 💬 1. Interactive LLM Chat (`App.kt`)

Interactive terminal conversation with text-generation `.litertlm` models.

```shell
# Linux / macOS
app/build/install/app/bin/app /path/to/model.litertlm

# Windows
.\app\build\install\app\bin\app.bat C:\path\to\model.litertlm
```

* **Recommended Models**: [Gemma-4-E2B-it](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) from the [HuggingFace LiteRT Community](https://huggingface.co/litert-community).

---

## 🧠 2. Multimodal Embedding & Benchmark App (`EmbeddingApp.kt`)

Evaluates multimodal embeddings (Text, Vision, and Audio) using `EmbeddingEngine`:
- **Single Vector Extraction**: Generates 768-dim embeddings for text, images, and audio with vector preview.
- **Similarity Verification (2 Same Kind vs 1 Opposite)**:
  - **Text**: `Apples are delicious...` vs `A crisp fresh red apple...` > `Quantum computers...`
  - **Vision**: `apple.png` vs `apple_similar.png` > `blue_car.png`
  - **Audio**: Speech anchor vs variant > 440Hz pure sine tone
  - **Cross-Modal**: Text query vs matching image/audio vs opposite
- **Hardware Acceleration Benchmark**: Evaluates latency on **CPU** vs. **GPU (Apple Metal on macOS)** with speedup metrics.

```shell
# Linux / macOS (runs CPU and GPU benchmarks by default)
app/build/install/app/bin/embed /path/to/embedding-gemma-v2.litertlm all

# Specific backend: 'cpu' or 'gpu'
app/build/install/app/bin/embed /path/to/embedding-gemma-v2.litertlm gpu

# Windows
.\app\build\install\app\bin\embed.bat C:\path\to\embedding-gemma-v2.litertlm all
```

* **Recommended Model**: `embedding-gemma-v2.litertlm`
* **Sample Assets**: Included in the [`samples/`](samples/) directory:
  - `samples/vision/` (`apple.png`, `apple_similar.png`, `blue_car.png`)
  - `samples/audio/` (`have_a_wonderful_day.wav`, `have_a_wonderful_day_similar.wav`, `synthetic_tone_440hz.wav`)

---

## 📂 Project Structure

```
├── app/
│   ├── build.gradle.kts       # Gradle config (LiteRT-LM 0.18.0)
│   └── src/main/kotlin/org/example/
│       ├── App.kt             # Interactive LLM Chat
│       └── EmbeddingApp.kt    # Multimodal Embedding & Benchmark
├── samples/
│   ├── vision/                # Test images
│   └── audio/                 # Test audio clips (.wav)
├── build.gradle.kts
└── settings.gradle.kts
```
