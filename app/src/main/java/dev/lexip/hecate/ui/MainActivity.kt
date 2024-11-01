package dev.lexip.hecate.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.lexip.hecate.R
import dev.lexip.hecate.ui.components.SwitchPreferenceCard
import dev.lexip.hecate.ui.theme.HecateTheme
import dev.lexip.hecate.ui.theme.hecateTopAppBarColors


class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		setContent {
			AdaptiveThemeScreen()
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveThemeScreen(
	viewModel: MainActivityViewModel = viewModel()
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	HecateTheme {
		val scrollBehavior =
			TopAppBarDefaults.enterAlwaysScrollBehavior()
		Scaffold(
			modifier = Modifier
				.nestedScroll(scrollBehavior.nestedScrollConnection),
			containerColor = MaterialTheme.colorScheme.surfaceContainer,
			topBar = {
				LargeTopAppBar(
					colors = hecateTopAppBarColors(),
					title = {
						Text(
							text = stringResource(id = R.string.app_name),
						)
					},
					scrollBehavior = scrollBehavior
				)
			}
		) { innerPadding ->
			Column(
				modifier = Modifier
					.windowInsetsPadding(WindowInsets.systemGestures.only(WindowInsetsSides.Horizontal))
					.fillMaxSize()
					.padding(innerPadding)
					.verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(18.dp)

			) {
				Card(
					modifier = Modifier
						.fillMaxWidth()
						.height(300.dp),
					colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
					shape = MaterialTheme.shapes.extraLarge
				) {
					// TODO #15: Add infographic / demo animation to the main activity
				}
				SwitchPreferenceCard(
					text = stringResource(
						id = R.string.action_use_adaptive_theme
					),
					isChecked = uiState.isAdaptiveThemeEnabled,
					onCheckedChange = { _ -> viewModel.toggleAdaptiveTheme() }
				)
			}
		}
	}
}