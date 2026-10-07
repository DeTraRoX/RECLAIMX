package com.reclaimx.controllers;

import com.reclaimx.dao.UserDao;
import com.reclaimx.models.User;
import com.reclaimx.util.PasswordUtil;
import com.reclaimx.util.SecurityUtil;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuthController {
    private static final UserDao userDao = new UserDao();
    public static void renderRegister(Context ctx) {
        User current = SecurityUtil.getCurrentUser(ctx);
        if (current != null) {
            ctx.redirect(current.isAdmin() ? "/admin" : "/dashboard");
            return;
        }
        ctx.render("templates/auth/register.html", SecurityUtil.baseModel(ctx));
    }
    public static void handleRegister(Context ctx) {
        User current = SecurityUtil.getCurrentUser(ctx);
        if (current != null) {
            ctx.redirect(current.isAdmin() ? "/admin" : "/dashboard");
            return;
        }
        String name = ctx.formParamAsClass("name", String.class).getOrDefault("").trim();
        String studentId = ctx.formParamAsClass("student_id", String.class).getOrDefault("").trim();
        String email = ctx.formParamAsClass("email", String.class).getOrDefault("").trim();
        String department = ctx.formParamAsClass("department", String.class).getOrDefault("").trim();
        String yearStr = ctx.formParamAsClass("year", String.class).getOrDefault("").trim();
        String phone = ctx.formParamAsClass("phone", String.class).getOrDefault("").trim();
        String password = ctx.formParamAsClass("password", String.class).getOrDefault("");
        String confirmPassword = ctx.formParamAsClass("confirm_password", String.class).getOrDefault("");
        List<String> errors = new ArrayList<>();
        if (name.isEmpty()) errors.add("Full Name is required.");
        if (studentId.isEmpty()) errors.add("Student ID is required.");
        if (email.isEmpty()) {
            errors.add("Email is required.");
        } else if (!email.endsWith("@reclaimx.com") && !email.endsWith(".edu") && !(email.contains("@") && email.length() > 5)) {
            errors.add("Enter a valid college email address.");
        }
        if (department.isEmpty()) errors.add("Department is required.");
        int year = 0;
        try {
            year = Integer.parseInt(yearStr);
            if (year < 1 || year > 5) errors.add("Year must be between 1 and 5.");
        } catch (NumberFormatException e) {
            errors.add("Year must be an integer.");
        }
        if (phone.isEmpty()) errors.add("Phone number is required.");
        if (password.length() < 6) errors.add("Password must be at least 6 characters long.");
        if (!password.equals(confirmPassword)) errors.add("Passwords do not match.");
        if (userDao.findByEmail(email) != null) {
            errors.add("An account with this email already exists.");
        }
        if (!errors.isEmpty()) {
            for (String err : errors) {
                SecurityUtil.addFlash(ctx, "danger", err);
            }
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("name", name);
            model.put("student_id", studentId);
            model.put("email", email);
            model.put("department", department);
            model.put("year", yearStr);
            model.put("phone", phone);
            ctx.render("templates/auth/register.html", model);
            return;
        }
        User newUser = new User();
        newUser.setName(name);
        newUser.setStudentId(studentId);
        newUser.setEmail(email);
        newUser.setDepartment(department);
        newUser.setYear(year);
        newUser.setPhone(phone);
        newUser.setPasswordHash(PasswordUtil.hashPassword(password));
        newUser.setRole("student");
        newUser.setIsActive(true);
        if (userDao.createUser(newUser)) {
            SecurityUtil.addFlash(ctx, "success", "Registration successful! Please login below.");
            ctx.redirect("/login");
        } else {
            SecurityUtil.addFlash(ctx, "danger", "An error occurred during registration. Please try again.");
            ctx.render("templates/auth/register.html", SecurityUtil.baseModel(ctx));
        }
    }
    public static void renderLogin(Context ctx) {
        User current = SecurityUtil.getCurrentUser(ctx);
        if (current != null) {
            ctx.redirect(current.isAdmin() ? "/admin" : "/dashboard");
            return;
        }
        ctx.render("templates/auth/login.html", SecurityUtil.baseModel(ctx));
    }
    public static void handleLogin(Context ctx) {
        String email = ctx.formParamAsClass("email", String.class).getOrDefault("").trim();
        String password = ctx.formParamAsClass("password", String.class).getOrDefault("");
        User user = userDao.findByEmail(email);
        if (user == null || !PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            SecurityUtil.addFlash(ctx, "danger", "Invalid email or password.");
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("email", email);
            ctx.render("templates/auth/login.html", model);
            return;
        }
        if (!user.getIsActive()) {
            SecurityUtil.addFlash(ctx, "danger", "Your account has been deactivated. Please contact an admin.");
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("email", email);
            ctx.render("templates/auth/login.html", model);
            return;
        }
        SecurityUtil.setCurrentUser(ctx, user);
        SecurityUtil.addFlash(ctx, "success", "Welcome back, " + user.getName() + "!");
        if (user.isAdmin()) {
            ctx.redirect("/admin");
        } else {
            ctx.redirect("/dashboard");
        }
    }
    public static void handleLogout(Context ctx) {
        SecurityUtil.setCurrentUser(ctx, null);
        SecurityUtil.addFlash(ctx, "info", "You have been logged out.");
        ctx.redirect("/");
    }
}
