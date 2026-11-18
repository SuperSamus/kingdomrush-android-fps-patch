package app.kingdomrush.patches

import app.reseam.patch.*
import app.reseam.patch.types.*

val kingdomRushFpsPatch = patch(KHFPSPatchStrings.fpsPatchName) {
    description(KHFPSPatchStrings.fpsPatchDescription)

    val targetFps =
        intOption(
            key = "target_fps",
            title = KHFPSPatchStrings.fpsPatchTargetTitle,
            description = KHFPSPatchStrings.fpsPatchTargetDescription,
            default = 60,
            required = false
        )

    compatibleWith("com.ironhidegames.android.kingdomrush")
    compatibleWith("com.ironhidegames.android.kingdomrushfrontiers")
    compatibleWith("com.ironhidegames.android.kingdomrushorigins")
    dependsOn(ExternalPatch("reseam-patches", "app.reseam.patches.universal.removePairip"))

    execute {
        val fps = options[targetFps]

        require(fps in 0..0x7FFF) { "Target FPS must be between 0 and 32767" }

        val fpsLow = (fps and 0xFF).toByte()
        val fpsHigh = ((fps shr 8) and 0xFF).toByte()

        for (componentName in files.components()) {
            // Only modify split_kr1_gc32.apk and split_kr1_gc64.apk (replace kr1 with kr2 or kr3 for the sequels)
            if (!componentName.contains("gc32") && !componentName.contains("gc64")) continue

            val assetPath = when {
                componentName.contains("gc32") -> "assets/gc32/all/main_utils.lua"
                componentName.contains("gc64") -> "assets/gc64/all/main_utils.lua"
                else -> continue
            }

            val data = files.component(componentName).read(assetPath) ?: continue
            replacePattern(data, fpsLow, fpsHigh)
            files.component(componentName).write(assetPath, data)
        }
    }
}

/**
 * Searches for all occurrences of the pattern:
 *   29 ?? 3C 00 58 ?? 01 80 29 ?? 1E 00
 * and replaces the 3C 00 and 1E 00 values with the given low/high bytes.
 */
private fun replacePattern(
    data: ByteArray,
    fpsLow: Byte,
    fpsHigh: Byte
) {
    val patternLength = 12

    var i = 0
    while (i <= data.size - patternLength) {
        if (matchesPattern(data, i)) {
            // Replace the first 16‑bit value
            data[i + 2] = fpsLow
            data[i + 3] = fpsHigh
            // Replace the second 16‑bit value
            data[i + 10] = fpsLow
            data[i + 11] = fpsHigh
            // Advance past this match to avoid overlapping replacements
            i += patternLength
        } else {
            i++
        }
    }
}

private fun matchesPattern(data: ByteArray, offset: Int): Boolean {
    // Pattern: 29 ?? 3C 00 58 ?? 01 80 29 ?? 1E 00
    if (data[offset] != 0x29.toByte()) return false
    // offset + 1 is wildcard
    if (data[offset + 2] != 0x3C.toByte()) return false
    if (data[offset + 3] != 0x00.toByte()) return false
    if (data[offset + 4] != 0x58.toByte()) return false
    // offset + 5 is wildcard
    if (data[offset + 6] != 0x01.toByte()) return false
    if (data[offset + 7] != 0x80.toByte()) return false
    if (data[offset + 8] != 0x29.toByte()) return false
    // offset + 9 is wildcard
    if (data[offset + 10] != 0x1E.toByte()) return false
    if (data[offset + 11] != 0x00.toByte()) return false
    return true
}
