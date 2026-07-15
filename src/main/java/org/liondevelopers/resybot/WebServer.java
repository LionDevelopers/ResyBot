package org.liondevelopers.resybot;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

/**
 * Embedded Javalin server exposing the monitoring REST API and serving the
 * static frontend from the classpath (src/main/resources/public).
 */
public class WebServer {

    private static final String JSON = "application/json";

    private final MonitorManager manager;
    private Javalin app;

    public WebServer(MonitorManager manager) {
        this.manager = manager;
    }

    public void start(String host, int port) {
        app = Javalin.create(config ->
                config.staticFiles.add(staticFiles -> {
                    staticFiles.directory = "/public";
                    staticFiles.location = Location.CLASSPATH;
                }));

        // Start a new background monitoring job.
        app.post("/api/monitors", ctx -> {
            try {
                MonitorRequest request = ApiJson.parseMonitorRequest(ctx.body());
                String id = manager.start(request);
                ctx.status(201).contentType(JSON).result(ApiJson.jobToJson(manager.get(id)));
            } catch (IllegalArgumentException e) {
                ctx.status(400).contentType(JSON).result(ApiJson.error(e.getMessage()));
            }
        });

        // List active monitoring jobs and their status.
        app.get("/api/monitors", ctx ->
                ctx.contentType(JSON).result(ApiJson.jobsToJson(manager.list())));

        // Stop and remove a monitoring job.
        app.delete("/api/monitors/{id}", ctx -> {
            boolean removed = manager.stop(ctx.pathParam("id"));
            if (removed) {
                ctx.status(204);
            } else {
                ctx.status(404).contentType(JSON).result(ApiJson.error("No such job: " + ctx.pathParam("id")));
            }
        });

        app.start(host, port);
        System.out.println("ResyBot server listening on http://" + host + ":" + port);
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }
}
