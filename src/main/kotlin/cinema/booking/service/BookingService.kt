package cinema.booking.service

import cinema.booking.data.DatabaseManager
import cinema.booking.model.*

class BookingService(
    private val db: DatabaseManager,
    private val discountService: DiscountService
) {

    // Requirement a — Film Search
    fun getAllFilms(): List<Film> = db.getAllFilms()

    fun searchFilms(query: String): List<Film> =
        if (query.isBlank()) db.getAllFilms() else db.searchFilms(query)

    // Requirement b — Screening Display
    fun getScreeningsForFilm(film: Film): List<Screening> =
        db.getScreeningsForFilm(film)

    // Requirement c — Seat Selection
    fun getAvailableSeats(screeningId: Long): List<Seat> =
        db.getSeatsForScreening(screeningId)

    // Price calculation
    fun calculatePrice(film: Film, screening: Screening, seatCount: Int): DiscountResult =
        discountService.calculateTotal(film, screening, seatCount)

    fun calculateDiscountedTotal(film: Film, screening: Screening, seatCount: Int): Double =
        discountService.calculateTotal(film, screening, seatCount).finalPrice

    // Requirement d — Ticket Purchase
    fun purchaseTickets(
        film: Film,
        screening: Screening,
        selectedSeats: List<Seat>,
        customerName: String,
        payment: Double
    ): BookingResult {
        if (selectedSeats.isEmpty())
            return BookingResult.Failure("No seats selected.")

        val result = discountService.calculateTotal(film, screening, selectedSeats.size)

        if (payment < result.finalPrice)
            return BookingResult.Failure(
                "Insufficient funds. Required: £${"%.2f".format(result.finalPrice)}, " +
                        "provided: £${"%.2f".format(payment)}"
            )

        val seatIds = selectedSeats.mapNotNull { it.id }
        db.bookSeats(screening.id!!, seatIds, customerName, result.finalPrice)
        db.updateScreeningTakings(screening.id!!, result.finalPrice, selectedSeats.size)

        return BookingResult.Success(
            Booking(
                customer    = Customer(customerName),
                screening   = screening,
                seatsBooked = selectedSeats.toMutableList(),
                totalCost   = result.finalPrice
            ).also {
                it.morningDiscountApplied = result.morningDiscountApplied
                it.groupDiscountApplied   = result.groupDiscountApplied
            }
        )
    }

    fun bookSeats(
        screeningId: Long,
        seats: List<Seat>,
        customerName: String,
        totalCost: Double
    ): Boolean {
        return try {
            val seatIds = seats.mapNotNull { it.id }
            db.bookSeats(screeningId, seatIds, customerName, totalCost)
            db.updateScreeningTakings(screeningId, totalCost, seats.size)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

sealed class BookingResult {
    data class Success(val booking: Booking) : BookingResult()
    data class Failure(val reason: String)   : BookingResult()
}