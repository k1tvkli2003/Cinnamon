package com.cinnamon.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinnamon.app.data.gamification.CelebrationTier
import com.cinnamon.app.data.gamification.PendingRewardPresentation

/**
 * A durable receipt gets one calm, user-dismissed acknowledgement. The card is
 * intentionally static: product meaning stays available to reduced-motion and
 * screen-reader users, while the persisted reward record remains authoritative.
 */
@Composable
fun RewardPresentationHost(
    receipt: PendingRewardPresentation,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val copy = rewardPresentationCopy(receipt)
    val isMilestone = receipt.tier in setOf(
        CelebrationTier.MILESTONE.wireName,
        CelebrationTier.SHOWPIECE.wireName
    )
    val containerColor = if (isMilestone) scheme.tertiaryContainer else scheme.secondaryContainer
    val contentColor = if (isMilestone) scheme.onTertiaryContainer else scheme.onSecondaryContainer
    val icon: ImageVector = if (isMilestone) Icons.Filled.WorkspacePremium else Icons.Filled.AutoAwesome

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.12f),
                contentColor = contentColor
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.detail,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = copy.dismissLabel
                )
            }
        }
    }
}

internal data class RewardPresentationCopy(
    val title: String,
    val detail: String,
    val dismissLabel: String
)

/** Copy is deliberately proportional to a completed, persisted outcome. */
internal fun rewardPresentationCopy(xpAwarded: Long): RewardPresentationCopy =
    if (xpAwarded > 0L) {
        RewardPresentationCopy(
            title = "Progress saved",
            detail = "+$xpAwarded XP earned from this completed activity.",
            dismissLabel = "Dismiss reward notice"
        )
    } else {
        RewardPresentationCopy(
            title = "Practice saved",
            detail = "Your completed activity is saved.",
            dismissLabel = "Dismiss reward notice"
        )
    }

internal fun rewardPresentationCopy(receipt: PendingRewardPresentation): RewardPresentationCopy = when {
    receipt.presentationFamily.startsWith("presentation.journey.") -> RewardPresentationCopy(
        title = "Milestone complete",
        detail = "+${receipt.xpAwarded} XP earned. This Foundation milestone is now complete.",
        dismissLabel = "Dismiss milestone reward"
    )
    receipt.presentationFamily.startsWith("presentation.quest.") -> RewardPresentationCopy(
        title = "Quest complete",
        detail = "+${receipt.xpAwarded} XP earned from this checkpoint.",
        dismissLabel = "Dismiss quest reward"
    )
    receipt.presentationFamily.startsWith("presentation.achievement.") -> RewardPresentationCopy(
        title = "Achievement unlocked",
        detail = if (receipt.xpAwarded > 0L) {
            "+${receipt.xpAwarded} XP earned. This achievement stays in your collection."
        } else {
            "Your completed learning activity unlocked this achievement."
        },
        dismissLabel = "Dismiss achievement notice"
    )
    else -> rewardPresentationCopy(receipt.xpAwarded)
}
