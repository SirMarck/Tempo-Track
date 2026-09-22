package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

object TagManager {
    private const val PREFS_NAME = "tempo_tags_prefs"
    private const val KEY_TAGS = "custom_tags"

    val DEFAULT_TAGS = listOf("#Dev", "#Reunião", "#Design", "#Suporte", "#Consultoria", "#Operacional")

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTags(context: Context): List<String> {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_TAGS, null) ?: return DEFAULT_TAGS
        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            if (list.isEmpty()) DEFAULT_TAGS else list
        } catch (e: Exception) {
            DEFAULT_TAGS
        }
    }

    fun saveTags(context: Context, tags: List<String>) {
        val cleanTags = tags.map { formatTag(it) }.filter { it.isNotBlank() }.distinct()
        val jsonArray = JSONArray(cleanTags)
        getPrefs(context).edit().putString(KEY_TAGS, jsonArray.toString()).apply()
    }

    fun addTag(context: Context, rawTag: String): List<String> {
        val formatted = formatTag(rawTag)
        if (formatted.isBlank()) return getTags(context)
        val current = getTags(context).toMutableList()
        if (!current.contains(formatted)) {
            current.add(formatted)
            saveTags(context, current)
        }
        return current
    }

    fun updateTag(context: Context, oldTag: String, newRawTag: String): List<String> {
        val formattedNew = formatTag(newRawTag)
        if (formattedNew.isBlank()) return getTags(context)
        val current = getTags(context).toMutableList()
        val idx = current.indexOf(oldTag)
        if (idx != -1) {
            current[idx] = formattedNew
            saveTags(context, current)
        }
        return current
    }

    fun deleteTag(context: Context, tag: String): List<String> {
        val current = getTags(context).toMutableList()
        current.remove(tag)
        saveTags(context, current)
        return current
    }

    private fun formatTag(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("#")) trimmed else "#$trimmed"
    }
}
