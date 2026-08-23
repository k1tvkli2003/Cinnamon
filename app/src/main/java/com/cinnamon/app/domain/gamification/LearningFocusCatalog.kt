package com.cinnamon.app.domain.gamification

enum class LearningFocusTone {
    LANGUAGE_PRECISION,
    RECALL_RANGE
}

data class LearningFocusOption(
    val id: String,
    val title: String,
    val tagline: String,
    val metricSummary: String,
    val tone: LearningFocusTone,
    val milestonePlan: JourneyDefinition
) {
    init {
        require(id.startsWith("learning-focus.option.")) {
            "Learning Focus option ids must use the canonical option namespace"
        }
        require(title.isNotBlank() && tagline.isNotBlank() && metricSummary.isNotBlank()) {
            "Learning Focus option copy must be complete"
        }
        require(milestonePlan.id.startsWith("milestone-plan.learning-focus.")) {
            "Learning Focus milestone plans must use the canonical plan namespace"
        }
    }

    val totalRewardXp: Long
        get() = milestonePlan.stages.sumOf(JourneyStageDefinition::rewardXp)
}

data class LearningFocusDefinition(
    val id: String,
    val version: Int,
    val eyebrow: String,
    val title: String,
    val description: String,
    val prerequisiteDefinitionId: String,
    val prerequisiteDefinitionVersion: Int,
    val prerequisiteMilestoneDefinitionId: String,
    val options: List<LearningFocusOption>
) {
    init {
        require(id.startsWith("learning-focus.definition.")) {
            "Learning Focus definition ids must use the canonical definition namespace"
        }
        require(version > 0) { "Learning Focus version must be positive" }
        require(eyebrow.isNotBlank() && title.isNotBlank() && description.isNotBlank()) {
            "Learning Focus choice copy must be complete"
        }
        require(prerequisiteDefinitionId.isNotBlank()) {
            "Learning Focus prerequisite definition must not be blank"
        }
        require(prerequisiteDefinitionVersion > 0) {
            "Learning Focus prerequisite version must be positive"
        }
        require(prerequisiteMilestoneDefinitionId.isNotBlank()) {
            "Learning Focus prerequisite milestone must not be blank"
        }
        require(options.size >= 2) { "A Learning Focus choice needs at least two options" }
        require(options.map(LearningFocusOption::id).distinct().size == options.size) {
            "Learning Focus option ids must be unique"
        }
        require(options.map { it.milestonePlan.id }.distinct().size == options.size) {
            "Learning Focus milestone-plan ids must be unique"
        }
    }

    fun option(optionId: String): LearningFocusOption? = options.firstOrNull { it.id == optionId }
}

object FoundationLearningFocusCatalog {
    private val languagePrecisionPlan = JourneyDefinition(
        id = "milestone-plan.learning-focus.language-precision",
        version = 1,
        eyebrow = "LANGUAGE PRECISION",
        title = "Language Precision",
        description = "Strengthen control across practice formats and confirm recall after a delay.",
        completionTitle = "Language Precision complete",
        completionDescription = "You completed all three milestones and earned 110 XP.",
        stages = listOf(
            JourneyStageDefinition(
                id = "milestone.language-precision.practice-kinds",
                order = 1,
                title = "Use three practice formats",
                description = "Complete practice in three distinct formats. " +
                    "Repeating one format still helps, but counts once here.",
                evidenceMetric = JourneyEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS,
                target = 3L,
                rewardXp = 25L,
                destination = JourneyDestination.PRACTICE,
                actionLabel = "Explore practice"
            ),
            JourneyStageDefinition(
                id = "milestone.language-precision.delayed-mastery",
                order = 2,
                title = "Confirm two items after a delay",
                description = "Confirm delayed recall for two distinct items through later reviews, " +
                    "not in the same sitting.",
                evidenceMetric = JourneyEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY,
                target = 2L,
                rewardXp = 35L,
                destination = JourneyDestination.REVIEW,
                actionLabel = "Continue reviews"
            ),
            JourneyStageDefinition(
                id = "milestone.language-precision.active-days",
                order = 3,
                title = "Learn on five different days",
                description = "Record meaningful learning activity on five distinct days after choosing this focus.",
                evidenceMetric = JourneyEvidenceMetric.ACTIVE_STUDY_DAYS,
                target = 5L,
                rewardXp = 50L,
                destination = JourneyDestination.PRACTICE,
                actionLabel = "Keep the rhythm"
            )
        )
    )

    private val recallRangePlan = JourneyDefinition(
        id = "milestone-plan.learning-focus.recall-range",
        version = 1,
        eyebrow = "RECALL RANGE",
        title = "Recall Range",
        description = "Broaden active recall and carry it across more practice formats.",
        completionTitle = "Recall Range complete",
        completionDescription = "You completed all three milestones and earned 110 XP.",
        stages = listOf(
            JourneyStageDefinition(
                id = "milestone.recall-range.reviewed-items",
                order = 1,
                title = "Review eight different items",
                description = "Review eight distinct items. Repeated reviews still help, " +
                    "but each item counts once here.",
                evidenceMetric = JourneyEvidenceMetric.DISTINCT_REVIEWED_ITEMS,
                target = 8L,
                rewardXp = 25L,
                destination = JourneyDestination.REVIEW,
                actionLabel = "Start reviews"
            ),
            JourneyStageDefinition(
                id = "milestone.recall-range.practice-kinds",
                order = 2,
                title = "Use four practice formats",
                description = "Complete practice in four distinct formats so recall is not tied " +
                    "to one familiar activity.",
                evidenceMetric = JourneyEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS,
                target = 4L,
                rewardXp = 35L,
                destination = JourneyDestination.PRACTICE,
                actionLabel = "Explore practice"
            ),
            JourneyStageDefinition(
                id = "milestone.recall-range.active-days",
                order = 3,
                title = "Learn on five different days",
                description = "Record meaningful learning activity on five distinct days after choosing this focus.",
                evidenceMetric = JourneyEvidenceMetric.ACTIVE_STUDY_DAYS,
                target = 5L,
                rewardXp = 50L,
                destination = JourneyDestination.PRACTICE,
                actionLabel = "Keep the rhythm"
            )
        )
    )

    val definition: LearningFocusDefinition = LearningFocusDefinition(
        id = "learning-focus.definition.foundation",
        version = 1,
        eyebrow = "LEARNING FOCUS",
        title = "Choose what to sharpen",
        description = "Choose the language skill pattern you want to strengthen. " +
            "Your focus advances only through recorded practice and later recall.",
        prerequisiteDefinitionId = FoundationJourneyCatalog.definition.id,
        prerequisiteDefinitionVersion = FoundationJourneyCatalog.definition.version,
        prerequisiteMilestoneDefinitionId = "stage.memory-spark",
        options = listOf(
            LearningFocusOption(
                id = "learning-focus.option.language-precision",
                title = "Language Precision",
                tagline = "Strengthen control across practice formats and delayed review.",
                metricSummary = "3 milestones · 110 XP · 3 practice formats · " +
                    "2 items recalled after a delay · 5 active days",
                tone = LearningFocusTone.LANGUAGE_PRECISION,
                milestonePlan = languagePrecisionPlan
            ),
            LearningFocusOption(
                id = "learning-focus.option.recall-range",
                title = "Recall Range",
                tagline = "Broaden active recall across more items and practice formats.",
                metricSummary = "3 milestones · 110 XP · 8 reviewed items · 4 practice formats · 5 active days",
                tone = LearningFocusTone.RECALL_RANGE,
                milestonePlan = recallRangePlan
            )
        )
    )
}
