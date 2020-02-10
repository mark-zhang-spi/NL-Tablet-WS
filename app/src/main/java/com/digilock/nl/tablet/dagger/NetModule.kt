package com.digilock.nl.tablet.dagger

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.content.SharedPreferences
import com.digilock.nl.tablet.database.*
import com.digilock.nl.tablet.database.dao.*
import com.digilock.nl.tablet.util.*
import com.securitypeople.packagehold.util.scheduler.SchedulerProvider
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


/**
 * - NetModule for signaling Dagger to search within the available methods for possible instance providers
 */
@Module
class NetModule(private val baseUrl: String) {

    @Provides
    @Singleton
    fun providesUsersDatabase(application: Application): UsersDatabase =
            Room.databaseBuilder(application, UsersDatabase::class.java, DB_USERS).build()

    @Provides
    @Singleton
    fun providesUserDao(database: UsersDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun providesLocksDatabase(application: Application): LocksDatabase =
            Room.databaseBuilder(application, LocksDatabase::class.java, DB_LOCKS).build()

    @Provides
    @Singleton
    fun providesLockDao(database: LocksDatabase): LockDao = database.lockDao()

    @Provides
    @Singleton
    fun providesCredentialDao(database: CredentialsDatabase): CredentialDao = database.credentialDao()

    @Provides
    @Singleton
    fun providesCredentialsDatabase(application: Application): CredentialsDatabase =
            Room.databaseBuilder(application, CredentialsDatabase::class.java, DB_CREDS).build()

    @Provides
    @Singleton
    fun providesUserCredAssignsDatabase(application: Application): UserCredAssignsDatabase =
            Room.databaseBuilder(application, UserCredAssignsDatabase::class.java, DB_USERCREDASSIGNS).build()

    @Provides
    @Singleton
    fun providesUserCredAssignDao(database: UserCredAssignsDatabase): UserCredAssignDao = database.userCredAssignDao()

    @Provides
    @Singleton
    fun providesUserLockAssignsDatabase(application: Application): UserLockAssignsDatabase =
            Room.databaseBuilder(application, UserLockAssignsDatabase::class.java, DB_USERLOCKASSIGNS).build()

    @Provides
    @Singleton
    fun providesUserLockAssignDao(database: UserLockAssignsDatabase): UserLockAssignDao = database.userLockAssignDao()

    @Provides
    @Singleton
    fun provideSharedPreferences(application: Application): SharedPreferences = application.getSharedPreferences(SYSTEM_CONFIG, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun providesRetrofit(): Retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(baseUrl)
                    .build()

    @Provides
    fun providesSchedulerProvider(): SchedulerProvider = SchedulerProvider

/*
    @Provides
    @Singleton
    fun providesApiService(retrofit: Retrofit): PackageLockerApiService = retrofit.create(PackageLockerApiService::class.java)
*/

    companion object {
        private val LOG_TAG: String = NetModule::class.java.simpleName
    }

}