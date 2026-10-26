# Atlas de pantallas GalaxOT

Rama: `opentune-rebuild-v1` · Inventario verificado: 20 de octubre de 2026.

El catálogo contiene **los 98 archivos Kotlin de `ui/screens/`**, tanto pantallas navegables como subcomponentes. Un guion significa que el archivo no tiene una ruta propia en `NavigationBuilder.kt`; puede ser visible a través de una pantalla principal. No todos los `Screen.kt` son entradas independientes.

## Encuentra rápidamente la pantalla que quieres editar

| Interfaz | Archivo principal |
| --- | --- |
| Inicio | `ui/screens/HomeScreen.kt`, `ui/screens/GalaxMainPages.kt` |
| Buscar | `ui/screens/GalaxMainPages.kt`, `ui/screens/search/GalaxSearchExperience.kt`, `ui/screens/search/UniversalSearchScreen.kt` |
| Biblioteca | `ui/screens/library/LibraryScreen.kt`, `ui/screens/library/GalaxArchiveDesign.kt` |
| Ajustes | `ui/screens/settings/SettingsScreen.kt`, `ui/screens/settings/SettingsLayout.kt` |
| Apariencia | `ui/screens/settings/AppearanceSettings.kt` |
| Liquid Glass, solo controles de preferencias | `ui/screens/settings/GalaxLiquidSettingsScreen.kt` |
| Los 17 reproductores | `ui/player/GalaxStudioPlayer/` y `ui/player/Player.kt` |
| Estudio de reproductores | `ui/screens/settings/GalaxPlayerStyleStudioScreen.kt` |
| Cola/Descargas | `ui/player/Queue.kt`, `ui/screens/DownloadQueueScreen.kt` |
| Letras | `ui/player/LyricsScreen.kt` |

**Raíz para todos los archivos de las tablas:** `app/src/main/kotlin/com/arturo254/opentune/ui/screens/`.

## Pantallas y auxiliares principales (31)

| Archivo relativo | Qué controla | Ruta en la app |
| --- | --- | --- |
| `AccountScreen.kt` | Account Screen | `account` |
| `AlbumScreen.kt` | Album Screen | `album/{albumId}` |
| `AlwaysOnDisplayScreen.kt` | Always On Display Screen | `always_on_display` |
| `BrowseScreen.kt` | Browse Screen | `browse/{browseId}` |
| `ChartsScreen.kt` | Charts Screen | `charts_screen` |
| `DownloadQueueScreen.kt` | Download Queue Screen | `download_queue` |
| `ExploreScreen.kt` | Exploración alternativa/auxiliar | `—` |
| `GalaxDiscoverExperience.kt` | Descubrimiento galáctico | `—` |
| `GalaxFeaturedTrack.kt` | Canción destacada | `—` |
| `GalaxHomeActions.kt` | Acciones de Inicio | `—` |
| `GalaxHomeCatalogControl.kt` | Expansión de catálogos | `—` |
| `GalaxHomeExperience.kt` | Mosaicos destacados de Inicio | `—` |
| `GalaxHomeZones.kt` | Secciones editoriales de Inicio | `—` |
| `GalaxListeningDeck.kt` | Panel de escucha | `—` |
| `GalaxDiscoverEditorial.kt` | Nuevo diseño inmersivo de descubrimiento | `search` |
| `GalaxEditorialAtmosphere.kt` | Capas estelares y materiales compartidos | `—` |
| `GalaxHomeEditorial.kt` | Nueva composición editorial de Inicio | `home` |
| `library/GalaxArchiveDesign.kt` | Nueva biblioteca: portada, tarjetas y listas | `library` |
| `search/GalaxUniversalResultCard.kt` | Nueva tarjeta de resultados multiproveedor | `search/{query}` |
| `GalaxAetherHome.kt` | Portadas, disco y filas de Inicio | `home` |
| `GalaxAetherVisuals.kt` | Estrellas, ondas, cristal y movimiento | `home` |
| `GalaxAtlasDiscovery.kt` | Componentes propios del atlas musical | `search` |
| `GalaxAtlasScreen.kt` | Nueva pestaña de descubrimiento y géneros | `search` |
| `GalaxAetherCatalog.kt` | Nuevas colecciones editoriales con navegación y menús de YouTube Music | `home` |
| `GalaxCollectionTileCompat.kt` | API conservada para pantallas secundarias de la colección | `—` |
| `search/GalaxAtlasResultsFrame.kt` | Carga, avisos y métricas visuales de resultados | `search/{query}` |
| `GalaxMainPages.kt` | Contenedor de tres pestañas | `home · search · library` |
| `GalaxMediaHeader.kt` | Cabecera de álbum/artista/playlist | `—` |
| `GalaxRemoteShelf.kt` | Estantes remotos | `—` |
| `GalaxScaffold.kt` | Estructura compartida | `—` |
| `GalaxSourceDiscoveryCard.kt` | Tarjeta de fuentes | `—` |
| `GalaxTopAppBar.kt` | Barra superior reutilizable | `—` |
| `HistoryScreen.kt` | History Screen | `history` |
| `HomeScreen.kt` | Inicio y recomendaciones | `home` |
| `HomeScreenComponents.kt` | Tarjetas comunes de Inicio | `—` |
| `LoginScreen.kt` | Login Screen | `login?url={url}` |
| `MoodAndGenresScreen.kt` | Mood And Genres Screen | `mood_and_genres` |
| `NavigationBuilder.kt` | Registro de rutas | `—` |
| `NewReleaseScreen.kt` | New Release Screen | `new_release` |
| `Screens.kt` | Declaraciones de pestañas | `—` |
| `StatsScreen.kt` | Stats Screen | `stats` |
| `YearInMusicScreen.kt` | Year In Music Screen | `year_in_music` |
| `YouTubeBrowseScreen.kt` | You Tube Browse Screen | `youtube_browse/{browseId}?params={params}` |

## Artistas (4)

| Archivo relativo | Qué controla | Ruta en la app |
| --- | --- | --- |
| `artist/ArtistAlbumsScreen.kt` | Artist Albums Screen | `artist/{artistId}/albums` |
| `artist/ArtistItemsScreen.kt` | Artist Items Screen | `artist/{artistId}/items?browseId={browseId}&params={params}` |
| `artist/ArtistScreen.kt` | Artist Screen | `artist/{artistId}` |
| `artist/ArtistSongsScreen.kt` | Artist Songs Screen | `artist/{artistId}/songs` |

## Descargas (1)

| Archivo relativo | Qué controla | Ruta en la app |
| --- | --- | --- |
| `downloads/GalaxDownloadOverview.kt` | Resumen y filtros de descargas | `—` |

## Biblioteca (11)

| Archivo relativo | Qué controla | Ruta en la app |
| --- | --- | --- |
| `library/GalaxLibraryChapter.kt` | Capítulos de Biblioteca | `—` |
| `library/GalaxLibraryDestinationHero.kt` | Cabecera de categoría de biblioteca | `—` |
| `library/GalaxLibraryOrbitNavigator.kt` | Accesos rápidos de Biblioteca | `—` |
| `library/LibraryAlbumsScreen.kt` | Álbumes guardados | `library (álbumes)` |
| `library/LibraryArtistsScreen.kt` | Artistas guardados | `library (artistas)` |
| `library/LibraryMixScreen.kt` | Biblioteca general | `library (general)` |
| `library/LibraryOrdering.kt` | Ordenamiento de Biblioteca | `—` |
| `library/LibraryPlaylistsScreen.kt` | Playlists guardadas | `library (playlists)` |
| `library/LibraryScreen.kt` | Contenedor de Biblioteca | `library` |
| `library/LibrarySongsScreen.kt` | Canciones guardadas | `library (canciones)` |
| `library/LibrarySpotifyPlaylistsScreen.kt` | Playlists Spotify | `library (Spotify)` |

## Reconocimiento (1)

| Archivo relativo | Qué controla | Ruta en la app |
| --- | --- | --- |
| `musicrecognition/MusicRecognitionScreen.kt` | Reconocimiento musical | `MusicRecognitionRoute` |

## Playlists (7)

| Archivo relativo | Qué controla | Ruta en la app |
| --- | --- | --- |
| `playlist/AutoPlaylistScreen.kt` | Auto Playlist Screen | `auto_playlist/{playlist}` |
| `playlist/CachePlaylistScreen.kt` | Cache Playlist Screen | `cache_playlist/{playlist}` |
| `playlist/LocalPlaylistScreen.kt` | Local Playlist Screen | `local_playlist/{playlistId}` |
| `playlist/OnlinePlaylistScreen.kt` | Online Playlist Screen | `online_playlist/{playlistId}` |
| `playlist/PlaylistSuggestionSection.kt` | Recomendaciones en playlists | `—` |
| `playlist/SpotifyPlaylistScreen.kt` | Spotify Playlist Screen | `spotify_playlist/{playlistId}` |
| `playlist/TopPlaylistScreen.kt` | Top Playlist Screen | `top_playlist/{top}` |

## Búsqueda (4)

| Archivo relativo | Qué controla | Ruta en la app |
| --- | --- | --- |
| `search/GalaxQualitySelectionHeader.kt` | Cabecera del selector de calidad | `—` |
| `search/GalaxSearchExperience.kt` | Búsqueda en pestaña principal | `search (pestaña)` |
| `search/UniversalSearchScreen.kt` | Resultados universales multiproveedor | `search/{query}` |
| `search/UniversalVersionQualityDialog.kt` | Diálogo de versiones y calidad | `diálogo interno` |

## Ajustes, apariencia y pantallas secundarias (39)

| Archivo relativo | Qué controla | Ruta en la app |
| --- | --- | --- |
| `settings/AboutScreen.kt` | About Screen | `settings/about` |
| `settings/AccountSettings.kt` | Componentes avanzados de cuenta | `—` |
| `settings/AlwaysOnDisplaySettings.kt` | Always On Display Settings | `settings/appearance/always_on_display` |
| `settings/AndroidAutoSettings.kt` | Android Auto Settings | `settings/android_auto` |
| `settings/AppearanceSettings.kt` | Colores, animaciones, Liquid Glass y fuentes | `settings/appearance` |
| `settings/BackupAndRestore.kt` | Backup And Restore | `settings/backup_restore` |
| `settings/ChangelogScreen.kt` | Changelog Screen | `settings/changelog` |
| `settings/ContentSettings.kt` | Idioma, contenido, proveedores | `settings/content` |
| `settings/CustomizeBackground.kt` | Customize Background | `customize_background` |
| `settings/DebugSettings.kt` | Debug Settings | `settings/misc` |
| `settings/DiscordExperimental.kt` | Discord Experimental | `settings/discord/experimental` |
| `settings/DiscordLoginScreen.kt` | Discord Login Screen | `settings/discord/login` |
| `settings/DiscordPresenceManager.kt` | Gestión de estado Discord (no pantalla) | `—` |
| `settings/DiscordSettings.kt` | Discord Settings | `settings/discord` |
| `settings/GalaxLiquidSettingsScreen.kt` | Controles de los cinco modos Liquid Glass | `settings/appearance/galax_liquid` |
| `settings/GalaxPlayerStyleStudioScreen.kt` | Estudio de 17 diseños de reproductor | `settings/appearance/player_studio` |
| `settings/GalaxSettingsCreativeShortcuts.kt` | Atajos a Estudio, Liquid Glass y Paleta desde Ajustes | `settings` (tarjetas integradas) |
| `settings/GalaxSettingsSectionHero.kt` | Cabeceras editoriales de seis secciones de Ajustes | `cabecera reutilizable` |
| `settings/IntegrationScreen.kt` | Conexiones y scrobbling | `settings/integration` |
| `settings/LastFMSettings.kt` | Last FMSettings | `settings/lastfm` |
| `settings/ListenBrainzManager.kt` | Gestión de ListenBrainz (no pantalla) | `—` |
| `settings/MusicTogetherScreen.kt` | Music Together Screen | `settings/music_together` |
| `settings/PalettePickerScreen.kt` | Palette Picker Screen | `settings/appearance/palette_picker` |
| `settings/PlayerSettings.kt` | Ajustes de audio y reproducción | `settings/player` |
| `settings/PoTokenExtractionActivity.kt` | Actividad interna PO Token | `Activity interna` |
| `settings/PoTokenScreen.kt` | Po Token Screen | `settings/po_token` |
| `settings/PrivacySettings.kt` | Historial y privacidad | `settings/privacy` |
| `settings/SettingsComponents.kt` | Componentes de Ajustes | `—` |
| `settings/SettingsDataBuilders.kt` | Menú y destinos de Ajustes | `datos de navegación de Ajustes` |
| `settings/SettingsDimensions.kt` | Dimensiones y animaciones | `—` |
| `settings/SettingsIntegrations.kt` | Tarjetas de integraciones | `—` |
| `settings/SettingsLayout.kt` | Columnas adaptables de Ajustes | `—` |
| `settings/SettingsModels.kt` | Modelos de tarjetas | `—` |
| `settings/SettingsQuickActions.kt` | Accesos rápidos | `—` |
| `settings/SettingsScreen.kt` | Portada de Ajustes | `settings` |
| `settings/SettingsSearch.kt` | Filtro/búsqueda de Ajustes | `—` |
| `settings/StorageSettings.kt` | Cachés, almacenamiento y descargas | `settings/storage` |
| `settings/ThemeCreatorScreen.kt` | Theme Creator Screen | `settings/appearance/theme_creator` |
| `settings/UpdateScreen.kt` | Update Screen | `settings/update` |

## Pantallas y menús adicionales fuera de `ui/screens/`

Raíz: `app/src/main/kotlin/com/arturo254/opentune/`.

| Ruta Kotlin | Para qué sirve |
| --- | --- |
| `ui/player/Player.kt` | Host del reproductor y conexión con controles |
| `ui/player/MiniPlayer.kt` | Minirreproductor |
| `ui/player/Queue.kt` | Cola, lista y gestos de reordenar |
| `ui/player/LyricsScreen.kt` | Letras y controles sincronizados |
| `ui/player/Thumbnail.kt` | Portada base y presentación de artwork |
| `ui/player/CanvasArtworkPlayer.kt` | Portada animada/Canvas |
| `ui/player/GalaxStudioPlayer/StudioThemeV1.kt` … `StudioThemeV17VinJax.kt` | Archivos individuales de 17 estilos |
| `ui/player/layout/PlayerLayout.kt` | Enrutador de estilos de reproductor |
| `ui/component/WidgetSettings.kt` | Ajustes de widget (ruta `settings/widget`) |
| `ui/menu/PlayerMenu.kt` | Menú del reproductor y ecualizador |
| `ui/menu/SongMenu.kt` | Menú de canción |
| `ui/menu/AlbumMenu.kt` | Menú de álbum |
| `ui/menu/ArtistMenu.kt` | Menú de artista |
| `ui/menu/PlaylistMenu.kt` | Menú de playlist |
| `ui/menu/LyricsMenu.kt` | Menú de letras |
| `ui/menu/YouTubeSongMenu.kt` | Menú de canción YouTube |
| `ui/menu/YouTubeAlbumMenu.kt` | Menú de álbum YouTube |
| `ui/menu/YouTubeArtistMenu.kt` | Menú de artista YouTube |
| `ui/menu/YouTubePlaylistMenu.kt` | Menú de playlist YouTube |
| `ui/menu/SelectionSongsMenu.kt` | Menú de selección múltiple |
| `ui/menu/AddToPlaylistDialog.kt` | Diálogo para añadir a playlist local |
| `ui/menu/AddToPlaylistDialogOnline.kt` | Diálogo para playlist remota |
| `ui/menu/ImportPlaylistDialog.kt` | Diálogo de importación |
| `ui/component/BottomSheetMenu.kt` | Hoja inferior de menús |
| `MainActivity.kt` | Ventana, tema y contenedor de la app |

## Reglas para personalizar

- `Screens.kt` define Inicio, Buscar y Biblioteca. `NavigationBuilder.kt` define las rutas restantes. No alteres esas rutas al cambiar diseños.
- `SettingsDataBuilders.kt` administra accesos y categorías. Cada archivo `*Settings.kt` conserva las preferencias reales.
- Los archivos `ui/screens/settings/GalaxSettingsSectionHero.kt` y `SettingsLayout.kt` configuran el diseño editorial de la Parte 7.
- Los archivos `core/designsystem/liquid/` constituyen el motor Liquid Glass; están fuera del alcance de rediseños normales y no deben tocarse para cambiar una pantalla.
- Algunas pantallas se abren desde filtros, hojas o diálogos; que no tengan ruta propia no implica que sean código muerto.


`settings/AsterSettingsDesign.kt` — Sistema Aster para las 27 pantallas de ajustes, controles, cabeceras y entorno visual.
