package com.kyant.backdrop.catalog

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.BackdropHdrScope
import com.kyant.backdrop.LocalBackdropHdrHeadroom
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class CatalogHdrWallpaper(val title: String, val painter: Painter, val sdrPainter: Painter)

internal val LocalCatalogWallpaperIndex = compositionLocalOf { 0 }
internal val LocalCatalogPickedWallpaper = compositionLocalOf<CatalogHdrWallpaper?> { null }
internal val LocalCatalogSetPickedWallpaper = staticCompositionLocalOf<(CatalogHdrWallpaper) -> Unit> { {} }
internal val LocalCatalogImageGainmap = staticCompositionLocalOf { true }
internal val LocalCatalogHdrWallpapers = staticCompositionLocalOf<List<CatalogHdrWallpaper>> { emptyList() }

/** HDR window controls and shared Ultra HDR wallpaper for the catalog. */
@Composable
fun CatalogHdrDemo(content: @Composable (setControlsVisible: (Boolean) -> Unit) -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val wallpapers by produceState(emptyList(), context) {
        value = withContext(Dispatchers.IO) {
            listOf(
                "Windows · Ultra HDR" to "office.jpg",
                "Station · Ultra HDR" to "train_station_night.jpg"
            ).mapNotNull { (title, asset) ->
                context.assets.open(asset).use { stream ->
                    BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inScaled = false
                    })?.let { bitmap ->
                        val sdr = requireNotNull(
                            bitmap.copy(
                                bitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888,
                                false
                            )
                        )
                        if (Build.VERSION.SDK_INT >= 34) sdr.gainmap = null
                        CatalogHdrWallpaper(
                            title,
                            BitmapPainter(bitmap.asImageBitmap()),
                            BitmapPainter(sdr.asImageBitmap())
                        )
                    }
                }
            }
        }
    }
    // HDR starts at API 34: the display ratio callback, the HDR component output and Ultra HDR
    // gain maps all require it, so the catalog keeps the window in SDR below that by default.
    // The toggle stays available from API 26, where COLOR_MODE_HDR itself exists.
    var componentHdr by rememberSaveable { mutableStateOf(Build.VERSION.SDK_INT >= 34) }
    var imageHdr by rememberSaveable { mutableStateOf(Build.VERSION.SDK_INT >= 34) }
    var wallpaperIndex by rememberSaveable { mutableIntStateOf(0) }
    var pickedWallpaper by remember { mutableStateOf<CatalogHdrWallpaper?>(null) }
    val setPickedWallpaper = remember { { picked: CatalogHdrWallpaper -> pickedWallpaper = picked } }
    val hdr = componentHdr || imageHdr
    var showControls by rememberSaveable { mutableStateOf(false) }
    val setControlsVisible = remember { { visible: Boolean -> showControls = visible } }
    SideEffect {
        if (Build.VERSION.SDK_INT >= 26) {
            activity?.window?.colorMode = if (hdr) ActivityInfo.COLOR_MODE_HDR else ActivityInfo.COLOR_MODE_DEFAULT
        }
        if (Build.VERSION.SDK_INT >= 35) activity?.window?.setDesiredHdrHeadroom(if (hdr) 4f else 1f)
    }
    BackdropHdrScope {
        val headroom = LocalBackdropHdrHeadroom.current
        val backdrop = rememberLayerBackdrop()
        CompositionLocalProvider(
            LocalCatalogHdrStrength provides if (componentHdr && headroom > 1.01f) 4f else 1f,
            LocalCatalogComponentHdr provides componentHdr,
            LocalCatalogImageGainmap provides imageHdr,
            LocalCatalogHdrWallpapers provides wallpapers,
            LocalCatalogWallpaperIndex provides wallpaperIndex,
            LocalCatalogPickedWallpaper provides pickedWallpaper,
            LocalCatalogSetPickedWallpaper provides setPickedWallpaper
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) { content(setControlsVisible) }
                if (showControls) LiquidButton(
                    onClick = { pickedWallpaper = null; wallpaperIndex = (wallpaperIndex + 1) % 3 },
                    backdrop = backdrop,
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).height(40.dp),
                    surfaceColor = Color(0x99000000)
                ) {
                    BasicText(
                        pickedWallpaper?.title ?: wallpapers.getOrNull(wallpaperIndex)?.title
                        ?: if (wallpaperIndex == 2) "Gradient" else "Ultra HDR",
                        Modifier.padding(horizontal = 12.dp), style = TextStyle(Color.White, 12.sp)
                    )
                }
                if (showControls) Column(
                    Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    LiquidButton(
                        onClick = { if (Build.VERSION.SDK_INT >= 26) componentHdr = !componentHdr },
                        backdrop = backdrop, modifier = Modifier.height(40.dp), surfaceColor = Color(0x99000000)
                    ) {
                        BasicText(
                            if (componentHdr) "Components: HDR" else "Components: SDR",
                            Modifier.padding(horizontal = 12.dp), style = TextStyle(Color.White, 13.sp)
                        )
                    }
                    LiquidButton(
                        onClick = { if (Build.VERSION.SDK_INT >= 34) imageHdr = !imageHdr },
                        backdrop = backdrop, modifier = Modifier.height(40.dp), surfaceColor = Color(0x99000000)
                    ) {
                        BasicText(
                            if (imageHdr) "Image: HDR" else "Image: SDR",
                            Modifier.padding(horizontal = 12.dp), style = TextStyle(Color.White, 13.sp)
                        )
                    }
                    BasicText(
                        "Headroom ×${"%.1f".format(headroom)}",
                        Modifier.background(Color(0x66000000)).padding(horizontal = 6.dp, vertical = 2.dp),
                        style = TextStyle(Color.White, 11.sp)
                    )
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()
