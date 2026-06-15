package cinema.booking.model

open class User(
    var id: Long? = null,
    val username: String,
    val password: String
) {
    open fun getRole(): String = "User"
}
