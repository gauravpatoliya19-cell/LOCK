import 'package:flutter/material.dart';
import '../models/app_item.dart';
import '../theme/app_colors.dart';
import 'custom_app_icon.dart';

class AppTileItem extends StatelessWidget {
  final AppItem app;
  final ValueChanged<bool> onToggle;
  final VoidCallback onTap;

  const AppTileItem({
    Key? key,
    required this.app,
    required this.onToggle,
    required this.onTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: app.isLocked
            ? AppColors.surfaceDark.withOpacity(0.95)
            : AppColors.surfaceDark.withOpacity(0.5),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: app.isLocked
              ? AppColors.primary.withOpacity(0.3)
              : Colors.white.withOpacity(0.05),
          width: 1.2,
        ),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(18),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              children: [
                // Custom App Icon
                CustomAppIcon(
                  appId: app.id,
                  size: 46,
                ),
                const SizedBox(width: 14),

                // App Details
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Flexible(
                            child: Text(
                              app.name,
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                color: AppColors.textPrimaryDark,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          if (app.isSystemApp) ...[
                            const SizedBox(width: 6),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: AppColors.surfaceDarkLight,
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: const Text(
                                'SYSTEM',
                                style: TextStyle(
                                  fontSize: 9,
                                  fontWeight: FontWeight.bold,
                                  color: AppColors.textSecondaryDark,
                                ),
                              ),
                            ),
                          ],
                        ],
                      ),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          Icon(
                            app.isLocked
                                ? Icons.lock_rounded
                                : Icons.lock_open_rounded,
                            size: 13,
                            color: app.isLocked
                                ? AppColors.accentCyan
                                : AppColors.textMutedDark,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            app.isLocked ? 'Protected' : 'Tap to test launch',
                            style: TextStyle(
                              fontSize: 12,
                              color: app.isLocked
                                  ? AppColors.accentCyan
                                  : AppColors.textMutedDark,
                              fontWeight: app.isLocked ? FontWeight.w500 : FontWeight.normal,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),

                // Lock/Unlock Switch
                Switch(
                  value: app.isLocked,
                  onChanged: onToggle,
                  activeColor: Colors.white,
                  activeTrackColor: AppColors.primary,
                  inactiveThumbColor: Colors.white70,
                  inactiveTrackColor: AppColors.surfaceDarkLight,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
