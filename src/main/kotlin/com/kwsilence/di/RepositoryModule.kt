package com.kwsilence.di

import com.kwsilence.db.repository.AuthRepository
import com.kwsilence.db.repository.SyncRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { AuthRepository() }
    single { SyncRepository() }
}