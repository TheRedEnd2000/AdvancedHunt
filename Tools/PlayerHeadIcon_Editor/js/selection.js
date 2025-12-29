import {MAPPINGS} from './constants.js';
import {state} from './state.js';

export class SelectionManager {
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
