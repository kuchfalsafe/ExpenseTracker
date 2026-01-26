package com.kishan.expensetracker.data.database

import androidx.room.TypeConverter
import com.kishan.expensetracker.data.entity.TransactionSource
import com.kishan.expensetracker.data.entity.TransactionType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun transactionTypeToString(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun fromTransactionSource(value: String): TransactionSource {
        return TransactionSource.valueOf(value)
    }

    @TypeConverter
    fun transactionSourceToString(source: TransactionSource): String {
        return source.name
    }
}

