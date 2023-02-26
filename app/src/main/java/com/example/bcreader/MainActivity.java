package com.example.bcreader;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.cardview.widget.CardView;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.mlkit.nl.entityextraction.Entity;
import com.google.mlkit.nl.entityextraction.EntityAnnotation;
import com.google.mlkit.nl.entityextraction.EntityExtraction;
import com.google.mlkit.nl.entityextraction.EntityExtractionParams;
import com.google.mlkit.nl.entityextraction.EntityExtractor;
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    AppCompatButton selectImageBtn;
    AppCompatImageButton BtnAddContact, BtnShare, BtnCall, BtnMessage, btnRetakeImage;
    //    FloatingActionButton btnRetakeImage;
    ImageView imageView;
    CardView imageCardView;
    LinearLayout selectImageButtonLayout, buttonsLayout, textLayoutBelowButtons;
    TextView textViewData, phoneNumberTextView, phoneTextView, emailTextView, addressTextView, webURLTextView;
    Uri uri;
    Bitmap bitmap;

    public int PICK_CONTACT = 100;
    public Intent addContactIntent = new Intent(Intent.ACTION_INSERT);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectImageBtn = findViewById(R.id.BtnCapture);
//        phoneNumberTextView = findViewById(R.id.phoneTextData);
        imageView = findViewById(R.id.imageView);
        imageCardView = findViewById(R.id.cardView);
        BtnAddContact = findViewById(R.id.BtnCopy);
        textViewData = findViewById(R.id.textData);
        BtnShare = findViewById(R.id.btnShare);
        btnRetakeImage = findViewById(R.id.btnRetakeImage);
        BtnCall = findViewById(R.id.btn_Call);
        BtnMessage = findViewById(R.id.btnMessage);
        phoneTextView = findViewById(R.id.showPhoneNumberTextView);
        emailTextView = findViewById(R.id.emailAddressTv);
        addressTextView = findViewById(R.id.showAddressTv);
        webURLTextView = findViewById(R.id.showWebURLTv);
        selectImageButtonLayout = findViewById(R.id.selectImageButtonLayout);
        buttonsLayout = findViewById(R.id.ButtonsLayout);


        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PickImage();
            }
        });

        btnRetakeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addressTextView.setText("");
                phoneTextView.setText("");
                emailTextView.setText("");
                webURLTextView.setText("");
                PickImage();
            }
        });

        BtnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (phoneTextView.getText().toString().trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Phone Number Not Found", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phoneTextView.getText().toString()));
                    startActivity(intent);
                }

            }
        });

        BtnMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (phoneTextView.getText().toString().trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Phone Number Not Found", Toast.LENGTH_SHORT).show();
                } else {
                    Uri uri = Uri.parse("smsto:" + phoneTextView.getText().toString());
                    Intent it = new Intent(Intent.ACTION_SENDTO, uri);
                    startActivity(it);
                }
            }
        });

        BtnAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                String TVData = textViewData.getText().toString();

                if (TVData.trim().isEmpty()) {

                    Toast.makeText(MainActivity.this, "No data found Error!", Toast.LENGTH_SHORT).show();
                } else {
////                    copyToClipBoard(TVData);
//                    extractPhoneNumberAndNameFromTextView(TVData);
//                    entityExtraction(TVData);
                    startActivityForResult(addContactIntent, PICK_CONTACT);

                }
            }

        });


        BtnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String shareTextViewData = textViewData.getText().toString();
                if (shareTextViewData.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "No data found Error!", Toast.LENGTH_SHORT).show();
                } else {
                    shareText(shareTextViewData);
                }
            }
        });
    }

    public void PickImage() {
        ImagePicker.with(MainActivity.this)
                .crop(16, 9)                    //Crop image(Optional), Check Customization for more option
                .compress(1024)            //Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent(intent -> {
                    ActivityResultLauncher.launch(intent);
                    return null;

                });
    }

    private final ActivityResultLauncher<Intent> ActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data != null && result.getResultCode() == Activity.RESULT_OK) {
                    uri = data.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), uri);

                        // this will show the Scanned Image in ImageView After Text Recognition
                        imageView.setImageBitmap(bitmap);

                        // we have passed bitmap(Cropped Image) object to below method to detect Text from It
                        getTextFromImage(bitmap);

                        // The Linkify class will make all the links clickable present in the text View and it will HighLight all links
                       /* Linkify.addLinks(textViewData, Linkify.ALL);
                        String[] str=textViewData.getText().toString().split("\n");
                        String email;
                        for (String st:str) {
                            if (android.util.Patterns.EMAIL_ADDRESS.matcher(st).matches()){
                                email=st;
                            }
                        }*/
                        // I have use this here to extract entity when image is scanned and this will set result to the text View
//                        String textForExtraction = textViewData.getText().toString();

                        if (textViewData.getText().toString().isEmpty()) {
                            Toast.makeText(this, " Card contains No Data!", Toast.LENGTH_SHORT).show();
                            return;
                        }
// in entityExtraction Method we have used Google MLKit
                        entityExtraction(textViewData.getText().toString());
//                        extractPhoneNumberAndNameFromTextView(textViewData.getText().toString());


                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    Toast.makeText(getApplicationContext(), ImagePicker.getError(data), Toast.LENGTH_SHORT).show();
                }

            });

    private void getTextFromImage(Bitmap bitmap2) {
        TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!recognizer.isOperational()) {
            Toast.makeText(MainActivity.this, "Error Occurred!", Toast.LENGTH_SHORT).show();
        } else {
            Frame frame = new Frame.Builder().setBitmap(bitmap2).build();
            SparseArray<TextBlock> textBlockSparseArray = recognizer.detect(frame);
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < textBlockSparseArray.size(); i++) {
                TextBlock textBlock = textBlockSparseArray.valueAt(i);
                stringBuilder.append(textBlock.getValue());
                stringBuilder.append("\n");
            }

            textViewData.setText(stringBuilder.toString());
//            selectImageBtn.setText("Retake");
//            BtnAddContact.setVisibility(View.VISIBLE);
//            BtnShare.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
            selectImageButtonLayout.setVisibility(View.GONE);
            buttonsLayout.setVisibility(View.VISIBLE);
            imageCardView.setVisibility(View.VISIBLE);
//            textLayoutBelowButtons.setVisibility(View.VISIBLE);


        }
    }

    private void copyToClipBoard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("Copied Data", text);
        clipboard.setPrimaryClip(clipData);
        Toast.makeText(MainActivity.this, "Information Saved Successfully", Toast.LENGTH_SHORT).show();
    }

    public void shareText(String textData) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, textData);
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent, "Share Text To");
        startActivity(shareIntent);
    }

    // Google Ml Kit Entity Extraction Method
    public void entityExtraction(String entityText) {
        addContactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);


        EntityExtractor entityExtractor = EntityExtraction.getClient(
                new EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH)
                        .build());
        entityExtractor.downloadModelIfNeeded()
                .addOnSuccessListener(Void -> {
                            EntityExtractionParams params = new EntityExtractionParams.Builder(entityText.trim())//entityText
                                    .build();
                            entityExtractor.annotate(params)
                                    .addOnSuccessListener(new OnSuccessListener<List<EntityAnnotation>>() {

                                        @Override
                                        public void onSuccess(List<EntityAnnotation> entityAnnotations) {
                                            // write something on success
                                            for (EntityAnnotation entityAnnotation : entityAnnotations) {
                                                List<Entity> entities = entityAnnotation.getEntities();
                                                String Tag = "";
                                                Log.d(Tag, String.format("Range: [%d, %d]", entityAnnotation.getStart(),
                                                        entityAnnotation.getEnd()));
                                                for (Entity entity : entities) {
                                              /*  Entity EMAIL=entities.get(0);
                                                Entity ADDRESS=entities.get(1);
                                                Entity PHONE=entities.get(2);
                                                Entity URL=entities.get(3);*/


                                                    if (entity.getType() == 8 && entity.getType() != 3) {

                                                        Log.d("ABC", "type number= " + entityAnnotation.getAnnotatedText().toString());
                                                        phoneTextView.setText(entityAnnotation.getAnnotatedText().toString());
                                                        Linkify.addLinks(phoneTextView, Linkify.PHONE_NUMBERS);
                                                        addContactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneTextView.getText().toString());


                                                    } else if (entity.getType() == 3) {
                                                        emailTextView.setText(entityAnnotation.getAnnotatedText());
                                                        addContactIntent.putExtra(ContactsContract.Intents.Insert.EMAIL, emailTextView.getText().toString());
                                                        Linkify.addLinks(emailTextView, Linkify.EMAIL_ADDRESSES);

                                                    } else if (entity.getType() == 10) {
                                                        webURLTextView.setText(entityAnnotation.getAnnotatedText());
                                                        addContactIntent.putExtra(ContactsContract.Intents.Insert.COMPANY, webURLTextView.getText().toString());
                                                        Linkify.addLinks(webURLTextView, Linkify.WEB_URLS);

                                                    } else if (entity.getType() == 1) {
                                                        addressTextView.setText(entityAnnotation.getAnnotatedText());
                                                        addContactIntent.putExtra(ContactsContract.Intents.Insert.POSTAL, addressTextView.getText().toString());
                                                        Linkify.addLinks(addressTextView, Linkify.MAP_ADDRESSES);
                                                    }

                                                    if (addressTextView.getText().toString().trim().contains(phoneTextView.getText().toString().trim())) {
                                                        // entityExtraction(entityText.replace(phoneTextView.getText().toString().trim(),""));
                                                        entityExtraction(entityText.trim().replace(addressTextView.getText().toString().trim(), ""));
//                                                      Toast.makeText(MainActivity.this, "Method Called", Toast.LENGTH_SHORT).show();


                                                    }


//                                                  if (addressTextView.getText().toString().trim().contains(phoneTextView.getText().toString())){
//                                                      phoneTextView.setText("");
//                                                  }


                                                }



                                               /* switch (entity.getType()) {

                                                    case Entity.TYPE_PHONE:

//                                                        PhoneNumberUtils.formatNumber(entityText);
                                                        addContactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, entityAnnotation.getAnnotatedText());
                                                        phoneTextView.setText(entityAnnotation.getAnnotatedText());
//                                                            startActivityForResult(addContactIntent, PICK_CONTANCT);
//                                                            Toast.makeText(MainActivity.this, "Phone Number is available", Toast.LENGTH_SHORT).show();
                                                        break;

                                                    case Entity.TYPE_EMAIL:
                                                        addContactIntent.putExtra(ContactsContract.Intents.Insert.EMAIL, displayEmailInfo(entityText));
                                                        Toast.makeText(MainActivity.this, "Email is Available", Toast.LENGTH_SHORT).show();
                                                        emailTextView.setText(entityAnnotation.getAnnotatedText());
                                                       // phoneNumberTextView.setText(entityAnnotation.getAnnotatedText());
                                                        //phoneNumberTextView.setVisibility(View.VISIBLE);
                                                        break;

                                                    case Entity.TYPE_URL:
                                                        addContactIntent.putExtra(ContactsContract.Intents.Insert.COMPANY, entityAnnotation.getAnnotatedText());
//                                                            startActivityForResult(addContactIntent, PICK_CONTANCT);
                                                        webURLTextView.setText(entityAnnotation.getAnnotatedText());
                                                        break;
                                                        case Entity.TYPE_ADDRESS:
                                                            addContactIntent.putExtra(ContactsContract.Intents.Insert.POSTAL,entityAnnotation.getAnnotatedText());
                                                            addressTextView.setText(entityAnnotation.getAnnotatedText());

                                                    case Entity.TYPE_FLIGHT_NUMBER:
                                                        FlightNumberEntity flightNumberEntity = entity.asFlightNumberEntity();
//                                                            Log.d(TAG, "Airline Code: " + flightNumberEntity.getAirlineCode());
//                                                            Log.d(TAG, "Flight number: " + flightNumberEntity.getFlightNumber());
                                                        //phoneNumberTextView.setText(flightNumberEntity.getFlightNumber() + "\n" + flightNumberEntity.getAirlineCode());
                                                        // using it Temporary
                                                       // phoneNumberTextView.setVisibility(View.VISIBLE);
                                                        break;

                                                    case Entity.TYPE_DATE_TIME:
                                                        DateTimeEntity dateTimeEntity = entity.asDateTimeEntity();
                                                        break;
//                                                            Log.d(TAG, "Granularity: " + dateTimeEntity.getDateTimeGranularity());
//                                                            Log.d(TAG, "Timestamp: " + dateTimeEntity.getTimestampMillis());
//                                                            phoneNumberTextView.setText(dateTimeEntity.getTimestampMillis()+"\n");

                                                    default:
                                                        Log.d(Tag, "Entity" + entity);
                                                        break;
                                                }*/
                                            }
                                            //}
                                            EntityExtractorOptions options = new EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build();
                                            EntityExtractor extractor = EntityExtraction.getClient(options);
                                            getLifecycle().addObserver(extractor);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {

                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Handle Failure
                                            Toast.makeText(MainActivity.this, "Error Occurred!", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }

                )
                .addOnFailureListener(Exception -> {
                    // Handle Failure Here
                });
    }

    // Extraction Method using Java Builtin Classes Pattern and Matcher
    public void extractPhoneNumberAndNameFromTextView(String text) {
        addContactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
// below is specific phone Pattern
//        Pattern phoneNumberPattern = Pattern.compile("\\d{1,3}-\\d{3}-\\d{4}");
//        Pattern phoneNumberPattern = Pattern.compile("(\\d{3}[-\\.\\s]?){2}\\d{4}");
        Pattern phoneNumberPattern = Patterns.PHONE;
        Matcher phoneNumberMatcher = phoneNumberPattern.matcher(text);

        while (phoneNumberMatcher.find()) {
            String phoneNumber = phoneNumberMatcher.group();
            phoneTextView.setText(phoneNumber);
            Linkify.addLinks(phoneTextView, Linkify.PHONE_NUMBERS);
            // Just a try on below line to pick the Phone Number but it doesn't work
//            String phoneName = String.valueOf(Linkify.addLinks(textViewData, Linkify.PHONE_NUMBERS));
            addContactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber);
////            startActivityForResult(addContactIntent, PICK_CONTANCT);
        }
//        Pattern namePattern = Pattern.compile("[A-Z][a-z]+ [A-Z][a-z]+");
        Pattern namePattern = Patterns.DOMAIN_NAME;
        Matcher nameMatcher = namePattern.matcher(text);

        while (nameMatcher.find()) {
            String name = nameMatcher.group();
            String userName = "Write Name";
            addContactIntent.putExtra(ContactsContract.Intents.Insert.NAME, name);

//            startActivityForResult(addContactIntent, PICK_CONTANCT);


//            Toast.makeText(this, "Name Pattern Matches", Toast.LENGTH_SHORT).show();
        }

//        Pattern emailPattern = Pattern.compile("@"+"[a-z]"+"\\.com");
//        Pattern emailPattern = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        Matcher emailMatcher = emailPattern.matcher(text);

        while (emailMatcher.find()) {
            String emailAddress = emailMatcher.group();
            // Just a try on below line to pick the email Addresses but it does not work
//            String email = String.valueOf(Linkify.addLinks(textViewData, Linkify.PHONE_NUMBERS));
            addContactIntent.putExtra(ContactsContract.Intents.Insert.EMAIL, emailAddress);
            emailTextView.setText(emailAddress);
            Linkify.addLinks(phoneTextView, Linkify.EMAIL_ADDRESSES);

//            phoneNumberTextView.setText(emailAddress);
//            phoneNumberTextView.setVisibility(View.VISIBLE);

        }

//        Pattern websitePattern = Pattern.compile("(?i)\\b((?:[\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[a-z]{2,4}/)(?:[^\\s()<>]+|(([^\\s()<>]+|(([^\\s()<>]+)))))+(?:(([^\\s()<>]+|(([^\\s()<>]+))))|[^\\s`!()\\[\\]{};:'\".,<>?«»\"\"‘’]))");
        Pattern websitePattern = Patterns.WEB_URL;
        Matcher websiteMatcher = websitePattern.matcher(text);
        while (websiteMatcher.find()) {
            String websiteAddress = websiteMatcher.group();
            addContactIntent.putExtra(ContactsContract.CommonDataKinds.Website.URL, websiteAddress);
            webURLTextView.setText(websiteAddress);
            Linkify.addLinks(webURLTextView, Linkify.WEB_URLS);
        }
    }
}