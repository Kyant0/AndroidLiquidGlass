package com.kyant.backdrop.catalog.chat.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CachedConversationEntity::class, CachedMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatCacheDatabase : RoomDatabase() {
    abstract fun chatCacheDao(): ChatCacheDao

    companion object {
        @Volatile
        private var instance: ChatCacheDatabase? = null

        fun getInstance(context: Context): ChatCacheDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatCacheDatabase::class.java,
                    "chat_cache.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
