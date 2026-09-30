package com.ultimatetool.app.linux

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Движок proot-виртуализации.
 *
 * Конвейер: бинарник proot → скачивание rootfs → распаковка в каталог
 * приложения → запуск оболочки через `proot -0 -r <rootfs>`.
 *
 * Ограничения (осознанные):
 *  - проот перехватывает сисколлы в userspace: медленнее нативного,
 *    нет доступа к устройствам ядра, но не нужен root;
 *  - PTY-терминал в полном объёме требует нативной библиотеки — здесь
 *    используется линейный ввод/вывод поверх потоков процесса;
 *  - графика — через VNC-сервер внутри гостя (см. LinuxGuiScreen).
 */
class ProotEngine(private val context: Context?) {

    data class Progress(val stage: String, val done: Long, val total: Long) {
        val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    }

    /** Запущенный экземпляр дистрибутива: процесс + потоки ввода/вывода. */
    class RunningInstance(
        val distroId: String,
        val process: Process,
        val input: java.io.OutputStream,
        val output: java.io.InputStream
    ) {
        val isAlive: Boolean get() = try { process.isAlive } catch (_: Exception) { false }

        fun sendLine(line: String) {
            try {
                input.write((line + "\n").toByteArray(Charsets.UTF_8))
                input.flush()
            } catch (_: Exception) {
            }
        }

        fun destroy() {
            try { process.destroy() } catch (_: Exception) {}
        }
    }

    private val running = HashMap<String, RunningInstance>()

    private val filesDir: File
        get() = context?.filesDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")

    private val binDir: File get() = File(filesDir, "linux/bin").apply { mkdirs() }
    private val distrosDir: File get() = File(filesDir, "linux/distros").apply { mkdirs() }

    fun rootfsDir(distroId: String): File = File(distrosDir, "$distroId/rootfs")
    private fun archiveFile(distroId: String): File = File(distrosDir, "$distroId/rootfs.archive")

    val abi: String get() = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

    val prootFile: File get() = File(binDir, "proot-${LinuxCatalog.abiToArch(abi)}")

    val isProotReady: Boolean get() = prootFile.exists() && prootFile.canExecute()

    // ---------- Установка proot ----------

    /** URL бинарника проот (пакеты проекта UserLAnd-Assets-Support, пересобираемы). */
    fun prootDownloadUrl(): String =
        "https://github.com/CypherpunkArmory/UserLAnd-Assets-Support/raw/master/" +
            "${LinuxCatalog.abiToArch(abi)}/proot"

    fun ensureProot(onProgress: (Progress) -> Unit): Boolean {
        if (isProotReady) return true
        val target = prootFile
        target.parentFile?.mkdirs()
        return try {
            downloadFile(prootDownloadUrl(), target, "proot") { done, total ->
                onProgress(Progress("Загрузка proot", done, total))
            }
            target.setExecutable(true, true)
            target.canExecute()
        } catch (e: Exception) {
            try { target.delete() } catch (_: Exception) {}
            false
        }
    }

    // ---------- Установка дистрибутива ----------

    fun isInstalled(distroId: String): Boolean {
        val marker = File(rootfsDir(distroId), ".installed")
        return marker.exists()
    }

    /**
     * Скачивает и распаковывает rootfs. [customUrl] перекрывает каталожный
     * (для экспериментальных дистрибутивов).
     */
    fun installDistro(
        def: DistroDef,
        customUrl: String?,
        onProgress: (Progress) -> Unit
    ): Boolean {
        val url = customUrl?.takeIf { it.isNotBlank() } ?: LinuxCatalog.rootfsUrl(def, abi)
            ?: return false

        val archive = archiveFile(def.id)
        archive.parentFile?.mkdirs()
        val rootfs = rootfsDir(def.id)

        return try {
            downloadFile(url, archive, "rootfs") { done, total ->
                onProgress(Progress("Загрузка ${def.name}", done, total))
            }
            rootfs.mkdirs()
            com.ultimatetool.app.core.util.TarExtractor.extract(archive, rootfs) { extracted ->
                onProgress(Progress("Распаковка ${def.name}", extracted, archive.length() * 4))
            }
            // resolv.conf для сети внутри гостя.
            try {
                File(rootfs, "etc/resolv.conf").apply {
                    parentFile?.mkdirs()
                    writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")
                }
            } catch (_: Exception) {}
            File(rootfs, ".installed").writeText(def.id)
            try { archive.delete() } catch (_: Exception) {}
            true
        } catch (e: Exception) {
            try { archive.delete() } catch (_: Exception) {}
            false
        }
    }

    fun uninstallDistro(distroId: String) {
        stop(distroId)
        try { File(distrosDir, distroId).deleteRecursively() } catch (_: Exception) {}
    }

    // ---------- Запуск ----------

    /** Запускает оболочку дистрибутива под proot. Возвращает null при ошибке. */
    @Synchronized
    fun startShell(distroId: String): RunningInstance? {
        running[distroId]?.takeIf { it.isAlive }?.let { return it }
        if (context == null) return null
        if (!isProotReady || !isInstalled(distroId)) return null

        val rootfs = rootfsDir(distroId)
        val command = listOf(
            prootFile.absolutePath,
            "--link2symlink", "-0",
            "-r", rootfs.absolutePath,
            "-w", "/root",
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "/etc/resolv.conf:/etc/resolv.conf",
            "/bin/sh"
        )
        return try {
            val builder = ProcessBuilder(command)
                .directory(rootfs)
                .redirectErrorStream(true)
            builder.environment().apply {
                put("HOME", "/root")
                put("TERM", "xterm-256color")
                put("LANG", "C.UTF-8")
                put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
            }
            val process = builder.start()
            val instance = RunningInstance(distroId, process, process.outputStream, process.inputStream)
            running[distroId] = instance
            instance
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Команда запуска VNC-сервера внутри гостя. Пакет ставится один раз
     * (для debian-семейства — tigervnc-standalone-server), затем стартует
     * дисплей :1 на порту 5901.
     */
    fun vncStartCommand(family: String): String = when (family) {
        "debian" -> "apt-get update && apt-get install -y tigervnc-standalone-server && " +
            "vncserver :1 -localhost no -geometry 1280x720 -depth 24"
        "alpine" -> "apk add tigervnc && vncserver :1 -localhost no -geometry 1280x720"
        else -> "echo 'Установите VNC-сервер пакетным менеджером дистрибутива'"
    }

    fun vncPort(): Int = 5901

    @Synchronized
    fun runningInstance(distroId: String): RunningInstance? =
        running[distroId]?.takeIf { it.isAlive }

    @Synchronized
    fun stop(distroId: String) {
        running.remove(distroId)?.destroy()
    }

    @Synchronized
    fun stopAll() {
        running.values.forEach { it.destroy() }
        running.clear()
    }

    fun isRunning(distroId: String): Boolean = running[distroId]?.isAlive == true

    // ---------- Загрузка файлов ----------

    private fun downloadFile(
        url: String,
        target: File,
        label: String,
        onProgress: (Long, Long) -> Unit
    ) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw RuntimeException("$label: HTTP $code")
            }
            val total = connection.contentLengthLong
            var downloaded = 0L
            FileOutputStream(target).use { out ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
