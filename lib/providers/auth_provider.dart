import 'dart:async';
import 'package:flutter/foundation.dart';
import '../models/lock_type.dart';
import '../services/storage_service.dart';
import '../services/biometric_service.dart';

class AuthProvider extends ChangeNotifier {
  LockType _lockType = LockType.pin;
  String? _savedPin;
  String? _savedPattern;
  bool _isBiometricEnabled = false;
  bool _isSetupComplete = false;
  bool _isBiometricSupported = false;

  // Lockout logic
  int _failedAttempts = 0;
  bool _isLockedOut = false;
  int _lockoutSecondsRemaining = 0;
  Timer? _lockoutTimer;

  static const int maxFailedAttempts = 5;
  static const int lockoutDurationSeconds = 30;

  // Getters
  LockType get lockType => _lockType;
  bool get isBiometricEnabled => _isBiometricEnabled;
  bool get isSetupComplete => _isSetupComplete;
  bool get isBiometricSupported => _isBiometricSupported;
  int get failedAttempts => _failedAttempts;
  bool get isLockedOut => _isLockedOut;
  int get lockoutSecondsRemaining => _lockoutSecondsRemaining;
  int get attemptsRemaining => maxFailedAttempts - _failedAttempts;

  AuthProvider() {
    _loadAuthSettings();
  }

  Future<void> _loadAuthSettings() async {
    _isSetupComplete = StorageService.isSetupComplete;
    _lockType = StorageService.lockType;
    _savedPin = StorageService.savedPin;
    _savedPattern = StorageService.savedPattern;
    _isBiometricEnabled = StorageService.isBiometricEnabled;
    _isBiometricSupported = await BiometricService.isBiometricAvailable();
    notifyListeners();
  }

  // Set up PIN
  Future<void> setupPin(String pin, {bool enableBiometric = false}) async {
    _savedPin = pin;
    _lockType = enableBiometric ? LockType.fingerprint : LockType.pin;
    _isBiometricEnabled = enableBiometric;
    _isSetupComplete = true;

    await StorageService.setSavedPin(pin);
    await StorageService.setLockType(_lockType);
    await StorageService.setBiometricEnabled(enableBiometric);
    await StorageService.setSetupComplete(true);
    notifyListeners();
  }

  // Set up Pattern
  Future<void> setupPattern(String pattern, {bool enableBiometric = false}) async {
    _savedPattern = pattern;
    _lockType = enableBiometric ? LockType.fingerprint : LockType.pattern;
    _isBiometricEnabled = enableBiometric;
    _isSetupComplete = true;

    await StorageService.setSavedPattern(pattern);
    await StorageService.setLockType(_lockType);
    await StorageService.setBiometricEnabled(enableBiometric);
    await StorageService.setSetupComplete(true);
    notifyListeners();
  }

  // Change Lock Type
  Future<void> setLockType(LockType type) async {
    _lockType = type;
    await StorageService.setLockType(type);
    notifyListeners();
  }

  // Toggle Biometric
  Future<void> setBiometricEnabled(bool enabled) async {
    _isBiometricEnabled = enabled;
    await StorageService.setBiometricEnabled(enabled);
    notifyListeners();
  }

  // Verify PIN
  bool verifyPin(String enteredPin) {
    if (_isLockedOut) return false;

    if (_savedPin != null && _savedPin == enteredPin) {
      _resetFailedAttempts();
      return true;
    } else {
      _handleFailedAttempt();
      return false;
    }
  }

  // Verify Pattern
  bool verifyPattern(String enteredPattern) {
    if (_isLockedOut) return false;

    if (_savedPattern != null && _savedPattern == enteredPattern) {
      _resetFailedAttempts();
      return true;
    } else {
      _handleFailedAttempt();
      return false;
    }
  }

  // Authenticate with Biometrics
  Future<bool> authenticateWithBiometrics({String reason = 'Authenticate to unlock'}) async {
    if (_isLockedOut) return false;

    final success = await BiometricService.authenticate(reason: reason);
    if (success) {
      _resetFailedAttempts();
    }
    return success;
  }

  void _handleFailedAttempt() {
    _failedAttempts++;
    if (_failedAttempts >= maxFailedAttempts) {
      _triggerLockout();
    }
    notifyListeners();
  }

  void _resetFailedAttempts() {
    _failedAttempts = 0;
    _isLockedOut = false;
    _lockoutTimer?.cancel();
    notifyListeners();
  }

  void _triggerLockout() {
    _isLockedOut = true;
    _lockoutSecondsRemaining = lockoutDurationSeconds;
    _lockoutTimer?.cancel();

    _lockoutTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_lockoutSecondsRemaining > 1) {
        _lockoutSecondsRemaining--;
        notifyListeners();
      } else {
        _isLockedOut = false;
        _failedAttempts = 0;
        _lockoutSecondsRemaining = 0;
        timer.cancel();
        notifyListeners();
      }
    });
  }

  Future<void> resetAll() async {
    _lockoutTimer?.cancel();
    _savedPin = null;
    _savedPattern = null;
    _isSetupComplete = false;
    _isBiometricEnabled = false;
    _failedAttempts = 0;
    _isLockedOut = false;
    await StorageService.resetAll();
    notifyListeners();
  }

  @override
  void dispose() {
    _lockoutTimer?.cancel();
    super.dispose();
  }
}
