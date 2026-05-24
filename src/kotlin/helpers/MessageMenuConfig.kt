package desu.inugram.helpers

import org.telegram.messenger.R
import org.telegram.ui.ChatActivity

class MessageMenuConfig(key: String) : MenuOrderConfig<MessageMenuConfig.Item>(key, Item.entries, OFF_BY_DEFAULT) {
    enum class Item(
        override val key: String,
        val optionIds: List<Int>,
        override val labelRes: Int,
        override val iconRes: Int,
    ) : MenuOrderItem {
        REPLY("reply", listOf(ChatActivity.OPTION_REPLY), R.string.Reply, R.drawable.menu_reply),
        REPLY_IN("reply_in", listOf(ChatHelper.OPTION_REPLY_IN), R.string.InuReplyIn, R.drawable.menu_reply),
        ADD_TO_STICKERS(
            "add_to_stickers",
            listOf(ChatActivity.OPTION_ADD_TO_STICKERS_OR_MASKS, ChatActivity.OPTION_ADD_STICKER_TO_FAVORITES, ChatActivity.OPTION_ADD_TO_GIFS),
            R.string.InuAddToStickersGifs,
            R.drawable.msg_sticker
        ),
        COPY("copy", listOf(ChatActivity.OPTION_COPY), R.string.Copy, R.drawable.msg_copy),
        COPY_LINK("copy_link", listOf(ChatActivity.OPTION_COPY_LINK), R.string.CopyLink, R.drawable.msg_link),
        SAVE_TO_GALLERY(
            "save_to_gallery",
            listOf(ChatActivity.OPTION_SAVE_TO_GALLERY, ChatActivity.OPTION_SAVE_TO_GALLERY2),
            R.string.SaveToGallery,
            R.drawable.msg_gallery
        ),
        SAVE_TO_DOWNLOADS(
            "save_to_downloads",
            listOf(ChatActivity.OPTION_SAVE_TO_DOWNLOADS_OR_MUSIC),
            R.string.SaveToDownloads,
            R.drawable.msg_download
        ),
        FORWARD("forward", listOf(ChatActivity.OPTION_FORWARD), R.string.Forward, R.drawable.msg_forward),
        FORWARD_NO_QUOTE("forward_no_quote", listOf(ChatHelper.OPTION_FORWARD_NO_QUOTE), R.string.InuForwardNoQuote, R.drawable.msg_forward),
        SAVE("save", listOf(ChatHelper.OPTION_SAVE), R.string.InuSaveToSavedMessages, R.drawable.msg_saved),
        PIN("pin", listOf(ChatActivity.OPTION_PIN, ChatActivity.OPTION_UNPIN), R.string.PinMessage, R.drawable.msg_pin),
        TRANSLATE(
            "translate",
            listOf(ChatActivity.OPTION_TRANSLATE, ChatHelper.OPTION_TRANSLATE_REVERT),
            R.string.TranslateMessage,
            R.drawable.msg_translate
        ),
        SUMMARIZE("summarize", listOf(ChatHelper.OPTION_SUMMARIZE), R.string.InuSummarize, R.drawable.magic_stick_solar),
        EDIT("edit", listOf(ChatActivity.OPTION_EDIT), R.string.Edit, R.drawable.msg_edit),
        REPORT("report", listOf(ChatActivity.OPTION_REPORT_CHAT), R.string.ReportChat, R.drawable.msg_report),
        SHARE("share", listOf(ChatActivity.OPTION_SHARE), R.string.ShareFile, R.drawable.msg_share),
        STATISTICS("statistics", listOf(ChatActivity.OPTION_STATISTICS), R.string.Statistics, R.drawable.msg_stats),
        SHOW_IN_CHAT("show_in_chat", listOf(ChatHelper.OPTION_SHOW_IN_CHAT), R.string.InuShowInChat, R.drawable.msg_openin),
        REMOVE_FROM_CACHE("remove_from_cache", listOf(ChatHelper.OPTION_REMOVE_FROM_CACHE), R.string.InuRemoveFromCache, R.drawable.msg_clear),
        DELETE("delete", listOf(ChatActivity.OPTION_DELETE), R.string.Delete, R.drawable.msg_delete),
        DETAILS("details", listOf(ChatHelper.OPTION_DETAILS), R.string.InuMessageDetails, R.drawable.msg_info);

        companion object {
            private val byOption: Map<Int, Item> by lazy {
                val map = HashMap<Int, Item>()
                for (e in Item.entries) for (id in e.optionIds) map[id] = e
                map
            }

            private val byKey: Map<String, Item> by lazy { Item.entries.associateBy { it.key } }

            fun forOption(optionId: Int): Item? = byOption[optionId]
            fun forKey(key: String): Item? = byKey[key]
        }
    }

    override fun itemByKey(key: String): Item? = Item.forKey(key)

    companion object {
        private val OFF_BY_DEFAULT = setOf(Item.REPLY_IN, Item.DETAILS, Item.FORWARD_NO_QUOTE, Item.SUMMARIZE, Item.REMOVE_FROM_CACHE)
    }
}
