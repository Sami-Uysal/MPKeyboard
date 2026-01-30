package com.samiuysal.keyboard.data.password

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Password(
        val id: Long = 0,
        val siteName: String,
        val packageName: String,
        val username: String,
        val password: String,
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis()
)

@Singleton
class PasswordRepository
@Inject
constructor(private val passwordDao: PasswordDao, private val crypto: PasswordCrypto) {
    fun getAll(): Flow<List<Password>> =
            passwordDao.getAll().map { entities -> entities.map { it.toPassword() } }

    fun getByPackageName(packageName: String): Flow<List<Password>> =
            passwordDao.getByPackageName(packageName).map { entities ->
                entities.map { it.toPassword() }
            }

    fun search(query: String): Flow<List<Password>> =
            passwordDao.search(query).map { entities -> entities.map { it.toPassword() } }

    suspend fun getById(id: Long): Password? = passwordDao.getById(id)?.toPassword()

    suspend fun insert(password: Password): Long {
        val entity = password.toEntity()
        return passwordDao.insert(entity)
    }

    suspend fun update(password: Password) {
        val entity = password.toEntity().copy(updatedAt = System.currentTimeMillis())
        passwordDao.update(entity)
    }

    suspend fun delete(id: Long) {
        passwordDao.deleteById(id)
    }

    private fun PasswordEntity.toPassword(): Password =
            Password(
                    id = id,
                    siteName = siteName,
                    packageName = packageName,
                    username = username,
                    password =
                            try {
                                crypto.decrypt(encryptedPassword)
                            } catch (e: Exception) {
                                ""
                            },
                    createdAt = createdAt,
                    updatedAt = updatedAt
            )

    private fun Password.toEntity(): PasswordEntity =
            PasswordEntity(
                    id = id,
                    siteName = siteName,
                    packageName = packageName,
                    username = username,
                    encryptedPassword = crypto.encrypt(password),
                    createdAt = createdAt,
                    updatedAt = updatedAt
            )
}
