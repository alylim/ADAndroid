package com.team2.getfitwithhenry.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private int id;
    private String name;
    private String username;
    private String password;
    private Role role;
    private Goal goal;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateofbirth;
    private String dobStringFormat;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateCreated;
    private String dateCreatedStringFormat;
    private String gender;
    private Double calorieintake_limit_inkcal;
    private Double waterintake_limit_inml;
    private String activitylevel;
    private List<Ingredient> dislike = new ArrayList<>();

    public User(@JsonProperty("id") int id, @JsonProperty("name") String name, @JsonProperty("username") String username,
                @JsonProperty("password") String password, @JsonProperty("role") Role role, @JsonProperty("goal") Goal goal,
                @JsonProperty("dateofbirth") LocalDate dateofbirth,@JsonProperty("dateCreated") LocalDate dateCreated, @JsonProperty("gender") String gender,
                @JsonProperty("calorieintake_limit_inkcal") Double calorieintake_limit_inkcal, @JsonProperty("waterintake_limit_inml") Double waterintake_limit_inml,
                @JsonProperty("activitylevel") String activitylevel, @JsonProperty("dislike") List<Ingredient> dislike) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
        this.dateofbirth = dateofbirth;
        this.dobStringFormat = dateofbirth.toString();
        this.dateCreated = dateCreated;
        this.dateCreatedStringFormat = dateCreated.toString();
        this.gender = gender;
        this.calorieintake_limit_inkcal = calorieintake_limit_inkcal;
        this.waterintake_limit_inml = waterintake_limit_inml;
        this.activitylevel = activitylevel;
        this.goal = goal;
        this.dislike = dislike;
    }

    public User(int id, String name, String username, String password, Role role, Goal goal, LocalDate dateofbirth, String gender, Double calorieintake_limit_inkcal, Double waterintake_limit_inml, String activitylevel, LocalDate dateCreated) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
        this.dateofbirth = dateofbirth;
        this.dobStringFormat = dateofbirth.toString();
        this.dateCreated = dateCreated;
        this.dateCreatedStringFormat = dateCreated.toString();
        this.gender = gender;
        this.calorieintake_limit_inkcal = calorieintake_limit_inkcal;
        this.waterintake_limit_inml = waterintake_limit_inml;
        this.activitylevel = activitylevel;
        this.goal = goal;
    }

//    public User(@JsonProperty("id") int id, @JsonProperty("name") String name, @JsonProperty("username") String username, @JsonProperty("password") String password, @JsonProperty("role") Role role, @JsonProperty("goal") Goal goal, @JsonProperty("dislike") List<Ingredient> dislike) {
//        this.id = id;
//        this.name = name;
//        this.username = username;
//        this.password = password;
//        this.role = role;
//        this.goal = goal;
//        this.dislike = dislike;
//    }

    public User(int id, String name, String username, String password, Role role, Goal goal) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
        this.goal = goal;
    }

    public User(String username) {
        this.username = username;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String name, String username, String password, LocalDate dob, String gender, Goal goal) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.dateofbirth = dob;
        this.gender = gender;
        this.goal = goal;
        this.role = Role.NORMAL;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public LocalDate getDateofbirth() {
        return dateofbirth;
    }

    public void setDateofbirth(LocalDate dateofbirth) {
        this.dateofbirth = dateofbirth;
    }

    public LocalDate getDateCreated() { return dateCreated; }

    public void setDateCreated(LocalDate dateCreated) { this.dateCreated  = dateCreated; }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Double getCalorieintake_limit_inkcal() {
        return calorieintake_limit_inkcal;
    }

    public void setCalorieintake_limit_inkcal(Double calorieintake_limit_inkcal) {
        this.calorieintake_limit_inkcal = calorieintake_limit_inkcal;
    }

    public Double getWaterintake_limit_inml() {
        return waterintake_limit_inml;
    }

    public void setWaterintake_limit_inml(Double waterintake_limit_inml) {
        this.waterintake_limit_inml = waterintake_limit_inml;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Goal getGoal() {
        return goal;
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
    }

    public String getDobStringFormat() {
        return dobStringFormat;
    }

    public void setDobStringFormat(String dobStringFormat) {
        this.dobStringFormat = dobStringFormat;
    }

    public String getDateCreatedStringFormat() { return dateCreatedStringFormat; }

    public void setDateCreatedStringFormat(String dateCreatedStringFormat){
        this.dateCreatedStringFormat = dateCreatedStringFormat;
    }

    public String getActivitylevel() {
        return activitylevel;
    }

    public void setActivitylevel(String activitylevel) {
        this.activitylevel = activitylevel;
    }
}
