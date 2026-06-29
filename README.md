# AskAI

AskAI ist eine Desktop-App für providerbasierten lokalen und Remote-AI-Betrieb. Der erste Provider ist Ollama: Die App sucht Modelle direkt auf dem Hugging Face Hub, lädt die relevanten Dateien herunter, lädt sie als Ollama-Blobs auf einen (lokalen oder Remote-)Ollama-Server hoch, erstellt daraus ein Ollama-Modell und chattet anschließend direkt damit.

Der Ablauf ist durchgängig such- und repo-getrieben — es gibt keine handgepflegten Datei-URLs mehr:

**Hugging-Face-Suche → Repo-Auswahl → automatische Dateiliste → Download via `huggingface4j` → optionaler Ollama-Import.**

## Architektur

Die UI (Swing) spricht nur mit einer Service-Schicht, nie direkt mit HTTP/JSON oder einer Provider-Bibliothek:

- **Ollama** läuft über [`ollama4j`](https://github.com/ollama4j/ollama4j). `AskAiOllamaClient` ist der einzige Adapter; `OllamaService` ist die UI-Grenze (Version, Modelle listen/`ps`, Chat/Streaming, Generate, Embeddings, Pull, Unload, Delete, Model-Info).
- **Hugging Face** läuft über `huggingface4j`. `HuggingFaceModelLoader` ist der einzige Adapter; `ModelDownloadService` ist die UI-Grenze (Suche, Manifest aus der Repo-Dateiliste, Download mit Fortschritt/Resume).
- **Modell-Import** (Blob-Upload `/api/blobs` + `/api/create`) läuft über den isolierten `AskAiOllamaImportClient`, weil `ollama4j` diese Endpunkte nicht passend abdeckt. Digest-Präfixe (`sha256:`) werden genau einmal gesetzt.

## Start

```powershell
.\gradlew.bat run
```

oder als Fat-Jar:

```powershell
.\gradlew.bat shadowJar
java -jar build\libs\askai-all.jar
```

## Tabs

- **Chat** — vollwertiges Chatfenster mit Mehrfach-Turn-Verlauf, Live-Streaming, Modellauswahl, System-Prompt, `keep_alive` und Token/s-Metrik. Enter sendet, Shift+Enter macht eine neue Zeile.
- **Models** — installierte und laufende Modelle (`/api/tags`, `/api/ps`) als Karten, inklusive Löschen.
- **Actions** — Live-Operationen gegen den Server: Server-Health, laufende Modelle, Model-Details, Pull (mit Fortschritt), Unload, Generate, Embeddings.
- **Configuration → Install** — Hugging-Face-Suche und Modellinstallation (siehe unten).
- **Configuration → Connections** — Ollama-Base-URL setzen und testen, z. B. `http://10.126.26.41:11434`.
- **Configuration → Network** — Proxy-Auflösung (`win-proxy-java`).
- **Help → About** — Kurzbeschreibung.

## Erster Ablauf

1. Unter **Connections** die Ollama-Base-URL setzen und mit *Test Connection* prüfen.
2. Unter **Install** im Suchfeld z. B. `qwen2.5 coder 0.5b` eingeben und **Search Hugging Face** klicken.
3. In der Ergebnisliste ein echtes Repo wählen, etwa `Qwen/Qwen2.5-Coder-0.5B-Instruct`.
4. Bei Bedarf anpassen:
   - **Revision / branch** (Standard `main`),
   - **HF token** (nur für gated/protected Repos),
   - **Install as** (Ollama-Zielname, z. B. `qwen2.5-coder-0.5b:latest`),
   - **Ollama profile** (`Auto` / `Qwen ChatML` / `Default`),
   - optional **Force download**.
5. **Download** lädt nur die Dateien, oder **Download and install** lädt und erstellt direkt das Ollama-Modell.
6. Im Tab **Chat** die Modelle aktualisieren und mit dem neuen Modell chatten.

Die relevante Dateiliste wird automatisch aus der Repo-Struktur abgeleitet (Gewichte, Config, Tokenizer-Dateien, Chat-Template). `special_tokens_map.json` ist optional und bricht die Installation nicht ab, wenn das Repo es nicht veröffentlicht.

## Warum es ein Ollama-Profil gibt

Ollama braucht für korrektes Chat-Verhalten Template/System/Stop-Tokens, die sich nicht zuverlässig aus einem Hugging-Face-Repo ableiten lassen. Das **Ollama profile** ist deshalb eine Import-/Runtime-Einstellung, keine Download-Konfiguration. Für Qwen muss z. B. `Qwen ChatML` wählbar bleiben; `Auto` wählt es anhand des Repo-Namens automatisch.

## Speicherorte

AskAI schreibt eigene Einstellungen unter `%APPDATA%\.askai`:

- `settings.properties` für Ollama-URL, Modellordner, Quantisierung und `keep_alive`
- `model` als Standardordner für heruntergeladene Modelle

Der HF-Token für gated Modelle wird im **Install**-Tab pro Sitzung eingegeben (kein persistenter URL-/Access-Store mehr).

## Hinweise

- Alle Hugging-Face-Operationen (Suche, Dateiliste, Download) laufen über die im **Network**-Tab konfigurierte Proxy-Einstellung. Manueller Proxy nutzt einen festen `java.net.Proxy`, PAC-/Windows-Modi einen `ProxySelector` über `win-proxy-java`; `Disabled` verbindet direkt. Die Einstellung wirkt sofort — es gibt keinen separaten „Apply to downloads"-Schritt mehr.
- `huggingface4j` und `ollama4j` werden ins Fat-Jar gebündelt; es ist keine externe CLI nötig.
