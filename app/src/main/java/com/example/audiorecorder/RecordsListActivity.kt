package com.example.audiorecorder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Adapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.audiorecorder.databinding.ActivityRecordsListBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RecordsListActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var binding: ActivityRecordsListBinding

    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var mAdapter: RVAdapter
    private lateinit var db: ARDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        records = ArrayList()

        db = ARDatabase.getDB(this)



        mAdapter = RVAdapter(records, this)

        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
        }

        fetchData()
    }


    private fun fetchData() {
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().getAll()
            records.addAll(queryResult)

            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onItemClickListener(position: Int) {
        Toast.makeText(this, "Click", Toast.LENGTH_SHORT).show()
    }

    override fun onItemLongClickListener(position: Int) {
        Toast.makeText(this, "Long Click", Toast.LENGTH_SHORT).show()
    }

}