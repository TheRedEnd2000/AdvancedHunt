# Minecraft Head Editor - Modular Structure

A modular pixel art editor for creating and editing Minecraft player head skins with 2D and 3D preview capabilities.

## Project Structure

```
PlayerHeadIcon_Editor/
├── index.html          # Original monolithic version (legacy)
├── index_new.html      # New modular HTML entry point
├── styles.css          # All CSS styles
├── js/
│   ├── app.js          # Main application controller
│   ├── canvas.js       # Canvas rendering and management
│   ├── constants.js    # Application constants and configurations
│   ├── eventHandlers.js # Event handling for mouse/keyboard
│   ├── export.js       # Export functionality (skin & icon)
│   ├── history.js      # Undo/redo history management
│   ├── preview3d.js    # 3D preview cube management
│   ├── selection.js    # Selection tool functionality
│   ├── state.js        # Global application state
│   ├── tools.js        # Drawing tools (pen, eraser, bucket, etc.)
│   └── ui.js           # UI management (colors, buttons, panels)
└── README.md           # This file
```

## Module Overview

### Core Modules

#### `app.js` - Main Application Controller
- Initializes all managers and components
- Coordinates rendering pipeline
- Provides high-level operations (undo, redo, clear)
- Entry point for the application

#### `state.js` - Global State Management
- Centralized application state
- Tool settings, view mode, zoom levels
- Selection and drawing state
- 3D view rotation state

#### `constants.js` - Configuration Constants
- Canvas dimensions and scaling
- UV mapping definitions for Minecraft head
- Color palette
- History limits

### Feature Modules

#### `canvas.js` - Canvas Management
- Handles 2D canvas rendering
- Coordinate transformation (screen → source UV)
- Pixel manipulation on data canvas
- Zoom controls for 2D view

#### `tools.js` - Drawing Tools
- Pen, eraser, shade, bucket fill tools
- Mirror painting logic
- Layer lock checking
- Shape tools (line, circle) using Bresenham algorithms

#### `selection.js` - Selection Management
- Rectangular selection tool
- Lift, move, and commit selections
- Transform operations (rotate, flip)
- Multi-region selection across UV mapping

#### `history.js` - History Management
- Undo/redo functionality
- State snapshots using data URLs
- Circular buffer with max history limit
- UI button state updates

#### `preview3d.js` - 3D Preview
- Real-time 3D cube preview
- Face rendering from UV mapping
- Interactive rotation controls
- Grid overlay toggle
- Zoom for 3D view

#### `export.js` - Export Utilities
- Export full 64x64 skin PNG
- Export isometric head icon (512x512)
- Isometric projection with proper face mapping
- Hat layer scaling (1.15x)

#### `ui.js` - UI Management
- Color palette initialization
- Tool button states
- View switching (2D/3D)
- Panel visibility management
- Brush size and mirror mode controls

#### `eventHandlers.js` - Event Management
- Mouse events (down, move, up)
- Keyboard shortcuts
- 2D and 3D canvas interaction
- File import handling
- Tool-specific event routing

## Features

### Drawing Tools
- **Pen (P)**: Draw pixels with selected color
- **Eraser (E)**: Remove pixels
- **Bucket Fill (B)**: Fill connected regions
- **Color Picker (I)**: Sample colors from canvas
- **Shading (S)**: Lighten/darken pixels (Alt for darken)
- **Line (L)**: Draw straight lines
- **Circle (C)**: Draw circles

### Selection Tools
- **Rectangular Marquee (M)**: Select regions
- **Move (V)**: Move selected content
- Transform: Rotate 90°/−90°, Flip H/V
- Delete: Del/Backspace to clear selection

### View Modes
- **2D View**: Unfolded UV layout editor
- **3D View**: Interactive 3D cube preview with rotation

### Additional Features
- **Mirror Painting**: Paint symmetrically across left/right faces
- **Layer Management**: Toggle visibility and lock inner head/outer hat
- **History**: Undo (Ctrl+Z) / Redo (Ctrl+Y) with 50-step buffer
- **Import**: Load existing PNG skins
- **Export**:
  - Full 64x64 skin PNG
  - 512x512 isometric head icon

## Architecture Benefits

### Maintainability
- **Separation of Concerns**: Each module has a single responsibility
- **Easy to Locate**: Bug fixes and features are isolated to specific modules
- **Clear Dependencies**: Import/export system makes relationships explicit

### Extensibility
- **Plugin Architecture**: New tools can be added to `tools.js`
- **Event System**: New interactions can be added to `eventHandlers.js`
- **Modular Rendering**: Canvas and 3D preview are independent

### Performance
- **Lazy Loading**: ES6 modules load on demand
- **Efficient Rendering**: Separate data canvas for operations, display canvas for rendering
- **Optimized History**: Uses data URLs for snapshots

### Testing
- **Unit Testable**: Each module can be tested independently
- **Mock Friendly**: Dependencies are injected, not hard-coded
- **State Isolation**: Global state is centralized and predictable

## Usage

### Development
1. Open `index_new.html` in a modern browser (Chrome, Firefox, Edge)
2. Ensure browser supports ES6 modules
3. No build step required - native ES6 modules

### Migration from Legacy
- `index.html` contains the original monolithic version
- `index_new.html` uses the new modular structure
- Both versions have feature parity

## Browser Compatibility
- Requires ES6 module support
- Requires HTML5 Canvas API
- Requires CSS Grid and Flexbox
- Tested on Chrome 90+, Firefox 88+, Edge 90+

## Future Improvements
- Add automated tests for each module
- Implement layer system for multiple elements
- Add animation frame support
- WebGL-based 3D preview for better performance
- Collaborative editing with WebSockets
