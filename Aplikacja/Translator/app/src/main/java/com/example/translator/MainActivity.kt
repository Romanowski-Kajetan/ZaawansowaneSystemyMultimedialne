package com.example.translator

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.translator.ui.theme.TranslatorTheme
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object{

        private val TAG = "MAIN_TAG"
    }

    private var languageArrayList: ArrayList<ModelLanguage>? = null

    private var sourceLanguageCode = "en"
    private var sourceLanguageTitle = "English"
    private var targetLanguageCode = "pl"
    private var targetLanguageTitle = "Polish"

    private lateinit var translatorOptions: TranslatorOptions

    private lateinit var translator: Translator



    override fun onCreate(savedInstanceState: Bundle?) {

        loadAvailableLanguages()
        super.onCreate(savedInstanceState)
        setContent {
            TranslatorTheme {
                // A surface container using the 'background' color from the theme
                var inputText by remember { mutableStateOf("") }
                var translatedText by remember { mutableStateOf("") }
                var selectedSourceLanguage by remember { mutableStateOf(sourceLanguageTitle) }
                var selectedSourceCode by remember { mutableStateOf(sourceLanguageCode) }
                var selectedTargetLanguage by remember { mutableStateOf(targetLanguageTitle) }
                var selectedTargetCode by remember { mutableStateOf(targetLanguageCode) }
                var showDialog by remember { mutableStateOf(false) }
                var dialogMessage by remember { mutableStateOf("Please wait...") }


                        if (showDialog){
                            Dialog(
                                onDismissRequest = {showDialog = false},
                                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                            ){
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(140.dp).background(Color.White, shape = RoundedCornerShape(8.dp))
                                ){
                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceAround, horizontalAlignment = Alignment.CenterHorizontally){
                                        CircularProgressIndicator()
                                        Text(text = dialogMessage)
                                    }

                                }
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize(),horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.fillMaxWidth()){
                                SourceTextField(labelText = selectedSourceLanguage, textToTranslate = inputText, onTextChange = {inputText = it})
                                Text(modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 0.dp,0.dp), fontSize = 12.sp, color = Color.Gray, text = selectedTargetLanguage)
                                Text(modifier = Modifier.fillMaxWidth().padding(16.dp, 0.dp, 0.dp,0.dp), text = translatedText)
                            }

                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround, ){
                                    /*Button(modifier = Modifier.weight(1.0f),onClick = {

                                    }){
                                        Text("Polski")
                                    }*/LanguagePicker(modifier = Modifier.weight(1.0f), languages = languageArrayList, selectedLanguage = selectedSourceLanguage, onLanguageChange = {selectedSourceLanguage = it}, onLanguageCodeChange = {selectedSourceCode = it})
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Translation"
                                    )
                                    /*Button(modifier = Modifier.weight(1.0f), onClick = {

                                    }){
                                        Text("Angielski")
                                    }*/
                                    Row(modifier = Modifier.weight(1.0f), horizontalArrangement = Arrangement.End){
                                        LanguagePicker(modifier = Modifier.fillMaxWidth(), languages = languageArrayList, selectedLanguage = selectedTargetLanguage, onLanguageChange = {selectedTargetLanguage = it}, onLanguageCodeChange = {selectedTargetCode = it})
                                    }

                                }
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    validateData(selectedSourceCode,selectedTargetCode, inputText, showDialog = {showDialog = it}, dialogMessage = {dialogMessage = it}, translatedText = {translatedText = it})

                                }){
                                    Text("Translate")
                                }
                            }


                        }


            }
        }

    }

    private fun validateData(selectedSourceCode:String,selectedTargetCode:String, inputData: String, showDialog: (Boolean) -> Unit, dialogMessage: (String) -> Unit, translatedText: (String) -> Unit) {

        if (inputData.isEmpty()){
            showToast("Enter text to translate...")
        }
        else{
            startTranslation(selectedSourceCode,selectedTargetCode,inputData, showDialog, dialogMessage, translatedText)
        }

    }

    private fun startTranslation(selectedSourceCode:String,selectedTargetCode:String, inputData: String, showDialog: (Boolean) -> Unit, dialogMessage: (String) -> Unit, translatedText: (String) -> Unit) {
        dialogMessage("Processing language model...")
        showDialog(true)

        translatorOptions = TranslatorOptions.Builder()
            .setSourceLanguage(selectedSourceCode)
            .setTargetLanguage(selectedTargetCode)
            .build()
        translator = Translation.getClient(translatorOptions)

        val downloadConditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator.downloadModelIfNeeded(downloadConditions)
            .addOnSuccessListener {
                Log.d(TAG, "startTranslation: model ready, start translation...")

                dialogMessage("Translating...")

                translator.translate(inputData)
                    .addOnSuccessListener{translatedText ->
                        Log.d(TAG, "startTranslation: translatedText: $translatedText")

                        showDialog(false)
                        translatedText(translatedText)
                    }
                    .addOnFailureListener{e->
                        showDialog(false)
                        Log.e(TAG, "startTranslation: ", e)

                        showToast("Failed to translate due to ${e.message}")
                    }
            }
            .addOnFailureListener {e->
                showDialog(false)
                Log.e(TAG, "startTranslation: ", e)

                showToast("Failed due to ${e.message}")
            }

    }

    private fun loadAvailableLanguages(){

        languageArrayList = ArrayList()

        val languageCodeList = TranslateLanguage.getAllLanguages()

        for (languageCode in languageCodeList){
            val languageTitle = Locale(languageCode).displayLanguage

            Log.d(TAG, "loadAvailableLanguages: languageCode: $languageCode")
            Log.d(TAG, "loadAvailableLanguages: languageTitle: $languageTitle")

            val modelLanguage = ModelLanguage(languageCode, languageTitle)

            languageArrayList!!.add(modelLanguage)
        }
    }

    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_LONG)
    }
}




@Composable
fun SourceTextField(labelText:String, textToTranslate: String, onTextChange: (String) -> Unit) {

    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = textToTranslate,
        onValueChange = {onTextChange(it) },
        label= { Text(labelText)}
    )
}

@Composable
fun LanguagePicker(modifier: Modifier, languages: ArrayList<ModelLanguage>?, selectedLanguage: String, onLanguageChange: (String) -> Unit, onLanguageCodeChange: (String) -> Unit) {
    //var text by remember { mutableStateOf("Polski") }
    var expanded by remember { mutableStateOf(false) }
    //var languages = arrayOf("English","Polski")

    Button(modifier = modifier, onClick = {
        expanded = true
    }){
        Text(selectedLanguage)
    }
    DropdownMenu(

        expanded = expanded,
        onDismissRequest = {
                           expanded = false
        }){

        if (languages != null) {
            for (language in languages){
                DropdownMenuItem(onClick = {
                    expanded = false
                    Log.d("MAIN_TAG", "l2p: ${language.languageTitle}")
                    Log.d("MAIN_TAG", "l2p: ${language.languageCode}")
                    onLanguageChange(language.languageTitle)
                    onLanguageCodeChange(language.languageCode)
                },
                    interactionSource = MutableInteractionSource(),
                    text = {
                        Text(language.languageTitle)
                    }
                )
            }
        }
        /*
            DropdownMenuItem(onClick = {
                expanded = false
                text = "English"
            },
                interactionSource = MutableInteractionSource(),
                text = {
                    Text("English")
                }
            )
        DropdownMenuItem(onClick = {
            expanded = false
            text = "Polski"
        },
            interactionSource = MutableInteractionSource(),
            text = {
                Text("Polski")
            }
        )*/
        }

}