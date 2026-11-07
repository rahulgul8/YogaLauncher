## How a video is recognised

Options considered, from most to least reliable:

| Approach | Accuracy | Privacy | Cost | Decision |
| --- | --- | --- | --- | --- |
| User's channel list (Regain's approach) | High: the user decides | On device | Low | **Core** |
| Starter list of known education channels | High for those channels | On device (shipped list) | Low | **Setup chips only**, never on by default |
| Title keywords (lecture, tutorial, chapter, class 12, JEE…) | Low: "lecture" can mean anything | On device | Low | **Hint only**: offers to add the channel |
| On-device LLM (Gemini Nano via ML Kit Prompt API) | Medium–high | On device | Only on recent flagships; alpha API; runs only while the app is in the foreground | **Phase 2**, suggestions only |
| YouTube Data API `categoryId` (27 = Education) | Low–medium: the uploader picks the category | Sends video IDs to Google with Yoga's key, which breaks "nothing leaves the phone" | The YouTube app doesn't expose the video ID; quota of 10k units a day | **Rejected** |
| Yoga's own in-app player for study videos | High | Network | YouTube's API terms restrict players; users want the YouTube app | **Rejected** |

### Reading the watch page

- The guard already walks a bounded part of YouTube's accessibility tree to find Shorts. On the watch page it would also read:
  - the **channel name**: the text or content description of the channel row under the player;
  - the **title**: only to show the "Is this learning?" hint.
- Both stay in memory for the current tick and are compared after normalising (case, spaces, emoji).
- **Resilience:**
  - Match by view id first, then fall back to content-description patterns.
  - If neither is found, the state is "unknown" and counts as ordinary YouTube, which is the safe default.
  - Record YouTube app versions in the launch audit, as for Shorts.
- **Autoplay:** when the next video comes from a non-learning channel, the state flips within one guard tick (≤ 1 s) and counting resumes.
- **Mini-player and picture-in-picture:** read the same fields when they're visible. If they can't be read, the state is unknown.

### Accounting

- Usage allowances come from `UsageStatsManager` per app. Learning time is measured by the guard as foreground segments with `learning = true`, summed per local day and per package.
- The effective YouTube use is the measured use minus that day's learning seconds, never below zero.
- "Learning time" is shown on its own on the dashboard and in insights.

### Focus

- During a focus session, the YouTube watch page is allowed only when the channel is on the list.
- Otherwise the guard uses Shorts' existing debounced Back, then Home if needed, and shows "Not on your learning list · Add channel".
- The home feed and search results are blocked during focus unless the user turns this off.

### Starter chips

Shipped with the app (no remote list), for students and working people alike. Learning: Khan Academy, NPTEL, MIT OpenCourseWare, CrashCourse, 3Blue1Brown, Physics Wallah, TED-Ed. Work and skills: freeCodeCamp, Google for Developers, Harvard Business Review, TED, Ali Abdaal, Y Combinator. The user taps them; none are on by default.
