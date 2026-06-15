package cinema.booking.model

class Admin(
    username: String,
    password: String
) : User(null, username, password) {

    override fun getRole(): String {
        return "Administrator"
    }
}