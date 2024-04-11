package com.universe.audioflare.ui.screens.ondevice

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import com.universe.compose.persist.PersistMapCleanup
import com.universe.compose.routing.RouteHandler
import com.universe.audioflare.Database
import com.universe.audioflare.R
import com.universe.audioflare.enums.BuiltInPlaylist
import com.universe.audioflare.enums.DeviceLists
import com.universe.audioflare.enums.ExoPlayerDiskCacheMaxSize
import com.universe.audioflare.enums.ExoPlayerDiskDownloadCacheMaxSize
import com.universe.audioflare.enums.MaxTopPlaylistItems
import com.universe.audioflare.models.SearchQuery
import com.universe.audioflare.query
import com.universe.audioflare.ui.components.Scaffold
import com.universe.audioflare.ui.screens.builtinplaylist.BuiltInPlaylistSongs
import com.universe.audioflare.ui.screens.globalRoutes
import com.universe.audioflare.ui.screens.search.SearchScreen
import com.universe.audioflare.ui.screens.searchResultRoute
import com.universe.audioflare.ui.screens.searchRoute
import com.universe.audioflare.ui.screens.searchresult.SearchResultScreen
import com.universe.audioflare.utils.MaxTopPlaylistItemsKey
import com.universe.audioflare.utils.exoPlayerDiskCacheMaxSizeKey
import com.universe.audioflare.utils.exoPlayerDiskDownloadCacheMaxSizeKey
import com.universe.audioflare.utils.pauseSearchHistoryKey
import com.universe.audioflare.utils.preferences
import com.universe.audioflare.utils.rememberPreference
import com.universe.audioflare.utils.showSearchTabKey

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun DeviceListSongsScreen(deviceLists: DeviceLists) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabIndexChanged) = rememberSaveable {
        mutableStateOf(when (deviceLists) {
            DeviceLists.LocalSongs -> 4
        })
    }
    val showSearchTab by rememberPreference(showSearchTabKey, false)

    var exoPlayerDiskCacheMaxSize by rememberPreference(
        exoPlayerDiskCacheMaxSizeKey,
        ExoPlayerDiskCacheMaxSize.`32MB`
    )

    var exoPlayerDiskDownloadCacheMaxSize by rememberPreference(
        exoPlayerDiskDownloadCacheMaxSizeKey,
        ExoPlayerDiskDownloadCacheMaxSize.`2GB`
    )

    val maxTopPlaylistItems by rememberPreference(
        MaxTopPlaylistItemsKey,
        MaxTopPlaylistItems.`10`
    )

    PersistMapCleanup(tagPrefix = "${deviceLists.name}/")

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()
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
                onViewPlaylist = {}
            )
        }

        host {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                topIconButton2Id = R.drawable.chevron_back,
                onTopIconButton2Click = pop,
                showButton2 = false,
                showBottomButton = showSearchTab,
                onBottomIconButtonClick = { searchRoute("") },
                tabIndex = tabIndex,
                onTabChanged = onTabIndexChanged,
                tabColumnContent = { Item ->
                    Item(0, stringResource(R.string.favorites), R.drawable.heart)
                    if(exoPlayerDiskCacheMaxSize != ExoPlayerDiskCacheMaxSize.Disabled)
                        Item(1, stringResource(R.string.cached), R.drawable.sync)
                    if(exoPlayerDiskDownloadCacheMaxSize != ExoPlayerDiskDownloadCacheMaxSize.Disabled)
                        Item(2, stringResource(R.string.downloaded), R.drawable.downloaded)
                    Item(3, stringResource(R.string.my_playlist_top)  + " ${maxTopPlaylistItems.number}" , R.drawable.trending)
                    Item(4, stringResource(R.string.on_device), R.drawable.musical_notes)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> BuiltInPlaylistSongs(
                            builtInPlaylist = BuiltInPlaylist.Favorites,
                            onSearchClick = { searchRoute("") }
                        )
                        1 -> BuiltInPlaylistSongs(
                            builtInPlaylist = BuiltInPlaylist.Offline,
                            onSearchClick = { searchRoute("") }
                        )
                        2 -> BuiltInPlaylistSongs(
                            builtInPlaylist = BuiltInPlaylist.Downloaded,
                            onSearchClick = { searchRoute("") }
                        )
                        3 -> BuiltInPlaylistSongs(
                            builtInPlaylist = BuiltInPlaylist.Top,
                            onSearchClick = { searchRoute("") }
                        )
                        4 -> DeviceListSongs(
                            deviceLists = DeviceLists.LocalSongs,
                            onSearchClick = { searchRoute("") }
                        )

                    }
                }
            }
        }
    }
}
