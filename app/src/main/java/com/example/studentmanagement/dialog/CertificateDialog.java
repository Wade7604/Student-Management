package com.example.studentmanagement.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.studentmanagement.R;
import com.example.studentmanagement.models.Certificate;
import com.example.studentmanagement.models.Student;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CertificateDialog extends Dialog {

    private Certificate certificate;
    private CertificateDialogListener listener;
    private String preSelectedStudentId;

    private TextView tvTitle;
    private TextView tvStudentName;
    private TextInputLayout tilCertificateName, tilIssuingAuthority, tilIssueDate, tilExpiryDate, tilDescription;
    private TextInputEditText etCertificateName, etIssuingAuthority, etIssueDate, etExpiryDate, etDescription;
    private Button btnSave, btnCancel;
    private boolean readOnlyMode = false;

    private FirebaseFirestore db;
    private Calendar calendarIssue = Calendar.getInstance();
    private Calendar calendarExpiry = Calendar.getInstance();

    public interface CertificateDialogListener {
        void onCertificateAdded(Certificate certificate);
        void onCertificateUpdated(Certificate certificate);
        void onCertificateDeleted(Certificate certificate);
    }

    public CertificateDialog(@NonNull Context context,
                             Certificate certificate,
                             CertificateDialogListener listener) {
        super(context);
        this.certificate = certificate;
        this.listener = listener;
        db = FirebaseFirestore.getInstance();

        if (context instanceof com.example.studentmanagement.activity.CertificateActivity) {
            preSelectedStudentId = ((com.example.studentmanagement.activity.CertificateActivity) context)
                    .getIntent()
                    .getStringExtra("STUDENT_ID");
        }
    }

    public void setReadOnlyMode(boolean readOnlyMode) {
        this.readOnlyMode = readOnlyMode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Không chủ đề
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Inflate layout thủ công
        View root = getLayoutInflater()
                .inflate(R.layout.dialog_certificate, null);
        setContentView(root);

        initViews(root);
        setupDatePickers();
        loadStudentName();
        if (readOnlyMode) {
            tvTitle.setText("Certificate Details");

            if (certificate != null) {
                etCertificateName.setText(certificate.getCertificateName());
                etIssuingAuthority.setText(certificate.getIssuingAuthority());
                etIssueDate.setText(certificate.getIssueDate());
                etExpiryDate.setText(certificate.getExpiryDate());
                etDescription.setText(certificate.getDescription());
                etCertificateName.setEnabled(false);
                etIssuingAuthority.setEnabled(false);
                etIssueDate.setEnabled(false);
                etExpiryDate.setEnabled(false);
                etDescription.setEnabled(false);
                btnSave.setVisibility(View.GONE);
                btnCancel.setText("Close");
            } else {
                Toast.makeText(getContext(), "No certificate data", Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }

        } else {
            if (certificate == null) {
                tvTitle.setText("Add New Certificate");
                btnSave.setText("Add");
            } else {
                tvTitle.setText("Edit Certificate");
                btnSave.setText("Update");
                etCertificateName.setText(certificate.getCertificateName());
                etIssuingAuthority.setText(certificate.getIssuingAuthority());
                etIssueDate.setText(certificate.getIssueDate());
                etExpiryDate.setText(certificate.getExpiryDate());
                etDescription.setText(certificate.getDescription());
            }
        }

        btnSave.setOnClickListener(v -> saveCertificate());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void initViews(View root) {
        tvTitle           = root.findViewById(R.id.tv_dialog_title);
        tilCertificateName= root.findViewById(R.id.til_certificate_name);
        tilIssuingAuthority = root.findViewById(R.id.til_issuing_authority);
        tilIssueDate      = root.findViewById(R.id.til_issue_date);
        tilExpiryDate     = root.findViewById(R.id.til_expiry_date);
        tilDescription    = root.findViewById(R.id.til_description);

        etCertificateName = root.findViewById(R.id.et_certificate_name);
        etIssuingAuthority= root.findViewById(R.id.et_issuing_authority);
        etIssueDate       = root.findViewById(R.id.et_issue_date);
        etExpiryDate      = root.findViewById(R.id.et_expiry_date);
        etDescription     = root.findViewById(R.id.et_description);

        tvStudentName     = root.findViewById(R.id.tv_student_name);
        btnSave           = root.findViewById(R.id.btn_save);
        btnCancel         = root.findViewById(R.id.btn_cancel);

        Log.d("CDebug", "btnCancel=" + btnCancel + ", btnSave=" + btnSave);
    }

    private void setupDatePickers() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        etIssueDate.setOnClickListener(v -> {
            new DatePickerDialog(getContext(),
                    (view, y, m, d) -> {
                        calendarIssue.set(y, m, d);
                        etIssueDate.setText(sdf.format(calendarIssue.getTime()));
                    },
                    calendarIssue.get(Calendar.YEAR),
                    calendarIssue.get(Calendar.MONTH),
                    calendarIssue.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        etExpiryDate.setOnClickListener(v -> {
            new DatePickerDialog(getContext(),
                    (view, y, m, d) -> {
                        calendarExpiry.set(y, m, d);
                        etExpiryDate.setText(sdf.format(calendarExpiry.getTime()));
                    },
                    calendarExpiry.get(Calendar.YEAR),
                    calendarExpiry.get(Calendar.MONTH),
                    calendarExpiry.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void loadStudentName() {
        String studentId = certificate != null ? certificate.getStudentId() : preSelectedStudentId;

        if (studentId != null && !studentId.isEmpty()) {
            db.collection("students")
                    .document(studentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Student student = documentSnapshot.toObject(Student.class);
                            if (student != null) {
                                student.setId(documentSnapshot.getId());
                                tvStudentName.setText("Student: " + student.getName());
                            } else {
                                tvStudentName.setText("Student: Unknown");
                            }
                        } else {
                            tvStudentName.setText("Student: Unknown");
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvStudentName.setText("Student: Unknown");
                        Toast.makeText(getContext(),
                                "Error loading student: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            tvStudentName.setText("Student: Not specified");
        }
    }

    private void saveCertificate() {
        String name  = etCertificateName.getText().toString().trim();
        String auth  = etIssuingAuthority.getText().toString().trim();
        String issue = etIssueDate.getText().toString().trim();
        String exp   = etExpiryDate.getText().toString().trim();
        String desc  = etDescription.getText().toString().trim();

        if (name.isEmpty()) {
            tilCertificateName.setError("Required");
            return;
        }
        if (auth.isEmpty()) {
            tilIssuingAuthority.setError("Required");
            return;
        }
        if (issue.isEmpty()) {
            tilIssueDate.setError("Required");
            return;
        }

        // Use existing student ID or preselected one
        String stuDoc = certificate != null ? certificate.getStudentId() : preSelectedStudentId;

        if (stuDoc == null || stuDoc.isEmpty()) {
            Toast.makeText(getContext(), "No student selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (certificate==null) {
            certificate = new Certificate();
            certificate.setCertificateName(name);
            certificate.setIssuingAuthority(auth);
            certificate.setIssueDate(issue);
            certificate.setExpiryDate(exp);
            certificate.setDescription(desc);
            certificate.setStudentId(stuDoc);

            db.collection("certificates")
                    .add(certificate.toMap())
                    .addOnSuccessListener(ref -> {
                        certificate.setId(ref.getId());
                        if (listener!=null) listener.onCertificateAdded(certificate);
                        Toast.makeText(getContext(),
                                "Added", Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .addOnFailureListener(e->{
                        Toast.makeText(getContext(),
                                "Error: "+e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });

        } else {
            certificate.setCertificateName(name);
            certificate.setIssuingAuthority(auth);
            certificate.setIssueDate(issue);
            certificate.setExpiryDate(exp);
            certificate.setDescription(desc);
            certificate.setStudentId(stuDoc);

            db.collection("certificates").document(certificate.getId())
                    .set(certificate.toMap())
                    .addOnSuccessListener(_v->{
                        if (listener!=null) listener.onCertificateUpdated(certificate);
                        Toast.makeText(getContext(),
                                "Updated", Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .addOnFailureListener(e->{
                        Toast.makeText(getContext(),
                                "Error: "+e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }
}