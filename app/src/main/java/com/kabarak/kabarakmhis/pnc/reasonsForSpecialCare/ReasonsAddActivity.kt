package com.kabarak.kabarakmhis.pnc.reasonsForSpecialCare

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.kabarak.kabarakmhis.R
import com.kabarak.kabarakmhis.network_request.requests.RetrofitCallsFhir
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.hl7.fhir.r4.model.QuestionnaireResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReasonsAddActivity : AppCompatActivity() {
    private lateinit var retrofitCallsFhir: RetrofitCallsFhir
    private var questionnaireJsonString: String? = null
    private lateinit var identifier: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reasons_add)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        retrofitCallsFhir = RetrofitCallsFhir()
        identifier = intent.getStringExtra("identifier") ?: ""

        questionnaireJsonString = getStringFromAssets("reason_for_special_care.json")

        if (savedInstanceState == null && !questionnaireJsonString.isNullOrEmpty()) {
            setupQuestionnaireFragment()
            setupSubmitListener()
        } else {
            showError("Failed to load questionnaire data.")
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

    private fun setupQuestionnaireFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(
                R.id.fragment_container_view,
                QuestionnaireFragment.builder()
                    .setQuestionnaire(questionnaireJsonString!!)
                    .build()
            )
        }
    }

    private fun setupSubmitListener() {
        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.SUBMIT_REQUEST_KEY,
            this
        ) { _, _ ->
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? QuestionnaireFragment
            fragment?.let { submitQuestionnaire(it) } ?: showError("Error loading questionnaire.")
        }
    }

    private fun submitQuestionnaire(fragment: QuestionnaireFragment) {
        lifecycleScope.launch {
            try {
                val questionnaireResponse = fragment.getQuestionnaireResponse()

                val identifierItem = QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                    linkId = "identifier"
                    addAnswer().value = org.hl7.fhir.r4.model.StringType(identifier)
                }
                questionnaireResponse.item = questionnaireResponse.item.toMutableList().apply { add(identifierItem) }

                val fhirContext = FhirContext.forR4()
                val questionnaireResponseString = fhirContext.newJsonParser()
                    .encodeResourceToString(questionnaireResponse)

                retrofitCallsFhir.submitQuestionnaireResponse(questionnaireResponseString, createCallback())
            } catch (e: Exception) {
                showError("Error processing questionnaire response.")
            }
        }
    }

    private fun createCallback(): Callback<ResponseBody> {
        return object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    showToast("Successfully submitted!")
                    finish()
                } else {
                    showError("Submission failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                showError("Error occurred while submitting: ${t.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        showToast(message)
    }
}