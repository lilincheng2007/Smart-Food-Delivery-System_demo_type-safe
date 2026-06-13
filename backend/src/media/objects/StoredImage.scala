package delivery.media.objects

final case class StoredImage(id: String, scope: String, contentType: String, bytes: Array[Byte])
