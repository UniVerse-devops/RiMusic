package com.universe.audioflare.ui.screens.search

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import com.universe.innertube.Innertube
import com.universe.innertube.models.bodies.BrowseBody
import com.universe.innertube.requests.playlistPage
import com.universe.innertube.requests.song
import com.universe.audioflare.LocalPlayerAwareWindowInsets
import com.universe.audioflare.LocalPlayerServiceBinder
import com.universe.audioflare.R
import com.universe.audioflare.ui.components.themed.HeaderWithIcon
import com.universe.audioflare.ui.components.themed.IconButton
import com.universe.audioflare.ui.components.themed.InputTextField
import com.universe.audioflare.ui.screens.albumRoute
import com.universe.audioflare.ui.screens.artistRoute
import com.universe.audioflare.ui.screens.playlistRoute
import com.universe.audioflare.ui.styling.LocalAppearance
import com.universe.audioflare.ui.styling.favoritesIcon
import com.universe.audioflare.utils.asMediaItem
import com.universe.audioflare.utils.forcePlay
import com.universe.audioflare.utils.semiBold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun GoToLink(
    textFieldValue: TextFieldValue,
    onTextFieldValueChanged: (TextFieldValue) -> Unit,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit,
    onAction1: () -> Unit,
    onAction2: () -> Unit,
    onAction3: () -> Unit,
    onAction4: () -> Unit,
) {

    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = CoroutineScope(Dispatchers.IO) + Job()

    val lazyListState = rememberLazyListState()

    var textLink by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current
    //val navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Left)
    //val contentWidth = context.preferences.getFloat(contentWidthKey,0.8f)

    Box(
        modifier = Modifier
            .background(colorPalette.background0)
            //.fillMaxSize()
            .fillMaxHeight()
            //.fillMaxWidth(if (navigationBarPosition == NavigationBarPosition.Left) 1f else contentWidth)
            .fillMaxWidth()
    ) {

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier
                .fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = 0
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    HeaderWithIcon(
                        title = stringResource(R.string.go_to_link),
                        iconId = R.drawable.query_stats,
                        enabled = true,
                        showIcon = true,
                        modifier = Modifier
                            .padding(bottom = 8.dp),
                        onClick = {}
                    )

                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onAction1,
                        icon = R.drawable.globe,
                        color = colorPalette.favoritesIcon,
                        modifier = Modifier
                            .size(24.dp)
                    )
                    IconButton(
                        onClick = onAction2,
                        icon = R.drawable.library,
                        color = colorPalette.favoritesIcon,
                        modifier = Modifier
                            .size(24.dp)
                    )
                    IconButton(
                        onClick = onAction3,
                        icon = R.drawable.link,
                        color = colorPalette.favoritesIcon,
                        modifier = Modifier
                            .size(24.dp)
                    )
                    IconButton(
                        onClick = onAction4,
                        icon = R.drawable.chevron_back,
                        color = colorPalette.favoritesIcon,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }

                InputTextField(
                    onDismiss = { },
                    title = stringResource(R.string.paste_or_type_a_valid_url),
                    value = textFieldValue.text,
                    placeholder = "https://........",
                    setValue = { textLink = it }
                )

                BasicText(
                    text = stringResource(R.string.you_can_put_a_complete_link),
                    style = typography.s.semiBold,
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 24.dp)
                )

                if (textLink.isNotEmpty()) {

                    val uri = textLink.toUri()

                    LaunchedEffect(Unit) {
                        coroutineScope.launch(Dispatchers.IO) {
                            when (val path = uri.pathSegments.firstOrNull()) {
                                "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                                    val browseId = "VL$playlistId"

                                    if (playlistId.startsWith("OLAK5uy_")) {
                                        Innertube.playlistPage(BrowseBody(browseId = browseId))
                                            ?.getOrNull()?.let {
                                                it.songsPage?.items?.firstOrNull()?.album?.endpoint?.browseId?.let { browseId ->
                                                    albumRoute.ensureGlobal(browseId)
                                                }
                                            }
                                    } else {
                                        //playlistRoute.ensureGlobal(browseId, null)
                                        playlistRoute.ensureGlobal(browseId, uri.getQueryParameter("params"), null)
                                    }
                                }

                                "channel", "c" -> uri.lastPathSegment?.let { channelId ->
                                    artistRoute.ensureGlobal(channelId)
                                }

                                else -> when {
                                    path == "watch" -> uri.getQueryParameter("v")
                                    uri.host == "youtu.be" -> path
                                    else -> null
                                }?.let { videoId ->
                                    Innertube.song(videoId)?.getOrNull()?.let { song ->
                                        val binder = snapshotFlow { binder }.filterNotNull().first()
                                        withContext(Dispatchers.Main) {
                                            binder.player.forcePlay(song.asMediaItem)
                                        }
                                    }
                                }
                            }
                        }

                    }
                }

            }

        }

    }
}
