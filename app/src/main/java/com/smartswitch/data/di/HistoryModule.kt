package com.smartswitch.data.di

import android.content.Context
import com.smartswitch.presentation.database.AppDatabase
import com.smartswitch.domain.repository.MediaHistoryRepository
import com.smartswitch.presentation.database.MediaHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HistoryModule {

    @Provides
    @Singleton
    fun provideDatabase(context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideMediaHistoryDao(database: AppDatabase): MediaHistoryDao {
        return database.mediaHistoryDao()
    }

    @Provides
    @Singleton
    fun provideMediaHistoryRepository(dao: MediaHistoryDao): MediaHistoryRepository {
        return MediaHistoryRepository(dao)
    }
}
