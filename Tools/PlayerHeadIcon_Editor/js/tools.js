import {MAPPINGS, SKIN_HEIGHT, SKIN_WIDTH} from './constants.js';
import {state} from './state.js';

export class Tools {
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
export class ShapeTools {
    static getLinePixels(x0, y0, x1, y1) {
        const pixels = [];
        const dx = Math.abs(x1 - x0);
        const dy = Math.abs(y1 - y0);
        const sx = (x0 < x1) ? 1 : -1;
        const sy = (y0 < y1) ? 1 : -1;
        let err = dx - dy;

        while (true) {
            pixels.push({ x: x0, y: y0 });
            if (x0 === x1 && y0 === y1) break;
            const e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
        return pixels;
    }

    static getCirclePixels(x0, y0, x1, y1) {
        const pixels = [];
        const radius = Math.floor(Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2)));
        let x = radius;
        let y = 0;
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
        }
        return pixels;
    }
}
