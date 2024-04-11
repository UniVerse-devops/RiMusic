package com.universe.audioflare.ui.screens.localplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import com.universe.compose.persist.PersistMapCleanup
import com.universe.compose.routing.RouteHandler
import com.universe.audioflare.R
import com.universe.audioflare.ui.components.Scaffold
import com.universe.audioflare.ui.screens.globalRoutes
import com.universe.audioflare.ui.screens.searchRoute
import com.universe.audioflare.utils.rememberPreference
import com.universe.audioflare.utils.showSearchTabKey

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun LocalPlaylistScreen(playlistId: Long) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val showSearchTab by rememberPreference(showSearchTabKey, false)
    PersistMapCleanup(tagPrefix = "localPlaylist/$playlistId/")

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()

        host {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                topIconButton2Id = R.drawable.chevron_back,
                onTopIconButton2Click = pop,
                showButton2 = false,
                //showBottomButton = showSearchTab,
                onBottomIconButtonClick = { searchRoute("") },
                tabIndex = 0,
                onTabChanged = { },
                tabColumnContent = { Item ->
                    Item(0, stringResource(R.string.songs), R.drawable.musical_notes)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> LocalPlaylistSongs(
                            playlistId = playlistId,
                            onDelete = pop
                        )
                    }
                }
            }
        }
    }
}
