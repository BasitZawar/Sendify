package com.smartswitch.data.di

import com.smartswitch.data.repository.MediaRepositoryImp
import com.smartswitch.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Singleton
    @Binds
    abstract fun bindMediaRepository(mediaRepositoryImp: MediaRepositoryImp): MediaRepository

}