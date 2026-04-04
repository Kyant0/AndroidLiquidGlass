package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.kyant.backdrop.catalog.R

const val PROFILE_FRAME_RING_ID = "profile_frame"

fun isAnimatedProfileFrame(profileRing: String?): Boolean {
    return profileRing.equals(PROFILE_FRAME_RING_ID, ignoreCase = true)
}

@Composable
fun ProfileFrameLottie(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.profile_frame))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isPlaying
    )

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier,
            contentScale = ContentScale.Fit,
            clipToCompositionBounds = true
        )
    } else {
        Image(
            painter = painterResource(R.drawable.profile_frame_static),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.fillMaxSize()
        )
    }
}
