# AvaChat 🤖

A premium, production-ready AI chat application for Android — built with Kotlin and Jetpack Compose, fully compatible with any OpenAI-compatible API provider.

## Features

- 💬 **AI Chat** — smooth streaming (SSE) responses, stop generation, regenerate, retry
- 🔧 **Bring your own provider** — configurable API Key, Base URL, and Model (works with OpenAI, OpenRouter, Groq, Ollama, LM Studio, Azure OpenAI-compatible endpoints, and any OpenAI-compatible server)
- 🔐 **Security-first** — API key stored locally via DataStore, masked in UI, never logged
- 🌓 **Light / Dark / System themes** + Material 3 dynamic colors
- 🌍 **Full RTL + Persian & English** — automatic layout mirroring, per-app language selection
- 📝 **Markdown rendering** — headings, lists, quotes, links, code blocks with copy button and horizontal scrolling
- 🗂 **Conversation management** — history, search, pin, favorite, rename, delete, auto-titling from the first user message
- 📴 **Offline-aware** — connectivity detection, offline banner, unsent text preserved
- 📱 **Responsive** — edge-to-edge UI, adaptive to phones/tablets/foldables, landscape-safe, font-scale friendly
- ♿ **Accessible** — content descriptions, 44dp touch targets, screen-reader friendly
- ⚡ **Fast & light** — no DI framework, minimal dependencies, streaming without blocking the main thread

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture (data / domain / presentation) |
| Async | Coroutines + StateFlow (UDF) |
| Network | OkHttp + Retrofit + kotlinx.serialization (SSE streaming on OkHttp) |
| Persistence | Room (conversations/messages), DataStore (settings) |
| DI | Manual (lightweight — no framework) |
| Tests | JUnit4, MockWebServer, coroutines-test |

## Building

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK (minified + resource-shrunk)
./gradlew testDebugUnitTest # unit tests
```

APK output: `app/build/outputs/apk/<variant>/`

## Configuring your provider

1. Open **AvaChat** → tap the ⚙️ **Settings** icon (top-right menu)
2. Fill in:
   - **API Key** — your secret key (stored on-device only)
   - **Base URL** — e.g. `https://api.openai.com/v1` (a trailing `/chat/completions` is also accepted and normalized automatically)
   - **Model** — e.g. `gpt-4o-mini`
3. Adjust Temperature / Max tokens / Streaming as you like
4. Tap **Save**, then **Test connection** to verify

The app POSTs to `{BASE_URL}/chat/completions` using the standard OpenAI wire format, so any compatible provider works without code changes.

## Project layout

```
app/src/main/java/com/avachat/app/
├── data/
│   ├── local/        # Room DB (conversations, messages)
│   ├── remote/       # OkHttp/Retrofit client, SSE streaming, URL normalizer
│   ├── repository/   # ChatRepository (single source of truth)
│   └── settings/     # DataStore-backed settings repository
├── domain/
│   ├── error/        # Centralized AppError taxonomy (HTTP/IO → human messages)
│   ├── model/        # Message, Conversation, Settings
│   └── repository/   # AiChatRepository (streaming + non-streaming use cases)
├── presentation/     # ViewModels (chat, settings, history) — UDF state
└── ui/
    ├── chat/         # Chat screen, bubbles, composer
    ├── components/   # Typing indicator, sliders, switches
    ├── history/      # Conversation list
    ├── markdown/     # Lightweight markdown renderer
    ├── settings/     # Settings screen
    └── theme/        # Centralized Material 3 theme (light/dark/dynamic)
```

## License

MIT
