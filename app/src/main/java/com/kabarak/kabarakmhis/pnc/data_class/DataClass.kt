package com.kabarak.kabarakmhis.pnc.data_class

data class Detail(
    val detailQuestion: String?,
    val detailAnswer: String?
)
data class Child(
    val id: String,
    val name: String?,
    val birthDate: String?,

)
data class Patient(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String
)




data class ChildDetail(
    val question: String,
    val answer: String
)

data class CongenitalAbnormality(
    val id: String,
    val description: String,
    val remarks: String?
)

data class BcgVaccination(
    val id: String,
    val status: String,
    val dateGiven: String?,
    val dateOfNextVisit: String?,
    val batchNumber: String?,
    val lotNumber: String?,
    val manufacturer: String?,
    val dateOfExpiry: String?
)

data class PolioVaccination(
    val id: String,
    val status: String,
    val dose: String?,
    val dateGiven: String?,
    val dateOfNextVisit: String?,
    val batchNumber: Int?,
    val lotNumber: Int?,
    val manufacturer: String?,
    val dateOfExpiry: String?
)

data class MeaslesImmunization(
    val id: String,
    val visit: String,
    val dose: String?,
    val batchNumber: String?,
    val lotNumber: String?,
    val manufacturer: String?,
    val dateOfExpiry: String?
)

data class ChildItem(
    val id: String,
    val name: String,
    val gender: String,
    val dob: String
)

data class CivilRegistration(
    val id: String,
    val name: String,
    val sexOfChild: String,
    val birthDate: String,
)

data class IPV(
    val id: String,
    val dateGiven: String,
    val nextVisit: String,
)

data class Diphtheria(
    val id: String,
    val dose: String,
    val date: String,
    val nextDate: String,
    val batch: String?,
    val lotnumber: String?,
    val manufacturer: String?,
    val expiryDate: String?
)

data class Milestone(
    val id: String,
    val visit: String,
    val age: String,
    val time: String
)

data class QuestionnaireDetails(
    val detailQuestion: String,
    val detailAnswer: String,
)

data class CancerScreening(
    val id: String,
    val type: String,
    val date: String,
    val responseId: String,

)

data class EyeProblems(
    val id: String,
    val VisitType: String,
    val VisitDate: String,
)

data class BroadClinical(
    val id: String,
    val age: String,
    val weight: String,
    val length: String

)

data class OtherProblems(
    val id: String,
    val sleepingProblems: String,
    val irritability: String,
    val othersSpecify: String,
)

data class Vaccines(
    val id: String,
    val VaccineName: String,
    val VaccineDate: String,
)
data class BabyTeethRecordDataClass(
    val id: String,
    val dateSeen: String,
    val ageWhenSeen: String,
    val teethType: String?
)

data class ReasonsForSpecialCare(
    val id: String,
    val reasons: List<String>)
