package com.saikalyan.jobmate.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saikalyan.jobmate.R
import com.saikalyan.jobmate.ui.dashboard.DashboardScreen
import com.saikalyan.jobmate.ui.dashboard.DashboardViewModel
import com.saikalyan.jobmate.ui.contacts.ContactImportRoute
import com.saikalyan.jobmate.ui.contacts.ContactListRoute
import com.saikalyan.jobmate.ui.contacts.ContactMatchRoute
import com.saikalyan.jobmate.ui.resumes.ResumeListRoute
import com.saikalyan.jobmate.ui.templates.TemplateListRoute
import com.saikalyan.jobmate.ui.outreach.OutreachComposerRoute
import com.saikalyan.jobmate.ui.settings.GmailSettingsRoute
import com.saikalyan.jobmate.ui.capture.CaptureRoute
import com.saikalyan.jobmate.ui.followups.FollowUpRoute
import com.saikalyan.jobmate.ui.settings.AppSettingsRoute
import com.saikalyan.jobmate.ui.settings.SheetsSyncRoute
import com.saikalyan.jobmate.ui.settings.AiSettingsRoute
import com.saikalyan.jobmate.ui.jobs.JobDetailEvent
import com.saikalyan.jobmate.ui.jobs.JobDetailScreen
import com.saikalyan.jobmate.ui.jobs.JobDetailViewModel
import com.saikalyan.jobmate.ui.jobs.JobEditorScreen
import com.saikalyan.jobmate.ui.jobs.JobEditorViewModel
import com.saikalyan.jobmate.ui.jobs.JobListScreen
import com.saikalyan.jobmate.ui.jobs.JobListViewModel

private object Routes {
    const val DASHBOARD = "dashboard"
    const val JOBS = "jobs"
    const val JOB_DETAIL = "job/{jobId}?correct={correct}"
    const val NEW_JOB = "job-editor/new"
    const val EDIT_JOB = "job-editor/{jobId}"
    const val CONTACTS = "contacts"
    const val CONTACT_IMPORT = "contacts/import"
    const val CONTACT_MATCH = "contacts/{contactId}/matches"
    const val RESUMES = "resumes"
    const val TEMPLATES = "templates"
    const val OUTREACH = "outreach"
    const val FOLLOW_UPS = "follow-ups"
    const val SETTINGS = "settings"
    const val APP_SETTINGS = "app-settings"
    const val SHEETS_SYNC = "sheets-sync"
    const val AI_SETTINGS = "ai-settings"
    const val CAPTURE_REVIEW = "capture-review"
}

private data class NavItem(val route: String, val title: String, val symbol: String = "")

private val primaryNavItems = listOf(
    NavItem(Routes.DASHBOARD, "Home", "⌂"),
    NavItem(Routes.JOBS, "Jobs", "▤"),
    NavItem(Routes.CAPTURE_REVIEW, "Capture", "+"),
)

private val moreNavItems = listOf(
    NavItem(Routes.CONTACTS, "Contacts"),
    NavItem(Routes.CONTACT_IMPORT, "Import contacts"),
    NavItem(Routes.RESUMES, "Resumes"),
    NavItem(Routes.TEMPLATES, "Templates"),
    NavItem(Routes.OUTREACH, "Outreach"),
    NavItem(Routes.FOLLOW_UPS, "Follow-ups"),
    NavItem(Routes.SETTINGS, "Gmail settings"),
    NavItem(Routes.APP_SETTINGS, "App settings"),
    NavItem(Routes.SHEETS_SYNC, "Sheets sync"),
    NavItem(Routes.AI_SETTINGS, "AI correction settings"),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppNavHost(modifier: Modifier = Modifier, sharedText: String? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val route = currentDestination?.route.orEmpty()
    var moreExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) navController.navigate(Routes.CAPTURE_REVIEW) { launchSingleTop = true }
    }

    val title = when (route) {
        Routes.DASHBOARD -> "JobMate"
        Routes.JOBS -> "Your jobs"
        Routes.CAPTURE_REVIEW -> "Capture a job"
        Routes.CONTACTS -> "Contacts"
        Routes.CONTACT_IMPORT -> "Import contacts"
        Routes.RESUMES -> "Resumes"
        Routes.TEMPLATES -> "Email templates"
        Routes.OUTREACH -> "Outreach"
        Routes.FOLLOW_UPS -> "Follow-ups"
        Routes.SETTINGS -> "Gmail settings"
        Routes.APP_SETTINGS -> "App settings"
        Routes.SHEETS_SYNC -> "Sheets sync"
        Routes.AI_SETTINGS -> "AI correction settings"
        Routes.NEW_JOB -> "Add a job"
        Routes.EDIT_JOB -> "Edit job"
        Routes.JOB_DETAIL -> "Job details"
        Routes.CONTACT_MATCH -> "Company matches"
        else -> "JobMate"
    }
    val isNested = route == Routes.AI_SETTINGS || route.startsWith("job/") || route.startsWith("job-editor/") || route.startsWith("contacts/")

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(
                            painter = painterResource(R.drawable.jobmate_mark),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                        )
                        Text(title, style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    if (isNested) IconButton(onClick = { navController.popBackStack() }) {
                        Text("‹", style = MaterialTheme.typography.headlineMedium)
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                primaryNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = route == item.route || (item.route == Routes.JOBS && (route.startsWith("job/") || route.startsWith("job-editor/"))),
                        onClick = { moreExpanded = false; navController.navigate(item.route) { launchSingleTop = true } },
                        icon = { Text(item.symbol, style = MaterialTheme.typography.titleLarge) },
                        label = { Text(item.title) },
                    )
                }
                NavigationBarItem(
                    selected = moreNavItems.any { route == it.route || route.startsWith("${it.route}/") },
                    onClick = { moreExpanded = true },
                    icon = { Text("•••", style = MaterialTheme.typography.titleMedium) },
                    label = { Text("More") },
                )
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            composable(Routes.DASHBOARD) {
                val viewModel: DashboardViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    state = state,
                    onRetry = viewModel::retry,
                    onAddJob = { navController.navigate(Routes.NEW_JOB) },
                    onViewJobs = { navController.navigate(Routes.JOBS) },
                    onOpenJob = { id -> navController.navigate("job/$id") },
                )
            }
            composable(Routes.JOBS) {
                val viewModel: JobListViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                JobListScreen(
                    state = state,
                    onSearch = viewModel::setSearchQuery,
                    onStatusFilter = viewModel::setStatusFilter,
                    onCaptureFilter = viewModel::setCaptureFilter,
                    onRetry = viewModel::retry,
                    onAddJob = { navController.navigate(Routes.NEW_JOB) },
                    onOpenJob = { id -> navController.navigate("job/$id") },
                    onArchive = viewModel::archive,
                    onCorrect = { id -> navController.navigate("job/$id?correct=true") },
                )
            }
            composable(
                Routes.JOB_DETAIL,
                arguments = listOf(
                    navArgument("jobId") { type = NavType.StringType },
                    navArgument("correct") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { entry ->
                val viewModel: JobDetailViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val jobId = entry.arguments?.getString("jobId").orEmpty()
                val requestedCorrection = entry.arguments?.getBoolean("correct") == true
                var correctionStarted by remember(jobId) { mutableStateOf(false) }
                LaunchedEffect(jobId, viewModel) { viewModel.load(jobId) }
                LaunchedEffect(jobId, state, requestedCorrection) {
                    if (requestedCorrection && !correctionStarted && state is com.saikalyan.jobmate.ui.jobs.JobDetailUiState.Content) {
                        correctionStarted = true
                        viewModel.correctDetails(jobId)
                    }
                }
                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        if (event == JobDetailEvent.Exit) navController.popBackStack()
                    }
                }
                JobDetailScreen(
                    state = state,
                    onEdit = { navController.navigate("job-editor/$jobId") },
                    onStatusChange = { viewModel.updateStatus(jobId, it) },
                    onArchive = { viewModel.archive(jobId) },
                    onDelete = { viewModel.delete(jobId) },
                    onCorrectDetails = { viewModel.correctDetails(jobId) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.NEW_JOB) {
                val viewModel: JobEditorViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(viewModel) { viewModel.load(null) }
                LaunchedEffect(state.saved) { if (state.saved) navController.popBackStack() }
                JobEditorScreen(
                    state = state,
                    onFormChange = viewModel::updateForm,
                    onSave = viewModel::save,
                    onLeave = { navController.popBackStack() },
                )
            }
            composable(Routes.EDIT_JOB, arguments = listOf(navArgument("jobId") { type = NavType.StringType })) { entry ->
                val viewModel: JobEditorViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val jobId = entry.arguments?.getString("jobId")
                LaunchedEffect(jobId, viewModel) { viewModel.load(jobId) }
                LaunchedEffect(state.saved) { if (state.saved) navController.popBackStack() }
                JobEditorScreen(
                    state = state,
                    onFormChange = viewModel::updateForm,
                    onSave = viewModel::save,
                    onLeave = { navController.popBackStack() },
                )
            }
            composable(Routes.CONTACTS) { ContactListRoute(onImport = { navController.navigate(Routes.CONTACT_IMPORT) }, onMatch = { id -> navController.navigate("contacts/$id/matches") }) }
            composable(Routes.CONTACT_IMPORT) { ContactImportRoute(onBack = { navController.popBackStack() }) }
            composable(Routes.CONTACT_MATCH, arguments = listOf(navArgument("contactId") { type = NavType.StringType })) { entry ->
                ContactMatchRoute(
                    contactId = entry.arguments?.getString("contactId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.RESUMES) { ResumeListRoute() }
            composable(Routes.TEMPLATES) { TemplateListRoute() }
            composable(Routes.OUTREACH) { OutreachComposerRoute() }
            composable(Routes.FOLLOW_UPS) { FollowUpRoute() }
            composable(Routes.SETTINGS) { GmailSettingsRoute() }
            composable(Routes.APP_SETTINGS) { AppSettingsRoute() }
            composable(Routes.SHEETS_SYNC) { SheetsSyncRoute() }
            composable(Routes.AI_SETTINGS) { AiSettingsRoute() }
            composable(Routes.CAPTURE_REVIEW) { CaptureRoute(sharedText = sharedText) }
        }
    }
    if (moreExpanded) {
        ModalBottomSheet(onDismissRequest = { moreExpanded = false }) {
            Column(Modifier.padding(bottom = 20.dp)) {
                Text("More from JobMate", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), style = MaterialTheme.typography.titleLarge)
                moreNavItems.forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.title) },
                        modifier = Modifier.clickable {
                            moreExpanded = false
                            navController.navigate(item.route) { launchSingleTop = true }
                        },
                    )
                }
            }
        }
    }
}


