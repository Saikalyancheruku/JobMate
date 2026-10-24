# Gmail authentication and sending

v1 uses Gmail SMTP with App Password authentication only.

- SMTP host: `smtp.gmail.com`
- Port: 587
- Encryption: STARTTLS with certificate validation and timeouts
- Authentication: Gmail App Password (never a normal Gmail password)

Do not implement OAuth2, XOAUTH2, Google AuthorizationClient, Gmail REST API, OAuth refresh tokens, OAuth client IDs, or OAuth consent screens for sending. Do not silently replace SMTP with the Gmail API. Do not embed confidential client secrets in the APK.

App Password availability depends on Google account eligibility and Workspace administrator policies. Do not promise that every account can generate an App Password. Label the credential field “Gmail App Password”. Explain that the user must generate it in Google Account settings. Show a useful error when authentication is rejected or the account cannot use this method.

Handle disabled SMTP, quota, rate limits, network failures and uncertain delivery. If the connection drops after the message may have been submitted, mark outcome UNKNOWN and ask the user to verify before retry. Never automatically retry UNKNOWN attempts.

For every recipient, build a separate MIME message with exactly one To recipient, no other recipients in To/CC/BCC, personalized subject/body and selected PDF only. Persist separate outreach/attempt records. No unattended sending. No WorkManager email send.
