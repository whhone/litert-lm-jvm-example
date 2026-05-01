# LiteRT-LM JVM Example

![](./demo.gif)

```shell
# Build and run (Linux/macOS)
./gradlew installDist && app/build/install/app/bin/app /path/to/model.litertlm

# Build and run (Windows PowerShell)
.\gradlew.bat installDist; .\app\build\install\app\bin\app.bat C:\path\to\model.litertlm
```

Available `.litertlm` models are on the [HuggingFace LiteRT
Community](https://huggingface.co/litert-community). The above animation was
using the [Gemma4 e2b](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm).

Note: `./gradlew run` mixes the Gradle's output with the app's output and breaks
the user experience. We need to build and run separately like the above command.
