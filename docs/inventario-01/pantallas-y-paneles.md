# Pantallas, reproductores, paneles y menús

Inventario de archivos, no conteo de pantallas únicas. Los auxiliares se incluyen para no perder dependencias. Rutas exactas y expresiones dinámicas: [rutas.json](rutas.json). SHA, destino y colisiones: [archivos.csv](archivos.csv). Prefijo de origen: `app/src/main/kotlin/com/mochyy/mochymusic/`.

| Archivo | Operación principal | Funciones Compose detectadas |
| --- | --- | --- |
| `ui/menu/AddToPlaylistDialogOnline.kt` | 16 | AddToPlaylistDialogOnline |
| `ui/menu/AddToPlaylistDialog.kt` | 16 | AddToPlaylistDialog, AddToPlaylistContent, PlaylistPickRow |
| `ui/menu/AlbumMenu.kt` | 16 | AlbumMenu |
| `ui/menu/ArtistMenu.kt` | 16 | ArtistMenu |
| `ui/menu/PlaylistMenu.kt` | 16 | PlaylistMenu |
| `ui/menu/ImportPlaylistDialog.kt` | 16 | ImportPlaylistDialog |
| `ui/menu/CsvColumnMappingDialog.kt` | 16 | CsvColumnMappingDialog, ColumnSelector, CsvImportProgressDialog |
| `ui/menu/SelectionSongsMenu.kt` | 16 | SelectionSongMenu |
| `ui/menu/CustomThumbnailMenu.kt` | 16 | CustomThumbnailMenu |
| `ui/menu/SongMenu.kt` | 16 | SongMenu |
| `ui/menu/PlaylistScreenMenus.kt` | 16 | LocalPlaylistMenu, AutoPlaylistMenu, TopPlaylistMenu, CachePlaylistMenu, QueueAndDownloadCards, AutoDownloadRow |
| `ui/menu/PlaylistResyncMenu.kt` | 16 | rememberPlaylistResync, rememberResyncAll, ResyncLinkDialog |
| `ui/menu/LoadingScreen.kt` | 16 | LoadingScreen |
| `ui/menu/YouTubeAlbumMenu.kt` | 16 | YouTubeAlbumMenu |
| `ui/menu/ChooseVersionSheet.kt` | 16 | ChooseVersionContent, VersionRow |
| `ui/menu/YouTubeSelectionSongMenu.kt` | 16 | YouTubeSelectionSongMenu |
| `ui/player/BackdropBlurTransformation.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/CanvasArtworkPlayer.kt` | 11 | CanvasArtworkPlayer |
| `ui/player/CanvasArtworkUtils.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/menu/YouTubeSongMenu.kt` | 16 | YouTubeSongMenu |
| `ui/player/ChevronMenuLabel.kt` | 11 | chevronMenuLabelStyle |
| `ui/player/CardExtraLines.kt` | 11 | CardExtraLines, CardExtraLine |
| `ui/menu/YouTubePlaylistMenu.kt` | 16 | YouTubePlaylistMenu |
| `ui/menu/YouTubeArtistMenu.kt` | 16 | YouTubeArtistMenu |
| `ui/player/BottomSheetPlayer.kt` | 11 | BottomSheetPlayer, PlayerSheet, OnSheetHeadingDown, PageBackHandler, ExpandMenuOverlay |
| `ui/player/CoverReflection.kt` | 11 | CoverReflection |
| `ui/player/LarpMode.kt` | 11 | rememberLarpMode, larpQualityText, larpDacDetail, larpStatCards |
| `ui/player/HiddenTouchThrough.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/LikeBurst.kt` | 11 | LikeBurstOverlay, HeartFlight, LikedLabel, NoteText |
| `ui/player/LyricsInterludeDots.kt` | 12 | LyricsInterludeDots |
| `ui/player/LyricsSourceFooter.kt` | 12 | LyricsSourceFooter, LabeledText, SwitchSourceButton, StarOnGitHubButton, FooterButton, FooterIcon |
| `ui/player/LyricsReadings.kt` | 12 | LyricsReadings |
| `ui/player/LyricsSteadyWrap.kt` | 12 | rememberSteadyWrap |
| `ui/player/LyricsStagger.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/LyricsScreen.kt` | 12 | LyricsScreen, rememberLyricsMirrored, LyricsLoadingDots |
| `ui/player/LyricsGlowLine.kt` | 12 | LyricsGlowLine, TranslationSubText, FadingSubText, TranslationLoadingDots, rememberRubyOverlay, KaraokeSweepText, BackingVocalLine, rememberBackingExpand, rememberBloomModifier, rememberLegacyWaveModifier, rememberKaraokeHead, rememberWordMotionModifier |
| `ui/player/MorphingSongInfo.kt` | 11 | MorphingSongInfo |
| `ui/player/MixingNote.kt` | 11 | MixingNote |
| `ui/player/MorphingCover.kt` | 11 | MorphingCover, MiniPreviousButton, MiniPlayerButton, MiniSongInfo, MiniSongLines, CoverPicture, PlayerBackdropCrossfade, CoverBackdropFade, VideoBackdropBlur, SheetPillGlass, SheetBackdrop, CoverCanvasLayer |
| `ui/player/PlayerBottomCardStack.kt` | 11 | PlayerBottomCardStack, DeckCard, flightProgress, QueueCardContent, PlayerDeckPreview, NextSongRow |
| `ui/player/PlayerHandle.kt` | 11 | rememberPlayerHandleState, PlayerHandle, rememberOnLightBehind |
| `ui/player/PlayerProgressSlider.kt` | 11 | PlayerProgressSlider, TimeText, rememberSettled, LoadingLabel |
| `ui/player/PlayerPageTransition.kt` | 11 | rememberPlayerPageTransition, ExpandingCardSurface, pageTransitionLayer |
| `ui/player/PlayerLayoutTweaks.kt` | 11 | rememberPlayerLayoutTweaks |
| `ui/player/QueueScreen.kt` | 12 | QueueScreen, QueueSectionHeader, QueueSongRow, NowPlayingIndicator |
| `ui/player/PlayerUiState.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/SeamBlur.kt` | 11 | SeamBlur |
| `ui/player/PlayerVolumeBar.kt` | 11 | playerVolumeBarFootprint, PlayerVolumeBar, VolumeRow, rememberMusicVolume |
| `ui/player/SongCarousel.kt` | 11 | rememberSongCarousel, SongCarouselFeed, rememberTextSlots, CarouselPictures, CarouselCrossfade |
| `ui/player/SongInfoRow.kt` | 11 | SongInfoRow, SongLines, FadingOverflowText, PressScaleActionButton |
| `ui/player/PlayerControls.kt` | 11 | PlayerControls, PlayerTransportRow, AnimatedPressScaleSkipButton, AnimatedPressScalePlayPauseButton |
| `ui/player/StreamFailureUi.kt` | 11 | rememberStreamFailureProgress, rememberStreamFailureLabel |
| `ui/player/StreamFormatBadge.kt` | 11 | StreamFormatBadge |
| `ui/player/PlaybackClock.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/TvPlayerRemote.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/VideoLetterbox.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/TurntableControls.kt` | 11 | TurntableControls, TurntablePills, Pill, PressScaleIcon, ChevronButton, TitleBlock, TitleLines |
| `ui/player/MiniLyricsTicker.kt` | 05 | MiniLyricsTicker, TickerUpcoming, TickerReadings, SettlingLine, UpcomingLine, TickerLine |
| `ui/player/YouTubeLyricsVideoSync.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/bedside/BedsideChrome.kt` | 13 | bedsidePagerFling, RememberPage, PageDots, BedsideHandle, rememberPullToClose, bedsideInsets |
| `ui/player/bedside/BedsideClock.kt` | 13 | rememberBedsideTime, BedsideClockFaceView, BigFace, StackedFace, AnalogFace, AnalogDial, CoverFace, FlipDigits, clockStyle, labelStyle, dpToSp |
| `ui/player/YouTubeVideoLookup.kt` | 11 | rememberSongVideo, rememberSongVideoLookup, rememberVideoTimeline |
| `ui/player/YouTubeVideoBackground.kt` | 11 | YouTubeVideoBackground |
| `ui/player/bedside/BedsideInfoTile.kt` | 13 | InfoTilePage, LyricsTile, StackedTile, AnalogTile, BarsTile, DialBarsTile, BedsideBars |
| `ui/player/bedside/BedsideNowPlaying.kt` | 13 | BedsideNowPlaying, SongDetails, CoverSlots, SongSlots, BedsideCover, rememberBedsideProgress, ProgressLine, rememberPlayingTrack, rememberCoverPainter |
| `ui/player/bedside/BedsidePlayerStyle.kt` | 13 | BedsidePlayerStyle, BedsideLayout, BedsideClockSection, TopButtons |
| `ui/player/customize/DiscCoverStack.kt` | 11 | DiscCoverStack, rememberAlbumArtwork |
| `ui/player/bedside/BedsideWindow.kt` | 13 | BedsideWindowFlags, rememberPhoneLocked, LockedBackHandler |
| `ui/player/customize/PlayerCoverLayout.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/customize/CoverGradientBackdrop.kt` | 11 | rememberWarpShader, CoverGradientBackdrop |
| `ui/player/bedside/BedsideTilesStyle.kt` | 13 | BedsideTilesStyle, TilesLayout, MusicTile, InfoTile, MenuButton |
| `ui/player/customize/TurntableArm.kt` | 11 | TurntableArm, TurntableBase |
| `ui/player/customize/DiscCover.kt` | 11 | DiscCover, DiscNamePlate, DiscTonearm |
| `ui/player/customize/PlayerCoverStyles.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/customize/PlayerBackgroundStyles.kt` | 11 | PlayerBackgroundContent, StandaloneAmbientBackground, PlayerBackgroundPreview, SolidBackground, DarkGradientBackground, AppleMusicBackground |
| `ui/player/bedside/BedsideRest.kt` | 13 | rememberBedsideRest, rememberBedsideInk, rememberBedsideNight |
| `ui/player/customize/TurntableArt.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/bedside/BedsideLockAccess.kt` | 13 | rememberLockScreenAccess, LockScreenPrompt, PromptButton |
| `ui/player/bedside/BedsideMusicTile.kt` | 13 | MusicTilePage, CoverPage, CardPage, SongTimes, ProgressTime, SmallCover, LikeButton, TileTransport, TileButton |
| `ui/player/flagship/AmbientColors.kt` | 11 | rememberAmbientColors, FollowPicture |
| `ui/player/flagship/ControlsShade.kt` | 11 | ControlsShade |
| `ui/player/flagship/AmbientBackdrop.kt` | 11 | AmbientBackdrop, rememberAmbientClock |
| `ui/player/flagship/BlurredCover.kt` | 11 | BlurredCoverPicture, BlurredLiveCover |
| `ui/player/flagship/CoverVeil.kt` | 11 | CoverVeil, VeilPage |
| `ui/player/flagship/CoverFrost.kt` | 11 | CoverFrost |
| `ui/player/flagship/FlagshipLandscape.kt` | 11 | FlagshipLandscape, LandscapeCover, LyricsBand, SongTitles, TitleLines, CoverButtons, SquareCover |
| `ui/player/flagship/FlagshipBottomBar.kt` | 11 | flagshipBarFootprint, FlagshipBottomBar, AppleVolume, OutputPill, pressShrink |
| `ui/player/flagship/SpeakerIcons.kt` | 11 | rememberSpeakerMotion |
| `ui/player/flagship/TransportIcons.kt` | 11 | rememberTransportShapes, rememberPlayGlyph, AnimatedPlayGlyph, AnimatedSkipGlyph |
| `ui/player/flagship/PagesPill.kt` | 11 | PagesPill, PillHalf, FlagshipPillMorphSurface |
| `ui/player/info/AboutPreview.kt` | 11 | AboutPreviewCard |
| `ui/player/info/ForceProviderSheet.kt` | 11 | ForceProviderSheet, SwitchStatus, ProviderChoiceRow, QualityChip |
| `ui/player/info/AboutSongSheet.kt` | 11 | AboutSongSheet, GeneralInformation, LoadingBullet, SourceChip, AboutCard, ArtistRow, StoreRow |
| `ui/player/flagship/FlagshipTransport.kt` | 11 | FlagshipTransportRow, SlotRow, pressScale, PlayButton, SkipButton, TenSecondsButton, TenSecondsIcon, TransportLayoutPreview, StillSkip |
| `ui/player/info/AboutSourceSheet.kt` | 11 | AboutSourceSheet, SourceRow, SourceMark |
| `ui/player/info/InfoSheetKit.kt` | 11 | InfoSheetTitle, InfoSectionTitle, InfoDivider, NerdsDivider, InfoCard, CardConnector, StatLine, InfoPill, LoadingBar, InfoBullet, InfoColumn |
| `ui/player/info/SongAbout.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/info/ProviderLogo.kt` | 11 | ProviderLogo |
| `ui/player/ipod/IpodArt.kt` | 13 | rememberCoverPainter |
| `ui/player/ipod/IpodController.kt` | 13 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/info/StreamStats.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/ipod/IpodClickWheel.kt` | 13 | IpodClickWheel |
| `ui/player/ipod/IpodHaptics.kt` | 13 | rememberIpodHaptics |
| `ui/player/ipod/IpodCoverFlow.kt` | 13 | IpodCoverFlow, AlbumCaption, AlbumBack |
| `ui/player/info/StreamInfoSheet.kt` | 11 | StreamInfoSheet, ProviderRow, StatCardView, rememberStatCards |
| `ui/player/ipod/IpodHeader.kt` | 13 | IpodHeader, rememberIsPlaying, rememberBatteryLevel |
| `ui/player/ipod/IpodGeometry.kt` | 13 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/ipod/IpodMenu.kt` | 13 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/ipod/IpodNowPlaying.kt` | 13 | IpodNowPlaying, ClassicLayout, CoverWithReflection, SongText, SongLine, rememberTrack, TimeRow, VolumeRow, TimeText |
| `ui/player/ipod/IpodPlayerStyle.kt` | 13 | IpodPlayerStyle, IpodFace, BarTimeout, TitleBar, IpodPageContent, rememberArtwork, Chevron, DarkStatusIcons |
| `ui/player/menu/DeviceOutputSheet.kt` | 11 | DeviceOutputSheet, SectionLabel, VolumeCard, DeviceRow |
| `ui/player/ipod/IpodScreen.kt` | 13 | IpodPages, IpodSidebarShadow, IpodMenuList, IpodMenuRow, IpodScrollbar, IpodCoverCycle |
| `ui/player/menu/ModuleMenuRows.kt` | 11 | ModuleMenuRows, ModuleMenuRow |
| `ui/player/menu/LineSlider.kt` | 11 | LineSlider |
| `ui/player/menu/LyricsToolSheets.kt` | 12 | LyricsEditorSheet, LyricsProviderSheet, LyricsTranslationSheet, rememberYouTubeTranslationLanguages, YouTubeTick, LanguagePickerSheet, PillButton |
| `ui/player/menu/PlaybackSpeedSheet.kt` | 11 | PlaybackSpeedSheet, OnlyThisSongRow, ValueSlider |
| `ui/player/menu/MenuSheet.kt` | 11 | rememberMenuSheetStack, MenuSheetSurface |
| `ui/player/menu/PlayerActionMenu.kt` | 11 | PlayerActionMenu, MenuRow, KeepScreenOnRow, SleepTimerRow, DeviceVolumeRow |
| `ui/player/ipod/IpodLook.kt` | 13 | rememberIpodLook |
| `ui/player/menu/SkinCustomiseSheet.kt` | 11 | SkinCustomiseSheet |
| `ui/player/menu/SleepTimerSheet.kt` | 11 | SleepTimerSheet, SheetButton |
| `ui/player/menu/SongDetailsSheet.kt` | 11 | SongDetailsSheet, SheetColumn, SongHeader, DetailRow, DetailGlyph, DetailImage, DoneButton, rememberArtistAvatar |
| `ui/player/models/PlayerModels.kt` | 11 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/share/ShareBackdrops.kt` | 12 | BackdropOf, PictureBackdrop, MusicBackdrop |
| `ui/player/menu/LyricsOffsetSheet.kt` | 12 | LyricsOffsetSheet, AutoSyncStatus, OffsetValueBox, NudgeLabel, OffsetTrack |
| `ui/player/share/ShareCanvas.kt` | 12 | MochyMark, SongHeader, CinematicLayout, rememberCinematicTextSize |
| `ui/player/share/LyricsShareSession.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/share/ShareChoreography.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/menu/TvMenuRows.kt` | 11 | rememberPhoneLinkHost, TvMenuRows, TvRemoteDialog |
| `ui/player/share/ShareBackgroundKit.kt` | 12 | AddCircle, PictureBackgroundControls, ColourBackgroundControls, ShareHint |
| `ui/player/share/ShareCinematicFlow.kt` | 12 | SungLine |
| `ui/player/share/ShareColourSpectrum.kt` | 12 | ColourSpectrum |
| `ui/player/share/ShareEditOutlines.kt` | 12 | EditOutlines, rememberSquish, TapArea, MoveSongArrow |
| `ui/player/share/ShareKit.kt` | 12 | ShareTitle |
| `ui/player/share/ShareLineSheets.kt` | 12 | LinePickerContent, PickedSummary, WordMeter, LineRow |
| `ui/player/share/SharePalette.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/share/song/ShareSources.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/share/ShareWordEditor.kt` | 12 | WordEditorContent, WordChip, MakeItCard |
| `ui/player/share/song/ShareTimeline.kt` | 12 | ShareTimeline, TimeLabel |
| `ui/player/share/song/SongPreviewPlayer.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/share/song/ShareVideoPartSheet.kt` | 12 | VideoPartContent |
| `ui/player/share/ShareFlight.kt` | 12 | ShareFlight |
| `ui/player/share/song/SongSharePreview.kt` | 12 | rememberIsFront, SharePictureFit, SongSharePreview, ShareVideoLayer, LoopingVideo |
| `ui/player/share/song/SongShare.kt` | 12 | SongShareHost, LyricsSongShare, SongShareDialog, MovesThePlayhead, ReadLyricsLook, SongPreviewAudio, WatchesTheApp, EdgeToEdgeDialogWindow |
| `ui/player/share/song/SongShareEditor.kt` | 12 | EditorContent, PlaysThePreview, SongTimeline, EditBackgroundPill, SectionLabel, CardCloseUp, CardColours |
| `ui/player/share/song/SongShareSession.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/share/song/export/OffscreenFrames.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/share/song/SongShareCards.kt` | 12 | MusicOnlyCard, LyricsCard, CardCover, ClipProgress, PlayingBadge, StillLyrics, rememberStillTextSize, SungLyrics, rememberLineScale, SungLine |
| `ui/player/share/song/SongSharePages.kt` | 12 | GoBackButton, TilePicture, ChoiceTile |
| `ui/player/share/song/export/ShareExportParts.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/share/song/SongShareBackgroundSheet.kt` | 12 | SongBackgroundContent, CoverStill, DimCard, TextAction |
| `ui/player/share/song/SongShareSendPage.kt` | 12 | SendContent, ShareAsVideoRow, SendButton, PreviewPill, AppTile, QualityPicker, ExportProgress, Caption, Divider |
| `ui/player/share/song/export/ShareGlEffects.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/share/song/export/ShareTargets.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/studio/BarEditor.kt` | 14 | BarEditor |
| `ui/player/studio/IpodOptions.kt` | 14 | IpodOptions, Swatch |
| `ui/player/share/song/export/SongShareExport.kt` | 12 | ExportPicture |
| `ui/player/studio/EditParts.kt` | 14 | rememberStudioParts, EditTiles, TileTarget |
| `ui/player/studio/MiddleEditor.kt` | 14 | MiddleEditor, MenuItemButton, MenuItemLabel, MiddleLabel, TileTap |
| `ui/player/studio/BedsideOptions.kt` | 14 | BedsideOptions, LockScreenRows |
| `ui/player/studio/PlayerMenuActions.kt` | 14 | rememberPlayerMenuLayout |
| `ui/player/studio/PlayerMenuExtras.kt` | 14 | rememberPlayerMenuExtras |
| `ui/player/studio/PlayerSelector.kt` | 14 | PlayerSelectorOverlay, CardButton |
| `ui/player/studio/StudioCamera.kt` | 14 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/studio/PlayerStudioState.kt` | 14 | rememberPlayerStudioState |
| `ui/player/share/song/SongSharePicture.kt` | 12 | SongSharePicture, CinematicWords, CardSlot, CardGlass, SongShareBackdrop, StillBackdrop |
| `ui/player/studio/StudioSheets.kt` | 14 | StudioSheets, StyleSheets, PosterOptions, OptionSheet, OptionRow, TwoLines, SwitchRow, TextRow, LabelField |
| `ui/player/studio/StudioPreviews.kt` | 14 | CoverArt, FlagshipPreview |
| `ui/player/studio/StudioHaptics.kt` | 14 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/studio/StudioEditOverlay.kt` | 14 | StudioEditOverlay, StudioTopBar, CoverEditor |
| `ui/player/studio/StudioBackdrop.kt` | 14 | rememberStudioPieces, StudioCameraEffect |
| `ui/player/styles/CoverRing.kt` | 13 | rememberCoverRingState, CoverRing |
| `ui/player/styles/AlternatePlayerStyles.kt` | 13 | AlternatePlayerStyle, rememberStyleTrack, rememberStyleCarousel, StyleSongs, StyleFrame, rememberBackdropClock, styleInsets, PosterPlayerStyle, PosterText, CreditLine, rememberStyleLyrics, CircleCover, TimelinePlayerStyle, LyricsPlayerStyle, StyleControls, RidingTimes, PlayedTime |
| `ui/player/styles/ExpressiveText.kt` | 13 | ExpressiveTitle, ExpressiveLyrics, InterludeRow, LyricLine |
| `ui/player/styles/SkinBackdrops.kt` | 13 | SkinBackdrop, SkinLayers, SkinBackgroundFill, SkinPictureBackdrop, AmbientSkinBackdrop |
| `ui/player/styles/SkinElements.kt` | 13 | SkinButton, SkinShape, SkinTime, rememberSkinMenuExtras, rememberTargetOn, rememberTargetRunner |
| `ui/player/styles/SkinPulse.kt` | 13 | rememberSkinPulse, rememberEnterProgress |
| `ui/player/styles/ModulePlayerStyle.kt` | 13 | ModulePlayerStyle, ColumnBlocks, RowBlocks, StackBlocks, Block, SkinCover, SongText, TextLine, SkinPicture, SkinProgress, SkinControls, SkinLyrics, rememberRecordTurn |
| `ui/player/styles/StyleKit.kt` | 13 | rememberStyleTones, withLyricsOffset, rememberLivePosition, rememberStyleShowing, rememberDurationMs, rememberPlayedSeconds, StyleCircle, TransportCircles, StyleMenu, MenuChevron, ReportSplit, StyleStage, rememberResponsive |
| `ui/player/styles/SkinShader.kt` | 13 | SkinShaderSurface, ShaderFill, SkinShaderBlock |
| `ui/player/studio/StudioType.kt` | 14 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/styles/StyleLyricsPage.kt` | 13 | StyleLyricsPage, StyleQueuePage, StyleOverlayPage |
| `ui/player/styles/TurnedFlingFix.kt` | 13 | TurnedFlingFix |
| `ui/player/video/VideoModeControls.kt` | 12 | rememberControlsVisible, VideoTopBar, TopButton, VideoTransport, TransportButton, VideoVolumeBar, VideoSeekRow, TimeText, VideoSeekFlash |
| `ui/player/video/VideoLoadingIndicator.kt` | 12 | VideoLoadingIndicator |
| `ui/player/video/VideoDash.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/video/VideoModeMenu.kt` | 12 | VideoModeMenu, TrailingValue, qualityLabel, VideoQualitySheet, QualityRow |
| `ui/player/video/VideoModePane.kt` | 12 | VideoPaneBackdrop, VideoPaneContent, VideoPaneHandle |
| `ui/player/video/VideoModeLyrics.kt` | 12 | rememberVideoLyrics, rememberVideoLyricLook, ProvideVideoLyricScale, VideoLyricRow, VideoRegularLyrics, RegularSlot, VideoPaneLyrics, FollowingLyrics |
| `ui/screens/AlbumScreen.kt` | 09 | AlbumScreen, AlbumRetry |
| `ui/player/video/YouTubeVideoSurface.kt` | 12 | rememberYouTubeVideoSession, DriveYouTubeVideo, YouTubeVideoSurface |
| `ui/player/video/YouTubeVideoSession.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/player/video/VideoModeScreen.kt` | 12 | VideoPlayerStyle, VideoModeScreen, VideoModeLayout, VideoTurnFrame, VideoArtwork, VideoAddToPlaylist, rememberAmbient, VideoModePreview |
| `ui/screens/BrowseScreen.kt` | 16 | BrowseScreen |
| `ui/player/styles/SkinVisualizer.kt` | 13 | SkinVisualizer |
| `ui/screens/ExploreScreen.kt` | 16 | ExploreScreen |
| `ui/screens/ComingSoonScreen.kt` | 16 | ComingSoonScreen |
| `ui/screens/HistoryScreen.kt` | 16 | HistoryScreen |
| `ui/player/video/VideoModeState.kt` | 12 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/SectionDetailScreen.kt` | 06 | SectionDetailScreen |
| `ui/screens/HomeScreen.kt` | 06 | rememberCardRowState, rememberTrackGridState, HomeCompactTop, CommunityPlaylistCard, AllTimeHitsCard, HomeShell, HomeScreen |
| `ui/screens/artist/ArtistDesign.kt` | 09 | ArtistBanner, BannerBlurBand, BannerScrims, ArtistAvatar, ArtistName, ArtistActions, SubscribePill, ArtistReleasePager, ArtistCard, ReleaseCard, BioCard, ArtistShelfTitle, ArtistMenuItem, ArtistTopBar, ArtistHeaderBones |
| `ui/screens/YouTubeBrowseScreen.kt` | 16 | YouTubeBrowseScreen |
| `ui/screens/Screens.kt` | 04 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/artist/ArtistItemsScreen.kt` | 09 | ArtistItemsScreen, ItemsRetry |
| `ui/screens/NetworkReload.kt` | 16 | NetworkReload |
| `ui/screens/artist/ArtistSimilarScreen.kt` | 09 | ArtistSimilarScreen |
| `ui/screens/artist/ArtistAlbumsScreen.kt` | 09 | ArtistAlbumsScreen |
| `ui/screens/artist/ArtistSongsScreen.kt` | 09 | ArtistSongsScreen |
| `ui/screens/NavigationBuilder.kt` | 04 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/artist/ArtistScreen.kt` | 09 | ArtistScreen, ArtistAbout, ArtistRetry |
| `ui/screens/artist/SimilarDesign.kt` | 09 | SimilarPage, SimilarTitleBand, StartOfTheLine, ReceiptLine, TallPlaylistCard, PagedSongGrid |
| `ui/screens/audiolab/AudioFxPictures.kt` | 16 | PictureCanvas, LoudnessPicture, SpatialPicture, AtmosPicture, SpeedPicture, VolumeCurvePicture |
| `ui/screens/artist/ArtistSubpageKit.kt` | 09 | rememberArtistBanner, ArtistSubpageHeader, LayoutToggle, SubpageSquareCell, SubpageVideoCell |
| `ui/screens/audiolab/AudioFxScreen.kt` | 16 | AudioFxScreen, EffectCard, pct, LoudnessCard, SpatialCard, MonoCard, AtmosCard, SupportPill, SpeedCard, StepButton, VolumeCard |
| `ui/screens/audiolab/AudioPassScreen.kt` | 16 | AudioPassScreen, rememberBluetoothCodec, subtitleFor, StatusCard, dacRows, BluetoothDetails, bluetoothRows, ldacLabel, InfoRows, Note, PillButton, OptionTile, PassthroughCard, statusLine, DirectUsbCard, BlockersCard |
| `ui/screens/audiolab/AudioLabPage.kt` | 16 | AudioLabPage, LabBackdrop, LabSheetTitle, LabGroup, LabHeader, LabBadge, TopScrim, LabBackButton |
| `ui/screens/audiolab/AudioLabScreen.kt` | 16 | AudioLabScreen, BigHubCard, SmallHubCard, StatusLine, sketchTime, EqualizerSketch, FxSketch, QualitySketch, PassSketch, TransitionSketch |
| `ui/screens/audiolab/AudioQualityScreen.kt` | 16 | AudioQualityScreen, Footnote, ProviderCaption, ProviderLogo, QualityRow |
| `ui/screens/audiolab/AudioLabKit.kt` | 16 | LabCard, LabStage, LabToggleRow, LabSlider, LabPanel, LabCollapsible, LabSectionLabel, LabTextAction, LabChips, LabSegmented, LabGlyph, animatedDim, rememberPicturesHeld, rememberLabTime, trackOnScreen, ListenToAudio, rememberOnScreen, dpToPx |
| `ui/screens/audiolab/FxCardStack.kt` | 16 | FxCardStack, StackedCard |
| `ui/screens/audiolab/LabVisualizer.kt` | 16 | rememberLabSpectrum, LabBars |
| `ui/screens/audiolab/LabHeaderCard.kt` | 16 | LabHeaderCardFrame, LabCardFace |
| `ui/screens/audiolab/ProviderOrder.kt` | 16 | ProviderOrder |
| `ui/screens/donate/DonateParts.kt` | 16 | DonateCharacter, DonatePillButton |
| `ui/screens/donate/NameMarquee.kt` | 16 | NameMarquee |
| `ui/screens/donate/SupportSheet.kt` | 16 | SupportSheetContent, SupportButtons |
| `ui/screens/donate/DonatePrompt.kt` | 16 | DonatePromptScheduler |
| `ui/screens/donate/DonateSheet.kt` | 16 | DonateSheetContent, DonateLinkRow |
| `ui/screens/audiolab/ReverbCard.kt` | 16 | ReverbCard, FineTuneLabel, ReverbPicture |
| `ui/screens/equalizer/AutoEqTab.kt` | 16 | AutoEqTab, Legend, ResponseGraph, SelectedHeadphone, FormGlyph, Database, SearchField, ModelRow, AlphabetRail, ShimmerRow |
| `ui/screens/equalizer/EqBandsEditor.kt` | 16 | EqBandsSection, BandSliders |
| `ui/screens/equalizer/EqualizerScreen.kt` | 16 | EqualizerScreen, Presets, Output |
| `ui/screens/equalizer/EqGraphEditor.kt` | 16 | EqGraphSection, EqGraph, NodeDetail |
| `ui/screens/gesture/CustomisationCards.kt` | 16 | CoverRoundingSection, CardTitle, CustomiseSliderCard, RoundingSwatch, BareSlider, PlayerStyleCard, StyleCaption, StyleCard |
| `ui/screens/gesture/CustomisationLivePreviews.kt` | 16 | BlurPreview, BlurPreviewPage, ElementBlurPreview, AnimationPreview, LyricsStylePreview, StatusBarPreview, NavBarPreview |
| `ui/screens/gesture/SwipePreview.kt` | 16 | SwipePreview, SampleRow |
| `ui/screens/gesture/CustomisationIcons.kt` | 16 | IconsSection, rememberIconArt, PickedIcon, FamilyIcons, IconTile, IconArt, TileLabel, CustomizeButton, ColorSwatches, Swatch |
| `ui/screens/library/LibraryAddMusicSheet.kt` | 08 | SongPickerContent, PickerRow, PickerCheckbox |
| `ui/screens/library/LibraryArtistsScreen.kt` | 08 | LibraryArtistsScreen |
| `ui/screens/gesture/CustomisationPreviews.kt` | 16 | PreviewFrame, PlaylistStylePreview, PlayerStylePreview, HomeStylePreview, IpodPreview, NowPlayingBars, TitleBars, TrackRow, ListenRow, HopTile, Shelf, Bar |
| `ui/screens/library/LibraryCachedTab.kt` | 08 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/library/LibraryGridScreen.kt` | 08 | LibraryGridScreen, LibraryGridTile |
| `ui/screens/gesture/CustomisationEffects.kt` | 16 | BlurSection, ElementBlurSection, AnimationSection, LyricsStyleSection, TopBarSection, BottomBarSection, EffectCard, PillSwitchRow, MiniSegmented |
| `ui/screens/library/LibraryEntityListScreen.kt` | 08 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/library/LibraryDesign.kt` | 08 | LibraryRule, LibrarySectionTitle, LibraryGridArtwork, LibraryPinnedTile, LibraryCoverStack, LibraryCardLabel, LibraryCount, LibraryEmptyState, LibraryCategoryCard, LibraryWideCard, LibraryListRow, LibraryLetterHeader, LibraryPlayBar |
| `ui/screens/library/LibraryImportedScreen.kt` | 08 | LibraryImportedScreen |
| `ui/screens/library/LibraryImportFlow.kt` | 08 | LibraryImportFlow |
| `ui/screens/library/LibraryImportedTab.kt` | 08 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/library/LibraryImport.kt` | 08 | ImportSourceIcon, LibraryImportBanner, ImportPromoBanner, ProviderMarquee, ImportProgressBanner |
| `ui/screens/library/LibraryHomeScreen.kt` | 08 | LibraryHomeScreen |
| `ui/screens/library/LibraryLocalImport.kt` | 08 | rememberLocalImportActions |
| `ui/screens/library/LibraryPlaylistsScreen.kt` | 08 | LibraryPlaylistsScreen |
| `ui/screens/library/LibraryManageSheets.kt` | 08 | ManageLayout, ManageTile, ManageRow, ManageIcon, ManageLabel |
| `ui/screens/library/LibraryIndex.kt` | 08 | LibraryIndexRail |
| `ui/screens/library/LibraryLocalTab.kt` | 08 | LocalImportCard, LocalCard, LocalCardTitle |
| `ui/screens/library/LibraryShelfCards.kt` | 08 | collectShelfTabData |
| `ui/screens/library/LibraryProgressSheet.kt` | 08 | TransferSheetContent, TransferRow, CancelDownloadsButton, CreatePlaylistButton |
| `ui/screens/library/LibraryRecentlyAddedScreen.kt` | 08 | LibraryRecentlyAddedScreen |
| `ui/screens/library/LibraryAlbumsScreen.kt` | 08 | LibraryAlbumsScreen, ImportedAlbumsScreen, ShelfAlbumsScreen |
| `ui/screens/gesture/GestureScreen.kt` | 16 | GestureScreen, CustomisationScreen, CustomiseSectionContent, PlayerSection, GestureSection, highlightTarget, SectionRule, SwipeCard, ActionDropdown, ActionPicker, ActionOptionBadge, PlayerGestureCard |
| `ui/screens/library/LibraryScaffold.kt` | 08 | LibraryScaffold |
| `ui/screens/library/LibraryProgressCard.kt` | 08 | rememberCardVisible, LibraryProgressCard, ProgressBar, TransferRing |
| `ui/screens/playlist/CachePlaylistScreen.kt` | 09 | CachePlaylistScreen |
| `ui/screens/playlist/GeneratedPlaylistKit.kt` | 09 | GeneratedPlaylistPage, GeneratedSearchCapsule, GeneratedGroupHeader, GeneratedNoResults, rememberSongsDownloadState |
| `ui/screens/playlist/LocalPlaylistScreen.kt` | 09 | LocalPlaylistScreen |
| `ui/screens/playlist/AutoPlaylistScreen.kt` | 09 | AutoPlaylistScreen, LikedSourceSeparator |
| `ui/screens/profile/ProfileMorph.kt` | 16 | rememberProfileMorphState, ProfileMorphHost, ProfileBackHandler, ProfileBackdrop, rememberProfileNameStyle, FlyingParts |
| `ui/screens/playlist/AddMusicRows.kt` | 09 | PickRow, PickBox, AddMusicChip, PickRowBones, SheetNote |
| `ui/screens/library/LibraryTabs.kt` | 08 | LibraryTabChips, LibraryTabChip, LibraryCapsLabel, LibraryManagePill, LibraryCardGrid |
| `ui/screens/playlist/AddMusicSheet.kt` | 09 | AddMusicContent |
| `ui/screens/profile/EditProfileSheet.kt` | 16 | EditProfileContent, EditablePhoto |
| `ui/screens/playlist/TopPlaylistScreen.kt` | 09 | TopPlaylistScreen |
| `ui/screens/library/LibrarySongsScreen.kt` | 08 | LibrarySongsScreen |
| `ui/screens/profile/UserProfilePage.kt` | 16 | UserProfilePage, NameLine, EditProfilePill, ListensRow, ListensCard, ListensFigure, RecentSection, PlaylistsSection, ArtistsSection |
| `ui/screens/recap/IdCardBack.kt` | 16 | IdCardBack, ScratchCoating |
| `ui/screens/profile/ProfileStore.kt` | 16 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/playlist/OnlinePlaylistScreen.kt` | 09 | OnlinePlaylistScreen |
| `ui/screens/recap/RecapSnippetPlayer.kt` | 16 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/recap/RecapStory.kt` | 16 | RecapStoryHost, RecapStory, StoryLogo, StoryAvatar, StoryCard |
| `ui/screens/playlist/AddMusicSources.kt` | 09 | rememberSongSearch |
| `ui/screens/recap/RecapData.kt` | 16 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/recap/StatsRuler.kt` | 16 | StatsRuler |
| `ui/screens/search/GenreDescriptions.kt` | 07 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/recap/RecapStoryPages.kt` | 16 | visible, Picture, StoryPages, HeyPage, JourneyPage, MonthPage, MonthBar, WeekdayPage, ArtistSequence, ArtistReveal, SongSequence, SongReveal, ReceiptPage, Receipt, ReceiptLine, ReceiptHeading, ReceiptItem, EndPage |
| `ui/screens/search/OnlineSearchResult.kt` | 07 | OnlineSearchResult, TopResultCard, ResultRow |
| `ui/screens/recap/StatsCharts.kt` | 16 | rememberGrowIn, ListeningLine, WeekdayBars, GenreDonut, RankList |
| `ui/screens/recap/RecapVisuals.kt` | 16 | rememberRecapTime, MeshGradient, OdometerText |
| `ui/screens/recap/StatsScreen.kt` | 16 | StatsScreen, StatsCardBox, Chevron, rememberRoll, TotalCard, Fact, GenreCard, ArtistCard, SongCard, WeekdayCard, HourCard, StatsPicker |
| `ui/screens/search/RecentSearches.kt` | 07 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/recap/RecapGenres.kt` | 16 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/search/LocalSearchScreen.kt` | 07 | LocalSearchScreen |
| `ui/screens/search/GenreScreen.kt` | 07 | GenreScreen, GenreHeader, GenreTopBar, GenreItemCard, GenreCommunityCard |
| `ui/screens/search/OnlineSearchScreen.kt` | 07 | OnlineSearchScreen, SuggestionItem |
| `ui/screens/search/suggestions/AppleMusicScraper.kt` | 07 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/search/suggestions/SuggestionRegionSheet.kt` | 07 | SuggestionRegionSheet, RegionListItem |
| `ui/screens/settings/AppBehaviourSettingsScreen.kt` | 16 | AppBehaviourSettingsScreen, Section, larpItems, generalItems, languageItem, localItems, playerBehaviourItems, playerLookItems, videoQualityItem, otherItems, relatedItems, ValueTitle |
| `ui/screens/settings/AppLanguageSheet.kt` | 16 | AppLanguagePicker |
| `ui/screens/search/SearchScreen.kt` | 07 | SearchScreen, SearchBrowseContent, SearchActiveContent, RowGlyph, SuggestionResultRow |
| `ui/screens/search/suggestions/SuggestionsViewModel.kt` | 07 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/settings/CipherSettingsScreen.kt` | 16 | CipherSettingsScreen, CipherInfoRow |
| `ui/screens/search/SearchDesign.kt` | 07 | rememberScrollCollapseProgress, rememberCollapseProgress, searchTopBarHeight, SearchGlassTopBar, SearchAvatar, SearchField, SearchSectionHeader, MoodTile, LayeredCover, SearchCategoryRow, SearchSimpleRow, SearchOverflowDots, SearchRule, SearchArtwork, SearchLoadingDots |
| `ui/screens/search/suggestions/SuggestionModels.kt` | 07 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/settings/CrossmixSettingsScreen.kt` | 16 | CrossmixSettingsScreen, TransitionPicture, InProgressTag |
| `ui/screens/settings/ExperimentalSettingsScreen.kt` | 16 | ExperimentalSettingsScreen |
| `ui/screens/settings/CrossmixTestTools.kt` | 16 | crossmixTestItems |
| `ui/screens/settings/PlaybackDiagnosticsScreen.kt` | 16 | PlaybackDiagnosticsScreen, tint, DiagnosticsRow |
| `ui/screens/settings/MochySettingsScreen.kt` | 16 | MochySettingsScreen, CategoryBubble |
| `ui/screens/settings/ProxySettingsScreen.kt` | 16 | ProxySettingsScreen, AddressDialog, TypeDialog, LoginDialog |
| `ui/screens/settings/ProfileScreen.kt` | 16 | ProfileScreen, figureSp, ListeningTimeCard, TimeValue, VersionCard, VersionCapsule, DeveloperCard, DiscordCard, OpenSettingsRow |
| `ui/screens/settings/VideoQualitySheet.kt` | 16 | videoSizeName, videoQualityName, VideoQualityPicker, ChoiceMark |
| `ui/screens/settings/devspace/DevPersonPicker.kt` | 16 | DevPersonPicker |
| `ui/screens/settings/TransportFadeSection.kt` | 16 | TransportFadeSection, BlendAheadRow, EffectPicker, FadeCurvePicture |
| `ui/screens/settings/devspace/DevProfiles.kt` | 16 | DevAvatar, DevProfilePage, rise, ProfileHeader, Rule, SectionTitle, DevLinkRow |
| `ui/screens/settings/devspace/DevPlaygroundScreen.kt` | 16 | DevPlaygroundScreen |
| `ui/screens/settings/devspace/DevRepoCards.kt` | 16 | DevPanel, PlaygroundCard, RepoFiguresCard, RepoIssuesSection, FilterChips, IssueList, IssueRow, SkeletonRow, Note, RetryNote |
| `ui/screens/settings/LarpModeSheet.kt` | 16 | LarpSheetContent, LarpLevels, LarpPicture |
| `ui/screens/settings/devspace/DevStyle.kt` | 16 | devFigureSp |
| `ui/screens/settings/devspace/DevSpaceData.kt` | 16 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/settings/importsync/AccountImportScreen.kt` | 16 | AccountImportScreen, PickForResyncContent, BusyCard, ChoosingContent |
| `ui/screens/settings/devspace/DeveloperSpaceScreen.kt` | 16 | DeveloperSpaceScreen, BackButton |
| `ui/screens/settings/importsync/ImportProgressScreen.kt` | 16 | ImportProgressScreen, SummaryLine |
| `ui/screens/settings/SettingsPane.kt` | 16 | SettingsListPane, SettingsPanePlaceholder |
| `ui/screens/settings/importsync/CaptureWebView.kt` | 16 | CaptureWebView, CaptureWebContent, WebLoginHeader |
| `ui/screens/settings/importsync/ImportSyncScreen.kt` | 16 | ImportSyncScreen |
| `ui/screens/settings/importsync/TuneMyMusicImportScreen.kt` | 16 | TuneMyMusicImportScreen, Step |
| `ui/screens/settings/importsync/ImportProvidersScreen.kt` | 16 | ImportProvidersScreen |
| `ui/screens/settings/importsync/YouTubeLoginScreen.kt` | 16 | YouTubeLoginScreen, YouTubeLoginWebContent |
| `ui/screens/settings/importsync/YouTubeSyncScreen.kt` | 16 | YouTubeSyncScreen, toggleItem |
| `ui/screens/settings/integrations/IntegrationRow.kt` | 16 | IntegrationPageSubtitle, IntegrationRow |
| `ui/screens/settings/importsync/ImportSyncComponents.kt` | 16 | rememberYouTubeSignedIn, ImportInfoCard, LinkInputDialog, PendingImportHost, ImportReviewDialog |
| `ui/screens/settings/integrations/ScrobbleServiceScreen.kt` | 16 | ScrobbleServiceScreen, ScrobbleAccountGroup |
| `ui/screens/settings/integrations/discord/DiscordPreview.kt` | 16 | DiscordToggleCard, DiscordProfileCard, Banner, Avatar, DiscordActivityPreview, Line, Times, Pictures, PictureView, StatusBadge, SpinningPicture, DiscordFloatingPreview |
| `ui/screens/settings/integrations/discord/DiscordScreen.kt` | 16 | DiscordScreen, AccountButton, CustomizeSection |
| `ui/screens/settings/modules/AddItemsSheet.kt` | 16 | AddItemsContent |
| `ui/screens/settings/integrations/IntegrationsScreen.kt` | 16 | IntegrationsScreen |
| `ui/screens/settings/modules/AddServerSheet.kt` | 16 | AddServerContent |
| `ui/screens/settings/integrations/ScrobbleSignInSheet.kt` | 16 | ScrobbleSignInContent |
| `ui/screens/settings/modules/ModuleCards.kt` | 16 | PageCard, EnableCard, InfoCard, GlassNote, InfoLine, AccountActions, DataSaverCard, PillButton, PageSectionTitle |
| `ui/screens/settings/modules/FindModulesSheet.kt` | 16 | FindModulesContent, ProblemText, CatalogList, CatalogNote, CatalogRow, ConfirmAdd |
| `ui/screens/settings/integrations/discord/DiscordSettingCards.kt` | 16 | DiscordSectionTitle, DiscordCard, DiscordCardTitle, DiscordSwitchCard, FieldLabel, DiscordTextField, DiscordTextCard, DropdownRow, DiscordImageSourceCard, CustomPictureRow, DiscordButtonCard, DiscordStepSlider, DiscordNoticeCard, DiscordPlaceholderCard |
| `ui/screens/settings/modules/ModuleList.kt` | 16 | ModuleList, ModuleCard, DragHandle |
| `ui/screens/settings/modules/ModuleParts.kt` | 16 | ModuleLogo, LetterMark, FeatureChips, featureSummary, moduleStatus, moduleOrigin, StatusDot, ModuleSectionTitle, DashedSlot, PlusSquare |
| `ui/screens/settings/modules/ModuleSettingsPages.kt` | 16 | ModuleSettingsPages |
| `ui/screens/settings/modules/ModuleScreen.kt` | 16 | ModuleScreen, ModuleBanner, ModuleHeader, BackButton, RemoveButton |
| `ui/screens/settings/modules/ModulePageScreen.kt` | 16 | ModulePageScreen, PageBlockView, PageRow |
| `ui/screens/settings/modules/ModuleOptions.kt` | 16 | ModuleOptions, FieldLabel, OptionList, ToggleField, TextField |
| `ui/screens/settings/modules/ModuleTest.kt` | 16 | ModuleTest, SearchPill, ResultRow, StepRow |
| `ui/screens/settings/modules/ModuleSignInSheet.kt` | 16 | ModuleSignInContent |
| `ui/screens/settings/storage/StorageParts.kt` | 10 | StorageCardColumn, UsageBar, LegendSwatch, LegendRow, StepSlider, StorageActionButton |
| `ui/screens/settings/storage/ManageDownloadsSheet.kt` | 10 | ManageDownloadsContent, SortSwitch, DownloadRow |
| `ui/screens/settings/storage/ExportDownloadsSheet.kt` | 10 | ExportSheetContent, ExportStatus, LayoutCard, SwitchRow, RowIcon |
| `ui/screens/settings/storage/TransferSection.kt` | 10 | TransferSection |
| `ui/screens/settings/storage/StoragePlaceSheet.kt` | 10 | PlaceSheetContent, ChooseFolderRow, AccessCard, PlaceIcon, PlaceRow |
| `ui/screens/settings/tvlink/TvLinkScreen.kt` | 16 | TvLinkScreen, SectionLabel, TvLinkHero, TvCard, statusOf, CardButton, LookingDots |
| `ui/screens/settings/storage/StorageScreen.kt` | 10 | StorageScreen, SectionText, headline, OverallCard, BreakdownCard, SmartHint, CacheLimitCard, cacheLimitLabel, ClearButtons, PlaceButtons, moveDetail |
| `ui/screens/setup/SetupWizardContainer.kt` | 16 | SetupWizardContainer |
| `ui/screens/settings/tvlink/TvPairSheet.kt` | 16 | TvPairSheetContent, CodeBoxes |
| `ui/screens/setup/PhotoCropperOverlay.kt` | 16 | PhotoCropperContainer |
| `ui/screens/setup/SetupWizardScreen.kt` | 16 | SetupWizardScreen |
| `ui/screens/setup/onboarding/BlurCapability.kt` | 16 | Auxiliar o declaración no capturada por el patrón |
| `ui/screens/setup/WelcomeStep.kt` | 16 | WelcomeStep, DiscAnimation, GridAnimation |
| `ui/screens/settings/modules/ModulesScreen.kt` | 16 | ModulesScreen, ThirdPartyCard, TwoPhones, ModulesBarBanner |
| `ui/screens/setup/onboarding/OnboardingBrowserSheet.kt` | 16 | OnboardingBrowserSheet, BrowserSheetBar |
| `ui/screens/setup/onboarding/CardMoments.kt` | 16 | HelloCardStep, DoneStep |
| `ui/screens/setup/onboarding/OnboardingPages.kt` | 16 | ImportAskPage, ImportModePage, RestoringPage, ProviderListPage, ProviderSetupPage, WhatToSyncPage, CoverTile, SyncOptionsPage, ProfilePage, BlurWarningPage, LastOnePage |
| `ui/screens/setup/onboarding/ProviderSquircle.kt` | 16 | ProviderSquircle |
| `ui/screens/update/UpdateNotifications.kt` | 16 | UpdateNotificationOpener |
| `ui/screens/update/AboutSheet.kt` | 16 | AboutSheetContent, CreditsSheetContent, LicenseSheetContent, SectionHeader, AboutPillRow, AboutCardButton, PersonRow, AndMoreRow, LicenseCard, ArrowBadge, GithubAvatar |
| `ui/screens/update/UpdatePromptSheet.kt` | 16 | UpdatePromptSheet, PromptButton |
| `ui/screens/update/ChangelogMarkdown.kt` | 16 | ChangelogMarkdown |
| `ui/screens/setup/onboarding/Illustrations.kt` | 16 | CrossfadeBanner |
| `ui/screens/setup/onboarding/OnboardingDesign.kt` | 16 | rememberSidePadding, OnboardingPage, PageBadge, OnbButton, OnbTextField, OnbCard, OnbChoiceCard, OnbToggleCard |
| `ui/screens/update/UpdateSheet.kt` | 16 | rememberUpdateState, SheetPrimaryButton, UpdateSheetContent, ChannelBadge, UpdateStatusCard, StatusRow, ActionRow, UpdateDetailContent, AppIconImage |
| `ui/screens/update/VersionRing.kt` | 16 | VersionRing |
| `ui/screens/setup/onboarding/ProfileIdCard.kt` | 16 | ProfileIdCard, EditHint, MarqueeStrip |
| `ui/tv/TvCards.kt` | 16 | TvItemCard, TvShelf, TvShelfTitle, TvSongRow, TvIconButton, TvPillButton |
| `ui/tv/TvArtistScreen.kt` | 16 | TvArtistScreen, TvReleaseCard |
| `ui/tv/TvActions.kt` | 16 | Auxiliar o declaración no capturada por el patrón |
| `ui/tv/TvCollections.kt` | 16 | TvAlbumScreen, TvOnlinePlaylistScreen, TvLocalPlaylistScreen, TvSongsScreen, TvGridScreen |
| `ui/tv/TvActivity.kt` | 16 | TvRoot |
| `ui/tv/TvHomeScreen.kt` | 16 | TvHomeScreen, TvHeroText, TvHeroDots, rememberHomeRows, TvHomeBackdrop, TvLocalShelf |
| `ui/tv/TvGraph.kt` | 16 | Auxiliar o declaración no capturada por el patrón |
| `ui/tv/TvMotion.kt` | 16 | tvReveal, tvPopIn |
| `ui/tv/TvPhoneScreen.kt` | 16 | TvPhoneScreen, TvPhoneLinked, syncLine, TvPhoneCard, TvPhonePairing, TvPairingSteps |
| `ui/tv/TvKit.kt` | 16 | tvClickable, TvCover, TvBackdropImage |
| `ui/tv/TvPlayerHost.kt` | 16 | TvPlayerHost, TvRemoteLayer, TvPlayingNext, TvQueueMenu, TvControlStrip, TvTransport, TvTransportExtras, TvGlyphButton, TvSeekBar, TvTime |
| `ui/tv/TvLinkToast.kt` | 16 | TvLinkToast |
| `ui/tv/TvProfilePicker.kt` | 16 | TvProfilePicker, TvProfileCircle, TvConfirm |
| `ui/tv/TvRecapAndBrowse.kt` | 16 | TvRecapScreen, TvBrowseScreen |
| `ui/tv/TvChrome.kt` | 16 | TvLogo, TvAvatar, TvNowPlayingPill, TvRail |
| `ui/tv/TvSearchScreen.kt` | 16 | TvSearchScreen, TvSearchField, TvGenreCard, TvSearchResults |
| `ui/tv/TvShell.kt` | 16 | TvShell, TvChromeLayer |
| `ui/tv/TvSongMenu.kt` | 16 | TvSongMenu |
| `ui/tv/TvSongInfo.kt` | 16 | TvSongInfoPanel, TvInfoLabel, TvInfoRow |
| `ui/tv/TvWelcome.kt` | 16 | TvHello, TvWelcome, TvPairWelcome, TvPhoneMock, TvNameEntry |
| `ui/tv/TvTrackListPage.kt` | 16 | TvTrackListPage, TvLedger |
| `ui/tv/TvLibraryScreen.kt` | 16 | TvLibraryScreen, cardsFor, TvLibraryTabLabel, TvLibraryCard |
| `ui/tv/TvMochyScreen.kt` | 16 | TvMochyScreen, TvMochyTileCard |
| `ui/screens/settings/modules/ModuleLists.kt` | 16 | ModuleServers, ServerRow, ModuleItems, ItemRow, ListCard, ModulePages |
