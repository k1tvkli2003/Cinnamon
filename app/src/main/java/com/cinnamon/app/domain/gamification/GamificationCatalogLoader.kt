package com.cinnamon.app.domain.gamification

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import java.io.IOException
import java.io.InputStream

class GamificationCatalogLoader(
    moshi: Moshi = Moshi.Builder().build()
) {
    private val catalogAdapter = moshi.adapter(GamificationCatalog::class.java).failOnUnknown()
    private val copyAdapter = moshi.adapter(GamificationCopyCatalog::class.java).failOnUnknown()

    fun parseCatalog(json: String): GamificationCatalog = parse("catalog") {
        catalogAdapter.fromJson(json)
    }

    fun parseCopy(json: String): GamificationCopyCatalog = parse("copy") {
        copyAdapter.fromJson(json)
    }

    fun loadValidated(catalogJson: String, copyJson: String): GamificationCatalogBundle {
        val bundle = GamificationCatalogBundle(
            catalog = parseCatalog(catalogJson),
            copy = parseCopy(copyJson)
        )
        val report = GamificationCatalogValidator.validate(bundle)
        if (!report.isValid) throw GamificationCatalogValidationException(report)
        return bundle
    }

    fun loadValidated(catalogInput: InputStream, copyInput: InputStream): GamificationCatalogBundle {
        val catalogJson = catalogInput.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val copyJson = copyInput.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return loadValidated(catalogJson, copyJson)
    }

    private fun <T : Any> parse(label: String, block: () -> T?): T {
        return try {
            block() ?: throw GamificationCatalogFormatException("$label document is empty")
        } catch (error: GamificationCatalogFormatException) {
            throw error
        } catch (error: JsonDataException) {
            throw GamificationCatalogFormatException("$label document does not match the catalog contract", error)
        } catch (error: IOException) {
            throw GamificationCatalogFormatException("$label document could not be read", error)
        }
    }
}

class GamificationCatalogFormatException(
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)

class GamificationCatalogValidationException(
    val report: CatalogValidationReport
) : IllegalStateException(
    report.errors.joinToString(
        prefix = "Gamification catalog validation failed: ",
        separator = "; "
    ) { "${it.code} at ${it.path}" }
)
