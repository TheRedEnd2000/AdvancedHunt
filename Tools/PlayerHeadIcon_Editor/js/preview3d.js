import {MAPPINGS} from './constants.js';
import {state} from './state.js';

export class Preview3DManager {
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
