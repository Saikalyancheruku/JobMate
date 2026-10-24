# Commented skeleton — `GmailSmtpClient.kt`

```kotlin
// TODO: Gmail SMTP App Password only. No OAuth2, XOAUTH2, or Gmail REST API.
// Never request or store a normal Gmail password.
// smtp.gmail.com:587 STARTTLS + certificate validation + timeout; one To recipient only.
// Return SENT / FAILED / UNKNOWN; redact App Password and message content.
class GmailSmtpClient {
    // TODO: suspend fun send(message: OutgoingMessage, auth: GmailAppPasswordAuth): SendResult
}
```
