# Weekly Schedule (Android)

A native Kotlin app to plan your week in time blocks and keep a **persistent, un-swipeable
notification** showing what you should be doing right now.

## What it does
- Add time blocks per day of the week: pick a **day**, **start/end time**, and **what to do**.
- Blocks are stored locally (Room database) and grouped by day on the main screen.
- At each block boundary, an **AlarmManager** alarm fires and updates a single **ongoing**
  notification (`setOngoing(true)` → it cannot be swiped away) with the current block's task.
- Survives reboot, app update, and clock/timezone changes via `BootReceiver`, which re-arms
  the alarm and re-posts the notification.

## Tech
- Kotlin, Jetpack Compose (Material 3), Room, AlarmManager, `BroadcastReceiver`s.
- `minSdk 26`, `targetSdk 34`. No third-party services, no network, no ads yet.

## Project layout
```
app/src/main/java/com/untamedflame/schedule/
  core/BlockResolver.kt        Pure logic: current block + next boundary (unit-tested)
  data/                        Room entity, DAO, database
  notify/                      NotificationHelper, ScheduleAlarmScheduler, receivers
  ui/                          MainActivity (Compose), ScheduleViewModel, Theme
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
