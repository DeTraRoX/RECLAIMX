package com.reclaimx.util;

import com.reclaimx.dao.UserDao;
import com.reclaimx.models.User;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecurityUtil {

    private static final UserDao userDao = new UserDao();

    public static User getCurrentUser(Context ctx) {
        Integer userId = ctx.sessionAttribute("user_id");
        if (userId != null) {
            return userDao.findById(userId);
        }
        return null;
    }

    public static void setCurrentUser(Context ctx, User user) {
        if (user != null) {
            ctx.sessionAttribute("user_id", user.getId());
        } else {
            ctx.sessionAttribute("user_id", null);
        }
    }

    public static void addFlash(Context ctx, String type, String message) {
        List<Map<String, String>> flashes = ctx.sessionAttribute("flashes");
        if (flashes == null) {
            flashes = new ArrayList<>();
        }
        Map<String, String> flash = new HashMap<>();
        flash.put("type", type); // success, danger, warning, info
        flash.put("message", message);
        flashes.add(flash);
        ctx.sessionAttribute("flashes", flashes);
    }

    public static List<Map<String, String>> getAndClearFlashes(Context ctx) {
        List<Map<String, String>> flashes = ctx.sessionAttribute("flashes");
        if (flashes == null) {
            flashes = new ArrayList<>();
        }
        ctx.sessionAttribute("flashes", null);
        return flashes;
    }

    public static Map<String, Object> baseModel(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        User currentUser = getCurrentUser(ctx);
        model.put("current_user", currentUser);
        model.put("flashes", getAndClearFlashes(ctx));
        return model;
    }

    public static boolean requireAuth(Context ctx) {
        User user = getCurrentUser(ctx);
        if (user == null) {
            addFlash(ctx, "warning", "Please log in to access this page.");
            ctx.redirect("/login");
            return false;
        }
        if (!user.getIsActive()) {
            setCurrentUser(ctx, null);
            addFlash(ctx, "danger", "Your account has been deactivated. Please contact an admin.");
            ctx.redirect("/login");
            return false;
        }
        return true;
    }

    public static boolean requireAdmin(Context ctx) {
        if (!requireAuth(ctx)) {
            return false;
        }
        User user = getCurrentUser(ctx);
        if (user == null || !user.isAdmin()) {
            ctx.status(403);
            ctx.render("templates/403.html", baseModel(ctx));
            return false;
        }
        return true;
    }
}
