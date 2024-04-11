package com.universe.audioflare.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.universe.compose.persist.persist
import com.universe.audioflare.Database
import com.universe.audioflare.LocalPlayerAwareWindowInsets
import com.universe.audioflare.R
import com.universe.audioflare.enums.AlbumSortBy
import com.universe.audioflare.enums.NavigationBarPosition
import com.universe.audioflare.enums.SortOrder
import com.universe.audioflare.enums.UiType
import com.universe.audioflare.models.Album
import com.universe.audioflare.ui.components.LocalMenuState
import com.universe.audioflare.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.universe.audioflare.ui.components.themed.HeaderIconButton
import com.universe.audioflare.ui.components.themed.HeaderInfo
import com.universe.audioflare.ui.components.themed.HeaderWithIcon
import com.universe.audioflare.ui.components.themed.SortMenu
import com.universe.audioflare.ui.items.AlbumItem
import com.universe.audioflare.ui.styling.Dimensions
import com.universe.audioflare.ui.styling.LocalAppearance
import com.universe.audioflare.ui.styling.px
import com.universe.audioflare.utils.UiTypeKey
import com.universe.audioflare.utils.albumSortByKey
import com.universe.audioflare.utils.albumSortOrderKey
import com.universe.audioflare.utils.contentWidthKey
import com.universe.audioflare.utils.navigationBarPositionKey
import com.universe.audioflare.utils.preferences
import com.universe.audioflare.utils.rememberPreference
import com.universe.audioflare.utils.semiBold
import com.universe.audioflare.utils.showSearchTabKey

@ExperimentalTextApi
@UnstableApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeAlbums(
    onAlbumClick: (Album) -> Unit,
    onSearchClick: () -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val menuState = LocalMenuState.current

    val uiType  by rememberPreference(UiTypeKey, UiType.audioflare)

    var sortBy by rememberPreference(albumSortByKey, AlbumSortBy.DateAdded)
    var sortOrder by rememberPreference(albumSortOrderKey, SortOrder.Descending)

    var items by persist<List<Album>>(tag = "home/albums", emptyList())

    LaunchedEffect(sortBy, sortOrder) {
        Database.albums(sortBy, sortOrder).collect { items = it }
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song * 2
    val thumbnailSizePx = thumbnailSizeDp.px

    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing), label = ""
    )

    val context = LocalContext.current
    val navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Left)
    val contentWidth = context.preferences.getFloat(contentWidthKey,0.8f)
/*
    var showSortTypeSelectDialog by remember {
        mutableStateOf(false)
    }

 */

    val lazyListState = rememberLazyListState()

    val showSearchTab by rememberPreference(showSearchTabKey, false)
    //val effectRotationEnabled by rememberPreference(effectRotationKey, true)
    var isRotated by rememberSaveable { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRotated) 360F else 0f,
        animationSpec = tween(durationMillis = 300), label = ""
    )

    Box (
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

                HeaderWithIcon(
                    title = stringResource(R.string.albums),
                    iconId = R.drawable.search,
                    enabled = true,
                    showIcon = !showSearchTab,
                    modifier = Modifier,
                    onClick = onSearchClick
                )

                Row (
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth()
                ){
                    HeaderInfo(
                        title = "${items.size}",
                        icon = painterResource(R.drawable.album),
                        spacer = 0
                    )

                    HeaderIconButton(
                        modifier = Modifier.rotate(rotationAngle),
                        icon = R.drawable.dice,
                        enabled = items.isNotEmpty() ,
                        color = colorPalette.text,
                        onClick = {
                            isRotated = !isRotated
                            onAlbumClick(items.get((0..<items.size).random()))
                        },
                        iconSize = 16.dp
                    )

                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                    )



                    BasicText(
                        text = when (sortBy) {
                            AlbumSortBy.Title -> stringResource(R.string.sort_title)
                            AlbumSortBy.Year -> stringResource(R.string.sort_year)
                            AlbumSortBy.DateAdded -> stringResource(R.string.sort_date_added)
                        },
                        style = typography.xs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable {
                                menuState.display{
                                    SortMenu(
                                        title = stringResource(R.string.sorting_order),
                                        onDismiss = menuState::hide,
                                        onTitle = { sortBy = AlbumSortBy.Title },
                                        onYear = { sortBy = AlbumSortBy.Year },
                                        onDateAdded = { sortBy = AlbumSortBy.DateAdded },
                                    )
                                }
                                //showSortTypeSelectDialog = true
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

            items(
                items = items,
                key = Album::id
            ) { album ->
                AlbumItem(
                    album = album,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    modifier = Modifier
                        .combinedClickable(
                            /*
                            onLongClick = {
                                menuState.display {
                                    AlbumsItemMenu(
                                        onDismiss = menuState::hide,
                                        album = album
                                    )
                                }
                            },
                             */
                            onClick = {
                                onAlbumClick(album)
                            }
                        )

                        .animateItemPlacement()
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)

        if(uiType == UiType.ViMusic)
        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = R.drawable.search,
            onClick = onSearchClick
        )


    }
}
