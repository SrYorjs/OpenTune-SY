# Base media components — 3.10

`Artwork`, `SongRow`, `AlbumCard` and `ArtistCard` are the canonical
presentational primitives in this first batch. They use the theme's semantic
colors, typography, spacing, sizes, and shapes, without owning playback,
database, source resolution, navigation or download logic.

- `Artwork`: Coil 3 imagery with placeholder and optional overlay slots.
  Hosts provide size/aspect ratio. The configured Coil loader determines
  support for animated or offline imagery.
- `SongRow`: one compact music row, metadata, active emphasis and trailing slot.
- `AlbumCard`: square artwork + title/artist; source-neutral and clickable.
- `ArtistCard`: circular artist artwork + optional subtitle, clickable.

`UniversalSearchScreen` migrates the artwork/overlay primitive without
replacing the provider/quality/versions UI. Full Home, Library and Player
adoption is reserved for 3.15, then duplicate removal for 3.16.

## 3.11 — Buttons, chips, sliders, switches, icons

`OpenTuneButton`, `OpenTunePillChip`, `OpenTuneSlider`,
`OpenTuneSwitch`, `OpenTuneIcon`, `OpenTuneIconButton` are controlled,
source-neutral presentation primitives. They do not read/write preferences.

Standard forms delegate to Material 3 for accessibility and touch targets.
Glass pill/icon options delegate to the current Liquid Glass components so
their existing animation, fallback and hit areas remain unchanged.
`ChipsRow` is the first real adopter; its public signature is unchanged.
Keep the specialized player seek-bar implementation until 3.15.
