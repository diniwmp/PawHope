package lk.zenova.pawhope.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import lk.zenova.pawhope.R
import lk.zenova.pawhope.fragment.HomeFragment;
import lk.zenova.pawhope.fragment.PlaceholderFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNavigationView.setOnItemSelectedListener { item ->
            // when = switch. item.itemId = item.getItemId()
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_map -> loadFragment(PlaceholderFragment.newInstance(getString(R.string.nav_map)))
                R.id.nav_add -> loadFragment(PlaceholderFragment.newInstance(getString(R.string.nav_add)))
                R.id.nav_profile -> loadFragment(PlaceholderFragment.newInstance(getString(R.string.nav_profile)))
            }
            true
        }

        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.nav_home
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}