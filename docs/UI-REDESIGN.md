# Orvell UI redesign — "Orbit Glass"

Status: implemented in the `claude/salom-ejp2u0` branch (see the PR for what was compiled and what was not seen on a device).
Scope: UI only (`ui/`, theme, resources, `MainActivity` shell). No change to `watch/`, `data/` or `sync/`.

---

## 1. Diagnosis of the previous UI

The prototype is solid and readable, but:

| Problem | Effect |
|---|---|
| One flat dark background with a single fixed glow | Every tab feels the same; no sense of place or depth |
| Metric colors used directly for text (`tone.a`) | Lime/cyan labels are unreadable in the light theme (≈1.4:1 on white) |
| `text3` at 3.0–3.5:1 (dark) and 2.4:1 (light) | Captions fail WCAG AA |
| Rings are flat gradient arcs | The signature motif has no "wow"; nothing happens when a goal closes |
| Charts are static pictures | You cannot read an exact value or time from any chart |
| Numbers appear instantly | No feeling of "loading your day" |
| Card → detail is a plain fade | No spatial continuity |
| No loading skeletons, few empty-state illustrations | First run feels unfinished |
| Settings is a long list | Hard to find "language" or "export" |
| No previews | The owner cannot review screens without a phone and a paired watch |

## 2. Concept

**Orbit Glass.** Every metric is a small light source orbiting the user. Surfaces are layered dark glass lit from above, tinted by the metric that sits on them. Motion is physical: things spring, orbit and settle, and never loop for no reason.

The concept rests on three principles.

1. **Light carries meaning.** Color appears where it encodes a metric: ring, badge, chart, glow. Everything else is neutral ink and glass, so the colored parts read first.
2. **Calm by default, alive on purpose.** Idle screens are still. Motion only answers the user (press, drag, pull), new data (count-up, ring fill), or a real event (measuring pulse, goal closed, watch found).
3. **Honest data.** No invented scores. Insights and achievements are derived only from stored readings and the user's own goals.

## 3. Tokens

### 3.1 Color

There are three layers: background → surface → raised. Each step is lighter in dark mode and whiter in light mode, with a 1 dp top highlight ("light from above").

| Token | Dark | Light | Contrast of text3 on it |
|---|---|---|---|
| bg | `#05060A` | `#F2F3F7` | 6.3 / 4.7 |
| surface | `#0E1118` | `#FFFFFF` | 5.9 / 5.3 |
| surfaceHi | `#161A24` | `#F7F8FB` | 5.4 / 5.0 |
| raised | `#1D2230` | `#EEF0F5` | 5.0 / 4.6 |
| text | `#F4F6FA` | `#0A0D13` | — |
| text2 | `#AEB6C6` | `#4E5768` | ≥ 7.2 |
| text3 | `#8890A2` | `#646C7F` | ≥ 4.6 (was 2.4–3.5) |

Each metric `Tone` now has three roles:

- `a`, `b`: the luminous gradient pair, used for rings, bars and glows;
- `ink`: a text-safe color per theme, checked at ≥ 4.5:1 on every surface;
- `glow`: a low-alpha halo.

| Metric | Gradient (a → b) | Dark ink | Light ink |
|---|---|---|---|
| Activity | `#C8F56A → #3BE3A0` | `#C8F56A` | `#3B6B00` |
| Heart | `#FF5D73 → #FF9A62` | `#FF6B80` | `#C4223D` |
| Sleep | `#9B8CFF → #4F8CFF` | `#A497FF` | `#5442D6` |
| Oxygen | `#45E3F2 → #3B8BFF` | `#45E3F2` | `#086A82` |
| Pressure | `#FFC15A → #FF7A45` | `#FFC15A` | `#945300` |
| Calories | `#FFB04A → #FF6B4A` | `#FFB04A` | `#A84A0A` |
| Distance | `#6CD3FF → #4F8CFF` | `#6CD3FF` | `#1A62B3` |
| Breathing | `#7CF0D0 → #3FB7E8` | `#7CF0D0` | `#0A6F5E` |
| Device | `#B9C2D3 → #7D8799` | `#B9C2D3` | `#5A6374` |

The sleep stage colors are unchanged: Deep `#5B4BFF`, Light `#6FA8FF`, REM `#C98BFF`, Awake `#FFB547`.

Text on a filled tone is always near-black (`#0B0E14`), at ≥ 6.5:1 on every gradient start color.

### 3.2 Space, radius, elevation

- **Spacing:** `4 / 8 / 12 / 16 / 24 / 32` (`Space.xs … xxl`). Screen gutter 16; gap between cards 12; inside a card 20.
- **Radius:** `chip = full`, `field 16`, `row 20`, `card 28`, `sheet/hero 32`. Concentric rule: an inner radius equals the outer radius minus the padding.
- **Elevation is light, not shadow.**
  - Dark mode: a colored glow under the content (radial, 18–24% alpha) plus a 1 dp top-highlight stroke.
  - Light mode: a soft neutral shadow (10 dp, 8% alpha) plus the same stroke.
  - No Material tonal elevation.

### 3.3 Type

Manrope is bundled (OFL-1.1, variable weight 200–800, Latin + Cyrillic, 165 KB, license in-app).

**Why Manrope:**
- geometric, with open apertures (reads well at small sizes);
- distinctive numerals with real tabular figures (`tnum`) for counters;
- Cyrillic support, so Russian is not rendered in a fallback font.

Missing symbols (✓ ′ ″) fall back to the system font automatically.

| Role | Size/line | Weight | Tracking | Notes |
|---|---|---|---|---|
| hero | 56/58 | 700 | −2.4 | ring centre, big numbers, tnum |
| display | 34/38 | 700 | −1.0 | screen titles, primary value |
| title | 22/28 | 700 | −0.4 | card titles in detail |
| headline | 17/22 | 700 | −0.2 | card titles |
| number | 26/30 | 700 | −0.8 | small cards, tnum |
| body | 15/22 | 400 | 0 | |
| bodyStrong | 15/22 | 600 | 0 | |
| caption | 13/18 | 400 | 0 | |
| label | 12/16 | 600 | +0.2 | |
| overline | 11/14 | 700 | +1.2 | uppercase section labels |

All sizes are in `sp` and scale with the system font. Layouts were reviewed for 200% scale:
- rows wrap instead of truncating values;
- number rows switch to a column when they don't fit.

### 3.4 Motion

| Token | Spec | Use |
|---|---|---|
| `press` | spring(dampingRatio 0.6, stiffness 700), scale 0.97 | cards, buttons |
| `settle` | spring(0.8, 400) | indicators, chips, selection |
| `enter` | 380 ms, FastOutSlowIn, 24 dp rise + fade, 45 ms stagger per item (max 8) | list entrance |
| `fill` | 1100 ms, EaseOutCubic | rings, bars, counters |
| `container` | shared bounds 420 ms | card → detail |

**Reduce motion.** `LocalReduceMotion` is true when the system animator scale is 0 (Developer options or Accessibility → "Remove animations"). When it is on:

- infinite animations (pulse, trail, radar, float, shimmer) stop and are drawn in their resting state;
- entrance and container transforms become instant;
- counters jump to the final value;
- the celebration burst is replaced by a single static highlight.

**Haptics** (`LocalHapticFeedback`):

| Moment | Feedback |
|---|---|
| Chart scrub crosses a data point | segment tick |
| Measurement starts, pull-to-refresh triggers | confirm |
| Ring closes (once per goal per day) | long press |
| Destructive confirm | reject |

**Performance rules:**
- `drawWithCache` holds every Brush and Path, keyed on size;
- moving glows are drawn by translating one cached brush;
- animated values are read inside `graphicsLayer {}` / draw lambdas, so a frame doesn't recompose;
- the background noise is a 64 px tile built once and drawn with an `ImageShader`.

## 4. Signature visuals

### 4.1 Orbit rings v2

Each ring is a **glass tube** made of four layers:

1. **Groove:** a recessed track — a dark inner stroke plus a 1 px lower highlight.
2. **Tube:** a sweep gradient `b → a` along the arc, with round caps.
3. **Specular:** a thin white arc at 30% alpha, offset towards the light (upper-left), along the same sweep.
4. **Planet:** a bright dot at the tip with a radial halo and a comet trail of 5 fading dots behind it. The trail breathes slowly (2.6 s) only while the ring is visible and reduce-motion is off.

**States:**
- **Filling:** the arc grows over 1100 ms on first show and whenever data changes.
- **Measuring:** the outer halo pulses with a heartbeat curve (0.9 s).
- **Closed (≥ 100%):** the ring flashes, 14 particles burst out once and fade, a long-press haptic fires, and the tube gains a permanent soft glow.
- **Parallax:** on scroll the rings move at 0.4× the scroll speed and fade out, which gives the header depth.

### 4.2 Aurora background

Two or three large radial "light pools" drift (very slowly) behind the content. Their colors follow the active context:

| Context | Light colors |
|---|---|
| Today | brand lime → cyan |
| Health | heart coral |
| Device | oxygen cyan |
| Profile | sleep violet |
| Detail screen | its metric |

Colors cross-fade over 600 ms. A 3% noise tile removes gradient banding on AMOLED.

### 4.3 Illustrations

All art is drawn in Compose Canvas, in the same glass-and-orbit language, with no images:

- onboarding scenes per step;
- empty states (no watch, no data yet, no sleep, nothing found);
- success (pairing done, goal closed) and error (connection failed);
- a premium watch render with a live face.

## 5. Data visualization

- **Bar charts** (hourly, week, month):
  - rounded bars with a gradient, the goal line dashed with a label;
  - drag or tap selects a bar: it lifts, the others dim, and a tooltip shows the exact value and date/hour;
  - a haptic tick fires on each new bar;
  - the bar heights grow in.
- **Line charts** (heart, SpO₂):
  - a monotone smooth curve with a gradient fill under it, and min/max reference labels;
  - drag scrubs a crosshair with a value bubble and a time label, with a haptic tick per sample.
- **Hypnogram:**
  - four lanes (Awake, REM, Light, Deep) with tinted bands, rounded stage blocks and vertical connectors;
  - tapping a block shows the stage, start–end and duration;
  - lane labels sit on the left.
- **Heart-rate zones:**
  - time in zones (rest, 1–5) from the day's readings, using max HR = 220 − age;
  - shown only if the birth date is known, and labelled "estimate, for information".
- **Sparklines:** in Health list cards, with an end dot.
- **Counters:** count-up animation with tabular figures; the width never jumps.

## 6. Navigation and feel

- **Floating tab bar:** four equal slots, each with an icon and an always-visible label (accessibility). A pill indicator springs between slots and takes the tab's metric tint.
- **Tab content:** cross-fades with a small directional slide (12 dp).
- **Card → detail:** a container transform (shared bounds). The card's glass expands into the detail screen. Back reverses it.
- **Lists:** staggered entrance the first time a screen appears in a session.
- **Pull to refresh:** a branded orbit indicator. The planet orbits faster as you pull, and the ring closes when the threshold is reached.
- **Skeletons:** shimmer placeholders with the exact card shapes while the first data loads.
- **Snackbars with Undo:**
  - "Delete all measurements" and "Unpair watch" wait until the snackbar closes (Material short duration, which the system extends for accessibility users) before the real action, and can be undone;
  - "Reset app" keeps a confirmation dialog, because it cannot be undone.
- **Coach marks:** two one-time hints on Today — "pull down to sync" and "tap a card for details". They are dismissible and stored in UI-only preferences.

## 7. Screens: before → after

| Screen | Before | After |
|---|---|---|
| **Onboarding** | Same layout per step, static rings | **Cinematic.** Animated progress bar; a Canvas scene per step (orbit system, shielded phone, goal rings filling, bell waves, watch + phone link, celebration burst; the profile step uses the live avatar instead of a scene, so the name field stays above the fold); the main button is pinned to the bottom; content enters staggered |
| **Today** | Long list of equal cards | **A story of the day.** Greeting + date; insight line ("You slept 40 min more than your weekly average"); hero rings with planets and parallax; quick actions row (Measure, Sync); measure card with live pulse; heart/sleep story cards with sparkline and stage bar; small metric tiles; interactive week chart; disclaimer. Skeleton while loading, illustrated empty state |
| **Health** | Flat list | Metric cards with ink-colored title, 7-day average counter, sparkline with end dot, trend chip (↑/↓ vs the previous 7 days); shared-element entry to detail |
| **Detail** | Static charts | Hero value counter; segmented Day/Week/Month with sliding thumb; interactive charts with tooltips; HR zones; tap-to-inspect hypnogram; stat grid that wraps at large fonts; "About this metric" |
| **Device** | 2D drawing, small buttons | Premium watch render (metal case, bezel, glass reflection, live face: time, battery ring, steps arc, glow by connection state); battery ring + charge state; state chip; full-width Sync/Reconnect; banners with one primary action each; Undo on unpair |
| **Pairing** | Radar + list | Radar with sweeping beam, devices appear as blips; compatible devices first with a badge; signal-strength bars; "confirm on watch" scene; success celebration (rings close + burst); illustrated error |
| **Profile** | Avatar card + stats | Identity card (orbit avatar, name, age · height · weight, storage badge); streaks (current and best step-goal streak); achievements earned from real data (locked ones shown dimmed with progress); goals; body |
| **Settings** | Long list | Search field filters every row; grouped glass sections with metric-colored icons; clear value chips; Undo for delete |
| **About / Licenses / Feedback / Privacy** | Plain cards | About: animated orbit logo, tagline, version; Licenses: includes the Manrope OFL; Feedback and Privacy: glass sections, section icons, readable measure |
| **App icon** | Gradient ring, same drawable for monochrome | Refined foreground (thicker ring, defined orbit, planet with highlight) + a dedicated monochrome layer for themed icons |

## 8. Accessibility checklist

- Text contrast ≥ 4.5:1 in both themes (§3.1); color is never the only signal (labels, icons and values always shown).
- Touch targets ≥ 48 dp: icon buttons, chips, tabs and chart bars all have 48 dp hit areas.
- Every icon has a `contentDescription`, or is marked decorative (`null`) when a text label sits next to it. Charts expose a summary through `semantics { contentDescription }`.
- Font scale up to 200%: nothing is clipped; rows wrap; values use `maxLines` only where the width is guaranteed.
- Reduce motion is respected (§3.4).

## 9. Previews

Every screen has a stateless `…Content` composable fed by plain data, so it can be previewed without a phone or a watch. `@OrvellPreviews` renders each screen and component in:

- dark, normal font;
- light, normal font;
- dark, 200% font;
- 360 dp width, Russian.

Sample data lives in `ui/preview/PreviewData.kt`.
