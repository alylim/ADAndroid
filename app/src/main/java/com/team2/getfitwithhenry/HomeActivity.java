package com.team2.getfitwithhenry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.tabs.TabLayout;
import com.github.mikephil.charting.data.Entry;
import com.google.gson.Gson;
import com.team2.getfitwithhenry.adapter.DashboardProgressAdapter;
import com.team2.getfitwithhenry.adapter.GraphFilterAdapter;
import com.team2.getfitwithhenry.helper.ProgressArcDrawable;
import com.team2.getfitwithhenry.model.Constants;
import com.team2.getfitwithhenry.model.DietRecord;
import com.team2.getfitwithhenry.model.Goal;
import com.team2.getfitwithhenry.model.HealthRecord;
import com.team2.getfitwithhenry.model.Role;
import com.team2.getfitwithhenry.model.User;
import com.team2.getfitwithhenry.model.UserCombinedData;
import com.team2.getfitwithhenry.model.WeekMonthData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HomeActivity extends AppCompatActivity implements AddWaterFragment.IAddWater {


    private Toolbar mToolbar;
    private NavigationBarView bottomNavView;
    private final OkHttpClient client = new OkHttpClient();
    private List<HealthRecord> healthRecordList;
    private List<DietRecord> dietRecordList;
    private List<WeekMonthData> weekList;
    private List<WeekMonthData> monthList;
    private User user;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapterItem;
    private String[] itemLists = {"Weight", "Calories", "Water Intake"};
    private String dropdownItem = null;
    private LineChart mpLineChart;
    private ViewPager2 vp2;
    private ViewPager2 graphVP;
    private static final int TIME_INTERVAL = 2000;
    private long mBackPressed;
    public List<String> getXAxisData;
    private Map<String, String> getData = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setTopNavBar();
        setBottomNavBar();

        SharedPreferences pref = getSharedPreferences("UserDetailsObj", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = pref.getString("userDetails", "");
        user = gson.fromJson(json, User.class);
        dropdownItem = "Weight";
       // showDropdownList();

        getData.put("username", user.getUsername());
        getData.put("date", String.valueOf(LocalDate.now()));
        getFromServer(getData, "/user/getuserrecords", "daily");


    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences pref = getSharedPreferences("UserDetailsObj", MODE_PRIVATE);
        if (!pref.contains("userDetails"))
            startLoginActivity();
        else if (pref.contains("userDetails") && (user.getCalorieintake_limit_inkcal() == null || user.getCalorieintake_limit_inkcal() <= 0))
            startQuestionnaireActivity();
    }

    public void setGraphTabView(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FragmentManager fm = getSupportFragmentManager();
                GraphFilterAdapter myGraphAdapter = new GraphFilterAdapter(fm, getLifecycle(), user, healthRecordList,weekList,monthList);
                graphVP = findViewById(R.id.graph_pager);
                graphVP.setAdapter(myGraphAdapter);
                TabLayout tabLayout = findViewById(R.id.graph_tab_layout);
                if (tabLayout != null) {
                    tabLayout.removeAllTabs();
                }
                tabLayout.addTab(tabLayout.newTab().setText("Weight"));
                tabLayout.addTab(tabLayout.newTab().setText("Calories"));
                tabLayout.addTab(tabLayout.newTab().setText("Water Intake"));

                tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        graphVP.setCurrentItem(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                    }
                });
                //allows swipe gesture for tabs
                graphVP.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        tabLayout.selectTab(tabLayout.getTabAt(position));
                    }
                });
            }
        });
    }

    public void setUpTabview() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FragmentManager fm = getSupportFragmentManager();
                DashboardProgressAdapter myAdapter = new DashboardProgressAdapter(fm, getLifecycle(), user, healthRecordList.get(0), dietRecordList);
                vp2 = findViewById(R.id.pager);
                vp2.setAdapter(myAdapter);
                TabLayout tabLayout = findViewById(R.id.tab_layout);
                if (tabLayout != null) {
                    tabLayout.removeAllTabs();
                }
                tabLayout.addTab(tabLayout.newTab().setText("Overall"));
                tabLayout.addTab(tabLayout.newTab().setText("Macros"));

                tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        vp2.setCurrentItem(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                    }
                });
                //allows swipe gesture for tabs
                vp2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        tabLayout.selectTab(tabLayout.getTabAt(position));
                    }
                });
            }
        });
    }


    private void getFromServer(Map<String, ?> dataMap, String url, String graphFilter) {
        JSONObject postData = new JSONObject();
        try {
            for (Map.Entry<String, ?> entry : dataMap.entrySet()) {
                postData.put(entry.getKey(), entry.getValue());
            }
            postData.put("username", user.getUsername());
            postData.put("graphFilter",graphFilter);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(postData.toString(), JSON);


            //need to use your own pc's ip address here, cannot use local host.
            Request request = new Request.Builder()
                    .url(Constants.javaURL + url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.out.println("Error");
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        ResponseBody responseBody = response.body();
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        }

                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.registerModule(new JavaTimeModule());
                        UserCombinedData ucd = objectMapper.readValue(responseBody.string(), UserCombinedData.class);
                        weekList = ucd.getWeekList();
                        monthList = ucd.getMonthList();
                        healthRecordList = ucd.getMyHrList();
                        dietRecordList = ucd.getMyDietRecord();

                        setGraphTabView();
                        setUpTabview();
                        response.body().close();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendToServer(Map<String, ?> dataMap, String url) {
        JSONObject postData = new JSONObject();
        try {
            for (Map.Entry<String, ?> entry : dataMap.entrySet()) {
                postData.put(entry.getKey(), entry.getValue());
            }
            postData.put("username", user.getUsername());

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(postData.toString(), JSON);


            //need to use your own pc's ip address here, cannot use local host.
            Request request = new Request.Builder()
                    .url(Constants.javaURL + url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.out.println("Error");
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        ResponseBody responseBody = response.body();
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        }
                        response.body().close();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSelectedData(Double selectedMils) {

        Map<String, Object> waterData = new HashMap<>();
        waterData.put("hrID", healthRecordList.get(0).getId());
        waterData.put("addMils", selectedMils);
        sendToServer(waterData, "/user/addwater");

        Double newWaterVal = healthRecordList.get(0).getWaterIntake() + selectedMils;
        healthRecordList.get(0).setWaterIntake(newWaterVal < 0 ? 0 : newWaterVal);
        setUpTabview();
    }

        public void setTopNavBar () {
            mToolbar = findViewById(R.id.top_navbar);
            setSupportActionBar(mToolbar);
        }

        @Override
        public boolean onCreateOptionsMenu (Menu menu){
            super.onCreateOptionsMenu(menu);
            getMenuInflater().inflate(R.menu.top_nav_bar, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected (@NonNull MenuItem item){
            switch (item.getItemId()) {
                case R.id.editProfile:
                    startProfileActivity();
                    return true;
                case R.id.logout:
                    logout();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }


        public void setBottomNavBar () {
            bottomNavView = findViewById(R.id.bottom_navigation);
            bottomNavView.setSelectedItemId(R.id.nav_home);
            bottomNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Intent intent;
                    int id = item.getItemId();
                    switch (id) {

                        case (R.id.nav_scanner):
                            intent = new Intent(getApplicationContext(), CameraActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                            break;  //or should this be finish?

                        case (R.id.nav_search):
                            intent = new Intent(getApplicationContext(), SearchFoodActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right);
                            break;

                        case (R.id.nav_log):
                            intent = new Intent(getApplicationContext(), LoggerActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right);
                            break;

                        case (R.id.nav_recipe):
                            intent = new Intent(getApplicationContext(), RecipeActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                            break;

//                    case(R.id.nav_home):
//                        intent = new Intent(getApplicationContext(), HomeActivity.class);
//                        startActivity(intent);
//                        break;
                    }

                    return false;
            }
        });
    }

        private void logout() {
            SharedPreferences pref = getSharedPreferences("UserDetailsObj", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.clear();
            editor.commit();

            Toast.makeText(getApplicationContext(), "You have logged out successfully", Toast.LENGTH_LONG).show();
            startLoginActivity();
        }

        private void startQuestionnaireActivity () {
            Intent intent = new Intent(this, QuestionnaireActivity.class);
            startActivity(intent);
        }

        private void startLoginActivity () {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }

        private void startProfileActivity () {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        }

        @Override
        public void onBackPressed() {
            if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
                finishAffinity();
                System.exit(0);
            } else {
                Toast.makeText(getBaseContext(), "Press BACK again to exit", Toast.LENGTH_SHORT).show();
            }
            mBackPressed = System.currentTimeMillis();
        }
}