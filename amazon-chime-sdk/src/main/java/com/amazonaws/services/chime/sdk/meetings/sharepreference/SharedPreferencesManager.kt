package com.amazonaws.services.chime.sdk.meetings.sharepreference

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import com.google.gson.Gson

class SharedPreferencesManager constructor(context: Context) {

    private var mPref: SharedPreferences? = null
    private var mBulkUpdate = false
    private var mEditor: Editor? = null
    init {
        mPref = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
    }

    fun put(sharePreferenceKey: SharePreferenceKey, value: String?) {
        doEdit()
        mEditor?.putString(sharePreferenceKey.name, value)
        doCommit()
    }

    fun putObject(sharePreferenceKey: SharePreferenceKey, `object`: Any?) {
        doEdit()
        val gson = Gson()
        requireNotNull(`object`) { "Object is null" }
        require(
            sharePreferenceKey.name != ""
        ) { "Key is empty or null" }
        mEditor!!.putString(sharePreferenceKey.name, gson.toJson(`object`))
        doCommit()
    }

    fun <T> getObject(sharePreferenceKey: SharePreferenceKey, a: Class<T>?): T? {
        val gson = Gson()
        val json = mPref!!.getString(sharePreferenceKey.name, null)
        return try {
            gson.fromJson(json, a)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                ("Object stored with key " +
                        sharePreferenceKey.name) + " is instance of other class"
            )
        }
    }
    private fun doEdit() {
        if (!mBulkUpdate && mEditor == null) {
            mEditor = mPref?.edit()
        }
    }

    private fun doCommit() {
        if (!mBulkUpdate && mEditor != null) {
            mEditor?.commit()
            mEditor = null
        }
    }

    companion object {
        private const val SETTINGS_NAME = "default_settings"
        private var sSharedPrefs: SharedPreferencesManager? = null
        fun getInstance(context: Context): SharedPreferencesManager {
            if (sSharedPrefs == null) {
                sSharedPrefs = SharedPreferencesManager(context.applicationContext)
            }
            return sSharedPrefs!!
        }
    }
}
