package com.x0550.rubiccubesolver.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.x0550.rubiccubesolver.R
import com.x0550.rubiccubesolver.ui.theme.CyberCyan
import com.x0550.rubiccubesolver.ui.theme.ElectricLime
import com.x0550.rubiccubesolver.ui.theme.FrostedSlateSurface
import com.x0550.rubiccubesolver.ui.theme.ObsidianBackground
import com.x0550.rubiccubesolver.ui.theme.TextPrimary
import com.x0550.rubiccubesolver.ui.theme.TextSecondary

@Composable
fun CubicExitConfirmationDialog(
    solved: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val accent = if (solved) ElectricLime else CyberCyan
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(dimensionResource(R.dimen.dimen_20dp)),
            color = FrostedSlateSurface,
            border = BorderStroke(dimensionResource(R.dimen.dimen_1dp), accent)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.dimen_24dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(if (solved) R.string.exit_solved_header else R.string.exit_paused_header),
                    color = accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.dimen_16dp)))
                Text(
                    text = stringResource(if (solved) R.string.exit_solved_title else R.string.exit_paused_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.dimen_12dp)))
                Text(
                    text = stringResource(if (solved) R.string.exit_solved_body else R.string.exit_paused_body),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.dimen_24dp)))
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_12dp))) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().heightIn(min = dimensionResource(R.dimen.dimen_48dp)),
                        border = BorderStroke(dimensionResource(R.dimen.dimen_1dp), accent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                    ) {
                        Text(stringResource(if (solved) R.string.exit_stay_here else R.string.exit_keep_solving))
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth().heightIn(min = dimensionResource(R.dimen.dimen_48dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = ObsidianBackground
                        )
                    ) {
                        Text(stringResource(R.string.solve_close_app))
                    }
                }
            }
        }
    }
}