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
import com.reclaimx.util.SecurityUtil;
import io.javalin.http.Context;

import java.util.*;

public class AdminController {

    private static final StatisticsService statsService = new StatisticsService();
    private static final UserDao userDao = new UserDao();
    private static final ItemDao itemDao = new ItemDao();
    private static final ClaimDao claimDao = new ClaimDao();
    private static final NotificationDao notificationDao = new NotificationDao();

    public static void renderDashboard(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;

        Map<String, Object> stats = statsService.getGeneralStatistics();
        List<Claim> recentClaims = claimDao.findAllFiltered("Pending");
        if (recentClaims.size() > 5) recentClaims = recentClaims.subList(0, 5);

        List<Item> recentItems = itemDao.findAllFiltered(null, null, null);
        if (recentItems.size() > 5) recentItems = recentItems.subList(0, 5);

        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("stats", stats);
        model.put("recent_claims", recentClaims);
        model.put("recent_items", recentItems);
        ctx.render("templates/admin/dashboard.html", model);
    }

    public static void renderUsers(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;

        String q = ctx.queryParamAsClass("q", String.class).getOrDefault("").trim();
        String role = ctx.queryParamAsClass("role", String.class).getOrDefault("all").trim();
        String department = ctx.queryParamAsClass("department", String.class).getOrDefault("all").trim();

        List<User> usersList = userDao.findAllFiltered(q, role, department);
        List<String> departments = userDao.findDistinctDepartments();

        Map<String, String> filters = new HashMap<>();
        filters.put("q", q);
        filters.put("role", role);
        filters.put("department", department);

        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("users", usersList);
        model.put("departments", departments);
        model.put("filters", filters);
        ctx.render("templates/admin/users.html", model);
    }

    public static void handleToggleUserStatus(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int id = Integer.parseInt(ctx.pathParam("id"));

        User user = userDao.findById(id);
        if (user == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }

        if (user.getId() == current.getId()) {
            SecurityUtil.addFlash(ctx, "danger", "You cannot deactivate your own admin account.");
            ctx.redirect("/admin/users");
            return;
        }

        boolean newStatus = !user.getIsActive();
        if (userDao.toggleUserStatus(user.getId(), newStatus)) {
            String statusText = newStatus ? "activated" : "deactivated";
            SecurityUtil.addFlash(ctx, "success", "User " + user.getName() + " has been " + statusText + ".");
        } else {
            SecurityUtil.addFlash(ctx, "danger", "An error occurred.");
        }
        ctx.redirect("/admin/users");
    }

    public static void handleChangeUserRole(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int id = Integer.parseInt(ctx.pathParam("id"));

        User user = userDao.findById(id);
        if (user == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }

        if (user.getId() == current.getId()) {
            SecurityUtil.addFlash(ctx, "danger", "You cannot change your own admin role.");
            ctx.redirect("/admin/users");
            return;
        }

        String newRole = ctx.formParamAsClass("role", String.class).getOrDefault("student").trim();
        if (!"student".equals(newRole) && !"admin".equals(newRole)) {
            SecurityUtil.addFlash(ctx, "danger", "Invalid role specified.");
            ctx.redirect("/admin/users");
            return;
        }

        if (userDao.changeUserRole(user.getId(), newRole)) {
            SecurityUtil.addFlash(ctx, "success", "User " + user.getName() + "'s role updated to " + newRole.toUpperCase() + ".");
        } else {
            SecurityUtil.addFlash(ctx, "danger", "An error occurred.");
        }
        ctx.redirect("/admin/users");
    }

    public static void renderItems(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;

        String q = ctx.queryParamAsClass("q", String.class).getOrDefault("").trim();
        String itemType = ctx.queryParamAsClass("type", String.class).getOrDefault("all").trim();
        String status = ctx.queryParamAsClass("status", String.class).getOrDefault("all").trim();

        List<Item> itemsList = itemDao.findAllFiltered(q, itemType, status);

        Map<String, String> filters = new HashMap<>();
        filters.put("q", q);
        filters.put("type", itemType);
        filters.put("status", status);

        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("items", itemsList);
        model.put("filters", filters);
        ctx.render("templates/admin/items.html", model);
    }

    public static void handleCloseItem(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;
        int id = Integer.parseInt(ctx.pathParam("id"));

        Item item = itemDao.findById(id);
        if (item == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }

        if (itemDao.updateStatus(item.getId(), "Closed")) {
            SecurityUtil.addFlash(ctx, "success", "Item '" + item.getTitle() + "' has been closed.");
        } else {
            SecurityUtil.addFlash(ctx, "danger", "An error occurred.");
        }
        ctx.redirect("/admin/items");
    }

    public static void renderClaims(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;

        String status = ctx.queryParamAsClass("status", String.class).getOrDefault("Pending").trim();
        List<Claim> claimsList = claimDao.findAllFiltered(status);

        List<Map<String, Object>> claimsData = new ArrayList<>();
        for (Claim claim : claimsList) {
            Map<String, Object> cData = new HashMap<>();
            cData.put("claim", claim);
            cData.put("score", 0); // Omitted matching feature
            claimsData.add(cData);
        }

        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("claims_data", claimsData);
        model.put("current_status", status);
        ctx.render("templates/admin/claims.html", model);
    }

    public static void handleApproveClaim(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int id = Integer.parseInt(ctx.pathParam("id"));

        Claim claim = claimDao.findById(id);
        if (claim == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }

        if (!"Pending".equalsIgnoreCase(claim.getStatus())) {
            SecurityUtil.addFlash(ctx, "warning", "This claim has already been processed.");
            ctx.redirect("/admin/claims");
            return;
        }

        // Approve target claim
        claimDao.updateStatus(claim.getId(), "Approved", current.getId());

        // Update item status to Verified
        Item item = claim.getItem();
        if (item != null) {
            itemDao.updateStatus(item.getId(), "Verified");

            // Notify approved claimant
            Notification notifClaimant = new Notification();
            notifClaimant.setUserId(claim.getClaimantId());
            notifClaimant.setMessage("Your claim for found item '" + item.getTitle() + "' has been APPROVED! Please visit the college administrative block to collect it.");
            notifClaimant.setType("claim_approved");
            notifClaimant.setRelatedItemId(item.getId());
            notificationDao.createNotification(notifClaimant);

            // Reject other pending claims for this item
            List<Claim> otherClaims = claimDao.findByItemId(item.getId());
            for (Claim other : otherClaims) {
                if (other.getId() != claim.getId() && "Pending".equalsIgnoreCase(other.getStatus())) {
                    claimDao.updateStatus(other.getId(), "Rejected", current.getId());

                    Notification notifOther = new Notification();
                    notifOther.setUserId(other.getClaimantId());
                    notifOther.setMessage("Your claim for found item '" + item.getTitle() + "' was rejected because it was claimed by another student.");
                    notifOther.setType("claim_rejected");
                    notifOther.setRelatedItemId(item.getId());
                    notificationDao.createNotification(notifOther);
                }
            }
        }

        SecurityUtil.addFlash(ctx, "success", "Claim approved successfully!");
        ctx.redirect("/admin/claims");
    }

    public static void handleRejectClaim(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int id = Integer.parseInt(ctx.pathParam("id"));

        Claim claim = claimDao.findById(id);
        if (claim == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }

        if (!"Pending".equalsIgnoreCase(claim.getStatus())) {
            SecurityUtil.addFlash(ctx, "warning", "This claim has already been processed.");
            ctx.redirect("/admin/claims");
            return;
        }

        claimDao.updateStatus(claim.getId(), "Rejected", current.getId());

        Item item = claim.getItem();
        if (item != null) {
            List<Claim> itemClaims = claimDao.findByItemId(item.getId());
            long pendingCount = itemClaims.stream().filter(c -> c.getId() != claim.getId() && "Pending".equalsIgnoreCase(c.getStatus())).count();
            if (pendingCount == 0) {
                itemDao.updateStatus(item.getId(), "Found");
            }

            Notification notifClaimant = new Notification();
            notifClaimant.setUserId(claim.getClaimantId());
            notifClaimant.setMessage("Your claim for found item '" + item.getTitle() + "' has been rejected. Please provide more proof or contact support.");
            notifClaimant.setType("claim_rejected");
            notifClaimant.setRelatedItemId(item.getId());
            notificationDao.createNotification(notifClaimant);
        }

        SecurityUtil.addFlash(ctx, "success", "Claim rejected successfully.");
        ctx.redirect("/admin/claims");
    }

    public static void handleMarkReturned(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;
        int id = Integer.parseInt(ctx.pathParam("id"));

        Item item = itemDao.findById(id);
        if (item == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }

        if (!Arrays.asList("Verified", "Claimed", "Found", "Lost", "Matched").contains(item.getStatus())) {
            SecurityUtil.addFlash(ctx, "warning", "Cannot return item with status " + item.getStatus() + ".");
            ctx.redirect("/admin/items");
            return;
        }

        itemDao.updateStatus(item.getId(), "Returned");

        // Notify item reporter
        Notification notifReporter = new Notification();
        notifReporter.setUserId(item.getUserId());
        notifReporter.setMessage("Item '" + item.getTitle() + "' has been successfully returned.");
        notifReporter.setType("returned");
        notifReporter.setRelatedItemId(item.getId());
        notificationDao.createNotification(notifReporter);

        // Notify approved claimant if exists
        Claim approvedClaim = claimDao.findApprovedClaimForItem(item.getId());
        if (approvedClaim != null) {
            Notification notifClaimant = new Notification();
            notifClaimant.setUserId(approvedClaim.getClaimantId());
            notifClaimant.setMessage("Your claimed item '" + item.getTitle() + "' has been marked as returned/collected.");
            notifClaimant.setType("returned");
            notifClaimant.setRelatedItemId(item.getId());
            notificationDao.createNotification(notifClaimant);
        }

        SecurityUtil.addFlash(ctx, "success", "Item '" + item.getTitle() + "' has been marked as Returned.");
        ctx.redirect("/admin/items");
    }

    public static void renderReports(Context ctx) {
        if (!SecurityUtil.requireAdmin(ctx)) return;

        Map<String, Object> detailedData = statsService.getDetailedReportData();
        Map<String, Object> generalStats = statsService.getGeneralStatistics();

        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("detailed_data", detailedData);
        model.put("stats", generalStats);
        ctx.render("templates/admin/reports.html", model);
    }
}
