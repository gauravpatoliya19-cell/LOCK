import 'package:flutter/material.dart';

class AppColors {
  // Brand Blue Primary Palette (Matching reference app)
  static const Color primary = Color(0xFF0284C7); // Vibrant Sky/Blue
  static const Color primaryDark = Color(0xFF0369A1);
  static const Color primaryLight = Color(0xFF38BDF8);
  static const Color accentCyan = Color(0xFF06B6D4);
  static const Color accentBlue = Color(0xFF2563EB);
  
  // Gradients
  static const LinearGradient primaryGradient = LinearGradient(
    colors: [Color(0xFF0284C7), Color(0xFF2563EB)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient splashGradient = LinearGradient(
    colors: [Color(0xFF0C4A6E), Color(0xFF0284C7), Color(0xFF0369A1)],
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
  );

  static const LinearGradient cardGradient = LinearGradient(
    colors: [Color(0xFF1E293B), Color(0xFF0F172A)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient lockedCardGradient = LinearGradient(
    colors: [Color(0xFF0C4A6E), Color(0xFF1E293B)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  // Background & Surface
  static const Color backgroundDark = Color(0xFF0B1120);
  static const Color surfaceDark = Color(0xFF1E293B);
  static const Color surfaceDarkLight = Color(0xFF334155);
  static const Color cardDark = Color(0xFF1E293B);
  
  static const Color backgroundLight = Color(0xFFF8FAFC);
  static const Color surfaceLight = Color(0xFFFFFFFF);
  static const Color cardLight = Color(0xFFFFFFFF);

  // Text Colors
  static const Color textPrimaryDark = Color(0xFFF8FAFC);
  static const Color textSecondaryDark = Color(0xFF94A3B8);
  static const Color textMutedDark = Color(0xFF64748B);

  static const Color textPrimaryLight = Color(0xFF0F172A);
  static const Color textSecondaryLight = Color(0xFF64748B);
  static const Color textMutedLight = Color(0xFF94A3B8);

  // Status & Utility
  static const Color success = Color(0xFF10B981);
  static const Color error = Color(0xFFEF4444);
  static const Color warning = Color(0xFFF59E0B);
  static const Color info = Color(0xFF3B82F6);

  // Lock Components Colors
  static const Color pinDotActive = Color(0xFF38BDF8);
  static const Color pinDotInactive = Color(0xFF475569);
  static const Color patternDotActive = Color(0xFF38BDF8);
  static const Color patternDotNormal = Color(0xFF64748B);
  static const Color patternLine = Color(0xFF0284C7);
  static const Color patternError = Color(0xFFEF4444);
}
