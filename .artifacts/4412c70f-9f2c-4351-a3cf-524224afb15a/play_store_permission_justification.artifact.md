# Google Play Console: SMS Permission Justification Draft

If you decide to keep the **SMS Search** feature, you will need to complete the "Permissions Declaration Form" in the Google Play Console. Use the following drafts to prepare your submission.

> [!CAUTION]
> **Probability of Approval**: **Low to Moderate**.
> Google typically restricts SMS permissions to apps that are the **Default SMS Handler**. Because FREE Launcher is a launcher, Google may argue that search can be performed within the native SMS app. However, the drafts below focus on "Core Launcher Efficiency" as the primary argument.

---

## 1. Core Functionality Declaration
**Question**: What is the core functionality of your app that requires these permissions?

**Draft Response**:
FREE Launcher is a minimalist productivity tool designed to consolidate all device interactions into a single, distraction-free interface. The **Universal Search** feature is a core component of this experience, allowing users to find apps, contacts, and relevant information (including text messages) without navigating through multiple distracting third-party application UIs. This "Search-First" approach is fundamental to the app's goal of reducing screen time and improving user intentionality.

---

## 2. Specific Permission Use Case: `READ_SMS`
**Question**: Describe the user-facing feature that uses this permission.

**Draft Response**:
The `READ_SMS` permission powers the **"Local Message Search"** within our Universal Search bar. This feature allows users to quickly find specific text conversations or snippets directly from the home screen. By providing this integrated search, the app prevents "mindless scrolling" that occurs when users are forced to open a full SMS application and get distracted by new notifications or other threads.

---

## 3. Prominent Disclosure & Data Safety
**Question**: How is the user informed about this permission?

**Draft Response**:
The app follows a strict **Opt-In** model.
1. **Prominent Disclosure**: Within the Universal Search results, a section titled "Messages" clearly displays a "PROCESSED LOCALLY" tag.
2. **Explicit Consent**: Before requesting the permission, users must click a dedicated "Enable Message Search" button which triggers a clear explanation of why the permission is needed.
3. **Local-Only Processing**: As stated in our Data Safety section, no SMS data is stored, transmitted off the device, or shared with third parties. All search indexing happens in real-time within the device's secure environment.

---

## 4. Alternative Functionality
**Question**: Why can't this feature be implemented without the permission?

**Draft Response**:
Access to the SMS provider is technically required to provide the search results within our UI. Without this permission, we cannot offer the consolidated, unified search experience that is the hallmark of the FREE Launcher productivity suite.

---

## 💡 Recommendation: "The Backup Plan"
If Google rejects this justification, I recommend **removing the `READ_SMS` permission** entirely and keeping only the **`READ_CONTACTS`** permission. Contacts are much easier to justify for a launcher, and removing SMS will almost certainly guarantee approval for the rest of your app.
