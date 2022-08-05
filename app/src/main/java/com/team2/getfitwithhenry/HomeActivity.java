package com.team2.getfitwithhenry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.navigation.NavigationBarView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.team2.getfitwithhenry.model.Goal;
import com.team2.getfitwithhenry.model.HealthRecord;
import com.team2.getfitwithhenry.model.Role;
import com.team2.getfitwithhenry.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HomeActivity extends AppCompatActivity {
    Button searchBtn;
    Button scanBtn;
    Button mlogoutBtn;

    NavigationBarView bottomNavView;
    private User tempUser;
    private final OkHttpClient client = new OkHttpClient();
    List<HealthRecord> healthRecordList;
    private final double dailyCal = 3000;
    private final double dailyWaterIntake = 2700;
    TextView caloriesText;
    TextView waterText;
    GraphView graph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setBottomNavBar();

        caloriesText = findViewById(R.id.caloriesText);
        waterText = findViewById(R.id.waterText);
        graph = (GraphView) findViewById(R.id.GraphView);
        //temp user just to add in the logic
        //int id, String name, String username, String password, Role role, Goal goal
        tempUser = new User(1, "Henry", "Henry@gmail.com", "password", Role.NORMAL, Goal.MUSCLE);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String currDate = LocalDate.now().format(formatter);
        getHealthRecordsbyUserNameAndDateFromServer(tempUser, currDate);
        getHealthRecordsbyUserNameFromServer(tempUser);

        mlogoutBtn = findViewById(R.id.logoutBtn);
        SharedPreferences pref = getSharedPreferences("UserDetailsObj", MODE_PRIVATE);

        mlogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = pref.edit();
                editor.clear();
                editor.commit();

                Toast.makeText(getApplicationContext(), "You have logged out successfully", Toast.LENGTH_LONG).show();
                startLoginActivity();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences pref = getSharedPreferences("UserDetailsObj", MODE_PRIVATE);
        if (!pref.contains("userDetails"))
            startLoginActivity();
    }

    private void showGraphView(List<HealthRecord> healthRecordList) {
        //plot data(curve) on X and Y
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();
        ArrayList<String> xAxisLables = new ArrayList<>();
        for (int i = 0; i < healthRecordList.size(); i++) {
            series.appendData(new DataPoint(i, healthRecordList.get(i).getUserWeight()), true, healthRecordList.size());
            xAxisLables.add(healthRecordList.get(i).getDate().toString());
        }

        //set the appearance of the curve
        series.setColor(Color.rgb(0, 80, 100)); //set the color of the curve
        series.setTitle("Weight Curve"); // set the curve name for the legend
        series.setDrawDataPoints(true); // draw points
        series.setDataPointsRadius(5); // the radius of the data point
        series.setThickness(2); //line thickness

        graph.addSeries(series);


        // graph.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xAxisLables));
        // XAxis xAxis = graph.getXAxis();

        //set title for graph
        graph.setTitle("Weight Tracking");
        graph.setTitleTextSize(50);
        graph.setTitleColor(Color.BLUE);
        //legend
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        //Axis signatures
        GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
        gridLabel.setHorizontalAxisTitle("Date");
        gridLabel.setVerticalAxisTitle("Weight");
    }

    private void getHealthRecordsbyUserNameFromServer(User user) {
        JSONObject postData = new JSONObject();
        try {
            postData.put("username", user.getUsername());
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(postData.toString(), JSON);


            //need to use your own pc's ip address here, cannot use local host.
            Request request = new Request.Builder()
                    .url("http://192.168.10.127:8080/home/gethealthrecordsbyUserName")
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

                        String msg = String.valueOf(responseBody);
                        //to do: convert responseBody into list of HealthRecords
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.registerModule(new JavaTimeModule());
                        List<HealthRecord> hrList = Arrays.asList(objectMapper.readValue(responseBody.string(), HealthRecord[].class));
                        if (hrList.size() != 0) {
                            showGraphView(hrList);
                        } else {
                            Toast.makeText(HomeActivity.this, "No weight tracking for this user", Toast.LENGTH_SHORT).show();
                        }


//                        Log.i("data", responseBody.string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getHealthRecordsbyUserNameAndDateFromServer(User user, String date) {
        JSONObject postData = new JSONObject();
        try {
            postData.put("username", user.getUsername());
            postData.put("date", date);
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(postData.toString(), JSON);


            //need to use your own pc's ip address here, cannot use local host.
            Request request = new Request.Builder()
                    .url("http://172.29.208.1:8080/home/gethealthrecords")
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

                        String msg = String.valueOf(responseBody);
                        //to do: convert responseBody into list of HealthRecords
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.registerModule(new JavaTimeModule());
                        List<HealthRecord> healthRecordList = Arrays.asList(objectMapper.readValue(responseBody.string(), HealthRecord[].class));
                        if (healthRecordList.size() != 0) {
                            Double calLeft = getClaoriesLeft(healthRecordList);
                            Double waterLeft = getWaterLeft(healthRecordList);
                            caloriesText.setText(("Calories Left: " + calLeft).toString());
                            waterText.setText(("Water intake Left: " + waterLeft).toString());
                        } else {
                            caloriesText.setText(("Calories Left: " + dailyCal).toString());
                            waterText.setText(("Water intake Left: " + dailyWaterIntake).toString());
                        }


//                        Log.i("data", responseBody.string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private double getClaoriesLeft(List<HealthRecord> healthRecordList) {
        Double calTaken = healthRecordList.get(0).getCalIntake();
        return dailyCal - calTaken;
    }

    private double getWaterLeft(List<HealthRecord> healthRecordList) {
        Double waterTaken = healthRecordList.get(0).getWaterIntake();
        return dailyWaterIntake - waterTaken;
    }

    public void setBottomNavBar() {
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
                        break;  //or should this be finish?

                    case (R.id.nav_search):
                        intent = new Intent(getApplicationContext(), SearchFoodActivity.class);
                        startActivity(intent);
                        break;

                    case (R.id.nav_log):
                        intent = new Intent(getApplicationContext(), LoggerActivity.class);
                        startActivity(intent);
                        break;

                    case (R.id.nav_recipe):
                        intent = new Intent(getApplicationContext(), RecipeActivity.class);
                        startActivity(intent);
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

    private void startLogoutActivity() {
        Intent intent = new Intent(this, LogoutActivity.class);
        startActivity(intent);
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }

}