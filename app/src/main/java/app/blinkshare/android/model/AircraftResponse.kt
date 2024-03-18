package app.blinkshare.android.model

data class AircraftResponse(
    val count: Int,
    val next: String,
    val per_page: Int,
    val previous: Any,
    val results: List<AircraftResult>
)

data class AircraftResult(
    val aircraft_class_name: String,
    val aircraft_id: Int,
    val aircraft_type_name: String,
    val company_name: String,
    val home_base: String,
    val is_for_charter: Boolean,
    val is_for_sale: Boolean,
    val manufacturer_name: String,
    val max_passengers: Int,
    val serial_number: String,
    val tail_number: String,
    val year_of_production: Int,
    var isSelected: Boolean = false
)