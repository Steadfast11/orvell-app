> **Outdated (2026-10-12):** the cloud, AI, Health Connect, source-code and "coming soon" features in this brief were removed from the app. See the root README for the current state.

# Orvell — Design Brief

## 1. Product
Orvell is an Android app (iOS later) for smartwatches built on the **FitCloud (TopStep)** platform — watches sold under many brands that normally use the "FitCloudPro" app (e.g. GreenLion GL-SW26). Orvell connects to the watch directly over Bluetooth, keeps the user's data **on the phone**, and presents it beautifully and clearly.

- **Promise:** "Your watch. Your data. Your rules." — no mandatory account, no ads, no trackers.
- **Audience:** everyday smartwatch owners (teens to adults, Uzbekistan first, then global). Not athletes-only, not medical.
- **Languages:** Uzbek (Latin), English, Russian. Russian strings are ~30% longer — layouts must survive that.
- **Business:** free core app; later Pro / Max subscriptions (AI coach, cloud sync, more limits).
- **Status:** working prototype exists (Jetpack Compose). This brief asks for a complete, polished, production design.

## 2. Brand
- **Name:** Orvell. **Motif: orbit** — every metric is an orbit ring with a glowing "planet" dot at its tip. The logo is a ring + tilted orbit + planet dot.
- **Mood:** premium, calm, alive. Depth and light (soft glows, subtle 3D, layered cards) — not flat, not cartoonish, not "gamer RGB".
- **Dark-first**, with a full light theme.

### Tokens ("Orbit Glass", in code: `ui/theme/Theme.kt`; rationale and measured contrast: `docs/UI-REDESIGN.md` §3)
| Token | Dark | Light |
|---|---|---|
| Background / glow | #05060A / #151A2E | #F2F3F7 / #DDE4FF |
| Surface / surface-high / raised | #0E1118 / #161A24 / #1D2230 | #FFFFFF / #F7F8FB / #EEF0F5 |
| Stroke / stroke-high / top highlight | white 8% / 16% / 14% | ink 8% / 14% / white 80% |
| Text / secondary / tertiary | #F4F6FA / #AEB6C6 / #8890A2 | #0A0D13 / #4E5768 / #646C7F |
| Accent (brand) | #C8F56A → #45E3F2 gradient | ink #3B6B00 |
| Text on a colored fill | #0B0E14 | #0B0E14 |
| Status: success / warning / danger | #3BE3A0 / #FFB547 / #FF6B80 | #0A7A50 / #8F5600 / #C4223D |

Every metric tone has a gradient (a→b) for rings, bars and glows, plus a readable **ink** for text that passes 4.5:1 on every surface. Light-theme inks: activity #3B6B00 · heart #C4223D · sleep #5442D6 · oxygen #086A82 · pressure #945300 · calories #A84A0A · distance #1A62B3 · breathing #0A6F5E · device #5A6374.

Space 4/8/12/16/24/32 dp (gutter 16, card padding 20) · radius: field 16, row 20, card 28, hero 32 · type: Manrope (OFL), weights 700/600/400, tabular numbers · motion: enter 380 ms (stagger 45 ms), container 420 ms, fill 1100 ms; springs for press/settle; all motion off when the system animator scale is 0.

**Semantic colors (must stay consistent everywhere):**
Activity/steps — lime→green (#C8F56A→#3BE3A0) · Distance — sky→blue (#6CD3FF→#4F8CFF) · Calories — orange (#FFB04A→#FF6B4A) · Heart — coral→red (#FF5D73→#FF9A62) · Sleep — violet→blue (#9B8CFF→#4F8CFF) · Oxygen (SpO₂) — cyan→blue (#45E3F2→#3B8BFF) · Blood pressure — amber→orange (#FFC15A→#FF7A45) · Breathing — mint→teal (#7CF0D0→#3FB7E8) · Device — neutral steel (#B9C2D3→#7D8799).
Sleep stages: Deep #5B4BFF · Light #6FA8FF · REM #C98BFF · Awake #FFB547.
Status: success green, warning amber, danger red.

## 3. Data the app really has (do not design for data we don't have)
| Metric | Unit | Source / freshness |
|---|---|---|
| Steps, distance, active calories | count, m/km, kcal | watch, every ~5 min; hourly breakdown (24 bars) |
| Heart rate | bpm | watch auto-measure every 5–60 min (user setting); min/avg/max; "during sleep" value |
| Sleep | bedtime, wake time, minutes per stage (deep/light/REM/awake), timeline | once per night |
| SpO₂ | % | auto + on-demand |
| Blood pressure (estimate) | systolic/diastolic mmHg | on-demand measurement only |
| Breathing rate | /min | on-demand measurement only |
| Watch | name, battery %, charging, connection state, last sync time | live |
| Profile | name, sex (optional), birth date, height, weight, avatar color, goals (steps, sleep, kcal, distance) | user-entered |
History: day / week / month. All values are "for information, not medical".

## 4. Screens (all must be designed)
**Onboarding (7 steps, progress dots):** Welcome (logo animation, 3 value points) → Where to store data (Phone only / Cloud account — cloud "coming soon") → About you (name, sex, birth date, height, weight, avatar color) → Daily goals (steps, sleep presets) → Notifications permission → Pair your watch → Done.

**Pairing flow:** Bluetooth permission → Bluetooth off → Scanning (radar/orbit animation, list of found devices with signal strength, "compatible" badge) → Connecting → **"Confirm on your watch"** (watch shows a prompt; user taps ✓) → Connected (success) → error state (watch bound to another app, etc.).

**Main (bottom navigation, 4 tabs):**
1. **Today:** greeting + name + avatar; watch status chip (battery, synced N min ago); hero card with 3 orbit rings (steps / distance / calories vs goals) + hourly activity bars; **"Measure now"** card (30 s live measurement: heart, SpO₂, pressure; measuring and failed states); heart card (latest bpm + sparkline + min/avg/max); sleep card (duration, bed→wake, stage bar); small cards: SpO₂, blood pressure, breathing, weekly average; this-week steps bar chart; medical disclaimer. Pull-to-refresh.
2. **Health:** list of metrics with 7-day average + sparkline; data stats footer.
3. **Device:** 3D watch illustration with live time/battery; name, connection pill, battery bar, last sync; Sync / Reconnect; health-monitoring interval; time sync info; Bluetooth address; "coming soon" features (alarms, sedentary reminder, notifications to watch, find watch, watch faces, weather); Unpair (destructive).
4. **Profile:** avatar, name, age · height · weight, storage badge; stats (days with Orvell, 30-day steps, record day); goals; body (height, weight, BMI info, birth date, sex); links to Settings, Privacy, About.

**Detail screen (one template, 6 metrics: activity, heart, sleep, oxygen, pressure, breathing):** Day / Week / Month switch, date navigation (‹ date ›), big value, stats row, chart (bars for steps, smooth line for heart/SpO₂, **sleep hypnogram** with 4 rows), sleep stage legend with minutes and %, "About this metric" + disclaimer. Pressure/breathing show a list of measurements.

**Settings (wide):** Account & data (storage mode, account — coming soon) · Appearance (theme system/dark/light, language, units metric/imperial, 24-hour clock) · Notifications (low watch battery, watch disconnected, goal reached, system settings) · Watch (auto-measure interval, background permission) · Integrations (Health Connect, own server, AI assistant) · Data (export CSV, delete all) · App (feedback, privacy policy, open-source licenses, about, reset app). Sub-screens: own-server setup (URL, token, test, status), About (logo, version, tagline), Privacy policy, Feedback.

**System surfaces:** app icon (adaptive + monochrome), splash, notification icon, persistent "watch connected" notification, alerts.

## 5. States to cover
Watch: not paired · connecting · confirm on watch · connected · syncing · Bluetooth off · disconnected · error. Data: loading · empty (first day / no sleep yet) · partial · normal. Measuring · measurement failed. Permission denied. Light and dark for everything.

## 6. Constraints
- Android, Jetpack Compose + Material 3 underneath (custom look is fine). Phone portrait, 360–430 dp wide; support font scaling and Russian text length.
- Accessibility: contrast ≥ 4.5:1 for text, touch targets ≥ 48 dp, never color-only meaning (add icons/labels).
- Motion: subtle (ring fill on open, pulse while measuring, radar while scanning); must respect "reduce motion".
- No medical claims, no scores we cannot compute honestly.

## 7. Deliverables
1. Figma file: design tokens (color, type, spacing, radius, elevation/glow), components, all screens and states above, dark + light.
2. Interaction notes for key flows (onboarding, pairing, measure now, day/week/month switch).
3. App icon (512×512 + adaptive layers), Google Play feature graphic (1024×500), 4–8 store screenshot frames (Uzbek and English text).
