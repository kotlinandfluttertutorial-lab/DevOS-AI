package com.devos.ai.data.ai.recommendation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Persists a dismissed recommendation ID to DataStore so it is filtered
 * on the next Home Dashboard load.
 *
 * Uses the `@Named("home")` [DataStore] that is already provided by
 * [com.devos.ai.feature.home.di.HomeModule], which lives in [SingletonComponent].
 *
 * DEVOS-058 / DA-70
 */
@Singleton
class DismissRecommendationUseCase @Inject constructor(
    @Named("home") private val dataStore: DataStore<Preferences>,
) {

    companion object {
        /** Preferences key that stores the set of dismissed recommendation IDs. */
        val DISMISSED_IDS_KEY = stringSetPreferencesKey("dismissed_recommendations")
    }

    /**
     * Adds [id] to the persisted set of dismissed recommendation IDs.
     *
     * This is a fire-and-forget operation; errors are logged but not rethrown.
     *
     * @param id Recommendation ID to dismiss.
     */
    suspend operator fun invoke(id: String) {
        runCatching {
            dataStore.edit { prefs ->
                val current = prefs[DISMISSED_IDS_KEY] ?: emptySet()
                prefs[DISMISSED_IDS_KEY] = current + id
            }
        }.onFailure { e ->
            Timber.e(e, "DismissRecommendationUseCase: failed to persist dismissed id=%s", id)
        }
    }
}
