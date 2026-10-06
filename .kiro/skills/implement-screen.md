---
name: implement-screen
description: Implement a new Jetpack Compose screen for DevOS AI following Clean Architecture, MVVM, and the design system
---

# Skill: Implement DevOS AI Screen

## When to Use
Use this skill when implementing any new screen in DevOS AI.

## Required Inputs
- Jira ticket ID (e.g., DEVOS-026)
- Figma screen reference (e.g., FIGMA-16)
- Screen name (e.g., AIChatScreen)
- Feature module (e.g., feature-ai-chat)

## Step-by-Step Process

### 1. Read the Spec First
```
.kiro/specs/<module>/<screen>.md
docs/figma/screen-inventory.md  → find the screen's section
docs/figma/component-inventory.md → identify which components to use
```

### 2. Define UiState
Create `<ScreenName>UiState.kt` in `presentation/`:
```kotlin
sealed interface <ScreenName>UiState {
    data object Loading : <ScreenName>UiState
    data class Success(/* typed data */) : <ScreenName>UiState
    data object Empty : <ScreenName>UiState
    data class Error(val message: String, val retryable: Boolean) : <ScreenName>UiState
}
```

### 3. Define ViewModel
```kotlin
@HiltViewModel
class <ScreenName>ViewModel @Inject constructor(
    private val useCase: <PrimaryUseCase>,
) : ViewModel() {
    private val _uiState = MutableStateFlow<<ScreenName>UiState>(<ScreenName>UiState.Loading)
    val uiState: StateFlow<<ScreenName>UiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            useCase()
                .catch { e -> _uiState.value = <ScreenName>UiState.Error(e.message ?: "Error", true) }
                .collect { data -> _uiState.value = <ScreenName>UiState.Success(data) }
        }
    }
}
```

### 4. Implement Screen Composable
```kotlin
@Composable
fun <ScreenName>Screen(
    onNavigateBack: () -> Unit,
    onNavigateTo<Detail>: (String) -> Unit,
    viewModel: <ScreenName>ViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { DevOSTopBar(title = "<Title>", navigationIcon = { BackButton(onNavigateBack) }) }
    ) { padding ->
        when (val s = uiState) {
            is <ScreenName>UiState.Loading -> DevOSLoadingState(modifier = Modifier.padding(padding))
            is <ScreenName>UiState.Success -> <ScreenName>Content(s, Modifier.padding(padding))
            is <ScreenName>UiState.Empty   -> DevOSEmptyState(
                icon = Icons.Outlined.<Relevant>,
                title = "No <items>",
                description = "<Context-appropriate description>",
                modifier = Modifier.padding(padding),
            )
            is <ScreenName>UiState.Error   -> DevOSErrorState(
                description = s.message,
                onRetry = if (s.retryable) viewModel::load else null,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
```

### 5. Register Navigation Route
Add to `<FeatureName>Navigation.kt`:
```kotlin
fun NavGraphBuilder.<featureName>Navigation(navController: NavController) {
    composable(
        route = DevOSRoutes.<SCREEN_NAME>,
        deepLinks = listOf(navDeepLink { uriPattern = "devos://<path>" }),
    ) {
        <ScreenName>Screen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
```

### 6. Add Hilt Module (if needed)
Only needed when providing screen-specific dependencies not already in global modules.

### 7. Write Tests
```kotlin
// ViewModel test
class <ScreenName>ViewModelTest {
    @Test fun `loading state emitted on init`() = runTest { ... }
    @Test fun `success state emitted when use case returns data`() = runTest { ... }
    @Test fun `error state emitted when use case throws`() = runTest { ... }
}

// UI test
class <ScreenName>ScreenTest {
    @Test fun `loading state shows shimmer`() { ... }
    @Test fun `success state shows content`() { ... }
    @Test fun `empty state shows empty illustration`() { ... }
    @Test fun `error state shows retry button`() { ... }
}
```

## Checklist Before Done
- [ ] UiState sealed interface defined with all 4 states
- [ ] ViewModel uses StateFlow
- [ ] Screen composable uses `collectAsStateWithLifecycle()`
- [ ] All 4 states rendered
- [ ] Only DevOS design system components used
- [ ] No hardcoded colors, sizes, or spacing
- [ ] Accessibility labels present on all icons
- [ ] Navigation route registered
- [ ] Dark mode looks correct
- [ ] ViewModel unit test written
- [ ] Screen UI test written
- [ ] Jira ticket acceptance criteria checked
