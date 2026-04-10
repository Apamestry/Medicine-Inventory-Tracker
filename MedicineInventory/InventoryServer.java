package MedicineInventory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class InventoryServer {

    private static final InventoryManager inventory = new InventoryManager();
    private static final int PORT = 8082;   // was 8081

    public static void main(String[] args) throws IOException {
        TestDataLoader.loadTestData(inventory);
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/medicines", new MedicinesHandler());
        server.createContext("/api/medicines/search", new SearchHandler());
        server.createContext("/api/medicines/quantity", new QuantityHandler());
        server.createContext("/api/expiring-soon", new ExpiringSoonHandler());
        server.createContext("/api/low-stock", new LowStockHandler());
        server.createContext("/api/alerts", new AlertsHandler());
        server.createContext("/api/stats", new StatsHandler());

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("Medicine Inventory backend running at http://localhost:" + PORT + "/api");
    }

    private static class MedicinesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if (isOptions(exchange)) {
                sendResponse(exchange, 204, "");
                return;
            }

            String method = exchange.getRequestMethod();
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    List<Medicine> list = inventory.getAllMedicinesSortedByExpiry();
                    sendJson(exchange, 200, toJson(list));
                    return;
                }
                if ("POST".equalsIgnoreCase(method)) {
                    String body = readRequestBody(exchange);
                    Map<String, String> data = parseJson(body);
                    inventory.addMedicine(
                            data.getOrDefault("name", ""),
                            data.getOrDefault("batchNo", ""),
                            Integer.parseInt(data.getOrDefault("quantity", "0")),
                            data.getOrDefault("expiry", ""));
                    sendJson(exchange, 201, "{\"message\":\"Medicine added\"}");
                    return;
                }
                if ("DELETE".equalsIgnoreCase(method)) {
                    String name = params.get("name");
                    inventory.removeMedicine(name == null ? "" : name);
                    sendJson(exchange, 200, "{\"message\":\"Medicine removed\"}");
                    return;
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage());
                return;
            }

            sendError(exchange, "Unsupported method");
        }
    }

    private static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if (isOptions(exchange)) {
                sendResponse(exchange, 204, "");
                return;
            }
            String name = parseQuery(exchange.getRequestURI().getQuery()).get("name");
            if (name == null || name.isBlank()) {
                sendError(exchange, "Missing name query parameter");
                return;
            }
            Medicine med = inventory.findMedicineByName(name);
            if (med == null) {
                sendJson(exchange, 200, "null");
            } else {
                sendJson(exchange, 200, med.toJson());
            }
        }
    }

    private static class QuantityHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if (isOptions(exchange)) {
                sendResponse(exchange, 204, "");
                return;
            }
            if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, "Only PUT is allowed for quantity updates");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseJson(body);
            String name = data.get("name");
            int quantity = Integer.parseInt(data.getOrDefault("quantity", "0"));
            inventory.updateQuantity(name == null ? "" : name, quantity);
            sendJson(exchange, 200, "{\"message\":\"Quantity updated\"}");
        }
    }

    private static class ExpiringSoonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if (isOptions(exchange)) {
                sendResponse(exchange, 204, "");
                return;
            }
            sendJson(exchange, 200, toJson(inventory.getExpiringSoonList()));
        }
    }

    private static class LowStockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if (isOptions(exchange)) {
                sendResponse(exchange, 204, "");
                return;
            }
            sendJson(exchange, 200, toJson(inventory.getLowStockList()));
        }
    }

    private static class AlertsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if (isOptions(exchange)) {
                sendResponse(exchange, 204, "");
                return;
            }
            Map<String, List<Medicine>> alerts = inventory.getAlertsDashboard();
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            builder.append("\"expired\":").append(toJson(alerts.get("expired"))).append(',');
            builder.append("\"expiringSoon\":").append(toJson(alerts.get("expiringSoon"))).append(',');
            builder.append("\"lowStock\":").append(toJson(alerts.get("lowStock")));
            builder.append('}');
            sendJson(exchange, 200, builder.toString());
        }
    }

    private static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if (isOptions(exchange)) {
                sendResponse(exchange, 204, "");
                return;
            }
            sendJson(exchange, 200, toJson(inventory.getInventoryStats()));
        }
    }

    private static boolean isOptions(HttpExchange exchange) {
        return "OPTIONS".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }

    private static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        sendResponse(exchange, status, json);
    }

    private static void sendError(HttpExchange exchange, String message) throws IOException {
        sendJson(exchange, 400, "{\"error\":\"" + escapeJson(message) + "\"}");
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = decode(pair.substring(0, idx));
                String value = decode(pair.substring(idx + 1));
                params.put(key, value);
            }
        }
        return params;
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> values = new HashMap<>();
        if (json == null || json.isBlank()) return values;
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }
        for (String part : splitJsonFields(json)) {
            int idx = part.indexOf(":");
            if (idx > 0) {
                String key = part.substring(0, idx).trim();
                String value = part.substring(idx + 1).trim();
                key = trimQuotes(key);
                value = trimQuotes(value);
                values.put(key, value);
            }
        }
        return values;
    }

    private static List<String> splitJsonFields(String source) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            }
            if (c == ',' && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts;
    }

    private static String trimQuotes(String text) {
        text = text.trim();
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static String decode(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }

    private static String toJson(List<Medicine> medicines) {
        List<String> entries = new ArrayList<>();
        for (Medicine med : medicines) {
            entries.add(med.toJson());
        }
        return "[" + String.join(",", entries) + "]";
    }

    private static String toJson(Map<String, Object> stats) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append('"').append(escapeJson(entry.getKey())).append('"');
            json.append(':');
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append('"').append(escapeJson(String.valueOf(value))).append('"');
            }
        }
        json.append('}');
        return json.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
