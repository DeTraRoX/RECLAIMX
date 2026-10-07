package com.reclaimx.controllers;

import com.reclaimx.dao.NotificationDao;
import com.reclaimx.models.Notification;
import com.reclaimx.models.User;
import com.reclaimx.util.SecurityUtil;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationController {
    private static final NotificationDao notificationDao = new NotificationDao();
    public static void renderNotifications(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        List<Notification> notifs = notificationDao.findByUserId(current.getId(), 0);
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("notifications", notifs);
        ctx.render("templates/notifications/notifications.html", model);
    }
    public static void handleMarkRead(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int id = Integer.parseInt(ctx.pathParam("id"));
        Notification notif = notificationDao.findById(id);
        if (notif == null || notif.getUserId() != current.getId()) {
            ctx.status(403);
            ctx.render("templates/403.html", SecurityUtil.baseModel(ctx));
            return;
        }
        notificationDao.markAsRead(id);
        if ("XMLHttpRequest".equalsIgnoreCase(ctx.header("X-Requested-With"))) {
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Notification marked as read");
            ctx.json(res);
            return;
        }
        ctx.redirect("/notifications");
    }
    public static void handleMarkAllRead(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        notificationDao.markAllAsRead(current.getId());
        if ("XMLHttpRequest".equalsIgnoreCase(ctx.header("X-Requested-With"))) {
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "All notifications marked as read");
            ctx.json(res);
            return;
        }
        SecurityUtil.addFlash(ctx, "success", "All notifications marked as read.");
        ctx.redirect("/notifications");
    }
}
