package uz.xasan.orvell.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import uz.xasan.orvell.R
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Space
import uz.xasan.orvell.ui.theme.Tones

/**
 * Eski versiyadan qolgan qiymatlar haqida ogohlantirish. Talqini tasdiqlanmagan, yangilanmaydi.
 * Ekran uni faqat eski namuna bor bo'lsa ko'rsatadi.
 */
@Composable
fun LegacyNotice(modifier: Modifier = Modifier) {
    OCard(modifier, glow = Tones.pressure.a, padding = Space.m) {
        Text(stringResource(R.string.legacy_notice), style = OType.caption, color = O.c.text2)
    }
}
