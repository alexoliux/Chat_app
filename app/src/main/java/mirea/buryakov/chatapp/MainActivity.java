package mirea.buryakov.chatapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //    //Исправить это
    private static final int RC_GET_IMAGE = 1333;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private RecyclerView recyclerViewMessages;
    private MessagesAdapter adapter;

    private EditText editTextMessage;
    private ImageView imageViewSendMessage;
    private ImageView imageViewAddImage;

    private SharedPreferences preferences;

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    onSignInResult(result);
                }
            }
    );

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.itemSignOut) {
            mAuth.signOut();
            signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Create a storage reference from our app
        storageRef = storage.getReference();
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        imageViewSendMessage = findViewById(R.id.imageViewSendMessage);
        imageViewAddImage = findViewById(R.id.imageViewAddImage);
        adapter = new MessagesAdapter(this);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(adapter);
        imageViewAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
//                Устарвшний метод
                //                startActivityForResult(intent,RC_GET_IMAGE );
                // Изменил на это
                mStartForResult.launch(intent);
            }
        });

        imageViewSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(null);
            }
        });


        if (mAuth.getCurrentUser() != null) {
            preferences.edit().putString("author", mAuth.getCurrentUser().getEmail()).apply();
        } else {
            signOut();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        db.collection("messages").orderBy("date").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value != null) {
                    List<Message> messages = value.toObjects(Message.class);
                    adapter.setMessages(messages);
                    recyclerViewMessages.scrollToPosition(adapter.getItemCount() - 1);
                }
            }
        });
    }

    private void sendMessage(String urlToImage) {
        Message message = null;
        String author = preferences.getString("author", "Anonim");
        String textOfMessage = editTextMessage.getText().toString().trim();
        if (!textOfMessage.isEmpty()) {
            message = new Message(author, textOfMessage, System.currentTimeMillis(), null);
        } else if (urlToImage != null && !urlToImage.isEmpty()) {
            message = new Message(author, null, System.currentTimeMillis(), urlToImage);
        }
        db.collection("messages").add(message).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                editTextMessage.setText("");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Сообщение не отправлено", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signOut() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Choose authentication providers
                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build());

                    Intent signInIntent = AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build();
                    signInLauncher.launch(signInIntent);
                }
            }
        });
    }

    private void onSignInResult(@NonNull FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Toast.makeText(this, user.getEmail(), Toast.LENGTH_SHORT).show();
                preferences.edit().putString("author", user.getEmail()).apply();
            }
            // ...
        } else {
            if (response != null) {
                Toast.makeText(this, "Error: " + response.getError(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            }
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
        }
    }

    //Новая реалтзаия
    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri uri = null;
                        if (result.getData() != null) {
                            uri = result.getData().getData();
                        }
                        if (uri != null) {
                            StorageReference referenceToImage = storageRef.child("images/" + uri.getLastPathSegment());
                            referenceToImage.putFile(uri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                @Override
                                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    if (!task.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Err :" + task.getException(), Toast.LENGTH_SHORT).show();
                                    }

                                    // Continue with the task to get the download URL
                                    return referenceToImage.getDownloadUrl();
                                }
                            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "loaded", Toast.LENGTH_SHORT).show();
                                        Uri downloadUri = task.getResult();
                                        sendMessage(downloadUri.toString());
                                    } else {
                                        // Handle failures
                                        // ...
                                    }
                                }
                            });
                        }
                    }
                }
            });

    ////Устаревший метод

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (resultCode == RESULT_OK) {
//            if (data != null) {
//                Uri uri = data.getData();
////                Uri uri = Uri.fromFile(new File("path/media/external/images/media/42"));
////                Uri uri = Uri.fromFile(new File(data.getData().toString()));
//                if (uri != null) {
//                    StorageReference referenceToImage = storageRef.child("images/" + uri.getLastPathSegment());
//                    referenceToImage.putFile(uri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
//                        @Override
//                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                            if (!task.isSuccessful()) {
//                                Toast.makeText(MainActivity.this, "Err :" + task.getException() , Toast.LENGTH_SHORT).show();
//                            }
//
//                            // Continue with the task to get the download URL
//                            return referenceToImage.getDownloadUrl();
//                        }
//                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Uri> task) {
//                            if (task.isSuccessful()) {
//                                Toast.makeText(MainActivity.this, "Sus", Toast.LENGTH_SHORT).show();
//                                Uri downloadUri = task.getResult();
//                                sendMessage(downloadUri.toString());
//                            } else {
//                                // Handle failures
//                                // ...
//                            }
//                        }
//                    });
//                }
//            }
//        }
//    }
}