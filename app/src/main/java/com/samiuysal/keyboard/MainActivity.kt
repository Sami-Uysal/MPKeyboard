package com.samiuysal.keyboard

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs =
                newBase.getSharedPreferences("keyboard_prefs", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("selected_language", "tr") ?: "tr"
        val locale = java.util.Locale(lang)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        viewPager.adapter = SettingsPagerAdapter(this)

        if (intent.getBooleanExtra("ACTION_OPEN_THEME", false)) {
            viewPager.post { viewPager.setCurrentItem(1, false) }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text =
                            when (position) {
                                0 -> getString(R.string.tab_home)
                                1 -> getString(R.string.tab_settings)
                                else -> ""
                            }
                }
                .attach()
    }
}
