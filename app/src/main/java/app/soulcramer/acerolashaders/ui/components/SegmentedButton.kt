package app.soulcramer.acerolashaders.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.soulcramer.acerolashaders.ui.theme.AcerolaShadersTheme

@Composable
internal fun SegmentedButton(
    options: List<String>,
    selectedOption: String,
    onOptionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedcolor: Color = LocalContentColor.current,
//    state: MultiSelectorState = rememberMultiSelectorState(
//        options = options,
//        selectedOption = selectedOption,
//    ),
) {
    require(options.size >= 2) { "This composable requires at least 2 options" }
    require(options.size <= 5) { "This composable requires at most 5 options" }
    require(options.contains(selectedOption)) { "Invalid selected option [$selectedOption]" }

    val selectedIndex by remember(options, selectedOption) {
        mutableStateOf(
            options.indexOf(
                selectedOption
            )
        )
    }

    val backgroundProgress by animateFloatAsState(
        label = "Background Position Progress",
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
        ),
        targetValue = selectedIndex.toFloat(),
    )
    Layout(
        modifier = modifier
            .clip(
                shape = RoundedCornerShape(100f),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(100f),
            ),
        content = {
            options.forEachIndexed { index, option ->
                val textColor by animateColorAsState(
                    label = "Button text color",
                    targetValue = if (index == selectedIndex) selectedColor else unselectedcolor,
                )

                Box(
                    modifier = Modifier
                        .layoutId(MultiSelectorOption.Option)
                        .clickable { onOptionSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        color = textColor,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            val startCornerShape by animateIntAsState(
                label = "Selector start shape",
                targetValue = if (options.first() == selectedOption) 50 else 15,
            )
            val endCornerShape by animateIntAsState(
                label = "Selector end shape",
                targetValue = if (options.last() == selectedOption) 50 else 15,
            )
            Box(
                modifier = Modifier
                    .layoutId(MultiSelectorOption.Background)
                    .clip(
                        shape = RoundedCornerShape(
                            topStartPercent = startCornerShape,
                            bottomStartPercent = startCornerShape,
                            topEndPercent = endCornerShape,
                            bottomEndPercent = endCornerShape,
                        ),
                    )
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            )
        },
    ) { measurables, constraints ->
        val optionWidth = constraints.maxWidth / options.size
        val optionConstraints = Constraints.fixed(
            width = optionWidth,
            height = constraints.maxHeight,
        )
        val optionPlaceables = measurables
            .filter { measurable -> measurable.layoutId == MultiSelectorOption.Option }
            .map { measurable -> measurable.measure(optionConstraints) }
        val backgroundPlaceable = measurables
            .first { measurable -> measurable.layoutId == MultiSelectorOption.Background }
            .measure(optionConstraints)
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
        ) {
            backgroundPlaceable.placeRelative(
                x = (backgroundProgress * optionWidth).toInt(),
                y = 0,
            )
            optionPlaceables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = optionWidth * index,
                    y = 0,
                )
            }
        }
    }
}

private val animationSpec = spring<Int>()

@Stable
internal interface MultiSelectorState {
    val selectedIndex: Int
    fun selectOption(index: Int)
}

@Stable
internal class MultiSelectorStateImpl(
    options: List<String>,
    selectedOption: String,
) : MultiSelectorState {

    override val selectedIndex: Int
        get() = _selectedIndex

    private var _selectedIndex = options.indexOf(selectedOption)

    override fun selectOption(index: Int) {
        _selectedIndex = index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultiSelectorStateImpl

        return _selectedIndex == other._selectedIndex
    }

    override fun hashCode(): Int {
        return _selectedIndex.hashCode()
    }
}

@Composable
internal fun rememberMultiSelectorState(
    options: List<String>,
    selectedOption: String,
) = remember {
    MultiSelectorStateImpl(
        options,
        selectedOption,
    )
}

internal enum class MultiSelectorOption {
    Option,
    Background,
}

@Preview(widthDp = 420)
@Composable
private fun PreviewMultiSelector() {
    AcerolaShadersTheme {
        Surface {

            val options1 = listOf("Day", "Week", "Month")
            var selectedOption1 by remember {
                mutableStateOf(options1.first())
            }
            val options2 = listOf("Sit", "Amet", "Consectetur", "Elit", "Quis")
            var selectedOption2 by remember {
                mutableStateOf(options2.first())
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SegmentedButton(
                    options = options1,
                    selectedOption = selectedOption1,
                    onOptionSelect = { option ->
                        selectedOption1 = option
                    },
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                )

                SegmentedButton(
                    options = options2,
                    selectedOption = selectedOption2,
                    onOptionSelect = { option ->
                        selectedOption2 = option
                    },
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .height(48.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}