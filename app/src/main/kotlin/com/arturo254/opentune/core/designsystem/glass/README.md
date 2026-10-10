# Canonical Glass engine — 3.9

`GlassEngine.kt` now owns Kyant Backdrop, Sky-compatible legacy locals,
the safe no-Backdrop fallback, preset optics, and `GlassSurface`.

- Glass OFF: `GlassSurface` always clips to the requested shape and draws
  an opaque Material surface with an outlined edge.
- Glass ON + Backdrop: use the live Kyant refraction/blur path.
- Glass ON without Backdrop: use safe static fallback and platform shader
  only where supported.
- The old `ui.component.liquidGlassSurface` modifier delegates here.
  `LocalSky`, `LocalEnableLiquidGlass`, and `LocalLiquidBackdrop` retain
  their original public locations as aliases to the same canonical locals.

New consumers use `GlassSurface` for surfaces and `GlassPreset` for
optical defaults; existing custom values remain accepted through the old
wrapper. No playback, network, preferences, or source logic is owned here.
