package com.saikalyan.jobmate.ui.navigation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    const val JOB_DETAIL = "job/{jobId}"
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
    const val CAPTURE_REVIEW = "capture-review"
}

private data class NavItem(val route: String, val title: String)

private val navItems = listOf(
    NavItem(Routes.DASHBOARD, "Dashboard"),
    NavItem(Routes.JOBS, "Jobs"),
    NavItem(Routes.CONTACTS, "Contacts"),
    NavItem(Routes.CONTACT_IMPORT, "Import"),
    NavItem(Routes.RESUMES, "Resumes"),
    NavItem(Routes.TEMPLATES, "Templates"),
    NavItem(Routes.OUTREACH, "Outreach"),
    NavItem(Routes.FOLLOW_UPS, "Follow-ups"),
    NavItem(Routes.SETTINGS, "Settings"),
    NavItem(Routes.APP_SETTINGS, "App settings"),
    NavItem(Routes.SHEETS_SYNC, "Sheets sync"),
    NavItem(Routes.CAPTURE_REVIEW, "Capture review"),
)

@Composable
fun AppNavHost(modifier: Modifier = Modifier, sharedText: String? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) navController.navigate(Routes.CAPTURE_REVIEW) { launchSingleTop = true }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                navItems.forEach { item ->
                    FilterChip(
                        selected = currentDestination?.route == item.route,
                        onClick = { navController.navigate(item.route) { launchSingleTop = true } },
                        label = { Text(item.title) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
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
                    onRetry = viewModel::retry,
                    onAddJob = { navController.navigate(Routes.NEW_JOB) },
                    onOpenJob = { id -> navController.navigate("job/$id") },
                    onArchive = viewModel::archive,
                )
            }
            composable(Routes.JOB_DETAIL, arguments = listOf(navArgument("jobId") { type = NavType.StringType })) { entry ->
                val viewModel: JobDetailViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val jobId = entry.arguments?.getString("jobId").orEmpty()
                LaunchedEffect(jobId, viewModel) { viewModel.load(jobId) }
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
            composable(Routes.CAPTURE_REVIEW) { CaptureRoute(sharedText = sharedText) }
        }
    }
}

@Composable
private fun DestinationPlaceholder(title: String) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Navigation shell only. This feature will be implemented in a later step.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}






