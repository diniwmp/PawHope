package lk.zenova.pawhope.activity;


import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import lk.zenova.pawhope.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import lk.zenova.pawhope.fragment.PlaceholderFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(PlaceholderFragment.newInstance(getString(R.string.nav_home)));
            } else if (id == R.id.nav_map) {
                loadFragment(PlaceholderFragment.newInstance(getString(R.string.nav_map)));
            } else if (id == R.id.nav_add) {
                loadFragment(PlaceholderFragment.newInstance(getString(R.string.nav_add)));
            } else if (id == R.id.nav_profile) {
                loadFragment(PlaceholderFragment.newInstance(getString(R.string.nav_profile)));
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}


