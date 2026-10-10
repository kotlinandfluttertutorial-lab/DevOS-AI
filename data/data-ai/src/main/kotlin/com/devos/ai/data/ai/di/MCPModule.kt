package com.devos.ai.data.ai.di

import com.devos.ai.data.ai.repository.MCPRepositoryImpl
import com.devos.ai.domain.ai.repository.MCPRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding the MCP repository.
 *
 * DEVOS-038 / DA-49
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MCPModule {

    @Binds
    @Singleton
    abstract fun bindMCPRepository(impl: MCPRepositoryImpl): MCPRepository
}
