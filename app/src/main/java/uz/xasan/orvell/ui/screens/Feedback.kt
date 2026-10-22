package uz.xasan.orvell.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.xasan.orvell.BuildConfig
import uz.xasan.orvell.Contacts
import uz.xasan.orvell.R
import uz.xasan.orvell.ui.components.OCard
import uz.xasan.orvell.ui.components.OField
import uz.xasan.orvell.ui.components.Overline
import uz.xasan.orvell.ui.components.PrimaryButton
import uz.xasan.orvell.ui.components.IconBadge
import uz.xasan.orvell.ui.components.TopBar
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Tones

/** Xabarga qo'shiladigan texnik ma'lumot: faqat versiyalar va model, shaxsiy ma'lumot yo'q. */
private data class DeviceInfo(val app: String, val android: String, val model: String) {
    fun asText(ctx: Context) = listOf(
        ctx.getString(R.string.feedback_app_version) to app,
        ctx.getString(R.string.feedback_android_version) to android,
        ctx.getString(R.string.feedback_phone_model) to model,
    ).joinToString("\n") { (k, v) -> "$k: $v" }
}

private fun deviceInfo() = DeviceInfo(
    app = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    android = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    model = listOf(Build.MANUFACTURER, Build.MODEL).filter { !it.isNullOrBlank() }.distinct().joinToString(" ")
        .replaceFirstChar { it.uppercase() },
)

/**
 * Pochta ilovasini to'ldirilgan xat bilan ochadi (ACTION_SENDTO, mailto). Pochta ilovasi bo'lmasa false.
 * Loyiha manzili bo'sh bo'lsa qabul qiluvchini foydalanuvchi pochta ilovasida o'zi kiritadi.
 */
private fun sendEmail(ctx: Context, subject: String, body: String): Boolean {
    val to = Contacts.FEEDBACK_EMAIL
    val uri = Uri.parse("mailto:$to?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
    val intent = Intent(Intent.ACTION_SENDTO, uri)
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, body)
    if (to.isNotBlank()) intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
    return try { ctx.startActivity(intent); true } catch (_: ActivityNotFoundException) { false }
}

@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val info = deviceInfo()
    var message by rememberSaveable { mutableStateOf("") }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.feedback_title), onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OCard(glow = Tones.heart.a) {
                    Row(verticalAlignment = Alignment.Top) {
                        IconBadge(Icons.Rounded.Feedback, Tones.heart, 44.dp, filled = true)
                        Spacer(Modifier.width(14.dp))
                        Text(stringResource(R.string.feedback_intro), style = OType.body, color = O.c.text2, modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                OField(message, { message = it; notice = null }, stringResource(R.string.feedback_hint), Modifier.heightIn(min = 160.dp), singleLine = false)
            }
            item {
                OCard {
                    Overline(stringResource(R.string.feedback_attached))
                    Spacer(Modifier.height(8.dp))
                    InfoLine(stringResource(R.string.feedback_app_version), info.app)
                    InfoLine(stringResource(R.string.feedback_android_version), info.android)
                    InfoLine(stringResource(R.string.feedback_phone_model), info.model)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.feedback_attached_note), style = OType.caption, color = O.c.text2)
                }
            }
            item {
                val subject = stringResource(R.string.feedback_subject, BuildConfig.VERSION_NAME)
                val noEmailApp = stringResource(R.string.feedback_no_email_app)
                PrimaryButton(stringResource(R.string.feedback_send_email), {
                    val body = message.trim() + "\n\n---\n" + info.asText(ctx)
                    if (!sendEmail(ctx, subject, body)) {
                        // pochta ilovasi yo'q: matnni nusxalab, buni aytamiz
                        ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(subject, body))
                        notice = noEmailApp
                    }
                }, Modifier.fillMaxWidth(), enabled = message.isNotBlank(), icon = Icons.AutoMirrored.Rounded.Send)
                notice?.let { Text(it, style = OType.caption, color = O.c.warning, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = OType.caption, color = O.c.text2, modifier = Modifier.weight(1f))
        Text(value, style = OType.bodyStrong, color = O.c.text)
    }
}

@uz.xasan.orvell.ui.preview.OrvellPreviews
@Composable
private fun FeedbackPreview() = uz.xasan.orvell.ui.preview.PreviewSurface(Tones.heart) { FeedbackScreen {} }
