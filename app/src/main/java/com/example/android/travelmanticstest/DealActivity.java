package com.example.android.travelmanticstest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    private static final int PICTURE_RESULT = 42;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    Button saveButton;
    Button imageButton;
    EditText textTitle;
    EditText textDescription;
    EditText textPrice;
    ImageView mImageView;
    TravelDeal deal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        loadData();

        imageButton = findViewById(R.id.upload_image_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDeal();
            }
        });

        if (FirebaseUtil.isAdmin) {
            saveButton.setVisibility(View.VISIBLE);
            imageButton.setVisibility(View.VISIBLE);
        } else {
            saveButton.setVisibility(View.GONE);
            imageButton.setVisibility(View.GONE);
        }

        mImageView = findViewById(R.id.image);
        showImage(deal.getImageUrl());
        Log.i("URL", "Got here: " + deal.getImageUrl());
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent, "Insert Picture"), PICTURE_RESULT);
            }
        });
    }

    public void loadData() {
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        textTitle = findViewById(R.id.txt_name);
        textDescription = findViewById(R.id.txt_description);
        textPrice = findViewById(R.id.txt_price);
        saveButton = findViewById(R.id.save_button);

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (deal == null) {
            deal = new TravelDeal();
            saveButton.setText(R.string.save_deal_button_text);
        }
        this.deal = deal;
        textTitle.setText(deal.getTitle());
        textDescription.setText(deal.getDescription());
        textPrice.setText(deal.getPrice());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.deal_activity_menu_items, menu);
        MenuItem delete_menu = menu.findItem(R.id.delete_menu);
        if (FirebaseUtil.isAdmin) {
            delete_menu.setVisible(true);
//            showMenu();
            enableEditText(true);
        } else {
            delete_menu.setVisible(false);
            enableEditText(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_menu:
                deleteDeal();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveDeal() {
        deal.setTitle(textTitle.getText().toString());
        deal.setPrice(textPrice.getText().toString());
        deal.setDescription(textDescription.getText().toString());

        if (deal.getId() == null) {
            mDatabaseReference.push().setValue(deal);
            startActivity(new Intent(DealActivity.this, ListActivity.class));
            Toast.makeText(this, "Deal Saved", Toast.LENGTH_LONG).show();
            clean();
        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
            startActivity(new Intent(DealActivity.this, ListActivity.class));
            Toast.makeText(this, "Deal Updated", Toast.LENGTH_LONG).show();
            clean();
        }
    }

    private void deleteDeal() {
        if (deal.getId() == null) {
            Toast.makeText(this, "This is a not a valid deal", Toast.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(deal.getId()).removeValue();
        startActivity(new Intent(DealActivity.this, ListActivity.class));
        Toast.makeText(this, "Deal Deleted", Toast.LENGTH_LONG).show();
        if (deal.getImageName() != null && !deal.getImageName().isEmpty()) {
            StorageReference pictureReference = FirebaseUtil.mFirebaseStorage.getReference().child(deal.getImageName());
            pictureReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Delete Image", "Image Deleted Successfully");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Delete Image", "Unable to delete image");
                }
            });
        }
        clean();
    }

    private void clean() {
        textTitle.setText("");
        textPrice.setText("");
        textDescription.setText("");

        textTitle.requestFocus();
    }


    private void enableEditText(boolean isEnabled) {
        textTitle.setEnabled(isEnabled);
        textDescription.setEnabled(isEnabled);
        textPrice.setEnabled(isEnabled);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
                    final String pictureName = taskSnapshot.getStorage().getPath();
                    task.addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String url = uri.toString();
                            deal.setImageUrl(url);
                            deal.setImageName(pictureName);
                            showImage(url);
                        }
                    });
                }
            });
        }
    }

    private void showImage(String url) {
        if (url != null && !url.isEmpty()) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.with(this)
                    .load(url)
                    .resize(width, width * 2 / 3)
                    .centerCrop()
                    .into(mImageView);
        }
    }

}
