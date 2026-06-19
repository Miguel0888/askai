# AskAI first Ollama slice

Goal: reuse the tested DirectML Workbench download path and add the smallest possible Ollama import/chat flow.

## Preserved download path

The copied download classes still live under `com.aresstack.windirectml.workbench.download` to keep the proven implementation stable for this first extraction. The UI calls `ModelDownloader.downloadFromManifest(...)` with the current `ProxyConfiguration` from the AskAI model, just like the original workbench path.

The **Proxy** tab is present and is adapted from the original DirectML Workbench `ProxyPanel`. The default remains the PAC/PowerShell path used by the old workbench model.

## Provider boundary

The application name is provider-neutral. Ollama-specific code is intentionally contained in Ollama-named packages/classes so llama.cpp or an OpenAI-compatible provider can be added later without renaming the product.

## Recommended first model

Use `google/gemma-3-270m-it` for the first Safetensors import attempt because it is small and belongs to an Ollama-supported family. Phi-3 is included as a larger fallback. Qwen is present as an experimental candidate and should become a GGUF path later.


## AskAI settings location

Application settings, download URL overrides, and Hugging Face access tokens are stored below `%APPDATA%\.askai`. The copied download store keeps its proven behavior, but its default path helpers now point to AskAI paths instead of the old `.directml` directory.
