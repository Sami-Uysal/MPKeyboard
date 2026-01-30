package com.samiuysal.keyboard.data.password

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY siteName ASC") fun getAll(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE packageName = :packageName ORDER BY siteName ASC")
    fun getByPackageName(packageName: String): Flow<List<PasswordEntity>>

    @Query(
            "SELECT * FROM passwords WHERE siteName LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' ORDER BY siteName ASC"
    )
    fun search(query: String): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE id = :id") suspend fun getById(id: Long): PasswordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(password: PasswordEntity): Long

    @Update suspend fun update(password: PasswordEntity)

    @Delete suspend fun delete(password: PasswordEntity)

    @Query("DELETE FROM passwords WHERE id = :id") suspend fun deleteById(id: Long)
}
