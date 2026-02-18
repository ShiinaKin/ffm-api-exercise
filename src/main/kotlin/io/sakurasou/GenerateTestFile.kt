package io.sakurasou

import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.writeLines

/**
 * @author Shiina Kin
 * 2026/2/18 20:12
 */
fun main() {
    generate()
}

fun generate() {
    val file = Path("test.txt")
    val list =
        buildList {
            for (i in 1..200000000) {
                add(i.toString().padStart(10, '0'))
            }
        }
    file.writeLines(list, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
}
