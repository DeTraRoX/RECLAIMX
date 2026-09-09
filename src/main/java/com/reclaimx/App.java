package com.reclaimx;

import com.reclaimx.config.Database;
import com.reclaimx.controllers.*;
import com.reclaimx.util.SecurityUtil;
import com.reclaimx.util.SeedData;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.FileRenderer;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;

import java.io.File;
import java.io.StringWriter;
import java.util.Map;

public class App {

    public static void main(String[] args) {
        // Initialize DB Schema and Seed Admin User
        Database.initDatabase();
        SeedData.seedIfEmpty();

        // Create upload folders
        new File("uploads").mkdirs();
        new File("src/main/resources/public/uploads").mkdirs();

        // Configure Pebble Template Engine
        ClasspathLoader loader = new ClasspathLoader();
        loader.setPrefix("templates/");
        PebbleEngine pebbleEngine = new PebbleEngine.Builder()
                .loader(loader)
                .cacheActive(false)
                .build();

        FileRenderer pebbleRenderer = (filePath, model, ctx) -> {
            StringWriter writer = new StringWriter();
            try {
                String templatePath = filePath;
                if (templatePath.startsWith("/")) {
                    templatePath = templatePath.substring(1);
                }
                if (templatePath.startsWith("templates/")) {
                    templatePath = templatePath.substring("templates/".length());
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> mapModel = (Map<String, Object>) model;
                pebbleEngine.getTemplate(templatePath).evaluate(writer, mapModel);
                return writer.toString();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error rendering template " + filePath, e);
            }
        };

        Javalin app = Javalin.create(config -> {
            // Serve static files from resources /public
            config.staticFiles.add("/public", Location.CLASSPATH);
            // Also serve uploaded files from external uploads folder
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/uploads";
                staticFiles.directory = "uploads";
                staticFiles.location = Location.EXTERNAL;
            });
            // Register Pebble renderer
            config.fileRenderer(pebbleRenderer);
        });

        // Error Handlers
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.render("500.html", SecurityUtil.baseModel(ctx));
        });

        app.error(404, ctx -> {
            ctx.render("404.html", SecurityUtil.baseModel(ctx));
        });

        app.error(403, ctx -> {
            ctx.render("403.html", SecurityUtil.baseModel(ctx));
        });

        // Routes Mapping
        // 1. Auth Routes
        app.get("/register", AuthController::renderRegister);
        app.post("/register", AuthController::handleRegister);
        app.get("/login", AuthController::renderLogin);
        app.post("/login", AuthController::handleLogin);
        app.get("/logout", AuthController::handleLogout);

        // 2. Main Routes
        app.get("/", MainController::renderIndex);
        app.get("/dashboard", MainController::renderDashboard);
        app.get("/browse", MainController::renderBrowse);
        app.get("/profile", MainController::renderProfile);
        app.get("/profile/edit", MainController::renderEditProfile);
        app.post("/profile/edit", MainController::handleEditProfile);

        // 3. Item Routes
        app.get("/items/my-items", ItemController::renderMyItems);
        app.get("/items/create", ItemController::renderCreate);
        app.post("/items/create", ItemController::handleCreate);
        app.get("/items/{id}", ItemController::renderDetails);
        app.get("/items/{id}/edit", ItemController::renderEdit);
        app.post("/items/{id}/edit", ItemController::handleEdit);
        app.post("/items/{id}/delete", ItemController::handleDelete);

        // 4. Claim Routes
        app.get("/claims/create/{itemId}", ClaimController::renderCreate);
        app.post("/claims/create/{itemId}", ClaimController::handleCreate);
        app.get("/claims/my-claims", ClaimController::renderMyClaims);
        app.get("/claims/{id}", ClaimController::renderDetails);

        // 5. Admin Routes
        app.get("/admin", AdminController::renderDashboard);
        app.get("/admin/users", AdminController::renderUsers);
        app.post("/admin/users/{id}/toggle-status", AdminController::handleToggleUserStatus);
        app.post("/admin/users/{id}/change-role", AdminController::handleChangeUserRole);
        app.get("/admin/items", AdminController::renderItems);
        app.post("/admin/items/{id}/close", AdminController::handleCloseItem);
        app.post("/admin/items/{id}/return", AdminController::handleMarkReturned);
        app.get("/admin/claims", AdminController::renderClaims);
        app.post("/admin/claims/{id}/approve", AdminController::handleApproveClaim);
        app.post("/admin/claims/{id}/reject", AdminController::handleRejectClaim);
        app.get("/admin/reports", AdminController::renderReports);

        // 6. Notification Routes
        app.get("/notifications", NotificationController::renderNotifications);
        app.post("/notifications/read/{id}", NotificationController::handleMarkRead);
        app.post("/notifications/read-all", NotificationController::handleMarkAllRead);

        int port = 5000;
        app.start(port);
        System.out.println("RECLAIMX Javalin Server running at http://localhost:" + port);
    }
}
