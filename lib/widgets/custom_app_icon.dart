import 'dart:math' as math;
import 'package:flutter/material.dart';

class CustomAppIcon extends StatelessWidget {
  final String appId;
  final double size;
  final Color? customBgColor;

  const CustomAppIcon({
    Key? key,
    required this.appId,
    this.size = 48,
    this.customBgColor,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: customBgColor ?? _getBgColor(appId),
        borderRadius: BorderRadius.circular(size * 0.26),
        boxShadow: [
          BoxShadow(
            color: (customBgColor ?? _getBgColor(appId)).withOpacity(0.35),
            blurRadius: 8,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(size * 0.26),
        child: Center(
          child: _getIconWidget(appId, size * 0.58),
        ),
      ),
    );
  }

  Color _getBgColor(String id) {
    switch (id) {
      case 'whatsapp':
        return const Color(0xFF25D366);
      case 'instagram':
        return const Color(0xFFE1306C);
      case 'facebook':
        return const Color(0xFF1877F2);
      case 'gallery':
        return const Color(0xFFEA4335);
      case 'settings':
        return const Color(0xFF4B5563);
      case 'snapchat':
        return const Color(0xFFFFFC00);
      case 'tiktok':
        return const Color(0xFF0F172A);
      case 'youtube':
        return const Color(0xFFFF0000);
      case 'messenger':
        return const Color(0xFF0084FF);
      case 'telegram':
        return const Color(0xFF229ED9);
      case 'gmail':
        return const Color(0xFFEA4335);
      case 'chrome':
        return const Color(0xFF1A73E8);
      case 'gpay':
        return const Color(0xFF1A73E8);
      case 'camera':
        return const Color(0xFF374151);
      case 'app_lock':
      default:
        return const Color(0xFF0284C7);
    }
  }

  Widget _getIconWidget(String id, double iconSize) {
    switch (id) {
      case 'whatsapp':
        return Icon(Icons.chat_bubble_rounded, color: Colors.white, size: iconSize);
      case 'instagram':
        return Icon(Icons.camera_alt_rounded, color: Colors.white, size: iconSize);
      case 'facebook':
        return Icon(Icons.facebook_rounded, color: Colors.white, size: iconSize);
      case 'gallery':
        return Icon(Icons.photo_library_rounded, color: Colors.white, size: iconSize);
      case 'settings':
        return Icon(Icons.settings_rounded, color: Colors.white, size: iconSize);
      case 'snapchat':
        return Icon(Icons.flash_on_rounded, color: Colors.black87, size: iconSize);
      case 'tiktok':
        return Icon(Icons.music_note_rounded, color: const Color(0xFF00F2FE), size: iconSize);
      case 'youtube':
        return Icon(Icons.play_arrow_rounded, color: Colors.white, size: iconSize);
      case 'messenger':
        return Icon(Icons.send_rounded, color: Colors.white, size: iconSize);
      case 'telegram':
        return Icon(Icons.near_me_rounded, color: Colors.white, size: iconSize);
      case 'gmail':
        return Icon(Icons.mail_rounded, color: Colors.white, size: iconSize);
      case 'chrome':
        return Icon(Icons.public_rounded, color: Colors.white, size: iconSize);
      case 'gpay':
        return Icon(Icons.account_balance_wallet_rounded, color: Colors.white, size: iconSize);
      case 'camera':
        return Icon(Icons.camera_rounded, color: Colors.white, size: iconSize);
      case 'app_lock':
      default:
        return Icon(Icons.lock_rounded, color: Colors.white, size: iconSize);
    }
  }
}

/// The Official App Lock Fingerprint Logo Widget (Matches the Play Store App Icon)
class BrandAppLockLogo extends StatelessWidget {
  final double size;
  final bool showGlow;

  const BrandAppLockLogo({
    Key? key,
    this.size = 110,
    this.showGlow = true,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(size * 0.28),
        gradient: const LinearGradient(
          colors: [Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF1D4ED8)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: showGlow
            ? [
                BoxShadow(
                  color: const Color(0xFF0284C7).withOpacity(0.55),
                  blurRadius: 24,
                  spreadRadius: 2,
                  offset: const Offset(0, 8),
                ),
                BoxShadow(
                  color: const Color(0xFF38BDF8).withOpacity(0.3),
                  blurRadius: 12,
                  spreadRadius: -2,
                  offset: const Offset(0, -2),
                ),
              ]
            : null,
      ),
      child: Center(
        child: CustomPaint(
          size: Size(size * 0.65, size * 0.65),
          painter: LockFingerprintPainter(),
        ),
      ),
    );
  }
}

/// Custom vector painter that renders a modern Padlock containing concentric Fingerprint biometric ridges
class LockFingerprintPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;

    // Padlock Shackle
    final shacklePaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = w * 0.09
      ..strokeCap = StrokeCap.round;

    final shackleRect = Rect.fromCenter(
      center: Offset(w * 0.5, h * 0.36),
      width: w * 0.44,
      height: h * 0.44,
    );
    canvas.drawArc(shackleRect, math.pi, math.pi, false, shacklePaint);

    // Padlock Body
    final bodyPaint = Paint()
      ..shader = const LinearGradient(
        colors: [Color(0xFFFFFFFF), Color(0xFFE2E8F0)],
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
      ).createShader(Rect.fromLTWH(w * 0.14, h * 0.36, w * 0.72, h * 0.58))
      ..style = PaintingStyle.fill;

    final bodyRRect = RRect.fromRectAndRadius(
      Rect.fromLTWH(w * 0.14, h * 0.36, w * 0.72, h * 0.56),
      Radius.circular(w * 0.18),
    );
    canvas.drawRRect(bodyRRect, bodyPaint);

    // Center Inner Screen for Fingerprint
    final innerScreenPaint = Paint()
      ..color = const Color(0xFF0F172A)
      ..style = PaintingStyle.fill;

    final innerScreenRRect = RRect.fromRectAndRadius(
      Rect.fromLTWH(w * 0.25, h * 0.46, w * 0.50, h * 0.38),
      Radius.circular(w * 0.12),
    );
    canvas.drawRRect(innerScreenRRect, innerScreenPaint);

    // Cyan Fingerprint Ridges Inside Lock
    final fpPaint = Paint()
      ..color = const Color(0xFF00E5FF)
      ..style = PaintingStyle.stroke
      ..strokeWidth = w * 0.038
      ..strokeCap = StrokeCap.round;

    final centerX = w * 0.5;
    final centerY = h * 0.65;

    // Inner ridge
    canvas.drawArc(
      Rect.fromCenter(center: Offset(centerX, centerY), width: w * 0.12, height: h * 0.14),
      -math.pi * 0.8,
      math.pi * 1.6,
      false,
      fpPaint,
    );

    // Middle ridge
    canvas.drawArc(
      Rect.fromCenter(center: Offset(centerX, centerY), width: w * 0.24, height: h * 0.22),
      -math.pi * 0.85,
      math.pi * 1.7,
      false,
      fpPaint,
    );

    // Outer ridge
    canvas.drawArc(
      Rect.fromCenter(center: Offset(centerX, centerY), width: w * 0.36, height: h * 0.28),
      -math.pi * 0.75,
      math.pi * 1.5,
      false,
      fpPaint,
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
