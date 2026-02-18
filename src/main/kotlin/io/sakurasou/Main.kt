package io.sakurasou

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream

/**
 * @author Shiina Kin
 * 2026/2/18 19:50
 */
fun main() {
    val path = "test.txt"

    println("Xmx: ${Runtime.getRuntime().maxMemory() / 1024 / 1024.0} MB")
    val file = Path(path)
    println("File Size: ${file.fileSize() / 1024 / 1024.0} MB")
    helloFFM(file)
}

fun helloFFM(file: Path) {
    // OOM
    // file.readBytes().toString()

    // < 2GB
    // file.inputStream().use { fileIS ->
    //     Arena.ofConfined().use { arena ->
    //         val memorySegment = arena.allocate(file.fileSize())
    //         val byteBuffer = memorySegment.asByteBuffer()
    //
    //         var point = 0L
    //         val bufferSize = 1024 * 1024
    //         val buffer = ByteArray(1024 * 1024)
    //         while (point < file.fileSize()) {
    //             val read = fileIS.readNBytes(buffer, 0, bufferSize)
    //             if (read <= 0) break
    //             byteBuffer.position(point.toInt()).put(buffer, 0, read)
    //             point += read
    //         }
    //
    //         println("Read file into memory segment: ${point / 1024 / 1024.0} MB")
    //         println("===== last 5 lines =====")
    //         var cursor = 0L
    //         val totalLineCnt = 100000000
    //         repeat(totalLineCnt) { i ->
    //             readLine(memorySegment, cursor).let {
    //                 cursor = it.first + 1
    //                 if (i > (totalLineCnt - 5)) println(it.second)
    //             }
    //         }
    //     }
    // }

    // >= 2GB
    file.inputStream().use { fileIS ->
        Arena.ofConfined().use { arena ->
            val memorySegment = arena.allocate(file.fileSize())

            var point = 0L
            val bufferSize = 1024 * 1024
            val buffer = ByteArray(bufferSize)
            while (point < file.fileSize()) {
                val read = fileIS.readNBytes(buffer, 0, bufferSize)
                if (read <= 0) break
                putBytesIntoMemorySegment(
                    memSegment = memorySegment,
                    offset = point,
                    bytes = buffer,
                    effactiveByteSize = read,
                )
                point += read
            }

            println("Read file into memory segment: ${point / 1024 / 1024.0} MB")
            println("===== last 5 lines =====")
            var cursor = 0L
            val totalLineCnt = 200000000
            repeat(totalLineCnt) { i ->
                readLine(memorySegment, cursor).let {
                    cursor = it.first + 1
                    if (i >= (totalLineCnt - 5)) println(it.second)
                }
            }

            val last5LineByteSlice = memorySegment.asSlice(point - 5 * 11)
            cursor = 0L
            println("===== last 5 lines (slice) =====")
            repeat(5) {
                readLine(last5LineByteSlice, cursor).let {
                    cursor = it.first + 1
                    println(it.second)
                }
            }
        }
    }
}

fun putBytesIntoMemorySegment(
    memSegment: MemorySegment,
    offset: Long,
    bytes: ByteArray,
    effactiveByteSize: Int,
) {
    for (idx in 0..<effactiveByteSize) {
        memSegment.set(ValueLayout.JAVA_BYTE, offset + idx, bytes[idx])
    }
}

fun readLine(
    memSegment: MemorySegment,
    offset: Long,
): Pair<Long, String> {
    var i = offset
    val out = ArrayList<Byte>(64)

    while (i < memSegment.byteSize()) {
        val b = memSegment.get(ValueLayout.JAVA_BYTE, i)
        if (b.toInt() == '\n'.code) break
        out.add(b)
        i++
    }

    return i to out.toByteArray().toString(Charsets.UTF_8)
}
