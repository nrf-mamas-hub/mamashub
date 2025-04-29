package com.kabarak.kabarakmhis.pnc.microNutrients

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.kabarak.kabarakmhis.R
import com.kabarak.kabarakmhis.fhir.FhirApplication
import com.kabarak.kabarakmhis.fhir.viewmodels.PatientDetailsViewModel
import com.kabarak.kabarakmhis.helperclass.FormatterClass
import com.kabarak.kabarakmhis.network_request.requests.RetrofitCallsFhir
import com.kabarak.kabarakmhis.pnc.data_class.MicroNutrients
import com.kabarak.kabarakmhis.pnc.data_class.PolioVaccination
import com.kabarak.kabarakmhis.pnc.data_class.ReasonsForSpecialCare
import com.kabarak.kabarakmhis.pnc.reasonsForSpecialCare.ReasonsAddActivity
import com.kabarak.kabarakmhis.pnc.reasonsForSpecialCare.ReasonsEditActivity
import com.kabarak.kabarakmhis.pnc.reasonsForSpecialCare.ReasonsViewAdapter
import kotlinx.android.synthetic.main.activity_child_birth_view.tvANCID
import kotlinx.android.synthetic.main.activity_child_birth_view.tvAge
import kotlinx.android.synthetic.main.activity_child_birth_view.tvName
import kotlinx.android.synthetic.main.activity_reasons_view.btnAdd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.QuestionnaireResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MicroNutrientsViewActivity : AppCompatActivity() {
    private var microNutrients: MutableList<MicroNutrients> = mutableListOf()
    private lateinit var retrofitCallsFhir: RetrofitCallsFhir
    private lateinit var noRecordView: View
    private lateinit var fhirEngine: FhirEngine
    private lateinit var formatter: FormatterClass
    private lateinit var patientId: String
    private lateinit var patientDetailsViewModel: PatientDetailsViewModel
    private lateinit var microNutrientsRC: RecyclerView
    private lateinit var microNutrientsAdapter: MicroNutrientsViewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_micro_nutrients_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        noRecordView = findViewById(R.id.no_record)
        microNutrientsRC = findViewById(R.id.recycler_view_micro_nutrients)

        formatter = FormatterClass()
        fhirEngine = FhirApplication.fhirEngine(this)

        patientId = formatter.retrieveSharedPreference(this, "patientId").toString()

        patientDetailsViewModel = ViewModelProvider(
            this,
            PatientDetailsViewModel.PatientDetailsViewModelFactory(
                application,
                fhirEngine,
                patientId
            )
        )[PatientDetailsViewModel::class.java]

        btnAdd.setOnClickListener {
            val intent = Intent(this, ReasonsAddActivity::class.java)
            startActivity(intent)
        }

        noRecordView = findViewById(R.id.no_record)

        microNutrientsRC = findViewById(R.id.recycler_view_reasons)
        microNutrientsRC.layoutManager = LinearLayoutManager(this)

        microNutrientsAdapter = MicroNutrientsViewAdapter(microNutrients) { id ->
            val responseId = extractResponseId(id)
            Toast.makeText(this, "Response ID: $id", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MicroNutrientsEditActivity::class.java)
            intent.putExtra("responseId", responseId)
            startActivity(intent)
        }
        microNutrientsRC.adapter = microNutrientsAdapter

        retrofitCallsFhir = RetrofitCallsFhir()

        fetchReasonsFromFHIR()
        fetchPatientData()
    }

    private fun fetchPatientData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val patientLocalName =
                    formatter.retrieveSharedPreference(
                        this@MicroNutrientsViewActivity,
                        "patientName"
                    )
                val patientLocalDob =
                    formatter.retrieveSharedPreference(this@MicroNutrientsViewActivity, "dob")
                val patientLocalIdentifier =
                    formatter.retrieveSharedPreference(
                        this@MicroNutrientsViewActivity,
                        "identifier"
                    )

                if (patientLocalName.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val progressDialog = ProgressDialog(this@MicroNutrientsViewActivity)
                        progressDialog.setTitle("Please wait...")
                        progressDialog.setMessage("Fetching patient details...")
                        progressDialog.show()

                        var patientName = ""
                        var dob = ""
                        var identifier = ""

                        val job = Job()
                        CoroutineScope(Dispatchers.IO + job).launch {
                            val patientData = getPatientDataFromFhirEngine()
                            patientName = patientData.first
                            dob = patientData.second

                            formatter.saveSharedPreference(
                                this@MicroNutrientsViewActivity,
                                "patientName",
                                patientName
                            )
                            formatter.saveSharedPreference(
                                this@MicroNutrientsViewActivity,
                                "dob",
                                dob
                            )

                            if (identifier.isNotEmpty()) {
                                formatter.saveSharedPreference(
                                    this@MicroNutrientsViewActivity,
                                    "identifier",
                                    identifier
                                )
                            }
                        }.join()

                        showPatientDetails(patientName, dob, identifier)

                        progressDialog.dismiss()
                    }
                } else {
                    // Display the data from local storage
                    showPatientDetails(patientLocalName, patientLocalDob, patientLocalIdentifier)
                }
            } catch (e: Exception) {
                Log.e("ChildViewActivity", "Error fetching patient data: ${e.message}")
            }
        }
    }

    private fun showPatientDetails(patientName: String, dob: String?, identifier: String?) {
        tvName.text = patientName
        if (!identifier.isNullOrEmpty()) tvANCID.text = identifier
        if (!dob.isNullOrEmpty()) tvAge.text = "${formatter.calculateAge(dob)} years"
    }

    private fun getPatientDataFromFhirEngine(): Pair<String, String> {
        // Use FHIR engine to fetch patient data, then return the name and date of birth
        val patientData = patientDetailsViewModel.getPatientData()
        val patientName = patientData.name
        val dob = patientData.dob

        return Pair(patientName, dob)
    }

    private fun fetchReasonsFromFHIR() {
        lifecycleScope.launch(Dispatchers.IO) {
            retrofitCallsFhir.fetchAllQuestionnaireResponses(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { responseBody ->
                            val rawResponse = responseBody.string()
                            val fhirContext = FhirContext.forR4()
                            val parser = fhirContext.newJsonParser()
                            val bundle = parser.parseResource(
                                org.hl7.fhir.r4.model.Bundle::class.java,
                                rawResponse
                            )

                            microNutrients.clear()
                            extractReasonsFromBundle(bundle)
                            runOnUiThread { toggleViews() }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@MicroNutrientsViewActivity,
                                "Failed to fetch data",
                                Toast.LENGTH_SHORT
                            ).show()
                            toggleViews()
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MicroNutrientsViewActivity,
                            "Error: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        toggleViews()
                    }
                }
            })
        }
    }

    private fun extractReasonsFromBundle(bundle: org.hl7.fhir.r4.model.Bundle) {
        for (entry in bundle.entry) {
            val fullUrl = entry.fullUrl
            val responseId = extractResponseId(fullUrl)

            val resource = entry.resource
            if (resource is QuestionnaireResponse) {
                parseReasonsResponse(resource, responseId)
            }
        }

        runOnUiThread {
            microNutrientsAdapter.notifyDataSetChanged()
        }
    }

    private fun parseReasonsResponse(
        questionnaireResponse: QuestionnaireResponse,
        responseId: String
    ) {

        var age: Int? = null
        var noIssued: Int? = null
        var dateIssued: String? = null
        var dateOfNextVisit: String? = null

        questionnaireResponse.item.forEach { item ->
            if (item.text == "POLIO VACCINE: (Bivalent Oral Polio Vaccine(bOPV):") {

                // Parse nested items
                item.answer.firstOrNull()?.item?.forEach { subItem ->
                    when (subItem.text) {

                        "Age in months" -> age =
                            subItem.answer.firstOrNull()?.valueIntegerType?.value

                        "Date issued" -> {
                            dateIssued = subItem.answer.firstOrNull()?.let { answer ->
                                when (val value = answer.value) {
                                    is org.hl7.fhir.r4.model.DateTimeType -> value.valueAsString
                                    is org.hl7.fhir.r4.model.DateType -> value.valueAsString
                                    else -> null
                                }
                            }
                        }

                        "Date of next visit" -> {
                            dateOfNextVisit = subItem.answer.firstOrNull()?.let { answer ->
                                when (val value = answer.value) {
                                    is org.hl7.fhir.r4.model.DateTimeType -> value.valueAsString
                                    is org.hl7.fhir.r4.model.DateType -> value.valueAsString
                                    else -> null
                                }
                            }
                        }

                        "Number issued" -> noIssued = subItem.answer.firstOrNull()?.valueIntegerType?.value

                    }
                }
            }
        }


            microNutrients.add(
                MicroNutrients(
                    id = responseId,
                    age = age,
                    noIssued = noIssued,
                    dateIssued = dateIssued,
                    dateOfNextVisit = dateOfNextVisit,

                )
            )


    }

    private fun extractResponseId(fullUrl: String): String {
        val regex = Regex("QuestionnaireResponse/(\\d+)")
        val matchResult = regex.find(fullUrl)
        return matchResult?.groupValues?.get(1) ?: fullUrl
    }


    private fun toggleViews() {
        if ( microNutrients.isEmpty()) {
            microNutrientsRC.visibility = View.GONE
            noRecordView.visibility = View.VISIBLE
        } else {
            microNutrientsRC.visibility = View.VISIBLE
            noRecordView.visibility = View.GONE
        }
    }
}