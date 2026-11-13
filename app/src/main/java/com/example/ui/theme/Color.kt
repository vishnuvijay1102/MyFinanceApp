package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Royal Indigo & Cobalt palette (non-green base)
val PrimaryIndigo = Color(0xFF4F46E5)
val PrimaryIndigoLight = Color(0xFF6366F1)
val PrimaryIndigoDark = Color(0xFF3730A3)
val PrimaryIndigoContainer = Color(0xFFE0E7FF)
val OnPrimaryIndigoContainer = Color(0xFF1E1B4B)

// Legacy aliases kept for compatibility where semantic status is used
val EmeraldPrimary = PrimaryIndigo
val EmeraldLight = PrimaryIndigoLight
val EmeraldDark = PrimaryIndigoDark
val EmeraldContainer = PrimaryIndigoContainer
val OnEmeraldContainer = OnPrimaryIndigoContainer

// High-Contrast Obsidian & Ink
val ObsidianBlack = Color(0xFF090B10)
val CharcoalInk = Color(0xFF1E293B)

// Vibrant Contrasting Accents
val GoldAccent = Color(0xFFF59E0B)
val AmberAccent = Color(0xFFD97706)
val GoldContainer = Color(0xFFFEF3C7)
val IndigoAccent = Color(0xFF4F46E5)
val SkyAccent = Color(0xFF0EA5E9)
val VioletAccent = Color(0xFF7C3AED)
val CyanAccent = Color(0xFF06B6D4)
val RoseAccent = Color(0xFFE11D48)
val OrangeAccent = Color(0xFFEA580C)
val TealAccent = Color(0xFF0284C7)

// Semantic Status colors
val IncomeGreen = Color(0xFF0EA5E9)
val ExpenseRed = Color(0xFFF43F5E)
val WarningAmber = Color(0xFFF59E0B)
val InfoBlue = Color(0xFF3B82F6)
val PurpleCard = Color(0xFF8B5CF6)

// Gradient Stop Palettes
val HeroBrandGradient = listOf(Color(0xFF090B14), Color(0xFF1E1B4B), Color(0xFF4338CA))
val HeroEmeraldGradient = HeroBrandGradient
val AccountsIndigoGradient = listOf(Color(0xFF090B14), Color(0xFF0C4A6E), Color(0xFF0284C7))
val VioletRoseGradient = listOf(Color(0xFF5B21B6), Color(0xFF9D174D))
val AmberOrangeGradient = listOf(Color(0xFFD97706), Color(0xFFEA580C))

// Dark Theme Surfaces
val DarkBackground = Color(0xFF080B14)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

// Light Theme Surfaces
val LightBackground = Color(0xFFEEF2FF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF090B10)
val LightTextSecondary = Color(0xFF475569)
