package org.liondevelopers.resybot;

class Main {

    public static void main(String[] args) {
        int port = Integer.parseInt(Config.get("PORT", "8080"));

        MonitorManager manager = new MonitorManager();
        WebServer server = new WebServer(manager);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            manager.shutdown();
            server.stop();
        }));

        server.start("0.0.0.0", port);
    }
}
