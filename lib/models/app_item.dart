import 'package:flutter/material.dart';

enum AppCategory {
  social,
  media,
  system,
  tools,
  payment,
}

class AppItem {
  final String id;
  final String name;
  final String packageName;
  final AppCategory category;
  final IconData? iconData;
  final Color iconBgColor;
  final bool isSystemApp;
  bool isLocked;
  DateTime? unlockedUntil;

  AppItem({
    required this.id,
    required this.name,
    required this.packageName,
    required this.category,
    this.iconData,
    this.iconBgColor = const Color(0xFF0284C7),
    this.isSystemApp = false,
    this.isLocked = false,
    this.unlockedUntil,
  });

  bool get isCurrentlyLocked {
    if (!isLocked) return false;
    if (unlockedUntil == null) return true;
    return DateTime.now().isAfter(unlockedUntil!);
  }

  void unlockForSeconds(int seconds) {
    unlockedUntil = DateTime.now().add(Duration(seconds: seconds));
  }

  void relock() {
    unlockedUntil = null;
  }

  AppItem copyWith({
    String? id,
    String? name,
    String? packageName,
    AppCategory? category,
    IconData? iconData,
    Color? iconBgColor,
    bool? isSystemApp,
    bool? isLocked,
    DateTime? unlockedUntil,
  }) {
    return AppItem(
      id: id ?? this.id,
      name: name ?? this.name,
      packageName: packageName ?? this.packageName,
      category: category ?? this.category,
      iconData: iconData ?? this.iconData,
      iconBgColor: iconBgColor ?? this.iconBgColor,
      isSystemApp: isSystemApp ?? this.isSystemApp,
      isLocked: isLocked ?? this.isLocked,
      unlockedUntil: unlockedUntil ?? this.unlockedUntil,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'packageName': packageName,
      'category': category.name,
      'isSystemApp': isSystemApp,
      'isLocked': isLocked,
    };
  }
}
