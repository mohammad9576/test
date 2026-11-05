package com.kodomo.app

import com.kodomo.app.data.seed.CatalogText
import com.kodomo.app.domain.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogTextTest {

    @Test
    fun parsesHeaderAndPages() {
        val parsed = CatalogText.parse(
            """
            # comment
            @x1 | عنوان | دسته | 🐘 | 3-7 | ANIMALS,BOOKS | توضیح
            🌧️> صفحه‌ی اول
            🌈> صفحه‌ی دوم
            """.trimIndent(),
            ContentType.BOOK,
        )
        assertEquals(1, parsed.items.size)
        assertEquals(3, parsed.items[0].rating.minAge)
        assertEquals(7, parsed.items[0].rating.maxAge)
        assertEquals(2, parsed.pages.size)
        assertEquals("🌈", parsed.pages[1].emoji)
        assertEquals("صفحه‌ی دوم", parsed.pages[1].text)
    }

    /** Every bundled catalog parses, ids are unique and every item has at least three pages/steps. */
    @Test
    fun bundledCatalogsAreComplete() {
        val ids = mutableSetOf<String>()
        CatalogText.files.forEach { (path, type) ->
            val file = File("src/main/assets/$path")
            assertTrue("missing $path", file.exists())
            val parsed = CatalogText.parse(file.readText(), type)
            assertTrue("$path is too small", parsed.items.size >= 40)
            parsed.items.forEach { item ->
                assertTrue("duplicate id ${item.id}", ids.add(item.id))
                val pages = parsed.pages.count { it.contentId == item.id }
                assertTrue("${item.id} has only $pages pages", pages >= 3)
            }
        }
    }
}
