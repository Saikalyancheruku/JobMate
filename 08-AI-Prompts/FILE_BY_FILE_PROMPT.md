# Reusable AI coding prompt

Implement JobMate, a native Android app, following the supplied specification.

- Implement only the requested file(s) and narrow required imports/changes.
- Use Kotlin, Compose, MVVM + Repository, Room source of truth, package `com.saikalyan.jobmate`, minSdk 26.
- Do not add backend, browser extension, Chrome, React or Angular.
- Ask rather than invent unclear product behavior.
- Never request/store normal Gmail password or hardcode secrets.
- Keep resumes/screenshots local; never sync them to Sheets.
- Capture only user-consented visible screens with MediaProjection; no hidden scraping or secure-screen bypass.
- Send one separate email per recipient after explicit user confirmation.
- Include error states, accessibility and tests.
- Explain dependency/version choices and compatibility.

Inputs: architecture, feature catalogue, schema, build sequence, exact file spec, matching skeleton, current project tree and existing contracts.

Output: complete requested file, imports, interface/migration impacts, tests, compile/test commands, and any unresolved questions. Do not silently decide product requirements.
