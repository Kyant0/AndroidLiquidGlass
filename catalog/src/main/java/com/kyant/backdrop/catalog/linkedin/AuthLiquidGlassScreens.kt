package com.kyant.backdrop.catalog.linkedin

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.utils.rememberUISensor
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur as backdropBlur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun LiquidGlassLoginScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    isGoogleLoading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LiquidGlassAuthFrame(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        title = "Vormex",
        subtitle = "Sign in to continue",
        modeLabel = "RETURNING MEMBER",
        scrollable = false
    ) {
        AuthInputField(
            value = email,
            onValueChange = {
                email = it
                onClearError()
            },
            placeholder = "Email",
            iconRes = R.drawable.ic_email,
            accentColor = accentColor
        )

        AuthInputField(
            value = password,
            onValueChange = {
                password = it
                onClearError()
            },
            placeholder = "Password",
            iconRes = R.drawable.ic_lock,
            accentColor = accentColor,
            visualTransformation = PasswordVisualTransformation()
        )

        error?.let { AuthInlineError(message = it) }

        LiquidButton(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank() && !isLoading) {
                    onLogin(email, password)
                }
            },
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            tint = accentColor,
            isInteractive = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                BasicText(
                    "Sign In",
                    style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
                )
            }
        }

        AuthDivider(contentColor = contentColor)

        AuthGoogleButton(
            backdrop = backdrop,
            contentColor = contentColor,
            isLoading = isGoogleLoading,
            isEnabled = !isGoogleLoading && !isLoading,
            onClick = onGoogleSignIn
        )

        BasicText(
            "Forgot password?",
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable {
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isBlank()) {
                        Toast.makeText(context, "Enter your email first", Toast.LENGTH_SHORT).show()
                    } else {
                        onForgotPassword(trimmedEmail)
                    }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
        )

        AuthFooterRow(
            prompt = "New to Vormex?",
            action = "Create account",
            accentColor = accentColor,
            contentColor = contentColor,
            onClick = onSignUpClick
        )
    }
}

@Composable
internal fun LiquidGlassSignUpScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    isGoogleLoading: Boolean,
    error: String?,
    onSignUp: (email: String, password: String, name: String, username: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onLoginClick: () -> Unit,
    onClearError: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

    LiquidGlassAuthFrame(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        title = "Create Account",
        subtitle = "Drop into the Vormex network",
        modeLabel = "NEW MEMBER",
        scrollable = true
    ) {
        AuthInputField(
            value = name,
            onValueChange = {
                name = it
                onClearError()
            },
            placeholder = "Full name",
            iconRes = R.drawable.ic_profile,
            accentColor = accentColor
        )

        AuthInputField(
            value = username,
            onValueChange = {
                username = it.lowercase().filter { char -> char.isLetterOrDigit() || char == '_' }
                onClearError()
            },
            placeholder = "Username",
            iconRes = R.drawable.ic_mention,
            accentColor = accentColor
        )

        AuthInputField(
            value = email,
            onValueChange = {
                email = it
                onClearError()
            },
            placeholder = "Email",
            iconRes = R.drawable.ic_email,
            accentColor = accentColor
        )

        AuthInputField(
            value = password,
            onValueChange = {
                password = it
                onClearError()
                passwordError = null
            },
            placeholder = "Password (min 8 chars)",
            iconRes = R.drawable.ic_lock,
            accentColor = accentColor,
            visualTransformation = PasswordVisualTransformation()
        )

        AuthInputField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                passwordError = null
            },
            placeholder = "Confirm password",
            iconRes = R.drawable.ic_check,
            accentColor = accentColor,
            visualTransformation = PasswordVisualTransformation()
        )

        passwordError?.let { AuthInlineError(message = it) }
        error?.let { AuthInlineError(message = it) }

        LiquidButton(
            onClick = {
                when {
                    name.isBlank() -> passwordError = "Name is required"
                    username.isBlank() -> passwordError = "Username is required"
                    username.length < 3 -> passwordError = "Username must be at least 3 characters"
                    email.isBlank() -> passwordError = "Email is required"
                    password.length < 8 -> passwordError = "Password must be at least 8 characters"
                    password != confirmPassword -> passwordError = "Passwords do not match"
                    !isLoading -> onSignUp(email, password, name, username)
                }
            },
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            tint = accentColor,
            isInteractive = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                BasicText(
                    "Create Account",
                    style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
                )
            }
        }

        AuthDivider(contentColor = contentColor)

        AuthGoogleButton(
            backdrop = backdrop,
            contentColor = contentColor,
            isLoading = isGoogleLoading,
            isEnabled = !isGoogleLoading && !isLoading,
            onClick = onGoogleSignIn
        )

        AuthFooterRow(
            prompt = "Already have an account?",
            action = "Sign in",
            accentColor = accentColor,
            contentColor = contentColor,
            onClick = onLoginClick
        )
    }
}

@Composable
private fun LiquidGlassAuthFrame(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    title: String,
    subtitle: String,
    modeLabel: String,
    scrollable: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val sensor = rememberUISensor()
    val logoDrop = remember { Animatable(-220f) }
    val cardDrop = remember { Animatable(120f) }
    val fade = remember { Animatable(0f) }
    val scale = remember { Animatable(0.92f) }
    val scrollState = rememberScrollState()
    val sensorX by animateFloatAsState(
        targetValue = sensor.gravity.x,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 150f),
        label = "authSensorX"
    )
    val sensorY by animateFloatAsState(
        targetValue = sensor.gravity.y,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 145f),
        label = "authSensorY"
    )

    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, tween(durationMillis = 520)) }
        launch { scale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 360f)) }
        launch { logoDrop.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 260f)) }
        delay(90)
        cardDrop.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 300f))
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
            .imePadding()
    ) {
        val isCompactHeight = maxHeight < 760.dp
        val cardPadding = if (isCompactHeight) 20.dp else 24.dp
        val headerSpacing = if (isCompactHeight) 16.dp else 20.dp
        val verticalPadding = if (scrollable || isCompactHeight) 20.dp else 28.dp

        AuthAmbientOrbs(
            accentColor = accentColor,
            sensorX = sensorX,
            sensorY = sensorY
        )

        val columnModifier = Modifier
            .fillMaxSize()
            .then(
                if (scrollable || isCompactHeight) {
                    Modifier.verticalScroll(scrollState)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 22.dp, vertical = verticalPadding)

        Column(
            modifier = columnModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (scrollable || isCompactHeight) Arrangement.spacedBy(headerSpacing) else Arrangement.Center
        ) {
            if (!scrollable && !isCompactHeight) {
                Spacer(Modifier.height(8.dp))
            }

            AuthHero(
                accentColor = accentColor,
                contentColor = contentColor,
                title = title,
                subtitle = subtitle,
                modeLabel = modeLabel,
                logoDrop = logoDrop.value,
                alpha = fade.value,
                scale = scale.value,
                sensorX = sensorX,
                sensorY = sensorY
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = fade.value
                        translationX = sensorX * 20f
                        translationY = cardDrop.value - sensorY * 18f
                        rotationX = sensorY * 3.1f
                        rotationY = -sensorX * 3.6f
                        cameraDistance = 28f * density
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(34f.dp) },
                        effects = {
                            vibrancy()
                            backdropBlur(22f.dp.toPx())
                            lens(10f.dp.toPx(), 18f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.16f))
                        }
                    )
                    .padding(cardPadding)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun AuthHero(
    accentColor: Color,
    contentColor: Color,
    title: String,
    subtitle: String,
    modeLabel: String,
    logoDrop: Float,
    alpha: Float,
    scale: Float,
    sensorX: Float,
    sensorY: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(168.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(6.dp)
                    .height(92.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .blur(1.dp)
                    .graphicsLayer { this.alpha = alpha * 0.85f }
            )

            Image(
                painter = painterResource(R.drawable.vormex_logo),
                contentDescription = "Vormex logo",
                modifier = Modifier
                    .padding(top = 28.dp)
                    .size(136.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                        this.scaleX = scale
                        this.scaleY = scale
                        translationX = -sensorX * 38f
                        translationY = logoDrop + sensorY * 28f
                        rotationZ = -sensorX * 6.5f
                        rotationX = sensorY * 2.2f
                    },
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.26f),
                            Color.White.copy(alpha = 0.14f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .graphicsLayer { this.alpha = alpha }
        ) {
            BasicText(
                modeLabel,
                style = TextStyle(
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp
                )
            )
        }

        Spacer(Modifier.height(16.dp))

        BasicText(
            title,
            modifier = Modifier.graphicsLayer { this.alpha = alpha },
            style = TextStyle(
                color = accentColor,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        Spacer(Modifier.height(8.dp))

        BasicText(
            subtitle,
            modifier = Modifier.graphicsLayer { this.alpha = alpha },
            style = TextStyle(
                color = contentColor.copy(alpha = 0.76f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        )
    }
}

@Composable
private fun AuthAmbientOrbs(
    accentColor: Color,
    sensorX: Float,
    sensorY: Float
) {
    val skyColor = Color(0xFF9FD4FF)
    val iceColor = Color(0xFFEAF6FF)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Orb(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-42).dp, y = 34.dp),
            size = 220.dp,
            colors = listOf(
                accentColor.copy(alpha = 0.24f),
                skyColor.copy(alpha = 0.17f),
                Color.Transparent
            ),
            translationX = -sensorX * 28f,
            translationY = sensorY * 22f
        )

        Orb(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 36.dp, y = (-26).dp),
            size = 260.dp,
            colors = listOf(
                iceColor.copy(alpha = 0.42f),
                accentColor.copy(alpha = 0.18f),
                Color.Transparent
            ),
            translationX = sensorX * 34f,
            translationY = -sensorY * 18f
        )

        Orb(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 18.dp, y = 40.dp),
            size = 180.dp,
            colors = listOf(
                accentColor.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.12f),
                Color.Transparent
            ),
            translationX = sensorX * 18f,
            translationY = sensorY * 26f
        )
    }
}

@Composable
private fun Orb(
    modifier: Modifier,
    size: androidx.compose.ui.unit.Dp,
    colors: List<Color>,
    translationX: Float,
    translationY: Float
) {
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                this.translationX = translationX
                this.translationY = translationY
            }
            .blur(78.dp)
            .background(
                brush = Brush.radialGradient(colors),
                shape = CircleShape
            )
    )
}

@Composable
private fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    iconRes: Int,
    accentColor: Color,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    val shape = RoundedCornerShape(22.dp)
    val borderColor = Color.White.copy(alpha = 0.72f)
    val baseColor = Color(0xFFF9FCFF)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TextStyle(
            color = Color(0xFF102033),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(accentColor),
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                baseColor.copy(alpha = 0.92f),
                                baseColor.copy(alpha = 0.76f)
                            )
                        )
                    )
                    .border(1.dp, borderColor, shape)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            BasicText(
                                placeholder,
                                style = TextStyle(
                                    color = Color(0xFF6B7280),
                                    fontSize = 15.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            }
        }
    )
}

@Composable
private fun AuthDivider(contentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(contentColor.copy(alpha = 0.18f))
        )
        BasicText(
            "OR",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.52f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(contentColor.copy(alpha = 0.18f))
        )
    }
}

@Composable
private fun AuthGoogleButton(
    backdrop: LayerBackdrop,
    contentColor: Color,
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    LiquidButton(
        onClick = {
            if (isEnabled) {
                onClick()
            }
        },
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        surfaceColor = Color.White.copy(alpha = 0.2f),
        isInteractive = isEnabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp)
                )
                BasicText(
                    "Continue with Google",
                    style = TextStyle(contentColor, 15.sp, FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun AuthFooterRow(
    prompt: String,
    action: String,
    accentColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            prompt,
            style = TextStyle(contentColor.copy(alpha = 0.72f), 14.sp)
        )
        Spacer(Modifier.width(6.dp))
        BasicText(
            action,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            style = TextStyle(accentColor, 14.sp, FontWeight.SemiBold)
        )
    }
}

@Composable
private fun AuthInlineError(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFF1F2).copy(alpha = 0.92f))
            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicText(
            message,
            style = TextStyle(
                color = Color(0xFFB91C1C),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        )
    }
}
