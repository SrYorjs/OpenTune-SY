# theme

Owns Design System tokens and their Compose providers.

Roadmap ownership:
- 3.3 semantic color roles ✅
- 3.4 typography tokens
- 3.5 spacing and dimensions
- 3.6 shapes
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
