# Configure administrator access

Google and email/password accounts start as regular travelers. Firestore authorizes management only when the signed-in account has a verified email and the matching entry is enabled.

1. In Firebase Console, open **Firestore Database** in the app project and select the named database listed in `firebase.json`.
2. Create the collection **admin_emails**. For each person you choose, use their complete lowercase email address as the document ID. Add a **boolean** field named **enabled** and set it to **true**. You can create this entry before the person registers.
3. For initial rollout, create all intended entries before deploying the repository’s Firestore rules to that database. Release the matching Android build. The former hardcoded address and UID grants confer no access under the new rules.
4. Have the administrator sign in online with the approved address. For an unverified email/password account, open **Reviews → Verify email**, follow the email link, and sign in again.

To revoke access, set **enabled** to **false** or delete the entry. The rules check the entry on each protected request. The app listens for changes and requires a server-confirmed entry to display management controls. Only trusted Firebase project operators can edit this list.

Administrators can manage shared transit schedules, attractions and emergency contacts, and view the traveler directory. Regular travelers can browse published content and use their own profile, plans and reviews.
