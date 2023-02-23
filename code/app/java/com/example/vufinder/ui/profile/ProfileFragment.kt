package com.example.vufinder.ui.profile

import android.R
import android.app.Activity.RESULT_OK
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.vufinder.Communicator
import com.example.vufinder.ShowOnMapActivity
import com.example.vufinder.UserMenuActivity
import com.example.vufinder.databinding.FragmentProfileBinding
import com.example.vufinder.ui.CreateActivity.my_Activity
import com.example.vufinder.ui.Message
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


data class MyUser(
    val name: String? = null,
    var gender: String? = null,
    val email: String? = null,
    val password: String? = null,
    val phone_number: String? = null,
    val date_of_birth: String? = null,
    var skills:MutableMap<String, Boolean>? =null,
    var hosted_activities:MutableMap<String, String>? =null,
    var joined_activities:MutableMap<String, String>? =null
)

class HomeFragment : Fragment() {
    private lateinit var buttonShowParticipant: Button
    private lateinit var comm: Communicator
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var user = MyUser()
    private lateinit var currentUser: FirebaseUser
    private val CheckBoxes = HashMap<String, CheckBox>()
    private var skillsDoc= HashMap<String, Boolean>()
    private var hostedActDoc= HashMap<String, String>()
    private var joinedActDoc= HashMap<String, String>()
    private lateinit var id:String
    val isIDInitialized get() = this::id.isInitialized
    var isUserValid = false
    private var _binding: FragmentProfileBinding? = null
    private lateinit var spinnerHostedAct: Spinner
    private lateinit var spinnerJoinedAct: Spinner
    private lateinit var spinnerParticipants:Spinner
    private lateinit var participantsMap:MutableMap<String,String>
    private val pickImage = 100
    private var imageUri: Uri? = null
    private var imageBitmap:Bitmap? = null
    lateinit var imageViewProfile: ImageView

    private fun dateTimeCon(date:String,time:String):String
    {
        var words = date.split("/")
        val month = words[1]
        val day = words[0]
        val year = words[2]
        words = time.split(":")
        val hour = words[0]
        val minute = words[1]
        return "$year$month$day$hour$minute"

    }
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.

        if(auth.currentUser != null || GoogleSignIn.getLastSignedInAccount(requireContext())!=null) {

            /* TO DO:
                search for user in firestore database by id field
                get name field / other fields.

             */
            if (auth.currentUser != null) {
                currentUser = auth.currentUser!!
                updateUI(currentUser)
                id = currentUser.uid
            }
            else {
                id = GoogleSignIn.getLastSignedInAccount(requireContext())?.id.toString()
            }

            //download profile pic
            val sr: StorageReference = FirebaseStorage.getInstance().reference
            sr.child("users/$id/profile").downloadUrl.addOnSuccessListener {
                Glide.with(this@HomeFragment)
                    .load(it)
                    .into(imageViewProfile)

                binding.buttonDeleteProfileImage.visibility = View.VISIBLE

                Log.e("Firebase", "download passed")
            }.addOnFailureListener {
                Log.e("Firebase", "Failed in downloading")
            }


            db.collection("users").document(id).get().addOnSuccessListener { documentSnapshot ->
                user = documentSnapshot.toObject<MyUser>() as MyUser
                isUserValid = true
                if (user != null) {
                    skillsDoc = user.skills as HashMap<String, Boolean>
                    hostedActDoc = user.hosted_activities as HashMap<String, String>
                    joinedActDoc = user.joined_activities as HashMap<String, String>
                    val name = user.name.toString()
                    val gender = user.gender.toString()
                    val textViewHelloUSER: TextView = binding.textViewHelloUser
                    textViewHelloUSER.text = "Hello ${name}"
                    db.collection("messages_$id").whereEqualTo("read",false).get().addOnSuccessListener {
                        val messagesNum = it.size()
                        binding.textViewMessagesnumber.text = messagesNum.toString()
                        binding.textViewMessagesnumber.setTextColor(Color.rgb(0,255,0))

                    }
                  /*  val result = skillsDoc.toList().sortedBy { (_,value) -> value }.toMap()
                    skillsDoc = result as HashMap<String, Boolean>*/
                    if(skillsDoc!= null) {
                        skillsDoc.forEach { (skill, value) ->
                            if (value != null) {
                                if (value) {
                                    //Log.w(ContentValues.TAG, "Error getting documents: ")
                                    CheckBoxes.getValue(skill).setChecked(true)
                                }
                            }
                        }
                    }

                    when (gender) {
                        "Male" ->
                            {
                                binding.checkBoxMale.setChecked(true)
                                binding.checkBoxFemale.setChecked(false)
                                binding.checkBoxNotSay.setChecked(false)
                            }
                        "Female" ->
                            {
                                binding.checkBoxMale.setChecked(false)
                                binding.checkBoxFemale.setChecked(true)
                                binding.checkBoxNotSay.setChecked(false)
                            }
                        "rather not say" ->
                            {
                                binding.checkBoxMale.setChecked(false)
                                binding.checkBoxFemale.setChecked(false)
                                binding.checkBoxNotSay.setChecked(true)
                            }
                    }
                    //joined and hosted activities

                    //get current date and format it right!
                    val currentDate = Calendar.getInstance().time
                    val formatterDate = SimpleDateFormat("dd/MM/yyyy")
                    val formattedDate = formatterDate.format(currentDate)
                    //time format --- not used, no idea how to check both (separate fields in fireStore database for date and time
                    val formatterTime = SimpleDateFormat("HH:mm")
                    val formattedTime = formatterTime.format(currentDate)



                    //filter joined activities
                    db.collection("activities").get().addOnSuccessListener { result ->
                        val allActivities = HashMap<String, my_Activity>()
                        val allFutureActivities = HashMap<String, my_Activity>()
                        for (document in result) {
                            val currActivity = document.toObject<my_Activity>() as my_Activity
                            val activityDate = currActivity.startingDate.toString()
                            val activityTime = currActivity.startingTime.toString()
                            if (dateTimeCon(activityDate,activityTime) >= dateTimeCon(formattedDate, formattedTime))
                                allFutureActivities[document.id] = currActivity
                            allActivities[document.id] = currActivity
                        }
                        //hosted
                        val newHostedAct = HashMap<String,String>()
                        for(entry in hostedActDoc)
                        {
                            if(entry.key in allFutureActivities.keys)
                                newHostedAct[entry.key] = entry.value
                            else if(true || entry.key in allActivities) {
                               /* //send email to user
                                val userEmail = user.email.toString()

                                val auth = EmailService.UserPassAuthenticator(
                                    "findervolunteer@gmail.com",
                                    "Firas?123"
                                )
                                val to = listOf(InternetAddress(userEmail))
                                val from = InternetAddress("finderVolunteer@gmail.com")
                                val email = EmailService.Email(
                                    auth,
                                    to,
                                    from,
                                    "Test Subject",
                                    "Hello Body World"
                                )
                                val emailService = EmailService()

                                GlobalScope.launch { // or however you do background threads
                                    emailService.send(email)
                                }*/
                            }
                        }
                        user.hosted_activities = newHostedAct



                        //joined
                        val newJoinedAct = HashMap<String,String>()
                        for(entry in joinedActDoc)
                        {
                            if(entry.key in allFutureActivities.keys)
                                newJoinedAct[entry.key] = entry.value
                        }
                        user.joined_activities = newJoinedAct
                        db.collection("users").document(id).set(user).addOnSuccessListener {
                            //hosted
                            val hosted_activities_array = ArrayList<String>(newHostedAct.values)
                            val adapterHostedAct = ArrayAdapter(this.requireContext(),
                                R.layout.simple_spinner_item,hosted_activities_array )

                            if (newHostedAct.size > 0) {
                                adapterHostedAct.setDropDownViewResource(R.layout.simple_spinner_item)
                            }
                            else{
                                spinnerHostedAct.isEnabled = false
                                binding.buttonGoToHostedAct.isEnabled = false
                            }
                            spinnerHostedAct.setAdapter(adapterHostedAct);

                            //joined
                            val joined_activities_array = ArrayList<String>(newJoinedAct.values)
                            val adapterJoinedAct = ArrayAdapter(this.requireContext(),
                                R.layout.simple_spinner_item,joined_activities_array )

                            if (newJoinedAct.size > 0) {
                                adapterJoinedAct.setDropDownViewResource(R.layout.simple_spinner_item)
                            }
                            else{
                                spinnerJoinedAct.isEnabled = false
                                binding.buttonGoTojoinedAct.isEnabled = false
                            }
                            spinnerJoinedAct.setAdapter(adapterJoinedAct);

                        }
                    }



                }
            }.addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

            reload();
        }
    }
    private fun updateUI(user: FirebaseUser?) {

    }
    private fun sendUnJoinedMessage(joinedUserID:String,activity:my_Activity,imageUri: Bitmap?)
    {
        sendJoinedMessage(joinedUserID,activity,imageUri,true)
    }
    private fun sendJoinedMessage(joinedUserID:String, activity:my_Activity, imageUri: Bitmap?, unJoined:Boolean = false)
    {
        val hostId = activity.creator_id.toString()
        var message = Message("user_joined","someone joined your activity",activity,null,null,null,joinedUserID)
        if(unJoined)
        {
            message = Message("user_unJoined","someone unJoined from your activity",activity,null,null,null,joinedUserID)
        }
        db.collection("messages_$hostId").add(message).addOnSuccessListener { document ->
            val messageId = document.id
            val sr = Firebase.storage.reference
            if(imageUri != null) {
                val baos = ByteArrayOutputStream();
                imageUri.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                val data = baos.toByteArray()
                val uploadTask = sr.child("messages/$messageId/image1").putBytes(data)
                uploadTask.addOnSuccessListener {
                    Log.e("Firebase", "upload passed")
                }.addOnFailureListener {
                    Log.e("Firebase", "upload in downloading")
                }
            }
            db.collection("messages_$hostId").whereIn("type", listOf("user_joined","user_unJoined")).whereEqualTo("joinedUserID",joinedUserID).get().addOnSuccessListener {
                for (m in it)
                {
                    db.collection("messages_$hostId").document(m.id).delete()
                    sr.child("messages/${m.id}/image1").delete()
                }
            }
        }
    }
    private fun reload() {

    }
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root
        comm = requireActivity() as Communicator
        auth = Firebase.auth
        db = Firebase.firestore

        //skills and gender hide
        binding.groupSkills.visibility=View.GONE
        binding.groupGender.visibility=View.GONE
        binding.buttonHideSkills.visibility = View.INVISIBLE
        binding.buttonHideGender.visibility = View.INVISIBLE
        binding.buttonShowSkills.setOnClickListener {
            binding.groupSkills.visibility=View.VISIBLE
            binding.buttonHideSkills.visibility = View.VISIBLE
            binding.buttonShowSkills.visibility = View.INVISIBLE
        }
        binding.buttonHideSkills.setOnClickListener {
            binding.groupSkills.visibility=View.GONE
            binding.buttonHideSkills.visibility = View.INVISIBLE
            binding.buttonShowSkills.visibility = View.VISIBLE
        }
        binding.buttonShowGender.setOnClickListener {
            binding.groupGender.visibility=View.VISIBLE
            binding.buttonHideGender.visibility = View.VISIBLE
            binding.buttonShowGender.visibility = View.INVISIBLE

        }
        binding.buttonHideGender.setOnClickListener {
            binding.groupGender.visibility=View.GONE
            binding.buttonHideGender.visibility = View.INVISIBLE
            binding.buttonShowGender.visibility = View.VISIBLE

        }
        //upload profile pic
        imageViewProfile = binding.imageViewProfilePic

        binding.buttonUploadProfilePic.setOnClickListener{
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }
        binding.buttonDeleteProfileImage.visibility = View.GONE
        binding.buttonDeleteProfileImage.setOnClickListener {
            val sr = Firebase.storage.reference
            sr.child("users/$id/profile").delete()
            imageViewProfile.setImageURI(null)
            imageViewProfile.setImageResource(com.example.vufinder.R.drawable.profile)
            binding.buttonDeleteProfileImage.visibility = View.GONE
        }

        //skills checkboxes
        CheckBoxes["hand_craft"] = binding.checkBoxHandCraft
        CheckBoxes["sport"] = binding.checkBoxSport
        CheckBoxes["teach"] = binding.checkBoxTeach
        CheckBoxes["hard_worker"] = binding.checkBoxHardWorker
        CheckBoxes["artist"] = binding.checkBoxArtist
        CheckBoxes["fix_things"] = binding.checkBoxFixer
        CheckBoxes["bilingual"] = binding.checkBoxBilingual
        CheckBoxes["medical_training"] = binding.checkBoxMedical
        CheckBoxes["cocking"] = binding.checkBoxCocking
        CheckBoxes["other"] = binding.checkBoxOther

        CheckBoxes.forEach { (skill, value) ->
            value.setOnCheckedChangeListener { buttonView, isChecked ->
                    skillsDoc[skill] = isChecked
                    user.skills = skillsDoc
                if(isUserValid && isIDInitialized) {
                    db.collection("users").document(id).update(mapOf(
                        "skills.$skill" to isChecked
                    ))
                    //set(user)
                }

            }
        }


        binding.checkBoxMale.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked)
            {
            user.gender = "Male"
                if(isUserValid && isIDInitialized && id.isNotEmpty()) {
                    db.collection("users").document(id).update("gender","Male")
                    //set(user)
                }
                binding.checkBoxFemale.setChecked(false)
                binding.checkBoxNotSay.setChecked(false)
        }
            else if(!binding.checkBoxFemale.isChecked && !binding.checkBoxNotSay.isChecked){
                binding.checkBoxMale.setChecked(true)
            }
        }
        binding.checkBoxFemale.setOnCheckedChangeListener{buttonView, isChecked ->
        if(isChecked)
        {
        user.gender = "Female"
            if(isUserValid && isIDInitialized) {
                db.collection("users").document(id).update("gender","Female")
                //set(user)
            }
            binding.checkBoxMale.setChecked(false)
            binding.checkBoxNotSay.setChecked(false)
    }else if(!binding.checkBoxMale.isChecked && !binding.checkBoxNotSay.isChecked){
            binding.checkBoxFemale.setChecked(true)
        }
        }
        binding.checkBoxNotSay.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked)
            {
    user.gender = "rather not say"
                if(isUserValid && isIDInitialized) {
                    db.collection("users").document(id).update("gender","rather not say")
                    //set(user)
                }
                binding.checkBoxMale.setChecked(false)
                binding.checkBoxFemale.setChecked(false)
}
            else if(!binding.checkBoxFemale.isChecked && !binding.checkBoxMale.isChecked){
                binding.checkBoxNotSay.setChecked(true)
            }
        }

        //*******************/hosted activities spinner/***********************************/
        spinnerHostedAct = binding.spinnerHostedAct

        binding.buttonGoToHostedAct.setOnClickListener {
            val index = spinnerHostedAct.selectedItemPosition as Int//if selected Id = index
            val activity_id_list = ArrayList<String>(user.hosted_activities!!.keys)
            val activity_id = activity_id_list[index]
            comm.passDataCom(activity_id)

        }

        //participants
        //buttonShowParticipant
        buttonShowParticipant = binding.buttonShowParticipant
        buttonShowParticipant.isEnabled = false
        buttonShowParticipant.setOnClickListener{
            val index = spinnerParticipants.selectedItemPosition
            val participantsIdsList = ArrayList<String>(participantsMap.keys)
            val participantId = participantsIdsList[index]
            addUserPopView(participantId)
        }
        //spinner participants
        spinnerParticipants = binding.spinnerParticipants
        spinnerParticipants.isEnabled = false
        spinnerParticipants.visibility = View.GONE
        val tvParticipants = binding.textViewParticipants
        val tvParticipantsNumber = binding.textViewParticipantsNumber
        tvParticipantsNumber.visibility = View.GONE
        tvParticipants.visibility = View.GONE
        buttonShowParticipant.visibility = View.GONE
        spinnerHostedAct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                spinnerParticipants.isEnabled = false
                buttonShowParticipant.isEnabled = false
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val index = spinnerHostedAct.selectedItemPosition as Int//if selected Id = index
                val activity_id_list = ArrayList<String>(user.hosted_activities!!.keys)
                val activity_id = activity_id_list[index]
                db.collection("activities").document(activity_id).get().addOnSuccessListener {
                    val activity = it.toObject<my_Activity>() as my_Activity
                    participantsMap = activity.participants?.toMutableMap() as MutableMap<String,String>
                    val participants_array = ArrayList<String>(participantsMap.values)
                    val adapterParticipants = ArrayAdapter(requireContext(),
                        R.layout.simple_spinner_item,participants_array )

                    if (participantsMap.isNotEmpty()) {
                        tvParticipants.visibility = View.VISIBLE
                        tvParticipantsNumber.visibility = View.VISIBLE
                        tvParticipantsNumber.setText(participantsMap.size.toString())
                        spinnerParticipants.visibility = View.VISIBLE
                        buttonShowParticipant.visibility = View.VISIBLE
                        adapterParticipants.setDropDownViewResource(R.layout.simple_spinner_item)
                        buttonShowParticipant.isEnabled = true
                        spinnerParticipants.isEnabled = true
                        spinnerParticipants.adapter = adapterParticipants
                    }
                    else{
                        spinnerParticipants.visibility = View.GONE
                        tvParticipants.visibility = View.GONE
                        tvParticipantsNumber.visibility = View.GONE
                        buttonShowParticipant.visibility = View.GONE
                        spinnerParticipants.isEnabled = false
                        buttonShowParticipant.isEnabled = false
                    }
                }

            }
        }

        //*******************/joined activities spinner/***********************************/
        spinnerJoinedAct = binding.spinnerJoinedAct

        binding.buttonGoTojoinedAct.setOnClickListener {
            val index = spinnerJoinedAct.selectedItemPosition as Int//if selected Id = index
            val activity_id_list = ArrayList<String>(user.joined_activities!!.keys)
            val activity_id = activity_id_list[index]
            //pop up view and show required activity
            db.collection("activities").document(activity_id).get().addOnSuccessListener{ documentSnapShot ->
                val activity = documentSnapShot.toObject<my_Activity>() as my_Activity
                addPopView(activity,activity_id)
            }

        }

        return root
    }


    private fun addPopView(activity:my_Activity,activityId:String) {
        // this method inflates the single item layout
        // inside the parent linear layout
        val newView = LayoutInflater.from(requireContext()).inflate(com.example.vufinder.R.layout.activity_one_row, null)



        //bind the text views with new view
        val textViewActName : TextView = newView.findViewById(com.example.vufinder.R.id.textViewRowActivityName)
        val textViewActDescription : TextView = newView.findViewById(com.example.vufinder.R.id.textViewRowActivityDescription)
        val textViewActExEffort : TextView = newView.findViewById(com.example.vufinder.R.id.textViewRowActivityEffort)
        val textViewActExTime : TextView = newView.findViewById(com.example.vufinder.R.id.textViewRowActivityExTime)
        val textViewActAge : TextView = newView.findViewById(com.example.vufinder.R.id.textViewRowActivityAgeRestriction)
        val textViewActCategory : TextView = newView.findViewById(com.example.vufinder.R.id.textViewRowActivityCategory)
        val textViewActStartTime : TextView = newView.findViewById(com.example.vufinder.R.id.textViewRowActivityStartingTime)
        val buttonJoin : Button = newView.findViewById(com.example.vufinder.R.id.buttonJoinActivity)
        val buttonUnJoin : Button = newView.findViewById(com.example.vufinder.R.id.buttonUnJoinActivity)
        val textViewLocation : TextView = newView.findViewById(com.example.vufinder.R.id.textViewLocation)
        val textViewCountry :TextView = newView.findViewById(com.example.vufinder.R.id.textViewCountry)
        val textViewCity :TextView = newView.findViewById(com.example.vufinder.R.id.textViewCity)
        val textViewDistance :TextView = newView.findViewById(com.example.vufinder.R.id.textViewDistance)
        val textViewCreator : TextView = newView.findViewById(com.example.vufinder.R.id.textViewCreator)
        val imageViewCreator : ImageView = newView.findViewById(com.example.vufinder.R.id.imageViewCreator)
        val imageViewImage : ImageView = newView.findViewById(com.example.vufinder.R.id.imageViewImageOneRow)

        val sr: StorageReference = FirebaseStorage.getInstance().reference
        val ONE_MEGABYTE: Long = 1024 * 1024
        sr.child("activities/$activityId/activity").getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes->
            imageBitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.size, BitmapFactory.Options())
        }.addOnFailureListener {
            Log.e("Firebase", "Failed in downloading")
        }

        sr.child("activities/$activityId/activity").downloadUrl.addOnSuccessListener {
            Glide.with(this@HomeFragment)
                .load(it)
                .into(imageViewImage)


            Log.e("Firebase", "download passed")
        }.addOnFailureListener {
            Log.e("Firebase", "Failed in downloading")
        }

        textViewDistance.visibility = View.GONE
        //location
        val location = activity.location
        if(location?.isNotEmpty() == true) {
            val cityName = location?.get("cityName")
            val countryName = location?.get("countryName")
            val latitude = location?.get("latitude")
            val longitude = location?.get("longitude")
            textViewCountry.setText(countryName)
            textViewCity.setText(",$cityName")

            textViewLocation.setOnClickListener {
                val intent = Intent(this.context, ShowOnMapActivity::class.java)
                intent.putExtra("activityName", activity.name.toString())
                intent.putExtra("latitude", latitude)
                intent.putExtra("longitude", longitude)
                startActivityForResult(intent, 1)
            }
        }
        else
        {
            textViewCity.setText("")
            textViewCountry.setText("From home")
            val imageView : ImageView = newView.findViewById(com.example.vufinder.R.id.imageViewLocation)
            imageView.setBackgroundResource(com.example.vufinder.R.drawable.home)
        }

        //creator

        if(isUserValid && activityId in user.hosted_activities!!.keys)
        {
            textViewCreator.visibility = View.GONE
            imageViewCreator.visibility = View.GONE
        }
        else
        {
            textViewCreator.setTextColor(Color.BLUE)
            textViewCreator.setOnClickListener{
                addUserPopView(activity.creator_id.toString())
            }
        }




        //fill the data from the input variable (my_activity)
        textViewActName.setText(activity.name)
        textViewActDescription.setText(activity.description)
        textViewActExEffort.setText(activity.effort)
        textViewActExTime.setText(activity.time)
        textViewActAge.setText(activity.minAge + "-" +activity.maxAge+" years old!")
        textViewActCategory.setText(activity.category)
        textViewActStartTime.setText(activity.startingDate + ", " + activity.startingTime)



        // create the popup window
        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it

        val popupWindow = PopupWindow(newView, width, height, focusable)
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.CYAN))
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        buttonJoin.setText("Close")
        //dismiss pop up view
        buttonJoin.setOnClickListener{
            popupWindow.dismiss();
        }

        //UnJoin activity button
        buttonUnJoin.setOnClickListener{
            if(isUserValid && isIDInitialized) {
                user.joined_activities!!.remove(activityId)
                db.collection("users").document(id).update(mapOf(
                    "joined_activities.$activityId" to FieldValue.delete())
                )
                //set(user)
                //activity.participants!!.remove(id)
                db.collection("activities").document(activityId).update(mapOf(
                    "participants.$id" to FieldValue.delete())
                )
                //set(activity)
                    .addOnSuccessListener {
                    sendUnJoinedMessage(id,activity,imageBitmap)
                }
            }

            //refresh profile to reload joined activities
            val intent = Intent(this.context, UserMenuActivity::class.java)
            startActivity(intent)
        }
    }

    private fun addUserPopView(userId:String) {
        val newView = LayoutInflater.from(requireContext())
            .inflate(com.example.vufinder.R.layout.profile, null)
        val textViewName: TextView =
            newView.findViewById(com.example.vufinder.R.id.textViewProfileNameValue)
        val textViewAge: TextView =
            newView.findViewById(com.example.vufinder.R.id.textViewProfileAgeValue)
        val textViewGender: TextView =
            newView.findViewById(com.example.vufinder.R.id.textViewProfileGender)
        val imageViewProfileParticipant:ImageView = newView.findViewById(com.example.vufinder.R.id.imageViewProfile)

        //download profile pic
        val sr: StorageReference = FirebaseStorage.getInstance().reference
        sr.child("users/$userId/profile").downloadUrl.addOnSuccessListener {
            Glide.with(this@HomeFragment)
                .load(it)
                .into(imageViewProfileParticipant)

            Log.e("Firebase", "download passed")
        }.addOnFailureListener {
            Log.e("Firebase", "Failed in downloading")
        }

        db.collection("users").document(userId).get().addOnSuccessListener {
            val user = it.toObject<MyUser>()
            if (user != null) {
                textViewName.setText(user.name)
                val userDOB = user.date_of_birth.toString()
                val userDOBSimple = DOBtoSimple(userDOB)
                textViewAge.setText(getAge(userDOBSimple).toString())
                textViewGender.setText(user.gender)
            }
        }

        // create the popup window
        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it

        val popupWindow = PopupWindow(newView, width, height, focusable)
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.CYAN))
        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);


    }

    private fun getMonthIntFromFormat(month: String): Int {
        if (month == "JAN") return 1
        if (month == "FEB") return 2
        if (month == "MAR") return 3
        if (month == "APR") return 4
        if (month == "MAY") return 5
        if (month == "JUN") return 6
        if (month == "JUL") return 7
        if (month == "AUG") return 8
        if (month == "SEP") return 9
        if (month == "OCT") return 10
        if (month == "NOV") return 11
        return if (month == "DEC") 12 else 1

        //default should never happen
    }

    private fun getAge(dobString: String): Int {
        var date: Date? = null
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        date = sdf.parse(dobString)
        if (date == null) return 0
        val dob = Calendar.getInstance()
        val today = Calendar.getInstance()
        dob.time = date
        val year = dob[Calendar.YEAR]
        val month = dob[Calendar.MONTH]
        val day = dob[Calendar.DAY_OF_MONTH]
        dob[year, month + 1] = day
        var age = today[Calendar.YEAR] - dob[Calendar.YEAR]
        if (today[Calendar.DAY_OF_YEAR] < dob[Calendar.DAY_OF_YEAR]) {
            age--
        }
        return age
    }

    private fun DOBtoSimple(DOB:String):String
    {
        val words = DOB.split(" ")
        val month = getMonthIntFromFormat(words[0]).toString()
        val day = words[1]
        val year = words[2]
        return "$day/$month/$year"

    }

    //image upload and download helpers

    private fun getFileExtension(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (cursor != null) {
                    if(cursor.moveToFirst()) {
                        var index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if(index < 0)
                            index = 0
                        return cursor.getString(index)
                    }
                }
            }
        }
        return uri.path?.lastIndexOf('.')?.let { uri.path?.substring(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            // getting URI of selected Image
            imageUri = data?.data
            imageBitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver,imageUri)
            // extract the file name with extension
            //val ext = getFileExtension(requireContext(), imageUri!!)
            // Upload Task with upload to directory 'file'
            // and name of the file remains same
            val sr = Firebase.storage.reference
            val uploadTask = sr.child("users/$id/profile").putFile(imageUri!!)
            // On success, download the file URL and display it
            uploadTask.addOnSuccessListener {
                // using glide library to display the image
                sr.child("users/$id/profile").downloadUrl.addOnSuccessListener {
                    Glide.with(this@HomeFragment)
                        .load(it)
                        .into(imageViewProfile)

                    binding.buttonDeleteProfileImage.visibility = View.VISIBLE

                    Log.e("Firebase", "download passed")
                }.addOnFailureListener {
                    Log.e("Firebase", "Failed in downloading")
                }
            }.addOnFailureListener {
                Log.e("Firebase", "Image Upload fail")
            }
        }
    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        id = ""
        user = MyUser()
        isUserValid = false
    }
}