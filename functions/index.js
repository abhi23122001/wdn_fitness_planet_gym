const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

exports.sendNotificationPush = onDocumentCreated("notifications/{notificationId}", async (event) => {
  const data = event.data && event.data.data();
  if (!data) return;

  const title = String(data.title || "Fitness Planet Gym");
  const body = String(data.body || "");
  const snap = await db.collection("users").get();
  const tokens = [];

  snap.forEach((doc) => {
    const user = doc.data();
    if (String(user.role || "").toLowerCase() !== "admin" &&
        typeof user.fcmToken === "string" && user.fcmToken.length > 0) {
      tokens.push(user.fcmToken);
    }
  });

  for (let i = 0; i < tokens.length; i += 500) {
    await admin.messaging().sendEachForMulticast({
      tokens: tokens.slice(i, i + 500),
      notification: { title, body },
      data: { title, body, notificationId: event.params.notificationId },
      android: {
        priority: "high",
        notification: { channelId: "fitness_planet_general" }
      }
    });
  }
});
