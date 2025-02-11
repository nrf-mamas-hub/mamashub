package com.kabarak.kabarakmhis.new_designs.new_patient

import android.app.Application
import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.kabarak.kabarakmhis.R
import com.kabarak.kabarakmhis.fhir.FhirApplication
import com.kabarak.kabarakmhis.fhir.viewmodels.AddPatientViewModel
import com.kabarak.kabarakmhis.helperclass.DbObservationValues
import com.kabarak.kabarakmhis.helperclass.FormatterClass
import com.kabarak.kabarakmhis.new_designs.NewMainActivity
import com.kabarak.kabarakmhis.new_designs.data_class.*
import com.kabarak.kabarakmhis.new_designs.roomdb.KabarakViewModel
import com.kabarak.kabarakmhis.new_designs.screens.ConfirmParentAdapter
import com.kabarak.kabarakmhis.new_designs.screens.FragmentConfirmDetails
import kotlinx.android.synthetic.main.activity_antenatal_profile_view.*
import kotlinx.coroutines.*
import org.hl7.fhir.r4.model.QuestionnaireResponse


class FragmentConfirmPatient : Fragment(){

    private val formatter = FormatterClass()

    private lateinit var kabarakViewModel: KabarakViewModel

    private lateinit var rootView: View

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: RecyclerView.LayoutManager

    private lateinit var btnSave: Button

    private lateinit var fhirEngine: FhirEngine
    private val viewModel: AddPatientViewModel by viewModels()

    private lateinit var btnEditDetails: Button

    private var encounterDetailsList = ArrayList<DbConfirmDetails>()


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {



        rootView = inflater.inflate(R.layout.frament_confirm, container, false)

        btnEditDetails = rootView.findViewById(R.id.btnEditDetails)

        fhirEngine = FhirApplication.fhirEngine(requireContext())

        btnEditDetails.setOnClickListener {
            //Go back to the previous activity
            requireActivity().onBackPressed()
        }

        updateArguments()

        if (savedInstanceState == null){
            addQuestionnaireFragment()
        }

        kabarakViewModel = KabarakViewModel(requireContext().applicationContext as Application)

        recyclerView = rootView.findViewById(R.id.confirmList);
        layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)

        btnSave = rootView.findViewById(R.id.btnSave)

        btnSave.setOnClickListener {

            val progressDialog= ProgressDialog(requireContext())
            progressDialog.setMessage("Saving...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            CoroutineScope(Dispatchers.Main).launch {

                val job = Job()
                CoroutineScope(Dispatchers.IO + job).launch {

                    var clientName = ""
                    var dob = ""
                    var maritalStatus = ""
                    var nationalId = ""
                    var telephoneName = ""
                    var spinnerCountyValue = ""
                    var spinnerSubCountyValue = ""
                    var spinnerWardValue = ""

                    var townName = ""
                    var addressName = ""
                    var estateName = ""

                    var kinPhone = ""
                    var spinnerRshpValue = ""
                    var kinName = ""
                    var ancCode = ""

                    val dataQuantityList = ArrayList<QuantityObservation>()
                    val dataCodeList = ArrayList<CodingObservation>()

                    val observationList = kabarakViewModel.getAllObservations(requireContext())
                    observationList.forEach {


                        val title = it.title
                        val codeLabel = it.codeLabel
                        val value = it.value

                        when (codeLabel) {

                            //Patient resource
                            DbObservationValues.CLIENT_NAME.name -> { clientName = value }
                            DbObservationValues.DATE_OF_BIRTH.name -> { dob = value }
                            DbObservationValues.MARITAL_STATUS.name -> { maritalStatus = value }
                            DbObservationValues.NATIONAL_ID.name -> { nationalId = value }
                            DbObservationValues.COUNTY_NAME.name -> { spinnerCountyValue = value }
                            DbObservationValues.SUB_COUNTY_NAME.name -> { spinnerSubCountyValue = value }
                            DbObservationValues.WARD_NAME.name -> { spinnerWardValue = value }
                            DbObservationValues.TOWN_NAME.name -> { townName = value }
                            DbObservationValues.ADDRESS_NAME.name -> { addressName = value }
                            DbObservationValues.ESTATE_NAME.name -> { estateName = value }
                            DbObservationValues.RELATIONSHIP.name -> { spinnerRshpValue = value }
                            DbObservationValues.COMPANION_NAME.name -> { kinName = value }
                            DbObservationValues.COMPANION_NUMBER.name -> { kinPhone = value }
                            DbObservationValues.PHONE_NUMBER.name -> { telephoneName = value }
                            //Patient Observation resource
                            else ->{


                                val codeValue = formatter.getCodes(codeLabel)
                                val checkObservation = formatter.checkObservations(title)
                                if (codeValue != ""){

                                    if (checkObservation == ""){
                                        //Save as a value string

                                        val codingObservation = CodingObservation(
                                            codeValue,
                                            title,
                                            value)
                                        dataCodeList.add(codingObservation)

                                    }else{
                                        //Save as a value quantity
                                        val quantityObservation = QuantityObservation(
                                            codeValue,
                                            title,
                                            value,
                                            checkObservation
                                        )
                                        dataQuantityList.add(quantityObservation)

                                    }

                                }



                            }

                        }

                        if (title == "ANC Code"){
                            if (title != ""){
                                ancCode = value
                            }else{
                                if (title == "PNC Code"){
                                    ancCode = value
                                }
                            }
                        }

                    }

                    val lineString = ArrayList<String>()
                    lineString.add(townName)
                    lineString.add(estateName)

                    val addressList = ArrayList<DbAddress>()
                    val address = DbAddress(
                        addressName,
                        lineString,
                        spinnerWardValue,
                        spinnerSubCountyValue,
                        spinnerCountyValue,
                        "KENYA-KABARAK-MHIS6")
                    addressList.add(address)

                    val telecomList = ArrayList<DbTelecom>()
                    val dbTelecom1 = DbTelecom("phone", telephoneName)
                    telecomList.add(dbTelecom1)

                    val kinContactList = ArrayList<DbKinDetails>()
                    val kinPhoneList = ArrayList<DbTelecom>()
                    val dbTelecom = DbTelecom("phone", kinPhone)
                    kinPhoneList.add(dbTelecom)
                    val kinContact = DbKinDetails(
                        spinnerRshpValue,
                        kinName, kinPhoneList)
                    kinContactList.add(kinContact)

                    val id = formatter.retrieveSharedPreference(requireContext(), "FHIRID").toString()

                    var encounterId = formatter.generateUuid()

//                    val savedEncounter = formatter.retrieveSharedPreference(requireContext(), "savedEncounter")
//                    if (savedEncounter != null) {
//                        encounterId = savedEncounter
//                    }else{
//                        formatter.generateUuid()
//                    }


                    val dbPatientFhirInformation = DbPatientFhirInformation(
                        id, clientName, telecomList,"female", dob, addressList,
                        kinContactList, maritalStatus,ancCode, nationalId,
                        dataCodeList, dataQuantityList)

                    val questionnaireFragment = childFragmentManager.findFragmentByTag(
                        QUESTIONNAIRE_FRAGMENT_TAG
                    ) as QuestionnaireFragment
                    savePatient(
                        dbPatientFhirInformation,
                        questionnaireFragment.getQuestionnaireResponse(),
                        encounterId
                    )

                }.join()

                delay(7000)

                progressDialog.dismiss()

                val intent = Intent(requireContext(), NewMainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()

            }

        }


        return rootView
    }

    override fun onStart() {
        super.onStart()

        getConfirmDetails()
    }

    private fun savePatient(
        dbPatientFhirInformation: DbPatientFhirInformation,
        questionnaireResponse: QuestionnaireResponse,
        encounterId: String
    ) {

        viewModel.savePatient(dbPatientFhirInformation, questionnaireResponse, encounterId)
    }

    private fun updateArguments(){
        requireArguments()
            .putString(FragmentConfirmDetails.QUESTIONNAIRE_FILE_PATH_KEY, "patient.json")
    }

    private fun addQuestionnaireFragment(){
        val fragment = QuestionnaireFragment()
        fragment.arguments =
            bundleOf(QuestionnaireFragment.SUBMIT_REQUEST_KEY to viewModel.questionnaire)
        childFragmentManager.commit {
            add(R.id.add_patient_container, fragment, QUESTIONNAIRE_FRAGMENT_TAG)
        }
    }


    private fun getConfirmDetails() {

        //Get the data from the previous screen
        //Use fhirId, loggedIn User, and title

        encounterDetailsList = kabarakViewModel.getConfirmDetails(requireContext())
        val confirmParentAdapter = ConfirmParentAdapter(encounterDetailsList,requireContext())
        recyclerView.adapter = confirmParentAdapter

        getUserDetails()


    }

    private fun getUserDetails() {

        val identifier = formatter.retrieveSharedPreference(requireContext(), "identifier")
        val patientName = formatter.retrieveSharedPreference(requireContext(), "patientName")

        if (identifier != null && patientName != null) {
            tvPatient.text = patientName
            tvAncId.text = identifier
        }

    }


    companion object {
        const val QUESTIONNAIRE_FILE_PATH_KEY = "questionnaire-file-path-key"
        const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    }

}