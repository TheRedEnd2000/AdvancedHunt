package de.theredend2000.advancedegghunt.util.embed;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class EmbedCreator {

    private Main plugin;

    public EmbedCreator(){
        plugin = Main.getInstance();
    }

    public String getExportEmbedContent(Player player, String preset){
        InetAddress IP = null;
        try {
            IP = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }


        String serverIP = IP.getHostAddress();
        String playerName = player.getName();
        String date = plugin.getDatetimeUtils().getNowDate();
        String time = plugin.getDatetimeUtils().getNowTime();

        int presetUploads = 10;
        int todayPresets = 1;
        boolean linked = true;
        String discordName = "NONE";

        int commands = 2;
        String name = preset;
        String type = "Individual";

        String field1 = "Server-IP: "+serverIP + "\\u000A" +
                        "Player: "+playerName + "\\u000A" +
                        "Date: "+date + "\\u000A" +
                        "Time: "+time + "\\u000A" +
                        "";
        String field2 = "Presets Uploaded: "+presetUploads+ "\\u000A" +
                        "Today: "+todayPresets+ "\\u000A" +
                        "Linked account: "+linked + "\\u000A" +
                        "Discord: "+discordName+ "\\u000A" +
                        "";
        String field3 = "Commands: "+commands+ "\\u000A" +
                        "Name: "+name+ "\\u000A" +
                        "Type: "+type + "\\u000A" +
                        "";

        String embedContent = "{\n" +
                "  \"content\": \"\",\n" +
                "  \"embeds\": [\n" +
                "    {\n" +
                "      \"title\": \"NEW PRESET UPLOAD\",\n" +
                "      \"description\": \"A new Preset was uploaded by " + playerName + "\",\n" +
                "      \"color\": 65280,\n" +
                "      \"fields\": [\n" +
                "        {\n" +
                "          \"name\": \"Information\",\n" +
                "          \"value\": \""+ field1 +"\",\n" +
                "          \"inline\": true\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"User\",\n" +
                "          \"value\": \""+field2+"\",\n" +
                "          \"inline\": true\n" +
                "        },\n"+
                "        {\n" +
                "           \"name\":\"Preset\",\n" +
                "           \"value\": \""+field3+"\",\n" +
                "           \"inline\": true\n" +
                "         }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        return embedContent;
    }

}
