package com.example.vufinder.ui.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.collection.ArrayMap
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.vufinder.R
import com.example.vufinder.ShowOnMapActivity
import com.example.vufinder.databinding.FragmentJoinActivityBinding
import com.example.vufinder.ui.CreateActivity.my_Activity
import com.example.vufinder.ui.Message
import com.example.vufinder.ui.profile.MyUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
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
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class JoinActivityFragment : Fragment(), OnMapReadyCallback {

    //mapView
    private  var mMap: GoogleMap? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val permissionId = 2
    private  var mapFragment : SupportMapFragment? = null
    private lateinit var mapView: View
    var isMapReady = false
    private var current_lat : Double? = null
    private var current_long : Double? = null

    private var _binding: FragmentJoinActivityBinding? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var user_id:String
    private lateinit var spinnerEffort:Spinner
    private lateinit var spinnerTime:Spinner
    private lateinit var etKeyWord:EditText
    private lateinit var buttonNextPage:ImageButton
    private lateinit var buttonPrevPage:ImageButton
    private lateinit var tvPages:TextView
    private lateinit var tvSortByLocation:TextView
    private var user = MyUser()
    private val numOfActInPage = 4
    private var currentPageNumber = 1
    private var totalPageNumber = 1
    private var areActivitiesValid = false
    private var isUsrValid = false
    private var filters = ArrayMap<String, Boolean>()
    private var activitiesArrayMap :ArrayMap<String, my_Activity> = ArrayMap<String, my_Activity> ()
    private lateinit var filteredActivities :MutableMap<String, my_Activity>//:ArrayMap<String, my_Activity>
    //private var all_activities : MutableMap<String, my_Activity> = HashMap<String,my_Activity>()

    //private
    //companion object {var isMapViewOn = false} //by Delegates.observable(false){property, oldValue, newValue ->

    //}

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private fun stringToDate(str: String):Date
    {
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        return sdf.parse(str)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentJoinActivityBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //pages initial
        buttonNextPage = binding.imageButtonNextPage
        buttonPrevPage = binding.imageButtonPrevPage
        tvPages = binding.textViewPages


        //INIT DATABASE
        auth = Firebase.auth
        db = Firebase.firestore
        // Check if user is signed in (non-null) and update UI accordingly.
        //currentUser = auth.currentUser!!
        if(auth.currentUser != null) {
            user_id = auth.currentUser!!.uid
        }
        else
        {
            user_id = GoogleSignIn.getLastSignedInAccount(requireContext())?.id.toString()
        }
        //get the current user from dataBase (before he joined activities)
        db.collection("users").document(user_id).get().addOnSuccessListener { documentSnapshot ->
            user = documentSnapshot.toObject<MyUser>() as MyUser
            isUsrValid = true
            //delete gender filter if userGender is not specified
            if(user.gender.toString() == "rather not say")
            {
                binding.checkBoxGenderFilter.visibility = View.GONE
            }
            for(i in 0 until binding.parentActivitiesLayout.childCount)
            {
                val view = binding.parentActivitiesLayout.getChildAt(i)
                //if the user is the host
                val activityId = view.tag.toString()
                val buttonJoin : Button = view.findViewById(R.id.buttonJoinActivity)
                val buttonUnJoin : Button = view.findViewById(R.id.buttonUnJoinActivity)
                if(isUsrValid && activityId in user.hosted_activities!!.keys)
                {
                    buttonJoin.isEnabled = false
                    buttonUnJoin.isEnabled = false
                }
                //if already joined disable join button
                else if(isUsrValid && activityId in user.joined_activities!!.keys)
                {
                    buttonJoin.isEnabled = false
                    buttonUnJoin.isEnabled = true
                }
                else // if not joined disable unJoin button
                {
                    buttonJoin.isEnabled = true
                    buttonUnJoin.isEnabled = false
                }

            }
        }

        //get current date and format it right!
        val currentDate = Calendar.getInstance().time
        val formatterDate = SimpleDateFormat("dd/MM/yyyy")
        val formattedDate = formatterDate.format(currentDate)
        //time format --- not used, no idea how to check both (separate fields in fireStore database for date and time
        val formatterTime = SimpleDateFormat("HH:mm")
        val formattedTime = formatterTime.format(currentDate)
        //get all activities from data-base --- filter old activities
        //.whereGreaterThanOrEqualTo("startingDate", formattedDate)
        db.collection("activities").get().addOnSuccessListener { result ->
            for (document in result){//document = activity
                //all_activities[document.id] = document.toObject<my_Activity>() as my_Activity
                val currActivity = document.toObject<my_Activity>() as my_Activity
                val activityDate = currActivity.startingDate.toString()
                val activityTime = currActivity.startingTime.toString()
                if(dateTimeCon(activityDate,activityTime)>=dateTimeCon(formattedDate,formattedTime))
                    activitiesArrayMap[document.id] = currActivity
        }
            areActivitiesValid = true
            filteredActivities = activitiesArrayMap
            if(filteredActivities.isNotEmpty())
                currentPageNumber = 1
            else
                currentPageNumber = 0
            totalPageNumber = Math.ceil(filteredActivities.size.toDouble()/numOfActInPage).toInt()
            //add numOfActInPage activities in each page
            addActivitiesToView((currentPageNumber-1)*numOfActInPage,numOfActInPage)
            disablePrevImageButton()
            if(currentPageNumber == totalPageNumber)
                disableNextImageButton()
            tvPages.setText("page ${currentPageNumber} out of $totalPageNumber")
        }

        //location (mapView)

        val buttonMapView = binding.imageButtonMapView
        val pageFooter = binding.pageNavigateFooter
        buttonMapView.setOnClickListener {
            val filtersLayoutParent = binding.constraintFiltersLayout
            val params = filtersLayoutParent.layoutParams as LinearLayout.LayoutParams
            params.weight = 4.0f
            filtersLayoutParent.layoutParams = params
            buttonMapView.tag = true
            addMapView()
            pageFooter.visibility = View.GONE
        }

        //nextPage button
        buttonNextPage.setOnClickListener {
            if(currentPageNumber < totalPageNumber)
                currentPageNumber += 1
            changePage()
        }

        //prevPage button
        buttonPrevPage.setOnClickListener {
            if(currentPageNumber > 1)
                currentPageNumber -= 1
            changePage()
        }

        //location and distance


        mFusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        getCurrLocation()

        //sort
        //by date
        binding.textViewSortByDate.setTextColor(Color.BLUE)
        binding.textViewSortByDate.setOnClickListener {
            binding.parentActivitiesLayout.removeAllViews()
            if(filteredActivities.isEmpty())
                currentPageNumber = 0
            else
                currentPageNumber = 1
            val simpleMap = filteredActivities.toMutableMap()
            val sortedMap = simpleMap.entries.sortedWith(compareBy { stringToDate(it.value.startingDate.toString()) }).map{it.key to it.value}.toMap() as MutableMap<String,my_Activity>
            //filteredActivities = convertMapToArrayMap(sortedMap)
            filteredActivities = sortedMap.toMutableMap<String,my_Activity>()
            addActivitiesToView((currentPageNumber-1)*numOfActInPage, numOfActInPage)
        }
        //by name
        binding.textViewSortByName.setTextColor(Color.BLUE)
        binding.textViewSortByName.setOnClickListener {
            binding.parentActivitiesLayout.removeAllViews()
            if(filteredActivities.isEmpty())
                currentPageNumber = 0
            else
                currentPageNumber = 1
            val simpleMap = filteredActivities.toMutableMap()
            val sortedMap = simpleMap.entries.sortedWith(compareBy { it.value.name.toString() }).map{it.key to it.value}.toMap()
            filteredActivities = sortedMap.toMutableMap()//convertMapToArrayMap(sortedMap)
            addActivitiesToView((currentPageNumber-1)*numOfActInPage, numOfActInPage)
        }

        //sort by location
        tvSortByLocation = binding.textViewSortByLocation
        tvSortByLocation.visibility = View.GONE

        /**********************************FILTERS****************/
        //show all filters button

        binding.imageButtonHideFilters.visibility = View.INVISIBLE
        binding.imageButtonShowAllFilters.setOnClickListener {
            val filtersLayoutParent = binding.constraintFiltersLayout
            val params = filtersLayoutParent.layoutParams as LinearLayout.LayoutParams
            if(params.weight > 0.0f)
            {
                params.weight = 0.0f
                binding.imageButtonShowAllFilters.visibility = View.INVISIBLE
                binding.imageButtonShowAllFilters.isClickable = false
                binding.imageButtonHideFilters.visibility = View.VISIBLE
                binding.imageButtonHideFilters.isClickable = true
                //binding.imageButtonShowAllFilters.setBackgroundResource(R.drawable.uparrow)
                //binding.textViewShowAllFilters.setText("hide filters")
            }
            filtersLayoutParent.layoutParams = params

        }
        binding.imageButtonHideFilters.setOnClickListener {
            val filtersLayoutParent = binding.constraintFiltersLayout
            val params = filtersLayoutParent.layoutParams as LinearLayout.LayoutParams
            if(params.weight == 0.0f)
            {
                if(binding.pageNavigateFooter.visibility == View.GONE)
                    params.weight = 4.0f
                else
                    params.weight = 3.0f
                binding.imageButtonHideFilters.visibility = View.INVISIBLE
                binding.imageButtonHideFilters.isClickable = false
                binding.imageButtonShowAllFilters.visibility = View.VISIBLE
                binding.imageButtonShowAllFilters.isClickable = true
            }
            filtersLayoutParent.layoutParams = params

        }



        //spinner filters initialize

        //*******************/effort spinner/***********************************/
        spinnerEffort = binding.spinnerEffortFilter
        val adapterEffort =
            this.context?.let { ArrayAdapter.createFromResource(it, com.example.vufinder.R.array.effortFilter, android.R.layout.simple_spinner_item) }

        if (adapterEffort != null) {
            adapterEffort.setDropDownViewResource(android.R.layout.simple_spinner_item)
        }
        spinnerEffort.setAdapter(adapterEffort);

        //*******************/time spinner/***************************************
        spinnerTime= binding.spinnerTimeFilter
        val adapterTime =
            this.context?.let { ArrayAdapter.createFromResource(it, com.example.vufinder.R.array.timeFilter, android.R.layout.simple_spinner_item) }

        if (adapterTime != null) {
            adapterTime.setDropDownViewResource(android.R.layout.simple_spinner_item)
        }
        spinnerTime.setAdapter(adapterTime);


        //filters set

        //category-skills filter
        val checkBoxCategoryFilter = binding.checkBoxCategoryFilter
        checkBoxCategoryFilter.setOnCheckedChangeListener { buttonView, isChecked ->
            filters["Category"] = isChecked
            applyFilters()
        }


        //age filter
        val checkBoxAgeFilter = binding.checkBoxAgeFilter
        checkBoxAgeFilter.setOnCheckedChangeListener { buttonView, isChecked ->
            filters["Age"] = isChecked
            applyFilters()
        }

        //gender filter
        val checkBoxGenderFilter = binding.checkBoxGenderFilter
        checkBoxGenderFilter.setOnCheckedChangeListener { buttonView, isChecked ->
            filters["Gender"] = isChecked
            applyFilters()
        }


        //keyword filter
        val buttonSearch = binding.buttonSearch
        etKeyWord = binding.editTextTextKeyWord
        buttonSearch.setOnClickListener{
            filters["KeyWord"] = (etKeyWord.text.toString() != "")
            applyFilters()
        }


        // effort filter
        spinnerEffort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(spinnerEffort.selectedItem.toString() != "show all") {
                    filters["Effort"] = true
                }
                else
                {
                    filters["Effort"] = false
                }
                applyFilters()
            }

        }

        // time filter
        spinnerTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(spinnerTime.selectedItem.toString() != "show all") {
                    filters["Time"] = true
                }
                else
                {
                    filters["Time"] = false
                }
                applyFilters()
            }

        }

        return root
    }

    public override fun onStart() {
        super.onStart()

        applyFilters()//refresh page (after user and activities fetched correctly from DB).

    }
    private fun areFiltersApplied():Boolean
    {
        return filters.filter { it.value == true }.isNotEmpty()
    }
    private fun applyFilters()
    {
        if(!areActivitiesValid)// if we still didn't got the activities from dataBase
            return
        var filteredMap = activitiesArrayMap.toMap<String,my_Activity>()
        //category filter apply
        if(filters["Category"] == true)
        {
            val userSkills = user.skills as HashMap<String,Boolean>
            filteredMap = filteredMap.filter {
                userSkills[it.value.category]==true
            }
        }

        //age filter apply
        if(filters["Age"] == true)
        {
            val userDOB = user.date_of_birth.toString()
            val userDOBSimple = DOBtoSimple(userDOB)
            val userAge = getAge(userDOBSimple)
            filteredMap = filteredMap.filter {
                userAge in it.value.minAge!!.toInt() .. it.value.maxAge!!.toInt()
            }
        }

        //gender filter apply
        if(filters["Gender"] == true)
        {
            val gender = user.gender
            filteredMap = filteredMap.filter {
                (key,value) ->
                (gender=="Male" && "men only"== value.gender) || (gender=="Female" && "women only"== value.gender)
            }
        }

        //effort filter apply
        if(filters["Effort"] == true)
        {
            val effort = spinnerEffort.selectedItem.toString()
            filteredMap = filteredMap.filter {
                    (key,value) ->
                effort == value.effort
            }
        }

        //time filter apply
        if(filters["Time"] == true)
        {
            val time = spinnerTime.selectedItem.toString()
            filteredMap = filteredMap.filter {
                    (key,value) ->
                time == value.time
            }
        }

        //keyword filter
        if(filters["KeyWord"] == true)
        {
            val keyWord = etKeyWord.text.toString()
            filteredMap = filteredMap.filter {
                    (key,value) ->
                keyWord in value.description.toString() || keyWord in value.name.toString()
            }
        }


        filteredActivities =  filteredMap.toMutableMap()//convertMapToArrayMap(filteredMap)
        if(filteredActivities.isNotEmpty())
            currentPageNumber = 1
        else
            currentPageNumber = 0
        totalPageNumber = Math.ceil(filteredActivities.size.toDouble()/numOfActInPage).toInt()
        if(currentPageNumber > 1)
            enablePrevImageButton()
        else
            disablePrevImageButton()
        if(currentPageNumber < totalPageNumber)
            enableNextImageButton()
        else
           disableNextImageButton()
        tvPages.setText("page ${currentPageNumber} out of $totalPageNumber")
        //last lines of apply filters , clear the scroll view and add filtered activities
        binding.parentActivitiesLayout.removeAllViews()
        addActivitiesToView((currentPageNumber-1)*numOfActInPage, numOfActInPage)

    }
    private fun changePage()
    {
        if(currentPageNumber > 1)
            enablePrevImageButton()
        else
            disablePrevImageButton()
        if(currentPageNumber < totalPageNumber)
            enableNextImageButton()
        else
            disableNextImageButton()
        tvPages.setText("page ${currentPageNumber} out of $totalPageNumber")
        binding.parentActivitiesLayout.removeAllViews()
        addActivitiesToView((currentPageNumber-1)*numOfActInPage, numOfActInPage)
    }
    private fun DOBtoSimple(DOB:String):String
    {
        val words = DOB.split(" ")
        val month = getMonthIntFromFormat(words[0]).toString()
        val day = words[1]
        val year = words[2]
        return "$day/$month/$year"

    }
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
    private fun convertMapToArrayMap(map:Map<String,my_Activity>):ArrayMap<String,my_Activity>
    {
        val result = ArrayMap<String,my_Activity>()
        for(entry in map.entries.iterator())
        {
            result[entry.key] = entry.value
        }
        return result
    }
    private fun addActivitiesToView(first_index:Int,numOfActivitiesInPage:Int)
    {
        var noActFlag = true
        binding.parentActivitiesLayout.removeAllViews()
        var i = 0
        for(entry in filteredActivities.entries.iterator())//(i in first_index until first_index+numOfActivitiesInPage)
        {
            if( i== first_index+numOfActivitiesInPage)
                break
            if(i >= first_index) {
                addNewView(entry.value, entry.key)
                noActFlag = false
            }
            i += 1
        }
        if(noActFlag)
        {
            addNoActFoundView()
        }
        //update the map
        getLocation()

    }
    private fun addNoActFoundView()
    {
        val newViewIndex = binding.parentActivitiesLayout.childCount
        val inflater = LayoutInflater.from(requireContext()).inflate(R.layout.no_activities_found, null)
        binding.parentActivitiesLayout.addView(inflater,newViewIndex )
        val newView = binding.parentActivitiesLayout.getChildAt(newViewIndex)

        //bind the text views with new view
        val textViewActFilterSearch : TextView = newView.findViewById(R.id.textViewTryDifferentFilters)
        if(areFiltersApplied())
            textViewActFilterSearch.visibility = View.VISIBLE
        else
            textViewActFilterSearch.visibility = View.INVISIBLE
    }
    private fun addNewView(activity:my_Activity,activityId:String) {
        // this method inflates the single item layout
        // inside the parent linear layout
        val newViewIndex = binding.parentActivitiesLayout.childCount
        val inflater = LayoutInflater.from(requireContext()).inflate(R.layout.activity_one_row, null)
        binding.parentActivitiesLayout.addView(inflater,newViewIndex )
        val newView = binding.parentActivitiesLayout.getChildAt(newViewIndex)
        newView.tag= activityId

        //bind the text views with new view
        val textViewActName : TextView = newView.findViewById(R.id.textViewRowActivityName)
        val textViewActDescription : TextView = newView.findViewById(R.id.textViewRowActivityDescription)
        val textViewActExEffort : TextView = newView.findViewById(R.id.textViewRowActivityEffort)
        val textViewActExTime : TextView = newView.findViewById(R.id.textViewRowActivityExTime)
        val textViewActAge : TextView = newView.findViewById(R.id.textViewRowActivityAgeRestriction)
        val textViewActCategory : TextView = newView.findViewById(R.id.textViewRowActivityCategory)
        val textViewActStartTime : TextView = newView.findViewById(R.id.textViewRowActivityStartingTime)
        val buttonJoin : Button = newView.findViewById(R.id.buttonJoinActivity)
        val buttonUnJoin : Button = newView.findViewById(R.id.buttonUnJoinActivity)
        val textViewLocation : TextView = newView.findViewById(R.id.textViewLocation)
        val textViewCountry :TextView = newView.findViewById(R.id.textViewCountry)
        val textViewCity :TextView = newView.findViewById(R.id.textViewCity)
        val textViewDistance :TextView = newView.findViewById(R.id.textViewDistance)
        val textViewCreator : TextView = newView.findViewById(R.id.textViewCreator)
        val imageViewCreator : ImageView = newView.findViewById(R.id.imageViewCreator)
        val imageViewImage : ImageView = newView.findViewById(com.example.vufinder.R.id.imageViewImageOneRow)
        var imageUri :Bitmap?=null
        val sr: StorageReference = FirebaseStorage.getInstance().reference
        val ONE_MEGABYTE: Long = 1024 * 1024
        sr.child("activities/$activityId/activity").getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes->
            imageUri = BitmapFactory.decodeByteArray(bytes,0,bytes.size, BitmapFactory.Options())
        }.addOnFailureListener {
            Log.e("Firebase", "Failed in downloading")
        }
        sr.child("activities/$activityId/activity").downloadUrl.addOnSuccessListener {
            Glide.with(this@JoinActivityFragment)
                .load(it)
                .into(imageViewImage)
            Log.e("Firebase", "download passed")
        }.addOnFailureListener {
            Log.e("Firebase", "Failed in downloading")
        }


        //location
        val location = activity.location
        if(location?.isNotEmpty() == true) {
            val cityName = location.get("cityName")
            val countryName = location.get("countryName")
            val latitude = location.get("latitude")
            val longitude = location.get("longitude")
            textViewCountry.setText(countryName)
            textViewCity.setText(",$cityName")

            //show distance to activity
            if (current_lat != null && current_long != null) {
                val currentLat = current_lat as Double
                val currentLong = current_long as Double
                if (latitude != null && longitude != null) {
                    val distance = distanceInKm(currentLat,currentLong,latitude.toDouble(),longitude.toDouble())
                    val distanceOneDigit = doubleToShortDouble(distance)
                    textViewDistance.setText(distanceOneDigit.toString() + " Km")
                }

            }

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
            textViewLocation.setTextColor(Color.BLACK)
            textViewCountry.setText("From home")
            val imageView : ImageView = newView.findViewById(R.id.imageViewLocation)
            imageView.setBackgroundResource(R.drawable.home)
            textViewDistance.visibility = View.INVISIBLE
        }


        //fill the data from the input variable (my_activity)
        textViewActName.setText(activity.name)
        textViewActDescription.setText(activity.description)
        textViewActExEffort.setText(activity.effort)
        textViewActExTime.setText(activity.time)
        textViewActAge.setText(activity.minAge + "-" +activity.maxAge+" years old!")
        textViewActCategory.setText(activity.category)
        textViewActStartTime.setText(activity.startingDate + ", " + activity.startingTime)


        if(isUsrValid && activityId in user.hosted_activities!!.keys)
        {
            buttonJoin.isEnabled = false
            buttonUnJoin.isEnabled = false
        }
        //if already joined disable join button
        else if(isUsrValid && activityId in user.joined_activities!!.keys)
        {
            buttonJoin.isEnabled = false
            buttonUnJoin.isEnabled = true
        }
        else // if not joined disable unJoin button
        {
            buttonJoin.isEnabled = true
            buttonUnJoin.isEnabled = false
        }

        if(isUsrValid && activityId in user.hosted_activities!!.keys)
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

        //join activity button
        buttonJoin.setOnClickListener{
            if(isUsrValid) {

                user.joined_activities!![activityId] = activity.name.toString()
                //db.collection("users").document(user_id).set(user)
                db.collection("users").document(user_id).update(mapOf(
                    "joined_activities.$activityId" to activity.name.toString())
                ).addOnSuccessListener {
                    activity.participants!![user_id] = user.name.toString()
                    /*db.collection("activities").document(activityId). set(activity)*/
                    db.collection("activities").document(activityId).update(mapOf(
                        "participants.$user_id" to user.name.toString())
                    ).addOnSuccessListener {
                            sendJoinedMessage(user_id,activity, imageUri)

                            reloadJoinActivityFragment()
                        }
                }
            }
        }

        //UnJoin activity button
        buttonUnJoin.setOnClickListener{
            if(isUsrValid) {
                user.joined_activities!!.remove(activityId)
                db.collection("users").document(user_id).update(mapOf(
                    "joined_activities.$activityId" to FieldValue.delete())
                )//.set(user)
                    .addOnSuccessListener {
                    //activity.participants!!.remove(user_id)
                    db.collection("activities").document(activityId).update(mapOf(
                        "participants.$user_id" to FieldValue.delete())
                    )
                    //set(activity)
                        .addOnSuccessListener {
                        sendUnJoinedMessage(user_id,activity,imageUri)
                        reloadJoinActivityFragment()
                    }
                }
            }
        }

    }

    private fun addUserPopView(userId:String) {
        val newView = LayoutInflater.from(requireContext()).inflate(com.example.vufinder.R.layout.profile, null)
        val textViewName : TextView = newView.findViewById(R.id.textViewProfileNameValue)
        val textViewAge : TextView = newView.findViewById(R.id.textViewProfileAgeValue)
        val textViewGender : TextView = newView.findViewById(R.id.textViewProfileGender)
        val imageViewProfileParticipant:ImageView = newView.findViewById(com.example.vufinder.R.id.imageViewProfile)

        //download profile pic
        val sr: StorageReference = FirebaseStorage.getInstance().reference
        sr.child("users/$userId/profile").downloadUrl.addOnSuccessListener {
            Glide.with(this@JoinActivityFragment)
                .load(it)
                .into(imageViewProfileParticipant)

            Log.e("Firebase", "download passed")
        }.addOnFailureListener {
            Log.e("Firebase", "Failed in downloading")
        }
        db.collection("users").document(userId).get().addOnSuccessListener {
            val user = it.toObject<MyUser>()
            if(user!=null)
            {
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

    private fun addMapView() {
        // this method inflates the single item layout
        // inside the parent linear layout
        binding.parentActivitiesLayout.removeAllViews()
        val newViewIndex = binding.parentActivitiesLayout.childCount

        //isMapViewOn = true

        //Map View init
        if(mMap==null) {
            val inflater = LayoutInflater.from(requireContext()).inflate(R.layout.activity_showon_map, null)
            binding.parentActivitiesLayout.addView(inflater,newViewIndex )
            val newView = binding.parentActivitiesLayout.getChildAt(newViewIndex)
            mapFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.mapShowLocation) as SupportMapFragment
            mapFragment!!.getMapAsync(this)
            mapView = newView
        }
        else{
            binding.parentActivitiesLayout.addView(mapView,newViewIndex )
            getLocation()
            }

        //val containerActivity = activity as UserMenuActivity
        //var mMap = containerActivity.mMap
        //var isMapReady = containerActivity.isMapReady
        //buttonMapViewOn.performClick()
        /*while (mMap == null || !isMapReady){
            Thread.sleep(1)
            //mMap = containerActivity.mMap
            //isMapReady = containerActivity.isMapReady
        }*/
        //bind the text views with new view
        val buttonClose : Button = mapView.findViewById(R.id.buttonCloseMap)

        //close mapView button
        buttonClose.setOnClickListener{
            //isMapViewOn = false
            //mMap = null
            //mapFragment = null
            binding.parentActivitiesLayout.removeAllViews()
            binding.pageNavigateFooter.visibility = View.VISIBLE

            val filtersLayoutParent = binding.constraintFiltersLayout
            val params = filtersLayoutParent.layoutParams as LinearLayout.LayoutParams
            params.weight = 3.0f
            filtersLayoutParent.layoutParams = params

            addActivitiesToView((currentPageNumber-1)*numOfActInPage,numOfActInPage)
        }


    }
/*
    private fun addMapToFragment(fragment: SupportMapFragment) {
            val mapFragment = fragment
            mapFragment.getMapAsync { googleMap->
                mapFragment.getMapAsync { googleMap = mMap!! }
            }
    }
*/

    private fun reloadJoinActivityFragment(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fragmentManager?.beginTransaction()?.detach(this)?.commitNow();
            fragmentManager?.beginTransaction()?.attach(this)?.commitNow();
        } else {
            fragmentManager?.beginTransaction()?.detach(this)?.attach(this)?.commit();
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        areActivitiesValid = false
        isUsrValid = false
        totalPageNumber = 0
        currentPageNumber = 1
    }
    public fun enablePrevImageButton()
    {
        buttonPrevPage.isEnabled = true
        buttonPrevPage.isClickable = true

        //buttonPrevPage.visibility = View.VISIBLE
        buttonPrevPage.setBackgroundResource(R.drawable.ic_vufinder_left_arrow_black)
    }
    public fun enableNextImageButton()
    {
        buttonNextPage.isEnabled = true
        buttonNextPage.isClickable = true
        //buttonNextPage.visibility = View.VISIBLE
        buttonNextPage.setBackgroundResource(R.drawable.ic_vufinder_right_arrow_black)
    }
    public fun disablePrevImageButton()
    {
        buttonPrevPage.isEnabled = false
        buttonPrevPage.isClickable = false
        //buttonPrevPage.visibility = View.INVISIBLE
        buttonPrevPage.setBackgroundResource(R.drawable.ic_vufinder_arrow_left_gray)
    }
    public fun disableNextImageButton()
    {
        buttonNextPage.isEnabled = false
        buttonNextPage.isClickable = false
        buttonNextPage.setBackgroundResource(R.drawable.ic_vufinder_right_arrow_gray)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                // Write your code if there's no result
            }
        }
    } //onActivityResult



    //mapView

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }
    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true
        getLocation()
    }

     fun secondTimeMap(googleMap: GoogleMap) {
        //mMap = googleMap
        isMapReady = true
        getLocation()
    }


    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocation() {
        if (mMap != null && checkPermissions()) {
            if (isLocationEnabled()) {
                mMap?.clear()
                mFusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val list: List<Address> =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) as List<Address>
                        val current = LatLng(list[0].latitude, list[0].longitude)
                        val latLongBoundBuilder = LatLngBounds.builder()
                        //add activities to map
                        for(activity in filteredActivities.values)
                        {
                            if(activity.location != null  && activity.location.isNotEmpty()) {
                                val geocoder2 = Geocoder(requireContext(), Locale.getDefault())
                                val list2: List<Address> =
                                    geocoder2.getFromLocation(
                                        activity.location?.get("latitude")!!.toDouble(),
                                        activity.location?.get("longitude")!!.toDouble(),
                                        1
                                    ) as List<Address>
                                val activityLocation = LatLng(list2[0].latitude, list2[0].longitude)
                                mMap?.addMarker(
                                    MarkerOptions()
                                        .position(activityLocation)
                                        .title(activity.name)
                                )
                                latLongBoundBuilder.include(activityLocation)
                            }
                        }

                        mMap?.addMarker(
                            MarkerOptions()
                                .position(current)
                                .title("current location"))
                        latLongBoundBuilder.include(current)
                        val width = resources.displayMetrics.widthPixels
                        val height = resources.displayMetrics.heightPixels
                        val padding = (width * 0.15).toInt()
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(latLongBoundBuilder.build(), width, height, padding))
                        //mMap?.moveCamera(CameraUpdateFactory.newLatLng(current))
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getCurrLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val list: List<Address> =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) as List<Address>
                        val current = LatLng(list[0].latitude, list[0].longitude)
                        current_lat = current.latitude
                        current_long = current.longitude
                        tvSortByLocation.visibility = View.VISIBLE
                        tvSortByLocation.setTextColor(Color.BLUE)
                            tvSortByLocation.setOnClickListener {
                                binding.parentActivitiesLayout.removeAllViews()
                                if (filteredActivities.isEmpty())
                                    currentPageNumber = 0
                                else
                                    currentPageNumber = 1
                                val simpleMap = filteredActivities.toMutableMap()
                                val locationsMap = HashMap<String,my_Activity>() as MutableMap<String,my_Activity>
                                val noLocationsMap = HashMap<String,my_Activity>() as MutableMap<String,my_Activity>
                                //split into two maps (with and without locations)
                                for (entry in simpleMap.entries){
                                    val activity = entry.value
                                    if(activity.location == null || activity.location.isEmpty()
                                        || ("latitude" !in activity.location.keys) || "longitude" !in activity.location.keys
                                    )
                                        noLocationsMap[entry.key] = activity
                                    else
                                        locationsMap[entry.key] = activity
                                }
                                val sortedMap = locationsMap.entries.sortedWith(compareBy {
                                    distanceInKm(
                                        it.value.location!!["latitude"]!!.toDouble(),
                                        it.value.location!!["longitude"]!!.toDouble(),
                                        current_lat!!.toDouble(),
                                        current_long!!.toDouble()
                                    )
                                }).map { it.key to it.value }.toMap()
                                val concatMap = sortedMap + noLocationsMap
                                filteredActivities = concatMap.toMutableMap()//convertMapToArrayMap(sortedMap)
                                addActivitiesToView((currentPageNumber - 1) * numOfActInPage, numOfActInPage)
                            }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }
    fun distanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515
        dist = dist * 1.609344
        return dist

    }
    fun doubleToShortDouble(num:Double): Double {
        val number2digits:Double = String.format("%.2f", num).toDouble()
        val solution:Double = Math.round(number2digits * 10.0) / 10.0
        return solution
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }
    private fun bitmapToFile(bitmap: Bitmap): Uri {
        // Get the context wrapper
        val wrapper = ContextWrapper(requireContext())

        // Initialize a new file instance to save bitmap object
        var file = wrapper.getDir("Images",Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")

        try{
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e: IOException){
            e.printStackTrace()
        }

        // Return the saved bitmap uri
        return Uri.parse(file.absolutePath)
    }
    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
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
                    if(m.id != messageId) {
                        db.collection("messages_$hostId").document(m.id).delete()
                        sr.child("messages/${m.id}/image1").delete()
                    }
                }
            }
        }
    }
}
