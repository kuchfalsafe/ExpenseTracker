# Gmail Integration Setup Guide

## Overview
The Expense Tracker app can automatically import transactions from Gmail emails sent by:
- HDFC Bank (UPI and Credit Card)
- ICICI Bank (Credit Card)
- SBI (UPI)

## Prerequisites
1. Google Cloud Console account
2. Android app with package name: `com.kishan.expensetracker`
3. SHA-1 certificate fingerprint from your debug/release keystore

## Step 1: Google Cloud Console Setup

### 1.1 Create a Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Note your project name

### 1.2 Enable Gmail API
1. Navigate to **APIs & Services** > **Library**
2. Search for "Gmail API"
3. Click **Enable**

### 1.3 Create OAuth 2.0 Credentials
1. Go to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **OAuth client ID**
3. If prompted, configure the OAuth consent screen:
   - User Type: **External** (for testing) or **Internal** (for organization)
   - App name: **Expense Tracker**
   - User support email: Your email
   - Developer contact: Your email
   - Scopes: Add `https://www.googleapis.com/auth/gmail.readonly`
   - Save and continue
4. Create OAuth Client ID:
   - Application type: **Android**
   - Name: **Expense Tracker Android**
   - Package name: `com.kishan.expensetracker`
   - SHA-1 certificate fingerprint: (see Step 2 below)
   - Click **Create**

### 1.4 Get SHA-1 Fingerprint

#### For Debug Build:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### For Release Build:
```bash
keytool -list -v -keystore /path/to/your/keystore.jks -alias your-key-alias
```

Copy the SHA-1 value (looks like: `AA:BB:CC:DD:...`)

### 1.5 Download Credentials
1. After creating OAuth client ID, download the JSON file
2. Place it in `app/src/main/res/raw/` (you may need to create the `raw` folder)
3. Rename it to `credentials.json` (optional, but recommended)

## Step 2: Update App Configuration

### 2.1 Add Credentials to App
The app uses `GoogleAccountCredential` which automatically handles OAuth flow using the device's Google accounts. No manual credential file is needed if you're using the standard Android OAuth flow.

### 2.2 Update AndroidManifest.xml
The manifest already includes necessary permissions:
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `GET_ACCOUNTS`

## Step 3: Using Gmail Sync

### 3.1 First Time Setup
1. Open the app
2. Tap the **Settings** icon (⚙️) in the top bar
3. You'll see the **Gmail Sync** screen
4. Select your Google account from the list
5. The app will request Gmail read-only permission
6. Grant permission when prompted

### 3.2 Manual Sync
1. Go to **Gmail Sync** screen
2. Ensure an account is selected
3. Tap **Sync Now** button
4. Wait for sync to complete
5. Check the status message for results

### 3.3 Automatic Daily Sync
- The app automatically schedules a daily background job
- It runs once per day to fetch new transactions
- Requires device to be connected to internet
- Only syncs if user has authenticated

## Step 4: Email Parsing

The app searches for emails with these patterns:

### HDFC UPI:
- From: `noreply@hdfcbank.net` OR `alerts@hdfcbank.net`
- Subject contains: `UPI`

### HDFC Credit Card:
- From: `noreply@hdfcbank.net` OR `alerts@hdfcbank.net`
- Subject contains: `credit`

### ICICI Credit Card:
- From: `alerts@icicibank.com`
- Subject contains: `transaction`

### SBI UPI:
- From: `alerts@sbi.co.in` OR `onlinesbi@sbi.co.in`
- Subject contains: `UPI`

## Troubleshooting

### "No Google accounts found"
- Add a Google account in device Settings > Accounts
- Ensure the account has Gmail enabled

### "Sync failed" errors
- Check internet connection
- Verify Gmail API is enabled in Google Cloud Console
- Ensure OAuth consent screen is configured
- Check that SHA-1 fingerprint matches in Google Cloud Console

### "Permission denied"
- Go to device Settings > Apps > Expense Tracker > Permissions
- Grant necessary permissions
- Re-authenticate in the app

### Transactions not appearing
- Check email search queries match your bank's email format
- Verify emails are in your Gmail inbox (not just sent items)
- Check if transactions are being filtered as duplicates
- Review email parsing patterns in `GmailTransactionScraper.kt`

## Security Notes

- The app only requests `gmail.readonly` scope
- No write access to your Gmail
- Credentials are stored securely on device
- All API calls are encrypted (HTTPS)

## Testing

1. Send a test transaction email to your Gmail
2. Wait a few minutes for email to arrive
3. Open app and tap "Sync Now"
4. Check if transaction appears in the app
5. Verify amount, date, and category are correct

## Next Steps

- Customize email parsing patterns for your specific bank formats
- Add more transaction sources
- Improve categorization logic
- Add sync status notifications

