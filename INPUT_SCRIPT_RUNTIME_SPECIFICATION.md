# Input Script Runtime Specification

## 1. Overview

This document specifies the Input Script Runtime for the WMMT Remote Controller Android application. The runtime enables the execution of custom JavaScript scripts to process input data and generate output commands.

## 2. Core Design Principles

### 2.1 Stability
- **Frozen Execution Model**: The core execution model will remain stable for at least 2 years
- **Backward Compatibility**: Scripts written today must work with future versions of the runtime
- **Clear API Boundaries**: Explicit separation between stable and experimental APIs

### 2.2 Security
- **Sandboxed Execution**: Scripts run in a restricted environment
- **Controlled Capabilities**: Scripts can only access explicitly permitted functionality
- **Resource Limits**: Enforced execution time and memory constraints

### 2.3 Performance
- **Deterministic Execution**: Predictable performance characteristics
- **Low Overhead**: Minimal impact on the host application
- **Optimized API**: Efficient API design for common operations

## 3. Script Execution Model

### 3.1 Single Execution Entry Point

The runtime enforces a single execution entry point for all scripts:

```javascript
function update(raw, state, event) {
    // Mapping logic here
}
```

### 3.2 Parameter Contracts

#### `raw` Parameter
- **Type**: Read-only object
- **Purpose**: Provides access to all input data
- **Content**: RawInput data including gyro, touch, and gamepad inputs
- **Constraints**: Cannot be modified by scripts

#### `state` Parameter
- **Type**: Mutable only via API calls
- **Purpose**: Allows scripts to generate output commands
- **API**: Access via ScriptContext methods only
- **Constraints**: Direct modification is prohibited

#### `event` Parameter
- **Type**: Push-only event queue
- **Purpose**: Provides access to discrete events
- **Content**: GameInputEvent objects
- **Constraints**: Cannot be read back, only processed

### 3.3 Script Lifecycle

Scripts can implement optional lifecycle methods:

```javascript
// Optional initialization method
function init() {
    // One-time initialization logic
}

// Mandatory update method
function update(raw, state, event) {
    // Per-frame update logic
}

// Optional event handling method
function onEvent(event) {
    // Event-specific logic
}

// Optional cleanup method
function reset() {
    // Cleanup logic
}
```

### 3.4 Execution Constraints

- **Update Frequency**: Maximum 60 updates per second (16.67ms budget)
- **Maximum Execution Time**: 5ms per update call
- **Memory Limit**: 1MB per script
- **Error Handling**: Uncaught errors terminate script execution gracefully

## 4. ScriptContext API

### 4.1 Core API (Stable)

#### Keyboard Control
- **`holdKey(key: string)`**: Press and hold a keyboard key
- **`releaseKey(key: string)`**: Release a keyboard key
- **`releaseAllKeys()`**: Release all pressed keys
- **`isKeyHeld(key: string)`**: Check if a key is held

#### Mouse Control
- **`setMousePosition(x: number, y: number)`**: Set mouse position
- **`setMouseButton(button: string, pressed: boolean)`**: Set mouse button state

#### Input Access
- **`getGyro()`**: Get gyroscope data object
- **`getTouch()`**: Get touch data object
- **`getAxis(axisName: string)`**: Get gamepad axis value
- **`isGamepadButtonPressed(buttonName: string)`**: Check gamepad button state

### 4.2 Experimental API (Subject to Change)

- **`log(message: string)`**: Log a message to the host
- **`setConfig(key: string, value: any)`**: Set runtime configuration
- **`triggerEvent(eventName: string, data: any)`**: Trigger a custom event

### 4.3 API Categories

| Category | Methods | Usage Frequency |
|----------|---------|----------------|
| High-Frequency Safe | holdKey, releaseKey, setMousePosition, getGyro, getTouch | 60 FPS |
| Low-Frequency Only | log, setConfig, triggerEvent | ≤ 1 Hz |

## 5. Script Profile Structure

### 5.1 Metadata Format

```json
{
  "name": "WMMT Gamepad Profile",
  "version": "1.0.0",
  "author": "Line",
  "description": "Gamepad input profile for WMMT",
  "entry": "gamepad_input.js",
  "compatibility": {
    "gamepads": ["*"],
    "androidVersion": ">= 10"
  },
  "dependencies": [],
  "createdAt": "2026-01-21T12:00:00Z",
  "id": "wmmt-gamepad-1.0.0"
}
```

### 5.2 Required Fields
- **`name`**: Human-readable name
- **`version`**: Semantic version (major.minor.patch)
- **`author`**: Profile author
- **`entry`**: Main script file name

### 5.3 Optional Fields
- **`description`**: Profile description
- **`compatibility`**: Device compatibility information
- **`dependencies`**: Array of required dependencies
- **`createdAt`**: Creation timestamp
- **`id`**: Unique identifier

## 6. Import/Export Format

### 6.1 Package Structure

```
profile-package.zip/
├── profile.json      # Profile metadata
└── scripts/
    ├── main.js       # Main script file
    └── lib/          # Optional library files
```

### 6.2 Export Process
1. Validate profile metadata
2. Package script files and dependencies
3. Generate a unique ID if not provided
4. Create zip archive

### 6.3 Import Process
1. Validate archive structure
2. Verify profile metadata format
3. Extract script files
4. Validate script syntax
5. Register profile

## 7. Error Handling Strategy

### 7.1 Script Errors
- **Syntax Errors**: Detected during script loading
- **Runtime Errors**: Terminate script execution with error log
- **Timeout Errors**: Script terminated after exceeding execution time limit

### 7.2 Error Reporting
- Errors are logged to the host application
- Scripts can implement error handlers
- Host provides error codes for common failure scenarios

## 8. Security Model

### 8.1 Sandbox Restrictions
- No file system access
- No network access (except via host API)
- No direct device hardware access
- No reflection or dynamic code execution

### 8.2 Permission Model
- Scripts have implicit read access to input data
- Explicit permissions required for advanced functionality
- User must approve sensitive operations

## 9. Performance Guidelines

### 9.1 Best Practices
- Minimize API calls in update loop
- Cache frequently accessed values
- Avoid complex calculations in update function
- Use event handlers for infrequent operations

### 9.2 Performance Monitoring
- Runtime tracks script execution time
- Excessive resource usage triggers warnings
- Persistent violations may result in script suspension

## 10. Future Extensibility

### 10.1 API Evolution
- Stable APIs will be supported indefinitely
- Experimental APIs may be promoted to stable or removed
- Breaking changes require major version increment

### 10.2 New Features
- New input types can be added to the `raw` parameter
- New output capabilities can be added via API extensions
- Scripts can opt-in to new features via version constraints

## 11. Compliance Requirements

### 11.1 Script Validation
- All scripts must pass syntax validation
- API usage must follow documented constraints
- Resource usage must stay within limits

### 11.2 Versioning Compliance
- Scripts must specify compatibility constraints
- Profiles must use semantic versioning

## 12. Conclusion

This specification document defines the core architecture and constraints for the Input Script Runtime. By adhering to these specifications, the runtime will provide a stable, secure, and performant platform for custom input scripts while maintaining long-term compatibility and extensibility.