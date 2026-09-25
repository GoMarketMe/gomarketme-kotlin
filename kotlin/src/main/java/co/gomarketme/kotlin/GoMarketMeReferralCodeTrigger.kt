package co.gomarketme.kotlin

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** A remotely configured referral-code button or link that opens the native sheet. */
@Composable
fun GoMarketMeReferralCodeTrigger(
    modifier: Modifier = Modifier,
    onResult: (GoMarketMeAffiliateMarketingData?) -> Unit = {},
    onError: (Throwable) -> Unit = {}
) {
    val activity = LocalContext.current.findActivity()
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf<TriggerSettings?>(null) }
    var opening by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settings = try {
            TriggerSettings.from(GoMarketMe.referralCodeSettings())
        } catch (error: Throwable) {
            onError(error)
            TriggerSettings.defaults
        }
    }

    val current = settings ?: return
    val shape = RoundedCornerShape(current.borderRadius.dp)
    val foreground = if (current.isLink) current.linkColor else current.buttonTextColor
    val decoration = if (current.isLink && current.linkUnderline) TextDecoration.Underline else null
    val control = Modifier
        .defaultMinSize(minHeight = 44.dp)
        .clip(shape)
        .then(
            if (current.isLink) Modifier
            else Modifier
                .background(current.buttonBackground, shape)
                .border(BorderStroke(current.borderWidth.dp, current.buttonBorder), shape)
                .padding(
                    horizontal = current.horizontalPadding.dp,
                    vertical = current.verticalPadding.dp
                )
        )
        .alpha(if (opening) 0.6f else 1f)
        .clickable(enabled = !opening && activity != null) {
            val host = activity ?: return@clickable
            opening = true
            scope.launch {
                try {
                    GoMarketMe.showReferralCodeSheet(host, onResult = onResult)
                } catch (error: Throwable) {
                    onError(error)
                } finally {
                    opening = false
                }
            }
        }

    Box(modifier = modifier.fillMaxWidth()) {
        BasicText(
            text = current.text,
            modifier = control.align(current.alignment),
            style = TextStyle(
                color = foreground,
                fontSize = current.fontSize.sp,
                fontFamily = current.fontFamily,
                fontWeight = current.fontWeight,
                textDecoration = decoration
            )
        )
    }
}

private data class TriggerSettings(
    val isLink: Boolean,
    val text: String,
    val alignment: Alignment,
    val fontSize: Float,
    val fontFamily: FontFamily,
    val fontWeight: FontWeight,
    val linkColor: Color,
    val linkUnderline: Boolean,
    val buttonBackground: Color,
    val buttonTextColor: Color,
    val buttonBorder: Color,
    val borderWidth: Float,
    val borderRadius: Float,
    val horizontalPadding: Float,
    val verticalPadding: Float
) {
    companion object {
        val defaults = from(emptyMap())

        fun from(response: Map<String, Any?>): TriggerSettings {
            val marketer = response.map("marketer_settings")
            val defaults = response.map("default_settings")
            val source = when {
                marketer.isNotEmpty() -> marketer
                defaults.isNotEmpty() -> defaults
                else -> response
            }
            fun string(key: String, fallback: String) = source[key] as? String ?: fallback
            fun number(key: String, fallback: Float, minimum: Float = 0f): Float {
                val parsed = (source[key] as? Number)?.toFloat() ?: fallback
                return parsed.coerceIn(minimum, 200f)
            }
            return TriggerSettings(
                isLink = string("triggerType", "link") != "button",
                text = string("triggerText", "Have a referral code?"),
                alignment = when (string("triggerAlignment", "center")) {
                    "left" -> Alignment.CenterStart
                    "right" -> Alignment.CenterEnd
                    else -> Alignment.Center
                },
                fontSize = number("triggerFontSize", 15f, 10f),
                fontFamily = fontFamily(string("family", "")),
                fontWeight = fontWeight(string("triggerFontWeight", "500")),
                linkColor = color(string("triggerLinkColor", "#1F2937"), Color(0xFF1F2937)),
                linkUnderline = source["triggerLinkUnderline"] as? Boolean ?: false,
                buttonBackground = color(string("triggerButtonBackground", "#3B82F6"), Color(0xFF3B82F6)),
                buttonTextColor = color(string("triggerButtonTextColor", "#FFFFFF"), Color.White),
                buttonBorder = color(string("triggerButtonBorder", "#3B82F6"), Color(0xFF3B82F6)),
                borderWidth = number("triggerButtonBorderWidth", 1f),
                borderRadius = number("triggerButtonBorderRadius", 10f),
                horizontalPadding = number("triggerButtonPaddingHorizontal", 16f),
                verticalPadding = number("triggerButtonPaddingVertical", 10f)
            )
        }

        private fun fontWeight(value: String): FontWeight = when (value) {
            "100" -> FontWeight.W100
            "200" -> FontWeight.W200
            "300" -> FontWeight.W300
            "400", "normal" -> FontWeight.W400
            "600" -> FontWeight.W600
            "700", "bold" -> FontWeight.W700
            "800" -> FontWeight.W800
            "900" -> FontWeight.W900
            else -> FontWeight.W500
        }

        private fun fontFamily(value: String): FontFamily = when (value.lowercase()) {
            "serif" -> FontFamily.Serif
            "monospace" -> FontFamily.Monospace
            "cursive" -> FontFamily.Cursive
            "sans-serif", "sans serif", "system default", "" -> FontFamily.SansSerif
            else -> FontFamily.Default
        }

        private fun color(value: String, fallback: Color): Color = try {
            val raw = value.removePrefix("#")
            when (raw.length) {
                6 -> Color(0xFF000000 or raw.toLong(16))
                8 -> Color((raw.takeLast(2) + raw.take(6)).toLong(16))
                else -> fallback
            }
        } catch (_: NumberFormatException) {
            fallback
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.map(key: String): Map<String, Any?> =
    this[key] as? Map<String, Any?> ?: emptyMap()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
