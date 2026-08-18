import 'package:flutter/material.dart';
import '../services/storage_service.dart';

class SettingsProvider extends ChangeNotifier {
  int _relockTimeoutSeconds = 30;
  bool _isDarkMode = true;
  bool _hapticsEnabled = true;

  int get relockTimeoutSeconds => _relockTimeoutSeconds;
  bool get isDarkMode => _isDarkMode;
  bool get hapticsEnabled => _hapticsEnabled;

  SettingsProvider() {
    _loadSettings();
  }

  void _loadSettings() {
    _relockTimeoutSeconds = StorageService.relockTimeoutSeconds;
    notifyListeners();
  }

  Future<void> setRelockTimeoutSeconds(int seconds) async {
    _relockTimeoutSeconds = seconds;
    await StorageService.setRelockTimeoutSeconds(seconds);
    notifyListeners();
  }

  void toggleTheme(bool isDark) {
    _isDarkMode = isDark;
    notifyListeners();
  }

  void toggleHaptics(bool enabled) {
    _hapticsEnabled = enabled;
    notifyListeners();
  }

  String get timeoutDisplayText {
    if (_relockTimeoutSeconds == 0) return 'Immediately';
    if (_relockTimeoutSeconds < 60) return '$_relockTimeoutSeconds seconds';
    final mins = _relockTimeoutSeconds ~/ 60;
    return '$mins minute${mins > 1 ? 's' : ''}';
  }
}
