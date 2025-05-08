package es.uc3m.android.mobile_app.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
// Import CircularProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
// Import collectAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import es.uc3m.android.mobile_app.NavGraph
import es.uc3m.android.mobile_app.R
import es.uc3m.android.mobile_app.viewmodel.AuthResult // Import AuthResult
import es.uc3m.android.mobile_app.viewmodel.MyViewModel

@Composable
fun SignUpScreen(
    viewModel: MyViewModel,
    navController: NavHostController
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPasswd by rememberSaveable { mutableStateOf("") }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    // Observe the auth sta/te from the ViewModel
    val authState by viewModel.authResult.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        Spacer(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
        )
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (authState == AuthResult.Loading) {
                CircularProgressIndicator()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.fillMaxHeight(0.2f))
                    Text(
                        text = stringResource(R.string.create_account),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.sign_in_to_continue),
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextField(
                        value = login,
                        onValueChange = { login = it },
                        placeholder = {
                            Text(stringResource(R.string.email_edit_text))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = authState !is AuthResult.Loading
                    )
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = {
                            Text(stringResource(R.string.password_edit_text))
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = authState !is AuthResult.Loading
                    )
                    TextField(
                        value = confirmPasswd,
                        onValueChange = { confirmPasswd = it },
                        placeholder = {
                            Text(stringResource(R.string.confirm_edit_text))
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        // Disable fields when loading
                        enabled = authState !is AuthResult.Loading
                    )

                    Button(
                        onClick = {
                            if (password != confirmPasswd) {
                                showDialog = true // Show password mismatch dialog
                            } else {
                                viewModel.signUp(login, password)
                            }
                        },
                        modifier = Modifier
                            .align(alignment = Alignment.End)
                            .padding(top = 16.dp),
                        enabled = authState !is AuthResult.Loading
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.signup_label))
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_arrow_forward_24),
                                contentDescription = stringResource(R.string.signup_label),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                Column(
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    TextButton(
                        onClick = { navController.navigate(NavGraph.Login.route) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = authState !is AuthResult.Loading
                    ) {
                        Text(stringResource(R.string.already_account))
                    }
                }
            }
        }
        Spacer(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
        )

        if (showDialog) {
            DisplayDialog(
                title = stringResource(R.string.error),
                errorMessage = stringResource(R.string.password_does_not_match),
                onDismiss = { showDialog = false }
            )
        }
    }
}

@Composable
fun DisplayDialog(
    title: String,
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = errorMessage) },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}