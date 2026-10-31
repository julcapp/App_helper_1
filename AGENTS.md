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
Read `docs/SUBSCRIPTIONS_CRM_REQUIREMENTS.md` for the confirmed **90-day trial counted from account activation**, **950 RUB/month** subscription and **automatic monthly charges only with separate user consent**, plus CRM, Sberbank payment, registration and push requirements. Treat its explicit open questions as requiring approval. Do not reuse credentials, database or merchant IDs from other projects. No production payment integration until the merchant, fiscalization, distribution and privacy arrangements are approved.

## Engineering
- Active working branch: `ux/discovery-accessibility-2026-10`. Do not push directly to `main`.
- First milestone: build and test Android app, then stabilize message reading and accessibility.
- Preserve existing code. Use Kotlin, Jetpack Compose, Android NotificationListenerService and supported system APIs.
- Prioritize TalkBack, 200% text scaling, large controls, privacy, STT/TTS recovery, offline/error states.
- MAX, Telegram and WhatsApp integrations share a channel architecture. Notification access is not permission to send.
- Drafts never send automatically. Require an actually verified available transport, exact channel/recipient/text, user confirmation, and accurate delivery-status semantics.
- Do not claim successful build or device tests without logs.
- Report files changed, tests run, results and remaining risks.

## ESIA / Gosuslugi
Support ESIA (Gosuslugi) as an optional future identity provider only. Integration requires verified eligibility, registration, compliance and credentials. Feature flag OFF until access is approved and tested. Never ask for or store Gosuslugi passwords, SNILS or passport details without an independently approved, necessary purpose. Maintain a non-ESIA authentication path.

## Local biometric sign-in
After successful provider login, offer optional fast re-entry with system Android BiometricPrompt and appropriate device credential fallback. Use Android Keystore for secured session keys; never access or store biometric templates. Server session validity and subscription status must still be checked. Follow AUTH-BIO-01..06 in docs/TECHNICAL_SPECIFICATION.md.

## Multiple devices and one subscription
Implement user-level billing shared across up to 2–3 linked devices; design configurable max_devices (3 proposed pending final cap). New devices require secure pairing with a newly generated six-digit PIN valid for 3 minutes, displayed on an existing authenticated device, bounded attempts, single use, revocation and safe account recovery. Never treat the PIN alone as authentication. See DEV-01..11 in the technical specification.

## Referral program
Implement a configurable, versioned per-user referral program as specified in docs/TECHNICAL_SPECIFICATION.md (REF-01..11). Users share personal invite links; the inviter earns 50% off their next paid subscription period only after the invitee's first confirmed payment. No discounts on signup/trial; prevent double-awards, self-referrals, sensitive-data leaks, and unauthorized discount stacking. Terms not yet settled must be flagged for approval, not silently invented.

## Notes and product feedback
Implement the user-facing 'Мои мысли и предложения' module according to FB-01..11 in docs/TECHNICAL_SPECIFICATION.md. 'Мои мысли' is a one-way product feedback channel: a saved written or transcribed note is uploaded to CRM automatically, without a per-note consent checkbox. Clearly inform users before use, do not upload draft keystrokes, use a lawful data-processing basis, and do not implement feedback replies or processing-status messaging. 'Техническая поддержка' is a separate two-way support channel with its own records. Maintain accessibility, privacy, RBAC and human review of AI categorization.

## Updated feedback requirements
See the newer clarification in docs/TECHNICAL_SPECIFICATION.md: thought drafts and unfinished input are automatically queued for CRM synchronization, and CRM notifies an administrator only after server acknowledgment. Clear prior notice and the legally necessary separate personal-data consent remain prerequisites for rollout. Keep Technical Support separate.

## 2GIS maps and trusted people
See GEO-01..11 in docs/TECHNICAL_SPECIFICATION.md. Use 2GIS as a map/search/routing/navigation provider candidate; location permissions come from Android, sharing with family uses a separate consent-controlled App_helper_1 server module. Never invent a 2GIS family-tracking API. No hidden location monitoring or CRM access. Support TalkBack, revocation, expiring links and foreground-service requirements.

## User-selectable map providers
Users choose Google Maps, Yandex Maps or 2GIS. Persist preference per account and allow per-route override. Separate Android location, maps, routing, navigation and trusted-person location sharing. External-app navigation is a valid initial implementation but must be clearly labeled; embedded SDK integration needs licensing/API verification. See MAP-01..09 in docs/TECHNICAL_SPECIFICATION.md.

## Emergency assistance design
Review SOS-01..10 in docs/TECHNICAL_SPECIFICATION.md. Distinguish emergency system calling from trusted-person alerts, never assert delivery before acknowledgment, never guarantee rescue, preserve TalkBack and non-network fallbacks, and do not gate emergency calling on a paid subscription. This remains a design requirement; do not implement without later user instruction.

## SOS subscription exception
User approved: system emergency-service calling must remain accessible even after subscription expiry, without requiring active billing. See SOS-11..15 in docs/TECHNICAL_SPECIFICATION.md. Trusted-person onboarding/delivery details remain proposals pending approval.

## Emergency 112 two-mode concept
See SOS-16..23 in docs/TECHNICAL_SPECIFICATION.md: max 3 trusted contacts; user chooses one or all. Standard voice 112 dialer and an editable personal emergency message for a user unable to speak. Automated playback twice into a cellular emergency call is a feasibility question, not an implemented or guaranteed Android capability. Never test against live emergency dispatch without formal coordination. Separate medical/SOS data from analytics.

## Lock-screen emergency card and 112 agent feasibility
Review SOS-31..39 in docs/TECHNICAL_SPECIFICATION.md. After SOS contact notification, display persistent emergency card on lock screen only where platform permits and provide bottom close control 'Помощь оказывается — закрыть'. Full-screen intent is restricted on modern Android. AI answering detection, TTS injection and dialogue with emergency 112 operator are future feasibility research requiring authorized telecom/emergency integration; no unsupported implementation claims, no live 112 test calls without approval.

## Local-only passport and medical insurance SOS fields
See SOS-40..47 in docs/TECHNICAL_SPECIFICATION.md. Passport and OMS insurance number are strictly per-device encrypted local fields, with no CRM, server sync, backups, analytics, logs, push, or cross-device transfer. They may be shown to emergency responders when explicitly requested and only shared via an authorized, verified channel. Protect full document details from casual lockscreen exposure.

## Persistent SOS entry points
Approved: large Android home-screen SOS widget and optional persistent-notification shortcut subject to OS permissions. Opening the SOS menu does not itself send an alert; preserve accessible explicit call and trusted-contact options. See SOS-63..68 in docs/TECHNICAL_SPECIFICATION.md. Do not claim guaranteed lock-screen overlay.

## Internationalization and localized emergency dialing
See I18N-01..02, PHONE-01 and SOS-69..74 in docs/TECHNICAL_SPECIFICATION.md. Support ru/en/es/zh-Hans/ar (RTL), country-specific E.164 contact-number formatting, and emergency calling according to current network/location rather than the account/SIM country code. Russian emergency number is 112 even for a foreign +375 account currently in Russia. Never prepend +country code to 112/911; do not assume all international numbers are 11 digits. No implementation requested yet.

## Approved international phone formats and SOS language
Implement requirements I18N-SOS-01..09 in docs/TECHNICAL_SPECIFICATION.md when coding is authorized: country-aware phone mask and canonical E.164 storage; emergency number is separate and based on the device's current country/network, not the user's SIM/account code; prepare operator text in the current country's appropriate language, validated in advance with editable local preview. Keep passport/OMS strictly local. Do not claim Android can inject TTS into an emergency call.

## International subscription pricing
Approved: after one shared 90-day trial, Russian regional subscription is RUB 950/month and outside-Russia subscription is USD 19.90/month. Keep one subscription across the user's devices; recurring billing requires separate consent. Billing region cannot be determined solely from GPS/SIM/IP, and international payment integration and region transition rules remain open. See BILL-INT-01..07 in docs/TECHNICAL_SPECIFICATION.md.

## Voice interaction and billing region
See VOICE-01..14 and BILL-INT-08..09 in docs/TECHNICAL_SPECIFICATION.md. Preserve subscription currency during temporary travel and leave international payment integration undecided. Plan a safe first-launch STT/TTS/intent test flow without real communications. Do not claim real tests were performed. Codex implementation is postponed until user instruction.

## Foreign payment candidates and crypto restrictions
Foreign USD 19.90 subscription: CloudPayments and Robokassa are candidate providers, not yet selected or enabled. Owner requests future USDT/BTC acceptance; treat as feasibility/legal review only, never enable or promise cryptocurrency subscription payments until lawful merchant/jurisdiction/store rules are established. See PAY-FOREIGN-01..06 in docs/TECHNICAL_SPECIFICATION.md. Recurring payments require opt-in and server-verified status.

## Approved opt-in memory behavior
Long-term personal AI memory is OFF by default, enabled only through user choice during onboarding or later. Language, accessibility and map preferences persist independently of AI memory. Users can inspect, correct, delete and disable AI memory; medical SOS passport and OMS fields never sync. See MEM-12..17 in technical specification. Codex remains postponed.

## Voice agents to be implemented via Codex
The project owner approved using Codex to build the shared voice AI-agent platform with user-isolated profiles/memory. Implementation remains postponed pending an explicit launch instruction. Follow CODEX-VOICE-01..08 in docs/TECHNICAL_SPECIFICATION.md. Do not assert implementation/tests are complete. Keep medical SOS data local, AI memory opt-in, CRM observability metadata-only by default.

## AI agents CRM admin dashboard
See CRM-AI-01..11 in docs/TECHNICAL_SPECIFICATION.md: operational monitoring, STT/LLM/TTS provider configuration, limits, cost ledger, privacy-safe technical diagnosis, RBAC/audit, provider fallback and isolated tests. No raw conversations or memory in regular CRM logs. Implementation through Codex requires explicit later authorization.
