package cinema.booking.model

class Booking(
    val customer: Customer,
    val screening: Screening,
    val seatsBooked: MutableList<Seat>,
    var totalCost: Double
) {
    // AE2 additions — set by BookingService after discount calculation
    var morningDiscountApplied: Boolean = false
    var groupDiscountApplied: Boolean   = false

    fun printTicket(): String {
        val seatNumbers = seatsBooked.joinToString(", ") { it.seatNumber }
        return buildString {
            appendLine("*******************************")
            appendLine("       SOLENT CINEMA")
            appendLine("Film: ${screening.film.title}")
            appendLine("Date: ${screening.date}")
            appendLine("Time: ${screening.startTime}")
            appendLine("Seats: $seatNumbers")
            appendLine("Price: £${"%.2f".format(totalCost)}")
            if (morningDiscountApplied) appendLine("* Morning Discount (25% off)")
            if (groupDiscountApplied)   appendLine("* Group Booking Discount applied")
            append("*******************************")
        }
    }

    // AE1-compatible console print kept for backward compatibility
    fun printToConsole() = println(printTicket())
}