package es.uc3m.android.mobile_app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import es.uc3m.android.mobile_app.viewmodel.MyViewModel
import es.uc3m.android.mobile_app.viewmodel.Review
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishReviewScreen(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel(),
    restaurantId: String = "",
    restaurantName: String,
    dishName: String
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val userEmail = user?.email ?: "Unknown"

    var title by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var submitSuccess by remember { mutableStateOf(false) }

    // Image handling states
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageOptions by remember { mutableStateOf(false) }

    // Permission dialog states
    var showCameraPermissionRationale by remember { mutableStateOf(false) }
    var showStoragePermissionRationale by remember { mutableStateOf(false) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var permissionDeniedMessage by remember { mutableStateOf("") }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    val photoFile = remember {
        File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            context.cacheDir
        )
    }

    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    // Multiple permission launcher
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.entries.all { it.value }

        if (allPermissionsGranted) {
            showImageOptions = true
        } else {
            if (permissions[Manifest.permission.CAMERA] == false) {
                showCameraPermissionRationale = true
            }

            val storagePermission = PermissionHandler.getStoragePermission()
            if (permissions[storagePermission] == false) {
                showStoragePermissionRationale = true
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val tempFile = File.createTempFile(
                    "JPEG_${timeStamp}_",
                    ".jpg",
                    context.cacheDir
                )
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                imageUri = Uri.fromFile(tempFile)
            } catch (e: Exception) {
                submitError = "Error processing gallery image: ${e.message}"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                // Ensure the photo file exists and has content
                if (photoFile.exists() && photoFile.length() > 0) {
                    imageUri = photoUri
                } else {
                    submitError = "Camera returned success but photo file is empty or missing"
                }
            } catch (e: Exception) {
                submitError = "Error processing camera image: ${e.message}"
            }
        } else {
            submitError = "Failed to take photo"
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(photoUri)
        } else {
            showCameraPermissionRationale = true
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            photoPickerLauncher.launch("image/*")
        } else {
            showStoragePermissionRationale = true
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState), // Add scrolling capability
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Review for $dishName",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "at $restaurantName",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Review Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Text(
                text = "Rating",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                repeat(5) { index ->
                    IconButton(onClick = { rating = index + 1 }) {
                        Icon(
                            imageVector = if (index < rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Star ${index + 1}",
                            tint = if (index < rating) Color(0xFFFFC107) else Color.Gray
                        )
                    }
                }
            }

            Text(
                text = "Add Photo (Optional)",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            // Image preview or placeholder
            if (imageUri != null) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Photo",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .clickable {
                            val requiredPermissions = PermissionHandler.getRequiredImagePermissions()
                            multiplePermissionsLauncher.launch(requiredPermissions.toTypedArray())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Photo",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = "Add Photo",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comment input
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Your Review") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(bottom = 16.dp),
                maxLines = 5
            )

            // Submit button
            Button(
                onClick = {
                    if (rating == 0) {
                        submitError = "Please select a rating"
                        return@Button
                    }

                    if (title.isBlank()) {
                        submitError = "Please enter a title for your review"
                        return@Button
                    }

                    isSubmitting = true
                    submitError = null

                    val review = Review(
                        user = userEmail,
                        restaurant = restaurantName,
                        dish = dishName,
                        rating = rating,
                        comment = comment,
                        title = title,
                        photoUrl = "",
                        timestamp = System.currentTimeMillis()
                    )

                    viewModel.addDishReview(
                        review = review,
                        imageUri = imageUri,
                        onSuccess = {
                            isSubmitting = false
                            submitSuccess = true
                            navController.popBackStack()
                        },
                        onError = { error ->
                            isSubmitting = false
                            submitError = "Error: $error. Please try again."
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submitting...")
                } else {
                    Text("Submit Review")
                }
            }

            // Error message
            submitError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Cancel button
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            title = { Text("Add Photo") },
            text = { Text("Choose a photo source") },
            confirmButton = {
                Button(
                    onClick = {
                        showImageOptions = false
                        val storagePermission = PermissionHandler.getStoragePermission()
                        if (PermissionHandler.isStoragePermissionGranted(context)) {
                            photoPickerLauncher.launch("image/*")
                        } else {
                            storagePermissionLauncher.launch(storagePermission)
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Gallery",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Gallery")
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showImageOptions = false
                        // Check camera permission
                        if (PermissionHandler.isCameraPermissionGranted(context)) {
                            try {
                                if (!photoFile.exists()) {
                                    photoFile.createNewFile()
                                } else if (!photoFile.canWrite()) {
                                    photoFile.delete()
                                    photoFile.createNewFile()
                                }
                                cameraLauncher.launch(photoUri)
                            } catch (e: Exception) {
                                submitError = "Error preparing camera: ${e.message}"
                            }
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Camera")
                    }
                }
            }
        )
    }

    // Camera permission rationale dialog
    if (showCameraPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionRationale = false },
            title = { Text("Camera Permission Required") },
            text = {
                Text("Camera permission is needed to take photos for your review. Would you like to grant this permission?")
            },
            confirmButton = {
                Button(onClick = {
                    showCameraPermissionRationale = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showCameraPermissionRationale = false
                    permissionDeniedMessage = "You won't be able to take photos without camera permission"
                    showPermissionSettingsDialog = true
                }) {
                    Text("Not Now")
                }
            }
        )
    }

    if (showStoragePermissionRationale) {
        AlertDialog(
            onDismissRequest = { showStoragePermissionRationale = false },
            title = { Text("Storage Permission Required") },
            text = {
                Text("Storage permission is needed to select photos from your gallery. Would you like to grant this permission?")
            },
            confirmButton = {
                Button(onClick = {
                    showStoragePermissionRationale = false
                    val storagePermission = PermissionHandler.getStoragePermission()
                    storagePermissionLauncher.launch(storagePermission)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showStoragePermissionRationale = false
                    permissionDeniedMessage = "You won't be able to select photos without storage permission"
                    showPermissionSettingsDialog = true
                }) {
                    Text("Not Now")
                }
            }
        )
    }

    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            title = { Text("Permission Denied") },
            text = {
                Text("$permissionDeniedMessage. You can enable it in app settings.")
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                Button(onClick = { showPermissionSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}