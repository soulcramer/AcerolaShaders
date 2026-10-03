package app.soulcramer.shaders.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ShaderParamLabel(
    paramName: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = paramName,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    )
}
