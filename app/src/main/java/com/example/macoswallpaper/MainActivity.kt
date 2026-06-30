package com.example.macoswallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.macoswallpaper.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val selectVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Take persistable URI permission so the WallpaperService can access it later
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                
                // Save URI to SharedPreferences
                val sharedPrefs = getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putString("video_uri", uri.toString()).apply()

                binding.tvSelectedVideo.text = "Video Selected"
                binding.btnSetWallpaper.isEnabled = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to get permission for video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPrefs = getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
        val savedUri = sharedPrefs.getString("video_uri", null)
        
        if (savedUri != null) {
            binding.tvSelectedVideo.text = "Video Selected"
            binding.btnSetWallpaper.isEnabled = true
        }

        binding.btnSelectVideo.setOnClickListener {
            selectVideoLauncher.launch("video/*")
        }

        binding.btnSetWallpaper.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, VideoWallpaperService::class.java)
                )
            }
            startActivity(intent)
        }
    }
}
