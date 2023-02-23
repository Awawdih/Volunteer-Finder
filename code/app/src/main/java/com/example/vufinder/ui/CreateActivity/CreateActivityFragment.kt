package com.example.vufinder.ui.CreateActivity

import android.R
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.vufinder.MapsActivity
import com.example.vufinder.ShowOnMapActivity
import com.example.vufinder.UserMenuActivity
import com.example.vufinder.databinding.FragmentCreateActivityBinding
import com.example.vufinder.ui.Message
import com.example.vufinder.ui.profile.MyUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
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


data class my_Activity(
    val name: String? = null,
    val description: String? = null,
    val minAge: String? = null,
    val maxAge: String? = null,
    val effort: String? = null,
    val time: String? = null,
    val gender: String? = null,
    val category:String? = null,
    val creator_id:String? = null,
    val creator_name:String? = null,
    val startingDate:String? = null,
    val startingTime:String? = null,
    val location : MutableMap<String,String>?=null,//:String?=null
    val participants:HashMap<String,String>? = null
)

class CreateActivityFragment : Fragment() {
    var cal = Calendar.getInstance()
    private lateinit var button_date: Button
    private lateinit var button_time: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var user = MyUser()
    private lateinit var creator_name : String
    private lateinit var user_id:String
    private var _binding: FragmentCreateActivityBinding? = null
    private var inputText: String? = ""
    private lateinit var location : MutableMap<String,String>
    private lateinit var imageViewActivity: ImageView
    private val pickImage = 100
    private var imageUri: Uri? = null
    private var oldImageBitmap : Bitmap ? = null
    private var imageBitmap : Bitmap ? = null
    private var updateLocation = false
    private var cameraUpload = false
    private lateinit var bitmap : Bitmap
    private val REQUEST_IMAGE_CAPTURE = 13
    //private var latitude = ""
    //private var longitude = ""

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val slideshowViewModel =
            ViewModelProvider(this).get(SlideshowViewModel::class.java)

        _binding = FragmentCreateActivityBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //INIT DATABASE
        auth = Firebase.auth
        db = Firebase.firestore

        //hide delete button & cancel button
        val deleteButton = binding.imageButtonDelete
        deleteButton.setVisibility(View.INVISIBLE)

        val cancelButton = binding.buttonCancel
        cancelButton.setVisibility(View.INVISIBLE)

        //*******************/effort spinner/***********************************/
        val spinnerEffort: Spinner = binding.spinnerEffort
        val adapterEffort =
            this.context?.let { ArrayAdapter.createFromResource(it, com.example.vufinder.R.array.effort, R.layout.simple_spinner_item) }

        if (adapterEffort != null) {
            adapterEffort.setDropDownViewResource(R.layout.simple_spinner_item)
        }
        spinnerEffort.setAdapter(adapterEffort);

        //*******************/time spinner/***************************************
        val spinnerTime: Spinner = binding.spinnerTime
        val adapterTime =
            this.context?.let { ArrayAdapter.createFromResource(it, com.example.vufinder.R.array.time, R.layout.simple_spinner_item) }

        if (adapterTime != null) {
            adapterTime.setDropDownViewResource(R.layout.simple_spinner_item)
        }
        spinnerTime.setAdapter(adapterTime);

        //**********************/gender spinner/**********************************
        val spinnerGender: Spinner = binding.spinnerGender
        val adapterGender =
            this.context?.let { ArrayAdapter.createFromResource(it, com.example.vufinder.R.array.gender, R.layout.simple_spinner_item) }

        if (adapterGender != null) {
            adapterGender.setDropDownViewResource(R.layout.simple_spinner_item)
        }
        spinnerGender.setAdapter(adapterGender);

        //**********************/gender spinner/**********************************
        val spinnerCategory: Spinner = binding.spinnerCategory
        val adapterCategory =
            this.context?.let { ArrayAdapter.createFromResource(it, com.example.vufinder.R.array.categories, R.layout.simple_spinner_item) }

        if (adapterCategory != null) {
            adapterCategory.setDropDownViewResource(R.layout.simple_spinner_item)
        }
        spinnerCategory.setAdapter(adapterCategory);

        ///location /**************************/////
        val locationButton = binding.buttonLocation
        val textViewStoredLocation = binding.textViewStoredLocation
        textViewStoredLocation.visibility = View.INVISIBLE
        locationButton.setOnClickListener {
            val intent = Intent(this.context, MapsActivity::class.java)
            startActivityForResult(intent,1)
            //startActivity(intent)
        }
        val noLocation = binding.checkBoxNoLocation
        noLocation.setOnCheckedChangeListener{ _, isChecked ->
            locationButton.isEnabled = !isChecked
        }

        location = HashMap<String,String>()
        //activity name editText binding
        val editTextActivityName = binding.editTextActivityName as EditText

        //activity description editText binding
        val editTextActivityDescription = binding.editTextActivityDescription as EditText

        //activity min age editText binding
        val editTextMinAge = binding.editTextMinAge as EditText

        //activity max age editText binding
        val editTextMaxAge = binding.editTextMaxAge as EditText
/**********************************************************/
        //upload picture
        //upload from gallery
        imageViewActivity = binding.imageViewUploadedImage

        binding.buttonUploadImageActivity.setOnClickListener{
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }

        binding.imageButtonCameraUpload.setOnClickListener {
            dispatchTakePictureIntent()
        }

        binding.imageButtonDeleteImageActivity.setOnClickListener {
            imageUri = null
            imageBitmap = null
            imageViewActivity.setImageURI(imageUri)
            imageViewActivity.setImageResource(com.example.vufinder.R.drawable.activity_bg)
        }


        /**********************************************************/
        val buttonCreateActivity = binding.buttonCreateActivity
        buttonCreateActivity.setOnClickListener {
            //getting user input values
            val activityName = editTextActivityName.text.toString()
            val activityDescription = editTextActivityDescription.text.toString()
            val activityMinAge = editTextMinAge.text.toString()
            val activityMaxAge = editTextMaxAge.text.toString()
            val activityEffort = spinnerEffort.selectedItem.toString()
            val activityExpectedTime = spinnerTime.selectedItem.toString()
            val activityGender = spinnerGender.selectedItem.toString()
            val activityCategory = spinnerCategory.selectedItem.toString()
            val activityDate = button_date.text.toString()
            val activityStartingTime = button_time.text.toString()
            var activityLocation = HashMap<String,String>()
            val activityParticipants = HashMap<String,String>()

            if(!noLocation.isChecked)
            {
                activityLocation = location as HashMap<String, String>
            }
            //check user inputs
            if(check_user_inputs(activityName,activityDescription,activityMinAge,activityMaxAge,activityEffort,activityExpectedTime,activityGender,activityCategory,activityDate,activityStartingTime))
            {
                //add the activity to dataBase
                val myActivity = my_Activity(activityName,activityDescription,activityMinAge,activityMaxAge,activityEffort,activityExpectedTime,activityGender,activityCategory,user_id,creator_name,activityDate,activityStartingTime,activityLocation,
                    activityParticipants
                )
                db.collection("activities").add(myActivity).addOnSuccessListener { documentReference ->
                   val user_created_activities = user.hosted_activities as HashMap<String, String>
                    val activityID = documentReference.id
                    user_created_activities[documentReference.id] = activityName
                    user.hosted_activities = user_created_activities
                    db.collection("users").document(user_id).set(user).addOnSuccessListener {
                        val sr = Firebase.storage.reference
                        if(imageUri != null) {
                            val uploadTask =
                                sr.child("activities/$activityID/activity").putFile(imageUri!!)
                            // On success, download the file URL and display it
                            uploadTask.addOnCompleteListener {
                                if(it.isSuccessful){
                                    val uri = it.result
                                // using glide library to display the image
                                sr.child("activities/$activityID/activity").downloadUrl.addOnSuccessListener { it1->
                                    Glide.with(this@CreateActivityFragment)
                                        .load(uri)
                                        .into(imageViewActivity)

                                    Log.e("Firebase", "download passed")
                                }
                                }
                            }
                        }
                        val intent = Intent(this.context, UserMenuActivity::class.java)
                        startActivity(intent)
                    }
                }
                    .addOnFailureListener { e ->

                    }
            }


        }

        inputText = arguments?.getString("input_txt")
        val activity_id = inputText
        if(activity_id != null)
        {
            //show delete button
            deleteButton.setVisibility(View.VISIBLE)
            cancelButton.setVisibility(View.VISIBLE)
            //remove from DB, delete button listener
            cancelButton.setOnClickListener {
                val intent = Intent(this.context, UserMenuActivity::class.java)
                startActivity(intent)
            }

            //fill in the activity data from FireStore
            db.collection("activities").document(activity_id).get().addOnSuccessListener { documentSnapshot ->
                val activity = documentSnapshot.toObject<my_Activity>() as my_Activity
                editTextActivityName.setText(activity.name)
                editTextActivityDescription.setText(activity.description)
                editTextMinAge.setText(activity.minAge)
                editTextMaxAge.setText(activity.maxAge)
                val position_category = getIndex(spinnerCategory, activity.category.toString())
                spinnerCategory.setSelection(position_category)
                val position_effort = getIndex(spinnerEffort, activity.effort.toString())
                spinnerEffort.setSelection(position_effort)
                val position_time = getIndex(spinnerTime, activity.time.toString())
                spinnerTime.setSelection(position_time)
                val position_gender = getIndex(spinnerGender, activity.gender.toString())
                spinnerGender.setSelection(position_gender)
                button_date.setText(activity.startingDate)
                button_time.setText(activity.startingTime)
                val locationButton = binding.buttonLocation
                locationButton.setOnClickListener {
                    updateLocation = true
                    val intent = Intent(this.context, MapsActivity::class.java)
                    startActivityForResult(intent,1)
                    //startActivity(intent)
                }
                val noLocation = binding.checkBoxNoLocation
                noLocation.isChecked = activity.location.isNullOrEmpty()
                noLocation.setOnCheckedChangeListener{ _, isChecked ->
                    locationButton.isEnabled = !isChecked
                }
                val location = activity.location
                if(location?.isNotEmpty() == true) {
                    textViewStoredLocation.visibility = View.VISIBLE
                    textViewStoredLocation.setTextColor(Color.BLUE)
                    val cityName = location?.get("cityName")
                    val countryName = location?.get("countryName")
                    val latitude = location?.get("latitude")
                    val longitude = location?.get("longitude")
                    textViewStoredLocation.setText("$countryName,$cityName")

                    textViewStoredLocation.setOnClickListener {
                        val intent = Intent(this.context, ShowOnMapActivity::class.java)
                        intent.putExtra("activityName", activity.name.toString())
                        intent.putExtra("latitude", latitude)
                        intent.putExtra("longitude", longitude)
                        startActivityForResult(intent, 1)
                    }
                }
                else
                {
                    textViewStoredLocation.visibility = View.INVISIBLE
                }



                binding.buttonCreateActivity.setText("Update Activity")
                val activityParticipants = activity.participants
                buttonCreateActivity.setOnClickListener {
                    //getting user input values
                    val activityName = editTextActivityName.text.toString()
                    val activityDescription = editTextActivityDescription.text.toString()
                    val activityMinAge = editTextMinAge.text.toString()
                    val activityMaxAge = editTextMaxAge.text.toString()
                    val activityEffort = spinnerEffort.selectedItem.toString()
                    val activityTime = spinnerTime.selectedItem.toString()
                    val activityGender = spinnerGender.selectedItem.toString()
                    val activityCategory = spinnerCategory.selectedItem.toString()
                    val activityDate = button_date.text.toString()
                    val activityStartingTime = button_time.text.toString()

                    //check user inputs
                    if(check_user_inputs(activityName,activityDescription,activityMinAge,activityMaxAge,activityEffort,activityTime,activityGender,activityCategory,activityDate,activityStartingTime))
                    {
                        //add the activity to dataBase
                        var activityLocation = location
                        if(updateLocation)
                            activityLocation = this.location
                        if(noLocation.isChecked)
                            activityLocation = HashMap<String,String>()
                        val myActivity = my_Activity(activityName,activityDescription,activityMinAge,activityMaxAge,activityEffort,activityTime,activityGender,activityCategory,user_id,creator_name,activityDate,activityStartingTime,activityLocation,activityParticipants)
                        db.collection("activities").document(activity_id).set(myActivity).addOnSuccessListener {
                            val sr = Firebase.storage.reference
                            if (imageUri != null) {
                                val uploadTask =
                                    sr.child("activities/$activity_id/activity").putFile(imageUri!!)
                                // On success, download the file URL and display it
                                uploadTask.addOnSuccessListener {
                                    // using glide library to display the image
                                    sr.child("activities/$activity_id/activity").downloadUrl.addOnSuccessListener {
                                        Glide.with(this@CreateActivityFragment)
                                            .load(it)
                                            .into(imageViewActivity)


                                        Log.e("Firebase", "download passed")
                                    }.addOnFailureListener {
                                        Log.e("Firebase", "Failed in downloading")
                                    }
                                }.addOnFailureListener {
                                    Log.e("Firebase", "Image Upload fail")
                                }
                            }
                            for (participantId in activityParticipants!!.keys) {
                                sendUpdatedMessage(participantId,activity,myActivity,oldImageBitmap,imageBitmap)
                            }

                            val intent = Intent(this.context, UserMenuActivity::class.java)
                            startActivity(intent)
                        }
                    }

                }

                deleteButton.setOnClickListener {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setMessage("Are you sure you want to Delete?")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { dialog, id ->
                            // Delete selected activity from database
                            db.collection("activities").document(activity_id).delete().addOnSuccessListener {
                                //delete activity from user hosted activities map
                                user.hosted_activities!!.remove(activity_id)
                                db.collection("users").document(user_id).set(user).addOnSuccessListener {
                                    val sr = Firebase.storage.reference
                                    sr.child("activities/$activity_id/activity").delete()

                                    for(participantId in activityParticipants!!.keys)
                                    {
                                        sendDeletedMessage(participantId,activity,imageBitmap)
                                    }
                                    val intent = Intent(this.context, UserMenuActivity::class.java)
                                    startActivity(intent)
                                }
                            }
                        }
                        .setNegativeButton("No") { dialog, id ->
                            // Dismiss the dialog
                            dialog.dismiss()
                        }
                    val alert = builder.create()
                    alert.show()

                }
            }


            //fill image
            //download image
            //download activity pic
            val sr: StorageReference = FirebaseStorage.getInstance().reference
            val ONE_MEGABYTE: Long = 1024 * 1024
            sr.child("activities/$activity_id/activity").getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes->
                imageBitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.size, BitmapFactory.Options())
                oldImageBitmap = imageBitmap
            }.addOnFailureListener {
                Log.e("Firebase", "Failed in downloading")
            }
            sr.child("activities/$activity_id/activity").downloadUrl.addOnSuccessListener {
                Glide.with(this@CreateActivityFragment)
                    .load(it)
                    .into(imageViewActivity)

                Log.e("Firebase", "download passed")
            }.addOnFailureListener {
                Log.e("Firebase", "Failed in downloading")
            }
        }
        //date button
        button_date = binding.buttonDate
        button_date.setText("--/--/----")
        //time button
        button_time = binding.buttonTime
        button_time.setText("--:--")

        // create an OnDateSetListener
        val dateSetListener = object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int,
                                   dayOfMonth: Int) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }
        }

        // create an OnTimeSetListener
        val timeSetListener = object : TimePickerDialog.OnTimeSetListener {
            override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                updateTimeInView()
            }
        }

        // when you click on the button, show DatePickerDialog that is set with OnDateSetListener
        button_date!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                DatePickerDialog(requireContext(),
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }

        })

        // when you click on the button, show TimePickerDialog that is set with OnTimeSetListener
        button_time!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                TimePickerDialog(requireContext(),
                    timeSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),true).show()
            }

        })

        return root
    }
    private fun updateDateInView() {
        val myFormat = "dd/MM/yyyy" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        button_date.setText(sdf.format(cal.getTime()))
    }
    fun givenString_toDate(text:String): Date? {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm")
        val date = formatter.parse(text)
        return date
    }
    private fun updateTimeInView() {
        val myFormat = "HH:mm" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        button_time.setText(sdf.format(cal.getTime()))
    }
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.

        if(auth.currentUser != null) {
            user_id = auth.currentUser!!.uid
        }
        else
        {
            user_id = GoogleSignIn.getLastSignedInAccount(requireContext())?.id.toString()
        }
        db.collection("users").document(user_id).get().addOnSuccessListener { documentSnapshot ->
            user = documentSnapshot.toObject<MyUser>() as MyUser
            creator_name = user.name.toString()
        }

    }
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
        }
    }

        private fun getIndex(spinner: Spinner, myString: String): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString().equals(myString, ignoreCase = true)) {
                return i
            }
        }
        return 0
    }
    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            inContext.getContentResolver(),
            inImage,
            "Title",
            null
        )
        return Uri.parse(path)
    }
    private fun sendUpdatedMessage(joinedUserId:String,oldActivity:my_Activity,newActivity:my_Activity,imageUri1: Bitmap?,imageUri2: Bitmap?)
    {

        val message = Message("activity_update","an activity you joined was updated",oldActivity,newActivity,null,null,null)
        db.collection("messages_$joinedUserId").add(message).addOnSuccessListener { document ->
            val messageId = document.id
            val sr = Firebase.storage.reference
            if(imageUri1 != null)
            {
                val baos1 = ByteArrayOutputStream();
                imageUri1.compress(Bitmap.CompressFormat.JPEG, 100, baos1);
                val data1 = baos1.toByteArray()
                sr.child("messages/$messageId/image1").putBytes(data1)
            }
            if(imageUri2 != null) {
                val baos2 = ByteArrayOutputStream();
                imageUri2.compress(Bitmap.CompressFormat.JPEG, 100, baos2);
                val data2 = baos2.toByteArray()
                sr.child("messages/$messageId/image2").putBytes(data2)
            }
        }
    }

    private fun sendDeletedMessage(joinedUserId:String,activity:my_Activity,imageUri: Bitmap?)
    {

        val message = Message("activity_delete","an activity you joined was deleted!",activity,null,null,null,null)
        db.collection("messages_$joinedUserId").add(message).addOnSuccessListener { document ->
            val messageId = document.id
            val sr = Firebase.storage.reference
            if(imageUri != null)
            {
                val baos = ByteArrayOutputStream();
                imageUri.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                val data = baos.toByteArray()
                sr.child("messages/$messageId/image1").putBytes(data)
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                val longitude = data?.getStringExtra("longitude").toString()
                val latitude = data?.getStringExtra("latitude").toString()
                val countryName = data?.getStringExtra("countryName").toString()
                val cityName = data?.getStringExtra("cityName").toString()
                location["cityName"] = cityName
                location["countryName"] = countryName
                location["longitude"] = longitude
                location["latitude"] = latitude
                binding.checkBoxNoLocation.isChecked = false
                binding.textViewStoredLocation.visibility = View.VISIBLE
                binding.textViewStoredLocation.text = "$countryName,$cityName"
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                // Write your code if there's no result
            }
        else if(requestCode == pickImage)
        {
            // getting URI of selected Image
            imageUri = data?.data
            imageViewActivity.setImageURI(imageUri)
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
            //oldImageBitmap = imageBitmap
            imageBitmap = bitmap

            // extract the file name with extension
            //val ext = getFileExtension(requireContext(), imageUri!!)
            // Upload Task with upload to directory 'file'
            // and name of the file remains same

        }
            else if(requestCode == REQUEST_IMAGE_CAPTURE)
            {
                val bitmap = data?.extras?.get("data") as Bitmap
                //oldImageBitmap = imageBitmap
                imageBitmap = bitmap
                imageUri = getImageUri(requireContext(),bitmap)
                imageViewActivity.setImageURI(imageUri)
            }
        }
    } //onActivityResult



    private fun check_user_inputs(activityName:String,activityDescription:String,activityMinAge:String,activityMaxAge:String,activityEffort:String,activityTime:String,activityGender:String,activityCategory:String,activityDate:String,activityStartingTime:String): Boolean
    {
        var valid = true;
        var error_message = "";
        if(activityName == "")
        {
            error_message = "activity name cannot be empty!"
            valid = false;
        }
        else if(activityDescription.length < 20)
        {
            error_message = "activity description should be at least 20 letters!"
            valid = false;
        }
        else if(activityMaxAge == "" || activityMinAge == "")
        {
            error_message = "age cannot be empty!"
            valid = false;
        }
        else if(activityMinAge.toInt() < 0 || activityMaxAge.toInt() < 0 )
        {
            error_message = "age should be a positive number!";
            valid = false
        }
        else if(activityMinAge.toInt() > 100 || activityMaxAge.toInt() > 100 )
        {
            error_message = "maximum allowed age for volunteers is 100 years old.";
            valid = false
        }
        else if(activityMinAge.toInt() > activityMaxAge.toInt())
        {
            error_message = "minimum age cannot be larger than maximum age!!!!";
            valid = false
        }
        else if(activityEffort == "")
        {
            error_message = "please select expected effort for the activity"
            valid = false;
        }
        else if(activityTime == "")
        {
            error_message = "please select expected time for the activity"
            valid = false;
        }
        else if(activityGender == "")
        {
            error_message = "please select wanted gender for the activity"
            valid = false;
        }
        else if(activityCategory == "")
        {
            error_message = "please select category for the activity"
            valid = false;
        }
        else if(activityDate == "--/--/----")
        {
            error_message = "please select starting date for the activity"
            valid = false;
        }
        else if(activityDate == "--:--")
        {
            error_message = "please select starting time for the activity"
            valid = false;
        }
        else if(activityDate == "--/--/----")
        {
            error_message = "please select starting date for the activity"
            valid = false;
        }
        else
        {
            val inputDate = givenString_toDate(activityDate + " " + activityStartingTime)
            val currentDate = Calendar.getInstance().time
            if(inputDate!!.before(currentDate)) {
                error_message = "starting time of the activity should be in the future!"
                valid = false;
            }
        }
        if(!valid)
            //Toast.makeText(this.context, error_message, Toast.LENGTH_LONG).show()
            Snackbar.make(this.requireView(), error_message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        return valid
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}