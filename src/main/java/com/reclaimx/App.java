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
        //This lines initiate DB Schema
 Database.initDatabase();
SeedData.seedIfEmpty();

        // This line create Upload folder
new File("uploads").mkdirs();
new File("src/main/resources/public/uploads").mkdirs();

        // here is the configuration of pebble engine
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
            //static files from server ya upload folder
 config.staticFiles.add("/public", Location.CLASSPATH);
            // this is for uploaded files from external source
 config.staticFiles.add(staticFiles -> {
staticFiles.hostedPath = "/uploads";
staticFiles.directory = "uploads";
staticFiles.location = Location.EXTERNAL;
});
            // register pebble rendered
config.fileRenderer(pebbleRenderer);
});

        // This is for error handling 
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

        // Routes for the project 
        //Auth routess
app.get("/register", AuthController::renderRegister);
app.post("/register", AuthController::handleRegister);
    app.get("/login", AuthController::renderLogin);
 app.post("/login", AuthController::handleLogin);
    app.get("/logout", AuthController::handleLogout);

        // Main routes
 app.get("/", MainController::renderIndex);
    app.get("/dashboard", MainController::renderDashboard);
app.get("/browse", MainController::renderBrowse);
    app.get("/profile", MainController::renderProfile);
    app.get("/profile/edit", MainController::renderEditProfile);
     app.post("/profile/edit", MainController::handleEditProfile);

        // Lost or found lost items
    app.get("/items/my-items", ItemController::renderMyItems);
 app.get("/items/create", ItemController::renderCreate);
    app.post("/items/create", ItemController::handleCreate);
    app.get("/items/{id}", ItemController::renderDetails);
  app.get("/items/{id}/edit", ItemController::renderEdit);
    app.post("/items/{id}/edit", ItemController::handleEdit);
    app.post("/items/{id}/delete", ItemController::handleDelete);

        //Item claim routes
 app.get("/claims/create/{itemId}", ClaimController::renderCreate);
     app.post("/claims/create/{itemId}", ClaimController::handleCreate);
app.get("/claims/my-claims", ClaimController::renderMyClaims);
app.get("/claims/{id}", ClaimController::renderDetails);

        // Admin routes for project
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

        // notification routes 
  app.get("/notifications", NotificationController::renderNotifications);
  app.post("/notifications/read/{id}", NotificationController::handleMarkRead);
app.post("/notifications/read-all", NotificationController::handleMarkAllRead);

int port = 5000;
app.start(port);
System.out.println("RECLAIMX Javalin Server running at http://localhost:" + port);
    }
}
