/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.plugins

import android.content.Context
import android.util.Log
import cx.ring.fragments.CallFragment
import cx.ring.settings.pluginssettings.PluginDetails
import net.jami.daemon.JamiService
import java.io.File
import java.lang.StringBuilder
import java.util.*

object PluginUtils {
    val TAG = PluginUtils::class.simpleName!!

    /**
     * Fetches the plugins folder in the internal storage for plugins subfolder
     * Gathers the details of each plugin in a PluginDetails instance
     * @param mContext The current context
     * @return List of PluginDetails
     */
    fun getInstalledPlugins(mContext: Context): List<PluginDetails> {
        //tree(mContext.filesDir.toString() + File.separator + "plugins", 0)
        //tree(mContext.cacheDir.absolutePath, 0)
        val pluginsPaths: List<String> = JamiService.getInstalledPlugins()
        val loadedPluginsPaths: List<String> = JamiService.getLoadedPlugins()
        val pluginsList: MutableList<PluginDetails> = ArrayList(pluginsPaths.size)
        for (pluginPath in pluginsPaths) {
            val pluginFolder = File(pluginPath)
            if (pluginFolder.isDirectory) {
                val pluginHandler = retrieveHandlerId(pluginFolder.name)
                pluginsList.add(
                    PluginDetails(
                        pluginFolder.name,
                        pluginFolder.absolutePath,
                        loadedPluginsPaths.contains(pluginPath),
                        pluginHandler
                    )
                )
            }
        }
        return pluginsList
    }

    private fun retrieveHandlerId(name: String): String{
        var res  = ""
        val mediaHandlers = JamiService.getCallMediaHandlers().toList()
        for (callMediaHandler in mediaHandlers) {
            val pDetail = JamiService.getCallMediaHandlerDetails(callMediaHandler)
            if (pDetail["pluginId"]!!.lowercase(Locale.getDefault()).contains(name.lowercase(Locale.getDefault())))
            {
                res = callMediaHandler!!
            }
        }
        return res
    }

    /**
     * Fetches the plugins folder in the internal storage for plugins subfolder
     * Gathers the details of each plugin in a PluginDetails instance
     * @param mContext The current context
     * @param accountId The current account id
     * @param peerId The current conversation peer id
     * @return List of PluginDetails
     */
    fun getChatHandlersDetails(mContext: Context, accountId: String, peerId: String): List<PluginDetails> {
        //tree(mContext.filesDir.toString() + File.separator + "plugins", 0)
        //tree(mContext.cacheDir.absolutePath, 0)
        val chatHandlersId: List<String> = JamiService.getChatHandlers()
        val chatHandlerStatus: List<String> = JamiService.getChatHandlerStatus(accountId, peerId)
        val handlersList: MutableList<PluginDetails> = ArrayList(chatHandlersId.size)
        for (handlerId in chatHandlersId) {
            val handlerDetails = JamiService.getChatHandlerDetails(handlerId)
            var pluginPath = handlerDetails["pluginId"]
            pluginPath = pluginPath!!.substring(0, pluginPath.lastIndexOf("/data"))
            var enabled = false
            if (chatHandlerStatus.contains(handlerId)) {
                enabled = true
            }
            handlersList.add(PluginDetails(handlerDetails["name"]!!, pluginPath, enabled, handlerId))
        }
        return handlersList
    }

    /**
     * Loads the so file and instantiates the plugin init function (toggle on)
     * @param path root path of the plugin
     * @return true if loaded
     */
    fun loadPlugin(path: String): Boolean {
        return JamiService.loadPlugin(path)
    }

    /**
     * Toggles the plugin off (destroying any objects created by the plugin)
     * then unloads the so file
     * @param path root path of the plugin
     * @return true if unloaded
     */
    fun unloadPlugin(path: String): Boolean {
        return JamiService.unloadPlugin(path)
    }

    /**
     * Displays the content of any directory
     * @param dirPath directory to display
     * @param level default 0, exists because the function is recursive
     */
    fun tree(dirPath: String, level: Int) {
        val repeated = String(CharArray(level)).replace("\u0000", "\t|")
        val file = File(dirPath)
        if (file.exists()) {
            Log.d(TAG, "|" + repeated + "-- " + file.name)
            if (file.isDirectory) {
                val files = file.listFiles()
                if (files != null && files.isNotEmpty()) {
                    for (f in files) {
                        tree(f.absolutePath, level + 1)
                    }
                }
            }
        }
    }

    /**
     * Converts a string that contains a list to a List<String>
     * E.g: String entries = "[AAA,BBB,CCC]" to List<String> l, where l.get(0) = "AAA"
     * @return List of strings
     * @param stringList a string in the form "[AAA,BBB,CCC]"
    </String></String> */
    fun stringListToListString(stringList: String): List<String> {
        val listString: MutableList<String> = ArrayList()
        val currentWord = StringBuilder()
        if (stringList.isNotEmpty()) {
            for (i in stringList.indices) {
                val currentChar = stringList[i]
                if (currentChar != ',') {
                    currentWord.append(currentChar)
                } else {
                    listString.add(currentWord.toString())
                    currentWord.clear()
                }
                if (i == stringList.length - 1) {
                    listString.add(currentWord.toString())
                    break
                }
            }
        }
        return listString
    }
}