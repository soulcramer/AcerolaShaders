package app.soulcramer.shaders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.soulcramer.shaders.ui.shaders.colorblindness.ColorBlindScreen
import app.soulcramer.shaders.ui.shaders.crt.CrtScreen
import app.soulcramer.shaders.ui.theme.AcerolaShadersTheme

private const val HomeRoute = "home"
private const val ColorBlindRoute = "colorBlindShader"
private const val CrtRoute = "crtShader"

internal class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AcerolaShadersTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController, modifier = Modifier.safeDrawingPadding())
                }
            }
        }
    }
}

// Composable that show a list of card that redirect to the detail screen on click
@Composable
internal fun Home(
    modifier: Modifier = Modifier,
    onShaderClick: (destination: String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = spacedBy(16.dp),
    ) {
        item {
            ShaderItem(
                shaderName = "Color Blindness",
                onItemClick = { onShaderClick(ColorBlindRoute) },
            )
        }
        item {
            ShaderItem(
                shaderName = "CRT",
                onItemClick = { onShaderClick(CrtRoute) },
            )
        }
    }
}

@Composable
internal fun ShaderItem(
    shaderName: String,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onItemClick, modifier = modifier.fillMaxWidth()) {
        Text(
            text = shaderName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
    ) {
        composable(HomeRoute) {
            Home(
                modifier = modifier,
                onShaderClick = { navController.navigate(it) },
            )
        }
        composable(ColorBlindRoute) { ColorBlindScreen(modifier = modifier) }
        composable(CrtRoute) { CrtScreen(modifier = modifier) }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    AcerolaShadersTheme {
        Home(
            modifier = Modifier.fillMaxSize(),
            onShaderClick = {},
        )
    }
}

@Preview
@Composable
private fun ShaderItemPreview() {
    AcerolaShadersTheme {
        ShaderItem(shaderName = "Color Blindness", onItemClick = {})
    }
}
