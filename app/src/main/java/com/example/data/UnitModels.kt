package com.example.data

enum class UnitCategory(val title: String) {
    LENGTH("Length"),
    AREA("Area"),
    VOLUME("Volume"),
    MASS("Mass"),
    PRESSURE("Pressure"),
    SPEED("Speed"),
    DATA("Data Storage"),
    VOLTAGE("Voltage"),
    CURRENT("Current"),
    RESISTANCE("Resistance"),
    POWER("Power"),
    TEMPERATURE("Temperature"),
    ENERGY("Energy"),
    CURRENCY("Currency")
}

data class MeasureUnit(
    val id: String,
    val category: UnitCategory,
    val symbol: String,
    val name: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double
)

val StaticUnits = listOf(
    // LENGTH (Base: Meter)
    MeasureUnit("m", UnitCategory.LENGTH, "m", "Meter", { it }, { it }),
    MeasureUnit("mm", UnitCategory.LENGTH, "mm", "Millimeter", { it / 1000.0 }, { it * 1000.0 }),
    MeasureUnit("cm", UnitCategory.LENGTH, "cm", "Centimeter", { it / 100.0 }, { it * 100.0 }),
    MeasureUnit("km", UnitCategory.LENGTH, "km", "Kilometer", { it * 1000.0 }, { it / 1000.0 }),
    MeasureUnit("in", UnitCategory.LENGTH, "in", "Inch", { it * 0.0254 }, { it / 0.0254 }),
    MeasureUnit("ft", UnitCategory.LENGTH, "ft", "Foot", { it * 0.3048 }, { it / 0.3048 }),
    MeasureUnit("yd", UnitCategory.LENGTH, "yd", "Yard", { it * 0.9144 }, { it / 0.9144 }),
    MeasureUnit("mi", UnitCategory.LENGTH, "mi", "Mile", { it * 1609.344 }, { it / 1609.344 }),
    MeasureUnit("nmi", UnitCategory.LENGTH, "NM", "Nautical Mile", { it * 1852.0 }, { it / 1852.0 }),

    // AREA (Base: Square Meter)
    MeasureUnit("sqm", UnitCategory.AREA, "m²", "Square Meter", { it }, { it }),
    MeasureUnit("sqkm", UnitCategory.AREA, "km²", "Square Kilometre", { it * 1000000.0 }, { it / 1000000.0 }),
    MeasureUnit("sqcm", UnitCategory.AREA, "cm²", "Square Centimeter", { it / 10000.0 }, { it * 10000.0 }),
    MeasureUnit("sqmm", UnitCategory.AREA, "mm²", "Square Millimeter", { it / 1000000.0 }, { it * 1000000.0 }),
    MeasureUnit("acre", UnitCategory.AREA, "ac", "Acre", { it * 4046.85642 }, { it / 4046.85642 }),
    MeasureUnit("hectare", UnitCategory.AREA, "ha", "Hectare", { it * 10000.0 }, { it / 10000.0 }),
    MeasureUnit("sqin", UnitCategory.AREA, "in²", "Square Inch", { it * 0.00064516 }, { it / 0.00064516 }),
    MeasureUnit("sqft", UnitCategory.AREA, "ft²", "Square Foot", { it * 0.092903 }, { it / 0.092903 }),
    MeasureUnit("sqyd", UnitCategory.AREA, "yd²", "Square Yard", { it * 0.836127 }, { it / 0.836127 }),
    MeasureUnit("sqmi", UnitCategory.AREA, "mi²", "Square Mile", { it * 2589988.11 }, { it / 2589988.11 }),

    // VOLUME (Base: Cubic Meter)
    MeasureUnit("cum", UnitCategory.VOLUME, "m³", "Cubic Meter", { it }, { it }),
    MeasureUnit("l", UnitCategory.VOLUME, "L", "Liter", { it / 1000.0 }, { it * 1000.0 }),
    MeasureUnit("ml", UnitCategory.VOLUME, "mL", "Milliliter", { it / 1000000.0 }, { it * 1000000.0 }),
    MeasureUnit("gal", UnitCategory.VOLUME, "gal", "US Gallon", { it * 0.00378541 }, { it / 0.00378541 }),
    MeasureUnit("qt", UnitCategory.VOLUME, "qt", "US Quart", { it * 0.000946353 }, { it / 0.000946353 }),
    MeasureUnit("pt", UnitCategory.VOLUME, "pt", "US Pint", { it * 0.000473176 }, { it / 0.000473176 }),
    MeasureUnit("cup", UnitCategory.VOLUME, "cup", "US Cup", { it * 0.000236588 }, { it / 0.000236588 }),
    MeasureUnit("floz", UnitCategory.VOLUME, "fl oz", "US Fluid Ounce", { it * 0.0000295735 }, { it / 0.0000295735 }),
    MeasureUnit("imp_gal", UnitCategory.VOLUME, "imp gal", "Imperial Gallon", { it * 0.00454609 }, { it / 0.00454609 }),

    // SPEED (Base: Meters per Second)
    MeasureUnit("mps", UnitCategory.SPEED, "m/s", "Meter per Second", { it }, { it }),
    MeasureUnit("kmph", UnitCategory.SPEED, "km/h", "Kilometer per Hour", { it / 3.6 }, { it * 3.6 }),
    MeasureUnit("mph", UnitCategory.SPEED, "mph", "Mile per Hour", { it * 0.44704 }, { it / 0.44704 }),
    MeasureUnit("knot", UnitCategory.SPEED, "kn", "Knot", { it * 0.514444 }, { it / 0.514444 }),
    MeasureUnit("ftps", UnitCategory.SPEED, "ft/s", "Foot per Second", { it * 0.3048 }, { it / 0.3048 }),

    // DATA (Base: Byte)
    MeasureUnit("b", UnitCategory.DATA, "B", "Byte", { it }, { it }),
    MeasureUnit("kb", UnitCategory.DATA, "KB", "Kilobyte (10³)", { it * 1000.0 }, { it / 1000.0 }),
    MeasureUnit("mb", UnitCategory.DATA, "MB", "Megabyte", { it * 1000000.0 }, { it / 1000000.0 }),
    MeasureUnit("gb", UnitCategory.DATA, "GB", "Gigabyte", { it * 1000000000.0 }, { it / 1000000000.0 }),
    MeasureUnit("tb", UnitCategory.DATA, "TB", "Terabyte", { it * 1000000000000.0 }, { it / 1000000000000.0 }),
    MeasureUnit("kib", UnitCategory.DATA, "KiB", "Kibibyte (2¹⁰)", { it * 1024.0 }, { it / 1024.0 }),
    MeasureUnit("mib", UnitCategory.DATA, "MiB", "Mebibyte", { it * 1048576.0 }, { it / 1048576.0 }),
    MeasureUnit("gib", UnitCategory.DATA, "GiB", "Gibibyte", { it * 1073741824.0 }, { it / 1073741824.0 }),

    // MASS (Base: Kilogram)
    MeasureUnit("kg", UnitCategory.MASS, "kg", "Kilogram", { it }, { it }),
    MeasureUnit("g", UnitCategory.MASS, "g", "Gram", { it / 1000.0 }, { it * 1000.0 }),
    MeasureUnit("mg", UnitCategory.MASS, "mg", "Milligram", { it / 1000000.0 }, { it * 1000000.0 }),
    MeasureUnit("t", UnitCategory.MASS, "t", "Tonne", { it * 1000.0 }, { it / 1000.0 }),
    MeasureUnit("lb", UnitCategory.MASS, "lb", "Pound", { it * 0.45359237 }, { it / 0.45359237 }),
    MeasureUnit("oz", UnitCategory.MASS, "oz", "Ounce", { it * 0.02834952 }, { it / 0.02834952 }),
    MeasureUnit("st", UnitCategory.MASS, "st", "Stone", { it * 6.35029318 }, { it / 6.35029318 }),
    
    // PRESSURE (Base: Pascal)
    MeasureUnit("Pa", UnitCategory.PRESSURE, "Pa", "Pascal", { it }, { it }),
    MeasureUnit("kPa", UnitCategory.PRESSURE, "kPa", "Kilopascal", { it * 1000.0 }, { it / 1000.0 }),
    MeasureUnit("MPa", UnitCategory.PRESSURE, "MPa", "Megapascal", { it * 1000000.0 }, { it / 1000000.0 }),
    MeasureUnit("bar", UnitCategory.PRESSURE, "bar", "Bar", { it * 100000.0 }, { it / 100000.0 }),
    MeasureUnit("atm", UnitCategory.PRESSURE, "atm", "Atmosphere", { it * 101325.0 }, { it / 101325.0 }),
    MeasureUnit("psi", UnitCategory.PRESSURE, "psi", "Pound/Square Inch", { it * 6894.757 }, { it / 6894.757 }),
    MeasureUnit("mmHg", UnitCategory.PRESSURE, "mmHg", "Millimeter of Mercury", { it * 133.322 }, { it / 133.322 }),

    // VOLTAGE (Base: Volt)
    MeasureUnit("V", UnitCategory.VOLTAGE, "V", "Volt", { it }, { it }),
    MeasureUnit("mV", UnitCategory.VOLTAGE, "mV", "Millivolt", { it / 1000.0 }, { it * 1000.0 }),
    MeasureUnit("kV", UnitCategory.VOLTAGE, "kV", "Kilovolt", { it * 1000.0 }, { it / 1000.0 }),
    MeasureUnit("MV", UnitCategory.VOLTAGE, "MV", "Megavolt", { it * 1000000.0 }, { it / 1000000.0 }),

    // CURRENT (Base: Ampere)
    MeasureUnit("A", UnitCategory.CURRENT, "A", "Ampere", { it }, { it }),
    MeasureUnit("mA", UnitCategory.CURRENT, "mA", "Milliampere", { it / 1000.0 }, { it * 1000.0 }),
    MeasureUnit("uA", UnitCategory.CURRENT, "µA", "Microampere", { it / 1000000.0 }, { it * 1000000.0 }),
    MeasureUnit("kA", UnitCategory.CURRENT, "kA", "Kiloampere", { it * 1000.0 }, { it / 1000.0 }),

    // RESISTANCE (Base: Ohm)
    MeasureUnit("ohm", UnitCategory.RESISTANCE, "Ω", "Ohm", { it }, { it }),
    MeasureUnit("mohm", UnitCategory.RESISTANCE, "mΩ", "Milliohm", { it / 1000.0 }, { it * 1000.0 }),
    MeasureUnit("kohm", UnitCategory.RESISTANCE, "kΩ", "Kiloohm", { it * 1000.0 }, { it / 1000.0 }),
    MeasureUnit("Mohm", UnitCategory.RESISTANCE, "MΩ", "Megaohm", { it * 1000000.0 }, { it / 1000000.0 }),

    // POWER (Base: Watt)
    MeasureUnit("W", UnitCategory.POWER, "W", "Watt", { it }, { it }),
    MeasureUnit("mW", UnitCategory.POWER, "mW", "Milliwatt", { it / 1000.0 }, { it * 1000.0 }),
    MeasureUnit("kW", UnitCategory.POWER, "kW", "Kilowatt", { it * 1000.0 }, { it / 1000.0 }),
    MeasureUnit("MW", UnitCategory.POWER, "MW", "Megawatt", { it * 1000000.0 }, { it / 1000000.0 }),
    MeasureUnit("hp", UnitCategory.POWER, "hp", "Horsepower", { it * 745.699872 }, { it / 745.699872 }),
    MeasureUnit("BTU/h", UnitCategory.POWER, "BTU/h", "BTU per hour", { it * 0.293071 }, { it / 0.293071 }),

    // TEMPERATURE (Base: Celsius)
    MeasureUnit("c", UnitCategory.TEMPERATURE, "°C", "Celsius", { it }, { it }),
    MeasureUnit("f", UnitCategory.TEMPERATURE, "°F", "Fahrenheit", { (it - 32.0) * 5.0/9.0 }, { it * 9.0/5.0 + 32.0 }),
    MeasureUnit("k", UnitCategory.TEMPERATURE, "K", "Kelvin", { it - 273.15 }, { it + 273.15 }),

    // ENERGY (Base: Joule)
    MeasureUnit("J", UnitCategory.ENERGY, "J", "Joule", { it }, { it }),
    MeasureUnit("kJ", UnitCategory.ENERGY, "kJ", "Kilojoule", { it * 1000.0 }, { it / 1000.0 }),
    MeasureUnit("cal", UnitCategory.ENERGY, "cal", "Calorie", { it * 4.184 }, { it / 4.184 }),
    MeasureUnit("kcal", UnitCategory.ENERGY, "kcal", "Kilocalorie", { it * 4184.0 }, { it / 4184.0 }),
    MeasureUnit("Wh", UnitCategory.ENERGY, "Wh", "Watt-hour", { it * 3600.0 }, { it / 3600.0 }),
    MeasureUnit("kWh", UnitCategory.ENERGY, "kWh", "Kilowatt-hour", { it * 3600000.0 }, { it / 3600000.0 }),
    MeasureUnit("eV", UnitCategory.ENERGY, "eV", "Electronvolt", { it * 1.602176634e-19 }, { it / 1.602176634e-19 }),
    MeasureUnit("BTU", UnitCategory.ENERGY, "BTU", "British Thermal Unit", { it * 1055.06 }, { it / 1055.06 })
)

