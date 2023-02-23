package com.example.vufinder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.vufinder.databinding.ActivityUserMenuBinding
import com.example.vufinder.ui.CreateActivity.CreateActivityFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

interface Communicator {
    fun passDataCom(editTextInput: String)
}

class UserMenuActivity : AppCompatActivity(),Communicator {


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityUserMenuBinding

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var buttonMapViewOn : Button

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

        binding = ActivityUserMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FullScreencall()

        //db = Firebase.firestore
        db = FirebaseFirestore.getInstance()
        auth = Firebase.auth
        var isGoogleUser = false
        val googleUser = GoogleSignIn.getLastSignedInAccount(this)
        if(auth.currentUser != null){
            currentUser = auth.currentUser!!
        }
        else if(googleUser != null){
            isGoogleUser = true
        }
        else
        {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        //toolbar
        val toolbar = binding.appBarUserMenu.toolbar

        setSupportActionBar(toolbar)

        /*binding.appBarUserMenu.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }*/
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_user_menu)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_profile, R.id.nav_join_activity, R.id.nav_create_activity
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.user_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_user_menu)
        return navController.navigateUp(appBarConfiguration) //|| super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_sign_out -> {
            // User chose the "sign out" item, show the app settings UI...
            if(Firebase.auth.currentUser != null)
                Firebase.auth.signOut()
            else {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
                val t = GoogleSignIn.getClient(this,gso)
                t.signOut()
            }
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }


    override fun passDataCom(editTextInput: String) {
        val bundle = Bundle()
        bundle.putString("input_txt", editTextInput)
        val transaction = this.supportFragmentManager.beginTransaction()
        val frag2 = CreateActivityFragment()
        frag2.arguments = bundle
        binding.drawerLayout.removeAllViews()
        transaction.replace(binding.drawerLayout.id, frag2)
        transaction.addToBackStack(null)
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        transaction.commit()
    }


}