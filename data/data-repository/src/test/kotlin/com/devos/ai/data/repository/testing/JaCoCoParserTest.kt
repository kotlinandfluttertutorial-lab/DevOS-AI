package com.devos.ai.data.repository.testing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [JaCoCoParser].
 *
 * All tests parse in-memory XML strings — no file I/O or Android runtime needed.
 * [XmlPullParserFactory] resolves to the JVM KXML2 implementation on the test JVM.
 *
 * DEVOS-049 / DA-60
 */
class JaCoCoParserTest {

    private val parser = JaCoCoParser()

    // ── Basic parsing ─────────────────────────────────────────────────────────

    @Test
    fun `parse returns FileCoverage for single sourcefile with LINE counter`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="MyApp">
              <package name="com/example">
                <sourcefile name="MyClass.kt">
                  <counter type="LINE" covered="20" missed="10"/>
                </sourcefile>
              </package>
            </report>
        """.trimIndent()

        val results = parser.parse(xml.byteInputStream())

        assertEquals(1, results.size)
        val coverage = results[0]
        assertEquals("com/example/MyClass.kt", coverage.filePath)
        assertEquals(20, coverage.coveredLines)
        assertEquals(30, coverage.totalLines)
        assertEquals(20f / 30f, coverage.lineCoverage, 0.001f)
    }

    @Test
    fun `parse calculates 66_7 percent for 20 covered 10 missed`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="TestReport">
              <package name="com/devos">
                <sourcefile name="TargetFile.kt">
                  <counter type="LINE" covered="20" missed="10"/>
                </sourcefile>
              </package>
            </report>
        """.trimIndent()

        val results = parser.parse(xml.byteInputStream())
        val coverage = results[0]

        // 20 / 30 = 0.6667 (66.7%)
        assertEquals(0.6667f, coverage.lineCoverage, 0.001f)
        assertEquals(20, coverage.coveredLines)
        assertEquals(30, coverage.totalLines)
    }

    @Test
    fun `parse returns zero coverage when all lines are missed`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="MyApp">
              <package name="com/example">
                <sourcefile name="Uncovered.kt">
                  <counter type="LINE" covered="0" missed="15"/>
                </sourcefile>
              </package>
            </report>
        """.trimIndent()

        val results = parser.parse(xml.byteInputStream())

        assertEquals(1, results.size)
        assertEquals(0f, results[0].lineCoverage, 0.001f)
        assertEquals(0, results[0].coveredLines)
        assertEquals(15, results[0].totalLines)
    }

    @Test
    fun `parse returns full coverage when all lines are covered`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="MyApp">
              <package name="com/example">
                <sourcefile name="FullyCovered.kt">
                  <counter type="LINE" covered="50" missed="0"/>
                </sourcefile>
              </package>
            </report>
        """.trimIndent()

        val results = parser.parse(xml.byteInputStream())

        assertEquals(1f, results[0].lineCoverage, 0.001f)
    }

    @Test
    fun `parse ignores counters that are not LINE type`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="MyApp">
              <package name="com/example">
                <sourcefile name="Mixed.kt">
                  <counter type="BRANCH" covered="5" missed="3"/>
                  <counter type="LINE" covered="10" missed="5"/>
                  <counter type="METHOD" covered="2" missed="1"/>
                </sourcefile>
              </package>
            </report>
        """.trimIndent()

        val results = parser.parse(xml.byteInputStream())

        // Only LINE counter should be counted: 10 covered + 5 missed = 15 total
        assertEquals(1, results.size)
        assertEquals(10, results[0].coveredLines)
        assertEquals(15, results[0].totalLines)
    }

    @Test
    fun `parse handles multiple sourcefiles in one package`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="MyApp">
              <package name="com/example">
                <sourcefile name="ClassA.kt">
                  <counter type="LINE" covered="10" missed="0"/>
                </sourcefile>
                <sourcefile name="ClassB.kt">
                  <counter type="LINE" covered="5" missed="5"/>
                </sourcefile>
              </package>
            </report>
        """.trimIndent()

        val results = parser.parse(xml.byteInputStream())

        assertEquals(2, results.size)
        assertTrue(results.any { it.filePath == "com/example/ClassA.kt" })
        assertTrue(results.any { it.filePath == "com/example/ClassB.kt" })
    }

    @Test
    fun `parse handles multiple packages`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="MyApp">
              <package name="com/example/ui">
                <sourcefile name="Screen.kt">
                  <counter type="LINE" covered="8" missed="2"/>
                </sourcefile>
              </package>
              <package name="com/example/data">
                <sourcefile name="Repository.kt">
                  <counter type="LINE" covered="12" missed="3"/>
                </sourcefile>
              </package>
            </report>
        """.trimIndent()

        val results = parser.parse(xml.byteInputStream())

        assertEquals(2, results.size)
        assertTrue(results.any { it.filePath == "com/example/ui/Screen.kt" })
        assertTrue(results.any { it.filePath == "com/example/data/Repository.kt" })
    }

    @Test
    fun `parse returns empty list for report with no sourcefiles`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="Empty">
              <package name="com/example"/>
            </report>
        """.trimIndent()

        val results = parser.parse(xml.byteInputStream())

        assertTrue(results.isEmpty())
    }

    @Test
    fun `parse returns zero coverage when sourcefile has no LINE counter`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <report name="MyApp">
              <package name="com/example">
                <sourcefile name="NoLineCounter.kt">
                  <counter type="METHOD" covered="2" missed="1"/>
                </sourcefile>
              </package>
            </report>
        """.trimIndent()

        val results = parser.parse(xml.byteInputStream())

        assertEquals(1, results.size)
        // No LINE counter → totalLines = 0, lineCoverage = 0
        assertEquals(0, results[0].totalLines)
        assertEquals(0f, results[0].lineCoverage, 0.001f)
    }
}
