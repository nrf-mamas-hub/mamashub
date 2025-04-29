package com.kabarak.kabarakmhis.pnc.reasonsForSpecialCare

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
import com.kabarak.kabarakmhis.pnc.data_class.ReasonsForSpecialCare
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

class ReasonsViewActivity : AppCompatActivity() {
    private var reasonsForSpecialCare: MutableList<ReasonsForSpecialCare> = mutableListOf()
    private lateinit var retrofitCallsFhir: RetrofitCallsFhir
    private lateinit var noRecordView: View
    private lateinit var fhirEngine: FhirEngine
    private lateinit var formatter: FormatterClass
    private lateinit var patientId: String
    private lateinit var patientDetailsViewModel: PatientDetailsViewModel
    private lateinit var reasonsRc: RecyclerView
    private lateinit var reasonsAdapter: ReasonsViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reasons_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        noRecordView = findViewById(R.id.no_record)
        reasonsRc = findViewById(R.id.recycler_view_reasons)

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

        reasonsRc = findViewById(R.id.recycler_view_reasons)
        reasonsRc.layoutManager = LinearLayoutManager(this)

        reasonsAdapter = ReasonsViewAdapter(reasonsForSpecialCare){ id ->
            val responseId = extractResponseId(id)
            Toast.makeText(this, "Response ID: $id", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, ReasonsEditActivity::class.java)
            intent.putExtra("responseId", responseId)
            startActivity(intent)
        }
        reasonsRc.adapter = reasonsAdapter

        retrofitCallsFhir = RetrofitCallsFhir()

        fetchReasonsFromFHIR()
        fetchPatientData()
    }

    private fun fetchPatientData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val patientLocalName =
                    formatter.retrieveSharedPreference(this@ReasonsViewActivity, "patientName")
                val patientLocalDob =
                    formatter.retrieveSharedPreference(this@ReasonsViewActivity, "dob")
                val patientLocalIdentifier =
                    formatter.retrieveSharedPreference(this@ReasonsViewActivity, "identifier")

                if (patientLocalName.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val progressDialog = ProgressDialog(this@ReasonsViewActivity)
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
                                this@ReasonsViewActivity,
                                "patientName",
                                patientName
                            )
                            formatter.saveSharedPreference(this@ReasonsViewActivity, "dob", dob)

                            if (identifier.isNotEmpty()) {
                                formatter.saveSharedPreference(
                                    this@ReasonsViewActivity,
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

                            reasonsForSpecialCare.clear()
                            extractReasonsFromBundle(bundle)
                            runOnUiThread { toggleViews() }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@ReasonsViewActivity,
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
                            this@ReasonsViewActivity,
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
            reasonsAdapter.notifyDataSetChanged()
        }
    }

    private fun parseReasonsResponse(
        questionnaireResponse: QuestionnaireResponse,
        responseId: String
    ) {

        val reasons = mutableListOf<String>()

        questionnaireResponse.item.forEach { item ->
            if (item.text == "Reason for Special Care (Tick as appropriate)") {
                item.answer.forEach { answer ->
                    val display = (answer.value as? Coding)?.display
                    if (!display.isNullOrBlank()) {
                        reasons.add(display)
                    } else {
                        item.answer.firstOrNull()?.item?.forEach { subItem ->
                            val pattern = Regex("""Any other\(specify\)\s*_+""")
                            if (pattern.containsMatchIn(subItem.text)) {
                                val reason = subItem.answer.firstOrNull()?.valueStringType?.value
                                reason?.let { reasons.add(it) }
                            }
                        }
                    }
                }
            }
        }
        reasonsForSpecialCare.add(
            ReasonsForSpecialCare(responseId, reasons)
        )

    }

    private fun extractResponseId(fullUrl: String): String {
        val regex = Regex("QuestionnaireResponse/(\\d+)")
        val matchResult = regex.find(fullUrl)
        return matchResult?.groupValues?.get(1) ?: fullUrl
    }


    private fun toggleViews() {
        if (reasonsForSpecialCare.isEmpty()) {
            reasonsRc.visibility = View.GONE
            noRecordView.visibility = View.VISIBLE
        } else {
            reasonsRc.visibility = View.VISIBLE
            noRecordView.visibility = View.GONE
        }
    }
}