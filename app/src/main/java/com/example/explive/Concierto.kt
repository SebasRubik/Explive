import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Concierto (
    var id: String,
    var artista: String,
    var ciudad: String,
    var centro_de_eventos: String,
    var fecha: String,
    var hora: String,
    var generos: List<String> = listOf()
): Parcelable {
    constructor() : this("", "", "", "", "", "", listOf())
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "artista" to artista,
            "ciudad" to ciudad,
            "centro_de_eventos" to centro_de_eventos,
            "fecha" to fecha,
            "hora" to hora,
            "generos" to generos
        )
    }
}
