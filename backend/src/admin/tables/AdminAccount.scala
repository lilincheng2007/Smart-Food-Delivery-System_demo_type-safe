package delivery.admin.tables

final case class AdminAccount(
    role: String,
    username: String,
    password: String,
    displayName: String
)
