package com.ultimatetool.app.linux

/**
 * Каталог дистрибутивов для proot-виртуализации.
 *
 * Важно про архитектуру (без root, без сторонних приложений):
 *  - полноценная KVM-виртуализация без привилегий невозможна;
 *  - используется proot-userland (подход UserLAnd/AnLinux): системные вызовы
 *    перехватываются в userspace, дистрибутив живёт в каталоге приложения;
 *  - бинарник proot и rootfs-образы скачиваются в рантайме;
 *  - графика — через VNC-сервер внутри гостя и встроенный просмотрщик.
 */
data class DistroDef(
    val id: String,
    val name: String,
    val family: String,
    val packageManager: String,
    val sizeHintMb: Int,
    /** URL rootfs по ABI-архитектуре (arm64 / armv7 / x86_64); null — не поддерживается. */
    val urlByAbi: Map<String, String>,
    /** Экспериментальный: официальный стабильный URL отсутствует, можно задать свой. */
    val experimental: Boolean = false,
    val hint: String = ""
)

object LinuxCatalog {

    /** Трансляция Android ABI в идентификатор архитектуры каталога. */
    fun abiToArch(abi: String): String = when {
        abi.startsWith("arm64") -> "arm64"
        abi.startsWith("armeabi") -> "armv7"
        abi.startsWith("x86_64") -> "x86_64"
        abi.startsWith("x86") -> "x86"
        else -> "unknown"
    }

    val all: List<DistroDef> = listOf(
        DistroDef(
            id = "alpine320",
            name = "Alpine Linux 3.20",
            family = "alpine",
            packageManager = "apk",
            sizeHintMb = 3,
            urlByAbi = mapOf(
                "arm64" to "https://dl-cdn.alpinelinux.org/alpine/v3.20/releases/aarch64/alpine-minirootfs-3.20.3-aarch64.tar.gz",
                "armv7" to "https://dl-cdn.alpinelinux.org/alpine/v3.20/releases/armv7/alpine-minirootfs-3.20.3-armv7.tar.gz",
                "x86_64" to "https://dl-cdn.alpinelinux.org/alpine/v3.20/releases/x86_64/alpine-minirootfs-3.20.3-x86_64.tar.gz"
            ),
            hint = "Минимальный образ (~3 МБ). Лучший выбор для первого запуска."
        ),
        DistroDef(
            id = "ubuntu2404",
            name = "Ubuntu 24.04 Minimal",
            family = "debian",
            packageManager = "apt",
            sizeHintMb = 360,
            urlByAbi = mapOf(
                "arm64" to "https://cloud-images.ubuntu.com/minimal/releases/noble/release/ubuntu-24.04-minimal-cloudimg-arm64-root.tar.xz",
                "x86_64" to "https://cloud-images.ubuntu.com/minimal/releases/noble/release/ubuntu-24.04-minimal-cloudimg-amd64-root.tar.xz"
            ),
            hint = "Полноценная Ubuntu из официальных cloud-образов."
        ),
        DistroDef(
            id = "debian12",
            name = "Debian 12 (bookworm)",
            family = "debian",
            packageManager = "apt",
            sizeHintMb = 120,
            urlByAbi = mapOf(
                "arm64" to "https://github.com/debuerreotype/docker-debian-artifacts/raw/distfiles/bookworm/arm64/rootfs.tar.xz",
                "armv7" to "https://github.com/debuerreotype/docker-debian-artifacts/raw/distfiles/bookworm/armhf/rootfs.tar.xz",
                "x86_64" to "https://github.com/debuerreotype/docker-debian-artifacts/raw/distfiles/bookworm/amd64/rootfs.tar.xz"
            ),
            hint = "Rootfs из проекта debuerreotype (те же образы, что у Docker)."
        ),
        DistroDef(
            id = "archlinux",
            name = "Arch Linux",
            family = "arch",
            packageManager = "pacman",
            sizeHintMb = 190,
            urlByAbi = mapOf(
                "x86_64" to "https://geo.mirror.pkgbuild.com/system/latest/archlinux-bootstrap-x86_64.tar.zst"
            ),
            experimental = true,
            hint = "Архив .tar.zst: укажите свой URL распакованного rootfs (.tar.gz)."
        ),
        DistroDef(
            id = "kalilinux",
            name = "Kali Linux",
            family = "debian",
            packageManager = "apt",
            sizeHintMb = 200,
            urlByAbi = emptyMap(),
            experimental = true,
            hint = "Укажите URL rootfs-образа вручную."
        ),
        DistroDef(
            id = "fedora40",
            name = "Fedora 40",
            family = "fedora",
            packageManager = "dnf",
            sizeHintMb = 250,
            urlByAbi = emptyMap(),
            experimental = true,
            hint = "Укажите URL rootfs-образа вручную."
        ),
        DistroDef(
            id = "voidlinux",
            name = "Void Linux",
            family = "void",
            packageManager = "xbps",
            sizeHintMb = 150,
            urlByAbi = emptyMap(),
            experimental = true,
            hint = "Укажите URL rootfs-образа вручную."
        ),
        DistroDef(
            id = "opensuse",
            name = "openSUSE Tumbleweed",
            family = "suse",
            packageManager = "zypper",
            sizeHintMb = 250,
            urlByAbi = emptyMap(),
            experimental = true,
            hint = "Укажите URL rootfs-образа вручную."
        )
    )

    fun byId(id: String): DistroDef? = all.firstOrNull { it.id == id }

    /** URL rootfs для дистрибутива на текущем ABI; null, если не поддерживается. */
    fun rootfsUrl(def: DistroDef, abi: String): String? = def.urlByAbi[abiToArch(abi)]
}
