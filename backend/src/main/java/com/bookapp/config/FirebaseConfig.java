package com.bookapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path:classpath:serviceAccountKey.json}")
    private Resource firebaseConfigPath;

    @Value("${FIREBASE_SERVICE_ACCOUNT_KEY:}")
    private String firebaseServiceAccountKey;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = null;

                // Try environment variable first (for production)
                if (firebaseServiceAccountKey != null && !firebaseServiceAccountKey.isEmpty()) {
                    System.out.println("Initializing Firebase from environment variable...");
                    serviceAccount = new ByteArrayInputStream(
                            firebaseServiceAccountKey.getBytes(StandardCharsets.UTF_8)
                    );
                }
                // Fall back to file (for local development)
                else if (firebaseConfigPath.exists()) {
                    System.out.println("Initializing Firebase from file...");
                    serviceAccount = firebaseConfigPath.getInputStream();
                } else {
                    System.err.println("Firebase config not found. Push notifications will not work.");
                    System.err.println("Set FIREBASE_SERVICE_ACCOUNT_KEY environment variable or provide serviceAccountKey.json file.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin initialized successfully.");
            }
        } catch (Exception e) {
            System.err.println("Error initializing Firebase Admin: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
