package com.example.mywhatsath.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mywhatsath.R
import com.example.mywhatsath.adapters.UserAdapter
import com.example.mywhatsath.databinding.ActivityDashboardUserBinding
import com.example.mywhatsath.models.ModelUser
import com.example.mywhatsath.utils.SwipeToDeleteCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DashboardUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardUserBinding

    // firebase auth, db
    private lateinit var fbAuth: FirebaseAuth
    private lateinit var fbDbRef: FirebaseDatabase

    companion object{
        const val TAG = "DASHBOARD_USER_TAG"
    }

    // userlist recyclerview & adapter
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userList: ArrayList<ModelUser>
    private lateinit var userAdapter: UserAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init main toolbar
        setSupportActionBar(binding.mainToolbar)
        binding.mainToolbar.setNavigationOnClickListener{
            onBackPressed()
        }

        // init auth&db
        fbAuth = FirebaseAuth.getInstance()
        fbDbRef = FirebaseDatabase.getInstance()
        checkUser()

        // init arraylist for holder
        userList = ArrayList()
        userAdapter = UserAdapter(this, userList)

        // init recyclerview with swipe to delete a specific chat
        initRecyclerView()

        // load chatlist
        loadChatlist()

        // inflate bottom drawer
        BottomSheetBehavior.from(binding.bottomDrawerSheet).apply {
            peekHeight = 180
            this.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // <MAIN BOTTOM DRAWER menu>
        binding.logoutBtn.setOnClickListener {
            fbAuth.signOut()
            checkUser()
        }

        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this@DashboardUserActivity, ProfileActivity::class.java))
        }

        binding.searchBtn.setOnClickListener {
            startActivity(Intent(this@DashboardUserActivity, SearchActivity::class.java))
        }

        binding.newsBtn.setOnClickListener{
            startActivity(Intent(this@DashboardUserActivity, HeadlineActivity::class.java))
        }

        binding.gymBtn.setOnClickListener{
            showInstructionDialog()
        }

        binding.chatBtn.setOnClickListener {
            this.recreate()
        }
    }

    // instruction about nearby gyms
    private fun showInstructionDialog() {
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val view: View = inflater.inflate(R.layout.customized_alertdialog, null)
        val okBtn: Button = view.findViewById(R.id.okBtn)

        okBtn.setOnClickListener {
            startActivity(Intent(this@DashboardUserActivity, MapsScreenActivity::class.java))
        }

        val builder = AlertDialog.Builder(this)
            .setView(view)

        val instructionDialog = builder.create()
        instructionDialog.show()
    }

    // inflate menu to toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // set menu functions
    override fun onOptionsItemSelected(item: MenuItem) = when(item?.itemId) {
        R.id.homeBtn -> {
            startActivity(Intent(this@DashboardUserActivity, DashboardUserActivity::class.java))
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

    private fun initRecyclerView() {
        userRecyclerView = binding.userRecyclerView.apply{
            adapter = userAdapter
            layoutManager = LinearLayoutManager(this@DashboardUserActivity)
            val swipeDelete = object: SwipeToDeleteCallback(this@DashboardUserActivity){
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val arrPosition = viewHolder.absoluteAdapterPosition
                    val receiverId = userList[arrPosition].uid.toString()

                    userAdapter.deleteItem(viewHolder.adapterPosition, receiverId)
                }
            }

            val touchHelper = ItemTouchHelper(swipeDelete)
            touchHelper.attachToRecyclerView(this)
        }
    }

    private fun loadChatlist() {
        val chatRef = fbDbRef.getReference("Chats")
        val userRef = fbDbRef.getReference("Users")

        chatRef.child(fbAuth.uid!!)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    userList.clear()

                    for(ds in snapshot.children){
                        Log.d(TAG, "onDataChange: successfully get the chat key - ${ds.key}")
                        val chatKey = ds.key!!

                        // check if uid matches
                        userRef.addListenerForSingleValueEvent(object: ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for(pds in snapshot.children){
                                    val chatUser = pds.getValue(ModelUser::class.java)
                                    if(chatUser!!.uid == chatKey)
                                        userList.add(chatUser!!)
                                }
                                userAdapter.notifyDataSetChanged()
                            }
                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun checkUser() {
        // get current user
        val fbUser = fbAuth.currentUser
        if(fbUser == null){
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}