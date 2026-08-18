import 'package:flutter/material.dart';
import '../models/app_item.dart';
import '../services/storage_service.dart';

class AppLockProvider extends ChangeNotifier {
  List<AppItem> _apps = [];
  String _searchQuery = '';
  AppCategory? _selectedCategory;
  bool _isLoading = true;

  // Initial Sample Applications
  static final List<AppItem> _defaultApps = [
    AppItem(
      id: 'whatsapp',
      name: 'WhatsApp',
      packageName: 'com.whatsapp',
      category: AppCategory.social,
      iconBgColor: const Color(0xFF25D366),
      isLocked: true,
    ),
    AppItem(
      id: 'instagram',
      name: 'Instagram',
      packageName: 'com.instagram.android',
      category: AppCategory.social,
      iconBgColor: const Color(0xFFE1306C),
      isLocked: true,
    ),
    AppItem(
      id: 'facebook',
      name: 'Facebook',
      packageName: 'com.facebook.katana',
      category: AppCategory.social,
      iconBgColor: const Color(0xFF1877F2),
      isLocked: true,
    ),
    AppItem(
      id: 'gallery',
      name: 'Gallery & Photos',
      packageName: 'com.google.android.apps.photos',
      category: AppCategory.media,
      iconBgColor: const Color(0xFFEA4335),
      isLocked: true,
    ),
    AppItem(
      id: 'settings',
      name: 'Settings',
      packageName: 'com.android.settings',
      category: AppCategory.system,
      iconBgColor: const Color(0xFF607D8B),
      isSystemApp: true,
      isLocked: false,
    ),
    AppItem(
      id: 'snapchat',
      name: 'Snapchat',
      packageName: 'com.snapchat.android',
      category: AppCategory.social,
      iconBgColor: const Color(0xFFFFFC00),
      isLocked: false,
    ),
    AppItem(
      id: 'tiktok',
      name: 'TikTok',
      packageName: 'com.zhiliaoapp.musically',
      category: AppCategory.social,
      iconBgColor: const Color(0xFF000000),
      isLocked: false,
    ),
    AppItem(
      id: 'youtube',
      name: 'YouTube',
      packageName: 'com.google.android.youtube',
      category: AppCategory.media,
      iconBgColor: const Color(0xFFFF0000),
      isLocked: false,
    ),
    AppItem(
      id: 'messenger',
      name: 'Messenger',
      packageName: 'com.facebook.orca',
      category: AppCategory.social,
      iconBgColor: const Color(0xFF0084FF),
      isLocked: false,
    ),
    AppItem(
      id: 'telegram',
      name: 'Telegram',
      packageName: 'org.telegram.messenger',
      category: AppCategory.social,
      iconBgColor: const Color(0xFF229ED9),
      isLocked: false,
    ),
    AppItem(
      id: 'gmail',
      name: 'Gmail',
      packageName: 'com.google.android.gm',
      category: AppCategory.tools,
      iconBgColor: const Color(0xFFD93025),
      isLocked: false,
    ),
    AppItem(
      id: 'chrome',
      name: 'Chrome',
      packageName: 'com.android.chrome',
      category: AppCategory.tools,
      iconBgColor: const Color(0xFF4285F4),
      isLocked: false,
    ),
    AppItem(
      id: 'gpay',
      name: 'Google Pay',
      packageName: 'com.google.android.apps.walletnfcrel',
      category: AppCategory.payment,
      iconBgColor: const Color(0xFF1A73E8),
      isLocked: true,
    ),
    AppItem(
      id: 'camera',
      name: 'Camera',
      packageName: 'com.android.camera',
      category: AppCategory.media,
      iconBgColor: const Color(0xFF455A64),
      isSystemApp: true,
      isLocked: false,
    ),
  ];

  // Getters
  bool get isLoading => _isLoading;
  String get searchQuery => _searchQuery;
  AppCategory? get selectedCategory => _selectedCategory;

  List<AppItem> get allApps => _apps;

  List<AppItem> get filteredApps {
    return _apps.where((app) {
      final matchesSearch = app.name.toLowerCase().contains(_searchQuery.toLowerCase()) ||
          app.packageName.toLowerCase().contains(_searchQuery.toLowerCase());
      final matchesCategory = _selectedCategory == null || app.category == _selectedCategory;
      return matchesSearch && matchesCategory;
    }).toList();
  }

  int get totalAppsCount => _apps.length;
  int get lockedAppsCount => _apps.where((a) => a.isLocked).length;
  int get unlockedAppsCount => _apps.where((a) => !a.isLocked).length;
  bool get areAllAppsLocked => _apps.isNotEmpty && _apps.every((a) => a.isLocked);

  AppLockProvider() {
    _loadApps();
  }

  Future<void> _loadApps() async {
    _isLoading = true;
    notifyListeners();

    final savedLockedIds = StorageService.lockedAppIds;

    if (savedLockedIds.isNotEmpty) {
      _apps = _defaultApps.map((app) {
        return app.copyWith(isLocked: savedLockedIds.contains(app.id));
      }).toList();
    } else {
      _apps = List.from(_defaultApps);
      // Save default locked IDs
      final defaultLocked = _apps.where((a) => a.isLocked).map((a) => a.id).toSet();
      await StorageService.setLockedAppIds(defaultLocked);
    }

    _isLoading = false;
    notifyListeners();
  }

  void setSearchQuery(String query) {
    _searchQuery = query;
    notifyListeners();
  }

  void setSelectedCategory(AppCategory? category) {
    _selectedCategory = category;
    notifyListeners();
  }

  Future<void> toggleAppLock(String appId, bool lock) async {
    final index = _apps.indexWhere((a) => a.id == appId);
    if (index != -1) {
      _apps[index].isLocked = lock;
      if (!lock) {
        _apps[index].relock();
      }
      await _syncLockedAppIds();
      notifyListeners();
    }
  }

  Future<void> lockAllApps() async {
    for (var app in _apps) {
      app.isLocked = true;
    }
    await _syncLockedAppIds();
    notifyListeners();
  }

  Future<void> unlockAllApps() async {
    for (var app in _apps) {
      app.isLocked = false;
      app.relock();
    }
    await _syncLockedAppIds();
    notifyListeners();
  }

  void unlockAppTemporarily(String appId, {int? timeoutSeconds}) {
    final timeout = timeoutSeconds ?? StorageService.relockTimeoutSeconds;
    final index = _apps.indexWhere((a) => a.id == appId);
    if (index != -1) {
      _apps[index].unlockForSeconds(timeout);
      notifyListeners();
    }
  }

  void relockApp(String appId) {
    final index = _apps.indexWhere((a) => a.id == appId);
    if (index != -1) {
      _apps[index].relock();
      notifyListeners();
    }
  }

  AppItem? getAppById(String appId) {
    try {
      return _apps.firstWhere((a) => a.id == appId);
    } catch (_) {
      return null;
    }
  }

  Future<void> _syncLockedAppIds() async {
    final lockedIds = _apps.where((a) => a.isLocked).map((a) => a.id).toSet();
    await StorageService.setLockedAppIds(lockedIds);
  }

  Future<void> resetAll() async {
    _apps = List.from(_defaultApps);
    await _syncLockedAppIds();
    notifyListeners();
  }
}
