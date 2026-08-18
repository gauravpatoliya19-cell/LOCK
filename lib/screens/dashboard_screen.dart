import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../models/app_item.dart';
import '../providers/app_lock_provider.dart';
import '../providers/auth_provider.dart';
import '../theme/app_colors.dart';
import '../widgets/app_tile_item.dart';
import '../widgets/lock_summary_card.dart';
import '../widgets/custom_app_icon.dart';
import 'lock_overlay_screen.dart';
import 'mock_app_view_screen.dart';
import 'settings_screen.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({Key? key}) : super(key: key);

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  final TextEditingController _searchController = TextEditingController();
  bool _isSearching = false;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final appLockProvider = Provider.of<AppLockProvider>(context);
    final authProvider = Provider.of<AuthProvider>(context);

    final apps = appLockProvider.filteredApps;

    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      appBar: _buildAppBar(context, appLockProvider),
      body: RefreshIndicator(
        onRefresh: () async {
          HapticFeedback.lightImpact();
          await Future.delayed(const Duration(milliseconds: 500));
        },
        color: AppColors.primary,
        backgroundColor: AppColors.surfaceDark,
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(
            parent: BouncingScrollPhysics(),
          ),
          slivers: [
            // Top Banner: Lock All / Status Summary
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 16),
                child: LockSummaryCard(
                  lockedCount: appLockProvider.lockedAppsCount,
                  totalCount: appLockProvider.totalAppsCount,
                  areAllLocked: appLockProvider.areAllAppsLocked,
                  onToggleAll: () {
                    HapticFeedback.mediumImpact();
                    if (appLockProvider.areAllAppsLocked) {
                      appLockProvider.unlockAllApps();
                      _showSnackBar(context, 'All apps unlocked');
                    } else {
                      appLockProvider.lockAllApps();
                      _showSnackBar(context, 'All apps are now protected!');
                    }
                  },
                ),
              ),
            ),

            // Search Bar & Filter Tabs
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  children: [
                    _buildSearchBar(appLockProvider),
                    const SizedBox(height: 16),
                    _buildCategoryChips(appLockProvider),
                    const SizedBox(height: 16),
                  ],
                ),
              ),
            ),

            // Section Header
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(22, 4, 22, 12),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      _getCategoryTitle(appLockProvider.selectedCategory),
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: AppColors.textPrimaryDark,
                      ),
                    ),
                    Text(
                      '${apps.length} apps',
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppColors.textSecondaryDark,
                      ),
                    ),
                  ],
                ),
              ),
            ),

            // App List Items
            if (apps.isEmpty)
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.all(40.0),
                  child: Center(
                    child: Column(
                      children: [
                        Icon(
                          Icons.search_off_rounded,
                          size: 54,
                          color: AppColors.textMutedDark.withOpacity(0.5),
                        ),
                        const SizedBox(height: 14),
                        const Text(
                          'No applications found',
                          style: TextStyle(
                            fontSize: 16,
                            color: AppColors.textSecondaryDark,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                sliver: SliverList(
                  delegate: SliverChildBuilderDelegate(
                    (context, index) {
                      final app = apps[index];
                      return AppTileItem(
                        app: app,
                        onToggle: (bool locked) {
                          HapticFeedback.selectionClick();
                          appLockProvider.toggleAppLock(app.id, locked);
                          _showSnackBar(
                            context,
                            locked
                                ? '${app.name} is now locked 🔒'
                                : '${app.name} unlocked 🔓',
                          );
                        },
                        onTap: () => _handleAppLaunch(context, app, appLockProvider),
                      );
                    },
                    childCount: apps.length,
                  ),
                ),
              ),

            // Bottom Spacing
            const SliverToBoxAdapter(
              child: SizedBox(height: 40),
            ),
          ],
        ),
      ),
    );
  }

  PreferredSizeWidget _buildAppBar(BuildContext context, AppLockProvider provider) {
    return AppBar(
      title: Row(
        children: [
          const BrandAppLockLogo(size: 38, showGlow: false),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: const [
              Text(
                'App Lock',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w800,
                  color: Colors.white,
                  letterSpacing: -0.3,
                ),
              ),
              Text(
                'Fingerprint & PIN',
                style: TextStyle(
                  fontSize: 11,
                  color: AppColors.accentCyan,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ],
      ),
      actions: [
        IconButton(
          icon: const Icon(Icons.settings_outlined, color: Colors.white, size: 24),
          tooltip: 'Settings',
          onPressed: () {
            HapticFeedback.lightImpact();
            Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const SettingsScreen()),
            );
          },
        ),
        const SizedBox(width: 8),
      ],
    );
  }

  Widget _buildSearchBar(AppLockProvider provider) {
    return Container(
      height: 48,
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: Colors.white.withOpacity(0.08),
        ),
      ),
      child: TextField(
        controller: _searchController,
        onChanged: (val) => provider.setSearchQuery(val),
        style: const TextStyle(color: Colors.white, fontSize: 14),
        decoration: InputDecoration(
          hintText: 'Search installed apps...',
          hintStyle: const TextStyle(color: AppColors.textMutedDark, fontSize: 14),
          prefixIcon: const Icon(Icons.search_rounded, color: AppColors.textMutedDark, size: 20),
          suffixIcon: _searchController.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear_rounded, color: AppColors.textMutedDark, size: 18),
                  onPressed: () {
                    _searchController.clear();
                    provider.setSearchQuery('');
                  },
                )
              : null,
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(vertical: 12),
        ),
      ),
    );
  }

  Widget _buildCategoryChips(AppLockProvider provider) {
    final categories = <AppCategory?>[
      null,
      AppCategory.social,
      AppCategory.media,
      AppCategory.tools,
      AppCategory.system,
    ];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      physics: const BouncingScrollPhysics(),
      child: Row(
        children: categories.map((cat) {
          final isSelected = provider.selectedCategory == cat;
          final title = _getCategoryChipTitle(cat);

          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ChoiceChip(
              label: Text(title),
              selected: isSelected,
              onSelected: (_) {
                HapticFeedback.selectionClick();
                provider.setSelectedCategory(cat);
              },
              backgroundColor: AppColors.surfaceDark,
              selectedColor: AppColors.primary,
              labelStyle: TextStyle(
                fontSize: 13,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                color: isSelected ? Colors.white : AppColors.textSecondaryDark,
              ),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
                side: BorderSide(
                  color: isSelected ? AppColors.primary : Colors.white.withOpacity(0.06),
                ),
              ),
              showCheckmark: false,
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            ),
          );
        }).toList(),
      ),
    );
  }

  String _getCategoryTitle(AppCategory? cat) {
    if (cat == null) return 'Installed Applications';
    switch (cat) {
      case AppCategory.social:
        return 'Social & Messaging Apps';
      case AppCategory.media:
        return 'Media & Photos Apps';
      case AppCategory.tools:
        return 'Tools & Utilities';
      case AppCategory.system:
        return 'System Apps';
      case AppCategory.payment:
        return 'Payment & Banking';
    }
  }

  String _getCategoryChipTitle(AppCategory? cat) {
    if (cat == null) return 'All';
    switch (cat) {
      case AppCategory.social:
        return 'Social';
      case AppCategory.media:
        return 'Media';
      case AppCategory.tools:
        return 'Tools';
      case AppCategory.system:
        return 'System';
      case AppCategory.payment:
        return 'Payment';
    }
  }

  void _handleAppLaunch(BuildContext context, AppItem app, AppLockProvider provider) {
    HapticFeedback.lightImpact();

    // If app is currently locked, trigger the realistic Lock Screen Overlay!
    if (app.isCurrentlyLocked) {
      Navigator.of(context).push(
        PageRouteBuilder(
          opaque: false,
          pageBuilder: (context, _, __) {
            return LockOverlayScreen(
              app: app,
              onUnlocked: () {
                // Grant temporary unlock session
                provider.unlockAppTemporarily(app.id);

                // Open simulated app window
                Navigator.of(context).pushReplacement(
                  MaterialPageRoute(
                    builder: (_) => MockAppViewScreen(app: app),
                  ),
                );
              },
            );
          },
        ),
      );
    } else {
      // Direct open if unlocked or unprotected
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => MockAppViewScreen(app: app),
        ),
      );
    }
  }

  void _showSnackBar(BuildContext context, String message) {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message, style: const TextStyle(fontWeight: FontWeight.w500)),
        backgroundColor: AppColors.surfaceDark,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(color: Colors.white.withOpacity(0.1)),
        ),
        duration: const Duration(seconds: 2),
      ),
    );
  }
}
