package delivery.merchant.objects

enum ProductBundleSelectionType derives CanEqual:
  case fixed, repeatable, nonRepeatable
end ProductBundleSelectionType

object ProductBundleSelectionType:
  def fromString(value: String): Option[ProductBundleSelectionType] =
    values.find(_.toString == value.trim)

  def normalize(value: String): ProductBundleSelectionType =
    fromString(value).getOrElse(repeatable)

end ProductBundleSelectionType
