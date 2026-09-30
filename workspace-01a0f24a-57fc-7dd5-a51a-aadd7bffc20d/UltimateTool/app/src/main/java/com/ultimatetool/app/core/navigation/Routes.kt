package com.ultimatetool.app.core.navigation

/** Все маршруты навигации в одном месте (единая точка правки). */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val AI_CHAT = "ai_chat"
    const val PLUGINS = "plugins"
    const val COMMUNITY = "community"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    const val TOOL = "tool/{toolId}"
    fun tool(toolId: String): String = "tool/$toolId"

    const val LINUX = "linux"
    const val LINUX_TERMINAL = "linux_terminal/{distroId}"
    fun linuxTerminal(distroId: String): String = "linux_terminal/$distroId"
    const val LINUX_GUI = "linux_gui/{distroId}"
    fun linuxGui(distroId: String): String = "linux_gui/$distroId"

    const val PLUGIN_EDITOR = "plugin_editor?pluginId={pluginId}"
    fun pluginEditor(pluginId: String?): String =
        if (pluginId == null) "plugin_editor" else "plugin_editor?pluginId=$pluginId"
}
