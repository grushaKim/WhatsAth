package com.example.mywhatsath.activities

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.core.text.toSpannable
import com.bumptech.glide.Glide
import com.example.mywhatsath.R
import com.example.mywhatsath.databinding.ActivityProfileEditBinding
import com.example.mywhatsath.models.ModelSport
import com.example.mywhatsath.utils.LinearGradientSpan
import com.example.mywhatsath.utils.MyApplication
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import es.dmoral.toasty.Toasty

class ProfileEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var fbAuth: FirebaseAuth
    private lateinit var fbDbRef: FirebaseDatabase
    private lateinit var pDialog: ProgressDialog

    //image uri
    private var imageUri: Uri? = null

    // sports select dialog
    private lateinit var sportsList: ArrayList<ModelSport>
    private var selectedSportId = ""
    private var selectedSport = ""

    //info to be updated
    private var updatedName = ""
    private var updatedLevel = ""
    private var updatedAboutMe = ""

    private val maxLength = 100

    companion object{
        const val TAG = "PROFILE_EDIT_TAG"
        const val REQ_PERMISSIONS_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init main toolbar
        setSupportActionBar(binding.mainToolbar)
        binding.mainToolbar.setNavigationOnClickListener{
            onBackPressed()
        }

        // set linear gradient for tv
        getGradientTextView()

        // init auth&db
        fbAuth = FirebaseAuth.getInstance()
        fbDbRef = FirebaseDatabase.getInstance()

        // set progressDialog
        pDialog = ProgressDialog(this)
        pDialog.setTitle("Please wait")
        pDialog.setCanceledOnTouchOutside(false)

        //load the profile
        loadUserProfile()

        // load sport categories
        loadSports()

        // upload profile image
        binding.profileIv.setOnClickListener {
            pickImageFromGallery()
        }

        binding.sportTv.setOnClickListener {
            sportPickDialog()
        }

        // level radio group click
        binding.levelRg.setOnCheckedChangeListener { radioGroup, checkedId ->
            when(checkedId){
                R.id.amateurRb -> updatedLevel = resources.getStringArray(R.array.levels)[0].toString()
                R.id.semiProRb -> updatedLevel = resources.getStringArray(R.array.levels)[1].toString()
                R.id.proRb -> updatedLevel = resources.getStringArray(R.array.levels)[2].toString()
            }
        }

        // save profile click button
        binding.saveProfileBtn.setOnClickListener {
            if(imageUri==null){
                validateData("")
            }else{
                uploadImage()
            }
        }

        // cancel
        binding.cancelBtn.setOnClickListener{
            onBackPressed()
        }

        // set text limit of aboutMe section
        binding.aboutMeEt.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
    }

    private fun getGradientTextView() {
        val sloganText = binding.profileTv.text.toString()
        val startColor = ContextCompat.getColor(this, R.color.start)
        val endColor = ContextCompat.getColor(this, R.color.end)
        val spannable = sloganText.toSpannable()
        spannable[0..sloganText.length] = LinearGradientSpan(sloganText, sloganText, startColor, endColor)
        binding.profileTv.text = spannable
    }

    // inflate menu to toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // set menu functions
    override fun onOptionsItemSelected(item: MenuItem) = when(item?.itemId) {
        R.id.homeBtn -> {
            startActivity(Intent(this@ProfileEditActivity, DashboardUserActivity::class.java))
            true
        }
        R.id.logoutBtn -> {
            fbAuth.signOut()
            checkUser()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun loadSports() {
        sportsList = ArrayList()
        // get data from db
        val ref = fbDbRef.getReference("Sports")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                sportsList.clear()
                for(ds in snapshot.children){
                    val model = ds.getValue(ModelSport::class.java)
                    sportsList.add(model!!)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun loadUserProfile() {
        val ref = fbDbRef.getReference("Users")
        ref.child(fbAuth.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user profile
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val name = "${snapshot.child("name").value}"
                    val email = "${snapshot.child("email").value}"
                    val gender = "${snapshot.child("gender").value}"
                    val regDate = "${snapshot.child("regDate").value}"
                    val aboutMe = "${snapshot.child("aboutMe").value}"

                    //convert regdate
                    val formattedRegDate = MyApplication.formatRegDate(regDate.toLong())

                    //set data
                    if(profileImage == "" || profileImage.isEmpty()){
                        binding.profileIv.setBackgroundResource(R.drawable.ic_baseline_person_24)
                    }else{
                        try{
                            Glide.with(this@ProfileEditActivity)
                                .load(profileImage)
                                .into(binding.profileIv)
                        } catch(e: Exception){
                            Log.d(TAG, "onDataChange: failed to load profileImage. Error: ${e.message}")
                        }
                    }

                    binding.nameEt.hint = name
                    binding.emailTv.text = email
                    binding.sexTv.text = gender
                    binding.regDateTv.text = formattedRegDate

                    if(aboutMe == "" || aboutMe.isEmpty()){
                        binding.aboutMeEt.hint = "Describe yourself less than $maxLength characters  "
                    }else{
                        binding.aboutMeEt.hint = aboutMe
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun sportPickDialog() {
        Log.d(RegisterActivity.TAG, "sportPickDialog: displaying the selected sport from dialog")
        val sportsArr = arrayOfNulls<String>(sportsList.size)
        for(i in sportsList.indices){
            sportsArr[i] = sportsList[i].sport
        }
        // alertdialog for selection
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select your major sport")
            .setItems(sportsArr) {_, which ->
                selectedSport = sportsList[which].sport
                selectedSportId = sportsList[which].id
                binding.sportTv.text = selectedSport
            }
            .show()
    }

    private fun uploadImage(){

        // image path, name
        val filePathAndName = "ProfileImages/"+fbAuth.uid
        //storage
        val reference = FirebaseStorage.getInstance().getReference(filePathAndName)
        reference.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->

                //get uri of uploaded image
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while(!uriTask.isSuccessful);
                val uploadedImageUrl = "${uriTask.result}"

                validateData(uploadedImageUrl)
                Log.d(TAG, "uploadImage: uploaded your profile image to firebase storage")
            }
            .addOnFailureListener { e ->
                Toasty.error(this,"Failed to upload profile image. Error - ${e.message}", Toast.LENGTH_SHORT, true ).show()
                Log.d(TAG, "uploadImage: failed to upload your profile image to firebase storage. Error: ${e.message}")
            }

    }

    private fun validateData(uploadedImageUrl: String) {

        // variable which are able to be without any change
        val name = binding.nameEt.text.toString().trim()
        val aboutMe = binding.aboutMeEt.text.toString()

        if(name == "" || name == null){
            updatedName = binding.nameEt.hint.toString()
        }else{
            updatedName = name
        }

        if(aboutMe == "" || aboutMe == null){
            updatedAboutMe = binding.aboutMeEt.hint.toString()
        }else{
            updatedAboutMe = aboutMe
        }

        if(binding.levelRg.checkedRadioButtonId == -1){
            Toasty.warning(this, "Please check your level", Toast.LENGTH_SHORT, true).show()
        }else if(selectedSport == "" || selectedSport == null) {
            Toasty.warning(this, "Please select your sport", Toast.LENGTH_SHORT, true).show()
        }else{
            Log.d(TAG, "updateProfile: $uploadedImageUrl, $updatedName, $selectedSport, $selectedSportId, $updatedLevel, $updatedAboutMe")
            updateProfile(uploadedImageUrl, updatedName, selectedSport, selectedSportId, updatedLevel, updatedAboutMe)
        }
    }

    private fun updateProfile(
        uploadedImageUrl: String, 
        updatedName: String,
        selectedSport: String, 
        selectedSportId: String, 
        updatedLevel: String, 
        updatedAboutMe: String){

        // collect all data to be updated
        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["profileImage"] = uploadedImageUrl
        hashMap["name"] = updatedName
        hashMap["sport"] = selectedSport
        hashMap["sportId"] = selectedSportId
        hashMap["level"] = updatedLevel
        hashMap["aboutMe"] = updatedAboutMe

        val ref = fbDbRef.getReference("Users")
        ref.child(fbAuth.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "updateProfile: Successfully updated your profile")
                imageUri = null
                Toasty.success(this, "Updated your profile", Toast.LENGTH_SHORT, true).show()
                startActivity(Intent(this@ProfileEditActivity, ProfileActivity::class.java))
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "updateProfile: Failed to update your profile")
                Toasty.error(this, "Failed to update your profile. Error - ${e.message}", Toast.LENGTH_SHORT, true).show()
            }
    }

    private fun pickImageFromGallery() {
        // check permission
        var writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        var readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

            if(writePermission == PackageManager.PERMISSION_DENIED ||
                readPermission == PackageManager.PERMISSION_DENIED){

                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE), REQ_PERMISSIONS_CODE
                )

            } else{

                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                galleryActivityResultLauncher.launch(intent)

            }
    }

    // to handle gallery intent result
    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult>{ result ->
            if(result.resultCode == RESULT_OK){
                val data = result.data
                imageUri = data!!.data
                //set imageview
                binding.profileIv.setImageURI(imageUri)
            }else{
                Toasty.normal(this, "Cancelled to upload", Toast.LENGTH_SHORT).show()
            }
        }
    )

    private fun checkUser() {
        // get current user
        val fbUser = fbAuth.currentUser
        if (fbUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}