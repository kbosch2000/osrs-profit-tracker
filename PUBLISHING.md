# Publishing to RuneLite Plugin Hub

## Local testing

Run `run-profit-tracker-dev.bat` to launch a RuneLite development client with Profit Tracker loaded.

The normal installed RuneLite client only installs external plugins through the Plugin Hub. Until this plugin is accepted there, use the development launcher.

## GitHub upload

1. Sign in to GitHub CLI:

   ```powershell
   gh auth login
   ```

2. Create a public GitHub repo from this project:

   ```powershell
   gh repo create osrs-profit-tracker --public --source . --remote origin --push
   ```

3. Copy the final commit hash:

   ```powershell
   git rev-parse HEAD
   ```

## RuneLite Plugin Hub submission

RuneLite's Plugin Hub submission flow is:

1. Fork `https://github.com/runelite/plugin-hub`.
2. Create a branch.
3. Add a file under `plugins/` containing:

   ```text
   repository=https://github.com/<your-github-user>/osrs-profit-tracker.git
   commit=<the-full-40-character-commit-hash>
   ```

4. Commit that one new file, push it to your fork, and open a pull request back to `runelite/plugin-hub`.

Official instructions: https://github.com/runelite/plugin-hub
