package com.universe.audioflare.ui.screens.home

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.universe.compose.persist.persistList
import com.universe.audioflare.Database
import com.universe.audioflare.LocalPlayerAwareWindowInsets
import com.universe.audioflare.R
import com.universe.audioflare.enums.BuiltInPlaylist
import com.universe.audioflare.enums.MaxTopPlaylistItems
import com.universe.audioflare.enums.NavigationBarPosition
import com.universe.audioflare.enums.PlaylistSortBy
import com.universe.audioflare.enums.SortOrder
import com.universe.audioflare.enums.StatisticsType
import com.universe.audioflare.enums.UiType
import com.universe.audioflare.models.Playlist
import com.universe.audioflare.models.PlaylistPreview
import com.universe.audioflare.models.Song
import com.universe.audioflare.models.SongPlaylistMap
import com.universe.audioflare.query
import com.universe.audioflare.transaction
import com.universe.audioflare.ui.components.LocalMenuState
import com.universe.audioflare.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.universe.audioflare.ui.components.themed.Header
import com.universe.audioflare.ui.components.themed.HeaderIconButton
import com.universe.audioflare.ui.components.themed.HeaderWithIcon
import com.universe.audioflare.ui.components.themed.IconButton
import com.universe.audioflare.ui.components.themed.InputTextDialog
import com.universe.audioflare.ui.components.themed.SortMenu
import com.universe.audioflare.ui.items.PlaylistItem
import com.universe.audioflare.ui.screens.globalRoutes
import com.universe.audioflare.ui.screens.statistics.StatisticsPage
import com.universe.audioflare.ui.screens.statisticsTypeRoute
import com.universe.audioflare.ui.styling.Dimensions
import com.universe.audioflare.ui.styling.LocalAppearance
import com.universe.audioflare.ui.styling.favoritesIcon
import com.universe.audioflare.ui.styling.px
import com.universe.audioflare.utils.MaxTopPlaylistItemsKey
import com.universe.audioflare.utils.UiTypeKey
import com.universe.audioflare.utils.contentWidthKey
import com.universe.audioflare.utils.navigationBarPositionKey
import com.universe.audioflare.utils.playlistSortByKey
import com.universe.audioflare.utils.playlistSortOrderKey
import com.universe.audioflare.utils.preferences
import com.universe.audioflare.utils.rememberPreference
import com.universe.audioflare.utils.semiBold
import com.universe.audioflare.utils.showCachedPlaylistKey
import com.universe.audioflare.utils.showDownloadedPlaylistKey
import com.universe.audioflare.utils.showFavoritesPlaylistKey
import com.universe.audioflare.utils.showMyTopPlaylistKey
import com.universe.audioflare.utils.showOnDevicePlaylistKey
import com.universe.audioflare.utils.showPlaylistsKey
import com.universe.audioflare.utils.showSearchTabKey
import com.universe.audioflare.utils.toast


@ExperimentalMaterialApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomeLibrary(
    onBuiltInPlaylist: (BuiltInPlaylist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDeviceListSongsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onStatisticsClick: () -> Unit
) {
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val menuState = LocalMenuState.current
    val uiType  by rememberPreference(UiTypeKey, UiType.audioflare)

    var isCreatingANewPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

/*
    var exoPlayerDiskCacheMaxSize by rememberPreference(
        exoPlayerDiskCacheMaxSizeKey,
        ExoPlayerDiskCacheMaxSize.`32MB`
    )

    var exoPlayerDiskDownloadCacheMaxSize by rememberPreference(
        exoPlayerDiskDownloadCacheMaxSizeKey,
        ExoPlayerDiskDownloadCacheMaxSize.`2GB`
    )

 */

    if (isCreatingANewPlaylist) {
        InputTextDialog(
            onDismiss = { isCreatingANewPlaylist = false },
            title = stringResource(R.string.enter_the_playlist_name),
            value = "",
            placeholder = stringResource(R.string.enter_the_playlist_name),
            setValue = { text ->
                query {
                    Database.insert(Playlist(name = text))
                }
            }
        )
    }

    var sortBy by rememberPreference(playlistSortByKey, PlaylistSortBy.DateAdded)
    var sortOrder by rememberPreference(playlistSortOrderKey, SortOrder.Descending)

    var items by persistList<PlaylistPreview>("home/playlists")

    LaunchedEffect(sortBy, sortOrder) {
        Database.playlistPreviews(sortBy, sortOrder).collect { items = it }
    }

    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing), label = ""
    )

    val thumbnailSizeDp = 108.dp
    val thumbnailSizePx = thumbnailSizeDp.px

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    var plistId by remember {
        mutableStateOf(0L)
    }
    val context = LocalContext.current
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            //requestPermission(activity, "Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED")

            context.applicationContext.contentResolver.openInputStream(uri)
                ?.use { inputStream ->
                    csvReader().open(inputStream) {
                        readAllWithHeaderAsSequence().forEachIndexed { index, row: Map<String, String> ->

                            transaction {
                                plistId = row["PlaylistName"]?.let {
                                    Database.playlistExistByName(
                                        it
                                    )
                                } ?: 0L

                                if (plistId == 0L) {
                                    plistId = row["PlaylistName"]?.let {
                                        Database.insert(
                                            Playlist(
                                                name = it,
                                                browseId = row["PlaylistBrowseId"]
                                            )
                                        )
                                    }!!
                                } else {
                                    /**/
                                    if (row["MediaId"] != null && row["Title"] != null) {
                                        val song =
                                            row["MediaId"]?.let {
                                                row["Title"]?.let { it1 ->
                                                    Song(
                                                        id = it,
                                                        title = it1,
                                                        artistsText = row["Artists"],
                                                        durationText = row["Duration"],
                                                        thumbnailUrl = row["ThumbnailUrl"]
                                                    )
                                                }
                                            }
                                        transaction {
                                            if (song != null) {
                                                Database.insert(song)
                                                Database.insert(
                                                    SongPlaylistMap(
                                                        songId = song.id,
                                                        playlistId = plistId,
                                                        position = index
                                                    )
                                                )
                                            }
                                        }


                                    }
                                    /**/
                                }
                            }

                        }
                    }
                }
        }

    val maxTopPlaylistItems by rememberPreference(
        MaxTopPlaylistItemsKey,
        MaxTopPlaylistItems.`10`
    )

    val navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Left)
    val contentWidth = context.preferences.getFloat(contentWidthKey,0.8f)

    val lazyGridState = rememberLazyGridState()

    val showSearchTab by rememberPreference(showSearchTabKey, false)

    val showFavoritesPlaylist by rememberPreference(showFavoritesPlaylistKey, true)
    val showCachedPlaylist by rememberPreference(showCachedPlaylistKey, true)
    val showMyTopPlaylist by rememberPreference(showMyTopPlaylistKey, true)
    val showDownloadedPlaylist by rememberPreference(showDownloadedPlaylistKey, true)
    val showOnDevicePlaylist by rememberPreference(showOnDevicePlaylistKey, true)
    val showPlaylists by rememberPreference(showPlaylistsKey, true)



    Box(
        modifier = Modifier
            .background(colorPalette.background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(if (navigationBarPosition == NavigationBarPosition.Left) 1f else contentWidth)
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(Dimensions.thumbnails.song * 2 + Dimensions.itemsVerticalPadding * 2),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(Dimensions.itemsVerticalPadding * 2),
            horizontalArrangement = Arrangement.spacedBy(
                space = Dimensions.itemsVerticalPadding * 2,
                alignment = Alignment.CenterHorizontally
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(colorPalette.background0)
        ) {
            item(key = "header", contentType = 0, span = { GridItemSpan(maxLineSpan) }) {

                HeaderWithIcon(
                    title = stringResource(R.string.library),
                    iconId = R.drawable.search,
                    enabled = true,
                    showIcon = !showSearchTab,
                    modifier = Modifier,
                    onClick = onSearchClick
                )

                Header(title = "") {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 50.dp)
                            .fillMaxSize()
                    ) {

                        HeaderIconButton(
                            icon = R.drawable.stats_chart,
                            color = colorPalette.text,
                            onClick = { onStatisticsClick() }
                        )

                        /*
                        HeaderInfo(
                            title = "${items.size}",
                            icon = painterResource(R.drawable.playlist),
                            spacer = 0
                        )

                         */

                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                        )

                        /*
                        HeaderIconButton(
                            icon = R.drawable.add_in_playlist,
                            color = colorPalette.text,
                            onClick = { isCreatingANewPlaylist = true }
                        )

                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                        )
                         */

                        BasicText(
                            text = when (sortBy) {
                                PlaylistSortBy.Name -> stringResource(R.string.sort_name)
                                PlaylistSortBy.SongCount -> stringResource(R.string.sort_songs_number)
                                PlaylistSortBy.DateAdded -> stringResource(R.string.sort_date_added)
                                PlaylistSortBy.MostPlayed -> stringResource(R.string.most_played_playlists)
                            },
                            style = typography.xs.semiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clickable {
                                    menuState.display {
                                        SortMenu(
                                            title = stringResource(R.string.sorting_order),
                                            onDismiss = menuState::hide,
                                            onName = { sortBy = PlaylistSortBy.Name },
                                            onSongNumber = { sortBy = PlaylistSortBy.SongCount },
                                            onDateAdded = { sortBy = PlaylistSortBy.DateAdded },
                                            onPlayTime = { sortBy = PlaylistSortBy.MostPlayed },
                                        )
                                    }
                                }
                        )


                        HeaderIconButton(
                            icon = R.drawable.arrow_up,
                            color = colorPalette.text,
                            onClick = { sortOrder = !sortOrder },
                            modifier = Modifier
                                .graphicsLayer { rotationZ = sortOrderIconRotation }
                        )
                    }
                }
            }

            if (showFavoritesPlaylist)
                item(key = "favorites") {
                    PlaylistItem(
                        icon = R.drawable.heart,
                        colorTint = colorPalette.favoritesIcon,
                        name = stringResource(R.string.favorites),
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .clip(thumbnailShape)
                            .clickable(onClick = { onBuiltInPlaylist(BuiltInPlaylist.Favorites) })
                            .animateItemPlacement()

                    )
                }

            if(showCachedPlaylist)
                item(key = "offline") {
                    PlaylistItem(
                        icon = R.drawable.sync,
                        colorTint = colorPalette.favoritesIcon,
                        name = stringResource(R.string.cached),
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .clip(thumbnailShape)
                            .clickable(onClick = { onBuiltInPlaylist(BuiltInPlaylist.Offline) })
                            .animateItemPlacement()
                    )
                }

            if(showDownloadedPlaylist)
                item(key = "downloaded") {
                    PlaylistItem(
                        icon = R.drawable.downloaded,
                        colorTint = colorPalette.favoritesIcon,
                        name = stringResource(R.string.downloaded),
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .clip(thumbnailShape)
                            .clickable(onClick = { onBuiltInPlaylist(BuiltInPlaylist.Downloaded) })
                            .animateItemPlacement()
                    )
                }

            if (showMyTopPlaylist)
                item(key = "top") {
                    PlaylistItem(
                        icon = R.drawable.trending,
                        colorTint = colorPalette.favoritesIcon,
                        name = stringResource(R.string.my_playlist_top) + " ${maxTopPlaylistItems.number}",
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .clip(thumbnailShape)
                            .clickable(onClick = { onBuiltInPlaylist(BuiltInPlaylist.Top) })
                            .animateItemPlacement()
                    )
                }

            if (showOnDevicePlaylist)
                item(key = "ondevice") {
                    PlaylistItem(
                        icon = R.drawable.musical_notes,
                        colorTint = colorPalette.favoritesIcon,
                        name = stringResource(R.string.on_device),
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .clip(thumbnailShape)
                            .clickable(onClick = { onDeviceListSongsClick() })
                            .animateItemPlacement()
                    )
                }

            /*    */

            if (showPlaylists) {
                if (items.filter {
                        it.playlist.name.startsWith(PINNED_PREFIX,0,true)
                    }.isNotEmpty())
                item(
                    key = "headerPinnedPlaylist",
                    contentType = 0,
                    span = { GridItemSpan(maxLineSpan) }) {
                    BasicText(
                        text = "${stringResource(R.string.pinned_playlists)} (${items.filter {
                            it.playlist.name.startsWith(PINNED_PREFIX,0,true)
                        }.size})",
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )
                }

                items(items = items.filter {
                    it.playlist.name.startsWith(PINNED_PREFIX,0,true)
                }, key = { it.playlist.id }) { playlistPreview ->
                    PlaylistItem(
                        playlist = playlistPreview,
                        thumbnailSizeDp = thumbnailSizeDp,
                        thumbnailSizePx = thumbnailSizePx,
                        alternative = true,
                        modifier = Modifier
                            .clickable(onClick = { onPlaylistClick(playlistPreview.playlist) })
                            .animateItemPlacement()
                            .fillMaxSize()
                    )
                }

                item(
                    key = "headerplaylist",
                    contentType = 0,
                    span = { GridItemSpan(maxLineSpan) }) {
                    Row (
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        BasicText(
                            text = "${stringResource(R.string.playlists)} (${
                                items.filter {
                                    !it.playlist.name.startsWith(PINNED_PREFIX, 0, true)
                                }.size
                            })",
                            style = typography.m.semiBold,
                            //modifier = sectionTextModifier
                            modifier = Modifier
                                .padding(15.dp)
                        )

                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                        )

                        IconButton(
                            icon = R.drawable.add_in_playlist,
                            color = colorPalette.text,
                            onClick = { isCreatingANewPlaylist = true },
                            modifier = Modifier
                                .padding(10.dp)
                                .size(20.dp)
                        )
                        IconButton(
                            icon = R.drawable.resource_import,
                            color = colorPalette.text,
                            onClick = {
                                try {
                                    importLauncher.launch(
                                        arrayOf(
                                            "text/*"
                                        )
                                    )
                                } catch (e: ActivityNotFoundException) {
                                    context.toast(e.message ?: "Couldn't find an application to open documents")
                                }
                            },
                            modifier = Modifier
                                .padding(10.dp)
                                .size(20.dp)
                        )

                    }

                }

                items(items = items.filter {
                    !it.playlist.name.startsWith(PINNED_PREFIX,0,true)
                }, key = { it.playlist.id }) { playlistPreview ->
                    PlaylistItem(
                        playlist = playlistPreview,
                        thumbnailSizeDp = thumbnailSizeDp,
                        thumbnailSizePx = thumbnailSizePx,
                        alternative = true,
                        modifier = Modifier
                            .clickable(onClick = { onPlaylistClick(playlistPreview.playlist) })
                            .animateItemPlacement()
                            .fillMaxSize()
                    )
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyGridState = lazyGridState)

        if(uiType == UiType.ViMusic)
        FloatingActionsContainerWithScrollToTop(
            lazyGridState = lazyGridState,
            iconId = R.drawable.search,
            onClick = onSearchClick
        )


    }
}
