package desu.inugram.helpers.emoji

import desu.inugram.InuConfig
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.BulletinFactory

object PremiumEmojiHelper {
    private const val HINT_PREFS = "premium_emoji_hint"

    @JvmStatic
    fun shouldAllowNonPremium(): Boolean = InuConfig.ALLOW_PREMIUM_EMOJI_FOR_ALL.getValue()

    @JvmStatic
    fun shouldShowHint(account: Int): Boolean {
        val prefs = ApplicationLoader.applicationContext
            .getSharedPreferences(HINT_PREFS, 0)
        return !prefs.getBoolean("hint_shown_$account", false)
    }

    @JvmStatic
    fun markHintShown(account: Int) {
        ApplicationLoader.applicationContext
            .getSharedPreferences(HINT_PREFS, 0)
            .edit()
            .putBoolean("hint_shown_$account", true)
            .apply()
    }

    @JvmStatic
    fun showHintBulletin(fragment: BaseFragment?) {
        if (fragment == null) return
        BulletinFactory.of(fragment)
            .createSimpleBulletin(
                R.raw.chats_infotip,
                LocaleController.getString(R.string.InuLocalPremiumEmojiHint),
            )
            .show()
    }

    @JvmStatic
    fun isFreeEmoji(document: TLRPC.Document?): Boolean {
        if (document == null) return false
        for (i in 0 until document.attributes.size) {
            if (document.attributes[i] is TLRPC.TL_documentAttributeCustomEmoji) {
                val attr = document.attributes[i] as TLRPC.TL_documentAttributeCustomEmoji
                if (attr.free) return true
            }
        }
        return false
    }

    @JvmStatic
    fun isPremiumSticker(document: TLRPC.Document?): Boolean {
        if (document == null) return false
        for (i in 0 until document.attributes.size) {
            if (document.attributes[i] is TLRPC.TL_documentAttributeSticker) {
                for (j in 0 until document.video_thumbs.size) {
                    val thumb = document.video_thumbs[j]
                    if (thumb is TLRPC.TL_videoSize && "f" == thumb.type) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
