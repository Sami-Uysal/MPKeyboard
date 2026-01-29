package com.samiuysal.keyboard.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val KeyboardTypography =
        Typography(
                titleLarge =
                        TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Normal,
                                fontSize = 24.sp,
                                lineHeight = 28.sp,
                                letterSpacing = 0.sp
                        ),
                bodyLarge =
                        TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Normal,
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                                letterSpacing = 0.5.sp
                        ),
                bodyMedium =
                        TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.25.sp
                        ),
                headlineMedium =
                        TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                lineHeight = 32.sp,
                                letterSpacing = 0.sp
                        )
        )
