class PremiumPlan {
  const PremiumPlan({
    required this.id,
    required this.name,
    required this.subtitle,
    required this.price,
    required this.productId,
  });

  final String id;
  final String name;
  final String subtitle;
  final String price;
  final String? productId;
}
