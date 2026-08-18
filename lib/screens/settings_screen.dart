import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../models/lock_type.dart';
import '../providers/auth_provider.dart';
import '../providers/settings_provider.dart';
import '../providers/app_lock_provider.dart';
import '../theme/app_colors.dart';
import 'setup_screen.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    final settingsProvider = Provider.of<SettingsProvider>(context);

    return Scaffold(
      backgroundColor: AppColors.backgroundDark,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new_rounded, color: Colors.white, size: 20),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Settings',
          style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.white),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          physics: const BouncingScrollPhysics(),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Security Section
              _buildSectionTitle('SECURITY & LOCK TYPE'),
              const SizedBox(height: 10),

              _buildSettingsCard(
                children: [
                  _buildSettingsTile(
                    icon: Icons.lock_outline_rounded,
                    title: 'Change Lock Credentials',
                    subtitle: 'Update your master PIN or Pattern',
                    onTap: () {
                      HapticFeedback.lightImpact();
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => const SetupScreen(isChangingLock: true),
                        ),
                      );
                    },
                    trailing: const Icon(Icons.chevron_right_rounded, color: AppColors.textMutedDark),
                  ),
                  const Divider(color: Colors.white10, height: 1),
                  _buildSettingsTile(
                    icon: Icons.fingerprint_rounded,
                    title: 'Fingerprint Unlock',
                    subtitle: authProvider.isBiometricSupported
                        ? 'Quick unlock using biometric sensor'
                        : 'Not supported on this hardware',
                    iconColor: AppColors.accentCyan,
                    trailing: Switch(
                      value: authProvider.isBiometricEnabled && authProvider.isBiometricSupported,
                      onChanged: authProvider.isBiometricSupported
                          ? (val) {
                              HapticFeedback.selectionClick();
                              authProvider.setBiometricEnabled(val);
                            }
                          : null,
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 24),

              // Locker Preferences Section
              _buildSectionTitle('LOCKER BEHAVIOR'),
              const SizedBox(height: 10),

              _buildSettingsCard(
                children: [
                  _buildSettingsTile(
                    icon: Icons.timer_outlined,
                    title: 'Re-lock Timeout',
                    subtitle: settingsProvider.timeoutDisplayText,
                    onTap: () => _showRelockTimeoutDialog(context, settingsProvider),
                    trailing: const Icon(Icons.chevron_right_rounded, color: AppColors.textMutedDark),
                  ),
                  const Divider(color: Colors.white10, height: 1),
                  _buildSettingsTile(
                    icon: Icons.vibration_rounded,
                    title: 'Haptic Feedback',
                    subtitle: 'Vibrate on keypad and pattern touches',
                    trailing: Switch(
                      value: settingsProvider.hapticsEnabled,
                      onChanged: (val) {
                        settingsProvider.toggleHaptics(val);
                      },
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 24),

              // Android Device Permissions Section
              _buildSectionTitle('ANDROID REAL DEVICE PERMISSIONS'),
              const SizedBox(height: 10),

              _buildSettingsCard(
                children: [
                  _buildSettingsTile(
                    icon: Icons.security_rounded,
                    title: 'Usage Access Permission',
                    subtitle: 'Enables background app launch detection',
                    iconColor: AppColors.info,
                    trailing: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: AppColors.success.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Text(
                        'Active',
                        style: TextStyle(color: AppColors.success, fontSize: 11, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                  const Divider(color: Colors.white10, height: 1),
                  _buildSettingsTile(
                    icon: Icons.layers_outlined,
                    title: 'Display Over Apps',
                    subtitle: 'Shows lock overlay over protected apps',
                    iconColor: AppColors.accentCyan,
                    trailing: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: AppColors.success.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Text(
                        'Active',
                        style: TextStyle(color: AppColors.success, fontSize: 11, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 24),

              // Danger Zone
              _buildSectionTitle('DANGER ZONE'),
              const SizedBox(height: 10),

              _buildSettingsCard(
                children: [
                  _buildSettingsTile(
                    icon: Icons.delete_forever_rounded,
                    title: 'Reset All Settings',
                    subtitle: 'Erase PIN, Pattern and unlock all applications',
                    iconColor: AppColors.error,
                    onTap: () => _showResetConfirmation(context, authProvider),
                    trailing: const Icon(Icons.chevron_right_rounded, color: AppColors.error),
                  ),
                ],
              ),

              const SizedBox(height: 32),

              // About Footer
              Center(
                child: Column(
                  children: [
                    const Text(
                      'App Lock: Fingerprint & PIN',
                      style: TextStyle(color: Colors.white70, fontSize: 14, fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Version 1.0.0 (Build 1001)',
                      style: TextStyle(color: Colors.white.withOpacity(0.4), fontSize: 12),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(
      title,
      style: const TextStyle(
        fontSize: 12,
        fontWeight: FontWeight.bold,
        color: AppColors.accentCyan,
        letterSpacing: 0.8,
      ),
    );
  }

  Widget _buildSettingsCard({required List<Widget> children}) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white.withOpacity(0.06)),
      ),
      child: Column(children: children),
    );
  }

  Widget _buildSettingsTile({
    required IconData icon,
    required String title,
    required String subtitle,
    Color iconColor = AppColors.primary,
    VoidCallback? onTap,
    Widget? trailing,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        child: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: iconColor.withOpacity(0.12),
              ),
              child: Icon(icon, color: iconColor, size: 22),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: AppColors.textPrimaryDark,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondaryDark,
                    ),
                  ),
                ],
              ),
            ),
            if (trailing != null) trailing,
          ],
        ),
      ),
    );
  }

  void _showRelockTimeoutDialog(BuildContext context, SettingsProvider provider) {
    final options = [
      {'label': 'Immediately', 'seconds': 0},
      {'label': '15 seconds', 'seconds': 15},
      {'label': '30 seconds', 'seconds': 30},
      {'label': '1 minute', 'seconds': 60},
      {'label': '5 minutes', 'seconds': 300},
    ];

    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: AppColors.surfaceDark,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          title: const Text('Re-lock Timeout', style: TextStyle(color: Colors.white, fontSize: 18)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: options.map((opt) {
              final seconds = opt['seconds'] as int;
              final isSelected = provider.relockTimeoutSeconds == seconds;

              return RadioListTile<int>(
                value: seconds,
                groupValue: provider.relockTimeoutSeconds,
                activeColor: AppColors.accentCyan,
                title: Text(opt['label'] as String, style: const TextStyle(color: Colors.white, fontSize: 14)),
                onChanged: (val) {
                  if (val != null) {
                    provider.setRelockTimeoutSeconds(val);
                    Navigator.of(ctx).pop();
                  }
                },
              );
            }).toList(),
          ),
        );
      },
    );
  }

  void _showResetConfirmation(BuildContext context, AuthProvider auth) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: AppColors.surfaceDark,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          title: const Text('Reset App Lock?', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          content: const Text(
            'This will clear your master PIN, pattern, biometric credentials, and unlock all applications.',
            style: TextStyle(color: AppColors.textSecondaryDark, fontSize: 14),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancel', style: TextStyle(color: AppColors.textSecondaryDark)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: AppColors.error),
              onPressed: () async {
                final appLockProvider = Provider.of<AppLockProvider>(context, listen: false);
                await auth.resetAll();
                await appLockProvider.resetAll();
                Navigator.of(ctx).pop();
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const SetupScreen()),
                  (route) => false,
                );
              },
              child: const Text('Reset All'),
            ),
          ],
        );
      },
    );
  }
}
