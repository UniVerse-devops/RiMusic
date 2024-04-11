package com.universe.audioflare.ui.components.themed

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.universe.innertube.models.NavigationEndpoint
import com.universe.audioflare.Database
import com.universe.audioflare.R
import com.universe.audioflare.enums.MenuStyle
import com.universe.audioflare.enums.PlayerThumbnailSize
import com.universe.audioflare.query
import com.universe.audioflare.service.PlayerService
import com.universe.audioflare.utils.menuStyleKey
import com.universe.audioflare.utils.playerThumbnailSizeKey
import com.universe.audioflare.utils.rememberPreference
import com.universe.audioflare.utils.seamlessPlay
import com.universe.audioflare.utils.toast

@ExperimentalTextApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun PlayerMenu(
    binder: PlayerService.Binder,
    mediaItem: MediaItem,
    onDismiss: () -> Unit,

    ) {

    val menuStyle by rememberPreference(
        menuStyleKey,
        MenuStyle.List
    )

    val context = LocalContext.current

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    var isHiding by remember {
        mutableStateOf(false)
    }

    if (isHiding) {
        ConfirmationDialog(
            text = stringResource(R.string.hidesong),
            onDismiss = { isHiding = false },
            onConfirm = {
                onDismiss()
                query {
                    binder.cache.removeResource(mediaItem.mediaId)
                    Database.resetTotalPlayTimeMs(mediaItem.mediaId)
                    /*
                    if (binder.player.hasNextMediaItem()) {
                        binder.player.forceSeekToNext()
                        binder.player.removeMediaItem(binder.player.currentMediaItemIndex - 1)
                    }
                    if (binder.player.hasPreviousMediaItem()) {
                        binder.player.forceSeekToPrevious()
                        binder.player.removeMediaItem(binder.player.currentMediaItemIndex + 1)
                    }
                     */
                }
            }
        )
    }


    if (menuStyle == MenuStyle.Grid) {
        BaseMediaItemGridMenu(
            mediaItem = mediaItem,
            onDismiss = onDismiss,
            onStartRadio = {
                binder.stopRadio()
                binder.player.seamlessPlay(mediaItem)
                binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
            },
            onGoToEqualizer = {
                try {
                    activityResultLauncher.launch(
                        Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder.player.audioSessionId)
                            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    context.toast("Couldn't find an application to equalize audio")
                }
            },
            onHideFromDatabase = { isHiding = true },
        )
    } else {
        BaseMediaItemMenu(
            mediaItem = mediaItem,
            onStartRadio = {
                binder.stopRadio()
                binder.player.seamlessPlay(mediaItem)
                binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
            },
            onGoToEqualizer = {
                try {
                    activityResultLauncher.launch(
                        Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder.player.audioSessionId)
                            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    context.toast("Couldn't find an application to equalize audio")
                }
            },
            onShowSleepTimer = {},
            onHideFromDatabase = { isHiding = true },
            onDismiss = onDismiss
        )
    }

}