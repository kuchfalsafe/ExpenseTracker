# GitHub Setup Guide for Personal Account - First Time Setup

This guide helps you set up individual projects as separate git repositories under your `Self` folder, and push them to your personal GitHub account, while keeping your Wayfair credentials for work projects.

**Key Points:**
- The `Self` folder is **NOT** a git repository - it's just an organizational folder
- Each project is its own separate git repository with its own `.git` folder
- Projects are organized under category folders:
  - `AndroidStudioProjects/` - All Android projects (each is a separate repo)
  - `WebDevelopmentProjects/` - All web development projects (each is a separate repo)
- Each project can be pushed to its own GitHub repository

## Repository Structure

```
Self/
├── AndroidStudioProjects/
│   ├── ExpenseTracker/          # Git repo A
│   │   ├── .git/
│   │   └── ...
│   └── AnotherApp/             # Git repo B
│       ├── .git/
│       └── ...
└── WebDevelopmentProjects/
    └── MyWebApp/                # Git repo C
        ├── .git/
        └── ...
```

## Step 1: Initialize Git Repository for Your Project

For each project you want to push to GitHub, initialize git in that project's folder:

```bash
# Navigate to your project folder (example: ExpenseTracker)
cd ~/Downloads/Self/AndroidStudioProjects/ExpenseTracker

# Initialize git repository
git init

# Set default branch to main
git branch -m main
```

**Repeat this for each project** you want to version control.

## Step 2: Configure Local Git Credentials for Each Project

For each project, set your personal email and name. These settings only apply to that specific project:

```bash
# Navigate to your project folder
cd ~/Downloads/Self/AndroidStudioProjects/ExpenseTracker

# Set your personal email (replace with your personal GitHub email)
git config user.email "your-personal-email@example.com"

# Set your personal name
git config user.name "Kishan Sharma"

# Verify the configuration
git config user.email
git config user.name
```

**Important:** You need to do this for **each project** separately. This ensures each project uses your personal credentials while your global settings (Wayfair email) remain for work projects.

## Step 3: Set Up GitHub Authentication

You have a few options for authentication:

### Option A: Use Personal Access Token (Recommended for First Time)

1. Go to GitHub.com and sign in with your **personal account**
2. Go to **Settings** > **Developer settings** > **Personal access tokens** > **Tokens (classic)**
3. Click **Generate new token (classic)**
4. Give it a name like "Personal-Projects"
5. Select scopes: `repo` (full control of private repositories)
6. Click **Generate token**
7. **Copy the token** (you won't see it again!) - save it securely

### Option B: Use SSH Key (More Secure)

1. Generate an SSH key for your personal account:
   ```bash
   ssh-keygen -t ed25519 -C "your-personal-email@example.com" -f ~/.ssh/id_ed25519_personal
   ```
   - Press Enter to accept default location
   - Enter a passphrase (recommended) or press Enter for no passphrase

2. Add the SSH key to your ssh-agent:
   ```bash
   eval "$(ssh-agent -s)"
   ssh-add ~/.ssh/id_ed25519_personal
   ```

3. Copy your public key:
   ```bash
   cat ~/.ssh/id_ed25519_personal.pub
   ```

4. Go to GitHub.com > **Settings** > **SSH and GPG keys** > **New SSH key**
5. Paste your public key, give it a title like "Personal Projects", and save

## Step 4: Create GitHub Repository for Each Project

For each project, create a separate GitHub repository:

1. Go to [GitHub.com](https://github.com) and sign in with your **personal account**
2. Click the **+** icon in the top right > **New repository**
3. Repository name: `ExpenseTracker` (or match your project name)
4. Description: "Android Expense Tracker App with Gmail Integration" (or appropriate description)
5. Choose **Public** or **Private** (recommend Private for personal projects)
6. **DO NOT** initialize with README, .gitignore, or license (if your project already has these)
7. Click **Create repository**

**Repeat this for each project** you want to push to GitHub.

## Step 5: Add Remote and Push Each Project

For each project, add the remote and push:

### If using Personal Access Token:

```bash
# Navigate to your project folder
cd ~/Downloads/Self/AndroidStudioProjects/ExpenseTracker

# Add remote (replace YOUR_USERNAME with your GitHub username and REPO_NAME with your repo name)
git remote add origin https://YOUR_USERNAME@github.com/YOUR_USERNAME/ExpenseTracker.git

# Stage all files
git add .

# Create initial commit
git commit -m "Initial commit: Expense Tracker Android app"

# Push to GitHub (you'll be prompted for password - use your Personal Access Token)
git push -u origin main
```

**Note:** When prompted for password, paste your Personal Access Token (not your GitHub password).

### If using SSH:

```bash
# Navigate to your project folder
cd ~/Downloads/Self/AndroidStudioProjects/ExpenseTracker

# Add remote (replace YOUR_USERNAME with your GitHub username and REPO_NAME with your repo name)
git remote add origin git@github.com:YOUR_USERNAME/ExpenseTracker.git

# Stage all files
git add .

# Create initial commit
git commit -m "Initial commit: Expense Tracker Android app"

# Push to GitHub
git push -u origin main
```

**Repeat Steps 4 and 5 for each project** (a, b, c, etc.) you want to push.

## Step 6: Verify

For each project:
1. Go to your GitHub repository page: `https://github.com/YOUR_USERNAME/REPO_NAME`
2. You should see all your project files there
3. Check that commits show your personal email (not Wayfair email)

## Adding New Projects

When you create new projects in the future:

### For Android Projects:
```bash
# Create your project in Android Studio or manually
cd ~/Downloads/Self/AndroidStudioProjects
# Your new project folder will be created here

# Then initialize git for that project
cd YourNewProject
git init
git branch -m main
git config user.email "your-personal-email@example.com"
git config user.name "Kishan Sharma"

# Create GitHub repo and push (follow Steps 4-5)
```

### For Web Development Projects:
```bash
# Create your project
cd ~/Downloads/Self/WebDevelopmentProjects
mkdir my-web-project
cd my-web-project
# Initialize your web project (npm init, etc.)

# Then initialize git for that project
git init
git branch -m main
git config user.email "your-personal-email@example.com"
git config user.name "Kishan Sharma"

# Create GitHub repo and push (follow Steps 4-5)
```

## Future Pushes for Each Project

After initial setup, for each project:
```bash
cd ~/Downloads/Self/AndroidStudioProjects/ExpenseTracker  # or your project path
git add .
git commit -m "Your commit message"
git push
```

## Managing Multiple Repositories

Since each project is a separate repository, you'll need to:

1. **Navigate to each project** to make commits and pushes
2. **Configure credentials once per project** (Step 2) - they'll be remembered
3. **Create separate GitHub repos** for each project (Step 4)

### Quick Check: Which Projects Are Git Repos?

To see which projects under Self are git repositories:

```bash
cd ~/Downloads/Self
find . -name ".git" -type d
```

This will show all projects that have git initialized.

## Troubleshooting

### "Permission denied" error
- Make sure you're using your **personal** GitHub account credentials
- If using token, ensure you copied the full token and use it as password
- If using SSH, ensure you added the correct SSH key to GitHub and it's loaded in ssh-agent

### Wrong email in commits
- Check local config for that project: `cd project-folder && git config user.email`
- If wrong, update it: `git config user.email "your-personal-email@example.com"`
- For existing commits, you may need to amend: `git commit --amend --reset-author`

### "fatal: not a git repository"
- Make sure you're in the project folder (not the Self folder)
- Check if `.git` folder exists: `ls -la | grep .git`
- If missing, initialize git: `git init`

### Want to use different credentials per project?
- **Projects under Self**: Each uses personal credentials (local config per project)
- **Work projects**: Use Wayfair credentials (global config)
- No need to change anything - git automatically uses the right credentials per repository!

### Excluding specific files/folders
- Each project should have its own `.gitignore` file
- Edit the `.gitignore` in the project root to add patterns
- Files already tracked: `git rm --cached filename` then add to .gitignore

## Switching Between Work and Personal Accounts

- **Projects under Self folder**: Each uses personal credentials (local config per project)
- **Work projects**: Use Wayfair credentials (global config)
- No need to change anything - git automatically uses the right credentials per repository!

## Best Practices

- **One project = One git repository**: Each project has its own `.git` folder
- **Organize by category**: Keep projects in AndroidStudioProjects, WebDevelopmentProjects, etc.
- **Separate GitHub repos**: Each project gets its own GitHub repository
- **Project-specific .gitignore**: Each project should have its own `.gitignore` file
- **Use descriptive commit messages**: Helpful for tracking changes
- **Keep sensitive files out**: Use .gitignore for keystores, API keys, etc.

## Example: Setting Up Multiple Projects

Let's say you have 3 projects to set up:

**Project A (Android) - ExpenseTracker:**
```bash
cd ~/Downloads/Self/AndroidStudioProjects/ExpenseTracker
git init
git branch -m main
git config user.email "your-personal-email@example.com"
git config user.name "Kishan Sharma"
# Create GitHub repo: ExpenseTracker
git remote add origin https://YOUR_USERNAME@github.com/YOUR_USERNAME/ExpenseTracker.git
git add .
git commit -m "Initial commit"
git push -u origin main
```

**Project B (Android) - AnotherApp:**
```bash
cd ~/Downloads/Self/AndroidStudioProjects/AnotherApp
git init
git branch -m main
git config user.email "your-personal-email@example.com"
git config user.name "Kishan Sharma"
# Create GitHub repo: AnotherApp
git remote add origin https://YOUR_USERNAME@github.com/YOUR_USERNAME/AnotherApp.git
git add .
git commit -m "Initial commit"
git push -u origin main
```

**Project C (Web) - MyWebApp:**
```bash
cd ~/Downloads/Self/WebDevelopmentProjects/MyWebApp
git init
git branch -m main
git config user.email "your-personal-email@example.com"
git config user.name "Kishan Sharma"
# Create GitHub repo: MyWebApp
git remote add origin https://YOUR_USERNAME@github.com/YOUR_USERNAME/MyWebApp.git
git add .
git commit -m "Initial commit"
git push -u origin main
```

Each project is independent and can be managed separately!
