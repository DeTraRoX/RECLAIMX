package com.reclaimx.controllers;

import com.reclaimx.dao.ItemDao;
import com.reclaimx.models.Item;
import com.reclaimx.models.User;
import com.reclaimx.util.SecurityUtil;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ItemController {
    private static final ItemDao itemDao = new ItemDao();
    private static final List<String> CATEGORIES_LIST = Arrays.asList(
        "ID Card", "Wallet", "Mobile Phone", "Laptop", "Bag", "Books",
        "Notebook", "Earphones", "Headphones", "Bottle", "Keys",
        "Clothing", "Watch", "Jewellery", "Electronics", "Documents", "Other"
    );
    private static final List<String> LOCATIONS_LIST = Arrays.asList(
        "Library", "Cafeteria", "Classroom", "Laboratory", "Hostel", "Parking",
        "Sports Ground", "Auditorium", "Administrative Block", "Computer Lab", "Other"
    );
    public static void renderMyItems(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        List<Item> items = itemDao.findByUserId(current.getId());
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("items", items);
        ctx.render("templates/items/my_items.html", model);
    }
    public static void renderCreate(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("categories", CATEGORIES_LIST);
        model.put("locations", LOCATIONS_LIST);
        model.put("form_data", new HashMap<String, String>());
        ctx.render("templates/items/create.html", model);
    }
    public static void handleCreate(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        String title = ctx.formParamAsClass("title", String.class).getOrDefault("").trim();
        String category = ctx.formParamAsClass("category", String.class).getOrDefault("").trim();
        String color = ctx.formParamAsClass("color", String.class).getOrDefault("").trim();
        String brand = ctx.formParamAsClass("brand", String.class).getOrDefault("").trim();
        String description = ctx.formParamAsClass("description", String.class).getOrDefault("").trim();
        String locationSelect = ctx.formParamAsClass("location_select", String.class).getOrDefault("").trim();
        String locationCustom = ctx.formParamAsClass("location_custom", String.class).getOrDefault("").trim();
        String dateStr = ctx.formParamAsClass("date", String.class).getOrDefault("").trim();
        String itemType = ctx.formParamAsClass("type", String.class).getOrDefault("").trim();
        String location = "Other".equalsIgnoreCase(locationSelect) ? locationCustom : locationSelect;
        List<String> errors = new ArrayList<>();
        if (title.isEmpty()) errors.add("Item Name/Title is required.");
        if (!CATEGORIES_LIST.contains(category)) errors.add("Please select a valid category.");
        if (color.isEmpty()) errors.add("Color is required.");
        if (description.isEmpty()) errors.add("Description is required.");
        if (location.isEmpty()) errors.add("Location is required.");
        if (dateStr.isEmpty()) errors.add("Date is required.");
        if (!"lost".equalsIgnoreCase(itemType) && !"found".equalsIgnoreCase(itemType)) {
            errors.add("Type must be either Lost or Found.");
        }
        String filename = null;
        UploadedFile uploadedFile = ctx.uploadedFile("image");
        if (uploadedFile != null && uploadedFile.size() > 0) {
            String origName = uploadedFile.filename();
            String ext = "";
            int dot = origName.lastIndexOf('.');
            if (dot >= 0) {
                ext = origName.substring(dot + 1).toLowerCase();
            }
            if (Arrays.asList("png", "jpg", "jpeg", "webp").contains(ext)) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                filename = timestamp + "_" + origName.replaceAll("[^a-zA-Z0-9._-]", "_");
                saveFile(uploadedFile, filename);
            } else {
                errors.add("Invalid image format. Allowed formats: png, jpg, jpeg, webp.");
            }
        }
        Map<String, String> formData = new HashMap<>();
        formData.put("title", title);
        formData.put("category", category);
        formData.put("color", color);
        formData.put("brand", brand);
        formData.put("description", description);
        formData.put("location_select", locationSelect);
        formData.put("location_custom", locationCustom);
        formData.put("date", dateStr);
        formData.put("type", itemType);
        if (!errors.isEmpty()) {
            for (String err : errors) {
                SecurityUtil.addFlash(ctx, "danger", err);
            }
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("categories", CATEGORIES_LIST);
            model.put("locations", LOCATIONS_LIST);
            model.put("form_data", formData);
            ctx.render("templates/items/create.html", model);
            return;
        }
        String status = "lost".equalsIgnoreCase(itemType) ? "Lost" : "Found";
        Item newItem = new Item();
        newItem.setUserId(current.getId());
        newItem.setTitle(title);
        newItem.setCategory(category);
        newItem.setColor(color);
        newItem.setBrand(brand.isEmpty() ? null : brand);
        newItem.setDescription(description);
        newItem.setLocation(location);
        newItem.setDateLostFound(dateStr);
        newItem.setImage(filename);
        newItem.setType(itemType.toLowerCase());
        newItem.setStatus(status);
        if (itemDao.createItem(newItem)) {
            SecurityUtil.addFlash(ctx, "success", "Item '" + title + "' reported successfully as " + status.toUpperCase() + "!");
            ctx.redirect("/dashboard");
        } else {
            SecurityUtil.addFlash(ctx, "danger", "An error occurred while saving the report.");
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("categories", CATEGORIES_LIST);
            model.put("locations", LOCATIONS_LIST);
            model.put("form_data", formData);
            ctx.render("templates/items/create.html", model);
        }
    }
    public static void renderDetails(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Item item = itemDao.findById(id);
        if (item == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }
        User current = SecurityUtil.getCurrentUser(ctx);
        boolean showClaimBtn = false;
        if (current != null) {
            if ("found".equalsIgnoreCase(item.getType())
                    && Arrays.asList("Found", "Matched").contains(item.getStatus())
                    && item.getUserId() != current.getId()
                    && !current.isAdmin()) {
                showClaimBtn = true;
            }
        }
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("item", item);
        model.put("show_claim_btn", showClaimBtn);
        ctx.render("templates/items/details.html", model);
    }
    public static void renderEdit(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int id = Integer.parseInt(ctx.pathParam("id"));
        Item item = itemDao.findById(id);
        if (item == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }
        if (item.getUserId() != current.getId() && !current.isAdmin()) {
            ctx.status(403);
            ctx.render("templates/403.html", SecurityUtil.baseModel(ctx));
            return;
        }
        if (Arrays.asList("Returned", "Closed").contains(item.getStatus())) {
            SecurityUtil.addFlash(ctx, "warning", "Cannot edit items that have already been returned or closed.");
            ctx.redirect("/items/" + item.getId());
            return;
        }
        Map<String, Object> model = SecurityUtil.baseModel(ctx);
        model.put("item", item);
        model.put("categories", CATEGORIES_LIST);
        model.put("locations", LOCATIONS_LIST);
        ctx.render("templates/items/edit.html", model);
    }
    public static void handleEdit(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int id = Integer.parseInt(ctx.pathParam("id"));
        Item item = itemDao.findById(id);
        if (item == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }
        if (item.getUserId() != current.getId() && !current.isAdmin()) {
            ctx.status(403);
            ctx.render("templates/403.html", SecurityUtil.baseModel(ctx));
            return;
        }
        String title = ctx.formParamAsClass("title", String.class).getOrDefault("").trim();
        String category = ctx.formParamAsClass("category", String.class).getOrDefault("").trim();
        String color = ctx.formParamAsClass("color", String.class).getOrDefault("").trim();
        String brand = ctx.formParamAsClass("brand", String.class).getOrDefault("").trim();
        String description = ctx.formParamAsClass("description", String.class).getOrDefault("").trim();
        String locationSelect = ctx.formParamAsClass("location_select", String.class).getOrDefault("").trim();
        String locationCustom = ctx.formParamAsClass("location_custom", String.class).getOrDefault("").trim();
        String dateStr = ctx.formParamAsClass("date", String.class).getOrDefault("").trim();
        String location = "Other".equalsIgnoreCase(locationSelect) ? locationCustom : locationSelect;
        List<String> errors = new ArrayList<>();
        if (title.isEmpty()) errors.add("Item Name is required.");
        if (!CATEGORIES_LIST.contains(category)) errors.add("Invalid category.");
        if (color.isEmpty()) errors.add("Color is required.");
        if (description.isEmpty()) errors.add("Description is required.");
        if (location.isEmpty()) errors.add("Location is required.");
        if (dateStr.isEmpty()) errors.add("Date is required.");
        UploadedFile uploadedFile = ctx.uploadedFile("image");
        if (uploadedFile != null && uploadedFile.size() > 0) {
            String origName = uploadedFile.filename();
            String ext = "";
            int dot = origName.lastIndexOf('.');
            if (dot >= 0) ext = origName.substring(dot + 1).toLowerCase();
            if (Arrays.asList("png", "jpg", "jpeg", "webp").contains(ext)) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                String newFileName = timestamp + "_" + origName.replaceAll("[^a-zA-Z0-9._-]", "_");
                saveFile(uploadedFile, newFileName);
                item.setImage(newFileName);
            } else {
                errors.add("Invalid image format.");
            }
        }
        if (!errors.isEmpty()) {
            for (String err : errors) {
                SecurityUtil.addFlash(ctx, "danger", err);
            }
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("item", item);
            model.put("categories", CATEGORIES_LIST);
            model.put("locations", LOCATIONS_LIST);
            ctx.render("templates/items/edit.html", model);
            return;
        }
        item.setTitle(title);
        item.setCategory(category);
        item.setColor(color);
        item.setBrand(brand.isEmpty() ? null : brand);
        item.setDescription(description);
        item.setLocation(location);
        item.setDateLostFound(dateStr);
        if (itemDao.updateItem(item)) {
            SecurityUtil.addFlash(ctx, "success", "Item report updated successfully!");
            ctx.redirect("/items/" + item.getId());
        } else {
            SecurityUtil.addFlash(ctx, "danger", "An error occurred while updating the item.");
            Map<String, Object> model = SecurityUtil.baseModel(ctx);
            model.put("item", item);
            model.put("categories", CATEGORIES_LIST);
            model.put("locations", LOCATIONS_LIST);
            ctx.render("templates/items/edit.html", model);
        }
    }
    public static void handleDelete(Context ctx) {
        if (!SecurityUtil.requireAuth(ctx)) return;
        User current = SecurityUtil.getCurrentUser(ctx);
        int id = Integer.parseInt(ctx.pathParam("id"));
        Item item = itemDao.findById(id);
        if (item == null) {
            ctx.status(404);
            ctx.render("templates/404.html", SecurityUtil.baseModel(ctx));
            return;
        }
        if (item.getUserId() != current.getId() && !current.isAdmin()) {
            ctx.status(403);
            ctx.render("templates/403.html", SecurityUtil.baseModel(ctx));
            return;
        }
        if (itemDao.deleteItem(id)) {
            SecurityUtil.addFlash(ctx, "success", "Item report deleted successfully.");
        } else {
            SecurityUtil.addFlash(ctx, "danger", "Could not delete item.");
        }
        if (current.isAdmin()) {
            ctx.redirect("/admin/items");
        } else {
            ctx.redirect("/items/my-items");
        }
    }
    private static void saveFile(UploadedFile uploadedFile, String filename) {
        try {
            File dir1 = new File("uploads");
            if (!dir1.exists()) dir1.mkdirs();
            File dest1 = new File(dir1, filename);
            try (InputStream is = uploadedFile.content(); FileOutputStream fos = new FileOutputStream(dest1)) {
                is.transferTo(fos);
            }
            File dir2 = new File("src/main/resources/public/uploads");
            if (!dir2.exists()) dir2.mkdirs();
            File dest2 = new File(dir2, filename);
            Files.copy(dest1.toPath(), dest2.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
