package com.universe.audioflare.ui.screens.searchresult

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.universe.compose.persist.PersistMapCleanup
import com.universe.compose.persist.persistMap
import com.universe.compose.routing.RouteHandler
import com.universe.innertube.Innertube
import com.universe.innertube.models.bodies.ContinuationBody
import com.universe.innertube.models.bodies.SearchBody
import com.universe.innertube.requests.searchPage
import com.universe.innertube.utils.from
import com.universe.audioflare.Database
import com.universe.audioflare.LocalPlayerServiceBinder
import com.universe.audioflare.R
import com.universe.audioflare.models.Song
import com.universe.audioflare.query
import com.universe.audioflare.ui.components.LocalMenuState
import com.universe.audioflare.ui.components.themed.Header
import com.universe.audioflare.ui.components.themed.NonQueuedMediaItemMenu
import com.universe.audioflare.ui.components.Scaffold
import com.universe.audioflare.ui.items.AlbumItem
import com.universe.audioflare.ui.items.AlbumItemPlaceholder
import com.universe.audioflare.ui.items.ArtistItem
import com.universe.audioflare.ui.items.ArtistItemPlaceholder
import com.universe.audioflare.ui.items.PlaylistItem
import com.universe.audioflare.ui.items.PlaylistItemPlaceholder
import com.universe.audioflare.ui.items.SongItem
import com.universe.audioflare.ui.items.SongItemPlaceholder
import com.universe.audioflare.ui.items.VideoItem
import com.universe.audioflare.ui.items.VideoItemPlaceholder
import com.universe.audioflare.ui.screens.albumRoute
import com.universe.audioflare.ui.screens.artistRoute
import com.universe.audioflare.ui.screens.globalRoutes
import com.universe.audioflare.ui.screens.playlistRoute
import com.universe.audioflare.ui.styling.Dimensions
import com.universe.audioflare.ui.styling.px
import com.universe.audioflare.utils.asMediaItem
import com.universe.audioflare.utils.downloadedStateMedia
import com.universe.audioflare.utils.forcePlay
import com.universe.audioflare.utils.getDownloadState
import com.universe.audioflare.utils.manageDownload
import com.universe.audioflare.utils.rememberPreference
import com.universe.audioflare.utils.searchResultScreenTabIndexKey
@ExperimentalMaterialApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun SearchResultScreen(query: String, onSearchAgain: () -> Unit) {
    val context = LocalContext.current
    val saveableStateHolder = rememberSaveableStateHolder()
    val (tabIndex, onTabIndexChanges) = rememberPreference(searchResultScreenTabIndexKey, 0)

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    PersistMapCleanup(tagPrefix = "searchResults/$query/")

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()

        host {
            val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit = {
                Header(
                    title = query,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures {
                                context.persistMap?.keys?.removeAll {
                                    it.startsWith("searchResults/$query/")
                                }
                                onSearchAgain()
                            }
                        }
                )
            }

            val emptyItemsText = stringResource(R.string.no_results_found)

            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                topIconButton2Id = R.drawable.chevron_back,
                onTopIconButton2Click = pop,
                showButton2 = false,
                tabIndex = tabIndex,
                onTabChanged = onTabIndexChanges,
                tabColumnContent = { Item ->
                    Item(0, stringResource(R.string.songs), R.drawable.musical_notes)
                    Item(1, stringResource(R.string.albums), R.drawable.album)
                    Item(2, stringResource(R.string.artists), R.drawable.artist)
                    Item(3, stringResource(R.string.videos), R.drawable.video)
                    Item(4, stringResource(R.string.playlists), R.drawable.playlist)
                    Item(5, stringResource(R.string.featured), R.drawable.featured_playlist)
                }
            ) { tabIndex ->
                saveableStateHolder.SaveableStateProvider(tabIndex) {
                    when (tabIndex) {
                        0 -> {
                            val binder = LocalPlayerServiceBinder.current
                            val menuState = LocalMenuState.current
                            val thumbnailSizeDp = Dimensions.thumbnails.song
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/songs",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Innertube.searchPage(
                                            body = SearchBody(query = query, params = Innertube.SearchFilter.Song.value),
                                            fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                                        )
                                    } else {
                                        Innertube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { song ->
                                    //Log.d("mediaItem",song.toString())
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
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
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
                                                    binder?.player?.forcePlay(song.asMediaItem)
                                                    binder?.setupRadio(song.info?.endpoint)
                                                }
                                            )
                                    )
                                },
                                itemPlaceholderContent = {
                                    SongItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        1 -> {
                            val thumbnailSizeDp = 108.dp
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/albums",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Innertube.searchPage(
                                            body = SearchBody(query = query, params = Innertube.SearchFilter.Album.value),
                                            fromMusicShelfRendererContent = Innertube.AlbumItem::from
                                        )
                                    } else {
                                        Innertube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Innertube.AlbumItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { album ->
                                    AlbumItem(
                                        album = album,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .clickable(onClick = { albumRoute(album.key) })
                                    )

                                },
                                itemPlaceholderContent = {
                                    AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        2 -> {
                            val thumbnailSizeDp = 64.dp
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/artists",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Innertube.searchPage(
                                            body = SearchBody(query = query, params = Innertube.SearchFilter.Artist.value),
                                            fromMusicShelfRendererContent = Innertube.ArtistItem::from
                                        )
                                    } else {
                                        Innertube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Innertube.ArtistItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { artist ->
                                    ArtistItem(
                                        artist = artist,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .clickable(onClick = { artistRoute(artist.key) })
                                    )
                                },
                                itemPlaceholderContent = {
                                    ArtistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        3 -> {
                            val binder = LocalPlayerServiceBinder.current
                            val menuState = LocalMenuState.current
                            val thumbnailHeightDp = 72.dp
                            val thumbnailWidthDp = 128.dp

                            ItemsPage(
                                tag = "searchResults/$query/videos",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Innertube.searchPage(
                                            body = SearchBody(query = query, params = Innertube.SearchFilter.Video.value),
                                            fromMusicShelfRendererContent = Innertube.VideoItem::from
                                        )
                                    } else {
                                        Innertube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Innertube.VideoItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { video ->
                                    VideoItem(
                                        video = video,
                                        thumbnailWidthDp = thumbnailWidthDp,
                                        thumbnailHeightDp = thumbnailHeightDp,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onLongClick = {
                                                    menuState.display {
                                                        NonQueuedMediaItemMenu(
                                                            mediaItem = video.asMediaItem,
                                                            onDismiss = menuState::hide
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlay(video.asMediaItem)
                                                    binder?.setupRadio(video.info?.endpoint)
                                                }
                                            )
                                    )
                                },
                                itemPlaceholderContent = {
                                    VideoItemPlaceholder(
                                        thumbnailHeightDp = thumbnailHeightDp,
                                        thumbnailWidthDp = thumbnailWidthDp
                                    )
                                }
                            )
                        }

                        4, 5 -> {
                            val thumbnailSizeDp = Dimensions.thumbnails.playlist
                            val thumbnailSizePx = thumbnailSizeDp.px
                            //val thumbnailSizeDp = 108.dp
                            //val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/${if (tabIndex == 4) "playlists" else "featured"}",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        val filter = if (tabIndex == 4) {
                                            Innertube.SearchFilter.CommunityPlaylist
                                        } else {
                                            Innertube.SearchFilter.FeaturedPlaylist
                                        }

                                        Innertube.searchPage(
                                            body = SearchBody(query = query, params = filter.value),
                                            fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                                        )
                                    } else {
                                        Innertube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        showSongsCount = false,
                                        modifier = Modifier
                                            .clickable(onClick = { playlistRoute(playlist.key) })
                                    )
                                },
                                itemPlaceholderContent = {
                                    PlaylistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
