package delivery.shared.api

object apiNameOf:

  def apply(className: String): String =
    APIMessage.apiNameFromClassName(className)

end apiNameOf
