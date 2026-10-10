package com.devos.ai.data.ai.di

import dagger.MapKey

/** Hilt map key for the `Map<String, AgentToolExecutor>` multibinding. */
@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AgentToolKey(val value: String)
