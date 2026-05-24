package desu.inugram.helpers

import org.telegram.messenger.R
import org.telegram.ui.ChatActivity

class ChatMenuConfig(key: String) : MenuOrderConfig<ChatMenuConfig.Item>(key, Item.entries, OFF_BY_DEFAULT) {
    enum class Item(
        override val key: String,
        val ids: List<Int>,
        override val labelRes: Int,
        override val iconRes: Int,
    ) : MenuOrderItem {
        VIEW_AS_TOPICS("view_as_topics", listOf(ChatActivity.view_as_topics), R.string.TopicViewAsTopics, R.drawable.msg_topics),
        OPEN_DIRECT("open_direct", listOf(ChatActivity.open_direct), R.string.ChannelOpenDirect, R.drawable.msg_markunread),
        CALL("call", listOf(ChatActivity.call), R.string.Call, R.drawable.msg_callback),
        VIDEO_CALL("video_call", listOf(ChatActivity.video_call), R.string.VideoCall, R.drawable.msg_videocall),
        SEARCH("search", listOf(ChatActivity.search), R.string.Search, R.drawable.msg_search),
        TRANSLATE("translate", listOf(ChatActivity.translate), R.string.TranslateMessage, R.drawable.msg_translate),
        REPORT("report", listOf(ChatActivity.report), R.string.ReportChat, R.drawable.msg_report),
        ADD_CONTACT("add_contact", listOf(ChatActivity.share_contact), R.string.AddToContacts, R.drawable.msg_addcontact),
        SET_TIMER("set_timer", listOf(ChatActivity.chat_enc_timer), R.string.SetTimer, R.drawable.msg_autodelete),
        CHANGE_COLORS("change_colors", listOf(ChatActivity.change_colors), R.string.SetWallpapers, R.drawable.msg_background),
        ADD_SHORTCUT("add_shortcut", listOf(ChatActivity.add_shortcut), R.string.AddShortcut, R.drawable.msg_home),
        CLEAR_HISTORY("clear_history", listOf(ChatActivity.clear_history), R.string.ClearHistory, R.drawable.msg_clear),
        DELETE_CHAT("delete_chat", listOf(ChatActivity.delete_chat), R.string.DeleteChatUser, R.drawable.msg_delete),
        BOT_SETTINGS("bot_settings", listOf(ChatActivity.bot_settings), R.string.InuBotSettings, R.drawable.msg_settings_old),
        BOT_HELP("bot_help", listOf(ChatActivity.bot_help), R.string.InuBotHelp, R.drawable.msg_help),
        OPEN_FORUM("open_forum", listOf(ChatActivity.open_forum), R.string.OpenAllTopics, R.drawable.msg_discussion),
        CLOSE_TOPIC("close_topic", listOf(ChatActivity.topic_close), R.string.CloseTopic, R.drawable.msg_topic_close),
        SHOW_PINNED_PANEL("show_pinned_panel", listOf(ChatActionsHelper.ACTION_SHOW_PINNED_PANEL), R.string.InuShowPinnedPanel, R.drawable.msg_pin),
        RECENT_ACTIONS("recent_actions", listOf(ChatActionsHelper.ACTION_RECENT_ACTIONS), R.string.EventLog, R.drawable.msg_log),
        GO_TO_BEGINNING("go_to_beginning", listOf(ChatActionsHelper.ACTION_GO_TO_BEGINNING), R.string.InuJumpToBeginning, R.drawable.msg_go_up),
        GO_TO_MESSAGE("go_to_message", listOf(ChatActionsHelper.ACTION_GO_TO_MESSAGE), R.string.InuGoToMessage, R.drawable.msg_message);

        companion object {
            private val byId: Map<Int, Item> by lazy {
                val map = HashMap<Int, Item>()
                for (e in Item.entries) for (id in e.ids) map[id] = e
                map
            }

            private val byKey: Map<String, Item> by lazy { Item.entries.associateBy { it.key } }

            fun forId(id: Int): Item? = byId[id]
            fun forKey(k: String): Item? = byKey[k]
        }
    }

    override fun itemByKey(key: String): Item? = Item.forKey(key)

    companion object {
        private val OFF_BY_DEFAULT = setOf(Item.RECENT_ACTIONS, Item.GO_TO_BEGINNING, Item.GO_TO_MESSAGE)
    }
}
