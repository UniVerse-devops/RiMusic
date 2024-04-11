package com.universe.audioflare.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import com.universe.compose.persist.PersistMapCleanup
import com.universe.compose.routing.RouteHandler
import com.universe.compose.routing.defaultStacking
import com.universe.compose.routing.defaultStill
import com.universe.compose.routing.defaultUnstacking
import com.universe.compose.routing.isStacking
import com.universe.compose.routing.isUnknown
import com.universe.compose.routing.isUnstacking
import com.universe.audioflare.Database
import com.universe.audioflare.R
import com.universe.audioflare.enums.CheckUpdateState
import com.universe.audioflare.enums.HomeScreenTabs
import com.universe.audioflare.enums.StatisticsType
import com.universe.audioflare.models.SearchQuery
import com.universe.audioflare.models.toUiMood
import com.universe.audioflare.query
import com.universe.audioflare.ui.components.themed.ConfirmationDialog
import com.universe.audioflare.ui.components.Scaffold
import com.universe.audioflare.ui.screens.albumRoute
import com.universe.audioflare.ui.screens.artistRoute
import com.universe.audioflare.ui.screens.builtInPlaylistRoute
import com.universe.audioflare.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import com.universe.audioflare.ui.screens.deviceListSongRoute
import com.universe.audioflare.ui.screens.globalRoutes
import com.universe.audioflare.ui.screens.homeRoute
import com.universe.audioflare.ui.screens.localPlaylistRoute
import com.universe.audioflare.ui.screens.localplaylist.LocalPlaylistScreen
import com.universe.audioflare.ui.screens.moodRoute
import com.universe.audioflare.ui.screens.playlist.PlaylistScreen
import com.universe.audioflare.ui.screens.playlistRoute
import com.universe.audioflare.ui.screens.search.SearchScreen
import com.universe.audioflare.ui.screens.searchResultRoute
import com.universe.audioflare.ui.screens.searchRoute
import com.universe.audioflare.ui.screens.searchresult.SearchResultScreen
import com.universe.audioflare.ui.screens.settings.SettingsScreen
import com.universe.audioflare.ui.screens.settingsRoute
import com.universe.audioflare.ui.screens.statisticsTypeRoute
import com.universe.audioflare.utils.CheckAvailableNewVersion
import com.universe.audioflare.utils.checkUpdateStateKey
import com.universe.audioflare.utils.getEnum
import com.universe.audioflare.utils.homeScreenTabIndexKey
import com.universe.audioflare.utils.indexNavigationTabKey
import com.universe.audioflare.utils.pauseSearchHistoryKey
import com.universe.audioflare.utils.preferences
import com.universe.audioflare.utils.rememberPreference
import com.universe.audioflare.utils.showSearchTabKey

const val PINNED_PREFIX = "pinned:"

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun HomeScreen(
    onPlaylistUrl: (String) -> Unit,
    openTabFromShortcut: Int
) {
    var showNewversionDialog by remember {
        mutableStateOf(true)
    }

    var checkUpdateState by rememberPreference(checkUpdateStateKey, CheckUpdateState.Disabled)

    val saveableStateHolder = rememberSaveableStateHolder()

    val preferences = LocalContext.current.preferences
    val showSearchTab by rememberPreference(showSearchTabKey, false)

    PersistMapCleanup("home/")

    RouteHandler(
        listenToGlobalEmitter = true,
        transitionSpec = {
            when {
                isStacking -> defaultStacking
                isUnstacking -> defaultUnstacking
                isUnknown -> when {
                    initialState.route == searchRoute && targetState.route == searchResultRoute -> defaultStacking
                    initialState.route == searchResultRoute && targetState.route == searchRoute -> defaultUnstacking
                    else -> defaultStill
                }

                else -> defaultStill
            }
        }
    ) {
        globalRoutes()

        settingsRoute {
            SettingsScreen()
        }

        localPlaylistRoute { playlistId ->
            LocalPlaylistScreen(
                playlistId = playlistId ?: error("playlistId cannot be null")
            )
        }

        builtInPlaylistRoute { builtInPlaylist ->
            BuiltInPlaylistScreen(
                builtInPlaylist = builtInPlaylist
            )
        }

        playlistRoute { browseId, params, maxDepth ->
            PlaylistScreen(
                browseId = browseId ?: error("browseId cannot be null"),
                params = params,
                maxDepth = maxDepth
            )
        }

        /*
        playlistRoute { browseId, params ->
            PlaylistScreen(
                browseId = browseId ?: "",
                params = params
            )
        }
         */

        searchResultRoute { query ->
            SearchResultScreen(
                query = query,
                onSearchAgain = {
                    searchRoute(query)
                }
            )
        }

        searchRoute { initialTextInput ->
            val context = LocalContext.current

            SearchScreen(
                initialTextInput = initialTextInput,
                onSearch = { query ->
                    pop()
                    searchResultRoute(query)

                    if (!context.preferences.getBoolean(pauseSearchHistoryKey, false)) {
                        query {
                            Database.insert(SearchQuery(query = query))
                        }
                    }
                },
                onViewPlaylist = onPlaylistUrl,
                onDismiss = { homeRoute::global }
            )
        }

        host {

            var (tabIndex, onTabChanged) =
                when (openTabFromShortcut) {
                    -1 -> when (preferences.getEnum(indexNavigationTabKey, HomeScreenTabs.Default)) {
                            HomeScreenTabs.Default -> rememberPreference(homeScreenTabIndexKey,
                            HomeScreenTabs.QuickPics.index)
                          else -> remember {
                                mutableStateOf(preferences.getEnum(indexNavigationTabKey, HomeScreenTabs.QuickPics).index)
                          }
                        }
                    else -> remember { mutableStateOf(openTabFromShortcut) }
                }

            /*
            var (tabIndex, onTabChanged) =
                if (preferences.getEnum(indexNavigationTabKey, HomeScreenTabs.Default) == HomeScreenTabs.Default)
                    rememberPreference(
                        homeScreenTabIndexKey,
                        HomeScreenTabs.QuickPics.index
                    ) else
                    remember {
                        mutableStateOf(preferences.getEnum(indexNavigationTabKey, HomeScreenTabs.QuickPics).index)
                    }

             */

            Scaffold(
                topIconButtonId = R.drawable.settings,
                onTopIconButtonClick = { settingsRoute() },
                topIconButton2Id = R.drawable.stats_chart,
                onTopIconButton2Click = { statisticsTypeRoute(StatisticsType.Today) },
                showButton2 = false,
                showBottomButton = showSearchTab,
                onBottomIconButtonClick = { searchRoute("") },
                tabIndex = tabIndex,
                onTabChanged = onTabChanged,
                tabColumnContent = { Item ->
                    Item(0, stringResource(R.string.quick_picks), R.drawable.sparkles)
                    Item(1, stringResource(R.string.songs), R.drawable.musical_notes)
                    Item(2, stringResource(R.string.artists), R.drawable.artists)
                    Item(3, stringResource(R.string.albums), R.drawable.album)
                    Item(4, stringResource(R.string.library), R.drawable.library)
                    //Item(5, stringResource(R.string.discovery), R.drawable.megaphone)
                    //if (showSearchTab)
                    //Item(6, stringResource(R.string.search), R.drawable.search)
                    //Item(6, "Equalizer", R.drawable.musical_notes)
                    //Item(6, "Settings", R.drawable.equalizer)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> QuickPicks(
                            onAlbumClick = { albumRoute(it) },
                            onArtistClick = { artistRoute(it) },
                            onPlaylistClick = { playlistRoute(it) },
                            onSearchClick = { searchRoute("") },
                            onMoodClick = { mood -> moodRoute(mood.toUiMood()) },
                        )

                        1 -> HomeSongs(
                            onSearchClick = { searchRoute("") }
                        )

                        2 -> HomeArtistList(
                            onArtistClick = { artistRoute(it.id) },
                            onSearchClick = { searchRoute("") }
                        )

                        3 -> HomeAlbums(
                            onAlbumClick = { albumRoute(it.id) },
                            onSearchClick = { searchRoute("") }
                        )

                        4 -> HomeLibrary(
                            onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                            onPlaylistClick = { localPlaylistRoute(it.id) },
                            onSearchClick = { searchRoute("") },
                            onDeviceListSongsClick = { deviceListSongRoute("") },
                            onStatisticsClick = { statisticsTypeRoute(StatisticsType.Today) }

                        )

                        5 -> HomeDiscovery(
                            onMoodClick = { mood -> moodRoute(mood.toUiMood()) },
                            onNewReleaseAlbumClick = { albumRoute(it) },
                            onSearchClick = { searchRoute("") }
                        )

                        //6 -> HomeEqualizer( )
                        /*
                        5 -> HomeStatistics(
                            onStatisticsType = { statisticsTypeRoute(it)},
                            onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                            onPlaylistClick = { localPlaylistRoute(it.id) },
                            onSearchClick = { searchRoute("") }
                        )
                         */

                        /*
                        6 -> HomeSearch(
                            onSearchType = { searchTypeRoute(it) }
                        )
                         */
                    }
                }
            }
        }
    }

    if (showNewversionDialog && checkUpdateState == CheckUpdateState.Enabled)
        CheckAvailableNewVersion(
            onDismiss = { showNewversionDialog = false }
        )

    if (checkUpdateState == CheckUpdateState.Ask)
        ConfirmationDialog(
            text = stringResource(R.string.check_at_github_for_updates) + "\n\n" +
                    stringResource(R.string.when_an_update_is_available_you_will_be_asked_if_you_want_to_install_info) + "\n\n" +
                    stringResource(R.string.but_these_updates_would_not_go_through) + "\n\n" +
                    stringResource(R.string.you_can_still_turn_it_on_or_off_from_the_settings),
            confirmText = stringResource(R.string.enable),
            cancelText = stringResource(R.string.don_t_enable),
            cancelBackgroundPrimary = true,
            onCancel = { checkUpdateState = CheckUpdateState.Disabled },
            onDismiss = { checkUpdateState = CheckUpdateState.Disabled },
            onConfirm = { checkUpdateState = CheckUpdateState.Enabled },
        )
    
}
