import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import cinema.booking.data.DatabaseManager
import cinema.booking.service.AdminService
import cinema.booking.service.BookingService
import cinema.booking.service.DiscountService
import cinema.booking.service.OfferManager
import cinema.booking.ui.screens.App

fun main() = application {
    val db             = DatabaseManager()
    db.connect()

    val offerManager    = OfferManager(db)
    val discountService = DiscountService(offerManager)
    val bookingService  = BookingService(db, discountService)
    val adminService    = AdminService(db, offerManager)

    Window(
        onCloseRequest = { db.disconnect(); exitApplication() },
        title          = "Cinema Ticket Booking System - Solent Cinema",
        state          = WindowState(width = 900.dp, height = 700.dp)
    ) {
        App(bookingService = bookingService, adminService = adminService)
    }
}