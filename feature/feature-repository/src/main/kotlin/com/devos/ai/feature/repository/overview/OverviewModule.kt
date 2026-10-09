package com.devos.ai.feature.repository.overview

import com.devos.ai.feature.repository.model.RepoOverview
import com.devos.ai.feature.repository.model.stubRepoOverview
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the (stub) repository overview.
 *
 * Defined as an interface so it can be mocked in [OverviewViewModelTest] and so
 * the Error state can be exercised by making [loadOverview] throw.
 * DEVOS-015 will replace this with a real domain use case.
 */
interface RepositoryOverviewProvider {
    suspend fun loadOverview(): RepoOverview
}

/** DEVOS-016 stub. Returns the mockup data. Replaced by a real domain use case in a later ticket. */
@Singleton
class StubRepositoryOverviewProvider @Inject constructor() : RepositoryOverviewProvider {
    override suspend fun loadOverview(): RepoOverview = stubRepoOverview()
}

@Module
@InstallIn(SingletonComponent::class)
object OverviewModule {
    @Provides
    @Singleton
    fun provideRepositoryOverviewProvider(
        impl: StubRepositoryOverviewProvider,
    ): RepositoryOverviewProvider = impl
}
