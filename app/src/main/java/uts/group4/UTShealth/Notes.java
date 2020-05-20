package uts.group4.UTShealth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import maes.tech.intentanim.CustomIntent;

public class Notes extends AppCompatActivity {

    EditText notesTf;
    Button sendBtn, prescriptionBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        notesTf = findViewById(R.id.notesTf);
        sendBtn = findViewById(R.id.sendBtn1);
        prescriptionBtn = findViewById(R.id.prescriptionBtn);

        sendBtn.setOnClickListener(new View.OnClickListener() {
           String notes = notesTf.getText().toString().trim();
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(notes)) {
                    notesTf.setError("Cannot have Empty Field");
                    return;
                }
                startActivity(new Intent(getApplicationContext(), PatientDashboard.class));
                CustomIntent.customType(Notes.this, "fadein-to-fadeout");
            }
        });

        prescriptionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Prescription.class));
                CustomIntent.customType(Notes.this, "fadein-to-fadeout");
            }
        });
    }
}
