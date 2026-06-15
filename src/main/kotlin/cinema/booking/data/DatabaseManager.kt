package cinema.booking.data

import cinema.booking.model.*
import java.sql.*

class DatabaseManager(private val dbUrl: String = "jdbc:sqlite:cinema.db") {
    private var connection: Connection? = null

    fun connect() {
        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection(dbUrl)
            connection!!.autoCommit = true
            createTables()
            if (isDatabaseEmpty()) seedData()
            println("✅ Database connected and ready.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() { connection?.close() }

    private fun createTables() {
        listOf(
            "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, name TEXT)",
            "CREATE TABLE IF NOT EXISTS films (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT UNIQUE NOT NULL, genre TEXT NOT NULL, base_ticket_price REAL NOT NULL, total_tickets_sold INTEGER DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS screenings (id INTEGER PRIMARY KEY AUTOINCREMENT, film_id INTEGER NOT NULL, hall_number INTEGER NOT NULL, date TEXT NOT NULL, start_time TEXT NOT NULL, total_takings REAL DEFAULT 0, FOREIGN KEY(film_id) REFERENCES films(id))",
            "CREATE TABLE IF NOT EXISTS seats (id INTEGER PRIMARY KEY AUTOINCREMENT, screening_id INTEGER NOT NULL, seat_number TEXT NOT NULL, is_available BOOLEAN DEFAULT 1, FOREIGN KEY(screening_id) REFERENCES screenings(id))",
            "CREATE TABLE IF NOT EXISTS offers (name TEXT PRIMARY KEY, is_enabled BOOLEAN DEFAULT 1)",
            "CREATE TABLE IF NOT EXISTS bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_name TEXT, screening_id INTEGER NOT NULL, total_cost REAL NOT NULL, booking_date TEXT DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(screening_id) REFERENCES screenings(id))"
        ).forEach { connection!!.createStatement().executeUpdate(it) }
    }

    private fun isDatabaseEmpty(): Boolean {
        val rs = connection!!.createStatement().executeQuery("SELECT COUNT(*) FROM films")
        return rs.next() && rs.getInt(1) == 0
    }

    private fun seedData() {
        executeUpdate("INSERT OR IGNORE INTO users (username, password, role, name) VALUES ('admin', 'Admin123', 'Admin', 'Administrator')")
        executeUpdate("INSERT OR IGNORE INTO users (username, password, role, name) VALUES ('customer1', 'cust123', 'Customer', 'Customer One')")
        executeUpdate("INSERT OR IGNORE INTO offers (name, is_enabled) VALUES ('Morning Discount', 1)")
        executeUpdate("INSERT OR IGNORE INTO offers (name, is_enabled) VALUES ('Group Booking Discount', 1)")
        seedFilms()
    }

    private fun seedFilms() {
        val f1 = insertFilm("Inception",  "Sci-Fi",    10.0)
        addScreening(f1, 1, "2026-04-07", "10:00")
        addScreening(f1, 1, "2026-04-07", "14:00")
        addScreening(f1, 1, "2026-04-07", "18:00")

        val f2 = insertFilm("Titanic",    "Romance",    8.0)
        addScreening(f2, 2, "2026-04-08", "11:00")
        addScreening(f2, 2, "2026-04-08", "15:30")
        addScreening(f2, 2, "2026-04-08", "20:00")

        val f3 = insertFilm("Batman",     "Action",     9.5)
        addScreening(f3, 3, "2026-04-09", "09:30")
        addScreening(f3, 3, "2026-04-09", "13:30")
        addScreening(f3, 3, "2026-04-09", "19:00")

        val f4 = insertFilm("Frozen",     "Animation",  7.0)
        addScreening(f4, 4, "2026-04-10", "10:30")
        addScreening(f4, 4, "2026-04-10", "14:30")
        addScreening(f4, 4, "2026-04-10", "17:30")
    }

    // ── Film CRUD ─────────────────────────────────────────────────────────────

    fun insertFilm(title: String, genre: String, price: Double): Long {
        val stmt = connection!!.prepareStatement(
            "INSERT INTO films (title, genre, base_ticket_price) VALUES (?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        )
        stmt.setString(1, title)
        stmt.setString(2, genre)
        stmt.setDouble(3, price)
        stmt.executeUpdate()
        val rs = stmt.generatedKeys
        return if (rs.next()) rs.getLong(1) else -1L
    }

    fun getAllFilms(): List<Film> {
        val rs = connection!!.createStatement()
            .executeQuery("SELECT * FROM films ORDER BY title")
        return collectFilms(rs)
    }

    fun searchFilms(query: String): List<Film> {
        val stmt = connection!!.prepareStatement(
            "SELECT * FROM films WHERE title LIKE ? OR genre LIKE ?"
        )
        stmt.setString(1, "%$query%")
        stmt.setString(2, "%$query%")
        return collectFilms(stmt.executeQuery())
    }

    fun getFilmById(id: Long): Film? {
        val stmt = connection!!.prepareStatement("SELECT * FROM films WHERE id = ?")
        stmt.setLong(1, id)
        return collectFilms(stmt.executeQuery()).firstOrNull()
    }

    fun updateFilmPrice(filmId: Long, newPrice: Double) {
        val stmt = connection!!.prepareStatement(
            "UPDATE films SET base_ticket_price = ? WHERE id = ?"
        )
        stmt.setDouble(1, newPrice)
        stmt.setLong(2, filmId)
        stmt.executeUpdate()
    }

    private fun collectFilms(rs: ResultSet): List<Film> {
        val list = mutableListOf<Film>()
        while (rs.next()) {
            list += Film(
                id               = rs.getLong("id"),
                title            = rs.getString("title"),
                genre            = rs.getString("genre"),
                baseTicketPrice  = rs.getDouble("base_ticket_price"),
                totalTicketsSold = rs.getInt("total_tickets_sold")
            )
        }
        return list
    }

    // ── Screening CRUD ────────────────────────────────────────────────────────

    fun addScreening(filmId: Long, hall: Int, date: String, time: String): Long {
        val stmt = connection!!.prepareStatement(
            "INSERT INTO screenings (film_id, hall_number, date, start_time) VALUES (?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        )
        stmt.setLong(1, filmId)
        stmt.setInt(2, hall)
        stmt.setString(3, date)
        stmt.setString(4, time)
        stmt.executeUpdate()
        val rs = stmt.generatedKeys
        val screeningId = if (rs.next()) rs.getLong(1) else -1L
        createSeatsForScreening(screeningId)
        return screeningId
    }

    fun getScreeningsForFilm(film: Film): List<Screening> {
        val stmt = connection!!.prepareStatement(
            "SELECT * FROM screenings WHERE film_id = ? ORDER BY date, start_time"
        )
        stmt.setLong(1, film.id!!)
        val rs = stmt.executeQuery()
        val list = mutableListOf<Screening>()
        while (rs.next()) {
            list += Screening(
                id           = rs.getLong("id"),
                film         = film,
                hallNumber   = rs.getInt("hall_number"),
                date         = rs.getString("date"),
                startTime    = rs.getString("start_time"),
                seats        = getSeatsForScreening(rs.getLong("id")).toMutableList(),
                totalTakings = rs.getDouble("total_takings")
            )
        }
        return list
    }

    fun getAllScreeningsForFilm(filmId: Long): List<Screening> {
        val film = getFilmById(filmId) ?: return emptyList()
        return getScreeningsForFilm(film)
    }

    fun updateScreeningTakings(screeningId: Long, amount: Double, ticketCount: Int) {
        val stmt = connection!!.prepareStatement(
            "UPDATE screenings SET total_takings = total_takings + ? WHERE id = ?"
        )
        stmt.setDouble(1, amount)
        stmt.setLong(2, screeningId)
        stmt.executeUpdate()
        val stmt2 = connection!!.prepareStatement(
            "UPDATE films SET total_tickets_sold = total_tickets_sold + ? " +
                    "WHERE id = (SELECT film_id FROM screenings WHERE id = ?)"
        )
        stmt2.setInt(1, ticketCount)
        stmt2.setLong(2, screeningId)
        stmt2.executeUpdate()
    }

    // ── Seat CRUD ─────────────────────────────────────────────────────────────

    private fun createSeatsForScreening(screeningId: Long) {
        for (i in 1..10) {
            val stmt = connection!!.prepareStatement(
                "INSERT INTO seats (screening_id, seat_number, is_available) VALUES (?, ?, 1)"
            )
            stmt.setLong(1, screeningId)
            stmt.setString(2, "A$i")
            stmt.executeUpdate()
        }
    }

    fun getSeatsForScreening(screeningId: Long): List<Seat> {
        // ORDER BY id returns seats in insertion order (A1, A2, A3, ..., A10).
        // Sorting by seat_number as text would put A10 between A1 and A2 because
        // "A10" < "A2" alphabetically. Sorting by id (insertion order) avoids that.
        val stmt = connection!!.prepareStatement(
            "SELECT * FROM seats WHERE screening_id = ? ORDER BY id"
        )
        stmt.setLong(1, screeningId)
        val rs = stmt.executeQuery()
        val list = mutableListOf<Seat>()
        while (rs.next()) {
            list += Seat(
                id          = rs.getLong("id"),
                seatNumber  = rs.getString("seat_number"),
                isAvailable = rs.getBoolean("is_available")
            )
        }
        // Safety net: sort numerically by the digits after "A" so the order is
        // guaranteed even if rows arrive in some other order.
        return list.sortedBy { it.seatNumber.removePrefix("A").toIntOrNull() ?: 0 }
    }

    fun getSeatsByScreening(screeningId: Long): List<Seat> =
        getSeatsForScreening(screeningId)

    fun updateSeatAvailability(seatId: Long, available: Boolean) {
        val stmt = connection!!.prepareStatement(
            "UPDATE seats SET is_available = ? WHERE id = ?"
        )
        stmt.setBoolean(1, available)
        stmt.setLong(2, seatId)
        stmt.executeUpdate()
    }

    fun bookSeats(screeningId: Long, selectedSeatIds: List<Long>, customerName: String, totalCost: Double) {
        val stmt = connection!!.prepareStatement(
            "INSERT INTO bookings (customer_name, screening_id, total_cost) VALUES (?, ?, ?)"
        )
        stmt.setString(1, customerName)
        stmt.setLong(2, screeningId)
        stmt.setDouble(3, totalCost)
        stmt.executeUpdate()
        selectedSeatIds.forEach { seatId -> updateSeatAvailability(seatId, false) }
    }

    // ── Admin / User CRUD ─────────────────────────────────────────────────────

    fun findAdmin(username: String, password: String): Boolean {
        val stmt = connection!!.prepareStatement(
            "SELECT id FROM users WHERE username = ? AND password = ? AND role = 'Admin'"
        )
        stmt.setString(1, username)
        stmt.setString(2, password)
        return stmt.executeQuery().next()
    }

    // ── Special Offer CRUD ────────────────────────────────────────────────────

    fun getOfferEnabled(name: String): Boolean {
        val stmt = connection!!.prepareStatement(
            "SELECT is_enabled FROM offers WHERE name = ?"
        )
        stmt.setString(1, name)
        val rs = stmt.executeQuery()
        return if (rs.next()) rs.getBoolean(1) else false
    }

    fun setOfferEnabled(name: String, enabled: Boolean) {
        val stmt = connection!!.prepareStatement(
            "UPDATE offers SET is_enabled = ? WHERE name = ?"
        )
        stmt.setBoolean(1, enabled)
        stmt.setString(2, name)
        stmt.executeUpdate()
    }

    fun toggleOffer(name: String) {
        executeUpdate("UPDATE offers SET is_enabled = NOT is_enabled WHERE name = '$name'")
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private fun executeUpdate(sql: String) {
        connection!!.createStatement().executeUpdate(sql)
    }
}