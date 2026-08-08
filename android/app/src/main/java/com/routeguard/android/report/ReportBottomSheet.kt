package com.routeguard.android.report

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.image.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asColorFilter
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.routeguard.android.R
import com.routeguard.android.data.AuthRepository
import com.routeguard.android.map.HazardMapper
import com.routeguard.android.permissions.PermissionsUtils
import com.routeguard.android.report.HazardTagChip
import com.routeguard.android.report.ReportViewModel
import com.routeguard.android.ui.auth.AuthUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectAsLifecycleWithLifecycle
import kotlinx.coroutines.launch

/**
 * Bottom sheet for tag-based obstacle reporting
 * Implements FR-4, FR-5: Tag-based reporting interface (<10s)
 * Allows users to submit reports using preset tags in under 10 seconds
 */
@AndroidEntryPoint
class ReportBottomSheet : androidx.compose.material3.BottomSheetScaffoldScreen() {

    private val viewModel: ReportViewModel = hiltViewModel()
    private val authRepository: AuthRepository = hiltViewModel()

    private val hazardTags = listOf(
        "flooded",
        "fully_blocked",
        "debris",
        "accident",
        "partially_passable"
    )

    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReportBottomSheetContent(viewModel)
        }
    }

    @Composable
    private fun ReportBottomSheetContent(viewModel: ReportViewModel) {
        val uiState by viewModel.uiState.collectAsLifecycleWithLifecycle()
        val selectedTag by viewModel.selectedTag.collectAsLifecycleWithLifecycle()
        val reportResult by viewModel.reportResult.collectAsLifecycleWithLifecycle()
        val context = LocalContext.current
        val activity = context as Activity

        // Pending description to hold the description while waiting for permission
        var pendingDescription by remember { mutableStateOf<String?>(null) }
        // Pending media URI to hold the selected image while waiting for permission
        var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }

        // Permission launcher for location
        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, submit the report with the pending description and media
                viewModel.handlePermissionResult(true, pendingDescription, pendingMediaUri)
                // Clear the pending values
                pendingDescription = null
                pendingMediaUri = null
            } else {
                // Permission denied, show error via ViewModel
                viewModel.handlePermissionResult(false, pendingDescription, pendingMediaUri)
                // Clear the pending values
                pendingDescription = null
                pendingMediaUri = null
            }
        }

        // Launcher for picking an image from gallery
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            // Update the ViewModel with the selected image URI
            viewModel.selectMedia(uri)
        }

        BottomSheetScaffold(
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.report_obstacle),
                        style = MaterialTypography.titleMedium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 12.dp)
                    )

                    // Hazard tags selection
                    Text(
                        text = stringResource(R.string.select_hazard_type),
                        style = MaterialTypography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        items(hazardTags) { tag ->
                            HazardTagChip(
                                tag = tag,
                                selected = selectedTag == tag,
                                onClick = { viewModel.selectTag(tag) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }

                    // Optional description field
                    var description by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.add_optional_details)) },
                        placeholder = { Text(stringResource(R.string.describe_ hazard_if_needed)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        maxLines = 3
                    )

                    // Optional image attachment
                    var imageUri by remember { viewModel.mediaUri.value }
                    LaunchedEffect(imageUri) {
                        // Update pendingMediaUri when ViewModel's mediaUri changes
                        pendingMediaUri = imageUri
                    }
                    if (imageUri != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.selected_image),
                                style = MaterialTypography.bodySmall,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(bottom = 4.dp)
                            )
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            enabled = true
                        ) {
                            Text(stringResource(R.string.attach_image))
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { /* Dismiss bottom sheet */ },
                            enabled = true
                        ) {
                            Text(stringResource(R.string.cancel))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val tag = selectedTag
                                if (tag == null || tag.isEmpty()) {
                                    // No tag selected, show error
                                    viewModel.setError("Please select a hazard type")
                                } else {
                                    // We have a tag, save the description and media URI and check permission
                                    pendingDescription = description
                                    // pendingMediaUri is updated via LaunchedEffect from viewModel.mediaUri
                                    if (PermissionsUtils.isLocationPermissionGranted(context)) {
                                        // Permission already granted, proceed to submit
                                        viewModel.handlePermissionResult(true, pendingDescription, pendingMediaUri)
                                        // Clear the pending values
                                        pendingDescription = null
                                        pendingMediaUri = null
                                    } else {
                                        // Permission not granted, show permission rationale
                                        // The UI will show PermissionRequiredState based on ViewModel updates
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                }
                            },
                            enabled = true // Always enabled, we'll handle validation in the click
                        ) {
                            Text(stringResource(R.string.report))
                        }
                    }
                }
            },
            scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = BottomSheetState.Collapsed
            ),
            backgroundColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth()
        ) { padding ->
            when (uiState) {
                is ReportViewModel.ReportUiState.Ready -> {
                    ReportFormContent(
                        viewModel = viewModel,
                        onTagSelected = { viewModel.selectTag(it) },
                        onSubmit = { description ->
                            viewModel.submitReport(description)
                        },
                        onCancel = { /* Dismiss bottom sheet */ }
                    )
                }
                is ReportViewModel.ReportUiState.RequiresLogin -> {
                    LoginRequiredView(
                        onLoginClicked = { /* Navigate to login screen */ }
                    )
                }
                is ReportViewModel.ReportUiState.Submitting -> {
                    CenteredCircularProgressIndicator(modifier = Modifier.align(CenterHorizontally))
                }
                is ReportViewModel.ReportUiState.PermissionRequired -> {
                    PermissionRequiredView(
                        onPermissionGranted = {
                            // Try to submit the report again now that permission is granted
                            viewModel.handlePermissionResult(true, pendingDescription, pendingMediaUri)
                            // Clear the pending values
                            pendingDescription = null
                            pendingMediaUri = null
                        },
                        onPermissionDenied = {
                            // User denied permission, show error
                            viewModel.handlePermissionResult(false, pendingDescription, pendingMediaUri)
                            // Clear the pending values
                            pendingDescription = null
                            pendingMediaUri = null
                        },
                        onCancel = { /* Dismiss bottom sheet */ }
                    )
                }
                is ReportViewModel.ReportUiState.Error -> {
                    ErrorState(
                        message = (uiState as ReportViewModel.ReportUiState.Error).message,
                        onRetry = { /* Retry logic */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(padding)
                    )
                }
                is ReportViewModel.ReportUiState.Success -> {
                    SuccessState(
                        onCloseClicked = { /* Dismiss bottom sheet and reset */ }
                    )
                }
            }
        }
    }

    @Composable
    private fun ReportFormContent(
        viewModel: ReportViewModel,
        onTagSelected: (String) -> Unit,
        onSubmit: (String?) -> Unit,
        onCancel: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.report_obstacle),
                style = MaterialTypography.titleMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 12.dp)
            )

            // Hazard tags selection
            Text(
                text = stringResource(R.string.select_hazard_type),
                style = MaterialTypography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                items(hazardTags) { tag ->
                    HazardTagChip(
                        tag = tag,
                        selected = viewModel.selectedTag.value == tag,
                        onClick = { onTagSelected(tag) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }

            // Optional description field
            var description by remember { mutableStateOf("") }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.add_optional_details)) },
                placeholder = { Text(stringResource(R.string.describe_ hazard_if_needed)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                maxLines = 3
            )

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onCancel() },
                    enabled = true
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        onSubmit(description)
                    },
                    enabled = viewModel.selectedTag.value != null
                ) {
                    Text(stringResource(R.string.report))
                }
            }
        }
    }

    @Composable
    private fun LoginRequiredView(onLoginClicked: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Login,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.please_login_to_report),
                style = MaterialTypography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onLoginClicked() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.login))
            }
        }
    }

    @Composable
    private fun SuccessState(onCloseClicked: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.success,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.report_submitted_successfully),
                style = MaterialTypography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.thank_you_for_helping_keep_roads_safe),
                style = MaterialTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onCloseClicked() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.close))
            }
        }
    }

    @Composable
    private fun ErrorState(
        message: String,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                Modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTypography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onRetry() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.retry))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { /* Dismiss */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }

    @Composable
    private fun PermissionRequiredView(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit,
        onCancel: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.location_permission_required),
                style = MaterialTypography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.location_permission_explanation),
                style = MaterialTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    // Request location permission
                    val activity = context as Activity
                    PermissionsUtils.requestLocationPermission(activity)
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(stringResource(R.string.grant_permission))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onCancel() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }

    @Composable
    private fun CenteredCircularProgressIndicator(modifier: Modifier = Modifier) = Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}