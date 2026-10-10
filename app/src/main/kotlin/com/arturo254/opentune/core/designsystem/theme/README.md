# theme

Owns Design System tokens and their Compose providers.

Roadmap ownership:
- 3.3 semantic color roles ✅
- 3.4 typography tokens ✅
- 3.5 spacing, dimensions and density ✅
- 3.6 shapes ✅
- 3.7 elevation, transparency and borders
- 3.8 motion

3.2 only installs the provider boundary; it intentionally does not change visuals.


## Semantic color contract

Use `MaterialTheme.colorScheme` for standard Material semantics.

Use `OpenTuneDesignSystem.colors` only for OpenTune-specific meanings such as:

- glass surfaces and outlines;
- artwork/scrim overlays;
- muted/subtle metadata text;
- interactive/player surfaces;
- accent badges;
- positive/warning accents.

The semantic roles are derived from the active Material color scheme, so dynamic
theme, custom palettes, dark mode and pure black remain the source of truth.

## Semantic typography contract (3.4)

Use `MaterialTheme.typography` for standard Material components and
`OpenTuneDesignSystem.typography` for named musical UI roles:

- `screenTitle`, `sectionTitle`, `playerTitle`
- `songTitle`, `songSubtitle`
- `metadata`, `badge`

The provider reads the active `MaterialTheme.typography`, so the existing
OpenTune / System / Monocraft font preference remains authoritative. Do not
set font families or absolute `sp` sizes inside feature components for roles
already covered by these tokens. Full UI migration belongs to 3.15.

## Spacing, sizes, and density (3.5)

Use `OpenTuneDesignSystem.spacing` for the `0/2/4/8/12/16/24/32 dp`
spacing scale and named spacing in media rows and badges. Use
`OpenTuneDesignSystem.sizes` for shared media/control dimensions.

Legacy constants in `constants/Dimensions.kt` delegate to
`DefaultOpenTuneSizeTokens` so existing users keep their exact values.
Artwork radius and other shapes are still owned by 3.6.

Compose `Dp` is density-independent: DO NOT multiply it by device density.
`OpenTuneDesignSystem.density` returns `LocalDensity.current` for pixel
conversion at platform, gesture and drawing boundaries. It includes the
actual device density and fontScale, and updates with configuration changes.

`UniversalSearchScreen` is the first controlled consumer. Remaining UI
migration belongs to 3.15; no overall resizing happens in 3.5.

## Shape roles and personalization (3.6)

`OpenTuneDesignSystem.shapes` mirrors the active Material 3 shapes for
generic elements and supplies named roles for music/glass UI:

- `listArtwork`, `gridArtwork`, `searchArtwork` (unchanged legacy radii);
- `artwork` (user's `ThumbnailCornerRadiusKey`, 0–45 dp);
- `mediaCard`, `badge`, `pill`, `sheet`;
- `glassControlCluster`, `glassCard`.

The theme host reads the existing artwork preference and passes the
value through `OpenTuneTheme` to the Design System provider. This
preserves existing preference storage, settings, and previews.

`OpenTuneShapeRadii` is the canonical source for fixed radii.
Legacy constants in `constants/Dimensions.kt` remain available.
`artworkShapeFor(radiusDp)` is pure, sanitizes invalid input, and allows
interactive preview before users save their preference.

The first migration covers Universal Search, glass controls, and the
thumbnail radius selector without changing their initial appearances.
Liquid Glass optics, elevation and border styling are not modified:
those belong to steps 3.7 and 3.9.
