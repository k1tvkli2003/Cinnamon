package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.QuestInstanceEntity
import com.cinnamon.app.domain.gamification.ClaimBehavior
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import com.cinnamon.app.domain.gamification.PresentationTier
import com.cinnamon.app.domain.gamification.QuestDefinition
import com.cinnamon.app.domain.gamification.RewardType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.Locale

/**
 * Immutable assignment-time contracts persisted with a quest instance.
 *
 * Quest definitions may evolve after an app update. These snapshots keep the evidence window,
 * assignment time zone, economic value, and presentation policy of an already-assigned quest
 * stable without requiring a new Room schema column.
 */
@JsonClass(generateAdapter = true)
internal data class QuestCriteriaSnapshot(
    val schemaVersion: Int = QUEST_ASSIGNMENT_SCHEMA_VERSION,
    val metric: String,
    val aggregation: String,
    val distinctBy: String,
    @param:Json(name = "timezone") val assignmentTimeZoneId: String,
    val evidenceStartsAtEpochMillis: Long,
    val evidenceEndsAtEpochMillis: Long
)

@JsonClass(generateAdapter = true)
internal data class QuestRewardGrantSnapshot(
    val rewardId: String,
    val type: String,
    val amount: Long
)

@JsonClass(generateAdapter = true)
internal data class QuestRewardSnapshot(
    val schemaVersion: Int = QUEST_ASSIGNMENT_SCHEMA_VERSION,
    val catalogStorageVersion: Int,
    val definitionId: String,
    val claimBehavior: String,
    val presentationId: String,
    val presentationTier: String,
    val presentationPriority: Int,
    val presentationExpiresAfterSeconds: Int,
    val grants: List<QuestRewardGrantSnapshot>
)

internal data class ResolvedQuestClaim(
    val xpAmount: Long,
    val presentationId: String,
    val presentationTier: String,
    val presentationPriority: Int,
    val presentationExpiresAfterSeconds: Int
)

internal object QuestAssignmentContracts {
    private val moshi = Moshi.Builder().build()
    private val criteriaAdapter = moshi.adapter(QuestCriteriaSnapshot::class.java)
    private val rewardAdapter = moshi.adapter(QuestRewardSnapshot::class.java)
    private val legacyRewardRefsAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    fun encodeCriteria(snapshot: QuestCriteriaSnapshot): String = criteriaAdapter.toJson(snapshot)

    fun decodeCriteria(json: String): QuestCriteriaSnapshot? = runCatching {
        criteriaAdapter.fromJson(json)
    }.getOrNull()?.takeIf(::isValidCriteria)

    fun createRewardSnapshot(
        bundle: GamificationCatalogBundle,
        definition: QuestDefinition
    ): QuestRewardSnapshot {
        check(definition.claimBehavior == ClaimBehavior.MANUAL_ONCE) {
            "Only manually claimable quests can persist a manual reward snapshot"
        }
        val rewardsById = bundle.catalog.rewards.associateBy { reward -> reward.id }
        val grants = definition.rewardRefs.map { rewardId ->
            val reward = checkNotNull(rewardsById[rewardId]) {
                "Validated catalog is missing quest reward $rewardId"
            }
            check(reward.type == RewardType.XP && reward.amount != null && reward.amount > 0) {
                "Quest reward $rewardId has no supported immutable claim contract"
            }
            QuestRewardGrantSnapshot(
                rewardId = reward.id,
                type = reward.type.name.lowercase(Locale.ROOT),
                amount = reward.amount.toLong()
            )
        }
        check(grants.isNotEmpty()) { "A claimable quest must persist at least one reward grant" }
        val presentation = checkNotNull(
            bundle.catalog.presentations.firstOrNull { item -> item.id == definition.presentationId }
        ) { "Validated catalog is missing quest presentation ${definition.presentationId}" }

        return QuestRewardSnapshot(
            catalogStorageVersion = bundle.catalog.storageVersion,
            definitionId = definition.id,
            claimBehavior = definition.claimBehavior.name.lowercase(Locale.ROOT),
            presentationId = presentation.id,
            presentationTier = presentation.tier.name.lowercase(Locale.ROOT),
            presentationPriority = presentation.tier.priority(),
            presentationExpiresAfterSeconds = presentation.expiresAfterSeconds,
            grants = grants
        )
    }

    fun encodeReward(snapshot: QuestRewardSnapshot): String = rewardAdapter.toJson(snapshot)

    /** Resolves only assignment-time value. Malformed or unknown snapshots fail closed. */
    fun resolveClaim(instance: QuestInstanceEntity): ResolvedQuestClaim? {
        val currentSnapshot = runCatching { rewardAdapter.fromJson(instance.rewardJson) }.getOrNull()
        if (currentSnapshot != null) {
            return currentSnapshot.toResolvedClaim(instance)
        }
        return resolveLegacyV1Claim(instance)
    }

    private fun isValidCriteria(snapshot: QuestCriteriaSnapshot): Boolean =
        snapshot.schemaVersion == QUEST_ASSIGNMENT_SCHEMA_VERSION &&
            snapshot.metric.isNotBlank() &&
            snapshot.aggregation.isNotBlank() &&
            snapshot.distinctBy.isNotBlank() &&
            snapshot.assignmentTimeZoneId.isNotBlank() &&
            snapshot.evidenceStartsAtEpochMillis >= 0L &&
            snapshot.evidenceStartsAtEpochMillis < snapshot.evidenceEndsAtEpochMillis

    private fun QuestRewardSnapshot.toResolvedClaim(
        instance: QuestInstanceEntity
    ): ResolvedQuestClaim? = runCatching {
        check(schemaVersion == QUEST_ASSIGNMENT_SCHEMA_VERSION)
        check(catalogStorageVersion == instance.catalogVersion)
        check(definitionId == instance.definitionId)
        check(claimBehavior == ClaimBehavior.MANUAL_ONCE.name.lowercase(Locale.ROOT))
        check(presentationId.isNotBlank())
        check(presentationExpiresAfterSeconds > 0)
        check(presentationPriority == presentationTier.expectedPriority())
        check(grants.isNotEmpty())
        check(grants.map { grant -> grant.rewardId }.distinct().size == grants.size)

        val xp = grants.fold(0L) { total, grant ->
            check(grant.rewardId.isNotBlank())
            check(grant.type == RewardType.XP.name.lowercase(Locale.ROOT))
            check(grant.amount > 0L)
            Math.addExact(total, grant.amount)
        }
        check(xp > 0L)
        ResolvedQuestClaim(
            xpAmount = xp,
            presentationId = presentationId,
            presentationTier = presentationTier,
            presentationPriority = presentationPriority,
            presentationExpiresAfterSeconds = presentationExpiresAfterSeconds
        )
    }.getOrNull()

    /**
     * Compatibility for instances created before object snapshots were introduced. Values are
     * pinned to storage version 1 instead of being looked up from today's mutable catalog.
     */
    private fun resolveLegacyV1Claim(instance: QuestInstanceEntity): ResolvedQuestClaim? = runCatching {
        check(instance.catalogVersion == 1)
        check(instance.definitionId in LEGACY_V1_MANUAL_QUEST_IDS)
        val rewardIds = checkNotNull(legacyRewardRefsAdapter.fromJson(instance.rewardJson))
        check(rewardIds.isNotEmpty() && rewardIds.distinct().size == rewardIds.size)
        val xp = rewardIds.fold(0L) { total, rewardId ->
            Math.addExact(total, checkNotNull(LEGACY_V1_XP_BY_REWARD_ID[rewardId]))
        }
        val comeback = instance.definitionId == "quest.comeback.restart_small"
        ResolvedQuestClaim(
            xpAmount = xp,
            presentationId = if (comeback) {
                "presentation.comeback.gentle"
            } else {
                "presentation.quest.complete"
            },
            presentationTier = if (comeback) "milestone" else "standard",
            presentationPriority = if (comeback) 30 else 20,
            presentationExpiresAfterSeconds = 604_800
        )
    }.getOrNull()
}

private const val QUEST_ASSIGNMENT_SCHEMA_VERSION = 1

private val LEGACY_V1_XP_BY_REWARD_ID = mapOf(
    "reward.xp.10" to 10L,
    "reward.xp.20" to 20L,
    "reward.xp.30" to 30L,
    "reward.xp.40" to 40L,
    "reward.xp.50" to 50L,
    "reward.xp.75" to 75L,
    "reward.xp.100" to 100L
)

private val LEGACY_V1_MANUAL_QUEST_IDS = setOf(
    "quest.daily.due_review",
    "quest.daily.repair_one",
    "quest.daily.context_use",
    "quest.daily.confusable_pair",
    "quest.weekly.rhythm_three_days",
    "quest.weekly.durable_mastery",
    "quest.weekly.mixed_practice",
    "quest.weekly.queue_relief",
    "quest.weekend.focused_review",
    "quest.comeback.restart_small",
    "quest.monthly.correction_craft",
    "quest.monthly.durable_growth"
)

private fun String.expectedPriority(): Int = when (this) {
    "micro" -> 10
    "standard" -> 20
    "milestone" -> 30
    "showpiece" -> 40
    else -> -1
}

private fun PresentationTier.priority(): Int = when (this) {
    PresentationTier.MICRO -> 10
    PresentationTier.STANDARD -> 20
    PresentationTier.MILESTONE -> 30
    PresentationTier.SHOWPIECE -> 40
}
