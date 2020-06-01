package uts.group4.UTShealth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import maes.tech.intentanim.CustomIntent;
import uts.group4.UTShealth.Model.ChatMessage;
public class Chat extends AppCompatActivity {
    private static final java.util.UUID UUID = null;

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        TextView messengerTimeView;


        public MessageViewHolder(View v) {
            super(v);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            messengerTextView = itemView.findViewById(R.id.messengerTextView);
            messengerTimeView = itemView.findViewById(R.id.messengerTimeView);

        }
    }
    private boolean isDoctor;
    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    public static final String CHATS_PATH = "Chats/";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private String mUsername;
    private SharedPreferences mSharedPreferences;
    private static final String MESSAGE_URL = "https://uts-health-application.firebaseio.com/Chats";
    private Uri filePath;
    String imageUrl;
    String chatCode;
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("EEE, MMM dd hh:mm a");
    String dateAndTime = formatter.format(date);

    private Button mSendButton;
    private Button endBtn;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;

    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser mFirebaseUser;
    private FirebaseFirestore fStore = FirebaseFirestore.getInstance();
    private FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder>
            mFirebaseAdapter;
    StorageReference storageReference = FirebaseStorage.getInstance().getReference();
    DocumentReference docRef;
    FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
    String userID = mFirebaseAuth.getCurrentUser().getUid();
    DocumentReference staffRef = fStore.collection("Doctor").document(userID);

    /**********************************************************************************************
     * onCreate
     ************************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_layout);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Set default username is anonymous.
        mUsername = firebaseUser.getEmail();

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.progressBar);
        mMessageRecyclerView = findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        mProgressBar.setVisibility(View.INVISIBLE);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        SnapshotParser<ChatMessage> parser = new SnapshotParser<ChatMessage>() {
            @Override
            public ChatMessage parseSnapshot(DataSnapshot dataSnapshot) {
                ChatMessage Message = dataSnapshot.getValue(ChatMessage.class);
                if (Message != null) {
                    Message.setId(dataSnapshot.getKey());
                }
                return Message;
            }
        };

        DatabaseReference messagesRef;
        Bundle extras = getIntent().getExtras();
        chatCode = null;
        if(extras != null){
            chatCode = extras.getString("chatroomcode");
            isDoctor = extras.getBoolean("isDoctor");
            messagesRef = mFirebaseDatabaseReference.child(CHATS_PATH + chatCode);
        }
        else{
            messagesRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        }


        final String chatRoomPath = CHATS_PATH + chatCode;

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        DocumentReference apptRef = fStore.collection("Appointment").document(chatCode.substring(4));
        apptRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(isDoctor){
                    mUsername = documentSnapshot.get("DoctorFullName").toString();
                }
                else{
                    mUsername = documentSnapshot.get("PatientFullName").toString();
                }
            }
        });

        FirebaseRecyclerOptions<ChatMessage> options =
                new FirebaseRecyclerOptions.Builder<ChatMessage>()
                        .setQuery(messagesRef, parser)
                        .build();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder>(options) {
            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder,
                                            int position,
                                            ChatMessage Message) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if(!Message.hasImageUrl()){
                viewHolder.messageTextView.setText(Message.getText());
                }
                viewHolder.messengerTextView.setText(Message.getName());
                viewHolder.messengerTimeView.setText(Message.getDateAndTime());
                viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                if(Message.hasImageUrl()){
                    Glide.with(viewHolder.messageImageView.getContext()).load(Message.getImageUrl()).into(viewHolder.messageImageView);
                }
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        mMessageEditText = findViewById(R.id.messageEditText);
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send messages on click.
                ChatMessage Message = new
                        ChatMessage(mMessageEditText.getText().toString(),
                        mUsername,
                        null, dateAndTime);
                mFirebaseDatabaseReference.child(chatRoomPath)
                        .push().setValue(Message);
                mMessageEditText.setText("");

            }
        });

        endBtn = findViewById(R.id.endBtn);
        staffRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    return;
                } else {
                    endBtn.setVisibility(View.INVISIBLE); // Sets end button to invisible if user is a patient
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Chat.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });

        mAddMessageImageView = findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // opens page to  Select image for image message.
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_IMAGE);
            }
        });
    }

    /**********************************************************************************************
     * Screenshot testing
     ************************************************************************************************/

    public void BtnPressed(View v) {

        storeImage(getRecyclerViewScreenshot(mMessageRecyclerView));
    }


    public static Bitmap getRecyclerViewScreenshot(RecyclerView view) {
        int size = view.getAdapter().getItemCount();
        RecyclerView.ViewHolder holder = view.getAdapter().createViewHolder(view, 0);
        view.getAdapter().onBindViewHolder(holder, 0);
        holder.itemView.measure(View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        holder.itemView.layout(0, 0, holder.itemView.getMeasuredWidth(), holder.itemView.getMeasuredHeight());
        Bitmap bigBitmap = Bitmap.createBitmap(view.getMeasuredWidth(), holder.itemView.getMeasuredHeight() * size,
                Bitmap.Config.ARGB_8888);
        Canvas bigCanvas = new Canvas(bigBitmap);
        bigCanvas.drawColor(Color.TRANSPARENT);
        Paint paint = new Paint();
        int iHeight = 0;
        holder.itemView.setDrawingCacheEnabled(true);
        holder.itemView.buildDrawingCache();
        bigCanvas.drawBitmap(holder.itemView.getDrawingCache(), 0f, iHeight, paint);
        holder.itemView.setDrawingCacheEnabled(false);
        holder.itemView.destroyDrawingCache();
        iHeight += holder.itemView.getMeasuredHeight();
        for (int i = 1; i < size; i++) {
            view.getAdapter().onBindViewHolder(holder, i);
            holder.itemView.setDrawingCacheEnabled(true);
            holder.itemView.buildDrawingCache();
            bigCanvas.drawBitmap(holder.itemView.getDrawingCache(), 0f, iHeight, paint);
            iHeight += holder.itemView.getMeasuredHeight();
            holder.itemView.setDrawingCacheEnabled(false);
            holder.itemView.destroyDrawingCache();
        }
        return bigBitmap;
    }

    /**********************************************************************************************
     * storeImage and getOutputMediaFile are for testing purposes. Screenshot is stored at android SDK.
     * Android > data > uts.group4.UTSHealth > Files
     ************************************************************************************************/

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }


    private  File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ timeStamp +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // in the mAddMessageImageView page, this saves the path from the device then calls putImageInStorage()
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            filePath = data.getData();
            putImageInStorage();
                                }
                }
    private void putImageInStorage() {
        // uploads the image into the database under the file named images
        final StorageReference ref = storageReference.child("images/" + java.util.UUID.randomUUID().toString());
        ref.putFile(filePath)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (taskSnapshot.getMetadata() != null) {
                            if (taskSnapshot.getMetadata().getReference() != null) {
                                Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                                result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                    public void onSuccess(Uri uri) {
                                        imageUrl = uri.toString();
                                        ChatMessage Message = new
                                                ChatMessage(null,
                                                mUsername,
                                                imageUrl, dateAndTime);
                                        mFirebaseDatabaseReference.child(CHATS_PATH + chatCode)
                                                .push().setValue(Message);
                                    }
                                });
                            }
                        }
                    }
                });
    }

    public void endChat(View view) {

        Intent i = new Intent(getApplicationContext(), PrescriptionNotes.class);
        Bundle bundle = new Bundle();
        bundle.putString("Chatroomcode", chatCode);
//        bundle.putString("Bitmap", stbmp);
        i.putExtras(bundle);
        startActivity(i);
        CustomIntent.customType(Chat.this, "left-to-right");
    }

    public void backBtnPressed(View view) {
        staffRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    finish();
                    startActivity(new Intent(getApplicationContext(), StaffDashboard.class));
                    CustomIntent.customType(Chat.this, "left-to-right");
                } else {
                    finish();
                    startActivity(new Intent(getApplicationContext(), PatientDashboard.class));
                    CustomIntent.customType(Chat.this, "left-to-right");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Chat.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public Bitmap getScreenBitmap() {
        View v= mMessageRecyclerView;
        v.setDrawingCacheEnabled(true);
        v.buildDrawingCache(true);
        Bitmap b = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false); // clear drawing cache
        return b;
    }



    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(this, "left-to-right");
    } // Fade transition
            }

