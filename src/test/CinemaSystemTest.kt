import cinema.booking.data.DatabaseManager
import cinema.booking.model.*
import cinema.booking.service.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * JUnit 5 test suite for the Cinema Ticket Booking System — AE2.
 *
 * Each nested class covers one assessed requirement (a through i).
 * Every test method has a comment identifying:
 *   — which requirement it covers
 *   — whether it is a success or error case
 *   — which AE1 method or behaviour it mirrors
 *
 * All tests use an in-memory SQLite database so no .db file is created
 * and every test run starts from the same clean seeded state.
 *
 * IMPORTANT: Change the DatabaseManager class declaration in DatabaseManager.kt to:
 *   class DatabaseManager(private val dbUrl: String = "jdbc:sqlite:cinema.db")
 * and remove the line:  private val dbUrl = "jdbc:sqlite:cinema.db"
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CinemaSystemTest {

    private lateinit var db:              DatabaseManager
    private lateinit var offerManager:    OfferManager
    private lateinit var discountService: DiscountService
    private lateinit var bookingService:  BookingService
    private lateinit var adminService:    AdminService

    @BeforeAll
    fun setup() {
        // In-memory SQLite — no files written to disk, clean state every run
        db              = DatabaseManager("jdbc:sqlite::memory:")
        db.connect()
        offerManager    = OfferManager(db)
        discountService = DiscountService(offerManager)
        bookingService  = BookingService(db, discountService)
        adminService    = AdminService(db, offerManager)
    }

    @AfterAll
    fun teardown() {
        db.disconnect()
    }

    // =========================================================================
    // REQUIREMENT a — Film Search
    // =========================================================================

    @Nested
    @DisplayName("a) Film Search")
    inner class FilmSearchTests {

        @Test
        @DisplayName("a-01 SUCCESS: blank query returns all seeded films")
        fun blankQueryReturnsAllFilms() {
            // Requirement a: all films loaded from SQLite on startup
            // Mirrors AE1 CinemaSystem.searchFilms("") returning the full film list
            val films = bookingService.searchFilms("")
            assertTrue(films.isNotEmpty(), "Expected seeded films to be returned")
            assertTrue(films.size >= 4, "Expected at least 4 seeded films")
        }

        @Test
        @DisplayName("a-02 SUCCESS: search by title is case-insensitive")
        fun searchByTitleCaseInsensitive() {
            // Requirement a: title search must work regardless of letter case
            val results = bookingService.searchFilms("inception")
            assertTrue(
                results.any { it.title.equals("Inception", ignoreCase = true) },
                "Expected Inception to be found by lowercase title search"
            )
        }

        @Test
        @DisplayName("a-03 SUCCESS: search by genre returns matching films")
        fun searchByGenre() {
            // Requirement a: genre search must return only matching films
            val results = bookingService.searchFilms("Action")
            assertTrue(results.isNotEmpty(), "Expected at least one Action film")
            assertTrue(
                results.all { it.genre.contains("Action", ignoreCase = true) },
                "All results must match the searched genre"
            )
        }

        @Test
        @DisplayName("a-04 SUCCESS: partial title search returns a match")
        fun partialTitleSearch() {
            // Requirement a: partial matches must be returned (SQL LIKE %query%)
            val results = bookingService.searchFilms("bat")
            assertTrue(
                results.any { it.title.contains("Batman", ignoreCase = true) },
                "Partial search 'bat' should return Batman"
            )
        }

        @Test
        @DisplayName("a-05 ERROR: unrecognised query returns empty list, not an exception")
        fun unknownQueryReturnsEmpty() {
            // Requirement a: error case — unmatched query must return empty gracefully
            val results = bookingService.searchFilms("ZZZNOMATCH999")
            assertTrue(results.isEmpty(), "Unmatched query should return an empty list")
        }

        @Test
        @DisplayName("a-06 SUCCESS: every film has title, genre and a positive base price")
        fun filmDataComplete() {
            // Requirement a: displayed films must include title, genre, and base price
            val films = bookingService.searchFilms("")
            films.forEach { film ->
                assertTrue(film.title.isNotBlank(), "Film title must not be blank")
                assertTrue(film.genre.isNotBlank(), "Film genre must not be blank")
                assertTrue(film.baseTicketPrice > 0, "Base ticket price must be positive")
            }
        }
    }

    // =========================================================================
    // REQUIREMENT b — Screening Display
    // =========================================================================

    @Nested
    @DisplayName("b) Screening Display")
    inner class ScreeningDisplayTests {

        @Test
        @DisplayName("b-01 SUCCESS: selecting a film returns its screenings")
        fun filmHasScreenings() {
            // Requirement b: screenings must be shown after selecting a film
            // Mirrors AE1 CinemaSystem.getScreeningsForFilm(film)
            val film       = bookingService.searchFilms("Inception").first()
            val screenings = bookingService.getScreeningsForFilm(film)
            assertTrue(screenings.isNotEmpty(), "Inception must have at least one screening")
        }

        @Test
        @DisplayName("b-02 SUCCESS: each screening has hall number, date and start time")
        fun screeningHasRequiredFields() {
            // Requirement b: hall number, date and start time must all be present
            val film       = bookingService.searchFilms("Inception").first()
            val screenings = bookingService.getScreeningsForFilm(film)
            screenings.forEach { s ->
                assertTrue(s.hallNumber > 0, "Hall number must be positive")
                assertTrue(
                    s.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
                    "Date must be in YYYY-MM-DD format"
                )
                assertTrue(
                    s.startTime.matches(Regex("\\d{2}:\\d{2}")),
                    "Start time must be in HH:MM format"
                )
            }
        }

        @Test
        @DisplayName("b-03 SUCCESS: base ticket price is accessible via the screening")
        fun basePriceAccessibleFromScreening() {
            // Requirement b: base ticket price must be displayed on the screening
            val film      = bookingService.searchFilms("Batman").first()
            val screening = bookingService.getScreeningsForFilm(film).first()
            assertEquals(
                film.baseTicketPrice,
                screening.film.baseTicketPrice,
                "Screening film reference must carry the correct base ticket price"
            )
        }

        @Test
        @DisplayName("b-04 SUCCESS: morning screenings have start hour before 12")
        fun morningScreeningIdentified() {
            // Requirement b: morning screenings are flagged in the UI
            val film    = bookingService.searchFilms("Batman").first()
            val morning = bookingService.getScreeningsForFilm(film).filter {
                it.startTime.substringBefore(":").toIntOrNull() ?: 12 < 12
            }
            assertTrue(morning.isNotEmpty(), "Batman must have at least one morning screening")
        }

        @Test
        @DisplayName("b-05 ERROR: film with no screenings returns empty list, not exception")
        fun filmWithNoScreeningsReturnsEmpty() {
            // Requirement b: error case — film with no screenings must not throw
            val filmId = db.insertFilm("EmptyFilm", "Test", 5.0)
            val film   = db.getFilmById(filmId)!!
            val result = bookingService.getScreeningsForFilm(film)
            assertTrue(result.isEmpty(), "Film with no screenings must return empty list")
        }
    }

    // =========================================================================
    // REQUIREMENT c — Seat Selection
    // =========================================================================

    @Nested
    @DisplayName("c) Seat Selection")
    inner class SeatSelectionTests {

        @Test
        @DisplayName("c-01 SUCCESS: each screening has exactly 10 seats labelled A1-A10")
        fun tenSeatsPerScreening() {
            // Requirement c: AE1 createSeatsForScreening() produced A1..A10 per screening
            val film      = bookingService.searchFilms("Inception").first()
            val screening = bookingService.getScreeningsForFilm(film).first()
            val seats     = bookingService.getAvailableSeats(screening.id!!)
            assertEquals(10, seats.size, "Each screening must have exactly 10 seats")
            assertTrue(
                seats.all { it.seatNumber.startsWith("A") },
                "All seats must be labelled A1-A10"
            )
        }

        @Test
        @DisplayName("c-02 SUCCESS: all seats are available before any booking is made")
        fun seatsInitiallyAvailable() {
            // Requirement c: seats must show as available before any booking
            // Mirrors AE1 Seat(isAvailable = true) initial state from seedData
            val film      = bookingService.searchFilms("Titanic").first()
            val screening = bookingService.getScreeningsForFilm(film).first()
            val seats     = bookingService.getAvailableSeats(screening.id!!)
            assertTrue(seats.all { it.isAvailable },
                "All seats must be available before any booking is made")
        }

        @Test
        @DisplayName("c-03 SUCCESS: booked seat is immediately marked unavailable in SQLite")
        fun bookedSeatMarkedUnavailable() {
            // Requirement c: seat availability must be updated immediately after booking
            // Mirrors AE1: seat.isAvailable = false persisted to database
            val film      = bookingService.searchFilms("Frozen").first()
            val screening = bookingService.getScreeningsForFilm(film).first()
            val seat      = bookingService.getAvailableSeats(screening.id!!).first()

            bookingService.bookSeats(screening.id!!, listOf(seat), "TestCustomer", 10.0)

            val updated = bookingService.getAvailableSeats(screening.id!!)
                .first { it.seatNumber == seat.seatNumber }
            assertFalse(updated.isAvailable,
                "Booked seat must be marked unavailable in SQLite immediately")
        }

        @Test
        @DisplayName("c-04 ERROR: booked seat remains unavailable on reload from SQLite")
        fun bookedSeatRemainsUnavailableOnReload() {
            // Requirement c: error case — seat must stay booked after re-reading from DB
            val film      = bookingService.searchFilms("Batman").first()
            val screening = bookingService.getScreeningsForFilm(film)[1]
            val seat      = bookingService.getAvailableSeats(screening.id!!).first()

            bookingService.bookSeats(screening.id!!, listOf(seat), "Customer1", 9.50)

            val refreshed = bookingService.getAvailableSeats(screening.id!!)
                .first { it.seatNumber == seat.seatNumber }
            assertFalse(refreshed.isAvailable,
                "Seat must remain booked after reloading seat data from SQLite")
        }
    }

    // =========================================================================
    // REQUIREMENT d — Ticket Purchase
    // =========================================================================

    @Nested
    @DisplayName("d) Ticket Purchase")
    inner class TicketPurchaseTests {

        @Test
        @DisplayName("d-01 SUCCESS: payment equal to total completes the booking")
        fun exactPaymentSucceeds() {
            // Requirement d: payment >= total must produce BookingResult.Success
            val film      = bookingService.searchFilms("Inception").first()
            val screening = bookingService.getScreeningsForFilm(film).last()
            val seat      = bookingService.getAvailableSeats(screening.id!!).first()
            val price     = bookingService.calculateDiscountedTotal(film, screening, 1)

            val result = bookingService.purchaseTickets(
                film, screening, listOf(seat), "Customer", price)
            assertTrue(result is BookingResult.Success,
                "Payment equal to total must return BookingResult.Success")
        }

        @Test
        @DisplayName("d-02 ERROR: payment below total is refused")
        fun insufficientPaymentRefused() {
            // Requirement d: transaction must be refused if insufficient funds are entered
            // Mirrors AE1 "Insufficient funds" check when payment < totalCost
            val film      = bookingService.searchFilms("Inception").first()
            val screening = bookingService.getScreeningsForFilm(film).last()
            val seat      = bookingService.getAvailableSeats(screening.id!!).first()

            val result = bookingService.purchaseTickets(
                film, screening, listOf(seat), "Customer", 0.01)
            assertTrue(result is BookingResult.Failure,
                "Payment below required amount must return BookingResult.Failure")
            assertTrue(
                (result as BookingResult.Failure).reason.contains("Insufficient"),
                "Failure reason must mention Insufficient"
            )
        }

        @Test
        @DisplayName("d-03 ERROR: purchasing with zero seats selected is rejected")
        fun emptySeatSelectionRejected() {
            // Requirement d: error case — zero seats must be rejected before payment check
            val film      = bookingService.searchFilms("Inception").first()
            val screening = bookingService.getScreeningsForFilm(film).last()

            val result = bookingService.purchaseTickets(
                film, screening, emptyList(), "Customer", 999.0)
            assertTrue(result is BookingResult.Failure,
                "Purchasing with no seats must return BookingResult.Failure")
        }

        @Test
        @DisplayName("d-04 SUCCESS: ticket receipt matches the AE1 printTicket() format")
        fun ticketReceiptMatchesFormat() {
            // Requirement d: receipt must show *** border, SOLENT CINEMA, Film/Date/Time/Seats/Price
            // Verbatim from AE1 Booking.printTicket()
            val film      = Film(1L, "Batman", "Action", 9.5, 0)
            val screening = Screening(1L, film, 3, "2026-04-09", "19:00", mutableListOf(), 0.0)
            val booking   = Booking(
                customer    = Customer("TestUser"),
                screening   = screening,
                seatsBooked = mutableListOf(Seat(1L, "A1", true)),
                totalCost   = 9.50
            )
            val ticket = booking.printTicket()
            assertTrue(ticket.contains("*******************************"),
                "Ticket must have starred border")
            assertTrue(ticket.contains("SOLENT CINEMA"),
                "Ticket must contain SOLENT CINEMA")
            assertTrue(ticket.contains("Film:"),  "Ticket must contain Film label")
            assertTrue(ticket.contains("Date:"),  "Ticket must contain Date label")
            assertTrue(ticket.contains("Time:"),  "Ticket must contain Time label")
            assertTrue(ticket.contains("Seats:"), "Ticket must contain Seats label")
            assertTrue(ticket.contains("Price:"), "Ticket must contain Price label")
            assertTrue(ticket.contains("9.50"),   "Ticket must display the correct price")
        }

        @Test
        @DisplayName("d-05 SUCCESS: total takings increase in SQLite after purchase")
        fun takingsIncreasedAfterPurchase() {
            // Requirement d: total takings must be persisted in SQLite after booking
            val film      = bookingService.searchFilms("Batman").first()
            val screening = bookingService.getScreeningsForFilm(film).last()
            val seat      = bookingService.getAvailableSeats(screening.id!!).first()
            val before    = screening.totalTakings

            bookingService.purchaseTickets(film, screening, listOf(seat), "Customer", 999.0)

            val after = db.getAllScreeningsForFilm(film.id!!)
                .find { it.id == screening.id }!!.totalTakings
            assertTrue(after > before,
                "Total takings must increase in SQLite after a successful purchase")
        }
    }

    // =========================================================================
    // REQUIREMENT e — Administrator Login
    // =========================================================================

    @Nested
    @DisplayName("e) Administrator Login")
    inner class AdminLoginTests {

        @Test
        @DisplayName("e-01 SUCCESS: correct credentials (admin / Admin123) grant access")
        fun correctCredentialsGrantAccess() {
            // Requirement e: admin account stored in SQLite, verified on login
            // Mirrors AE1 CinemaSystem.login() checking admin/Admin123 from seedUsers()
            assertTrue(adminService.login("admin", "Admin123"),
                "Correct credentials must return true")
        }

        @Test
        @DisplayName("e-02 ERROR: wrong password is rejected")
        fun wrongPasswordRejected() {
            // Requirement e: error case — incorrect password must deny access
            assertFalse(adminService.login("admin", "wrongpassword"),
                "Wrong password must return false")
        }

        @Test
        @DisplayName("e-03 ERROR: unknown username is rejected")
        fun unknownUsernameRejected() {
            // Requirement e: error case — non-existent user must deny access
            assertFalse(adminService.login("unknownuser", "Admin123"),
                "Unknown username must return false")
        }

        @Test
        @DisplayName("e-04 ERROR: empty credentials are rejected")
        fun emptyCredentialsRejected() {
            // Requirement e: error case — blank input must not authenticate anyone
            assertFalse(adminService.login("", ""),
                "Empty credentials must return false")
        }

        @Test
        @DisplayName("e-05 SUCCESS: Admin.getRole() returns Administrator (verbatim from AE1)")
        fun adminGetRoleMatchesAE1() {
            // Requirement e: Admin.getRole() must match AE1 exactly
            assertEquals("Administrator", Admin("admin", "Admin123").getRole(),
                "Admin.getRole() must return Administrator")
        }

        @Test
        @DisplayName("e-06 SUCCESS: Customer.getRole() returns Customer (verbatim from AE1)")
        fun customerGetRoleMatchesAE1() {
            // Requirement e: Customer.getRole() must also match AE1
            assertEquals("Customer", Customer("user1").getRole(),
                "Customer.getRole() must return Customer")
        }
    }

    // =========================================================================
    // REQUIREMENT f — View Film and Screening Information (Admin)
    // =========================================================================

    @Nested
    @DisplayName("f) View Film and Screening Information")
    inner class AdminViewTests {

        @Test
        @DisplayName("f-01 SUCCESS: admin retrieves all films from the system")
        fun getAllFilmsReturnsData() {
            // Requirement f: admin must see all films stored in SQLite
            // Mirrors AE1 CinemaSystem.viewFilmAndScreeningInformation()
            val data = adminService.getAllFilmsWithScreenings()
            assertTrue(data.isNotEmpty(), "Admin view must return at least one film")
        }

        @Test
        @DisplayName("f-02 SUCCESS: film records include genre and base ticket price")
        fun filmRecordIncludesGenreAndPrice() {
            // Requirement f: genre and base ticket price must be visible in admin view
            val data = adminService.getAllFilmsWithScreenings()
            data.forEach { (film, _) ->
                assertTrue(film.genre.isNotBlank(), "Genre must not be blank")
                assertTrue(film.baseTicketPrice > 0, "Base ticket price must be positive")
            }
        }

        @Test
        @DisplayName("f-03 SUCCESS: each seeded film has at least one associated screening")
        fun eachFilmHasScreenings() {
            // Requirement f: associated screenings must be shown for each film
            val seededTitles = listOf("Inception", "Titanic", "Batman", "Frozen")
            val data = adminService.getAllFilmsWithScreenings()
                .filter { it.first.title in seededTitles }
            data.forEach { (film, screenings) ->
                assertTrue(screenings.isNotEmpty(),
                    "${film.title} must have at least one screening in admin view")
            }
        }

        @Test
        @DisplayName("f-04 SUCCESS: total tickets sold is accessible for each film")
        fun totalTicketsSoldAccessible() {
            // Requirement f: total tickets sold must be available in admin view
            val data = adminService.getAllFilmsWithScreenings()
            data.forEach { (film, _) ->
                assertTrue(film.totalTicketsSold >= 0,
                    "totalTicketsSold must be a non-negative integer")
            }
        }
    }

    // =========================================================================
    // REQUIREMENT g — Film and Pricing Management (Admin)
    // =========================================================================

    @Nested
    @DisplayName("g) Film and Pricing Management")
    inner class PricingManagementTests {

        @Test
        @DisplayName("g-01 SUCCESS: adding a new film increases the total film count")
        fun addNewFilmPersists() {
            // Requirement g: admin must be able to add a new film with at least one screening
            // Mirrors AE1 CinemaSystem.addNewFilmAndScreening()
            val countBefore = bookingService.searchFilms("").size
            adminService.addFilmWithScreening(
                "Avengers", "Action", 11.50, 5, "2026-07-01", "20:00")
            val countAfter = bookingService.searchFilms("").size
            assertEquals(countBefore + 1, countAfter,
                "Film count must increase by 1 after adding a new film")
        }

        @Test
        @DisplayName("g-02 SUCCESS: new film's screening automatically gets 10 seats A1-A10")
        fun newFilmHasTenSeats() {
            // Requirement g: createSeatsForScreening() must run for every new screening
            adminService.addFilmWithScreening(
                "SeatTestFilm", "Drama", 8.0, 2, "2026-08-01", "18:00")
            val film      = bookingService.searchFilms("SeatTestFilm").first()
            val screening = bookingService.getScreeningsForFilm(film).first()
            val seats     = bookingService.getAvailableSeats(screening.id!!)
            assertEquals(10, seats.size,
                "New film's screening must have exactly 10 seats")
        }

        @Test
        @DisplayName("g-03 SUCCESS: admin can update a film's base ticket price")
        fun updateFilmPriceChangesBasePrice() {
            // Requirement g: admin must be able to modify ticket prices
            // Mirrors AE1 CinemaSystem.modifyTicketPricing()
            val film = bookingService.searchFilms("Inception").first()
            adminService.updateFilmPrice(film.id!!, 15.0, 1.0)
            val updated = db.getFilmById(film.id!!)!!
            assertEquals(15.0, updated.baseTicketPrice, 0.001,
                "Base ticket price must update to the new value in SQLite")
        }

        @Test
        @DisplayName("g-04 SUCCESS: price factor above 1.0 increases the stored price")
        fun priceFactorIncreasesPrice() {
            // Requirement g: factor may increase prices — 1.20 on 10.00 gives 12.00
            val film = bookingService.searchFilms("Titanic").first()
            adminService.updateFilmPrice(film.id!!, 10.0, 1.20)
            val updated = db.getFilmById(film.id!!)!!
            assertEquals(12.0, updated.baseTicketPrice, 0.001,
                "Price factor 1.20 on 10.00 must store 12.00")
        }

        @Test
        @DisplayName("g-05 SUCCESS: price factor below 1.0 decreases the stored price")
        fun priceFactorDecreasesPrice() {
            // Requirement g: factor may also decrease prices — 0.80 on 10.00 gives 8.00
            val film = bookingService.searchFilms("Batman").first()
            adminService.updateFilmPrice(film.id!!, 10.0, 0.80)
            val updated = db.getFilmById(film.id!!)!!
            assertEquals(8.0, updated.baseTicketPrice, 0.001,
                "Price factor 0.80 on 10.00 must reduce price to 8.00")
        }
    }

    // =========================================================================
    // REQUIREMENT h — Special Offer Management (Admin)
    // =========================================================================

    @Nested
    @DisplayName("h) Special Offer Management")
    inner class SpecialOfferManagementTests {

        @Test
        @DisplayName("h-01 SUCCESS: both offers are enabled by default after seeding")
        fun bothOffersEnabledByDefault() {
            // Requirement h: both offers must be stored and enabled in SQLite on startup
            // Mirrors AE1 seedOffers() enabling both MorningDiscount and GroupBookingDiscount
            assertTrue(adminService.isMorningDiscountEnabled(),
                "Morning Discount must be enabled by default")
            assertTrue(adminService.isGroupDiscountEnabled(),
                "Group Booking Discount must be enabled by default")
        }

        @Test
        @DisplayName("h-02 SUCCESS: disabling Morning Discount persists in SQLite")
        fun disableMorningDiscountPersists() {
            // Requirement h: offer state changes must persist in SQLite
            adminService.setMorningDiscountEnabled(false)
            assertFalse(adminService.isMorningDiscountEnabled(),
                "Morning Discount must be disabled after setMorningDiscountEnabled(false)")
            adminService.setMorningDiscountEnabled(true) // restore for other tests
        }

        @Test
        @DisplayName("h-03 SUCCESS: disabling Group Booking Discount persists in SQLite")
        fun disableGroupDiscountPersists() {
            // Requirement h: group discount disable must persist in SQLite
            adminService.setGroupDiscountEnabled(false)
            assertFalse(adminService.isGroupDiscountEnabled(),
                "Group Discount must be disabled after setGroupDiscountEnabled(false)")
            adminService.setGroupDiscountEnabled(true) // restore for other tests
        }

        @Test
        @DisplayName("h-04 SUCCESS: re-enabling an offer restores it to the active state")
        fun reEnablingOfferRestoresEffect() {
            // Requirement h: toggling off then on must fully restore the offer
            adminService.setMorningDiscountEnabled(false)
            assertFalse(adminService.isMorningDiscountEnabled())
            adminService.setMorningDiscountEnabled(true)
            assertTrue(adminService.isMorningDiscountEnabled(),
                "Morning Discount must be active again after re-enabling")
        }
    }

    // =========================================================================
    // REQUIREMENT i — Automatic Offer Application and Data Persistence
    // =========================================================================

    @Nested
    @DisplayName("i) Automatic Offer Application and Data Persistence")
    inner class AutomaticOfferTests {

        @Test
        @DisplayName("i-01 SUCCESS: morning discount (25%) applied before noon on a weekday")
        fun morningDiscountAppliedWeekdayBeforeNoon() {
            // Requirement i: morning discount fires automatically for eligible screenings
            // 2026-04-07 is a Tuesday; 10:00 is before noon
            // Inception 10.00 x 1 seat x 0.75 = 7.50
            adminService.setMorningDiscountEnabled(true)
            val film      = bookingService.searchFilms("Inception").first()
            val screening = bookingService.getScreeningsForFilm(film)
                .first { it.startTime == "10:00" }
            val result    = bookingService.calculatePrice(film, screening, 1)
            assertTrue(result.morningDiscountApplied,
                "Morning Discount must be applied for a 10:00 weekday screening")
            assertEquals(7.50, result.finalPrice, 0.01,
                "10.00 x 0.75 = 7.50 with morning discount")
        }

        @Test
        @DisplayName("i-02 SUCCESS: morning discount NOT applied for afternoon screenings")
        fun morningDiscountNotAppliedAfternoon() {
            // Requirement i: screenings at or after noon must not receive morning discount
            val film      = bookingService.searchFilms("Inception").first()
            val screening = bookingService.getScreeningsForFilm(film)
                .first { it.startTime == "14:00" }
            val result    = bookingService.calculatePrice(film, screening, 1)
            assertFalse(result.morningDiscountApplied,
                "Morning Discount must NOT apply to a 14:00 screening")
            assertEquals(10.0, result.finalPrice, 0.01,
                "Full price must apply when no discounts are eligible")
        }

        @Test
        @DisplayName("i-03 SUCCESS: group discount (30%) applied automatically for 3+ seats")
        fun groupDiscountAppliedForThreeSeats() {
            // Requirement i: group discount fires when seatsCount > 2
            // Inception 10.00 evening 3 seats:
            // firstTwo = 2 x 10 = 20; remaining = 1 x (10 x 0.70) = 7 — total 27
            adminService.setGroupDiscountEnabled(true)
            val film      = bookingService.searchFilms("Inception").first()
            val screening = bookingService.getScreeningsForFilm(film)
                .first { it.startTime == "18:00" }
            val result    = bookingService.calculatePrice(film, screening, 3)
            assertTrue(result.groupDiscountApplied,
                "Group Discount must be applied automatically for 3 seats")
            assertEquals(27.0, result.finalPrice, 0.01,
                "2 x 10 + 1 x 7 = 27 with group discount")
        }

        @Test
        @DisplayName("i-04 SUCCESS: group discount NOT applied for only 2 seats")
        fun groupDiscountNotAppliedForTwoSeats() {
            // Requirement i: threshold is seatsCount > 2 so exactly 2 seats gets no discount
            val film      = bookingService.searchFilms("Inception").first()
            val screening = bookingService.getScreeningsForFilm(film)
                .first { it.startTime == "18:00" }
            val result    = bookingService.calculatePrice(film, screening, 2)
            assertFalse(result.groupDiscountApplied,
                "Group Discount must NOT be applied for only 2 seats")
            assertEquals(20.0, result.finalPrice, 0.01,
                "2 seats at full price = 20.00")
        }

        @Test
        @DisplayName("i-05 SUCCESS: MORNING applied FIRST then GROUP — total equals 19.24")
        fun morningAppliedBeforeGroup() {
            // Requirement i: when both discounts apply, morning MUST be applied first
            // Batman 9.50, 3 seats, 09:30 Thursday 2026-04-09:
            //   Step 1 morning first:  effectivePrice = 9.50 x 0.75 = 7.125
            //   Step 2 group on top:   firstTwo = 2 x 7.125 = 14.25
            //                          remaining = 1 x (7.125 x 0.70) = 4.9875
            //                          TOTAL = 19.2375 rounded to 19.24
            adminService.setMorningDiscountEnabled(true)
            adminService.setGroupDiscountEnabled(true)
            val film      = bookingService.searchFilms("Batman").first()
            val screening = bookingService.getScreeningsForFilm(film)
                .first { it.startTime == "09:30" }
            val result    = bookingService.calculatePrice(film, screening, 3)
            assertTrue(result.morningDiscountApplied,
                "Morning Discount must be applied for Batman 09:30 Thursday")
            assertTrue(result.groupDiscountApplied,
                "Group Discount must be applied for 3 seats")
            val expected = (9.5 * 0.75 * 2) + (9.5 * 0.75 * 0.70)
            assertEquals(expected, result.finalPrice, 0.01,
                "Morning applied first: (9.50x0.75x2) + (9.50x0.75x0.70) = 19.24")
        }

        @Test
        @DisplayName("i-06 SUCCESS: disabled morning offer is not applied even when eligible")
        fun disabledMorningOfferNotApplied() {
            // Requirement i: a disabled offer must have no effect on pricing
            adminService.setMorningDiscountEnabled(false)
            val film      = bookingService.searchFilms("Inception").first()
            val screening = bookingService.getScreeningsForFilm(film)
                .first { it.startTime == "10:00" }
            val result    = bookingService.calculatePrice(film, screening, 1)
            assertFalse(result.morningDiscountApplied,
                "Disabled Morning Discount must not be applied")
            assertEquals(10.0, result.finalPrice, 0.01,
                "Full price must apply when morning discount is disabled")
            adminService.setMorningDiscountEnabled(true) // restore
        }

        @Test
        @DisplayName("i-07 SUCCESS: disabled group offer is not applied even for 3+ seats")
        fun disabledGroupOfferNotApplied() {
            // Requirement i: disabling group discount must prevent it from firing
            adminService.setGroupDiscountEnabled(false)
            val film      = bookingService.searchFilms("Batman").first()
            val screening = bookingService.getScreeningsForFilm(film)
                .first { it.startTime == "19:00" }
            val result    = bookingService.calculatePrice(film, screening, 3)
            assertFalse(result.groupDiscountApplied,
                "Disabled Group Discount must not be applied for 3 seats")
            adminService.setGroupDiscountEnabled(true) // restore
        }

        @Test
        @DisplayName("i-08 SUCCESS: all booking data persists in SQLite between service calls")
        fun allDataPersistsInSQLite() {
            // Requirement i: films, screenings, seats and sales data must all persist
            val film      = bookingService.searchFilms("Frozen").first()
            val screening = bookingService.getScreeningsForFilm(film)
                .first { it.startTime == "17:30" }
            val seat      = bookingService.getAvailableSeats(screening.id!!).first()
            val before    = screening.totalTakings

            bookingService.purchaseTickets(film, screening, listOf(seat), "Customer", 999.0)

            // Re-read from SQLite in a fresh call to verify persistence
            val afterScreening = db.getAllScreeningsForFilm(film.id!!)
                .find { it.id == screening.id }!!
            assertTrue(afterScreening.totalTakings > before,
                "Total takings must be persisted in SQLite after purchase")

            val afterSeat = bookingService.getAvailableSeats(screening.id!!)
                .first { it.seatNumber == seat.seatNumber }
            assertFalse(afterSeat.isAvailable,
                "Seat availability change must persist in SQLite")
        }
    }
}