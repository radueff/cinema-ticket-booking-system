package cinema.booking.model

class Screening(
    var id: Long? = null,
    val film: Film,
    val hallNumber: Int,
    val date: String,
    val startTime: String,
    val seats: MutableList<Seat>,
    var totalTakings: Double = 0.0
) {
    fun hasAvailableSeats(): Boolean = seats.any { it.isAvailable }

    fun displayScreeningInfo() {
        println("Film: ${film.title} | Hall: $hallNumber | Date: $date | StartTime: $startTime | Takings: £${"%.2f".format(totalTakings)}")
    }
}