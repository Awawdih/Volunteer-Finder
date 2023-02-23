package com.example.vufinder

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.StateSet.TAG
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class HandleGoogleSignInActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var datePickerDialog: DatePickerDialog
    private lateinit var dateButton: Button
    private fun FullScreencall() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            val v: View = this.getWindow().getDecorView()
            v.setSystemUiVisibility(View.GONE)
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            val decorView: View = this.getWindow().getDecorView()
            val uiOptions: Int =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            decorView.setSystemUiVisibility(uiOptions)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_handle_google_sign_in)
        FullScreencall()

        db = Firebase.firestore
        initDatePicker();
        dateButton = findViewById(R.id.buttonDateGoogle) as Button
        dateButton.text = getTodaysDate();

        val account = GoogleSignIn.getLastSignedInAccount(this)
        var name = ""
        var email = ""
        var userID = "ERROR"
        if(account != null) {
            name = account.displayName.toString()
            email = account.email.toString()
            userID = account.id.toString()
        }

        val buttonConfirm = findViewById(R.id.buttonConfirmGoogle) as Button
        buttonConfirm.setOnClickListener{
            val DOB = dateButton.text.toString()
            val phoneNumber = findViewById<EditText>(R.id.phoneNumberGoogle).text.toString()

            if(check_user_inputs(name,email,phoneNumber,DOB))
            {
                addUserBasicInfo(email,name,phoneNumber,DOB,userID)
                val intent = Intent(this, UserMenuActivity::class.java)
                startActivity(intent)
            }
        }
    }


    private fun check_user_inputs(personName:String,email:String,phoneNumber:String,date_of_birth:String): Boolean
    {
        var valid = true;
        var error_message = ""
        if(phoneNumber.length != 10 || !phoneNumber.startsWith("05"))
        {
            error_message = "phone mobile number is not valid.";
            valid = false
        }
        else if(!minimumAge(5,date_of_birth))
        {
            error_message = "you should be at least 5 years old!";
            valid = false
        }
        if(!valid)
            Toast.makeText(this, error_message, Toast.LENGTH_LONG).show()
        return valid
    }

    /************************************dateFUNS********************/
    //date pickup
    private fun getTodaysDate(): String? {
        val cal: Calendar = Calendar.getInstance()
        val year: Int = cal.get(Calendar.YEAR)
        var month: Int = cal.get(Calendar.MONTH)
        month = month + 1
        val day: Int = cal.get(Calendar.DAY_OF_MONTH)
        return makeDateString(day, month, year)
    }
    private fun minimumAge(years:Int , DOB:String):Boolean{
        val DOBSimple = DOBtoSimplePlus(DOB,years)
        val formatter = SimpleDateFormat("dd/MM/yyyy")
        val date = formatter.parse(DOBSimple)
        val currentDate = Calendar.getInstance().time
        return date!!.before(currentDate)
    }
    private fun DOBtoSimplePlus(DOB:String,years: Int):String
    {
        val words = DOB.split(" ")
        val month = getMonthIntFromFormat(words[0]).toString()
        val day = words[1]
        val year = words[2].toInt() + years
        return "$day/$month/$year"

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
    private fun initDatePicker() {
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                var month = month
                month = month + 1
                val date = makeDateString(day, month, year)
                dateButton?.setText(date)
            }
        val cal: Calendar = Calendar.getInstance()
        val year: Int = cal.get(Calendar.YEAR)
        val month: Int = cal.get(Calendar.MONTH)
        val day: Int = cal.get(Calendar.DAY_OF_MONTH)
        val style: Int = AlertDialog.THEME_HOLO_LIGHT
        datePickerDialog = DatePickerDialog(this, style, dateSetListener, year, month, day)
        //datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
    }

    private fun makeDateString(day: Int, month: Int, year: Int): String {
        return getMonthFormat(month) + " " + day + " " + year
    }

    private fun getMonthFormat(month: Int): String {
        if (month == 1) return "JAN"
        if (month == 2) return "FEB"
        if (month == 3) return "MAR"
        if (month == 4) return "APR"
        if (month == 5) return "MAY"
        if (month == 6) return "JUN"
        if (month == 7) return "JUL"
        if (month == 8) return "AUG"
        if (month == 9) return "SEP"
        if (month == 10) return "OCT"
        if (month == 11) return "NOV"
        return if (month == 12) "DEC" else "JAN"

        //default should never happen
    }

    private fun addUserBasicInfo(email: String, name: String, phone_number: String,date_of_birth: String,userID:String) {
        // [START add_ada_lovelace]
        //define skills hashmap
        val skills: MutableMap<String, Boolean> = HashMap()
        skills["hand_craft"] = false
        skills["sport"] = false
        skills["teach"] = false
        skills["hard_worker"] = false
        skills["artist"] = false
        skills["fix_things"] = false
        skills["bilingual"] = false
        skills["medical_training"] = false
        skills["cocking"] = false
        skills["other"] = false
        val empty_activities : MutableMap<String, String> = HashMap()
        // Create a new user with a first and last name
        val user = hashMapOf(
            //"userID" to userID,
            "name" to name,
            "gender" to "rather not say",
            "email" to email,
            "password" to "",
            "phone_number" to phone_number,
            "date_of_birth" to date_of_birth,
            "skills" to skills,
            "joined_activities" to empty_activities,
            "hosted_activities" to empty_activities
        )
        // Add a new document with a generated ID
        //db.collection("users").add(user)
        db.collection("users").document(userID).set(user)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${userID}")
                //documentReference.id.
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
        // [END add_ada_lovelace]
    }

    fun openDatePicker(view: View?) {
        datePickerDialog?.show()
    }
}