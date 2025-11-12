package com.freaky.iulms.model

import java.io.Serializable

data class VoucherItem(
    val voucherNumber: String,
    val semester: String,
    val dueDate: String,
    val installmentNumber: String,
    val description: String,
    val amount: String,
    val isLate: Boolean,
    val printableVoucherNumber: String, // Hidden form value
    val printableStudentId: String      // Hidden form value
) : Serializable
