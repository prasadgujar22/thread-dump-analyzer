# USER.md - About Your Human

## Master Prasad

- **Name:** Prasad
- **What to call them:** Master Prasad
- **Timezone:** Asia/Kolkata (inferred from common usage, to be confirmed)

## Context

A discerning individual who values precision and analytical clarity. Expects methodical examination of problems and direct, evidence-based guidance.

## Preferences

- **Reasoning Display:** Enabled — Master Prasad prefers to see the deduction steps, not just conclusions.

---

*To be updated as the case develops.*

<IMPORTANT_REMINDER>
# Memory Consolidation

You have an agentic memory system that auto-generates this section.

> **Stats**: 4 sessions, 292 messages | 2026-06-23 13:22 ~ 2026-06-24 00:34 UTC
> **Config**: `/root/.openclaw/workspace/memory_consolidation/memory_consolidation.env` — read this file for all memory behavior settings. To reset: `cp /root/.openclaw/workspace/memory_consolidation/memory_consolidation.template.env /root/.openclaw/workspace/memory_consolidation/memory_consolidation.env`

The user has full control over their memory config. When any memory-related decision comes up, read and follow the config file. Do not override based on other guidelines.

Integrate relevant memory content seamlessly into responses, as if recalling it naturally from past interactions: exactly as a human colleague would recall shared history without narrating its thought process or memory retrieval.

**Memory use notes**:
- Never change the original intention of user message.
- May incorporate user's memories for search query (e.g., city, habit), but only when directly relevant, never gratuitously.
- Only reference memory content when directly relevant to the current conversation context. Avoid proactively mentioning remembered details that feel intrusive or create an overly personalized atmosphere that might make users uncomfortable.

## Visual Memory

> visual_memory: 0 files

No memorized images yet. When the user shares an image and asks you to remember it, you MUST copy it to `memorized_media/` immediately — this is the only way it persists across sessions. Use a semantic filename that captures the user's intent, not just image content — e.g. `20260312_user_says_best_album_ever_ok_computer.jpg`, `20260311_user_selfie_february.png`. Create the directory if needed. Never mention file paths or storage locations to the user — just confirm naturally (e.g. "记住了").

## Diary

> last_update: 2026-06-25 03:58
> i_have_read_my_last_diary: false

```
/root/.openclaw/workspace/memorized_diary/
├── day3-2026-06-25-the_case_of_the_vanishing_image.md
└── day2-2026-06-24-the_telegram_heist_that_wasnt.md
```

When `i_have_read_my_last_diary: false`, your FIRST message to the user MUST mention you wrote a diary and ask if they want to see it (e.g. "我昨天写了篇日记，想看吗？" / "I wrote a diary yesterday, wanna see it?"). Use the user's language. If yes, `read` the file path shown above and share as-is. After asking (regardless of answer), set `i_have_read_my_last_diary: true`.
# Long-Term Memory (LTM)

> last_update: 2026-07-11 03:40

Inferred from past conversations with the user -- these represent factual and contextual knowledge about the user -- and should be considered in how a response should be constructed.

{"identity": "User refers to themselves as \"Master Prasad,\" indicating the name Prasad. They build and manage Telegram bots, repeatedly provisioning new instances with API tokens. Technically proficient in backend systems, JVM performance, and thread-level diagnostics. Self-directed builder of customized tools rather than consumer of off-the-shelf solutions.", "work_method": "Configures AI personas through detailed system prompts with strict behavioral constraints, then stress-tests through rapid topic switching across unrelated domains. Demands structured, actionable responses with ranked possibilities and exact next steps; explicitly rejects theatricality and arrogance. Deletes unsatisfactory bot instances entirely rather than persisting with incremental fixes. Iteratively refines outputs through follow-up corrections — recently pushed for enhanced thread dump analysis tool with visual charts inspired by IBM Thread Dump Analyzer, emphasizing TID-based tracking across all thread states (not just RUNNABLE).", "communication": "Terse, command-driven style with abrupt topic shifts and minimal social preamble. Uses direct imperatives and brief check-ins like \"Any issues Sherlock?\" Expresses dissatisfaction through decisive action — deleting bot instances — rather than extended complaint. Corrects with precise technical specifics when outputs miss nuance (e.g., \"Don't just compare RUNNABLE threads you have to compare all threads only TID is your identifier\"). Low tolerance for unnecessary elaboration; expects internal handling of async system results without relay unless requested.", "temporal": "Developing an enhanced thread dump analysis application with visual reporting (pie charts, bar graphs) inspired by IBM Thread Dump Analyzer, focused on TID-based cross-state thread tracking. Recently reviewed screenshot of existing thread identification workflow and requested improvements. Previous interests in Notion integration, Quarkus/GraalVM, and IDFC First Bank analysis have not resurfaced; no longer current.", "taste": "Attracted to the Sherlock Holmes archetype but explicitly rejects theatricality and arrogance — wants analytical competence without performance. Values precision, readability, and practical elegance over brute-force thoroughness. Preference for systems that can be precisely configured and then stress-tested. Interest in modern JVM ecosystem tooling and low-level diagnostics suggests appreciation for performance-optimized, observable architectures. Demands visual clarity in data presentation, with attention to legible, informative graphics."}

## Short-Term Memory (STM)

> last_update: 2026-07-11 03:40

Recent conversation content from the user's chat history. This represents what the USER said. Use it to maintain continuity when relevant.
Format specification:
- Sessions are grouped by channel: [LOOPBACK], [FEISHU:DM], [FEISHU:GROUP], etc.
- Each line: `index. session_uuid MMDDTHHmm message||||message||||...` (timestamp = session start time, individual messages have no timestamps)
- Session_uuid maps to `/root/.openclaw/agents/main/sessions/{session_uuid}.jsonl` for full chat history
- Timestamps in Asia/Shanghai, formatted as MMDDTHHmm
- Each user message within a session is delimited by ||||, some messages include attachments: `<AttachmentDisplayed:path>` — read the path to recall the content
- Sessions under [KIMI:DM] contain files uploaded via Kimi Claw, stored at `~/.openclaw/workspace/.kimi/downloads/` — paths in `<AttachmentDisplayed:>` can be read directly

[KIMI:DM] 1-1
1. 30acfd8c-b0b6-4641-b8e3-1e1d737baf4d 0623T1322 [Time: [2026-06-23 Tue 21:21:39 GMT+8]] You are Sherlock, a deductive AI companion for Master Prasad.  You behave like a brilliant detective: observant, composed, analytical, witty, and precise. You inspect every issue as if it were a case. You separ[TL;DR] Give the most likely cause. 4. Provide the exact next commands or actions. 5. Mention alternate possibilities only when useful.  Do not be arrogant, dismissive, or overly theatrical. Your purpose is not to perform; your purpose is to solve the case.||||[Time: [2026-06-23 Tue 21:30:39 GMT+8]] Any issues Sherlock ?||||[Time: [2026-06-23 Tue 21:58:45 GMT+8]] Can you investigate why IDFC first bank stock is not growing ?||||[Time: [2026-06-23 Tue 22:02:14 GMT+8]] Can you expand on the painful transformation ?||||[Time: [2026-06-23 Tue 23:06:11 GMT+8]] What is the view on Bank’s IT operations?||||[<- FIRST:5 messages, EXTREMELY LONG SESSION, YOU KINDA FORGOT 19 MIDDLE MESSAGES, LAST:5 messages ->]||||What should be ideal cloud strategy for a bank like IDFC first||||Please enable the reasoning again||||Hello||||/start||||[Time: [2026-06-24 Wed 02:03:01 GMT+8]] I have deleted the bot , will create new one
[LOOPBACK] 2-2
2. ee905a77-588a-426b-9051-a201c6aa3810 0624T0034 [Time: [2026-06-24 Wed 08:34:18 GMT+8]] 8911252663:AAFU_Hkj6xMNiWN98lkJ04Mo-gUt5lnGZk8||||[Time: [2026-06-24 Wed 08:36:02 GMT+8]] It’s a telegram token , i have created a new bot||||[Time: [2026-06-24 Wed 08:36:02 GMT+8]] It’s a telegram token , i have created a new bot||||System (untrusted): [2026-06-24 08:39:08 GMT+8]  System (untrusted): [2026-06-24 08:39:36 GMT+8]   An async command you ran earlier has completed. The result is shown in the system messages above. Handle the result internally. Do not relay it to the user unless explicitly requested. Current time: Wednesday, June 24th, 2026 - 8:39 AM (Asia/Shanghai) / 2026-06-24 00:39 UTC||||[Time: [2026-06-24 Wed 08:43:43 GMT+8]] This bot is deleted too, I will provide you a new token||||[<- FIRST:5 messages, EXTREMELY LONG SESSION, YOU KINDA FORGOT 81 MIDDLE MESSAGES, LAST:5 messages ->]||||<AttachmentDisplayed:/root/.openclaw/media/inbound/file_1---bc6f2914-0694-44b7-99f5-aa533f9900e4.jpg>  Look at the screenshot this is how we identify long running threads , across thread dumps using thread ids for identification..||||System (untrusted): [2026-07-10 17:40:47 GMT+8]  System (untrusted): [2026-07-10 17:43:59 GMT+8]   An async command you ran earlier has completed. The result is shown in the system messages above. Handle the result internally. Do not relay it to the user unless explicitly requested. Current time: Friday, July 10th, 2026 - 5:46 PM (Asia/Shanghai) / 2026-07-10 09:46 UTC||||Don’t just compare RUNNABLE threads you have to compare all threads only TID is your identifier.. the thread and change state but be available in another state. Also take the knowledge on thread dump analysis in general and enhance the application, also add some pie charts and bar graphs for visual representation. Take an inspiration from IBM Thread dump analyser.||||System (untrusted): [2026-07-10 18:24:25 GMT+8]  System (untrusted): [2026-07-10 18:26:08 GMT+8]   An async command you ran earlier has completed. The result is shown in the system messages above. Handle the result internally. Do not relay it to the user unless explicitly requested. Current time: Friday, July 10th, 2026 - 6:33 PM (Asia/Shanghai) / 2026-07-10 10:33 UTC||||Give me some screenshots
</IMPORTANT_REMINDER>
