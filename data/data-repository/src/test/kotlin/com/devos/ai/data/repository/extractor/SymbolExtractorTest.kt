package com.devos.ai.data.repository.extractor

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SymbolExtractor].
 *
 * All tests use in-memory source strings — no file I/O needed.
 * Tests verify that the correct symbol names, kinds, visibility, and doc
 * comments are extracted from representative Kotlin and Java snippets.
 */
class SymbolExtractorTest {

    private val repoId   = "test-repo"
    private val ktPath   = "src/main/kotlin/Foo.kt"
    private val javaPath = "src/main/java/Bar.java"

    // ─── Kotlin: classes ─────────────────────────────────────────────────────

    @Test
    fun `extracts Kotlin class name and kind`() {
        val source = """
            class MyViewModel : ViewModel() {
            }
        """.trimIndent()

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        val cls = symbols.find { it.name == "MyViewModel" }

        assertThat(cls).isNotNull()
        assertThat(cls!!.kind).isEqualTo("CLASS")
        assertThat(cls.visibility).isEqualTo("PUBLIC")
    }

    @Test
    fun `extracts Kotlin data class`() {
        val source = "data class User(val id: String, val name: String)"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        assertThat(symbols.any { it.name == "User" && it.kind == "CLASS" }).isTrue()
    }

    @Test
    fun `extracts Kotlin internal class`() {
        val source = "internal class InternalHelper {"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        val cls = symbols.find { it.name == "InternalHelper" }
        assertThat(cls).isNotNull()
        assertThat(cls!!.visibility).isEqualTo("INTERNAL")
    }

    @Test
    fun `extracts Kotlin interface`() {
        val source = "interface MyRepository {"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        assertThat(symbols.any { it.name == "MyRepository" && it.kind == "INTERFACE" }).isTrue()
    }

    @Test
    fun `extracts Kotlin object`() {
        val source = "object SomeSingleton {"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        assertThat(symbols.any { it.name == "SomeSingleton" && it.kind == "OBJECT" }).isTrue()
    }

    @Test
    fun `extracts Kotlin enum class`() {
        val source = "enum class SyncStatus { IDLE, SYNCING, SYNCED, ERROR }"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        assertThat(symbols.any { it.name == "SyncStatus" && it.kind == "ENUM" }).isTrue()
    }

    @Test
    fun `extracts Kotlin annotation class`() {
        val source = "annotation class MyAnnotation"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        assertThat(symbols.any { it.name == "MyAnnotation" && it.kind == "ANNOTATION" }).isTrue()
    }

    // ─── Kotlin: functions ────────────────────────────────────────────────────

    @Test
    fun `extracts Kotlin fun name and kind`() {
        val source = """
            fun doSomething(input: String): Boolean {
                return true
            }
        """.trimIndent()

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        val fn = symbols.find { it.name == "doSomething" }

        assertThat(fn).isNotNull()
        assertThat(fn!!.kind).isEqualTo("FUNCTION")
    }

    @Test
    fun `extracts private Kotlin fun`() {
        val source = "private fun helper(): Unit {"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        val fn = symbols.find { it.name == "helper" }
        assertThat(fn).isNotNull()
        assertThat(fn!!.visibility).isEqualTo("PRIVATE")
    }

    @Test
    fun `extracts suspend fun`() {
        val source = "    suspend fun fetchData(): Result<String> {"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        assertThat(symbols.any { it.name == "fetchData" && it.kind == "FUNCTION" }).isTrue()
    }

    @Test
    fun `extracts override fun`() {
        val source = "    override fun onCleared() {"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        assertThat(symbols.any { it.name == "onCleared" && it.kind == "FUNCTION" }).isTrue()
    }

    // ─── Kotlin: properties ───────────────────────────────────────────────────

    @Test
    fun `extracts top-level Kotlin val`() {
        val source = "val TAG: String = \"MyTag\""

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        assertThat(symbols.any { it.name == "TAG" && it.kind == "PROPERTY" }).isTrue()
    }

    @Test
    fun `extracts top-level Kotlin var`() {
        val source = "var counter: Int = 0"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        assertThat(symbols.any { it.name == "counter" && it.kind == "PROPERTY" }).isTrue()
    }

    // ─── Kotlin: doc comment ──────────────────────────────────────────────────

    @Test
    fun `captures KDoc comment on following fun`() {
        val source = """
            /**
             * Loads the user by ID.
             * Returns null if not found.
             */
            fun loadUser(id: String): User? = null
        """.trimIndent()

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        val fn = symbols.find { it.name == "loadUser" }
        assertThat(fn).isNotNull()
        assertThat(fn!!.docComment).isNotNull()
        assertThat(fn.docComment!!.contains("Loads the user")).isTrue()
    }

    // ─── Kotlin: multi-symbol file ────────────────────────────────────────────

    @Test
    fun `extracts multiple symbols from same file`() {
        val source = """
            class Outer {
            }

            interface Gateway {
            }

            fun topLevel() {}
        """.trimIndent()

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        assertThat(symbols.size).isGreaterThan(2)
        assertThat(symbols.any { it.name == "Outer" }).isTrue()
        assertThat(symbols.any { it.name == "Gateway" }).isTrue()
        assertThat(symbols.any { it.name == "topLevel" }).isTrue()
    }

    // ─── Kotlin: line numbers ─────────────────────────────────────────────────

    @Test
    fun `lineStart is 1-based and correct`() {
        val source = "\n\nfun myFunc() {"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        val fn = symbols.find { it.name == "myFunc" }
        assertThat(fn).isNotNull()
        assertThat(fn!!.lineStart).isEqualTo(3)
    }

    // ─── Kotlin: ID uniqueness ────────────────────────────────────────────────

    @Test
    fun `symbol IDs are unique within a file`() {
        val source = """
            fun alpha() {}
            fun beta() {}
            fun gamma() {}
        """.trimIndent()

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        val ids = symbols.map { it.id }.toSet()
        assertThat(ids.size).isEqualTo(symbols.size)
    }

    // ─── Java: classes ────────────────────────────────────────────────────────

    @Test
    fun `extracts Java class`() {
        val source = """
            public class UserRepository {
            }
        """.trimIndent()

        val symbols = SymbolExtractor.extract(repoId, javaPath, source)
        assertThat(symbols.any { it.name == "UserRepository" && it.kind == "CLASS" }).isTrue()
    }

    @Test
    fun `extracts Java interface`() {
        val source = "public interface Callback {"

        val symbols = SymbolExtractor.extract(repoId, javaPath, source)
        assertThat(symbols.any { it.name == "Callback" && it.kind == "INTERFACE" }).isTrue()
    }

    @Test
    fun `extracts Java enum`() {
        val source = "public enum Status { ACTIVE, INACTIVE }"

        val symbols = SymbolExtractor.extract(repoId, javaPath, source)
        assertThat(symbols.any { it.name == "Status" && it.kind == "ENUM" }).isTrue()
    }

    // ─── Java: methods ────────────────────────────────────────────────────────

    @Test
    fun `extracts Java public method`() {
        val source = """
            public class Foo {
                public String getName() {
                }
            }
        """.trimIndent()

        val symbols = SymbolExtractor.extract(repoId, javaPath, source)
        assertThat(symbols.any { it.name == "getName" && it.kind == "FUNCTION" }).isTrue()
    }

    @Test
    fun `extracts Java private method`() {
        val source = """
            public class Foo {
                private void helper() {
                }
            }
        """.trimIndent()

        val symbols = SymbolExtractor.extract(repoId, javaPath, source)
        val method = symbols.find { it.name == "helper" }
        assertThat(method).isNotNull()
        assertThat(method!!.visibility).isEqualTo("PRIVATE")
    }

    // ─── Java: package-private visibility ─────────────────────────────────────

    @Test
    fun `Java class without modifier gets PACKAGE_PRIVATE visibility`() {
        val source = "class PackageClass {"

        val symbols = SymbolExtractor.extract(repoId, javaPath, source)
        val cls = symbols.find { it.name == "PackageClass" }
        assertThat(cls).isNotNull()
        assertThat(cls!!.visibility).isEqualTo("PACKAGE_PRIVATE")
    }

    // ─── Unsupported extensions ───────────────────────────────────────────────

    @Test
    fun `non-Kotlin non-Java file returns empty list`() {
        val symbols = SymbolExtractor.extract(repoId, "src/main/res/layout/main.xml", "<root/>")
        assertThat(symbols).isEmpty()
    }

    @Test
    fun `empty source returns empty list`() {
        assertThat(SymbolExtractor.extract(repoId, ktPath, "")).isEmpty()
        assertThat(SymbolExtractor.extract(repoId, javaPath, "")).isEmpty()
    }

    // ─── Composite ID format ──────────────────────────────────────────────────

    @Test
    fun `symbol ID contains repoId and filePath`() {
        val source = "class Foo {"

        val symbols = SymbolExtractor.extract(repoId, ktPath, source)
        val cls = symbols.find { it.name == "Foo" }
        assertThat(cls).isNotNull()
        assertThat(cls!!.id.startsWith("$repoId:$ktPath:")).isTrue()
    }
}
