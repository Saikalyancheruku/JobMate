# Proposed Room schema

Use UUID string primary keys, `createdAt`/`updatedAt` timestamps, explicit indexes, foreign keys and migrations. Room is source of truth.

- **JobEntity:** id, title*, companyName*, location, description, skills, experienceMin/Max, salaryText, employmentType, postedAt, source, sourceUrl, status, appliedAt, followUpAt, notes, captureMethod, fingerprint*, timestamps.
- **ContactEntity:** id, fullName, normalizedEmail*, companyName, domain, designation, notes, timestamps. Unique normalized email unless product owner changes policy.
- **JobContactEntity:** jobId + contactId composite key, matchMethod, confidence, isConfirmed, createdAt.
- **ResumeEntity:** id, displayName, originalFileName, app-private relative path, MIME, sizeBytes, SHA-256, importedAt.
- **EmailTemplateEntity:** id, name, subjectTemplate, bodyTemplate, type, updatedAt.
- **OutreachEntity:** id, optional jobId, contactId, templateId, resumeId, renderedSubject/body, status, sentAt, timestamps.
- **EmailAttemptEntity:** id, outreachId, attemptNumber, start/end, outcome SENT/FAILED/UNKNOWN, sanitizedErrorCode, messageId, userConfirmedRetry.
- **SyncQueueEntity:** id, entityType/id, operation, payloadVersion, queuedAt, attemptCount, lastErrorCode, state, remoteUpdatedAt.
- **SyncConflictEntity:** id, entityType/id, local/remote snapshots (no secrets), detectedAt, resolution.

Index job company/status/fingerprint; contact email/domain; outreach status/time; sync state/time. Never store tokens, passwords, PDF bytes or screenshots in Room. Resume paths must resolve inside app-private storage. Use explicit migrations, never destructive production migration.
