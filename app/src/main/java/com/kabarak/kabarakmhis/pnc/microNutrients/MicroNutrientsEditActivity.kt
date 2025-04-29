package com.kabarak.kabarakmhis.pnc.microNutrients

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.kabarak.kabarakmhis.R
import com.kabarak.kabarakmhis.network_request.requests.RetrofitCallsFhir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.QuestionnaireResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MicroNutrientsEditActivity : AppCompatActivity() {
    private lateinit var retrofitCallsFhir: RetrofitCallsFhir
    private var questionnaireJsonString: String? = null
    private lateinit var responseId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_micro_nutrients_edit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        retrofitCallsFhir = RetrofitCallsFhir()

        // Load the Polio vaccination questionnaire JSON file
        questionnaireJsonString = getStringFromAssets("micro_nutrients.json")
        responseId = intent.getStringExtra("responseId") ?: ""

        if (savedInstanceState == null && questionnaireJsonString != null) {
            renderInitialQuestionnaire()
            CoroutineScope(Dispatchers.IO).launch {
                fetchAndPopulateQuestionnaireResponse(responseId)
            }
        }

        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.SUBMIT_REQUEST_KEY,
            this
        ) { _, _ ->
            updateQuestionnaireResponse()
        }
    }

    private fun getStringFromAssets(fileName: String): String? {
        return try {
            assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("ResponseEditActivity", "Error reading JSON from assets", e)
            null
        }
    }

    private fun renderInitialQuestionnaire() {
        val questionnaireFragment = QuestionnaireFragment.builder()
            .setQuestionnaire(questionnaireJsonString!!)
            .build()

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container_view, questionnaireFragment, "initial-questionnaire-fragment")
        }
    }

    private suspend fun fetchAndPopulateQuestionnaireResponse(responseId: String) {
        retrofitCallsFhir.fetchQuestionnaireResponse(responseId, object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                CoroutineScope(Dispatchers.Main).launch {
                    if (response.isSuccessful) {
                        response.body()?.string()?.let { questionnaireResponseString ->
                            try {
                                val fhirContext = FhirContext.forR4()
                                val jsonParser = fhirContext.newJsonParser()
                                val questionnaireResponse = jsonParser.parseResource(
                                    QuestionnaireResponse::class.java, questionnaireResponseString)
                                populateQuestionnaireFragment(questionnaireResponse)
                            } catch (e: Exception) {
                                Log.e("ResponseEditActivity", "Error parsing questionnaire response", e)
                                Toast.makeText(this@MicroNutrientsEditActivity, "Error populating questionnaire", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            Toast.makeText(this@MicroNutrientsEditActivity, "Failed to retrieve the response data.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("ResponseEditActivity", "Failed to fetch response. Response code: ${response.code()}")
                        Toast.makeText(this@MicroNutrientsEditActivity, "Failed to fetch the questionnaire response: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                CoroutineScope(Dispatchers.Main).launch {
                    Log.e("ResponseEditActivity", "Error occurred while fetching questionnaire response", t)
                    Toast.makeText(this@MicroNutrientsEditActivity, "Error occurred while fetching: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun populateQuestionnaireFragment(questionnaireResponse: QuestionnaireResponse) {
        try {
            val fhirContext = FhirContext.forR4()
            val jsonParser = fhirContext.newJsonParser()
            val questionnaireResponseString = jsonParser.encodeResourceToString(questionnaireResponse)

            val questionnaireFragment = QuestionnaireFragment.builder()
                .setQuestionnaire(questionnaireJsonString!!)
                .setQuestionnaireResponse(questionnaireResponseString)
                .build()

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container_view, questionnaireFragment, "populated-questionnaire-fragment")
            }

            Log.d("ResponseEditActivity", "Questionnaire response populated successfully.")
        } catch (e: Exception) {
            Log.e("ResponseEditActivity", "Error initializing the questionnaire fragment", e)
            Toast.makeText(this, "Error initializing questionnaire", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateQuestionnaireResponse() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
        if (fragment is QuestionnaireFragment) {
            val questionnaireResponse: QuestionnaireResponse = fragment.getQuestionnaireResponse()
            val fhirContext = FhirContext.forR4()
            val jsonParser = fhirContext.newJsonParser()

            questionnaireResponse.meta = questionnaireResponse.meta ?: Meta()
            val currentVersionId = questionnaireResponse.meta.versionId?.toIntOrNull() ?: 0
            questionnaireResponse.meta.versionId = (currentVersionId + 1).toString()
            questionnaireResponse.id = responseId

            val questionnaireResponseString = jsonParser.encodeResourceToString(questionnaireResponse)
            Log.d("updateQuestionnaireResponse", "Generated JSON for update: $questionnaireResponseString")

            retrofitCallsFhir.updateQuestionnaireResponse(responseId, questionnaireResponseString, object :
                Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    CoroutineScope(Dispatchers.Main).launch {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MicroNutrientsEditActivity, "Successfully updated!", Toast.LENGTH_SHORT).show()
                            Log.d("ResponseEditActivity", "Successfully updated the questionnaire response.")
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "No error body"
                            Log.e("ResponseEditActivity", "Failed to update. Response code: ${response.code()}, Body: $errorBody")
                            Toast.makeText(this@MicroNutrientsEditActivity, "Update failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(this@MicroNutrientsEditActivity, "Error occurred while updating: ${t.message}", Toast.LENGTH_SHORT).show()
                        Log.e("ResponseEditActivity", "Error occurred while updating questionnaire response", t)
                    }
                }
            })
        } else {
            Log.e("updateQuestionnaireResponse", "QuestionnaireFragment not found or is null")
        }
    }
}