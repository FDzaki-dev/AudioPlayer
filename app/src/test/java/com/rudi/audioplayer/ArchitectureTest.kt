package com.rudi.audioplayer

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Batch 570 — aturan arsitektur (ArchUnit) sebagai LAPORAN, bukan gerbang.
 *
 * - Dilewati (JUnit "skipped") kecuali env `SONIX_ARCH_REPORT=1` (hanya dipasang oleh
 *   `.github/workflows/android-artifact.yml`) -> `build.yml` (`testReleaseUnitTest`) tidak menjalankan
 *   analisis dan tidak pernah gagal karenanya.
 * - Saat aktif TIDAK PERNAH gagal karena pelanggaran: hasil ditulis ke
 *   `app/build/reports/archunit/archunit-report.txt` (dibaca workflow). Bila ArchUnit sendiri gagal jalan
 *   atau tak ada kelas ter-import -> `ARCHUNIT_STATUS=NOT_VERIFIED` (tool gagal != lulus).
 * - Aturan = hanya arah dependensi yang SAAT INI sudah dipenuhi (dicek dari graf import Batch 570), jadi
 *   pelanggaran baru = regresi arsitektur. Tepi lama `data -> ui.theme` (ThemeStore) sengaja TIDAK
 *   dilarang. Siklus antar-package (mis. bubble <-> playback) sengaja tidak diuji: sudah ada.
 * - Pola package selalu berawalan `com.rudi.audioplayer` (pola `..ui..` polos akan menangkap
 *   `androidx.compose.ui` dan memberi pelanggaran palsu).
 * - Pakai `archunit` inti (BUKAN `archunit-junit5`): tes proyek ini JUnit 4.
 */
class ArchitectureTest {

    private class RuleOutcome(val name: String, val violations: Int, val details: List<String>)

    @Test
    fun writeArchitectureReport() {
        assumeTrue(
            "Dilewati: set $ENV_FLAG=1 untuk menjalankan (dipasang android-artifact.yml).",
            System.getenv(ENV_FLAG) == "1",
        )
        val text = try {
            buildReport()
        } catch (t: Throwable) {
            "ARCHUNIT_STATUS=NOT_VERIFIED\nREASON=${t.javaClass.name}: ${t.message}\n"
        }
        val file = File(REPORT_PATH)
        file.parentFile?.mkdirs()
        file.writeText(text)
        println(text)
    }

    private fun buildReport(): String {
        val classes = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .withImportOption { location -> !location.contains("UnitTest") }
            .importPackages(ROOT)
        val total = classes.count()
        if (total == 0) {
            return "ARCHUNIT_STATUS=NOT_VERIFIED\nREASON=0 kelas ter-import dari $ROOT (classpath tes tak memuat kelas app)\n"
        }
        val outcomes = rules().map { (name, rule) -> evaluate(name, rule, classes) }
        val violations = outcomes.sumOf { it.violations }
        return buildString {
            appendLine("ARCHUNIT_STATUS=OK")
            appendLine("CLASSES=$total")
            appendLine("RULES=${outcomes.size}")
            appendLine("VIOLATIONS=$violations")
            outcomes.forEach { outcome ->
                if (outcome.violations == 0) {
                    appendLine("[PASS] ${outcome.name}")
                } else {
                    appendLine("[FAIL x${outcome.violations}] ${outcome.name}")
                    outcome.details.forEach { appendLine("    $it") }
                    if (outcome.violations > outcome.details.size) {
                        appendLine("    ... (${outcome.violations - outcome.details.size} baris lain disingkat)")
                    }
                }
            }
        }
    }

    private fun evaluate(name: String, rule: ArchRule, classes: JavaClasses): RuleOutcome =
        try {
            rule.check(classes)
            RuleOutcome(name, 0, emptyList())
        } catch (e: AssertionError) {
            val message = e.message.orEmpty()
            val count = VIOLATION_COUNT.find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val lines = message.lines().drop(1).map { it.trim() }.filter { it.isNotEmpty() }
            RuleOutcome(name, count, lines.take(MAX_DETAILS))
        }

    private fun forbid(from: String, vararg targets: String): ArchRule =
        noClasses().that().resideInAPackage(from)
            .should().dependOnClassesThat().resideInAnyPackage(*targets)

    private fun rules(): List<Pair<String, ArchRule>> = listOf(
        "util mandiri: tidak bergantung ke lapisan app lain" to
            forbid(UTIL, UI, DATA, PLAYBACK, BUBBLE, WIDGET, WORKER, UPDATE),
        "data tidak bergantung ke playback/bubble/widget/worker/update/layar UI (ui.theme dikecualikan)" to
            forbid(DATA, PLAYBACK, BUBBLE, WIDGET, WORKER, UPDATE, UI_SCREENS, UI_LYRICS, UI_ADAPTIVE),
        "ui.theme hanya bergantung ke util" to
            forbid(UI_THEME, DATA, PLAYBACK, BUBBLE, WIDGET, WORKER, UPDATE, UI_SCREENS, UI_LYRICS, UI_ADAPTIVE),
        "ui tidak bergantung ke bubble/widget/worker" to
            forbid(UI, BUBBLE, WIDGET, WORKER),
        "playback tidak bergantung ke layar UI (ui.theme dikecualikan)" to
            forbid(PLAYBACK, UI_SCREENS, UI_LYRICS, UI_ADAPTIVE),
        "worker hanya bergantung ke data/util" to
            forbid(WORKER, UI, PLAYBACK, BUBBLE, WIDGET, UPDATE),
        "update mandiri: tidak bergantung ke ui/data/playback/bubble/widget/worker" to
            forbid(UPDATE, UI, DATA, PLAYBACK, BUBBLE, WIDGET, WORKER),
    )

    private companion object {
        const val ENV_FLAG = "SONIX_ARCH_REPORT"
        const val REPORT_PATH = "build/reports/archunit/archunit-report.txt"
        const val MAX_DETAILS = 12
        val VIOLATION_COUNT = Regex("""was violated \((\d+) times?\)""")

        const val ROOT = "com.rudi.audioplayer"
        const val UTIL = "$ROOT.util.."
        const val DATA = "$ROOT.data.."
        const val PLAYBACK = "$ROOT.playback.."
        const val BUBBLE = "$ROOT.bubble.."
        const val WIDGET = "$ROOT.widget.."
        const val WORKER = "$ROOT.worker.."
        const val UPDATE = "$ROOT.update.."
        const val UI = "$ROOT.ui.."
        const val UI_SCREENS = "$ROOT.ui" // package `ui` saja (tanpa sub-package)
        const val UI_LYRICS = "$ROOT.ui.lyrics.."
        const val UI_ADAPTIVE = "$ROOT.ui.adaptive.."
        const val UI_THEME = "$ROOT.ui.theme.."
    }
}
