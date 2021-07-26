package cx.ring.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cx.ring.settings.pluginssettings.PluginDetails;

import net.jami.daemon.JamiService;
import net.jami.daemon.StringMap;
import net.jami.utils.Log;

public class PluginUtils {

    public static final String TAG = PluginUtils.class.getSimpleName();

    /**
     * Fetches the plugins folder in the internal storage for plugins subfolder
     * Gathers the details of each plugin in a PluginDetails instance
     * @param mContext The current context
     * @return List of PluginDetails
     */
    public static List<PluginDetails> getInstalledPlugins(Context mContext){
        tree(mContext.getFilesDir() + File.separator+ "plugins",0);
        tree(mContext.getCacheDir().getAbsolutePath(),0);

        List<String> pluginsPaths = JamiService.getInstalledPlugins();
        List<String> loadedPluginsPaths = JamiService.getLoadedPlugins();

        List<PluginDetails> pluginsList = new ArrayList<>(pluginsPaths.size());
        for (String pluginPath : pluginsPaths) {
            File pluginFolder = new File(pluginPath);
            if(pluginFolder.isDirectory()) {
                pluginsList.add(new PluginDetails(
                        pluginFolder.getName(),
                        pluginFolder.getAbsolutePath(), loadedPluginsPaths.contains(pluginPath)));
            }
        }
        return pluginsList;
    }

    /**
     * Fetches the plugins folder in the internal storage for plugins subfolder
     * Gathers the details of each plugin in a PluginDetails instance
     * @param mContext The current context
     * @param accountId The current account id
     * @param peerId The current conversation peer id
     * @return List of PluginDetails
     */
    public static List<PluginDetails> getChatHandlersDetails(Context mContext, String accountId, String peerId){
        tree(mContext.getFilesDir() + File.separator+ "plugins",0);
        tree(mContext.getCacheDir().getAbsolutePath(),0);

        List<String> chatHandlersId = JamiService.getChatHandlers();
        List<String> chatHandlerStatus = JamiService.getChatHandlerStatus(accountId, peerId);

        List<PluginDetails> handlersList = new ArrayList<>(chatHandlersId.size());
        for (String handlerId : chatHandlersId) {
            StringMap handlerDetails = JamiService.getChatHandlerDetails(handlerId);
            String pluginPath = handlerDetails.get("pluginId");
            pluginPath = pluginPath.substring(0, pluginPath.lastIndexOf("/data"));
            boolean enabled = false;

            if (chatHandlerStatus.contains(handlerId)) {
                enabled = true;
            }
            handlersList.add(new PluginDetails(
                    handlerDetails.get("name"),
                    pluginPath, enabled, handlerId));
        }
        return handlersList;
    }

    /**
     * Loads the so file and instantiates the plugin init function (toggle on)
     * @param path root path of the plugin
     * @return true if loaded
     */
    public static boolean loadPlugin(String path) {
        return JamiService.loadPlugin(path);
    }

    /**
     * Toggles the plugin off (destroying any objects created by the plugin)
     * then unloads the so file
     * @param path root path of the plugin
     * @return true if unloaded
     */
    public static boolean unloadPlugin(String path) {
        return JamiService.unloadPlugin(path);
    }

    /**
     * Lists the root paths of the loaded plugins
     * @return list of path
     */
    public static List<String> getLoadedPlugins() {
        return JamiService.getLoadedPlugins();
    }

    /**
     * Displays the content of any directory
     * @param dirPath directory to display
     * @param level default 0, exists because the function is recursive
     */
    public static void tree(String dirPath,  int level) {
        String repeated = new String(new char[level]).replace("\0", "\t|");
        File file = new File(dirPath);
        if(file.exists()) {
            Log.d(TAG, "|"+ repeated + "-- " + file.getName());
            if(file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null && files.length > 0) {
                    for(File f : files) {
                        tree(f.getAbsolutePath(),level+1);
                    }
                }
            }
        }
    }

    /**
     * Useful Util method that is available in for android api >=  24
     * We emulate it here
     *
     * @param input        input object that can be null
     * @param defaultValue default NonNull object of the same type as input
     * @return input if not null, defaultValue otherwise
     */
    public static <T> T getOrElse(T input, @NonNull T defaultValue) {
        if (input == null) {
            return defaultValue;
        } else {
            return input;
        }
    }

    /**
     *
     * @param listString List<String>
     * @return String of the form String entries = "[AAA,BBB,CCC]"
     */
    public static String listStringToStringList(List<String> listString) {
        StringBuilder stringBuilder = new StringBuilder();

        if(!listString.isEmpty()) {
            for(int i=0; i< listString.size(); i++){
                stringBuilder.append(listString.get(i)).append(",");
            }
            stringBuilder.append(listString.get(listString.size()));
        }

        return stringBuilder.toString();
    }

    /**
     * Converts a string that contains a list to a java List<String>
     * E.g: String entries = "[AAA,BBB,CCC]" to List<String> l, where l.get(0) = "AAA"
     * @return List of strings
     * @param stringList a string in the form "[AAA,BBB,CCC]"
     */
    public static List<String> stringListToListString(String stringList) {
        List<String> listString = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();
        if(!stringList.isEmpty()) {
            for (int i = 0; i < stringList.length(); i++) {
                char currentChar = stringList.charAt(i);
                if (currentChar != ',') {
                    currentWord.append(currentChar);
                } else {
                    listString.add(currentWord.toString());
                    currentWord = new StringBuilder();
                }

                if (i == stringList.length() - 1) {
                    listString.add(currentWord.toString());
                    break;
                }
            }
        }
        return listString;
    }
}
