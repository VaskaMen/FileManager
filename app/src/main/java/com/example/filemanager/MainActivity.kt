package com.example.filemanager

import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.values
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage



val storageRef = Firebase.storage.reference
val dbRef = FirebaseDatabase.getInstance().reference
lateinit var context: Context
lateinit var globalContentResolver: ContentResolver
var passwordGlobal: String = ""
lateinit var fileData: ByteArray


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            globalContentResolver = contentResolver
            context = applicationContext

            var fileList = remember{ mutableStateListOf<SelectedFile>()}

            var password by remember {
                mutableStateOf("")
            }

            var fileSelected: SelectedFile by remember {
                mutableStateOf(SelectedFile())
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
                    val fileID = dbRef.push().key.toString()
                    val name = cursor.getString(fileName)
                    val type = cursor.getString(fileType)
                    val size = cursor.getString(fileSize)
                    val path = "${passwordGlobal}/${name}"
                    fileSelected = SelectedFile(fileID,name, type, size, path)
                }

                fileData = contentResolver.openInputStream(it).use {
                    it!!.readBytes()
                }
            }

            getFilesFromStorage(
                callback =  {
                    val snapFile = it.getValue(SelectedFile::class.java)!!

                    if(passwordGlobal != ""){
                        if (snapFile !in fileList.toList() && snapFile.size.isEmpty().not()){
                            fileList.add(snapFile)
                        }
                    }
                },
                callbackDelete = {
                    val snapFile = it.getValue(SelectedFile::class.java)!!

                    if(passwordGlobal != ""){
                        if (snapFile in fileList.toList() && snapFile.size.isEmpty().not()){
                            fileList.remove(snapFile)
                        }
                    }
                },
                callbackChanged = {
                    val snapFile = it.getValue(SelectedFile::class.java)!!
//                    val fileListTemp = fileList.toMutableList()
//                    fileListTemp.forEach{
//                        if (it.fileID == snapFile.fileID){
//                            Toast.makeText(this@MainActivity, "${snapFile.fileID} + ${snapFile.loaded}", Toast.LENGTH_SHORT).show()
//                        }
//                    }
                    for(ff in fileList.toMutableList()){
                        if (ff.fileID == snapFile.fileID){
                            fileList.set(fileList.indexOf(ff), snapFile)
                            break
                        }
                    }

                }
                )

            Column {
                TextField(value = password, onValueChange ={
                    password=it
                    passwordGlobal = it
                    fileList.clear()
                } )

                Button(onClick = {
                    launcherSelect.launch("*/*")
                }) {
                    Text(text = "Выбрать файл")
                }
                
                Button(onClick = {
                    if (passwordGlobal != "")
                        sendFile(fileData, fileSelected)
                    else
                        Toast.makeText(this@MainActivity, "Необходми указать путь сохранения", Toast.LENGTH_SHORT).show()
                }) {
                    Text(text = "Отправить")
                }



                LazyColumn{
                    items(fileList) {
                        FileCard(it){
                            fileSelected = it
                        }
                    }
                }



            }
        }
    }
}




data class SelectedFile(
    val fileID: String = "",
    val name: String = "",
    val type: String = "",
    val size: String = "",
    val path: String = "",
    var loaded: Boolean = false
)