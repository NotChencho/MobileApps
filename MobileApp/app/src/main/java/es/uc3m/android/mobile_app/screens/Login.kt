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
import androidx.compose.material3.Button
// Import CircularProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// Import collectAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import es.uc3m.android.mobile_app.NavGraph
import es.uc3m.android.mobile_app.R
import es.uc3m.android.mobile_app.viewmodel.AuthResult // Import AuthResult
import es.uc3m.android.mobile_app.viewmodel.MyViewModel

@Composable
fun LoginScreen(
    navController: NavHostController,
    // Make sure you are potentially passing the same instance if needed,
    // or using a ViewModel provider (like hiltViewModel())
    viewModel: MyViewModel // Consider using hiltViewModel() or similar
) {

    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // Observe the auth state from the ViewModel
    val authState by viewModel.authResult.collectAsState()

    // --- Optional: Add state for showing error messages ---
    // var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    // LaunchedEffect(authState) {
    //     if (authState is AuthResult.Error) {
    //         errorMessage = authState.message
    //         // Optionally reset the state in VM after showing message
    //         // viewModel.resetAuthResult()
    //     } else {
    //         errorMessage = null
    //     }
    // }
    // ---

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
            // Center content within this Box (useful for the loading indicator)
            contentAlignment = Alignment.Center
        ) {
            // Conditionally display Loading or Content
            if (authState == AuthResult.Loading) {
                CircularProgressIndicator()
            } else {
                // --- Main Content Column ---
                Column(modifier = Modifier.fillMaxSize()) { // Fill the box
                    Spacer(modifier = Modifier.fillMaxHeight(0.2f))
                    Text(
                        text = stringResource(R.string.login_label),
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
                        // Disable fields when loading
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
                        // Disable fields when loading
                        enabled = authState !is AuthResult.Loading
                    )

                    // --- Optional: Display Error Message ---
                    // errorMessage?.let {
                    //     Text(
                    //         text = it,
                    //         color = MaterialTheme.colorScheme.error,
                    //         modifier = Modifier.padding(top = 8.dp)
                    //     )
                    // }
                    // ---

                    Button(
                        onClick = {
                            // Reset error message if shown previously
                            // errorMessage = null
                            viewModel.login(login, password)
                        },
                        modifier = Modifier
                            .align(alignment = Alignment.End)
                            .padding(top = 16.dp),
                        // Disable button when loading
                        enabled = authState !is AuthResult.Loading
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.login_label))
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_arrow_forward_24),
                                contentDescription = stringResource(R.string.login_label),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    // Spacer to push the bottom button down
                    Spacer(modifier = Modifier.weight(1f))
                }

                // --- Bottom Navigation --- (Aligned to bottom of the outer Box)
                Column(
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .fillMaxWidth() // Ensure it takes width for alignment
                        .padding(bottom = 32.dp) // Add some padding from the bottom edge
                ) {
                    TextButton(
                        onClick = { navController.navigate(NavGraph.SignUp.route) },
                        modifier = Modifier.fillMaxWidth(),
                        // Disable button when loading
                        enabled = authState !is AuthResult.Loading
                    ) {
                        Text(stringResource(R.string.don_t_have_an_account_sign_up))
                    }
                }
            }
        }
        Spacer(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
        )
    }
}