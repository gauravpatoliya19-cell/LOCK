import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../models/app_item.dart';
import '../models/lock_type.dart';
import '../providers/auth_provider.dart';
import '../theme/app_colors.dart';
import '../widgets/pin_keypad.dart';
import '../widgets/pattern_grid_widget.dart';
import '../widgets/custom_app_icon.dart';
import '../widgets/biometric_prompt_dialog.dart';

class LockOverlayScreen extends StatefulWidget {
  final AppItem app;
  final VoidCallback onUnlocked;

  const LockOverlayScreen({
    Key? key,
    required this.app,
    required this.onUnlocked,
  }) : super(key: key);

  @override
  State<LockOverlayScreen> createState() => _LockOverlayScreenState();
}

class _LockOverlayScreenState extends State<LockOverlayScreen> {
  String _currentPin = '';
  String? _errorMessage;
  bool _isPatternError = false;
  bool _hasPromptedBiometric = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _checkAndTriggerBiometric();
    });
  }

  void _checkAndTriggerBiometric() async {
    final auth = Provider.of<AuthProvider>(context, listen: false);
    if (auth.isBiometricEnabled && !_hasPromptedBiometric && !auth.isLockedOut) {
      _hasPromptedBiometric = true;
      final success = await auth.authenticateWithBiometrics(
        reason: 'Unlock ${widget.app.name}',
      );
      if (success && mounted) {
        _handleSuccessUnlock();
      }
    }
  }

  void _handleSuccessUnlock() {
    HapticFeedback.mediumImpact();
    widget.onUnlocked();
  }

  @override
  Widget build(BuildContext context) {
    final auth = Provider.of<AuthProvider>(context);

    return WillPopScope(
      onWillPop: () async {
        // Prevent going back to the locked app
        return true;
      },
      child: Scaffold(
        backgroundColor: AppColors.backgroundDark,
        body: Container(
          width: double.infinity,
          height: double.infinity,
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [
                AppColors.backgroundDark,
                const Color(0xFF0F172A),
                AppColors.backgroundDark,
              ],
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
            ),
          ),
          child: SafeArea(
            child: Column(
              children: [
                // Top Close / Cancel Button
                Align(
                  alignment: Alignment.topLeft,
                  child: Padding(
                    padding: const EdgeInsets.only(left: 16, top: 8),
                    child: IconButton(
                      icon: const Icon(Icons.close_rounded, color: Colors.white70, size: 28),
                      onPressed: () {
                        Navigator.of(context).pop();
                      },
                    ),
                  ),
                ),

                const SizedBox(height: 10),

                // App Locked Header
                CustomAppIcon(
                  appId: widget.app.id,
                  size: 68,
                ),
                const SizedBox(height: 14),

                Text(
                  widget.app.name,
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimaryDark,
                  ),
                ),
                const SizedBox(height: 4),

                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.lock_rounded, size: 14, color: AppColors.accentCyan),
                    const SizedBox(width: 6),
                    Text(
                      auth.lockType == LockType.pattern
                          ? 'Draw pattern to unlock'
                          : 'Enter PIN to unlock',
                      style: const TextStyle(
                        fontSize: 14,
                        color: AppColors.textSecondaryDark,
                      ),
                    ),
                  ],
                ),

                // Error Message or Lockout Warning
                if (auth.isLockedOut) ...[
                  const SizedBox(height: 16),
                  Container(
                    margin: const EdgeInsets.symmetric(horizontal: 32),
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    decoration: BoxDecoration(
                      color: AppColors.error.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: AppColors.error.withOpacity(0.3)),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.timer_outlined, color: AppColors.error, size: 20),
                        const SizedBox(width: 8),
                        Text(
                          'Locked out! Try in ${auth.lockoutSecondsRemaining}s',
                          style: const TextStyle(
                            color: AppColors.error,
                            fontWeight: FontWeight.bold,
                            fontSize: 14,
                          ),
                        ),
                      ],
                    ),
                  ),
                ] else if (_errorMessage != null) ...[
                  const SizedBox(height: 14),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppColors.error.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppColors.error.withOpacity(0.3)),
                    ),
                    child: Text(
                      _errorMessage!,
                      style: const TextStyle(
                        color: AppColors.error,
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],

                const Spacer(),

                // PIN Keypad or Pattern Grid
                if (auth.lockType == LockType.pattern) ...[
                  PatternGridWidget(
                    isError: _isPatternError,
                    onPatternComplete: (dots) {
                      if (auth.isLockedOut) return;

                      final patternStr = dots.join('-');
                      final isCorrect = auth.verifyPattern(patternStr);
                      if (isCorrect) {
                        _handleSuccessUnlock();
                      } else {
                        HapticFeedback.heavyImpact();
                        setState(() {
                          _isPatternError = true;
                          _errorMessage = 'Wrong pattern! ${auth.attemptsRemaining} attempts left';
                        });
                        Future.delayed(const Duration(milliseconds: 1000), () {
                          if (mounted) {
                            setState(() {
                              _isPatternError = false;
                            });
                          }
                        });
                      }
                    },
                  ),
                  if (auth.isBiometricEnabled) ...[
                    const SizedBox(height: 16),
                    TextButton.icon(
                      onPressed: () => _checkAndTriggerBiometric(),
                      icon: const Icon(Icons.fingerprint_rounded, color: AppColors.accentCyan),
                      label: const Text(
                        'Use Fingerprint',
                        style: TextStyle(color: AppColors.accentCyan, fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                ] else ...[
                  PinKeypad(
                    currentPin: _currentPin,
                    pinLength: 4,
                    showBiometric: auth.isBiometricEnabled,
                    onDigitEntered: (digit) {
                      if (auth.isLockedOut) return;

                      if (_currentPin.length < 4) {
                        setState(() {
                          _currentPin += digit;
                          _errorMessage = null;
                        });

                        if (_currentPin.length == 4) {
                          Future.delayed(const Duration(milliseconds: 150), () {
                            final isCorrect = auth.verifyPin(_currentPin);
                            if (isCorrect) {
                              _handleSuccessUnlock();
                            } else {
                              HapticFeedback.heavyImpact();
                              setState(() {
                                _errorMessage = 'Incorrect PIN! ${auth.attemptsRemaining} attempts left';
                                _currentPin = '';
                              });
                            }
                          });
                        }
                      }
                    },
                    onDelete: () {
                      if (_currentPin.isNotEmpty) {
                        setState(() {
                          _currentPin = _currentPin.substring(0, _currentPin.length - 1);
                        });
                      }
                    },
                    onBiometricClick: () => _checkAndTriggerBiometric(),
                  ),
                ],

                const Spacer(),
                const SizedBox(height: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
