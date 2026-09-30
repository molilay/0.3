package com.ultimatetool.app.core.util

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.GZIPInputStream
import org.tukaani.xz.XZInputStream

/**
 * Минимальный распаковщик tar-архивов (.tar, .tar.gz, .tgz, .tar.xz)
 * без нативных зависимостей. Поддерживает файлы, каталоги и симлинки —
 * этого достаточно для rootfs-образов дистрибутивов.
 */
object TarExtractor {

    private const val BLOCK = 512

    fun extract(archive: File, targetDir: File, onProgressBytes: (Long) -> Unit = {}) {
        val raw: InputStream = archive.inputStream().buffered()
        val stream: InputStream = when {
            archive.name.endsWith(".tar") -> raw
            archive.name.endsWith(".tar.gz") || archive.name.endsWith(".tgz") -> GZIPInputStream(raw)
            archive.name.endsWith(".tar.xz") -> XZInputStream(raw)
            else -> {
                raw.close()
                throw IllegalArgumentException("Неизвестный формат архива: ${archive.name}")
            }
        }
        try {
            readTar(stream, targetDir, onProgressBytes)
        } finally {
            try { stream.close() } catch (_: Exception) {}
        }
    }

    private fun readTar(input: InputStream, targetDir: File, onProgress: (Long) -> Unit) {
        val header = ByteArray(BLOCK)
        val canonicalTarget = targetDir.canonicalPath
        var extracted = 0L

        while (true) {
            if (!readFully(input, header)) break
            if (header.all { it == 0.toByte() }) break

            val name = parseString(header, 0, 100)
            val prefix = parseString(header, 345, 155)
            val sizeText = parseString(header, 124, 12).trim().trimEnd(0.toChar())
            val size = if (sizeText.isEmpty()) 0L else try { sizeText.toLong(8) } catch (_: Exception) { 0L }
            val type = header[156].toInt().toChar()

            val entryName = (if (prefix.isNotEmpty()) "$prefix/$name" else name).trimStart('/')

            // Защита от zip-slip: путь не должен выходить за пределы каталога.
            val entry: File? = if (entryName.isEmpty()) {
                null
            } else {
                val candidate = File(targetDir, entryName).canonicalFile
                if (candidate.path.startsWith(canonicalTarget)) candidate else null
            }

            when (type) {
                '5' -> entry?.mkdirs()
                '2' -> if (entry != null) {
                    val linkTarget = parseString(header, 157, 100)
                    try {
                        entry.parentFile?.mkdirs()
                        Files.deleteIfExists(entry.toPath())
                        Files.createSymbolicLink(entry.toPath(), Paths.get(linkTarget))
                    } catch (_: Exception) {
                        // Симлинк не критичен для работоспособности большинства rootfs.
                    }
                }
                '0', '\u0000' -> if (entry != null && size > 0) {
                    entry.parentFile?.mkdirs()
                    FileOutputStream(entry).use { out ->
                        extracted += copyBytes(input, out, size)
                        onProgress(extracted)
                    }
                    skipPadding(input, size)
                    continue
                }
                else -> Unit
            }
            skipBytes(input, size)
            skipPadding(input, size)
        }
    }

    private fun copyBytes(input: InputStream, out: FileOutputStream, size: Long): Long {
        val buffer = ByteArray(64 * 1024)
        var remaining = size
        var written = 0L
        while (remaining > 0) {
            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
            val read = input.read(buffer, 0, toRead)
            if (read <= 0) break
            out.write(buffer, 0, read)
            remaining -= read
            written += read
        }
        return written
    }

    private fun skipPadding(input: InputStream, size: Long) {
        val rem = size % BLOCK
        if (rem != 0L) skipBytes(input, BLOCK - rem)
    }

    private fun skipBytes(input: InputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                if (input.read() == -1) break
                remaining -= 1
            } else {
                remaining -= skipped
            }
        }
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) return false
            offset += read
        }
        return true
    }

    private fun parseString(buffer: ByteArray, offset: Int, length: Int): String {
        val end = minOf(offset + length, buffer.size)
        var stop = offset
        while (stop < end && buffer[stop] != 0.toByte()) stop++
        return String(buffer, offset, stop - offset, Charsets.UTF_8).trim()
    }
}
