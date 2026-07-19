package com.cinnamon.app.domain.gamification

enum class CampaignRouteTone {
    PRECISION,
    MOMENTUM
}

data class CampaignRouteDefinition(
    val id: String,
    val title: String,
    val tagline: String,
    val commitmentCopy: String,
    val tone: CampaignRouteTone,
    val journey: JourneyDefinition
) {
    init {
        require(id.isNotBlank()) { "Campaign route id must not be blank" }
        require(title.isNotBlank() && tagline.isNotBlank() && commitmentCopy.isNotBlank()) {
            "Campaign route copy must be complete"
        }
        require(journey.id.startsWith("journey.route.")) {
            "Campaign route journeys must use the route journey namespace"
        }
    }

    val totalRewardXp: Long
        get() = journey.stages.sumOf(JourneyStageDefinition::rewardXp)
}

data class CampaignDefinition(
    val id: String,
    val version: Int,
    val eyebrow: String,
    val title: String,
    val description: String,
    val prerequisiteJourneyDefinitionId: String,
    val prerequisiteJourneyDefinitionVersion: Int,
    val prerequisiteStageDefinitionId: String,
    val routes: List<CampaignRouteDefinition>
) {
    init {
        require(id.isNotBlank()) { "Campaign id must not be blank" }
        require(version > 0) { "Campaign version must be positive" }
        require(eyebrow.isNotBlank() && title.isNotBlank() && description.isNotBlank()) {
            "Campaign choice copy must be complete"
        }
        require(prerequisiteJourneyDefinitionId.isNotBlank()) {
            "Campaign prerequisite journey must not be blank"
        }
        require(prerequisiteJourneyDefinitionVersion > 0) {
            "Campaign prerequisite journey version must be positive"
        }
        require(prerequisiteStageDefinitionId.isNotBlank()) {
            "Campaign prerequisite stage must not be blank"
        }
        require(routes.size >= 2) { "A campaign choice needs at least two routes" }
        require(routes.map(CampaignRouteDefinition::id).distinct().size == routes.size) {
            "Campaign route ids must be unique"
        }
        require(routes.map { it.journey.id }.distinct().size == routes.size) {
            "Campaign route journey ids must be unique"
        }
    }

    fun route(routeId: String): CampaignRouteDefinition? = routes.firstOrNull { it.id == routeId }
}

/**
 * Identity-neutral route content. The chosen visual identity can later replace framing and art
 * without changing route IDs, evidence thresholds, saved choice, or reward history.
 */
object FoundationCampaignCatalog {
    private val precisionJourney = JourneyDefinition(
        id = "journey.route.precision-trail",
        version = 1,
        eyebrow = "YOUR COMMITTED ROUTE",
        title = "Precision Trail",
        description = "Change the rules, prove recall later, and turn careful practice into dependable control.",
        completionTitle = "Precision Trail secured",
        completionDescription = "Every checkpoint is preserved in your learning record.",
        stages = listOf(
            JourneyStageDefinition(
                id = "stage.switch-the-rules",
                order = 1,
                title = "Switch the rules",
                description = "Finish three different practice formats. This chapter advances only when all three formats are verified.",
                evidenceMetric = JourneyEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS,
                target = 3L,
                rewardXp = 25L,
                destination = JourneyDestination.PRACTICE,
                actionLabel = "Explore formats"
            ),
            JourneyStageDefinition(
                id = "stage.prove-it-later",
                order = 2,
                title = "Prove it later",
                description = "Bring two terms to lasting mastery in later reviews—not in the same sitting.",
                evidenceMetric = JourneyEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY,
                target = 2L,
                rewardXp = 35L,
                destination = JourneyDestination.REVIEW,
                actionLabel = "Continue reviews"
            ),
            JourneyStageDefinition(
                id = "stage.hold-the-line",
                order = 3,
                title = "Hold the line",
                description = "Complete meaningful learning on five different days to secure the Precision Trail.",
                evidenceMetric = JourneyEvidenceMetric.ACTIVE_STUDY_DAYS,
                target = 5L,
                rewardXp = 50L,
                destination = JourneyDestination.PRACTICE,
                actionLabel = "Keep the rhythm"
            )
        )
    )

    private val momentumJourney = JourneyDefinition(
        id = "journey.route.momentum-circuit",
        version = 1,
        eyebrow = "YOUR COMMITTED ROUTE",
        title = "Momentum Circuit",
        description = "Widen recall, carry meaning across formats, and make the work hold across days.",
        completionTitle = "Momentum Circuit secured",
        completionDescription = "Your route is complete, and every verified chapter remains in your record.",
        stages = listOf(
            JourneyStageDefinition(
                id = "stage.widen-recall",
                order = 1,
                title = "Widen the recall lane",
                description = "Review eight different terms so recall is not tied to one familiar prompt.",
                evidenceMetric = JourneyEvidenceMetric.DISTINCT_REVIEWED_ITEMS,
                target = 8L,
                rewardXp = 25L,
                destination = JourneyDestination.REVIEW,
                actionLabel = "Start reviews"
            ),
            JourneyStageDefinition(
                id = "stage.carry-meaning",
                order = 2,
                title = "Carry meaning across formats",
                description = "Finish four different practice formats while keeping the same knowledge in play.",
                evidenceMetric = JourneyEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS,
                target = 4L,
                rewardXp = 35L,
                destination = JourneyDestination.PRACTICE,
                actionLabel = "Explore formats"
            ),
            JourneyStageDefinition(
                id = "stage.make-momentum-stick",
                order = 3,
                title = "Make momentum stick",
                description = "Complete meaningful learning on five different days to secure the Momentum Circuit.",
                evidenceMetric = JourneyEvidenceMetric.ACTIVE_STUDY_DAYS,
                target = 5L,
                rewardXp = 50L,
                destination = JourneyDestination.PRACTICE,
                actionLabel = "Keep the rhythm"
            )
        )
    )

    val definition: CampaignDefinition = CampaignDefinition(
        id = "campaign.foundation-route-choice",
        version = 1,
        eyebrow = "CHOOSE YOUR TRAINING ROUTE",
        title = "The next expedition is yours",
        description = "Commit to one route. Your choice is saved; every chapter moves only on verified learning evidence.",
        prerequisiteJourneyDefinitionId = FoundationJourneyCatalog.definition.id,
        prerequisiteJourneyDefinitionVersion = FoundationJourneyCatalog.definition.version,
        prerequisiteStageDefinitionId = "stage.memory-spark",
        routes = listOf(
            CampaignRouteDefinition(
                id = "route.precision-trail",
                title = "Precision Trail",
                tagline = "Make careful recall survive new formats and later reviews.",
                commitmentCopy = "Three chapters · 110 XP · practice breadth and delayed mastery",
                tone = CampaignRouteTone.PRECISION,
                journey = precisionJourney
            ),
            CampaignRouteDefinition(
                id = "route.momentum-circuit",
                title = "Momentum Circuit",
                tagline = "Build wider recall, carry it across formats, and make it hold across days.",
                commitmentCopy = "Three chapters · 110 XP · recall breadth and cross-format rhythm",
                tone = CampaignRouteTone.MOMENTUM,
                journey = momentumJourney
            )
        )
    )
}
