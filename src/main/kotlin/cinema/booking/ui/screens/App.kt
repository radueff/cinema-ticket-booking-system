package cinema.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cinema.booking.model.*
import cinema.booking.service.*

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun App(bookingService: BookingService, adminService: AdminService) {
    var screen by remember { mutableStateOf("welcome") }

    MaterialTheme {
        when (screen) {
            "welcome"         -> WelcomeScreen(
                onAdminClick    = { screen = "admin_login" },
                onCustomerClick = { screen = "customer" }
            )
            "admin_login"     -> AdminLoginScreen(
                adminService  = adminService,
                onLoginSuccess = { screen = "admin_dashboard" },
                onBack        = { screen = "welcome" }
            )
            "admin_dashboard" -> AdminDashboardScreen(
                adminService = adminService,
                onLogout     = { screen = "welcome" }
            )
            "customer"        -> CustomerBookingScreen(
                bookingService = bookingService,
                onBack         = { screen = "welcome" }
            )
        }
    }
}

// ── Screen 1: Welcome ─────────────────────────────────────────────────────────

@Composable
fun WelcomeScreen(onAdminClick: () -> Unit, onCustomerClick: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Cinema Ticket Booking System",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onAdminClick, modifier = Modifier.width(280.dp).height(52.dp)) {
            Text("Login as Administrator", fontSize = 16.sp)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCustomerClick, modifier = Modifier.width(280.dp).height(52.dp)) {
            Text("Login as Customer", fontSize = 16.sp)
        }
    }
}

// ── Screen 2: Admin Login (Requirement e) ─────────────────────────────────────
// Requirement e: login system stored in SQLite, admin features only after login

@Composable
fun AdminLoginScreen(
    adminService: AdminService,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error    by remember { mutableStateOf("") }

    Column(
        modifier            = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Administrator Login",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value         = username,
            onValueChange = { username = it; error = "" },
            label         = { Text("Username") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(0.6f)
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value                = password,
            onValueChange        = { password = it; error = "" },
            label                = { Text("Password") },
            singleLine           = true,
            isError              = error.isNotEmpty(),
            visualTransformation = PasswordVisualTransformation(),
            modifier             = Modifier.fillMaxWidth(0.6f)
        )

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = {
                if (adminService.login(username.trim(), password))
                    onLoginSuccess()
                else
                    error = "Invalid username or password."
            }) { Text("Log In") }
        }
    }
}

// ── Screen 3: Admin Dashboard (Requirements f, g, h) ─────────────────────────

@Composable
fun AdminDashboardScreen(adminService: AdminService, onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Films & Screenings", "Pricing", "Special Offers")

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Admin Dashboard",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onLogout) { Text("Logout") }
        }

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick  = { selectedTab = i },
                    text     = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        when (selectedTab) {
            0    -> FilmsScreeningsTab(adminService)
            1    -> PricingTab(adminService)
            2    -> SpecialOffersTab(adminService)
            else -> {}
        }
    }
}

// ── Tab f: View Films & Screenings ────────────────────────────────────────────
// Requirement f: view all films — genre, base price, screenings, total tickets sold

@Composable
fun FilmsScreeningsTab(adminService: AdminService) {
    val data = remember { adminService.getAllFilmsWithScreenings() }

    if (data.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No films found.")
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(data) { (film, screenings) ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        // Film header
                        Text(film.title,
                            fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "Genre: ${film.genre}   " +
                                    "Base Price: £${"%.2f".format(film.baseTicketPrice)}   " +
                                    "Total Tickets Sold: ${film.totalTicketsSold}",
                            fontSize = 13.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (screenings.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(6.dp))
                            Text("Screenings:",
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            screenings.forEach { s ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Hall ${s.hallNumber}  |  ${s.date}  |  ${s.startTime}",
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Takings: £${"%.2f".format(s.totalTakings)}",
                                        fontSize = 12.sp,
                                        color    = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Tab g: Pricing Management ─────────────────────────────────────────────────
// Requirement g: add new film + screening, modify prices with factor

@Composable
fun PricingTab(adminService: AdminService) {
    var films      by remember { mutableStateOf(adminService.getAllFilmsWithScreenings().map { it.first }) }
    var message    by remember { mutableStateOf("") }
    var msgIsError by remember { mutableStateOf(false) }

    // Add film form state
    var newTitle  by remember { mutableStateOf("") }
    var newGenre  by remember { mutableStateOf("") }
    var newPrice  by remember { mutableStateOf("") }
    var newHall   by remember { mutableStateOf("1") }
    var newDate   by remember { mutableStateOf("") }
    var newTime   by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status message
        if (message.isNotEmpty()) {
            Text(
                message,
                color      = if (msgIsError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp
            )
        }

        // ── Modify existing prices ────────────────────────────────────────────
        Text("Modify Ticket Prices",
            fontWeight = FontWeight.Bold, fontSize = 16.sp)

        films.forEach { film ->
            var baseInput   by remember(film.id) { mutableStateOf(film.baseTicketPrice.toString()) }
            var factorInput by remember(film.id) { mutableStateOf("1.0") }

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("${film.title} — current: £${"%.2f".format(film.baseTicketPrice)}",
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value         = baseInput,
                            onValueChange = { baseInput = it },
                            label         = { Text("Base £") },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value         = factorInput,
                            onValueChange = { factorInput = it },
                            label         = { Text("× Factor") },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            val b = baseInput.toDoubleOrNull()
                            val f = factorInput.toDoubleOrNull()
                            if (b == null || b <= 0) {
                                message = "Enter a valid base price."; msgIsError = true
                            } else if (f == null || f <= 0) {
                                message = "Enter a valid factor."; msgIsError = true
                            } else {
                                adminService.updateFilmPrice(film.id!!, b, f)
                                films = adminService.getAllFilmsWithScreenings().map { it.first }
                                message = "'${film.title}' price updated to £${"%.2f".format(b * f)}."
                                msgIsError = false
                            }
                        }) { Text("Save") }
                    }
                    Text(
                        "New effective price: £${"%.2f".format(
                            (baseInput.toDoubleOrNull() ?: film.baseTicketPrice) *
                                    (factorInput.toDoubleOrNull() ?: 1.0)
                        )}",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider()

        // ── Add new film ──────────────────────────────────────────────────────
        Text("Add New Film & Screening",
            fontWeight = FontWeight.Bold, fontSize = 16.sp)

        OutlinedTextField(newTitle, { newTitle = it },
            label = { Text("Film Title") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())

        OutlinedTextField(newGenre, { newGenre = it },
            label = { Text("Genre") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(newPrice, { newPrice = it },
                label = { Text("Base Price (£)") }, singleLine = true,
                modifier = Modifier.weight(1f))
            OutlinedTextField(newHall, { newHall = it },
                label = { Text("Hall No.") }, singleLine = true,
                modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(newDate, { newDate = it },
                label = { Text("Date (YYYY-MM-DD)") }, singleLine = true,
                modifier = Modifier.weight(1f))
            OutlinedTextField(newTime, { newTime = it },
                label = { Text("Time (HH:MM)") }, singleLine = true,
                modifier = Modifier.weight(1f))
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick  = {
                val p = newPrice.toDoubleOrNull()
                val h = newHall.toIntOrNull()
                when {
                    newTitle.isBlank()       -> { message = "Title is required."; msgIsError = true }
                    newGenre.isBlank()       -> { message = "Genre is required."; msgIsError = true }
                    p == null || p <= 0      -> { message = "Enter a valid price."; msgIsError = true }
                    h == null || h <= 0      -> { message = "Enter a valid hall number."; msgIsError = true }
                    newDate.isBlank()        -> { message = "Date is required."; msgIsError = true }
                    newTime.isBlank()        -> { message = "Time is required."; msgIsError = true }
                    else -> {
                        try {
                            adminService.addFilmWithScreening(
                                newTitle.trim(), newGenre.trim(), p,
                                h, newDate.trim(), newTime.trim()
                            )
                            films = adminService.getAllFilmsWithScreenings().map { it.first }
                            message = "'${newTitle.trim()}' added successfully."
                            msgIsError = false
                            newTitle = ""; newGenre = ""; newPrice = ""
                            newHall  = "1"; newDate = ""; newTime = ""
                        } catch (e: Exception) {
                            message = e.message ?: "Error adding film."; msgIsError = true
                        }
                    }
                }
            }
        ) { Text("Add Film & Screening") }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Tab h: Special Offers ─────────────────────────────────────────────────────
// Requirement h: enable/disable Morning Discount and Group Booking Discount

@Composable
fun SpecialOffersTab(adminService: AdminService) {
    var morningEnabled by remember { mutableStateOf(adminService.isMorningDiscountEnabled()) }
    var groupEnabled   by remember { mutableStateOf(adminService.isGroupDiscountEnabled()) }
    var statusMsg      by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Text("Special Offer Management",
            fontWeight = FontWeight.Bold, fontSize = 16.sp)

        if (statusMsg.isNotEmpty()) {
            Text(statusMsg, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        }

        // Morning Discount
        Card(Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Morning Discount", fontWeight = FontWeight.Bold)
                    Text("25% off — before noon on weekdays",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Status: ${if (morningEnabled) "✅ Enabled" else "❌ Disabled"}",
                        fontSize = 12.sp)
                }
                Switch(
                    checked         = morningEnabled,
                    onCheckedChange = {
                        adminService.setMorningDiscountEnabled(it)
                        morningEnabled = it
                        statusMsg = "Morning Discount ${if (it) "enabled" else "disabled"}."
                    }
                )
            }
        }

        // Group Booking Discount
        Card(Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Group Booking Discount", fontWeight = FontWeight.Bold)
                    Text("30% off from seat 3 onwards (3+ seats)",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Status: ${if (groupEnabled) "✅ Enabled" else "❌ Disabled"}",
                        fontSize = 12.sp)
                }
                Switch(
                    checked         = groupEnabled,
                    onCheckedChange = {
                        adminService.setGroupDiscountEnabled(it)
                        groupEnabled = it
                        statusMsg = "Group Booking Discount ${if (it) "enabled" else "disabled"}."
                    }
                )
            }
        }
    }
}

// ── Screen 4: Customer Booking (Requirements a, b, c, d) ─────────────────────

@Composable
fun CustomerBookingScreen(bookingService: BookingService, onBack: () -> Unit) {

    var films             by remember { mutableStateOf(bookingService.searchFilms("")) }
    var selectedFilm      by remember { mutableStateOf<Film?>(null) }
    var screenings        by remember { mutableStateOf(listOf<Screening>()) }
    var selectedScreening by remember { mutableStateOf<Screening?>(null) }
    var availableSeats    by remember { mutableStateOf(listOf<Seat>()) }
    val  selectedSeats     = remember { mutableStateListOf<Seat>() }
    var searchQuery       by remember { mutableStateOf("") }
    var paymentAmount     by remember { mutableStateOf("") }
    var showTicket        by remember { mutableStateOf(false) }
    var ticketText        by remember { mutableStateOf("") }
    var errorMessage      by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        // Back button row
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("Film Booking",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))

        // ── a) Film Search ────────────────────────────────────────────────────
        OutlinedTextField(
            value         = searchQuery,
            onValueChange = {
                searchQuery       = it
                films             = bookingService.searchFilms(it)
                selectedFilm      = null
                selectedScreening = null
                selectedSeats.clear()
            },
            label      = { Text("Search film or genre") },
            modifier   = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        // Film list — fixed: no height restriction so all films show
        films.forEach { film ->
            TextButton(
                onClick   = {
                    selectedFilm      = film
                    selectedScreening = null
                    selectedSeats.clear()
                    errorMessage      = ""
                    screenings        = bookingService.getScreeningsForFilm(film)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${film.title} (${film.genre})")
                    Text("£${"%.2f".format(film.baseTicketPrice)}", fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // ── b) Screening Display ──────────────────────────────────────────────
        if (selectedFilm != null) {
            Spacer(Modifier.height(16.dp))
            Text("Screenings for ${selectedFilm!!.title}:",
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))

            if (screenings.isEmpty()) {
                Text("No screenings available.", color = MaterialTheme.colorScheme.error)
            } else {
                screenings.forEach { screening ->
                    val hour = screening.startTime.substringBefore(":").toIntOrNull() ?: 12
                    TextButton(
                        onClick  = {
                            selectedScreening = screening
                            selectedSeats.clear()
                            errorMessage      = ""
                            availableSeats    = bookingService.getAvailableSeats(screening.id!!)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Hall ${screening.hallNumber}  |  ${screening.date}  |  ${screening.startTime}" +
                                        if (hour < 12) " ☀️" else ""
                            )
                            Text("£${"%.2f".format(selectedFilm!!.baseTicketPrice)}")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        // ── c) Seat Selection ─────────────────────────────────────────────────
        if (selectedScreening != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Select seats — Hall ${selectedScreening!!.hallNumber}  " +
                        "${selectedScreening!!.date}  ${selectedScreening!!.startTime}:",
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp
            )
            Spacer(Modifier.height(8.dp))

            // SCREEN banner
            Box(
                Modifier.fillMaxWidth().height(26.dp)
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(thickness = 3.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                Text("SCREEN", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp))
            }

            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns  = GridCells.Fixed(5),
                modifier = Modifier.height(200.dp)
            ) {
                items(availableSeats) { seat ->
                    val isSelected = selectedSeats.contains(seat)
                    Button(
                        onClick = {
                            if (seat.isAvailable) {
                                if (isSelected) selectedSeats.remove(seat)
                                else            selectedSeats.add(seat)
                                errorMessage = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                !seat.isAvailable -> Color(0xFF888888)
                                isSelected        -> Color(0xFF4CAF50)
                                else              -> MaterialTheme.colorScheme.primary
                            }
                        ),
                        modifier = Modifier.padding(3.dp)
                    ) {
                        Text(seat.seatNumber, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Legend
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(MaterialTheme.colorScheme.primary, "Available")
                LegendItem(Color(0xFF4CAF50), "Selected")
                LegendItem(Color(0xFF888888), "Booked")
            }

            // ── d) Ticket Purchase ────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))

            val total = if (selectedSeats.isNotEmpty())
                bookingService.calculateDiscountedTotal(
                    selectedFilm!!, selectedScreening!!, selectedSeats.size)
            else 0.0

            // Show which discounts will apply
            if (selectedSeats.isNotEmpty()) {
                val result = bookingService.calculatePrice(
                    selectedFilm!!, selectedScreening!!, selectedSeats.size)
                if (result.morningDiscountApplied)
                    Text("☀️  Morning Discount (25%) applied",
                        color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                if (result.groupDiscountApplied)
                    Text("👥  Group Booking Discount (30% from seat 3) applied",
                        color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }

            Text("Total: £${"%.2f".format(total)}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value         = paymentAmount,
                onValueChange = { paymentAmount = it; errorMessage = "" },
                label         = { Text("Enter payment amount (£)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                isError       = errorMessage.isNotEmpty()
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick  = {
                    val paid = paymentAmount.toDoubleOrNull() ?: 0.0
                    when {
                        selectedSeats.isEmpty() ->
                            errorMessage = "Please select at least one seat."
                        paid < total ->
                            errorMessage =
                                "Insufficient funds. Required: £${"%.2f".format(total)}, " +
                                        "provided: £${"%.2f".format(paid)}"
                        else -> {
                            val success = bookingService.bookSeats(
                                selectedScreening!!.id!!,
                                selectedSeats.toList(),
                                "Customer",
                                total
                            )
                            if (success) {
                                val seatNums = selectedSeats.joinToString(", ") { it.seatNumber }
                                val result   = bookingService.calculatePrice(
                                    selectedFilm!!, selectedScreening!!, selectedSeats.size)
                                ticketText = buildString {
                                    appendLine("*******************************")
                                    appendLine("       SOLENT CINEMA")
                                    appendLine("Film:  ${selectedFilm!!.title}")
                                    appendLine("Date:  ${selectedScreening!!.date}")
                                    appendLine("Time:  ${selectedScreening!!.startTime}")
                                    appendLine("Seats: $seatNums")
                                    appendLine("Price: £${"%.2f".format(total)}")
                                    if (result.morningDiscountApplied)
                                        appendLine("* Morning Discount (25%) applied")
                                    if (result.groupDiscountApplied)
                                        appendLine("* Group Discount (30%) applied")
                                    append("*******************************")
                                }
                                showTicket   = true
                                errorMessage = ""
                                selectedSeats.clear()
                                // Refresh seats to show booked ones as grey
                                availableSeats = bookingService.getAvailableSeats(
                                    selectedScreening!!.id!!)
                            } else {
                                errorMessage = "Booking failed. Please try again."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Purchase Tickets — £${"%.2f".format(total)}") }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Ticket receipt dialog
    if (showTicket) {
        AlertDialog(
            onDismissRequest = { showTicket = false },
            title            = { Text("Booking Confirmed ✅") },
            text             = { Text(ticketText, fontFamily = FontFamily.Monospace) },
            confirmButton    = {
                Button(onClick = { showTicket = false }) { Text("Close") }
            }
        )
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Card(
            Modifier.size(14.dp),
            colors = CardDefaults.cardColors(containerColor = color)
        ) {}
        Text(label, fontSize = 11.sp)
    }
}