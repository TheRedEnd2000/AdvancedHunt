import {state} from './state.js';
import {ShapeTools} from './tools.js';

export class EventHandlers {
    constructor(app) {
        this.app = app;
    }

    init() {
        this.setupCanvasEvents();
        this.setupToolbarEvents();
        this.setupKeyboardEvents();
        this.setupSidebarEvents();
        this.setup3DEvents();
    }

    setupCanvasEvents() {
        const canvas = this.app.canvasManager.canvas;

        canvas.addEventListener('mousedown', (e) => this.handleMouseDown(e));
        window.addEventListener('mousemove', (e) => this.handleMouseMove(e));
        window.addEventListener('mouseup', () => this.handleMouseUp());

        document.getElementById('editor-container').addEventListener('wheel',
            (e) => this.app.canvasManager.handleZoom2D(e), { passive: false });
        document.getElementById('editor-container-3d').addEventListener('wheel',
            (e) => this.app.preview3D.handleZoom3D(e), { passive: false });
    }

    setup3DEvents() {
        document.querySelectorAll('.face').forEach(face => {
            face.addEventListener('mousedown', (e) => this.handle3DMouseDown(e));
            face.addEventListener('mousemove', (e) => this.handle3DMouseMove(e));
            face.addEventListener('mouseup', () => this.handleMouseUp());
        });

        document.getElementById('view-reset').onclick = () => {
            this.app.preview3D.resetViewAngle();
        };

        document.getElementById('toggle-grid-3d').onclick = () => {
            this.app.preview3D.toggleGrid();
        };
    }

    setupToolbarEvents() {
        // View buttons
        document.getElementById('view-2d').onclick = () => {
            this.app.ui.setView('2d');
        };

        document.getElementById('view-3d').onclick = () => {
            this.app.ui.setView('3d');
        };

        // Tool buttons
        document.getElementById('tool-pen').onclick = () => this.app.ui.setTool('pen');
        document.getElementById('tool-eraser').onclick = () => this.app.ui.setTool('eraser');
        document.getElementById('tool-bucket').onclick = () => this.app.ui.setTool('bucket');
        document.getElementById('tool-picker').onclick = () => this.app.ui.setTool('picker');
        document.getElementById('tool-shade').onclick = () => this.app.ui.setTool('shade');
        document.getElementById('tool-line').onclick = () => this.app.ui.setTool('line');
        document.getElementById('tool-circle').onclick = () => this.app.ui.setTool('circle');
        document.getElementById('tool-select').onclick = () => this.app.ui.setTool('select');
        document.getElementById('tool-move').onclick = () => this.app.ui.setTool('move');

        // Action buttons
        document.getElementById('action-undo').onclick = () => this.app.undo();
        document.getElementById('action-redo').onclick = () => this.app.redo();
        document.getElementById('action-clear').onclick = () => this.app.clearAll();

        // File import
        document.getElementById('file-import').addEventListener('change', (e) => this.handleImport(e));
    }

    setupSidebarEvents() {
        // Selection actions
        document.getElementById('sel-deselect').onclick = () => {
            this.app.selectionManager.deselect();
            this.app.render();
        };

        document.getElementById('sel-rotate-cw').onclick = () => {
            this.app.selectionManager.rotateSelection(90);
            this.app.render();
        };

        document.getElementById('sel-rotate-ccw').onclick = () => {
            this.app.selectionManager.rotateSelection(-90);
            this.app.render();
        };

        document.getElementById('sel-flip-h').onclick = () => {
            this.app.selectionManager.flipSelection('h');
            this.app.render();
        };

        document.getElementById('sel-flip-v').onclick = () => {
            this.app.selectionManager.flipSelection('v');
            this.app.render();
        };

        // Tool options
        document.getElementById('brush-size').oninput = (e) => {
            this.app.ui.updateBrushSize(e.target.value);
        };

        document.getElementById('opt-mirror').onchange = (e) => {
            this.app.ui.updateMirrorMode(e.target.checked);
        };

        // Color picker
        this.app.ui.colorPicker.addEventListener('input', (e) => {
            this.app.ui.setColor(e.target.value);
        });

        // Layer toggles
        document.getElementById('toggle-hat').onchange = () => {
            this.app.ui.updateLayerVisibility();
        };

        document.getElementById('toggle-head').onchange = () => {
            this.app.ui.updateLayerVisibility();
        };

        // Export buttons
        document.getElementById('btn-export').onclick = () => {
            this.app.exportManager.exportSkin();
        };

        document.getElementById('btn-export-icon').onclick = () => {
            this.app.exportManager.exportHeadIcon();
        };
    }

    setupKeyboardEvents() {
        window.addEventListener('keydown', (e) => {
            if (e.target.tagName === 'INPUT') return;

            const key = e.key.toLowerCase();

            // Tool shortcuts
            if (key === 'p') this.app.ui.setTool('pen');
            if (key === 'e') this.app.ui.setTool('eraser');
            if (key === 'b') this.app.ui.setTool('bucket');
            if (key === 'i') this.app.ui.setTool('picker');
            if (key === 's') this.app.ui.setTool('shade');
            if (key === 'l') this.app.ui.setTool('line');
            if (key === 'c') this.app.ui.setTool('circle');
            if (key === 'm') this.app.ui.setTool('select');
            if (key === 'v') this.app.ui.setTool('move');

            // Selection shortcuts
            if (key === 'escape') {
                this.app.selectionManager.deselect();
                this.app.render();
            }

            if (key === 'delete' || key === 'backspace') {
                this.app.selectionManager.deleteSelection();
                this.app.render();
                this.app.historyManager.saveState();
            }

            // History shortcuts
            if ((e.ctrlKey || e.metaKey) && key === 'z') {
                e.preventDefault();
                this.app.undo();
            }

            if ((e.ctrlKey || e.metaKey) && key === 'y') {
                e.preventDefault();
                this.app.redo();
            }
        });
    }

    handleMouseDown(e) {
        if (e.target !== this.app.canvasManager.canvas) return;

        state.isDrawing = true;

        if (state.currentTool === 'select') {
            const coords = this.app.canvasManager.getVisualCoords(e);
            this.app.selectionManager.startSelection(coords.x, coords.y);
            this.app.render();
            return;
        }

        if (state.currentTool === 'move') {
            this.app.selectionManager.startMove(e.clientX, e.clientY);
            return;
        }

        this.handleToolAction(e);
    }

    handleMouseMove(e) {
        if (!state.isDrawing) return;

        const canvas = this.app.canvasManager.canvas;
        const rect = canvas.getBoundingClientRect();

        if (state.currentTool === 'select') {
            const coords = this.app.canvasManager.getVisualCoords(e);
            this.app.selectionManager.updateSelection(coords.x, coords.y);
            this.app.render();
            return;
        }

        if (state.currentTool === 'move') {
            this.app.selectionManager.updateMove(e.clientX, e.clientY, canvas);
            this.app.render();
            return;
        }

        if (state.currentTool === 'line' || state.currentTool === 'circle') {
            const coords = this.app.canvasManager.getVisualCoords(e);
            state.shapeCurrentX = coords.x;
            state.shapeCurrentY = coords.y;
            this.app.render();
            return;
        }

        if (e.clientX < rect.left || e.clientX > rect.right ||
            e.clientY < rect.top || e.clientY > rect.bottom) return;

        if (state.currentTool === 'pen' || state.currentTool === 'eraser' || state.currentTool === 'shade') {
            this.handleToolAction(e);
        }
    }

    handleMouseUp() {
        if (state.isDrawing) {
            if (state.currentTool === 'line' || state.currentTool === 'circle') {
                this.commitShape();
            }

            state.isDrawing = false;
            this.app.historyManager.saveState();
        }

        if (state.isSelecting) {
            state.isSelecting = false;
        }

        if (state.isMovingSelection) {
            state.isMovingSelection = false;
        }
    }

    handle3DMouseDown(e) {
        state.isDrawing = true;
        this.handle3DToolAction(e);
    }

    handle3DMouseMove(e) {
        if (!state.isDrawing) return;
        this.handle3DToolAction(e);
    }

    handle3DToolAction(e) {
        e.preventDefault();

        const face = e.target;
        const sx = parseInt(face.dataset.sx);
        const sy = parseInt(face.dataset.sy);

        const localX = Math.floor(e.offsetX / 10);
        const localY = Math.floor(e.offsetY / 10);

        if (localX < 0 || localX >= 8 || localY < 0 || localY >= 8) return;

        const targetX = sx + localX;
        const targetY = sy + localY;

        if (state.currentTool === 'line' || state.currentTool === 'circle') {
            if (e.type === 'mousedown') {
                state.shapeStartX = targetX;
                state.shapeStartY = targetY;
                state.shapeCurrentX = targetX;
                state.shapeCurrentY = targetY;
            } else {
                state.shapeCurrentX = targetX;
                state.shapeCurrentY = targetY;
            }
            this.app.preview3D.updateFaces(state.selection, this.getShapePreview());
            return;
        }

        if (state.currentTool === 'picker') {
            const color = this.app.canvasManager.getPixelColor(targetX, targetY);
            if (color) this.app.ui.setColor(color);
            state.isDrawing = false;
            return;
        }

        this.app.tools.applyToolWithMirror(targetX, targetY, false);
        this.app.render();
    }

    handleToolAction(e) {
        const coords = this.app.canvasManager.getSourceCoords(e);
        if (!coords) return;

        if (state.currentTool === 'line' || state.currentTool === 'circle') {
            const visualCoords = this.app.canvasManager.getVisualCoords(e);

            if (e.type === 'mousedown') {
                state.shapeStartX = visualCoords.x;
                state.shapeStartY = visualCoords.y;
                state.shapeCurrentX = visualCoords.x;
                state.shapeCurrentY = visualCoords.y;
            } else {
                state.shapeCurrentX = visualCoords.x;
                state.shapeCurrentY = visualCoords.y;
            }

            this.app.render();
            return;
        }

        if (state.currentTool === 'picker') {
            const color = this.app.canvasManager.getPixelColor(coords.x, coords.y);
            if (color) this.app.ui.setColor(color);
            state.isDrawing = false;
            return;
        }

        if (state.selection && state.selection.floating) {
            this.app.selectionManager.commitSelection();
        }

        this.app.tools.applyToolWithMirror(coords.x, coords.y, false);
        this.app.render();
    }

    commitShape() {
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

        for (const p of pixels) {
            let sourceX, sourceY;

            if (state.currentView === '2d') {
                const source = this.app.canvasManager.getSourceFromVisual(p.x, p.y);
                if (source) {
                    sourceX = source.x;
                    sourceY = source.y;
                } else {
                    continue;
                }
            } else {
                sourceX = p.x;
                sourceY = p.y;
            }

            if (!this.app.tools.isLayerLocked(sourceX, sourceY)) {
                this.app.canvasManager.setPixel(sourceX, sourceY, state.currentColor);
            }
        }

        this.app.render();
    }

    getShapePreview() {
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

    handleImport(e) {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (event) => {
            const img = new Image();
            img.onload = () => {
                this.app.canvasManager.loadImage(img);
                this.app.render();
                this.app.historyManager.saveState();
            };
            img.src = event.target.result;
        };
        reader.readAsDataURL(file);

        e.target.value = '';
    }
}
