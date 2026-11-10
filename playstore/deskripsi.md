# AutoAngler — Google Play listing

Package `tf.fog.autoangler` · v1.0 · Category: Tools · Language: English

## Title (max 30 characters)
AutoAngler

## Short description (max 80 characters)
Floating overlay that automates a fishing mini-game, with Root and Non-Root modes.

## Full description
AutoAngler is a floating overlay tool that automates a repetitive fishing mini-game. It watches the screen, recognizes the game state, and taps the right buttons for you.

Features:
- Automatic cast, reel, and catch-result handling.
- Keeps the bar in the target zone during the fishing mini-game.
- Two modes, chosen on the main screen: Root (taps via su) or Non-Root (taps via the Accessibility service).
- Floating HUD with start, stop, and a live status menu, shown on top of the game.
- Activity log to review what the bot did.
- Screen analysis runs on the device. No screenshots or screen data are uploaded.

Setup: grant the overlay permission, allow screen capture when asked, and enable the Accessibility service if you use Non-Root mode. A registration code is needed to unlock the bot; the code is checked once against the developer's server.

## Release notes
First release: Root and Non-Root modes, floating HUD, and activity log.

## Play Console notes
- Permissions: `INTERNET` (registration check), `SYSTEM_ALERT_WINDOW` (HUD), `FOREGROUND_SERVICE` (`mediaProjection`, `dataSync`), `POST_NOTIFICATIONS`, plus the Accessibility service.
- Declarations needed: Accessibility API, foreground service types, overlay. Privacy policy URL is required.
- Data safety: only the registration code and a device ID are sent to fog.tf; no screen data leaves the device.
- Likely rejection: automating a game is treated as a policy violation (Accessibility misuse, device and network abuse, cheating). Details in `PLAY_STORE.md`.
- Screenshots: `screenshots/` (01, 02 landscape; 03, 04 portrait). The Root-only variant has its own in `../autoangler-root-only/screenshots/`.
