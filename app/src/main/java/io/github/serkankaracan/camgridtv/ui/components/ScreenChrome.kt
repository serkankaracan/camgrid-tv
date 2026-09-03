package io.github.serkankaracan.camgridtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.ui.theme.CamGridDimens
import io.github.serkankaracan.camgridtv.ui.theme.CamGridPalette

@Composable
fun CamGridBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier.background(
                Brush.verticalGradient(
                    colors = listOf(CamGridPalette.BackgroundTop, CamGridPalette.BackgroundBottom)
                )
            ),
        content = content,
    )
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    eyebrow: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow,
                color = CamGridPalette.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = title,
                modifier = Modifier.padding(top = 3.dp),
                color = CamGridPalette.TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 3.dp),
                color = CamGridPalette.TextMuted,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Composable
fun ControlPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .background(
                    color = CamGridPalette.Surface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(CamGridDimens.PanelRadius),
                )
                .border(
                    width = 1.dp,
                    color = CamGridPalette.Outline.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(CamGridDimens.PanelRadius),
                )
                .padding(18.dp),
        content = content,
    )
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
                .border(1.dp, color.copy(alpha = 0.58f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.padding(end = 8.dp).background(color, RoundedCornerShape(50)).padding(4.dp)
        )
        Text(text = text, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
