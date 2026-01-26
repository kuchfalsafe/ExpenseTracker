# Expense Tracker Android Application

An Android application to track expenses from multiple sources (HDFC UPI/Credit Card, ICICI Credit Card, SBI UPI) with automatic transaction scraping from Gmail.

## Features

### Daily Job Tasks
1. **Automatic Transaction Scraping**: Runs once daily to scrape transaction data from Gmail
2. **Multi-Source Support**: Supports transactions from:
   - HDFC UPI and Credit Card
   - ICICI Credit Card
   - SBI UPI
3. **Automatic Categorization**: Automatically categorizes expenses based on transaction descriptions
4. **Unknown Category Handling**: Marks transactions as "Unknown" if category cannot be identified

### Other Features
1. **Add New Category**: Option to add custom categories
2. **Manual Transaction Entry**: Add transactions manually
3. **Expenditure Summary**: View daily, weekly, and monthly expenditure summaries
4. **Category-wise Expenses**: View expenses by category for daily, weekly, and monthly periods

## Tech Stack

- **UI**: Jetpack Compose
- **Database**: Room (Local SQLite)
- **Background Jobs**: WorkManager
- **Gmail Integration**: Gmail API
- **Architecture**: MVVM with Repository pattern
- **Navigation**: Navigation Compose

## Setup Instructions

### 1. Gmail API Configuration

To enable Gmail transaction scraping, you need to set up Gmail API credentials:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable Gmail API for your project
4. Create OAuth 2.0 credentials (Android application type)
5. Add your app's package name and SHA-1 fingerprint
6. Download the `google-services.json` file (if using Firebase) or configure OAuth credentials

**Note**: The Gmail API integration requires proper OAuth setup. The current implementation uses `GoogleAccountCredential` which needs to be configured with your OAuth client ID.

### 2. Build and Run

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run the application

### 3. Initial Setup

- The app will automatically initialize default categories on first launch:
  - Food, Groceries, Transport, Shopping, Bills, Entertainment, Healthcare, Education, Travel, Utilities, Unknown

### 4. Gmail Authentication

When the daily job runs or when you manually trigger transaction scraping:
1. The app will prompt for Gmail account selection
2. Grant Gmail read permissions
3. The app will start scraping transaction emails

## Project Structure

```
app/src/main/java/com/kishan/expensetracker/
├── data/
│   ├── entity/          # Room entities (Transaction, Category)
│   ├── dao/             # Data Access Objects
│   ├── database/        # Room database setup
│   └── repository/      # Repository pattern implementation
├── ui/
│   ├── screens/         # Compose screens
│   ├── navigation/      # Navigation setup
│   └── theme/           # App theme
├── util/
│   ├── TransactionCategorizer.kt    # Categorization logic
│   ├── GmailTransactionScraper.kt   # Gmail scraping logic
│   └── WorkManagerHelper.kt         # WorkManager setup
├── viewmodel/           # ViewModels for UI
├── worker/              # WorkManager workers
└── ExpenseTrackerApp.kt # Application class
```

## Transaction Categorization

The app uses keyword matching to categorize transactions:
- **Food**: restaurant, zomato, swiggy, pizza, burger, etc.
- **Groceries**: grocery, bigbasket, dmart, etc.
- **Transport**: uber, ola, taxi, petrol, fuel, etc.
- **Shopping**: amazon, flipkart, myntra, etc.
- And more...

You can extend the categorization logic in `TransactionCategorizer.kt`.

## Daily Job Configuration

The daily job is scheduled using WorkManager and runs every 24 hours. It:
1. Connects to Gmail API
2. Searches for transaction emails from configured sources
3. Parses transaction details (amount, date, description)
4. Categorizes transactions
5. Stores them in the local database

## Permissions

The app requires:
- `INTERNET`: For Gmail API access
- `ACCESS_NETWORK_STATE`: To check network connectivity
- `GET_ACCOUNTS`: For Gmail account selection

## Notes

- The Gmail API integration requires proper OAuth 2.0 setup
- Transaction parsing relies on email format from banks - you may need to adjust regex patterns in `GmailTransactionScraper.kt` based on actual email formats
- The app stores all data locally using Room database
- WorkManager ensures the daily job runs even if the app is closed

## Future Enhancements

- Export transactions to CSV/Excel
- Data visualization with charts
- Budget tracking and alerts
- Multiple account support
- Transaction search and filters

