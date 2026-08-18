import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/lock_type.dart';
import '../providers/auth_provider.dart';
import '../theme/app_colors.dart';
import '../widgets/pin_keypad.dart';
import '../widgets/pattern_grid_widget.dart';
import '../widgets/custom_app_icon.dart';
import 'dashboard_screen.dart';

enum SetupStage {
  chooseType,
  enterLock,
  confirmLock,
  enableBiometric,
  complete,
}

class SetupScreen extends StatefulWidget {
  final bool isChangingLock;

  const SetupScreen({
    Key? key,
    this.isChangingLock = false,
  }) : super(key: key);

  @override
  State<SetupScreen> createState() => _SetupScreenState();
}

class _SetupScreenState extends State<SetupScreen> {
  SetupStage _currentStage = SetupStage.chooseType;
  LockType _selectedLockType = LockType.pin;

  String _firstEntry = '';
  String _currentInput = '';
  String? _errorMessage;
  bool _isError = false;
  bool _enableBiometric = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      body: SafeArea(
        child: Column(
          children: [
            // Top App Bar / Indicator
            _buildHeader(),

            // Dynamic Step Content
            Expanded(
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 350),
                transitionBuilder: (child, animation) {
                  return FadeTransition(
                    opacity: animation,
                    child: SlideTransition(
                      position: Tween<Offset>(
                        begin: const Offset(0.05, 0),
                        end: Offset.zero,
                      ).animate(animation),
                      child: child,
                    ),
                  );
                },
                child: _buildStageContent(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          if (widget.isChangingLock || _currentStage != SetupStage.chooseType)
            IconButton(
              icon: const Icon(Icons.arrow_back_ios_new_rounded, color: Colors.white, size: 20),
              onPressed: _handleBack,
            )
          else
            const SizedBox(width: 40),

          // Steps indicator
          Row(
            children: List.generate(4, (index) {
              final activeIndex = _getStepIndex(_currentStage);
              final isPassed = index <= activeIndex;
              return AnimatedContainer(
                duration: const Duration(milliseconds: 300),
                margin: const EdgeInsets.symmetric(horizontal: 4),
                width: index == activeIndex ? 24 : 8,
                height: 8,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(4),
                  color: isPassed ? AppColors.accentCyan : AppColors.surfaceDarkLight,
                ),
              );
            }),
          ),

          const SizedBox(width: 40),
        ],
      ),
    );
  }

  int _getStepIndex(SetupStage stage) {
    switch (stage) {
      case SetupStage.chooseType:
        return 0;
      case SetupStage.enterLock:
        return 1;
      case SetupStage.confirmLock:
        return 2;
      case SetupStage.enableBiometric:
      case SetupStage.complete:
        return 3;
    }
  }

  void _handleBack() {
    setState(() {
      _errorMessage = null;
      _isError = false;
      _currentInput = '';
      if (_currentStage == SetupStage.confirmLock) {
        _currentStage = SetupStage.enterLock;
      } else if (_currentStage == SetupStage.enterLock) {
        _currentStage = SetupStage.chooseType;
      } else if (widget.isChangingLock) {
        Navigator.of(context).pop();
      }
    });
  }

  Widget _buildStageContent() {
    switch (_currentStage) {
      case SetupStage.chooseType:
        return _buildChooseTypeStage();
      case SetupStage.enterLock:
        return _buildEnterLockStage();
      case SetupStage.confirmLock:
        return _buildConfirmLockStage();
      case SetupStage.enableBiometric:
        return _buildEnableBiometricStage();
      case SetupStage.complete:
        return _buildCompleteStage();
    }
  }

  // 1. Choose PIN or Pattern
  Widget _buildChooseTypeStage() {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          const SizedBox(height: 20),
          const BrandAppLockLogo(size: 80),
          const SizedBox(height: 24),
          const Text(
            'Secure Your Apps',
            style: TextStyle(
              fontSize: 26,
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimaryDark,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Choose your preferred protection method',
            style: TextStyle(
              fontSize: 14,
              color: AppColors.textSecondaryDark,
            ),
          ),
          const SizedBox(height: 40),

          // PIN Option Card
          _buildTypeOptionCard(
            type: LockType.pin,
            title: 'PIN Code Lock',
            subtitle: 'Enter a 4-digit master security code',
            icon: Icons.dialpad_rounded,
          ),
          const SizedBox(height: 16),

          // Pattern Option Card
          _buildTypeOptionCard(
            type: LockType.pattern,
            title: 'Pattern Lock',
            subtitle: 'Draw a connect-the-dots gesture',
            icon: Icons.grid_3x3_rounded,
          ),

          const Spacer(),

          // Continue Button
          SizedBox(
            width: double.infinity,
            height: 54,
            child: ElevatedButton(
              onPressed: () {
                setState(() {
                  _currentStage = SetupStage.enterLock;
                });
              },
              child: const Text('Continue'),
            ),
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  Widget _buildTypeOptionCard({
    required LockType type,
    required String title,
    required String subtitle,
    required IconData icon,
  }) {
    final isSelected = _selectedLockType == type;

    return InkWell(
      onTap: () {
        setState(() {
          _selectedLockType = type;
        });
      },
      borderRadius: BorderRadius.circular(20),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.all(18),
        decoration: BoxDecoration(
          color: isSelected
              ? AppColors.primary.withOpacity(0.15)
              : AppColors.surfaceDark,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isSelected ? AppColors.primary : Colors.white.withOpacity(0.08),
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            Container(
              width: 50,
              height: 50,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: isSelected
                    ? AppColors.primary
                    : AppColors.surfaceDarkLight,
              ),
              child: Icon(icon, color: Colors.white, size: 26),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: AppColors.textPrimaryDark,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: const TextStyle(
                      fontSize: 13,
                      color: AppColors.textSecondaryDark,
                    ),
                  ),
                ],
              ),
            ),
            Icon(
              isSelected ? Icons.check_circle_rounded : Icons.radio_button_unchecked_rounded,
              color: isSelected ? AppColors.accentCyan : AppColors.textMutedDark,
              size: 24,
            ),
          ],
        ),
      ),
    );
  }

  // 2. Enter Lock (First Entry)
  Widget _buildEnterLockStage() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          const SizedBox(height: 16),
          Text(
            _selectedLockType == LockType.pin ? 'Create Master PIN' : 'Draw Master Pattern',
            style: const TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimaryDark,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            _selectedLockType == LockType.pin
                ? 'Enter a 4-digit code to protect your apps'
                : 'Connect at least 3 dots to set pattern',
            style: const TextStyle(
              fontSize: 14,
              color: AppColors.textSecondaryDark,
            ),
          ),
          const Spacer(),

          if (_selectedLockType == LockType.pin) ...[
            PinKeypad(
              currentPin: _currentInput,
              pinLength: 4,
              showBiometric: false,
              onDigitEntered: (digit) {
                if (_currentInput.length < 4) {
                  setState(() {
                    _currentInput += digit;
                  });
                  if (_currentInput.length == 4) {
                    Future.delayed(const Duration(milliseconds: 250), () {
                      setState(() {
                        _firstEntry = _currentInput;
                        _currentInput = '';
                        _currentStage = SetupStage.confirmLock;
                      });
                    });
                  }
                }
              },
              onDelete: () {
                if (_currentInput.isNotEmpty) {
                  setState(() {
                    _currentInput = _currentInput.substring(0, _currentInput.length - 1);
                  });
                }
              },
            ),
          ] else ...[
            PatternGridWidget(
              onPatternComplete: (dots) {
                final patternStr = dots.join('-');
                setState(() {
                  _firstEntry = patternStr;
                  _currentStage = SetupStage.confirmLock;
                });
              },
            ),
          ],

          const Spacer(),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  // 3. Confirm Lock (Verification Entry)
  Widget _buildConfirmLockStage() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          const SizedBox(height: 16),
          Text(
            _selectedLockType == LockType.pin ? 'Confirm Your PIN' : 'Confirm Your Pattern',
            style: const TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimaryDark,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            _selectedLockType == LockType.pin
                ? 'Re-enter the 4-digit code to confirm'
                : 'Redraw the pattern to confirm',
            style: const TextStyle(
              fontSize: 14,
              color: AppColors.textSecondaryDark,
            ),
          ),
          if (_errorMessage != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
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

          if (_selectedLockType == LockType.pin) ...[
            PinKeypad(
              currentPin: _currentInput,
              pinLength: 4,
              showBiometric: false,
              onDigitEntered: (digit) {
                if (_currentInput.length < 4) {
                  setState(() {
                    _currentInput += digit;
                    _errorMessage = null;
                    _isError = false;
                  });

                  if (_currentInput.length == 4) {
                    if (_currentInput == _firstEntry) {
                      _proceedAfterConfirmation();
                    } else {
                      setState(() {
                        _errorMessage = "PINs don't match! Try again.";
                        _isError = true;
                        _currentInput = '';
                      });
                    }
                  }
                }
              },
              onDelete: () {
                if (_currentInput.isNotEmpty) {
                  setState(() {
                    _currentInput = _currentInput.substring(0, _currentInput.length - 1);
                  });
                }
              },
            ),
          ] else ...[
            PatternGridWidget(
              isError: _isError,
              onPatternComplete: (dots) {
                final patternStr = dots.join('-');
                if (patternStr == _firstEntry) {
                  _proceedAfterConfirmation();
                } else {
                  setState(() {
                    _errorMessage = "Patterns don't match! Try again.";
                    _isError = true;
                  });
                }
              },
            ),
          ],

          const Spacer(),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  void _proceedAfterConfirmation() {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    if (authProvider.isBiometricSupported) {
      setState(() {
        _currentStage = SetupStage.enableBiometric;
      });
    } else {
      _finalizeSetup(enableBiometric: false);
    }
  }

  // 4. Enable Biometric Stage
  Widget _buildEnableBiometricStage() {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Spacer(),
          Container(
            width: 110,
            height: 110,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: RadialGradient(
                colors: [
                  AppColors.accentCyan.withOpacity(0.3),
                  Colors.transparent,
                ],
              ),
            ),
            child: const Center(
              child: Icon(
                Icons.fingerprint_rounded,
                size: 72,
                color: AppColors.accentCyan,
              ),
            ),
          ),
          const SizedBox(height: 28),
          const Text(
            'Enable Fingerprint Unlock',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimaryDark,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 12),
          const Text(
            'Use your device fingerprint scanner for lightning fast app unlocking alongside your PIN/Pattern.',
            style: TextStyle(
              fontSize: 14,
              color: AppColors.textSecondaryDark,
              height: 1.4,
            ),
            textAlign: TextAlign.center,
          ),
          const Spacer(),

          // Enable Button
          SizedBox(
            width: double.infinity,
            height: 54,
            child: ElevatedButton.icon(
              onPressed: () => _finalizeSetup(enableBiometric: true),
              icon: const Icon(Icons.fingerprint_rounded),
              label: const Text('Enable Fingerprint'),
            ),
          ),
          const SizedBox(height: 12),

          // Skip Button
          TextButton(
            onPressed: () => _finalizeSetup(enableBiometric: false),
            child: const Text(
              'Skip for Now',
              style: TextStyle(color: AppColors.textSecondaryDark),
            ),
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  // 5. Complete Celebration Stage
  Widget _buildCompleteStage() {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Spacer(),
          Container(
            width: 100,
            height: 100,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: AppColors.success.withOpacity(0.2),
              border: Border.all(color: AppColors.success, width: 2),
            ),
            child: const Center(
              child: Icon(
                Icons.check_circle_rounded,
                color: AppColors.success,
                size: 60,
              ),
            ),
          ),
          const SizedBox(height: 28),
          const Text(
            'Protection Active!',
            style: TextStyle(
              fontSize: 26,
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimaryDark,
            ),
          ),
          const SizedBox(height: 10),
          const Text(
            'Your security credentials have been configured successfully. Your privacy is now guarded.',
            style: TextStyle(
              fontSize: 14,
              color: AppColors.textSecondaryDark,
              height: 1.4,
            ),
            textAlign: TextAlign.center,
          ),
          const Spacer(),

          SizedBox(
            width: double.infinity,
            height: 54,
            child: ElevatedButton(
              onPressed: () {
                if (widget.isChangingLock) {
                  Navigator.of(context).pop();
                } else {
                  Navigator.of(context).pushReplacement(
                    MaterialPageRoute(builder: (_) => const DashboardScreen()),
                  );
                }
              },
              child: const Text('Go to Dashboard'),
            ),
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  Future<void> _finalizeSetup({required bool enableBiometric}) async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);

    if (_selectedLockType == LockType.pin) {
      await authProvider.setupPin(_firstEntry, enableBiometric: enableBiometric);
    } else {
      await authProvider.setupPattern(_firstEntry, enableBiometric: enableBiometric);
    }

    setState(() {
      _currentStage = SetupStage.complete;
    });
  }
}
