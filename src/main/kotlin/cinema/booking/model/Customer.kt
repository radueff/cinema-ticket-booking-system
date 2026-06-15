package cinema.booking.model

class Customer(
    username: String,
    password: String = "",
    val name: String = "Guest"
) : User(null, username, password) {

    override fun getRole(): String {
        return "Customer"
    }

    fun displayCustomerInfo() {
        println("Customer: $name")
    }
}