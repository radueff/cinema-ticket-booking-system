package cinema.booking.service

import cinema.booking.data.DatabaseManager
import cinema.booking.model.*

class AdminService(
    private val db: DatabaseManager,
    private val offerManager: OfferManager
) {

    // Requirement e — Admin Login
    fun login(username: String, password: String): Boolean =
        db.findAdmin(username.trim(), password)

    // Requirement f — View Films & Screenings
    fun getAllFilmsWithScreenings(): List<Pair<Film, List<Screening>>> =
        db.getAllFilms().map { film -> film to db.getAllScreeningsForFilm(film.id!!) }

    // Requirement g — Add Film
    fun addFilmWithScreening(
        title: String, genre: String, basePrice: Double,
        hall: Int, date: String, time: String
    ): Film {
        require(title.isNotBlank()) { "Title must not be blank." }
        require(basePrice > 0)      { "Price must be positive." }
        require(hall > 0)           { "Hall must be positive." }
        val filmId = db.insertFilm(title.trim(), genre.trim(), basePrice)
        db.addScreening(filmId, hall, date.trim(), time.trim())
        return db.getFilmById(filmId)!!
    }

    // Requirement g — Modify Price
    fun updateFilmPrice(filmId: Long, newBasePrice: Double, priceFactor: Double = 1.0) {
        require(newBasePrice > 0) { "Price must be positive." }
        require(priceFactor > 0)  { "Factor must be positive." }
        db.updateFilmPrice(filmId, newBasePrice * priceFactor)
    }

    // Requirement h — Special Offers
    fun isMorningDiscountEnabled(): Boolean  = offerManager.isMorningDiscountEnabled()
    fun isGroupDiscountEnabled(): Boolean    = offerManager.isGroupBookingDiscountEnabled()

    fun setMorningDiscountEnabled(enabled: Boolean) =
        offerManager.setMorningDiscountEnabled(enabled)

    fun setGroupDiscountEnabled(enabled: Boolean) =
        offerManager.setGroupBookingDiscountEnabled(enabled)

    fun toggleMorningDiscount()      = offerManager.toggleMorningDiscount()
    fun toggleGroupBookingDiscount() = offerManager.toggleGroupBookingDiscount()
}