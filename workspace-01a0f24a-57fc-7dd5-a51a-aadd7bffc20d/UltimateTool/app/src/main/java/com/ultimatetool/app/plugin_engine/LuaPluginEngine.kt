package com.ultimatetool.app.plugin_engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JsePlatform

/**
 * Движок Lua-плагинов на LuaJ.
 *
 * Модель песочницы:
 *  - убраны os, io, require, loadfile, dofile, load/loadstring;
 *  - плагины видят только таблицу `tool` (log, toast, version, httpGet);
 *  - вывод ограничен 20 000 символов, ответы сети — 10 000 символов;
 *  - таймаут выполнения: поток с зависшим скриптом бросается (безопасно
 *    прервать выполняющийся Lua-код в LuaJ нельзя — это осознанное ограничение).
 *
 * Контекст передаётся обычным параметром: так движок создаётся через @Provides
 * в DI и при этом конструируется с null в JVM-тестах без Android.
 */
class LuaPluginEngine(
    private val appContext: Context?
) {

    data class RunResult(
        val output: String,
        val error: String?,
        val durationMs: Long
    ) {
        val isSuccess: Boolean get() = error == null
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    fun run(source: String, timeoutMs: Long = 5_000L): RunResult {
        val start = System.currentTimeMillis()
        val logBuffer = StringBuilder()
        val lock = Any()
        val threadError = AtomicReference<String?>(null)
        val finished = AtomicBoolean(false)

        val worker = thread(name = "lua-plugin-runner", isDaemon = true) {
            try {
                val globals = JsePlatform.standardGlobals()

                // --- песочница: режем опасные модули ---
                listOf("os", "io", "require", "loadfile", "dofile", "load", "loadstring")
                    .forEach { globals.set(it, LuaValue.NIL) }

                val logFn = object : OneArgFunction() {
                    override fun call(arg: LuaValue): LuaValue {
                        val line = arg.optjstring("nil").take(500)
                        synchronized(lock) {
                            if (logBuffer.length < MAX_OUTPUT) {
                                logBuffer.append(line).append('\n')
                            }
                        }
                        return LuaValue.NIL
                    }
                }
                globals.set("print", logFn)

                val tool = LuaTable()
                tool.set("log", logFn)
                tool.set("version", object : ZeroArgFunction() {
                    override fun call(): LuaValue = LuaValue.valueOf(APP_VERSION)
                })
                tool.set("toast", object : OneArgFunction() {
                    override fun call(arg: LuaValue): LuaValue {
                        val message = arg.optjstring("").take(200)
                        mainHandler.post {
                            appContext?.let { ctx ->
                                try {
                                    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                }
                            }
                        }
                        return LuaValue.NIL
                    }
                })
                tool.set("httpGet", object : OneArgFunction() {
                    override fun call(arg: LuaValue): LuaValue {
                        val url = arg.optjstring("")
                        return try {
                            LuaValue.valueOf(httpGet(url).take(MAX_HTTP_RESPONSE))
                        } catch (e: Exception) {
                            LuaValue.valueOf("error: " + (e.message ?: "network"))
                        }
                    }
                })
                globals.set("tool", tool)

                val chunk = globals.load(source, "plugin")
                chunk.call()

                // Точка входа плагина (необязательная).
                val onRun = globals.get("on_run")
                if (!onRun.isnil() && onRun.isfunction()) {
                    onRun.call()
                }
            } catch (e: Throwable) {
                threadError.set(e.message ?: e.javaClass.simpleName)
            } finally {
                finished.set(true)
            }
        }

        try {
            worker.join(timeoutMs)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        val output = synchronized(lock) { logBuffer.toString() }
        val error = if (finished.get()) threadError.get() else "Таймаут выполнения (>${timeoutMs} мс)"
        return RunResult(output, error, System.currentTimeMillis() - start)
    }

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            requestMethod = "GET"
        }
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val APP_VERSION = "1.0.0"
        const val MAX_OUTPUT = 20_000
        const val MAX_HTTP_RESPONSE = 10_000

        /** Пример плагина для быстрого старта. */
        val SAMPLE_PLUGIN = """
            -- Пример плагина UltimateTool
            -- Доступное API: tool.log(s), tool.toast(s),
            --                tool.version(), tool.httpGet(url)
            function on_run()
              tool.log("Привет от плагина!")
              tool.log("Версия приложения: " .. tool.version())
              local sum = 0
              for i = 1, 10 do sum = sum + i end
              tool.log("Сумма 1..10 = " .. sum)
              return "OK"
            end
        """.trimIndent()
    }
}
