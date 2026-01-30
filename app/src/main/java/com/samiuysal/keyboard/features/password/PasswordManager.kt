package com.samiuysal.keyboard.features.password

import com.samiuysal.keyboard.data.password.Password
import com.samiuysal.keyboard.data.password.PasswordRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Singleton
class PasswordManager
@Inject
constructor(private val repository: PasswordRepository, private val scope: CoroutineScope) {
    fun getPasswordsForPackage(packageName: String): Flow<List<Password>> =
            repository.getByPackageName(packageName)

    fun getAllPasswords(): Flow<List<Password>> = repository.getAll()

    fun searchPasswords(query: String): Flow<List<Password>> = repository.search(query)

    fun savePassword(
            siteName: String,
            packageName: String,
            username: String,
            password: String,
            onComplete: (Boolean) -> Unit = {}
    ) {
        scope.launch {
            try {
                repository.insert(
                        Password(
                                siteName = siteName,
                                packageName = packageName,
                                username = username,
                                password = password
                        )
                )
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun updatePassword(password: Password, onComplete: (Boolean) -> Unit = {}) {
        scope.launch {
            try {
                repository.update(password)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun deletePassword(id: Long, onComplete: (Boolean) -> Unit = {}) {
        scope.launch {
            try {
                repository.delete(id)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}
