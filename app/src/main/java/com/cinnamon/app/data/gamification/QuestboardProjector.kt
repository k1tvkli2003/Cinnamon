package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.QuestInstanceEntity
import com.cinnamon.app.domain.gamification.CatalogLifecycleState
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import com.cinnamon.app.domain.gamification.QuestCadence
import java.util.Locale

/** Read-only UI projection. It never grants value or advances progress. */
internal data class AssignedQuestProjection(
    val instanceId: String,
    val definitionId: String,
    val cadence: String,
    val title: String,
    val reason: String,
    val progress: Int,
    val target: Int,
    val completed: Boolean,
    val claimable: Boolean,
    val claimed: Boolean,
    val rewardXp: Int
)

internal object QuestboardProjector {

    fun projectAssignedWeeklyQuests(
        bundle: GamificationCatalogBundle,
        instances: List<QuestInstanceEntity>,
        nowEpochMillis: Long
    ): List<AssignedQuestProjection> = projectAssignedQuests(
        bundle = bundle,
        instances = instances,
        nowEpochMillis = nowEpochMillis,
        cadence = QuestCadence.WEEKLY,
        visibleDefinitionIds = VISIBLE_WEEKLY_QUEST_IDS
    )

    fun projectAssignedDailyQuests(
        bundle: GamificationCatalogBundle,
        instances: List<QuestInstanceEntity>,
        nowEpochMillis: Long
    ): List<AssignedQuestProjection> = projectAssignedQuests(
        bundle = bundle,
        instances = instances,
        nowEpochMillis = nowEpochMillis,
        cadence = QuestCadence.DAILY,
        visibleDefinitionIds = VISIBLE_DAILY_QUEST_IDS
    )

    private fun projectAssignedQuests(
        bundle: GamificationCatalogBundle,
        instances: List<QuestInstanceEntity>,
        nowEpochMillis: Long,
        cadence: QuestCadence,
        visibleDefinitionIds: Set<String>
    ): List<AssignedQuestProjection> {
        val definitionsById = bundle.catalog.quests
            .asSequence()
            .filter { definition ->
                definition.lifecycle.state == CatalogLifecycleState.ACTIVE &&
                    definition.cadence == cadence &&
                    definition.id in visibleDefinitionIds
            }
            .associateBy { definition -> definition.id }

        return instances.asSequence()
            .filter { instance ->
                instance.cadence == cadence.name.lowercase(Locale.ROOT) &&
                    instance.definitionId in definitionsById &&
                    instance.isRelevantToQuestboard(nowEpochMillis)
            }
            .sortedWith(
                compareBy<QuestInstanceEntity> { instance ->
                    when (instance.state) {
                        "completed" -> 0
                        "in_progress" -> 1
                        "available" -> 2
                        "claimed" -> 3
                        else -> 4
                    }
                }.thenByDescending { instance -> instance.startsAtEpochMillis }
            )
            .mapNotNull { instance ->
                val definition = definitionsById.getValue(instance.definitionId)
                val claim = QuestAssignmentContracts.resolveClaim(instance)
                val title = bundle.copy.strings[definition.titleKey] ?: return@mapNotNull null
                val reason = bundle.copy.strings[definition.reasonKey] ?: return@mapNotNull null
                AssignedQuestProjection(
                    instanceId = instance.questInstanceId,
                    definitionId = instance.definitionId,
                    cadence = instance.cadence,
                    title = title,
                    reason = reason,
                    progress = instance.progress.coerceIn(0L, instance.target).toUiCount(),
                    target = instance.target.toUiCount(),
                    completed = instance.state == "completed" || instance.state == "claimed",
                    claimable = instance.state == "completed" && claim != null,
                    claimed = instance.state == "claimed",
                    rewardXp = claim?.xpAmount?.toUiCount() ?: 0
                )
            }
            .toList()
    }
}

private val VISIBLE_DAILY_QUEST_IDS = setOf("quest.daily.due_review")
private val VISIBLE_WEEKLY_QUEST_IDS = setOf("quest.weekly.durable_mastery")

private fun QuestInstanceEntity.isRelevantToQuestboard(nowEpochMillis: Long): Boolean = when (state) {
    "available", "in_progress" -> nowEpochMillis < endsAtEpochMillis
    "completed" -> true
    "claimed" -> nowEpochMillis < endsAtEpochMillis
    else -> false
}

private fun Long.toUiCount(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
