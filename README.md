# Weekly Schedule (Android)

A native Kotlin app to plan your week in time blocks and keep a **persistent, un-swipeable
notification** showing what you should be doing right now.

## What it does
- **Google Calendar-style week grid**: Sun–Sat columns, a 24-hour time axis (scrolls, opens at
  ~6 AM). Long-press and drag a rectangle to create a block; drag across multiple day-columns to
  put the same block on several days at once (e.g. Sun→Sat, 8–9 AM). Tap an empty slot for a quick
  1-hour block; tap an existing block to edit it.
- **Full-screen editor** with a Sun–Sat multi-select chip row, title, and start/end time pickers.
  Blocks created together share a `groupId`, so editing/deleting affects the whole set. Everything
  repeats weekly by design (blocks are stored per weekday).
- **Two notification modes** (Settings):
  - **Locked** — an ongoing notification (`setOngoing(true)`) that can't be swiped away, shown for
    the whole active task.
  - **Reminder** — a normal, dismissable notification that alerts when a task starts and repeats
    on a chosen interval (Only at start / 15 / 30 / 45 / 60 / 90 / 120 min). The interval control
    only appears in this mode.
- An **AlarmManager** alarm fires at the next relevant time (block boundary in Locked mode, next
  reminder tick in Reminder mode); `BootReceiver` re-arms after reboot, update, and clock/timezone
  changes.

## Tech
- Kotlin, Jetpack Compose (Material 3), Room, AlarmManager, `BroadcastReceiver`s, SharedPreferences.
- `minSdk 26`, `targetSdk 36`. No third-party services, no network, no ads yet.

## Project layout
```
app/src/main/java/com/untamedflame/schedule/
  core/BlockResolver.kt        Pure logic: current block, next boundary, next reminder (unit-tested)
  data/                        Room entity/DAO/db, SettingsStore (notification mode + interval)
  notify/                      NotificationHelper (2 channels), ScheduleAlarmScheduler, receivers
  ui/                          MainActivity, WeekGrid, BlockEditor, SettingsScreen, ViewModel, Theme
app/src/test/                  JUnit tests for BlockResolver
```

## Build & run
1. Open the `WeeklySchedule` folder in **Android Studio** (File → Open).
2. On first open, let it **sync Gradle** — this downloads Gradle 8.9 and regenerates the
   Gradle wrapper automatically. It also creates `local.properties` pointing at your SDK.
3. Plug in a device (or start an emulator) and press **Run** ▶.
4. Grant the **notification** permission when prompted, and tap **Allow** on the exact-alarm
   banner if it appears (Settings → Alarms & reminders).

To run the unit tests: `Run 'BlockResolverTest'` in Android Studio, or `./gradlew test`.

## Honest limitations of "notifications that can't go away"
Android does **not** allow a truly permanent, unremovable notification. This app gets as
close as the platform permits:
- `setOngoing(true)` stops the user swiping it away.
- It's re-posted at every block boundary and after reboot.

But it can still be cleared by: **force-stopping** the app, revoking notification permission,
or the OS killing the app under heavy memory pressure (the next alarm re-posts it). Aggressive
OEM battery managers (Xiaomi, Samsung, etc.) may also delay alarms — asking the user to disable
battery optimization for the app helps.

If you later want it even stickier, the next step is a **foreground service** (notification
truly can't be dismissed while the service runs), at the cost of a Play Store
`foregroundServiceType` justification.

## Ideas for next iterations
- "Snooze"/"Done" actions on the notification.
- Copy a day's blocks to other days; templates.
- Optional sound/vibration or full-screen alarm per block.
- Export/import schedule; widget.
