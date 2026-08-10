package com.routeguard.android.report

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.routeguard.android.R
import com.routeguard.android.permissions.PermissionsUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class ReportBottomSheet : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ReportScreenContent()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ReportScreenContent(
        viewModel: ReportViewModel = hiltViewModel()
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
        val mediaUri by viewModel.mediaUri.collectAsStateWithLifecycle()
        val description by viewModel.description.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            val lat = intent.getDoubleExtra("latitude", Double.NaN)
            val lng = intent.getDoubleExtra("longitude", Double.NaN)
            if (!lat.isNaN() && !lng.isNaN()) {
                viewModel.setCustomLocation(lat, lng)
            }
        }

        val hazardTags = listOf(
            "flooded",
            "fully_blocked",
            "debris",
            "accident",
            "partially_passable"
        )

        var pendingDescription by remember { mutableStateOf<String?>(null) }
        var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            viewModel.handlePermissionResult(isGranted, pendingDescription, pendingMediaUri)
            pendingDescription = null
            pendingMediaUri = null
        }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            viewModel.selectMedia(uri)
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.report_obstacle)) },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (uiState) {
                    is ReportViewModel.ReportUiState.Ready -> {
                        ReportForm(
                            hazardTags = hazardTags,
                            selectedTag = selectedTag,
                            mediaUri = mediaUri,
                            description = description,
                            onDescriptionChange = { viewModel.updateDescription(it) },
                            onTagSelected = { viewModel.selectTag(it) },
                            onImagePick = { imagePickerLauncher.launch("image/*") },
                            onSubmit = { desc ->
                                if (selectedTag.isNullOrEmpty()) {
                                    viewModel.setError("Please select a hazard type")
                                } else if (mediaUri == null) {
                                    viewModel.setError("Please attach a photo as evidence")
                                } else {
                                    pendingDescription = desc
                                    pendingMediaUri = mediaUri
                                    if (PermissionsUtils.isLocationPermissionGranted(context)) {
                                        viewModel.handlePermissionResult(true, pendingDescription, pendingMediaUri)
                                        pendingDescription = null
                                        pendingMediaUri = null
                                    } else {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                }
                            },
                            onCancel = { (context as? Activity)?.finish() }
                        )
                    }
                    is ReportViewModel.ReportUiState.RequiresLogin -> {
                        LoginRequiredView(onLoginClicked = { /* Navigate */ })
                    }
                    is ReportViewModel.ReportUiState.Submitting -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is ReportViewModel.ReportUiState.PermissionRequired -> {
                        PermissionRequiredView(
                            onPermissionGranted = {
                                viewModel.handlePermissionResult(true, pendingDescription, pendingMediaUri)
                                pendingDescription = null
                                pendingMediaUri = null
                            },
                            onPermissionDenied = {
                                viewModel.handlePermissionResult(false, pendingDescription, pendingMediaUri)
                                pendingDescription = null
                                pendingMediaUri = null
                            },
                            onCancel = { (context as? Activity)?.finish() }
                        )
                    }
                    is ReportViewModel.ReportUiState.Error -> {
                        val error = uiState as ReportViewModel.ReportUiState.Error
                        val isValidationError = error.message.contains("photo", ignoreCase = true) || 
                                               error.message.contains("type", ignoreCase = true)
                        ErrorState(
                            message = error.message,
                            onRetry = { 
                                if (isValidationError) {
                                    viewModel.resetState()
                                } else {
                                    viewModel.submitReport(pendingDescription)
                                }
                            },
                            retryLabel = if (isValidationError) "Go Back" else stringResource(R.string.retry),
                            onCancel = { (context as? Activity)?.finish() }
                        )
                    }
                    is ReportViewModel.ReportUiState.Success -> {
                        SuccessState(onCloseClicked = { (context as? Activity)?.finish() })
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun ReportForm(
        hazardTags: List<String>,
        selectedTag: String?,
        mediaUri: Uri?,
        description: String,
        onDescriptionChange: (String) -> Unit,
        onTagSelected: (String) -> Unit,
        onImagePick: () -> Unit,
        onSubmit: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = stringResource(R.string.select_hazard_type),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hazardTags.forEach { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { onTagSelected(tag) },
                        label = { 
                            Text(tag.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) 
                        },
                        leadingIcon = if (selectedTag == tag) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.add_optional_details),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = { Text(stringResource(R.string.describe_hazard_if_needed)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Attach Proof (Required)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (mediaUri != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = mediaUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                }
            }

            OutlinedButton(
                onClick = onImagePick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.attach_image))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onSubmit(description) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.report))
            }

            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }

    @Composable
    private fun LoginRequiredView(onLoginClicked: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.please_login_to_report), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onLoginClicked, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.login))
            }
        }
    }

    @Composable
    private fun SuccessState(onCloseClicked: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.report_submitted_successfully), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCloseClicked, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.close))
            }
        }
    }

    @Composable
    private fun ErrorState(
        message: String, 
        onRetry: () -> Unit, 
        onCancel: () -> Unit,
        retryLabel: String = stringResource(R.string.retry)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(retryLabel)
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
    }

    @Composable
    private fun PermissionRequiredView(onPermissionGranted: () -> Unit, onPermissionDenied: () -> Unit, onCancel: () -> Unit) {
        val context = LocalContext.current
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.location_permission_required), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { PermissionsUtils.requestLocationPermission(context as Activity) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.grant_permission))
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}
