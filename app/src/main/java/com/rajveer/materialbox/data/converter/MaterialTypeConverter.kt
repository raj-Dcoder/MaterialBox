package com.rajveer.materialbox.data.converter

import androidx.room.TypeConverter
import com.rajveer.materialbox.data.entity.MaterialType

class MaterialTypeConverter {
    @TypeConverter
    fun fromMaterialType(value: MaterialType): String {
        return value.name
    }

    @TypeConverter
    fun toMaterialType(value: String): MaterialType {
        return MaterialType.valueOf(value)
    }
} 