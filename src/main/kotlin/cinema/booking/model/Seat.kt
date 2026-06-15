package cinema.booking.model

class Seat(
    var id: Long? = null,
    val seatNumber: String,
    var isAvailable: Boolean = true
) {
    fun displaySeatInfo() {
        println("Seat: $seatNumber | Available: $isAvailable")
    }
}