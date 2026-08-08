package com.routeguard.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.edit.put
import kotlinx.coroutines.flow.*

class TokenStore(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val EXPIRES_AT_KEY = longPreferencesKey("expires_at")
    }

    private val dataStore: DataStore<Preferences> = context.createDataStore(name = "token_store")

    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
            preferences[EXPIRES_AT_KEY] = expiresAt
        }
    }

    suspend fun getAccessToken(): String? {
        return dataStore.data.first().entries
            .firstOrNull { it.key == ACCESS_TOKEN_KEY }
            ?.value
    }

    suspend fun getRefreshToken(): String? {
        return dataStore.data.first().entries
            .firstOrNull { it.key == REFRESH_TOKEN_KEY }
            ?.value
    }

    suspend fun getExpiresAt(): Long {
        return dataStore.data.first().entries
            .firstOrNull { it.key == EXPIRES_AT_KEY }
            ?.value ?: 0L
    }

    suspend fun isTokenExpired(): Boolean {
        val expiresAt = getExpiresAt()
        return System.currentTimeMillis() >= expiresAt
    }

    suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(EXPIRES_AT_KEY)
        }
    }
}