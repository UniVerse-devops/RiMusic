package com.universe.audioflare.ui.screens.artist

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.universe.innertube.Innertube
import com.universe.innertube.models.NavigationEndpoint
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
import com.universe.audioflare.ui.components.themed.IconButton
import com.universe.audioflare.ui.components.themed.LayoutWithAdaptiveThumbnail
import com.universe.audioflare.ui.components.themed.NonQueuedMediaItemMenu
import com.universe.audioflare.ui.components.themed.TextPlaceholder
import com.universe.audioflare.ui.items.AlbumItem
import com.universe.audioflare.ui.items.AlbumItemPlaceholder
import com.universe.audioflare.ui.items.SongItem
import com.universe.audioflare.ui.items.SongItemPlaceholder
import com.universe.audioflare.ui.styling.Dimensions
import com.universe.audioflare.ui.styling.LocalAppearance
import com.universe.audioflare.ui.styling.px
import com.universe.audioflare.utils.UiTypeKey
import com.universe.audioflare.utils.align
import com.universe.audioflare.utils.asMediaItem
import com.universe.audioflare.utils.color
import com.universe.audioflare.utils.contentWidthKey
import com.universe.audioflare.utils.downloadedStateMedia
import com.universe.audioflare.utils.forcePlay
import com.universe.audioflare.utils.getDownloadState
import com.universe.audioflare.utils.getHttpClient
import com.universe.audioflare.utils.languageDestination
import com.universe.audioflare.utils.manageDownload
import com.universe.audioflare.utils.navigationBarPositionKey
import com.universe.audioflare.utils.preferences
import com.universe.audioflare.utils.rememberPreference
import com.universe.audioflare.utils.secondary
import com.universe.audioflare.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bush.translator.Language
import me.bush.translator.Translator

@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@UnstableApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun ArtistOverview(
    youtubeArtistPage: Innertube.ArtistPage?,
    onViewAllSongsClick: () -> Unit,
    onViewAllAlbumsClick: () -> Unit,
    onViewAllSinglesClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    thumbnailContent: @Composable () -> Unit,
    headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val uiType  by rememberPreference(UiTypeKey, UiType.audioflare)

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = 108.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)

    val scrollState = rememberScrollState()

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    val context = LocalContext.current

    var showConfirmDeleteDownloadDialog by remember {
        mutableStateOf(false)
    }

    var showConfirmDownloadAllDialog by remember {
        mutableStateOf(false)
    }

    var translateEnabled by remember {
        mutableStateOf(false)
    }

    val translator = Translator(getHttpClient())
    val languageDestination = languageDestination()

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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        windowInsets
                            .only(WindowInsetsSides.Vertical)
                            .asPaddingValues()
                    )
            ) {
                Box(
                    modifier = Modifier
                        .padding(endPaddingValues)
                ) {
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
                                    if (youtubeArtistPage?.songs?.isNotEmpty() == true)
                                        youtubeArtistPage.songs?.forEach {
                                            binder?.cache?.removeResource(it.asMediaItem.mediaId)
                                            query {
                                                Database.insert(
                                                    Song(
                                                        id = it.asMediaItem.mediaId,
                                                        title = it.asMediaItem.mediaMetadata.title.toString(),
                                                        artistsText = it.asMediaItem.mediaMetadata.artist.toString(),
                                                        thumbnailUrl = it.thumbnail?.url,
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
                                    if (youtubeArtistPage?.songs?.isNotEmpty() == true)
                                        youtubeArtistPage.songs?.forEach {
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

                        youtubeArtistPage?.shuffleEndpoint?.let { endpoint ->
                            HeaderIconButton(
                                icon = R.drawable.shuffle,
                                enabled = true,
                                color = colorPalette.text,
                                onClick = {
                                    binder?.stopRadio()
                                    binder?.playRadio(endpoint)
                                }
                            )
                        }
                        youtubeArtistPage?.radioEndpoint?.let { endpoint ->
                            HeaderIconButton(
                                icon = R.drawable.radio,
                                enabled = true,
                                color = colorPalette.text,
                                onClick = {
                                    binder?.stopRadio()
                                    binder?.playRadio(endpoint)
                                }
                            )
                        }
                    }
                }

                thumbnailContent()

                youtubeArtistPage?.subscriberCountText ?.let {
                    BasicText(
                        text = String.format(stringResource(R.string.artist_subscribers),it),
                        style = typography.xs.semiBold,
                        maxLines = 1
                    )
                }

                if (youtubeArtistPage != null) {
                    youtubeArtistPage.songs?.let { songs ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(endPaddingValues)
                        ) {
                            BasicText(
                                text = stringResource(R.string.songs),
                                style = typography.m.semiBold,
                                modifier = sectionTextModifier
                            )

                            youtubeArtistPage.songsEndpoint?.let {
                                BasicText(
                                    text = stringResource(R.string.view_all),
                                    style = typography.xs.secondary,
                                    modifier = sectionTextModifier
                                        .clickable(onClick = onViewAllSongsClick),
                                )
                            }
                        }

                        songs.forEach { song ->

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
                                                    thumbnailUrl = song.thumbnail?.url,
                                                    durationText = null
                                                )
                                            )
                                    }

                                    manageDownload(
                                        context = context,
                                        songId = song.asMediaItem.mediaId,
                                        songTitle = song.asMediaItem.mediaMetadata.title.toString(),
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
                                            val mediaItem = song.asMediaItem
                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(mediaItem)
                                            binder?.setupRadio(
                                                NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                            )
                                        }
                                    )
                                    .padding(endPaddingValues)
                            )
                        }
                    }

                    youtubeArtistPage.albums?.let { albums ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(endPaddingValues)
                        ) {
                            BasicText(
                                text = stringResource(R.string.albums),
                                style = typography.m.semiBold,
                                modifier = sectionTextModifier
                            )

                            youtubeArtistPage.albumsEndpoint?.let {
                                BasicText(
                                    text = stringResource(R.string.view_all),
                                    style = typography.xs.secondary,
                                    modifier = sectionTextModifier
                                        .clickable(onClick = onViewAllAlbumsClick),
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = endPaddingValues,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            items(
                                items = albums,
                                key = Innertube.AlbumItem::key
                            ) { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSizePx = albumThumbnailSizePx,
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier
                                        .clickable(onClick = { onAlbumClick(album.key) })
                                )
                            }
                        }
                    }

                    youtubeArtistPage.singles?.let { singles ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(endPaddingValues)
                        ) {
                            BasicText(
                                text = stringResource(R.string.singles),
                                style = typography.m.semiBold,
                                modifier = sectionTextModifier
                            )

                            youtubeArtistPage.singlesEndpoint?.let {
                                BasicText(
                                    text = stringResource(R.string.view_all),
                                    style = typography.xs.secondary,
                                    modifier = sectionTextModifier
                                        .clickable(onClick = onViewAllSinglesClick),
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = endPaddingValues,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            items(
                                items = singles,
                                key = Innertube.AlbumItem::key
                            ) { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSizePx = albumThumbnailSizePx,
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier
                                        .clickable(onClick = { onAlbumClick(album.key) })
                                )
                            }
                        }
                    }

                    youtubeArtistPage.description?.let { description ->
                        val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")

                        Row(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .padding(vertical = 16.dp, horizontal = 8.dp)
                                .padding(endPaddingValues)
                        ) {
                            IconButton(
                                icon = R.drawable.translate,
                                color = if (translateEnabled == true) colorPalette.text else colorPalette.textDisabled,
                                enabled = true,
                                onClick = {
                                    translateEnabled = !translateEnabled
                                },
                                modifier = Modifier
                                    .padding(all = 8.dp)
                                    .size(18.dp)
                            )
                            BasicText(
                                text = "“",
                                style = typography.xxl.semiBold,
                                modifier = Modifier
                                    .offset(y = (-8).dp)
                                    .align(Alignment.Top)
                            )

                            var translatedText by remember { mutableStateOf("") }
                            val nonTranslatedText by remember { mutableStateOf(
                                    if (attributionsIndex == -1) {
                                        description
                                    } else {
                                        description.substring(0, attributionsIndex)
                                    }
                                )
                            }


                            if (translateEnabled == true) {
                                LaunchedEffect(Unit) {
                                    val result = withContext(Dispatchers.IO) {
                                        try {
                                            translator.translate(
                                                nonTranslatedText,
                                                languageDestination,
                                                Language.AUTO
                                            ).translatedText
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    translatedText =
                                        if (result.toString() == "kotlin.Unit") "" else result.toString()
                                }
                            } else translatedText = nonTranslatedText

                            BasicText(
                                text = translatedText,
                                style = typography.xxs.secondary,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .weight(1f)
                            )

                            BasicText(
                                text = "„",
                                style = typography.xxl.semiBold,
                                modifier = Modifier
                                    .offset(y = 4.dp)
                                    .align(Alignment.Bottom)
                            )
                        }

                        if (attributionsIndex != -1) {
                            BasicText(
                                text = stringResource(R.string.from_wikipedia_cca),
                                style = typography.xxs.color(colorPalette.textDisabled).align(TextAlign.End),
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp)
                                    .padding(endPaddingValues)
                            )
                        }
                    }
                } else {
                    ShimmerHost {
                        TextPlaceholder(modifier = sectionTextModifier)

                        repeat(5) {
                            SongItemPlaceholder(
                                thumbnailSizeDp = songThumbnailSizeDp,
                            )
                        }

                        repeat(2) {
                            TextPlaceholder(modifier = sectionTextModifier)

                            Row {
                                repeat(2) {
                                    AlbumItemPlaceholder(
                                        thumbnailSizeDp = albumThumbnailSizeDp,
                                        alternative = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if(uiType == UiType.ViMusic)
            youtubeArtistPage?.radioEndpoint?.let { endpoint ->
                FloatingActionsContainerWithScrollToTop(
                    scrollState = scrollState,
                    iconId = R.drawable.radio,
                    onClick = {
                        binder?.stopRadio()
                        binder?.playRadio(endpoint)
                    }
                )
            }


        }
    }
}
