package com.example.humiditeettemperature

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.humiditeettemperature.databinding.ActivityDatabaseViewBinding


class DatabaseView : AppCompatActivity() {

    private lateinit var binding: ActivityDatabaseViewBinding

    private val db = Database(this, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_view)
        binding = ActivityDatabaseViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            downD.setOnClickListener(onClickListener())
            upD.setOnClickListener(onClickListener())
            refresh.setOnClickListener(onClickListener())
            backButton.setOnClickListener(onClickListener())
            clearD.setOnClickListener(onClickListener())
        }

        loadFromDatabase ()

    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        onBackPressedDispatcher.onBackPressed()
    }


    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener {
            when (it.id) {

                R.id.backButton -> {
                    val intent = Intent(this@DatabaseView, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                }

                R.id.downD -> {
                    binding.scrollViewDatabase.fullScroll(View.FOCUS_DOWN)
                }

                R.id.upD -> {
                    binding.scrollViewDatabase.fullScroll(View.FOCUS_UP)
                }

                R.id.refresh -> {
                    binding.dataFromDatabase.text = ""
                    loadFromDatabase ()
                }

                R.id.clearD -> {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Очистка базы данных")
                    builder.setMessage("Вы действительно хотите очистить всю базу данных?")
                    builder.setPositiveButton("Очистить") { _, _ ->
                        db.eraseDatabase()
                    }

                    builder.setNegativeButton("Отмена", null)
                    val dialog = builder.create()
                    dialog.show()

                }
            }
        }
    }


    @SuppressLint("Range")
    private fun loadFromDatabase () {

        val cursor = db.getData()

        if((cursor != null) && cursor.moveToFirst()){
            cursor.moveToFirst()
            binding.dataFromDatabase.append("\n\t\t" + cursor.getString(cursor.getColumnIndex(Database.POS_COL)) + ") Температура: " + cursor.getString(cursor.getColumnIndex(Database.TEMP_COL)) + ". Влажность: " +
                    cursor.getString(cursor.getColumnIndex(Database.HUMID_COL)) + ". Дата: " + cursor.getString(cursor.getColumnIndex(Database.DATE_COL)) + "\n")
            while(cursor.moveToNext()) {
                binding.dataFromDatabase.append("\n\t\t" + cursor.getString(cursor.getColumnIndex(Database.POS_COL)) + ") Температура: " + cursor.getString(cursor.getColumnIndex(Database.TEMP_COL)) + ". Влажность: " +
                        cursor.getString(cursor.getColumnIndex(Database.HUMID_COL)) + ". Дата: " + cursor.getString(cursor.getColumnIndex(Database.DATE_COL)) + "\n")
            }
            cursor.close();
        }

        else {
            binding.dataFromDatabase.text = "База данных пустая"
        }
    }
}

