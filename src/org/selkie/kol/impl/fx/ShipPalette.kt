package wisp.perseanchronicles.telos.boats

import com.fs.starfarer.api.Global
import java.awt.Color

val defaultShipPalette: ShipPalette
    get() = if (Global.getSector()?.playerPerson?.nameString?.contains("wisp", ignoreCase = true) == true)
        ShipPalette.RED
    else ShipPalette.PLAYER

enum class ShipPalette(
    val baseNebula: Color,
    val baseSwirlyNebula: Color,
    val baseNegative: Color,
    val glowBase: Color,
    val speedGlow: Color,
    val fluxGlow: Color,
    val phaseInitial: Color,
    val phaseMain: Color,
) {
    PLAYER(
        baseNebula = Color.decode("#1972DB"),
        baseSwirlyNebula = Color.decode("#3498DB"),
        baseNegative = Color.decode("#18FE6D"),
        glowBase = Color.decode("#39A3FF"),
        speedGlow = Color.decode("#03C6FC"),
        fluxGlow = Color.decode("#FF4069"),
        phaseInitial = Color.decode("#F065FF"),
        phaseMain = Color.decode("#9648FF"),
    ),
    BLUE(
        baseNebula = Color.decode("#1d5edb"),
        baseSwirlyNebula = Color.decode("#437edb"),
        baseNegative = Color.decode("#18FE6D"),
        glowBase = Color.decode("#4181ff"),
        speedGlow = Color.decode("#049bfc"),
        fluxGlow = Color.decode("#ff4d46"),
        phaseInitial = Color.decode("#ff70df"),
        phaseMain = Color.decode("#d25eff"),
    ),
    TEAL(
        baseNebula = Color(21, 163, 182),
        baseSwirlyNebula = Color(50, 159, 175),
        baseNegative = Color(47, 254, 24),
        glowBase = Color(2, 186, 212),
        speedGlow = Color(64, 205, 225),
        fluxGlow = Color(255, 64, 105),
        phaseInitial = Color.decode("#ff2456"),
        phaseMain = Color.decode("#E00B43"),
    ),
    WHITE(
        baseNebula = Color.decode("#5489db"),
        baseSwirlyNebula = Color(255, 255, 255),
        baseNegative = Color(24, 254, 109),
        glowBase = Color.decode("#FFFFFF"),
        speedGlow = Color.decode("#FFFFFF"),
        fluxGlow = Color(255, 64, 105),
        phaseInitial = Color.decode("#bbbbbb"),
        phaseMain = Color.decode("#aefdff"),
    ),
    SEASERPENT(
        baseNebula = Color.decode("#57C9CE"),
        baseSwirlyNebula = Color.decode("#82DFE3"),
        baseNegative = Color.decode("#18FE6D"),
        glowBase = Color.decode("#79e2e3"),
        speedGlow = Color.decode("#6efffc"),
        fluxGlow = Color.decode("#FFCE6B"),
        phaseInitial = Color.decode("#8FA8E8"),
        phaseMain = Color.decode("#6787D6"),
    ),
    ORANGE(
        baseNebula = Color(255, 98, 9),
        baseSwirlyNebula = Color(25, 114, 219),
        baseNegative = Color(255, 98, 9),
        glowBase = Color(255, 98, 9),
        speedGlow = Color(255, 132, 62),
        fluxGlow = Color(255, 64, 105),
        phaseInitial = Color(240, 101, 255),
        phaseMain = Color(150, 72, 255),
    ),
    RED(
        baseNebula = Color(255, 64, 105),
        baseSwirlyNebula = Color(255, 107, 139),
        baseNegative = Color(64, 255, 197),
        glowBase = Color.decode("#A00020"),
        speedGlow = Color.decode("#FE0037"),
        fluxGlow = Color(255, 64, 105),
        phaseInitial = Color.decode("#FF6B8B"),
        phaseMain = Color.decode("#FE0037"),
    ),
}