package com.example.streamingtext.ui.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.EmojiNature
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.EmojiTransportation
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class EmojiCategory(
    val name: String,
    val icon: ImageVector,
    val emojis: List<String>,
)

private val EmojiCategories = listOf(
    EmojiCategory(
        name = "Smileys",
        icon = Icons.Outlined.EmojiEmotions,
        emojis = listOf(
            "😀", "😃", "😄", "😁",
            "😆", "😅", "🤣", "😂",
            "🙂", "🙃", "😉", "😊",
            "😇", "😍", "🥰", "😘",
            "😗", "😙", "😚", "😋",
            "😛", "😜", "🤪", "😝",
            "🤑", "🤗", "🤭", "🤫",
            "🤔", "🤐", "🤨", "😐",
            "😑", "😶", "😏", "😒",
            "🙄", "😬", "🤥", "😌",
            "😔", "😪", "🤤", "😴",
            "😷", "🤒", "🤕", "🤢",
            "🤮", "🤧", "🥵", "🥶",
            "🥴", "😵", "🤯", "🤠",
            "😎", "🤓", "🧐", "😕",
            "😟", "🙁", "☹️", "😮",
            "😯", "😲", "😳", "😦",
            "😧", "😨", "😰", "😥",
            "😢", "😭", "😱", "😖",
            "😣", "😞", "😓", "😩",
            "😫", "😤", "😡", "😠",
            "🤬", "😈", "👿", "💀",
            "👹", "👺", "👻", "👽",
            "👾", "🤖", "💩",
            "👍", "👎", "👏", "🙌",
            "🙏", "🤝", "👋", "✌️",
            "🤞", "🤟", "🤘", "👌",
            "✋", "✊", "👊", "🤛",
            "🤜", "💪", "🧠", "👀",
            "👁️", "👄", "👅", "👂",
        ),
    ),
    EmojiCategory(
        name = "Animals",
        icon = Icons.Outlined.EmojiNature,
        emojis = listOf(
            "🐶", "🐱", "🐭", "🐹",
            "🐰", "🦊", "🐻", "🐼",
            "🐨", "🐯", "🦁", "🐮",
            "🐷", "🐸", "🐵", "🙈",
            "🙉", "🙊", "🐒", "🐔",
            "🐧", "🐦", "🐤", "🐣",
            "🦆", "🦅", "🦉", "🦋",
            "🐌", "🐛", "🐜", "🐝",
            "🐞", "🦗", "🕷️", "🦂",
            "🦀", "🦑", "🐙", "🐚",
            "🐊", "🐍", "🐢", "🦎",
            "🐲", "🐉", "🐳", "🐋",
            "🐬", "🐟", "🐠", "🐡",
            "🦈", "🐋", "🐬", "🐘",
            "🦏", "🦒", "🐃", "🐂",
            "🐄", "🐎", "🐕", "🐩",
            "🐈", "🐇", "🐑", "🐐",
            "🐓", "🦃", "🐥", "🐖",
            "🐗", "🐴", "🦄", "🐛",
            "🌱", "🌲", "🌳", "🌴",
            "🌵", "🍀", "🌸", "🌷",
            "🌹", "🌺", "🌻", "🌼",
            "🌾", "🌿", "☘️", "🍂",
            "🍃", "🍁",
        ),
    ),
    EmojiCategory(
        name = "Food",
        icon = Icons.Outlined.EmojiFoodBeverage,
        emojis = listOf(
            "🍏", "🍎", "🍐", "🍊",
            "🍋", "🍌", "🍉", "🍇",
            "🍓", "🍈", "🍒", "🍑",
            "🥭", "🍍", "🥥", "🥑",
            "🍅", "🍆", "🥒", "🥕",
            "🌽", "🌶️", "🥔", "🍠",
            "🥐", "🍞", "🥖", "🥨",
            "🥞", "🧀", "🍖", "🍗",
            "🥩", "🍔", "🍟", "🍕",
            "🌭", "🥪", "🌮", "🌯",
            "🥙", "🥚", "🍳", "🥘",
            "🍲", "🥣", "🥗", "🍿",
            "🥫", "🍱", "🍘", "🍙",
            "🍚", "🍛", "🍜", "🍝",
            "🍠", "🍢", "🍣", "🍤",
            "🍥", "🥮", "🍡", "🍧",
            "🍨", "🍦", "🍰", "🎂",
            "🍮", "🍭", "🍬", "🍫",
            "🍪", "🍩", "🍯", "🍼",
            "🥛", "☕", "🍵", "🍶",
            "🍾", "🍷", "🍸", "🍹",
            "🍺", "🍻", "🥂", "🥃",
            "🥄", "🍽️",
        ),
    ),
    EmojiCategory(
        name = "Travel",
        icon = Icons.Outlined.EmojiTransportation,
        emojis = listOf(
            "🌍", "🌎", "🌏", "🌐",
            "🗺️", "🗾", "🏔️", "⛰️",
            "🌋", "🗻", "🏕️", "🏖️",
            "🏜️", "🏝️", "🏞️", "🏟️",
            "🏛️", "🏗️", "🏘️", "🏠",
            "🏡", "🏢", "🏣", "🏤",
            "🏥", "🏦", "🏨", "🏩",
            "🏪", "🏫", "🏬", "🏭",
            "🏯", "🏰", "💒", "🗼",
            "🗽", "⛪", "🕌", "🕍",
            "🕋", "⛲", "⛺", "🌁",
            "🌃", "🏙️", "🌄", "🌅",
            "🌆", "🌇", "🌉", "🐟",
            "🚂", "🚃", "🚄", "🚅",
            "🚆", "🚇", "🚈", "🚉",
            "🚊", "🚝", "🚞", "🚋",
            "🚌", "🚍", "🚎", "🚐",
            "🚑", "🚒", "🚓", "🚔",
            "🚕", "🚖", "🚗", "🚘",
            "🚙", "🚚", "🚛", "🚜",
            "🏎️", "🏍️", "🛵", "🚲",
            "🛴", "🛹", "🚏", "🛣️",
            "🛤️", "⛽", "🚨", "🚥",
            "🚦", "🛑", "⚓", "⛵",
            "🛶", "🚤", "🛳️", "⛴️",
            "🛥️", "🚢", "✈️", "🛩️",
            "🛫", "🛬", "🪂", "💺",
            "🚁", "🚟", "🎠", "🎡",
            "🎢", "💈", "🎪",
        ),
    ),
    EmojiCategory(
        name = "Objects",
        icon = Icons.Outlined.EmojiObjects,
        emojis = listOf(
            "⌚", "📱", "📲", "💻",
            "⌨️", "🖥️", "🖨️", "🖱️",
            "🖲️", "🕹️", "🗜️", "💽",
            "💾", "💿", "📀", "📼",
            "📷", "📸", "📹", "🎥",
            "📽️", "🎞️", "📞", "☎️",
            "📟", "📠", "📺", "📻",
            "🎙️", "🎚️", "🎛️", "⏱️",
            "⏲️", "⏰", "🕰️", "⌛",
            "⏳", "📡", "🔋", "🔌",
            "💡", "🔦", "🕯️", "🗑️",
            "🛒", "🎁", "🎈", "🎏",
            "🎀", "🎊", "🎉", "🎎",
            "🏮", "🎐", "✉️", "📩",
            "📨", "📧", "💌", "📥",
            "📤", "📦", "🏷️", "📪",
            "📫", "📬", "📭", "📮",
            "📯", "📜", "📃", "📄",
            "📑", "📊", "📈", "📉",
            "🗒️", "🗓️", "📆", "📅",
            "📇", "🗃️", "🗳️", "🗄️",
            "📋", "📁", "📂", "🗂️",
            "🗞️", "📰", "📓", "📔",
            "📒", "📕", "📗", "📘",
            "📙", "📚", "📖", "🔖",
            "🔗", "📎", "🖇️", "📐",
            "📏", "📌", "📍", "✂️",
            "🖊️", "🖋️", "✒️", "🖌️",
            "🖍️", "📝", "✏️", "🔍",
            "🔎", "🔏", "🔐", "🔒",
            "🔓",
        ),
    ),
    EmojiCategory(
        name = "Symbols",
        icon = Icons.Outlined.EmojiSymbols,
        emojis = listOf(
            "❤️", "🧡", "💛", "💚",
            "💙", "💜", "🖤", "🤍",
            "🤎", "💔", "❣️", "💕",
            "💞", "💓", "💗", "💖",
            "💘", "💝", "💟", "☮️",
            "✝️", "☪️", "🕉️", "☸️",
            "✡️", "🔯", "🕎", "☯️",
            "☦️", "🛐", "⛎", "♈",
            "♉", "♊", "♋", "♌",
            "♍", "♎", "♏", "♐",
            "♑", "♒", "♓", "🆔",
            "⚛️", "🌀", "🔅", "🔆",
            "📴", "📳", "♀️", "♂️",
            "⚕️", "♾️", "♻️", "⚜️",
            "🔰", "🔱", "⭕", "✅",
            "☑️", "✔️", "✖️", "❌",
            "❎", "➕", "➖", "➗",
            "➰", "➿", "〽️", "✳️",
            "✴️", "❇️", "‼️", "⁉️",
            "❓", "❔", "❕", "❗",
            "〰️", "©️", "®️", "™️",
        ),
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPanel(
    onEmojiClick: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableStateOf(0) }
    val current = EmojiCategories[selectedIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // The panel itself extends to the screen edge (matching the keyboard's
            // footprint), but its interactive content needs to sit above the gesture pill.
            .navigationBarsPadding(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
        ) {
            items(current.emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onEmojiClick(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emoji,
                        style = TextStyle(fontSize = 26.sp, fontFamily = FontFamily.Default),
                    )
                }
            }
        }

        CategoryStrip(
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it },
            onBackspace = onBackspace,
        )
    }
}

@Composable
private fun CategoryStrip(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onBackspace: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        EmojiCategories.forEachIndexed { index, category ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.name,
                    tint = if (index == selectedIndex)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onBackspace() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "⌫",






                style = TextStyle(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}