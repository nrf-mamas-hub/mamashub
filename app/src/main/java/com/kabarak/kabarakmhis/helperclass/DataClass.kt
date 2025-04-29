package com.kabarak.kabarakmhis.helperclass

import android.graphics.drawable.Drawable
import com.google.gson.annotations.SerializedName
import com.kabarak.kabarakmhis.R
import com.kabarak.kabarakmhis.new_designs.data_class.CodingObservation
import com.kabarak.kabarakmhis.new_designs.data_class.DbIdentifier
import com.kabarak.kabarakmhis.new_designs.data_class.QuantityObservation
import java.time.LocalDate

enum class UrlData(var message: Int) {
    BASE_URL(R.string.base_url),
    FHIR_URL(R.string.fhir_url)
}
data class SuccessLogin(
    val details: String
)
data class UserLogin(
    val email: String,
    val password: String
)
data class UserResetPassword(
    val emailAddress: String,

    )
data class AuthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("token") val token: String,
    @SerializedName("issued") val issued: String,
    @SerializedName("expires") val expires: String,
    @SerializedName("newUser") val newUser: Boolean
    )
data class PatientItem(
    val id: String,
    val resourceId: String,
    val name: String,
    val gender: String,
    val dob: LocalDate? = null,
    val phone: String,
    val city: String,
    val country: String,
    val isActive: Boolean,
    val html: String,
    var risk: String? = "",
) {
    override fun toString(): String = name
}

data class DbPatientDetails(
    val id : String,
    val name : String,
    val lastUpdated: String,
    val dob: String = "",
    val kmflCode: String? = null
)
data class DbChwPatientData(
    val id : String,
    val name : String,
    val dob: String,
    val referralDate: String,
    val lastUpdated: String,

    )

data class RiskAssessmentItem(
    var riskStatusColor: Int,
    var riskStatus: String,
    var lastContacted: String,
    var patientCardColor: Int
)
data class RelatedPersonItem(
    val id: String,
    val name: String,
    val gender: String,
    val dob: String,
    var riskItem: RiskAssessmentItem? = null,
) {
    override fun toString(): String = name
}
data class ObservationItem(
    val id: String,
    val code: String?,
    val text: String,
    val value: String,
    var issued: String? = null,
    var issuedTime: String? = null,
)
data class ConditionItem(
    val id: String,
    val code: String,
    val effective: String,
    val value: String
) {
    override fun toString(): String = code
}
enum class ReferralTypes {
    REFERRAL_TO_FACILITY,
    REFERRAL_TO_CHW,
    REFERRAL_TYPE,

    CLIENT_RECORDS,
    REFERRED

}
data class EncounterItem(
    val id: String,
    val code: String,
    val effective: String,
    val value: String
) {
    override fun toString(): String = code
}
data class Steps(
    val fistIn: String?,
    val lastIn: String?,
    val secondButton: Boolean?
)
enum class ViewType {
    HEADER,
    PATIENT,
    CHILD,
    PATIENT_PROPERTY,
    RELATION,
    OBSERVATION,
    ENCOUNTER,
    CONDITION;

    companion object {
        fun from(ordinal: Int): ViewType {
            return values()[ordinal]
        }
    }

}


/**
 * Encounter
 */


enum class ObservationViewTypes {
    OBSERVATION,
    CONDITION;

    companion object {
        fun from(ordinal: Int): ObservationViewTypes {
            return values()[ordinal]
        }
    }

}

enum class Navigation{
    FRAGMENT,
    ACTIVITY
}



data class DbUserData(
    val data: DbData,
    val status: String
)
data class DbData(
    val id: String,
    val names: String,
    val email: String,
    val role: String,
    val kmhflCode: String? = null,
    val facilityName: String? = null,
)

data class DbAddressData(
    val text: String,
    val city: String,
    val district: String,
    val state: String
)
data class DbPatientRecord(
    val id: String,
    val name: String,
    val dob: String,
    val phone: String?,
    val kinData: DbKinData,
    val identifier: ArrayList<DbIdentifier>,
    val maritalStatus: String,
    val address: ArrayList<DbAddressData>,
)
data class DbKinData(
    val name: String,
    val phone: String,
    val relationship: String
)
data class DbObservationLabel(
    val value: String,
    val label: String
)
data class DbWeightChart(
    val gestation: Float,
    val weight: Float,
)
data class DbChwData(
    val id: String,
    val name: String,
    val dob: String,
    val dataQuantityList: ArrayList<QuantityObservation>,
    val dataCodeList: ArrayList<CodingObservation>,
)
data class DbSmartWatchReadings(
    val dateIssued: String,
    val recordingList : ArrayList<DbWatchReading>
)

data class Model(
    val namehello: String,
    val date:String,
    val apilevel: String,
    val description : String,
    var expandable : Boolean = false)

data class DbMaternalProfile(
    val title: String,
    val childrenList : ArrayList<DbMaternalProfileChild>,
    val isPatient : Boolean,
    var expandable : Boolean = false,

)
data class DbMaternalProfileChild(
    val id:Double,
    val image: Drawable,
    val title: String,

)
data class DbPMTCTRegimen(
    val regimen: String,
    val amount: Double,
    val dosage: Double,
    val frequency: String,
)
data class DbServiceReferralRequest(
    val id: String,
    val referralType: String,
    val patient: String,
    val authoredOn: String,
    val referralDetails: List<DbReasonCodeData>,
    val supportingInfo: List<DbSupportingInfo>,
    val actionTaken: String,

    val chwDetails: DbChwDetails,
    val clinicianDetails: DbClinicianDetails,
    val locationDetails: DbLocation

)
data class DbLocation(
    val facilityName: String,
    val facilityCode: String,
)
data class DbClinicianDetails(
    val clinicianRole: String,
    val clinicianId: String
)
data class DbChwDetails(
    val loggedInUserId: String,
    val loggedInChwUnit: String
)
data class DbReasonCodeData(
    val text: String,
    val code: String,
    val display: String
)
data class DbSupportingInfo(
    val reference: String,
    val display: String
)
data class DbAppointments(
    val id: String,
    val title: String,
    val date: String,

    )
data class DbWatchReading(
    val text: String,
    val value: String
)

data class DbWatchData(
    val date: String,
    val readings: ArrayList<DbWatchDataValues>
)
data class DbWatchDataValues(
    val time: String,
    val text: String,
    val value: String
)



data class DbWatchTimeData(
    val time: String,
    val readings: DbWatchRecord
)
data class DbWatchRecord(
    val systolic: String,
    val diastolic: String,
    val pulse: String,
)

data class DbFhirIdentifier(
    val id: String,
    val values: String
)