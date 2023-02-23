package com.example.vufinder.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.vufinder.R
import com.example.vufinder.ShowOnMapActivity
import com.example.vufinder.databinding.FragmentInboxBinding
import com.example.vufinder.ui.CreateActivity.my_Activity
import com.example.vufinder.ui.profile.MyUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.*


data class Message(
    val type: String? = null,
    val subject: String? = null,
    val activity1 : my_Activity? = null,
    val activity2 : my_Activity? = null,
    var image1 : Bitmap? = null,
    var image2 : Bitmap? = null,
    val joinedUserID:String? = null,
    var isRead: Boolean=false,
    val createdAt: Timestamp = Timestamp.now()
)
class InboxFragment : Fragment() {
    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!
    private lateinit var db:FirebaseFirestore
    private lateinit var auth:FirebaseAuth
    private lateinit var viewModel: InboxViewModel
    private lateinit var userID:String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        val root: View = binding.root
        db = Firebase.firestore
        auth = Firebase.auth
        if (auth.currentUser != null) {
            userID = auth.currentUser!!.uid
        }
        else {
            userID = GoogleSignIn.getLastSignedInAccount(requireContext())?.id.toString()
        }

        buildInboxView()

        return root

    }
    private fun buildInboxView() {
        var noMessages = true
        var index = 0
        var messagesMap = HashMap<String,Message>()
        db.collection("messages_$userID").get().addOnSuccessListener { result ->
            for (messageDoc in result) {
                noMessages = false
                val currentMessage = messageDoc.toObject<Message>()
                val messageID = messageDoc.id
                messagesMap[messageID] = currentMessage
            }
            if(!messagesMap.isNullOrEmpty() && messagesMap.size>1) {
                messagesMap = messagesMap.entries.sortedWith(compareBy { it.value.createdAt })
                    .map { it.key to it.value }.toMap() as HashMap<String, Message>
            }
            for (entry in messagesMap.entries) {
                val messageID = entry.key
                val currentMessage = entry.value
                //FireBase storage download images
                val sr = Firebase.storage.reference
                val ONE_MEGABYTE: Long = 1024 * 1024
                val newViewIndex = binding.subjectInboxLayout.childCount
                val inflater = LayoutInflater.from(requireContext())
                    .inflate(R.layout.subject, null)
                sr.child("messages/$messageID/image1").getBytes(ONE_MEGABYTE)
                    .addOnCompleteListener { it1 ->
                        if (it1.isSuccessful) {
                            val bytes1 = it1.result
                            val imageUri1 =
                                BitmapFactory.decodeByteArray(
                                    bytes1,
                                    0,
                                    bytes1.size,
                                    BitmapFactory.Options()
                                )
                            currentMessage.image1 = imageUri1
                        }
                        sr.child("messages/$messageID/image2").getBytes(ONE_MEGABYTE)
                            .addOnCompleteListener { it2 ->
                                if (it2.isSuccessful) {
                                    val bytes2 = it2.result
                                    val imageUri2 =
                                        BitmapFactory.decodeByteArray(
                                            bytes2,
                                            0,
                                            bytes2.size,
                                            BitmapFactory.Options()
                                        )
                                    currentMessage.image2 = imageUri2
                                }

                                addMessageView(messageID, currentMessage, index, inflater)
                                index += 1
                            }
                    }
                binding.subjectInboxLayout.addView(inflater, newViewIndex)

            }
            if (noMessages)
                addNoMailView()
        }

    }
    private fun addMessageView(messageID:String,message:Message,index:Int,newView: View){
        //val newView = binding.subjectInboxLayout.getChildAt(index)
        val buttonSubject : Button = newView.findViewById(R.id.buttonInboxSubject)
        val tvAgo : TextView = newView.findViewById(R.id.textViewMessageAgo)
        //calculate time from now to message.timestamp
        val timestamp = message.createdAt
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        //val time = sdf.parse("2016-01-24T16:00:00.000Z").time
        val now = System.currentTimeMillis()
        val ago = DateUtils.getRelativeTimeSpanString(timestamp.toDate().time, now, DateUtils.MINUTE_IN_MILLIS)


        if(message.isRead == true)
        {
            buttonSubject.setBackgroundColor(Color.GRAY)
        }
        val subject = message.subject.toString()
        val type = message.type.toString()

        buttonSubject.text= subject
        tvAgo.text= ago.toString()

        buttonSubject.setOnClickListener{
            binding.parentInboxLayout.removeAllViews()
            val newViewIndex = binding.parentInboxLayout.childCount
            val inflater = LayoutInflater.from(requireContext()).inflate(R.layout.update_body_message, null)

            binding.parentInboxLayout.addView(inflater,newViewIndex )
            val newView = binding.parentInboxLayout.getChildAt(newViewIndex)
            val closeButton = newView.findViewById<Button>(R.id.buttonCloseMessage)
            closeButton.setOnClickListener {
                reloadInboxFragment()
            }
                when (type){
            "activity_update" ->
                addUpdateBody(newView,messageID,message)

            "activity_delete" ->
                addDeletedBody(newView,messageID,message)

            "user_joined" ->
                addJoinedBody(newView,messageID,message)

            "user_unJoined" ->
                addUnJoinBody(newView,messageID,message)
        }
            message.isRead = true
            message.image2 = null
            message.image1 = null
            db.collection("messages_$userID").document(messageID).set(message)
        }
        }

    private fun addUnJoinBody(newView:View,messageId:String,message: Message) {
        addJoinedBody(newView,messageId,message,true)
    }

    private fun addJoinedBody(newView:View,messageId:String,message: Message,unJoined:Boolean = false) {

        val joinedUserLayout = newView.findViewById<LinearLayout>(R.id.body_message_update_old_activity)
        val activityLayout = newView.findViewById<LinearLayout>(R.id.body_message_update_new_activity)
        val textViewNewAct = newView.findViewById<TextView>(R.id.textViewNewActivity)
        val textViewOldAct = newView.findViewById<TextView>(R.id.textViewOldActivity)
        val textViewUpdateMessageContent = newView.findViewById<TextView>(R.id.textViewUpdateMessageContent)

        val deleteButton = newView.findViewById<Button>(R.id.buttonDeleteMessage)

        deleteButton.setOnClickListener{
            db.collection("messages_$userID").document(messageId).delete().addOnSuccessListener {
                val sr = Firebase.storage.reference
                sr.child("messages/$messageId/image1").delete()
                reloadInboxFragment()
            }
        }


        val joinedUserID = message.joinedUserID.toString()

        //update text
        textViewOldAct.text = "user:"
        textViewNewAct.text = "activity:"
        if(unJoined)
        {
            textViewUpdateMessageContent.text = "a user has Un-joined from your activity"
            textViewUpdateMessageContent.setTextColor(Color.RED)
        }
        else
            textViewUpdateMessageContent.text = "a user has joined your activity"

        //inflate layouts
        val inflaterActivity = LayoutInflater.from(requireContext()).inflate(R.layout.activity_one_row, null)
        activityLayout.addView(inflaterActivity,0 )
        val activityView = activityLayout.getChildAt(0)
        val inflaterUser = LayoutInflater.from(requireContext()).inflate(R.layout.profile, null)
        joinedUserLayout.addView(inflaterUser,0 )
        val userView = joinedUserLayout.getChildAt(0)

        //fill layouts
        fillActivityView(message.activity1!!,activityView,message.image1)
        fillUserInfo(userView,joinedUserID)
    }

    private fun addUpdateBody(newView:View,messageId:String,message: Message) {


        val oldActivityLayout = newView.findViewById<LinearLayout>(R.id.body_message_update_old_activity)
        val newActivityLayout = newView.findViewById<LinearLayout>(R.id.body_message_update_new_activity)
        val deleteButton = newView.findViewById<Button>(R.id.buttonDeleteMessage)


        deleteButton.setOnClickListener{
            db.collection("messages_$userID").document(messageId).delete().addOnSuccessListener {
                val sr = Firebase.storage.reference
                sr.child("messages/$messageId/image1").delete()
                sr.child("messages/$messageId/image2").delete()
                reloadInboxFragment()
            }
        }

        val inflaterOldActivity = LayoutInflater.from(requireContext()).inflate(R.layout.activity_one_row, null)
        val inflaterNewActivity = LayoutInflater.from(requireContext()).inflate(R.layout.activity_one_row, null)
        oldActivityLayout.addView(inflaterOldActivity,0 )
        newActivityLayout.addView(inflaterNewActivity,0 )
        val oldActivityView = oldActivityLayout.getChildAt(0)
        val newActivityView = newActivityLayout.getChildAt(0)

        fillActivityView(message.activity1!!,oldActivityView,message.image1)
        fillActivityView(message.activity2!!,newActivityView,message.image2)

    }
    private fun addDeletedBody(newView:View,messageId:String,message: Message) {

        val oldActivityLayout = newView.findViewById<LinearLayout>(R.id.body_message_update_old_activity)
        val newActivityLayout = newView.findViewById<LinearLayout>(R.id.body_message_update_new_activity)
        val textViewNewAct = newView.findViewById<TextView>(R.id.textViewNewActivity)
        val textViewUpdateMessageContent = newView.findViewById<TextView>(R.id.textViewUpdateMessageContent)

        val deleteButton = newView.findViewById<Button>(R.id.buttonDeleteMessage)

        deleteButton.setOnClickListener{
            db.collection("messages_$userID").document(messageId).delete().addOnSuccessListener {
                val sr = Firebase.storage.reference
                sr.child("messages/$messageId/image1").delete()
                reloadInboxFragment()
            }
        }


        newActivityLayout.visibility = View.GONE
        textViewNewAct.visibility = View.GONE
        textViewUpdateMessageContent.text.toString().replace("updated","deleted")

        val inflaterActivity = LayoutInflater.from(requireContext()).inflate(R.layout.activity_one_row, null)
        oldActivityLayout.addView(inflaterActivity,0 )
        val oldActivityView = oldActivityLayout.getChildAt(0)

        fillActivityView(message.activity1!!,oldActivityView,message.image1)

    }
    private fun reloadInboxFragment(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fragmentManager?.beginTransaction()?.detach(this)?.commitNow();
            fragmentManager?.beginTransaction()?.attach(this)?.commitNow();
        } else {
            fragmentManager?.beginTransaction()?.detach(this)?.attach(this)?.commit();
        }
    }
    private fun fillActivityView(activity:my_Activity,view:View,imageUri: Bitmap?) {
        val textViewActName: TextView = view.findViewById(R.id.textViewRowActivityName)
        val textViewActDescription: TextView =
            view.findViewById(R.id.textViewRowActivityDescription)
        val textViewActExEffort: TextView = view.findViewById(R.id.textViewRowActivityEffort)
        val textViewActExTime: TextView = view.findViewById(R.id.textViewRowActivityExTime)
        val textViewActAge: TextView = view.findViewById(R.id.textViewRowActivityAgeRestriction)
        val textViewActCategory: TextView = view.findViewById(R.id.textViewRowActivityCategory)
        val textViewActStartTime: TextView = view.findViewById(R.id.textViewRowActivityStartingTime)
        val buttonJoin: Button = view.findViewById(R.id.buttonJoinActivity)
        val buttonUnJoin: Button = view.findViewById(R.id.buttonUnJoinActivity)
        val textViewLocation: TextView = view.findViewById(R.id.textViewLocation)
        val textViewCountry: TextView = view.findViewById(R.id.textViewCountry)
        val textViewCity: TextView = view.findViewById(R.id.textViewCity)
        val textViewDistance: TextView = view.findViewById(R.id.textViewDistance)
        val textViewCreator: TextView = view.findViewById(R.id.textViewCreator)
        val imageViewCreator: ImageView = view.findViewById(R.id.imageViewCreator)
        val imageViewImage: ImageView = view.findViewById(R.id.imageViewImageOneRow)

        buttonJoin.visibility = View.GONE
        buttonUnJoin.visibility = View.GONE
        textViewDistance.visibility = View.GONE

        //location
        val location = activity.location
        if (location?.isNotEmpty() == true) {
            val cityName = location.get("cityName")
            val countryName = location.get("countryName")
            val latitude = location.get("latitude")
            val longitude = location.get("longitude")
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
            val imageView : ImageView = view.findViewById(R.id.imageViewLocation)
            imageView.setBackgroundResource(R.drawable.home)
        }
        //fill the data from the input variable (my_activity)
        textViewActName.setText(activity.name)
        textViewActDescription.setText(activity.description)
        textViewActExEffort.setText(activity.effort)
        textViewActExTime.setText(activity.time)
        textViewActAge.setText(activity.minAge + "-" +activity.maxAge+" years old!")
        textViewActCategory.setText(activity.category)
        textViewActStartTime.setText(activity.startingDate + ", " + activity.startingTime)

        textViewCreator.setTextColor(Color.BLUE)
        textViewCreator.setOnClickListener{
            addUserPopView(activity.creator_id.toString())
        }
        if(imageUri!=null)
            imageViewImage.setImageBitmap(imageUri)
    }
    private fun fillUserInfo (newView:View,userId:String )
    {
        val textViewName : TextView = newView.findViewById(R.id.textViewProfileNameValue)
        val textViewAge : TextView = newView.findViewById(R.id.textViewProfileAgeValue)
        val textViewGender : TextView = newView.findViewById(R.id.textViewProfileGender)
        val imageViewProfileParticipant:ImageView = newView.findViewById(com.example.vufinder.R.id.imageViewProfile)

        //download profile pic
        val sr: StorageReference = FirebaseStorage.getInstance().reference
        sr.child("users/$userId/profile").downloadUrl.addOnSuccessListener {
            Glide.with(this@InboxFragment)
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
    }
    private fun addUserPopView(userId:String) {
        val newView = LayoutInflater.from(requireContext()).inflate(com.example.vufinder.R.layout.profile, null)

        fillUserInfo(newView,userId)

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
    private fun DOBtoSimple(DOB:String):String
    {
        val words = DOB.split(" ")
        val month = getMonthIntFromFormat(words[0]).toString()
        val day = words[1]
        val year = words[2]
        return "$day/$month/$year"

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
    private fun addNoMailView()
    {
        binding.parentInboxLayout.removeAllViews()
        val newViewIndex = binding.parentInboxLayout.childCount
        val inflater = LayoutInflater.from(requireContext()).inflate(R.layout.no_activities_found, null)
        binding.parentInboxLayout.addView(inflater,newViewIndex )
        val newView = binding.parentInboxLayout.getChildAt(newViewIndex)

        //bind the text views with new view
        val textViewActFilterSearch : TextView = newView.findViewById(R.id.textViewTryDifferentFilters)
        val textViewNoMail : TextView = newView.findViewById(R.id.textViewNoActivitiesFound)
        textViewNoMail.setText("No mail in Inbox!")
        textViewActFilterSearch.visibility = View.INVISIBLE

    }
}