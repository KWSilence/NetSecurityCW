package com.kwsilence.di

import com.kwsilence.service.LoginService
import com.kwsilence.service.RegistrationService
import com.kwsilence.service.ResetPasswordService
import com.kwsilence.service.SyncService
import org.koin.dsl.module

val serviceModule = module {
    factory { LoginService(get()) }
    factory { RegistrationService(get()) }
    factory { ResetPasswordService(get()) }
    factory { SyncService(get()) }
}