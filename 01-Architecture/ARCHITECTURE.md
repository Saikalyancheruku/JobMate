# Architecture

## Layers
- `data/local`: Room database, entities, DAOs, converters.
- `data/remote`: Gmail SMTP (App Password authentication), optional Google Sheets. No OAuth2, XOAUTH2, or Gmail REST API for v1 email.
- `data/repository`: repository implementations.
- `domain/model`, `domain/repository`, `domain/usecase`: platform-independent business contracts.
- `ui`: Compose screens, navigation, theme and reusable components.
- `service`: MediaProjection foreground service.
- `worker`: WorkManager sync.
- `di`: dependency injection.
- `util`: validation, dispatchers, redacted logging, typed results.

Flow: Compose UI -> ViewModel -> UseCase -> Repository -> Room/remote source. Room is source of truth. Save locally first; remote failure must not lose local data.

## Engineering rules
- Immutable UI state via StateFlow; use one-off events for navigation/messages.
- No SQL, SMTP, OCR parsing or network calls in Composables.
- Repository interfaces return domain models, not Room entities.
- Use transactions for multi-record operations.
- Inject dispatchers and clock for testability.
- Handle loading, empty, partial-success and error states.
- Never log credentials, tokens, full email bodies, resume paths or captured screen content.
- Use Android Keystore-backed AES-256-GCM storage for the Gmail App Password ciphertext (not Room); never store a normal Gmail password.
- Keep PDFs in app-private storage; use Storage Access Framework for import.
- Ask for only permissions needed. MediaProjection requires explicit user consent and a visible active session.
- Use Material 3, accessibility labels, large touch targets and light/dark themes.
- Choose one DI approach (Hilt recommended, subject to current compatibility); do not mix frameworks.
