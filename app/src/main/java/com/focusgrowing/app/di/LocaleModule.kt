package com.focusgrowing.app.di

import com.focusgrowing.app.core.locale.ResDomainStrings
import com.focusgrowing.app.domain.repository.DomainStrings
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocaleModule {
    /** Domain texts (inbox entries, insights) come from the translated string resources. */
    @Binds abstract fun bindDomainStrings(impl: ResDomainStrings): DomainStrings
}
