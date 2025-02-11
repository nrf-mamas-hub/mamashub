package com.kabarak.kabarakmhis.network_request.requests

import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.kabarak.kabarakmhis.fhir.data.SYNC_VALUE
import com.kabarak.kabarakmhis.helperclass.*
import com.kabarak.kabarakmhis.network_request.builder.RetrofitBuilder
import com.kabarak.kabarakmhis.network_request.interfaces.Interface
import com.kabarak.kabarakmhis.new_designs.data_class.*
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class RetrofitCallsFhir {

    fun createPatient(context: Context, dbPatient: DbPatient) {

        CoroutineScope(Dispatchers.Main).launch {

            val job = Job()
            CoroutineScope(Dispatchers.IO + job).launch {

                createPatientBac(context, dbPatient)

            }.join()
        }

    }

    private suspend fun createPatientBac(context: Context, dbPatient: DbPatient) {

        val job1 = Job()
        CoroutineScope(Dispatchers.Main + job1).launch {

            var progressDialog = ProgressDialog(context)
            progressDialog.setTitle("Please wait..")
            progressDialog.setMessage("Patient creation in progress..")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()


            var messageToast = ""
            val job = Job()
            CoroutineScope(Dispatchers.IO + job).launch {

                var formatter = FormatterClass()

                val baseUrl = context.getString(UrlData.FHIR_URL.message)

                val apiService = RetrofitBuilder.getRetrofit(baseUrl).create(Interface::class.java)
                val apiInterface = apiService.createFhirPatient(dbPatient)
                apiInterface.enqueue(object : Callback<DbPatientSuccess> {
                    override fun onResponse(
                        call: Call<DbPatientSuccess>,
                        response: Response<DbPatientSuccess>
                    ) {

                        CoroutineScope(Dispatchers.Main).launch { progressDialog.dismiss() }

                        if (response.isSuccessful) {
                            messageToast = "User details added successfully."

                            val responseData = response.body()

                            if (responseData != null) {

                                Log.e("*** ", responseData.toString())

                            }

//                            val intent = Intent(context, NewMainActivity::class.java)
//                            context.startActivity(intent)

                            CoroutineScope(Dispatchers.Main).launch {

                                Toast.makeText(context, messageToast, Toast.LENGTH_SHORT).show()

                            }

                        } else {

                            progressDialog.dismiss()

                            val code = response.code()
                            val message = response.errorBody().toString()

                            if (code != 500) {

                                val jObjError = response.errorBody()?.string()
                                    ?.let { JSONObject(it) }

                                CoroutineScope(Dispatchers.IO).launch {

                                    messageToast = "There was an issue."

//                                    messageToast = Formatter().getObjectiveKeys(
//                                        jObjError
//                                    ).toString()

                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(context, messageToast, Toast.LENGTH_SHORT)
                                            .show()
                                    }

                                }

                            } else {
                                messageToast =
                                    "We are experiencing some server issues. Please try again later"

                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(context, messageToast, Toast.LENGTH_SHORT).show()
                                }
                            }


                        }


                    }

                    override fun onFailure(call: Call<DbPatientSuccess>, t: Throwable) {
                        Log.e("-*-*error ", t.localizedMessage)
                        messageToast = "There is something wrong. Please try again"
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, messageToast, Toast.LENGTH_SHORT).show()
                        }

                        progressDialog.dismiss()
                    }
                })


            }.join()

        }

    }

    fun getPatients(context: Context) = runBlocking {

        getPatientsBac(context)

    }

    private suspend fun getPatientsBac(context: Context): DbPatientResult {

        var confList = DbPatientResult(0, ArrayList())
        val job = Job()
        CoroutineScope(Dispatchers.IO + job).launch {


            val baseUrl = context.getString(UrlData.FHIR_URL.message)

            val apiService = RetrofitBuilder.getRetrofit(baseUrl).create(Interface::class.java)
            val callSync: Call<DbPatientResult> = apiService.getPatientList(SYNC_VALUE)

            try {
                val response: Response<DbPatientResult> = callSync.execute()
                if (response.isSuccessful) {

                    val list = response.body()
                    if (list != null) {
                        confList = list
                    }

                }


            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }.join()

        return confList

    }





    fun createObservation(encounterId: String, context: Context, dbCode: DbCode) {

        CoroutineScope(Dispatchers.IO).launch {

            val formatter = FormatterClass()

            val patientId = formatter.retrieveSharedPreference(context, "patientId")
            val id = formatter.generateUuid()
            val subject = DbSubject("Patient/$patientId")
            val encounter = DbEncounterData("Encounter/$encounterId")

            val dbObservation =
                DbObservation(DbResourceType.Observation.name, id, subject, encounter, dbCode)

            val baseUrl = context.getString(UrlData.FHIR_URL.message)

            val apiService = RetrofitBuilder.getRetrofit(baseUrl).create(Interface::class.java)
            val apiInterface = apiService.createFhirObservation(dbObservation)

            apiInterface.enqueue(object : Callback<DbObservation> {
                override fun onResponse(
                    call: Call<DbObservation>,
                    response: Response<DbObservation>
                ) {

                    if (response.isSuccessful) {

                        val responseData = response.body()

                        if (responseData != null) {

                            Log.e("***1 ", responseData.toString())

                        }



                        CoroutineScope(Dispatchers.Main).launch {

                            Toast.makeText(
                                context,
                                "User data has been saved successfully.",
                                Toast.LENGTH_SHORT
                            ).show()

//                            val intent = Intent(context, NewMainActivity::class.java)
//                            context.startActivity(intent)

                        }

                    } else {

                        val code = response.code()
                        val message = response.errorBody().toString()
                        Log.e("***2 ", response.toString())

                        if (code != 500) {

//                            val jObjError = JSONObject(response.errorBody()?.string())

                            CoroutineScope(Dispatchers.Main).launch {

                                Toast.makeText(
                                    context,
                                    "There was an issue. Please try again after some time.",
                                    Toast.LENGTH_SHORT
                                ).show()

                            }

                        } else {
                            val messageToast =
                                "We are experiencing some server issues. Please try again later"

                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, messageToast, Toast.LENGTH_SHORT).show()
                            }
                        }


                    }


                }

                override fun onFailure(call: Call<DbObservation>, t: Throwable) {
                    Log.e("-*-*error ", t.localizedMessage)
                    val messageToast = "There is something wrong. Please try again"
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, messageToast, Toast.LENGTH_SHORT).show()
                    }

//                    progressDialog.dismiss()
                }
            })

        }

    }


    fun getPatientEncounters(context: Context) = runBlocking {
        getPatientEncountersBac(context)
    }

    private suspend fun getPatientEncountersBac(context: Context) {

        coroutineScope {

            launch(Dispatchers.IO) {
                val formatter = FormatterClass()
                val patientId = formatter.retrieveSharedPreference(context, "patientId")

                FormatterClass().nukeEncounters(context)

                val baseUrl = context.getString(UrlData.FHIR_URL.message)

                val apiService = RetrofitBuilder.getRetrofit(baseUrl).create(Interface::class.java)

                if (patientId != null) {

                    val callSync: Call<DbEncounterList> = apiService.getEncounterList(patientId)

                    try {
                        val response: Response<DbEncounterList> = callSync.execute()
                        if (response.isSuccessful) {

                            val list = response.body()
                            if (list != null) {

                                val encounterList = list.entry
                                if (!encounterList.isNullOrEmpty()) {
                                    for (items in encounterList) {


                                        val encounterId = items.resource.id
                                        val reasonCode = items.resource.reasonCode[0].text

                                        formatter.saveSharedPreference(
                                            context,
                                            reasonCode,
                                            encounterId
                                        )

                                    }

                                }

                            }

                        }


                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }

                }

            }

        }


    }



    fun getEncounterDetails(context: Context, encounterId: String, encounterType: String) = runBlocking {
        getEncounterDetailsBac(context, encounterId, encounterType)
    }

    private fun getEncounterDetailsBac(context: Context, encounterId: String, encounterType: String) : ArrayList<DbObserveValue>{

        var simpleEncounterList = ArrayList<DbObserveValue>()

        val baseUrl = context.getString(UrlData.FHIR_URL.message)

        val apiService = RetrofitBuilder.getRetrofit(baseUrl).create(Interface::class.java)

        val callEncounterSync: Call<DbEncounterDetailsList> =
            apiService.getEncounterDetails(encounterId)
        val responseEncounter: Response<DbEncounterDetailsList> = callEncounterSync.execute()
        if (responseEncounter.isSuccessful) {

            Log.e("----1", responseEncounter.toString())

            val reasonBody = responseEncounter.body()
            Log.e("----2", reasonBody.toString())
            if (reasonBody != null) {

                val entryList = reasonBody.entry
                Log.e("----3", entryList.toString())
                if (!entryList.isNullOrEmpty()) {
                    simpleEncounterList = encounterOperations(entryList, encounterType)

                }
            }

        }
        return simpleEncounterList
    }

    private fun encounterOperations(entryList: List<DbEncounterDataEntry>, encounterType: String) :ArrayList<DbObserveValue>{

        val simpleEncounterList = ArrayList<DbObserveValue>()
        var count = 0
        var countPreventive = 0
        var countMalaria = 0
        var countIFAS = 0

        val weightList = ArrayList<String>()
        val gestationList = ArrayList<String>()

        for (observations in entryList) {

            val id = observations.resource.id
            val code = observations.resource.code
            val resourceType = observations.resource.resourceType

            if (code != null) {

                val codingList = code.coding
                val text = code.text

                if (text == DbResourceViews.PHYSICAL_EXAMINATION.name){

                    count += 1

                    val dbObserveValue = DbObserveValue(id, "$count visit")
                    simpleEncounterList.add(dbObserveValue)

                }
                if (text == DbResourceViews.PREVENTIVE_SERVICE.name){

                    countPreventive += 1

                    val dbObserveValue = DbObserveValue(id, "TT $countPreventive")
                    simpleEncounterList.add(dbObserveValue)

                }
                if (text == DbResourceViews.MALARIA_PROPHYLAXIS.name){

                    countMalaria += 1

                    val dbObserveValue = DbObserveValue(id, "ANC Contact $countMalaria")
                    simpleEncounterList.add(dbObserveValue)

                }
                if (text == DbResourceViews.IFAS.name){
                    val dbObserveValue = if (countIFAS == 0){
                         DbObserveValue(id, "First contact before first ANC (12 weeks)")
                    }else{
                        DbObserveValue(id, "ANC Contact $countIFAS")
                    }

                    countIFAS += 1


                    simpleEncounterList.add(dbObserveValue)

                }

                if (codingList != null){

                    for (items in codingList){

                        val codeValue = items.code
                        val displayData = items.display
                        val display = displayData ?: ""

                        if (encounterType == DbResourceViews.CLINICAL_NOTES.name){

                            if (codeValue == "Next Appointment"){
                                val dbObserveValue = DbObserveValue(id, display)
                                simpleEncounterList.add(dbObserveValue)
                            }

                        }
                        if (encounterType == DbResourceViews.WEIGHT_MONITORING.name){

                            simpleEncounterList.clear()

                            if (codeValue == "Mother Weight"){
                                weightList.add(display)
                            }
                            if (codeValue == "Gestation"){
                                gestationList.add(display)
                            }

                        }

                        if (text == DbResourceViews.BIRTH_PLAN.name){

                            val dbObserveValue = DbObserveValue(codeValue, display)
                            simpleEncounterList.add(dbObserveValue)

                        }
                        if (text == DbResourceViews.COUNSELLING.name){

                            val dbObserveValue = DbObserveValue(codeValue, display)
                            simpleEncounterList.add(dbObserveValue)

                        }
                        if (text == DbResourceViews.MEDICAL_HISTORY.name){

                            val dbObserveValue = DbObserveValue(codeValue, display)
                            simpleEncounterList.add(dbObserveValue)

                        }
                        if (text == DbResourceViews.ANTENATAL_PROFILE.name){

                            val dbObserveValue = DbObserveValue(codeValue, display)
                            simpleEncounterList.add(dbObserveValue)

                        }

                        if (text == DbResourceViews.PRESENT_PREGNANCY.name){

                            if (codeValue == "Pregnancy Contact"){
                                val dbObserveValue = DbObserveValue(id, display)
                                simpleEncounterList.add(dbObserveValue)
                            }

                        }
                        if (text == DbResourceViews.MALARIA_PROPHYLAXIS.name){

                            if (codeValue == "ANC Contact"){
                                val dbObserveValue = DbObserveValue(id, display)
                                simpleEncounterList.add(dbObserveValue)
                            }

                        }
                    }


                }



            }

        }


        for ((index, value) in weightList.withIndex()) {

            val gestation = gestationList[index]

            val dbObserveValue = DbObserveValue(value, gestation)
            simpleEncounterList.add(dbObserveValue)
        }

        Log.e("----4", simpleEncounterList.toString())

        return simpleEncounterList

    }


    fun getObservationDetails(context: Context, observationId: String) = runBlocking {
        getObservationDetailsBac(context, observationId)
    }

    private suspend fun getObservationDetailsBac(context: Context, observationId: String):ArrayList<DbCodingData>{

        val observationList = ArrayList<DbCodingData>()
        val job = Job()
        CoroutineScope(Dispatchers.IO + job).launch {

            val baseUrl = context.getString(UrlData.FHIR_URL.message)

            val apiService = RetrofitBuilder.getRetrofit(baseUrl).create(Interface::class.java)

            val callSync: Call<DbEncounterDataResourceData> = apiService.getObservationDetails(observationId)

            try {
                val response: Response<DbEncounterDataResourceData> = callSync.execute()
                if (response.isSuccessful){

                    val responseBody = response.body()

                    if (responseBody != null){

                        val codeData = responseBody.code
                        if (codeData != null){

                            val encounterList = codeData.coding
                            if (!encounterList.isNullOrEmpty()){

                                for (items in encounterList){

                                    val system = items.system
                                    val code = items.code
                                    val display = items.display
                                    val displayData = display ?: ""


                                    val dbCodingData = DbCodingData(system, code, displayData)
                                    observationList.add(dbCodingData)

                                    Log.e("+++++code ", code)
                                    Log.e("+++++display ", displayData)
                                    Log.e("+++++++++", "+++++")

                                }

                            }

                        }

                    }

                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }.join()

        return observationList

    }

    private val apiService = RetrofitBuilder.getRetrofit("http://192.168.253.236/fhir/").create(Interface::class.java)

    fun submitQuestionnaireResponse(questionnaireResponseString: String, callback: Callback<ResponseBody>) {
        val mediaType = "application/fhir+json".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, questionnaireResponseString)
        val call = apiService.submitQuestionnaireResponse(requestBody)
        call.enqueue(callback)
    }
    fun updateQuestionnaireResponse(responseId: String, questionnaireResponseString: String, callback: Callback<ResponseBody>) {
        val mediaType = "application/fhir+json".toMediaTypeOrNull()  // Ensure correct media type for FHIR JSON
        val requestBody = RequestBody.create(mediaType, questionnaireResponseString)  // Create request body

        // Make the API call to submit the updated QuestionnaireResponse
        val call = apiService.submitQuestionnaireResponse(responseId, requestBody)
        call.enqueue(callback)  // Enqueue the call to run asynchronously
    }

    fun fetchQuestionnaireResponse(responseId: String, callback: Callback<ResponseBody>) {
        apiService.getQuestionnaireResponse(responseId).enqueue(callback)
    }
    fun fetchAllQuestionnaireResponses(callback: Callback<ResponseBody>) {
        val call = apiService.getAllQuestionnaireResponses()
        call.enqueue(callback)
    }
}
