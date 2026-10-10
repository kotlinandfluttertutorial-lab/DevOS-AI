package com.devos.ai.data.repository.testing

import com.devos.ai.domain.repository.model.FileCoverage
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

/**
 * Parses a JaCoCo XML report into [FileCoverage] domain models using a SAX
 * parser ([DefaultHandler]).
 *
 * SAX is push-based and allocates no DOM tree, making it suitable for large
 * XML reports. It is also available on both JVM (unit tests) and Android
 * (production) without any extra dependencies.
 *
 * ## Expected XML shape
 * ```xml
 * <report name="...">
 *   <package name="com/example/feature">
 *     <sourcefile name="MyClass.kt">
 *       <counter type="LINE" covered="20" missed="10"/>
 *     </sourcefile>
 *   </package>
 * </report>
 * ```
 *
 * Only `<counter type="LINE">` elements are collected; BRANCH/METHOD/etc. are
 * ignored so results reflect line coverage only.
 *
 * DEVOS-049 / DA-60
 */
class JaCoCoParser {

    /**
     * Parses [xmlInputStream] and returns a list of [FileCoverage] objects.
     *
     * @throws Exception if the XML is malformed or the stream cannot be read.
     */
    fun parse(xmlInputStream: InputStream): List<FileCoverage> {
        val handler = JaCoCoHandler()
        SAXParserFactory.newInstance()
            .apply { isNamespaceAware = false }
            .newSAXParser()
            .parse(xmlInputStream, handler)
        return handler.results
    }

    // ── SAX handler ────────────────────────────────────────────────────────────

    private class JaCoCoHandler : DefaultHandler() {
        val results = mutableListOf<FileCoverage>()

        private var currentPackage: String? = null
        private var currentSourceFile: String? = null
        private var coveredLines = 0
        private var missedLines = 0

        override fun startElement(
            uri: String,
            localName: String,
            qName: String,
            attributes: Attributes,
        ) {
            when (qName) {
                "package" -> {
                    // package name uses '/' separators in JaCoCo XML
                    currentPackage = attributes.getValue("name")
                }

                "sourcefile" -> {
                    currentSourceFile = attributes.getValue("name")
                    coveredLines = 0
                    missedLines = 0
                }

                "counter" -> {
                    val type = attributes.getValue("type")
                    if (type == "LINE" && currentSourceFile != null) {
                        coveredLines += attributes.getValue("covered")?.toIntOrNull() ?: 0
                        missedLines += attributes.getValue("missed")?.toIntOrNull() ?: 0
                    }
                }
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            if (qName == "sourcefile" && currentSourceFile != null) {
                val totalLines = coveredLines + missedLines
                val lineCoverage = if (totalLines > 0) {
                    coveredLines.toFloat() / totalLines.toFloat()
                } else {
                    0f
                }

                val filePath = if (currentPackage != null) {
                    "$currentPackage/$currentSourceFile"
                } else {
                    currentSourceFile!!
                }

                results += FileCoverage(
                    filePath     = filePath,
                    lineCoverage = lineCoverage,
                    coveredLines = coveredLines,
                    totalLines   = totalLines,
                )

                currentSourceFile = null
                coveredLines = 0
                missedLines = 0
            }
        }
    }
}
