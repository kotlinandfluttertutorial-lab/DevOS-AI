package com.devos.ai.core.common.di

import javax.inject.Qualifier

/**
 * Hilt qualifier for [kotlinx.coroutines.Dispatchers.IO].
 *
 * Use this to inject the IO dispatcher into ViewModels and UseCases so that
 * the dispatcher can be replaced with a test dispatcher in unit tests.
 *
 * Usage:
 * ```kotlin
 * class MyViewModel @Inject constructor(
 *     @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
 * )
 * ```
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Hilt qualifier for [kotlinx.coroutines.Dispatchers.Default].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * Hilt qualifier for [kotlinx.coroutines.Dispatchers.Main].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
