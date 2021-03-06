package com.soywiz.korio.util

import com.soywiz.korio.KorioNative

object OS {
	val name = KorioNative.osName
	val nameLC = name.toLowerCase()

	val platformName = KorioNative.platformName
	val platformNameLC = platformName.toLowerCase()

	val isWindows by lazy { nameLC.contains("win") }
	val isUnix by lazy { !isWindows }
	val isLinux by lazy { nameLC.contains("nix") || nameLC.contains("nux") || nameLC.contains("aix") }
	val isMac by lazy { nameLC.contains("mac") }

	val isJs: Boolean by lazy { platformNameLC.contains("js") }
	val isNodejs: Boolean by lazy { (platformNameLC.contains("node.js")) }
	val isBrowserJs: Boolean get() = isJs && !isNodejs
}
