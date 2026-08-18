import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

class LockSummaryCard extends StatelessWidget {
  final int lockedCount;
  final int totalCount;
  final bool areAllLocked;
  final VoidCallback onToggleAll;

  const LockSummaryCard({
    Key? key,
    required this.lockedCount,
    required this.totalCount,
    required this.areAllLocked,
    required this.onToggleAll,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final double percent = totalCount > 0 ? (lockedCount / totalCount) : 0.0;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: LinearGradient(
          colors: areAllLocked
              ? [const Color(0xFF0284C7), const Color(0xFF1E40AF)]
              : [const Color(0xFF1E293B), const Color(0xFF0F172A)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        border: Border.all(
          color: areAllLocked
              ? AppColors.primaryLight.withOpacity(0.4)
              : Colors.white.withOpacity(0.08),
          width: 1.5,
        ),
        boxShadow: [
          BoxShadow(
            color: areAllLocked
                ? const Color(0xFF0284C7).withOpacity(0.35)
                : Colors.black.withOpacity(0.2),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              // Shield Status Icon
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white.withOpacity(0.15),
                ),
                child: Center(
                  child: Icon(
                    areAllLocked ? Icons.shield_rounded : Icons.shield_outlined,
                    color: Colors.white,
                    size: 28,
                  ),
                ),
              ),
              const SizedBox(width: 16),
              // Status Text
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      areAllLocked ? 'All Apps Protected' : 'Privacy Protection',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                        letterSpacing: -0.3,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '$lockedCount of $totalCount apps currently locked',
                      style: TextStyle(
                        fontSize: 13,
                        color: Colors.white.withOpacity(0.8),
                      ),
                    ),
                  ],
                ),
              ),
              // One-Touch Lock/Unlock Action Button
              Material(
                color: Colors.transparent,
                child: InkWell(
                  onTap: onToggleAll,
                  borderRadius: BorderRadius.circular(14),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                    decoration: BoxDecoration(
                      color: areAllLocked
                          ? Colors.white.withOpacity(0.2)
                          : AppColors.primary,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(
                        color: Colors.white.withOpacity(0.2),
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          areAllLocked ? Icons.lock_open_rounded : Icons.lock_rounded,
                          color: Colors.white,
                          size: 16,
                        ),
                        const SizedBox(width: 6),
                        Text(
                          areAllLocked ? 'Unlock All' : 'Lock All',
                          style: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          // Protection Progress Bar
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              value: percent,
              minHeight: 6,
              backgroundColor: Colors.white.withOpacity(0.12),
              valueColor: AlwaysStoppedAnimation<Color>(
                areAllLocked ? AppColors.accentCyan : AppColors.primaryLight,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
