# GalaxOT: main pages, discovery and consistent player glass

## Navigation

The three destinations are Home, Search (discovery) and Your library. A shared
`PagerState` owns page position and the single bottom indicator. The indicator
reads `currentPage + currentPageOffsetFraction`; it is not recreated per route.
Dragging the bar updates page displacement; releasing settles to a page. Taps
animate that same pager. Page swipes use Compose's HorizontalPager and its nested
scroll arbitration, so horizontal music carousels retain their own gestures.
Root routes remain compatible with shortcuts; the pager retains its saved page.
Back from Search/Library returns to Home unless a child handles Back first.

Search opens the keyboard only after tapping the rounded search field. Four
catalog genres precede listening recommendations, then the rest of the genre
catalog. Related recommendations use existing listening-derived data. Without
history there is an honest empty state and four general genre entry points.
The genre catalog can retry a failed request. No invented taste profile is stored.

Universal search fans out concurrently and now publishes completed source results
progressively. Each source has an 8-second deadline. Cancellation propagates;
results for an obsolete query cannot replace the current query. Pagination,
source resolution, version selection and offline downloads remain in their
existing controllers. Tests cover a fast provider publishing while another
provider is blocked behind a gate.

## Visual composition

Home uses an animated nebula header and artwork-based listening cards. Library
uses a matching header and a two-column collection grid. The source for each
optical card is separate from its consumer. All new optical surfaces use the
current global Galax Liquid variant/tuning; the disabled mode remains opaque.
Reduced motion disables the ambient orbit and press scale. Typography/colors
come from the active theme; visible labels have English and Spanish resources.

## Player and Canvas

The generic translucent Canvas override has been removed. The expanded player
always provides its own backdrop to the same optical pipeline used elsewhere.
A TextureView snapshot supplies current Canvas pixels to a Compose Image in the
source-only layer; controls and the AndroidView are siblings, not captured
consumers. Snapshots are at most 256x512, capped at about 15 fps, and suspended
when the lifecycle is below STARTED. Pausing keeps a still frame; song changes
clear the previous image. This is bounded frame sampling, not a promise of
60/120-fps live video refraction. Its GPU behavior, power cost and optical
fidelity still require a physical S24 FE check.

## S24 FE acceptance checklist

- Home, Search, Your library: exactly three tabs, readable at enlarged font size.
- Drag the selected capsule slowly both ways; pages and capsule follow together.
- Release near both edges and midway; try rapid reversal and taps during settling.
- Swipe page headers/vertical lists; horizontal music rows keep horizontal scroll.
- Back from Library/Search returns Home; nested pages and filters still handle Back.
- Search field opens keyboard; typing rapidly never shows stale-query results.
- Fast-provider results appear before slower providers; failed genres can retry.
- Check offline/empty library, liked songs, downloads, playlists and sorting.
- Compare selected variants on navigation, listening cards and player buttons.
- Try a Canvas track, pause/resume, skip tracks and collapse/expand repeatedly.
- Check Glass Studio after Canvas for black overlays or stale video frames.
- Repeat with disabled glass, reduced motion, light/dark themes and power saving.

CI checks Kotlin, unit tests, Android Lint and universal APK. Physical gesture,
GPU, performance and visual acceptance are separate from CI and are not claimed
by this document.
