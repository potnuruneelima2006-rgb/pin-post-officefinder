package com.example.pinpostofficefinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pinpostofficefinder.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private lateinit var db: AppDatabase

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    private fun googleSignIn() {
        val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
        val webClientId = if (resId != 0) {
            getString(resId)
        } else {
            "1051116522628.apps.googleusercontent.com"
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Signed in as: ${user?.email}", Toast.LENGTH_SHORT).show()
                    binding.tvStatus.text = "Signed in as: ${user?.email}"
                } else {
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    binding.tvStatus.text = "Authentication Failed"
                }
            }
    }

    private val locationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                detectPin()
            } else {
                Toast.makeText(
                    this,
                    "Location permission required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        db = AppDatabase.getDatabase(this)

        binding.recyclerView.layoutManager =
            LinearLayoutManager(this)

        binding.btnGoogleSignIn.setOnClickListener {
            googleSignIn()
        }

        binding.btnDetectPin.setOnClickListener {

            checkLocationPermission()
        }

        binding.btnPinSearch.setOnClickListener {

            val pin = binding.etSearch.text.toString().trim()

            if (pin.length == 6 && pin.all { it.isDigit() }) {

                searchByPin(pin)

            } else {

                Toast.makeText(
                    this,
                    "Enter a valid 6-digit PIN",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnNameSearch.setOnClickListener {

            val name = binding.etSearch.text.toString().trim()

            if (name.isNotEmpty()) {

                searchByName(name)

            } else {

                Toast.makeText(
                    this,
                    "Enter post office name",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ---------------- LOCATION ----------------

    private fun checkLocationPermission() {

        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        if (
            fine == PackageManager.PERMISSION_GRANTED ||
            coarse == PackageManager.PERMISSION_GRANTED
        ) {

            detectPin()

        } else {

            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun detectPin() {

        binding.tvStatus.text = "Getting your location..."

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            null
        ).addOnSuccessListener { location ->

            if (location != null) {

                getPinFromLocation(
                    location.latitude,
                    location.longitude
                )

            } else {

                binding.tvStatus.text =
                    "Unable to get location"
            }
        }
    }

    private fun getPinFromLocation(
        latitude: Double,
        longitude: Double
    ) {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val geocoder =
                    Geocoder(this@MainActivity, Locale.getDefault())

                val addresses =
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1
                    )

                val pin =
                    addresses?.firstOrNull()?.postalCode

                withContext(Dispatchers.Main) {

                    if (pin != null) {

                        binding.etSearch.setText(pin)

                        searchByPin(pin)

                    } else {

                        binding.tvStatus.text =
                            "PIN code not found"
                    }
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    binding.tvStatus.text =
                        "Error getting PIN"
                }
            }
        }
    }

    // ---------------- PIN SEARCH ----------------

    private fun searchByPin(pin: String) {

        binding.tvStatus.text = "Searching..."

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val url =
                    URL("https://api.postalpincode.in/pincode/$pin")

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                connection.disconnect()

                val offices =
                    parseResponse(response)

                withContext(Dispatchers.Main) {

                    showResults(offices)
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    binding.tvStatus.text =
                        "Error: ${e.message}"
                }
            }
        }
    }

    // ---------------- NAME SEARCH ----------------

    private fun searchByName(name: String) {

        binding.tvStatus.text = "Searching..."

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val url =
                    URL(
                        "https://api.postalpincode.in/postoffice/" +
                                name
                    )

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                connection.disconnect()

                val offices =
                    parseResponse(response)

                withContext(Dispatchers.Main) {

                    showResults(offices)
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    binding.tvStatus.text =
                        "Error: ${e.message}"
                }
            }
        }
    }

    // ---------------- JSON ----------------

    private fun parseResponse(
        response: String
    ): List<PostOffice> {

        val result = mutableListOf<PostOffice>()

        val root = try {
            JSONObject(response)
        } catch (e: Exception) {
            val jsonArray = org.json.JSONArray(response)
            if (jsonArray.length() > 0) jsonArray.getJSONObject(0) else return result
        }

        val status =
            root.optString("Status")

        if (status != "Success") {
            return result
        }

        val postOfficeArray =
            root.optJSONArray("PostOffice") ?: return result

        for (i in 0 until postOfficeArray.length()) {

            val obj =
                postOfficeArray.getJSONObject(i)

            result.add(
                PostOffice(
                    Name = obj.optString("Name"),
                    BranchType = obj.optString("BranchType"),
                    DeliveryStatus = obj.optString("DeliveryStatus"),
                    District = obj.optString("District"),
                    State = obj.optString("State"),
                    Pincode = obj.optString("Pincode")
                )
            )
        }

        return result
    }

    // ---------------- SHOW RESULTS ----------------

    private fun showResults(
        offices: List<PostOffice>
    ) {

        if (offices.isEmpty()) {

            binding.tvStatus.text =
                "Error: No records found"

            return
        }

        binding.tvStatus.text =
            "${offices.size} post office(s) found"

        binding.recyclerView.adapter =
            PostOfficeAdapter(
                offices
            ) { office ->

                savePin(office)
            }
    }

    // ---------------- ROOM ----------------

    private fun savePin(office: PostOffice) {

        CoroutineScope(Dispatchers.IO).launch {

            db.pinDao().savePin(
                SavedPin(
                    pin = office.Pincode ?: "",
                    name = office.Name ?: ""
                )
            )

            withContext(Dispatchers.Main) {

                Toast.makeText(
                    this@MainActivity,
                    "PIN saved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
