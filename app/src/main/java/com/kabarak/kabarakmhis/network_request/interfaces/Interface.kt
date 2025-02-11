package com.kabarak.kabarakmhis.network_request.interfaces

import com.kabarak.kabarakmhis.helperclass.*
import com.kabarak.kabarakmhis.new_designs.data_class.*
import com.kabarak.kabarakmhis.new_designs.data_class.DbPatientSuccess
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*


val param = "$ everything"

interface Interface {


    @POST("auth/login/")
    suspend fun loginUser(@Body user: UserLogin): Response<AuthResponse>

    @GET("auth/me/")
    suspend fun getUserData(@HeaderMap headers: Map<String, String>): Response<DbUserData>


    @POST("Patient")
    fun createFhirPatient(@Body dbPatient: DbPatient): Call<DbPatientSuccess>

    @GET("Patient")
    fun getPatientList(@Query("address-country") address_state:String): Call<DbPatientResult>

    @POST("Encounter")
    fun createFhirEncounter(@Body dbEncounter: DbEncounter): Call<DbEncounter>

    @PUT("Encounter/{encounterId}")
    fun updateFhirEncounter(@Body dbEncounter: DbEncounter): Call<DbEncounter>

    @GET("Encounter/")
    fun getEncounterList(@Query("patient") patient:String): Call<DbEncounterList>

    @GET("Encounter/{encounterId}/"+"$"+"everything")
    fun getEncounterDetails(
        @Path("encounterId") encounterId:String): Call<DbEncounterDetailsList>

    @POST("Observation")
    fun createFhirObservation(@Body dbObservation: DbObservation): Call<DbObservation>

    //This should be looked at
    @GET("Observation/{observationId}")
    fun getObservationDetails(
        @Path("observationId") observationId:String
    ): Call<DbEncounterDataResourceData>


    @POST("QuestionnaireResponse")
    @Headers("Content-Type: application/fhir+json")
    fun submitQuestionnaireResponse(@Body body: RequestBody): Call<ResponseBody>
    @GET("QuestionnaireResponse/{id}")
    fun getQuestionnaireResponse(@Path("id") responseId: String): Call<ResponseBody>

    @GET("QuestionnaireResponse")
    fun getAllQuestionnaireResponses(): Call<ResponseBody>


    @PUT("QuestionnaireResponse/{id}")
    fun submitQuestionnaireResponse(
        @Path("id") id: String,
        @Body requestBody: RequestBody
    ): Call<ResponseBody>


}