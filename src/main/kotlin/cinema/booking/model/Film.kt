package cinema.booking.model

class Film(
    var id: Long? = null,
    val title: String,
    val genre: String,
    var baseTicketPrice: Double,
    var totalTicketsSold: Int,
    val screenings: MutableList<Screening> = mutableListOf()
) {
    fun displayFilmInfo() {
        println("Film: $title | Genre: $genre")
    }
}