package com.arturo254.opentune.ui.original.adapters
import com.arturo254.opentune.constants.PlayerDesignStyle
internal enum class GalaxSceneFamily { LEGACY, APPLE, STUDIO, IMMERSIVE, VINJAX }
internal fun galaxSceneFamily(style: PlayerDesignStyle): GalaxSceneFamily = when (style) {
 PlayerDesignStyle.V1, PlayerDesignStyle.V2, PlayerDesignStyle.V3, PlayerDesignStyle.V4,
 PlayerDesignStyle.V5, PlayerDesignStyle.V6, PlayerDesignStyle.V7 -> GalaxSceneFamily.LEGACY
 PlayerDesignStyle.V8 -> GalaxSceneFamily.APPLE
 PlayerDesignStyle.V9, PlayerDesignStyle.V10, PlayerDesignStyle.V11, PlayerDesignStyle.V12,
 PlayerDesignStyle.V13, PlayerDesignStyle.V14, PlayerDesignStyle.V15 -> GalaxSceneFamily.STUDIO
 PlayerDesignStyle.V16 -> GalaxSceneFamily.IMMERSIVE
 PlayerDesignStyle.V17 -> GalaxSceneFamily.VINJAX
}
