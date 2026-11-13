package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GlassmorphicBackdrop
import com.example.ui.navigation.AppBottomNavigation
import com.example.ui.navigation.AppNavigationRail
import com.example.ui.navigation.Screen
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.records.RecordsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.transactions.TransactionsScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.MyFinanceTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PrimaryIndigoLight
import com.example.ui.theme.VioletAccent
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyFinanceTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainAppContent()

                    var showSplash by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(1100L)
                        showSplash = false
                    }

                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut(animationSpec = tween(480, easing = FastOutSlowInEasing))
                    ) {
                        BrandLoadingScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun BrandLoadingScreen(modifier: Modifier = Modifier) {
    val logoScale = remember { Animatable(0.84f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ObsidianBlack,
                        Color(0xFF10132E),
                        Color(0xFF1E1B4B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryIndigoLight.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.28f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.25f, size.height * 0.28f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyanAccent.copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.72f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.8f, size.height * 0.72f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                CyanAccent.copy(alpha = 0.40f),
                                VioletAccent.copy(alpha = 0.40f)
                            )
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_brand_logo),
                    contentDescription = "MyFinance Vector Logo",
                    modifier = Modifier.size(84.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(IndigoAccent, CyanAccent, GoldAccent)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Personal Wealth & Offline Ledger",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun MainAppContent(viewModel: FinanceViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = Screen.DASHBOARD
    }

    GlassmorphicBackdrop(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useNavRail) {
                        AppBottomNavigation(
                            currentScreen = currentScreen,
                            onScreenSelected = { currentScreen = it }
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        AppNavigationRail(
                            currentScreen = currentScreen,
                            onScreenSelected = { currentScreen = it },
                            modifier = Modifier.fillMaxHeight()
                        )
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                    slideInVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) { it / 28 })
                                .togetherWith(fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                        },
                        label = "screenTransition",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) { screen ->
                        val screenModifier = Modifier.fillMaxSize()
                        when (screen) {
                            Screen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToTransactions = { currentScreen = Screen.TRANSACTIONS },
                                onNavigateToAccounts = { currentScreen = Screen.ACCOUNTS },
                                onNavigateToRecords = { currentScreen = Screen.RECORDS },
                                modifier = screenModifier
                            )
                            Screen.TRANSACTIONS -> TransactionsScreen(
                                viewModel = viewModel,
                                modifier = screenModifier
                            )
                            Screen.ACCOUNTS -> AccountsScreen(
                                viewModel = viewModel,
                                modifier = screenModifier
                            )
                            Screen.RECORDS -> RecordsScreen(
                                viewModel = viewModel,
                                modifier = screenModifier
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                modifier = screenModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
