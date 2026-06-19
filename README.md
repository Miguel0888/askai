# AskAI

AskAI ist eine Desktop-App für providerbasierten lokalen und Remote-AI-Betrieb. Der erste Provider ist Ollama: Die App kann Hugging-Face-Safetensors-Modelle lokal herunterladen, Dateien als Ollama-Blobs auf einen Remote-Ollama-Server hochladen, daraus ein Ollama-Modell erstellen und anschließend direkt damit chatten.

Der Download-Kern wurde aus der bestehenden DirectML Workbench übernommen und bleibt für den Download-Pfad bewusst kompatibel:

- `ModelDownloader`
- `ModelDownloadManifest`
- `ModelDownloadUrls`
- URL-/Access-Dialoge
- Download-Override-Store
- `win-proxy-java`-basierte Proxy-Auflösung
- der ursprüngliche **Proxy**-Tab für Download-Proxy-Konfiguration

## Start

```powershell
.\gradlew.bat run
```

oder als Fat-Jar:

```powershell
.\gradlew.bat shadowJar
java -jar build\libs\askai-all.jar
```

## Erster Ablauf

1. Im Tab **Config** Ollama-Base-URL setzen, zum Beispiel `http://10.126.26.41:11434`.
2. Im Tab **Proxy** den Download-Proxy wie in der DirectML Workbench konfigurieren und testen.
3. Im Tab **Download & Import** `Gemma 3 270M IT SafeTensors` auswählen.
4. Gated Access konfigurieren, falls Hugging Face ein Token verlangt.
5. Download starten.
6. `Full Spike: Download -> Upload -> Create` ausführen.
7. Im Tab **Chat** Modelle aktualisieren und mit dem neu erstellten Modell chatten.

## Speicherorte

AskAI schreibt eigene Einstellungen nicht in `.directml`, sondern unter `%APPDATA%\.askai`:

- `settings.properties` für Ollama-URL, Modellordner, Quantisierung und `keep_alive`
- `download-overrides.json` für URL-Overrides und Hugging-Face-Tokens
- `model` als Standardordner für heruntergeladene Modelle

Der HF-Token für gated Modelle wie Gemma wird über **Download & Import → HF Access** gespeichert.

## Hinweise

Qwen-Safetensors ist absichtlich nur als experimenteller Eintrag enthalten. Die offizielle Ollama-Doku nennt für direkten Safetensors-Build derzeit nicht Qwen, sondern unter anderem Gemma und Phi3. Qwen sollte später über GGUF angebunden werden.
