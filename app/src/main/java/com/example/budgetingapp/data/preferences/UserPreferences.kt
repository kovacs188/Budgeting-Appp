package com.example.budgetingapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    private val context: Context
) {
    private object PreferenceKeys {
        val CATEGORY_CARD_ORDER = stringPreferencesKey("category_card_order")
    }

    val categoryCardOrder: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val orderString = preferences[PreferenceKeys.CATEGORY_CARD_ORDER]
                ?: "INCOME,FIXED_EXPENSE,VARIABLE_EXPENSE,DISCRETIONARY_EXPENSE"
            orderString.split(",")
        }

    suspend fun saveCategoryCardOrder(order: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.CATEGORY_CARD_ORDER] = order.joinToString(",")
        }
    }
}