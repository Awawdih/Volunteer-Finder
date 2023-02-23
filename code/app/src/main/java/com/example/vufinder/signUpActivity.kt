package com.example.vufinder

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


class signUpActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var datePickerDialog: DatePickerDialog
    private lateinit var dateButton: Button
    companion object {
        private const val TAG = "EmailPassword"
    }
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

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser != null){
            reload();
        }
    }

    private fun createAccount(email: String, password: String,name: String,phone_number: String,date_of_birth: String) {
        // [START create_user_with_email]
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                    addUserBasicInfo(email,password,name, phone_number,date_of_birth, user?.uid ?: "0")
                    Toast.makeText(this@signUpActivity, "Registered Successfully", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, LogInActivity::class.java)
                    intent.putExtra("Email",email)
                    //Thread.sleep(1_500)
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
        // [END create_user_with_email]
    }
    private fun setup() {
        // [START get_firestore_instance]
        val db = Firebase.firestore
        // [END get_firestore_instance]

        // [START set_firestore_settings]
        val settings = firestoreSettings {
            isPersistenceEnabled = true
        }
        db.firestoreSettings = settings
        // [END set_firestore_settings]
    }
    private fun addUserBasicInfo(email: String, password: String, name: String, phone_number: String,date_of_birth: String,userID:String) {
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
            "password" to password,
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
    public fun getname(user: FirebaseUser?): String? {
        if (user != null) {

            return user.displayName
        }
    return null
    }

    private fun sendEmailVerification() {
        // [START send_email_verification]
        val user = auth.currentUser!!
        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                // Email Verification sent
            }
        // [END send_email_verification]
    }

    private fun updateUI(user: FirebaseUser?) {

    }

    private fun reload() {

    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        FullScreencall()
        //FireBase initialize
        auth = Firebase.auth
        db = Firebase.firestore
        // get reference to all views
        var et_name = findViewById(R.id.Name) as EditText
        var et_Email = findViewById(R.id.Email) as EditText
        var et_phoneNumber = findViewById(R.id.phoneNumber) as EditText
        var et_password = findViewById(R.id.password) as EditText
        var et_confirmPassword = findViewById(R.id.confirmPassword) as EditText
        var btn_login = findViewById(R.id.log_in_button) as Button
        var btn_sign_up = findViewById(R.id.Signup_button) as Button

        val ibHome = findViewById<ImageButton>(R.id.imageButtonSignUp)
        ibHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        initDatePicker();
        dateButton = findViewById(R.id.buttonDate) as Button
        dateButton.text = getTodaysDate();

        btn_login.setOnClickListener {
            // clearing user_name and password edit text views on reset button click
            val intent = Intent(this, LogInActivity::class.java)
            startActivity(intent)

        }

        // set on-click listener
        btn_sign_up.setOnClickListener {
            val personName = et_name.text.toString();
            val password = et_password.text.toString();
            val email = et_Email.text.toString();
            val confirmPassword = et_confirmPassword.text.toString();
            val phoneNumber = et_phoneNumber.text.toString();
            val date_of_birth = dateButton.text.toString();
            if(check_user_inputs(personName,password,email,confirmPassword,phoneNumber,date_of_birth)){
                createAccount(email, password,personName,phoneNumber,date_of_birth)
            }

            // your code to validate the user_name and password combination
            // and verify the same

        }

    }

    fun Password_Validation(password: String): Boolean {
        return if (password.length >= 8) {
            val letter: Pattern = Pattern.compile("[a-zA-z]")
            val digit: Pattern = Pattern.compile("[0-9]")
            val special: Pattern = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]")
            //Pattern eight = Pattern.compile (".{8}");
            val hasLetter: Matcher = letter.matcher(password)
            val hasDigit: Matcher = digit.matcher(password)
            val hasSpecial: Matcher = special.matcher(password)
            hasLetter.find() && hasDigit.find() && hasSpecial.find()
        } else false
    }

    private fun check_user_inputs(personName:String,password:String,email:String,confirmPassword:String,phoneNumber:String,date_of_birth:String): Boolean
    {
        /*var isNewUser = true;
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener(OnCompleteListener<SignInMethodQueryResult?> { task ->
                isNewUser = task.result.signInMethods!!.isEmpty()
            })*/
        var valid = true;
        var error_message = "";
        if(!Password_Validation(password))
        {
            error_message = "Password should be at least 8 characters!\n" +
            "Contain at least one lowercase and one uppercase letter.\n" +
            "Contain at least one special character and one or more numbers."
            valid = false;
        }
        else if(password != confirmPassword)
        {
            error_message = "passwords doesn't match!";
            valid = false
        }
        else if(phoneNumber.length != 10 || !phoneNumber.startsWith("05"))
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
            Toast.makeText(this@signUpActivity, error_message, Toast.LENGTH_LONG).show()
        return valid
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
    //date pickup
    private fun getTodaysDate(): String? {
        val cal: Calendar = Calendar.getInstance()
        val year: Int = cal.get(Calendar.YEAR)
        var month: Int = cal.get(Calendar.MONTH)
        month = month + 1
        val day: Int = cal.get(Calendar.DAY_OF_MONTH)
        return makeDateString(day, month, year)
    }

    private fun initDatePicker() {
        val dateSetListener =
            OnDateSetListener { datePicker, year, month, day ->
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

    fun openDatePicker(view: View?) {
        datePickerDialog?.show()
    }
}