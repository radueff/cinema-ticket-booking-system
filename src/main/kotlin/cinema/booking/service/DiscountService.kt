package cinema.booking.service

import cinema.booking.model.Film
import cinema.booking.model.Screening
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class DiscountService(private val offerManager: OfferManager) {

    // AE2 method — returns DiscountResult so GUI can show which discounts applied
    fun calculateTotal(film: Film, screening: Screening, seatCount: Int): DiscountResult {
        val basePrice      = film.baseTicketPrice
        val morningApplied = isMorningDiscountApplicable(screening.startTime, screening.date)
                && offerManager.isMorningDiscountEnabled()
        val groupApplied   = seatCount > 2 && offerManager.isGroupBookingDiscountEnabled()

        val finalPrice = when {
            groupApplied   -> calculateGroupDiscount(basePrice, seatCount, morningApplied)
            morningApplied -> basePrice * 0.75 * seatCount
            else           -> basePrice * seatCount
        }
        return DiscountResult(finalPrice, morningApplied, groupApplied)
    }

    // AE1 original — kept so nothing else breaks
    fun calculateFinalTotal(screening: Screening, seatsCount: Int): Double {
        val basePrice      = screening.film.baseTicketPrice
        val morningApplied = isMorningDiscountApplicable(screening.startTime, screening.date)
                && offerManager.isMorningDiscountEnabled()
        val groupApplied   = seatsCount > 2 && offerManager.isGroupBookingDiscountEnabled()

        return if (groupApplied)
            calculateGroupDiscount(basePrice, seatsCount, morningApplied)
        else {
            val effectivePrice = if (morningApplied) basePrice * 0.75 else basePrice
            effectivePrice * seatsCount
        }
    }

    private fun isMorningDiscountApplicable(startTime: String, date: String): Boolean {
        return try {
            val time = LocalTime.parse(startTime)
            if (!time.isBefore(LocalTime.NOON)) return false
            val day = LocalDate.parse(date).dayOfWeek
            day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
        } catch (e: Exception) { false }
    }

    private fun calculateGroupDiscount(
        basePrice: Double,
        seatsCount: Int,
        morningApplied: Boolean
    ): Double {
        val effectivePrice      = if (morningApplied) basePrice * 0.75 else basePrice
        val firstTwoTickets     = 2 * effectivePrice
        val remainingTickets    = seatsCount - 2
        val discountedRemaining = remainingTickets * (effectivePrice * 0.70)
        return firstTwoTickets + discountedRemaining
    }
}

data class DiscountResult(
    val finalPrice: Double,
    val morningDiscountApplied: Boolean,
    val groupDiscountApplied: Boolean
)