import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../theme/app_colors.dart';

class PinKeypad extends StatelessWidget {
  final String currentPin;
  final int pinLength;
  final bool showBiometric;
  final ValueChanged<String> onDigitEntered;
  final VoidCallback onDelete;
  final VoidCallback? onBiometricClick;

  const PinKeypad({
    Key? key,
    required this.currentPin,
    this.pinLength = 4,
    this.showBiometric = true,
    required this.onDigitEntered,
    required this.onDelete,
    this.onBiometricClick,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        // PIN Dot Indicators
        _buildPinDots(),
        const SizedBox(height: 36),

        // Keypad Grid
        _buildKeypadRow(['1', '2', '3']),
        const SizedBox(height: 16),
        _buildKeypadRow(['4', '5', '6']),
        const SizedBox(height: 16),
        _buildKeypadRow(['7', '8', '9']),
        const SizedBox(height: 16),
        _buildBottomRow(),
      ],
    );
  }

  Widget _buildPinDots() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(pinLength, (index) {
        final isFilled = index < currentPin.length;
        return AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOutBack,
          margin: const EdgeInsets.symmetric(horizontal: 10),
          width: isFilled ? 18 : 14,
          height: isFilled ? 18 : 14,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: isFilled ? AppColors.pinDotActive : Colors.transparent,
            border: Border.all(
              color: isFilled ? AppColors.pinDotActive : AppColors.pinDotInactive,
              width: 2,
            ),
            boxShadow: isFilled
                ? [
                    BoxShadow(
                      color: AppColors.pinDotActive.withOpacity(0.5),
                      blurRadius: 10,
                      spreadRadius: 2,
                    ),
                  ]
                : null,
          ),
        );
      }),
    );
  }

  Widget _buildKeypadRow(List<String> digits) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: digits.map((digit) {
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14),
          child: _KeypadButton(
            label: digit,
            onTap: () {
              HapticFeedback.lightImpact();
              if (currentPin.length < pinLength) {
                onDigitEntered(digit);
              }
            },
          ),
        );
      }).toList(),
    );
  }

  Widget _buildBottomRow() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        // Biometric Button
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14),
          child: showBiometric && onBiometricClick != null
              ? _KeypadIconButton(
                  icon: Icons.fingerprint_rounded,
                  iconColor: AppColors.accentCyan,
                  onTap: () {
                    HapticFeedback.mediumImpact();
                    onBiometricClick!();
                  },
                )
              : const SizedBox(width: 72, height: 72),
        ),

        // Digit '0'
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14),
          child: _KeypadButton(
            label: '0',
            onTap: () {
              HapticFeedback.lightImpact();
              if (currentPin.length < pinLength) {
                onDigitEntered('0');
              }
            },
          ),
        ),

        // Backspace Button
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14),
          child: _KeypadIconButton(
            icon: Icons.backspace_outlined,
            iconColor: AppColors.textSecondaryDark,
            onTap: () {
              HapticFeedback.lightImpact();
              onDelete();
            },
          ),
        ),
      ],
    );
  }
}

class _KeypadButton extends StatelessWidget {
  final String label;
  final VoidCallback onTap;

  const _KeypadButton({
    Key? key,
    required this.label,
    required this.onTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(36),
        splashColor: AppColors.primary.withOpacity(0.3),
        highlightColor: AppColors.primary.withOpacity(0.1),
        child: Container(
          width: 72,
          height: 72,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: AppColors.surfaceDark.withOpacity(0.8),
            border: Border.all(
              color: Colors.white.withOpacity(0.08),
              width: 1,
            ),
          ),
          child: Center(
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 26,
                fontWeight: FontWeight.w600,
                color: AppColors.textPrimaryDark,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _KeypadIconButton extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final VoidCallback onTap;

  const _KeypadIconButton({
    Key? key,
    required this.icon,
    required this.iconColor,
    required this.onTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(36),
        splashColor: AppColors.primary.withOpacity(0.3),
        child: Container(
          width: 72,
          height: 72,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: AppColors.surfaceDark.withOpacity(0.6),
            border: Border.all(
              color: Colors.white.withOpacity(0.05),
              width: 1,
            ),
          ),
          child: Center(
            child: Icon(
              icon,
              color: iconColor,
              size: 28,
            ),
          ),
        ),
      ),
    );
  }
}
