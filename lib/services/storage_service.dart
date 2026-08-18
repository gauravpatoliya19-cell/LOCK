import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/lock_type.dart';

class StorageService {
  static late SharedPreferences _prefs;

  static const String _keyIsSetupComplete = 'is_setup_complete';
  static const String _keyLockType = 'lock_type';
  static const String _keyHashedPin = 'hashed_pin';
  static const String _keyHashedPattern = 'hashed_pattern';
  static const String _keyBiometricEnabled = 'biometric_enabled';
  static const String _keyRelockTimeout = 'relock_timeout_sec';
  static const String _keyLockedAppIds = 'locked_app_ids';
  static const String _keyThemeMode = 'theme_mode';
  static const String _keyIntruderSelfie = 'intruder_selfie';

  static Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
  }

  // Setup state
  static bool get isSetupComplete => _prefs.getBool(_keyIsSetupComplete) ?? false;
  static Future<void> setSetupComplete(bool complete) async {
    await _prefs.setBool(_keyIsSetupComplete, complete);
  }

  // Lock Type
  static LockType get lockType {
    final index = _prefs.getInt(_keyLockType) ?? LockType.pin.index;
    if (index >= 0 && index < LockType.values.length) {
      return LockType.values[index];
    }
    return LockType.pin;
  }

  static Future<void> setLockType(LockType type) async {
    await _prefs.setInt(_keyLockType, type.index);
  }

  // PIN
  static String? get savedPin => _prefs.getString(_keyHashedPin);
  static Future<void> setSavedPin(String pin) async {
    await _prefs.setString(_keyHashedPin, pin);
  }

  // Pattern (represented as string like "0-1-2-5-8")
  static String? get savedPattern => _prefs.getString(_keyHashedPattern);
  static Future<void> setSavedPattern(String pattern) async {
    await _prefs.setString(_keyHashedPattern, pattern);
  }

  // Biometric
  static bool get isBiometricEnabled => _prefs.getBool(_keyBiometricEnabled) ?? false;
  static Future<void> setBiometricEnabled(bool enabled) async {
    await _prefs.setBool(_keyBiometricEnabled, enabled);
  }

  // Relock Timeout
  static int get relockTimeoutSeconds => _prefs.getInt(_keyRelockTimeout) ?? 30;
  static Future<void> setRelockTimeoutSeconds(int seconds) async {
    await _prefs.setInt(_keyRelockTimeout, seconds);
  }

  // Locked App IDs
  static Set<String> get lockedAppIds {
    final list = _prefs.getStringList(_keyLockedAppIds) ?? [];
    return list.toSet();
  }

  static Future<void> setLockedAppIds(Set<String> ids) async {
    await _prefs.setStringList(_keyLockedAppIds, ids.toList());
  }

  // Reset all
  static Future<void> resetAll() async {
    await _prefs.clear();
  }
}
