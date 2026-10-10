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
replacing the provider/quality/versions UI. Further Home and Library consumers are migrated incrementally in 3.15;
function-owning media item wrappers and Player UI remain in their legacy
features until their behaviors can be migrated safely.

## 3.11 — Buttons, chips, sliders, switches, icons

`OpenTuneButton`, `OpenTunePillChip`, `OpenTuneSlider`,
`OpenTuneSwitch`, `OpenTuneIcon`, `OpenTuneIconButton` are controlled,
source-neutral presentation primitives. They do not read/write preferences.

Standard forms delegate to Material 3 for accessibility and touch targets.
Glass pill/icon options delegate to the current Liquid Glass components so
their existing animation, fallback and hit areas remain unchanged.
`ChipsRow` is the first real adopter; its public signature is unchanged.
Keep the specialized player seek-bar implementation until 3.15.

## 3.13 — Sheets, dialogs, context menus

`OverlayComponents.kt` introduces stateless OpenTuneSheet, OpenTuneDialog,
OpenTuneMenu, OpenTuneContextMenu and OpenTuneMenuItem. Legacy MenuState,
BottomSheetState and their callbacks remain unchanged. The popup overflow
and modal sheet adopt the new wrappers; dialog and player sheet radius
values are now centralized with the same previous geometry.

## 3.14–3.16 — Common states, gradual migration, deduplication

`UiStateComponents.kt` owns shared loading/empty/error/shimmer
presentations. The existing `ShimmerHost` remains the single animation
implementation. Search, Home, Library Albums/Artists and Settings now
consume the shared UI; the home global-catalog section also uses
`Artwork`.

`ui/component/EmptyPlaceholder.kt` remains a tiny compatibility adapter,
without its own duplicate visual implementation. Complex SongListItem,
AlbumGridItem, ArtistGridItem, Player, menus and navigation must not be
deleted while they still own interactions and have live consumers.
Structural tests guard contracts, but screenshot comparison needs a device.
