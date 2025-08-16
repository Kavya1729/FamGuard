package com.example.myapplication
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contactModel: ContactModel)

    @Query("Select * from contactmodel")
    suspend fun getAllContacts(): List<ContactModel>
}