package com.universe.audioflare.ui.components

import androidx.annotation.OptIn
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.universe.audioflare.LocalPlayerServiceBinder
import com.universe.audioflare.LocalPlayerSheetState
import com.universe.audioflare.R
import com.universe.audioflare.enums.NavigationBarPosition
import com.universe.audioflare.enums.NavigationBarType
import com.universe.audioflare.ui.styling.Dimensions
import com.universe.audioflare.ui.styling.LocalAppearance
import com.universe.audioflare.ui.styling.favoritesIcon
import com.universe.audioflare.utils.navigationBarPositionKey
import com.universe.audioflare.utils.navigationBarTypeKey
import com.universe.audioflare.utils.rememberPreference
import com.universe.audioflare.utils.semiBold

@OptIn(UnstableApi::class)
@Composable
inline fun NavigationRailTB(
    topIconButtonId: Int,
    noinline onTopIconButtonClick: () -> Unit,
    topIconButton2Id: Int,
    noinline onTopIconButton2Click: () -> Unit,
    showButton2: Boolean,
    bottomIconButtonId: Int? = R.drawable.search,
    noinline onBottomIconButtonClick: () -> Unit,
    showBottomButton: Boolean? = false,
    tabIndex: Int,
    crossinline onTabIndexChanged: (Int) -> Unit,
    content: @Composable() (ColumnScope.(@Composable (Int, String, Int) -> Unit) -> Unit),
    hideTabs: Boolean? = false,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current

    /*
    val isLandscape = isLandscape

    val paddingValues = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start).asPaddingValues()

     */

    val navigationBarType by rememberPreference(navigationBarTypeKey, NavigationBarType.IconAndText)
    val density = LocalDensity.current
    val windowsInsets = WindowInsets.systemBars
    val bottomDp = with(density) { windowsInsets.getBottom(density).toDp() }

    val navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Top)
    val localSheetState = LocalPlayerSheetState.current
    val bottomPadding = if (localSheetState.isCollapsed) bottomDp + Dimensions.collapsedPlayer else bottomDp
    val topPadding = if (navigationBarPosition == NavigationBarPosition.Top) 30.dp else 0.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
            //.padding(top = 30.dp)
            .padding(top = topPadding, bottom = bottomPadding) //bottom navigation
            .background(colorPalette.background0)
    ) {

        if (hideTabs == false)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val transition = updateTransition(targetState = tabIndex, label = null)

                val listIconContent = mutableListOf<@Composable () -> Unit>()

                content { index, text, icon ->

                    val textColor by transition.animateColor(label = "") {
                        if (it == index) colorPalette.text else colorPalette.textDisabled
                    }

                    val contentModifier = Modifier
                        .clickable(onClick = { onTabIndexChanged(index) })

                    val itemContent: @Composable () -> Unit = {
                        Box(
                            modifier = contentModifier
                        ) {
                            if (navigationBarType == NavigationBarType.IconOnly) {
                                Image(
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(textColor),
                                    modifier = Modifier
                                        .padding(all = 12.dp)
                                        .size(24.dp)
                                )
                            } else {
                                Column (
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ){
                                    Image(
                                        painter = painterResource(icon),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(textColor),
                                        modifier = Modifier
                                            .size(Dimensions.navigationRailIconOffset * 2)
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    BasicText(
                                        text = text,
                                        style = TextStyle(
                                            fontSize = typography.xs.semiBold.fontSize,
                                            fontWeight = typography.xs.semiBold.fontWeight,
                                            color = textColor,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }


                    listIconContent.add { itemContent() }

                   }

                val scrollState = rememberScrollState()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 8.dp)
                        .padding(top = 10.dp, bottom = 10.dp)

                ) {
                    listIconContent.forEach {
                        it()
                    }


                    if (showBottomButton == true)
                        Image(
                            painter = painterResource(bottomIconButtonId ?: R.drawable.search ),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                            modifier = Modifier
                                .clickable(onClick = onBottomIconButtonClick)
                                .padding(all = 4.dp)
                                .size(24.dp)
                        )

                    if (showButton2) {
                        Image(
                            painter = painterResource(topIconButton2Id),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onTopIconButton2Click)
                                .padding(all = 4.dp)
                                .size(20.dp)
                        )
                    }
                    Image(
                        painter = painterResource(topIconButtonId),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.favoritesIcon), //ColorFilter.tint(colorPalette.textSecondary),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onTopIconButtonClick)
                            .padding(all = 4.dp)
                            .size(20.dp)
                    )


                }

            }


    }
}
