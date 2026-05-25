# Base64 Image Sharing Fallback

Firebase Storage is intentionally not used in this project environment because it is unavailable for the current setup. To keep image sharing remote and available to other users, selected post and profile images are resized, JPEG-compressed, encoded as `data:image/jpeg;base64,...`, and saved in Firestore string fields.

This is a fallback for the course environment, not the preferred production architecture. If Firebase Storage becomes available later, keep the display path compatible with existing Base64 strings so older posts continue to render.
