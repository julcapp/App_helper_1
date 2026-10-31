# Codex instructions — App_helper_1

## Project
Android-first, voice-first accessibility assistant for blind, low-vision, older and motor-impaired users.
Read `docs/TECHNICAL_SPECIFICATION.md` as the consolidated baseline, then `README.md`, `docs/MESSAGE_ASSISTANT.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/UX_DISCOVERY_ACCESSIBILITY_2026-10-31.md` and `docs/CODEX_HANDOFF.md` before changes.

## Mandatory discovery skills
Use **Discovery Interview** to convert vague requests into explicit, testable technical requirements.
Use **UX Discovery Interviewer** to validate personas, real tasks, accessibility barriers, error recovery and field-test hypotheses.
If installed Codex skills matching these names are available, read their `SKILL.md` instructions and apply them. Names here are directives, NOT evidence that skills have been installed.
If unavailable, perform equivalent structured discovery and report the missing installation; do not invent outputs of user interviews.
Start from prior documented decisions, ask only unresolved, high-impact questions. Distinguish **confirmed decisions**, **hypotheses**, and **open questions**. Do not claim research with actual users until performed.
Record validated changes in technical specification/roadmap after owner approval; do not silently replace accepted requirements.

## Commercial addendum
Read `docs/SUBSCRIPTIONS_CRM_REQUIREMENTS.md` for the confirmed 3-month trial, 950 RUB/month subscription, CRM, Sberbank payment, registration and push requirements. Treat its explicit open questions as requiring approval. Do not reuse credentials, database or merchant IDs from other projects. No production payment integration until the merchant, fiscalization, distribution and privacy arrangements are approved.

## Engineering
- Active working branch: `ux/discovery-accessibility-2026-10`. Do not push directly to `main`.
- First milestone: build and test Android app, then stabilize message reading and accessibility.
- Preserve existing code. Use Kotlin, Jetpack Compose, Android NotificationListenerService and supported system APIs.
- Prioritize TalkBack, 200% text scaling, large controls, privacy, STT/TTS recovery, offline/error states.
- MAX, Telegram and WhatsApp integrations share a channel architecture. Notification access is not permission to send.
- Drafts never send automatically. Require an actually verified available transport, exact channel/recipient/text, user confirmation, and accurate delivery-status semantics.
- Do not claim successful build or device tests without logs.
- Report files changed, tests run, results and remaining risks.
