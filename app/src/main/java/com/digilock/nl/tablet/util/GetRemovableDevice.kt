package com.digilock.nl.tablet.util

import io.reactivex.Observable

import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.util.ArrayList
import java.util.HashSet

/**
 * Created by mark.zhang on 2/15/2016.
 */
fun getRemovableDeviceDirs(): List<String> {
    var tempFile: File
    var splits: Array<String>
    var bufferedReader: BufferedReader? = null
    val arrayList = ArrayList<String>()
    var bHasSDDevice: Boolean

    try {
        arrayList.clear()
        bufferedReader = BufferedReader(FileReader("/proc/mounts"))
        val readLines = bufferedReader.readLines()
        readLines.forEach() {lineRead ->
            splits = lineRead.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            bHasSDDevice = false
            if (splits[0].contains("/dev/block/")) {
                bHasSDDevice = true
                if (splits[0].contains("/dev/block/mtdblock")) {
                    bHasSDDevice = false
                } else if (splits[1].contains("/mnt")) {
                    if (splits[1].contains("/secure"))  bHasSDDevice = false
                    if (splits[1].contains("/mnt/asec"))    bHasSDDevice = false
                } else {
                    bHasSDDevice = false
                }
            } else if (splits[0].contains("/dev/fuse")) {
                if (splits[1].contains("/storage/sdcard")) {
                    bHasSDDevice = true
                } else if (splits[1].contains("/storage/extsd")) {
                    bHasSDDevice = true
                } else if (splits[1].contains("/storage/6263-3532")) {
                    bHasSDDevice = true
                }
            } else if (splits[0].contains("/mnt/media_rw/6263-3532")) {
                if (splits[1].contains("/storage/6263-3532")) {
                    bHasSDDevice = true
                }
            }

            if (bHasSDDevice) {
                tempFile = File(splits[1])

                val test0 = tempFile.exists()
                val test1 = tempFile.isDirectory()
                val test2 = tempFile.canRead()
                val test3 = tempFile.canWrite()

                if (tempFile.exists() && tempFile.isDirectory() && tempFile.canRead() && tempFile.canWrite()) {
                    arrayList.add(splits[1])
                }
            }
        }
    } catch (e: FileNotFoundException) {
    } catch (e: IOException) {
    } finally {
        if (bufferedReader != null) {
            try {
                bufferedReader.close()
            } catch (e: IOException) {
            }

        }
    }

    val hs = HashSet<String>()
    hs.addAll(arrayList)
    arrayList.clear()
    arrayList.addAll(hs)

    return arrayList
}



