# Verification: askai-ui-clickdummy-listener-against-currentzip.patch

Basis: `/mnt/data/askai-master(2).zip`, entpacktes Root `askai-master/`.

Patch:

- `src/main/java/com/aresstack/askai/service/FeatureAction.java` neu
- `src/main/java/com/aresstack/askai/service/FeatureActionService.java` erweitert
- `src/main/java/com/aresstack/askai/service/DummyFeatureActionService.java` erweitert
- `src/main/java/com/aresstack/askai/ui/OllamaActionsPanel.java` umgebaut

Scope:

- UI/Klickdummy/Listener-Service-Schicht für neue Actions
- keine Ollama-API-Implementierung
- keine Import-/Blob-/Chat-/Model-API-Änderung
- keine Buildscript-Änderung

Prüfung:

```bash
cd /mnt/data/patch_check2
git init -q
git apply --check /mnt/data/askai-ui-clickdummy-listener-against-currentzip.patch
```

Ergebnis: `git apply --check` erfolgreich.

Build-Hinweis:

`./gradlew compileJava` konnte im Sandbox-Container nicht laufen, weil der Gradle Wrapper `services.gradle.org` erreichen wollte und DNS/Internet im Container nicht verfügbar ist (`UnknownHostException: services.gradle.org`).
