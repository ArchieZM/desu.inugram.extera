package desu.inugram.helpers

import android.content.SharedPreferences
import desu.inugram.InuConfig
import org.json.JSONArray
import org.json.JSONObject

interface MenuOrderItem {
    val key: String
    val labelRes: Int
    val iconRes: Int
    val ordinal: Int
}

data class MenuOrderEntry<I : MenuOrderItem>(val item: I, val enabled: Boolean)

abstract class MenuOrderConfig<I : MenuOrderItem>(
    key: String,
    private val allItems: List<I>,
    private val offByDefault: Set<I>,
) : InuConfig.Item<List<MenuOrderEntry<I>>>(key, allItems.map { MenuOrderEntry(it, it !in offByDefault) }) {

    protected abstract fun itemByKey(key: String): I?

    override fun read(prefs: SharedPreferences): List<MenuOrderEntry<I>> {
        val json = prefs.getString(this.key, "") ?: ""
        if (json.isEmpty()) return default
        return try {
            val arr = JSONArray(json)
            val seen = HashSet<I>()
            val out = ArrayList<MenuOrderEntry<I>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val item = itemByKey(obj.getString("k")) ?: continue
                if (!seen.add(item)) continue
                out.add(MenuOrderEntry(item, obj.optBoolean("e", true)))
            }
            for (it in allItems) {
                if (!seen.contains(it)) out.add(MenuOrderEntry(it, it !in offByDefault))
            }
            out
        } catch (_: Exception) {
            default
        }
    }

    override fun SharedPreferences.Editor.write() {
        val arr = JSONArray()
        for (e in value) {
            arr.put(JSONObject().apply {
                put("k", e.item.key)
                put("e", e.enabled)
            })
        }
        putString(key, arr.toString())
    }

    fun resetToDefault() {
        value = default
    }
}
