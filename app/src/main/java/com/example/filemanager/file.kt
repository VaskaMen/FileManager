package com.example.filemanager

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import java.text.DecimalFormat

data class SelectedFile(
    val fileID: String = "",
    val name: String = "",
    val type: String = "",
    val size: String = "",
    val path: String = "",
    var loaded: Boolean = false
)

@Preview(showBackground = true)
@Composable
fun FileCard(file: SelectedFile = SelectedFile("Name", "image/png", "8100", "zxcasca"),
             callback: () -> Unit = {}
){
    val df = DecimalFormat("#.##")
    val sizeKB = df.format((file.size.toInt()/(1024)).toBigDecimal())
    val sizeMB = df.format(file.size.toInt()/(1024 * 1024))
    val sizeFormoted = if(sizeMB.toInt() == 0) sizeKB+" KB" else sizeMB +" MB"

//    Файл загружен?
    var failIsDownload by remember {
        mutableStateOf(true)
    }
//    Выбор метста для файла и скачивание
    val launcherDownload = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
        if(file == null) "*/*" else file!!.type))
    { it ->
        failIsDownload = false
        Log.e("Info", "${file?.path}")
        download(it!!, "${file?.path}"){
            failIsDownload = true
        }
    }

    val fileIcon: Int = when(file.type){
        ("image/jpeg") -> R.drawable.baseline_image_24
        ("image/png") -> R.drawable.baseline_image_24
        else -> R.drawable.baseline_insert_drive_file_24
    }

    var fileLoaded = rememberUpdatedState(file.loaded)



    Column(modifier = Modifier
        .fillMaxWidth()
        .background(color = colorResource(R.color.lightBrown))
    )
    {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(id = fileIcon), contentDescription = "FileType", Modifier.size(70.dp))
            Column(
                modifier = Modifier.weight(4f)
            ){
                Text(text = file.name, fontSize = 15.sp)
                if (fileLoaded.value.not() || failIsDownload.not())
                    LinearProgressIndicator(
                        modifier = Modifier.height(7.dp),
                        color = Color(0x7ECCCCCC)
                    )
            }

            Column(  modifier = Modifier.weight(2.7f))
            {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.backGround)),
                    enabled = fileLoaded.value,
                    onClick = {
                        if (file.loaded)
                            launcherDownload.launch(file.name)
                        else
                            Log.e("Load", "Файл не загружен")
                    }) {

                    Text(text = "Скачать")
                }

                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF942E2E)),
                    onClick = {
                    deleteFile(file)
                }) {
                    Text(text = "Удалить")
                }
            }


        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(end = 30.dp),
            horizontalArrangement = Arrangement.End,

        ) {
            Text(text = file.type)
        }

        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(end = 30.dp),
            horizontalArrangement = Arrangement.End,

            ) {
            Text(text = sizeFormoted)
        }

        Divider()
    }
}


fun download(path: Uri, pathRef: String, onFileLoaded: () -> Unit){
    storageRef.child(pathRef).getBytes(1024*1024*1024*1).addOnSuccessListener {byte ->
        onFileLoaded()
        globalContentResolver.openOutputStream(path).use {
            it!!.write(byte)
        }
    }.addOnFailureListener {
        Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show()
    }
}

fun sendFile(byteArray: ByteArray, fileSelected: SelectedFile?){

    if (fileSelected == null)
        return

    dbRef.child(passwordGlobal).child(fileSelected.fileID).setValue(fileSelected)
    storageRef.child(passwordGlobal).child(fileSelected!!.name).putBytes(byteArray).addOnSuccessListener {
        dbRef.child(passwordGlobal).child(fileSelected.fileID).child("loaded").setValue(true)
    }
}

//Отслеживание состояния файла
fun getFilesFromStorage(callback: (DataSnapshot) -> Unit, callbackDelete: (DataSnapshot) -> Unit, callbackChanged: (DataSnapshot) -> Unit){
    dbRef.child(passwordGlobal).addChildEventListener(object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            callback(snapshot)
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            callbackChanged(snapshot)
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            callbackDelete(snapshot)
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onCancelled(error: DatabaseError) {

        }

    })
}

fun deleteFile(fileSelected: SelectedFile){
    storageRef.child(fileSelected.path).delete()
    dbRef.child(passwordGlobal).child(fileSelected.fileID).removeValue()
}

@Preview(showBackground = true)
@Composable
fun PreviewFile(selectedFile: SelectedFile = SelectedFile(name = "Name", fileID = "sd", type = "image/png", size = "23424", path = "fdsf", loaded = false)){
    val df = DecimalFormat("#.##")
    val sizeKB = df.format((selectedFile.size.toInt()/(1024)).toBigDecimal())
    val sizeMB = df.format(selectedFile.size.toInt()/(1024 * 1024))
    val sizeFormoted = if(sizeMB.toInt() == 0) sizeKB+" KB" else sizeMB +" MB"

    val fileIcon: Int = when(selectedFile.type){
        ("image/jpeg") -> R.drawable.baseline_image_24
        ("image/png") -> R.drawable.baseline_image_24
        else -> R.drawable.baseline_insert_drive_file_24
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 70.dp)
            .background(color = colorResource(R.color.lightBrown)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(id = fileIcon), contentDescription = "FileType", Modifier.size(90.dp))
        Text(text = selectedFile.name, fontSize = 20.sp)
        Text(text = sizeFormoted)
    }
}
