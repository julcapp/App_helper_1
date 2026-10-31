# Inbox Reading Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Deliver the first local Android inbox and explicit reading flow in the existing application.
**Architecture:** Notification bridge feeds a bounded queue. Local reading controls consume immutable selected snapshots; optional accessibility button and Android share supply visible/selected text. No automatic cloud transfer or message sending.
**Tech Stack:** Existing Kotlin, Compose, NotificationListenerService, AccessibilityService, Android TTS, JUnit4.
**Spec:** docs/superpowers/specs/2026-10-31-inbox-reading-design.md

## Global Constraints
- Preserve minSdk 26, compile/target 37 and existing active branch; never merge main.
- No unsolicited content speech; no message content in CRM, journal or automatic AI memory.
- No sending, account tokens, production API calls, billing or SOS changes.
- First-stage notifications and screen text are not full mailbox/chat integrations.

## Review Focus
- Same notification key changes text: replace pending preview, preserve current spoken snapshot.
- More than 50 events: retain selected item and bound memory.
- Later/paused/cancelled reading: preserve messages and suppress stale completion callbacks.
- Hidden text and browser identity: never fabricate full content or claim browser is VK.
- Source disabled / locked screen / password nodes: stop access and exclude private content.

### Task 1: Reliable inbox queue and notification intake
**Files:** messages/MessageQueue.kt, notifications/NotificationEventStore.kt, notifications/NotificationEvent.kt, notifications/AppNotificationListenerService.kt, channels/MessageChannelRegistry.kt, notifications/NotificationReadingPreferences.kt, corresponding JUnit tests.
**Interfaces:** consume NotificationEvent; produce receive(event), defer(id), dismiss(id), select(id), restoreDeferred(), withoutPackage(packageName). Store upserts by event id and notifies only on semantic changes. MessageReadingText.original(event) labels notification preview.
- [x] Add failing tests for changed same-key preview, duplicate silence, selected retention at 51 items, defer/restore, source removal and explicit source labels.
- [x] Run JUnit and inspect expected failures; implement reducers and bridge; rerun suite.
- [x] Add MessagingStyle extraction, source toggles and metadata-only notification journal; compile.

### Task 2: Explicit voice and touch reading
**Files:** dialog/VoiceCommandRouter.kt, MainActivity.kt, voice/AndroidVoiceAssistant.kt; new command tests.
**Interfaces:** READ_ALL/NEXT/PAUSE/SENDER/SLOWER/READ_NEW commands; queue selected from Task 1. readLatestNotification uses labelled preview; speak chain advances only on TTS completion with cancellation generation guard.
- [x] Test commands and negation before implementing exact matching for reading controls.
- [x] Implement accessible queue controls, source selection, later, summary and locally completed state; disable automatic incoming speech.
- [x] Compile/unit/lint. Keep original selected snapshot stable while new previews arrive.

### Task 3: Explicit web/open-screen text reading and verification
**Files:** screen/ScreenTextCollector.kt, screen/ScreenReaderService.kt, res/xml/screen_reader_service.xml, AndroidManifest.xml, MainActivity.kt; collector tests; docs.
**Interfaces:** ScreenTextCollector collects user-visible strings under limits; Accessibility button reads current external window only, skips password and own UI; share/process-text supplies bounded local events.
- [x] Test password/duplicate/empty/size limits; implement collector and Android service. No background snapshot collection.
- [x] Add share/process-text intents and clear labels; update specification, roadmap and manual checklist.
- [x] Run full Android build/unit/lint; record exact output and physical-device checks still pending. Review diff and commit. Publish active working branch only after authorization for the public destination.

## Publication status
Local implementation and checks completed. Automated approval review rejected the attempted public GitHub push because implementation approval did not explicitly authorize public disclosure of code and documentation. The owner subsequently explicitly authorized public publication of code and documentation in the working branch on 31.10.2026. Publication is carried out through the connected GitHub account; no merge into main is authorized or performed.
