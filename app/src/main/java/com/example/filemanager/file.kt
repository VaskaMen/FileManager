package com.example.filemanager

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import java.text.DecimalFormat

@Preview(showBackground = true)
@Composable
fun FileCard(file: SelectedFile = SelectedFile("Name", "image/png", "8100", "zxcasca"),
             callback: () -> Unit = {}
){
    val df = DecimalFormat("#.##")
    val sizeKB = df.format((file.size.toInt()/(1024)).toBigDecimal())
    val sizeMB = df.format(file.size.toInt()/(1024 * 1024))
    val sizeFormoted = if(sizeMB.toInt() == 0) sizeKB+" KB" else sizeMB +" MB"

    val launcherDownload = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
        if(file == null) "*/*" else file!!.type)){ it ->
        download(it!!, "${passwordGlobal}/${file?.path}")
    }

    var fileIsCheked by remember {
        mutableStateOf(false)
    }

    val fileIcon: Int = when(file.type){
        ("image/jpeg") -> R.drawable.baseline_image_24
        ("image/png") -> R.drawable.baseline_image_24
        else -> R.drawable.baseline_insert_drive_file_24
    }

    var fileLoaded by remember {
        mutableStateOf(file.loaded)
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .background(color = if (fileIsCheked) Color.LightGray else Color.White)
    )
    {
        Text(text = fileLoaded.toString())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(id = fileIcon), contentDescription = "FileType", Modifier.size(70.dp))
            Text(text = file.name, fontSize = 15.sp, modifier = Modifier.weight(4f))

            Column(  modifier = Modifier.weight(2.7f))
            {
                Button(
                    onClick = {
                        launcherDownload.launch(file.name)
                        fileIsCheked = !fileIsCheked
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


fun download(path: Uri, pathRef: String){
    storageRef.child(pathRef).getBytes(1024*1024).addOnSuccessListener {byte ->
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