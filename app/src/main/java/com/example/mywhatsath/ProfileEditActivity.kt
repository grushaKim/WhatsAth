package com.example.mywhatsath

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.mywhatsath.databinding.ActivityProfileEditBinding
import com.example.mywhatsath.utils.MyApplication
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class ProfileEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var fbAuth: FirebaseAuth
    private lateinit var fbDbRef: FirebaseDatabase
    private lateinit var pDialog: ProgressDialog

    //image uri
    private var imageUri: Uri? = null

    //info to be updated
    private var updatedName = ""
    private var updatedSport = ""
    private var updatedLevel = ""
    private var updatedAboutMe = ""

    private val maxLength = 100

    companion object{
        const val TAG = "PROFILE_EDIT_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init auth&db
        fbAuth = FirebaseAuth.getInstance()
        fbDbRef = FirebaseDatabase.getInstance()

        // set progressDialog
        pDialog = ProgressDialog(this)
        pDialog.setTitle("Please wait")
        pDialog.setCanceledOnTouchOutside(false)

        //load the profile
        loadUserProfile()

        //set spinner for sports dropdown menu
        val sports = resources.getStringArray(R.array.sports)
        binding.sportsSp.adapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, sports)

        binding.sportsSp.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updatedSport = sports[position]
                Log.d(RegisterActivity.TAG, "onItemSelected: selected sport is $updatedSport")
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        //set spinner for levels dropdown menu
        val levels = resources.getStringArray(R.array.levels)
        binding.levelsSp.adapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, levels)

        binding.levelsSp.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updatedLevel = sports[position]
                Log.d(RegisterActivity.TAG, "onItemSelected: selected level is $updatedLevel")
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        // upload profile image
        binding.profileIv.setOnClickListener {
            pickImageFromGallery()
        }

        // save profile click button
        binding.saveProfileBtn.setOnClickListener {
            if(imageUri==null){
                updateProfile("")
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

    private fun loadUserProfile() {
        val ref = fbDbRef.getReference("Users")
        ref.child(fbAuth.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user profile
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val name = "${snapshot.child("name").value}"
                    val email = "${snapshot.child("email").value}"
                    val sex = "${snapshot.child("sex").value}"
                    val regDate = "${snapshot.child("regDate").value}"
                    val sport = "${snapshot.child("sport").value}"
                    val level = "${snapshot.child("level").value}"
                    val aboutMe = "${snapshot.child("aboutMe").value}"

                    //convert regdate
                    val formattedRegDate = MyApplication.formatRegDate(regDate.toLong())

                    //set data
                    if(profileImage == "" || profileImage.isEmpty()){
                        binding.profileIv.setBackgroundResource(R.drawable.ic_baseline_person_24)
                    }else{
//                        TODO()
                    }
                    binding.nameEt.hint = name
                    binding.emailTv.text = email
                    if(sex == R.string.male.toString()){
                        binding.sexIv.setImageResource(R.drawable.ic_man)
                    }else{
                        binding.sexIv.setImageResource(R.drawable.ic_woman)
                    }
                    binding.regDateTv.text = formattedRegDate
                    binding.sportsSp.setSelection(resources.getStringArray(R.array.sports).indexOf(sport))
                    binding.levelsSp.setSelection(resources.getStringArray(R.array.levels).indexOf(level))
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

    private fun uploadImage(){
        pDialog.setMessage("Uploading profile image")
        pDialog.show()

        // image path, name
        val filePathAndName = "ProfileImages/"+fbAuth.uid
        //storage
        val reference = FirebaseStorage.getInstance().getReference(filePathAndName)
        reference.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                pDialog.dismiss()

                //get uri of uploaded image
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while(!uriTask.isSuccessful);
                val uploadedImageUrl = "${uriTask.result}"

                updateProfile(uploadedImageUrl)
                Log.d(TAG, "uploadImage: uploaded your profile image to firebase storage")
            }
            .addOnFailureListener { e ->
                pDialog.dismiss()
                Toast.makeText(this, "Failed to upload profile image. Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "uploadImage: failed to upload your profile image to firebase storage. Error: ${e.message}")
            }

    }

    private fun updateProfile(uploadedImageUrl: String) {
        pDialog.setMessage("Updating profile...")

        // add msg to DB
        val hashMap: HashMap<String, Any?> = HashMap()

        if(imageUri != null){
            hashMap["profileImage"] = uploadedImageUrl
        }

        fbDbRef.getReference("Users").child(fbAuth.uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Successfully uploaded your profile", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "updateProfile: Successfully updated your profile")
                imageUri = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload your profile", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "updateProfile: Failed to update your profile")
            }

        /*fbDbRef.getReference("Chats").child(senderRoom!!)
            .child("messages").push()
            .setValue(hashMap)
            .addOnSuccessListener {
                pDialog.dismiss()

                fbDbRef.getReference("Chats").child(receiverRoom!!)
                    .child("messages").push()
                    .setValue(hashMap)

                binding.uploadImgBtn.setImageResource(R.drawable.ic_baseline_add_a_photo_gray24)
                binding.msgBoxEt.hint = "Send a message..."

                // nullify the value of imageUri for the next message
                imageUri = null
            }
            .addOnFailureListener { e ->
                pDialog.dismiss()
                Toast.makeText(this, "Failed to send message. Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        binding.msgBoxEt.setText("")*/

    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "images/*"
        galleryActivityResultLauncher.launch(intent)
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
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )
}