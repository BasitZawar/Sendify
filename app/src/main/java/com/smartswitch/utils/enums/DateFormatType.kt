package com.smartswitch.utils.enums

// Enum for different date formats
enum class DateFormatType(val format: String) {
    DAY_MONTH_YEAR("dd MMM yyyy"),
    MONTH_DAY_YEAR("MM-dd-yyyy"),
    CUSTOM("")  // Placeholder for custom format if needed
}