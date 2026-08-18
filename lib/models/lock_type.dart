enum LockType {
  pin,
  pattern,
  fingerprint,
}

extension LockTypeExtension on LockType {
  String get displayName {
    switch (this) {
      case LockType.pin:
        return 'PIN Code';
      case LockType.pattern:
        return 'Pattern';
      case LockType.fingerprint:
        return 'Fingerprint & PIN';
    }
  }

  String get description {
    switch (this) {
      case LockType.pin:
        return '4–6 digit numeric code';
      case LockType.pattern:
        return 'Connect 3×3 dot pattern';
      case LockType.fingerprint:
        return 'Biometric with PIN fallback';
    }
  }
}
