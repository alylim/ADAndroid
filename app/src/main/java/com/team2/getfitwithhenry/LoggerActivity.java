package com.team2.getfitwithhenry;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.Gson;
import com.team2.getfitwithhenry.model.Constants;
import com.team2.getfitwithhenry.model.DietRecord;
import com.team2.getfitwithhenry.model.Goal;
import com.team2.getfitwithhenry.model.HealthRecord;
import com.team2.getfitwithhenry.model.Ingredient;
import com.team2.getfitwithhenry.model.Role;
import com.team2.getfitwithhenry.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLOutput;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.jvm.internal.TypeReference;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.util.Calendar;
import java.util.Locale;

public class LoggerActivity extends AppCompatActivity implements MealButtonsFragment.IMealButtonsFragment, DefaultLifecycleObserver {


    private User tempUser;
    private final OkHttpClient client = new OkHttpClient();
    private DatePickerDialog datePickerDialog;
    private Button dateButton;
    private Toolbar mToolbar;
    private BottomNavigationView bottomNavView;
    private Button addHeight;
    private Button addWeight;
    private TextView weightText;
    private TextView heightText;
    User user;

    //TODO LIST:
    //refresh page after adding meal
    //get meal list sorted by meal type (combine meal type)
    //UI -> show break down of ingredients on click
    // first change logger ui to show the enum types with the cals
    // then set on click to each row
    // then inflate each row below with listview for each enum


    @Override
    public void itemClicked(String content) {
        DatePicker datePicker = datePickerDialog.getDatePicker();
        String dateSelect = datePicker.getYear() + "-" + String.format("%02d", (datePicker.getMonth() + 1)) + "-" + String.format("%02d", datePicker.getDayOfMonth());

        MealFragment mf = new MealFragment();
        Bundle args = new Bundle();
        args.putString("meal", content.split(" ")[0].toLowerCase());
        args.putString("date", dateSelect);
        args.putString("username", user.getUsername());
        mf.setArguments(args);
        mf.show(getSupportFragmentManager(), "Meal Fragment");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);

        setTopNavBar();

        //set up bottom navbar
        setBottomNavBar();

        SharedPreferences pref = getSharedPreferences("UserDetailsObj", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = pref.getString("userDetails", "");
        user = gson.fromJson(json, User.class);
        user.setDateofbirth(LocalDate.parse(user.getDobStringFormat(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        user.setDateCreated(LocalDate.parse(user.getDateCreatedStringFormat(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        // Set up Calendar
        initDatePicker();
        dateButton = findViewById(R.id.datePickerButton);
        dateButton.setText(setDate(LocalDate.now()));
        //
        addWeight = findViewById(R.id.addWeight);
        weightText = findViewById(R.id.weight);
        addHeight = findViewById(R.id.addHeight);
        heightText = findViewById(R.id.height);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String currDate = LocalDate.now().format(formatter);
        getDietRecordsFromServer(user, currDate);

        //Set up activity result launcher
        ActivityResultLauncher<Intent> addMealActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();

                            // handle results here
                            DatePicker datePicker = datePickerDialog.getDatePicker();
                            String dateSelect = datePicker.getYear() + "-" + String.format("%02d", (datePicker.getMonth() + 1)) + "-" + String.format("%02d", datePicker.getDayOfMonth());
                            getDietRecordsFromServer(user, dateSelect);
                        }
                    }
                });

        //add meal function
        Button addFoodBtn = findViewById(R.id.add_food);
        addFoodBtn.setOnClickListener((view -> {
            DatePicker datePicker = datePickerDialog.getDatePicker();
            String dateSelect = datePicker.getYear() + "-" + String.format("%02d", (datePicker.getMonth() + 1)) + "-" + String.format("%02d", datePicker.getDayOfMonth());
            Intent intent = new Intent(this, AddMealActivity.class);
            intent.putExtra("date", dateSelect);

            addMealActivityLauncher.launch(intent);


        }));

        //set My Records
        getHealthRecordFromServer(user, currDate);

    }


    public void getHealthRecordFromServer(User user, String date) {
        JSONObject postData = new JSONObject();
        try {
            postData.put("username", user.getUsername());
            postData.put("date", date);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(postData.toString(), JSON);

            //need to use your own pc's ip address here, cannot use local host.
            Request request = new Request.Builder()
                    .url(Constants.javaURL + "/user/gethealthrecorddate")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
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
                        HealthRecord myHr = objectMapper.readValue(responseBody.string(), HealthRecord.class);
                        setMyRecords(getApplicationContext(), myHr);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setMyRecords(final Context context, final HealthRecord myHr) {
        if (context != null && myHr != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {

                @Override
                public void run() {
                    TextView totalCals = findViewById(R.id.total_calories);
                    TextView currentCals = findViewById(R.id.current_calories);
                    TextView bmi = findViewById(R.id.BMI);

                    totalCals.setText("Calorie Limit: " + String.valueOf(user.getCalorieintake_limit_inkcal()));
                    currentCals.setText("Calories consumed: " + String.valueOf(Math.round(myHr.getCalIntake())));

                    Double myBmi = 0.0;
                    myBmi = calculateBMI(myHr);
                    setUserBMI(myBmi, bmi);

                    //set weight and height accordingly here
                    if (myHr.getUserWeight() <= 0) {
                        heightText.setText(String.format("Height: %.2f", myHr.getUserHeight()));
                        addHeight.setVisibility(View.GONE);

                        weightText.setText("Weight: N.A");
                        addWeight.setVisibility(View.GONE);

                        if (LocalDate.now().equals(myHr.getDate())) {
                            addHeight.setVisibility(View.VISIBLE);
                            weightText.setText(String.format("Weight: %.2f", myHr.getUserWeight()));
                            addWeight.setVisibility(View.VISIBLE);

                            addHeight.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String[] heightArr = String.valueOf(myHr.getUserHeight()).split("\\.");
                                    int intHeight = Integer.parseInt(heightArr[0]);
                                    showDialogForHeightInput(intHeight, myHr, bmi);
                                }
                            });

                            addWeight.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // add a dialog here for user to add new health record
                                    int intWeight = 50;
                                    // int decimalWeight = Integer.parseInt(arr[1]);
                                    showDialogForWeightInput(intWeight, myHr, bmi);
                                }
                            });
                        }
                    } else {
                        if (LocalDate.now().equals(myHr.getDate())) {
                            heightText.setText(String.format("Height: %.2f", myHr.getUserHeight()));
                            addHeight.setVisibility(View.VISIBLE);
                            weightText.setText(String.format("Weight: %.2f", myHr.getUserWeight()));
                            addWeight.setVisibility(View.VISIBLE);

                            addHeight.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String[] heightArr = String.valueOf(myHr.getUserHeight()).split("\\.");
                                    int intHeight = Integer.parseInt(heightArr[0]);
                                    showDialogForHeightInput(intHeight, myHr, bmi);
                                }
                            });
                            addWeight.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // add a dialog here for user to add new health record
                                    String[] arr = String.valueOf(myHr.getUserWeight()).split("\\.");
                                    int intWeight = Integer.parseInt(arr[0]);
                                    // couldn't do it due to string arr in display
                                    // int decimalWeight = Integer.parseInt(arr[1]);
                                    showDialogForWeightInput(intWeight, myHr, bmi);
                                }
                            });
                        } else {
                            heightText.setText(String.format("Height: %.2f", myHr.getUserHeight()));
                            addHeight.setVisibility(View.GONE);
                            weightText.setText(String.format("Weight: %.2f", myHr.getUserWeight()));
                            addWeight.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }

    public void showDialogForHeightInput(int intHeight, HealthRecord myHr, TextView v) {

        Dialog d = new Dialog(this);
        //d.setTitle("Update Your Height");
        d.setContentView(R.layout.height_input);
        Button save = d.findViewById(R.id.saveHeight);
        Button cancel = d.findViewById(R.id.cancelHeight);
        NumberPicker npInt = d.findViewById(R.id.heightInt);
        NumberPicker npDouble = d.findViewById(R.id.heightDouble);
        npInt.setMaxValue(300); // max value 100
        npInt.setMinValue(3);
        npInt.setValue(intHeight);
        npInt.setWrapSelectorWheel(false);
        String[] doubleArr = new String[]{".0 cm", ".1 cm", ".2 cm", ".3 cm", ".4 cm", ".5 cm", ".6 cm", ".7 cm", ".8 cm", ".9 cm"};
        npDouble.setMaxValue(9);
        npDouble.setMinValue(0);
        npDouble.setDisplayedValues(doubleArr);
        npDouble.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int val1 = npInt.getValue();
                int val2 = npDouble.getValue();
                double weight = myHr.getUserWeight();
                double height = val1 + val2 / 10.0;
                //update the selected value to server then after update the information of weight
                try {
                    updateUserHeight(height);
                    heightText.setText(String.format("Weight: %.2f", height));
                    Toast.makeText(getApplicationContext(), "You have successfully updated your height", Toast.LENGTH_LONG).show();
                    double b = calculateBMI(weight, height);
                    setUserBMI(b, v);

                    d.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                d.dismiss();
            }
        });
        d.show();
    }
    //show dialog for user to input their latest weight
    public void showDialogForWeightInput(int intWeight, HealthRecord myHr, TextView v) {

        Dialog d = new Dialog(this);
        //d.setTitle("Update Your Weight");
        d.setContentView(R.layout.weight_input);
        Button save = d.findViewById(R.id.saveWeight);
        Button cancel = d.findViewById(R.id.cancelWeight);
        NumberPicker npInt = d.findViewById(R.id.weightInt);
        NumberPicker npDouble = d.findViewById(R.id.weightDouble);
        npInt.setMaxValue(500); // max value 100
        npInt.setMinValue(3);
        npInt.setValue(intWeight);
        npInt.setWrapSelectorWheel(false);
        String[] doubleArr = new String[]{".0 Kg", ".1 Kg", ".2 Kg", ".3 Kg", ".4 Kg", ".5 Kg", ".6 Kg", ".7 Kg", ".8 Kg", ".9 Kg"};
        npDouble.setMaxValue(9);
        npDouble.setMinValue(0);
        npDouble.setDisplayedValues(doubleArr);
        npDouble.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int val1 = npInt.getValue();
                int val2 = npDouble.getValue();
                double height = myHr.getUserHeight();
                double weight = val1 + val2 / 10.0;
                //update the selected value to server then after update the information of weight
                try {
                    updateUserWeight(weight);
                    weightText.setText(String.format("Weight: %.2f", weight));
                    Toast.makeText(getApplicationContext(), "You have successfully updated your weight", Toast.LENGTH_LONG).show();
                    double b = calculateBMI(weight, height);
                    setUserBMI(b, v);

                    d.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                d.dismiss();
            }
        });
        d.show();
    }

    private void updateUserHeight(double height) throws JSONException, IOException {
        JSONObject postData = new JSONObject();
        postData.put("username", user.getUsername());
        //postData.put("date", setDate(LocalDate.now()));
        DatePicker datePicker = datePickerDialog.getDatePicker();
        String dateSelect = datePicker.getYear() + "-" + String.format("%02d", (datePicker.getMonth() + 1)) + "-" + String.format("%02d", datePicker.getDayOfMonth());
        //postData.put("date", setDate(LocalDate.now()));
        postData.put("date", dateSelect);
        postData.put("height", height);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(postData.toString(), JSON);
        //need to use your own pc's ip address here, cannot use local host.
        Request request = new Request.Builder()
                .url(Constants.javaURL + "/user/updateheight")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code" + response);
                }
            }
        });
    }

    private void updateUserWeight(double weight) throws JSONException, IOException {
        JSONObject postData = new JSONObject();
        postData.put("username", user.getUsername());
        //postData.put("date", setDate(LocalDate.now()));
        DatePicker datePicker = datePickerDialog.getDatePicker();
        String dateSelect = datePicker.getYear() + "-" + String.format("%02d", (datePicker.getMonth() + 1)) + "-" + String.format("%02d", datePicker.getDayOfMonth());
        //postData.put("date", setDate(LocalDate.now()));
        postData.put("date", dateSelect);
        postData.put("weight", weight);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(postData.toString(), JSON);
        //need to use your own pc's ip address here, cannot use local host.
        Request request = new Request.Builder()
                .url(Constants.javaURL + "/user/updateweight")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code" + response);
                }
            }
        });
    }

    private double calculateBMI(HealthRecord myHr){

        if (myHr.getUserWeight() != 0 && myHr.getUserHeight() != 0) {
            return myHr.getUserWeight() / Math.pow(myHr.getUserHeight() / 100, 2.0);
        }
        else
            return 0.0;
    }
    private double calculateBMI(double weight, double height){

        if (weight != 0 && height != 0) {
            return weight / Math.pow(height / 100, 2.0);
        }
        else
            return 0.0;
    }

    private void setUserBMI(double myBmi, TextView bmi){
        if (myBmi == 0) {
            bmi.setText("BMI: N.A");
        } else {
            if(myBmi <= 18.5){
                bmi.setText("BMI: " + String.format("%.2f", myBmi) + "  (Underweight !)");
                bmi.setTextColor(Color.parseColor("#ff0000"));
            }
            else if(myBmi > 18.5 && myBmi < 25){
                bmi.setText("BMI: " + String.format("%.2f", myBmi) + "  (Normal weight)");
                bmi.setTextColor(Color.parseColor("#006400"));
            }
            else if(myBmi >= 25 && myBmi < 30){
                bmi.setText("BMI: " + String.format("%.2f", myBmi) + "  (Overweight !)");
                bmi.setTextColor(Color.parseColor("#ff0000"));
            }
            else{
                bmi.setText("BMI: " + String.format("%.2f", myBmi) + "  (Obesity !)");
                bmi.setTextColor(Color.parseColor("#ff0000"));
            }
        }
    }

    public void openDatePicker(View view) {
        datePickerDialog.show();
    }

    private void initDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                String date = year + "-" + String.format("%02d", month) + "-" + String.format("%02d", day);
                DateTimeFormatter format2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate parsedDate = LocalDate.parse(date, format2);
                dateButton.setText(setDate(parsedDate));

                getDietRecordsFromServer(user, date);
                getHealthRecordFromServer(user, date);
            }
        };

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) +1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int style = AlertDialog.THEME_HOLO_LIGHT;
       LocalDate minDate = user.getDateCreated();
       // LocalDate minDatedob = user.getDateofbirth();

        datePickerDialog = new DatePickerDialog(this, style, dateSetListener, year, month, day);

        // reminder to set min date first before max date for same date issue.
        datePickerDialog.getDatePicker().setMinDate(setCalDate(minDate).getTimeInMillis());
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
    }

    private String setDate(LocalDate date) {
        DateTimeFormatter format1 = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        return date.format(format1);
    }

    private Calendar setCalDate(LocalDate date) {
        Calendar cal = Calendar.getInstance();
        int year = date.getYear();
        int month = date.getMonthValue() -1;
        int day = date.getDayOfMonth();

        cal.set(year, month, day);
        return cal;
    }

    public void getDietRecordsFromServer(User user, String date) {
        JSONObject postData = new JSONObject();
        try {
            postData.put("username", user.getUsername());
            postData.put("date", date);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(postData.toString(), JSON);

            //need to use your own pc's ip address here, cannot use local host.
            Request request = new Request.Builder()
                    .url(Constants.javaURL + "/user/getdietrecords")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
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
                        //convert responseBody into list of HealthRecords
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.registerModule(new JavaTimeModule());
                        List<DietRecord> dietRecordList = Arrays.asList(objectMapper.readValue(responseBody.string(), DietRecord[].class));

                        //do something with FM here
                        FragmentManager fm = getSupportFragmentManager();
                        MealButtonsFragment mealFragment = (MealButtonsFragment) fm.findFragmentById(R.id.fragment_meal);
                        mealFragment.setDietRecordList(dietRecordList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

<<<<<<< Updated upstream
=======
    public static void removeMeal(DietRecord dr) {

    }

>>>>>>> Stashed changes

    public void setTopNavBar() {
        mToolbar = findViewById(R.id.top_navbar);
        setSupportActionBar(mToolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.top_nav_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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

    public void setBottomNavBar() {
        bottomNavView = findViewById(R.id.bottom_navigation);
        bottomNavView.setSelectedItemId(R.id.nav_log);
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

                    case (R.id.nav_recipe):
                        intent = new Intent(getApplicationContext(), RecipeActivity.class);
                        startActivity(intent);
                        break;

                    case (R.id.nav_home):
                        intent = new Intent(getApplicationContext(), HomeActivity.class);
                        startActivity(intent);
                        break;
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


    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);

    }

    private void startProfileActivity() {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }
}