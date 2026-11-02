package com.x0550.rubiccubesolver.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.x0550.rubiccubesolver.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScanInstructionSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.dimen_24dp))
                .padding(bottom = dimensionResource(R.dimen.dimen_24dp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_12dp))
        ) {
            Text(
                text = stringResource(R.string.scan_instructions_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.scan_instructions_intro),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.scan_instructions_step_one),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.scan_instructions_step_two),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.scan_instructions_step_three),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.dimen_4dp)))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.scan_instructions_start))
            }
        }
    }
}