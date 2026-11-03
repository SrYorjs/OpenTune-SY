# Android UI changes

- Measure the available Compose constraints, not physical display dimensions. Support split-screen, portrait, landscape, tablets, and increased font scale.
- Keep backgrounds edge-to-edge. Apply safe drawing and player insets to content only; do not paint rectangular panels along screen edges.
- Use natural text height and scrolling. Avoid fixed-height containers around text or controls. Wrap action groups when they cannot fit.
- Use the selected Galax liquid variant and tuning. Record only a separate background/artwork layer for refraction; never include consumers in their own backdrop source.
- Ambient animation must pause when the page is inactive or the lifecycle is not resumed, and honor reduced motion.
- Preserve real playback, queues, navigation, contextual menus, pagination, loading, and empty states when replacing presentation.
- For new screens, follow the constraint/inset pattern in `OrbitScreenFrame`; validate compact, expanded, short-height, and large-text configurations.
