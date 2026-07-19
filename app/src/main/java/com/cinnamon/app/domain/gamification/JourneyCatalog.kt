package com.cinnamon.app.domain.gamification

/** Evidence sources supported by immutable Room queries. */
enum class JourneyEvidenceMetric(val wireName: String) {
    DISTINCT_REVIEWED_ITEMS("distinct_reviewed_items"),
    DISTINCT_PRACTICE_CONTENT_KINDS("distinct_practice_content_kinds"),
    MASTERED_ITEMS_AFTER_DELAY("mastered_items_after_delay"),
    ACTIVE_STUDY_DAYS("active_study_days")
}

/** The product destination that can help the learner progress a stage. */
enum class JourneyDestination {
    REVIEW,
    PRACTICE,
    JOURNEY
}

data class JourneyStageDefinition(
    val id: String,
    val order: Int,
    val title: String,
    val description: String,
    val evidenceMetric: JourneyEvidenceMetric,
    val target: Long,
    val rewardXp: Long,
    val destination: JourneyDestination,
    val actionLabel: String
) {
    init {
        require(id.isNotBlank()) { "Journey stage id must not be blank" }
        require(order > 0) { "Journey stage order must be positive" }
        require(title.isNotBlank() && description.isNotBlank()) {
            "Journey stage copy must be complete"
        }
        require(target > 0L) { "Journey stage target must be positive" }
        require(rewardXp > 0L) { "Journey stage reward must be positive" }
        require(actionLabel.isNotBlank()) { "Journey stage action must not be blank" }
    }
}

data class JourneyDefinition(
    val id: String,
    val version: Int,
    val eyebrow: String,
    val title: String,
    val description: String,
    val completionTitle: String,
    val completionDescription: String,
    val stages: List<JourneyStageDefinition>
) {
    init {
        require(id.isNotBlank()) { "Journey id must not be blank" }
        require(version > 0) { "Journey version must be positive" }
        require(stages.isNotEmpty()) { "Journey must contain at least one stage" }
        require(stages.map(JourneyStageDefinition::id).distinct().size == stages.size) {
            "Journey stage ids must be unique"
        }
        require(stages.map(JourneyStageDefinition::order) == (1..stages.size).toList()) {
            "Journey stage order must be contiguous and start at one"
        }
    }
}

/**
 * Identity-neutral campaign content. Visual identity can wrap this contract later without
 * rewriting progress, rewards, or migration history.
 */
object FoundationJourneyCatalog {
    val definition: JourneyDefinition = JourneyDefinition(
        id = "journey.foundation.expedition",
        version = 1,
        eyebrow = "YOUR LONG-RANGE JOURNEY",
        title = "Foundation Expedition",
        description = "Four connected chapters turn daily practice into a route you can finish—and keep.",
        completionTitle = "Expedition complete",
        completionDescription = "Your first learning route is secured. Every chapter remains in your record.",
        stages = listOf(
            JourneyStageDefinition(
                id = "stage.memory-spark",
                order = 1,
                title = "Spark the memory trail",
                description = "Review three different terms to light the first marker. Repeat reviews stay useful; this chapter advances with new terms.",
                evidenceMetric = JourneyEvidenceMetric.DISTINCT_REVIEWED_ITEMS,
                target = 3L,
                rewardXp = 10L,
                destination = JourneyDestination.REVIEW,
                actionLabel = "Start reviews"
            ),
            JourneyStageDefinition(
                id = "stage.cross-train",
                order = 2,
                title = "Cross-train your recall",
                description = "Complete two different practice formats so recall works beyond one familiar game.",
                evidenceMetric = JourneyEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS,
                target = 2L,
                rewardXp = 20L,
                destination = JourneyDestination.PRACTICE,
                actionLabel = "Explore practice"
            ),
            JourneyStageDefinition(
                id = "stage.first-mastery",
                order = 3,
                title = "Lock in a lasting mastery",
                description = "Bring one term through a later review until lasting mastery is confirmed.",
                evidenceMetric = JourneyEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY,
                target = 1L,
                rewardXp = 20L,
                destination = JourneyDestination.REVIEW,
                actionLabel = "Continue reviews"
            ),
            JourneyStageDefinition(
                id = "stage.rhythm",
                order = 4,
                title = "Build a three-day rhythm",
                description = "Finish a meaningful learning activity on three different days and close the expedition with momentum.",
                evidenceMetric = JourneyEvidenceMetric.ACTIVE_STUDY_DAYS,
                target = 3L,
                rewardXp = 30L,
                destination = JourneyDestination.PRACTICE,
                actionLabel = "Keep the rhythm"
            )
        )
    )
}
