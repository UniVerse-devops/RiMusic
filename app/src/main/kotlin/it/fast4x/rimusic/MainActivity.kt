package it.fast4x.rimusic

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.defaultShimmerTheme
import it.fast4x.compose.persist.PersistMap
import it.fast4x.compose.persist.PersistMapOwner
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.requests.playlistPage
import it.fast4x.innertube.requests.song
import it.fast4x.innertube.utils.LocalePreferenceItem
import it.fast4x.innertube.utils.LocalePreferences
import it.fast4x.innertube.utils.ProxyPreferenceItem
import it.fast4x.innertube.utils.ProxyPreferences
import it.fast4x.rimusic.enums.AudioQualityFormat
import it.fast4x.rimusic.enums.CheckUpdateState
import it.fast4x.rimusic.enums.ColorPaletteMode
import it.fast4x.rimusic.enums.ColorPaletteName
import it.fast4x.rimusic.enums.FontType
import it.fast4x.rimusic.enums.HomeScreenTabs
import it.fast4x.rimusic.enums.Languages
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.service.DownloadUtil
import it.fast4x.rimusic.service.PlayerService
import it.fast4x.rimusic.ui.components.BottomSheetMenu
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.screens.albumRoute
import it.fast4x.rimusic.ui.screens.artistRoute
import it.fast4x.rimusic.ui.screens.home.HomeScreen
import it.fast4x.rimusic.ui.screens.player.Player
import it.fast4x.rimusic.ui.screens.player.PlayerSheetState
import it.fast4x.rimusic.ui.screens.player.rememberPlayerSheetState
import it.fast4x.rimusic.ui.screens.playlistRoute
import it.fast4x.rimusic.ui.styling.Appearance
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.ui.styling.colorPaletteOf
import it.fast4x.rimusic.ui.styling.dynamicColorPaletteOf
import it.fast4x.rimusic.ui.styling.typographyOf
import it.fast4x.rimusic.utils.InitDownloader
import it.fast4x.rimusic.utils.OkHttpRequest
import it.fast4x.rimusic.utils.UiTypeKey
import it.fast4x.rimusic.utils.applyFontPaddingKey
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.audioQualityFormatKey
import it.fast4x.rimusic.utils.backgroundProgressKey
import it.fast4x.rimusic.utils.checkUpdateStateKey
import it.fast4x.rimusic.utils.closeWithBackButtonKey
import it.fast4x.rimusic.utils.colorPaletteModeKey
import it.fast4x.rimusic.utils.colorPaletteNameKey
import it.fast4x.rimusic.utils.disableClosingPlayerSwipingDownKey
import it.fast4x.rimusic.utils.disablePlayerHorizontalSwipeKey
import it.fast4x.rimusic.utils.effectRotationKey
import it.fast4x.rimusic.utils.fontTypeKey
import it.fast4x.rimusic.utils.forcePlay
import it.fast4x.rimusic.utils.getEnum
import it.fast4x.rimusic.utils.intent
import it.fast4x.rimusic.utils.isAtLeastAndroid6
import it.fast4x.rimusic.utils.isAtLeastAndroid8
import it.fast4x.rimusic.utils.isEnabledDiscoveryLangCodeKey
import it.fast4x.rimusic.utils.isKeepScreenOnEnabledKey
import it.fast4x.rimusic.utils.isProxyEnabledKey
import it.fast4x.rimusic.utils.keepPlayerMinimizedKey
import it.fast4x.rimusic.utils.languageAppKey
import it.fast4x.rimusic.utils.navigationBarPositionKey
import it.fast4x.rimusic.utils.navigationBarTypeKey
import it.fast4x.rimusic.utils.playerThumbnailSizeKey
import it.fast4x.rimusic.utils.playerVisualizerTypeKey
import it.fast4x.rimusic.utils.preferences
import it.fast4x.rimusic.utils.proxyHostnameKey
import it.fast4x.rimusic.utils.proxyModeKey
import it.fast4x.rimusic.utils.proxyPortKey
import it.fast4x.rimusic.utils.shakeEventEnabledKey
import it.fast4x.rimusic.utils.showButtonPlayerAddToPlaylistKey
import it.fast4x.rimusic.utils.showButtonPlayerArrowKey
import it.fast4x.rimusic.utils.showButtonPlayerDownloadKey
import it.fast4x.rimusic.utils.showButtonPlayerLoopKey
import it.fast4x.rimusic.utils.showButtonPlayerLyricsKey
import it.fast4x.rimusic.utils.showButtonPlayerMenuKey
import it.fast4x.rimusic.utils.showButtonPlayerShuffleKey
import it.fast4x.rimusic.utils.showButtonPlayerSleepTimerKey
import it.fast4x.rimusic.utils.showButtonPlayerSystemEqualizerKey
import it.fast4x.rimusic.utils.showSearchTabKey
import it.fast4x.rimusic.utils.showTotalTimeQueueKey
import it.fast4x.rimusic.utils.thumbnailRoundnessKey
import it.fast4x.rimusic.utils.useSystemFontKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File
import java.net.Proxy
import java.util.Locale
import java.util.Objects
import kotlin.math.sqrt


@UnstableApi
class MainActivity : AppCompatActivity(), PersistMapOwner {

    var downloadUtil = DownloadUtil

    var client = OkHttpClient()
    var request = OkHttpRequest(client)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlayerService.Binder) {
                this@MainActivity.binder = service
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    private var binder by mutableStateOf<PlayerService.Binder?>(null)

    override lateinit var persistMap: PersistMap

    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var shakeCounter = 0

    override fun onStart() {
        super.onStart()
        startService(Intent(this, PlayerService::class.java))
        bindService(intent<PlayerService>(), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    @ExperimentalMaterialApi
    @ExperimentalTextApi
    @UnstableApi
    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var splashScreenStays = true
        val delayTime = 800L

        installSplashScreen().setKeepOnScreenCondition { splashScreenStays }
        Handler(Looper.getMainLooper()).postDelayed({ splashScreenStays = false }, delayTime)


        if (!preferences.getBoolean(closeWithBackButtonKey, false))
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                //Log.d("onBackPress", "yeah")
            }
        }


        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        persistMap = lastCustomNonConfigurationInstance as? PersistMap ?: PersistMap()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val launchedFromNotification = intent?.extras?.getBoolean("expandPlayerBottomSheet") == true

        with(preferences){
            if(getBoolean(isKeepScreenOnEnabledKey,false)) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            if(getBoolean(isProxyEnabledKey,false)) {
                val hostName = getString(proxyHostnameKey,null)
                val proxyPort = getInt(proxyPortKey, 8080)
                val proxyMode = getEnum(proxyModeKey, Proxy.Type.HTTP)
                hostName?.let { hName->
                    ProxyPreferences.preference = ProxyPreferenceItem(hName,proxyPort,proxyMode)
                }
            }
            if(getBoolean(isEnabledDiscoveryLangCodeKey,true))
                LocalePreferences.preference = LocalePreferenceItem(Locale.getDefault().toLanguageTag(),"")
        }

        setContent {

            if (preferences.getEnum(checkUpdateStateKey, CheckUpdateState.Ask) == CheckUpdateState.Enabled) {
                val urlVersionCode =
                    "https://raw.githubusercontent.com/fast4x/RiMusic/master/updatedVersion/updatedVersionCode.ver"
                //val urlVersionCode = "https://rimusic.xyz/update/updatedVersionCode.ver"
                request.GET(urlVersionCode, object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val responseData = response.body?.string()
                        runOnUiThread {
                            try {
                                if (responseData != null) {
                                    val file = File(filesDir, "RiMusicUpdatedVersionCode.ver")
                                    file.writeText(responseData.toString())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                    }

                    override fun onFailure(call: Call, e: java.io.IOException) {
                        Log.d("UpdatedVersionCode", "Check failure")
                    }
                })
            }

            val coroutineScope = rememberCoroutineScope()
            val isSystemInDarkTheme = isSystemInDarkTheme()

            preferences.getEnum(audioQualityFormatKey, AudioQualityFormat.Auto)

            var appearance by rememberSaveable(
                isSystemInDarkTheme,
                stateSaver = Appearance.Companion
            ) {
                with(preferences) {
                    val colorPaletteName = getEnum(colorPaletteNameKey, ColorPaletteName.ModernBlack)
                    val colorPaletteMode = getEnum(colorPaletteModeKey, ColorPaletteMode.System)
                    val thumbnailRoundness =
                        getEnum(thumbnailRoundnessKey, ThumbnailRoundness.Heavy)
                    val useSystemFont = getBoolean(useSystemFontKey, false)
                    val applyFontPadding = getBoolean(applyFontPaddingKey, false)

                    val colorPalette =
                        colorPaletteOf(colorPaletteName, colorPaletteMode, isSystemInDarkTheme)

                    val fontType = getEnum(fontTypeKey, FontType.Rubik)

                    setSystemBarAppearance(colorPalette.isDark)

                    mutableStateOf(
                        Appearance(
                            colorPalette = colorPalette,
                            typography = typographyOf(colorPalette.text, useSystemFont, applyFontPadding, fontType),
                            thumbnailShape = thumbnailRoundness.shape()
                        )
                    )
                }

            }



            DisposableEffect(binder, isSystemInDarkTheme) {
                var bitmapListenerJob: Job? = null

                fun setDynamicPalette(colorPaletteMode: ColorPaletteMode) {
                    val isDark =
                        colorPaletteMode == ColorPaletteMode.Dark || (colorPaletteMode == ColorPaletteMode.System && isSystemInDarkTheme)
                    val isPicthBlack = colorPaletteMode == ColorPaletteMode.PitchBlack

                    binder?.setBitmapListener { bitmap: Bitmap? ->
                        if (bitmap == null) {
                            val colorPalette =
                                colorPaletteOf(
                                    ColorPaletteName.Dynamic,
                                    colorPaletteMode,
                                    isSystemInDarkTheme
                                )

                            setSystemBarAppearance(colorPalette.isDark)

                            appearance = appearance.copy(
                                colorPalette = colorPalette,
                                typography = appearance.typography.copy(colorPalette.text)
                            )

                            return@setBitmapListener
                        }

                        bitmapListenerJob = coroutineScope.launch(Dispatchers.IO) {
                            dynamicColorPaletteOf(bitmap, isDark, isPicthBlack)?.let {
                                withContext(Dispatchers.Main) {
                                    setSystemBarAppearance(it.isDark)
                                }
                                appearance = appearance.copy(
                                    colorPalette = it,
                                    typography = appearance.typography.copy(it.text)
                                )
                            }
                        }
                    }
                }

                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                        when (key) {

                            languageAppKey -> {
                                var lang = sharedPreferences.getEnum(
                                    languageAppKey,
                                    Languages.English
                                )

                                //val precLangCode = LocaleListCompat.getDefault().get(0).toString()
                                val systemLangCode = AppCompatDelegate.getApplicationLocales().get(0).toString()
                                //Log.d("LanguageActivity", "lang.code ${lang.code} precLangCode $precLangCode systemLangCode $systemLangCode")

                                val sysLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(systemLangCode)
                                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(lang.code)
                                AppCompatDelegate.setApplicationLocales( if (lang.code == "") sysLocale else appLocale )
                            }

                            effectRotationKey, playerThumbnailSizeKey,
                            playerVisualizerTypeKey,
                            UiTypeKey,
                            disablePlayerHorizontalSwipeKey,
                            audioQualityFormatKey,
                            showButtonPlayerArrowKey,
                            showButtonPlayerAddToPlaylistKey,
                            showButtonPlayerDownloadKey,
                            showButtonPlayerLoopKey,
                            showButtonPlayerLyricsKey,
                            showButtonPlayerShuffleKey,
                            showButtonPlayerSleepTimerKey,
                            showButtonPlayerMenuKey,
                            disableClosingPlayerSwipingDownKey,
                            showSearchTabKey,
                            navigationBarPositionKey,
                            navigationBarTypeKey,
                            showTotalTimeQueueKey,
                            backgroundProgressKey,
                            showButtonPlayerSystemEqualizerKey -> {
                                this@MainActivity.recreate()
                            }

                            colorPaletteNameKey, colorPaletteModeKey -> {
                                val colorPaletteName =
                                    sharedPreferences.getEnum(
                                        colorPaletteNameKey,
                                        ColorPaletteName.Dynamic
                                    )

                                val colorPaletteMode =
                                    sharedPreferences.getEnum(
                                        colorPaletteModeKey,
                                        ColorPaletteMode.System
                                    )

                                if (colorPaletteName == ColorPaletteName.Dynamic) {
                                    setDynamicPalette(colorPaletteMode)
                                } else {
                                    bitmapListenerJob?.cancel()
                                    binder?.setBitmapListener(null)

                                    val colorPalette = colorPaletteOf(
                                        colorPaletteName,
                                        colorPaletteMode,
                                        isSystemInDarkTheme
                                    )

                                    setSystemBarAppearance(colorPalette.isDark)

                                    appearance = appearance.copy(
                                        colorPalette = colorPalette,
                                        typography = appearance.typography.copy(colorPalette.text),
                                    )
                                }
                            }

                            thumbnailRoundnessKey -> {
                                val thumbnailRoundness =
                                    sharedPreferences.getEnum(key, ThumbnailRoundness.Heavy)

                                appearance = appearance.copy(
                                    thumbnailShape = thumbnailRoundness.shape()
                                )
                            }

                            useSystemFontKey, applyFontPaddingKey, fontTypeKey -> {
                                val useSystemFont = sharedPreferences.getBoolean(useSystemFontKey, false)
                                val applyFontPadding = sharedPreferences.getBoolean(applyFontPaddingKey, false)
                                val fontType = sharedPreferences.getEnum(fontTypeKey, FontType.Rubik)

                                appearance = appearance.copy(
                                    typography = typographyOf(appearance.colorPalette.text, useSystemFont, applyFontPadding, fontType),
                                )
                            }
                        }
                    }

                with(preferences) {
                    registerOnSharedPreferenceChangeListener(listener)

                    val colorPaletteName = getEnum(colorPaletteNameKey, ColorPaletteName.ModernBlack)
                    if (colorPaletteName == ColorPaletteName.Dynamic) {
                        setDynamicPalette(getEnum(colorPaletteModeKey, ColorPaletteMode.System))
                    }

                    onDispose {
                        bitmapListenerJob?.cancel()
                        binder?.setBitmapListener(null)
                        unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }
            }

            val rippleTheme =
                remember(appearance.colorPalette.text, appearance.colorPalette.isDark) {
                    object : RippleTheme {
                        @Composable
                        override fun defaultColor(): Color = RippleTheme.defaultRippleColor(
                            contentColor = appearance.colorPalette.text,
                            lightTheme = !appearance.colorPalette.isDark
                        )

                        @Composable
                        override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
                            contentColor = appearance.colorPalette.text,
                            lightTheme = !appearance.colorPalette.isDark
                        )
                    }
                }

            val shimmerTheme = remember {
                defaultShimmerTheme.copy(
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 800,
                            easing = LinearEasing,
                            delayMillis = 250,
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    shaderColors = listOf(
                        Color.Unspecified.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.50f),
                        Color.Unspecified.copy(alpha = 0.25f),
                    ),
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appearance.colorPalette.background0)
            ) {
                val density = LocalDensity.current
                val windowsInsets = WindowInsets.systemBars
                val bottomDp = with(density) { windowsInsets.getBottom(density).toDp() }

                /*
                val playerBottomSheetState = rememberBottomSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = Dimensions.collapsedPlayer + bottomDp,
                    expandedBound = maxHeight,
                )

                val playerAwareWindowInsets by remember(bottomDp, playerBottomSheetState.value) {
                    derivedStateOf {
                        val bottom = playerBottomSheetState.value.coerceIn(bottomDp, playerBottomSheetState.collapsedBound)

                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(bottom = bottom))
                    }
                }
                 */
                //change bottom navigation
                val playerSheetState = rememberPlayerSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = Dimensions.collapsedPlayer + bottomDp,
                    //collapsedBound = Dimensions.collapsedPlayer, // bottom navigation
                    expandedBound = maxHeight,
                )

                val playerAwareWindowInsets by remember(
                    bottomDp,
                    playerSheetState.value
                ) {
                    derivedStateOf {
                        val bottom = playerSheetState.value.coerceIn(bottomDp, playerSheetState.collapsedBound)

                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(bottom = bottom))
                    }
                }

                val openTabFromShortcut = remember {
                    when (intent?.action) {
                        action_songs -> HomeScreenTabs.Songs.index
                        action_albums -> HomeScreenTabs.Albums.index
                        action_library -> HomeScreenTabs.Library.index
                        action_search -> -2
                        else -> -1
                    }
                }

                CompositionLocalProvider(
                    LocalAppearance provides appearance,
                    LocalIndication provides rememberRipple(bounded = true),
                    LocalRippleTheme provides rippleTheme,
                    LocalShimmerTheme provides shimmerTheme,
                    LocalPlayerServiceBinder provides binder,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalLayoutDirection provides LayoutDirection.Ltr,
                    LocalDownloader provides downloadUtil,
                    LocalPlayerSheetState provides playerSheetState
                ) {

                    HomeScreen(
                        onPlaylistUrl = { url ->
                            onNewIntent(Intent.parseUri(url, 0))
                        },
                        openTabFromShortcut = openTabFromShortcut
                    )

                    /*
                    Player(
                        layoutState = playerBottomSheetState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    )
                     */
                    Player(
                        layoutState = playerSheetState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    )

                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    )

                }

                DisposableEffect(binder?.player) {
                    val player = binder?.player ?: return@DisposableEffect onDispose { }

                    /*
                    if (player.currentMediaItem == null) {
                        if (!playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.dismiss()
                        }
                    } else {
                        //if (playerBottomSheetState.isDismissed) {
                            if (launchedFromNotification) {
                                intent.replaceExtras(Bundle())
                                if (preferences.getBoolean(keepPlayerMinimizedKey, true))
                                    playerBottomSheetState.collapse(tween(700))
                                else playerBottomSheetState.expand(tween(500))
                            } else {
                                playerBottomSheetState.collapse(tween(700))
                            }
                        //}
                    }

                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null) {
                                if (mediaItem.mediaMetadata.extras?.getBoolean("isFromPersistentQueue") != true) {
                                    if (preferences.getBoolean(keepPlayerMinimizedKey, true))
                                    playerBottomSheetState.collapse(tween(700))
                                    else playerBottomSheetState.expand(tween(500))
                                } else {
                                    playerBottomSheetState.collapse(tween(700))
                                }
                            }
                        }
                    }
                     */
                    if (player.currentMediaItem == null) {
                        if (!playerSheetState.isDismissed) {
                            playerSheetState.dismiss()
                        }
                    } else {
                        //if (playerSheetState.isDismissed) {
                        if (launchedFromNotification) {
                            intent.replaceExtras(Bundle())
                            if (preferences.getBoolean(keepPlayerMinimizedKey, true))
                                playerSheetState.collapse(tween(700))
                            else playerSheetState.expand(tween(500))
                        } else {
                            playerSheetState.collapse(tween(700))
                        }
                        //}
                    }

                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null) {
                                if (mediaItem.mediaMetadata.extras?.getBoolean("isFromPersistentQueue") != true) {
                                    if (preferences.getBoolean(keepPlayerMinimizedKey, true))
                                        playerSheetState.collapse(tween(700))
                                    else playerSheetState.expand(tween(500))
                                } else {
                                    playerSheetState.collapse(tween(700))
                                }
                            }
                        }
                    }

                    player.addListener(listener)

                    onDispose { player.removeListener(listener) }
                }

                //VisualizerComputer.setupPermissions(this@MainActivity)
                //if (isConnected)
                InitDownloader()

            }
        }

        onNewIntent(intent)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        Objects.requireNonNull(sensorManager)!!
            .registerListener(sensorListener, sensorManager!!
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)

    }

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            if (preferences.getBoolean(shakeEventEnabledKey, false)) {
                // Fetching x,y,z values
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                lastAcceleration = currentAcceleration

                // Getting current accelerations
                // with the help of fetched x,y,z values
                currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta: Float = currentAcceleration - lastAcceleration
                acceleration = acceleration * 0.9f + delta

                // Display a Toast message if
                // acceleration value is over 12
                if (acceleration > 12) {
                    shakeCounter++
                    //Toast.makeText(applicationContext, "Shake event detected", Toast.LENGTH_SHORT).show()
                }
                if (shakeCounter >= 1) {
                    //Toast.makeText(applicationContext, "Shaked $shakeCounter times", Toast.LENGTH_SHORT).show()
                    shakeCounter = 0
                    binder?.player?.seekToNextMediaItem()
                }

            }

        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    override fun onResume() {
        sensorManager?.registerListener(sensorListener, sensorManager!!.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
        )
        super.onResume()
    }

    override fun onPause() {
        sensorManager!!.unregisterListener(sensorListener)
        super.onPause()
    }

    @UnstableApi
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

/*
               val action = intent.action
               val type = intent.type
               val data = intent.data

               Log.d("ShareActionInfo","Share action received action / type / data ${action} / ${type} / ${data}")
               if ("android.intent.action.SEND" == action && type != null && "text/plain" == type) {
                   Log.d("ShareActionTextExtra", intent.getStringExtra("android.intent.extra.TEXT")!!)
               }
*/

        //val uri = intent.getStringExtra("android.intent.extra.TEXT")?.toUri() ?: return
        val uri = intent.data ?: intent.getStringExtra("android.intent.extra.TEXT")?.toUri() ?: return
        //val uri = intent?.data ?: return

        intent.data = null
        this.intent = null

        Toast.makeText(this, "${"RiMusic "}${getString(R.string.opening_url)}", Toast.LENGTH_LONG).show()

        lifecycleScope.launch(Dispatchers.IO) {
            when (val path = uri.pathSegments.firstOrNull()) {
                "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                    val browseId = "VL$playlistId"

                    if (playlistId.startsWith("OLAK5uy_")) {
                        Innertube.playlistPage(BrowseBody(browseId = browseId))?.getOrNull()?.let {
                            it.songsPage?.items?.firstOrNull()?.album?.endpoint?.browseId?.let { browseId ->
                                albumRoute.ensureGlobal(browseId)
                            }
                        }
                    } else {
                        //playlistRoute.ensureGlobal(browseId)
                        //playlistRoute.ensureGlobal(browseId, uri.getQueryParameter("params"))
                        //playlistRoute.ensureGlobal(browseId,null)
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

    @Deprecated("Deprecated in Java", ReplaceWith("persistMap"))
    override fun onRetainCustomNonConfigurationInstance() = persistMap

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    @UnstableApi
    override fun onDestroy() {
        super.onDestroy()
        //stopService(Intent(this, MyDownloadService::class.java))
        //stopService(Intent(this, PlayerService::class.java))
        //Log.d("rimusic debug","onDestroy")
        if (!isChangingConfigurations) {
            persistMap.clear()
        }

    }

    private fun setSystemBarAppearance(isDark: Boolean) {
        with(WindowCompat.getInsetsController(window, window.decorView.rootView)) {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        if (!isAtLeastAndroid6) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }

        if (!isAtLeastAndroid8) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val action_search = "it.fast4x.rimusic.action.search"
        const val action_songs = "it.fast4x.rimusic.action.songs"
        const val action_albums = "it.fast4x.rimusic.action.albums"
        const val action_library = "it.fast4x.rimusic.action.library"
    }

}

val LocalPlayerServiceBinder = staticCompositionLocalOf<PlayerService.Binder?> { null }

val LocalPlayerAwareWindowInsets = staticCompositionLocalOf<WindowInsets> { TODO() }

val LocalDownloader = staticCompositionLocalOf<DownloadUtil> { error("No Downloader provided") }

val LocalPlayerSheetState = staticCompositionLocalOf<PlayerSheetState> { error("No player sheet state provided") }



