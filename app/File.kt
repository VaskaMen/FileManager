import com.example.filemanager.R

@Preview(showBackground = true)
@Composable
fun FileCard(file: SelectedFile = SelectedFile("Name", "jpg", "12342", "zxcasca"), callback: () -> Unit = {}){

    val fileIcon: Int = when(file.type){
        ("image/jpeg") -> R.drawable.baseline_image_24
        ("image/png") -> R.drawable.baseline_image_24
        else -> R.drawable.baseline_insert_drive_file_24
    }

    Column(modifier = Modifier.fillMaxWidth())
    {
        Text(text = file.name)
        Row {
            Image(painterResource(id = fileIcon), contentDescription = "FileType")
            Text(text = file.type)
            Text(text = "|")
            Text(text = file.size)
            Button(onClick = {
                callback()
            }) {
                Text(text = "Выбрать")
            }
        }
        Divider()
    }
}