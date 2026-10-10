# OpenTune Design System

This package is the stable visual boundary introduced in roadmap 3.2.

Rules:
- feature screens consume Design System contracts instead of defining new global visual rules;
- the existing `OpenTuneTheme` remains the app theme owner during migration;
- Design System code must not depend on ViewModels, Sources, playback services or database state;
- migration is incremental: legacy UI remains valid until its 3.x migration step.

Packages:
- `theme/` — theme bridge and visual tokens;
- `glass/` — Liquid Glass material contracts and surfaces;
- `component/` — reusable visual primitives;
- `preview/` — isolated gallery/previews for Design System validation.
