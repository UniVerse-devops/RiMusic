package com.universe.audioflare.ui.screens.artist

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.universe.compose.persist.persist
import com.universe.audioflare.Database
import com.universe.audioflare.LocalPlayerAwareWindowInsets
import com.universe.audioflare.LocalPlayerServiceBinder
import com.universe.audioflare.R
import com.universe.audioflare.enums.NavigationBarPosition
import com.universe.audioflare.enums.UiType
import com.universe.audioflare.models.Song
import com.universe.audioflare.query
import com.universe.audioflare.ui.components.LocalMenuState
import com.universe.audioflare.ui.components.ShimmerHost
import com.universe.audioflare.ui.components.themed.ConfirmationDialog
import com.universe.audioflare.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.universe.audioflare.ui.components.themed.HeaderIconButton
import com.universe.audioflare.ui.components.themed.LayoutWithAdaptiveThumbnail
import com.universe.audioflare.ui.components.themed.NonQueuedMediaItemMenu
import com.universe.audioflare.ui.items.SongItem
import com.universe.audioflare.ui.items.SongItemPlaceholder
import com.universe.audioflare.ui.styling.Dimensions
import com.universe.audioflare.ui.styling.LocalAppearance
import com.universe.audioflare.ui.styling.px
import com.universe.audioflare.utils.UiTypeKey
import com.universe.audioflare.utils.asMediaItem
import com.universe.audioflare.utils.contentWidthKey
import com.universe.audioflare.utils.downloadedStateMedia
import com.universe.audioflare.utils.enqueue
import com.universe.audioflare.utils.forcePlayAtIndex
import com.universe.audioflare.utils.forcePlayFromBeginning
import com.universe.audioflare.utils.getDownloadState
import com.universe.audioflare.utils.manageDownload
import com.universe.audioflare.utils.navigationBarPositionKey
import com.universe.audioflare.utils.preferences
import com.universe.audioflare.utils.rememberPreference
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun ArtistLocalSongs(
    browseId: String,
    headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit,
    thumbnailContent: @Composable () -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    val (colorPalette) = LocalAppearance.current
    val menuState = LocalMenuState.current
    val uiType  by rememberPreference(UiTypeKey, UiType.audioflare)

    var songs by persist<List<Song>?>("artist/$browseId/localSongs")

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Database.artistSongs(browseId).collect { songs = it }
/*
        val items = songs?.map { it.id }
        downloader.downloads.collect { downloads ->
            if (items != null) {
                downloadState =
                    if (items.all { downloads[it]?.state == Download.STATE_COMPLETED })
                        Download.STATE_COMPLETED
                    else if (items.all {
                            downloads[it]?.state == Download.STATE_QUEUED
                                    || downloads[it]?.state == Download.STATE_DOWNLOADING
                                    || downloads[it]?.state == Download.STATE_COMPLETED
                        })
                        Download.STATE_DOWNLOADING
                    else
                        Download.STATE_STOPPED
            }
        }

 */

    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px

    val lazyListState = rememberLazyListState()

    var showConfirmDeleteDownloadDialog by remember {
        mutableStateOf(false)
    }

    var showConfirmDownloadAllDialog by remember {
        mutableStateOf(false)
    }

    val navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Left)
    val contentWidth = context.preferences.getFloat(contentWidthKey,0.8f)

    LayoutWithAdaptiveThumbnail(thumbnailContent = thumbnailContent) {
        Box(
            modifier = Modifier
                .background(colorPalette.background0)
                //.fillMaxSize()
                .fillMaxHeight()
                .fillMaxWidth(if (navigationBarPosition == NavigationBarPosition.Left) 1f else contentWidth)
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        headerContent {

                            HeaderIconButton(
                                icon = R.drawable.downloaded,
                                color = colorPalette.text,
                                onClick = {
                                    showConfirmDownloadAllDialog = true
                                }
                            )

                            if (showConfirmDownloadAllDialog) {
                                ConfirmationDialog(
                                    text = stringResource(R.string.do_you_really_want_to_download_all),
                                    onDismiss = { showConfirmDownloadAllDialog = false },
                                    onConfirm = {
                                        showConfirmDownloadAllDialog = false
                                        downloadState = Download.STATE_DOWNLOADING
                                        if (songs?.isNotEmpty() == true)
                                            songs?.forEach {
                                                binder?.cache?.removeResource(it.asMediaItem.mediaId)
                                                query {
                                                    Database.insert(
                                                        Song(
                                                            id = it.asMediaItem.mediaId,
                                                            title = it.asMediaItem.mediaMetadata.title.toString(),
                                                            artistsText = it.asMediaItem.mediaMetadata.artist.toString(),
                                                            thumbnailUrl = it.thumbnailUrl,
                                                            durationText = null
                                                        )
                                                    )
                                                }
                                                manageDownload(
                                                    context = context,
                                                    songId = it.asMediaItem.mediaId,
                                                    songTitle = it.asMediaItem.mediaMetadata.title.toString(),
                                                    downloadState = false
                                                )
                                            }
                                    }
                                )
                            }

                            HeaderIconButton(
                                icon = R.drawable.download,
                                color = colorPalette.text,
                                onClick = {
                                    showConfirmDeleteDownloadDialog = true
                                }
                            )

                            if (showConfirmDeleteDownloadDialog) {
                                ConfirmationDialog(
                                    text = stringResource(R.string.do_you_really_want_to_delete_download),
                                    onDismiss = { showConfirmDeleteDownloadDialog = false },
                                    onConfirm = {
                                        showConfirmDeleteDownloadDialog = false
                                        downloadState = Download.STATE_DOWNLOADING
                                        if (songs?.isNotEmpty() == true)
                                            songs?.forEach {
                                                binder?.cache?.removeResource(it.asMediaItem.mediaId)
                                                manageDownload(
                                                    context = context,
                                                    songId = it.asMediaItem.mediaId,
                                                    songTitle = it.asMediaItem.mediaMetadata.title.toString(),
                                                    downloadState = true
                                                )
                                            }
                                    }
                                )
                            }

                            HeaderIconButton(
                                icon = R.drawable.enqueue,
                                enabled = !songs.isNullOrEmpty(),
                                color = if (!songs.isNullOrEmpty()) colorPalette.text else colorPalette.textDisabled,
                                onClick = { binder?.player?.enqueue(songs!!.map(Song::asMediaItem)) }
                            )
                            HeaderIconButton(
                                icon = R.drawable.shuffle,
                                enabled = !songs.isNullOrEmpty(),
                                color = if (!songs.isNullOrEmpty()) colorPalette.text else colorPalette.textDisabled,
                                onClick = {
                                    songs?.let { songs ->
                                        if (songs.isNotEmpty()) {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayFromBeginning(
                                                songs.shuffled().map(Song::asMediaItem)
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        thumbnailContent()
                    }
                }

                songs?.let { songs ->
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { index, song ->

                        downloadState = getDownloadState(song.asMediaItem.mediaId)
                        val isDownloaded = downloadedStateMedia(song.asMediaItem.mediaId)
                        SongItem(
                            song = song,
                            isDownloaded = isDownloaded,
                            onDownloadClick = {
                                binder?.cache?.removeResource(song.asMediaItem.mediaId)
                                query {
                                    Database.insert(
                                        Song(
                                            id = song.asMediaItem.mediaId,
                                            title = song.asMediaItem.mediaMetadata.title.toString(),
                                            artistsText = song.asMediaItem.mediaMetadata.artist.toString(),
                                            thumbnailUrl = song.thumbnailUrl,
                                            durationText = null
                                        )
                                    )
                                }

                                manageDownload(
                                    context = context,
                                    songId = song.id,
                                    songTitle = song.title,
                                    downloadState = isDownloaded
                                )
                            },
                            downloadState = downloadState,
                            thumbnailSizeDp = songThumbnailSizeDp,
                            thumbnailSizePx = songThumbnailSizePx,
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = song.asMediaItem,
                                            )
                                        }
                                    },
                                    onClick = {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(
                                            songs.map(Song::asMediaItem),
                                            index
                                        )
                                    }
                                )
                        )
                    }
                } ?: item(key = "loading") {
                    ShimmerHost {
                        repeat(4) {
                            SongItemPlaceholder(thumbnailSizeDp = Dimensions.thumbnails.song)
                        }
                    }
                }
            }

            if(uiType == UiType.ViMusic)
            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                iconId = R.drawable.shuffle,
                onClick = {
                    songs?.let { songs ->
                        if (songs.isNotEmpty()) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(
                                songs.shuffled().map(Song::asMediaItem)
                            )
                        }
                    }
                }
            )


        }
    }
}
