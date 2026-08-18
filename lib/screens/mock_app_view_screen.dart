import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/app_item.dart';
import '../providers/app_lock_provider.dart';
import '../theme/app_colors.dart';
import '../widgets/custom_app_icon.dart';

class MockAppViewScreen extends StatelessWidget {
  final AppItem app;

  const MockAppViewScreen({Key? key, required this.app}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F172A),
      appBar: AppBar(
        backgroundColor: AppColors.surfaceDark,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new_rounded, color: Colors.white, size: 20),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Row(
          children: [
            CustomAppIcon(appId: app.id, size: 32),
            const SizedBox(width: 10),
            Text(
              app.name,
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: Colors.white),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.lock_outline_rounded, color: AppColors.accentCyan),
            tooltip: 'Re-lock Now',
            onPressed: () {
              final provider = Provider.of<AppLockProvider>(context, listen: false);
              provider.relockApp(app.id);
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text('${app.name} is now locked again.'),
                  duration: const Duration(seconds: 1),
                ),
              );
              Navigator.of(context).pop();
            },
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Security Banner
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.success.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.success.withOpacity(0.3)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.verified_user_rounded, color: AppColors.success, size: 28),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Unlocked via App Lock',
                            style: TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            'Protected by Biometric & PIN Security',
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.white.withOpacity(0.8),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 24),

              // Mock App Feed/Content
              _buildMockAppContent(app.id),

              const SizedBox(height: 30),

              // Re-lock Action Button
              Center(
                child: OutlinedButton.icon(
                  onPressed: () {
                    final provider = Provider.of<AppLockProvider>(context, listen: false);
                    provider.relockApp(app.id);
                    Navigator.of(context).pop();
                  },
                  icon: const Icon(Icons.lock_rounded, color: AppColors.accentCyan, size: 18),
                  label: const Text(
                    'Lock & Exit App',
                    style: TextStyle(color: AppColors.accentCyan, fontWeight: FontWeight.bold),
                  ),
                  style: OutlinedButton.styleFrom(
                    side: const BorderSide(color: AppColors.accentCyan),
                    padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMockAppContent(String id) {
    if (id == 'whatsapp' || id == 'messenger' || id == 'telegram') {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Recent Messages', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white, fontSize: 16)),
          const SizedBox(height: 12),
          _buildChatItem('Alex Johnson', 'Hey, are you coming today?', '10:42 AM', 2),
          _buildChatItem('Design Team', 'New UI wireframes uploaded.', 'Yesterday', 0),
          _buildChatItem('Family Group', 'Photos from Sunday dinner 📸', 'Aug 14', 5),
        ],
      );
    } else if (id == 'gallery') {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Private Albums', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white, fontSize: 16)),
          const SizedBox(height: 12),
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              childAspectRatio: 1.1,
            ),
            itemCount: 4,
            itemBuilder: (context, index) {
              final titles = ['Camera Roll (142)', 'Screenshots (38)', 'Vacation 2026 (94)', 'Documents (12)'];
              return Container(
                decoration: BoxDecoration(
                  color: AppColors.surfaceDark,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: Colors.white.withOpacity(0.08)),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.photo_library_rounded, size: 36, color: AppColors.accentCyan.withOpacity(0.8)),
                    const SizedBox(height: 8),
                    Text(titles[index], style: const TextStyle(fontSize: 12, color: Colors.white70)),
                  ],
                ),
              );
            },
          ),
        ],
      );
    } else {
      return Column(
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: AppColors.surfaceDark,
              borderRadius: BorderRadius.circular(18),
            ),
            child: Column(
              children: [
                const Icon(Icons.shield_rounded, size: 48, color: AppColors.primary),
                const SizedBox(height: 12),
                const Text(
                  'Application Data Protected',
                  style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white, fontSize: 16),
                ),
                const SizedBox(height: 6),
                Text(
                  'Your session is encrypted and safe. When you minimize or exit, App Lock will re-protect this application.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withOpacity(0.7), fontSize: 13),
                ),
              ],
            ),
          ),
        ],
      );
    }
  }

  Widget _buildChatItem(String name, String preview, String time, int unread) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surfaceDark,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor: AppColors.primary.withOpacity(0.2),
            child: Text(name[0], style: const TextStyle(color: AppColors.accentCyan, fontWeight: FontWeight.bold)),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(name, style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
                const SizedBox(height: 2),
                Text(preview, style: const TextStyle(color: Colors.white70, fontSize: 13), overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(time, style: const TextStyle(color: Colors.white38, fontSize: 11)),
              if (unread > 0) ...[
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.all(5),
                  decoration: const BoxDecoration(color: AppColors.primary, shape: BoxShape.circle),
                  child: Text('$unread', style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold)),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }
}
