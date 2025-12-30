// Global application state
export const state = {
    currentColor: '#ff0000',
    currentTool: 'pen',
    isDrawing: false,
    brushSize: 1,
    isMirror: false,
    currentView: '2d',
    zoom2D: 1,
    zoom3D: 3,
    showGrid3D: false,

    // Selection state
    selection: null,
    isSelecting: false,
    isMovingSelection: false,
    moveStartX: 0,
    moveStartY: 0,

    // Shape tool state
    shapeStartX: 0,
    shapeStartY: 0,
    shapeCurrentX: 0,
    shapeCurrentY: 0,
    shapeStartFace: null, // Track which 3D face the shape started on
    shapeUpdatePending: false, // Throttle shape preview updates

    // 3D view state
    rotX: -30,
    rotY: 45,
    isDragging3D: false,
    lastMouseX: 0,
    lastMouseY: 0
};
