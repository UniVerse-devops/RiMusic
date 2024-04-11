package com.universe.audioflare.ui.screens.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import com.universe.audioflare.Database
import com.universe.audioflare.LocalPlayerAwareWindowInsets
import com.universe.audioflare.R
import com.universe.audioflare.enums.NavigationBarPosition
import com.universe.audioflare.enums.PlayEventsType
import com.universe.audioflare.query
import com.universe.audioflare.ui.components.themed.ConfirmationDialog
import com.universe.audioflare.ui.components.themed.HeaderWithIcon
import com.universe.audioflare.ui.styling.LocalAppearance
import com.universe.audioflare.utils.contentWidthKey
import com.universe.audioflare.utils.isEnabledDiscoveryLangCodeKey
import com.universe.audioflare.utils.navigationBarPositionKey
import com.universe.audioflare.utils.playEventsTypeKey
import com.universe.audioflare.utils.preferences
import com.universe.audioflare.utils.rememberPreference
import com.universe.audioflare.utils.showNewAlbumsArtistsKey
import com.universe.audioflare.utils.showNewAlbumsKey
import com.universe.audioflare.utils.showPlaylistMightLikeKey
import com.universe.audioflare.utils.showRelatedAlbumsKey
import com.universe.audioflare.utils.showSimilarArtistsKey
import kotlinx.coroutines.flow.distinctUntilChanged

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun  QuickPicsSettings() {
    val (colorPalette) = LocalAppearance.current
    var playEventType by rememberPreference(
        playEventsTypeKey,
        PlayEventsType.MostPlayed
    )
    var showRelatedAlbums by rememberPreference(showRelatedAlbumsKey, true)
    var showSimilarArtists by rememberPreference(showSimilarArtistsKey, true)
    var showNewAlbumsArtists by rememberPreference(showNewAlbumsArtistsKey, true)
    var showNewAlbums by rememberPreference(showNewAlbumsKey, true)
    var showPlaylistMightLike by rememberPreference(showPlaylistMightLikeKey, true)
    val eventsCount by remember {
        Database.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)
    var clearEvents by remember { mutableStateOf(false) }
    if (clearEvents) {
        ConfirmationDialog(
            text = stringResource(R.string.do_you_really_want_to_delete_all_playback_events),
            onDismiss = { clearEvents = false },
            onConfirm = { query(Database::clearEvents) }
        )
    }

    var isEnabledDiscoveryLangCode by rememberPreference(isEnabledDiscoveryLangCodeKey,   true)

    val context = LocalContext.current
    val navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Left)
    val contentWidth = context.preferences.getFloat(contentWidthKey,0.8f)

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(if (navigationBarPosition == NavigationBarPosition.Left) 1f else contentWidth)
            .verticalScroll(rememberScrollState())
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        HeaderWithIcon(
            title = stringResource(R.string.quick_picks),
            iconId = R.drawable.sparkles,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )

        //SettingsGroupSpacer()

        EnumValueSelectorSettingsEntry(
            title = stringResource(R.string.tips),
            selectedValue = playEventType,
            onValueSelected = { playEventType = it },
            valueText = {
                when (it) {
                    PlayEventsType.MostPlayed -> stringResource(R.string.by_most_played_song)
                    PlayEventsType.LastPlayed -> stringResource(R.string.by_last_played_song)
                }
            }
        )

        //SettingsGroupSpacer()

        SwitchSettingEntry(
            title = "${stringResource(R.string.show)} ${stringResource(R.string.related_albums)}",
            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " +stringResource(R.string.related_albums),
            isChecked = showRelatedAlbums,
            onCheckedChange = {
                showRelatedAlbums = it
            }
        )

        //SettingsGroupSpacer()

        SwitchSettingEntry(
            title = "${stringResource(R.string.show)} ${stringResource(R.string.similar_artists)}",
            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " +stringResource(R.string.similar_artists),
            isChecked = showSimilarArtists,
            onCheckedChange = {
                showSimilarArtists = it
            }
        )


        //SettingsGroupSpacer()

        SwitchSettingEntry(
            title = "${stringResource(R.string.show)} ${stringResource(R.string.new_albums_of_your_artists)}",
            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " +stringResource(R.string.new_albums_of_your_artists),
            isChecked = showNewAlbumsArtists,
            onCheckedChange = {
                showNewAlbumsArtists = it
            }
        )

        SwitchSettingEntry(
            title = "${stringResource(R.string.show)} ${stringResource(R.string.new_albums)}",
            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " +stringResource(R.string.new_albums),
            isChecked = showNewAlbums,
            onCheckedChange = {
                showNewAlbums = it
            }
        )

        //SettingsGroupSpacer()

        SwitchSettingEntry(
            title = "${stringResource(R.string.show)} ${stringResource(R.string.playlists_you_might_like)}",
            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " +stringResource(R.string.playlists_you_might_like),
            isChecked = showPlaylistMightLike,
            onCheckedChange = {
                showPlaylistMightLike = it
            }
        )

        SwitchSettingEntry(
            title = stringResource(R.string.enable_language_in_discovery),
            text = stringResource(R.string.if_possible_allows_discovery_content_language),
            isChecked = isEnabledDiscoveryLangCode,
            onCheckedChange = {
                isEnabledDiscoveryLangCode = it
            }
        )
        SettingsDescription(text = stringResource(R.string.restarting_audioflare_is_required))

        SettingsEntry(
            title = stringResource(R.string.reset_quick_picks),
            text = if (eventsCount > 0) {
                stringResource(R.string.delete_playback_events, eventsCount)
            } else {
                stringResource(R.string.quick_picks_are_cleared)
            },
            isEnabled = eventsCount > 0,
            onClick = { clearEvents = true }
        )

    }
}
