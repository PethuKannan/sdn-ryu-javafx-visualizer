package com.sdnmanager;

import java.util.stream.Collectors;
import com.google.gson.*;
import okhttp3.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class TopologyFX extends Application {
    private static final String BASE = System.getProperty(
            "ryu.url",
            System.getenv().getOrDefault("RYU_URL", "http://127.0.0.1:8080")
    );

    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();

    private final Pane canvas = new Pane();
    private final Text status = new Text();
    private ScheduledExecutorService exec;

    public static void main(String[] args) { launch(args); }

    @Override public void start(Stage stage) {
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> loadOnce());
        Button auto = new Button("Auto");
        auto.setOnAction(e -> toggleAuto(auto));

        ToolBar tb = new ToolBar(refresh, auto, status);
        BorderPane root = new BorderPane(canvas, tb, null, null, null);
        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("SDN Topology (JavaFX + Ryu)");
        stage.setScene(scene);
        stage.show();

        loadOnce();
    }

    private void toggleAuto(Button b) {
        if (exec == null) {
            exec = Executors.newSingleThreadScheduledExecutor();
            exec.scheduleAtFixedRate(this::loadOnce, 0, 3, TimeUnit.SECONDS);
            b.setText("Stop");
        } else {
            exec.shutdownNow();
            exec = null;
            b.setText("Auto");
        }
    }

    private void loadOnce() {
        CompletableFuture<List<String>> swF = CompletableFuture.supplyAsync(this::safeFetchSwitches);
        CompletableFuture<List<String[]>> lkF = CompletableFuture.supplyAsync(this::safeFetchLinks);
        swF.thenCombine(lkF, (dpids, links) -> {
            Platform.runLater(() -> draw(dpids, links));
            return null;
        }).exceptionally(ex -> {
            Platform.runLater(() -> status.setText("Error: " + ex.getMessage()));
            return null;
        });
    }

    private List<String> safeFetchSwitches() {
        try { return fetchSwitches(); } catch (Exception e) { return List.of(); }
    }
    private List<String[]> safeFetchLinks() {
        try { return fetchLinks(); } catch (Exception e) { return List.of(); }
    }

    private List<String> fetchSwitches() throws IOException {
        Request r = new Request.Builder().url(BASE + "/v1.0/topology/switches").build();
        try (Response resp = http.newCall(r).execute()) {
            String s = Objects.requireNonNull(resp.body()).string();
            JsonArray arr = JsonParser.parseString(s).getAsJsonArray();
            List<String> out = new ArrayList<>();
            for (JsonElement e : arr) out.add(e.getAsJsonObject().get("dpid").getAsString());
            return out;
        }
    }

    private List<String[]> fetchLinks() throws IOException {
        Request r = new Request.Builder().url(BASE + "/v1.0/topology/links").build();
        try (Response resp = http.newCall(r).execute()) {
            String s = Objects.requireNonNull(resp.body()).string();
            JsonArray arr = JsonParser.parseString(s).getAsJsonArray();
            List<String[]> out = new ArrayList<>();
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                String a = o.getAsJsonObject("src").get("dpid").getAsString();
                String b = o.getAsJsonObject("dst").get("dpid").getAsString();
                if (!a.equals(b)) out.add(new String[]{a, b});
            }
            return out;
        }
    }

	private List<Host> safeFetchHosts() {
    try { return fetchHosts(); } catch (Exception e) { return List.of(); }
}

private List<Host> fetchHosts() throws IOException {
    Request r = new Request.Builder().url(BASE + "/v1.0/topology/hosts").build();
    try (Response resp = http.newCall(r).execute()) {
        String s = Objects.requireNonNull(resp.body()).string();
        JsonArray arr = JsonParser.parseString(s).getAsJsonArray();
        List<Host> hosts = new ArrayList<>();
        int i = 1;
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            Host h = new Host();
            h.name = "h" + i++;
            h.mac = o.get("mac").getAsString();
            h.ipv4 = o.getAsJsonArray("ipv4").size() > 0 ? o.getAsJsonArray("ipv4").get(0).getAsString() : "";
            h.switchDpid = o.getAsJsonObject("port").get("dpid").getAsString();
            hosts.add(h);
        }
        return hosts;
    }
}


	static class Host {
    	String name, mac, ipv4, switchDpid;
	}

    private void draw(List<String> dpids, List<String[]> links) {
    canvas.getChildren().clear();
    double w = canvas.getWidth() == 0 ? 900 : canvas.getWidth();
    double h = canvas.getHeight() == 0 ? 600 : canvas.getHeight();
    double radius = Math.max(120, Math.min(w, h) * 0.35);
    double cx = w / 2, cy = h / 2;

    List<String> nodes = new ArrayList<>(dpids);
    Collections.sort(nodes);

    Map<String, double[]> pos = new HashMap<>();
    int n = Math.max(1, nodes.size());
    for (int i = 0; i < nodes.size(); i++) {
        double ang = 2 * Math.PI * i / n;
        pos.put(nodes.get(i), new double[]{cx + radius * Math.cos(ang), cy + radius * Math.sin(ang)});
    }

    // Switch-switch links
    for (String[] e : links) {
        double[] a = pos.get(e[0]), b = pos.get(e[1]);
        if (a != null && b != null) {
            Line line = new Line(a[0], a[1], b[0], b[1]);
            line.setStrokeWidth(2);
            line.setStroke(Color.GRAY);
            canvas.getChildren().add(line);
        }
    }

    // Switch nodes
    for (String dpid : nodes) {
        double[] p = pos.get(dpid);
        Circle c = new Circle(p[0], p[1], 18, Color.web("#6aa9ff"));
        c.setStroke(Color.web("#1f3b73"));
        Text t = new Text(p[0] - 20, p[1] - 24, shortId(dpid));
        canvas.getChildren().addAll(c, t);
    }

    // Fetch and render hosts
    List<Host> hosts = safeFetchHosts();
    for (Host hst : hosts) {
        double[] sp = pos.get(hst.switchDpid);
        if (sp == null) continue;
        // offset the host slightly from the switch
        double angle = Math.random() * 2 * Math.PI;
        double hx = sp[0] + 40 * Math.cos(angle);
        double hy = sp[1] + 40 * Math.sin(angle);
        Line link = new Line(sp[0], sp[1], hx, hy);
        link.setStroke(Color.LIGHTGREEN);
        Circle hc = new Circle(hx, hy, 8, Color.LIMEGREEN);
        hc.setStroke(Color.DARKGREEN);
        Text label = new Text(hx - 10, hy - 10, hst.name);
        canvas.getChildren().addAll(link, hc, label);
    }

    status.setText("Switches: " + dpids.size() +
            "  Links: " + links.size() +
            "  Hosts: " + hosts.size() +
            "  @ " + BASE);
}


    private String shortId(String dpid) {
        int L = dpid == null ? 0 : dpid.length();
        return L <= 4 ? dpid : dpid.substring(L - 4);
    }

    @Override public void stop() {
        if (exec != null) exec.shutdownNow();
    }
}
