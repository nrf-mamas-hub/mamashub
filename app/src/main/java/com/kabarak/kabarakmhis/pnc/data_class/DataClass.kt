package com.kabarak.kabarakmhis.pnc.data_class

data class Child(
    val id: String,
    val name: String,
    val birthDate: String,
)

data class OtherProblems(
    val id: String,
    val sleepingProblems: String,
    val irritability: String,
    val othersSpecify: String,
)

data class BroadClinical(
    val id: String,
    val age: String,
    val weight: String,
    val length: String

)
data class Detail(
    val detailQuestion: String?,
    val detailAnswer: String?
)

data class CancerScreening(
    val id: String,
    val type: String,
    val date: String,
//    val responseId: String,

    )