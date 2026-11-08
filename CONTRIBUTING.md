# Contributing to Smart Dialer & Caller ID

We welcome contributions from the community! Here is how you can help:

## Development Workflow

1. **Fork the Repository:**
   Fork the repository on GitHub and clone your fork locally:
   ```bash
   git clone https://github.com/rsk934128-dot/Smart-Dialer-Caller-ID.git
   cd Smart-Dialer-Caller-ID
   ```

2. **Create a Feature Branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Code Style & Guidelines:**
   - Adhere to Android Kotlin coding conventions.
   - Use Jetpack Compose with Material Design 3 tokens.
   - Write unit tests for new utility engines or repositories.

4. **Run Tests & Verify Build:**
   ```bash
   gradle :app:testDebugUnitTest
   gradle assembleDebug
   ```

5. **Commit & Push:**
   ```bash
   git commit -m "feat: describe your new feature or fix"
   git push origin feature/your-feature-name
   ```

6. **Open a Pull Request:**
   Submit your pull request describing the changes and referencing any relevant issues.
