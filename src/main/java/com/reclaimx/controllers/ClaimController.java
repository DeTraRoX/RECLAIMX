package com.reclaimx.controllers;

import com.reclaimx.dao.ClaimDao;
import com.reclaimx.dao.ItemDao;
import com.reclaimx.dao.NotificationDao;
import com.reclaimx.models.Claim;
import com.reclaimx.models.Item;
import com.reclaimx.models.Notification;
import com.reclaimx.models.User;
import com.reclaimx.util.SecurityUtil;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ClaimController {

    private static final ItemDao itemDao = new ItemDao();
    private static final ClaimDao claimDao = new ClaimDao();
    private static final NotificationDao notificationDao = new NotificationDao();

    public static void renderCreate(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int itemId = Integer.parseInt(ctx.pathParam("itemId"));

        Item item = itemDao.findById(itemId);
        if (item == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }

        if (!"found".equalsIgnoreCase(item.getType())) {
            SecurityUtil.addFlash(ctx, "danger", "You can only file a claim for found items.");
            ctx.redirect("/items/" + item.getId());
            return;
        }

        if (item.getUserId() == current.getId()) {
            SecurityUtil.addFlash(ctx, "danger", "You cannot file a claim for an item you reported as found yourself.");
            ctx.redirect("/items/" + item.getId());
            return;
        }

        if (Arrays.asList("Verified", "Returned", "Closed").contains(item.getStatus())) {
            SecurityUtil.addFlash(ctx, "warning", "This item has already been verified or returned.");
            ctx.redirect("/items/" + item.getId());
            return;
        }

        Claim existingClaim = claimDao.findPendingClaim(item.getId(), current.getId());
        if (existingClaim != null) {
            SecurityUtil.addFlash(ctx, "warning", "You already have a pending claim for this item.");
            ctx.redirect("/claims/my-claims");
            return;
        }

        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("item", item);
        ctx.render("templates/claims/create.html", model);
    }

    public static void handleCreate(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int itemId = Integer.parseInt(ctx.pathParam("itemId"));

        Item item = itemDao.findById(itemId);
        if (item == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }

        if (!"found".equalsIgnoreCase(item.getType())) {
            SecurityUtil.addFlash(ctx, "danger", "You can only file a claim for found items.");
            ctx.redirect("/items/" + item.getId());
            return;
        }

        if (item.getUserId() == current.getId()) {
            SecurityUtil.addFlash(ctx, "danger", "You cannot file a claim for an item you reported as found yourself.");
            ctx.redirect("/items/" + item.getId());
            return;
        }

        if (Arrays.asList("Verified", "Returned", "Closed").contains(item.getStatus())) {
            SecurityUtil.addFlash(ctx, "warning", "This item has already been verified or returned.");
            ctx.redirect("/items/" + item.getId());
            return;
        }

        Claim existingClaim = claimDao.findPendingClaim(item.getId(), current.getId());
        if (existingClaim != null) {
            SecurityUtil.addFlash(ctx, "warning", "You already have a pending claim for this item.");
            ctx.redirect("/claims/my-claims");
            return;
        }

        String message = ctx.formParamAsClass("message", String.class).getOrDefault("").trim();
        String proof = ctx.formParamAsClass("proof", String.class).getOrDefault("").trim();

        List<String> errors = new ArrayList<>();
        if (message.isEmpty()) errors.add("Please explain why you believe this item belongs to you.");
        if (proof.isEmpty()) errors.add("Please provide detailed identifying proof/marks.");

        if (!errors.isEmpty()) {
            for (String err : errors) {
                SecurityUtil.addFlash(ctx, "danger", err);
            }
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("item", item);
            ctx.render("templates/claims/create.html", model);
            return;
        }

        Claim newClaim = new Claim();
        newClaim.setItemId(item.getId());
        newClaim.setClaimantId(current.getId());
        newClaim.setMessage(message);
        newClaim.setProof(proof);
        newClaim.setStatus("Pending");

        if (claimDao.createClaim(newClaim)) {
            // Update item status to Claimed
            itemDao.updateStatus(item.getId(), "Claimed");

            // Send Notification to finder
            Notification notif = new Notification();
            notif.setUserId(item.getUserId());
            notif.setMessage("Someone has submitted a claim for the found item '" + item.getTitle() + "' you reported.");
            notif.setType("claim_submitted");
            notif.setRelatedItemId(item.getId());
            notificationDao.createNotification(notif);

            SecurityUtil.addFlash(ctx, "success", "Claim submitted successfully! The administrator will review your claim details.");
            ctx.redirect("/claims/my-claims");
        } else {
            SecurityUtil.addFlash(ctx, "danger", "An error occurred while submitting your claim.");
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("item", item);
            ctx.render("templates/claims/create.html", model);
        }
    }

    public static void renderMyClaims(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);

        List<Claim> claims = claimDao.findByClaimantId(current.getId());
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("claims", claims);
        ctx.render("templates/claims/my_claims.html", model);
    }

    public static void renderDetails(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int id = Integer.parseInt(ctx.pathParam("id"));

        Claim claim = claimDao.findById(id);
        if (claim == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }

        if (claim.getClaimantId() != current.getId() && !current.isAdmin()) {
            ctx.status(403);
            ctx.render("templates/403.html", SecurityUtil.baseModel(ctx));
            return;
        }

        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("claim", claim);
        ctx.render("templates/claims/details.html", model);
    }
}
