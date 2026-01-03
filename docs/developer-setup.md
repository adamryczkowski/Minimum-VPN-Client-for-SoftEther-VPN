# Developer Setup Guide

This guide will help you set up your development environment for SoftEther Connect.

## Prerequisites

### Required Software

- **Git**: Version control
- **Java 17**: Required for Android development
- **Android Studio** (optional): IDE with Android support
- **mise**: Tool version manager (recommended)

### Recommended Tools

- **just**: Command runner for build tasks
- **pre-commit**: Git hooks for code quality
- **bat**: Better `cat` with syntax highlighting

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/softether-android.git
cd softether-android
```

### 2. Install mise (Recommended)

mise manages Java and Android SDK versions automatically.

```bash
# Install mise
curl https://mise.run | sh

# Activate mise in your shell
echo 'eval "$(mise activate bash)"' >> ~/.bashrc
source ~/.bashrc

# Install tools defined in .mise.toml
mise install
```

### 3. Set Up Android SDK

If using mise, the Android SDK is configured automatically. Otherwise:

```bash
# Set ANDROID_HOME
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Accept licenses
yes | sdkmanager --licenses
```

### 4. Install Pre-commit Hooks

```bash
# Install pre-commit
pip install pre-commit

# Install hooks
pre-commit install --install-hooks
```

### 5. Build the Project

```bash
# Using just (recommended)
just build

# Or using Gradle directly
./gradlew assembleDebug
```

## Project Structure

```
softether-android/
├── app/                    # Android application
│   ├── src/
│   │   ├── main/          # Main source code
│   │   │   ├── java/      # Kotlin source files
│   │   │   └── res/       # Android resources
│   │   ├── test/          # Unit tests
│   │   └── androidTest/   # Instrumented tests
│   └── build.gradle       # App build configuration
├── docs/                   # Documentation
├── gradle/                 # Gradle wrapper
├── ref/                    # Reference implementations
├── scripts/                # Build scripts
├── .mise.toml              # mise configuration
├── .pre-commit-config.yaml # Pre-commit hooks
├── build.gradle            # Root build configuration
├── detekt.yml              # Detekt configuration
├── justfile                # Just commands
└── settings.gradle         # Gradle settings
```

## Development Workflow

### Building

```bash
# Build debug APK
just build

# Build release APK
just build-release

# Clean build artifacts
just clean
```

### Testing

```bash
# Run unit tests
just test

# Run tests with coverage
just test-coverage

# Run instrumented tests (requires device/emulator)
just test-instrumented

# Run all tests
just test-all
```

### Code Quality

```bash
# Format code with ktlint
just format

# Run linting (ktlint + detekt)
just lint

# Run detekt only
just detekt

# Create detekt baseline
just detekt-baseline

# Full validation (format + lint + test)
just validate
```

### Installing

```bash
# Install debug APK to connected device
just install

# Install and run
just run
```

## IDE Setup

### Android Studio

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the project directory
4. Wait for Gradle sync to complete

#### Recommended Plugins

- **Kotlin**: Built-in
- **ktlint**: Code formatting
- **Detekt**: Static analysis
- **Mermaid**: Diagram preview

### VS Code

1. Install extensions:
   - Kotlin Language
   - Gradle for Java
   - Android

2. Open the project folder

3. Configure settings:

```json
{
  "kotlin.languageServer.enabled": true,
  "java.home": "/path/to/java17"
}
```

## Environment Variables

### For Testing

Create a `.env` file (copy from `.env.example`):

```bash
cp .env.example .env
```

Edit `.env` with your test server credentials:

```bash
# VPN Test Server Configuration
TEST_HOST=your-vpn-server.example.com
TEST_PORT=443
TEST_USERNAME=testuser
TEST_PASSWORD=testpassword
TEST_HUB=DEFAULT
```

### For Building

```bash
# Android SDK location (if not using mise)
export ANDROID_HOME=$HOME/Android/Sdk

# Java home (if not using mise)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

## Dependency Injection

The project uses Koin for dependency injection. Dependencies are defined in:

- `app/src/main/java/kittoku/mvc/di/AppModule.kt`

### Adding New Dependencies

```kotlin
val appModule = module {
    // Singleton
    single { MyRepository(androidContext()) }

    // Factory (new instance each time)
    factory { MyService(get()) }

    // ViewModel
    viewModel { MyViewModel(get(), get()) }
}
```

### Using Dependencies

```kotlin
// In Activity/Fragment
class MyFragment : Fragment() {
    private val viewModel: MyViewModel by viewModel()
}

// In ViewModel
class MyViewModel(
    private val repository: MyRepository
) : ViewModel()
```

## Testing

### Unit Tests

Located in `app/src/test/java/`:

```kotlin
class MyTest {
    @Test
    fun `test something`() {
        // Arrange
        val subject = MyClass()

        // Act
        val result = subject.doSomething()

        // Assert
        assertThat(result).isEqualTo(expected)
    }
}
```

### Mocking with MockK

```kotlin
class MyViewModelTest {
    @MockK
    private lateinit var repository: MyRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `test with mock`() {
        every { repository.getData() } returns flowOf(data)

        val viewModel = MyViewModel(repository)

        // Test viewModel behavior
    }
}
```

### Running Specific Tests

```bash
# Run a specific test class
./gradlew test --tests "kittoku.mvc.MyTest"

# Run tests matching a pattern
./gradlew test --tests "*ViewModel*"
```

## Debugging

### Logcat

```bash
# View all logs
adb logcat

# Filter by tag
adb logcat -s "SoftEther"

# Filter by package
adb logcat --pid=$(adb shell pidof -s kittoku.mvc)
```

### Network Debugging

Enable logging in the app settings to capture VPN traffic details.

### Remote Debugging

1. Enable USB debugging on device
2. Connect via USB
3. In Android Studio: Run > Attach Debugger to Android Process

## Common Issues

### Gradle Sync Failed

```bash
# Clean and rebuild
./gradlew clean
./gradlew build --refresh-dependencies
```

### SDK Not Found

```bash
# Check SDK location
echo $ANDROID_HOME

# Or create local.properties
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

### Java Version Mismatch

```bash
# Check Java version
java -version

# Should be Java 17
# If using mise:
mise install java@17
```

### Pre-commit Hook Failures

```bash
# Run hooks manually to see errors
pre-commit run --all-files

# Update hooks
pre-commit autoupdate
```

## Contributing

### Code Style

- Follow Kotlin coding conventions
- Use ktlint for formatting
- Pass detekt static analysis
- Write tests for new features

### Commit Messages

Follow conventional commits:

```
type(scope): description

feat(vpn): add UDP acceleration support
fix(dhcp): handle timeout correctly
docs(readme): update installation instructions
test(unit): add ViewModel tests
```

### Pull Request Process

1. Create a feature branch
2. Make changes with tests
3. Run `just validate`
4. Push and create PR
5. Wait for review

## Resources

- [Architecture Documentation](architecture.md)
- [Protocol Implementation](protocol-implementation.md)
- [Known Issues](known-issues.md)
- [Manual Testing Checklist](manual-testing-checklist.md)

## Getting Help

- Check existing issues on GitHub
- Review the documentation
- Ask in the project discussions
