package com.devos.ai.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * App-level Hilt module.
 *
 * Database, DAOs, and WorkManager are provided by dedicated modules in their
 * respective core modules so the app module stays thin:
 *
 * - Database + DAOs  → :core:core-database  (DatabaseModule)
 * - WorkManager init → :app AndroidManifest <provider> (auto-init via HiltWorkerFactory)
 *
 * Add app-level singleton bindings here only when they have no natural home in
 * a core or feature module.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
