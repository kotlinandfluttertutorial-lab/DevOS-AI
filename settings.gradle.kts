pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DevOS-AI"

include(":app")

// Core
include(":core:core-common")
include(":core:core-network")
include(":core:core-database")
include(":core:core-security")
include(":core:core-ui")
include(":core:core-testing")

// Design System
include(":designsystem")

// Feature modules
include(":feature:feature-auth")
include(":feature:feature-home")
include(":feature:feature-project")
include(":feature:feature-repository")
include(":feature:feature-code")
include(":feature:feature-ai-chat")
include(":feature:feature-agents")
include(":feature:feature-git")
include(":feature:feature-issues")
include(":feature:feature-prs")
include(":feature:feature-security")
include(":feature:feature-testing")
include(":feature:feature-learning")
include(":feature:feature-memory")
include(":feature:feature-settings")

// Domain modules
include(":domain:domain-repository")
include(":domain:domain-code")
include(":domain:domain-ai")
include(":domain:domain-git")
include(":domain:domain-learning")

// Data modules
include(":data:data-repository")
include(":data:data-code")
include(":data:data-ai")
include(":data:data-git")
include(":data:data-learning")
