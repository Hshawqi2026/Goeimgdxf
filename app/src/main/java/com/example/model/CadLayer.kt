package com.example.model

data class CadLayer(
    val id: String,
    val name: String,
    val colorHex: String,
    val colorAci: Int, // AutoCAD Color Index (1-255)
    val isVisible: Boolean = true,
    val isLocked: Boolean = false
) {
    companion object {
        val LAYER_BUILDINGS = CadLayer(
            id = "layer_buildings",
            name = "BUILDINGS",
            colorHex = "#22C55E", // Green
            colorAci = 3
        )
        val LAYER_ROADS = CadLayer(
            id = "layer_roads",
            name = "ROADS",
            colorHex = "#F97316", // Orange
            colorAci = 30
        )
        val LAYER_BOUNDARIES = CadLayer(
            id = "layer_boundaries",
            name = "BOUNDARIES",
            colorHex = "#06B6D4", // Cyan
            colorAci = 4
        )
        val LAYER_PARCELS = CadLayer(
            id = "layer_parcels",
            name = "PARCELS",
            colorHex = "#EAB308", // Yellow
            colorAci = 2
        )
        val LAYER_VEGETATION = CadLayer(
            id = "layer_vegetation",
            name = "VEGETATION",
            colorHex = "#10B981", // Emerald
            colorAci = 92
        )
        val LAYER_0 = CadLayer(
            id = "layer_0",
            name = "0",
            colorHex = "#E2E8F0", // Slate White
            colorAci = 7
        )

        fun defaultLayers(): List<CadLayer> = listOf(
            LAYER_0,
            LAYER_BUILDINGS,
            LAYER_ROADS,
            LAYER_BOUNDARIES,
            LAYER_PARCELS,
            LAYER_VEGETATION
        )
    }
}
