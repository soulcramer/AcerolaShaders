package app.soulcramer.acerolashaders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import app.soulcramer.acerolashaders.ui.shaders.colorblindness.ColorBlindScreen
import app.soulcramer.acerolashaders.ui.theme.AcerolaShadersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AcerolaShadersTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}

// Composable that show a list of card that redirect to the detail screen on click
@Composable
fun Home(
    modifier: Modifier = Modifier,
    onShaderClick: (destination: String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            ShaderItem(
                shaderName = "Color Blindness",
                onItemClick = { onShaderClick("colorBlindShader") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShaderItem(
    shaderName: String,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onItemClick, modifier = modifier.fillMaxWidth()) {
        Box() {
            Text(
                text = shaderName,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            Home(
                modifier = modifier,
                onShaderClick = { navController.navigate(it) }
            )
        }
        composable("colorBlindShader") { ColorBlindScreen(modifier = modifier) }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    AcerolaShadersTheme {
        Home(
            modifier = Modifier.fillMaxSize(),
            onShaderClick = {}
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
