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
import es.uc3m.android.mobile_app.viewmodel.AuthResult
import es.uc3m.android.mobile_app.viewmodel.MyViewModel

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: MyViewModel
) {

    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

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

                    Button(
                        onClick = {
                            viewModel.login(login, password)
                        },
                        modifier = Modifier
                            .align(alignment = Alignment.End)
                            .padding(top = 16.dp),
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
                    Spacer(modifier = Modifier.weight(1f))
                }

                Column(
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    TextButton(
                        onClick = { navController.navigate(NavGraph.SignUp.route) },
                        modifier = Modifier.fillMaxWidth(),
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