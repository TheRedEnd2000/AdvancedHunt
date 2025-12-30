/** Minecraft Head Editor - Standalone Bundle - Generated: 30/12/2025 11:52:48.98 */

// Canvas and Skin Constants
const CANVAS_SCALE = 10;
const SKIN_WIDTH = 64;
const SKIN_HEIGHT = 64;

// Visual Layout Constants
const INNER_OFFSET_X = 0;
const OUTER_OFFSET_X = 40;

// Mapping Definitions: [VisualX, VisualY, SourceX, SourceY, Width, Height]
const MAPPINGS = [
    // Inner Head
    { vx: 8,  vy: 0,  sx: 8,  sy: 0,  w: 8, h: 8, name: 'Top' },
    { vx: 8,  vy: 16, sx: 16, sy: 0,  w: 8, h: 8, name: 'Bottom' },
    { vx: 0,  vy: 8,  sx: 0,  sy: 8,  w: 8, h: 8, name: 'Right' },
    { vx: 8,  vy: 8,  sx: 8,  sy: 8,  w: 8, h: 8, name: 'Front' },
    { vx: 16, vy: 8,  sx: 16, sy: 8,  w: 8, h: 8, name: 'Left' },
    { vx: 24, vy: 8,  sx: 24, sy: 8,  w: 8, h: 8, name: 'Back' },

    // Outer Hat (Offset X by 40)
    { vx: 48, vy: 0,  sx: 40, sy: 0,  w: 8, h: 8, name: 'Hat Top' },
    { vx: 48, vy: 16, sx: 48, sy: 0,  w: 8, h: 8, name: 'Hat Bottom' },
    { vx: 40, vy: 8,  sx: 32, sy: 8,  w: 8, h: 8, name: 'Hat Right' },
    { vx: 48, vy: 8,  sx: 40, sy: 8,  w: 8, h: 8, name: 'Hat Front' },
    { vx: 56, vy: 8,  sx: 48, sy: 8,  w: 8, h: 8, name: 'Hat Left' },
    { vx: 64, vy: 8,  sx: 56, sy: 8,  w: 8, h: 8, name: 'Hat Back' }
];

// History Constants
const MAX_HISTORY = 50;

// Default Colors Palette
const DEFAULT_COLORS = [
    '#1a1a1a', '#ffffff', '#b4202a', '#1e7432', '#193d82',
    '#f0c426', '#f27f18', '#9c4e26', '#4a2c18', '#d98e68'
];

// Global application state
const state = {
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




class CanvasManager {
    constructor(canvasElement) {
        this.canvas = canvasElement;
        this.ctx = this.canvas.getContext('2d');

        // Create offscreen canvas for actual data
        this.dataCanvas = document.createElement('canvas');
        this.dataCanvas.width = SKIN_WIDTH;
        this.dataCanvas.height = SKIN_HEIGHT;
        this.dataCtx = this.dataCanvas.getContext('2d');

        this.dataCtx.clearRect(0, 0, SKIN_WIDTH, SKIN_HEIGHT);
    }

    getDataCanvas() {
        return this.dataCanvas;
    }

    getDataContext() {
        return this.dataCtx;
    }

    setPixel(x, y, color) {
        if (color === 'transparent') {
            this.dataCtx.clearRect(x, y, 1, 1);
        } else {
            this.dataCtx.fillStyle = color;
            this.dataCtx.fillRect(x, y, 1, 1);
        }
    }

    getPixelColor(x, y) {
        const p = this.dataCtx.getImageData(x, y, 1, 1).data;
        if (p[3] === 0) return null;
        return "#" + ((1 << 24) + (p[0] << 16) + (p[1] << 8) + p[2]).toString(16).slice(1);
    }

    clearAll() {
        this.dataCtx.clearRect(0, 0, SKIN_WIDTH, SKIN_HEIGHT);
    }

    loadImage(img) {
        this.dataCtx.clearRect(0, 0, SKIN_WIDTH, SKIN_HEIGHT);
        this.dataCtx.drawImage(img, 0, 0);
    }

    getSourceCoords(e) {
        const rect = this.canvas.getBoundingClientRect();
        const scaleX = this.canvas.width / rect.width;
        const scaleY = this.canvas.height / rect.height;

        const vx = Math.floor((e.clientX - rect.left) * scaleX / CANVAS_SCALE);
        const vy = Math.floor((e.clientY - rect.top) * scaleY / CANVAS_SCALE);

        for (const map of MAPPINGS) {
            if (vx >= map.vx && vx < map.vx + map.w &&
                vy >= map.vy && vy < map.vy + map.h) {
                const offsetX = vx - map.vx;
                const offsetY = vy - map.vy;
                return { x: map.sx + offsetX, y: map.sy + offsetY };
            }
        }
        return null;
    }

    getVisualCoords(e) {
        const rect = this.canvas.getBoundingClientRect();
        const scaleX = this.canvas.width / rect.width;
        const scaleY = this.canvas.height / rect.height;

        const vx = Math.floor((e.clientX - rect.left) * scaleX / CANVAS_SCALE);
        const vy = Math.floor((e.clientY - rect.top) * scaleY / CANVAS_SCALE);

        return { x: vx, y: vy };
    }

    getSourceFromVisual(vx, vy) {
        for (const map of MAPPINGS) {
            if (vx >= map.vx && vx < map.vx + map.w &&
                vy >= map.vy && vy < map.vy + map.h) {
                const offsetX = vx - map.vx;
                const offsetY = vy - map.vy;
                return { x: map.sx + offsetX, y: map.sy + offsetY };
            }
        }
        return null;
    }

    getVisualFromSource(sx, sy) {
        for (const map of MAPPINGS) {
            if (sx >= map.sx && sx < map.sx + map.w &&
                sy >= map.sy && sy < map.sy + map.h) {
                const offsetX = sx - map.sx;
                const offsetY = sy - map.sy;
                return { x: map.vx + offsetX, y: map.vy + offsetY };
            }
        }
        return null;
    }

    render(selection = null, shapePreview = null) {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        this.ctx.imageSmoothingEnabled = false;

        // Draw each mapped region
        for (const map of MAPPINGS) {
            this.ctx.drawImage(
                this.dataCanvas,
                map.sx, map.sy, map.w, map.h,
                map.vx * CANVAS_SCALE, map.vy * CANVAS_SCALE,
                map.w * CANVAS_SCALE, map.h * CANVAS_SCALE
            );

            // Draw border
            this.ctx.strokeStyle = 'rgba(255,255,255,0.1)';
            this.ctx.lineWidth = 1;
            this.ctx.strokeRect(
                map.vx * CANVAS_SCALE, map.vy * CANVAS_SCALE,
                map.w * CANVAS_SCALE, map.h * CANVAS_SCALE
            );
        }

        // Draw floating selection
        if (selection && selection.floating) {
            this.ctx.drawImage(
                selection.content,
                selection.x * CANVAS_SCALE,
                selection.y * CANVAS_SCALE,
                selection.w * CANVAS_SCALE,
                selection.h * CANVAS_SCALE
            );
        }

        // Draw selection outline
        if (selection) {
            this.ctx.save();
            this.ctx.strokeStyle = '#fff';
            this.ctx.lineWidth = 1;
            this.ctx.setLineDash([4, 4]);
            this.ctx.strokeRect(
                selection.x * CANVAS_SCALE,
                selection.y * CANVAS_SCALE,
                selection.w * CANVAS_SCALE,
                selection.h * CANVAS_SCALE
            );
            this.ctx.strokeStyle = '#000';
            this.ctx.lineDashOffset = 4;
            this.ctx.strokeRect(
                selection.x * CANVAS_SCALE,
                selection.y * CANVAS_SCALE,
                selection.w * CANVAS_SCALE,
                selection.h * CANVAS_SCALE
            );
            this.ctx.restore();
        }

        // Draw shape preview
        if (shapePreview) {
            this.drawShapePreview(shapePreview);
        }

        // Draw grid
        this.ctx.strokeStyle = 'rgba(255,255,255,0.05)';
        this.ctx.lineWidth = 1;
        this.ctx.beginPath();
        for (let x = 0; x <= this.canvas.width; x += CANVAS_SCALE) {
            this.ctx.moveTo(x, 0);
            this.ctx.lineTo(x, this.canvas.height);
        }
        for (let y = 0; y <= this.canvas.height; y += CANVAS_SCALE) {
            this.ctx.moveTo(0, y);
            this.ctx.lineTo(this.canvas.width, y);
        }
        this.ctx.stroke();

        // Labels
        this.ctx.fillStyle = 'rgba(255,255,255,0.5)';
        this.ctx.font = '12px sans-serif';
        this.ctx.fillText("INNER HEAD", 8 * CANVAS_SCALE, 23 * CANVAS_SCALE);
        this.ctx.fillText("OUTER HAT", 48 * CANVAS_SCALE, 23 * CANVAS_SCALE);
    }

    drawShapePreview(shapePreview) {
        const { pixels, color, viewMode } = shapePreview;

        this.ctx.fillStyle = color;
        for (const p of pixels) {
            if (viewMode === '2d') {
                if (this.getSourceFromVisual(p.x, p.y)) {
                    this.ctx.fillRect(
                        p.x * CANVAS_SCALE,
                        p.y * CANVAS_SCALE,
                        CANVAS_SCALE,
                        CANVAS_SCALE
                    );
                }
            } else {
                for (const map of MAPPINGS) {
                    if (p.x >= map.sx && p.x < map.sx + map.w &&
                        p.y >= map.sy && p.y < map.sy + map.h) {
                        const offsetX = p.x - map.sx;
                        const offsetY = p.y - map.sy;
                        const vx = map.vx + offsetX;
                        const vy = map.vy + offsetY;
                        this.ctx.fillRect(
                            vx * CANVAS_SCALE,
                            vy * CANVAS_SCALE,
                            CANVAS_SCALE,
                            CANVAS_SCALE
                        );
                    }
                }
            }
        }
    }

    handleZoom2D(e) {
        if (e.ctrlKey) {
            e.preventDefault();
            const delta = e.deltaY > 0 ? -0.1 : 0.1;
            state.zoom2D = Math.max(0.5, Math.min(5, state.zoom2D + delta));

            this.canvas.style.width = (720 * state.zoom2D) + 'px';
            this.canvas.style.height = (240 * state.zoom2D) + 'px';
        }
    }
}




class Tools {
    constructor(canvasManager) {
        this.canvasManager = canvasManager;
    }

    getFaceAt(x, y) {
        for (const map of MAPPINGS) {
            if (x >= map.sx && x < map.sx + map.w &&
                y >= map.sy && y < map.sy + map.h) {
                return map;
            }
        }
        return null;
    }

    isLayerLocked(x, y) {
        const face = this.getFaceAt(x, y);
        if (!face) return false;

        const isHat = face.name.startsWith('Hat');
        if (isHat && document.getElementById('lock-hat').checked) return true;
        if (!isHat && document.getElementById('lock-head').checked) return true;

        return false;
    }

    getMirrorCoords(x, y) {
        const face = this.getFaceAt(x, y);
        if (!face) return null;

        const localX = x - face.sx;
        const localY = y - face.sy;
        const mirroredLocalX = face.w - 1 - localX;

        let targetFaceName = face.name;

        // Swap Left/Right faces
        if (face.name === 'Right') targetFaceName = 'Left';
        else if (face.name === 'Left') targetFaceName = 'Right';
        else if (face.name === 'Hat Right') targetFaceName = 'Hat Left';
        else if (face.name === 'Hat Left') targetFaceName = 'Hat Right';

        const targetFace = MAPPINGS.find(m => m.name === targetFaceName);
        if (!targetFace) return null;

        return { x: targetFace.sx + mirroredLocalX, y: targetFace.sy + localY };
    }

    applyPen(x, y) {
        if (this.isLayerLocked(x, y)) return;
        this.canvasManager.setPixel(x, y, state.currentColor);
    }

    applyEraser(x, y) {
        if (this.isLayerLocked(x, y)) return;
        this.canvasManager.setPixel(x, y, 'transparent');
    }

    applyShade(x, y, darken = false) {
        if (this.isLayerLocked(x, y)) return;

        const dataCtx = this.canvasManager.getDataContext();
        const p = dataCtx.getImageData(x, y, 1, 1).data;
        if (p[3] === 0) return;

        const amount = 20;
        const factor = darken ? -1 : 1;

        const r = Math.min(255, Math.max(0, p[0] + amount * factor));
        const g = Math.min(255, Math.max(0, p[1] + amount * factor));
        const b = Math.min(255, Math.max(0, p[2] + amount * factor));

        const hex = "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
        this.canvasManager.setPixel(x, y, hex);
    }

    applyBucketFill(startX, startY, fillColor) {
        const bounds = this.getFaceBounds(startX, startY);
        if (!bounds) return;

        const dataCtx = this.canvasManager.getDataContext();
        const imgData = dataCtx.getImageData(0, 0, SKIN_WIDTH, SKIN_HEIGHT);
        const pixels = imgData.data;

        const getPixelColor = (x, y) => {
            if (x < 0 || y < 0 || x >= SKIN_WIDTH || y >= SKIN_HEIGHT) return null;
            const idx = (y * SKIN_WIDTH + x) * 4;
            return { r: pixels[idx], g: pixels[idx + 1], b: pixels[idx + 2], a: pixels[idx + 3] };
        };

        const targetColor = getPixelColor(startX, startY);
        const r = parseInt(fillColor.slice(1, 3), 16);
        const g = parseInt(fillColor.slice(3, 5), 16);
        const b = parseInt(fillColor.slice(5, 7), 16);

        if (targetColor.r === r && targetColor.g === g && targetColor.b === b && targetColor.a === 255) return;

        const stack = [[startX, startY]];

        while (stack.length) {
            const [x, y] = stack.pop();
            const current = getPixelColor(x, y);

            if (x < bounds.minX || x >= bounds.maxX || y < bounds.minY || y >= bounds.maxY) continue;

            if (current.r === targetColor.r && current.g === targetColor.g &&
                current.b === targetColor.b && current.a === targetColor.a) {
                const idx = (y * SKIN_WIDTH + x) * 4;
                pixels[idx] = r;
                pixels[idx + 1] = g;
                pixels[idx + 2] = b;
                pixels[idx + 3] = 255;

                stack.push([x + 1, y]);
                stack.push([x - 1, y]);
                stack.push([x, y + 1]);
                stack.push([x, y - 1]);
            }
        }

        dataCtx.putImageData(imgData, 0, 0);
    }

    getFaceBounds(x, y) {
        for (const map of MAPPINGS) {
            if (x >= map.sx && x < map.sx + map.w &&
                y >= map.sy && y < map.sy + map.h) {
                return {
                    minX: map.sx,
                    maxX: map.sx + map.w,
                    minY: map.sy,
                    maxY: map.sy + map.h
                };
            }
        }
        return null;
    }

    applyTool(x, y, altKey = false) {
        if (this.isLayerLocked(x, y)) return;

        const half = Math.floor((state.brushSize - 1) / 2);
        const startX = x - half;
        const startY = y - half;

        for (let bx = 0; bx < state.brushSize; bx++) {
            for (let by = 0; by < state.brushSize; by++) {
                const curX = startX + bx;
                const curY = startY + by;

                if (curX < 0 || curX >= SKIN_WIDTH || curY < 0 || curY >= SKIN_HEIGHT) continue;

                if (state.currentTool === 'bucket') {
                    if (bx === 0 && by === 0) this.applyBucketFill(curX, curY, state.currentColor);
                    continue;
                }

                switch (state.currentTool) {
                    case 'pen':
                        this.applyPen(curX, curY);
                        break;
                    case 'eraser':
                        this.applyEraser(curX, curY);
                        break;
                    case 'shade':
                        this.applyShade(curX, curY, altKey);
                        break;
                }
            }
        }
    }

    applyToolWithMirror(x, y, altKey = false) {
        this.applyTool(x, y, altKey);

        if (state.isMirror) {
            const mirror = this.getMirrorCoords(x, y);
            if (mirror) {
                this.applyTool(mirror.x, mirror.y, altKey);
            }
        }
    }
}

// Shape Tools
class ShapeTools {
    static getLinePixels(x0, y0, x1, y1) {
        if (![x0, y0, x1, y1].every(Number.isFinite)) return [];

        x0 = Math.round(x0);
        y0 = Math.round(y0);
        x1 = Math.round(x1);
        y1 = Math.round(y1);

        const pixels = [];
        const dx = Math.abs(x1 - x0);
        const dy = Math.abs(y1 - y0);
        const sx = (x0 < x1) ? 1 : -1;
        const sy = (y0 < y1) ? 1 : -1;
        let err = dx - dy;

        const maxSteps = 2048;
        let steps = 0;

        while (true) {
            pixels.push({ x: x0, y: y0 });
            if (x0 === x1 && y0 === y1) break;
            const e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }

            steps++;
            if (steps >= maxSteps) break;
        }
        return pixels;
    }

    static getCirclePixels(x0, y0, x1, y1) {
        if (![x0, y0, x1, y1].every(Number.isFinite)) return [];

        x0 = Math.round(x0);
        y0 = Math.round(y0);
        x1 = Math.round(x1);
        y1 = Math.round(y1);

        const pixels = [];
        const radius = Math.floor(Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2)));
        if (!Number.isFinite(radius) || radius < 0) return [];
        let x = radius;
        let y = 0;

        const maxSteps = 2048;
        let steps = 0;
        let err = 0;

        while (x >= y) {
            pixels.push({ x: x0 + x, y: y0 + y });
            pixels.push({ x: x0 + y, y: y0 + x });
            pixels.push({ x: x0 - y, y: y0 + x });
            pixels.push({ x: x0 - x, y: y0 + y });
            pixels.push({ x: x0 - x, y: y0 - y });
            pixels.push({ x: x0 - y, y: y0 - x });
            pixels.push({ x: x0 + y, y: y0 - x });
            pixels.push({ x: x0 + x, y: y0 - y });

            if (err <= 0) {
                y += 1;
                err += 2 * y + 1;
            }
            if (err > 0) {
                x -= 1;
                err -= 2 * x + 1;
            }

            steps++;
            if (steps >= maxSteps) break;
        }
        return pixels;
    }
}



class HistoryManager {
    constructor(canvasManager, onStateRestored = null) {
        this.canvasManager = canvasManager;
        this.history = [];
        this.historyStep = -1;
        this.onStateRestored = onStateRestored;
        this._restoreSeq = 0;
    }

    setOnStateRestored(callback) {
        this.onStateRestored = callback;
    }

    saveState() {
        const dataCanvas = this.canvasManager.getDataCanvas();

        // Remove any redo steps
        if (this.historyStep < this.history.length - 1) {
            this.history.length = this.historyStep + 1;
        }

        this.history.push(dataCanvas.toDataURL());

        if (this.history.length > MAX_HISTORY) {
            this.history.shift();
        } else {
            this.historyStep++;
        }

        this.updateHistoryButtons();
    }

    undo() {
        if (this.historyStep > 0) {
            this.historyStep--;
            this.loadState(this.history[this.historyStep]);
        }
        this.updateHistoryButtons();
    }

    redo() {
        if (this.historyStep < this.history.length - 1) {
            this.historyStep++;
            this.loadState(this.history[this.historyStep]);
        }
        this.updateHistoryButtons();
    }

    loadState(dataUrl) {
        const restoreId = ++this._restoreSeq;

        const img = new Image();
        img.onload = () => {
            if (restoreId !== this._restoreSeq) return;

            this.canvasManager.loadImage(img);

            if (typeof this.onStateRestored === 'function') {
                this.onStateRestored();
            }
        };
        img.src = dataUrl;
    }

    updateHistoryButtons() {
        const undoBtn = document.getElementById('action-undo');
        const redoBtn = document.getElementById('action-redo');

        if (undoBtn) undoBtn.style.opacity = this.historyStep > 0 ? 1 : 0.3;
        if (redoBtn) redoBtn.style.opacity = this.historyStep < this.history.length - 1 ? 1 : 0.3;
    }

    canUndo() {
        return this.historyStep > 0;
    }

    canRedo() {
        return this.historyStep < this.history.length - 1;
    }
}




class SelectionManager {
    constructor(canvasManager) {
        this.canvasManager = canvasManager;
    }

    startSelection(x, y) {
        if (state.selection && state.selection.floating) {
            this.commitSelection();
        }

        state.selection = {
            startX: x,
            startY: y,
            x: x,
            y: y,
            w: 1,
            h: 1,
            floating: false,
            content: null
        };

        state.isSelecting = true;
    }

    updateSelection(x, y) {
        if (!state.selection) return;

        const minX = Math.min(state.selection.startX, x);
        const minY = Math.min(state.selection.startY, y);
        const maxX = Math.max(state.selection.startX, x);
        const maxY = Math.max(state.selection.startY, y);

        state.selection.x = minX;
        state.selection.y = minY;
        state.selection.w = maxX - minX + 1;
        state.selection.h = maxY - minY + 1;
    }

    liftSelection() {
        if (!state.selection || state.selection.floating) return;

        const tempCanvas = document.createElement('canvas');
        tempCanvas.width = state.selection.w;
        tempCanvas.height = state.selection.h;
        const tempCtx = tempCanvas.getContext('2d');

        const dataCanvas = this.canvasManager.getDataCanvas();
        const dataCtx = this.canvasManager.getDataContext();

        for (const map of MAPPINGS) {
            const ix = Math.max(state.selection.x, map.vx);
            const iy = Math.max(state.selection.y, map.vy);
            const iRight = Math.min(state.selection.x + state.selection.w, map.vx + map.w);
            const iBottom = Math.min(state.selection.y + state.selection.h, map.vy + map.h);

            if (ix < iRight && iy < iBottom) {
                const w = iRight - ix;
                const h = iBottom - iy;

                const mapOffsetX = ix - map.vx;
                const mapOffsetY = iy - map.vy;
                const selOffsetX = ix - state.selection.x;
                const selOffsetY = iy - state.selection.y;

                tempCtx.drawImage(
                    dataCanvas,
                    map.sx + mapOffsetX, map.sy + mapOffsetY, w, h,
                    selOffsetX, selOffsetY, w, h
                );

                dataCtx.clearRect(map.sx + mapOffsetX, map.sy + mapOffsetY, w, h);
            }
        }

        state.selection.content = tempCanvas;
        state.selection.floating = true;
    }

    commitSelection() {
        if (!state.selection || !state.selection.floating) {
            state.selection = null;
            return;
        }

        const dataCtx = this.canvasManager.getDataContext();

        for (const map of MAPPINGS) {
            const ix = Math.max(state.selection.x, map.vx);
            const iy = Math.max(state.selection.y, map.vy);
            const iRight = Math.min(state.selection.x + state.selection.w, map.vx + map.w);
            const iBottom = Math.min(state.selection.y + state.selection.h, map.vy + map.h);

            if (ix < iRight && iy < iBottom) {
                const w = iRight - ix;
                const h = iBottom - iy;

                const mapOffsetX = ix - map.vx;
                const mapOffsetY = iy - map.vy;
                const selOffsetX = ix - state.selection.x;
                const selOffsetY = iy - state.selection.y;

                dataCtx.drawImage(
                    state.selection.content,
                    selOffsetX, selOffsetY, w, h,
                    map.sx + mapOffsetX, map.sy + mapOffsetY, w, h
                );
            }
        }

        state.selection = null;
    }

    deleteSelection() {
        if (!state.selection) return;

        if (!state.selection.floating) {
            const dataCtx = this.canvasManager.getDataContext();

            for (const map of MAPPINGS) {
                const ix = Math.max(state.selection.x, map.vx);
                const iy = Math.max(state.selection.y, map.vy);
                const iRight = Math.min(state.selection.x + state.selection.w, map.vx + map.w);
                const iBottom = Math.min(state.selection.y + state.selection.h, map.vy + map.h);

                if (ix < iRight && iy < iBottom) {
                    const w = iRight - ix;
                    const h = iBottom - iy;
                    const mapOffsetX = ix - map.vx;
                    const mapOffsetY = iy - map.vy;

                    dataCtx.clearRect(map.sx + mapOffsetX, map.sy + mapOffsetY, w, h);
                }
            }
        }

        state.selection = null;
    }

    rotateSelection(deg) {
        if (!state.selection) return;
        if (!state.selection.floating) this.liftSelection();

        const src = state.selection.content;
        const dst = document.createElement('canvas');

        if (Math.abs(deg) === 90) {
            dst.width = src.height;
            dst.height = src.width;
        } else {
            dst.width = src.width;
            dst.height = src.height;
        }

        const dCtx = dst.getContext('2d');
        dCtx.translate(dst.width / 2, dst.height / 2);
        dCtx.rotate(deg * Math.PI / 180);
        dCtx.drawImage(src, -src.width / 2, -src.height / 2);

        state.selection.content = dst;
        state.selection.w = dst.width;
        state.selection.h = dst.height;
    }

    flipSelection(axis) {
        if (!state.selection) return;
        if (!state.selection.floating) this.liftSelection();

        const src = state.selection.content;
        const dst = document.createElement('canvas');
        dst.width = src.width;
        dst.height = src.height;

        const dCtx = dst.getContext('2d');
        dCtx.translate(dst.width / 2, dst.height / 2);
        if (axis === 'h') dCtx.scale(-1, 1);
        else dCtx.scale(1, -1);
        dCtx.drawImage(src, -src.width / 2, -src.height / 2);

        state.selection.content = dst;
    }

    startMove(clientX, clientY) {
        if (!state.selection) return;
        if (!state.selection.floating) this.liftSelection();

        state.isMovingSelection = true;
        state.moveStartX = clientX;
        state.moveStartY = clientY;
        state.selection.originX = state.selection.x;
        state.selection.originY = state.selection.y;
    }

    updateMove(clientX, clientY, canvas) {
        if (!state.isMovingSelection || !state.selection) return;

        const rect = canvas.getBoundingClientRect();
        const scale = (rect.width / canvas.width) * 10;

        const deltaX = Math.floor((clientX - state.moveStartX) / scale);
        const deltaY = Math.floor((clientY - state.moveStartY) / scale);

        state.selection.x = state.selection.originX + deltaX;
        state.selection.y = state.selection.originY + deltaY;
    }

    deselect() {
        this.commitSelection();
    }
}




class Preview3DManager {
    constructor(canvasManager) {
        this.canvasManager = canvasManager;
        this.scene = document.getElementById('scene');
        this.container = document.getElementById('editor-container-3d');
    }

    init() {
        this.setupControls();
        this.updateFaces();
    }

    setupControls() {
        this.container.addEventListener('mousedown', (e) => {
            if (e.target.classList.contains('face')) return;

            state.isDragging3D = true;
            state.lastMouseX = e.clientX;
            state.lastMouseY = e.clientY;
        });

        window.addEventListener('mousemove', (e) => {
            if (!state.isDragging3D) return;

            const deltaX = e.clientX - state.lastMouseX;
            const deltaY = e.clientY - state.lastMouseY;

            state.rotY += deltaX * 0.5;
            state.rotX -= deltaY * 0.5;

            if (state.currentView === '3d') {
                this.scene.style.transform = `scale(${state.zoom3D}) rotateX(${state.rotX}deg) rotateY(${state.rotY}deg)`;
            }

            state.lastMouseX = e.clientX;
            state.lastMouseY = e.clientY;
        });

        window.addEventListener('mouseup', () => {
            state.isDragging3D = false;
        });
    }

    updateFaces(selection = null, shapePreview = null) {
        const faces = document.querySelectorAll('.face');
        const dataCanvas = this.canvasManager.getDataCanvas();

        faces.forEach(face => {
            const ctx = face.getContext('2d');
            const sx = parseInt(face.dataset.sx);
            const sy = parseInt(face.dataset.sy);

            ctx.clearRect(0, 0, 8, 8);
            ctx.drawImage(dataCanvas, sx, sy, 8, 8, 0, 0, 8, 8);

            // Draw floating selection
            if (selection && selection.floating) {
                const map = MAPPINGS.find(m => m.sx === sx && m.sy === sy);

                if (map) {
                    const ix = Math.max(selection.x, map.vx);
                    const iy = Math.max(selection.y, map.vy);
                    const iRight = Math.min(selection.x + selection.w, map.vx + map.w);
                    const iBottom = Math.min(selection.y + selection.h, map.vy + map.h);

                    if (ix < iRight && iy < iBottom) {
                        const w = iRight - ix;
                        const h = iBottom - iy;
                        const selOffsetX = ix - selection.x;
                        const selOffsetY = iy - selection.y;
                        const faceX = ix - map.vx;
                        const faceY = iy - map.vy;

                        ctx.drawImage(selection.content, selOffsetX, selOffsetY, w, h, faceX, faceY, w, h);
                    }
                }
            }

            // Draw shape preview
            if (shapePreview && state.isDrawing) {
                this.drawShapePreviewOnFace(ctx, sx, sy, shapePreview);
            }

            // Draw grid if enabled
            if (state.showGrid3D) {
                ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
                ctx.lineWidth = 0.5;
                ctx.beginPath();
                for (let i = 1; i < 8; i++) {
                    ctx.moveTo(i, 0);
                    ctx.lineTo(i, 8);
                    ctx.moveTo(0, i);
                    ctx.lineTo(8, i);
                }
                ctx.stroke();
            }
        });
    }

    drawShapePreviewOnFace(ctx, sx, sy, shapePreview) {
        const { pixels, color } = shapePreview;

        ctx.fillStyle = color;
        for (const p of pixels) {
            // Check if pixel is inside this face
            if (p.x >= sx && p.x < sx + 8 && p.y >= sy && p.y < sy + 8) {
                const localX = p.x - sx;
                const localY = p.y - sy;
                ctx.fillRect(localX, localY, 1, 1);
            }
        }
    }

    resetViewAngle() {
        state.rotX = -30;
        state.rotY = 45;

        if (state.currentView === '3d') {
            this.scene.style.transform = `scale(${state.zoom3D}) rotateX(${state.rotX}deg) rotateY(${state.rotY}deg)`;
        } else {
            this.scene.style.transform = `rotateX(${state.rotX}deg) rotateY(${state.rotY}deg)`;
        }
    }

    toggleGrid() {
        state.showGrid3D = !state.showGrid3D;
        const btn = document.getElementById('toggle-grid-3d');
        if (btn) {
            btn.style.background = state.showGrid3D ? 'rgba(0, 122, 204, 0.8)' : 'rgba(0, 0, 0, 0.6)';
        }
        this.updateFaces();
    }

    handleZoom3D(e) {
        e.preventDefault();
        const delta = e.deltaY > 0 ? -0.2 : 0.2;
        state.zoom3D = Math.max(0.5, Math.min(10, state.zoom3D + delta));

        this.scene.style.transform = `scale(${state.zoom3D}) rotateX(${state.rotX}deg) rotateY(${state.rotY}deg)`;
    }
}

class ExportManager {
    constructor(canvasManager) {
        this.canvasManager = canvasManager;
    }

    exportSkin() {
        const dataCanvas = this.canvasManager.getDataCanvas();
        const link = document.createElement('a');
        link.download = 'minecraft_skin.png';
        link.href = dataCanvas.toDataURL();
        link.click();
    }

    exportHeadIcon() {
        const dataCanvas = this.canvasManager.getDataCanvas();
        const size = 512;
        const iconCanvas = document.createElement('canvas');
        iconCanvas.width = size;
        iconCanvas.height = size;
        const ctx = iconCanvas.getContext('2d');
        ctx.imageSmoothingEnabled = false;

        const cx = size / 2;
        const cy = size / 2 + 40;
        const scale = 24;

        const drawLayer = (sx, sy, m, isHat) => {
            ctx.save();

            if (isHat) {
                const centerX = cx;
                const centerY = cy + 2 * scale;

                ctx.translate(centerX, centerY);
                ctx.scale(1.15, 1.15);
                ctx.translate(-centerX, -centerY);
            }

            ctx.translate(cx, cy);
            ctx.transform(...m);
            ctx.drawImage(dataCanvas, sx, sy, 8, 8, 0, 0, 8 * scale, 8 * scale);
            ctx.restore();
        };

        // Transformation matrices
        const mLeft = [1, 0.5, 0, 1.118, -8 * scale, -4 * scale];
        const mRight = [1, -0.5, 0, 1.118, 0, 0];
        const mTop = [1, -0.5, 1, 0.5, -8 * scale, -4 * scale];

        // Draw Inner Head
        drawLayer(8, 0, mTop, false);    // Top
        drawLayer(0, 8, mLeft, false);   // Visual Left -> Right Texture
        drawLayer(8, 8, mRight, false);  // Visual Right -> Front Texture

        // Draw Outer Hat
        if (document.getElementById('toggle-hat').checked) {
            drawLayer(40, 0, mTop, true);   // Top Hat
            drawLayer(40, 8, mRight, true); // Front Hat
            drawLayer(32, 8, mLeft, true);  // Right Hat
        }

        const link = document.createElement('a');
        link.download = 'head_icon.png';
        link.href = iconCanvas.toDataURL();
        link.click();
    }
}




class UIManager {
    constructor() {
        this.paletteContainer = document.getElementById('palette');
        this.colorPicker = document.getElementById('color-picker');
    }

    init() {
        this.initPalette();
        this.updateToolButtons();
        this.updateViewButtons();
    }

    initPalette() {
        DEFAULT_COLORS.forEach(color => {
            const div = document.createElement('div');
            div.className = 'color-swatch';
            div.style.backgroundColor = color;
            div.onclick = () => this.setColor(color);
            this.paletteContainer.appendChild(div);
        });

        // Set initial color
        this.setColor(DEFAULT_COLORS[2]);
    }

    setColor(color) {
        state.currentColor = color;
        this.colorPicker.value = color;

        document.querySelectorAll('.color-swatch').forEach(swatch => {
            const swatchColor = this.rgbToHex(swatch.style.backgroundColor);
            swatch.classList.toggle('active', swatchColor === color);
        });
    }

    rgbToHex(rgb) {
        if (rgb.startsWith('#')) return rgb;
        const [r, g, b] = rgb.match(/\d+/g);
        return "#" + ((1 << 24) + (+r << 16) + (+g << 8) + +b).toString(16).slice(1);
    }

    setTool(tool) {
        state.currentTool = tool;
        this.updateToolButtons();
    }

    updateToolButtons() {
        document.querySelectorAll('.tool-btn').forEach(btn => {
            btn.classList.remove('active');
        });

        const activeBtn = document.getElementById(`tool-${state.currentTool}`);
        if (activeBtn) {
            activeBtn.classList.add('active');
        }

        this.updateSelectionPanel();
    }

    updateSelectionPanel() {
        const panel = document.getElementById('panel-selection');
        if (panel) {
            panel.style.display = state.selection ? 'block' : 'none';
        }
    }

    setView(mode) {
        state.currentView = mode;
        this.updateViewButtons();

        const editorContainer = document.getElementById('editor-container');
        const editorContainer3D = document.getElementById('editor-container-3d');
        const scene = document.querySelector('.scene');
        const previewContainer = document.querySelector('.preview-container');

        if (mode === '3d') {
            editorContainer.style.display = 'none';
            editorContainer3D.style.display = 'flex';

            // Move scene to workspace
            editorContainer3D.appendChild(scene);
            scene.style.transform = `scale(${state.zoom3D}) rotateX(${state.rotX}deg) rotateY(${state.rotY}deg)`;
        } else {
            editorContainer.style.display = 'flex';
            editorContainer3D.style.display = 'none';

            // Move scene back to sidebar
            previewContainer.appendChild(scene);
            scene.style.transform = 'rotateX(-30deg) rotateY(45deg)';
        }
    }

    updateViewButtons() {
        document.getElementById('view-2d').classList.toggle('active', state.currentView === '2d');
        document.getElementById('view-3d').classList.toggle('active', state.currentView === '3d');
    }

    updateBrushSize(value) {
        state.brushSize = parseInt(value);
        document.getElementById('brush-size-val').textContent = state.brushSize;
    }

    updateMirrorMode(checked) {
        state.isMirror = checked;
    }

    updateLayerVisibility() {
        const hatChecked = document.getElementById('toggle-hat').checked;
        const headChecked = document.getElementById('toggle-head').checked;

        document.getElementById('cube-outer').style.display = hatChecked ? 'block' : 'none';
        document.getElementById('cube-inner').style.display = headChecked ? 'block' : 'none';
    }
}




class EventHandlers {
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

        // In 3D view, drawing is handled by handle3DMouseMove.
        // Letting the global 2D mousemove handler run would compute coords from the hidden 2D canvas
        // (rect size 0 -> Infinity/NaN) and can freeze line/circle tools.
        if (state.currentView === '3d') return;

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
                state.shapeStartFace = null; // Clear face tracking
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
        // Don't interfere with rotation control
        if (!e.target.classList.contains('face')) return;

        e.preventDefault();
        state.isDrawing = true;

        const face = e.target;
        const sx = parseInt(face.dataset.sx);
        const sy = parseInt(face.dataset.sy);

        const localX = Math.floor(e.offsetX / 10);
        const localY = Math.floor(e.offsetY / 10);

        if (localX < 0 || localX >= 8 || localY < 0 || localY >= 8) return;

        const targetX = sx + localX;
        const targetY = sy + localY;
        const visualCoords = this.sourceToVisual(targetX, targetY);

        // Handle selection tool
        if (state.currentTool === 'select') {
            if (visualCoords) {
                state.isSelecting = true;
                this.app.selectionManager.startSelection(visualCoords.x, visualCoords.y);
                this.app.render();
            }
            return;
        }

        // Handle move tool
        if (state.currentTool === 'move') {
            this.app.selectionManager.startMove(e.clientX, e.clientY);
            this.app.render();
            return;
        }

        // Handle shape tools - store which face we started on
        if (state.currentTool === 'line' || state.currentTool === 'circle') {
            state.shapeStartX = targetX;
            state.shapeStartY = targetY;
            state.shapeCurrentX = targetX;
            state.shapeCurrentY = targetY;
            state.shapeStartFace = { sx, sy }; // Track starting face
            // Don't update preview on mousedown, just set starting point
            return;
        }

        // Handle color picker
        if (state.currentTool === 'picker') {
            const color = this.app.canvasManager.getPixelColor(targetX, targetY);
            if (color) this.app.ui.setColor(color);
            state.isDrawing = false;
            return;
        }

        // Handle other drawing tools
        this.handle3DToolAction(e);
    }

    handle3DMouseMove(e) {
        if (!state.isDrawing) return;
        if (!e.target.classList.contains('face')) return;

        e.preventDefault();

        const face = e.target;
        const sx = parseInt(face.dataset.sx);
        const sy = parseInt(face.dataset.sy);

        const localX = Math.floor(e.offsetX / 10);
        const localY = Math.floor(e.offsetY / 10);

        if (localX < 0 || localX >= 8 || localY < 0 || localY >= 8) return;

        const targetX = sx + localX;
        const targetY = sy + localY;
        const visualCoords = this.sourceToVisual(targetX, targetY);

        // Handle selection tool
        if (state.currentTool === 'select') {
            if (visualCoords && state.isSelecting) {
                this.app.selectionManager.updateSelection(visualCoords.x, visualCoords.y);
                this.app.render();
            }
            return;
        }

        // Handle move tool
        if (state.currentTool === 'move') {
            const canvas = this.app.canvasManager.canvas;
            this.app.selectionManager.updateMove(e.clientX, e.clientY, canvas);
            this.app.render();
            return;
        }

        // Handle shape tools - only allow within same face to prevent freeze
        if (state.currentTool === 'line' || state.currentTool === 'circle') {
            // Only update if we're still on the same face we started on
            if (state.shapeStartFace && state.shapeStartFace.sx === sx && state.shapeStartFace.sy === sy) {
                // Only update if coordinates actually changed
                if (state.shapeCurrentX !== targetX || state.shapeCurrentY !== targetY) {
                    state.shapeCurrentX = targetX;
                    state.shapeCurrentY = targetY;
                    // Schedule render on next frame to throttle updates
                    if (!state.shapeUpdatePending) {
                        state.shapeUpdatePending = true;
                        requestAnimationFrame(() => {
                            state.shapeUpdatePending = false;
                            const shapePreview = this.getShapePreview();
                            this.app.preview3D.updateFaces(state.selection, shapePreview);
                        });
                    }
                }
            }
            return;
        }

        // Handle other drawing tools
        this.handle3DToolAction(e);
    }

    handle3DToolAction(e) {
        const face = e.target;
        const sx = parseInt(face.dataset.sx);
        const sy = parseInt(face.dataset.sy);

        const localX = Math.floor(e.offsetX / 10);
        const localY = Math.floor(e.offsetY / 10);

        if (localX < 0 || localX >= 8 || localY < 0 || localY >= 8) return;

        const targetX = sx + localX;
        const targetY = sy + localY;

        // Only for pen, eraser, shade, bucket tools
        this.app.tools.applyToolWithMirror(targetX, targetY, false);
        this.app.render();
    }

    sourceToVisual(sourceX, sourceY) {
        // Convert source coordinates back to visual coordinates
        // Import MAPPINGS from constants
        return this.app.canvasManager.getVisualFromSource(sourceX, sourceY);
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
        if (!state.isDrawing) return null;
        if (state.currentTool !== 'line' && state.currentTool !== 'circle') return null;

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











class MinecraftHeadEditor {
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
