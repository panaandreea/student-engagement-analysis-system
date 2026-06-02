package com.student.engagement.system.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.student.engagement.system.R;
import com.student.engagement.system.activities.LoginActivity;
import com.student.engagement.system.activities.SecurityActivity;
import com.student.engagement.system.session.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ProfileFragment extends Fragment {
    private static final String TAG = "PROFILE";
    private ImageView ivProfile;
    private TextView tvName, tvEmail;
    private View layoutLogout, layoutSecurity;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && saveImageToInternalStorage(uri)) {

                    if (!isAdded()) return;

                    loadProfileImage();
                    Toast.makeText(requireContext(), "Profile image updated!", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivProfile = view.findViewById(R.id.ivProfile);
        tvName = view.findViewById(R.id.tvProfileName);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        layoutSecurity = view.findViewById(R.id.layoutSecurity);
        layoutLogout = view.findViewById(R.id.layoutLogout);

        displayUserInformation();
        loadProfileImage();

        layoutSecurity.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), SecurityActivity.class));
        });

        ivProfile.setOnClickListener(v -> imagePicker.launch("image/*"));

        layoutLogout.setOnClickListener(v -> performLogout());
    }

    private void displayUserInformation() {
        String firstName = SessionManager.getFirstName(requireContext());
        String lastName = SessionManager.getLastName(requireContext());
        String email = SessionManager.getEmail(requireContext());

        String fullName = (firstName + " " + lastName).trim();

        tvName.setText(fullName.isEmpty() ? "User" : fullName);
        tvEmail.setText(
                (email == null || email.isEmpty()) ? "teacher@ie.ase.ro" : email
        );
    }

    private void loadProfileImage() {
        File file = new File(requireContext().getFilesDir(), getFileNameForCurrentUser());

        if (file.exists()) {
            ivProfile.setImageURI(null);
            ivProfile.setImageURI(Uri.fromFile(file));
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile_profile);
        }
    }

    private boolean saveImageToInternalStorage(Uri uri) {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        File file = new File(context.getFilesDir(), getFileNameForCurrentUser());


        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);

             OutputStream outputStream = new FileOutputStream(file)) {

            if (inputStream == null) return false;

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error saving image: ", e);
            return false;
        }
    }

    private String getFileNameForCurrentUser() {
        String userId = SessionManager.getUserId(requireContext());
        return "profile_img_" +
                (userId != null && !userId.isEmpty() ? userId : "default") +
                ".jpg";
    }

    private void performLogout() {
        SessionManager.clearSession(requireContext());
        redirectToLogin();
    }

    private void redirectToLogin(){
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}