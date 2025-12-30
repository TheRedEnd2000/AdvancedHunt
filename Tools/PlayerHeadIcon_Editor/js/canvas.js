import {CANVAS_SCALE, MAPPINGS, SKIN_HEIGHT, SKIN_WIDTH} from './constants.js';
import {state} from './state.js';

export class CanvasManager {
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
