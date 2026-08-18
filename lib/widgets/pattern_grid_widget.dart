import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../theme/app_colors.dart';

class PatternGridWidget extends StatefulWidget {
  final ValueChanged<List<int>> onPatternComplete;
  final bool isError;
  final double size;

  const PatternGridWidget({
    Key? key,
    required this.onPatternComplete,
    this.isError = false,
    this.size = 300,
  }) : super(key: key);

  @override
  State<PatternGridWidget> createState() => _PatternGridWidgetState();
}

class _PatternGridWidgetState extends State<PatternGridWidget> {
  final List<int> _selectedDots = [];
  Offset? _currentDragPosition;
  bool _isDragging = false;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: GestureDetector(
        onPanStart: (details) => _onPanStart(details.localPosition),
        onPanUpdate: (details) => _onPanUpdate(details.localPosition),
        onPanEnd: (details) => _onPanEnd(),
        child: CustomPaint(
          size: Size(widget.size, widget.size),
          painter: _PatternPainter(
            selectedDots: _selectedDots,
            currentDragPos: _currentDragPosition,
            isDragging: _isDragging,
            isError: widget.isError,
          ),
        ),
      ),
    );
  }

  void _onPanStart(Offset localPos) {
    setState(() {
      _selectedDots.clear();
      _isDragging = true;
      _currentDragPosition = localPos;
    });

    final dotIndex = _getNearestDot(localPos);
    if (dotIndex != null && !_selectedDots.contains(dotIndex)) {
      _selectedDots.add(dotIndex);
      HapticFeedback.mediumImpact();
      setState(() {});
    }
  }

  void _onPanUpdate(Offset localPos) {
    setState(() {
      _currentDragPosition = localPos;
    });

    final dotIndex = _getNearestDot(localPos);
    if (dotIndex != null && !_selectedDots.contains(dotIndex)) {
      _selectedDots.add(dotIndex);
      HapticFeedback.lightImpact();
      setState(() {});
    }
  }

  void _onPanEnd() {
    setState(() {
      _isDragging = false;
      _currentDragPosition = null;
    });

    if (_selectedDots.length >= 3) {
      widget.onPatternComplete(List.from(_selectedDots));
    } else if (_selectedDots.isNotEmpty) {
      // Too short
      setState(() {
        _selectedDots.clear();
      });
    }
  }

  int? _getNearestDot(Offset pos) {
    final double spacing = widget.size / 4;
    const double hitRadius = 36.0;

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        final dotCenter = Offset(
          spacing * (col + 1),
          spacing * (row + 1),
        );
        final distance = (pos - dotCenter).distance;
        if (distance <= hitRadius) {
          return row * 3 + col;
        }
      }
    }
    return null;
  }
}

class _PatternPainter extends CustomPainter {
  final List<int> selectedDots;
  final Offset? currentDragPos;
  final bool isDragging;
  final bool isError;

  _PatternPainter({
    required this.selectedDots,
    required this.currentDragPos,
    required this.isDragging,
    required this.isError,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final double spacing = size.width / 4;
    final List<Offset> dotCenters = [];

    // Calculate centers of 9 dots
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        dotCenters.add(Offset(
          spacing * (col + 1),
          spacing * (row + 1),
        ));
      }
    }

    final Color primaryColor = isError ? AppColors.patternError : AppColors.patternDotActive;
    final Color lineColor = isError ? AppColors.patternError.withOpacity(0.8) : AppColors.patternLine;

    // Draw connecting lines between selected dots
    if (selectedDots.length > 1) {
      final linePaint = Paint()
        ..color = lineColor
        ..strokeWidth = 6.0
        ..strokeCap = StrokeCap.round
        ..style = PaintingStyle.stroke;

      final path = Path();
      path.moveTo(dotCenters[selectedDots[0]].dx, dotCenters[selectedDots[0]].dy);
      for (int i = 1; i < selectedDots.length; i++) {
        final pt = dotCenters[selectedDots[i]];
        path.lineTo(pt.dx, pt.dy);
      }
      canvas.drawPath(path, linePaint);
    }

    // Draw line from last selected dot to current dragging touch point
    if (isDragging && selectedDots.isNotEmpty && currentDragPos != null) {
      final dragLinePaint = Paint()
        ..color = lineColor.withOpacity(0.5)
        ..strokeWidth = 4.0
        ..strokeCap = StrokeCap.round;

      final lastDotPos = dotCenters[selectedDots.last()];
      canvas.drawLine(lastDotPos, currentDragPos!, dragLinePaint);
    }

    // Draw the 9 dots
    for (int i = 0; i < 9; i++) {
      final center = dotCenters[i];
      final isSelected = selectedDots.contains(i);

      if (isSelected) {
        // Outer glowing ring
        final outerPaint = Paint()
          ..color = primaryColor.withOpacity(0.25)
          ..style = PaintingStyle.fill;
        canvas.drawCircle(center, 28, outerPaint);

        final outerBorderPaint = Paint()
          ..color = primaryColor.withOpacity(0.8)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2;
        canvas.drawCircle(center, 28, outerBorderPaint);

        // Inner solid dot
        final innerPaint = Paint()
          ..color = primaryColor
          ..style = PaintingStyle.fill;
        canvas.drawCircle(center, 10, innerPaint);
      } else {
        // Normal dot
        final dotPaint = Paint()
          ..color = AppColors.patternDotNormal.withOpacity(0.5)
          ..style = PaintingStyle.fill;
        canvas.drawCircle(center, 8, dotPaint);

        // Subtle outer boundary
        final ringPaint = Paint()
          ..color = Colors.white.withOpacity(0.08)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.5;
        canvas.drawCircle(center, 18, ringPaint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant _PatternPainter oldDelegate) {
    return true;
  }
}
