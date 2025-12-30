import {CanvasManager} from './canvas.js';
import {ShapeTools, Tools} from './tools.js';
import {HistoryManager} from './history.js';
import {SelectionManager} from './selection.js';
import {Preview3DManager} from './preview3d.js';
import {ExportManager} from './export.js';
import {UIManager} from './ui.js';
import {EventHandlers} from './eventHandlers.js';
import {state} from './state.js';

export class MinecraftHeadEditor {
    constructor() {
        // Initialize managers
        this.canvasManager = new CanvasManager(document.getElementById('skinCanvas'));
        this.tools = new Tools(this.canvasManager);
        this.historyManager = new HistoryManager(this.canvasManager, () => this.render());
        this.selectionManager = new SelectionManager(this.canvasManager);
        this.preview3D = new Preview3DManager(this.canvasManager);
        this.exportManager = new ExportManager(this.canvasManager);
        this.ui = new UIManager();
        this.eventHandlers = new EventHandlers(this);
    }

    init() {
        // Initialize all components
        this.ui.init();
        this.preview3D.init();
        this.eventHandlers.init();

        // Set initial layer visibility
        this.ui.updateLayerVisibility();

        // Save initial state
        this.historyManager.saveState();

        // Initial render
        this.render();
    }

    render() {
        const shapePreview = this.getShapePreview();
        this.canvasManager.render(state.selection, shapePreview);
        this.preview3D.updateFaces(state.selection, shapePreview);
        this.ui.updateSelectionPanel();
    }

    getShapePreview() {
        if (!state.isDrawing || (state.currentTool !== 'line' && state.currentTool !== 'circle')) {
            return null;
        }

        let pixels = [];

        if (state.currentTool === 'line') {
            pixels = ShapeTools.getLinePixels(
                state.shapeStartX, state.shapeStartY,
                state.shapeCurrentX, state.shapeCurrentY
            );
        } else if (state.currentTool === 'circle') {
            pixels = ShapeTools.getCirclePixels(
                state.shapeStartX, state.shapeStartY,
                state.shapeCurrentX, state.shapeCurrentY
            );
        }

        return {
            pixels,
            color: state.currentColor,
            viewMode: state.currentView
        };
    }

    undo() {
        this.historyManager.undo();
    }

    redo() {
        this.historyManager.redo();
    }

    clearAll() {
        if (confirm('Are you sure you want to clear the entire skin?')) {
            this.canvasManager.clearAll();
            this.render();
            this.historyManager.saveState();
        }
    }
}

// Initialize the application when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    const app = new MinecraftHeadEditor();
    app.init();
});
