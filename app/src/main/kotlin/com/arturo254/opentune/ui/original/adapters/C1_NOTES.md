# 05/11-3 C1: expanded pieces, not activated

Reference: f8a3593fd0c37edca3da9012c0eb4bc37e907cc7. C1_PROVENANCE.json records complete source files and original blobs. BottomSheetPlayer (shared root, selector and Studio) remains for C3; no current Player.kt/MiniPlayer changes.

## Direct dependencies advanced
- Operations 11?14: original LyricsScreen/QueueScreen and layout, LikeBurst/PlayerBottomCardStack, flagship controls/landscape, seekbar/button styles, ArtworkPalette, MixingNote and original Studio PlayerMenuAction model (only the enum/slots; Studio is not activated).
- Share operations: LyricsShareSession and ShareChoreography models; fullBoundsInWindow helper only. Operation 12 image/video editor remains unavailable; the share bridge opens Android ACTION_SEND with the actual selected lyric text and actual song/artist.
- StyleSongSwipe + whileStyleInteractive extracted intact from StyleKit, without selector/menus. TvPlayerRemote is the original event/state model only; no TV surface is mounted.
- material-icons-extended uses the existing version catalog entry; needed by original lyrics footer icons.

## Real sources and actions
- OriginalPlayerConnection reads the actual GalaxOT PlayerConnection/MusicService, ExoPlayer timeline and real Room song/format/lyrics flows. toggleLike delegates to service; repeat uses existing Player.toggleRepeatMode; skip delegates through GalaxOT connection (Together/Discord routing preserved).
- Infinite queue reads/writes existing AutoLoadMoreKey (default true matches AutomixRadioController), then calls onInfiniteQueueEnabled/Disabled. No new preference/schema.
- Queue play/remove/next/reorder resolve the original Int UID against the current real Media3 timeline; ambiguous hash collisions are rejected. Shuffle moves use Media3 DefaultShuffleOrder rather than changing physical order. Current item cannot be removed. No fabricated autoplay flag.
- StreamFacts uses selected decoder track fields, falling back to the real format database record. Removed Larp badge selection, whose reference intentionally fabricates FLAC/rates.
- Lyrics VM uses the app LyricsHelper (configured real providers), preserves stored edits/translations, and stores its real result in existing LyricsEntity. Parsing/timestamps come from GalaxOT LyricsUtils. Whole-line Japanese/Korean romanization uses actual preferences/functions; no per-word readings are inferred. Source switching remains an explicit nullable callback for the real existing provider menu in C3.

## Explicitly unavailable
- GalaxOT LyricsEntity has no provider field; footer says Not available and never attributes a saved body to an invented provider. Simplest future improvement: retain provenance in memory when a real provider result is selected, without Room migration.
- Private auto-added identifiers are not exposed by the service: QueueItemData.isAutoplay remains null. Unknown items stay in the general upcoming section; simplest future improvement is exposing the existing service set read-only.
- No Mochy Cast/output-routing backend. Output pill disabled, labeled Output routing unavailable. Native system volume remains real AudioManager volume.
- Per-word ruby, parallel translation and image/video share unavailable (C5/C6/operation 12). Existing stored translated bodies and native text share work. No fake timeout: existing error adapter exposes real error label and no countdown.

## Verification
2026-11-14: attempt 1 failed with Kotlin missing direct-dependency references (no environment failure). Resolved with complete source helpers, real seek adapter, existing catalog material-icons dependency, and removal of simulated Larp badges. Attempt 2: BUILD SUCCESSFUL in 5m 7s for the combined :innertube:test :app:testUniversalDebugUnitTest :app:assembleUniversalDebug invocation. innertube:test UP-TO-DATE (5 tests, 0 failures/errors); app 578 tests, 0 failures/errors; app-universal-debug.apk generated (91,370,884 bytes). No local lint. Both invocations used the requested JBR/ANDROID_HOME prefix. Each finished before the ten-minute monitor timer; final logs were read after process exit. The existing git-rev-parse configuration-cache warning was non-blocking. No contract test changed or weakened, so no obsolete contract classification was necessary. No device visual claim: all new pieces remain unmounted.
