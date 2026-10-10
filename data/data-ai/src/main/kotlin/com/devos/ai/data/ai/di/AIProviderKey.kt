package com.devos.ai.data.ai.di

import com.devos.ai.domain.ai.model.AIProvider
import dagger.MapKey

/**
 * Hilt map key for the [AIProvider] → [AIProviderClient] multibinding.
 */
@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AIProviderKey(val value: AIProvider)
