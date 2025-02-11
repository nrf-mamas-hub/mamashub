package com.kabarak.kabarakmhis.pnc

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.kabarak.kabarakmhis.R
import com.kabarak.kabarakmhis.network_request.requests.RetrofitCallsFhir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.hl7.fhir.r4.model.QuestionnaireResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChildEdit : AppCompatActivity() {

    private lateinit var retrofitCallsFhir: RetrofitCallsFhir
    private var questionnaireJsonString: String? = null
    private lateinit var responseId: String // Define responseId globally

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_edit)

        // Initialize RetrofitCallsFhir
        retrofitCallsFhir = RetrofitCallsFhir()

        // Load the questionnaire JSON
        questionnaireJsonString = getStringFromAssets("other-problems.json")

        // Assign questionnaireResponseId to responseId
        responseId = intent.getStringExtra("responseId") ?: ""

        if (savedInstanceState == null && questionnaireJsonString != null) {
            // Render the questionnaire
            renderInitialQuestionnaire()

            // Fetch and populate the questionnaire response with the assigned responseId
            CoroutineScope(Dispatchers.IO).launch {
                fetchAndPopulateQuestionnaireResponse(responseId)
            }
        }

        // Listen for the submit request from the QuestionnaireFragment
        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.SUBMIT_REQUEST_KEY,
            this,
        ) { _, _ ->
            Log.d("ChildEdit", "Submit request received")
            submitUpdatedResponse()  // Call submission function when the form is submitted
        }
    }

    private fun getStringFromAssets(fileName: String): String? {
        return try {
            val inputStream = assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun renderInitialQuestionnaire() {
        // Render the empty questionnaire from the JSON file
        val questionnaireFragment = QuestionnaireFragment.builder()
            .setQuestionnaire(questionnaireJsonString!!)
            .build()

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container_view, questionnaireFragment, "initial-questionnaire-fragment")
        }
    }

    private suspend fun fetchAndPopulateQuestionnaireResponse(responseId: String) {
        // Fetch QuestionnaireResponse from the FHIR server
        retrofitCallsFhir.fetchQuestionnaireResponse(responseId, object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val questionnaireResponseString = response.body()?.string()

                    if (questionnaireResponseString != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                // Parse the response into a FHIR QuestionnaireResponse object
                                val fhirContext = FhirContext.forR4()
                                val jsonParser = fhirContext.newJsonParser()
                                val questionnaireResponse = jsonParser.parseResource(QuestionnaireResponse::class.java, questionnaireResponseString)

                                // Populate the questionnaire with the retrieved response
                                populateQuestionnaireFragment(questionnaireResponse)

                            } catch (e: Exception) {
                                Log.e("ChildEdit", "Error populating questionnaire", e)
                                Toast.makeText(this@ChildEdit, "Error populating questionnaire", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@ChildEdit, "Failed to retrieve the response data.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(this@ChildEdit, "Failed to fetch the questionnaire response: ${response.message()}", Toast.LENGTH_SHORT).show()
                        Log.e("ChildEdit", "Failed to fetch response. Response code: ${response.code()}")
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@ChildEdit, "Error occurred while fetching: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Error", "Error occurred while fetching questionnaire response", t)
                }
            }
        })
    }

    private fun populateQuestionnaireFragment(questionnaireResponse: QuestionnaireResponse) {
        try {
            // Convert the QuestionnaireResponse back into a JSON string
            val fhirContext = FhirContext.forR4()
            val jsonParser = fhirContext.newJsonParser()
            val questionnaireResponseString = jsonParser.encodeResourceToString(questionnaireResponse)

            // Prepare the bundle with both the Questionnaire and QuestionnaireResponse
            val questionnaireFragment = QuestionnaireFragment.builder()
                .setQuestionnaire(questionnaireJsonString!!)  // Your Questionnaire JSON
                .setQuestionnaireResponse(questionnaireResponseString)  // The populated QuestionnaireResponse JSON
                .build()

            // Load the QuestionnaireFragment with the response
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container_view, questionnaireFragment, "populated-questionnaire-fragment")
            }

            Log.d("ChildEdit", "Questionnaire response populated successfully.")
        } catch (e: Exception) {
            Log.e("ChildEdit", "Error initializing the questionnaire fragment or ViewModel", e)
            Toast.makeText(this, "Error initializing questionnaire", Toast.LENGTH_SHORT).show()
        }
    }

    // Submit updated data
    private fun submitUpdatedResponse() {
        // Retrieve the updated QuestionnaireResponse from the fragment
        val fragment = supportFragmentManager.findFragmentByTag("populated-questionnaire-fragment") as? QuestionnaireFragment
        val updatedQuestionnaireResponse = fragment?.getQuestionnaireResponse()

        if (updatedQuestionnaireResponse != null) {
            // Convert the updated QuestionnaireResponse into a JSON string
            val fhirContext = FhirContext.forR4()
            val jsonParser = fhirContext.newJsonParser()
            val updatedResponseString = jsonParser.encodeResourceToString(updatedQuestionnaireResponse)

            // Submit the updated response back to the server
            retrofitCallsFhir.updateQuestionnaireResponse(responseId, updatedResponseString, object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@ChildEdit, "Data updated successfully.", Toast.LENGTH_SHORT).show()
                            // End the activity after successful submission
                            finish()
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@ChildEdit, "Failed to update data: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(this@ChildEdit, "Error occurred while updating: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            Toast.makeText(this, "Failed to retrieve updated response", Toast.LENGTH_SHORT).show()
        }
    }
}
