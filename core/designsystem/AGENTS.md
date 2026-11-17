# core:designsystem

**Purpose:** Brand theme and components that match `design/` Foundations.

**Public API:** `MpdTheme` (`MpdTheme.colors`, `MpdTheme.type`), `LightColors`/`DarkColors`, Inter + Space Grotesk (subset variable fonts), `MpdIcons` + `MpdIcon` (Lucide vectors), components: `MpdButton(kind)`, `MpdCard`, `MpdListRow`, `CardList`, `ValueRow`, `MpdChip`, `SelectChip`, `MpdSwitch`, `MpdRadio`, `SegmentedControl`, `MpdField`, `MpdTopBar`, `BarAction`, `ScreenTitle`, `MpdNavBar`, `SaveBar`, `Banner`, `InfoNote`, `SectionLabel`, `IconTile`, `StatusDot`, `Grabber`, `Odometer`.

**Allowed dependencies:** Compose, Material 3.

**Tests:** Keep tokens in sync with `design-tools/build_mileage.py` LIGHT/DARK. Components must keep 48 dp targets and semantics.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
