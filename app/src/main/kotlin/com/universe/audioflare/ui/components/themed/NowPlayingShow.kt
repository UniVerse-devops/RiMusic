package com.universe.audioflare.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.universe.audioflare.LocalPlayerServiceBinder
import com.universe.audioflare.ui.components.MusicBars
import com.universe.audioflare.ui.styling.Dimensions
import com.universe.audioflare.ui.styling.LocalAppearance
import com.universe.audioflare.ui.styling.onOverlay

@Composable
fun NowPlayingShow (
    mediaId: String
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player

    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.25f),
                shape = thumbnailShape
            )
            .size(Dimensions.thumbnails.song)
    ) {

        if (player?.currentMediaItem?.mediaId == mediaId) {
            MusicBars(
                color = colorPalette.onOverlay,
                modifier = Modifier
                    .height(40.dp)
            )
            /*
            Image(
                painter = painterResource(R.drawable.musical_notes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.onOverlay),
                modifier = Modifier
                    .size(40.dp)
            )
             */
        }
    }

}