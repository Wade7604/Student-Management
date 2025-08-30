package com.example.studentmanagement.dialog;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.example.studentmanagement.R;
import com.example.studentmanagement.models.Student;
import com.example.studentmanagement.utils.CSVUtils;

import java.util.List;

public class ImportExportDialog {
    private AlertDialog dialog;
    private Button btnImport, btnExport;
    private TextView tvTitle;
    private ProgressBar progressBar;
    private Context context;
    private List<Student> studentList;
    private ImportExportListener listener;
    private ProgressDialog progressDialog;

    // Use launchers from the fragment instead of creating new ones
    private ActivityResultLauncher<Intent> importFileLauncher;
    private ActivityResultLauncher<Intent> exportFileLauncher;

    public interface ImportExportListener {
        void onImportCompleted();
        void onExportCompleted();
    }

    // Modified constructor that accepts pre-registered launchers
    public ImportExportDialog(Context context,
                              List<Student> studentList,
                              ImportExportListener listener,
                              ActivityResultLauncher<Intent> importFileLauncher,
                              ActivityResultLauncher<Intent> exportFileLauncher) {
        this.context = context;
        this.studentList = studentList;
        this.listener = listener;
        this.importFileLauncher = importFileLauncher;
        this.exportFileLauncher = exportFileLauncher;

        init();
    }

    private void init() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_import_export, null);

        // Initialize views
        tvTitle = view.findViewById(R.id.tv_title);
        btnImport = view.findViewById(R.id.btn_import);
        btnExport = view.findViewById(R.id.btn_export);
        progressBar = view.findViewById(R.id.progress_bar);

        // Set click listeners
        btnImport.setOnClickListener(v -> openImportFilePicker());
        btnExport.setOnClickListener(v -> openExportFilePicker());

        // Build dialog with a negative button instead of trying to use btnCancel
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        builder.setCancelable(true);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        dialog = builder.create();

        // Initialize progress dialog for long operations
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
    }

    private void openImportFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/csv"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        importFileLauncher.launch(intent);
        dialog.dismiss(); // Close dialog as we'll wait for the file picker result
    }

    private void openExportFilePicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "students_export.csv");
        exportFileLauncher.launch(intent);
        dialog.dismiss(); // Close dialog as we'll wait for the file picker result
    }

    public void show() {
        dialog.show();
    }
}