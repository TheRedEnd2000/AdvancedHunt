package de.theredend2000.advancedhunt.util.updater.source;

import de.theredend2000.advancedhunt.util.updater.UpdateSource;
import org.bukkit.Bukkit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class BukkitSource implements UpdateSource {

    private static final String RSS_URL = "https://dev.bukkit.org/projects/%s/files.rss";

    @Override
    public CompletableFuture<String> getLatestVersion(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(String.format(RSS_URL, id));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "AdvancedHunt/Updater");

                if (connection.getResponseCode() == 200) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(connection.getInputStream());
                    
                    NodeList items = doc.getElementsByTagName("item");
                    for (int i = 0; i < items.getLength(); i++) {
                        Element item = (Element) items.item(i);
                        String title = item.getElementsByTagName("title").item(0).getTextContent();
                        
                        if (!isUnstable(title)) {
                            return cleanVersion(title);
                        }
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to check Bukkit updates", e);
            }
            return null;
        });
    }

    private String cleanVersion(String version) {
        // Remove "v" prefix if present
        if (version.toLowerCase().startsWith("v") && version.length() > 1 && Character.isDigit(version.charAt(1))) {
            version = version.substring(1);
        }
        
        // If it contains spaces, take the last part if it looks like a version
        if (version.contains(" ")) {
            String[] parts = version.split(" ");
            String last = parts[parts.length - 1];
            if (Character.isDigit(last.charAt(0)) || (last.toLowerCase().startsWith("v") && last.length() > 1 && Character.isDigit(last.charAt(1)))) {
                return cleanVersion(last);
            }
        }
        return version;
    }

    private boolean isUnstable(String version) {
        String v = version.toLowerCase();
        return v.contains("alpha") || v.contains("beta") || v.contains("snapshot") || v.contains("rc");
    }

    @Override
    public CompletableFuture<Boolean> downloadPlugin(String id, String version, File destination) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Parse RSS again to find the link
                URL url = new URL(String.format(RSS_URL, id));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "AdvancedHunt/Updater");

                if (connection.getResponseCode() == 200) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(connection.getInputStream());
                    
                    NodeList items = doc.getElementsByTagName("item");
                    for (int i = 0; i < items.getLength(); i++) {
                        Element item = (Element) items.item(i);
                        String title = item.getElementsByTagName("title").item(0).getTextContent();
                        
                        if (title.equals(version)) {
                            String link = item.getElementsByTagName("link").item(0).getTextContent();
                            // The link in RSS is the page link, not the direct download link.
                            // Usually: https://dev.bukkit.org/projects/plugin-name/files/12345
                            // The download link is usually .../download
                            
                            return downloadFile(link + "/download", destination);
                        }
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to download from Bukkit", e);
            }
            return false;
        });
    }

    private boolean downloadFile(String urlString, File destination) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "AdvancedHunt/Updater");
            
            // Handle redirects (Bukkit/Curse often redirects)
            connection.setInstanceFollowRedirects(true);
            
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getName() {
        return "Bukkit";
    }
}
