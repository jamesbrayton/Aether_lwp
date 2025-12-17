package com.aether.wallpaper.model

/**
 * Definition of a shader parameter parsed from @param metadata tag.
 *
 * Example metadata:
 * ```
 * @param u_speed float 1.0 min=0.0 max=5.0 step=0.1 name="Speed" desc="Movement speed"
 * ```
 *
 * @property id The uniform name (e.g., "u_speed")
 * @property name Display name for UI (e.g., "Speed")
 * @property type Parameter type (FLOAT, INT, BOOL, etc.)
 * @property defaultValue Default value for the parameter
 * @property minValue Minimum value (for numeric types), null if not specified
 * @property maxValue Maximum value (for numeric types), null if not specified
 * @property step Increment step (for numeric types), null if not specified
 * @property description Human-readable description for tooltips/help
 */
data class ParameterDefinition(
    val id: String,
    val name: String,
    val type: ParameterType,
    val defaultValue: Any,
    val minValue: Any? = null,
    val maxValue: Any? = null,
    val step: Any? = null,
    val description: String = ""
) {
    /**
     * Validates that the parameter definition is internally consistent.
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        require(id.isNotBlank()) { "Parameter id cannot be blank" }
        require(name.isNotBlank()) { "Parameter name cannot be blank" }

        when (type) {
            ParameterType.FLOAT -> {
                require(defaultValue is Float || defaultValue is Double) {
                    "FLOAT parameter must have numeric default value"
                }
                minValue?.let { require(it is Float || it is Double) { "FLOAT minValue must be numeric" } }
                maxValue?.let { require(it is Float || it is Double) { "FLOAT maxValue must be numeric" } }
                step?.let { require(it is Float || it is Double) { "FLOAT step must be numeric" } }
            }
            ParameterType.INT -> {
                require(defaultValue is Int) { "INT parameter must have Int default value" }
                minValue?.let { require(it is Int) { "INT minValue must be Int" } }
                maxValue?.let { require(it is Int) { "INT maxValue must be Int" } }
                step?.let { require(it is Int) { "INT step must be Int" } }
            }
            ParameterType.BOOL -> {
                require(defaultValue is Boolean) { "BOOL parameter must have Boolean default value" }
            }
            ParameterType.COLOR -> {
                // COLOR can be represented as String (hex) or array of floats
                require(defaultValue is String || defaultValue is FloatArray || defaultValue is List<*>) {
                    "COLOR parameter must be String or numeric array"
                }
            }
            ParameterType.VEC2, ParameterType.VEC3, ParameterType.VEC4 -> {
                require(defaultValue is FloatArray || defaultValue is List<*>) {
                    "Vector parameters must be FloatArray or List"
                }
            }
        }
    }
}
