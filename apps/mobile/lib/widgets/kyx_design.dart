import 'package:flutter/material.dart';

class KyXColors {
  static const bg = Color(0xFFF6F8FB);
  static const surface = Color(0xFFFFFFFF);
  static const line = Color(0xFFE5E7EB);
  static const lineSoft = Color(0xFFF1F5F9);
  static const text = Color(0xFF111827);
  static const textSecondary = Color(0xFF6B7280);
  static const textTertiary = Color(0xFF9CA3AF);
  static const primary = Color(0xFF2563EB);
  static const primaryDark = Color(0xFF1D4ED8);
  static const green = Color(0xFF059669);
  static const red = Color(0xFFDC2626);
  static const amber = Color(0xFFD97706);
  static const cyan = Color(0xFF0891B2);
  static const indigo = Color(0xFF4F46E5);
  static const slate = Color(0xFF475569);
}

class KyXText {
  static const title = TextStyle(
    fontSize: 17,
    height: 1.22,
    fontWeight: FontWeight.w700,
    color: KyXColors.text,
  );

  static const pageTitle = TextStyle(
    fontSize: 20,
    height: 1.2,
    fontWeight: FontWeight.w800,
    color: KyXColors.text,
  );

  static const body = TextStyle(
    fontSize: 15,
    height: 1.35,
    color: KyXColors.text,
  );

  static const bodyStrong = TextStyle(
    fontSize: 15,
    height: 1.35,
    fontWeight: FontWeight.w600,
    color: KyXColors.text,
  );

  static const secondary = TextStyle(
    fontSize: 13,
    height: 1.35,
    color: KyXColors.textSecondary,
  );

  static const caption = TextStyle(
    fontSize: 12,
    height: 1.3,
    color: KyXColors.textTertiary,
  );

  static const section = TextStyle(
    fontSize: 12,
    height: 1.2,
    fontWeight: FontWeight.w700,
    color: KyXColors.textSecondary,
  );
}

class KyXSectionLabel extends StatelessWidget {
  final String text;
  final EdgeInsetsGeometry padding;

  const KyXSectionLabel(
    this.text, {
    super.key,
    this.padding = const EdgeInsets.fromLTRB(16, 16, 16, 8),
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: padding,
      child: Text(text, style: KyXText.section),
    );
  }
}

class KyXListSection extends StatelessWidget {
  final List<Widget>? children;
  final Widget? child;
  final EdgeInsetsGeometry margin;

  const KyXListSection({
    super.key,
    this.children,
    this.child,
    this.margin = EdgeInsets.zero,
  }) : assert(children != null || child != null);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: margin,
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(
          top: BorderSide(color: KyXColors.line),
          bottom: BorderSide(color: KyXColors.line),
        ),
      ),
      child: child ?? Column(children: children!),
    );
  }
}

class KyXListRow extends StatelessWidget {
  final Widget? leading;
  final String title;
  final String? subtitle;
  final Widget? trailing;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;
  final bool showDivider;
  final EdgeInsetsGeometry padding;

  const KyXListRow({
    super.key,
    this.leading,
    required this.title,
    this.subtitle,
    this.trailing,
    this.onTap,
    this.onLongPress,
    this.showDivider = true,
    this.padding = const EdgeInsets.symmetric(horizontal: 16, vertical: 11),
  });

  @override
  Widget build(BuildContext context) {
    final row = InkWell(
      onTap: onTap,
      onLongPress: onLongPress,
      child: Padding(
        padding: padding,
        child: Row(
          children: [
            if (leading != null) ...[leading!, const SizedBox(width: 12)],
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: KyXText.bodyStrong,
                  ),
                  if (subtitle != null && subtitle!.isNotEmpty) ...[
                    const SizedBox(height: 3),
                    Text(
                      subtitle!,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: KyXText.secondary,
                    ),
                  ],
                ],
              ),
            ),
            if (trailing != null) ...[const SizedBox(width: 12), trailing!],
          ],
        ),
      ),
    );

    if (!showDivider) return row;
    return Column(
      children: [
        row,
        const Divider(height: 1, indent: 56, color: KyXColors.lineSoft),
      ],
    );
  }
}

class KyXAvatar extends StatelessWidget {
  final String text;
  final String? imageUrl;
  final double size;
  final Color color;

  const KyXAvatar({
    super.key,
    required this.text,
    this.imageUrl,
    this.size = 36,
    this.color = KyXColors.primary,
  });

  @override
  Widget build(BuildContext context) {
    final url = imageUrl?.trim() ?? '';
    final initial = text.trim().isNotEmpty ? text.trim().substring(0, 1) : '?';
    final uri = Uri.tryParse(url);
    final isNetwork =
        uri != null && (uri.scheme == 'http' || uri.scheme == 'https');

    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
      ),
      clipBehavior: Clip.antiAlias,
      child: isNetwork
          ? Image.network(
              url,
              width: size,
              height: size,
              fit: BoxFit.cover,
              errorBuilder: (_, __, ___) =>
                  _Initial(initial: initial, color: color),
            )
          : _Initial(initial: initial, color: color),
    );
  }
}

class _Initial extends StatelessWidget {
  final String initial;
  final Color color;

  const _Initial({required this.initial, required this.color});

  @override
  Widget build(BuildContext context) {
    return Text(
      initial,
      style: TextStyle(color: color, fontSize: 15, fontWeight: FontWeight.w700),
    );
  }
}

ButtonStyle kyxPrimaryButtonStyle({Color color = KyXColors.primary}) {
  return ElevatedButton.styleFrom(
    minimumSize: const Size.fromHeight(42),
    backgroundColor: color,
    foregroundColor: Colors.white,
    elevation: 0,
    shadowColor: Colors.transparent,
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
    textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
  );
}

ButtonStyle kyxTextButtonStyle({Color color = KyXColors.primary}) {
  return TextButton.styleFrom(
    minimumSize: const Size(40, 36),
    foregroundColor: color,
    padding: const EdgeInsets.symmetric(horizontal: 10),
    textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
  );
}

InputDecoration kyxInputDecoration({
  required String hintText,
  Widget? prefixIcon,
  Widget? suffixIcon,
}) {
  return InputDecoration(
    hintText: hintText,
    prefixIcon: prefixIcon,
    suffixIcon: suffixIcon,
    prefixIconConstraints: const BoxConstraints.tightFor(width: 42, height: 42),
    suffixIconConstraints: const BoxConstraints.tightFor(width: 42, height: 42),
    isDense: true,
    filled: true,
    fillColor: KyXColors.surface,
    hintStyle: KyXText.secondary,
    contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: const BorderSide(color: KyXColors.line),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: const BorderSide(color: KyXColors.primary, width: 1.2),
    ),
    errorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: const BorderSide(color: KyXColors.red),
    ),
    focusedErrorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: const BorderSide(color: KyXColors.red),
    ),
  );
}
