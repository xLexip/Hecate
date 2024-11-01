package dev.lexip.hecate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SwitchPreferenceCard(text: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
	val mainSwitchTextSize = (MaterialTheme.typography.titleLarge.fontSize.value - 1).sp

	Card(
		modifier = Modifier
			.fillMaxWidth(),
		shape = MaterialTheme.shapes.extraLarge,
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
		onClick = { onCheckedChange(isChecked) }
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(PaddingValues(16.dp)),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Text(
				style = MaterialTheme.typography.titleLarge,
				fontSize = mainSwitchTextSize,
				color = MaterialTheme.colorScheme.onPrimaryContainer,
				text = text
			)
			Switch(checked = isChecked, onCheckedChange = onCheckedChange)
		}
	}
}