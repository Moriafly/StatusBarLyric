/*
 * StatusBarLyric
 * Copyright (C) 2021-2022 fkj@fkj233.cn
 * https://github.com/577fkj/StatusBarLyric
 *
 * This software is free opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by 577fkj.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/577fkj/StatusBarLyric/blob/main/LICENSE>.
 */

package statusbar.lyric.utils

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDateTime


object BackupUtils {
    const val CREATE_DOCUMENT_CODE = 255774
    const val OPEN_DOCUMENT_CODE = 277451

    private lateinit var sharedPreferences: SharedPreferences

    fun backup(activity: Activity, sp: SharedPreferences) {
        sharedPreferences = sp
        saveFile(activity, String.format("StatusBarLyric_%s.json", LocalDateTime.now().toString()))
    }

    fun recovery(activity: Activity, sp: SharedPreferences) {
        sharedPreferences = sp
        openFile(activity)
    }

    private fun openFile(activity: Activity) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        activity.startActivityForResult(intent, OPEN_DOCUMENT_CODE)
    }


    private fun saveFile(activity: Activity, fileName: String){
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        activity.startActivityForResult(intent, CREATE_DOCUMENT_CODE)
    }

    fun handleReadDocument(activity: Activity, data: Uri?) {
        val edit = sharedPreferences.edit()
        val uri = data ?: return
        try {
            activity.contentResolver.openInputStream(uri)?.let { loadFile ->
                BufferedReader(InputStreamReader(loadFile)).apply {
                    val sb = StringBuffer()
                    var line = readLine()
                    while(line != null){
                        sb.append(line)
                        line = readLine()
                    }
                    val read = sb.toString()
                    JSONObject(read).apply {
                        val key = keys()
                        while (key.hasNext()) {
                            val keys = key.next()
                            when (val value = get(keys)) {
                                is String -> {
                                    if (value.startsWith("Float:")) {
                                        edit.putFloat(keys, value.substring(value.indexOf("Float:")).toFloat() / 1000)
                                    } else {
                                        edit.putString(keys, value)
                                    }
                                }
                                is Boolean -> edit.putBoolean(keys, value)
                                is Int -> edit.putInt(keys, value)
                            }
                        }
                    }
                    close()
                }
            }
            edit.commit()
            ActivityUtils.showToastOnLooper(activity, "load ok")
        } catch (e: Throwable) {
            ActivityUtils.showToastOnLooper(activity, "load fail\n$e")
        }
    }

    fun handleCreateDocument(activity: Activity, data: Uri?) {
        val uri = data ?: return
        try {
            activity.contentResolver.openOutputStream(uri)?.let { saveFile ->
                BufferedWriter(OutputStreamWriter(saveFile)).apply {
                    write(
                        JSONObject().also {
                            for (entry: Map.Entry<String, *> in sharedPreferences.all) {
                                when (entry.value) {
                                    Float -> it.put(entry.key, "Float:" + (entry.value as Float * 1000).toInt().toString())
                                    else -> it.put(entry.key, entry.value)
                                }
                            }
                        }.toString()
                    )
                    close()
                }
            }
            ActivityUtils.showToastOnLooper(activity, "save ok")
        } catch (e: Throwable) {
            ActivityUtils.showToastOnLooper(activity, "save fail\n$e")
        }
    }
}