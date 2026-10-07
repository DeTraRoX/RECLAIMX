package com.reclaimx.controllers;

import com.reclaimx.dao.ClaimDao;
import com.reclaimx.dao.ItemDao;
import com.reclaimx.dao.NotificationDao;
import com.reclaimx.dao.UserDao;
import com.reclaimx.models.Claim;
import com.reclaimx.models.Item;
import com.reclaimx.models.Notification;
import com.reclaimx.models.User;
import com.reclaimx.services.StatisticsService;
import com.reclaimx.util.PasswordUtil;
import com.reclaimx.util.SecurityUtil;
import io.javalin.http.Context;
import java.util.*;

public class MainController {
    private static final StatisticsService statsService = new StatisticsService();
    private static final ItemDao itemDao = new ItemDao();
    private static final ClaimDao claimDao = new ClaimDao();
    private static final NotificationDao notificationDao = new NotificationDao();
    private static final UserDao userDao = new UserDao();
    public static void renderIndex(Context ctx) {
        User user = SecurityUtil.getCurrentUser(ctx);
        if (user != null) {
            ctx.redirect(user.isAdmin() ? "/admin" : "/dashboard");
            return;
        }
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("stats", statsService.getGeneralStatistics());
        ctx.render("templates/index.html", model);
    }
    public static void renderDashboard(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        if (current.isAdmin()) {
            ctx.redirect("/admin");
            return;
        }
        List<Item> myLostItems = itemDao.findByUserIdAndType(current.getId(), "lost");
        List<Item> myFoundItems = itemDao.findByUserIdAndType(current.getId(), "found");
        List<Claim> myClaims = claimDao.findByClaimantId(current.getId());
        List<Notification> notifications = notificationDao.findByUserId(current.getId(), 5);
        long returnedCount = myLostItems.stream().filter(i -> "Returned".equalsIgnoreCase(i.getStatus())).count()
                + myFoundItems.stream().filter(i -> "Returned".equalsIgnoreCase(i.getStatus())).count();
        Map<String, Object> counts = new HashMap<>();
        counts.put("lost", myLostItems.size());
        counts.put("found", myFoundItems.size());
        counts.put("claims", myClaims.size());
        counts.put("returned", returnedCount);
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("counts", counts);
        model.put("my_lost_items", myLostItems.size() > 5 ? myLostItems.subList(0, 5) : myLostItems);
        model.put("my_found_items", myFoundItems.size() > 5 ? myFoundItems.subList(0, 5) : myFoundItems);
        model.put("my_claims", myClaims.size() > 5 ? myClaims.subList(0, 5) : myClaims);
        model.put("notifications", notifications);
        ctx.render("templates/dashboard/dashboard.html", model);
    }
    public static void renderBrowse(Context ctx) {
        String q = ctx.queryParamAsClass("q", String.class).getOrDefault("").trim();
        String itemType = ctx.queryParamAsClass("type", String.class).getOrDefault("all").trim();
        String category = ctx.queryParamAsClass("category", String.class).getOrDefault("all").trim();
        String location = ctx.queryParamAsClass("location", String.class).getOrDefault("all").trim();
        String color = ctx.queryParamAsClass("color", String.class).getOrDefault("all").trim();
        String status = ctx.queryParamAsClass("status", String.class).getOrDefault("all").trim();
        String sort = ctx.queryParamAsClass("sort", String.class).getOrDefault("newest").trim();
        List<Item> items = itemDao.findBrowseItems(q, itemType, category, location, color, status, sort);
        List<String> categoriesList = Arrays.asList(
            "ID Card", "Wallet", "Mobile Phone", "Laptop", "Bag", "Books",
            "Notebook", "Earphones", "Headphones", "Bottle", "Keys",
            "Clothing", "Watch", "Jewellery", "Electronics", "Documents", "Other"
        );
        List<String> locations = itemDao.findDistinctLocations();
        Map<String, String> filters = new HashMap<>();
        filters.put("q", q);
        filters.put("type", itemType);
        filters.put("category", category);
        filters.put("location", location);
        filters.put("color", color);
        filters.put("status", status);
        filters.put("sort", sort);
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("items", items);
        model.put("categories", categoriesList);
        model.put("locations", locations);
        model.put("filters", filters);
        ctx.render("templates/items/browse.html", model);
    }
    public static void renderProfile(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        ctx.render("templates/dashboard/profile.html", model);
    }
    public static void renderEditProfile(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("user", SecurityUtil.getCurrentUser(ctx));
        ctx.render("templates/dashboard/profile_edit.html", model);
    }
    public static void handleEditProfile(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        String name = ctx.formParamAsClass("name", String.class).getOrDefault("").trim();
        String studentId = ctx.formParamAsClass("student_id", String.class).getOrDefault("").trim();
        String department = ctx.formParamAsClass("department", String.class).getOrDefault("").trim();
        String yearStr = ctx.formParamAsClass("year", String.class).getOrDefault("").trim();
        String phone = ctx.formParamAsClass("phone", String.class).getOrDefault("").trim();
        String password = ctx.formParamAsClass("password", String.class).getOrDefault("");
        String confirmPassword = ctx.formParamAsClass("confirm_password", String.class).getOrDefault("");
        List<String> errors = new ArrayList<>();
        if (name.isEmpty()) errors.add("Name is required.");
        if (studentId.isEmpty()) errors.add("Student ID is required.");
        if (department.isEmpty()) errors.add("Department is required.");
        int year = 0;
        try {
            year = Integer.parseInt(yearStr);
            if (year < 1 || year > 5) errors.add("Year must be between 1 and 5.");
        } catch (NumberFormatException e) {
            errors.add("Year must be an integer.");
        }
        if (phone.isEmpty()) errors.add("Phone is required.");
        if (!password.isEmpty()) {
            if (password.length() < 6) errors.add("New password must be at least 6 characters long.");
            if (!password.equals(confirmPassword)) errors.add("Passwords do not match.");
        }
        if (!errors.isEmpty()) {
            for (String err : errors) {
                SecurityUtil.addFlash(ctx, "danger", err);
            }
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("user", current);
            ctx.render("templates/dashboard/profile_edit.html", model);
            return;
        }
        current.setName(name);
        current.setStudentId(studentId);
        current.setDepartment(department);
        current.setYear(year);
        current.setPhone(phone);
        if (!password.isEmpty()) {
            current.setPasswordHash(PasswordUtil.hashPassword(password));
        }
        if (userDao.updateUser(current)) {
            SecurityUtil.addFlash(ctx, "success", "Profile updated successfully!");
            ctx.redirect("/profile");
        } else {
            SecurityUtil.addFlash(ctx, "danger", "An error occurred. Please try again.");
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("user", current);
            ctx.render("templates/dashboard/profile_edit.html", model);
        }
    }
}
