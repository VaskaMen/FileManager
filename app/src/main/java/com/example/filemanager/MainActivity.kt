package com.example.filemanager

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File


val storageRef = Firebase.storage.reference
val dbRef = Firebase
lateinit var context: Context
lateinit var globalContentResolver: ContentResolver
var passwordGlobal: String = ""
lateinit var fileSelected: SelectedFile
lateinit var fileData: ByteArray


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            globalContentResolver = contentResolver
            context = applicationContext

            var password by remember {
                mutableStateOf("")
            }


            val projection = arrayOf(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE
            )

//            Отправка файла
            val launcherSelect = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){ it ->
                contentResolver.query(it!!, projection, null, null)?.use { cursor ->
                    val fileName = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val fileType = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                    val fileSize = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)

                    cursor.moveToFirst()
                    val name = cursor.getString(fileName)
                    val type = cursor.getString(fileType)
                    val size = cursor.getString(fileSize)

                    fileSelected = SelectedFile(name, type, size)
                }

                fileData = contentResolver.openInputStream(it).use {
                    it!!.readBytes()
                }


            }

            var path: Uri? = null
            val launcherOpen = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("applicatio/*")){ it ->
                path = it
            }


            Column {


                TextField(value = password, onValueChange ={
                    password=it
                    passwordGlobal = it
                } )

                Button(onClick = {
                    launcherSelect.launch("*/*")
                }) {
                    Text(text = "Выбрать файл")
                }
                
                Button(onClick = {
                    sendFile(fileData)
                }) {
                    Text(text = "Отправить")
                }

                Button(onClick = {
                    if (path == null)
                            launcherOpen.launch("FileName")
                    else
                        download(path!!, "${passwordGlobal}")
                }) {
                    Text(text = "Скачать")
                }
            }
        }
    }
}

fun download(path: Uri, pathRef: String){
    storageRef.child(pathRef).getBytes(1024*1024).addOnSuccessListener {byte ->
        globalContentResolver.openOutputStream(path).use {
            it!!.write(byte)
        }
    }.addOnFailureListener {
        Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show()
    }
}

fun sendFile(byteArray: ByteArray){
    storageRef.child(passwordGlobal).child(fileSelected.name).putBytes(byteArray)
}


data class SelectedFile(
    val name: String,
    val type: String,
    val size: String
)