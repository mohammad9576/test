package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.contactsStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_contacts")

/** Family relations a parent can pick for an approved contact. */
enum class Relation(val label: String, val emoji: String) {
    MOM("مامان", "👩"), DAD("بابا", "👨"), SISTER("خواهر", "👧"), BROTHER("برادر", "👦"),
    GRANDMA("مادربزرگ", "👵"), GRANDPA("پدربزرگ", "👴"),
    AMOO("عمو", "🧔"), AMME("عمه", "👩‍🦰"), KHALE("خاله", "👱‍♀️"), DAYI("دایی", "🧑"),
    FRIEND("دوست خانوادگی", "🧒"), OTHER("دیگر", "🙂"),
}

data class ApprovedContact(val name: String, val phone: String, val relation: Relation)

/** Phone numbers the parents approved for the child to call (one list for the family, children see all). */
@Singleton
class ContactsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("contacts")

    val contacts: Flow<List<ApprovedContact>> = context.contactsStore.data.map { p -> decode(p[key]) }

    suspend fun add(c: ApprovedContact) = context.contactsStore.edit { p ->
        val list = decode(p[key]).filter { it.phone != c.phone } + c
        p[key] = encode(list)
    }

    suspend fun remove(phone: String) = context.contactsStore.edit { p -> p[key] = encode(decode(p[key]).filter { it.phone != phone }) }

    companion object {
        fun clean(phone: String) = phone.map { if (it in '۰'..'۹') '0' + (it - '۰') else it }.filter { it.isDigit() || it == '+' }.joinToString("").take(16)
        private fun encode(l: List<ApprovedContact>) = l.joinToString("\n") { "${it.relation.name}|${it.phone}|${it.name.replace('|', ' ').replace('\n', ' ')}" }
        private fun decode(s: String?): List<ApprovedContact> = s.orEmpty().lines().mapNotNull { l ->
            val p = l.split('|', limit = 3)
            if (p.size < 3) null else ApprovedContact(p[2], p[1], runCatching { Relation.valueOf(p[0]) }.getOrDefault(Relation.OTHER))
        }
    }
}
