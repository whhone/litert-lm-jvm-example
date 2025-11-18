# LiteRT-LM JVM Example

![](./demo.gif)

```shell
# Build and run
./gradlew installDist && app/build/install/app/bin/app /path/to/model.litertlm
```

Available `.litertlm` models are on the [HuggingFace LiteRT
Community](https://huggingface.co/litert-community). The above animation was
using the [Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT).

Note: `./gradlew run` mixes the Gradle's output with the app's output and breaks
the user experience. We need to build and run separately like the above command.
