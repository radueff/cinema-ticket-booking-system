package cinema.booking.service

import cinema.booking.data.DatabaseManager

class OfferManager(private val db: DatabaseManager) {

    fun isMorningDiscountEnabled(): Boolean =
        db.getOfferEnabled("Morning Discount")

    fun isGroupBookingDiscountEnabled(): Boolean =
        db.getOfferEnabled("Group Booking Discount")

    fun toggleMorningDiscount() =
        db.toggleOffer("Morning Discount")

    fun toggleGroupBookingDiscount() =
        db.toggleOffer("Group Booking Discount")

    fun setMorningDiscountEnabled(enabled: Boolean) =
        db.setOfferEnabled("Morning Discount", enabled)

    fun setGroupBookingDiscountEnabled(enabled: Boolean) =
        db.setOfferEnabled("Group Booking Discount", enabled)
}